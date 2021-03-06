/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.registration.InvitationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;

import com.vaadin.ui.ComboBox;

/**
 * Editor of {@link RegistrationFormNotifications}
 * @author K. Benedyczak
 */
public class RegistrationFormNotificationsEditor extends BaseFormNotificationsEditor
{
	private ComboBox invitationTemplate;
	private ComboBox submittedTemplate;
	
	public RegistrationFormNotificationsEditor(UnityMessageSource msg,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan) throws EngineException
	{
		super(msg, groupsMan, notificationsMan, msgTempMan);
		initMyUI();
	}

	private void initMyUI() throws EngineException
	{
		submittedTemplate = new CompatibleTemplatesComboBox(SubmitRegistrationTemplateDef.NAME, msgTempMan);
		submittedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		invitationTemplate =  new CompatibleTemplatesComboBox(InvitationTemplateDef.NAME, msgTempMan);
		invitationTemplate.setCaption(msg.getMessage("RegistrationFormViewer.invitationTemplate"));
		addComponents(submittedTemplate, invitationTemplate);
	}
	
	public void setValue(RegistrationFormNotifications toEdit)
	{
		super.setValue(toEdit);
		submittedTemplate.setValue(toEdit.getSubmittedTemplate());
		invitationTemplate.setValue(toEdit.getInvitationTemplate());
	}
	
	public RegistrationFormNotifications getValue()
	{
		RegistrationFormNotifications notCfg = new RegistrationFormNotifications();
		super.fill(notCfg);
		notCfg.setInvitationTemplate((String) invitationTemplate.getValue());
		notCfg.setSubmittedTemplate((String) submittedTemplate.getValue());
		return notCfg;
	}
}
