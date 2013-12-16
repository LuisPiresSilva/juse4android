package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class Include extends Element{

	private static String tag = "include";
	
	public Include(String layout){
		super(tag);
		if(layout != null)
			super.setAttribute(new Layout(layout));
	}
}
	