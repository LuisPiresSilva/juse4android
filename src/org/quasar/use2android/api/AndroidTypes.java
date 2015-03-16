package org.quasar.use2android.api;

import java.util.HashSet;
import java.util.Set;

import org.quasar.juse.api.implementation.FileUtilities;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.ocl.type.Type;

public class AndroidTypes {
	
	/***********************************************************
	 * @param oclType
	 * @param android type (smallest or normal)
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToWidget(Type oclType, AndroidWidgetPreference androidType)
	{
		String type = androidPrimitiveTypeToReadWidget(oclType, androidType);
		if(type.equals("ERROR!"))
			type = androidPrimitiveTypeToWriteWidget(oclType, androidType);
		return type;
	}
	
	/***********************************************************
	 * @param oclType
	 * @param android type (smallest or normal)
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToReadWidget(Type oclType, AndroidWidgetPreference androidType)
	{
		if (oclType.isInteger() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "TextView";
		if (oclType.isReal() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "TextView";
		if (oclType.isBoolean() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "CheckBox";//to do
		if (oclType.isString() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "TextView";
		if (oclType.isEnum() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "TextView";
		if (oclType.isObjectType()){
			if(oclType.toString().equals("CalendarDate") && androidType == AndroidWidgetPreference.NORMAL)
				return "DatePicker";
			else if(oclType.toString().equals("CalendarDate") && androidType == AndroidWidgetPreference.SMALLEST)
				return "TextView";
			else if(oclType.toString().equals("CalendarTime") && androidType == AndroidWidgetPreference.NORMAL)
				return "CustomTimePicker";
			else if(oclType.toString().equals("CalendarTime") && androidType == AndroidWidgetPreference.SMALLEST)
				return "TextView";
			else
				return "Fragment";//to do
		}
		if (oclType.isTrueObjectType())
			return oclType.toString();//to do
		if (oclType.isTrueOclAny())
			return "Object";//to do
		if (oclType.isVoidType())
			return "void";//to do
//		if(oclType.toString().equals("Date") && androidType == AndroidWidgetPreference.NORMAL)
//			return "DatePicker";
//		if(oclType.toString().equals("Date") && androidType == AndroidWidgetPreference.SMALLEST)
//			return "TextView";

		return "ERROR!";
	}
	
	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToWriteWidget(Type oclType, AndroidWidgetPreference androidType)
	{
		if (oclType.isInteger() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "EditText";
		if (oclType.isReal() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "EditText";
		if (oclType.isBoolean() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "CheckBox";
		if (oclType.isString() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "EditText";
		if (oclType.isEnum() && androidType == AndroidWidgetPreference.NORMAL || androidType == AndroidWidgetPreference.SMALLEST)
			return "Spinner";
		if (oclType.isObjectType()){
			if(oclType.toString().equals("CalendarDate") && androidType == AndroidWidgetPreference.NORMAL)
				return "DatePicker";
			else if(oclType.toString().equals("CalendarDate") && androidType == AndroidWidgetPreference.SMALLEST)
				return "TextView";
			else if(oclType.toString().equals("CalendarTime") && androidType == AndroidWidgetPreference.NORMAL)
				return "CustomTimePicker";
			else if(oclType.toString().equals("CalendarTime") && androidType == AndroidWidgetPreference.SMALLEST)
				return "TextView";
			else
				return "Fragment";//to do
		}
		if (oclType.isTrueObjectType())
			return oclType.toString();//to do
		if (oclType.isTrueOclAny())
			return "Object";//to do
		if (oclType.isVoidType())
			return "void";//to do
		if(oclType.toString().equals("Date") && androidType == AndroidWidgetPreference.NORMAL)
			return "DatePicker";
		if(oclType.toString().equals("Date") && androidType == AndroidWidgetPreference.SMALLEST)
			return "TextView";

		return "ERROR!";
	}

	/***********************************************************
	 * @param javaTypes
	 * @return
	 ***********************************************************/
	public static Set<String> javaInAndroidImportDeclarations(Set<Type> javaTypes)
	{
		Set<String> result = new HashSet<String>();

		for (Type javaType : javaTypes)
		{
			if (javaType != null)
			{
				if (javaType.isSequence())
				{
					result.add("import java.util.Queue;");
					// result.add("import java.util.ArrayDeque;");
				}
				if (javaType.isOrderedSet())
				{
					result.add("import java.util.SortedSet;");
					result.add("import java.util.TreeSet;");
				}
				if (javaType.isBag())
				{
					result.add("import java.util.List;");
					result.add("import java.util.ArrayList;");
				}
				if (javaType.isSet())
				{
					result.add("import java.util.HashSet;");
				}
				if (javaType.isDate() || (javaType.isObjectType() && javaType.toString().equals("Date")))
				{
					result.add("import java.util.Date;");
				}
			}
		}
		return result;
	}
	
	/***********************************************************
	 * @param androidTypes
	 * @return
	 ***********************************************************/
	public static Set<String> androidImportDeclarations(Set<String> androidTypes)
	{
		Set<String> result = new HashSet<String>();


		for (String androidType : androidTypes)
		{
			if (androidType != null)
			{
				if (androidType.equals("Activity"))
					result.add("import android.app.Activity;");
				if (androidType.equals("FragmentActivity"))
					result.add("import android.support.v4.app.FragmentActivity;");
				if (androidType.equals("Fragment"))
					result.add("import android.support.v4.app.Fragment;");
				if (androidType.equals("FragmentTransaction"))
					result.add("import android.support.v4.app.FragmentTransaction;");
				if (androidType.equals("InputMethodManager"))
					result.add("import android.view.inputmethod.InputMethodManager;");
				if (androidType.equals("Context"))
					result.add("import android.content.Context;");
				if (androidType.equals("Intent"))
					result.add("import android.content.Intent;");
				if (androidType.equals("Menu"))
					result.add("import android.view.Menu;");
				if (androidType.equals("MenuItem"))
					result.add("import android.view.MenuItem;");
				if (androidType.equals("ListView"))
					result.add("import android.widget.ListView;");
				if (androidType.equals("GridView"))
					result.add("import android.widget.GridView;");
				if (androidType.equals("Toast"))
					result.add("import android.widget.Toast;");
				if (androidType.equals("Color"))
					result.add("import android.graphics.Color;");
				if (androidType.equals("TransitionDrawable"))
					result.add("import android.graphics.drawable.TransitionDrawable;");
				if (androidType.equals("Bundle"))
					result.add("import android.os.Bundle;");
				if (androidType.equals("LayoutInflater"))
					result.add("import android.view.LayoutInflater;");
				if (androidType.equals("View"))
					result.add("import android.view.View;");
				if (androidType.equals("ViewGroup"))
					result.add("import android.view.ViewGroup;");
				if (androidType.equals("View.OnClickListener"))
					result.add("import android.view.View.OnClickListener;");
				if (androidType.equals("View.OnLongClickListener"))
					result.add("import android.view.View.OnLongClickListener;");
				if (androidType.equals("AdapterView"))
					result.add("import android.widget.AdapterView;");
				if (androidType.equals("AdapterView.OnItemClickListener"))
					result.add("import android.widget.AdapterView.OnItemClickListener;");
				if (androidType.equals("ImageView"))
					result.add("import android.widget.ImageView;");
				if (androidType.equals("TextView"))
					result.add("import android.widget.TextView;");
				if (androidType.equals("EditText"))
					result.add("import android.widget.EditText;");
				if (androidType.equals("DatePicker"))
					result.add("import android.widget.DatePicker;");
				if (androidType.equals("Spinner"))
					result.add("import android.widget.Spinner;");
				if (androidType.equals("CheckBox"))
					result.add("import android.widget.CheckBox;");
				
			}
		}
		return result;
	}

//	CASE SPINNER PARA ENUM
	public static String androidWidgetContentSetter(AndroidWidgetsTypes widgettype, Type type, String content, AndroidWidgetPreference androidType){
		String widget;
		if(widgettype == AndroidWidgetsTypes.WRITE_WIDGET)
			widget = androidPrimitiveTypeToWriteWidget(type, androidType);
		else if(widgettype == AndroidWidgetsTypes.READ_WIDGET)
			widget = androidPrimitiveTypeToReadWidget(type, androidType);
		else
			widget = "error";
		switch(widget){
			case "TextView":
				if(content.equals("\"null\""))
					return "setText(" + content + ")";
				else if(type.isDate() || (type.isObjectType() && type.toString().equals("CalendarDate")))
					return "setText(\"\" + " + content + ".year() + \"/\" + (" + content + ".month() + 1) + \"/\" + " + content + ".day())";
				else if(type.isDate() || (type.isObjectType() && type.toString().equals("CalendarTime")))
					return "setText(\"\" + " + content + ".hours() + \":\" + " + content + ".minutes() + \":\" + " + content + ".seconds())";
				else
					return "setText(\"\" + " + content + ")";//guarantees that is an String (example -> just double needs "" + double)
			case "EditText":
				if(content.equals("\"null\""))
					return "setText(" + content + ")";
				else
					return "setText(\"\" + " + content + ")";
			//this is java the java.util.Date is expected in business classes therefore its methods can be accessed
			case "DatePicker":
				return "init(" + content + ".year()," + content + ".month()," + content + ".day(), null)";
			case "CustomTimePicker":
				return "init(" + content + ".hours()," + content + ".minutes()," + content + ".seconds())";
				
			case "CheckBox":
				return "setChecked(" + content + ")";
			case "Spinner":
				return "setSelection(" + type.toString() + ".valueOf(" + content + ".toString()).ordinal())";
//			case "others":
//				return something;
			default:
				return "ERROR!";
		}
	}
	
	public static String androidInputWidgetContentGetter(AndroidWidgetsTypes widgettype, MAttribute att, String variable, AndroidWidgetPreference androidType, boolean isSpecialPrimitive){
		String widget;
		Type type;
		if(isSpecialPrimitive)
			type = att.owner().type();
		else
			type = att.type();		
		if(widgettype == AndroidWidgetsTypes.WRITE_WIDGET)
			widget = androidPrimitiveTypeToWriteWidget(type, androidType);
		else if(widgettype == AndroidWidgetsTypes.READ_WIDGET)
			widget = androidPrimitiveTypeToReadWidget(type, androidType);
		else
			widget = "error";
		switch(widget){
			case "EditText":
				return att.name() + variable + ".getText().toString()";
			case "DatePicker":
				if(att.name().toLowerCase().equals("day"))
					return att.owner().name().toLowerCase() + variable + ".getDayOfMonth()";
				else
					return att.owner().name().toLowerCase() + variable + ".get" + FileUtilities.capitalize(att.name() + "()");
			case "CustomTimePicker":
				return att.owner().name().toLowerCase() + variable + ".getCurrent" + FileUtilities.capitalize(att.name()) + "()";
			case "CheckBox":
				return att.name() + variable + ".isChecked()";
			//for this case we only work with the position since in multi-languages the values may differ - this way we avoid multiple code enums
			case "Spinner":
				return att.name() + variable + ".getSelectedItemPosition()";
//			case "others":
//				return something;
			default:
				return "ERROR!";
		}
	}

}
