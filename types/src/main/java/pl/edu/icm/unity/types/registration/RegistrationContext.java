/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.Constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Describes registration context, i.e. circumstances and environment at the request submission.
 * This data influences the submitted request's processing.
 * @author K. Benedyczak
 */
public class RegistrationContext
{
	/**
	 * Describes how the registration form was triggered.
	 * @author K. Benedyczak
	 */
	public enum TriggeringMode {
		/**
		 * User selected registration manually on one of login screens
		 */
		manualAtLogin, 
		
		/**
		 * User entered a well-known registration form link
		 */
		manualStandalone, 

		/**
		 * The form is being filled from the AdminUI
		 */
		manualAdmin, 
		
		/**
		 * Form was shown after a successful remote authentication 
		 * which was not mapped to a local entity by an input transaltion profile. 
		 */
		afterRemoteLogin
	}
	
	public final boolean tryAutoAccept;
	public final boolean isOnIdpEndpoint;
	public final TriggeringMode triggeringMode;
	
	public RegistrationContext(boolean tryAutoAccept, boolean isOnIdpEndpoint,
			TriggeringMode triggeringMode)
	{
		this.tryAutoAccept = tryAutoAccept;
		this.isOnIdpEndpoint = isOnIdpEndpoint;
		this.triggeringMode = triggeringMode;
	}
	
	@JsonCreator
	public RegistrationContext(JsonNode object)
	{
		this(object.get("tryAutoAccept").asBoolean(),
			object.get("isOnIdpEndpoint").asBoolean(),
			TriggeringMode.valueOf(object.get("triggeringMode").asText()));
	}
	
	@JsonValue
	public JsonNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("tryAutoAccept", tryAutoAccept);
		root.put("isOnIdpEndpoint", isOnIdpEndpoint);
		root.put("triggeringMode", triggeringMode.name());
		return root;
	}
}
