/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Helper class allowing to create string attributes easily.
 * @author K. Benedyczak
 */
public class StringAttribute extends Attribute<String>
{
	public StringAttribute(String name, String groupPath, AttributeVisibility visibility,
			String... values)
	{
		super(name, new StringAttributeSyntax(), groupPath, visibility, asList(values));
	}

	public StringAttribute(String name, String groupPath, AttributeVisibility visibility,
			List<String> values)
	{
		super(name, new StringAttributeSyntax(), groupPath, visibility, values);
	}

	public StringAttribute(String name, String groupPath, AttributeVisibility visibility,
			String value)
	{
		this(name, groupPath, visibility, Collections.singletonList(value));
	}
	
	public StringAttribute()
	{
	}
	
	private static List<String> asList(String[] vals)
	{
		List<String> ret = new ArrayList<String>(vals.length);
		Collections.addAll(ret, vals);
		return ret;
	}
}
