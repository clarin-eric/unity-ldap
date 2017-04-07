/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.FilePropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import eu.unicore.util.jetty.HttpServerProperties;

/**
 * Principal options are defined here: ids and corresponding default values.
 * @author K. Benedyczak
 */
@Component
public class UnityServerConfiguration extends UnityFilePropertiesHelper
{
	public static final String PROFILE_PRODUCTION = "production";
	
	public enum LogoutMode {internalOnly, internalAndSyncPeers, internalAndAsyncPeers}
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, UnityServerConfiguration.class);
	public static final String CONFIGURATION_FILE = "conf/unityServer.conf";
	public static final String DEFAULT_EMAIL_CHANNEL = "Default e-mail channel";

	public static final String SYSTEM_ALLOW_FULL_HTML = "unity.server.allowFullHtml"; 
	
	public static final String BASE_PREFIX = "unityServer.";

	@DocumentationReferencePrefix
	public static final String P = BASE_PREFIX + "core.";
	
	public static final String ENABLED_LOCALES = "enabledLocales.";
	public static final String DEFAULT_LOCALE = "defaultLocale";
	public static final String MAIL_CONF = "mailConfig";
	public static final String TEMPLATES_CONF = "templatesFile";
	public static final String PKI_CONF = "pkiConfigFile";
	public static final String THREAD_POOL_SIZE = "threadPoolSize";
	public static final String IGNORE_CONFIGURED_CONTENTS_SETTING = "ignoreContentsReloadingFromConfiguration";
	public static final String CONFIG_ONLY_ERA_CONTROL = "fullyRecreateEndpointsAROnStartup";
	private static final String RECREATE_ENDPOINTS_ON_STARTUP = "recreateEndpointsOnStartup";
	public static final String LOGOUT_MODE = "logoutMode";
	public static final String DEFAULT_WEB_CONTENT_PATH = "defaultWebContentDirectory";
	public static final String MESSAGES_DIRECTORY = "i18nMessagesDirectory";
	public static final String THEME = "defaultTheme";
	public static final String CONFIRMATION_THEME = "confirmationUITheme";
	public static final String CONFIRMATION_TEMPLATE = "confirmationUITemplate";
	public static final String WELL_KNOWN_URL_THEME = "wellKnownUrlUITheme";
	public static final String WELL_KNOWN_URL_TEMPLATE = "wellKnownUrlUITemplate";
	public static final String UNITYGW_WEB_CONTENT_PATH = "unityGWWebContentDirectory";
	public static final String ALLOW_FULL_HTML = "allowFullHtml"; 
	public static final String DEFAULT_WEB_PATH = "defaultWebPath";
	public static final String REDIRECT_MODE = "redirectMode";
	
	public static final String IMPORT_PFX = "userImport."; 
	
	public static final String ENDPOINTS = "endpoints.";
	public static final String ENDPOINT_DESCRIPTION = "endpointDescription";
	public static final String ENDPOINT_TYPE = "endpointType";
	public static final String ENDPOINT_CONFIGURATION = "endpointConfigurationFile";
	public static final String ENDPOINT_ADDRESS = "contextPath";
	public static final String ENDPOINT_NAME = "endpointName";	
	public static final String ENDPOINT_DISPLAYED_NAME = "endpointDisplayedName";	
	public static final String ENDPOINT_AUTHENTICATORS = "endpointAuthenticators";
	public static final String ENDPOINT_REALM = "endpointRealm";
	
	public static final String INITIALIZERS = "initializers.";
	public static final String UPDATE_INTERVAL = "asyncStateUpdateInterval";
	public static final String WORKSPACE_DIRECTORY = "workspaceDirectory";
	public static final String MAIN_CREDENTIAL = "credential";
	public static final String MAIN_TRUSTSTORE = "truststore";
	
	public static final String REALMS = "realms.";
	public static final String REALM_NAME = "realmName";
	public static final String REALM_DESCRIPTION = "realmDescription";
	public static final String REALM_BLOCK_AFTER_UNSUCCESSFUL = "blockAfterUnsuccessfulLogins";
	public static final String REALM_BLOCK_FOR = "blockFor";
	public static final String REALM_MAX_INACTIVITY = "maxInactivity";
	public static final String REALM_REMEMBER_ME = "enableRememberMeFor";
	
	public static final String AUTHENTICATORS = "authenticators.";
	public static final String AUTHENTICATOR_NAME = "authenticatorName";
	public static final String AUTHENTICATOR_TYPE = "authenticatorType";
	public static final String AUTHENTICATOR_CREDENTIAL = "localCredential";
	public static final String AUTHENTICATOR_VERIFICATOR_CONFIG = "verificatorConfigurationFile";
	public static final String AUTHENTICATOR_RETRIEVAL_CONFIG = "retrievalConfigurationFile";
	
	public static final String CREDENTIALS = "credentials.";
	public static final String CREDENTIAL_NAME = "credentialName";
	public static final String CREDENTIAL_TYPE = "credentialType";
	public static final String CREDENTIAL_DESCRIPTION = "credentialDescription";
	public static final String CREDENTIAL_CONFIGURATION = "credentialConfigurationFile";

	public static final String CREDENTIAL_REQS = "credentialRequirements.";
	public static final String CREDENTIAL_REQ_NAME = "credentialReqName";
	public static final String CREDENTIAL_REQ_DESCRIPTION = "credentialReqDescription";
	public static final String CREDENTIAL_REQ_CONTENTS = "credentialReqContents.";
	
	public static final String INITIAL_ADMIN_USER = "initialAdminUsername";
	public static final String INITIAL_ADMIN_PASSWORD = "initialAdminPassword";
	public static final String INITIAL_ADMIN_USER_OUTDATED = "initialAdminOutdated";
	
	public static final String TRANSLATION_PROFILES = "translationProfiles.";
	
	public static final String WIPE_DB_AT_STARTUP = "wipeDbAtStartup";
	
	public static final String CONFIRMATION_REQUEST_LIMIT = "confirmationRequestLimit";
	public static final String CONFIRMATION_DEFAULT_RETURN_URL = "defaultPostConfirmationReturnURL";
	public static final String CONFIRMATION_AUTO_REDIRECT = "automaticRedirectAfterConfirmation";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	public static final Map<String, Locale> SUPPORTED_LOCALES = new HashMap<String, Locale>();
	
	static
	{
		DocumentationCategory mainCat = new DocumentationCategory("General settings", "1");
		DocumentationCategory initCredCat = new DocumentationCategory("Content initializers: credentials", "2");
		DocumentationCategory initCredReqCat = new DocumentationCategory("Content initializers: credential requirements", "3");
		DocumentationCategory initAuthnCat = new DocumentationCategory("Content initializers: authenticators", "4");
		DocumentationCategory initRealmCat = new DocumentationCategory("Content initializers: authentication realms", "5");
		DocumentationCategory initEndpointsCat = new DocumentationCategory("Content initializers: endpoints", "6");
		DocumentationCategory otherCat = new DocumentationCategory("Other", "8");
		
		defaults.put(ENABLED_LOCALES, new PropertyMD().setList(true).setCategory(mainCat).
				setDescription("List of enabled locales. " +
				"Each entry must have a language code as 'en' or 'pl' first, " +
				"and then, after a space an optional, short name which will be presented in the UI. By default the 'en' locale is installed."));
		defaults.put(DEFAULT_LOCALE, new PropertyMD("en").setCategory(mainCat).
				setDescription("The default locale to be used. Must be one of the enabled locales."));
		defaults.put(MAIL_CONF, new PropertyMD().setPath().setCategory(mainCat).
				setDescription("A configuration file for the mail notification subsystem."));
		defaults.put(TEMPLATES_CONF, new PropertyMD("conf/msgTemplates.properties").setPath().setCategory(mainCat).
				setDescription("A file with the initial message templates. You can have this file empty and manage the templates via the Admin UI."));
		defaults.put(PKI_CONF, new PropertyMD("conf/pki.properties").setPath().setCategory(mainCat).
				setDescription("A file with the configuration of the PKI: credentials and truststores."));
		defaults.put(RECREATE_ENDPOINTS_ON_STARTUP, new PropertyMD("true").setCategory(mainCat).setDeprecated().
				setDescription("This setting is ignored. By default all endpoints, realms and authenticators are reloaded at startup."
						+ "As a more admin-friendly counterpart of this setting use ."));
		defaults.put(IGNORE_CONFIGURED_CONTENTS_SETTING, new PropertyMD("false").setCategory(mainCat).
				setDescription("If set to true then all configuration settings related to loading of "
						+ "database contents (endpoints, authenticators, credentials, ...) "
						+ "are ignored. This is useful in the case of redundant Unity instance,"
						+ " which should use the database contents configured at the master serevr."));
		defaults.put(CONFIG_ONLY_ERA_CONTROL, new PropertyMD("true").setCategory(mainCat).
				setDescription("If set to true then all Endpoints, Authenticators and authentication Realms "
						+ "are fully recreated from configuration at startup. This is convenient unless you "
						+ "use other management means for those artefacts (as REST interface). "
						+ "Then set it to false, to have only incremental changes from configuration "
						+ "- elements not present in configuration will not be removed then. "
						+ "Note that this option is ignored if " + IGNORE_CONFIGURED_CONTENTS_SETTING + 
						" is true."));
		defaults.put(LOGOUT_MODE, new PropertyMD(LogoutMode.internalAndSyncPeers).setCategory(mainCat).
				setDescription("Controls the way how the logout operation is performed. "
				+ "+internalOnly+ will perform only a local logout. +internalAndSyncPeers+ will also logout"
				+ " all remote session participants but only using a synchronous binding. Finally "
				+ "+internalAndAsyncPeers+ will logout remote session participants also using asynchronous"
				+ " protocols (with web browser redirects) if needed. This last option is risky as it may"
				+ " happen that a faulty peer won't redirect the web agent back."));
		defaults.put(THREAD_POOL_SIZE, new PropertyMD("4").setCategory(mainCat).setDescription(
				"Number of threads used by internal processes of the server. HTTP server threads use a separate pool."));
		defaults.put(INITIALIZERS, new PropertyMD().setList(true).setCategory(mainCat).setDescription(
				"List of identifiers of initialization modules that should be run on the first startup."));
		defaults.put(UPDATE_INTERVAL, new PropertyMD("60").setPositive().setCategory(mainCat).setDescription(
				"Defines the interval of background update tasks in seconds. Those tasks are used to update runtime state of the server (for instance the deployed endpoints) with the data which is stored in database."));
		defaults.put(WORKSPACE_DIRECTORY, new PropertyMD("data/workspace").setPath().setCategory(mainCat).setDescription(
				"Defines a folder where the server will write its internal files."));
		defaults.put(MESSAGES_DIRECTORY, new PropertyMD("i18n").setPath().setCategory(mainCat).setDescription(
				"Defines a folder where internacionalized messages are stored. Note that"
				+ " this directory is optional: a fallback messages are always included in the application."));
		defaults.put(DEFAULT_WEB_CONTENT_PATH, new PropertyMD("webContent").setPath().setCategory(mainCat).setDescription(
				"Defines a default folder from which the web endpoints will serve static content, configured locally. "
				+ "Also used for the shared endpoint under /unitygw path."));
		defaults.put(THEME, new PropertyMD().setCategory(mainCat).setDescription(
				"Overrides the default theme name as used for rendering the web endpoints. "
				+ "This setting can be overriden per-endpoint. Applicable only for the web endpoints."));
		defaults.put(ALLOW_FULL_HTML, new PropertyMD("false").setCategory(mainCat).setDescription(
				"If set to true then Unity will render full HTML in admin-configured descriptions"
				+ " of elements intended for end-user presentation "
				+ "(e.g. registration form agreements or credential description). If false then only "
				+ "a very limited set of HTML formatting tags will be rendered, the rest will be escaped. "
				+ "This setting must be set to false in case when Unity is used by not-fully "
				+ "trusted administrators, who (even with partially limited rights) may perform"
				+ "XSS attacks. Then, however functionality of registration forms etc is slightly limited"
				+ " as it is impossible to insert links and other advanced formating."));
		defaults.put(DEFAULT_WEB_PATH, new PropertyMD().setCategory(mainCat).setDescription(
				"If set Unity will redirect request without the path to this one"));
		defaults.put(UNITYGW_WEB_CONTENT_PATH, new PropertyMD().setPath().setCategory(mainCat).setDescription(
				"Defines a folder from which all the web applications operating on the shared unitygw path "
				+ "(e.g. the email confirmation screen) "
				+ "will serve static content as images. Overrides the default webContent path."));
		defaults.put(CONFIRMATION_THEME, new PropertyMD().setCategory(mainCat).setDescription(
				"Sets the theme used for rendering the confirmation UI (shown after confirming email etc)."
				+ "This setting overrides the default server theme."));
		defaults.put(CONFIRMATION_TEMPLATE, new PropertyMD("default.ftl").setCategory(mainCat).setDescription(
				"The name of a Freemarker template, relative to templates directory, with a "
				+ "template of the confirmation UI. Custom template can be used to add "
				+ "static header/footer etc."));
		defaults.put(WELL_KNOWN_URL_THEME, new PropertyMD().setCategory(mainCat).setDescription(
				"Sets the theme used for rendering the well known links UI "
				+ "(used for standalone registration forms and others)."
				+ "This setting overrides the default server theme."));
		defaults.put(WELL_KNOWN_URL_TEMPLATE, new PropertyMD("default.ftl").setCategory(mainCat).setDescription(
				"The name of a Freemarker template, relative to templates directory, with a "
				+ "template of the well-known links web interface. Custom template can be used to add "
				+ "static header/footer etc."));
		defaults.put(INITIAL_ADMIN_USER, new PropertyMD().setCategory(mainCat).
				setDescription("Username of the administrator to be installed to the database upon startup. Remove the property if no admin should be added."));
		defaults.put(INITIAL_ADMIN_PASSWORD, new PropertyMD("admin").setCategory(mainCat).
				setDescription("Password of the administrator to be installed to the empty database."));
		defaults.put(INITIAL_ADMIN_USER_OUTDATED, new PropertyMD("true").setCategory(mainCat).
				setDescription("If false then the default admin user is not " +
						"set to outdated state after creation. Useful only for testbeds."));
		defaults.put(IMPORT_PFX, new PropertyMD().setList(false).setCategory(mainCat).
				setDescription("List of file paths, where each file contains a definition of a "
						+ "user import subsystem. Use of user import feature is naturally optional"
						+ " and so this list typically is empty."));

		
		defaults.put(TRANSLATION_PROFILES, new PropertyMD().setList(false).setCategory(mainCat).
				setDescription("List of file paths, where each file contains a definition of a translation profile, " +
						"used to configure mapping of remote identities to the local representation."));

		
		defaults.put(ENDPOINTS, new PropertyMD().setStructuredList(true).setCategory(initEndpointsCat).
				setDescription("List of initially enabled endpoints"));
		defaults.put(ENDPOINT_TYPE, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(initEndpointsCat).
				setDescription("Endpoint type"));
		defaults.put(ENDPOINT_CONFIGURATION, new PropertyMD().setStructuredListEntry(ENDPOINTS).setPath().setMandatory().setCategory(initEndpointsCat).
				setDescription("Path of the file with JSON configuration of the endpoint"));
		defaults.put(ENDPOINT_DESCRIPTION, new PropertyMD("").setStructuredListEntry(ENDPOINTS).setCategory(initEndpointsCat).
				setDescription("Description of the endpoint"));
		defaults.put(ENDPOINT_ADDRESS, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(initEndpointsCat).
				setDescription("Context path of the endpoint"));
		defaults.put(ENDPOINT_NAME, new PropertyMD().setStructuredListEntry(ENDPOINTS).
				setMandatory().setCategory(initEndpointsCat).setDescription("Endpoint identifier. "
						+ "It is used to refer to this endpoint in other parts of the system."));
		defaults.put(ENDPOINT_DISPLAYED_NAME, new PropertyMD().setStructuredListEntry(ENDPOINTS).setCanHaveSubkeys().
				setCategory(initEndpointsCat).setDescription("Endpoint displayed name. "
						+ "It is used whenever endpoint's name is presented to the end-user, "
						+ "e.g. in top bars of web UIs. Localized values can be given "
						+ "with subkeys equal to locale name. If undefined then Unity "
						+ "will use " + ENDPOINT_NAME));
		defaults.put(ENDPOINT_AUTHENTICATORS, new PropertyMD().setStructuredListEntry(ENDPOINTS).setMandatory().setCategory(initEndpointsCat).
				setDescription("Endpoint authenticator names: each set is separated with ';' and particular authenticators in each set with ','."));
		defaults.put(ENDPOINT_REALM, new PropertyMD().setMandatory().setStructuredListEntry(ENDPOINTS).setCategory(initEndpointsCat).
				setDescription("Authentication realm name, to which this endpoint belongs."));

		defaults.put(AUTHENTICATORS, new PropertyMD().setStructuredList(true).setCategory(initAuthnCat).
				setDescription("List of initially enabled authenticators"));
		defaults.put(AUTHENTICATOR_NAME, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setMandatory().setCategory(initAuthnCat).
				setDescription("Authenticator name"));
		defaults.put(AUTHENTICATOR_TYPE, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setMandatory().setCategory(initAuthnCat).
				setDescription("Authenticator type"));
		defaults.put(AUTHENTICATOR_CREDENTIAL, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setCategory(initAuthnCat).
				setDescription("For local authenticator the name of the local credential associated with it."));
		defaults.put(AUTHENTICATOR_VERIFICATOR_CONFIG, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setCategory(initAuthnCat).
				setDescription("Authenticator configuration file of the verificator"));
		defaults.put(AUTHENTICATOR_RETRIEVAL_CONFIG, new PropertyMD().setStructuredListEntry(AUTHENTICATORS).setCategory(initAuthnCat).
				setDescription("Authenticator configuration file of the retrieval"));

		defaults.put(REALMS, new PropertyMD().setStructuredList(false).setCategory(initRealmCat).
				setDescription("List of authentication realm definitions."));
		defaults.put(REALM_NAME, new PropertyMD().setMandatory().setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("Defines the realm's name. Must contain only alphanumeric letters, "
						+ "and can not exceed 20 characters."));
		defaults.put(REALM_DESCRIPTION, new PropertyMD().setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("Realm's description."));
		defaults.put(REALM_BLOCK_AFTER_UNSUCCESSFUL, new PropertyMD("5").setPositive().setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("Defines maximum number of unsuccessful logins before the access is temporarely blocked for a client."));
		defaults.put(REALM_BLOCK_FOR, new PropertyMD("60").setPositive().setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("Defines for how long (in seconds) the access should be blocked for the" +
						"client reaching the limit of unsuccessful logins."));
		defaults.put(REALM_MAX_INACTIVITY, new PropertyMD("1800").setPositive().setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("Defines after what time of inactivity the login session is terminated (in seconds). "
						+ "Note: the HTTP sessions (if applicable for endpoint) will be couple of seconds "
						+ "shorter to allow for login session expiration warning."));
		defaults.put(REALM_REMEMBER_ME, new PropertyMD("-1").setStructuredListEntry(REALMS).setCategory(initRealmCat).
				setDescription("(web endpoints only) If set to positive number, the realm authentication will allow for "
						+ "remeberinging the user's login even after session is lost due "
						+ "to expiration or browser closing. The period of time to remember the login "
						+ "will be equal to the number of days as given to this option. "
						+ "IMPORTANT! This is an insecure option. Use it only for realms "
						+ "containing only endpoints with low security requirements."));

		
		defaults.put(CREDENTIALS, new PropertyMD().setStructuredList(true).setCategory(initCredCat).
				setDescription("List of initially defined credentials"));
		defaults.put(CREDENTIAL_NAME, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(initCredCat).
				setDescription("Credential name"));
		defaults.put(CREDENTIAL_TYPE, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(initCredCat).
				setDescription("Credential type"));
		defaults.put(CREDENTIAL_DESCRIPTION, new PropertyMD("").setStructuredListEntry(CREDENTIALS).setCategory(initCredCat).
				setDescription("Credential description"));
		defaults.put(CREDENTIAL_CONFIGURATION, new PropertyMD().setStructuredListEntry(CREDENTIALS).setMandatory().setCategory(initCredCat).
				setDescription("Credential configuration file"));

		defaults.put(CREDENTIAL_REQS, new PropertyMD().setStructuredList(true).setCategory(initCredReqCat).
				setDescription("List of initially defined credential requirements"));
		defaults.put(CREDENTIAL_REQ_NAME, new PropertyMD().setStructuredListEntry(CREDENTIAL_REQS).setMandatory().setCategory(initCredReqCat).
				setDescription("Credential requirement name"));
		defaults.put(CREDENTIAL_REQ_DESCRIPTION, new PropertyMD("").setStructuredListEntry(CREDENTIAL_REQS).setCategory(initCredReqCat).
				setDescription("Credential requirement description"));
		defaults.put(CREDENTIAL_REQ_CONTENTS, new PropertyMD().setStructuredListEntry(CREDENTIAL_REQS).setList(false).setCategory(initCredReqCat).
				setDescription("Credential requirement contents, i.e. credentials that belongs to it"));
		
		defaults.put(WIPE_DB_AT_STARTUP, new PropertyMD("false").setHidden().
				setDescription("For testing: if set to true then DB will be fully cleared at server startup"));
		
		defaults.put(CONFIRMATION_REQUEST_LIMIT, new PropertyMD("3").setCategory(mainCat).
				setDescription("Defines number of confirmation request that can be send to particular address in day"));
		defaults.put(CONFIRMATION_DEFAULT_RETURN_URL, new PropertyMD().setCategory(mainCat).
				setDescription("If set the value should be a valid URL. The URL is used as a return (redirect) URL "
						+ "to be used after confirmation of a verifiable element as email. "
						+ "Can be overriden for instance in registration form definition, for all confirmations related to the form."));
		defaults.put(CONFIRMATION_AUTO_REDIRECT, new PropertyMD("false").setCategory(mainCat).
				setDescription("If false Unity will show its confirmation screen after email verification. "
						+ "If true and a return URL is defined for the confirmation then the screen is not shown and redirect is immediate."));
		
		defaults.put(MAIN_TRUSTSTORE, new PropertyMD().setMandatory().setCategory(mainCat).
				setDescription("Name of the truststore to be used by the server."));
		defaults.put(MAIN_CREDENTIAL, new PropertyMD().setMandatory().setCategory(mainCat).
				setDescription("Name of the credential to be used by the server."));
		defaults.put(HttpServerProperties.DEFAULT_PREFIX, new PropertyMD().setCanHaveSubkeys().setCategory(otherCat).
				setDescription("Properties starting with this prefix are used to configure Jetty HTTP server settings. See separate table for details."));
		
		
		SUPPORTED_LOCALES.put("en", new Locale("en"));
		SUPPORTED_LOCALES.put("pl", new Locale("pl"));
		SUPPORTED_LOCALES.put("de", new Locale("de"));
	}

	private UnityHttpServerConfiguration jp;
	private UnityPKIConfiguration pkiConf;
	private Map<String, Locale> enabledLocales;
	private Locale defaultLocale;
	
	@Autowired
	public UnityServerConfiguration(Environment env, ConfigurationLocationProvider locProvider) throws ConfigurationException, IOException
	{
		super(P, getConfigurationFile(env, locProvider), defaults, log);
		pkiConf = new UnityPKIConfiguration(FilePropertiesHelper.load(getFileValue(PKI_CONF, false)));
		jp = new UnityHttpServerConfiguration(properties);
			
		enabledLocales = loadEnabledLocales();
		defaultLocale = safeLocaleDecode(getValue(DEFAULT_LOCALE));
		if (!isLocaleSupported(defaultLocale))
			throw new ConfigurationException("The default locale is not among enabled ones.");

		checkRealmNames();
		
		File workspace = new File(getValue(WORKSPACE_DIRECTORY));
		if (!workspace.exists())
			workspace.mkdirs();
		
		if (getBooleanValue(IGNORE_CONFIGURED_CONTENTS_SETTING) && 
				getBooleanValue(WIPE_DB_AT_STARTUP))
			throw new ConfigurationException("Using " + WIPE_DB_AT_STARTUP + " and " + 
				IGNORE_CONFIGURED_CONTENTS_SETTING + " settings together makes really no sense: "
						+ "database will be cleaned and not populated with any contents "
						+ "so it won't be possible to anyhow log in.");
		if (getBooleanValue(ALLOW_FULL_HTML))
			System.setProperty(SYSTEM_ALLOW_FULL_HTML, "true");
	}
	
	private void checkRealmNames()
	{
		Set<String> realmKeys = getStructuredListKeys(UnityServerConfiguration.REALMS);
		for (String realmKey: realmKeys)
		{
			String name = getValue(realmKey+REALM_NAME);
			if (name.length() > 20)
				throw new ConfigurationException("Realm name is longer then 20 characters: " + name);
			CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();
			if (!encoder.canEncode(name))
				throw new ConfigurationException("Realm name is not ASCII: " + name);
			for (char c: name.toCharArray())
				if (!Character.isLetterOrDigit(c))
					throw new ConfigurationException("Realm name must have only "
							+ "digits and letters: " + name);
		}
	}
	
	private static String getConfigurationFile(Environment env, ConfigurationLocationProvider locProvider)
	{
		String configFile;
		String[] nonOptionArgs = env.getProperty(CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME, 
				String[].class);
		if (nonOptionArgs != null && nonOptionArgs.length > 0)
			configFile = nonOptionArgs[0];
		else 
			configFile = locProvider.getConfigurationLocation();
			
		log.debug("Using configuration file: " + configFile);
		return configFile;
	}
		
	/**
	 * @return map with enabled locales. Key is the user-friendly label. 
	 */
	private Map<String, Locale> loadEnabledLocales()
	{
		List<String> locales = getListOfValues(ENABLED_LOCALES);
		if (locales.isEmpty())
		{
			locales = new ArrayList<String>();
			locales.add("en English");
		}
		Map<String, Locale> ret = new LinkedHashMap<String, Locale>();
		for (String locale: locales)
		{
			locale = locale.trim() + " ";
			int split = locale.indexOf(' ');
			String code = locale.substring(0, split);
			String name = locale.substring(split).trim();
			if (name.equals(""))
				name = code;
			Locale l = safeLocaleDecode(code);
			ret.put(name, l);
		}
		return ret;
	}
	
	public boolean isLocaleSupported(Locale toSearch)
	{
		for (Locale l: enabledLocales.values())
			if (l.equals(toSearch))
				return true;
		return false;
	}
	
	public static Locale safeLocaleDecode(String inputRaw)
	{
		if (inputRaw == null)
			return Locale.ENGLISH;
		Locale l;
		String input = inputRaw.trim();
		if (input.contains("_"))
		{
			String[] sp = input.split("_");
			l = new Locale(sp[0], sp[1]);
		} else
		{
			l = new Locale(input);
		}
		return l;
	}

	public UnityHttpServerConfiguration getJettyProperties()
	{
		return jp;
	}
	
	public Locale getDefaultLocale()
	{
		return defaultLocale;
	}

	public Map<String, Locale> getEnabledLocales()
	{
		return enabledLocales;
	}

	public UnityPKIConfiguration getPKIConfiguration()
	{
		return pkiConf;
	}
	
	public List<AuthenticationOptionDescription> getEndpointAuth(String endpointKey)
	{
		String spec = getValue(endpointKey+UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
		String[] authenticatorSets = spec.split(";");		
		List<AuthenticationOptionDescription> endpointAuthn = new ArrayList<AuthenticationOptionDescription>();
		for (String authenticatorSet : authenticatorSets)
		{
			String[] authenticators = authenticatorSet.split(",");
			if (authenticators.length > 2)
				throw new ConfigurationException("Invalid configuration of "
						+ "authenticators of the endpoint with id " + endpointKey +
						". In one authentication set maximum of 2 authenticators is allowed.");
			String secondary = authenticators.length == 2 ? authenticators[1] : null;
			endpointAuthn.add(new AuthenticationOptionDescription(authenticators[0], secondary));
		}
		return endpointAuthn;
	}
	
	/**
	 * Returns either a theme configured with the key given as argument or the default theme if the
	 * specific one is not defined. If neither is available returns default theme.
	 * @param themeConfigKey
	 * @return configuration theme
	 */
	public String getConfiguredTheme(String themeConfigKey, String defaultTheme)
	{
		if (isSet(themeConfigKey))
			return getValue(themeConfigKey);
		else if (isSet(THEME))
			return getValue(THEME);
		return defaultTheme;
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
