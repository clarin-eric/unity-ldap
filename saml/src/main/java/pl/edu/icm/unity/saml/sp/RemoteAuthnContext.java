/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;


/**
 * Context of a remote SAML authentication. Stored in HTTP session. Basically thread safe.
 * @author K. Benedyczak
 */
public class RemoteAuthnContext implements Serializable
{
	private Date creationTime;
	private String relayState;
	private String request;
	private String requestId;
	private String spUrl;
	private String idpUrl;
	private Binding requestBinding;
	private Binding responseBinding;
	private String response;
	private String returnUrl;
	private String groupAttribute;


	public RemoteAuthnContext()
	{
		this.relayState = String.valueOf(ThreadLocalRandom.current().nextLong()) + 
				String.valueOf(ThreadLocalRandom.current().nextLong());
		this.creationTime = new Date();
	}

	public synchronized String getReturnUrl()
	{
		return returnUrl;
	}

	public synchronized Date getCreationTime()
	{
		return creationTime;
	}

	public synchronized String getRelayState()
	{
		return relayState;
	}
	public synchronized String getRequest()
	{
		return request;
	}
	public synchronized void setRequest(String request, String requestId, String spUrl,
			Binding requestBinding, String idpUrl, String returnUrl, String groupAttribute)
	{
		this.request = request;
		this.requestId = requestId;
		this.spUrl = spUrl;
		this.requestBinding = requestBinding;
		this.idpUrl = idpUrl;
		this.returnUrl = returnUrl;
		this.groupAttribute = groupAttribute;
	}
	
	public synchronized void setResponse(String response, Binding responseBinding)
	{
		this.response = response;
		this.responseBinding = responseBinding;
	}

	public synchronized String getIdpUrl()
	{
		return idpUrl;
	}
	public synchronized String getResponse()
	{
		return response;
	}
	public synchronized Binding getRequestBinding()
	{
		return requestBinding;
	}
	public synchronized Binding getResponseBinding()
	{
		return responseBinding;
	}
	public synchronized String getRequestId()
	{
		return requestId;
	}
	public synchronized String getSpUrl()
	{
		return spUrl;
	}

	public synchronized String getGroupAttribute()
	{
		return groupAttribute;
	}
}