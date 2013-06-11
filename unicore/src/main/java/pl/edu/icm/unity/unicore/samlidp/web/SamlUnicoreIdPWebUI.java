/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.SamlPreferences.SPSettings;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.samlidp.web.SamlIdPWebUI;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.unicore.samlidp.SamlPreferencesWithETD;
import pl.edu.icm.unity.unicore.samlidp.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.saml.AuthnWithETDResponseProcessor;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.security.etd.DelegationRestrictions;


/**
 * The main UI of the SAML web IdP. It is an extension of the {@link SamlIdPWebUI}, adding a possibility
 * to configure UNICORE bootstrap ETD generation and using the {@link AuthnWithETDResponseProcessor}.
 *  
 * @author K. Benedyczak
 */
@Component("SamlUnicoreIdPWebUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class SamlUnicoreIdPWebUI extends SamlIdPWebUI implements UnityWebUI
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlUnicoreIdPWebUI.class);

	private static final long MS_IN_DAY = 24*3600*1000;
	private AuthnWithETDResponseProcessor samlWithEtdProcessor;
	protected CheckBox generateETD;
	protected Slider validityDays;

	@Autowired
	public SamlUnicoreIdPWebUI(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan, FreemarkerHandler freemarkerHandler,
			AttributeHandlerRegistry handlersRegistry, PreferencesManagement preferencesMan)
	{
		super(msg, identitiesMan, attributesMan, freemarkerHandler, handlersRegistry, preferencesMan);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		SAMLAuthnContext samlCtx = getContext();
		samlWithEtdProcessor = new AuthnWithETDResponseProcessor(samlCtx, Calendar.getInstance());
		super.appInit(request);
	}
	
	private SamlPreferencesWithETD getPreferencesWithETD() throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(String.valueOf(ae.getEntityId()));
		String raw = preferencesMan.getPreference(entity, SamlPreferencesWithETD.ID);
		SamlPreferencesWithETD ret = new SamlPreferencesWithETD();
		ret.setSerializedConfiguration(raw);
		return ret;
	}
	
	private void savePreferencesWithETD(SamlPreferencesWithETD preferences) throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(String.valueOf(ae.getEntityId()));
		preferencesMan.setPreference(entity, SamlPreferencesWithETD.ID, preferences.getSerializedConfiguration());
	}	
	
	@Override
	protected void createExposedDataPart(VerticalLayout contents)
	{
		Panel exposedInfoPanel = new Panel();
		contents.addComponent(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setMargin(true);
		eiLayout.setSpacing(true);
		exposedInfoPanel.setContent(eiLayout);
		try
		{
			createIdentityPart(eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createAttributesPart(eiLayout);
			eiLayout.addComponent(new Label("<br>", ContentMode.HTML));
			createETDPart(eiLayout);
		} catch (SAMLRequesterException e)
		{
			//we kill the session as the user may want to log as different user if has access to several entities.
			log.debug("SAML problem when handling client request", e);
			handleException(e, true);
			return;
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			handleException(e, true);
			return;
		}
		
		rememberCB = new CheckBox("Remember the settings for this service and do not show this dialog again");
		contents.addComponent(rememberCB);
	}

	protected void createETDPart(VerticalLayout eiLayout)
	{
		Label titleL = new Label(msg.getMessage("SamlUnicoreIdPWebUI.gridSettings"));
		titleL.setStyleName(Styles.bold.toString());
		
		final Label infoVal = new Label();
		generateETD = new CheckBox(msg.getMessage("SamlUnicoreIdPWebUI.generateETD"));
		generateETD.setImmediate(true);
		generateETD.setValue(true);
		generateETD.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean how = generateETD.getValue();
				validityDays.setEnabled(how);
				infoVal.setEnabled(how);
			}
		});
		validityDays = new Slider(1, 90);
		validityDays.setValue(14d);
		validityDays.setSizeFull();
		validityDays.setImmediate(true);
		validityDays.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				int days = validityDays.getValue().intValue();
				infoVal.setValue(msg.getMessage("SamlUnicoreIdPWebUI.etdValidity", days));
			}
		});
		infoVal.setValue(msg.getMessage("SamlUnicoreIdPWebUI.etdValidity", 
				String.valueOf(validityDays.getValue().intValue())));
		
		eiLayout.addComponents(titleL, generateETD, infoVal, validityDays);
	}
	
	
	@Override
	protected void loadPreferences(SAMLAuthnContext samlCtx)
	{
		try
		{
			SamlPreferencesWithETD preferences = getPreferencesWithETD();
			String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
			SPSettings baseSettings = preferences.getSPSettings(samlRequester);
			SPETDSettings settings = preferences.getSPETDSettings(samlRequester);
			updateETDUIFromPreferences(settings, samlCtx);
			super.updateUIFromPreferences(baseSettings, samlCtx);
		} catch (Exception e)
		{
			log.error("Engine problem when processing stored preferences", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			handleException(e, true);
			return;
		}
	}
	
	protected void updateETDUIFromPreferences(SPETDSettings settings, SAMLAuthnContext samlCtx) throws EngineException
	{
		if (settings == null)
			return;
		generateETD.setValue(settings.isGenerateETD());
		long validity = settings.getEtdValidity();
		validity /= MS_IN_DAY;
		validityDays.setValue((double)validity);
	}
	
	/**
	 * Applies UI selected values to the given preferences object
	 * @param preferences
	 * @param samlCtx
	 * @param defaultAccept
	 * @throws EngineException
	 */
	protected void updatePreferencesFromUI(SamlPreferencesWithETD preferences, SAMLAuthnContext samlCtx, 
			boolean defaultAccept) throws EngineException
	{
		super.updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
		if (!rememberCB.getValue())
			return;
		String samlRequester = samlCtx.getRequest().getIssuer().getStringValue();
		SPETDSettings settings = preferences.getSPETDSettings(samlRequester);
		if (settings == null)
		{
			settings = new SPETDSettings();
			preferences.setSPETDSettings(samlRequester, settings);
		}
		settings.setGenerateETD(generateETD.getValue());
		long validity = (long)validityDays.getValue().doubleValue();
		settings.setEtdValidity(validity*MS_IN_DAY);
	}
	
	@Override
	protected void storePreferences(boolean defaultAccept)
	{
		try
		{
			SAMLAuthnContext samlCtx = getContext();
			SamlPreferencesWithETD preferences = getPreferencesWithETD();
			updatePreferencesFromUI(preferences, samlCtx, defaultAccept);
			savePreferencesWithETD(preferences);
		} catch (EngineException e)
		{
			log.error("Unable to store user's preferences", e);
		}
	}
	
	protected DelegationRestrictions getRestrictions()
	{
		if (!generateETD.getValue())
			return null;
		
		double value = validityDays.getValue();
		long ms = ((long)value)*MS_IN_DAY;
		Date start = new Date();
		Date end = new Date(start.getTime() + ms);
		return new DelegationRestrictions(start, end, -1);
	}
	
	@Override
	protected void returnSamlResponse(ResponseDocument respDoc)
	{
		VaadinSession.getCurrent().setAttribute(ResponseDocument.class, respDoc);
		String thisAddress = endpointDescription.getContextAddress() + 
				SamlUnicoreIdPWebEndpointFactory.SERVLET_PATH;
		VaadinSession.getCurrent().addRequestHandler(new SendResponseRequestHandler());
		Page.getCurrent().open(thisAddress, null);		
	}

	
	@Override
	protected void confirm()
	{
		storePreferences(true);
		ResponseDocument respDoc;
		try
		{
			Collection<Attribute<?>> attributes = getUserFilteredAttributes();
			respDoc = samlWithEtdProcessor.processAuthnRequest(selectedIdentity, attributes, 
					getRestrictions());
		} catch (Exception e)
		{
			handleException(e, false);
			return;
		}
		returnSamlResponse(respDoc);
	}
}