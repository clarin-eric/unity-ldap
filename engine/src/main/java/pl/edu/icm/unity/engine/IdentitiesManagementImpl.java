/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.DBShared;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Implementation of identities management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
public class IdentitiesManagementImpl implements IdentitiesManagement
{
	private DBSessionManager db;
	private DBIdentities dbIdentities;
	private DBAttributes dbAttributes;
	private DBShared dbShared;
	private IdentitiesResolver idResolver;
	private EngineHelper engineHelper;
	private AuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;

	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities,
			DBAttributes dbAttributes, DBShared dbShared,
			IdentitiesResolver idResolver, EngineHelper engineHelper,
			AuthorizationManager authz, IdentityTypesRegistry idTypesRegistry)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.dbAttributes = dbAttributes;
		this.dbShared = dbShared;
		this.idResolver = idResolver;
		this.engineHelper = engineHelper;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<IdentityType> getIdentityTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			List<IdentityType> ret = dbIdentities.getIdentityTypes(sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateIdentityType(IdentityType toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toUpdate.getIdentityTypeProvider().getId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Map<String, AttributeType> atsMap = dbAttributes.getAttributeTypes(sqlMap);
			Map<String, String> extractedAts = toUpdate.getExtractedAttributes();
			Set<AttributeType> supportedForExtraction = idTypeDef.getAttributesSupportedForExtraction();
			Map<String, AttributeType> supportedForExtractionMap = new HashMap<String, AttributeType>();
			for (AttributeType at: supportedForExtraction)
				supportedForExtractionMap.put(at.getName(), at);
			
			for (Map.Entry<String, String> extracted: extractedAts.entrySet())
			{
				AttributeType type = atsMap.get(extracted.getValue());
				if (type == null)
					throw new IllegalAttributeTypeException("Can not extract attribute " + 
							extracted.getKey() + " as " + extracted.getValue() + 
							" because the latter is not defined in the system");
				AttributeType supportedType = supportedForExtractionMap.get(extracted.getKey());
				if (supportedType == null)
					throw new IllegalAttributeTypeException("Can not extract attribute " + 
							extracted.getKey() + " as " + extracted.getValue() + 
							" because the former is not supported by the identity provider");
			}
			dbIdentities.updateIdentityType(sqlMap, toUpdate);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId, EntityState initialState,
			boolean extractAttributes) throws EngineException
	{
		return addEntity(toAdd, credReqId, initialState, extractAttributes, null);
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId,
			EntityState initialState, boolean extractAttributes,
			List<Attribute<?>> attributes) throws EngineException
	{
		toAdd.validateInitialization();
		authz.checkAuthorization(AuthzCapability.identityModify);
		if (attributes == null)
			attributes = Collections.emptyList();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Identity ret = engineHelper.addEntity(toAdd, credReqId, initialState, 
					extractAttributes, attributes, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity, boolean extractAttributes)
			throws EngineException
	{
		toAdd.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(parentEntity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			Identity ret = dbIdentities.insertIdentity(toAdd, entityId, false, sqlMap);
			if (extractAttributes)
				engineHelper.extractAttributes(ret, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeIdentity(IdentityTaV toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(new EntityParam(toRemove), sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.removeIdentity(toRemove, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}


	@Override
	public void resetIdentity(EntityParam toReset, String typeIdToReset,
			String realm, String target) throws EngineException
	{
		toReset.validateInitialization();
		if (typeIdToReset == null)
			throw new IllegalIdentityValueException("Identity type can not be null");
		IdentityTypeDefinition idType = idTypesRegistry.getByName(typeIdToReset);
		if (!idType.isDynamic())
			throw new IllegalIdentityValueException("Identity type " + typeIdToReset + 
					" is not dynamic and can not be reset");
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toReset, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.resetIdentityForEntity(entityId, typeIdToReset, realm, target, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEntity(EntityParam toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toRemove, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.removeEntity(entityId, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEntityStatus(EntityParam toChange, EntityState status)
			throws EngineException
	{
		toChange.validateInitialization();
		if (status == EntityState.onlyLoginPermitted)
			throw new IllegalArgumentException("The new entity status 'only login permitted' "
					+ "can be only set as a side effect of scheduling an account "
					+ "removal with a grace period.");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toChange, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.setEntityStatus(entityId, status, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public Entity getEntity(EntityParam entity) throws EngineException
	{
		return getEntity(entity, null, true, "/");
	}
	
	@Override
	public Entity getEntityNoContext(EntityParam entity, String group) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Entity ret;
			try
			{
				authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.readHidden);
				Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sqlMap);
				ret = assembleEntity(entityId, identities, sqlMap);
			} catch (AuthorizationException e)
			{
				ret = resolveEntityBasic(entityId, null, false, group, sqlMap);
			}
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public Entity getEntity(EntityParam entity, String target, boolean allowCreate, String group)
			throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Entity ret = resolveEntityBasic(entityId, target, allowCreate, group, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * Checks if read cap is set and resolved the entity: identities and credential with respect to the
	 * given target.
	 * @param entityId
	 * @param target
	 * @param allowCreate
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity resolveEntityBasic(long entityId, String target, boolean allowCreate, String group, 
			SqlSession sqlMap) throws EngineException
	{
		authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.read);
		Identity[] identities = dbIdentities.getIdentitiesForEntity(entityId, target, allowCreate, 
				sqlMap);
		return assembleEntity(entityId, identities, sqlMap);
	}
	
	/**
	 * assembles the final entity by adding the credential and state info.
	 * @param entityId
	 * @param identities
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity assembleEntity(long entityId, Identity[] identities, SqlSession sqlMap) throws EngineException
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, sqlMap);
		EntityInformation theState = dbIdentities.getEntityInformation(entityId, sqlMap);
		return new Entity(entityId, identities, theState, credInfo);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getGroups(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
			Set<String> allGroups = dbShared.getAllGroups(entityId, sqlMap);
			sqlMap.commit();
			return allGroups;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			engineHelper.setEntityCredentialRequirements(entityId, requirementId, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public boolean isCurrentCredentialRequiredForChange(EntityParam entity, String credentialId)
			throws EngineException
	{
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			boolean fullAuthz = false;
			try
			{
				authz.checkAuthorization(AuthzCapability.credentialModify);
				fullAuthz = true;
			} catch (AuthorizationException e)
			{
				//OK
			}
			sqlMap.commit();
			return !fullAuthz;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}

	}
	
	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential) 
			throws EngineException
	{
		setEntityCredential(entity, credentialId, rawCredential, null);
	}
	
	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential,
			String currentRawCredential) throws EngineException
	{
		if (rawCredential == null)
			throw new IllegalCredentialException("The credential can not be null");
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			boolean fullAuthz = false;
			try
			{
				authz.checkAuthorization(AuthzCapability.credentialModify);
				fullAuthz = true;
			} catch (AuthorizationException e)
			{
				authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.credentialModify);
			}
			
			if (!fullAuthz && currentRawCredential == null)
				throw new IllegalPreviousCredentialException(
						"The current credential must be provided");
			//we don't check it 
			if (fullAuthz)
				currentRawCredential = null;
			
			engineHelper.setEntityCredentialInternal(entityId, credentialId, rawCredential, 
					currentRawCredential, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setEntityCredentialStatus(EntityParam entity, String credentialId,
			LocalCredentialState desiredCredentialState) throws EngineException
	{
		entity.validateInitialization();
		if (desiredCredentialState == LocalCredentialState.correct)
			throw new WrongArgumentException("Credential can not be put into the correct state with this method. Use setEntityCredential.");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
					entityId, "/", null, sqlMap);
			
			Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
			String credentialRequirements = (String)credReqA.getValues().get(0);
			CredentialRequirementsHolder credReqs = engineHelper.getCredentialRequirements(
					credentialRequirements, sqlMap);
			LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
			if (handler == null)
				throw new IllegalCredentialException("The credential id is not among the entity's credential requirements: " + credentialId);

			String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
			Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
			String currentCredential = currentCredentialA != null ? 
					(String)currentCredentialA.getValues().get(0) : null;
					
			if (currentCredential == null)
			{ 
				if (desiredCredentialState != LocalCredentialState.notSet)
					throw new IllegalCredentialException("The credential is not set, so it's state can be only notSet");
				return;
			}
			
			//remove or invalidate
			if (desiredCredentialState == LocalCredentialState.notSet)
			{
				dbAttributes.removeAttribute(entityId, "/", credentialAttributeName, sqlMap);
				attributes.remove(credentialAttributeName);
			} else if (desiredCredentialState == LocalCredentialState.outdated)
			{
				if (!handler.isSupportingInvalidation())
					throw new IllegalCredentialException("The credential doesn't support the outdated state");
				String updated = handler.invalidate(currentCredential);
				StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
						"/", AttributeVisibility.local, Collections.singletonList(updated));
				Date now = new Date();
				AttributeExt added = new AttributeExt(newCredentialA, true, now, now);
				attributes.put(credentialAttributeName, added);
				dbAttributes.addAttribute(entityId, added, true, sqlMap);
			}
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	private CredentialInfo getCredentialInfo(long entityId, SqlSession sqlMap) 
			throws EngineException
	{
		Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(entityId, "/", null, sqlMap);
		
		Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
		if (credReqA == null)
			throw new InternalException("No credential requirement set for an entity"); 
		String credentialRequirementId = (String)credReqA.getValues().get(0);
		
		CredentialRequirementsHolder credReq = engineHelper.getCredentialRequirements(
				credentialRequirementId, sqlMap);
		Set<String> required = credReq.getCredentialRequirements().getRequiredCredentials();
		Map<String, CredentialPublicInformation> credentialsState = new HashMap<>();
		for (String cd: required)
		{
			LocalCredentialVerificator handler = credReq.getCredentialHandler(cd);
			Attribute<?> currentCredA = attributes.get(SystemAttributeTypes.CREDENTIAL_PREFIX+cd);
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			
			credentialsState.put(cd, handler.checkCredentialState(currentCred));
		}
		
		return new CredentialInfo(credentialRequirementId, credentialsState);
	}
	

	@Override
	public void scheduleEntityChange(EntityParam toChange, Date changeTime,
			EntityScheduledOperation operation) throws EngineException
	{
		toChange.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toChange, sqlMap);

			AuthzCapability requiredCap = (operation == EntityScheduledOperation.REMOVAL_AFTER_GRACE_PERIOD) ?
					AuthzCapability.attributeModify : AuthzCapability.identityModify;
			authz.checkAuthorization(authz.isSelf(entityId), requiredCap);
			
			dbIdentities.setScheduledRemovalStatus(entityId, changeTime, operation, sqlMap);
			
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
}
