/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.translation.RegistrationTranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Maintains a simple registry of available {@link TranslationActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationTranslationActionsRegistry extends TypesRegistryBase<RegistrationTranslationActionFactory>
{
	@Autowired
	public RegistrationTranslationActionsRegistry(List<RegistrationTranslationActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(RegistrationTranslationActionFactory from)
	{
		return from.getName();
	}
}