/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.oauth.client.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.UIBgThread;

/**
 * UI part of OAuth retrieval. Shows available providers, redirects to the chosen one.
 * @author K. Benedyczak
 */
public class OAuth2RetrievalUI implements VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2RetrievalUI.class);
	private static final long RECHECK_DELAY = 200;
	
	private UnityMessageSource msg;
	private OAuthExchange credentialExchange;
	private ScheduledExecutorService execService;
	private OAuthContextsManagement contextManagement;
	
	private AuthenticationResultCallback callback;

	private ResponseWaitingRunnable responseWaitingRunnable;
	private String selectedProvider;
	private Label messageLabel;
	private Label errorDetailLabel;
	
	public OAuth2RetrievalUI(UnityMessageSource msg, OAuthExchange credentialExchange,
			OAuthContextsManagement contextManagement, ExecutorsService executorsService)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.contextManagement = contextManagement;
		this.execService = executorsService.getService();
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return false;
	}

	@Override
	public Component getComponent()
	{
		installRequestHandler();

		final OAuthClientProperties clientProperties = credentialExchange.getSettings();
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		
		Label title = new Label(clientProperties.getValue(OAuthClientProperties.DISPLAY_NAME));
		title.addStyleName(Reindeer.LABEL_H2);
		ret.addComponent(title);
		
		Set<String> idps = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		if (idps.size() > 1)
		{
			OptionGroup idpChooser = new OptionGroup(msg.getMessage("OAuth2Retrieval.selectProvider"));
			idpChooser.setImmediate(true);
			for (String idpKey: idps)
			{
				String name = clientProperties.getValue(idpKey+OAuthClientProperties.PROVIDER_NAME);
				idpChooser.addItem(idpKey);
				idpChooser.setItemCaption(idpKey, name);
			}
			idpChooser.select(idps.iterator().next());
			idpChooser.setNullSelectionAllowed(false);
			idpChooser.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedProvider = (String) event.getProperty().getValue();
				}
			});
			ret.addComponent(idpChooser);
		} else
		{
			String idpKey = idps.iterator().next();
			String name = clientProperties.getValue(idpKey+OAuthClientProperties.PROVIDER_NAME);
			Label selectedIdp = new Label(msg.getMessage("OAuth2Retrieval.selectedProvider", name));
			ret.addComponent(selectedIdp);
		}
		
		selectedProvider = idps.iterator().next();
		
		messageLabel = new Label();
		messageLabel.setContentMode(ContentMode.HTML);
		messageLabel.addStyleName(Styles.error.toString());
		errorDetailLabel = new Label();
		errorDetailLabel.setContentMode(ContentMode.HTML);
		errorDetailLabel.addStyleName(Styles.italic.toString());
		errorDetailLabel.setVisible(false);
		ret.addComponents(messageLabel, errorDetailLabel);

		checkInProgress();
		
		return ret;
	}

	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
	}

	@Override
	public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void triggerAuthentication()
	{
		startLogin(selectedProvider);
	}

	@Override
	public void cancelAuthentication()
	{
		breakLogin(false);
	}

	@Override
	public String getLabel()
	{
		return credentialExchange.getSettings().getValue(OAuthClientProperties.DISPLAY_NAME);
	}

	@Override
	public Resource getImage()
	{
		return null;
	}

	@Override
	public void clear()
	{
		//nop
	}
	
	private void showError(String message)
	{
		if (message == null)
		{
			messageLabel.setValue("");
			showErrorDetail(null);
			return;
		}
		messageLabel.setValue(message);
	}

	private void showErrorDetail(String message)
	{
		if (message == null)
		{
			errorDetailLabel.setVisible(false);
			errorDetailLabel.setValue("");
			return;
		}
		errorDetailLabel.setVisible(true);
		errorDetailLabel.setValue(message);
	}
	
	private void installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		boolean redirectInstalled = false;
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
				redirectInstalled = true;
		}
		if (!redirectInstalled)
			session.addRequestHandler(new RedirectRequestHandler());
	}
	
	private void checkInProgress()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		switchInProgress(context);
	}
	
	private synchronized void switchInProgress(OAuthContext context)
	{
		boolean inProgress = context != null;
		if (inProgress)
		{
			if (responseWaitingRunnable != null)
				responseWaitingRunnable.forceStop();
			responseWaitingRunnable = new ResponseWaitingRunnable(context);
			execService.schedule(responseWaitingRunnable, RECHECK_DELAY, TimeUnit.MILLISECONDS);
		} else
		{
			if (responseWaitingRunnable != null)
				responseWaitingRunnable.forceStop();
		}
	}

	private void breakLogin(boolean invokeCancel)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
			contextManagement.removeAuthnContext(context.getRelayState());
		}
		switchInProgress(null);
		if (invokeCancel)
			this.callback.cancelAuthentication();
	}
	
	private void startLogin(String providerKey)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			switchInProgress(context);
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("OAuth2Retrieval.loginInProgressError"));
			return;
		}
		
		try
		{
			context = credentialExchange.createRequest(providerKey);
			String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
					VaadinServletService.getCurrentServletRequest().getServletPath();
			context.setReturnUrl(servletPath);
			session.setAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT, context);
			switchInProgress(context);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("OAuth2Retrieval.configurationError"), e);
			log.error("Can not create OAuth2 request", e);
			breakLogin(true);
			return;
		}
		String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
				VaadinServletService.getCurrentServletRequest().getServletPath();
		
		Page.getCurrent().open(servletPath + RedirectRequestHandler.PATH, null);
	}

	
	/**
	 * Called when a OAuth authorization code response is received. This method is not called from the UI thread!
	 */
	private void onAuthzAnswer(OAuthContext authnContext)
	{
		VaadinSession session = VaadinSession.getCurrent();
		session.lock();
		AuthenticationResult authnResult;
		showError(null);
		try
		{
			String reason = null;
			Exception savedException = null;
			try
			{
				authnResult = credentialExchange.verifyOAuthAuthzResponse(authnContext);
			} catch (AuthenticationException e)
			{
				savedException = e;
				reason = ErrorPopup.getHumanMessage(e, "<br>");
				authnResult = e.getResult();
			} catch (Exception e)
			{
				log.error("Runtime error during OAuth2 response processing or principal mapping", e);
				authnResult = new AuthenticationResult(Status.deny, null);
			}
			String regFormForUnknown = credentialExchange.getSettings().getValue(
					authnContext.getProviderConfigKey() + OAuthClientProperties.REGISTRATION_FORM);
			if (authnResult.getStatus() == Status.success)
			{
				showError(null);
				breakLogin(false);
			} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
					regFormForUnknown != null) 
			{
				log.debug("There is a registration form to show for the unknown user: " + 
						regFormForUnknown);
				authnResult.setFormForUnknownPrincipal(regFormForUnknown);
				showError(null);
				breakLogin(true);
			} else
			{
				if (savedException != null)
					log.warn("OAuth2 authorization code verification or processing failed", 
							savedException);
				else
					log.warn("OAuth2 authorization code verification or processing failed");
				if (reason != null)
					showErrorDetail(msg.getMessage("OAuth2Retrieval.authnFailedDetailInfo", reason));
				showError(msg.getMessage("OAuth2Retrieval.authnFailedError"));
				breakLogin(false);
			}

		} finally
		{
			session.unlock();
		}
		callback.setAuthenticationResult(authnResult);
	}

	
	
	/**
	 * Waits for the OAuth answer which should appear in the session's {@link OAuthContext}. 
	 * @author K. Benedyczak
	 */
	private class ResponseWaitingRunnable extends UIBgThread
	{
		private OAuthContext context;
		private boolean stop = false;
		
		public ResponseWaitingRunnable(OAuthContext context)
		{
			this.context = context;
		}

		public void safeRun()
		{
			if (isStopped())
				return;
			if (!context.isAnswerPresent())
			{
				execService.schedule(this, RECHECK_DELAY, TimeUnit.MILLISECONDS);
			} else
			{
				onAuthzAnswer(context);
			}
		}
		
		public synchronized void forceStop()
		{
			stop = true;
		}
		
		private synchronized boolean isStopped()
		{
			return stop;
		}
	}

}
