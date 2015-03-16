package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class Strings extends Element{

	private static String tag = "string";
	
	public Strings(String name, String value){
		super(tag);
		if(name != null)
			super.setAttribute(new Name(name));
		if(value != null)
			super.addContent(value);
	}
	
}
