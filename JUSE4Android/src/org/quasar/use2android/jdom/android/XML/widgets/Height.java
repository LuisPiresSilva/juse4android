package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Height extends Attribute{
	
	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	public Height(String height){
		super("layout_height", height);
		super.setNamespace(namespace);
	}
	
}