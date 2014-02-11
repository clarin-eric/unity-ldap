/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;


/**
 * Returns SAML metadata generated by a given {@link MetadataProvider}. MetadataProviders are associated with paths.
 * 
 * 
 * @author K. Benedyczak
 */
public class MultiMetadataServlet extends MetadataServlet
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, MultiMetadataServlet.class);
	private Map<String, MetadataProvider> metaProviders;
	private String servletPath;
	
	public MultiMetadataServlet(String servletPath)
	{
		this.metaProviders = new HashMap<>();
		this.servletPath = servletPath;
	}
	
	public synchronized void addProvider(String path, MetadataProvider provider)
	{
		if (!metaProviders.containsKey(path))
			log.info("Added SAML metadata provider at " + getServletContext().getContextPath() + servletPath 
				+ path);
		metaProviders.put(path, provider);
	}
	
	public synchronized void removeProvider(String path)
	{
		metaProviders.remove(path);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		String path = req.getPathInfo();
		MetadataProvider provider;
		synchronized (this)
		{
			if (path == null || !metaProviders.containsKey(path))
			{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No metadata at this location: " 
						+ path);
				return;
			}
			provider = metaProviders.get(path);
		}
		serveMetadata(provider, req, resp);
	}
}
