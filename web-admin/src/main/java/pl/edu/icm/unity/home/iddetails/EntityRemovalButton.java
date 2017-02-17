/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;

import com.vaadin.ui.Button;

/**
 * Button allowing to launch {@link ScheduledEntityRemovalDialog}
 * @author K. Benedyczak
 */
public class EntityRemovalButton extends Button
{
	public EntityRemovalButton(UnityMessageSource msg, long entity, 
			IdentitiesManagement identitiesManagement, 
			IdentitiesManagement insecureIdentitiesManagement, 
			WebAuthenticationProcessor authnProcessor,
			HomeEndpointProperties config)
	{
		super(msg.getMessage("EntityRemovalButton.removeAccount"), Images.delete.getResource());
		addClickListener((event) ->
		{
			if (config.getBooleanValue(HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE))
			{
				RemovalModes removalMode = config.getEnumValue(
						HomeEndpointProperties.REMOVAL_MODE, RemovalModes.class);
				new ImmediateEntityRemovalDialog(msg, entity, insecureIdentitiesManagement, 
						authnProcessor, removalMode).show();
			} else
			{
				new ScheduledEntityRemovalDialog(msg, entity, identitiesManagement, 
						authnProcessor).show();
			}
		});
	}
}
