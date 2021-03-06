/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Component;

/**
 * Table with attribute values.
 * @author K. Benedyczak
 */
public class ValuesTable extends SmallTable
{
	private AttributeValueSyntax<?> syntax;
	@SuppressWarnings("rawtypes")
	private WebAttributeHandler handler;
	
	public ValuesTable(UnityMessageSource msg)
	{
		setSortEnabled(false);
		setNullSelectionAllowed(false);
		setSelectable(true);
		setMultiSelect(false);
		setSizeFull();
		
		BeanItemContainer<ValueItem> tableContainer = new BeanItemContainer<ValueItem>(ValueItem.class);
		setContainerDataSource(tableContainer);
		setColumnHeaders(new String[] {msg.getMessage("Attribute.values")});
	}
	

	public List<?> getValues()
	{
		@SuppressWarnings("unchecked")
		BeanItemContainer<ValueItem> tableContainer = (BeanItemContainer<ValueItem>) 
				getContainerDataSource();
		List<Object> ret = new ArrayList<Object>(tableContainer.size());
		for (ValueItem item: tableContainer.getItemIds())
		{
			ret.add(item.getAttributeValue());
		}
		return ret;
	}
	
	public void setValues(List<?> values, AttributeValueSyntax<?> syntax, WebAttributeHandler<?> handler)
	{
		this.syntax = syntax;
		this.handler = handler;
		Container tableContainer = getContainerDataSource();
		tableContainer.removeAllItems();
		for (Object val: values)
		{
			ValueItem item = new ValueItem(val);
			tableContainer.addItem(item);
		}
	}
	
	public Object getItemById(Object itemId)
	{
		final Item sourceItem = getItem(itemId);
		return ((ValueItem)((BeanItem<?>)sourceItem).getBean()).getAttributeValue();
	}
	
	public void updateItem(Object itemId, Object newValue)
	{
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		Object previous = container.prevItemId(itemId);
		container.removeItem(itemId);
		container.addItemAfter(previous, new ValueItem(newValue));
	}
	
	public void addItem(Object after, Object newValue)
	{
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		if (after == null)
			after = container.lastItemId();
		container.addItemAfter(after, new ValueItem(newValue));
	}
	
	public void moveItemAfter(Object toMoveItemId, Object moveAfterItemId)
	{
		final Object value = getItemById(toMoveItemId);
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		container.removeItem(toMoveItemId);
		container.addItemAfter(moveAfterItemId, new ValuesTable.ValueItem(value));
	}
	
	public void moveBefore(Object toMoveItemId, Object moveBeforeItemId)
	{
		final Object value = getItemById(toMoveItemId);
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		Object previous = container.prevItemId(moveBeforeItemId);
		if (toMoveItemId == previous)
			return;
		
		container.removeItem(toMoveItemId);
		container.addItemAfter(previous, new ValuesTable.ValueItem(value));
	}
	
	public class ValueItem
	{
		private Object value;
		private Component contents;

		@SuppressWarnings("unchecked")
		public ValueItem(Object value)
		{
			this.value = value;
			contents = handler.getRepresentation(value, syntax, RepresentationSize.LINE);
		}
		
		public Component getContents()
		{
			return contents; 
		}
		
		private Object getAttributeValue()
		{
			return value;
		}
	}
}
