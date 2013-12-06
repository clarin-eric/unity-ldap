/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Pair: condition and action; configured.
 *  
 * @author K. Benedyczak
 */
public class TranslationRule
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, TranslationRule.class);
	private TranslationAction action;
	private TranslationCondition condition;
	
	public TranslationRule(TranslationAction action, TranslationCondition condition)
	{
		this.action = action;
		this.condition = condition;
	}
	
	public void invoke(RemotelyAuthenticatedInput input) throws EngineException
	{
		if (condition.evaluate(input))
		{
			log.debug("Condition OK");
			action.invoke(input);
		} else
		{
			log.debug("Condition not met");			
		}
	}

	public TranslationAction getAction()
	{
		return action;
	}

	public void setAction(TranslationAction action)
	{
		this.action = action;
	}

	public TranslationCondition getCondition()
	{
		return condition;
	}

	public void setCondition(TranslationCondition condition)
	{
		this.condition = condition;
	}
}