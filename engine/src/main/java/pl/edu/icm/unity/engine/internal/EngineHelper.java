/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.engine.authn.CredentialHolder;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Misc operations on entities, attributes and generic objects useful for multiple
 * *ManagementImpl classes.
 * @author K. Benedyczak
 */
@Component
public class EngineHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EngineHelper.class);
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;
	private DBGroups dbGroups;
	private LocalCredentialsRegistry authReg;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialRequirementDB;
	private AttributesHelper attributesHelper;
	
	@Autowired
	public EngineHelper(DBAttributes dbAttributes, DBIdentities dbIdentities,
			DBGroups dbGroups, LocalCredentialsRegistry authReg,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			AttributesHelper attributesHelper)
	{
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.authReg = authReg;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
		this.attributesHelper = attributesHelper;
	}

	public void setEntityCredentialRequirements(long entityId, String credReqId, SqlSession sqlMap) 
			throws EngineException
	{
		if (!credentialRequirementDB.exists(credReqId, sqlMap))
			throw new IllegalArgumentException("There is no required credential set with id " + credReqId);
		setEntityCredentialRequirementsNoCheck(entityId, credReqId, sqlMap);
	}

	public void setEntityCredentialRequirementsNoCheck(long entityId, String credReqId, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalTypeException, IllegalAttributeTypeException, IllegalGroupValueException
	{
		StringAttribute credReq = new StringAttribute(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS,
				"/", AttributeVisibility.local, credReqId);
		dbAttributes.addAttribute(entityId, credReq, true, sqlMap);
	}

	public CredentialRequirementsHolder getCredentialRequirements(String requirementName, SqlSession sqlMap) 
			throws EngineException
	{
		CredentialRequirements requirements = credentialRequirementDB.get(requirementName, sqlMap);
		List<CredentialDefinition> credDefs = credentialDB.getAll(sqlMap);
		return new CredentialRequirementsHolder(authReg, requirements, credDefs);
	}
	

	public Set<Long> getEntitiesByAttribute(String attribute, Set<String> values, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		return dbAttributes.getEntitiesBySimpleAttribute("/", attribute, values, sql);
	}
	
	/**
	 * @param desiredCredState If value is 'correct', then method checks if there is an existing credential and 
	 * if it is correct with the given CredentialHolder. If it is set and incorrect, an exception is thrown. 
	 * If the value is 'outdated' then nothing is done.
	 * If the value is 'notSet' then the credential is removed if the entity has it set. 
	 * @param entityId
	 * @param credentialChanged
	 * @param sql
	 * @throws EngineException 
	 */
	public void checkEntityCredentialState(long entityId, LocalCredentialState desiredCredState,
			CredentialHolder credentialChanged, SqlSession sql) 
			throws EngineException
	{
		if (desiredCredState == LocalCredentialState.outdated)
			return;
		String credAttribute = SystemAttributeTypes.CREDENTIAL_PREFIX+
				credentialChanged.getCredentialDefinition().getName(); 
		Collection<AttributeExt<?>> attributes = dbAttributes.getAllAttributes(entityId, "/", false,
				credAttribute, sql);
		if (attributes.isEmpty())
			return;
		if (desiredCredState == LocalCredentialState.notSet)
		{
			dbAttributes.removeAttribute(entityId, "/", credAttribute, sql);
			return;
		}
		String credential = (String)attributes.iterator().next().getValues().get(0);
		CredentialPublicInformation currentState = 
				credentialChanged.getHandler().checkCredentialState(credential);
		if (currentState.getState() != LocalCredentialState.correct && 
				desiredCredState == LocalCredentialState.correct)
			throw new IllegalCredentialException("The new credential is not compatible with the previous definition and can not keep the credential state as correct");
	}
	
	/**
	 * Adds an entity with all the complicated logic around it. Does not perform authorization and DB 
	 * transaction set up: pure business logic.
	 * @param toAdd
	 * @param credReqId
	 * @param initialState
	 * @param extractAttributes
	 * @param attributes
	 * @param sqlMap
	 * @throws EngineException 
	 */
	public Identity addEntity(IdentityParam toAdd, String credReqId, EntityState initialState, 
			boolean extractAttributes, List<Attribute<?>> attributes, boolean honorInitialConfirmation,
			SqlSession sqlMap) throws EngineException
	{
		attributesHelper.checkGroupAttributeClassesConsistency(attributes, "/", sqlMap);
		
		Identity ret = dbIdentities.insertIdentity(toAdd, null, false, sqlMap);
		long entityId = ret.getEntityId();

		dbIdentities.setEntityStatus(entityId, initialState, sqlMap);
		dbGroups.addMemberFromParent("/", new EntityParam(ret.getEntityId()), null, null, new Date(), sqlMap);
		setEntityCredentialRequirements(entityId, credReqId, sqlMap);
		
		attributesHelper.addAttributesList(attributes, entityId, honorInitialConfirmation, sqlMap);
		
		if (extractAttributes)
			extractAttributes(ret, sqlMap);
		return ret;
	}
	
	public void extractAttributes(Identity from, SqlSession sql)
	{
		IdentityType idType = from.getType();
		IdentityTypeDefinition typeProvider = idType.getIdentityTypeProvider();
		Map<String, String> toExtract = idType.getExtractedAttributes();
		List<Attribute<?>> extractedList = typeProvider.extractAttributes(from.getValue(), toExtract);
		if (extractedList == null)
			return;
		long entityId = from.getEntityId();
		for (Attribute<?> extracted: extractedList)
		{
			extracted.setGroupPath("/");
			try
			{
				dbAttributes.addAttribute(entityId, extracted, false, sql);
			} catch (EngineException e)
			{
				log.warn("Can not add extracted attribute " + extracted.getName() 
						+ " for entity " + entityId + ": " + e.toString());
			}
		}
	}

	
	/**
	 * Sets entity's credential. This is internal method which doesn't perform any authorization nor
	 * argument initialization checking.
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void setEntityCredentialInternal(long entityId, String credentialId, String rawCredential, 
			String currentRawCredential,
			SqlSession sqlMap) throws EngineException
	{
		String newCred = prepareEntityCredentialInternal(entityId, credentialId, rawCredential, 
				currentRawCredential, sqlMap);
		setPreviouslyPreparedEntityCredentialInternal(entityId, newCred, credentialId, sqlMap);
	}
	
	/**
	 * Prepares entity's credential (hashes, checks etc). This is internal method which doesn't perform any authorization nor
	 * argument initialization checking.
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param sqlMap
	 * @throws EngineException
	 */
	public String prepareEntityCredentialInternal(long entityId, String credentialId, 
			String rawCredential, String currentRawCredential,
			SqlSession sqlMap) throws EngineException
	{
		Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
				entityId, "/", null, sqlMap);
		
		Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
		String credentialRequirements = (String)credReqA.getValues().get(0);
		CredentialRequirementsHolder credReqs = getCredentialRequirements(credentialRequirements, sqlMap);
		LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
		if (handler == null)
			throw new IllegalCredentialException("The credential id is not among the " +
					"entity's credential requirements: " + credentialId);

		String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
		Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;
				
		return currentRawCredential == null ? handler.prepareCredential(rawCredential, currentCredential) :
				handler.prepareCredential(rawCredential, currentRawCredential, currentCredential);
	}

	/**
	 * Sets a credential which was previously prepared (i.e. hashed etc). Absolutely no checking is performed.
	 * @param entityId
	 * @param newCred
	 * @param credentialId
	 * @param sqlMap
	 * @throws EngineException
	 */
	public void setPreviouslyPreparedEntityCredentialInternal(long entityId, String newCred, 
			String credentialId, SqlSession sqlMap) throws EngineException
	{
		String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
		StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
				"/", AttributeVisibility.local, Collections.singletonList(newCred));
		dbAttributes.addAttribute(entityId, newCredentialA, true, sqlMap);
	}
	
}
