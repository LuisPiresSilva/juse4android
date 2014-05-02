/*
 * J-USE - Java prototyping for the UML based specification environment (USE)
 * Copyright (C) 2012 Fernando Brito e Abrey, QUASAR research group
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.quasar.juse.api.implementation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.tzi.use.uml.ocl.type.*;
import org.tzi.use.uml.ocl.type.TupleType.Part;

/***********************************************************
 * @author fba 26 de Mai de 2013
 * 
 ***********************************************************/
public abstract class JavaTypes
{
	private static Set<Integer>	tupleTypesCardinalities = new HashSet<Integer>();

	/***********************************************************
	* @return
	***********************************************************/
	public static Set<Integer> getTupleTypesCardinalities()
	{
		return tupleTypesCardinalities;
	}
	
	/***********************************************************
	* @param collectionType
	* @return
	***********************************************************/
	public static Type oclCollectionInnerType(CollectionType collectionType)
	{
		return collectionType.elemType();
	}
	
	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String javaInterfaceType(Type oclType)
	{
		if (oclType.isOrderedSet())
			return "SortedSet<" + javaInterfaceType(((OrderedSetType) oclType).elemType()) + ">";
		if (oclType.isSet())
			return "Set<" + javaInterfaceType(((SetType) oclType).elemType()) + ">";
		if (oclType.isSequence())
			return "Queue<" + javaInterfaceType(((SequenceType) oclType).elemType()) + ">";
		if (oclType.isBag())
			return "List<" + javaInterfaceType(((BagType) oclType).elemType()) + ">";		
		if (oclType.isTupleType(true))
			return javaTupleType((TupleType) oclType);

		return javaPrimitiveType(oclType);
	}

	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String javaImplementationType(Type oclType)
	{
		if (oclType.isSet())
			return "HashSet<" + javaImplementationType(((SetType) oclType).elemType()) + ">";		
		if (oclType.isOrderedSet())
			return "TreeSet<" + javaImplementationType(((OrderedSetType) oclType).elemType()) + ">";
		if (oclType.isSequence())
			return "ArrayDeque<" + javaImplementationType(((SequenceType) oclType).elemType()) + ">";
		if (oclType.isBag())
			return "ArrayList<" + javaImplementationType(((BagType) oclType).elemType()) + ">";
		if (oclType.isTupleType(true))
			return javaTupleType((TupleType) oclType);

		return javaPrimitiveType(oclType);
	}

	/***********************************************************
	* @param oclType
	* @return
	***********************************************************/
	private static String javaTupleType(TupleType tupleType)
	{
		ArrayList<Part> tupleParts = new ArrayList<Part>(tupleType.getParts().values());
		tupleTypesCardinalities.add(tupleParts.size());
		
		String result = "Tuple" + tupleParts.size() + "<";
		for (int i = 0; i < tupleParts.size(); i++)
		{
			result += javaInterfaceType(tupleParts.get(i).type());
			if (i < tupleParts.size() - 1)
				result += ", ";
		}
		result += ">";
		return result;
	}

	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String javaPrimitiveType(Type oclType)
	{
		if (oclType.isInteger())
			return "int";
		if (oclType.isReal())
			return "double";
		if (oclType.isBoolean())
			return "boolean";
		if (oclType.isString())
			return "String";
		if (oclType.isEnum())
			return oclType.toString(); // --> DB4O Enum problem
		if (oclType.isObjectType())
			if((oclType.toString().equals("Date")))
				return "Date";
			else
				return oclType.toString();
		if (oclType.isTrueObjectType())
			return oclType.toString();
		if (oclType.isTrueOclAny())
			return "Object";
		if (oclType.isVoidType())
			return "void";
		if (oclType.isDate())
			return "Date";

		return "ERROR!";
	}

	/***********************************************************
	 * @param oclType
	 * @return
	 ***********************************************************/
	public static String javaDummyValue(Type oclType)
	{
		if (oclType.isNumber())
			return "-1";
		if (oclType.isInteger())
			return "-1";
		if (oclType.isReal())
			return "-1.0";
		if (oclType.isBoolean())
			return "true";
		if (oclType.isString())
			return "null";
		if (oclType.isEnum())
			return "null";
		if (oclType.isCollection(true))
			return "null";
		if (oclType.isTrueCollection())
			return "null";
		if (oclType.isSet())
			return "null";
		if (oclType.isTrueSet())
			return "null";
		if (oclType.isSequence())
			return "null";
		if (oclType.isTrueSequence())
			return "null";
		if (oclType.isOrderedSet())
			return "null";
		if (oclType.isTrueOrderedSet())
			return "null";
		if (oclType.isBag())
			return "null";
		if (oclType.isTrueBag())
			return "null";
		if (oclType.isInstantiableCollection())
			return "null";
		if (oclType.isObjectType())
			return "null";
		if (oclType.isTrueObjectType())
			return "null";
		if (oclType.isTrueOclAny())
			return "null";
		if (oclType.isTupleType(true))
			return "null";
		if (oclType.isVoidType())
			return "";
		if (oclType.isDate())
			return "null";

		return "ERROR!";
	}

	private String defaultValueType(Type oclType){
		
		if (oclType.isInteger())
			return "0";
		else if (oclType.isReal())
			return "0.0";
		else if (oclType.isBoolean())
			return "false";
		else if (oclType.isString())
			return "\"\"";
		else if (oclType.isEnum()){
			return oclType.toString() + ".valueOf(" + oclType.toString() + ".values()[0].toString())";
		}
		else if (oclType.isObjectType())
			if((oclType.toString().equals("Date")))
				return "";
			else
				return oclType.toString();
		else if (oclType.isTrueObjectType())
			return oclType.toString();
		else if (oclType.isTrueOclAny())
			return "null";
		else if (oclType.isVoidType())
			return "null";
		else if (oclType.isDate())
			return "";
		else
			return "null";
	}
	
	/***********************************************************
	 * @param oclTypes
	 * @return
	 ***********************************************************/
	public static Set<String> javaImportDeclarations(Set<Type> oclTypes)
	{
		Set<String> result = new HashSet<String>();

		// compulsory because of "allInstances()"
		result.add("import java.util.Set;");

		for (Type oclType : oclTypes)
		{
			if (oclType != null)
			{
				if (oclType.isSequence())
				{
					result.add("import java.util.Queue;");
					// result.add("import java.util.ArrayDeque;");
				}
				if (oclType.isOrderedSet())
				{
					result.add("import java.util.SortedSet;");
					result.add("import java.util.TreeSet;");
				}
				if (oclType.isBag())
				{
					result.add("import java.util.List;");
					result.add("import java.util.ArrayList;");
				}
				if (oclType.isSet())
				{
					result.add("import java.util.HashSet;");
				}
				if (oclType.isDate() || (oclType.isObjectType() && oclType.toString().equals("Date")))
				{
					result.add("import java.util.Date;");
				}
			}
		}
		return result;
	}

}