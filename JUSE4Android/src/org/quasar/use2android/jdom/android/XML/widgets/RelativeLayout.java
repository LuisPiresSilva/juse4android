package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Element;

public class RelativeLayout extends Element{

	private static String tag = "RelativeLayout";
	
	public RelativeLayout(String width, String height){
		super(tag);
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
	}
	
	public RelativeLayout(String id, String width, String height){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
	}
	
}
