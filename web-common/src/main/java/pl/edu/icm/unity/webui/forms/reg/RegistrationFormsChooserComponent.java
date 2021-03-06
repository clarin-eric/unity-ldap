/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;



/**
 * Responsible for showing registration forms list and allowing to launch editor of a chosen one.
 * Useful for showing filtered list of forms available for end user.  
 *  
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationFormsChooserComponent extends VerticalLayout
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormsChooserComponent.class);
	protected UnityMessageSource msg;
	protected RegistrationsManagement registrationsManagement;
	protected RegistrationFormDialogProvider formLauncher;

	protected Collection<String> allowedForms;
	protected List<RegistrationForm> displayedForms;
	protected VerticalLayout main;
	protected EventsBus bus;
	private Callback callback;
	protected TriggeringMode mode;
	
	@Autowired
	public RegistrationFormsChooserComponent(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			InsecureRegistrationFormLauncher formLauncher)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.formLauncher = formLauncher;
		bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<RegistrationFormChangedEvent>()
		{
			@Override
			public void handleEvent(RegistrationFormChangedEvent event)
			{
				try
				{
					refresh();
				} catch (EngineException e)
				{
					log.error("Can't refresh registration forms", e);
				}
			}
		}, RegistrationFormChangedEvent.class);
	}
	
	public void setAllowedForms(Collection<String> allowed)
	{
		allowedForms = allowed;
	}
	
	public List<RegistrationForm> getDisplayedForms()
	{
		return displayedForms;
	}
	
	
	protected void refresh() throws EngineException
	{
		if (main == null)
			return;
		main.removeAllComponents();
		List<RegistrationForm> forms = registrationsManagement.getForms();
		displayedForms = new ArrayList<>(forms.size());
		boolean available = false;
		for (RegistrationForm form: forms)
		{
			if (!form.isPubliclyAvailable())
				continue;
			if (allowedForms != null && !allowedForms.contains(form.getName()))
				continue;
			Button button = new Button(form.getDisplayedName().getValue(msg));
			button.setStyleName(Styles.vButtonLink.toString());
			button.addClickListener(new ButtonListener(form, mode));
			main.addComponent(button);
			displayedForms.add(form);
			available = true;
		}
		
		if (!available)
			main.addComponent(new Label(msg.getMessage("RegistrationFormsChooserComponent.noFormsInfo")));
	}
	
	public void initUI(TriggeringMode mode)
	{
		this.mode = mode;
		try
		{
			removeAllComponents();
			main = new VerticalLayout();
			main.setSpacing(true);
			main.setMargin(true);
			addComponent(main);
			refresh();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationFormsChooserComponent.errorGetForms"), e);
			removeAllComponents();
			addComponent(error);
		}
	}
	
	public void setCallback(Callback callback)
	{
		this.callback = callback;
	}

	private class ButtonListener implements ClickListener
	{
		private RegistrationForm form;
		private TriggeringMode mode;

		public ButtonListener(RegistrationForm form, TriggeringMode mode)
		{
			this.form = form;
			this.mode = mode;
		}
		
		@Override
		public void buttonClick(ClickEvent event)
		{
			formLauncher.showRegistrationDialog(form, 
					RemotelyAuthenticatedContext.getLocalContext(), 
					mode, this::handleError);
			if (callback != null)
				callback.closed();
		}
		
		private void handleError(Exception error)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("RegistrationFormsChooserComponent.errorShowFormEdit"), error);
		}
	}
	
	public interface Callback
	{
		void closed();
	}
}
