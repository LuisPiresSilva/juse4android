package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Namespace;

public class Below extends Attribute{

	private Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	
	public Below(Attribute below){
		super("layout_below", below.getValue());
		super.setNamespace(namespace);
	}
	
}