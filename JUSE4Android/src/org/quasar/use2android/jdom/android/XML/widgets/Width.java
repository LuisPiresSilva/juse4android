package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Width extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	public Width(String width){
		super("layout_width", width);
		super.setNamespace(namespace);
		
	}

}