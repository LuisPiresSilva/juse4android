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
package org.quasar.use2android.api.implementation;

import java.text.DateFormat; 
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.AssociationKind;
import org.quasar.juse.api.implementation.AttributeInfo;
import org.quasar.juse.api.implementation.JavaTypes;
import org.quasar.juse.api.implementation.ModelUtilities;
import org.tzi.use.uml.mm.*;
import org.tzi.use.uml.ocl.expr.*;
import org.tzi.use.uml.ocl.type.*;
import org.tzi.use.util.StringUtil;

public class AndroidBusinessVisitor extends BusinessVisitor
{
	private MModel			model;
	private String			author;
	private String			basePackageName;
	private String			businessLayerName;
	private String			persistenceLayerName;
	private String			presentationLayerName;
	private String			utilsLayerName;
	private ModelUtilities	util;

	/***********************************************************
	 * @param model
	 *            The corresponding to the compiled specification
	 * @param author
	 *            The author of the specification
	 * @param basePackageName
	 *            Full name of the base package where the code of the generated Java prototype will be placed
	 * @param businessLayerName
	 *            Relative name of the layer package where the source code for the business layer is to be placed
	 * @param persistenceLayerName
	 *            Relative name of the layer package where the source code for the persistence layer is to be placed
	 * @param presentationLayerName
	 *            Relative name of the layer package where the source code for the presentation layer is to be placed
	 ***********************************************************/
	public AndroidBusinessVisitor(MModel model, String author, String basePackageName, String businessLayerName,
					String persistenceLayerName, String presentationLayerName, String utilsLayerName)
	{
		this.model = model;
		this.author = author;
		this.basePackageName = basePackageName;
		this.businessLayerName = businessLayerName;
		this.persistenceLayerName = persistenceLayerName;
		this.presentationLayerName = presentationLayerName;
		this.utilsLayerName = utilsLayerName;
		
		//adds the ID attribute
		ModelFactory a = new ModelFactory();
		for(MClass c : this.model.classes())
			if(c.parents().isEmpty())//if is not subClass
				try {
					c.addAttribute(a.createAttribute("ID", TypeFactory.mkInteger()));
				} catch (MInvalidModelException e) {
					e.printStackTrace();
				}
		
		this.util = new ModelUtilities(model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printFileHeader(java.lang.String)
	 */
	@Override
	public void printFileHeader(String typeName, String layerName)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		// get current date time with Date()
		Date date = new Date();

		println("/**********************************************************************");
		println("* Filename: " + typeName + ".java");
		println("* Created: " + dateFormat.format(date));
		println("* @author " + author);
		println("**********************************************************************/");
		println("package " + basePackageName + "." + layerName + ";");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printEnumType(org.tzi.use.uml.ocl.type.EnumType)
	 */
	@Override
	public void printEnumType(EnumType t, String layerName)
	{
		printFileHeader(t.name(), layerName);
		// visitAnnotations(t);

		println("public enum " + t.name());
		println("{");
		incIndent();
		println(StringUtil.fmtSeq(t.literals(), ", "));
		decIndent();
		println("}");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printAttributes(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAttributes(MClass theClass)
	{
		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			if (attribute.getType().isSet() || attribute.getType().isOrderedSet())
				println("private " + JavaTypes.javaInterfaceType(attribute.getType()) + " " + attribute.getName() + " = "
								+ " new " + JavaTypes.javaImplementationType(attribute.getType()) + "();");
			else{
				if(isSuperClass(theClass)){
					if(attribute.getType().isEnum())
						println("private String " + attribute.getName() + ";");
					else if(attribute.getName().equals("ID"))
						println("protected " + JavaTypes.javaInterfaceType(attribute.getType()) + " " + attribute.getName() + ";");
					else
						println("private " + JavaTypes.javaInterfaceType(attribute.getType()) + " " + attribute.getName() + ";");
				}else
					if(attribute.getType().isEnum())
						println("private String " + attribute.getName() + ";");
					else
						println("private " + JavaTypes.javaInterfaceType(attribute.getType()) + " " + attribute.getName() + ";");
			}	
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printClassHeader(MClass theClass, String layerName)
	{
		printFileHeader(theClass.name(), layerName);
		// visitAnnotations(e);

		printImports(theClass);

		print("public ");
		if (theClass.isAbstract())
			print("abstract ");
		print("class " + theClass.name());

		Set<MClass> parents = theClass.parents();
		if (!parents.isEmpty())
			print(" extends " + StringUtil.fmtSeq(parents.iterator(), ","));
		
		println(" implements Comparable<Object>");
		println("{");
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printImports(MClass theClass)
	{
		println("import " + basePackageName + "." + persistenceLayerName + ".Database;");
		println("import " + basePackageName + "." + utilsLayerName + ".Utils;");
		println("import " + basePackageName + "." + utilsLayerName + ".ModelContracts;");
		println();

		Set<Type> classTypes = new HashSet<Type>();

		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			if (attribute.getKind() != AssociationKind.ASSOCIATIVE2MEMBER)
				classTypes.add(attribute.getType());

		for (AssociationInfo association : AssociationInfo.getAssociationsInfo(theClass))
			if (association.getKind() != AssociationKind.ASSOCIATIVE2MEMBER)
				classTypes.add(association.getTargetAE().getType());

		for (MOperation operation : theClass.allOperations())
		{
			classTypes.add(operation.resultType());
			for (VarDecl v : operation.paramList())
				classTypes.add(v.type());
		}
		
		for (MAttribute x : theClass.allAttributes())
			if(!classTypes.contains(x.type()))
				classTypes.add(x.type());

		// System.out.println("-------------------------" + theClass.name()
		// + "..........................................................");
		// for (Type oclType : classTypes)
		// System.out.println(oclType);
		// System.out.println();

		Set<String> imports = JavaTypes.javaImportDeclarations(classTypes);
		imports.add("import java.io.Serializable;");
		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
	}

	/***********************************************************
	* @param theClass whose root we want
	* @return the root parent of the class passed as parameter
	***********************************************************/
	private MClass baseAncestor(MClass theClass)
	{
		return theClass.parents().isEmpty() ? theClass : baseAncestor(theClass.parents().iterator().next());
	}
	
	/***********************************************************
	* @param theClass to check
	* @return true if is subclass, false if not
	***********************************************************/
	private boolean isSubClass(MClass theClass)
	{
		for(MClass x : model.classes())
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
		for(MClass x : model.classes())
			if( (!theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass))//middle super
				|| (theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass)) )//top super
				return true;
		return false;
	}
	
	/***********************************************************
	* @param theClass to check
	* @return returns a list with the indirect associations
	***********************************************************/
	public List<AssociationInfo> getIndirectAssociations(MClass theClass){
		List<AssociationInfo> allAssociations = new ArrayList<AssociationInfo>();
		for(MClass parent : theClass.allParents())
			allAssociations.addAll(AssociationInfo.getAssociationsInfo(parent));
		List<AssociationInfo> directAssociations = new ArrayList<AssociationInfo>(AssociationInfo.getAssociationsInfo(theClass));
		allAssociations.removeAll(directAssociations);
		return allAssociations;
	}
	
//	/***********************************************************
//	* @param list of attributes to compare
//	* @param keySet of the annotation
//	* @return a list with the attributes present in the annotation
//	***********************************************************/
//	private List<MAttribute> annotationValuesToAttribute(List<MAttribute> listAttributes, Set<String> listKeys){
//		List<MAttribute> finalList = new ArrayList<MAttribute>();
//		for(String x : listKeys)
//			for(MAttribute y : listAttributes)
//				if(y.name().equals(x))
//					finalList.add(y);
//		return finalList;
//	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printAllInstances(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAllInstances(MClass theClass)
	{
		println();
		println("/***********************************************************");
		println("* @return all instances of class " + theClass.name());
		println("***********************************************************/");
		print("public static Set<" + baseAncestor(theClass).name() + "> allInstances()");
		println("{");
		incIndent();
		println("return Database.allInstances(" + theClass.name() + ".class);");
		decIndent();
		println("}");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printDefaultConstructors(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printDefaultConstructor(MClass theClass)
	{
		println("/**********************************************************************");
		println("* Default constructor");
		println("**********************************************************************/");
		println("public " + theClass.name() + "()");
		println("{");
		incIndent();
		if (theClass.allParents().size() > 0)
			println("super();");
		
		decIndent();
		println("}");
		println();
	}

	@Override
	public void printParameterizedAttributeConstructor(MClass theClass)
	{
		List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
		for (MClass theParentClass : theClass.allParents()){
			List<AttributeInfo> inheritedAttributes_temp = new ArrayList<AttributeInfo>();
			for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
				inheritedAttributes_temp.add(attribute);
			inheritedAttributes.addAll(0, inheritedAttributes_temp);
		}

		if (inheritedAttributes.size() + theClass.attributes().size() <= 1)
			return;
		
		println("/**********************************************************************");
		println("* Parameterized Attribute constructor");
		for (AttributeInfo attribute : inheritedAttributes)
			if(attribute.getKind().toString().equals(AssociationKind.NONE.toString()) && !attribute.getName().equals("ID"))
				println("* @param " + attribute.getName() + " the " + attribute.getName() + " to initialize (inherited)");
		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			if((attribute.getKind() == AssociationKind.NONE || attribute.getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && !attribute.getName().equals("ID"))
				println("* @param " + attribute.getName() + " the " + attribute.getName() + " to initialize");
		println("**********************************************************************/");

		print("public " + theClass.name() + "(");
		for (int i = 0; i < inheritedAttributes.size(); i++)
		{
			if((inheritedAttributes.get(i).getKind() == AssociationKind.NONE || inheritedAttributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && !inheritedAttributes.get(i).getName().equals("ID")){
				print(JavaTypes.javaInterfaceType(inheritedAttributes.get(i).getType()) + " "
							+ inheritedAttributes.get(i).getName());
				if (i < inheritedAttributes.size() - 1)
					print(", ");
			}
		}

		List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
		if (inheritedAttributes.size() > 0 && attributes.size() > 0)
			print(", ");
		for (int i = 0; i < attributes.size(); i++)
		{
			if((attributes.get(i).getKind() == AssociationKind.NONE || attributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && !attributes.get(i).getName().equals("ID")){
				print(JavaTypes.javaInterfaceType(attributes.get(i).getType()) + " " + attributes.get(i).getName());
				if (i < attributes.size() - 1)
					print(", ");
			}
		}
		println(")");
		println("{");
		incIndent();
		if (inheritedAttributes.size() > 0)
		{
			print("super(");
			for (int i = 0; i < inheritedAttributes.size(); i++)
			{
				if((inheritedAttributes.get(i).getKind() == AssociationKind.NONE || inheritedAttributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && !inheritedAttributes.get(i).getName().equals("ID")){
					print(inheritedAttributes.get(i).getName());
					if (i < inheritedAttributes.size() - 1)
						print(", ");
				}
			}
			println(");");
		}
		
		if(isSubClass(theClass))
			printIDgeneratorCaller(theClass, "ID");
		
		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			if((attribute.getKind() == AssociationKind.NONE || attribute.getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && attribute.getName().equals("ID"))
				printIDgeneratorCaller(theClass, attribute.getName());
			else
				if(attribute.getKind() == AssociationKind.NONE || attribute.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
					if(attribute.getType().isEnum())
						println("this." + attribute.getName() + " = " + attribute.getName() + ".name();");
					else
						println("this." + attribute.getName() + " = " + attribute.getName() + ";");

		
		decIndent();
		println("}");
		println();
	}
	
	private void printIDgeneratorCaller(MClass theClass, String ID){
		print("this." + ID + " = Utils.generateMD5Id(new Object[]{");

//		A razão para este pedaço de codigo (herança de atributos) é simplesmente pelo facto
//		de a ordem de chamada ser importante ao usar apenas o allAttributes a ordem ficava inversa
//		no product_X visto este ter um atributo unico e o seu filho (product_XX) não, fazendo 
//		a chamada do gerador com os 2 atributos ao contrario gerando assim diferentes ids
//		--------------*************** CODIGO NOVO - START  ******************* ------------------
		List<AttributeInfo> inheritedUniqueAttributes = new ArrayList<AttributeInfo>();
		for (MClass theParentClass : theClass.allParents()){
			if(theParentClass.isAnnotated())
				if(theParentClass instanceof MAssociationClass)
					inheritedUniqueAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrderedWithAssociative2Member(AttributeInfo.getAttributesInfo(theParentClass), null));
				else if(theParentClass.getAnnotation("unique") != null)
					inheritedUniqueAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrderedWithAssociative2Member(AttributeInfo.getAttributesInfo(theParentClass), theParentClass.getAnnotation("unique").getValues()));
		}
		List<AttributeInfo> uniqueAttributes = new ArrayList<AttributeInfo>();
		uniqueAttributes.addAll(inheritedUniqueAttributes);
		if(theClass.isAnnotated())
			if(theClass instanceof MAssociationClass)
				uniqueAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrderedWithAssociative2Member(AttributeInfo.getAttributesInfo(theClass), null));
			else if(theClass.getAnnotation("unique") != null)
				uniqueAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrderedWithAssociative2Member(AttributeInfo.getAttributesInfo(theClass), theClass.getAnnotation("unique").getValues()));

//		so o chamamento do metodo e que e novo
//		--------------*************** CODIGO NOVO - END  ******************* ------------------
		print("\"" + baseAncestor(theClass) + "\",");
		for (int i = 0; i < uniqueAttributes.size(); ++i){
			if(inheritedUniqueAttributes.contains(uniqueAttributes.get(i)))
				if(uniqueAttributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
					print(uniqueAttributes.get(i).getName() + "() != null ? " + uniqueAttributes.get(i).getName() + "().ID() : null");
				else
					print(uniqueAttributes.get(i).getName() + "()");
			else
				if(uniqueAttributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
					print(uniqueAttributes.get(i).getName() + " != null ? " + uniqueAttributes.get(i).getName() + ".ID() : null");
				else
					print(uniqueAttributes.get(i).getName());
			if(i < uniqueAttributes.size() - 1)
				print(", ");
				
		}
		println("});");
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printAssociativeConstructors(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAssociativeConstructor(MClass theClass)
	{
		println("/**********************************************************************");
		println("* Associative constructor");
		for (AttributeInfo attribute : AttributeInfo.getAssociativeToMemberAttributesInfo(theClass))
			println("* @param " + attribute.getName() + " the " + attribute.getName() + " to initialize");
		println("**********************************************************************/");

		print("public " + theClass.name() + "(");

		List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
		boolean first = true;
		for (int i = 0; i < attributes.size(); i++)
		{
			if (attributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
			{
				print(JavaTypes.javaInterfaceType(attributes.get(i).getType()) + " " + attributes.get(i).getName());
				if (first)
				{
					print(", ");
					first = false;
				}
			}
		}
		println(")");
		println("{");
		incIndent();
		
		for (AttributeInfo attribute : AttributeInfo.getAssociativeToMemberAttributesInfo(theClass))
			println("this." + attribute.getName() + " = " + attribute.getName() + ";");

		
		List<AttributeInfo> attribute = new ArrayList<AttributeInfo>(AttributeInfo.getAssociativeToMemberAttributesInfo(theClass));
		print("this.ID = Utils.generateMD5Id(new Object[]{");
		for(int i = 0;i < attribute.size();++i)
//			if(attribute.get(i).isAnnotated() && attribute.get(i).getAnnotation("unique") != null)
				if(i < attribute.size() - 1)
					print(attribute.get(i).getName() + " != null ? " + attribute.get(i).getName() + ".ID() : null" + ",");
				else
					print(attribute.get(i).getName() + " != null ? " + attribute.get(i).getName() + ".ID() : null");
		println("});");
		
		decIndent();
		println("}");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printParameterizedConstructors(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printParameterizedConstructor(MClass theClass)
	{
		List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
		for (MClass theParentClass : theClass.allParents()){
			List<AttributeInfo> inheritedAttributes_temp = new ArrayList<AttributeInfo>();
			for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
				inheritedAttributes_temp.add(attribute);
			inheritedAttributes.addAll(0, inheritedAttributes_temp);
		}

		if (inheritedAttributes.size() + theClass.attributes().size() == 0)
			return;

		println("/**********************************************************************");
		println("* Parameterized constructor");
		for (AttributeInfo attribute : inheritedAttributes)
			println("* @param " + attribute.getName() + " the " + attribute.getName() + " to initialize (inherited)");
		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			println("* @param " + attribute.getName() + " the " + attribute.getName() + " to initialize");
		println("**********************************************************************/");

		print("public " + theClass.name() + "(");
		for (int i = 0; i < inheritedAttributes.size(); i++)
		{
			print(JavaTypes.javaInterfaceType(inheritedAttributes.get(i).getType()) + " "
							+ inheritedAttributes.get(i).getName());
			if (i < inheritedAttributes.size() - 1)
				print(", ");
		}

		List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
		if (inheritedAttributes.size() > 0 && attributes.size() > 0)
			print(", ");
		for (int i = 0; i < attributes.size(); i++)
		{
			print(JavaTypes.javaInterfaceType(attributes.get(i).getType()) + " " + attributes.get(i).getName());
			if (i < attributes.size() - 1)
				print(", ");
		}
		println(")");
		println("{");
		incIndent();
		if (inheritedAttributes.size() > 0)
		{
			print("super(");
			for (int i = 0; i < inheritedAttributes.size(); i++)
			{
				print(inheritedAttributes.get(i).getName());
				if (i < inheritedAttributes.size() - 1)
					print(", ");
			}
			println(");");
		}
		for (AttributeInfo attribute : AttributeInfo.getAttributesInfo(theClass))
			if(attribute.getType().isEnum())
				println("this." + attribute.getName() + " = " + attribute.getName() + ".name();");
			else
				println("this." + attribute.getName() + " = " + attribute.getName() + ";");

		decIndent();
		println("}");
		println();
	}

	/***********************************************************
	 * @param theClass
	 *            The class where the arribute belongs to
	 * @param currentAttribute
	 *            The current attribute
	 * @param tag
	 *            {"getter" | "setter"}
	 ***********************************************************/
	public void printHeaderBasicGettersSetters(MClass theClass, AttributeInfo currentAttribute, String tag)
	{
		println("/**********************************************************************");
		switch (currentAttribute.getKind())
		{
			case NONE:
				println("* " + "Standard attribute " + tag);
				break;
			case ONE2ONE:
				println("* " + currentAttribute.getKind() + " " + tag + " for " + theClass + "[1] <-> "
								+ currentAttribute.getType() + "[1]"
								+ (currentAttribute.getType().isOrderedSet() ? " ordered" : ""));
				break;
			case ONE2MANY:
				println("* " + currentAttribute.getKind() + " " + tag + " for " + theClass + "[*] <-> "
								+ currentAttribute.getType() + "[1]"
								+ (currentAttribute.getType().isOrderedSet() ? " ordered" : ""));
				break;
			case MANY2MANY:
				println("* " + currentAttribute.getKind() + " " + tag + " for " + theClass + "[*] <-> "
								+ currentAttribute.getType() + "[*]"
								+ (currentAttribute.getType().isOrderedSet() ? " ordered" : ""));
				break;
			case ASSOCIATIVE2MEMBER:
				println("* " + currentAttribute.getKind() + " " + tag + " for " + theClass + "[*] <-> "
								+ currentAttribute.getType() + "[1]"
								+ (currentAttribute.getType().isOrderedSet() ? " ordered" : ""));
				break;
			default:
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printBasicGettersSetters(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printBasicGettersSetters(MClass theClass)
	{
//		excessive code??
//		se tivesse reparado neste pormenor mais cedo talvez tivesse feito de forma diferente
		if(isSubClass(theClass)){
			printHeaderBasicGettersSetters(theClass, new AttributeInfo(AssociationKind.NONE, "ID", TypeFactory.mkInteger()), "setter");
			println("* @param sets the ID");
			println("**********************************************************************/");
			println("public void setID()");
			println("{");
			incIndent();
				printIDgeneratorCaller(theClass, "ID");
			decIndent();
			println("}");
			println();
		}
		
		for (AttributeInfo currentAttribute : AttributeInfo.getAttributesInfo(theClass))
		{
			printHeaderBasicGettersSetters(theClass, currentAttribute, "getter");
			println("* @return the " + currentAttribute.getName() + " of the " + theClass.nameAsRolename());
			println("**********************************************************************/");
			println("public " + JavaTypes.javaInterfaceType(currentAttribute.getType()) + " " + currentAttribute.getName()
							+ "()");
			println("{");
			incIndent();
				if(currentAttribute.getType().isEnum())
					println("return " + JavaTypes.javaInterfaceType(currentAttribute.getType()) + ".valueOf(" + currentAttribute.getName() + ");");
				else
					println("return " + currentAttribute.getName() + ";");
			decIndent();
			println("}");
			println();
				
			printHeaderBasicGettersSetters(theClass, currentAttribute, "setter");
			if(currentAttribute.getKind().toString().equals(AssociationKind.NONE.toString()) && currentAttribute.getName().equals("ID")){
				println("* @param sets the ID");
				println("**********************************************************************/");
				println("public void set" + capitalize(currentAttribute.getName()) + "()");
				println("{");
				incIndent();
					printIDgeneratorCaller(theClass, currentAttribute.getName());
				decIndent();
				println("}");
				println();
			}else{
				println("* @param " + currentAttribute.getName() + " the " + currentAttribute.getName() + " to set");
				println("**********************************************************************/");
				println("public void set" + capitalize(currentAttribute.getName()) + "("
							+ JavaTypes.javaInterfaceType(currentAttribute.getType()) + " " + currentAttribute.getName() + ")");
				println("{");
				incIndent();
					if(currentAttribute.getType().isEnum())
						println("this." + currentAttribute.getName() + " = " + currentAttribute.getName() + ".name();");
					else
						println("this." + currentAttribute.getName() + " = " + currentAttribute.getName() + ";");
				decIndent();
				println("}");
				println();
			}

			if (currentAttribute.getKind() == AssociationKind.MANY2MANY)
			{
				// String otherType = JavaTypes.javaPrimitiveType(currentAttribute.getType());
				// String otherType = JavaTypes.getJavaInterfaceType(currentAttribute.getType());
				String otherType = JavaTypes.oclCollectionInnerType(((CollectionType) currentAttribute.getType())).shortName();
				String otherName = otherType.toLowerCase();
				printHeaderBasicGettersSetters(theClass, currentAttribute, "single setter");
				println("* @param " + otherName + " the " + otherName + " to add");
				println("**********************************************************************/");
				println("public void add" + capitalize(currentAttribute.getName()) + "(" + otherType + " " + otherName + ")");
				println("{");
				incIndent();
				println("this." + currentAttribute.getName() + ".add(" + otherName + ");");
				decIndent();
				println("}");
				println();
				
				printHeaderBasicGettersSetters(theClass, currentAttribute, "single remover");
				println("* @param " + otherName + " the " + otherName + " to remove");
				println("**********************************************************************/");
				println("public void remove" + capitalize(currentAttribute.getName()) + "(" + otherType + " " + otherName + ")");
				println("{");
				incIndent();
				println("this." + currentAttribute.getName() + ".remove(" + otherName + ");");
				decIndent();
				println("}");
				println();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printNavigators(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printNavigators(MClass theClass)
	{
		for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
		{
			switch (ai.getKind())
			{
				case ASSOCIATIVE2MEMBER:
					// Already performed by the generated getters
					break;
				case MEMBER2ASSOCIATIVE:
					// Already performed in one direction by the collection attribute. This call generates two operations
					// (one getter and one setter) in the other direction
					// System.out.println(ai);
					if (theClass == ai.getSourceAE().cls())
						printMEMBER2ASSOCIATIVE(ai);
					if (theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls())
						printMEMBER2ASSOCIATIVE(ai.swapped());
					break;
				case MEMBER2MEMBER:
					// Uses the association class to obtain the assessor to the other member
					// System.out.println(ai);
					if (theClass == ai.getSourceAE().cls())
						printMEMBER2MEMBER(ai);
					if (theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls())
						printMEMBER2MEMBER(ai.swapped());
					break;
				case ONE2ONE:
					// Already performed in one direction by the attribute getter. This call generates an operation in the other
					// direction
					// System.out.println(ai);
					if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
						if(theClass == ai.getSourceAE().cls() && ai.getTargetAE().getAnnotation("holder") != null)
							printONE2ONE(ai);
						if(theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
								&& ai.getSourceAE().getAnnotation("holder") != null)
							printONE2ONE(ai.swapped());
					}else{
						if (theClass == ai.getSourceAE().cls()
										&& theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls()))
							printONE2ONE(ai);
						if (theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
										&& theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls()))
							printONE2ONE(ai.swapped());
					}
					break;
				case ONE2MANY:
					// // Already performed in one direction by the collection attribute. This call generates two operations
					// (one getter and one setter) in the other direction
//					 System.out.println(ai);
					if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered()))
						printONE2MANY(ai);
					if (theClass == ai.getTargetAE().cls() && (ai.getSourceAE().isCollection() || ai.getSourceAE().isOrdered())
									&& ai.getSourceAE().cls() != ai.getTargetAE().cls())
						printONE2MANY(ai.swapped());
					break;
				case MANY2MANY:
					// Already performed in one direction by the collection attribute getter. This call generates an operation
					// in the other direction
//					 System.out.println(ai);
					if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
						if(theClass == ai.getSourceAE().cls() && ai.getTargetAE().getAnnotation("holder") != null)
							printMANY2MANY(ai);
						if(theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
								&& ai.getSourceAE().getAnnotation("holder") != null)
							printMANY2MANY(ai.swapped());
					}else{
//						System.out.println("test - " + ai);
//						System.out.println("theClass - " + theClass.name());
//						System.out.println("ai.getSourceAE().cls() - " + ai.getSourceAE().cls().name());
//						System.out.println("ai.getTargetAE().cls() - " + ai.getTargetAE().cls().name());
//						System.out.println("more complex - " + util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls()).name());
						if (theClass == ai.getSourceAE().cls()
										&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
//							System.out.println("test2.1");
							printMANY2MANY(ai);
						}
							
						if (theClass == ai.getTargetAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
										&& theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
//							System.out.println("test2.2");
							printMANY2MANY(ai.swapped());
						}
							
					}
					break;
				default:
					System.out.println("ERROR: " + ai);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.quasar.use.api.implementation.IJavaVisitor#printMEMBER2ASSOCIATIVE(org.quasar.use.api.implementation.AssociationInfo)
	 */
	@Override
	public void printMEMBER2ASSOCIATIVE(AssociationInfo aInfo)
	{
		MAssociationEnd sourceAE = aInfo.getSourceAE();
		MAssociationEnd targetAE = aInfo.getTargetAE();
		MAssociationClass associationClass = aInfo.getAssociationClass();

		String sourceClass = sourceAE.cls().name();
		// String targetClass = targetAE.cls().name();
		String associativeClass = associationClass.name();

		String sourceRole = sourceAE.name();
		String associativeRole = associationClass.nameAsRolename();

		MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
		MMultiplicity targetMultiplicity = targetAE.multiplicity();

		String associativeInterfaceType = targetAE.getType().isOrderedSet() ? "SortedSet<" + associativeClass + ">" : (targetAE
						.getType().isSet() ? "Set<" + associativeClass + ">" : associativeClass);

		String associativeImplementationType = targetAE.getType().isOrderedSet() ? "TreeSet<" + associativeClass + ">"
						: (targetAE.getType().isSet() ? "HashSet<" + associativeClass + ">" : associativeClass);

		println("/**********************************************************************");
		println("* MEMBER2ASSOCIATIVE getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + associativeClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @return the " + associativeRole + " of the " + sourceRole);
		println("**********************************************************************/");
		println("public " + associativeInterfaceType + " " + associativeRole + "()");
		println("{");
		incIndent();
		if (targetAE.getType().isSet() || targetAE.getType().isOrderedSet())
		{
			println(associativeInterfaceType + " result = new " + associativeImplementationType + "();");
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "()  ==  this)");
			incIndent();
			println("result.add(x);");
			decIndent();
			decIndent();
			println("return result;");
		}
		else
		{
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "()  ==  this)");
			incIndent();
			println("return x;");
			decIndent();
			decIndent();
			println("return null;");
		}
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* MEMBER2ASSOCIATIVE setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + associativeClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + associativeRole + " the " + associativeRole + " to set");
		println("**********************************************************************/");
		println("public void set" + capitalize(associativeRole) + "(" + associativeInterfaceType + " " + associativeRole + ")");
		println("{");
		incIndent();
		if (aInfo.getTargetAE().getType().isSet() || aInfo.getTargetAE().getType().isOrderedSet())
		{
			println("for (" + associativeClass + " x : " + associativeRole + ")");
			incIndent();
			println("x.set" + capitalize(sourceRole) + "(this);");
			decIndent();
		}
		else
			println(associativeRole + ".set" + capitalize(sourceRole) + "(this);");
		decIndent();
		println("}");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printMEMBER2MEMBER(org.quasar.use.api.implementation.AssociationInfo)
	 */
	@Override
	public void printMEMBER2MEMBER(AssociationInfo aInfo)
	{
		MAssociationEnd sourceAE = aInfo.getSourceAE();
		MAssociationEnd targetAE = aInfo.getTargetAE();
		MAssociationClass associationClass = aInfo.getAssociationClass();

		String sourceClass = sourceAE.cls().name();
		String targetClass = targetAE.cls().name();
		String associativeClass = associationClass.name();

		String sourceRole = sourceAE.name();
		String targetRole = targetAE.name();
		// String associativeRole = associationClass.nameAsRolename();

		MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
		MMultiplicity targetMultiplicity = targetAE.multiplicity();

		String targetInterfaceType = JavaTypes.javaInterfaceType(targetAE.getType());
		String targetImplementationType = JavaTypes.javaImplementationType(targetAE.getType());

		println("/**********************************************************************");
		println("* MEMBER2MEMBER getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @return the " + targetRole + " of the " + sourceRole);
		println("**********************************************************************/");
		println("public " + targetInterfaceType + " " + targetRole + "()");
		println("{");
		incIndent();
		if (aInfo.getTargetAE().getType().isSet() || aInfo.getTargetAE().getType().isOrderedSet())
		{
			println(targetInterfaceType + " result = new " + targetImplementationType + "();");
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "()  ==  this && x. " + targetRole + "() != null)");
			incIndent();
			println("result.add(x." + targetRole + "());");
			decIndent();
			decIndent();
			println("return result;");
		}
		else
		{
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "()  ==  this)");
			incIndent();
			println("return x." + targetRole + "();");
			decIndent();
			decIndent();
			println("return null;");
		}
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* MEMBER2MEMBER setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetRole + " the " + targetRole + " to set");
		println("**********************************************************************/");
		println("public void set" + capitalize(targetRole) + "(" + targetInterfaceType + " " + targetRole + ")");
		println("{");
		incIndent();
		if (targetAE.getType().isSet() || targetAE.getType().isOrderedSet())
		{
			println("for (" + targetClass + " t : " + targetRole + ")");
			incIndent();
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "() == this)");
			incIndent();
			println("x.set" + capitalize(targetRole) + "(t);");
			decIndent();
			decIndent();
			decIndent();
		}
		else
		{
			print("for (" + associativeClass + " x : " + associativeClass);
//			println(associationClass.isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
			println(".allInstances())");
			incIndent();
			println("if (x." + sourceRole + "() == this)");
			incIndent();
			println("x.set" + capitalize(targetRole) + "(" + targetRole + ");");
			decIndent();
			decIndent();
		}
		decIndent();
		println("}");
		println();
				
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printONE2ONE(org.quasar.use.api.implementation.AssociationInfo)
	 */
	@Override
	public void printONE2ONE(AssociationInfo aInfo)
	{
		MAssociationEnd sourceAE = aInfo.getSourceAE();
		MAssociationEnd targetAE = aInfo.getTargetAE();
		// MAssociationClass associationClass = aInfo.getAssociationClass();

		String sourceClass = sourceAE.cls().name();
		String targetClass = targetAE.cls().name();
		// String associativeClass = associationClass.name();

		String sourceRole = sourceAE.name();
		String targetRole = targetAE.name();
		// String associativeRole = associationClass.nameAsRolename();

		MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
		MMultiplicity targetMultiplicity = targetAE.multiplicity();

		String targetInterfaceType = JavaTypes.javaInterfaceType(targetAE.getType());
		// String targetImplementationType = JavaTypes.getJavaImplementationType(targetAE.getType());

		// String allInstances = aInfo.getTargetAE().cls().isAbstract() ? "allInstances()" : "allInstances";

		println("/**********************************************************************");
		println("* ONE2ONE getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]");
		println("* @return the " + targetRole + " of the " + sourceRole);
		println("**********************************************************************/");
		println("public " + targetInterfaceType + " " + targetRole + "()");
		println("{");
		incIndent();
		// println("for (" + targetInterfaceType + " x : " + targetInterfaceType + "." + allInstances + ")");
		print("for (" + targetInterfaceType + " x : " + targetInterfaceType);
//		println(targetAE.cls().isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
		println(".allInstances())");
		incIndent();
		if(isSubClass(targetAE.cls()))
			println("if (((" + targetAE.cls() + ") x)." + sourceRole + "() == this)");
		else
			println("if (x." + sourceRole + "() == this)");

		incIndent();
		println("return x;");
		decIndent();
		decIndent();
		println("return null;");
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* ONE2ONE setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]");
		println("* @param " + targetRole + " the " + targetRole + " to set");
		println("**********************************************************************/");
		println("public void set" + capitalize(targetRole) + "(" + targetClass + " " + targetRole + ")");
		println("{");
		incIndent();
		println(targetRole + ".set" + capitalize(sourceRole) + "(this);");
		decIndent();
		println("}");
		println();
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printONE2MANY(org.quasar.use.api.implementation.AssociationInfo)
	 */
	@Override
	public void printONE2MANY(AssociationInfo aInfo)
	{
		MAssociationEnd sourceAE = aInfo.getSourceAE();
		MAssociationEnd targetAE = aInfo.getTargetAE();
		// MAssociationClass associationClass = aInfo.getAssociationClass();

		String sourceClass = sourceAE.cls().name();
		String targetClass = targetAE.cls().name();
		// String associativeClass = associationClass.name();

		String sourceRole = sourceAE.name();
		String targetRole = targetAE.name();
		// String associativeRole = associationClass.nameAsRolename();

		MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
		MMultiplicity targetMultiplicity = targetAE.multiplicity();

		String targetInterfaceType = JavaTypes.javaInterfaceType(targetAE.getType());
		String targetImplementationType = JavaTypes.javaImplementationType(targetAE.getType());

		// String allInstances = aInfo.getTargetAE().cls().isAbstract() ? "allInstances()" : "allInstances";

		println("/**********************************************************************");
		println("* ONE2MANY getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @return the " + targetRole + " of the " + sourceRole);
		println("**********************************************************************/");
		println("public " + targetInterfaceType + " " + targetRole + "()");
		println("{");
		incIndent();
		println(targetInterfaceType + " result = new " + targetImplementationType + "();");
		if(isSubClass(targetAE.cls()))
			print("for (" + baseAncestor(targetAE.cls()) + " x : " + targetClass);
		else
			print("for (" + targetClass + " x : " + targetClass);
		println(".allInstances())");
//		println(targetAE.cls().isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
		incIndent();
		if(isSubClass(targetAE.cls())){
			println("if (((" + targetAE.cls() + ") x)." + sourceRole + "()  ==  this)");
			incIndent();
			println("result.add((" + targetAE.cls() + ") x);");
		}else{
			println("if (x." + sourceRole + "()  ==  this)");
			incIndent();
			println("result.add(x);");
		}
		
		decIndent();
		decIndent();
		println("return result;");
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* ONE2MANY multiple setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetRole + " the " + targetRole + " to set");
		println("**********************************************************************/");
		println("public void set" + capitalize(targetRole) + "(" + targetInterfaceType + " " + targetRole + ")");
		println("{");
		incIndent();
		println("for (" + targetClass + " x : " + targetRole + ")");
		incIndent();
		println("x.set" + capitalize(sourceRole) + "(this);");
		decIndent();
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* ONE2MANY single setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetClass.toLowerCase() + " the " + targetClass.toLowerCase() + " to add");
		println("**********************************************************************/");
		println("public void add" + capitalize(targetRole) + "(" + targetClass + " " + targetClass.toLowerCase() + ")");
		println("{");
		incIndent();
		println(targetClass.toLowerCase() + ".set" + capitalize(sourceRole) + "(this);");
		decIndent();
		println("}");
		println();
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.quasar.use.api.implementation.IJavaVisitor#printNavigatorMANY2MANY(org.quasar.use.api.implementation.AssociationInfo)
	 */
	@Override
	public void printMANY2MANY(AssociationInfo aInfo)
	{
		MAssociationEnd sourceAE = aInfo.getSourceAE();
		MAssociationEnd targetAE = aInfo.getTargetAE();
		// MAssociationClass associationClass = aInfo.getAssociationClass();

		String sourceClass = sourceAE.cls().name();
		String targetClass = targetAE.cls().name();
		// String associativeClass = associationClass.name();

		String sourceRole = sourceAE.name();
		String targetRole = targetAE.name();
		// String associativeRole = associationClass.nameAsRolename();

		MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
		MMultiplicity targetMultiplicity = targetAE.multiplicity();

		String targetInterfaceType = JavaTypes.javaInterfaceType(targetAE.getType());
		String targetImplementationType = JavaTypes.javaImplementationType(targetAE.getType());

		// String allInstances = aInfo.getTargetAE().cls().isAbstract() ? "allInstances()" : "allInstances";

		println("/**********************************************************************");
		println("* MANY2MANY getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @return the " + targetRole + " of the " + sourceRole);
		println("**********************************************************************/");
		println("public " + targetInterfaceType + " " + targetRole + "()");
		println("{");
		incIndent();
		println(targetInterfaceType + " result = new " + targetImplementationType + "();");
		if(isSubClass(targetAE.cls()))
			print("for (" + baseAncestor(targetAE.cls()) + " x : " + targetClass);
		else
			print("for (" + targetClass + " x : " + targetClass);
//		println(targetAE.cls().isAbstract() ? ".allInstancesAbstract())" : ".allInstances())");
		println(".allInstances())");
		incIndent();
		if(isSubClass(targetAE.cls())){
			println("if (((" + targetAE.cls() + ") x)." + sourceRole + "() != null && ((" + targetAE.cls() + ") x)." + sourceRole + "().contains(this))");
			incIndent();
			println("result.add((" + targetAE.cls() + ") x);");
		}else{
			println("if (x." + sourceRole + "() != null && x." + sourceRole + "().contains(this))");
			incIndent();
			println("result.add(x);");
		}
		decIndent();
		decIndent();
		println("return result;");
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* MANY2MANY multiple setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetRole + " the " + targetRole + " to set");
		println("**********************************************************************/");
		println("public void set" + capitalize(targetRole) + "(" + targetInterfaceType + " " + targetRole + ")");
		println("{");
		incIndent();
		println("for (" + targetClass + " x : " + targetRole + ")");
		incIndent();
		println("x." + sourceRole + "().add(this);");
		decIndent();
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* MANY2MANY single setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetClass.toLowerCase() + " the " + targetClass.toLowerCase() + " to add");
		println("**********************************************************************/");
		println("public void add" + capitalize(targetRole) + "(" + targetClass + " " + targetClass.toLowerCase() + ")");
		println("{");
		incIndent();
		println(targetClass.toLowerCase() + ".add" + capitalize(sourceRole) + "(this);");
		decIndent();
		println("}");
		println();
		
		println("/**********************************************************************");
		println("* MANY2MANY single setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param " + targetClass.toLowerCase() + " the " + targetClass.toLowerCase() + " to remove");
		println("**********************************************************************/");
		println("public void remove" + capitalize(targetRole) + "(" + targetClass + " " + targetClass.toLowerCase() + ")");
		println("{");
		incIndent();
		println(targetClass.toLowerCase() + ".remove" + capitalize(sourceRole) + "(this);");
		decIndent();
		println("}");
		println();

	}

	public void printModelAssociativeRestrictionsState(String targetRole, String sourceClass, MMultiplicity sourceMultiplicity, String AssociationType, String targetClass, MMultiplicity targetMultiplicity, MAssociationEnd targetAE)
	{
		String associationStateAttribute = "valid" + capitalize(targetRole);
		println("private boolean " + associationStateAttribute + ";");
		println();
		
		println("/**********************************************************************");
		println("* " + AssociationType + " state getter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @return the state regarding the mandatory association " + targetRole);
		println("**********************************************************************/");
		println("public boolean " + associationStateAttribute + "()");
		println("{");
		incIndent();
	
		println("return " + associationStateAttribute + ";");
		
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* " + AssociationType + " state setter for " + sourceClass + "[" + sourceMultiplicity + "] <-> " + targetClass + "["
						+ targetMultiplicity + "]" + (targetAE.getType().isOrderedSet() ? " ordered" : ""));
		println("* @param the new state regarding the mandatory association " + targetRole);
		println("**********************************************************************/");
		println("public void set" + capitalize(associationStateAttribute) + "(boolean " + associationStateAttribute + ")");
		println("{");
		incIndent();
		
		println("this." + associationStateAttribute + "= " + associationStateAttribute + ";");
		
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printSoilOperation(org.tzi.use.uml.mm.MOperation)
	 */
	@Override
	public void printModelAssociationRestrictionsStateCheckers(MClass theClass){
		boolean validClass = false;
		if(!AssociationInfo.getAssociationsInfo(theClass).isEmpty())
			validClass = true;
		else
			for(MClass c : theClass.parents())
				if(!AssociationInfo.getAssociationsInfo(c).isEmpty())
					validClass = true;
//		if the class has any association or its parent does
		if(validClass){
			String AssociationRestrictionsValid = "AssociationRestrictionsValid";
			
			println("private boolean " + AssociationRestrictionsValid + " = false;");
			println();
			
			println("/**********************************************************************");
			println("*general association state getter ");
			println("* @return the state regarding all mandatory associations ");
			println("**********************************************************************/");
			println("public boolean is" + capitalize(AssociationRestrictionsValid) + "()");
			println("{");
			incIndent();
		
			println("return " + AssociationRestrictionsValid + ";");
			
			decIndent();
			println("}");
			println();
	
			println("/**********************************************************************");
			println("* general association state setter");
			println("* @param the new state regarding all mandatory association ");
			println("**********************************************************************/");
			println("public void set" + AssociationRestrictionsValid + "(boolean " + AssociationRestrictionsValid + ")");
			println("{");
			incIndent();
			
			println("this. " + AssociationRestrictionsValid + "= " + AssociationRestrictionsValid + ";");
			
			decIndent();
			println("}");
			println();
			
			if(!AssociationInfo.getAssociationsInfo(theClass).isEmpty()){
				for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
				{
					MAssociationEnd sourceAE = ai.getSourceAE();
					MAssociationEnd targetAE = ai.getTargetAE();
		
					String sourceClass = ai.getSourceAEClass().name();
					String targetClass = ai.getTargetAEClass().name();
		
					String targetRole = targetAE.name();
		
					MMultiplicity sourceMultiplicity = sourceAE.multiplicity();
					MMultiplicity targetMultiplicity = targetAE.multiplicity();
												
					switch (ai.getKind())
					{
						case ASSOCIATIVE2MEMBER:
							printModelAssociativeRestrictionsState(targetRole, sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, MMultiplicity.ONE, targetAE);
							break;
						case MEMBER2ASSOCIATIVE:
							printModelAssociativeRestrictionsState(ai.getTargetAEClass().nameAsRolename(), sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, targetMultiplicity, targetAE);
							break;
						case MEMBER2MEMBER:
							printModelAssociativeRestrictionsState(targetRole, sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, targetMultiplicity, targetAE);
							break;
						case ONE2ONE:
							printModelAssociativeRestrictionsState(targetRole, sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, targetMultiplicity, targetAE);
							break;
						case ONE2MANY:
							printModelAssociativeRestrictionsState(targetRole, sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, targetMultiplicity, targetAE);
							break;
						case MANY2MANY:
							printModelAssociativeRestrictionsState(targetRole, sourceClass, sourceMultiplicity, ai.getKind().name(), targetClass, targetMultiplicity, targetAE);					
							break;
						default:
							System.out.println("ERROR: " + ai);
					}
				}
			}
			
	//		checkModelRestritions
			println("/**********************************************************************");
			println("* association state setter");
			println("**********************************************************************/");
			println("public void checkModelRestrictions()");
			println("{");
			incIndent();		
			for(MClass c : theClass.parents())
				if(!AssociationInfo.getAssociationsInfo(c).isEmpty())
					println("super.checkModelRestrictions();");
			
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();

				String targetRole = targetAE.name();
	
				MMultiplicity targetMultiplicity = targetAE.multiplicity();
				
				String upperRange = targetMultiplicity.toString();
				String lowerRange = targetMultiplicity.toString();
//				System.out.println("association: " + targetRole + "   - teste 1 - " + targetMultiplicity.toString());
				if(targetMultiplicity.toString().contains("..")){
					upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
					lowerRange = targetMultiplicity.toString().split("\\.\\.")[0];
//					System.out.println("teste 2 - " + lowerRange + " and " + upperRange);
				}
				if(upperRange.equals("*")){
					upperRange = "-1";
					if(lowerRange.equals("*"))
						lowerRange = "0";
				}
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), 1, " + upperRange + "));");
						break;
					case MEMBER2ASSOCIATIVE:
						println("setValid" + capitalize(ai.getTargetAEClass().nameAsRolename()) + "(ModelContracts.Check(" + ai.getTargetAEClass().nameAsRolename() + "(), " + lowerRange + ", " + upperRange + "));");
						break;
					case MEMBER2MEMBER:
						println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), " + lowerRange + ", " + upperRange + "));");
						break;
					case ONE2ONE:					
							println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), " + lowerRange + ", " + upperRange + "));");
						break;
					case ONE2MANY:
						if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered()))
							println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), " + lowerRange + ", " + upperRange + "));");
						else
							println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), " + lowerRange + "," + upperRange + "));");						
						break;
					case MANY2MANY:
						println("setValid" + capitalize(targetRole) + "(ModelContracts.Check(" + targetRole + "(), " + lowerRange + ", " + upperRange + "));");
						break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
			decIndent();
			println("}");
			println();
			
	//		checkrestrictions
			println("/**********************************************************************");
			println("* general association state setter");
			println("**********************************************************************/");
			println("public void checkRestrictions()");
			println("{");
			incIndent();		
			print("if(");
			
			int i = 0;
			int last = AssociationInfo.getAssociationsInfo(theClass).size();
			
			for(MClass c : theClass.parents())
				if(!AssociationInfo.getAssociationsInfo(c).isEmpty()){
					print("super.is" + capitalize(AssociationRestrictionsValid) + "()");
					if(last != 0)
						print(" && ");
				}
			
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();
				String targetRole = targetAE.name();
			
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						print("valid" + capitalize(targetRole) + "()");
						break;
					case MEMBER2ASSOCIATIVE:
						print("valid" + capitalize(ai.getTargetAEClass().nameAsRolename()) + "()");
						break;
					case MEMBER2MEMBER:
						print("valid" + capitalize(targetRole) + "()");
						break;
					case ONE2ONE:					
						print("valid" + capitalize(targetRole) + "()");
						break;
					case ONE2MANY:
						if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered()))
							print("valid" + capitalize(targetRole) + "()");
						else
							print("valid" + capitalize(targetRole) + "()");						
						break;
					case MANY2MANY:
						print("valid" + capitalize(targetRole) + "()");
						break;
					default:
						System.out.println("ERROR: " + ai);
				}
				++i;
				if(i < last)
					print(" && ");
			}
			println(")");
			incIndent();
			println("set" + AssociationRestrictionsValid + "(true);");
			decIndent();
			println("else");
			incIndent();
			println("set" + AssociationRestrictionsValid + "(false);");
			decIndent();
			decIndent();
			println("}");
			println();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printSoilOperation(org.tzi.use.uml.mm.MOperation)
	 */
	@Override
	public void printBusinessControllerAccessMethods(MClass theClass){
		println("/**********************************************************************");
		println("* general association state setter");
		println("* @return a singleton instance to access the controll methods");
		println("**********************************************************************/");
		println("public static " + theClass.name() + "Access getAccess()");
		println("{");
		incIndent();
	
		println("return " + theClass.name() + "Access.getAccess();");
		
		decIndent();
		println("}");
		println();
		
//		Object Actions
		if(!theClass.isAbstract()){
			String [] actions = {"insert","update","delete"};
			for(String action : actions){
				println("/**********************************************************************");
				println("* " + action + " the caller object");
				println("**********************************************************************/");
				println("public void " + action + "()");
				println("{");
				incIndent();
		
				println("getAccess()." + action + "(this);");
			
				decIndent();
				println("}");
				println();
			}
		}
		
//		Object association Actions
		if(!theClass.allAssociations().isEmpty() || ModelUtilities.isAssociativeClass(theClass)){
			String [] associationActions = {"insertAssociation","deleteAssociation"};
			for(String action : associationActions){
				println("/**********************************************************************");
				println("* " + action + " in the caller object");
				println("* @param the neighbor object");
				println("**********************************************************************/");
				println("public void " + action + "(Object neibor, String AssociationID)");
				println("{");
				incIndent();
				
				List<AssociationInfo> allAssociations = new ArrayList<AssociationInfo>(AssociationInfo.getAllAssociationsInfo(theClass));
				List<AssociationInfo> indirectAssociations = getIndirectAssociations(theClass);

				
				if(!allAssociations.isEmpty()){
					if(!indirectAssociations.isEmpty()){
						print("if(");
						for(int i = 0;i < indirectAssociations.size(); ++i){
							print("AssociationID.equals(\"" + indirectAssociations.get(i).getName() + "Association\")");
							if(i < indirectAssociations.size() - 1)
								print(" || ");
							else
								println(")");
						}
						incIndent();
							println("super." + action + "(neibor, AssociationID);");
						decIndent();
						println("else");
						incIndent();
							println("getAccess()." + action + "(this,neibor, AssociationID);");
						decIndent();
					}else{
						println("getAccess()." + action + "(this,neibor, AssociationID);");
					}
				}
				decIndent();
				println("}");
				println();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printSoilOperation(org.tzi.use.uml.mm.MOperation)
	 */
	@Override
	public void printOtherMethods(MClass theClass){
		println("/**********************************************************************");
		println("* @param An ID");
		println("* @return the object with the given ID or null");	
		println("**********************************************************************/");
		println("public static " + theClass.name() + " get" + theClass.name() + "(int ID)");
		println("{");
		incIndent();
	
		println("return Database.get(" + theClass.name() + ".class, ID);");
		
		decIndent();
		println("}");
		println();
		
		println("/**********************************************************************");
		println("* @return the type Class");	
		println("**********************************************************************/");
		println("public Class<?> getType()");
		println("{");
		incIndent();
	
		println("return " + theClass.name() + ".class;");
		
		decIndent();
		println("}");
		println();
		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printSoilOperation(org.tzi.use.uml.mm.MOperation)
	 */
	@Override
	public void printSoilOperation(MOperation op)
	{
		// visitAnnotations(e);

		println("/**********************************************************************");
		println("* User-defined operation specified in SOIL/OCL");
		for (int i = 0; i < op.paramList().size(); i++)
			println("* @param " + op.paramList().varDecl(i).name() + " the " + op.paramList().varDecl(i).name() + " to set");
		println("**********************************************************************/");
		print("public " + (op.hasResultType() ? JavaTypes.javaInterfaceType(op.resultType()) : "void") + " ");
		print(op.name() + "(");
		VarDecl decl = null;
		for (int i = 0; i < op.paramList().size(); i++)
		{
			decl = op.paramList().varDecl(i);
			print(JavaTypes.javaInterfaceType(decl.type()) + " " + decl.name());
			if (i < op.paramList().size() - 1)
				print(", ");
		}
		println(")");
		println("{");
		incIndent();

		if (op.hasExpression())
		{
			printlnc("TODO");
			printlnc("return " + op.expression().toString());
		}
		else
		{
			if (op.hasStatement())
			{
				printlnc("" + op.getStatement());
				// printlnc(op.getStatement().toConcreteSyntax(4, 4));
				// String[] temp = op.getStatement().toString().split(";");
				// for (int i = 0; i < temp.length; i++)
				// printlnc(temp[i] + ";");
			}
			if (op.hasExpression())
				printlnc("" + op.expression());
		}
		if (op.hasResultType())
			println("return " + JavaTypes.javaDummyValue(op.resultType()) + ";");

		decIndent();
		println("}");
		println();

		if (!op.preConditions().isEmpty())
		{
			printlnc("PRE-CONDITIONS (TODO)");
			println("/*");
			for (MPrePostCondition pre : op.preConditions())
			{
				println("pre " + pre.name());
				incIndent();
				println(pre.expression().toString());
				decIndent();
				println();
			}
			println("*/");
			println();
		}

		if (!op.postConditions().isEmpty())
		{
			printlnc("POST-CONDITIONS (TODO)");
			println("/*");
			for (MPrePostCondition post : op.postConditions())
			{
				println("post " + post.name());
				incIndent();
				println(post.expression().toString());
				decIndent();
				println();
			}
			println("*/");
			println();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printToString(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printToString(MClass theClass)
	{
		// println("/* (non-Javadoc)");
		// println("* @see java.lang.Object#toString()");
		// println("*/");
		// println("@Override");
		println("/**********************************************************************");
		println("* Object serializer");
		println("**********************************************************************/");
		println("public String toString()");
		println("{");
		incIndent();
		print("return \"" + theClass.name() + " [");
		if (theClass.allParents().size() > 0)
			print("\" + super.toString() + \" ");
		List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
		for (int i = 0; i < attributes.size(); i++)
		{
			print(attributes.get(i).getName() + "=\" + " + attributes.get(i).getName() + " + \"");
			if (i < attributes.size() - 1)
				print(", ");
		}
		println("]\";");
		decIndent();
		println("}");
		println();
	}

	/* (non-Javadoc)
	 * @see org.quasar.juse.api.implementation.IJavaVisitor#printCompareTo(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printCompareTo(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param other " + theClass.name() + " to compare to the current one");
		println("* @return");
		println("**********************************************************************/");
		println("public int compareTo(Object other)");
		println("{");
		incIndent();
		println("return this.hashCode() - ((" + theClass.name() +") other).hashCode();");
		decIndent();
		println("}");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.implementation.JavaVisitor#printInvariants(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printInvariants(MClass theClass)
	{
		if (!model.classInvariants(theClass).isEmpty())
		{
			printlnc("-------------------------------------------------------------------------------");
			printlnc("INVARIANTS (TODO)");
			println("/*");
			for (MClassInvariant inv : model.classInvariants(theClass))
			{
				println("inv " + inv.name());
				incIndent();
				println(inv.bodyExpression().toString());
				decIndent();
				println();
			}
			println("*/");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.implementation.JavaVisitor#printTupleTypes(java.lang.Integer, java.lang.String)
	 */
	@Override
	public void printTupleTypes(Integer parameterNumber, String layerName)
	{
		printFileHeader("Tuple" + parameterNumber, businessLayerName);

		print("public class Tuple" + parameterNumber + "<");
		for (int i = 0; i < parameterNumber; i++)
		{
			print("T" + i);
			if (i < parameterNumber - 1)
				print(", ");
		}
		println(">");

		println("{");
		incIndent();

		for (int i = 0; i < parameterNumber; i++)
			println("private T" + i + " t" + i + ";");
		println();

		println("/***********************************************************");
		for (int i = 0; i < parameterNumber; i++)
			println("* @param t" + i);
		println("***********************************************************/");
		print("public Tuple" + parameterNumber + "(");
		for (int i = 0; i < parameterNumber; i++)
		{
			print("T" + i + " t" + i);
			if (i < parameterNumber - 1)
				print(", ");
		}
		println(")");
		println("{");
		incIndent();
		for (int i = 0; i < parameterNumber; i++)
			println("this.t" + i + "= t" + i + ";");
		decIndent();
		println("}");
		println();

		for (int i = 0; i < parameterNumber; i++)
		{
			println("/***********************************************************");
			println("* @return the t" + i);
			println("***********************************************************/");
			println("public T" + i + " getT" + i + "()");
			println("{");
			incIndent();
			println("return t" + i + ";");
			decIndent();
			println("}");
			println();
		}

		for (int i = 0; i < parameterNumber; i++)
		{
			println("/***********************************************************");
			println("* @param t" + i + " the t" + i + " to set");
			println("***********************************************************/");
			println("public void setT" + i + "(T" + i + " t" + i + ")");
			println("{");
			incIndent();
			println("this.t" + i + " = t" + i + ";");
			decIndent();
			println("}");
			println();
		}

		decIndent();
		println("}");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.implementation.JavaVisitor#printMain()
	 */
	@Override
	public void printMain()
	{
		printFileHeader("Main_" + model.name(), presentationLayerName);

		println("import " + basePackageName + "." + businessLayerName + ".*;");
		println("import " + basePackageName + "." + persistenceLayerName + ".Database;");
		println();
		println("import java.util.Scanner;");
		println();

		println("public abstract class Main_", model.name());
		println("{");
		incIndent();
		println("/***********************************************************");
		println("* @param args");
		println("***********************************************************/");
		println("public static void main(String[] args)");
		println("{");
		incIndent();
		println("Database.open(\"database\", \"" + model.name() + "\", \"db4o\");");
		println();
		println("boolean over = false;");
		println();
		println("do");
		println("{");
		incIndent();
		println("Scanner in = new Scanner(System.in);");
		println();
		println("displayMenu();");
		println();
		println("int option;");
		println("try");
		println("{");
		incIndent();
		println("String answer = in.next();");
		println("option = Integer.parseInt(answer);");
		decIndent();
		println("}");
		println("catch (NumberFormatException e)");
		println("{");
		incIndent();
		println("System.out.println(\"Invalid input!...\\n\");");
		println("continue;");
		decIndent();
		println("}");
		println();
		println("switch (option)");
		println("{");
		incIndent();
		println("case 0:");
		incIndent();
		println("over = true;");
		println("break;");
		decIndent();
		int i = 1;
		for (MClass cls : model.classes())
		{
			println("case " + i + ":");
			incIndent();
			println("showResults(" + cls.name() + ".class);");
			println("break;");
			decIndent();
			i++;
		}
		println("default:");
		incIndent();
		println("System.out.println(\"Invalid option!...\\n\");");
		println("break;");
		decIndent();
		println("}");
		decIndent();
		println("}");
		println("while (!over);");
		println();
		println("Database.close();");
		decIndent();
		println("}");
		println();
		println("/***********************************************************");
		println("* The main menu of the " + model.name() + " information system");
		println("***********************************************************/");
		println("public static void displayMenu()");
		println("{");
		incIndent();
		println("System.out.println(\"------------------------------------\");");
		println("System.out.println(\"" + model.name() + " Information System\");");
		println("System.out.println(\"------------------------------------\");");
		println("System.out.println(\"0) EXIT\");");
		i = 1;
		for (MClass cls : model.classes())
		{
			println("System.out.println(\"" + i + ") " + cls.name() + "\");");
			i++;
		}

		println("System.out.println();");
		println("System.out.print(\"OPTION> \");");
		decIndent();
		println("}");
		println();
		println("/***********************************************************");
		println("* @param c the class whose instances we want to show");
		println("***********************************************************/");
		println("public static void showResults(Class<?> c)");
		println("{");
		incIndent();
		println("System.out.println(\"---------------------------------------------------------------------------------------------------------------------\");");
		println("System.out.println(\"| \" + Database.allInstances(c).size() + \" instances of class \" + c.getSimpleName());");
		println("System.out.println(\"---------------------------------------------------------------------------------------------------------------------\");");
		println("for (Object o : Database.allInstances(c))");
		incIndent();
		println("System.out.println(o);");
		decIndent();
		println("System.out.println();");
		decIndent();
		println("}");
		decIndent();
		println("}");
	}
	
//	ACCESS CLASS specific methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessClassHeader(MClass theClass, String layerName)
	{
		printFileHeader(theClass.name(), layerName);
		// visitAnnotations(e);

		printAccessImports(theClass);

		print("public ");

		print("class " + theClass.name() + "Access");

		Set<MClass> parents = theClass.parents();
		if (!parents.isEmpty())
			print(" extends " + StringUtil.fmtSeq(parents.iterator(), ",") + "Access");//since it can only have one super it works for java and android

		println(" implements ModelMusts");
		println("{");
	}

	
	
	
	
	
	
	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printAccessImports(MClass theClass)
	{
		println("import java.util.List;");
		println("import java.util.ArrayList;");
		
		println("import " + basePackageName + "." + persistenceLayerName + ".Database;");
		println("import " + basePackageName + "." + utilsLayerName + ".Command;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandTargetLayer;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printDefaultConstructors(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessDefaultConstructor(MClass theClass)
	{
		println("/**********************************************************************");
		println("* Default constructor");
		println("**********************************************************************/");
		println("public " + theClass.name() + "Access()");
		println("{");
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessSingleton(MClass theClass)
	{
		println("private transient static " + theClass.name() + "Access " + theClass.name().toLowerCase() + "Access = new " + theClass.name() + "Access();");
		println();
		println("/**********************************************************************");
		println("* @return the singleton instance");
		println("**********************************************************************/");
		println("public static " + theClass.name() + "Access getAccess()");
		println("{");
		incIndent();
		println("return " + theClass.name().toLowerCase() +"Access;");
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessObserverListener(MClass theClass)
	{
//		Array that holds the the observers
		println("private transient List<PropertyChangeListener> listener = new ArrayList<PropertyChangeListener>();");
		println();
		
//		method that notifies the observers
		println("/**********************************************************************");
		println("* @param the class that holds the listeners");
		println("* @param the CommandType or persistence action");
		println("* @param the previous version of the object");
		println("* @param the new version of the object");
		println("* @param the previous version of the neighbor object");
		println("* @param the new version of the neighbor object");
		println("**********************************************************************/");
		println("@Override");
		println("public synchronized void notifyObjectListener(Object object, CommandType property, int oldObjectID, Object oldObject, Object newObject, int oldNeiborID, Object oldNeighbor, Object newNeighbor)");
		println("{");
		incIndent();
		println("for(PropertyChangeListener l : listener)");
		incIndent();
		println("l.propertyChange(new PropertyChangeEvent(object, property, oldObjectID, oldObject, newObject, oldNeiborID, oldNeighbor, newNeighbor));");
		decIndent();
		decIndent();
		println("}");
		println();
		
//		method that adds observers
		println("/**********************************************************************");
		println("* @param the class that will observe (this class must implement \"PropertyChangeListener\")");
		println("**********************************************************************/");
		println("public synchronized void setChangeListener(PropertyChangeListener listener)");
		println("{");
		incIndent();
		println("if(!this.listener.contains(listener))");
		incIndent();
		println("this.listener.add(listener);");
		decIndent();
		decIndent();
		println("}");
		println();
		
//		method that removes observers
		println("/**********************************************************************");
		println("* @param the class that will stop observing (this class must implement \"PropertyChangeListener\")");
		println("**********************************************************************/");
		println("public synchronized void removeChangeListener(PropertyChangeListener listener)");
		println("{");
		incIndent();
		println("if(this.listener.contains(listener))");
		incIndent();
		println("this.listener.remove(listener);");
		decIndent();
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessPersistanceMethods(MClass theClass)
	{
		if(!theClass.isAbstract()){
			printInsert(theClass);	
			printUpdate(theClass);
			printDelete(theClass);
		}
		
		if(!theClass.associations().isEmpty() || ModelUtilities.isAssociativeClass(theClass)){
			printInsertAssociation(theClass);
			printDeleteAssociation(theClass);
			printNotityDeletion(theClass);
		}
	}
	
	private void printInsert(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object that will be inserted");
		println("**********************************************************************/");
		println("public synchronized void insert(" + theClass.name() + " object)");
		println("{");
			incIndent();
			println("try{");
				incIndent();
				println("if(Database.get(" + theClass.name() + ".class, object.ID()) == null)");
				println("{");
					incIndent();
					println("object.checkModelRestrictions();");
					println("object.checkRestrictions();");
					println("");
					println("Transactions.getSession().store(object);");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT, CommandTargetLayer.DATABASE, 0, null, object, null, 0, null, null));");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT, CommandTargetLayer.VIEW, 0, null, object, null, 0, null, null));");
					decIndent();
				println("}else{");
					incIndent();
					println("Transactions.CancelTransaction(\"Failed in Insert\", \"this " + theClass.name() + " already exists\");");
					decIndent();
				println("}");
				decIndent();
			println("}catch(Exception e){");
			incIndent();
				println("Transactions.CancelTransaction(\"Failed in Insert\", \"Error ocurred while trying to save the " + theClass.name() + "\");");
			decIndent();
			println("}");
			decIndent();
		println("}");
		println();
		
	}
	private void printUpdate(MClass theClass)
	{		
		println("/**********************************************************************");
		println("* @param the object that will be updated");
		println("**********************************************************************/");
		println("public synchronized void update(" + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
			incIndent();
			println("try{");
				incIndent();
				println(theClass.name() + " object = Database.get(" + theClass.name() + ".class, " + theClass.name().toLowerCase() + ".ID());");
				println("int oldID = " + theClass.name().toLowerCase() + ".ID();");
				println("object.setID();");
				println("if(Database.get(" + theClass.name() + ".class, object.ID()) == null)");
				println("{");
					incIndent();
					println("object.checkModelRestrictions();");
					println("object.checkRestrictions();");
					println("");
					println("Transactions.getSession().store(object);");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.UPDATE, CommandTargetLayer.DATABASE, " + "oldID, " + theClass.name().toLowerCase() + ", object, null, 0, null, null));");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.UPDATE, CommandTargetLayer.VIEW, " + "oldID, " + theClass.name().toLowerCase() + ", object, null, 0, null, null));");
					decIndent();
				println("}else{");
					incIndent();
					println("Transactions.CancelTransaction(\"Failed in Edit\", \"this " + theClass.name() + " already exists\");");
					decIndent();
				println("}");
				decIndent();
			println("}catch(Exception e){");
			incIndent();
				println("Transactions.CancelTransaction(\"Failed in Edit\", \"Error ocurred while trying to save the " + theClass.name() + "\");");
			decIndent();
			println("}");
			decIndent();
		println("}");
		println();
		
	}
	private void printDelete(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object that will be deleted");
		println("**********************************************************************/");
		println("public synchronized void delete(" + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
			incIndent();
			println("try{");
				incIndent();
				println(theClass.name() + " object = Database.get(" + theClass.name() + ".class, " + theClass.name().toLowerCase() + ".ID());");
				println("if(object != null)");
				println("{");
					incIndent();
					println("int oldID = " + theClass.name().toLowerCase() + ".ID();");
					println("notifyDeletion(object);");
					println("Transactions.getSession().delete(object);");
					println("");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE, CommandTargetLayer.DATABASE, oldID, object, null, null, 0, null, null));");
					println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE, CommandTargetLayer.VIEW, oldID, object, null, null, 0, null, null));");
					decIndent();
				println("}else{");
					incIndent();
					println("Transactions.CancelTransaction(\"Failed in Delete\", \"this " + theClass.name() + " does not exists\");");
					decIndent();
				println("}");
				decIndent();
			println("}catch(Exception e){");
			incIndent();
				println("Transactions.CancelTransaction(\"Failed in Delete\", \"Error ocurred while trying to delete the " + theClass.name() + "\");");
			decIndent();
			println("}");
			decIndent();
		println("}");
		println();
		
	}
	private void printInsertAssociation(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object that will receive a new association");
		println("* @param the object that will be added as new association (neighbor)");
		println("**********************************************************************/");
		println("public synchronized void insertAssociation(" + theClass.name() + " " + theClass.name().toLowerCase() + ", Object neighbor, String AssociationID)");
		println("{");
			incIndent();
			println("try{");
				incIndent();
				println(theClass.name() + " object = Database.get(" + theClass.name() + ".class, " + theClass.name().toLowerCase() + ".ID());");
				println("if(object != null)");
				println("{");
					incIndent();
					println("int oldID = " + theClass.name().toLowerCase() + ".ID();");
					for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
					{
						MAssociationEnd targetAE = ai.getTargetAE();
						String targetRole = targetAE.name();
						
						switch (ai.getKind())
						{
							case ASSOCIATIVE2MEMBER:
								println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
								println("{");
								incIndent();
									println("object.set" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
									println("object.setID();");
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
								decIndent();
								println("}");
								break;
							case MEMBER2ASSOCIATIVE:
								println("if(neighbor instanceof " + ai.getTargetAEClass().name() + ")");
								println("{");
								incIndent();
									println("if(((" + ai.getTargetAEClass().name() + ") neighbor)." + ai.getSourceAE().nameAsRolename() + "() != object)");
									incIndent();
										println("((" + ai.getTargetAEClass().name() + ") neighbor).insertAssociation(object, AssociationID);");
									decIndent();
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAEClass().name() + ".class, ((" + ai.getTargetAEClass().name() + ") neighbor).ID(), null, neighbor));");
								decIndent();
								println("}");
								break;
							case MEMBER2MEMBER:
								println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
								println("{");
								incIndent();
									println("if(((" + ai.getTargetAE().cls().name() + ") neighbor)." + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()).nameAsRolename() + "() != null)");
									println("{");
									incIndent();
										println("((" + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()) + ") ((" + ai.getTargetAE().cls().name() + ") neighbor)." + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()).nameAsRolename() + "()).insertAssociation(object, AssociationID);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
									decIndent();
									println("}");
								decIndent();
								println("}");
								break;
							case ONE2ONE:
								if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.add" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& ai.getTargetAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).insertAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
								}else{
									if (theClass == ai.getSourceAE().cls()
													&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.add" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
									if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
													&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).insertAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
								}
								break;
							case ONE2MANY:
								if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered())){
									println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
									println("{");
									incIndent();
										println("((" + ai.getTargetAE().cls().name() + ") neighbor).insertAssociation(object, AssociationID);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
									decIndent();
									println("}");
								}
								if (theClass == ai.getSourceAE().cls() && (!ai.getTargetAE().isCollection() && !ai.getTargetAE().isOrdered())
												&& ai.getSourceAE().cls() != ai.getTargetAE().cls()){
									println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
									println("{");
									incIndent();
										println("object.set" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
									decIndent();
									println("}");
								}
								break;
							case MANY2MANY:
								if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.add" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
//											println("object.set" + capitalize(targetRole) + "(object." + targetRole  + "());");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& ai.getTargetAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).insertAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
								}else{
									if (theClass == ai.getSourceAE().cls()
													&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
											incIndent();
											println("object.add" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
//											println("object.set" + capitalize(targetRole) + "(object." + targetRole  + "());");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
									if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
													&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).insertAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.INSERT_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), null, neighbor));");
										decIndent();
										println("}");
									}
								}
								break;
							default:
								System.out.println("ERROR: " + ai);
						}
					}
					if(ModelUtilities.isAssociativeClass(theClass)){
						print("if(");
						boolean firts = true;
						for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
						{
							if(ai.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
								if(firts){
									print("(neighbor instanceof " + ai.getTargetAE().cls().name() + " || ");
									firts = false;
								}else
									print("neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
						}
						print(" && Database.get(" + theClass.name() + ".class, object.ID()) != null)");
						println("{");
						incIndent();
							println("Transactions.CancelTransaction(\"Failed in InsertAssociation\", \"this " + theClass.name() + " already exists\");");
						decIndent();
						println("}");
						println("else");
						println("{");
						incIndent();
							println("object.checkModelRestrictions();");
							println("object.checkRestrictions();");
							println("Transactions.getSession().store(object);");
						decIndent();
						println("}");
					
					}else{
						println("object.checkModelRestrictions();");
						println("object.checkRestrictions();");
						println("Transactions.getSession().store(object);");
					}
					
					decIndent();
				println("}else{");
					incIndent();
						println("Transactions.CancelTransaction(\"Failed in InsertAssociation\", \"the " + theClass.name() + " does not exists\");");
					decIndent();
				println("}");
				decIndent();
			println("}catch(Exception e){");
			incIndent();
				println("Transactions.CancelTransaction(\"Failed in InsertAssociation\", \"Error ocurred while trying to associate the " + theClass.name() + "\");");
			decIndent();
			println("}");
			decIndent();
		println("}");
		println();

	}
	
	private void printDeleteAssociation(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object that will remove an old association");
		println("* @param the object that will be removed as the old association (neighbor)");
		println("**********************************************************************/");
		println("public synchronized void deleteAssociation(" + theClass.name() + " " + theClass.name().toLowerCase() + ", Object neighbor, String AssociationID)");
		println("{");
			incIndent();
			println("try{");
				incIndent();
				println(theClass.name() + " object = Database.get(" + theClass.name() + ".class, " + theClass.name().toLowerCase() + ".ID());");
				println("if(object != null)");
				println("{");
					incIndent();
					println("int oldID = " + theClass.name().toLowerCase() + ".ID();");
					for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
					{
						MAssociationEnd targetAE = ai.getTargetAE();
						String targetRole = targetAE.name();
						
						switch (ai.getKind())
						{
							case ASSOCIATIVE2MEMBER:
								println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
								println("{");
								incIndent();
									println("object.set" + capitalize(targetRole) + "(null);");
									println("object.setID();");
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
								decIndent();
								println("}");
								break;
							case MEMBER2ASSOCIATIVE:
								println("if(neighbor instanceof " + ai.getTargetAEClass().name() + ")");
								println("{");
								incIndent();
									println("((" + ai.getTargetAEClass().name() + ") neighbor).deleteAssociation(object, AssociationID);");
									println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAEClass().name() + ".class, ((" + ai.getTargetAEClass().name() + ") neighbor).ID(), neighbor, null));");
								decIndent();
								println("}");
								break;
							case MEMBER2MEMBER:
								println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
								println("{");
								incIndent();
									println("if(((" + ai.getTargetAE().cls().name() + ") neighbor)." + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()).nameAsRolename() + "() != null)");
									println("{");
									incIndent();
										println("((" + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()) + ") ((" + ai.getTargetAE().cls().name() + ") neighbor)." + ModelUtilities.getAssociativeClass(ai.getSourceAEClass(), ai.getTargetAEClass()).nameAsRolename() + "()).deleteAssociation(object, AssociationID);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, null, 0, null, neighbor));");
									decIndent();
									println("}");
								decIndent();
								println("}");
								break;
							case ONE2ONE:
								if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.set" + capitalize(targetRole) + "(null);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										decIndent();
										println("}");
									}
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& ai.getTargetAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).deleteAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										decIndent();
										println("}");
									}
								}else{
									if (theClass == ai.getSourceAE().cls()
													&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.set" + capitalize(targetRole) + "(null);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										decIndent();
										println("}");
									}
									if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
													&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).deleteAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										decIndent();
										println("}");
									}
								}
								break;
							case ONE2MANY:
								if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered())){
									println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
									println("{");
									incIndent();
										println("((" + ai.getTargetAE().cls().name() + ") neighbor).deleteAssociation(object, AssociationID);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
									decIndent();
									println("}");
								}
								if (theClass == ai.getSourceAE().cls() && (!ai.getTargetAE().isCollection() && !ai.getTargetAE().isOrdered())
												&& ai.getSourceAE().cls() != ai.getTargetAE().cls()){
									println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
									println("{");
									incIndent();
										println("object.set" + capitalize(targetRole) + "(null);");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
									decIndent();
									println("}");
								}
								break;
							case MANY2MANY:
								if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.remove" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
//											println("object.set" + capitalize(targetRole) + "(object." + targetRole  + "());");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											decIndent();
										println("}");
									}
									if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& ai.getTargetAE().getAnnotation("holder") != null){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).deleteAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											decIndent();
										println("}");
									}
								}else{
									if (theClass == ai.getSourceAE().cls()
													&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("object.remove" + capitalize(targetRole) + "((" + ai.getTargetAE().cls().name() + ") neighbor);");
//											println("object.set" + capitalize(targetRole) + "(object." + targetRole  + "());");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");	
										decIndent();
										println("}");
									}
									if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
													&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
										println("if(neighbor instanceof " + ai.getTargetAE().cls().name() + ")");
										println("{");
										incIndent();
											println("((" + ai.getTargetAE().cls().name() + ") neighbor).deleteAssociation(object, AssociationID);");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.DATABASE, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
											println("Transactions.AddCommand(new Command(getAccess(), CommandType.DELETE_ASSOCIATION, CommandTargetLayer.VIEW, oldID, " + theClass.name().toLowerCase() + ", object, " + ai.getTargetAE().cls().name() + ".class, ((" + ai.getTargetAE().cls().name() + ") neighbor).ID(), neighbor, null));");
										decIndent();
										println("}");
									}
								}
								break;
							default:
								System.out.println("ERROR: " + ai);
						}
					}
					println("object.checkModelRestrictions();");
					println("object.checkRestrictions();");
					println("Transactions.getSession().store(object);");
					decIndent();
				println("}else{");
					incIndent();
						println("Transactions.CancelTransaction(\"Failed in DeleteAssociation\", \"the " + theClass.name() + " does not exists\");");
					decIndent();
				println("}");
				decIndent();
			println("}catch(Exception e){");
			incIndent();
				println("Transactions.CancelTransaction(\"Failed in DeleteAssociation\", \"Error ocurred while trying to delete the association of " + theClass.name() + "\");");
			decIndent();
			println("}");
			decIndent();
		println("}");
		println();
	}
	
	private void printNotityDeletion(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object that will be removed");
		println("**********************************************************************/");
		println("public synchronized void notifyDeletion(" + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
			incIndent();		
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();
				String targetRole = targetAE.name();
						
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("if(" + theClass.name().toLowerCase() + "." + targetRole + "() != null)");
						incIndent();
							println(theClass.name().toLowerCase() + "." + targetRole + "()." + "deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
						decIndent();
						break;
					case MEMBER2ASSOCIATIVE:
						if(ai.getTargetAE().isCollection()){
							println("for(" + ai.getTargetAEClass().name() + " x : " + theClass.name().toLowerCase() + "." + ai.getTargetAEClass().nameAsRolename() + "())");
							incIndent();
								println("x.deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
							decIndent();
						}else{
							println("if(" + theClass.name().toLowerCase() + "." + targetRole + "() != null)");
							incIndent();
								println(theClass.name().toLowerCase() + "." + targetRole + "()." + "deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
							decIndent();
						}
						break;
					case MEMBER2MEMBER:

						break;
					case ONE2ONE:
						println("if(" + theClass.name().toLowerCase() + "." + targetRole + "() != null)");
						incIndent();
							if(ai.getSourceAE().aggregationKind() == MAggregationKind.AGGREGATION
									|| ai.getSourceAE().aggregationKind() == MAggregationKind.COMPOSITION)
								println(theClass.name().toLowerCase() + "." + targetRole + "()." + "delete();");
							else
								println(theClass.name().toLowerCase() + "." + targetRole + "()." + "deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
						decIndent();
						break;
					case ONE2MANY:
						if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered())){
							println("for(" + ai.getTargetAE().cls().name() + " x : " + theClass.name().toLowerCase() + "." + targetRole + "())");
							incIndent();
								if(ai.getSourceAE().aggregationKind() == MAggregationKind.AGGREGATION
										|| ai.getSourceAE().aggregationKind() == MAggregationKind.COMPOSITION){
									println("x.delete();");
								}else{
									println("x.deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
								}
							decIndent();
						}
						if (theClass == ai.getSourceAE().cls() && (!ai.getTargetAE().isCollection() && !ai.getTargetAE().isOrdered())
										&& ai.getSourceAE().cls() != ai.getTargetAE().cls()){
							println("if(" + theClass.name().toLowerCase() + "." + targetRole + "() != null)");
							incIndent();
								if(ai.getSourceAE().aggregationKind() == MAggregationKind.AGGREGATION
										|| ai.getSourceAE().aggregationKind() == MAggregationKind.COMPOSITION){								
										println("x.delete();");
								}else{
									println(theClass.name().toLowerCase() + "." + targetRole + "()." + "deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
								}
							decIndent();
						}
						break;
					case MANY2MANY:
						println("for(" + ai.getTargetAE().cls().name() + " x : " + theClass.name().toLowerCase() + "." + targetRole + "())");
						incIndent();
							if(ai.getSourceAE().aggregationKind() == MAggregationKind.AGGREGATION
									|| ai.getSourceAE().aggregationKind() == MAggregationKind.COMPOSITION)
								println("x.delete();");
							else
								println("x.deleteAssociation(" + theClass.name().toLowerCase() + ", \"" + ai.getName() + "Association\");");
						decIndent();
						break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
			List<AssociationInfo> allAssociations = new ArrayList<AssociationInfo>();
			for(MClass parent : theClass.allParents())
				allAssociations.addAll(AssociationInfo.getAssociationsInfo(parent));
			List<AssociationInfo> directAssociations = new ArrayList<AssociationInfo>(AssociationInfo.getAssociationsInfo(theClass));
			allAssociations.removeAll(directAssociations);
			//if any parent as any association we need to notify our deletion do it can notify the associated class
			if(!allAssociations.isEmpty())
				println("super.notifyDeletion(" + theClass.name().toLowerCase() + ");");
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessNeededMethods(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the object to be identified");
		println("* @return the ID of the given object");
		println("**********************************************************************/");
		println("@Override");
		println("public int ID(Object " + theClass.name().toLowerCase() + ")");
		println("{");
		incIndent();
			println("return ((" + theClass.name() + ") " + theClass.name().toLowerCase() +").ID();");
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* @return the type of the object which this class works with");
		println("**********************************************************************/");
		println("@Override");
		println("public Class<?> getType()");
		println("{");
		incIndent();
			println("return " + theClass.name() +".class;");
		decIndent();
		println("}");
		println();

		println("/**********************************************************************");
		println("* @param the neibor object to be identified");
		println("* @return the ID of the given neibor");
		println("**********************************************************************/");
		println("@Override");
		println("public int getNeiborID(Object neibor)");
		println("{");
		incIndent();
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();						
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return ((" + targetAE.cls().name() + ") neibor).ID();");
						decIndent();
						break;
					case MEMBER2ASSOCIATIVE:
						println("if(neibor instanceof " + ai.getTargetAEClass().name() + ")");
						incIndent();
							println("return ((" + ai.getTargetAEClass().name() + ") neibor).ID();");
						decIndent();
						break;
					case MEMBER2MEMBER:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return ((" + targetAE.cls().name() + ") neibor).ID();");
						decIndent();
						break;
					case ONE2ONE:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return ((" + targetAE.cls().name() + ") neibor).ID();");
						decIndent();
						break;
					case ONE2MANY:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return ((" + targetAE.cls().name() + ") neibor).ID();");
						decIndent();
						break;
					case MANY2MANY:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return ((" + targetAE.cls().name() + ") neibor).ID();");
						decIndent();
						break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
			println("return 0;");
		decIndent();
		println("}");
		println();
		
		
		println("/**********************************************************************");
		println("* @param the neibor object which this class can work with");
		println("* @return the type of the neibor which this class can work with");
		println("**********************************************************************/");
		println("@Override");
		println("public Class<?> getNeiborType(Object neibor)");
		println("{");
		incIndent();
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();						
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return " + targetAE.cls().name() + ".class;");
						decIndent();
						break;
					case MEMBER2ASSOCIATIVE:
						println("if(neibor instanceof " + ai.getTargetAEClass().name() + ")");
						incIndent();
							println("return " + ai.getTargetAEClass().name() + ".class;");
						decIndent();
						break;
					case MEMBER2MEMBER:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return " + targetAE.cls().name() + ".class;");
						decIndent();
						break;
					case ONE2ONE:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return " + targetAE.cls().name() + ".class;");
						decIndent();
						break;
					case ONE2MANY:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return " + targetAE.cls().name() + ".class;");
						decIndent();
						break;
					case MANY2MANY:
						println("if(neibor instanceof " + targetAE.cls().name() + ")");
						incIndent();
							println("return " + targetAE.cls().name() + ".class;");
						decIndent();
						break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
			println("return null;");
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAccessServerPersistenceMethods(MClass theClass)
	{
		println("//Server specific methods - Start");
		printAccessServerInsert(theClass);
		printAccessServerUpdate(theClass);
		
		if(!theClass.associations().isEmpty() || ModelUtilities.isAssociativeClass(theClass)){
			printAccessServerInsertAssociation(theClass);
			printAccessServerDeleteAssociation(theClass);
		}
		println("//Server specific methods - End");
	}
	
	public void printAccessServerInsert(MClass theClass)
	{
		List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
		for (MClass theParentClass : theClass.allParents()){
			List<AttributeInfo> inheritedAttributes_temp = new ArrayList<AttributeInfo>();
			for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
				inheritedAttributes_temp.add(attribute);
			inheritedAttributes.addAll(0, inheritedAttributes_temp);
		}
		
		println("/**********************************************************************");
		println("* @param the object to be inserted");
		println("* @return a cleaned of associations object that be inserted");
		println("**********************************************************************/");
		println("@Override");
		println("public Object serverInsert(Object object)");
		println("{");
		incIndent();
		if(!theClass.isAbstract()){
			println("if(object instanceof " + theClass.name() + "){");
			incIndent();
				print(theClass.name() + " x = new " + theClass.name() + "(");
				for (int i = 0; i < inheritedAttributes.size(); i++)
				{
					if(inheritedAttributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !inheritedAttributes.get(i).getName().equals("ID")){
						print("((" + theClass.name() + ") object)." + inheritedAttributes.get(i).getName() + "()");
						if (i < inheritedAttributes.size() - 1)
							print(", ");
					}
				}
				
				List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
				for(AttributeInfo att : AttributeInfo.getAttributesInfo(theClass))
					if((att.getKind() == AssociationKind.ASSOCIATIVE2MEMBER || att.getKind() == AssociationKind.NONE) && !att.getName().equals("ID"))
						attributes.add(att);
				
				if (inheritedAttributes.size() > 0 && attributes.size() > 0)
					print(", ");
				for (int i = 0; i < attributes.size(); i++)
				{
					if((attributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER || attributes.get(i).getKind() == AssociationKind.NONE) && !attributes.get(i).getName().equals("ID"))
						if(attributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER)//just for insert in server (null,null)
							print("null");
						else
							print("((" + theClass.name() + ") object)." + attributes.get(i).getName() + "()");
					else
						print("((" + theClass.name() + ") object)." + attributes.get(i).getName() + "()");
					if (i < attributes.size() - 1)
						print(", ");
					
				}
				println(");");
				println("x.checkModelRestrictions();");
				println("x.checkRestrictions();");
				println("return x;");
			decIndent();
			println("}else");
			incIndent();
				println("return null;");
			decIndent();
		}else
			println("return object;");
		decIndent();
		println("}");
		println();
	}
	
	public void printAccessServerUpdate(MClass theClass)
	{
		List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
		for (MClass theParentClass : theClass.allParents()){
			List<AttributeInfo> inheritedAttributes_temp = new ArrayList<AttributeInfo>();
			for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
				inheritedAttributes_temp.add(attribute);
			inheritedAttributes.addAll(0, inheritedAttributes_temp);
		}
		
		println("/**********************************************************************");
		println("* @param the object to be update");
		println("* @return updated version of the given object");
		println("**********************************************************************/");
		println("@Override");
		println("public void serverUpdate(Object oldObject, Object newObject)");
		println("{");
		if(!theClass.isAbstract()){
			incIndent();
				println("if(oldObject instanceof " + theClass.name() + " && newObject instanceof " + theClass.name() + "){");
				incIndent();
					for (int i = 0; i < inheritedAttributes.size(); i++)
					{
						if(inheritedAttributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !inheritedAttributes.get(i).getName().equals("ID")){
							println("((" + theClass.name() + ") oldObject).set" + capitalize(inheritedAttributes.get(i).getName()) + "(((" + theClass.name() + ") newObject)." + inheritedAttributes.get(i).getName() + "());");
						}
					}
					List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
					for (int i = 0; i < attributes.size(); i++)
					{
						if(attributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !attributes.get(i).getName().equals("ID")){
							println("((" + theClass.name() + ") oldObject).set" + capitalize(attributes.get(i).getName()) + "(((" + theClass.name() + ") newObject)." + attributes.get(i).getName() + "());");
						}
					}
					println("((" + theClass.name() + ") oldObject).setID();");
				decIndent();
				println("}");
			decIndent();
		}
		println("}");
		println();
		
	}
	
	public void printAccessServerInsertAssociation(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the database session");
		println("* @param the object that will receive the new association");
		println("* @param the neibor that will be associated to");
		println("**********************************************************************/");
		println("@Override");
		println("public void serverInsertAssociation(Object oldObject, Object newNeibor)");
		println("{");
		incIndent();
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();
				String targetRole = targetAE.name();
				
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
						println("{");
						incIndent();
							println("boolean exists = false;");
							println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
							incIndent();
								println("exists = true;");
							decIndent();
							println("if(!exists)");
							println("{");
							incIndent();
								println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
								println("((" + theClass.name() + ") oldObject).setID();");
								println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
								println("((" + theClass.name() + ") oldObject).checkRestrictions();");
							decIndent();
							println("}");
						decIndent();
						println("}");
						break;
					case MEMBER2ASSOCIATIVE:
						println();
						println("//I'm not the holder class regarding the association between me and " + ai.getTargetAEClass().name());
						println("//i do not need to have code for this association insertion since the holder has it and ");
						println("//and the proper command was added when this action was made locally");
						break;
					case MEMBER2MEMBER:
						println();
						println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
						println("//i do not need to have code for this association insertion since the holder has it and ");
						println("//and the proper command was added when this action was made locally");
						break;
					case ONE2ONE:
						if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
								println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
									incIndent();
										println("exists = true;");
									decIndent();
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
										println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
										println("((" + theClass.name() + ") oldObject).checkRestrictions();");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
									&& ai.getTargetAE().getAnnotation("holder") != null){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association insertion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");

							}
						}else{
							if (theClass == ai.getSourceAE().cls()
											&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
									incIndent();
										println("exists = true;");
									decIndent();
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
										println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
										println("((" + theClass.name() + ") oldObject).checkRestrictions();");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association insertion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}
						break;
					case ONE2MANY:
						if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered())){
							println("");
							println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
							println("//i do not need to have code for this association insertion since the holder has it and ");
							println("//and the proper command was added when this action was made locally");
						}
						if (theClass == ai.getSourceAE().cls() && (!ai.getTargetAE().isCollection() && !ai.getTargetAE().isOrdered())
										&& ai.getSourceAE().cls() != ai.getTargetAE().cls()){
							println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
							println("{");
							incIndent();
								println("boolean exists = false;");
								println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
								incIndent();
									println("exists = true;");
								decIndent();
								println("if(!exists)");
								println("{");
								incIndent();
									println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
									println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
									println("((" + theClass.name() + ") oldObject).checkRestrictions();");
								decIndent();
								println("}");
							decIndent();
							println("}");
						}
						break;
					case MANY2MANY:
						if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
								println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("for(" + ai.getTargetAE().cls().name() + " x : ((" + theClass.name() + ") oldObject)." + targetRole + "())");
									incIndent();
										println("if(x.ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
										incIndent();
											println("exists = true;");
										decIndent();
									decIndent();
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).add" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
//										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(((" + theClass.name() + ") oldObject)." + targetRole + "());");
										println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
										println("((" + theClass.name() + ") oldObject).checkRestrictions();");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
									&& ai.getTargetAE().getAnnotation("holder") != null){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association insertion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}else{
							if (theClass == ai.getSourceAE().cls()
										&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("if(oldObject != null && newNeibor != null && oldObject instanceof " + theClass.name() + " && newNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("for(" + ai.getTargetAE().cls().name() + " x : ((" + theClass.name() + ") oldObject)." + targetRole + "())");
									incIndent();
										println("if(x.ID() == ((" + targetAE.cls().name() + ") newNeibor).ID())");
										incIndent();
											println("exists = true;");
										decIndent();
									decIndent();	
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).add" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") newNeibor);");
//										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(((" + theClass.name() + ") oldObject)." + targetRole + "());");
										println("((" + theClass.name() + ") oldObject).checkModelRestrictions();");
										println("((" + theClass.name() + ") oldObject).checkRestrictions();");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
										&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association insertion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}
					break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
		decIndent();
		println("}");
		println();
	}
	
	public void printAccessServerDeleteAssociation(MClass theClass)
	{
		println("/**********************************************************************");
		println("* @param the database session");
		println("* @param the object to whom will be removed the association");
		println("* @param the neibor that will be de-associated");
		println("**********************************************************************/");
		println("@Override");
		println("public void serverDeleteAssociation(Object oldObject, Object oldNeibor)");
		println("{");
		incIndent();
			for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
			{
				MAssociationEnd targetAE = ai.getTargetAE();
				String targetRole = targetAE.name();
				
				switch (ai.getKind())
				{
					case ASSOCIATIVE2MEMBER:
						println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
						println("{");
						incIndent();
							println("boolean exists = false;");
							println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
							incIndent();
								println("exists = true;");
							decIndent();
							println("if(exists)");
							println("{");
							incIndent();
								println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(null);");
							decIndent();
							println("}");
						decIndent();
						println("}");
						break;
					case MEMBER2ASSOCIATIVE:
						println();
						println("//I'm not the holder class regarding the association between me and " + ai.getTargetAEClass().name());
						println("//i do not need to have code for this association deletion since the holder has it ");
						println("//and the proper command was added when this action was made locally");
						break;
					case MEMBER2MEMBER:
						println();
						println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
						println("//i do not need to have code for this association deletion since the holder has it ");
						println("//and the proper command was added when this action was made locally");
						break;
					case ONE2ONE:
						if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
								println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
									incIndent();
										println("exists = true;");
									decIndent();
									println("if(exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(null);");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
									&& ai.getTargetAE().getAnnotation("holder") != null){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association deletion since the holder has it ");
								println("//and the proper command was added when this action was made locally");

							}
						}else{
							if (theClass == ai.getSourceAE().cls()
											&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
									incIndent();
										println("exists = true;");
									decIndent();
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(null);");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
											&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association deletion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}
						break;
					case ONE2MANY:
						if (theClass == ai.getSourceAE().cls() && (ai.getTargetAE().isCollection() || ai.getTargetAE().isOrdered())){
							println("");
							println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
							println("//i do not need to have code for this association deletion since the holder has it and ");
							println("//and the proper command was added when this action was made locally");
						}
						if (theClass == ai.getSourceAE().cls() && (!ai.getTargetAE().isCollection() && !ai.getTargetAE().isOrdered())
										&& ai.getSourceAE().cls() != ai.getTargetAE().cls()){
							println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
							println("{");
							incIndent();
								println("boolean exists = false;");
								println("if(((" + theClass.name() + ") oldObject)." + targetRole + "() != null && ((" + theClass.name() + ") oldObject)." + targetRole + "().ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
								incIndent();
									println("exists = true;");
								decIndent();
								println("if(exists)");
								println("{");
								incIndent();
									println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(null);");
								decIndent();
								println("}");
							decIndent();
							println("}");
						}
						break;
					case MANY2MANY:
						if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null){
								println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("for(" + ai.getTargetAE().cls().name() + " x : ((" + theClass.name() + ") oldObject)." + targetRole + "())");
									incIndent();
										println("if(x.ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
										incIndent();
											println("exists = true;");
										decIndent();
									decIndent();
									println("if(exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).remove" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") oldNeibor);");
//										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(((" + theClass.name() + ") oldNeibor)." + targetRole + "());");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
									&& ai.getTargetAE().getAnnotation("holder") != null){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association deletion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}else{
							if (theClass == ai.getSourceAE().cls()
										&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("if(oldObject != null && oldNeibor != null && oldObject instanceof " + theClass.name() + " && oldNeibor instanceof " + targetAE.cls().name() + ")");
								println("{");
								incIndent();
									println("boolean exists = false;");
									println("for(" + ai.getTargetAE().cls().name() + " x : ((" + theClass.name() + ") oldObject)." + targetRole + "())");
									incIndent();
										println("if(x.ID() == ((" + targetAE.cls().name() + ") oldNeibor).ID())");
										incIndent();
											println("exists = true;");
										decIndent();
									decIndent();	
									println("if(!exists)");
									println("{");
									incIndent();
										println("((" + theClass.name() + ") oldObject).remove" + capitalize(targetRole) + "((" + targetAE.cls().name() + ") oldNeibor);");
//										println("((" + theClass.name() + ") oldObject).set" + capitalize(targetRole) + "(((" + theClass.name() + ") oldObject)." + targetRole + "());");
									decIndent();
									println("}");
								decIndent();
								println("}");
							}
							if (theClass == ai.getSourceAE().cls() && ai.getSourceAE().cls() != ai.getTargetAE().cls()
										&& theClass != util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls())){
								println("");
								println("//I'm not the holder class regarding the association between me and " + targetAE.cls().name());
								println("//i do not need to have code for this association deletion since the holder has it and ");
								println("//and the proper command was added when this action was made locally");
							}
						}
					break;
					default:
						System.out.println("ERROR: " + ai);
				}
			}
		decIndent();
		println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printDB4OModelSpecification(String sourcePath, String target, MClass theClass)
	{
		openMethodForInput(sourcePath + "JUSE4Android/org/quasar/usemodel2Android/persistence", "Database", "Database", "dbConfig", null);
		int updateDepth = 1;

		//preciso de ver se preciso de fazer para as hierarquias - ou seja - se o updateDepth é maior que 2
		
		
		for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
		{
			if(ai.getKind().toString().equals(AssociationKind.MANY2MANY.toString())){
				if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
					if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null)
						updateDepth = 2;
				}else
					if (theClass == ai.getSourceAE().cls()	&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls()))
						updateDepth = 2;
			}	
		}
		
		println("configuration.common().objectClass(" + theClass.name() + ".class).objectField(\"ID\").indexed(true);");
		println("configuration.common().objectClass(" + theClass.name() + ".class).updateDepth(" + updateDepth + ");");

		closeMethodForInput();
		
		openMethodForInput(sourcePath + "JUSE4Android/org/quasar/usemodel2Android/persistence", "Database", "Database", "dbServerConfig", null);		
		for (AssociationInfo ai : AssociationInfo.getAssociationsInfo(theClass))
		{
			if(ai.getKind().toString().equals(AssociationKind.MANY2MANY.toString())){
				if(ai.getSourceAE().getAnnotation("holder") != null || ai.getTargetAE().getAnnotation("holder") != null){
					if(theClass == ai.getSourceAE().cls() && ai.getSourceAE().getAnnotation("holder") != null)
						updateDepth = 2;
				}else
					if (theClass == ai.getSourceAE().cls()	&&  theClass == util.moreComplexClass(ai.getSourceAE().cls(), ai.getTargetAE().cls()))
						updateDepth = 2;
			}	
		}
		println("configuration.common().objectClass(" + theClass.name() + ".class).objectField(\"ID\").indexed(true);");
		println("configuration.common().objectClass(" + theClass.name() + ".class).updateDepth(" + updateDepth + ");");
		closeMethodForInput();

	}
}