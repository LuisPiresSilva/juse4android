package org.quasar.use2android.jdom.android.XML.widgets;

import org.jdom2.Element;

public class ImageView extends Element{

	private static String tag = "ImageView";
	
	/***********************************************************
	 * public ImageView(String width, String height, String folder, String file)
	 * 
	 * 
	 * @param width
	 *           	To specify the width
	 * @param height
	 *           	To specify the height
	 * @param folder
	 *           	To specify the folder holding the file
	 * @param file
	 *           	To specify the file
	 ***********************************************************/
	public ImageView(String width, String height, String folder, String file){
		super(tag);
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(folder != null && file != null)
			super.setAttribute(new Src(folder, file));
	}
	
	/***********************************************************
	 * public ImageView(String id, String width, String height, String folder, String file)
	 * 
	 * @param id
	 *           	To specify the id
	 * @param width
	 *           	To specify the width
	 * @param height
	 *           	To specify the height
	 * @param folder
	 *           	To specify the folder holding the file
	 * @param file
	 *           	To specify the file
	 ***********************************************************/
	public ImageView(String id, String width, String height, String folder, String file){
		super(tag);
		if(id != null)
			super.setAttribute(new Id(id));
		if(width != null)
			super.setAttribute(new Width(width));
		if(height != null)
			super.setAttribute(new Height(height));
		if(folder != null && file != null)
			super.setAttribute(new Src(folder, file));
	}
	
}