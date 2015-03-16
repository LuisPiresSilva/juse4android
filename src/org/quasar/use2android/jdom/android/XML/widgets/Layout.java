package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;

public class Layout extends Attribute {

	public Layout(String layout){
		super("layout", "@layout/" + layout);
	}
}
