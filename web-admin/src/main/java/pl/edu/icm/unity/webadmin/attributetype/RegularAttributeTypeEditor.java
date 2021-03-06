/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.SortedSet;
import java.util.TreeSet;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Allows to edit an attribute type. Can be configured to edit an existing attribute (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class RegularAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	
	private AbstractTextField name;
	private I18nTextField displayedName;
	private I18nTextArea typeDescription;
	private TextField min;
	private IntegerBoundEditor max;
	private CheckBox uniqueVals;
	private CheckBox selfModificable;
	private EnumComboBox<AttributeVisibility> visibility;
	private ComboBox syntax;
	private VerticalLayout syntaxPanel;
	private AttributeSyntaxEditor<?> editor;
	private MetadataEditor metaEditor;
	private FormValidator validator;
	
	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerReg)
	{
		this(msg, registry, null, attrMetaHandlerReg);
	}

	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, AttributeType toEdit, 
			AttributeMetadataHandlerRegistry attrMetaHandlerReg)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new RequiredTextField(msg);
		if (toEdit != null)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
		} else
			name.setValue(msg.getMessage("AttributeType.defaultName"));
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nTextArea(msg, msg.getMessage("AttributeType.description"));
		addComponent(typeDescription);
		
		min = new RequiredTextField(msg);
		min.setCaption(msg.getMessage("AttributeType.min"));
		min.setConverter(new StringToIntegerConverter());
		min.setNullRepresentation("");
		min.addValidator(new IntegerRangeValidator(msg.getMessage("AttributeType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		addComponent(min);

		max = new IntegerBoundEditor(msg, msg.getMessage("AttributeType.maxUnlimited"), 
				msg.getMessage("AttributeType.max"), Integer.MAX_VALUE);
		max.setMin(0);
		addComponent(max);
		
		uniqueVals = new CheckBox(msg.getMessage("AttributeType.uniqueValuesCheck"));
		addComponent(uniqueVals);
		
		selfModificable = new CheckBox(msg.getMessage("AttributeType.selfModificableCheck"));
		addComponent(selfModificable);
		
		visibility = new EnumComboBox<AttributeVisibility>(msg, "AttributeType.visibility.", 
				AttributeVisibility.class, AttributeVisibility.full);
		visibility.setCaption(msg.getMessage("AttributeType.visibility"));
		visibility.setSizeUndefined();
		addComponent(visibility);
		
		syntax = new ComboBox(msg.getMessage("AttributeType.type"));
		syntax.setNullSelectionAllowed(false);
		syntax.setImmediate(true);
		SortedSet<String> syntaxes = new TreeSet<String>(registry.getSupportedSyntaxes());
		for (String syntaxId: syntaxes)
			syntax.addItem(syntaxId);
		addComponent(syntax);
		
		Panel syntaxPanelP = new SafePanel();
		syntaxPanel = new VerticalLayout();
		syntaxPanel.setMargin(true);
		syntaxPanelP.setContent(syntaxPanel);
		
		addComponent(syntaxPanelP);
		
		syntax.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public void valueChange(ValueChangeEvent event)
			{
				String syntaxId = (String)syntax.getValue();
				WebAttributeHandler handler = registry.getHandler(syntaxId);
				editor = handler.getSyntaxEditorComponent(null);
				syntaxPanel.removeAllComponents();
				syntaxPanel.addComponent(editor.getEditor());
			}
		});
		
		metaEditor = new MetadataEditor(msg, attrMetaHandlerReg);
		metaEditor.setMargin(true);
		Panel metaPanel = new SafePanel(msg.getMessage("AttributeType.metadata"), metaEditor);
		addComponent(metaPanel);
		
		if (toEdit != null)
			setInitialValues(toEdit);
		else
		{
			min.setValue("1");
			max.setValue(1);
			syntax.setValue(syntaxes.first());
		}
		
		validator = new FormValidator(this);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setInitialValues(AttributeType aType)
	{
		typeDescription.setValue(aType.getDescription());
		min.setValue(aType.getMinElements()+"");
		max.setValue(aType.getMaxElements());
		uniqueVals.setValue(aType.isUniqueValues());
		selfModificable.setValue(aType.isSelfModificable());
		visibility.setEnumValue(aType.getVisibility());
		String syntaxId = aType.getValueType().getValueSyntaxId();
		syntax.setValue(syntaxId);
		WebAttributeHandler handler = registry.getHandler(aType.getValueType().getValueSyntaxId());
		editor = handler.getSyntaxEditorComponent(aType.getValueType());
		syntaxPanel.removeAllComponents();
		syntaxPanel.addComponent(editor.getEditor());
		metaEditor.setInput(aType.getMetadata());
		displayedName.setValue(aType.getDisplayedName());
	}
	
	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		try
		{
			validator.validate();
		} catch (FormValidationException e)
		{
			throw new IllegalAttributeTypeException("");
		}
		
		AttributeValueSyntax<?> syntax = editor.getCurrentValue();
		AttributeType ret = new AttributeType();
		ret.setDescription(typeDescription.getValue());
		ret.setName(name.getValue());
		ret.setMaxElements(max.getValue());
		ret.setMinElements((Integer)min.getConvertedValue());
		ret.setSelfModificable(selfModificable.getValue());
		ret.setUniqueValues(uniqueVals.getValue());
		ret.setValueType(syntax);
		ret.setVisibility(visibility.getSelectedValue());
		ret.setMetadata(metaEditor.getValue());
		I18nString displayedNameS = displayedName.getValue();
		displayedNameS.setDefaultValue(ret.getName());
		ret.setDisplayedName(displayedNameS);
		return ret;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
