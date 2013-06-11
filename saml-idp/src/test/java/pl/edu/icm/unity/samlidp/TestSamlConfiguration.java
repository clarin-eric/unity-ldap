/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static pl.edu.icm.unity.samlidp.SamlProperties.*;
import org.junit.Test;

import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import eu.unicore.security.canl.CredentialProperties;

public class TestSamlConfiguration
{
	@Test
	public void testGroupChooser() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+ISSUER_URI, "foo");
		
		p.setProperty(P+GROUP_PFX+"1."+GROUP_TARGET, "http://sp.org1");
		p.setProperty(P+GROUP_PFX+"1."+GROUP, "/some/gr");
		p.setProperty(P+GROUP_PFX+"2."+GROUP_TARGET, "sp2");
		p.setProperty(P+GROUP_PFX+"2."+GROUP, "/");
		p.setProperty(P+DEFAULT_GROUP, "/def");
		p.setProperty(P+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_LOCATION, 
				"src/test/resources/demoKeystore.p12");
		p.setProperty(P+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_PASSWORD, 
				"the!uvos");
		SamlProperties cfg = new SamlProperties(p);
		
		GroupChooser chooser = cfg.getGroupChooser();
		assertEquals("/some/gr", chooser.chooseGroup("http://sp.org1"));
		assertEquals("/", chooser.chooseGroup("sp2"));
		assertEquals("/def", chooser.chooseGroup("other"));
	}
	
	@Test
	public void testAttributeFilter() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+ISSUER_URI, "foo");
		
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_TARGET, "sp.*");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_EXCLUDE+"1", "a");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_EXCLUDE+"2", "b");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_INCLUDE+"1", "z");
		
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_TARGET, "gg");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"1", "a");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"2", "b");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"3", "c");

		p.setProperty(P+ATTRIBUTE_FILTER+"3."+ATTRIBUTE_FILTER_TARGET, "qq");
		p.setProperty(P+ATTRIBUTE_FILTER+"3."+ATTRIBUTE_FILTER_INCLUDE+"1", "a");

		p.setProperty(P+DEFAULT_GROUP, "/");
		p.setProperty(P+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_LOCATION, 
				"src/test/resources/demoKeystore.p12");
		p.setProperty(P+CredentialProperties.DEFAULT_PREFIX+CredentialProperties.PROP_PASSWORD, 
				"the!uvos");
		SamlProperties cfg = new SamlProperties(p);
		
		AttributeFilters filter = cfg.getAttributeFilter();
		
		List<Attribute<?>> attributes = getAttrs();
		filter.filter(attributes, "spAAA");
		assertEquals(1, attributes.size());
		assertEquals("z", attributes.get(0).getName());
		
		attributes = getAttrs();
		filter.filter(attributes, "gg");
		assertEquals(2, attributes.size());
		assertEquals("d", attributes.get(0).getName());
		assertEquals("z", attributes.get(1).getName());
		
		attributes = getAttrs();
		filter.filter(attributes, "qq");
		assertEquals(1, attributes.size());
		assertEquals("a", attributes.get(0).getName());
	}
	
	private List<Attribute<?>> getAttrs()
	{
		List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
		attributes.add(new StringAttribute("a", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("b", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("c", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("d", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("z", "/", AttributeVisibility.local, ""));
		return attributes;
	}
}