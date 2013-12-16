package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class LinearLayout extends Element{

	private static String tag = "LinearLayout";
	
	public LinearLayout(String width, String height, String orientation){
		super(tag);
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));		
		if(orientation != null)
			super.setAttribute(new Orientation(orientation));
	}
	
	public LinearLayout(String id, String width, String height, String orientation){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));		
		if(orientation != null)
			super.setAttribute(new Orientation(orientation));
	}
	
}
