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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.BasicFacade;
import org.quasar.juse.api.implementation.FileUtilities;
import org.quasar.juse.api.implementation.JavaTypes;
import org.quasar.juse.api.implementation.ModelUtilities;

import org.tzi.use.uml.mm.MAssociationClass;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MOperation;
import org.tzi.use.uml.ocl.type.EnumType;
import org.tzi.use.uml.ocl.type.Type;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.EnumValue;
import org.tzi.use.uml.ocl.value.IntegerValue;
import org.tzi.use.uml.ocl.value.RealValue;
import org.tzi.use.uml.ocl.value.StringValue;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MLinkObject;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MObjectState;

/***********************************************************
 * @author fba 25 de Abr de 2012
 * 
 ***********************************************************/
public class PrototypeGeneratorFacade extends BasicFacade
{
	private Map<Integer, Object>	objectMapper	= null;
	
	public PrototypeGeneratorFacade()
	{
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_PrototypeGeneratorFacade#javaGeneration(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public void androidGeneration(final String author, final String projectName, final String javaWorkspace, final String basePackageName, final String businessLayerName,
			final String presentationLayerName, final String persistenceLayerName,
			final String libraryDirectory, final String db4oCoreJar, final String db4oCsJar, final String db4oOptionalJar,
			final String user, final String pass, final String port, final String ip)
	{
		if (getSystem().model() == null)
		{
			System.out.println("Please compile the specification first!");
			return;
		}
		
		String PROJECTNAME;
		if(projectName.equals(""))
			PROJECTNAME = getSystem().model().name() + "Android";
		else
			PROJECTNAME = projectName;
		
		String BASEPACKAGENAME = basePackageName + PROJECTNAME;
		
		
		System.out.println("\nJava plugin for USE version 1.0.6, Copyright (C) 2012-2013 QUASAR Group");
		
		System.out.println("\n\t - USE to Android Model Validation - Validating...");
		DomainModelRestrictions secondValidator = new DomainModelRestrictions();
		for (MClass cls : getSystem().model().classes()){
			secondValidator.checkAttributes(cls);
			for(AssociationInfo association : AssociationInfo.getAssociationsInfo(cls))
				secondValidator.checkAssociations(association);
		}
		System.out.println("\n\t - USE to Android Model Validation - Model passed");
		
		System.out.println("\n\t - generating Code for " + getSystem().model().name() + "...");
		
		String targetDirectory = javaWorkspace + "/" + PROJECTNAME + "/src/" + BASEPACKAGENAME.replace('.', '/');
		String businessDirectory = targetDirectory	+ "/" + businessLayerName;
		String presentationDirectory = targetDirectory + "/" + presentationLayerName;
		String persistenceDirectory = targetDirectory + "/" + persistenceLayerName;
		String utilsDirectory = targetDirectory + "/" + "utils";
		String utilsLayerName = "utils";
		String libraryPath = javaWorkspace + "/" + PROJECTNAME + "/libs";
		String USER = user;
		String PASS = pass;
		String PORT = port;
		String IP;
		if(ip.equals("0.0.0.0"))
			IP = "10.0.2.2";
		else
			IP = ip;
				
		System.out.println("\n\t\t - generating Android XML code...");
		
		ModelToXMLUtilities statistics = new ModelToXMLUtilities();
		AndroidViewLayer ViewVisitor = new AndroidViewLayer(BASEPACKAGENAME, PROJECTNAME, getSystem().model(), author, javaWorkspace, presentationLayerName, statistics);
		ViewVisitor.generateFolders();
		ViewVisitor.generateXMLs();
		
		System.out.println("\n\t\t" + " - Android XML generation concluded:\n" + statistics.toString(getSystem().model().name()));
		
		BusinessVisitor visitor = new AndroidBusinessVisitor(getSystem().model(), author, BASEPACKAGENAME, businessLayerName, persistenceLayerName, presentationLayerName, utilsLayerName);

		System.out.println("\n\t\t - generating Database layer code...");
		
		FileUtilities.createDirectory(presentationDirectory);

		FileUtilities.createDirectory(persistenceDirectory);
		//we create an temporary java file so that we can use reflection in this project without having to import
		//the new project and later we copy this file to the project and remove it
		FileUtilities.copyAndCompileFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/", "Database.txt", javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/", "Database");
		FileUtilities.createDirectory(libraryPath);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oCoreJar, libraryPath + "/" + db4oCoreJar);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oCsJar, libraryPath + "/" + db4oCsJar);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oOptionalJar, libraryPath + "/" + db4oOptionalJar);
		
		System.out.println("\n\t\t - Database layer generation concluded");
		
		// visitAnnotations(e);

		// print user-defined data types
		for (EnumType t : getSystem().model().enumTypes())
		{
			if (FileUtilities.openOutputFile(businessDirectory, t.name() + ".java"))
			{
				// visitAnnotations(t);
				visitor.printEnumType(t, businessLayerName);
				FileUtilities.println();
				FileUtilities.closeOutputFile();
			}
		}

		// visit classes
		for (MClass cls : getSystem().model().classes())
		{
			if(cls.isAnnotated() && cls.getAnnotation("business") != null){
				if (FileUtilities.openOutputFile(businessDirectory, cls.name() + ".java"))
				{
					visitor.printClassHeader(cls, businessLayerName);
	
					FileUtilities.incIndent();
	
					visitor.printAllInstances(cls);
	
					visitor.printAttributes(cls);
	
					if (cls instanceof MAssociationClass)
						visitor.printAssociativeConstructor(cls);
					else
						visitor.printDefaultConstructor(cls);
	
					visitor.printParameterizedConstructor(cls);
	
					visitor.printParameterizedAttributeConstructor(cls);
					
					visitor.printBasicGettersSetters(cls);
	
					visitor.printNavigators(cls);
	
					visitor.printModelAssociationRestrictionsStateCheckers(cls);
					
					visitor.printBusinessControllerAccessMethods(cls);
					
					visitor.printOtherMethods(cls);
					
					for (MOperation op : cls.operations())
						visitor.printSoilOperation(op);
	
					visitor.printToString(cls);
	
					visitor.printCompareTo(cls);
					
					visitor.printInvariants(cls);
	
					FileUtilities.decIndent();
					FileUtilities.println("}");
	
					FileUtilities.closeOutputFile();
				}
				
				if (FileUtilities.openOutputFile(businessDirectory, cls.name() + "Access.java"))
				{
					visitor.printAccessClassHeader(cls, businessLayerName);

					FileUtilities.incIndent();

					visitor.printAccessDefaultConstructor(cls);

					visitor.printAccessSingleton(cls);
					
					visitor.printAccessObserverListener(cls);
					
					visitor.printAccessPersistanceMethods(cls);
					
					visitor.printAccessNeededMethods(cls);
					
					visitor.printAccessServerPersistenceMethods(cls);
					
					FileUtilities.decIndent();
					FileUtilities.println("}");

					FileUtilities.closeOutputFile();
				}
				
				visitor.printDB4OModelSpecification(javaWorkspace, cls);
			}
		}
		
		FileUtilities.replaceStringInFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/Database.java", "org.quasar.usemodel2Android.persistence", BASEPACKAGENAME + "." + persistenceLayerName);
		StringBuffer modelimports = new StringBuffer();
		for (MClass cls : getSystem().model().classes())
		{
			if(cls.isAnnotated() && cls.getAnnotation("business") != null)
				modelimports.append("import " + BASEPACKAGENAME + "." + businessLayerName + "." + cls.name() + ";\r\n");
		}
		FileUtilities.replaceStringInFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/Database.java", "//MODEL_CLASSES_IMPORT", modelimports.toString());
		FileUtilities.copyFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/Database.java", persistenceDirectory + "/" + "Database.java");
		FileUtilities.deleteFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/persistence/Database.java");
		
		
//		for (Integer n : JavaTypes.getTupleTypesCardinalities())
//			if (FileUtilities.openOutputFile(businessDirectory, "Tuple" + n + ".java"))
//			{
//				visitor.printTupleTypes(n, businessLayerName);
//				FileUtilities.closeOutputFile();
//			}

//		if (FileUtilities.openOutputFile(presentationDirectory, "Main_" + getSystem().model().name() + ".java"))
//		{
//			visitor.printMain();
//			FileUtilities.closeOutputFile();
//		}

		generateAndroidTemplates(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/defaultdata/java", BASEPACKAGENAME, targetDirectory,
				businessDirectory, businessLayerName, persistenceDirectory, persistenceLayerName, presentationDirectory,
				presentationLayerName, utilsDirectory, utilsLayerName,
				USER, PASS, PORT, IP);
		
		ViewModelVisitor VMvisitor = new AndroidViewModelVisitor(getSystem().model(), author, BASEPACKAGENAME, businessLayerName, persistenceLayerName, presentationLayerName, utilsLayerName);
		if (FileUtilities.openOutputFile(targetDirectory + "/", "/MasterActivity.java"))
			VMvisitor.printMasterActivity();
			
		if (FileUtilities.openOutputFile(targetDirectory + "/", getSystem().model().name() + "Memory.java"))
			VMvisitor.printApplicationClass();
		
		if (FileUtilities.openOutputFile(targetDirectory + "/", getSystem().model().name() + "Launcher.java"))
			VMvisitor.printLauncher(getSystem().model());
			
		for (MClass cls : getSystem().model().classes())
		{
			if(cls.isAnnotated() && cls.getAnnotation("business") != null){
				if (FileUtilities.openOutputFile(presentationDirectory + "/" + cls.name() , cls.name() + "Activity.java"))
				{
					VMvisitor.printActivity_ClassHeader(cls, presentationLayerName);
	
					FileUtilities.incIndent();
	
					VMvisitor.printActivity_Attributes(cls);
										
					VMvisitor.printActivity_UsefullMethods(cls);
					
					VMvisitor.printActivity_onSaveInstanceState(cls);
					
					VMvisitor.printActivity_onCreate(cls);
					
					VMvisitor.printActivity_onStart(cls);
					
					VMvisitor.printActivity_onResume(cls);
					
					VMvisitor.printActivity_onPause(cls);
					
					VMvisitor.printActivity_onBackPressed(cls);
					
					VMvisitor.printActivity_onDestroy(cls);
					
					VMvisitor.printActivity_onItemSelected(cls);
					
					VMvisitor.printActivity_onOptionsItemSelected(cls);
					
					VMvisitor.printActivity_onActivityResult(cls);
					
					VMvisitor.printActivity_onDetailOK(cls);
					
					VMvisitor.printActivity_onDetailCancel(cls);
					
					VMvisitor.printActivity_addToList(cls);
					
					VMvisitor.printActivity_propertyChange(cls);
					
					FileUtilities.decIndent();
					FileUtilities.println("}");
	
					FileUtilities.closeOutputFile();
				}
				
				if (FileUtilities.openOutputFile(presentationDirectory + "/" + cls.name() , cls.name() + "DetailFragment.java"))
				{
					VMvisitor.printDetailFragment_ClassHeader(cls, presentationLayerName);
	
					FileUtilities.incIndent();
	
					VMvisitor.printDetailFragment_Attributes(cls);
									
					VMvisitor.printDetailFragment_DefaultConstructor(cls);
					
					VMvisitor.printDetailFragment_onCreate(cls);
					
					VMvisitor.printDetailFragment_onDestroy(cls);
					
					VMvisitor.printDetailFragment_onActivityCreated(cls);
					
					VMvisitor.printDetailFragment_onAttach(cls);
					
					VMvisitor.printDetailFragment_onCreateView(cls);
					
					VMvisitor.printDetailFragment_VisibilityState(cls);
					
					VMvisitor.printDetailFragment_SetInputMethod(cls);
					
					VMvisitor.printDetailFragment_getViewDetail(cls);
					
					VMvisitor.printDetailFragment_ActionViewDetail(cls);
					
					VMvisitor.printDetailFragment_getViewNewOrEdit(cls);
	
					VMvisitor.printDetailFragment_ActionViewNew(cls);
					
					VMvisitor.printDetailFragment_ActionViewEdit(cls);
					
					VMvisitor.printDetailFragment_InnerCallBackInterface(cls);
					
					VMvisitor.printDetailFragment_CallBackDeclaration(cls);
					
					VMvisitor.printDetailFragment_ScreenClickListeners(cls);
					
					FileUtilities.decIndent();
					FileUtilities.println("}");
	
					FileUtilities.closeOutputFile();
				}
				
				if (FileUtilities.openOutputFile(presentationDirectory + "/" + cls.name() , cls.name() + "NavigationBarFragment.java"))
				{
					VMvisitor.printNavigationBarFragment_ClassHeader(cls, presentationLayerName);
	
					FileUtilities.incIndent();
	
					VMvisitor.printNavigationBarFragment_Attributes(cls);
					
					VMvisitor.printNavigationBarFragment_DefaultConstructor(cls);
					
					VMvisitor.printNavigationBarFragment_onCreate(cls);
					
					VMvisitor.printNavigationBarFragment_onDestroy(cls);
					
					VMvisitor.printNavigationBarFragment_onActivityCreated(cls);
					
					VMvisitor.printNavigationBarFragment_onAttach(cls);
					
					VMvisitor.printNavigationBarFragment_onCreateView(cls);
					
					VMvisitor.printNavigationBarFragment_VisibilityState(cls);
					
					VMvisitor.printNavigationBarFragment_setViewingObject(cls);
					
					VMvisitor.printNavigationBarFragment_refreshNavigationBar(cls);
					
					VMvisitor.printNavigationBarFragment_prepareView(cls);
					
					VMvisitor.printNavigationBarFragment_setNumberAssociation(cls);
					
					VMvisitor.printNavigationBarFragment_objectValidation(cls);

					VMvisitor.printNavigationBarFragment_ScreenClickListeners(cls);
					
					VMvisitor.printNavigationBarFragment_BusinessListeners(cls);
									
					FileUtilities.decIndent();
					FileUtilities.println("}");
	
					FileUtilities.closeOutputFile();
				}
				
				if (FileUtilities.openOutputFile(presentationDirectory + "/" + cls.name() , cls.name() + "ListViewHolder.java"))
				{
					VMvisitor.printListViewHolder_ClassHeader(cls, presentationLayerName);
	
					FileUtilities.incIndent();
	
					VMvisitor.printListViewHolder_Attributes(cls);
					
					VMvisitor.printListViewHolder_ViewHolderInnerClass(cls);
					
					VMvisitor.printListViewHolder_RequiredMethods(cls);
					
					FileUtilities.decIndent();
					FileUtilities.println("}");
	
					FileUtilities.closeOutputFile();
				}
			}
			
		}
		
		System.out.println("\n\t\t - Editing proguard code...");
		
		FileUtilities.openOutputForExistingFile(javaWorkspace + "/" + PROJECTNAME, "proguard-project.txt");
		generateDb4oProGuardRequirements();
		FileUtilities.closeOutputForExistingFile();
		
		System.out.println("\n\t\t - Edition to proguard code concluded");
		
		ModelUtilities util = new ModelUtilities(getSystem().model());
		System.out.println("\n\t - " + getSystem().model().name() + " code generation concluded (" + util.numberClasses() + " classes, " + util.numberAttributes()
						+ " attributes, " + util.numberOperations() + " operations)\n");
	}

	private void generateAndroidTemplates(String sourceDirectory, String basePackageName, String baseDirectory, String businessDirectory,
			String businessLayerName, String persistenceDirectory, String persistenceLayerName,
			String presentationDirectory, String presentationLayerName, String utilsDirectory,
			String utilsLayerName,
			String USER, String PASS, String PORT, String IP){	
		
		//file e localização
		Map<String,String> files = new HashMap<String,String>();
		files.put("Command", utilsLayerName);
		files.put("CommandType", utilsLayerName);
		files.put("CommandTargetLayer", utilsLayerName);
		files.put("DetailFragment", utilsLayerName);
		files.put("FragmentMethods", utilsLayerName);
		files.put("InheritanceListFragment", utilsLayerName);
		files.put("ListAdapter", utilsLayerName);
		files.put("ListFragmentController", utilsLayerName);
		files.put("ListViewHolder", utilsLayerName);
		files.put("ModelContracts", utilsLayerName);
		files.put("ModelMusts", businessLayerName);
		files.put("NavigationBarFragment", utilsLayerName);
		files.put("PropertyChangeEvent", utilsLayerName);
		files.put("PropertyChangeEvent", utilsLayerName);
		files.put("PropertyChangeListener", utilsLayerName);
		files.put("ServerActions", utilsLayerName);
		files.put("ServerInfo", utilsLayerName);
		files.put("Transactions", utilsLayerName);
		files.put("AndroidTransaction", utilsLayerName);
		files.put("UtilNavigate", utilsLayerName);
		files.put("Utils", utilsLayerName);
		files.put("WarningDialogFragment", utilsLayerName);
		files.put("LauncherGridViewAdapter", basePackageName);
		
		FileUtilities.createDirectory(utilsDirectory);
		for(String file : files.keySet()){
//			ca parvoice o switch nao aceita String que na sejam final :/
			String currentFile = "";
			if(files.get(file).equals(utilsLayerName)){
				FileUtilities.copyFile(sourceDirectory + "/" + file + ".txt", utilsDirectory + "/" + file + ".java");
				FileUtilities.replaceStringInFile(utilsDirectory + "/" + file + ".java", "org.quasar.usemodel2Android.defaultdata.java", basePackageName + "." + utilsLayerName);
				currentFile = utilsDirectory + "/" + file + ".java";
			}
			if(files.get(file).equals(businessLayerName)){
				FileUtilities.copyFile(sourceDirectory + "/" + file + ".txt", businessDirectory + "/" + file + ".java");
				FileUtilities.replaceStringInFile(businessDirectory + "/" + file + ".java", "org.quasar.usemodel2Android.defaultdata.java", basePackageName + "." + businessLayerName);
				currentFile = businessDirectory + "/" + file + ".java";
			}
			if(files.get(file).equals(basePackageName)){
				FileUtilities.copyFile(sourceDirectory + "/" + file + ".txt", baseDirectory + "/" + file + ".java");
				FileUtilities.replaceStringInFile(baseDirectory + "/" + file + ".java", "org.quasar.usemodel2Android.defaultdata.java", basePackageName);
				currentFile = baseDirectory + "/" + file + ".java";
			}

			if(!currentFile.equals("")){
				FileUtilities.replaceStringInFile(currentFile, "TARGET_PACKAGE", basePackageName);
				FileUtilities.replaceStringInFile(currentFile, "UTILS_PACKAGE", basePackageName + "." + utilsLayerName);
				FileUtilities.replaceStringInFile(currentFile, "BUSINESS_PACKAGE", basePackageName + "." + businessLayerName);
				FileUtilities.replaceStringInFile(currentFile, "DATABASE_PACKAGE", basePackageName + "." + persistenceLayerName);
				//aqui a ordem importa o MAIN_APPLICATION_LOWERCASE tem de vir 1º
				FileUtilities.replaceStringInFile(currentFile, "MAIN_APPLICATION_LOWERCASE", getSystem().model().name().toLowerCase());
				FileUtilities.replaceStringInFile(currentFile, "MAIN_APPLICATION", getSystem().model().name() + "Memory");
				
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-IP", IP);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-PORT", PORT);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-USER", USER);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-PASS", PASS);
			}
		}
	}
	
	private void generateDb4oProGuardRequirements(){
		if(!FileUtilities.contains("########################" + System.getProperty("line.separator") +
			"-keep class com.db4odoc.android.**" + System.getProperty("line.separator") +
			"-keepnames class com.db4odoc.android.**" + System.getProperty("line.separator") +
			"-keepclassmembers class com.db4odoc.android.** {" + System.getProperty("line.separator") +
			"\t!static !transient <fields>;" + System.getProperty("line.separator") +
			"\t!private <fields>;" + System.getProperty("line.separator") +
			"\t!private <methods>;" + System.getProperty("line.separator") +
			"}")){
			
			FileUtilities.println("########################");
			FileUtilities.println("-keep class com.db4odoc.android.**");
			FileUtilities.println("-keepnames class com.db4odoc.android.**");
			FileUtilities.println("-keepclassmembers class com.db4odoc.android.** {");
			FileUtilities.incIndent();
				FileUtilities.println("!static !transient <fields>;");
				FileUtilities.println("!private <fields>;");
				FileUtilities.println("!private <methods>;");
			FileUtilities.decIndent();
			FileUtilities.println("}");
			FileUtilities.println();
		}
		
		if(!FileUtilities.contains("########################" + System.getProperty("line.separator") +
			"## Monitoring requires JMX, which is not available on Android" + System.getProperty("line.separator") +
			"-dontwarn com.db4o.monitoring.*" + System.getProperty("line.separator") +
			"-dontwarn com.db4o.cs.monitoring.*" + System.getProperty("line.separator") +
			"-dontwarn com.db4o.internal.monitoring.*")){
			FileUtilities.println("########################");
			FileUtilities.println("## Monitoring requires JMX, which is not available on Android");
			FileUtilities.println("-dontwarn com.db4o.monitoring.*");
			FileUtilities.println("-dontwarn com.db4o.cs.monitoring.*");
			FileUtilities.println("-dontwarn com.db4o.internal.monitoring.*");
			FileUtilities.println();
		}

		if(!FileUtilities.contains("## Ant is usually not used in a running app" + System.getProperty("line.separator") +
			"-dontwarn com.db4o.instrumentation.ant.*" + System.getProperty("line.separator") +
			"-dontwarn com.db4o.ta.instrumentation.ant.*")){
			FileUtilities.println("## Ant is usually not used in a running app");
			FileUtilities.println("-dontwarn com.db4o.instrumentation.ant.*");
			FileUtilities.println("-dontwarn com.db4o.ta.instrumentation.ant.*");
			FileUtilities.println();
		}

		if(!FileUtilities.contains("## Keep internal classes." + System.getProperty("line.separator") +
			"-keep class com.db4o.** { *; }")){
			FileUtilities.println("## Keep internal classes.");
			FileUtilities.println("-keep class com.db4o.** { *; }");
		}
	}
	
	private void generateServerTemplates(String sourceDirectory, String basePackageName, String baseDirectory,
			String USER, String PASS, String PORT, String IP){	
		
		//file e localização
		Map<String,String> files = new HashMap<String,String>();
		files.put("ServerInfo", basePackageName);
		
		for(String file : files.keySet()){
//			ca parvoice o switch nao aceita String que na sejam final :/
			String currentFile = "";
			if(files.get(file).equals(basePackageName)){
				FileUtilities.copyFile(sourceDirectory + "/" + file + ".txt", baseDirectory + "/" + file + ".java");
				FileUtilities.replaceStringInFile(baseDirectory + "/" + file + ".java", "org.quasar.usemodel2Android.defaultdata.java", basePackageName);
				currentFile = baseDirectory + "/" + file + ".java";
			}

			if(!currentFile.equals("")){				
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-IP", IP);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-PORT", PORT);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-USER", USER);
				FileUtilities.replaceStringInFile(currentFile, "GENERATION-PASS", PASS);
			}
		}
	}
	
	public void serverGeneration(final String author, final String projectName, final String javaWorkspace, final String basePackageName,
			final String libraryDirectory, final String db4oCoreJar, final String db4oCsJar, final String db4oOptionalJar,
			final String user, final String pass, final String port, final String ip) {
			
		String PROJECTNAME;
		if(projectName.equals(""))
			PROJECTNAME = getSystem().model().name() + "Server";
		else
			PROJECTNAME = projectName;
		
		String BASEPACKAGENAME = basePackageName + PROJECTNAME;
		
		String targetDirectory = javaWorkspace + "/" + PROJECTNAME + "/src/" + BASEPACKAGENAME.replace('.', '/');
		String libraryPath = javaWorkspace + "/" + PROJECTNAME + "/libs";
		String USER = user;
		String PASS = pass;
		String PORT = port;
		String IP;
		if(ip.equals("0.0.0.0"))
			IP = "localhost";
		else
			IP = ip;
		
		FileUtilities.createDirectory(targetDirectory);
		generateServerTemplates(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/defaultdata/java", BASEPACKAGENAME, targetDirectory,
				USER, PASS, PORT, IP);
		
		FileUtilities.copyFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/defaultdata/java/StartServer.txt", targetDirectory + "/" + "StartServer.java");
		FileUtilities.replaceStringInFile(targetDirectory + "/" + "StartServer.java", "org.quasar.usemodel2Android.defaultdata.java", BASEPACKAGENAME);
		FileUtilities.copyFile(javaWorkspace + "/J-USE/src/org/quasar/usemodel2Android/defaultdata/java/StopServer.txt", targetDirectory + "/" + "StopServer.java");
		FileUtilities.replaceStringInFile(targetDirectory + "/" + "StopServer.java", "org.quasar.usemodel2Android.defaultdata.java", BASEPACKAGENAME);
		
		FileUtilities.createDirectory(libraryPath);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oCoreJar, libraryPath + "/" + db4oCoreJar);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oCsJar, libraryPath + "/" + db4oCsJar);
		FileUtilities.copyFile(libraryDirectory + "/" + db4oOptionalJar, libraryPath + "/" + db4oOptionalJar);
		
	}

}