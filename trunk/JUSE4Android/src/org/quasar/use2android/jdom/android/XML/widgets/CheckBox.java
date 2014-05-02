package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class CheckBox extends Element{

	private static String tag = "CheckBox";
	
	/***********************************************************
	 * @param id
	 *            The id of the component
	 * @param widht
	 *            The desired width (wrap_content, match_parent or size specific dp)
	 * @param height
	 *            The desired height (wrap_content, match_parent or size specific dp)
	 * @param style
	 *            The reference to the XML containing the style
	 ***********************************************************/
	public CheckBox(String id, String width, String height, String style, boolean allowInput){
		super(tag);
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
}