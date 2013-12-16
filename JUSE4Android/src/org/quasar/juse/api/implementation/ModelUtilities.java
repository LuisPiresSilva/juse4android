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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.tzi.use.uml.mm.MAssociationClass;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MOperation;
import org.tzi.use.uml.ocl.expr.VarDecl;
import org.tzi.use.uml.ocl.type.Type;

/***********************************************************
 * @author fba 6 de Abr de 2012
 * 
 ***********************************************************/
public class ModelUtilities
{
	private MModel	model;

	/***********************************************************
	 * @param model
	 ***********************************************************/
	public ModelUtilities(MModel model)
	{
		this.model = model;
	}

	/***********************************************************
	* @param list of attributes to compare
	* @param map of the annotations
	* @return a list with the attributes present in the annotation
	***********************************************************/
	public static List<MAttribute> annotationValuesToAttribute(List<MAttribute> listAttributes, Map<String, String> map){
		List<MAttribute> finalList = new ArrayList<MAttribute>();
		for(String x : map.keySet())
			for(MAttribute y : listAttributes)
				if(y.name().equals(x))
					finalList.add(y);
		return finalList;
	}
	
	/***********************************************************
	* @param list of attributes to compare
	* @param map of the annotations
	* @return a list with the attributes present in the annotation in the order detailed in the annotation
	***********************************************************/
	public static List<MAttribute> annotationValuesToAttributeOrdered(List<MAttribute> listAttributes, Map<String, String> map){
		List<MAttribute> finalList = new ArrayList<MAttribute>();
		List<Entry<String, String>> entryList = new ArrayList<Entry<String, String>>();
		entryList.addAll(map.entrySet());
		while(!entryList.isEmpty()){
			boolean start = false;
			int index = 0;
			int lowest = -1;
			for(Entry<String, String> x : entryList){
				try{
					if(!start || lowest > Integer.parseInt(x.getValue())){
						lowest = Integer.parseInt(x.getValue());
						index = entryList.indexOf(x);
						start = true;
					}
				}catch(NumberFormatException e){
					System.err.println("NumberFormatException:\nAttribute: " + x.getKey());
					e.printStackTrace();
					throw new NumberFormatException();
				}
			}
			for(MAttribute y : listAttributes)
				if(y.name().equals(entryList.get(index).getKey()))
					finalList.add(y);
			entryList.remove(index);
		}
		return finalList;
	}
	/***********************************************************
	* @return
	***********************************************************/
	public int numberClasses()
	{
		return model.classes().size();
	}
	
	/***********************************************************
	* @return
	***********************************************************/
	public int numberAttributes()
	{
		int result = 0;
		for (MClass aClass: model.classes())
			result+= AttributeInfo.getAttributesInfo(aClass).size();
		return result;
	}

	/***********************************************************
	* @return
	***********************************************************/
	public int numberOperations()
	{
		int result = 0;
		for (MClass aClass: model.classes())
		{			
			if (AttributeInfo.getAttributesInfo(aClass).size() > 0)
				result++;	// DefaultConstructor;

			result++;	// Parameterized Constructor

			result+= 2 * aClass.attributes().size(); 	// Getters & Setters for "native" attributes

			if (aClass instanceof MAssociationClass)
				result += 4;	// Each association class adds 4 more navigators (towards the 2 members, in both directions)
			
			result += aClass.operations().size();	// Operations specified in OCL / SOIL
		}
		
		result += 4* model.associations().size();	// One getter and one setter for each navigation direction

		return result;
	}
	

	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public Set<Type> getClassOutboundDependencies(MClass theClass)
	{
		Set<Type> result = new HashSet<Type>();

		for (MClass p : theClass.parents())
		{
			result.addAll(getClassOutboundDependencies(p));
			result.add(p.type());
		}

		for (MAttribute a : theClass.attributes())
			if (a.type().isObjectType())
				result.add(a.type());

		for (MAttribute a : theClass.attributes())
			if (a.type().isObjectType())
				result.add(a.type());

		if (theClass instanceof MAssociationClass)
			for (MClass member : ((MAssociationClass) theClass).associatedClasses())
				result.add(member.type());

		for (MOperation op : theClass.allOperations())
		{
			if (op.hasResultType() && op.resultType().isObjectType())
				result.add(op.resultType());
			for (VarDecl v : op.paramList())
				if (v.type().isObjectType())
					result.add(v.type());
		}

		result.remove(null);

		return result;
	}

	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public Set<Type> getClassInboundDependencies(MClass theClass)
	{
		Set<Type> result = new HashSet<Type>();

		for (MClass other : model.classes())
			if (other != theClass && getClassOutboundDependencies(other).contains(theClass.type()))
				result.add(other.type());

		return result;
	}

	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public int outgoingCoupling(MClass theClass)
	{
		Set<Type> tmp = getClassOutboundDependencies(theClass);
		tmp.remove(theClass.type());
		return tmp.size();
	}

	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public int incomingCoupling(MClass theClass)
	{
		Set<Type> tmp = getClassInboundDependencies(theClass);
		tmp.remove(theClass.type());
		return tmp.size();
	}

	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public double typePriorityFactor(MClass theClass)
	{
		return outgoingCoupling(theClass) == 0 ? Double.POSITIVE_INFINITY : incomingCoupling(theClass)
						/ outgoingCoupling(theClass);
	}

	/***********************************************************
	 * @param classes
	 * @return
	 ***********************************************************/
	public MClass lessComplexClass(MClass... classes)
	{
		MClass lessComplex = classes[0];
		for (MClass aClass : classes)
			if (typePriorityFactor(aClass) == typePriorityFactor(lessComplex))
				if (outgoingCoupling(aClass) == outgoingCoupling(lessComplex))
					if (incomingCoupling(aClass) == incomingCoupling(lessComplex))
						if (aClass.allAttributes().size() == lessComplex.allAttributes().size())
							if (aClass.allOperations().size() == lessComplex.allOperations().size())
								lessComplex = aClass;
							else
								lessComplex = aClass.allOperations().size() < lessComplex.allOperations().size() ? aClass
												: lessComplex;
						else
							lessComplex = aClass.allAttributes().size() < lessComplex.allAttributes().size() ? aClass
											: lessComplex;
					else
						lessComplex = incomingCoupling(aClass) > incomingCoupling(lessComplex) ? aClass : lessComplex;
				else
					lessComplex = outgoingCoupling(aClass) < outgoingCoupling(lessComplex) ? aClass : lessComplex;
			else
				lessComplex = typePriorityFactor(aClass) > typePriorityFactor(lessComplex) ? aClass : lessComplex;
		return lessComplex;
	}

	/***********************************************************
	 * @param classes
	 * @return
	 ***********************************************************/
	public MClass moreComplexClass(MClass... classes)
	{
		MClass lessComplex = classes[0];
		for (MClass aClass : classes)
			if (typePriorityFactor(aClass) == typePriorityFactor(lessComplex))
				if (outgoingCoupling(aClass) == outgoingCoupling(lessComplex))
					if (incomingCoupling(aClass) == incomingCoupling(lessComplex))
						if (aClass.allAttributes().size() == lessComplex.allAttributes().size())
							if (aClass.allOperations().size() == lessComplex.allOperations().size())
								lessComplex = aClass;
							else
								lessComplex = aClass.allOperations().size() > lessComplex.allOperations().size() ? aClass
												: lessComplex;
						else
							lessComplex = aClass.allAttributes().size() > lessComplex.allAttributes().size() ? aClass
											: lessComplex;
					else
						lessComplex = incomingCoupling(aClass) < incomingCoupling(lessComplex) ? aClass : lessComplex;
				else
					lessComplex = outgoingCoupling(aClass) > outgoingCoupling(lessComplex) ? aClass : lessComplex;
			else
				lessComplex = typePriorityFactor(aClass) < typePriorityFactor(lessComplex) ? aClass : lessComplex;
		return lessComplex;
	}

	/***********************************************************
	* 
	***********************************************************/
	public void printModelUtilities()
	{
		for (MClass cls : model.classes())
		{
			System.out.println(cls + " outbound dependencies: " + getClassOutboundDependencies(cls) + outgoingCoupling(cls));
			System.out.println(cls + " inbound dependencies: " + getClassInboundDependencies(cls) + incomingCoupling(cls));
		}
		System.out.println("Less compless class: " + lessComplexClass(model.classes().toArray(new MClass[0])));
		System.out.println("More compless class: " + moreComplexClass(model.classes().toArray(new MClass[0])));
	}
}