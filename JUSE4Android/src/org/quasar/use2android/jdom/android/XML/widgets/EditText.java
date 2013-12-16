package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class EditText extends Element{

	private static String tag = "EditText";
	
	public EditText(String id, String width, String height, String inputType){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(inputType != null)
			super.setAttribute(new InputType(inputType));
	}
	
}
