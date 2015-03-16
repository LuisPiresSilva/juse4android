package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Gravity extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	/***********************************************************
	 * @param Gravity
	 *           	It can be set to center_horizontal, center or center_vertical
	 ***********************************************************/
	public Gravity(String gravity){
		super("gravity", gravity);
		super.setNamespace(namespace);
	}
	
}