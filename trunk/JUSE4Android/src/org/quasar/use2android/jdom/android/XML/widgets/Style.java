package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Style extends Attribute{
	
	public Style(String style, String folder){
		super("style", folder + style);
	}
	
}