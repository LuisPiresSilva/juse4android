package org.quasar.use2android.api.implementation;

import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.AssociationKind;
import org.quasar.juse.api.implementation.AttributeInfo;
import org.quasar.juse.api.implementation.ModelUtilities;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MElementAnnotation;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.type.Type;
import org.tzi.use.uml.ocl.type.TypeFactory;
import org.tzi.use.uml.sys.soil.MAttributeAssignmentStatement;

public class DomainModelRestrictions {

	public DomainModelRestrictions(){
		
	}
	
	public void checkAssociations(AssociationInfo association){
		if(association.getKind() == AssociationKind.MANY2MANY || association.getKind() == AssociationKind.ONE2ONE){
			if(association.getSourceAE().getAnnotation("holder") != null &&
				association.getTargetAE().getAnnotation("holder") != null)
				throw new RuntimeException("The " + association.getKind().toString() + " association type between " + association.getSourceAE().name() + " and " + association.getTargetAE().name() + 
						"\nMust contain one, and only one, 'holder' annotation specifying which class will hold the data");
		}
	}
	
	public void checkAnnotations(){
		
	}
	
	public void checkClasses(MClass theClass){
		
	}
	
	public void checkAttributes(MClass theClass){
		if(theClass.isAnnotated() && theClass.getAnnotation("domain") != null) 
			for(MAttribute att : theClass.attributes())
				if(att.type().isInteger() && att.name().equals("ID"))
						throw new RuntimeException("The domain class " + theClass.name() + " contains an attribute named 'ID' of the type 'int'" +
								"\nSince the same attribute will be generated automatically for every Domain class this attribute is not allowed in the domain model");
	
	}
	
	public void rectifyHolders(AssociationInfo association, ModelUtilities util, MModel model){
		if(association.getSourceAE().getAnnotation("holder") == null && isSuperClass(association.getSourceAE().cls(), model))
			association.getSourceAE().addAnnotation(new MElementAnnotation("holder"));
		if(association.getTargetAE().getAnnotation("holder") == null && isSuperClass(association.getTargetAE().cls(), model))
			association.getTargetAE().addAnnotation(new MElementAnnotation("holder"));
		
		if(association.getKind() == AssociationKind.MANY2MANY || association.getKind() == AssociationKind.ONE2ONE){
			if((association.getSourceAE().getAnnotation("holder") == null &&
					association.getTargetAE().getAnnotation("holder") == null))
					if(util.moreComplexClass(association.getSourceAE().cls(), association.getTargetAE().cls()) == 
							util.moreComplexClass(association.getTargetAE().cls(), association.getSourceAE().cls()))
						association.getTargetAE().addAnnotation(new MElementAnnotation("holder"));
		}
	}
	
	/***********************************************************
	* @param theClass to check
	* @return true if is subclass, false if not
	***********************************************************/
	private boolean isSubClass(MClass theClass, MModel model)
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
	private boolean isSuperClass(MClass theClass, MModel model)
	{
		for(MClass x : model.classes())
			if( (!theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass))//middle super
				|| (theClass.parents().isEmpty() && x != theClass && x.isSubClassOf(theClass)) )//top super
				return true;
		return false;
	}
}
