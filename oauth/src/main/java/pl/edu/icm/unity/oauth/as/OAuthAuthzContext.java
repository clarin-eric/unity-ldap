/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.types.basic.Attribute;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;

import eu.emi.security.authn.x509.X509Credential;

/**
 * Context stored in HTTP session maintaining authorization token.
 * 
 * TODO - simplify this class: it duplicates the {@link OAuthASProperties} contents to a large degree.
 * @author K. Benedyczak
 */
public class OAuthAuthzContext
{
	public static final long AUTHN_TIMEOUT = 900000;
	private AuthorizationRequest request;
	private Date timestamp;
	private URI returnURI;
	private String clientName;
	private String clientUsername;
	private long clientEntityId;
	private Attribute<BufferedImage> clientLogo;
	private String translationProfile;
	private String usersGroup;
	private Set<ScopeInfo> effectiveRequestedScopes = new HashSet<OAuthAuthzContext.ScopeInfo>();
	private Set<String> requestedAttrs = new HashSet<>();
	private GrantFlow flow;
	private boolean openIdMode;
	private boolean skipConsent;
	private int codeTokenValidity;
	private int accessTokenValidity;
	private int maxExtendedAccessTokenValidity;
	private int idTokenValidity;
	private String issuerName;
	private X509Credential credential;
	private String subjectIdentityType;
	private OAuthASProperties properties;
	

	public OAuthAuthzContext(AuthorizationRequest request, OAuthASProperties properties,
			int accessTokenValidity, int maxExtendedAccessTokenValidity,
			int codeTokenValidity,
			int idTokenValidity, String issuerName, X509Credential credential, boolean skipConsent,
			String subjectIdentityType)
	{
		super();
		this.properties = properties;
		this.maxExtendedAccessTokenValidity = maxExtendedAccessTokenValidity;
		this.subjectIdentityType = subjectIdentityType;
		this.timestamp = new Date();
		this.request = request;
		this.codeTokenValidity = codeTokenValidity;
		this.accessTokenValidity = accessTokenValidity;
		this.idTokenValidity = idTokenValidity;
		this.issuerName = issuerName;
		this.credential = credential;
		this.skipConsent = skipConsent;
	}

	public OAuthASProperties getProperties()
	{
		return properties;
	}

	public X509Credential getCredential()
	{
		return credential;
	}

	public AuthorizationRequest getRequest()
	{
		return request;
	}
	
	public boolean isExpired()
	{
		return System.currentTimeMillis() > AUTHN_TIMEOUT+timestamp.getTime();
	}

	public URI getReturnURI()
	{
		return returnURI;
	}

	public void setReturnURI(URI returnURI)
	{
		this.returnURI = returnURI;
	}

	public String getClientName()
	{
		return clientName;
	}

	public void setClientName(String clientName)
	{
		this.clientName = clientName;
	}

	public String getClientUsername()
	{
		return clientUsername;
	}

	public void setClientUsername(String clientUsername)
	{
		this.clientUsername = clientUsername;
	}

	public Attribute<BufferedImage> getClientLogo()
	{
		return clientLogo;
	}

	public void setClientLogo(Attribute<BufferedImage> clientLogo)
	{
		this.clientLogo = clientLogo;
	}

	public String getUsersGroup()
	{
		return usersGroup;
	}

	public void setUsersGroup(String usersGroup)
	{
		this.usersGroup = usersGroup;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}
	
	public void addScopeInfo(ScopeInfo scopeInfo)
	{
		effectiveRequestedScopes.add(scopeInfo);
		requestedAttrs.addAll(scopeInfo.getAttributes());
	}
	
	public Set<String> getRequestedAttrs()
	{
		return requestedAttrs;
	}

	public Set<ScopeInfo> getEffectiveRequestedScopes()
	{
		return effectiveRequestedScopes;
	}

	public String[] getEffectiveRequestedScopesList()
	{
		String[] ret = new String[effectiveRequestedScopes.size()];
		Iterator<ScopeInfo> sIt = effectiveRequestedScopes.iterator();
		for (int i=0; i<ret.length; i++)
			ret[i] = sIt.next().name;
		return ret;
	}

	public GrantFlow getFlow()
	{
		return flow;
	}

	public void setFlow(GrantFlow flow)
	{
		this.flow = flow;
	}

	public boolean isOpenIdMode()
	{
		return openIdMode;
	}

	public boolean isSkipConsent()
	{
		return skipConsent;
	}

	public void setOpenIdMode(boolean openIdMode)
	{
		this.openIdMode = openIdMode;
	}

	public int getCodeTokenValidity()
	{
		return codeTokenValidity;
	}

	public int getAccessTokenValidity()
	{
		return accessTokenValidity;
	}

	public int getMaxExtendedAccessTokenValidity()
	{
		return maxExtendedAccessTokenValidity;
	}

	public int getIdTokenValidity()
	{
		return idTokenValidity;
	}

	public String getIssuerName()
	{
		return issuerName;
	}

	public long getClientEntityId()
	{
		return clientEntityId;
	}

	public void setClientEntityId(long clientEntityId)
	{
		this.clientEntityId = clientEntityId;
	}

	public static long getAuthnTimeout()
	{
		return AUTHN_TIMEOUT;
	}

	public String getSubjectIdentityType()
	{
		return subjectIdentityType;
	}



	public static class ScopeInfo
	{
		private String name;
		private String description;
		private Set<String> attributes;
		
		public ScopeInfo(String name, String description, Collection<String> attributes)
		{
			super();
			this.name = name;
			this.description = description;
			this.attributes = new HashSet<String>(attributes);
		}

		public String getName()
		{
			return name;
		}

		public String getDescription()
		{
			return description;
		}

		public Set<String> getAttributes()
		{
			return attributes;
		}
	}
}
