package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class CustomTimePicker extends Element{

	private static String tag = "CustomTimePicker";
	
	/***********************************************************
	 * public TimePicker(String id, String width, String height, String text, String style)
	 * @param widht
	 *            The desired width (wrap_content, match_parent or size specific dp)
	 * @param height
	 *            The desired height (wrap_content, match_parent or size specific dp)
	 * @param text
	 *            The reference to the XML containing the data
	 * @param style
	 *            The reference to the XML containing the style
	 ***********************************************************/
	public CustomTimePicker(String customPackage, String id, String width, String height, String style, boolean allowInput){
		super(customPackage + ".customViews." + tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(style != null)
			super.setAttribute(new Style(style, ""));
		
		super.setAttribute(new Clickable(allowInput));	
	}
	
	
	private StringBuffer output;
	
	public void addAttribute(String att){
		output.append(" " + att);
	}
	
	public String toAndroidXML(){
		return output.toString();
	}
	
}
