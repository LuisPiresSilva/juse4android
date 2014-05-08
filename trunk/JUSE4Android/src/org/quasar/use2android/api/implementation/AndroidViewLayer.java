package org.quasar.use2android.api.implementation;

import java.io.FileOutputStream; 
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.AssociationKind;
import org.quasar.juse.api.implementation.FileUtilities;
import org.quasar.juse.api.implementation.ModelUtilities;
import org.quasar.use2android.jdom.android.XML.widgets.Background;
import org.quasar.use2android.jdom.android.XML.widgets.Below;
import org.quasar.use2android.jdom.android.XML.widgets.CheckBox;
import org.quasar.use2android.jdom.android.XML.widgets.Clickable;
import org.quasar.use2android.jdom.android.XML.widgets.DatePicker;
import org.quasar.use2android.jdom.android.XML.widgets.EditText;
import org.quasar.use2android.jdom.android.XML.widgets.Gravity;
import org.quasar.use2android.jdom.android.XML.widgets.Height;
import org.quasar.use2android.jdom.android.XML.widgets.Id;
import org.quasar.use2android.jdom.android.XML.widgets.ImageView;
import org.quasar.use2android.jdom.android.XML.widgets.Include;
import org.quasar.use2android.jdom.android.XML.widgets.Item;
import org.quasar.use2android.jdom.android.XML.widgets.LinearLayout;
import org.quasar.use2android.jdom.android.XML.widgets.LongClickable;
import org.quasar.use2android.jdom.android.XML.widgets.RelativeLayout;
import org.quasar.use2android.jdom.android.XML.widgets.Resources;
import org.quasar.use2android.jdom.android.XML.widgets.ScrollView;
import org.quasar.use2android.jdom.android.XML.widgets.Spinner;
import org.quasar.use2android.jdom.android.XML.widgets.StringArray;
import org.quasar.use2android.jdom.android.XML.widgets.Strings;
import org.quasar.use2android.jdom.android.XML.widgets.Style;
import org.quasar.use2android.jdom.android.XML.widgets.Text;
import org.quasar.use2android.jdom.android.XML.widgets.TextView;
import org.quasar.use2android.jdom.android.XML.widgets.Width;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.type.EnumType;

public class AndroidViewLayer extends ViewVisitor{

	private String anim = "anim";
	private String drawable = "drawable";
	private String layout = "layout";
	private String menu = "menu";
	private String values = "values";
	private String separator = "-";

	private String small = "small";
	private String normal = "normal";
	private String large = "large";
	private String xlarge = "xlarge";
	private String land = "land";
	private String port = "port";
	private String hdpi = "hdpi";
	private String ldpi = "ldpi";
	private String mdpi = "mdpi";
	private String xhdpi = "xhdpi";

	private String drawable_hdpi = drawable + separator + hdpi;
	private String drawable_ldpi = drawable + separator + ldpi;
	private String drawable_mdpi = drawable + separator + mdpi;
	private String drawable_xhdpi = drawable + separator + xhdpi;

	private String small_port = values + separator + small + separator + port;
	private String small_land = values + separator + small + separator + land;
	private String normal_port = values + separator + normal + separator + port;
	private String normal_land = values + separator + normal + separator + land;
	private String large_port = values + separator + large + separator + port;
	private String large_land = values + separator + large + separator + land;
	private String xlarge_port = values + separator + xlarge + separator + port;
	private String xlarge_land = values + separator + xlarge + separator + land;

	public AndroidViewLayer(String targetPackage, String ProjectName, MModel mModel, String author, String sourcePath, String targetWorkspace, String presentationLayerName, ModelToXMLUtilities statistics) {
		this.targetPackage = targetPackage;
		this.targetWorkspace = targetWorkspace;
		this.presentationLayerName = presentationLayerName;
		this.rootDirectory = targetWorkspace + "/" + ProjectName + "/res/";
		this.mModel = mModel;
		this.author = author;
		this.statistics = statistics;
		this.ProjectName = ProjectName;
		this.sourcePath = sourcePath;
		
		this.format = Format.getPrettyFormat();
		format.setIndent("    ");
	}

	private String ProjectName;
	private ModelToXMLUtilities statistics;
	private String targetPackage;
	private MModel mModel;
	private String author;
	private String targetWorkspace;
	private String presentationLayerName;
	private String rootDirectory;
	private String sourcePath;
	private Namespace namespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
	private Format format;
	
	/***********************************************************
	* @param theClass whose root we want
	* @param the Attribute whose root we want
	* @return the root parent of the class and attribute passed as parameters
	***********************************************************/
	private MClass attributeBaseAncestor(MClass theClass, MAttribute att)
	{
		return (!theClass.parents().isEmpty() && !theClass.attributes().contains(att)) ? attributeBaseAncestor(theClass.parents().iterator().next(), att) : theClass;
	}
	
	/***********************************************************
	* @param theClass to check
	* @return true if is subclass, false if not
	***********************************************************/
	private boolean isSubClass(MClass theClass)
	{
		for(MClass x : mModel.classes())
			if(x != theClass && theClass.isSubClassOf(x))
				return true;
		return false;
	}
	
	/***********************************************************
	* @param theClass to check
	* @return true if is super class, false if not
	***********************************************************/
	private boolean isSuperClass(MClass theClass)
	{
		for(MClass x : mModel.classes())
			if( (!theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass))//middle super
				|| (theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass)) )//top super
				return true;
		return false;
	}
	
	/***********************************************************
	* @param supers or array of classes to check
	* @return list of all subclasses
	***********************************************************/
	private List<MClass> getAllSubClasses(List<MClass> supers){
		List<MClass> subClasses = new ArrayList<MClass>();
		for(MClass x : supers)
			for(MClass y : mModel.classes())
				if(!subClasses.contains(y) && x != y && y.isSubClassOf(x))
					subClasses.add(y);
		
		return subClasses;
	}
	
	/***********************************************************
	* @param supers or array of classes to check
	* @return list of direct subclasses
	***********************************************************/
	private List<MClass> getAllDirectSubClasses(List<MClass> supers){
		List<MClass> directSubClasses = new ArrayList<MClass>();
		for(MClass x : getAllSubClasses(supers))
			if(supers.contains(x.parents().iterator().next()))
				directSubClasses.add(x);
		
		return directSubClasses;
	}
	
	
	public void generateFolders() {

		// Standard folders
		FileUtilities.createDirectory(rootDirectory + anim);
		FileUtilities.createDirectory(rootDirectory + drawable);
		FileUtilities.createDirectory(rootDirectory + layout);
		FileUtilities.createDirectory(rootDirectory + menu);
		FileUtilities.createDirectory(rootDirectory + values);

		// Resolution support folders
		FileUtilities.createDirectory(rootDirectory + drawable_hdpi);
		FileUtilities.createDirectory(rootDirectory + drawable_ldpi);
		FileUtilities.createDirectory(rootDirectory + drawable_mdpi);
		FileUtilities.createDirectory(rootDirectory + drawable_xhdpi);

		// Size and Orientation support folders
		FileUtilities.createDirectory(rootDirectory + small_land);
		FileUtilities.createDirectory(rootDirectory + small_port);
		FileUtilities.createDirectory(rootDirectory + normal_land);
		FileUtilities.createDirectory(rootDirectory + normal_port);
		FileUtilities.createDirectory(rootDirectory + large_land);
		FileUtilities.createDirectory(rootDirectory + large_port);
		FileUtilities.createDirectory(rootDirectory + xlarge_land);
		FileUtilities.createDirectory(rootDirectory + xlarge_port);
	}

	public void generateXMLs() {
		generateManifest();
		
		generateOnePaneLayout();
		generateTwoPaneLayout();
		
		generateMultiPaneRef(normal_port, "onepane");
		generateMultiPaneRef(normal_land, "onepane");
		generateMultiPaneRef(large_port, "twopane");
		generateMultiPaneRef(large_land, "twopane");
		generateMultiPaneRef(xlarge_port, "twopane");
		generateMultiPaneRef(xlarge_land, "twopane");
		
		generateDefaultComponentStyles(normal_port);
		generateDefaultComponentStyles(normal_land);
		generateDefaultComponentStyles(large_port);
		generateDefaultComponentStyles(large_land);
		generateDefaultComponentStyles(xlarge_port);
		generateDefaultComponentStyles(xlarge_land);
		
		generateTypeComponentStyles(normal_port);
		generateTypeComponentStyles(normal_land);
		generateTypeComponentStyles(large_port);
		generateTypeComponentStyles(large_land);
		generateTypeComponentStyles(xlarge_port);
		generateTypeComponentStyles(xlarge_land);
		
		generateRawStrings();
		generateDefaultXML();
		generateDefaultMedia(hdpi);
		generateDefaultMedia(mdpi);
		generateDefaultMedia(xhdpi);
		
		
		generateDetailForm();
		generateInsertUpdateForm();
		generateNaviagtionBarLists();
		generateListForm();
		
		generateDetailViews();
		generateInsertUpdateViews();
		generateNavigationViews();
		generateListView();
		
		
		generateMenuViews();
		
		generateAssociationsToGeneralizationOptions();
		generateAssociationToGeneralizationViews();//XML that shows what super and sub classes can be created or associated with a given class
	}

	public void generateManifest(){
		String targetDirectory = targetWorkspace + "/" + ProjectName + "/";
		String XMLName = "AndroidManifest";
		String classId;
		
		if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
			Element rootView = new Element("manifest");
			rootView.addNamespaceDeclaration(namespace);
			rootView.setAttribute("package", targetPackage);
			rootView.setAttribute("versionCode","1",namespace);
			rootView.setAttribute("versionName","1.0",namespace);
			
			Element version = new Element("uses-sdk");
			version.setAttribute("minSdkVersion","14",namespace);
			version.setAttribute("targetSdkVersion","17",namespace);
			
			Element internetPermission = new Element("uses-permission");
			internetPermission.setAttribute("name","android.permission.INTERNET",namespace);

			
			Element application = new Element("application");
			application.setAttribute("allowBackup","true",namespace);
			application.setAttribute("icon","@drawable/ic_launcher",namespace);
			application.setAttribute("label","@string/app_name",namespace);
			application.setAttribute("theme","@style/AppTheme",namespace);
			application.setAttribute("name",mModel.name() + "Memory",namespace);
			
			Element launcher_activity = new Element("activity");
			launcher_activity.setAttribute("name","" + mModel.name() + "Launcher",namespace);
			launcher_activity.setAttribute("label","@string/app_name",namespace);
			Element launcher_intent_filter = new Element("intent-filter");
			Element launcher_action = new Element("action");
			launcher_action.setAttribute("name","android.intent.action.MAIN",namespace);
			Element launcher_category = new Element("category");
			launcher_category.setAttribute("name","android.intent.category.LAUNCHER",namespace);
			
			launcher_activity.addContent(launcher_intent_filter);
			launcher_intent_filter.addContent(launcher_action);
			launcher_intent_filter.addContent(launcher_category);
			
			application.addContent(launcher_activity);
			
			for (MClass cls : mModel.classes()){
				if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
					classId = cls.name().toLowerCase();
					Element activity = new Element("activity");
					activity.setAttribute("configChanges","orientation",namespace);
					activity.setAttribute("name", targetPackage + "." + presentationLayerName + "." + cls.name() + "." + cls.name() + "Activity",namespace);
					activity.setAttribute("label","@string/tittle_master_" + classId,namespace);
					
					Element meta_data = new Element("meta-data");
					meta_data.setAttribute("name","android.support.PARENT_ACTIVITY",namespace);
					meta_data.setAttribute("value","." + mModel.name() + "Launcher",namespace);
					
					activity.addContent(meta_data);
					
					application.addContent(activity);
				}
			}
			
			rootView.addContent(version);
			rootView.addContent(internetPermission);
			rootView.addContent(application);
			
			XMLOutputter outputter = new XMLOutputter();
			try {
				outputter.setFormat(format);
				outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
				statistics.addOneToGenerated();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void generateOnePaneLayout(){
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_layout_onepane";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new RelativeLayout("match_parent","match_parent");
					rootView.setAttribute(new Id(classId + "_layout_onepane"));
					
					Element navigation_fragment = new Element("FrameLayout");
					navigation_fragment.setAttribute(new Id(classId + "_navigationbar_container"));
					navigation_fragment.setAttribute(new Style("default_onepane_navigationbar_container", "@style/"));
					
					Element list_fragment = new Element("FrameLayout");
					list_fragment.setAttribute(new Id(classId + "_list_container"));
					list_fragment.setAttribute(new Style("default_onepane_list_container", "@style/"));
					list_fragment.setAttribute(new Below(navigation_fragment.getAttribute("id",namespace)));
					
					Element detail_fragment = new Element("FrameLayout");
					detail_fragment.setAttribute(new Id(classId + "_detail_container"));
					detail_fragment.setAttribute(new Style("default_onepane_detail_container", "@style/"));
					detail_fragment.setAttribute(new Below(navigation_fragment.getAttribute("id",namespace)));

					rootView.addContent(navigation_fragment);
					rootView.addContent(list_fragment);
					rootView.addContent(detail_fragment);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void generateTwoPaneLayout(){
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_layout_twopane";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new RelativeLayout("match_parent","match_parent");
					
					Element navigation_fragment = new Element("FrameLayout");
					navigation_fragment.setAttribute(new Id(classId + "_navigationbar_container"));
					navigation_fragment.setAttribute(new Style("default_twopane_navigationbar_container", "@style/"));
					
					Element linear_layout = new Element("LinearLayout");
					linear_layout.setAttribute(new Id(classId + "_twopane_layout_container"));
					linear_layout.setAttribute(new Style("default_twopane_layout_container", "@style/"));
					linear_layout.setAttribute(new Below(navigation_fragment.getAttribute("id",namespace)));
					
					Element list_fragment = new Element("FrameLayout");
					list_fragment.setAttribute(new Id(classId + "_list_container"));
					list_fragment.setAttribute(new Style("default_twopane_list_container", "@style/"));
					
					Element detail_fragment = new Element("FrameLayout");
					detail_fragment.setAttribute(new Id(classId + "_detail_container"));
					detail_fragment.setAttribute(new Style("default_twopane_detail_container", "@style/"));

					linear_layout.addContent(list_fragment);
					linear_layout.addContent(detail_fragment);
					
					rootView.addContent(navigation_fragment);
					rootView.addContent(linear_layout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void generateMultiPaneRef(String typeScreen, String typePane){
		String targetDirectory = rootDirectory + typeScreen + "/";
		String XMLName;
		String classId;
		XMLName = "PaneDecider";
		if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
			Element rootView = new Resources();
			Element bool = new Element("bool");
			bool.setAttribute("name", "has_two_panes");
			if(typePane.equals("onepane"))
				bool.setText("false");
			else if(typePane.equals("twopane"))
				bool.setText("true");
			
			rootView.addContent(bool);
//				for (MClass cls : mModel.classes()){
//					if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
//						classId = cls.name().toLowerCase();
//						
//						Element item = new Element("item");
//						item.setAttribute("name", classId + "_layout_onepane");
//						item.setAttribute("type", "layout");
//						item.setText("@layout/" + classId + "_layout_" + typePane);
//						
//						
//						rootView.addContent(item);
//					}
//				}
//			}
						
			XMLOutputter outputter = new XMLOutputter();
			try {
				outputter.setFormat(format);
				outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
				statistics.addOneToGenerated();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void generateDefaultComponentStyles(String typeScreen) {
		String targetDirectory = rootDirectory + typeScreen + "/";
		String XMLName;
		XMLName = "default_component_styles";
		if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
			Element rootView = new Resources();
			
			//TextView - start
			Element textViewstyle = new Element("style");
			textViewstyle.setAttribute("name", "default_textview_style");
			if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
				textViewstyle.setAttribute("parent", "@android:style/TextAppearance.Medium");
			else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
				textViewstyle.setAttribute("parent", "@android:style/TextAppearance.Large");
			else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
				textViewstyle.setAttribute("parent", "@android:style/TextAppearance.Large");
			
			rootView.addContent(textViewstyle);
			//TextView - end
			
			//EditText - start
			Element edittextstyle = new Element("style");
			edittextstyle.setAttribute("name", "default_edittext_style");
			if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
				edittextstyle.setAttribute("parent", "@android:style/TextAppearance.Medium");
			else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
				edittextstyle.setAttribute("parent", "@android:style/TextAppearance.Large");
			else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
				edittextstyle.setAttribute("parent", "@android:style/TextAppearance.Large");
			
			rootView.addContent(edittextstyle);
			//EditText - end
			
			//Datepicker - start
			Element datepicker = new Element("style");
			datepicker.setAttribute("name", "default_datepicker_style");
			
			Element calendarshown = new Element("item");
			calendarshown.setAttribute("name", "android:calendarViewShown");
			if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
				calendarshown.addContent("false");
			else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
				calendarshown.addContent("true");
			else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
				calendarshown.addContent("true");
			datepicker.addContent(calendarshown);
			rootView.addContent(datepicker);
			//Datepicker - end
			
			//Spinner - start
			Element spinnerstyle = new Element("style");
			spinnerstyle.setAttribute("name", "default_spinner_style");
			rootView.addContent(spinnerstyle);
			//Spinner - end
			
			//CheckBox - start
			Element CheckBoxstyle = new Element("style");
			CheckBoxstyle.setAttribute("name", "default_CheckBox_style");
			rootView.addContent(CheckBoxstyle);
			//CheckBox - end
			XMLOutputter outputter = new XMLOutputter();
			try {
				outputter.setFormat(format);
				outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
				statistics.addOneToGenerated();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void generateTypeComponentStyles(String typeScreen) {
		String targetDirectory = rootDirectory + typeScreen + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				XMLName = cls.name().toLowerCase() + "_component_styles";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new Resources();
					classId = cls.name().toLowerCase();
					
					//detail - start
					List<MAttribute> finalReadAttributeList;
					if(cls.getAnnotation("display") != null)
						finalReadAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.attributes(), cls.getAnnotation("display").getValues());
					else
						finalReadAttributeList = cls.attributes();
					
					for(MAttribute att : finalReadAttributeList){
						Element style_descriptor = new Element("style");
						Element style_read = new Element("style");
						
						
						style_descriptor.setAttribute("name", classId + "_detail_" + att.name().toLowerCase() + "_descriptor_style");
						style_descriptor.setAttribute("parent", "@style/default_textview_style");
						style_read.setAttribute("name", classId + "_detail_" + att.name().toLowerCase() + "_value_style");
						
						if(att.type().isBoolean())
							style_read.setAttribute("parent", "@style/default_CheckBox_style");
						
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date"))){
							style_read.setAttribute("parent", "@style/default_datepicker_style");						
						}

						if(att.type().isEnum()){
							style_read.setAttribute("parent", "@style/default_textview_style");			
						}
						if(att.type().isInteger() || att.type().isNumber() || att.type().isString()){
							style_read.setAttribute("parent", "@style/default_textview_style");
						}
						
						if(style_read.getAttributesSize() > 0 && style_read.getAttributesSize() > 0){
							rootView.addContent(style_descriptor);
							rootView.addContent(style_read);
						}
					}
					//detail - end
					
					//insertupdate - start
					List<MAttribute> finalWriteAttributeList;
					if(cls.getAnnotation("creation") != null)
						finalWriteAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.attributes(), cls.getAnnotation("creation").getValues());
					else
						finalWriteAttributeList = cls.attributes();
					
					for(MAttribute att : finalWriteAttributeList){
						Element style_descriptor = new Element("style");
						Element style_write = new Element("style");
						
						style_descriptor.setAttribute("name", classId + "_insertupdate_" + att.name().toLowerCase() + "_descriptor_style");
						style_descriptor.setAttribute("parent", "@style/default_textview_style");
						style_write.setAttribute("name", classId + "_insertupdate_" + att.name().toLowerCase() + "_value_style");
						
						if(att.type().isBoolean())
							style_write.setAttribute("parent", "@style/default_CheckBox_style");
						
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
							style_write.setAttribute("parent", "@style/default_datepicker_style");						

						if(att.type().isEnum())
							style_write.setAttribute("parent", "default_spinner_style");			
						
						if(att.type().isInteger() || att.type().isNumber() || att.type().isString())
							style_write.setAttribute("parent", "@style/default_edittext_style");	
						
						if(style_write.getAttributesSize() > 0 && style_write.getAttributesSize() > 0){
							rootView.addContent(style_descriptor);
							rootView.addContent(style_write);
						}
					}
					//insertupdate - end
					
					//list - start
					List<MAttribute> finalListAttributeList;
					if(cls.getAnnotation("list") != null)
						finalListAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.attributes(), cls.getAnnotation("list").getValues());
					else
						finalListAttributeList = cls.attributes();
					
					for(MAttribute att : finalListAttributeList){
						Element style_list = new Element("style");
						
						style_list.setAttribute("name", classId + "_list_" + att.name().toLowerCase() + "_value_style");
						
						if(att.type().isBoolean())
							style_list.setAttribute("parent", "@style/default_CheckBox_style");
						
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
							style_list.setAttribute("parent", "@style/default_textview_style");						
						
						if(att.type().isEnum())
							style_list.setAttribute("parent", "@style/default_textview_style");			
						
						if(att.type().isInteger() || att.type().isNumber() || att.type().isString())
							style_list.setAttribute("parent", "@style/default_textview_style");	
						
						
						if(style_list.getAttributesSize() > 0)
							rootView.addContent(style_list);
					}
					//list - end
					
					//navigation bar - start
//					List<MClass> alreadyAdded = new ArrayList<MClass>();
//					Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
//					for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(cls)){
//						if(!alreadyAdded.contains(association.getTargetAE().cls())){
//							RepeteadNeighbors.put(association.getTargetAE().cls(), 1);
//							alreadyAdded.add(association.getTargetAE().cls());
//						}
//						else
//							RepeteadNeighbors.put(association.getTargetAE().cls(), RepeteadNeighbors.get(association.getTargetAE().cls()) + 1);
//					}
					
//					alreadyAdded.clear();
					for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(cls)){
//						if(!alreadyAdded.contains(association.getTargetAE().cls())){
							Element style_descriptor = new Element("style");
							Element style_numberObjects = new Element("style");
							
							if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE){
								style_descriptor.setAttribute("name", classId + "_navigationbar_association_" + association.getTargetAEClass().nameAsRolename().toLowerCase() + "_descriptor_style");
								style_numberObjects.setAttribute("name", classId + "_navigationbar_association_" + association.getTargetAEClass().nameAsRolename().toLowerCase() + "_numberobjects_style");
							}else{
								style_descriptor.setAttribute("name", classId + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + "_descriptor_style");
								style_numberObjects.setAttribute("name", classId + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + "_numberobjects_style");
							}
							style_descriptor.setAttribute("parent", "@style/default_textview_style");
							if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Small");
							else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Medium");
							else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Large");
							else
								style_numberObjects.setAttribute("parent", "@style/default_textview_style");
							
							if(style_descriptor.getAttributesSize() > 0 && style_numberObjects.getAttributesSize() > 0){
								rootView.addContent(style_descriptor);
								rootView.addContent(style_numberObjects);
							}
							
//							alreadyAdded.add(association.getTargetAE().cls());
//						}
					}
					if(isSuperClass(cls)){//navegacao sub -> super (ToONE)
						for(MClass x : cls.children()){
//							if(x.parents().iterator().next() == cls && !alreadyAdded.contains(cls)){//if is direct super
								Element style_descriptor = new Element("style");
								Element style_numberObjects = new Element("style");
								
								style_descriptor.setAttribute("name", classId + "_navigationbar_association_" + x.name().toLowerCase() + "_descriptor_style");
								style_descriptor.setAttribute("parent", "@style/default_textview_style");
								style_numberObjects.setAttribute("name", classId + "_navigationbar_association_" + x.name().toLowerCase() + "_numberobjects_style");
								if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
									style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Small");
								else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
									style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Medium");
								else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
									style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Large");
								else
									style_numberObjects.setAttribute("parent", "@style/default_textview_style");
								
								if(style_descriptor.getAttributesSize() > 0 && style_numberObjects.getAttributesSize() > 0){
									rootView.addContent(style_descriptor);
									rootView.addContent(style_numberObjects);
								}
//								alreadyAdded.add(cls);
//							}
						}
					}
					if(isSubClass(cls)){//navegacao super -> sub (ToMany)
//						if(!alreadyAdded.contains(cls.parents().iterator().next())){
							Element style_descriptor = new Element("style");
							Element style_numberObjects = new Element("style");
							
							style_descriptor.setAttribute("name", classId + "_navigationbar_association_" + cls.parents().iterator().next().name().toLowerCase() + "_descriptor_style");
							style_descriptor.setAttribute("parent", "@style/default_textview_style");
							style_numberObjects.setAttribute("name", classId + "_navigationbar_association_" + cls.parents().iterator().next().name().toLowerCase() + "_numberobjects_style");
							if(typeScreen.equals(normal_port) || typeScreen.equals(normal_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Small");
							else if(typeScreen.equals(large_port) || typeScreen.equals(large_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Medium");
							else if(typeScreen.equals(xlarge_port) || typeScreen.equals(xlarge_land))
								style_numberObjects.setAttribute("parent", "@android:style/TextAppearance.Large");
							else
								style_numberObjects.setAttribute("parent", "@style/default_textview_style");
							
							if(style_descriptor.getAttributesSize() > 0 && style_numberObjects.getAttributesSize() > 0){
								rootView.addContent(style_descriptor);
								rootView.addContent(style_numberObjects);
							}
//						}
					}
					//navigation bar - end
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}					
		}
	}
	
//	AKI -------------------------------------------------------------------------------------------------------------------------------------
//	VER SE ACRESCENTO - INDA NAO SEI
	public void generateRawStrings(){
//		strings estaticas para cada tipo - titulos na navigation bar, titulos descritores para detail e insert views
		String targetDirectory = rootDirectory + values + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){			
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_strings";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){				
					Element rootView = new Resources();
					
					for (MAttribute att : cls.attributes()){
						String attributeName = att.name().toLowerCase();
						Element strings_detail = new Strings(classId + "_detail_" + attributeName + "_descriptor", att.name() + ": ");
						Element strings_insert_update = new Strings(classId + "_insertupdate_" + attributeName + "_descriptor", "Choose a " + att.name() + ": ");
						rootView.addContent(strings_detail);
						rootView.addContent(strings_insert_update);
					}

					for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(cls)){
						String associationName = ass.getTargetAE().name().toLowerCase();
						if(ass.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
							associationName = ass.getTargetAEClass().nameAsRolename().toLowerCase();
						Element strings_associations = new Strings(classId + "_associationto_" + associationName, associationName);
						rootView.addContent(strings_associations);
					}
					
//					super to sub classes
					for(MClass child : cls.children()){
						String superName = classId;
						String childName = child.name().toLowerCase();
						Element super_associations = new Strings(superName + "_tosub_" + childName, child.name());
						rootView.addContent(super_associations);	
					}
//					sub to super classes
					for(MClass parent : cls.parents()){
						String childName = classId;
						String superName = parent.name().toLowerCase();
						Element child_associations = new Strings(childName + "_tosuper_" + superName, parent.name());
						rootView.addContent(child_associations);
					}
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		//volto a repetir ciclo desnecessariamente depois melhora-se quando se decidir melhor como sera esta geracao
		
		//enums - generates the arrays in enums.xml
		if(!mModel.enumTypes().isEmpty()){
			XMLName = "enums";
			if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
				Element rootView = new Resources();
				for (EnumType cls : mModel.enumTypes()){
					Element string_array = new StringArray(cls.name().toLowerCase(), null);
					for(String value : cls.getLiterals()){
						Element item = new Item(null,value);
						string_array.addContent(item);
					}
					rootView.addContent(string_array);
				}
				XMLOutputter outputter = new XMLOutputter();
				try {
					outputter.setFormat(format);
					outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//titles - generates the strings.xml which holds the titles of the app and all the activities
		XMLName = "strings";
		if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
			Element rootView = new Resources();
			Element appName = new Strings("app_name", mModel.name());
			rootView.addContent(appName);
			for (MClass cls : mModel.classes()){
				if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
					classId = cls.name().toLowerCase();	
					
					Element master_view_tittle = new Strings("tittle_master_" + classId , cls.name());
					Element detail_view_tittle = new Strings("tittle_detail_" + classId , cls.name() + " Details");
					rootView.addContent(master_view_tittle);
					rootView.addContent(detail_view_tittle);
				}
			}
			XMLOutputter outputter = new XMLOutputter();
			try {
				outputter.setFormat(format);
				outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void generateDefaultXML(){
		String targetDirectory = rootDirectory + layout + "/";
		String sourceDirectory = sourcePath + "/res/use2android/defaultdata/XML/" + layout + "/";
		
		FileUtilities.copyFile(sourceDirectory + "default_blank_fragment.xml",targetDirectory + "default_blank_fragment.xml");
		FileUtilities.copyFile(sourceDirectory + "default_navigationbar.xml",targetDirectory + "default_navigationbar.xml");
		FileUtilities.copyFile(sourceDirectory + "default_okcancel_buttons.xml",targetDirectory + "default_okcancel_buttons.xml");
		FileUtilities.copyFile(sourceDirectory + "default_warning_fragment.xml",targetDirectory + "default_warning_fragment.xml");
		FileUtilities.copyFile(sourceDirectory + "launcher_activity.xml",targetDirectory + mModel.name().toLowerCase() + "_launcher_activity.xml");
		FileUtilities.copyFile(sourceDirectory + "launcher_gridview_row.xml",targetDirectory + mModel.name().toLowerCase() + "_launcher_gridview_row.xml");
		FileUtilities.replaceStringInFile(targetDirectory + mModel.name().toLowerCase() + "_launcher_gridview_row.xml", "@drawable/launcher_gridview_selector", "@drawable/" + mModel.name().toLowerCase() + "_launcher_gridview_selector");
		
		targetDirectory = rootDirectory + drawable + "/";
		sourceDirectory = sourcePath + "/res/use2android/defaultdata/XML/" + drawable + "/";
		
		FileUtilities.copyFile(sourceDirectory + "default_list_selector.xml",targetDirectory + "default_list_selector.xml");
		FileUtilities.copyFile(sourceDirectory + "actionbar_compat_item_focused.xml",targetDirectory + "actionbar_compat_item_focused.xml");
		FileUtilities.copyFile(sourceDirectory + "actionbar_compat_item_pressed.xml",targetDirectory + "actionbar_compat_item_pressed.xml");
		FileUtilities.copyFile(sourceDirectory + "actionbar_compat_item.xml",targetDirectory + "actionbar_compat_item.xml");
		FileUtilities.copyFile(sourceDirectory + "launcher_gridview_round_borders.xml",targetDirectory + mModel.name().toLowerCase() + "_launcher_gridview_round_borders.xml");
		FileUtilities.copyFile(sourceDirectory + "launcher_gridview_selector.xml",targetDirectory + mModel.name().toLowerCase() + "_launcher_gridview_selector.xml");
		FileUtilities.replaceStringInFile(targetDirectory + mModel.name().toLowerCase() + "_launcher_gridview_selector.xml", "@drawable/launcher_gridview_round_borders", "@drawable/" + mModel.name().toLowerCase() + "_launcher_gridview_round_borders");
		
		FileUtilities.copyFile(sourceDirectory + "navigationbar_original_state.xml",targetDirectory + "navigationbar_original_state.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_association_new_object_state.xml",targetDirectory + "navigationbar_association_new_object_state.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_divider.xml",targetDirectory + "navigationbar_divider.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_new_object_state.xml",targetDirectory + "navigationbar_new_object_state.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_selector.xml",targetDirectory + "navigationbar_selector.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_error_state.xml",targetDirectory + "navigationbar_error_state.xml");
		FileUtilities.copyFile(sourceDirectory + "navigationbar_selector_error.xml",targetDirectory + "navigationbar_selector_error.xml");
		
		targetDirectory = rootDirectory + values + "/";
		sourceDirectory = sourcePath + "/res/use2android/defaultdata/XML/" + values + "/";
		
		FileUtilities.copyFile(sourceDirectory + "colors.xml",targetDirectory + "colors.xml");
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		
		targetDirectory = rootDirectory + normal_port + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		targetDirectory = rootDirectory + normal_land + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		targetDirectory = rootDirectory + large_port + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		targetDirectory = rootDirectory + large_land + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		targetDirectory = rootDirectory + xlarge_port + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
		targetDirectory = rootDirectory + xlarge_land + "/";
		FileUtilities.copyFile(sourceDirectory + "default_layout_styles.xml",targetDirectory + "default_layout_styles.xml");
	
		statistics.addToCopied(23);
	}
	
	public void generateDefaultMedia(String resolution){
		String targetDirectory;
		String sourceDirectory;
		if(resolution != null){
			sourceDirectory = sourcePath + "/res/use2android/defaultdata/Media/" + drawable + "/" + resolution + "/";
			targetDirectory = rootDirectory + drawable + separator + resolution + "/";
		}else{
			sourceDirectory = sourcePath + "/res/use2android/defaultdata/Media/" + drawable + "/";
			targetDirectory = rootDirectory + drawable + "/";
		}
		
		FileUtilities.copyFile(sourceDirectory + "ic_action_search.png", targetDirectory + "ic_action_search.png");
		FileUtilities.copyFile(sourceDirectory + "ic_dark_menu_add_to_queue.png", targetDirectory + "ic_dark_menu_add_to_queue.png");
		FileUtilities.copyFile(sourceDirectory + "ic_dark_menu_cloud.png", targetDirectory + "ic_dark_menu_cloud.png");
		FileUtilities.copyFile(sourceDirectory + "ic_dark_menu_content_discard.png", targetDirectory + "ic_dark_menu_content_discard.png");
		FileUtilities.copyFile(sourceDirectory + "ic_dark_menu_content_edit.png", targetDirectory + "ic_dark_menu_content_edit.png");
		FileUtilities.copyFile(sourceDirectory + "ic_dark_menu_content_new.png", targetDirectory + "ic_dark_menu_content_new.png");
		FileUtilities.copyFile(sourceDirectory + "ic_launcher.png", targetDirectory + "ic_launcher.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_associative_class_list_divider.png", targetDirectory + "ic_light_association_associative_class_list_divider.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_associative_class.png", targetDirectory + "ic_light_association_associative_class.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_inheritance_sub.png", targetDirectory + "ic_light_association_inheritance_sub.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_inheritance_super.png", targetDirectory + "ic_light_association_inheritance_super.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_many.png", targetDirectory + "ic_light_association_many.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_association_one.png", targetDirectory + "ic_light_association_one.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_dialog_error.png", targetDirectory + "ic_light_dialog_error.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_home.png", targetDirectory + "ic_light_home.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_menu_add_to_queue.png", targetDirectory + "ic_light_menu_add_to_queue.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_menu_content_discard.png", targetDirectory + "ic_light_menu_content_discard.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_menu_content_edit.png", targetDirectory + "ic_light_menu_content_edit.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_menu_content_new.png", targetDirectory + "ic_light_menu_content_new.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_navigation_bar_view_left_arrow.png", targetDirectory + "ic_light_navigation_bar_view_left_arrow.png");
		FileUtilities.copyFile(sourceDirectory + "ic_light_navigation_bar_view_right_arrow.png", targetDirectory + "ic_light_navigation_bar_view_right_arrow.png");
		FileUtilities.copyFile(sourceDirectory + "ic_menu_cancel_oncreation.png", targetDirectory + "ic_menu_cancel_oncreation.png");
		FileUtilities.copyFile(sourceDirectory + "ic_menu_confirm_creation.png", targetDirectory + "ic_menu_confirm_creation.png");
		FileUtilities.copyFile(sourceDirectory + "ic_menu_location.png", targetDirectory + "ic_menu_location.png");
		FileUtilities.copyFile(sourceDirectory + "ic_contract_error.png", targetDirectory + "ic_contract_error.png");
		FileUtilities.copyFile(sourceDirectory + "ic_android_mode_read.png", targetDirectory + "ic_android_mode_read.png");
		FileUtilities.copyFile(sourceDirectory + "ic_android_mode_write.png", targetDirectory + "ic_android_mode_write.png");
		
	}
	
	public void generateDetailForm(){
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_form_detail";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new Element("merge");
					rootView.addNamespaceDeclaration(namespace);
					
					Element relativeLayout = new RelativeLayout(classId + "_detail_layout","match_parent","wrap_content");
					
//					--------------*************** CODIGO NOVO - START  ******************* ------------------
					List<MAttribute> finalAttributeList;
					if(cls.getAnnotation("display") != null)
						finalAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.attributes(), cls.getAnnotation("display").getValues());
					else
						finalAttributeList = cls.attributes();
//					--------------*************** CODIGO NOVO - END  ******************* ------------------
					for (MAttribute att : finalAttributeList){
						String attributeName = att.name().toLowerCase();
						
						Element linearLayout = new LinearLayout(classId + "_detail_" + attributeName,"wrap_content","wrap_content","horizontal");
						
						if(relativeLayout.getChildren().size() > 0){
							linearLayout.setAttribute(new Below(relativeLayout.getChildren().get(relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
						}
						Element dataDescriptor = new TextView(classId + "_detail_" + attributeName + "_descriptor", "wrap_content", "wrap_content", classId + "_detail_" + attributeName + "_descriptor" , "@style/" + classId + "_detail_" + attributeName + "_descriptor_style");
						
						Element dataValue = null;
						
						if(att.type().isBoolean())
							dataValue = new CheckBox(classId + "_detail_" + attributeName + "_value", "wrap_content", "wrap_content", "@style/" + classId + "_detail_" + attributeName + "_value_style", false); 	

						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
							dataValue = new DatePicker(classId + "_detail_" + attributeName + "_value", "wrap_content", "wrap_content", "@style/" + classId + "_detail_" + attributeName + "_value_style", false); 	

						if(att.type().isEnum())
							dataValue = new TextView(classId + "_detail_" + attributeName + "_value", "wrap_content", "wrap_content", "@style/" + classId + "_detail_" + attributeName + "_value_style");
						
						if(att.type().isInteger() || att.type().isNumber() || att.type().isString())
							dataValue = new TextView(classId + "_detail_" + attributeName + "_value", "wrap_content", "wrap_content", "@style/" + classId + "_detail_" + attributeName + "_value_style"); 	
						
						
						linearLayout.addContent(dataDescriptor);
						if(dataValue != null)
							linearLayout.addContent(dataValue);
						
						relativeLayout.addContent(linearLayout);
					}
					
					rootView.addContent(relativeLayout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void generateInsertUpdateForm(){
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_form_insertupdate";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new Element("merge");
					rootView.addNamespaceDeclaration(namespace);
					
					Element relativeLayout = new RelativeLayout(classId + "_insertupdate_layout","match_parent","wrap_content");
					
//					--------------*************** CODIGO NOVO - START  ******************* ------------------
					List<MAttribute> finalAttributeList;
					if(cls.getAnnotation("creation") != null)
						finalAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.attributes(), cls.getAnnotation("creation").getValues());
					else
						finalAttributeList = cls.attributes();
//					--------------*************** CODIGO NOVO - END  ******************* ------------------
					
					for (MAttribute att : finalAttributeList){
						String attributeName = att.name().toLowerCase();
						Element linearLayout = new LinearLayout(classId + "_insertupdate_" + attributeName,"match_parent","wrap_content","horizontal");
						
						if(relativeLayout.getChildren().size() > 0){
							linearLayout.setAttribute(new Below(relativeLayout.getChildren().get(relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
						}
						Element dataDescriptor = new TextView(classId + "_insertupdate_" + attributeName + "_descriptor", "wrap_content", "wrap_content", classId + "_insertupdate_" + attributeName + "_descriptor" , "@style/" + classId + "_insertupdate_" + attributeName + "_descriptor_style");
						
						Element dataValue = null;
						
						if(att.type().isBoolean())
							dataValue = new CheckBox(classId + "_insertupdate_" + attributeName + "_value", "match_parent", "wrap_content", "@style/" + classId + "_detail_" + attributeName + "_value_style", true); 
						
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
							dataValue = new DatePicker(classId + "_insertupdate_" + attributeName + "_value", "wrap_content", "wrap_content", "@style/" + classId + "_insertupdate_" + attributeName + "_value_style", true); 	

						if(att.type().isEnum())
							dataValue = new Spinner(classId + "_insertupdate_" + attributeName + "_value", "match_parent", "wrap_content", att.type().shortName().toLowerCase(), classId + "_insertupdate_" + attributeName + "_descriptor"); 
						
						if(att.type().isString())
							dataValue = new EditText(classId + "_insertupdate_" + attributeName + "_value", "match_parent", "wrap_content", null); 	
						if(att.type().isNumber())
							dataValue = new EditText(classId + "_insertupdate_" + attributeName + "_value", "match_parent", "wrap_content", "numberDecimal"); 	
						if(att.type().isInteger())
							dataValue = new EditText(classId + "_insertupdate_" + attributeName + "_value", "match_parent", "wrap_content", "number"); 	
						
						
						linearLayout.addContent(dataDescriptor);
						if(dataValue != null)
							linearLayout.addContent(dataValue);
						
						relativeLayout.addContent(linearLayout);
					}
									
					rootView.addContent(relativeLayout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void generateListForm() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null && !ModelUtilities.isAssociativeClass(cls)){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_form_list";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
					Element rootView = new Element("merge");
					rootView.addNamespaceDeclaration(namespace);
					
					Element relativeLayout = new RelativeLayout("wrap_content","?android:attr/listPreferredItemHeight");
					relativeLayout.setAttribute(new Gravity("center_vertical"));
					relativeLayout.setAttribute(new Background("drawable", "default_list_selector"));
					
					Element form_relativeLayout = new RelativeLayout("wrap_content","wrap_content");
//					--------------*************** CODIGO NOVO - START  ******************* ------------------
					List<MAttribute> finalAttributeList;
					if(cls.getAnnotation("list") != null){
						finalAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues());
					}else
						finalAttributeList = cls.attributes();
//					--------------*************** CODIGO NOVO - END  ******************* ------------------
					for (MAttribute att : finalAttributeList){
							
						String attributeName = att.name().toLowerCase();
											
						Element dataValue = null;
						
						if(att.type().isBoolean()){
							dataValue = new LinearLayout(classId + "_list_" + attributeName,"match_parent","wrap_content","horizontal");
							Element dataDescriptor = new TextView(classId + "_list_" + attributeName + "_descriptor", "wrap_content", "wrap_content", classId + "_list_" + attributeName + "_descriptor" , "@style/" + classId + "_list_" + attributeName + "_descriptor_style");
														
							Element dataval = new CheckBox(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value", "wrap_content", "wrap_content" ,null, false); 	
							dataval.setAttribute(new Style(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value_style", "@style/"));
							dataValue.addContent(dataDescriptor);
							dataValue.addContent(dataval);
						}			
									
						if(att.type().isEnum() || att.type().isInteger() || att.type().isNumber() || att.type().isString() || att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date"))){
							dataValue = new TextView(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value", "wrap_content", "wrap_content", null); 	
							dataValue.setAttribute(new Style(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value_style", "@style/"));
						}
						
						if(form_relativeLayout.getChildren().size() > 0 && dataValue != null){
							dataValue.setAttribute(new Below(form_relativeLayout.getChildren().get(form_relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
						}
						if(dataValue != null)
							form_relativeLayout.addContent(dataValue);
					
					}

					relativeLayout.addContent(form_relativeLayout);
					
					rootView.addContent(relativeLayout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void generateDetailViews() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_view_detail";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new ScrollView(null,"match_parent","match_parent","true");
					
					Element relativeLayout = new RelativeLayout(classId + "_detail_layout","match_parent","wrap_content");
					
					Element form_relativeLayout = null;
					
					Element form = null;
					if(cls.allParents().isEmpty()){
						form_relativeLayout = new RelativeLayout(classId + "_view_detail","match_parent","wrap_content");
						form = new Include(classId + "_form_detail");
//						form.setAttribute(new Id(classId + "_view_detail"));
						form_relativeLayout.addContent(form);
						relativeLayout.addContent(form_relativeLayout);
						
						if(ModelUtilities.isAssociativeClass(cls)){
							Element linearLayout = new LinearLayout(cls.name().toLowerCase() + "_associatives", "match_parent", "wrap_content", "vertical");
							if(relativeLayout.getChildren().size() > 0)
								linearLayout.setAttribute(new Below(relativeLayout.getChildren().get(relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
							
							for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(cls)){
								if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
									Element assToMember = new Element("FrameLayout");
									assToMember.setAttribute(new Id(ass.getTargetAEClass().name().toLowerCase() + "_detail_container"));
									assToMember.setAttribute(new Width("match_parent"));
									assToMember.setAttribute(new Height("wrap_content"));
									linearLayout.addContent(assToMember);
								}
							}
															
							relativeLayout.addContent(linearLayout);
						}
					}else{
						LinkedList<MClass> parents = new LinkedList<MClass>();
						
						MClass mostSuper = cls.parents().iterator().next();
						parents.addFirst(mostSuper);
						
						while(!mostSuper.parents().isEmpty()){
							mostSuper = mostSuper.parents().iterator().next();
							parents.addFirst(mostSuper);
						}
						
						for(MClass x : parents){
							form_relativeLayout = new RelativeLayout(x.name().toLowerCase() + "_view_detail","match_parent","wrap_content");
							form = new Include(x.name().toLowerCase() + "_form_detail");
//							form.setAttribute(new Id(x.name().toLowerCase() + "_view_detail"));
							if(relativeLayout.getChildren().size() > 0)
								form_relativeLayout.setAttribute(new Below(relativeLayout.getChildren().get(relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
							form_relativeLayout.addContent(form);
							relativeLayout.addContent(form_relativeLayout);
						}
						
//						and finally the class it self
						form_relativeLayout = new RelativeLayout(classId + "_view_detail","match_parent","wrap_content");
						form = new Include(classId + "_form_detail");
//						form.setAttribute(new Id(classId + "_view_detail"));
						if(relativeLayout.getChildren().size() > 0)
							form_relativeLayout.setAttribute(new Below(relativeLayout.getChildren().get(relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
						form_relativeLayout.addContent(form);
						relativeLayout.addContent(form_relativeLayout);
					}
					
					rootView.addContent(relativeLayout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void generateInsertUpdateViews() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null && !cls.isAbstract()){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_view_insertupdate";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new ScrollView(null,"match_parent","match_parent","true");
					
					Element relativeLayout = new RelativeLayout("match_parent","wrap_content");
					
					Element layout_relativeLayout = new RelativeLayout(classId + "_insertupdate_layout","match_parent","wrap_content");
					
					Element form_relativeLayout = null;
					Element form = null;
					if(cls.allParents().isEmpty()){
						form_relativeLayout = new RelativeLayout(classId + "_view_insertupdate","match_parent","wrap_content");
						form = new Include(classId + "_form_insertupdate");
//						form.setAttribute(new Id(classId + "_view_insertupdate"));
						form_relativeLayout.addContent(form);
						layout_relativeLayout.addContent(form_relativeLayout);
					}else{
						LinkedList<MClass> parents = new LinkedList<MClass>();
						
						MClass mostSuper = cls.parents().iterator().next();
						parents.addFirst(mostSuper);
						
						while(!mostSuper.parents().isEmpty()){
							mostSuper = mostSuper.parents().iterator().next();
							parents.addFirst(mostSuper);
						}
						
						for(MClass x : parents){
							form_relativeLayout = new RelativeLayout(x.name().toLowerCase() + "_view_insertupdate","match_parent","wrap_content");
							form = new Include(x.name().toLowerCase() + "_form_insertupdate");
//							form.setAttribute(new Id(x.name().toLowerCase() + "_view_insertupdate"));
							if(layout_relativeLayout.getChildren().size() > 0)
								form_relativeLayout.setAttribute(new Below(layout_relativeLayout.getChildren().get(layout_relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
							form_relativeLayout.addContent(form);
							layout_relativeLayout.addContent(form_relativeLayout);
						}
						
//						and finally the class it self
						form_relativeLayout = new RelativeLayout(classId + "_view_insertupdate","match_parent","wrap_content");
						form = new Include(classId + "_form_insertupdate");
//						form.setAttribute(new Id(classId + "_view_insertupdate"));
						if(layout_relativeLayout.getChildren().size() > 0)
							form_relativeLayout.setAttribute(new Below(layout_relativeLayout.getChildren().get(layout_relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
						form_relativeLayout.addContent(form);
						layout_relativeLayout.addContent(form_relativeLayout);
					}
					
					Element buttons_relativeLayout = new RelativeLayout(classId + "_insert_update_buttons","wrap_content","wrap_content");
					buttons_relativeLayout.setAttribute(new Below(layout_relativeLayout.getAttribute("id",namespace)));
					
					Element buttons = new Include("default_okcancel_buttons");
					
					buttons_relativeLayout.addContent(buttons);
					
					relativeLayout.addContent(layout_relativeLayout);
					relativeLayout.addContent(buttons_relativeLayout);
					
					rootView.addContent(relativeLayout);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
	}
	
	public List<AssociationInfo> getAssociativeGetter(MClass caller, MClass target, List<AssociationInfo> list){
		for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(caller)){
			if(ModelUtilities.isAssociativeClass(ass.getTargetAEClass()) && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
				getAssociativeGetter(ass.getTargetAEClass(), target, list);
				if(list.size() > 0 && list.get(0).getTargetAEClass() == target)
					list.add(ass);
			}
			if(ass.getTargetAEClass() == target && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
//				if(list.size() > 0)
//					list.add(list.get(list.size() - 1) + "." + ass.getTargetAE().name().toLowerCase() + "()");
//				else
					list.add(ass);
				return list;
			}
			
		}
		return list;
	}
	
	public List<MClass> getAssociativeTree_Associatives(MClass caller, List<MClass> list){
		for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(caller)){
			if(ModelUtilities.isAssociativeClass(ass.getTargetAEClass()) && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
				getAssociativeTree_Associatives(ass.getTargetAEClass(), list);
				
			}
//			if(ass.getTargetAEClass() == target && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
////				if(list.size() > 0)
////					list.add(list.get(list.size() - 1) + "." + ass.getTargetAE().name().toLowerCase() + "()");
////				else
//					list.add(ass);
//				return list;
//			}
			
		}
		if(ModelUtilities.isAssociativeClass(caller))
			list.add(caller);
		return list;
	}
	
	public void generateListView() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				classId = cls.name().toLowerCase();
				XMLName = classId + "_view_list";
				if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){					
					Element rootView = new RelativeLayout("wrap_content","?android:attr/listPreferredItemHeight");
					rootView.setAttribute(new Gravity("center_vertical"));
					rootView.setAttribute(new Background("drawable", "default_list_selector"));
					
					Element form_relativeLayout = null;
					
					if(ModelUtilities.isAssociativeClass(cls)){
						
//						System.out.println(getAssociativeTree_Associatives(cls, new ArrayList<MClass>()).toString());
						List<MClass> targets = ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(cls);
						boolean firts = true;
						int it = 0;
						int i = 0;
						for(MClass theClass : getAssociativeTree_Associatives(cls, new ArrayList<MClass>())){
//							System.out.println(getAssociativeGetter(cls, theClass, new ArrayList<AssociationInfo>()).toString());
							Element associativedivider_view = null;
							
							for(AssociationInfo ass: AssociationInfo.getAssociationsInfo(theClass)){
								if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
									
							String id = targets.get(it).name().toLowerCase() + "_list_layout";
							
							
//							for(AssociationInfo ass : getAssociativeGetter(cls, theClass, new ArrayList<AssociationInfo>())){
									form_relativeLayout = new RelativeLayout(id,"wrap_content","wrap_content");
	//							String targetRole = ModelUtilities.getAssociationToAssociative(theClass, cls).getTargetAE().name().toLowerCase();
	//							System.out.println("AAAKKOIIIIII");
	//							System.out.println(theClass.name());
	//							System.out.println(ModelUtilities.getAssociativeRole(theClass, cls).toLowerCase());
									if(firts){
										form_relativeLayout.addContent(new Include(targets.get(it).name().toLowerCase() + "_form_list"));
										firts = false;
										rootView.addContent(form_relativeLayout);
										associativedivider_view = new RelativeLayout(ass.getSourceAEClass().name().toLowerCase() + "_associations", "wrap_content", "wrap_content");
										//FALTA O STYLE
										Element textleft = new TextView(ass.getSourceAEClass().name().toLowerCase() + "_list_layout_divider_text_" + ass.getTargetAE().name().toLowerCase(), "wrap_content", "wrap_content" , ass.getSourceAEClass().name().toLowerCase() + "_associationto_" + ass.getTargetAE().name().toLowerCase(), null);
		//								textleft.setAttribute("layout_alignTop", "true", namespace);
										associativedivider_view.addContent(textleft);
										
										Element image = new ImageView("default_association_associative_class_list_divider", "wrap_content", "wrap_content", "drawable", "ic_light_association_associative_class_list_divider");
										image.setAttribute("layout_centerHorizontal", "true", namespace);//fartei-me
										image.setAttribute("layout_toRightOf", associativedivider_view.getChildren().get(associativedivider_view.getChildren().size() - 1).getAttribute("id",namespace).getValue(), namespace);
										if(rootView.getChildren().size() > 0)
											associativedivider_view.setAttribute(new Below(rootView.getChildren().get(rootView.getChildren().size() - 1).getAttribute("id",namespace)));
										++i;
										++it;
										
										associativedivider_view.addContent(image);
									}else{
										
										if(i % 2 == 0){
											associativedivider_view = new RelativeLayout(ass.getSourceAEClass() + "_associations", "wrap_content", "wrap_content");
											//FALTA O STYLE
											Element textleft = new TextView(ass.getSourceAEClass().name().toLowerCase() + "_list_layout_divider_text_" + ass.getTargetAE().name().toLowerCase(), "wrap_content", "wrap_content" , ass.getSourceAEClass().name().toLowerCase() + "_associationto_" + ass.getTargetAE().name().toLowerCase(), null);
		//									textleft.setAttribute("layout_alignTop", "true", namespace);
											associativedivider_view.addContent(textleft);
											
											Element image = new ImageView("default_association_associative_class_list_divider", "wrap_content", "wrap_content", "drawable", "ic_light_association_associative_class_list_divider");
											
											image.setAttribute("layout_centerHorizontal", "true", namespace);//fartei-me
											image.setAttribute("layout_toRightOf", associativedivider_view.getChildren().get(associativedivider_view.getChildren().size() - 1).getAttribute("id",namespace).getValue(), namespace);
											if(rootView.getChildren().size() > 0)
												associativedivider_view.setAttribute(new Below(rootView.getChildren().get(rootView.getChildren().size() - 1).getAttribute("id",namespace)));
											++i;
											
											associativedivider_view.addContent(image);
										}else{
											Element textright = new TextView(ass.getSourceAEClass().name().toLowerCase() + "_list_layout_divider_text_" + ass.getTargetAE().name().toLowerCase(), "wrap_content", "wrap_content" , ass.getSourceAEClass().name().toLowerCase() + "_associationto_" + ass.getTargetAE().name().toLowerCase(), null);
											textright.setAttribute("layout_toRightOf", "@+id/default_association_associative_class_list_divider", namespace);
											textright.setAttribute("layout_alignBottom", "@+id/default_association_associative_class_list_divider", namespace);
											
											associativedivider_view.addContent(textright);
											rootView.addContent(associativedivider_view);
											i = 0;
											
											if(rootView.getChildren().size() > 0)
												form_relativeLayout.setAttribute(new Below(rootView.getChildren().get(rootView.getChildren().size() - 1).getAttribute("id",namespace)));
											form_relativeLayout.addContent(new Include(targets.get(it).name().toLowerCase() + "_form_list"));
												
												
											rootView.addContent(form_relativeLayout);

										}
									}
								}
							}
							it++;
						}
					}else{
						Element form = new Include(classId + "_form_list");
						form_relativeLayout = new RelativeLayout(classId + "_form_list","wrap_content","wrap_content");
						form_relativeLayout.addContent(form);
						rootView.addContent(form_relativeLayout);
					}
					
////					--------------*************** CODIGO NOVO - START  ******************* ------------------
//					List<MAttribute> finalAttributeList;
//					if(cls.getAnnotation("list") != null){
//						finalAttributeList = ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues());
//					}else
//						finalAttributeList = cls.attributes();
////					--------------*************** CODIGO NOVO - END  ******************* ------------------
//					for (MAttribute att : finalAttributeList){
//							
//						String attributeName = att.name().toLowerCase();
//											
//						Element dataValue = null;
//						
//						if(att.type().isBoolean()){
//							dataValue = new CheckBox(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value", "wrap_content", "wrap_content", attributeBaseAncestor(cls, att).name().toLowerCase() + "_detail_" + attributeName + "_descriptor" ,null); 	
//							dataValue.setAttribute(new Style(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value_style", "@style/"));
//						}			
//									
//						if(att.type().isEnum() || att.type().isInteger() || att.type().isNumber() || att.type().isString() || att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date"))){
//							dataValue = new TextView(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value", "wrap_content", "wrap_content", null); 	
//							dataValue.setAttribute(new Style(attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + attributeName + "_value_style", "@style/"));
//						}
//						
//						if(form_relativeLayout.getChildren().size() > 0 && dataValue != null){
//							dataValue.setAttribute(new Below(form_relativeLayout.getChildren().get(form_relativeLayout.getChildren().size() - 1).getAttribute("id",namespace)));
//						}
//						if(dataValue != null)
//							form_relativeLayout.addContent(dataValue);
//					
//					}
					
					Element image = new ImageView("default_list_invalid", "wrap_content", "wrap_content", "drawable", "ic_contract_error");
					image.setAttribute("layout_centerVertical", "true", namespace);//fartei-me
					image.setAttribute("layout_alignParentRight", "true", namespace);

					
					rootView.addContent(image);
					
					XMLOutputter outputter = new XMLOutputter();
					try {
						outputter.setFormat(format);
						outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
						statistics.addOneToGenerated();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void generateNaviagtionBarLists() {
		String targetDirectory = rootDirectory + layout + "/";
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
//				just for super classes who have associations
				if(ModelUtilities.isAssociativeClass(cls) || !cls.allAssociations().isEmpty()){
					String classId = cls.name().toLowerCase();
					String XMLName = classId + "_list_navigationbar";
					if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
						
						Element rootView = new Element("merge");
						rootView.addNamespaceDeclaration(namespace);
						
						String associationName;
							
						for (AssociationInfo association : AssociationInfo.getAssociationsInfo(cls)){
							associationName = association.getTargetAE().name().toLowerCase();
							if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
								associationName = association.getTargetAEClass().nameAsRolename().toLowerCase();
							generateNavigationBarAssociations(association, "association", associationName, classId, rootView);
						}
						
						if(isSubClass(cls)){
							Element subClass = new Include(cls.parents().iterator().next().name().toLowerCase() + "_list_navigationbar");
							rootView.addContent(subClass);
						}
									
						XMLOutputter outputter = new XMLOutputter();
						try {
							outputter.setFormat(format);
							outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
							statistics.addOneToGenerated();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void generateNavigationViews() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
//				if class has any association or is an super ou sub class (generalization also permit navigation)
				if(ModelUtilities.isAssociativeClass(cls) || !cls.associations().isEmpty() || cls.children().size() > 0 || cls.parents().size() > 0){
					classId = cls.name().toLowerCase();
					XMLName = classId + "_view_navigationbar";
					if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
						Element rootView = new RelativeLayout("match_parent","wrap_content");
						
						Element default_arrows = new Include("default_navigationbar");
						rootView.addContent(default_arrows);
						
						generateDividerView(rootView, new Attribute("layout_width","match_parent"), new Attribute("layout_height","1dp"));
						
						Element HorizontalScrollView = new Element("HorizontalScrollView");
						HorizontalScrollView.setAttribute(new Id(classId + "_navigationbar_view"));
						HorizontalScrollView.setAttribute(new Width("wrap_content"));
						HorizontalScrollView.setAttribute(new Height("wrap_content"));
						HorizontalScrollView.setAttribute("layout_centerInParent","true",namespace);
						HorizontalScrollView.setAttribute("layout_toLeftOf","@+id/navigation_bar_view_right_arrow",namespace);
						HorizontalScrollView.setAttribute("layout_toRightOf","@+id/navigation_bar_view_left_arrow",namespace);
						rootView.addContent(HorizontalScrollView);
						
						Element root_linearLayout = new LinearLayout("wrap_content", "wrap_content", "horizontal");
						HorizontalScrollView.addContent(root_linearLayout);
						
						generateDividerView(root_linearLayout, new Attribute("layout_width","2dp"), new Attribute("layout_height","match_parent"));
						
						String associationName;
//						for (AssociationInfo association : AssociationInfo.getAssociationsInfo(cls)){
//							associationName = association.getTargetAE().cls().name().toLowerCase();
//							generateNavigationBarAssociations(association, "association", associationName, classId, root_linearLayout);
//						}
						
						Element superClass = new Include(cls.name().toLowerCase() + "_list_navigationbar");
						root_linearLayout.addContent(superClass);
						
						if(isSubClass(cls)){
							associationName = cls.parents().iterator().next().name().toLowerCase();
							generateNavigationBarAssociations(cls.parents().iterator().next(), "super", associationName, classId, root_linearLayout);
						}
						
						if(isSuperClass(cls))
							for(MClass x : getAllDirectSubClasses(Arrays.asList(cls))){
								associationName = x.name().toLowerCase();
								generateNavigationBarAssociations(x, "child", associationName, classId, root_linearLayout);
							}
						
////						super to sub classes
//						for(MClass child : cls.children()){
//							associationName = child.name().toLowerCase();
//							generateNavigationBarAssociations(child, "child", associationName, classId, root_linearLayout);
//						}
////						sub to super classes
//						for(MClass parent : cls.parents()){
//							associationName = parent.name().toLowerCase();
//							generateNavigationBarAssociations(parent, "super", associationName, classId, root_linearLayout);
//						}
						
						Element divider = new Element("View");
						divider.setAttribute("layout_width", "match_parent", namespace);
						divider.setAttribute("layout_height", "2dp", namespace);
						divider.setAttribute("background", "@drawable/navigationbar_divider", namespace);
						divider.setAttribute(new Below(HorizontalScrollView.getAttribute("id",namespace)));
						
						rootView.addContent(divider);
						
						XMLOutputter outputter = new XMLOutputter();
						try {
							outputter.setFormat(format);
							outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
							statistics.addOneToGenerated();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void generateDividerView(Element parent, Attribute attribute1, Attribute attribute2){
		Element divider = new Element("View");
		divider.setAttribute(attribute1.getName(), attribute1.getValue(), namespace);
		divider.setAttribute(attribute2.getName(), attribute2.getValue(), namespace);
		divider.setAttribute("background", "@drawable/navigationbar_divider", namespace);
		parent.addContent(divider);
	}
	
	//super and sub are not associations but in this case they might be considered as associations
	// super -> sub : ToMANY
	//sub -> super : ToONE
	public void generateNavigationBarAssociations(Object association, String typeAssociation, String associationClassName, String classId, Element root_linearLayout){
		//add if statement here to change name for super and subs ("_navigationbar_association_" -> "tosuper" or "tosub")
		String name = "_navigationbar_association_";
				
		Element root_ass_linearLayout = new LinearLayout(classId + name + associationClassName ,"match_parent", "wrap_content", "horizontal");
		root_ass_linearLayout.setAttribute(new Clickable(false));
		root_ass_linearLayout.setAttribute(new LongClickable(true));
		root_linearLayout.addContent(root_ass_linearLayout);
		 
		Element associationDescription = new TextView(classId + name + associationClassName + "_descriptor","wrap_content","wrap_content", "@style/" + classId + name + associationClassName + "_descriptor_style");
		associationDescription.setAttribute("layout_gravity", "center", namespace);
									
		Element associationImage = null;
			
		Element associationStateInfo = null;
		
		//normal association
		if(association instanceof AssociationInfo){
			associationDescription.setAttribute(new Text(classId + "_associationto_" + associationClassName));
			
			if(((AssociationInfo) association).getKind() == AssociationKind.MEMBER2ASSOCIATIVE){
				associationImage = new ImageView(classId + name + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_associative_class");
				associationStateInfo = new TextView(classId + name + associationClassName + "_numberobjects", "wrap_content", "wrap_content", "@style/" + classId + name + associationClassName + "_numberobjects_style");
				associationStateInfo.setAttribute(new Attribute("text", "( 0 )", namespace));
			}else if(((AssociationInfo) association).getTargetAE().isCollection() && ((AssociationInfo) association).getKind() != AssociationKind.ASSOCIATIVE2MEMBER){
				associationImage = new ImageView(classId + name + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_many");
				associationStateInfo = new TextView(classId + name + associationClassName + "_numberobjects", "wrap_content", "wrap_content", "@style/" + classId + name + associationClassName + "_numberobjects_style");
				associationStateInfo.setAttribute(new Attribute("text", "( 0 )", namespace));
			}else{
				associationImage = new ImageView(classId + name + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_one");
				associationStateInfo = new TextView(classId + name + associationClassName + "_numberobjects", "wrap_content", "wrap_content", "@style/" + classId + name + associationClassName + "_numberobjects_style");
				associationStateInfo.setAttribute(new Attribute("text", "( 0 )", namespace));
			}
		}
		//Generalization
		if(association instanceof MClass){
			if(typeAssociation.equals("child")){
				associationDescription.setAttribute(new Text(classId + "_tosub_" + associationClassName));
				associationImage = new ImageView(classId + name + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_inheritance_sub");
				associationStateInfo = new TextView(classId + name + associationClassName + "_numberobjects", "wrap_content", "wrap_content", "@style/" + classId + name + associationClassName + "_numberobjects_style");
				associationStateInfo.setAttribute(new Attribute("text", "( 0 )", namespace));
			}
			if(typeAssociation.equals("super")){
				associationDescription.setAttribute(new Text(classId + "_tosuper_" + associationClassName));
				associationImage = new ImageView(classId + name + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_inheritance_super");				
				associationStateInfo = new TextView(classId + name + associationClassName + "_numberobjects", "wrap_content", "wrap_content", "@style/" + classId + name + associationClassName + "_numberobjects_style");
				associationStateInfo.setAttribute(new Attribute("text", "( 1 )", namespace));
			}
		}
		
			
		root_ass_linearLayout.addContent(associationDescription);
		if(associationImage != null)
			root_ass_linearLayout.addContent(associationImage);
		if(associationStateInfo != null){
			associationStateInfo.setAttribute(new Background(drawable, "navigationbar_selector"));
			root_ass_linearLayout.addContent(associationStateInfo);
		}
			
		generateDividerView(root_linearLayout, new Attribute("layout_width","2dp"), new Attribute("layout_height","match_parent"));
	}
	
	public void generateMenuViews(){
		String targetDirectory = rootDirectory + menu + "/";
		String sourceDirectory = sourcePath + "/res/use2android/defaultdata/XML/" + menu + "/";
		
		FileUtilities.copyFile(sourceDirectory + "menu_launcher.xml",targetDirectory + "menu_launcher.xml");
		FileUtilities.copyFile(sourceDirectory + "menu_read.xml",targetDirectory + "menu_read.xml");
		FileUtilities.copyFile(sourceDirectory + "menu_write.xml",targetDirectory + "menu_write.xml");
		
		statistics.addToCopied(3);
	}
	
	private void generateAssociationToGeneralizationViews() {
		String targetDirectory = rootDirectory + layout + "/";
		String XMLName;
		String classId;
		String targetId;
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
				
				for (AssociationInfo association : AssociationInfo.getAssociationsInfo(cls)){
//					if the target class is a super class (generalization)
					if(association.getTargetAE().cls().allChildren().size() > 0){
						String associationName = association.getTargetAE().name().toLowerCase();
						if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
							associationName = association.getTargetAEClass().nameAsRolename().toLowerCase();
						
						classId = cls.name().toLowerCase();
						targetId = association.getTargetAEClass().name().toLowerCase();
						XMLName = classId + "_generalizationoptions_" + associationName + "_view";
						if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
							Element rootView = new LinearLayout("match_parent","match_parent","vertical");
							
							if(!association.getTargetAE().cls().isAbstract()){
								generateGeneralizationOptionsElements(association, "association", associationName, classId, rootView);
//								generateDividerView(rootView, new Attribute("layout_width","match_parent"), new Attribute("layout_height","2dp"));

							}
//							generateGeneralizationOptionsElements(association, "", rootView, classId, targetId);
							
							Element superClass = new Include(targetId + "_generalizationoptions_offsprings");
							rootView.addContent(superClass);
							
							XMLOutputter outputter = new XMLOutputter();
							try {
								outputter.setFormat(format);
								outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
								statistics.addOneToGenerated();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	public void generateAssociationsToGeneralizationOptions(){
		String targetDirectory = rootDirectory + layout + "/";
		for (MClass cls : mModel.classes()){
			if(cls.isAnnotated() && cls.getAnnotation("domain") != null){
//				just for super classes who have associations
				if(!cls.allAssociations().isEmpty() && (isSuperClass(cls) || isSubClass(cls))){
					String classId = cls.name().toLowerCase();
					String XMLName = classId + "_generalizationoptions_offsprings";
					if (FileUtilities.openOutputFile(targetDirectory, XMLName + ".xml")){
						
						Element rootView = new Element("merge");
						rootView.addNamespaceDeclaration(namespace);
						
						String associationName;
						if(isSuperClass(cls)){
							for(MClass x : getAllDirectSubClasses(Arrays.asList(cls))){
								if(!x.isAbstract()){
									associationName = x.name().toLowerCase();
									generateGeneralizationOptionsElements(x, "child", associationName, cls.name().toLowerCase(), rootView);
								}
							
//							if(isSubClass(cls)){
//								if(!cls.isAbstract()){
//									associationName = cls.parents().iterator().next().name().toLowerCase();
//									generateGeneralizationOptionsElements(cls.parents().iterator().next(), "child", associationName, classId, rootView);
//								}
//							}
								
							}
							for(MClass x : getAllDirectSubClasses(Arrays.asList(cls))){
								Element subClass = new Include(x.name().toLowerCase() + "_generalizationoptions_offsprings");
								rootView.addContent(subClass);
							}
						}
//						Set<MClass> childs = new HashSet<MClass>();
//						childs.addAll(cls.allChildren());
//						while(!childs.isEmpty()){
//							for(MClass child : childs){
//								if(!childs.contains(child.parents().iterator().next())){
//									if(!child.isAbstract()){
//										generateGeneralizationOptionsElements(null,"child", rootView, child.parents().iterator().next().name().toLowerCase(), child.name().toLowerCase());
//										generateDividerView(rootView, new Attribute("layout_width","match_parent"), new Attribute("layout_height","2dp"));
//									}
//									childs.remove(child);
//									break;
//								}
//							}
//						}
									
						XMLOutputter outputter = new XMLOutputter();
						try {
							outputter.setFormat(format);
							outputter.output(new Document(rootView), new FileOutputStream (targetDirectory + XMLName + ".xml"));
							statistics.addOneToGenerated();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void generateGeneralizationOptionsElements(Object association, String typeAssociation, String associationClassName, String classId, Element root_linearLayout){
		Element root_ass_linearLayout = new LinearLayout(classId + "_generalizationoptions_" + associationClassName ,"match_parent", "wrap_content", "horizontal");
		root_linearLayout.addContent(root_ass_linearLayout);
    
		Element associationDescription = new TextView(classId + "_generalizationoptions_" + associationClassName + "_descriptor","wrap_content","wrap_content",null);
		associationDescription.setAttribute("layout_gravity", "center", namespace);
									
		Element associationImage = null;
					
		//normal association
		if(association instanceof AssociationInfo){
			associationDescription.setAttribute(new Text(classId + "_associationto_" + associationClassName));
			if(((AssociationInfo) association).getKind() == AssociationKind.MANY2MANY || (((AssociationInfo) association).getKind() == AssociationKind.ONE2MANY && ((AssociationInfo) association).getTargetAE().isCollection())){
				associationImage = new ImageView(classId + "_generalizationoptions_" + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_many");
			}else{
				associationImage = new ImageView(classId + "_generalizationoptions_" + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_one");
			}
		}
		//Generalization
		if(association instanceof MClass){
			if(typeAssociation.equals("child")){
				associationDescription.setAttribute(new Text(classId + "_tosub_" + associationClassName));
				associationImage = new ImageView(classId + "_generalizationoptions_" + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_inheritance_sub");
			}
//			not needed
//			if(typeAssociation.equals("super")){
//				associationDescription.setAttribute(new Text(classId + "_tosuper_" + associationClassName));
//				associationImage = new ImageView(classId + "_generalizationoptions_" + associationClassName + "_image","wrap_content","wrap_content","drawable","ic_light_association_inheritance_super");				
//			}
		}
		
			
		root_ass_linearLayout.addContent(associationDescription);
		if(associationImage != null)
			root_ass_linearLayout.addContent(associationImage);

		generateDividerView(root_linearLayout, new Attribute("layout_height","2dp"), new Attribute("layout_width","match_parent"));
	}
	
}
