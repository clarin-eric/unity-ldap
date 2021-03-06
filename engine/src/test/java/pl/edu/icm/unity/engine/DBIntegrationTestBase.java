/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.CertificateVerificatorFactory;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Same as {@link SecuredDBIntegrationTestBase} but additionally puts admin user in authentication context
 * so all operations are authZed 
 * @author K. Benedyczak
 */
public abstract class DBIntegrationTestBase extends SecuredDBIntegrationTestBase
{
	public static final String CRED_REQ_PASS = "cr-pass";
	
	@Autowired
	private SessionManagement sessionMan;
	
	@Before
	public void setupAdmin() throws Exception
	{
		setupUserContext("admin", false);
	}
	
	@After
	public void clearAuthnCtx() throws EngineException
	{
		InvocationContext.setCurrent(null);
	}	
	
	protected void setupUserContext(String user, boolean outdated) throws Exception
	{
		setupUserContext(sessionMan, identityResolver, user, outdated);
	}

	public static void setupUserContext(SessionManagement sessionMan, IdentityResolver identityResolver,
			String user, boolean outdated) throws Exception
	{
		EntityWithCredential entity = identityResolver.resolveIdentity(user, new String[] {UsernameIdentity.ID}, 
				EngineInitialization.DEFAULT_CREDENTIAL);
		InvocationContext virtualAdmin = new InvocationContext(null, getDefaultRealm());
		LoginSession ls = sessionMan.getCreateSession(entity.getEntityId(), getDefaultRealm(),
				user, outdated, null);
		virtualAdmin.setLoginSession(ls);
		virtualAdmin.setLocale(Locale.ENGLISH);
		//override for tests: it can happen that existing session is returned, therefore old state of cred is
		// there.
		ls.setUsedOutdatedCredential(outdated);
		InvocationContext.setCurrent(virtualAdmin);
	}
	
	private static AuthenticationRealm getDefaultRealm()
	{
		return new AuthenticationRealm("DEFAULT_AUTHN_REALM", 
				"For tests", 5, 10, -1, 30*60);
	}
	
	protected Identity createUsernameUser(String role) throws Exception
	{
		return createUsernameUser("user1", role, "mockPassword1");
	}

	/**
	 * Creates entity with username identity, password and given role. 
	 * The {@link #setupPasswordAuthn()} must be called before. 
	 * @param username
	 * @param role
	 * @return
	 * @throws Exception
	 */
	protected Identity createUsernameUser(String username, String role, String password) throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username), 
				CRED_REQ_PASS, EntityState.valid, false);
		idsMan.setEntityCredential(new EntityParam(added1), "credential1", 
				new PasswordToken(password).toJson());
		if (role != null)
		{
			EnumAttribute sa = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, role);
			attrsMan.setAttribute(new EntityParam(added1), sa, false);
		}
		return added1;
	}
	
	protected void createCertUser() throws EngineException
	{
		Identity added2 = createCertUserNoPassword(null);
		idsMan.setEntityCredential(new EntityParam(added2), "credential1", 
				new PasswordToken("mockPassword2").toJson());
	}

	protected Identity createCertUserNoPassword(String role) throws EngineException
	{
		Identity added2 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user2"), 
				"cr-certpass", EntityState.valid, false);
		idsMan.addIdentity(new IdentityParam(X500Identity.ID, "CN=Test UVOS,O=UNICORE,C=EU"), 
				new EntityParam(added2), false);
		if (role != null)
		{
			EnumAttribute sa = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, role);
			attrsMan.setAttribute(new EntityParam(added2), sa, false);
		}
		return added2;
	}
		
	protected void setupPasswordAuthn() throws EngineException
	{
		setupPasswordAuthn(4, true);
	}

	protected void setupPasswordAuthn(int minLen, boolean denySeq) throws EngineException
	{
		CredentialDefinition credDef = new CredentialDefinition(
				PasswordVerificatorFactory.NAME, "credential1");
		credDef.setJsonConfiguration("{\"minLength\": " + minLen + ", " +
				"\"historySize\": 5," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": " + denySeq + "," +
				"\"maxAge\": 30758400}");
		authnMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements(CRED_REQ_PASS, "", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);

		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, credDef.getName());
	}
	
	
	protected void setupPasswordAndCertAuthn() throws EngineException
	{
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificatorFactory.NAME, "credential2");
		credDef2.setJsonConfiguration("");
		authnMan.addCredentialDefinition(credDef2);
		
		CredentialRequirements cr2 = new CredentialRequirements("cr-cert", "", 
				Collections.singleton(credDef2.getName()));
		authnMan.addCredentialRequirement(cr2);

		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, "credential1", credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-certpass", "", creds);
		authnMan.addCredentialRequirement(cr3);
	}

}
