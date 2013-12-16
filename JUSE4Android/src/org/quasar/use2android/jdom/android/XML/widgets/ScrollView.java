package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Element;

public class ScrollView extends Element{

	private static String tag = "ScrollView";
	
	public ScrollView(String id, String width, String height,String fillViewport){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(fillViewport != null)
			super.setAttribute(new FillViewPort(true));
		
	}
	
	private StringBuffer output;
	
	public void addAttribute(String att){
		output.append(" " + att);
	}
	
	public String toAndroidXML(){
		return output.toString();
	}

}
