package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class CheckedTextView extends Element{

	private static String tag = "CheckedTextView";
	
	/***********************************************************
	 * public TextView(String id, String width, String height, String text, String style)
	 * 
	 * @param id
	 *            The id of the component
	 * @param widht
	 *            The desired width (wrap_content, match_parent or size specific dp)
	 * @param height
	 *            The desired height (wrap_content, match_parent or size specific dp)
	 * @param text
	 *            The reference to the XML containing the data
	 * @param style
	 *            The reference to the XML containing the style
	 ***********************************************************/
	public CheckedTextView(String id, String width, String height, String text, String style, boolean allowInput){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(text != null)
			super.setAttribute(new Text(text));
		if(style != null)
			super.setAttribute(new Style(style, ""));
		
		super.setAttribute(new Clickable(allowInput));
	}
}
