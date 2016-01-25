/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Component to edit or add input translation profile
 * 
 * @author P. Piernik
 * 
 */
public class OutputTranslationProfileEditor extends TranslationProfileEditor<OutputTranslationAction, OutputTranslationRule>
{
	public OutputTranslationProfileEditor(UnityMessageSource msg,
			OutputTranslationActionsRegistry registry, OutputTranslationProfile toEdit, 
			ActionParameterComponentFactory.Provider actionComponentProvider) throws EngineException
	{
		super(msg, registry, toEdit, OutputTranslationRule.FACTORY, actionComponentProvider);
	}

	@Override
	public OutputTranslationProfile createProfile(String name, List<OutputTranslationRule> trules)
	{
		return new OutputTranslationProfile(name, trules);
	}
}
