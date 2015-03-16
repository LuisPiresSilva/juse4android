package org.quasar.use2android.api.implementation;

import java.util.List;

import org.tzi.use.uml.mm.MClass;

public interface IPersistenceVisitor{

	public void printFileHeader(String typeName, String layerName);

	public void printClassHeader(String fileName, String layerName, String businessLayerName, List<MClass> domainClasses);
	
	public void printAllInstances();

	public void printAttributes();
	
	public void printDefaultDBMethods();
	
	public void printDefaultDBConfigMethods(List<MClass> list);
	
	public void printGetters();
}
