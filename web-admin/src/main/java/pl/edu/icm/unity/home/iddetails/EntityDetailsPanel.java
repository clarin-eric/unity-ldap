/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.sql.Date;
import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.HtmlLabel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Presents a complete and comprehensive information about a single entity. No editing is possible.
 * Targeted for admin user.
 * @author K. Benedyczak
 */
public class EntityDetailsPanel extends FormLayout
{
	private UnityMessageSource msg;
	private Label id;
	private Label status;
	private Label scheduledAction;
	private HtmlLabel identities;
	private Label credReq;
	private HtmlLabel credStatus;
	private HtmlLabel groups;
	
	
	public EntityDetailsPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		id = new Label();
		id.setCaption(msg.getMessage("IdentityDetails.id"));

		status = new Label();
		status.setCaption(msg.getMessage("IdentityDetails.status"));
		
		scheduledAction = new Label();
		scheduledAction.setCaption(msg.getMessage("IdentityDetails.expiration"));
		
		identities = new HtmlLabel(msg);
		identities.setCaption(msg.getMessage("IdentityDetails.identities"));

		credReq = new Label();
		credReq.setCaption(msg.getMessage("IdentityDetails.credReq"));

		credStatus = new HtmlLabel(msg);
		credStatus.setCaption(msg.getMessage("IdentityDetails.credStatus"));

		groups = new HtmlLabel(msg);
		groups.setCaption(msg.getMessage("IdentityDetails.groups"));
		
		addComponents(id, status, scheduledAction, identities, credReq, credStatus, groups);
	}
	
	public void setInput(EntityWithLabel entityWithLabel, Collection<String> groups)
	{
		id.setValue(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		status.setValue(msg.getMessage("EntityState." + entity.getState().toString()));
		
		EntityScheduledOperation operation = entity.getEntityInformation().getScheduledOperation();
		if (operation != null)
		{
			scheduledAction.setVisible(true);
			String action = msg.getMessage("EntityScheduledOperationWithDate." + operation.toString(), 
					entity.getEntityInformation().getScheduledOperationTime());
			scheduledAction.setValue(action);
		} else
		{
			scheduledAction.setVisible(false);
		}
		
		identities.resetValue();
		for (Identity id: entity.getIdentities())
		{
			if (id.isLocal())
			{
				if (id.getType().getIdentityTypeProvider().isVerifiable()
						&& id.getConfirmationInfo() != null)
				{
					ConfirmationInfo conData = id.getConfirmationInfo();	
					if (conData.isConfirmed())
					{
						
						Date dt = new Date(conData.getConfirmationDate());
						identities.addHtmlValueLine(
								"IdentityDetails.identityLocalConfirmed",
								id.getTypeId(),
								id.getType()
										.getIdentityTypeProvider()
										.toPrettyStringNoPrefix(
												id.getValue()),
								dt);

					} else
					{
						identities.addHtmlValueLine(
								"IdentityDetails.identityLocalNotConfirmed",
								id.getTypeId(),
								id.getType()
										.getIdentityTypeProvider()
										.toPrettyStringNoPrefix(
												id.getValue()),
								conData.getSentRequestAmount());
					}
				}
				
				else
				{
					identities.addHtmlValueLine(
							"IdentityDetails.identityLocal",
							id.getTypeId(),
							id.getType()
									.getIdentityTypeProvider()
									.toPrettyStringNoPrefix(
											id.getValue()));
				}
			} else
			{
				String trProfile = id.getTranslationProfile() == null ? 
						"-" : id.getTranslationProfile(); 
				String idValue = id.getType().getIdentityTypeProvider().
						toPrettyStringNoPrefix(id.getValue());
				identities.addHtmlValueLine("IdentityDetails.identityRemote", id.getTypeId(), 
						id.getRemoteIdp(), 
						trProfile, 
						idValue,
						id.getCreationTs(), id.getUpdateTs());
			}
		}
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setValue(credInf.getCredentialRequirementId());
		
		credStatus.resetValue();
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString());
			credStatus.addHtmlValueLine("IdentityDetails.credStatusValue", cred.getKey(), status);
		}
		
		this.groups.resetValue();
		for (String group: groups)
		{
			this.groups.addHtmlValueLine("IdentityDetails.groupLine", group);
		}
	}
}
