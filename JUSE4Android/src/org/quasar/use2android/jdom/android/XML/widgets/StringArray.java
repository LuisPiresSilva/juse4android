package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class StringArray extends Element{

	private static String tag = "string-array";
	
	public StringArray(String name, String value){
		super(tag);
		if(name != null)
			super.setAttribute(new Name(name));
		if(value != null)
			super.addContent(value);
	}
	
}
