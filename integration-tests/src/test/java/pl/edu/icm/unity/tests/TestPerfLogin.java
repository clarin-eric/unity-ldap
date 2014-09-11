/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import pl.edu.icm.unity.rest.MockRESTEndpointFactory;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
/**
 * Test user login performance
 * 
 * @author P.Piernik
 * 
 */
public class TestPerfLogin extends IntegrationTestBase
{
	public final int USERS_MULTIPLIER = 10; 
	public final int USERS = 100; 
	
	@Test
	public void testLogin() throws Exception
	{
		
		addUsers(USERS_MULTIPLIER * USERS);

		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 10, 100, -1, 600);
		realmsMan.addRealm(realm);

		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_REST_PASS)));
		endpointMan.deploy(MockRESTEndpointFactory.NAME, "endpoint1", "/mock", "desc",
				authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());
		httpServer.start();
		HttpHost host = new HttpHost("localhost", 53456, "https");

		// warn-up ...login user
		for (int i = 0; i < USERS_MULTIPLIER / 10 * USERS; i++)
		{
			DefaultHttpClient client = getClient();
			BasicHttpContext localcontext = getClientContext(client, host, "user" + i,
					"PassWord8743#%$^&*");
			HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
			HttpResponse response = client.execute(host, get, localcontext);
			assertEquals(response.getStatusLine().toString(), 200, response
					.getStatusLine().getStatusCode());
		}

		for (int j = 0; j < USERS_MULTIPLIER - 1; j++)
		{

			timer.startTimer();
			for (int i = j * USERS; i < (j + 1) * USERS; i++)
			{
				DefaultHttpClient client = getClient();
				BasicHttpContext localcontext = getClientContext(client, host,
						"user" + i, "PassWord8743#%$^&*");
				HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
				HttpResponse response = client.execute(host, get, localcontext);
				assertEquals(response.getStatusLine().toString(), 200, response
						.getStatusLine().getStatusCode());
			}
			timer.stopTimer(USERS, "Login user");
		}
		timer.calculateResults("Login user");

	}
}
