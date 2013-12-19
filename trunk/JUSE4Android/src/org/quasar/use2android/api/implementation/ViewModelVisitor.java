package org.quasar.use2android.api.implementation;

import org.quasar.juse.api.implementation.FileUtilities;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;

public abstract class ViewModelVisitor extends FileUtilities implements IViewModelVisitor 
{
	@Override
	public abstract void printFileHeader(String typeName, String layerName);
	

//	******************** --- DefaultClasses - Start --- *****************************
	
	@Override
	public abstract void printMasterActivity();

	@Override
	public abstract void printApplicationClass();
	
	@Override
	public abstract void printLauncher(MModel model);
	
//	******************** --- DefaultClasses - End --- *****************************
	
//	******************** --- Activity - Start --- *****************************
	
	@Override
	public abstract void printActivity_ClassHeader(MClass theClass, String layerName);

	@Override
	public abstract void printActivity_Attributes(MClass theClass);
	
	@Override
	public abstract void printActivity_UsefullMethods(MClass theClass);
	
	@Override
	public abstract void printActivity_onSaveInstanceState(MClass theClass);
	
	@Override
	public abstract void printActivity_onCreate(MClass theClass);
	
	@Override
	public abstract void printActivity_onStart(MClass theClass);
	
	@Override
	public abstract void printActivity_onResume(MClass theClass);
	
	@Override
	public abstract void printActivity_onPause(MClass theClass);
	
	@Override
	public abstract void printActivity_onBackPressed(MClass theClass);
	
	@Override
	public abstract void printActivity_onDestroy(MClass theClass);
	
	@Override
	public abstract void printActivity_onItemSelected(MClass theClass);
	
	@Override
	public abstract void printActivity_onOptionsItemSelected(MClass theClass);
	
	@Override
	public abstract void printActivity_onActivityResult(MClass theClass);
	
	@Override
	public abstract void printActivity_onDetailOK(MClass theClass);
	
	@Override
	public abstract void printActivity_onDetailCancel(MClass theClass);
	
	@Override
	public abstract void printActivity_addToList(MClass theClass);
	
	@Override
	public abstract void printActivity_propertyChange(MClass theClass);


//	******************** --- Activity - End --- *****************************
	
	
	
//	******************** --- Fragment - Detail - Start --- *****************************
	@Override
	public abstract void printDetailFragment_ClassHeader(MClass theClass, String layerName);

	@Override
	public abstract void printDetailFragment_Attributes(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_DefaultConstructor(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_onCreate(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_onSaveInstanceState(MClass cls);
	
	@Override
	public abstract void printDetailFragment_onDestroy(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_onActivityCreated(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_onAttach(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_onCreateView(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_VisibilityState(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_SetInputMethod(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_setViewDetailData(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_ActionViewDetail(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_setViewNewOrEditData(MClass theClass);

	@Override
	public abstract void printDetailFragment_ActionViewNew(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_ActionViewEdit(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_InnerCallBackInterface(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_CallBackDeclaration(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_ScreenClickListeners(MClass theClass);
	
	@Override
	public abstract void printDetailFragment_BusinessListeners(MClass theClass);

//	******************** --- Fragment - Detail - End --- *****************************

	
	
//	******************** --- Fragment - NavigationBar - Start --- *****************************
	@Override
	public abstract void printNavigationBarFragment_ClassHeader(MClass theClass, String layerName);

	@Override
	public abstract void printNavigationBarFragment_Attributes(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_DefaultConstructor(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onCreate(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onSaveInstanceState(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onDestroy(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onActivityCreated(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onAttach(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_onCreateView(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_VisibilityState(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_setViewingObject(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_refreshNavigationBar(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_prepareView(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_setNumberAssociation(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_objectValidation(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_ScreenClickListeners(MClass theClass);
	
	@Override
	public abstract void printNavigationBarFragment_BusinessListeners(MClass theClass);
	
//	******************** --- Fragment - NavigationBar - End --- *****************************
	
	
	
	
//	******************** --- ListViewHolder - Start --- *****************************
	@Override
	public abstract void printListViewHolder_ClassHeader(MClass theClass, String layerName);
	@Override
	public abstract void printListViewHolder_Attributes(MClass theClass);
	@Override
	public abstract void printListViewHolder_ViewHolderInnerClass(MClass theClass);
	@Override
	public abstract void printListViewHolder_RequiredMethods(MClass theClass);
//	******************** --- ListViewHolder - End --- *****************************



}
