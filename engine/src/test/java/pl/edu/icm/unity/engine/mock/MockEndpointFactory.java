/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

@Component
public class MockEndpointFactory implements EndpointFactory
{
	public static final String NAME = "Mock Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock endpoint for tests", Collections.singleton("web"),
			Collections.singletonMap("endPaths", "descEndPaths"));

	private NetworkServer httpServer;
	
	@Autowired
	public MockEndpointFactory(NetworkServer httpServer)
	{
		super();
		this.httpServer = httpServer;
	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new MockEndpoint(httpServer);
	}
}
