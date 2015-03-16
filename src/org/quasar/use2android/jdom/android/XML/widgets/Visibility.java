package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Visibility extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	/***********************************************************
	 * public Visibility(String visibility)
	 * 
	 * 
	 * @param Visibility
	 *           	To specify if the image is visible (visible, invisible, gone)
	 ***********************************************************/
	public Visibility(String visibility){
		super("visibility", visibility);
		super.setNamespace(namespace);
	}
	
}