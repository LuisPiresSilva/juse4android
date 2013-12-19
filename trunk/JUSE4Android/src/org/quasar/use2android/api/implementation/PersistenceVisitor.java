package org.quasar.use2android.api.implementation;

import java.util.List;

import org.quasar.juse.api.implementation.FileUtilities; 
import org.tzi.use.uml.mm.MClass;

public abstract class PersistenceVisitor extends FileUtilities implements IPersistenceVisitor {

	@Override
	public abstract void printFileHeader(String typeName, String layerName);

	@Override
	public abstract void printClassHeader(String fileName, String layerName, String businessLayerName, List<MClass> domainClasses);
	
	@Override
	public abstract void printAllInstances();

	@Override
	public abstract void printAttributes();
	
	@Override
	public abstract void printDefaultDBMethods();
	
	@Override
	public abstract void printDefaultDBConfigMethods(List<MClass> list);
	
	@Override
	public abstract void printGetters();
}
