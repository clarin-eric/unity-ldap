/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import java.util.Map;

import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ws.SAMLAssertionQueryImpl;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.samly2.webservice.SAMLQueryInterface;

/**
 * Endpoint exposing SAML SOAP binding. This version extends the {@link SamlSoapEndpoint}
 * by exposing a modified implementation of the {@link SAMLAuthnInterface}. The
 * {@link SAMLETDAuthnImpl} is used, which also returns a bootstrap ETD assertion.
 * 
 * @author K. Benedyczak
 */
public class SamlUnicoreSoapEndpoint extends SamlSoapEndpoint
{
	public SamlUnicoreSoapEndpoint(UnityMessageSource msg, NetworkServer server,
			String servletPath, String metadataServletPath, IdPEngine idpEngine,
			PreferencesManagement preferencesMan,
			PKIManagement pkiManagement, ExecutorsService executorsService, SessionManagement sessionMan,
			Map<String, RemoteMetaManager> remoteMetadataManagers, MetaDownloadManager downloadManager, 
			UnityServerConfiguration mainConfig,
			SAMLLogoutProcessorFactory logoutProcessorFactory, AuthenticationProcessor authnProcessor,
			AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry)
	{
		super(msg, server, servletPath, metadataServletPath, idpEngine, preferencesMan,
				pkiManagement, executorsService, sessionMan, remoteMetadataManagers, downloadManager, 
				mainConfig, logoutProcessorFactory, authnProcessor, attributeSyntaxFactoriesRegistry);
	}


	@Override
	protected void configureServices()
	{
		String endpointURL = getServletUrl(servletPath);
		SamlIdpProperties virtualConf = (SamlIdpProperties) myMetadataManager.getVirtualConfiguration();
		SAMLAssertionQueryImpl assertionQueryImpl = new SAMLAssertionQueryImpl(virtualConf, 
				endpointURL, idpEngine, preferencesMan, attributeSyntaxFactoriesRegistry);
		addWebservice(SAMLQueryInterface.class, assertionQueryImpl);
		SAMLETDAuthnImpl authnImpl = new SAMLETDAuthnImpl(virtualConf, endpointURL, 
				idpEngine, preferencesMan, attributeSyntaxFactoriesRegistry);
		addWebservice(SAMLAuthnInterface.class, authnImpl);
	}
}




