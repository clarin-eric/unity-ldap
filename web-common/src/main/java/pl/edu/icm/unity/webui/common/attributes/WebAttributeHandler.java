/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

import com.vaadin.ui.Component;

/**
 * Vaadin component implementing support for {@link AttributeValueSyntax} implementation.
 * Allows to render attribute value and to provide an edit panel.
 * @author K. Benedyczak
 */
public interface WebAttributeHandler<T>
{
	public String getSupportedSyntaxId();
	public final static int MIN_VALUE_TEXT_LEN = 16;
	
	/**
	 * Defines the size of the returned representation of an attribute value
	 * @author K. Benedyczak
	 */
	public enum RepresentationSize {
		/**
		 * no restrictions on size
		 */
		ORIGINAL, 
		
		/**
		 * Smallest representation should fit into one line, table line etc
		 */
		LINE, 
		
		/**
		 * Can be bigger then one line but should fit into a regular form, typically not more then 
		 * ca 3 lines in height.
		 */
		MEDIUM
	}
	
	/**
	 * @param value
	 * @param syntax
	 * @param limited if more then zero, then the string representation should be no longer then
	 * the limit. It may be assumed that the limited won't be between 0 and MIN_VALUE_TEXT_LEN. 
	 * @return string representation, never null. For values which have no string representation some
	 * type based description should be returned as 'Jpeg image'
	 */
	public String getValueAsString(T value, AttributeValueSyntax<T> syntax, int limited);
	
	/**
	 * @param value
	 * @param syntax
	 * @param size
	 * @return component allowing to present the value
	 */
	public Component getRepresentation(T value, AttributeValueSyntax<T> syntax, RepresentationSize size);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeValueEditor<T> getEditorComponent(T initialValue, String label, 
			AttributeValueSyntax<T> syntaxDesc);
	
	/**
	 * @param syntax
	 * @return read-only component showing the syntax settings
	 */
	public Component getSyntaxViewer(AttributeValueSyntax<T> syntax);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeSyntaxEditor<T> getSyntaxEditorComponent(AttributeValueSyntax<T> initialValue);
}
