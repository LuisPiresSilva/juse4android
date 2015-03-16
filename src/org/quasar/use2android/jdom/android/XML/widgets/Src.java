package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Src extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	/***********************************************************
	 * public Background(String folder, String background)
	 * 
	 * 
	 * @param Folder
	 *           	To specify the folder holding the file
	 * @param file
	 *           	To specify the file
	 ***********************************************************/
	public Src(String folder, String file){
		super("src", "@" + folder + "/" + file);
		super.setNamespace(namespace);
	}
	
}