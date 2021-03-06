/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.configuration.PropertiesHelperAPI;

/**
 * Common configuration settings which are shared by both the configuration of a remote OAuth AS
 * when unity authenticates as OAuth RP and particular trusted providers when unity authenticates 
 * as OAuth client.
 *  
 * @author K. Benedyczak
 */
public interface BaseRemoteASProperties extends PropertiesHelperAPI
{
	public static final String PROFILE_ENDPOINT = "profileEndpoint";
	public static final String CLIENT_ID = "clientId";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String CLIENT_AUTHN_MODE = "clientAuthenticationMode";
	public static final String CLIENT_TRUSTSTORE = "httpClientTruststore";
	public static final String CLIENT_HOSTNAME_CHECKING = "httpClientHostnameChecking";
	
	X509CertChainValidator getValidator();
}
