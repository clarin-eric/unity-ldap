/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;

/**
 * Viewer of {@link EnquiryFormNotifications}
 * @author K. Benedyczak
 */
public class EnquiryFormNotificationsViewer extends BaseFormNotificationsViewer
{
	private SimpleMessageTemplateViewer enquiryToFillTemplate;
	private SimpleMessageTemplateViewer submittedTemplate;
	
	public EnquiryFormNotificationsViewer(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(msg, msgTempMan);
		initMyUI();
	}

	private void initMyUI()
	{
		submittedTemplate = new SimpleMessageTemplateViewer(msg.getMessage(
				"RegistrationFormViewer.submittedTemplate"),
				msg, msgTempMan);

		enquiryToFillTemplate = new SimpleMessageTemplateViewer(
				msg.getMessage("EnquiryFormNotificationsViewer.enquiryToFillTemplate"),
				msg, msgTempMan);
		addComponents(enquiryToFillTemplate, submittedTemplate);
	}
	
	public void clear()
	{
		super.clear();
		enquiryToFillTemplate.clearContent();
		submittedTemplate.clearContent();
	}
	
	public void setValue(EnquiryFormNotifications notCfg)
	{
		super.setValue(notCfg);
		submittedTemplate.setInput(notCfg.getSubmittedTemplate());
		enquiryToFillTemplate.setInput(notCfg.getEnquiryToFillTemplate());
	}
}
