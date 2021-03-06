/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Set;

import pl.edu.icm.unity.types.DescribedObjectImpl;


/**
 * Set of credentials. It is applied to entities, to define what credentials must be defined/updated.
 * @author K. Benedyczak
 */
public class CredentialRequirements extends DescribedObjectImpl
{
	private Set<String> requiredCredentials;

	public CredentialRequirements()
	{
		super();
	}

	public CredentialRequirements(String name, String description,
			Set<String> requiredCredentials)
	{
		super(name, description);
		this.requiredCredentials = requiredCredentials;
	}

	public Set<String> getRequiredCredentials()
	{
		return requiredCredentials;
	}

	public void setRequiredCredentials(Set<String> requiredCredentials)
	{
		this.requiredCredentials = requiredCredentials;
	}
}
