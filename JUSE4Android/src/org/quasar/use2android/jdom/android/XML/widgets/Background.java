package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Background extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	/***********************************************************
	 * public Background(String folder, String background)
	 * 
	 * 
	 * @param Folder
	 *           	To specify the folder holding the file
	 * @param Background
	 *           	To specify the file
	 ***********************************************************/
	public Background(String folder, String background){
		super("background", "@" + folder + "/" + background);
		super.setNamespace(namespace);
	}
	
}