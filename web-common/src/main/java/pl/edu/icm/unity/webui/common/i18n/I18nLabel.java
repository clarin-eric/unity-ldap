/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows {@link I18nString} in read only mode. Implemented as Custom field for convenience. 
 * <p>
 * IMPORTANT! This class is using {@link HtmlConfigurableLabel} underneath, so use with caution.
 * @author K. Benedyczak
 */
public class I18nLabel extends CustomField<I18nString>
{
	private static final int MAX_LINE = 80;
	
	private String defaultLocaleCode;
	private Map<String, Locale> enabledLocales;
	private HtmlConfigurableLabel defaultTf;
	private Map<String, HPairLayout> translationTFs = new HashMap<String, HPairLayout>();
	private VerticalLayout main;
	
	public I18nLabel(UnityMessageSource msg)
	{
		this.enabledLocales = new HashMap<String, Locale>(msg.getEnabledLocales());
		this.defaultLocaleCode = msg.getDefaultLocaleCode();
		initUI();
	}

	public I18nLabel(UnityMessageSource msg, String caption)
	{
		this(msg);
		setCaption(caption);
	}
	
	private void initUI()
	{
		HPairLayout defL = new HPairLayout();
		defaultTf = new HtmlConfigurableLabel();
		Resource defStyle = Images.getFlagForLocale(defaultLocaleCode);
		if (defStyle != null)
			defL.addImage(defStyle);
		defL.addLabel(defaultTf);
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(defL);

		for (Map.Entry<String, Locale> locE: enabledLocales.entrySet())
		{
			String localeKey = locE.getValue().toString();
			if (defaultLocaleCode.equals(localeKey))
				continue;

			HPairLayout pair = new HPairLayout();
			HtmlConfigurableLabel tf = new HtmlConfigurableLabel();
			pair.addLabel(tf);
			Resource image = Images.getFlagForLocale(localeKey);
			if (image != null)
				pair.addImage(image);
			translationTFs.put(locE.getValue().toString(), pair);
			
			main.addComponent(pair);
		}
		this.main = main;
	}

	
	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	public void setValue(I18nString value)
	{
		super.setValue(value);
		for (HPairLayout locE: translationTFs.values())
			locE.setVisible(false);
		defaultTf.setVisible(false);
		main.setSpacing(false);
		for (Map.Entry<String, String> vE: value.getMap().entrySet())
		{
			if (vE.getValue().length() > MAX_LINE)
				main.setSpacing(true);
			if (vE.getKey().equals(defaultLocaleCode))
			{
				defaultTf.setValue(changeNewLines(vE.getValue()));
				defaultTf.setVisible(true);
			} else
			{
				HPairLayout tf = translationTFs.get(vE.getKey());
				if (tf != null)
				{
					tf.setLabelValue(changeNewLines(vE.getValue()));
					tf.setVisible(true);
				}
			}
		}
		if (!defaultTf.isVisible() && value.getDefaultValue() != null)
		{
			defaultTf.setValue(changeNewLines(value.getDefaultValue()));
			defaultTf.setVisible(true);
		}
	}
	
	public static String changeNewLines(String src)
	{
		return breakLines(src, MAX_LINE).replace("\n", "<br>");
	}
	
	public static String breakLines(String src, int maxLine)
	{
		StringBuilder sb = new StringBuilder();
		int start = 0;
		int breakPos;
		do
		{
			breakPos = src.indexOf('\n', start);
			if (breakPos == -1)
				breakPos = src.length();
			if (breakPos - start > maxLine)
			{
				int lastSpace = src.lastIndexOf(' ', start + maxLine - 1);
				if (lastSpace <= start)
					lastSpace = start + maxLine - 1;
				sb.append(src.subSequence(start, lastSpace + 1)).append("\n");
				start = lastSpace + 1;
			} else
			{
				sb.append(src.subSequence(start, breakPos));
				if (breakPos < src.length())
					sb.append("\n");
				start = breakPos + 1;
			}
		} while(start < src.length());
		return sb.toString();
	}
	
	@Override
	public Class<? extends I18nString> getType()
	{
		return I18nString.class;
	}
	
	private class HPairLayout extends HorizontalLayout
	{
		private HtmlConfigurableLabel label;
		
		public HPairLayout()
		{
			setSpacing(true);
			addStyleName(Styles.smallSpacing.toString());
		}
		
		public void addImage(Resource res)
		{
			Image img = new Image();
			img.setSource(res);
			addComponentAsFirst(img);
			setComponentAlignment(img, Alignment.MIDDLE_LEFT);
		}
		
		public void addLabel(HtmlConfigurableLabel l)
		{
			addComponent(l);
			setComponentAlignment(l, Alignment.MIDDLE_LEFT);
			this.label = l;
		}
		
		public void setLabelValue(String value)
		{
			this.label.setValue(value);
		}
	}
}
