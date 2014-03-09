/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link SessionManagement}
 * @author K. Benedyczak
 */
@Component
public class SessionManagementImpl implements SessionManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, SessionManagementImpl.class);
	public static final long DB_ACTIVITY_WRITE_DELAY = 5000;
	public static final String SESSION_TOKEN_TYPE = "session";
	private TokensManagement tokensManagement;
	
	/**
	 * map of timestamps indexed by session ids, when the last activity update was written to DB.
	 */
	private Map<String, Long> recentUsageUpdates = new WeakHashMap<>();
	
	@Autowired
	public SessionManagementImpl(TokensManagement tokensManagement)
	{
		this.tokensManagement = tokensManagement;
	}

	@Override
	public LoginSession getCreateSession(long loggedEntity, AuthenticationRealm realm, String entityLabel, 
			boolean outdatedCredential)
	{
		Object transaction = tokensManagement.startTokenTransaction();
		try
		{
			try
			{
				LoginSession ret = getOwnedSession(new EntityParam(loggedEntity), 
						realm.getName(), transaction);
				if (ret != null)
				{
					if (log.isTraceEnabled())
						log.trace("Using existing session " + ret.getId() + " for logged entity "
							+ ret.getEntityId() + " in realm " + realm.getName());
					tokensManagement.commitTokenTransaction(transaction);
					return ret;
				}
			} catch (EngineException e)
			{
				throw new InternalException("Can't retrieve current sessions of the "
						+ "authenticated user", e);
			}
			
			LoginSession ret = createSession(loggedEntity, realm, entityLabel, outdatedCredential,
					transaction);
			tokensManagement.commitTokenTransaction(transaction);
			if (log.isDebugEnabled())
				log.debug("Created a new session " + ret.getId() + " for logged entity "
					+ ret.getEntityId() + " in realm " + realm.getName());
			return ret;
		} finally
		{
			tokensManagement.closeTokenTransaction(transaction);
		}
	}
	
	private LoginSession createSession(long loggedEntity, AuthenticationRealm realm, String entityLabel, 
			boolean outdatedCredential, Object transaction)
	{
		UUID randomid = UUID.randomUUID();
		String id = randomid.toString();
		LoginSession ls = new LoginSession(id, new Date(), realm.getMaxInactivity(), loggedEntity, 
				realm.getName());
		ls.setUsedOutdatedCredential(outdatedCredential);
		ls.setEntityLabel(entityLabel);
		try
		{
			tokensManagement.addToken(SESSION_TOKEN_TYPE, id, new EntityParam(loggedEntity), 
					ls.getTokenContents(), ls.getStarted(), ls.getExpires(), transaction);
		} catch (Exception e)
		{
			throw new InternalException("Can't create a new session", e);
		}
		return ls;
	}

	@Override
	public void updateSessionAttributes(String id, AttributeUpdater updater) 
			throws WrongArgumentException
	{
		Object transaction = tokensManagement.startTokenTransaction();
		try
		{
			Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id, transaction);
			LoginSession session = token2session(token);
			
			updater.updateAttributes(session.getSessionData());

			byte[] contents = session.getTokenContents();
			tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents, transaction);
			tokensManagement.commitTokenTransaction(transaction);
		} finally
		{
			tokensManagement.closeTokenTransaction(transaction);
		}
	}

	@Override
	public void removeSession(String id)
	{
		try
		{
			tokensManagement.removeToken(SESSION_TOKEN_TYPE, id, null);
		} catch (WrongArgumentException e)
		{
			//not found - ok
		}
	}

	@Override
	public LoginSession getSession(String id) throws WrongArgumentException
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id, null);
		return token2session(token);
	}

	
	private LoginSession getOwnedSession(EntityParam owner, String realm, Object transaction)
			throws EngineException
	{
		List<Token> tokens = tokensManagement.getOwnedTokens(SESSION_TOKEN_TYPE, owner, transaction);
		for (Token token: tokens)
		{
			LoginSession ls = token2session(token);
			if (realm.equals(ls.getRealm()))
				return ls;
		}
		return null;
	}
	
	@Override
	public LoginSession getOwnedSession(EntityParam owner, String realm)
			throws EngineException
	{
		LoginSession ret = getOwnedSession(owner, realm, null);
		if (ret == null)
			throw new WrongArgumentException("No session for this owner in the given realm");
		return ret;
	}
	
	@Override
	public void updateSessionActivity(String id) throws WrongArgumentException
	{
		Long lastWrite = recentUsageUpdates.get(id);
		if (lastWrite != null)
		{
			if (System.currentTimeMillis() < lastWrite + DB_ACTIVITY_WRITE_DELAY)
				return;
		}
		
		Object transaction = tokensManagement.startTokenTransaction();
		try
		{
			Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id, transaction);
			LoginSession session = token2session(token);
			session.setLastUsed(new Date());
			byte[] contents = session.getTokenContents();
			tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents, transaction);
			tokensManagement.commitTokenTransaction(transaction);
			recentUsageUpdates.put(id, System.currentTimeMillis());
		} finally
		{
			tokensManagement.closeTokenTransaction(transaction);
		}
	}
	
	private LoginSession token2session(Token token)
	{
		LoginSession session = new LoginSession();
		session.deserialize(token);
		return session;
	}
}
