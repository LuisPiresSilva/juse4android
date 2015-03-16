package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class Spinner extends Element{
	private Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	private static String tag = "Spinner";
	
	public Spinner(String id, String width, String height, String entries, String prompt){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(entries != null)
			super.setAttribute("entries", "@array/" + entries, namespace);
		if(prompt != null)
			super.setAttribute("prompt", "@string/" + prompt, namespace);
	}
	
	
}