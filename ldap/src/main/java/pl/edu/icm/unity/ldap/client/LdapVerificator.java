/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.credential.CertificateExchange;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attributes and groups is also provided.
 * 
 * @author K. Benedyczak
 */
public class LdapVerificator extends AbstractRemoteVerificator implements PasswordExchange, CertificateExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP_CLIENT, LdapVerificator.class);
	private LdapProperties ldapProperties;
	private LdapClient client;
	private LdapClientConfiguration clientConfiguration;
	private PKIManagement pkiManagement;
	private String translationProfile;
	
	public LdapVerificator(String name, String description, 
			TranslationProfileManagement profileManagement, InputTranslationEngine trEngine,
			PKIManagement pkiManagement, String exchangeId)
	{
		super(name, description, exchangeId, profileManagement, trEngine);
		this.client = new LdapClient(name);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			ldapProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize LDAP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			ldapProperties = new LdapProperties(properties);
			translationProfile = ldapProperties.getValue(LdapProperties.TRANSLATION_PROFILE);
			clientConfiguration = new LdapClientConfiguration(ldapProperties, pkiManagement);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator(?)", e);
		}
	}

	@Override
	public AuthenticationResult checkPassword(String username, String password, SandboxAuthnResultCallback callback)
	{
		RemoteAuthnState state = startAuthnResponseProcessing(callback, 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_LDAP_CLIENT);
		
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(username, password);
			return getResult(input, translationProfile, state);
		} catch (Exception e)
		{
			finishAuthnResponseProcessing(state, e);
			return new AuthenticationResult(Status.deny, null, null);
		}
	}
	

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(
			String username, String password) throws AuthenticationException, LdapAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.bindAndSearch(username, password, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new AuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}	

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}

	@Override
	public AuthenticationResult checkCertificate(X509Certificate[] chain, 
			SandboxAuthnResultCallback sandboxCallback)
	{
		RemoteAuthnState state = startAuthnResponseProcessing(sandboxCallback, 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_LDAP_CLIENT);
		
		try
		{
			RemotelyAuthenticatedInput input = searchRemotelyAuthenticatedInput(
					chain[0].getSubjectX500Principal().getName());
			return getResult(input, translationProfile, state);
		} catch (Exception e)
		{
			log.debug("LDAP authentication with certificate failed", e);
			finishAuthnResponseProcessing(state, e);
			return new AuthenticationResult(Status.deny, null, null);
		}
	}
	
	private RemotelyAuthenticatedInput searchRemotelyAuthenticatedInput(
			String dn) throws AuthenticationException, LdapAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.search(dn, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new AuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}
}
