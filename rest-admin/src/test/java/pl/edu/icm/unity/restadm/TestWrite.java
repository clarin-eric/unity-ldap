/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class TestWrite extends TestRESTBase
{
	private HttpContext localcontext;
	
	private ObjectMapper m = new ObjectMapper();

	private HttpHost host;

	private HttpClient client;
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Before
	public void setup() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("System Manager");
		super.deployEndpoint(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm");		
		client = getClient();
		host = new HttpHost("localhost", 53456, "https");
		localcontext = getClientContext(client, host);
	}
	
	@Test
	public void setRemoveAttribute() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam("userName", "userC"), 
				"cr-pass", EntityState.valid, false);
		long entityId = identity.getEntityId();
		attrsMan.addAttributeType(new AttributeType("stringA", new StringAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("intA", new IntegerAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("floatA", new FloatingPointAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("enumA", new EnumAttributeSyntax("V1", "V2")));
		AttributeType email =  new AttributeType("emailA", new VerifiableEmailAttributeSyntax());
		email.setMaxElements(2);
		attrsMan.addAttributeType(email);
		attrsMan.addAttributeType(new AttributeType("jpegA", new JpegImageAttributeSyntax()));
		
		setSingleAttribute(entityId, new StringAttribute(
				"stringA", "/", AttributeVisibility.full, Collections.singletonList("value1")));

		setSingleAttribute(entityId, new IntegerAttribute(
				"intA", "/", AttributeVisibility.full, Collections.singletonList(123L)));

		setSingleAttribute(entityId, new FloatingPointAttribute(
				"floatA", "/", AttributeVisibility.full, Collections.singletonList(123.1)));

		setSingleAttribute(entityId, new EnumAttribute(
				"enumA", "/", AttributeVisibility.full, Collections.singletonList("V1")));

		setSingleAttribute(entityId, new VerifiableEmailAttribute(
				"emailA", "/", AttributeVisibility.full, new VerifiableEmail("some@example.com"),
				new VerifiableEmail("some2@example.com", new ConfirmationInfo(true))));
		
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		setSingleAttribute(entityId, new JpegImageAttribute(
				"jpegA", "/", AttributeVisibility.full, image));

		HttpDelete removeAttribute = new HttpDelete("/restadm/v1/entity/" + entityId + "/attribute/stringA");
		HttpResponse response = client.execute(host, removeAttribute, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertEquals(0, attrsMan.getAttributes(new EntityParam(entityId), "/", "stringA").size());
	}

	private void setSingleAttribute(long entityId, Attribute<?> a) throws EngineException, 
		UnsupportedCharsetException, ClientProtocolException, IOException
	{
		HttpPut setAttribute = new HttpPut("/restadm/v1/entity/" + entityId + "/attribute");
		AttributeParamRepresentation ap = new AttributeParamRepresentation(a);
		setAttribute.setEntity(new StringEntity(m.writeValueAsString(ap), ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(host, setAttribute, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertEquals(1, attrsMan.getAttributes(new EntityParam(entityId), a.getGroupPath(), 
				a.getName()).size());
		assertEquals(a.getValues().size(), attrsMan.getAttributes(new EntityParam(entityId), a.getGroupPath(), 
				a.getName()).iterator().next().getValues().size());
		System.out.println("Set attribute:\n" + m.writeValueAsString(ap));
	}
	
	@Test
	public void addRemoveIdentityAndEntity() throws Exception
	{
		HttpPost addEntity = new HttpPost("/restadm/v1/entity/identity/userName/userA?credentialRequirement=cr-pass");
		HttpResponse response = client.execute(host, addEntity, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		ObjectNode root = (ObjectNode) m.readTree(contents);
		long entityId = root.get("entityId").asLong();
		assertTrue(checkIdentity("userA"));
		System.out.println("Added entity:\n" + contents);
		

		HttpPost addIdentity = new HttpPost("/restadm/v1/entity/" + entityId + "/identity/userName/userB");
		response = client.execute(host, addIdentity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertTrue(checkIdentity("userB"));
		System.out.println("Added identity");

		
		HttpDelete deleteIdentity = new HttpDelete("/restadm/v1/entity/identity/userName/userB");
		response = client.execute(host, deleteIdentity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertFalse(checkIdentity("userB"));
		System.out.println("Removed identity");
		

		HttpPut setCredentialAdm = new HttpPut("/restadm/v1/entity/" + entityId + "/credential-adm/credential1");
		PasswordToken pass = new PasswordToken("newpass");
		pass.setQuestion(1);
		pass.setAnswer("Some answer");
	        setCredentialAdm.setEntity(new StringEntity(pass.toJson(), ContentType.APPLICATION_JSON));
		response = client.execute(host, setCredentialAdm, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertNull(response.getEntity());
		System.out.println("Set entity credential (adm) with new secret:\n" + pass.toJson());

		HttpPut setCredential = new HttpPut("/restadm/v1/entity/" + entityId + "/credential/credential1");
		PasswordToken pass2 = new PasswordToken("newpass2");
		pass2.setQuestion(0);
		pass2.setAnswer("Some answer2");
		ArrayNode arrayNode = m.createArrayNode();
		arrayNode.add(pass2.toJson());
		arrayNode.add(pass.toJson());
	        setCredential.setEntity(new StringEntity(m.writeValueAsString(arrayNode), 
	        		ContentType.APPLICATION_JSON));
		response = client.execute(host, setCredential, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Set entity credential (user) with new secret:\n" + m.writeValueAsString(arrayNode));
		
		
		groupsMan.addGroup(new Group("/example"));

		HttpPost addMember = new HttpPost("/restadm/v1/group/%2Fexample/entity/" + entityId);
		response = client.execute(host, addMember, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertTrue(idsMan.getGroups(new EntityParam(entityId)).contains("/example"));
		System.out.println("Added entity to group");

		
		HttpDelete removeMember = new HttpDelete("/restadm/v1/group/%2Fexample/entity/" + entityId);
		response = client.execute(host, removeMember, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		assertFalse(idsMan.getGroups(new EntityParam(entityId)).contains("/example"));
		System.out.println("Removed membership");

		
		HttpDelete deleteEntity = new HttpDelete("/restadm/v1/entity/" + entityId);
		response = client.execute(host, deleteEntity, localcontext);
		assertNull(response.getEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("Removed entity");
		
		try
		{
			idsMan.getEntity(new EntityParam(entityId));
			fail("Entity not removed");
		} catch (IllegalIdentityValueException e)
		{
			//OK
		}
	}
	
	private boolean checkIdentity(String name) throws EngineException
	{
		try
		{
			idsMan.getEntity(new EntityParam(new IdentityTaV("userName", name)));
			return true;
		} catch (IllegalIdentityValueException e)
		{
			return false;
		}
	}
}
