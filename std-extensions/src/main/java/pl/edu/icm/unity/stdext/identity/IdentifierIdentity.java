/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Opaque identifier identity. It is useful for storing a generic identifier, being a string. The only requirement is
 * that the string must be non-empty.
 * @author K. Benedyczak
 */
@Component
public class IdentifierIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "identifier";
	private static final List<Attribute<?>> empty = Collections.unmodifiableList(new ArrayList<Attribute<?>>(0));
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescription()
	{
		return "Opaque identifier";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		if (value == null || value.trim().length() == 0)
		{
			throw new IllegalIdentityValueException("Identifier must be non empty");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return empty;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("IdentifierIdentity.description");
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("IdentifierIdentity.name");
	}
}