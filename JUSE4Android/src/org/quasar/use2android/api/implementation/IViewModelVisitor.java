package org.quasar.use2android.api.implementation;

import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;

public interface IViewModelVisitor {

	/***********************************************************
	* @param typeName
	*            The name of the type (enumerated type, class or interface) to create
	* @param layerName
	* 			The name of the layer where the file is to be created
	***********************************************************/
	public void printFileHeader(String typeName, String layerName);

	
//	******************** --- DefaultClasses - Start --- *****************************
	
	public void printMasterActivity();

	public void printApplicationClass();
	
	public void printLauncher(MModel model);
	
//	******************** --- DefaultClasses - End --- *****************************

	
//	******************** --- Activity - Start --- *****************************
	
	public void printActivity_ClassHeader(MClass theClass, String layerName);

	public void printActivity_Attributes(MClass theClass);
	
	public void printActivity_UsefullMethods(MClass theClass);
	
	public void printActivity_onSaveInstanceState(MClass theClass);
	
	public void printActivity_onCreate(MClass theClass);
	
	public void printActivity_onStart(MClass theClass);
	
	public void printActivity_onResume(MClass theClass);
	
	public void printActivity_onPause(MClass theClass);
	
	public void printActivity_onBackPressed(MClass theClass);
	
	public void printActivity_onDestroy(MClass theClass);
	
	public void printActivity_onItemSelected(MClass theClass);
	
	public void printActivity_onOptionsItemSelected(MClass theClass);
	
	public void printActivity_onActivityResult(MClass theClass);
	
	public void printActivity_onDetailOK(MClass theClass);
	
	public void printActivity_onDetailCancel(MClass theClass);
	
	public void printActivity_addToList(MClass theClass);
	
	public void printActivity_propertyChange(MClass theClass);


//	******************** --- Activity - End --- *****************************

	
	
	
//	******************** --- Fragment - Detail - Start --- *****************************
	public void printDetailFragment_ClassHeader(MClass theClass, String layerName);

	public void printDetailFragment_Attributes(MClass theClass);
	
	public void printDetailFragment_DefaultConstructor(MClass theClass);
	
	public void printDetailFragment_onCreate(MClass theClass);
	
	public void printDetailFragment_onSaveInstanceState(MClass cls);
	
	public void printDetailFragment_onDestroy(MClass theClass);
	
	public void printDetailFragment_onActivityCreated(MClass theClass);
	
	public void printDetailFragment_onAttach(MClass theClass);
	
	public void printDetailFragment_onCreateView(MClass theClass);
	
	public void printDetailFragment_VisibilityState(MClass theClass);
	
	public void printDetailFragment_SetInputMethod(MClass theClass);
	
	public void printDetailFragment_setViewDetailData(MClass theClass);
	
	public void printDetailFragment_ActionViewDetail(MClass theClass);
	
	public void printDetailFragment_setViewNewOrEditData(MClass theClass);

	public void printDetailFragment_ActionViewNew(MClass theClass);
	
	public void printDetailFragment_ActionViewEdit(MClass theClass);
	
	public void printDetailFragment_InnerCallBackInterface(MClass theClass);
	
	public void printDetailFragment_CallBackDeclaration(MClass theClass);
	
	public void printDetailFragment_ScreenClickListeners(MClass theClass);
	
	public void printDetailFragment_BusinessListeners(MClass theClass);

//	******************** --- Fragment - Detail - End --- *****************************

	
	
//	******************** --- Fragment - NavigationBar - Start --- *****************************
	public void printNavigationBarFragment_ClassHeader(MClass theClass, String layerName);

	public void printNavigationBarFragment_Attributes(MClass theClass);
	
	public void printNavigationBarFragment_DefaultConstructor(MClass theClass);
	
	public void printNavigationBarFragment_onCreate(MClass theClass);
	
	public void printNavigationBarFragment_onSaveInstanceState(MClass theClass);
	
	public void printNavigationBarFragment_onDestroy(MClass theClass);
	
	public void printNavigationBarFragment_onActivityCreated(MClass theClass);
	
	public void printNavigationBarFragment_onAttach(MClass theClass);
	
	public void printNavigationBarFragment_onCreateView(MClass theClass);
	
	public void printNavigationBarFragment_VisibilityState(MClass theClass);
	
	public void printNavigationBarFragment_setViewingObject(MClass theClass);
	
	public void printNavigationBarFragment_refreshNavigationBar(MClass theClass);
	
	public void printNavigationBarFragment_prepareView(MClass theClass);
	
	public void printNavigationBarFragment_setNumberAssociation(MClass theClass);
	
	public void printNavigationBarFragment_objectValidation(MClass theClass);

	public void printNavigationBarFragment_ScreenClickListeners(MClass theClass);
	
	public void printNavigationBarFragment_BusinessListeners(MClass theClass);
	
//	******************** --- Fragment - NavigationBar - End --- *****************************
	
	
	
	
//	******************** --- ListViewHolder - Start --- *****************************
	public void printListViewHolder_ClassHeader(MClass theClass, String layerName);

	public void printListViewHolder_Attributes(MClass theClass);

	public void printListViewHolder_ViewHolderInnerClass(MClass theClass);

	public void printListViewHolder_RequiredMethods(MClass theClass);
//	******************** --- ListViewHolder - End --- *****************************



}
