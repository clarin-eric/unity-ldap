/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Configuration of a registration form. Registration form data contains:
 * <ol>
 *  <li> its identification and description,
 *  <li> visibility, which controls whether the form is publicly available for all (anonymous) clients or
 *  whether only for authorized administrators. 
 *  <li> configuration of what information is collected during registration (and in how),
 *  <li> extra information to be presented to the user
 *  <li> translation profile which can modify the data collected by the form
 * </ol>
 * <p>
 * Instances of this class can be built either from JSON or using a {@link RegistrationFormBuilder}.
 * 
 * @author K. Benedyczak
 */
public class RegistrationForm extends BaseForm
{
	public static final int MAX_CAPTCHA_LENGTH = 8;
	public static final String CAPTCHA = "CAPTCHA";
	public static final String REG_CODE = "REG_CODE";
	
	private boolean publiclyAvailable;
	private RegistrationFormNotifications notificationsConfiguration = new RegistrationFormNotifications();
	private int captchaLength;
	private String registrationCode;
	private boolean byInvitationOnly;
	private String defaultCredentialRequirement;

	@JsonCreator
	public RegistrationForm(ObjectNode json)
	{
		super(json);
		fromJson(json);
		validateRegistration();
		if (getLayout() != null)
			validateLayout();
	}
	
	RegistrationForm()
	{
	}
	
	@Override
	public RegistrationFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}

	void setNotificationsConfiguration(RegistrationFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
	}

	public boolean isByInvitationOnly()
	{
		return byInvitationOnly;
	}

	public void setByInvitationOnly(boolean byInvitationOnly)
	{
		this.byInvitationOnly = byInvitationOnly;
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}


	public int getCaptchaLength()
	{
		return captchaLength;
	}

	void setCaptchaLength(int captchaLength)
	{
		this.captchaLength = captchaLength;
	}

	public String getDefaultCredentialRequirement()
	{
		return defaultCredentialRequirement;
	}

	void setDefaultCredentialRequirement(String defaultCredentialRequirement)
	{
		this.defaultCredentialRequirement = defaultCredentialRequirement;
	}

	@Override
	public String toString()
	{
		return "RegistrationForm [name=" + name + "]";
	}

	protected void validateRegistration()
	{
		if (defaultCredentialRequirement == null)
			throw new IllegalStateException("Default credential requirement must be not-null "
					+ "in RegistrationForm");
	}
	
	@Override
	protected void updateOtherElementsInLayout(Set<String> definedElements)
	{
		super.updateOtherElementsInLayout(definedElements);
		if (captchaLength > 0)
			getLayout().addBasicElementIfMissing(CAPTCHA, definedElements);
		else
			getLayout().removeBasicElementIfPresent(CAPTCHA);
		
		if (registrationCode != null)
			getLayout().addBasicElementIfMissing(REG_CODE, definedElements);
		else
			getLayout().removeBasicElementIfPresent(REG_CODE);
	}
	
	@Override
	protected void checkOtherElementsInLayout(Set<String> definedElements)
	{
		super.checkOtherElementsInLayout(definedElements);
		if (captchaLength > 0)
			getLayout().checkLayoutElement(CAPTCHA, definedElements);
		if (registrationCode != null)
			getLayout().checkLayoutElement(REG_CODE, definedElements);

	}

	@Override
	public FormLayout getDefaultFormLayout(MessageSource msg)
	{
		List<FormElement> elements = new ArrayList<FormElement>();
		if (registrationCode != null)
			elements.add(new BasicFormElement(REG_CODE));
		elements.addAll(getDefaultParametersLayout(FormLayout.IDENTITY, getIdentityParams(), msg, 
				"RegistrationRequest.identities", "RegistrationRequest.externalIdentities"));
		elements.addAll(getDefaultBasicParamsLayout(FormLayout.CREDENTIAL, getCredentialParams(), msg, 
				"RegistrationRequest.credentials", true));
		elements.addAll(getDefaultParametersLayout(FormLayout.ATTRIBUTE, getAttributeParams(), msg, 
				"RegistrationRequest.attributes", "RegistrationRequest.externalAttributes"));
		elements.addAll(getDefaultParametersLayout(FormLayout.GROUP, getGroupParams(), msg, 
				"RegistrationRequest.groups", "RegistrationRequest.externalGroups"));
		if (isCollectComments())
			elements.add(new BasicFormElement(FormLayout.COMMENTS));
		elements.addAll(getDefaultBasicParamsLayout(FormLayout.AGREEMENT, getAgreements(), msg, 
				"RegistrationRequest.agreements", true));
		if (captchaLength > 0)
			elements.add(new BasicFormElement(CAPTCHA));
		return new FormLayout(elements);
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = super.toJson();
		root.put("DefaultCredentialRequirement", getDefaultCredentialRequirement());
		root.set("NotificationsConfiguration", jsonMapper.valueToTree(getNotificationsConfiguration()));
		root.put("PubliclyAvailable", isPubliclyAvailable());
		root.put("RegistrationCode", getRegistrationCode());
		root.put("CaptchaLength", getCaptchaLength());
		root.put("ByInvitationOnly", isByInvitationOnly());
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("DefaultCredentialRequirement");
			setDefaultCredentialRequirement(n == null ? null : n.asText());
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				RegistrationFormNotifications r = jsonMapper.readValue(v, 
						new TypeReference<RegistrationFormNotifications>(){});
				setNotificationsConfiguration(r);
			}

			n = root.get("PubliclyAvailable");
			setPubliclyAvailable(n.asBoolean());
			n = root.get("RegistrationCode");
			setRegistrationCode((n == null || n.isNull()) ? null : n.asText());
			
			if (root.has("CaptchaLength"))
			{
				n = root.get("CaptchaLength");
				setCaptchaLength(n.asInt());
			} else
			{
				setCaptchaLength(0);
			}

			n = root.get("ByInvitationOnly");
			if (n != null && !n.isNull())
				setByInvitationOnly(n.asBoolean());
			
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (byInvitationOnly ? 1231 : 1237);
		result = prime * result + captchaLength;
		result = prime
				* result
				+ ((defaultCredentialRequirement == null) ? 0
						: defaultCredentialRequirement.hashCode());
		result = prime
				* result
				+ ((notificationsConfiguration == null) ? 0
						: notificationsConfiguration.hashCode());
		result = prime * result + (publiclyAvailable ? 1231 : 1237);
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegistrationForm other = (RegistrationForm) obj;
		if (byInvitationOnly != other.byInvitationOnly)
			return false;
		if (captchaLength != other.captchaLength)
			return false;
		if (defaultCredentialRequirement == null)
		{
			if (other.defaultCredentialRequirement != null)
				return false;
		} else if (!defaultCredentialRequirement.equals(other.defaultCredentialRequirement))
			return false;
		if (notificationsConfiguration == null)
		{
			if (other.notificationsConfiguration != null)
				return false;
		} else if (!notificationsConfiguration.equals(other.notificationsConfiguration))
			return false;
		if (publiclyAvailable != other.publiclyAvailable)
			return false;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		return true;
	}
}
