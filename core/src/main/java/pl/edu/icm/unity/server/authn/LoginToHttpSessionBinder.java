/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Helper class, works as application singleton. Maintains an association of Unity's {@link LoginSession}s
 * with {@link HttpSession}s. The main purpose is to invalidate the latter when Unity session is expired.
 * The implementation also takes care about memory consumption: whenever a {@link HttpSession} is expired
 * it is removed from the registry. 
 * <p>
 * Thread safe.
 * @author K. Benedyczak
 */
@Component
public class LoginToHttpSessionBinder
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, LoginToHttpSessionBinder.class);
	/**
	 * Under this key the {@link LoginSession} id is stored in the HTTP session.
	 */
	public static final String USER_SESSION_KEY = "pl.edu.icm.unity.web.WebSession";

	private Map<String, Collection<HttpSessionWrapper>> bindings = 
			new HashMap<String, Collection<HttpSessionWrapper>>(1000);
	
	/**
	 * @param toRemove
	 * @param soft if true then only the login data is removed from the HTTP session. Otherwise the whole
	 * session is invalidated 
	 */
	public synchronized void removeLoginSession(String toRemove, boolean soft)
	{
		Collection<HttpSessionWrapper> httpSessions = bindings.remove(toRemove);
		if (httpSessions != null)
		{
			for (HttpSessionWrapper sw: httpSessions)
			{
				if (!soft)
				{
					log.debug("Invalidating HTTP session " + sw.session.getId()
							+ " of login session " + sw.loginSessionId);
					sw.session.invalidate();
				} else
				{
					log.debug("Removing logged session " + sw.loginSessionId +
							" from HTTP session " + sw.session.getId());
					sw.session.removeAttribute(USER_SESSION_KEY);
				}
			}
		}
	}
	
	public synchronized void bindHttpSession(HttpSession session, LoginSession owning)
	{
		Collection<HttpSessionWrapper> httpSessions = bindings.get(owning.getId());
		if (httpSessions == null)
		{
			httpSessions = new HashSet<HttpSessionWrapper>();
			bindings.put(owning.getId(), httpSessions);
		}
		log.debug("Binding HTTP session " + session.getId() + " to login session " + owning.getId());
		HttpSessionWrapper wrapper = new HttpSessionWrapper(session, owning.getId());
		httpSessions.add(wrapper);
		//to receive unbound event when the session is invalidated
		session.setAttribute(HttpSessionWrapper.class.getName(), wrapper);
		session.setAttribute(USER_SESSION_KEY, owning);
	}
	
	private synchronized void unbindHttpSession(HttpSessionWrapper session, String owning)
	{
		Collection<HttpSessionWrapper> httpSessions = bindings.get(owning);
		if (httpSessions != null)
			httpSessions.remove(session);
	}
	
	class HttpSessionWrapper implements HttpSessionBindingListener
	{
		private HttpSession session;
		private String loginSessionId;
		
		public HttpSessionWrapper(HttpSession session, String loginSessionId)
		{
			super();
			this.session = session;
			this.loginSessionId = loginSessionId;
		}

		@Override
		public void valueBound(HttpSessionBindingEvent event)
		{
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event)
		{
			log.trace("Value unbound for session " + loginSessionId);
			unbindHttpSession(this, loginSessionId);
		}
	}
}
