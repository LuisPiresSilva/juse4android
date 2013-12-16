package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Clickable extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	public Clickable(boolean isClickable){
		super("clickable", "" + isClickable);
		super.setNamespace(namespace);
	}

}