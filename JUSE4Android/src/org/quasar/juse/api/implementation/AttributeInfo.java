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
import java.util.List;

import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.ocl.type.Type;

public class AttributeInfo
{
	private AssociationKind kind;
	private String	name;
	private Type	type;

	/***********************************************************
	* @param kind
	* @param name
	* @param type
	***********************************************************/
	public AttributeInfo(AssociationKind kind, String name, Type type)
	{
		this.kind = kind;
		this.name = name;
		this.type = type;
	}

	/***********************************************************
	* @return
	***********************************************************/
	public AssociationKind getKind()
	{
		return kind;
	}
	
	/***********************************************************
	* @return
	***********************************************************/
	public String getName()
	{
		return name;
	}
	
	/***********************************************************
	* @return
	***********************************************************/
	public Type getType()
	{
		return type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "AtributeInfo(" + type + ", " + name + ", " + type + ")";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		if (obj instanceof AttributeInfo)
		{
			AttributeInfo other = (AttributeInfo) obj;
			return name.equals(other.name) && type.equals(other.type);
		}
		return false;
	}
	
	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public static List<AttributeInfo> getAttributesInfo(MClass theClass)
	{
		List<AttributeInfo> result = new ArrayList<AttributeInfo>();
		ModelUtilities util = new ModelUtilities(theClass.model());
		
		for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
		{
			MClass sourceClass = ai.getSourceAE().cls();
			MClass targetClass = ai.getTargetAE().cls();
			String sourceName = ai.getSourceAE().name();
			String targetName = ai.getTargetAE().name();		
			Type sourceType = ai.getSourceAE().getType();
			Type targetType = ai.getTargetAE().getType();	
			
			switch (ai.getKind())
			{
				case ONE2ONE:
					if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
						if(theClass == sourceClass && sourceClass != targetClass && ai.getSourceAE().getAnnotation("holder") != null)
							result.add(new AttributeInfo(ai.getKind(), targetName, targetType));
						if(theClass == targetClass && ai.getTargetAE().getAnnotation("holder") != null)
							result.add(new AttributeInfo(ai.getKind(), sourceName, sourceType));
					}else{
						if (theClass == sourceClass && sourceClass != targetClass 
										&& theClass == util.lessComplexClass(sourceClass, targetClass))
							result.add(new AttributeInfo(ai.getKind(), targetName, targetClass.type()));
						if (theClass == targetClass
										&& theClass == util.lessComplexClass(sourceClass, targetClass))	
							result.add(new AttributeInfo(ai.getKind(), sourceName, sourceClass.type()));
					}
					break;
				case ONE2MANY:
					if (theClass == sourceClass && sourceClass != targetClass && ai.getSourceAE().isCollection())
						result.add(new AttributeInfo(ai.getKind(), targetName, targetClass.type()));
					if (theClass == targetClass && ai.getTargetAE().isCollection())					
						result.add(new AttributeInfo(ai.getKind(), sourceName, sourceClass.type()));
					break;
				case MANY2MANY:
					if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
						if(theClass == sourceClass && sourceClass != targetClass && ai.getSourceAE().getAnnotation("holder") != null)
							result.add(new AttributeInfo(ai.getKind(), targetName, targetType));
						if(theClass == targetClass && ai.getTargetAE().getAnnotation("holder") != null)
							result.add(new AttributeInfo(ai.getKind(), sourceName, sourceType));
					}else{
						if (theClass == sourceClass && sourceClass != targetClass
										&& theClass == util.lessComplexClass(sourceClass, targetClass))
							result.add(new AttributeInfo(ai.getKind(), targetName, targetType));
						if (theClass == targetClass
										&& theClass == util.lessComplexClass(sourceClass, targetClass))
							result.add(new AttributeInfo(ai.getKind(), sourceName, sourceType));
					}
					break;
				case ASSOCIATIVE2MEMBER:
					result.add(new AttributeInfo(ai.getKind(), sourceName, sourceClass.type()));
					break;
				case MEMBER2ASSOCIATIVE:
					break;
				case MEMBER2MEMBER:
					break;
				default:
					System.out.println("ERROR: " + ai);
			}
		}
		
		for (MAttribute attribute : theClass.attributes())
			result.add(new AttributeInfo(AssociationKind.NONE, attribute.name(), attribute.type()));
		
		return result;
	}
	
	/***********************************************************
	 * @param theClass
	 * @return
	 ***********************************************************/
	public static List<AttributeInfo> getAssociativeToMemberAttributesInfo(MClass theClass)
	{
		List<AttributeInfo> result = new ArrayList<AttributeInfo>();
		
		for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
		{
			MClass sourceClass = ai.getSourceAE().cls();
			String sourceName = ai.getSourceAE().name();

			switch (ai.getKind())
			{
				case ONE2ONE:
					break;
				case ONE2MANY:
					break;
				case MANY2MANY:
					break;
				case ASSOCIATIVE2MEMBER:
					result.add(new AttributeInfo(ai.getKind(), sourceName, sourceClass.type()));
					break;
				case MEMBER2ASSOCIATIVE:
					break;
				case MEMBER2MEMBER:
					break;
				default:
					System.out.println("ERROR: " + ai);
			}
		}
		
		
		return result;
	}

}