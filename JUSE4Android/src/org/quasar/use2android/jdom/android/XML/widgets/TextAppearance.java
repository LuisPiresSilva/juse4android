package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class TextAppearance extends Attribute{

	Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	public TextAppearance(String textappearance){
		super("textAppearance", textappearance);
		super.setNamespace(namespace);
	}

}