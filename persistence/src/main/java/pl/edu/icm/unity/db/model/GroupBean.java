/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;


/**
 * In DB group representation.
 * @author K. Benedyczak
 */
public class GroupBean extends BaseBean
{
	private Long parent;
	
	public GroupBean() 
	{
	}
	
	public GroupBean(Long parent, String name) 
	{
		this.parent = parent;
		setName(name);
	}
	
	public Long getParent()
	{
		return parent;
	}
	public void setParent(Long parent)
	{
		this.parent = parent;
	}
}
