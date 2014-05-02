package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class Item extends Element{

	private static String tag = "item";
	
	public Item(String name, String value){
		super(tag);
		if(name != null)
			super.setAttribute(new Name(name));
		if(value != null)
			super.addContent(value);
	}
	
}
