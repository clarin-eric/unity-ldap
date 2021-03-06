/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Factory for {@link MapAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapAttributeActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "mapAttribute";
	private AttributesManagement attrsMan;
	
	@Autowired
	public MapAttributeActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		super(NAME, 
			new ActionParameterDefinition(
				"unityAttribute",
				"TranslationAction.mapAttribute.paramDesc.unityAttribute",
				Type.UNITY_ATTRIBUTE),
			new ActionParameterDefinition(
				"group",
				"TranslationAction.mapAttribute.paramDesc.group",
				Type.UNITY_GROUP),
			new ActionParameterDefinition(
				"expression",
				"TranslationAction.mapAttribute.paramDesc.expression",
				Type.EXPRESSION),
			new ActionParameterDefinition(
				"visibility",
				"TranslationAction.mapAttribute.paramDesc.visibility",
				AttributeVisibility.class),
			new ActionParameterDefinition(
				"effect",
				"TranslationAction.mapAttribute.paramDesc.effect",
				AttributeEffectMode.class));
		
		this.attrsMan = attrsMan;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MapAttributeAction(parameters, getActionType(), attrsMan);
	}
	
	
	public static class MapAttributeAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapAttributeAction.class);
		private final AttributesManagement attrMan;
		private String unityAttribute;
		private String group;
		private AttributeVisibility visibility;
		private Serializable expressionCompiled;
		private AttributeEffectMode mode;
		private AttributeType at;

		public MapAttributeAction(String[] params, TranslationActionType desc, AttributesManagement attrsMan) 
		{
			super(desc, params);
			setParameters(params);
			attrMan = attrsMan;
			try
			{
				at = attrMan.getAttributeTypesAsMap().get(unityAttribute);
			} catch (EngineException e)
			{
				throw new InternalException("Can't get attribute types", e);
			}
			if (at == null)
				throw new IllegalArgumentException("Attribute type " + unityAttribute + " is not known");
		}
		
		@Override
		protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
				String currentProfile)
		{
			MappingResult ret = new MappingResult();
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return ret;
			}
			
			List<Object> typedValues;
			try
			{
				typedValues = convertValues(value, at.getValueType());
			} catch (IllegalAttributeValueException e)
			{
				log.debug("Can't convert attribute values returned by the action's expression "
						+ "to the type of attribute " + unityAttribute + " , skipping it", e);
				return ret;
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Attribute<?> attribute = new Attribute(unityAttribute, at.getValueType(), group, 
					visibility, typedValues, input.getIdpName(), currentProfile);
			MappedAttribute ma = new MappedAttribute(mode, attribute);
			log.debug("Mapped attribute: " + attribute);
			ret.addAttribute(ma);
			return ret;
		}

		public static List<Object> convertValues(Object value, AttributeValueSyntax<?> syntax) 
				throws IllegalAttributeValueException
		{
			List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value);
			List<Object> ret = new ArrayList<Object>(aValues.size());
			for (Object o: aValues)
			{
				Object converted = syntax.convertFromString(o.toString());
				ret.add(converted);
			}
			return ret;
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 5)
				throw new IllegalArgumentException("Action requires exactly 5 parameters");
			unityAttribute = parameters[0];
			group = parameters[1];
			expressionCompiled = MVEL.compileExpression(parameters[2]);
			visibility = AttributeVisibility.valueOf(parameters[3]);
			mode = AttributeEffectMode.valueOf(parameters[4]);
		}
	}

}
