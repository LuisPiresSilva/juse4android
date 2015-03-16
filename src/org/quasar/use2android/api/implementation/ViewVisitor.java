package org.quasar.use2android.api.implementation;

public abstract class ViewVisitor implements IViewVisitor {

	@Override
	public abstract void generateFolders();

	@Override
	public abstract void generateXMLs();
}
