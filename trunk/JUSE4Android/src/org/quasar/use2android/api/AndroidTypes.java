package org.quasar.use2android.api;

import java.util.HashSet;
import java.util.Set;

import org.tzi.use.uml.ocl.type.Type;

public class AndroidTypes {

	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToWidget(Type oclType)
	{
		String type = androidPrimitiveTypeToReadWidget(oclType);
		if(type.equals("ERROR!"))
			type = androidPrimitiveTypeToWriteWidget(oclType);
		return type;
	}
	
	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToReadWidget(Type oclType)
	{
		if (oclType.isInteger())
			return "TextView";
		if (oclType.isReal())
			return "TextView";
		if (oclType.isBoolean())
			return "boolean";//to do
		if (oclType.isString())
			return "TextView";
		if (oclType.isEnum())
			return "TextView";//to do
		if (oclType.isObjectType()){
			if(oclType.toString().equals("Date"))
				return "DatePicker";
			else
				return oclType.toString();//to do
		}
		if (oclType.isTrueObjectType())
			return oclType.toString();//to do
		if (oclType.isTrueOclAny())
			return "Object";//to do
		if (oclType.isVoidType())
			return "void";//to do
		if (oclType.isDate())
			return "DatePicker";

		return "ERROR!";
	}
	
	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String androidPrimitiveTypeToWriteWidget(Type oclType)
	{
		if (oclType.isInteger())
			return "EditText";
		if (oclType.isReal())
			return "EditText";
		if (oclType.isBoolean())
			return "boolean";//to do
		if (oclType.isString())
			return "EditText";
		if (oclType.isEnum())
			return oclType.toString();//to do
		if (oclType.isObjectType()){
			if(oclType.toString().equals("Date"))
				return "DatePicker";
			else
				return oclType.toString();//to do
		}
		if (oclType.isTrueObjectType())
			return oclType.toString();//to do
		if (oclType.isTrueOclAny())
			return "Object";//to do
		if (oclType.isVoidType())
			return "void";//to do
		if (oclType.isDate())
			return "DatePicker";

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
				{
					result.add("import android.app.Activity;");
				}
				if (androidType.equals("Fragment"))
				{
					result.add("import android.app.Fragment;");
				}
				if (androidType.equals("FragmentTransaction"))
				{
					result.add("import android.app.FragmentTransaction;");
				}
				if (androidType.equals("InputMethodManager"))
				{
					result.add("import android.view.inputmethod.InputMethodManager;");
				}
				if (androidType.equals("Context"))
				{
					result.add("import android.content.Context;");
				}
				if (androidType.equals("Intent"))
				{
					result.add("import android.content.Intent;");
				}
				if (androidType.equals("Menu"))
				{
					result.add("import android.view.Menu;");
				}
				if (androidType.equals("MenuItem"))
				{
					result.add("import android.view.MenuItem;");
				}
				if (androidType.equals("ListView"))
				{
					result.add("import android.widget.ListView;");
				}
				if (androidType.equals("GridView"))
				{
					result.add("import android.widget.GridView;");
				}
				if (androidType.equals("Toast"))
				{
					result.add("import android.widget.Toast;");
				}
				if (androidType.equals("Color"))
				{
					result.add("import android.graphics.Color;");
				}
				if (androidType.equals("TransitionDrawable"))
				{
					result.add("import android.graphics.drawable.TransitionDrawable;");
				}
				if (androidType.equals("Bundle"))
				{
					result.add("import android.os.Bundle;");
				}
				if (androidType.equals("LayoutInflater"))
				{
					result.add("import android.view.LayoutInflater;");
				}
				if (androidType.equals("View"))
				{
					result.add("import android.view.View;");
				}
				if (androidType.equals("ViewGroup"))
				{
					result.add("import android.view.ViewGroup;");
				}
				if (androidType.equals("View.OnClickListener"))
				{
					result.add("import android.view.View.OnClickListener;");
				}
				if (androidType.equals("View.OnLongClickListener"))
				{
					result.add("import android.view.View.OnLongClickListener;");
				}
				if (androidType.equals("AdapterView"))
				{
					result.add("import android.widget.AdapterView;");
				}
				if (androidType.equals("AdapterView.OnItemClickListener"))
				{
					result.add("import android.widget.AdapterView.OnItemClickListener;");
				}
				if (androidType.equals("ImageView"))
				{
					result.add("import android.widget.ImageView;");
				}
				if (androidType.equals("TextView"))
				{
					result.add("import android.widget.TextView;");
				}
				if (androidType.equals("EditText"))
				{
					result.add("import android.widget.EditText;");
				}
				if (androidType.equals("DatePicker"))
				{
					result.add("import android.widget.DatePicker;");
				}
			}
		}
		return result;
	}

	public static String androidWidgetContentSetter(Type type, String content)
	{
		String widget = androidPrimitiveTypeToReadWidget(type);
		if(widget.equals("ERROR!"))
			widget = androidPrimitiveTypeToWriteWidget(type);
		switch(widget){
			case "TextView":
				return "setText(\"\" + " + content + ")";//guarantees that is an String (example -> just double needs "" + double)
			case "EditText":
				return "setText(\"\" + " + content + ")";
			//this is java the java.util.Date is expected in business classes therefore its methods
			//can be accessed
			case "DatePicker":
				return "updateDate(" + content + ".getYear()," + content + ".getMonth()," + content + ".getDay())";
//			case "others":
//				return something;
			default:
				return "ERROR!";
		}
	}
	
	public static String androidInputWidgetContentGetter(Type type, String variable){
		String widget = androidPrimitiveTypeToWriteWidget(type);
		switch(widget){
			case "EditText":
				return variable + ".getText().toString()";
			case "DatePicker":
				return variable +".getYear()," + variable + ".getMonth()," + variable + ".getDayOfMonth()";
//			case "others":
//				return something;
			default:
				return "ERROR!";
		}
	}

}
