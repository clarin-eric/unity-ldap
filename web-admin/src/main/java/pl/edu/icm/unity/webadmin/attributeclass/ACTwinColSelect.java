/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import pl.edu.icm.unity.types.basic.AttributesClass;

import com.vaadin.ui.TwinColSelect;

/**
 * Customization of the {@link TwinColSelect} for {@link AttributesClass} selection.
 * @author K. Benedyczak
 */
public class ACTwinColSelect extends TwinColSelect
{
	public ACTwinColSelect(String leftCaption, String rightCaption)
	{
		this("", leftCaption, rightCaption);
	}

	public ACTwinColSelect(String caption, String leftCaption, String rightCaption)
	{
		setCaption(caption);
		setLeftColumnCaption(leftCaption);
		setRightColumnCaption(rightCaption);
		setImmediate(true);
		setWidth(90, Unit.PERCENTAGE);
		setRows(5);
	}
}
