/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Filter outgoing identities by name and or type. Name filter is specified using regular expressions
 *   
 * @author K. Benedyczak
 */
@Component
public class FilterIdentityActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "filterIdentity";
	
	public FilterIdentityActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"identity",
				"TranslationAction.filterIdentity.paramDesc.idType",
				Type.UNITY_ID_TYPE),
		new ActionParameterDefinition(
				"identityValueRegexp",
				"TranslationAction.filterIdentity.paramDesc.idValueReqexp",
				Type.EXPRESSION));
	}

	@Override
	public FilterIdentityAction getInstance(String... parameters)
	{
		return new FilterIdentityAction(parameters, getActionType());
	}
	
	public static class FilterIdentityAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterIdentityAction.class);
		private String identity;
		private Pattern idValueRegexp;

		public FilterIdentityAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Set<IdentityParam> copy = new HashSet<IdentityParam>(result.getIdentities());
			for (IdentityParam i: copy)
			{
				if ((identity == null || i.getTypeId().equals(identity)) &&
						(idValueRegexp == null || idValueRegexp.matcher(i.getValue()).matches()))
				{
					log.debug("Filtering the identity " + i.toString());
					result.getIdentities().remove(i);
				}
			}
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			identity = parameters[0];
			idValueRegexp = parameters[1] == null ? null : Pattern.compile(parameters[1]);
		}

	}

}
