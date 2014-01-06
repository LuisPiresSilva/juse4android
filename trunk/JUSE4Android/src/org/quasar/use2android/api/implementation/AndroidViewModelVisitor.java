package org.quasar.use2android.api.implementation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.AssociationKind;
import org.quasar.juse.api.implementation.AttributeInfo;
import org.quasar.juse.api.implementation.FileUtilities;
import org.quasar.juse.api.implementation.JavaTypes;
import org.quasar.juse.api.implementation.ModelUtilities;
import org.quasar.use2android.api.AndroidTypes;
import org.quasar.use2android.api.JavaInputValidation;

import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MMultiplicity;
import org.tzi.use.uml.ocl.type.Type;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class AndroidViewModelVisitor extends ViewModelVisitor{

	private MModel			model;
	private String			author;
	private String			basePackageName;
	private String			businessLayerName;
	private String			persistenceLayerName;
	private String			presentationLayerName;
	private String			utilsLayerName;
	private ModelUtilities	util;
	private String			MainMemoryClass;

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
	public AndroidViewModelVisitor(MModel model, String author, String basePackageName, String businessLayerName,
					String persistenceLayerName, String presentationLayerName, String utilsLayerName)
	{
		this.model = model;
		this.author = author;
		this.basePackageName = basePackageName;
		this.businessLayerName = businessLayerName;
		this.persistenceLayerName = persistenceLayerName;
		this.presentationLayerName = presentationLayerName;
		this.utilsLayerName = utilsLayerName;
		this.MainMemoryClass = capitalize(model.name()) + "Memory";
		
	}
	
//	******************** --- Usefull methods - Start --- *****************************
	
	/***********************************************************
	* @param theClass whose root we want
	* @return the root parent of the class passed as parameter
	***********************************************************/
	private MClass baseAncestor(MClass theClass)
	{
		return theClass.parents().isEmpty() ? theClass : baseAncestor(theClass.parents().iterator().next());
	}
	
	/***********************************************************
	* @param theClass whose root we want
	* @param the Attribute whose root we want
	* @return the root parent of the class and attribute passed as parameters
	***********************************************************/
	private MClass attributeBaseAncestor(MClass theClass, MAttribute att)
	{
		return (!theClass.parents().isEmpty() && !theClass.attributes().contains(att)) ? attributeBaseAncestor(theClass.parents().iterator().next(), att) : theClass;
	}
	
	/***********************************************************
	* @param theClass whose root we want
	* @param the Association we want the root to have
	* @return the root parent that contains the association passed as parameters
	***********************************************************/
	private MClass associationTargetBaseAncestor(MClass theClass, AssociationInfo ass)
	{
//		System.out.println("" + ass.getSourceAE().cls().name());
		return (!theClass.parents().isEmpty() && ass.getSourceAE().cls() != theClass) ? associationTargetBaseAncestor(theClass.parents().iterator().next(), ass) : theClass;
	}
	
	/***********************************************************
	* @param an association
	* @return true if association is a MANY2MANY or ONE2MANY and target is an colection
	***********************************************************/
	private boolean is2Many(AssociationInfo association, MAssociationEnd end){
		if(association.getKind() == AssociationKind.MANY2MANY || (association.getKind() == AssociationKind.ONE2MANY && end.isCollection()))
			return true;
		else
			return false;
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
	* @param supers or array of classes to check
	* @return list of all subclasses
	***********************************************************/
	private List<MClass> getAllSubClasses(List<MClass> supers){
		List<MClass> subClasses = new ArrayList<MClass>();
		for(MClass x : supers)
			for(MClass y : model.classes())
				if(!subClasses.contains(y) && x != y && y.isSubClassOf(x))
					subClasses.add(y);
		
		return subClasses;
	}
	
	@Override
	public void printFileHeader(String typeName, String layerName) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		// get current date time with Date()
		Date date = new Date();

		println("/**********************************************************************");
		println("* Filename: " + typeName + ".java");
		println("* Created: " + dateFormat.format(date));
		println("* @author " + author);
		println("**********************************************************************/");
		if(layerName.equals("base"))
			println("package " + basePackageName + ";");
		else
			println("package " + basePackageName + "." + layerName + "." + typeName + ";");
		println();
		
	}
	
//	******************** --- Usefull methods - End --- *****************************
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	******************** --- DefaultClasses - Start --- *****************************

	@Override
	public void printMasterActivity(){
		printFileHeader("MasterActivity", "base");
		
		println("import " + basePackageName + ".R;");
		println("import " + basePackageName + "." + model.name() + "Launcher;");
		println("import android.app.Activity;");
		println("import android.content.Intent;");
		println("import android.graphics.drawable.AnimationDrawable;");
		println("import android.os.Bundle;");
		println("import android.view.Menu;");
		println("import android.view.MenuInflater;");
		println("import android.view.MenuItem;");
		println("import android.widget.ImageView;");
		println();
		println("public class MasterActivity extends Activity");
		println("{");
		FileUtilities.incIndent();
			println("public static final String ACTION_MODE_READ = \"READ\";");
			println("public static final String ACTION_MODE_WRITE = \"WRITE\";");
			println("public static final int ToMANY = -1;");
			println("public static final int ToONE = 1;");
			println("public boolean ACTION_MODE_WRITE_STARTER = false;");
			println("public boolean ONCREATION = false;");
			println("public static final int CREATION_CODE = 0;");
			println("protected boolean showingDetail = false;");
			println("protected final String SHOWINGDetail = \"ShowingDetail\";");
			println("private ImageView menuOnCreationImage;");
			println("private AnimationDrawable menuOnCreationAnimation;");
			println("protected " + MainMemoryClass + " " + MainMemoryClass.toLowerCase() + ";");
			println();
			println("@Override");
			println("public void onCreate(Bundle savedInstanceState)");
			println("{");
			FileUtilities.incIndent();
				println("super.onCreate(savedInstanceState);");
				println(MainMemoryClass.toLowerCase() + " = (" + MainMemoryClass + ")getApplication();");
		    	println();
		    	println("Intent intent = getIntent();");
		    	println("if (intent != null)");
		    	FileUtilities.incIndent();
		    		println("if (intent.getAction().equals(ACTION_MODE_WRITE))");
					FileUtilities.incIndent();
						println("ONCREATION = true;");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
		    FileUtilities.decIndent();
			println("}");
		    println();
		    println("@Override");
		    println("public void onResume()");
				println("{");
				FileUtilities.incIndent();
					println("super.onResume();");
					println(MainMemoryClass + ".setActiveActivity(this);");
		    	FileUtilities.decIndent();
				println("}");
		    println();
		    println("@Override");
		    println("public void onPause()");
				println("{");
				FileUtilities.incIndent();
					println("super.onPause();");
				FileUtilities.decIndent();
				println("}");
			println();
//			boolean destroyed = false;
			println("@Override");
			println("public void onBackPressed()");
				println("{");
				FileUtilities.incIndent();
					println("super.onBackPressed();");
				FileUtilities.decIndent();
				println("}");
			println();
			println("@Override");
			println("public void onDestroy()");
				println("{");
				FileUtilities.incIndent();
				println("super.onDestroy();");
				FileUtilities.decIndent();
				println("}");
		    println();
		    println("public boolean isOnCreation()");
				println("{");
				FileUtilities.incIndent();
					println("return ONCREATION;");
				FileUtilities.decIndent();
				println("}");
			println();
			println("public void setOnCreation(boolean onCreation)");
				println("{");
				FileUtilities.incIndent();
					println("this.ONCREATION = onCreation;");
					println("invalidateOptionsMenu();");
				FileUtilities.decIndent();
				println("}");
			println();
			println("@Override");
			println("public boolean onPrepareOptionsMenu(Menu menu)");
				println("{");
				FileUtilities.incIndent();
					println("MenuInflater menuInflater = getMenuInflater();");
					println("if(ONCREATION)");
					FileUtilities.incIndent();
						println("menuInflater.inflate(R.menu.menu_write, menu);");
					FileUtilities.decIndent();
					println("else");
					FileUtilities.incIndent();
						println("menuInflater.inflate(R.menu.menu_read, menu);");
					FileUtilities.decIndent();
					println("return true;");
				FileUtilities.decIndent();
				println("}");
			println();
			println("@Override");
			println("public boolean onCreateOptionsMenu(Menu menu)");
				println("{");
				FileUtilities.incIndent();
					println("return true;");
				FileUtilities.decIndent();
				println("}");
			println();
			println("@Override");
			println("public boolean onOptionsItemSelected(MenuItem item)");
				println("{");
				FileUtilities.incIndent();
				println("switch (item.getItemId())");
				println("{");
				FileUtilities.incIndent();
					println("case R.id.menu_home:");
					FileUtilities.incIndent();
						println("Intent upIntent = new Intent(this, " + model.name() + "Launcher.class);");
						println("upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);");
						println("startActivity(upIntent);");
						println("finish();");
					FileUtilities.decIndent();
					println("break;");
				FileUtilities.decIndent();
				println("}");
				println("return super.onOptionsItemSelected(item);");
			FileUtilities.decIndent();
			println("}");
			println();
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printApplicationClass(){
		printFileHeader(model.name(), "base");
		println("import " + basePackageName + "." + persistenceLayerName + ".Database;");
		println();
		println("import android.app.Activity;");
		println("import android.app.Application;");
		println();
		println("public class " + MainMemoryClass + " extends Application");
		println("{");
		println();
		FileUtilities.incIndent();
			println("private static Activity ActiveActivity;");
			println("public Database db = new Database();");
			println();
			println("@Override");
			println("public void onCreate()");
			println("{");
			FileUtilities.incIndent();
				println("super.onCreate();");
				println("db.setContext(getApplicationContext());");
				println("db.OpenDB();");
			FileUtilities.decIndent();
			println("}");
			println();
			println("public void setAppContextToDB()");
			println("{");
			FileUtilities.incIndent();
				println("db.setContext(getApplicationContext());");
			FileUtilities.decIndent();
			println("}");
			println();
			println("public synchronized static Activity getActiveActivity()");
			println("{");
			FileUtilities.incIndent();
				println("return ActiveActivity;");
			FileUtilities.decIndent();
			println("}");
			println();
			println("public synchronized static void setActiveActivity(Activity activity)");
			println("{");
			FileUtilities.incIndent();
				println("ActiveActivity = activity;");
			FileUtilities.decIndent();
			println("}");
			println();
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	public void printLauncher(MModel model){
		printFileHeader(model.name(), "base");
		println("import java.util.ArrayList;");
		println();
		println("import " + basePackageName + "." + persistenceLayerName + ".Database;");
		println("import " + basePackageName + "." + utilsLayerName + ".ServerActions;");
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
		println();
		for(MClass c : model.classes())
			if(c.isAnnotated() && c.getAnnotation("StartingPoint") != null)
				println("import " + basePackageName + "." + presentationLayerName + "." + c.name() + ". "+ c.name() +"Activity;");
		println();
		
		Set<String> androidClassTypes = new HashSet<String>();
		androidClassTypes.add("Activity");
		androidClassTypes.add("Intent");
		androidClassTypes.add("Bundle");
		androidClassTypes.add("Menu");
		androidClassTypes.add("MenuItem");
		androidClassTypes.add("View");
		androidClassTypes.add("AdapterView");
		androidClassTypes.add("AdapterView.OnItemClickListener");
		androidClassTypes.add("GridView");
		Set<String> imports = AndroidTypes.androidImportDeclarations(androidClassTypes);
		
		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
		
		println("public class " + model.name() + "Launcher extends Activity");
		println("{");
		println();
		FileUtilities.incIndent();
			println("private LauncherGridViewAdapter mAdapter;");
		    println("private ArrayList<String> listObjects;");
		    println("private ArrayList<Integer> listImages;");
		    println("private GridView gridView;");
		    println("private " + MainMemoryClass + " " + MainMemoryClass.toLowerCase() + ";");
		    println();
		    println("public void inicializer()");
		    println("{");
			FileUtilities.incIndent();
		    	println("if(" + MainMemoryClass.toLowerCase() + ".db.getContext() == null)");
			FileUtilities.incIndent();
		    		println("" + MainMemoryClass.toLowerCase() + ".setAppContextToDB();");
			FileUtilities.decIndent();
		    	println("" + MainMemoryClass.toLowerCase() + ".db.OpenDB();");
			FileUtilities.decIndent();
		    println("}");
		    println();
		    
		    println("@Override");
		    println("public void onCreate(Bundle savedInstanceState)");
		    println("{");
		    FileUtilities.incIndent();
		        println("super.onCreate(savedInstanceState);");
		        println("setContentView(R.layout." + model.name().toLowerCase() + "_launcher_activity);");
		    	println(MainMemoryClass.toLowerCase() + " = (" + MainMemoryClass + ")getApplication();");
		    	println();     	
		    	println("inicializer();");
		    	println("if(savedInstanceState == null)");
		    	println("{");
		    	FileUtilities.incIndent();
		    		println("inicializer();");
		    		println("if(getIntent().getExtras() != null)");
		    	FileUtilities.incIndent();
		    			println("UtilNavigate.showWarning(this, getIntent().getExtras().getString(\"TITLE\"), getIntent().getExtras().getString(\"MESSAGE\"));");
		    	FileUtilities.decIndent();
		    	FileUtilities.decIndent();
		        println("}");
		        println();
		        
		        println("prepareList();"); 
		        
		        println();
		        // prepared arraylist and passed it to the Adapter class
		        println("mAdapter = new LauncherGridViewAdapter(this,listObjects, listImages);");
		 
		        // Set custom adapter to gridview
		        println("gridView = (GridView) findViewById(R.id.gridView1);");
		        println("gridView.setAdapter(mAdapter);");
		        println();
		        // Implement On Item click listener
		        println("gridView.setOnItemClickListener(new OnItemClickListener()");
		        println("{");
		    	FileUtilities.incIndent();
		            println("@Override");
		            println("public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)");
		            println("{");
		    		FileUtilities.incIndent();
		    		for(MClass c : model.classes()){
		    			if(c.isAnnotated() && c.getAnnotation("StartingPoint") != null){
		    				if(c.getAnnotation("StartingPoint").getValues().containsKey("NameToDisplay")){
			    				for(Entry<String, String> x : c.getAnnotation("StartingPoint").getValues().entrySet())
			    					if(x.getKey().equals("NameToDisplay"))
			    						println("if(mAdapter.getItem(position).equals(\"" + x.getValue() + "\"))");
		    				}else
		    					println("if(mAdapter.getItem(position).equals(\"" + c.name() + "\"))");
				            println("{");
				    		FileUtilities.incIndent();
				    			println("Intent intent = new Intent(" + model.name() + "Launcher.this, " + c.name() + "Activity.class);");
				    			println("intent.setAction(\"READ\");");
				    			println("startActivity(intent);");
				    		FileUtilities.decIndent();
					        println("}");
		    			}
		    		}
		    		FileUtilities.decIndent();
		            println("}");
		    	FileUtilities.decIndent();
		        println("});");
		    FileUtilities.decIndent();
		    println("}");
		    println();

		    println("@Override");
			println("public void onResume()");
		    println("{");
		    FileUtilities.incIndent();
		    	println("super.onResume();");
		    	println(MainMemoryClass + ".setActiveActivity(this);");
		    FileUtilities.decIndent();
		    println("}");
		    println();
			println("@Override");
			println("public void onPause()");
			println("{");
		    FileUtilities.incIndent();
				println("super.onPause();");
		    FileUtilities.decIndent();
		    println("}");
		    
		    println();
			println("@Override");
			println("public void onDestroy()");
			println("{");
		    FileUtilities.incIndent();
				println("super.onDestroy();");
				println("Database.close();");
		    FileUtilities.decIndent();
		    println("}");
		    println();
		    
		    println("@Override");
		    println("public boolean onCreateOptionsMenu(Menu menu)");
		    println("{");
		    FileUtilities.incIndent();
		        println("getMenuInflater().inflate(R.menu.menu_launcher, menu);");
		        println("return true;");
		    FileUtilities.decIndent();
		    println("}");
		    println();
		    
		    println("Activity activity = this;");
		    println();
		    
		    println("@Override");
			println("public boolean onOptionsItemSelected(MenuItem item)");
		    println("{");
		    FileUtilities.incIndent();
		    	println("switch (item.getItemId())");
		    	println("{");
		    	FileUtilities.incIndent();
		    		println("case R.id.upload:");
					FileUtilities.incIndent();
						println("ServerActions.sendChanges();");
						println("break;");
					FileUtilities.decIndent();
					println("case R.id.download:");
					FileUtilities.incIndent();
						println("ServerActions.getChanges();");
						println("break;");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
				println("}");
				println("return super.onOptionsItemSelected(item);");
		    FileUtilities.decIndent();
			println("}");
			println();
		    println("public void prepareList()");
		    println("{");
		    	FileUtilities.incIndent();
		    	println("listObjects = new ArrayList<String>();");
				println("listImages = new ArrayList<Integer>();");
				for(MClass c : model.classes()){
	    			if(c.isAnnotated() && c.getAnnotation("StartingPoint") != null){
	    				if(c.getAnnotation("StartingPoint").getValues().containsKey("NameToDisplay")){
		    				for(Entry<String, String> x : c.getAnnotation("StartingPoint").getValues().entrySet())
		    					if(x.getKey().equals("NameToDisplay"))
		    						println("listObjects.add(\"" + x.getValue() + "\");");
	    				}else
	    					println("listObjects.add(\"" + c.name() + "\");");
	    				if(c.getAnnotation("StartingPoint").getValues().containsKey("ImageToDisplay")){
		    				for(Entry<String, String> x : c.getAnnotation("StartingPoint").getValues().entrySet())
		    					if(x.getKey().equals("ImageToDisplay"))
		    						if(x.getValue().equals(""))
		    							println("listImages.add(0);");
		    						else
		    							println("listImages.add(R.drawable." + x.getValue() + ");");
	    				}else
	    					println("listImages.add(R.drawable.ic_launcher);");
	    			}
	    		}
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");

	}
//	******************** --- DefaultClasses - End --- *****************************
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	******************** --- Activity - Start --- *****************************

	@Override
	public void printActivity_ClassHeader(MClass theClass, String layerName){
		printFileHeader(theClass.name(), layerName);
		// visitAnnotations(e);

		printActivity_Imports(theClass);

		print("public ");

		print("class " + theClass.name() + "Activity");
		
		println(" extends MasterActivity implements");
		FileUtilities.incIndent();
		FileUtilities.incIndent();
		FileUtilities.incIndent();
		println("ListFragmentController.Callbacks,");
		println(theClass.name() + "DetailFragment.Callbacks,");
		println("PropertyChangeListener");
		FileUtilities.decIndent();
		FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("{");
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printActivity_Imports(MClass theClass)
	{
		println("import " + basePackageName + ".R;");
		println("import " + basePackageName + ".MasterActivity;");

		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
//		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".ListFragmentController;");
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".ListAdapter;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		println("import " + basePackageName + "." + utilsLayerName + ".NavigationBarFragment;");
		println("import " + basePackageName + "." + utilsLayerName + ".DetailFragment;");
//		println("import " + basePackageName + "." + utilsLayerName + ".Utils;");
					
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		
		List<MClass> alreadyAdded = new ArrayList<MClass>();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			if(!alreadyAdded.contains(association.getTargetAE().cls())){
				println("import " + basePackageName + "." + businessLayerName + "." + association.getTargetAE().cls().name() + ";");
				alreadyAdded.add(association.getTargetAE().cls());
			}
//			if(!alreadyAdded.contains(association.getSourceAE().cls()))
//				if(isSuperClass(association.getSourceAE().cls())){
//					println("import " + basePackageName + "." + businessLayerName + "." + association.getSourceAE().cls().name() + ";");
//					println("import " + basePackageName + "." + presentationLayerName + "." + association.getSourceAE().cls().name() + "." + association.getSourceAE().cls().name() + "ListViewHolder;");
//					alreadyAdded.add(association.getSourceAE().cls());
//				}
		}
		
//		if(isSuperClass(theClass)){
//			for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
//				if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super
////					println("import " + basePackageName + "." + businessLayerName + "." + x.name() + ";");
//					alreadyAdded.add(theClass);
//				}
//			}
//		}
		if(isSubClass(theClass)){
			MClass x = theClass;
			println("import " + basePackageName + "." + presentationLayerName + "." + x.parents().iterator().next().name() + "." + x.parents().iterator().next().name() + "ListViewHolder;");
			do{
				if(!alreadyAdded.contains(x.parents().iterator().next())){
					println("import " + basePackageName + "." + businessLayerName + "." + x.parents().iterator().next().name() + ";");
					alreadyAdded.add(theClass.parents().iterator().next());
				}
				x = x.parents().iterator().next();
			}while(!x.parents().isEmpty());
		}
		println();

		Set<String> androidClassTypes = new HashSet<String>();
		Set<Type> javaClassTypes = new HashSet<Type>();
		
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass))
			javaClassTypes.add(association.getSourceAE().getType());
		
		for (MAttribute att : theClass.attributes()){
			String type = AndroidTypes.androidPrimitiveTypeToReadWidget(att.type());
			androidClassTypes.add(type);
			if(!type.equals(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type())))
				androidClassTypes.add(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type()));
			
//			javaClassTypes.add(att.type());
		}

		androidClassTypes.add("Activity");
		androidClassTypes.add("Fragment");
		androidClassTypes.add("FragmentTransaction");
		androidClassTypes.add("Intent");
		androidClassTypes.add("Bundle");
		androidClassTypes.add("MenuItem");
		androidClassTypes.add("Menu");
		androidClassTypes.add("ListView");
//		androidClassTypes.add("Toast");
		
		Set<String> AndroidImports = AndroidTypes.androidImportDeclarations(androidClassTypes);
		Set<String> JavaImports = AndroidTypes.javaInAndroidImportDeclarations(javaClassTypes);
		
		JavaImports.add("import java.util.Set;");
		JavaImports.add("import java.util.Collection;");
		
		for (String importDeclaration : JavaImports)
			println(importDeclaration);
		println();
		for (String importDeclaration : AndroidImports)
			println(importDeclaration);
		
		println("import android.util.Log;");
		println();
	}
	
	@Override
	public void printActivity_Attributes(MClass theClass){
		println("private boolean restarted = false;");
		println("private boolean mTwoPane = false;");
		println("private boolean showingDetail = false;");
		println("private int AssociationEnd;");
		println();
		println("private Fragment navigation_bar;");
		println("private ListFragmentController list_fragment;");
		println("private Fragment detail_fragment;");
		println();
		println("//Objects & Associations");
		println("private " + theClass.name() + " clicked" + theClass.name() + ";");
		
		List<MClass> directAssociations = new ArrayList<MClass>();
		for(AssociationInfo x : AssociationInfo.getAssociationsInfo(theClass))
			directAssociations.add(x.getSourceAE().cls());

		List<MClass> alreadyAdded = new ArrayList<MClass>();
		Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			if(!alreadyAdded.contains(association.getTargetAE().cls())){
				println("private " + association.getTargetAE().cls().name() + " " + association.getTargetAE().cls().name().toLowerCase() + ";");
				println("private final String " + association.getTargetAE().cls().name().toUpperCase() + "Object = \"" + association.getTargetAE().cls().name().toUpperCase() + "Object\";");
				alreadyAdded.add(association.getTargetAE().cls());
				RepeteadNeighbors.put(association.getTargetAE().cls(), 1);
			}else
				RepeteadNeighbors.put(association.getTargetAE().cls(), RepeteadNeighbors.get(association.getTargetAE().cls()) + 1);
			println("private final String " + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association = \"" + association.getTargetAE().name().toUpperCase() +  "_" + association.getSourceAE().name().toUpperCase() + "Association\";");

		}
		
		
		if(isSuperClass(theClass)){
			for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
				if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super
					println("private " + theClass.name() + " " + theClass.name().toLowerCase() + ";");
					println("private final String " + theClass.name().toUpperCase() + "Object = \"" + theClass.name().toUpperCase() + "Object\";");
					println("private final String " + theClass.name().toUpperCase() + "Association = \"" + theClass.name().toUpperCase() + "Association\";");
					alreadyAdded.add(theClass);
				}
			}
		}
		if(isSubClass(theClass)){
			if(!alreadyAdded.contains(theClass.parents().iterator().next())){
				println("private " + theClass.parents().iterator().next().name() + " " + theClass.parents().iterator().next().name().toLowerCase() + ";");
				println("private final String " + theClass.parents().iterator().next().name().toUpperCase() + "Object = \"" + theClass.parents().iterator().next().name().toUpperCase() + "Object\";");
				println("private final String " + theClass.parents().iterator().next().name().toUpperCase() + "Association = \"" + theClass.parents().iterator().next().name().toUpperCase() + "Association\";");
				alreadyAdded.add(theClass.parents().iterator().next());
			}
			
			println();
//			super and sub have different associations for the same class(neighbor)
//			sub must have a way to distinguish both associations
			for(MClass repeteadNeighbor : RepeteadNeighbors.keySet())
				if(RepeteadNeighbors.get(repeteadNeighbor) > 1){
					println("private String " + repeteadNeighbor.name() + "End;");
					println("private final String " + repeteadNeighbor.name().toUpperCase() + "END = \"" + repeteadNeighbor.name().toUpperCase() + "\";");
				}
		}
		println();
	}

	@Override
	public void printActivity_UsefullMethods(MClass theClass){
		printActivity_Extras();
		printActivity_StartActivity_ToONE(theClass);
		printActivity_StartActivity_ToMANY(theClass);
		printActivity_SetDetailFragment(theClass);
		printActivity_orientationSettings(theClass);
	}
	
	private void printActivity_Extras(){
		println("public Bundle extras()");
		println("{");
		FileUtilities.incIndent();
			println("return getIntent().getExtras();");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	private void printActivity_StartActivity_ToMANY(MClass theClass){
		println("public <T> void startActivity_ToMANY(Collection<T> subSet, Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("if(!restarted)");
			println("{");
			FileUtilities.incIndent();
			boolean isSub = false;
				if(isSubClass(theClass)){
					isSub = true;
					println("Class<T> clazz;");
					println("if(subSet == null)");
					FileUtilities.incIndent();
						println("clazz = (Class<T>) " + theClass.name() + ".class;");
					FileUtilities.decIndent();
					println("else");
					FileUtilities.incIndent();
						println("clazz = (Class<T>) subSet.getClass();");
					FileUtilities.decIndent();
				}
				println("if(subSet != null)");
				println("{");
				FileUtilities.incIndent();
					if(isSub){
						println("if(clazz.getClass() == " + theClass.parents().iterator().next().name() + ".class.getClass())");
						FileUtilities.incIndent();
							println("list_fragment.setListAdapter(new ListAdapter(this, new " + theClass.parents().iterator().next().name() + "ListViewHolder(), subSet));");
						FileUtilities.decIndent();
						println("if(clazz.getClass() == " + theClass.name() + ".class.getClass())");
						FileUtilities.incIndent();
							println("list_fragment.setListAdapter(new ListAdapter(this, new " + theClass.name() + "ListViewHolder(), subSet));");
						FileUtilities.decIndent();
					}else
						println("list_fragment.setListAdapter(new ListAdapter(this, new " + theClass.name() + "ListViewHolder(), subSet));");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("list_fragment.setListAdapter(new ListAdapter(this, new " + theClass.name() + "ListViewHolder(), " + theClass.name() + ".allInstances()));");
				FileUtilities.decIndent();
				println("}");
//				println("if(ONCREATION)");
//				FileUtilities.incIndent();
//					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_NEW, null);");
//				FileUtilities.decIndent();
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	private void printActivity_StartActivity_ToONE(MClass theClass){
		println("public void startActivity_ToONE(" + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
		FileUtilities.incIndent();
			println("if(!restarted)");
			println("{");
			FileUtilities.incIndent();
				if(!theClass.isAbstract()){
					println("if(ONCREATION)");
					FileUtilities.incIndent();
						println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_NEW, null);");
					FileUtilities.decIndent();
					println("else");
					FileUtilities.incIndent();
				}
					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL, " + theClass.name().toLowerCase() + ");");
				if(!theClass.isAbstract())
					FileUtilities.decIndent();
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	private void printActivity_SetDetailFragment(MClass theClass){
		println("public void setDetailFragment(String View, " + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
		FileUtilities.incIndent();
			println("detail_fragment = new " + theClass.name() + "DetailFragment();");
			println("UtilNavigate.replaceFragment(this, detail_fragment, R.id." + theClass.name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(View," + theClass.name().toLowerCase() + "));");
			println();
		println("if(!mTwoPane && AssociationEnd != ToONE)");
		println("{");
		FileUtilities.incIndent();
			println("list_fragment.hide();");
			println("((DetailFragment) detail_fragment).show();");
		FileUtilities.decIndent();
		println("}");
		println("showingDetail = true;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	private void printActivity_orientationSettings(MClass theClass){
		println("public void orientationSettings(" + theClass.name() + " clicked" + theClass.name() + ")");
		println("{");
		FileUtilities.incIndent();
//			println("if(mTwoPane)");
//			println("{");
//			FileUtilities.incIndent();
				println("if((!showingDetail || detail_fragment == null) && clicked" + theClass.name() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL, clicked" + theClass.name() + ");");
				FileUtilities.decIndent();
				println("}");
//				println("showingDetail = true;");
//			FileUtilities.decIndent();
//			println("}");
			println("if(!mTwoPane && AssociationEnd != ToONE)");
			println("{");
				FileUtilities.incIndent();
				println("if(detail_fragment != null && showingDetail)");
				println("{");
				FileUtilities.incIndent();
					println("list_fragment.hide();");
					println("((DetailFragment) detail_fragment).show();");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onSaveInstanceState(MClass theClass){
		println("@Override");
		println("public void onSaveInstanceState(Bundle outState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onSaveInstanceState(outState);");
			println("if(AssociationEnd != ToONE)");
			FileUtilities.incIndent();
				println("outState.putBoolean(SHOWINGDetail, showingDetail);");
			FileUtilities.decIndent();
			println("outState.putBoolean(\"RESTARTED\", true);");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onCreate(MClass theClass){
		println("@Override");
		println("public void onCreate(Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onCreate(savedInstanceState);");
			println();
			println("if(extras() != null)");
			println("{");
			FileUtilities.incIndent();
			
				List<MClass> alreadyAdded = new ArrayList<MClass>();
				Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					if(!alreadyAdded.contains(association.getTargetAE().cls())){
						RepeteadNeighbors.put(association.getTargetAE().cls(), 1);
						alreadyAdded.add(association.getTargetAE().cls());
					}
					else
						RepeteadNeighbors.put(association.getTargetAE().cls(), RepeteadNeighbors.get(association.getTargetAE().cls()) + 1);
				}
				
				alreadyAdded.clear();
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					if(!alreadyAdded.contains(association.getTargetAE().cls())){
						println("if(extras().containsKey(" + association.getTargetAE().cls().name().toUpperCase() + "Object))");
						println("{");
						
						FileUtilities.incIndent();
							if(is2Many(association, association.getSourceAE())){//source(theClass) -> 2MANY
								println(association.getTargetAE().cls().name().toLowerCase() + " = " + association.getTargetAE().cls().name() + ".get" + association.getTargetAE().cls().name() + "((Integer)extras().getInt(" + association.getTargetAE().cls().name().toUpperCase() + "Object));");
								println("if(ONCREATION)");
								FileUtilities.incIndent();
									println("AssociationEnd = ToMANY;");
								FileUtilities.decIndent();
								println("else ");
								FileUtilities.incIndent();
									println("AssociationEnd = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
								FileUtilities.decIndent();
							}else{//source(theClass) -> 2ONE
								println(association.getTargetAE().cls().name().toLowerCase() + " = " + association.getTargetAE().cls().name() + ".get" + association.getTargetAE().cls().name() + "((Integer)extras().getInt(" + association.getTargetAE().cls().name().toUpperCase() + "Object));");
								println("if(ONCREATION)");
								FileUtilities.incIndent();
									println("AssociationEnd = ToMANY;");
								FileUtilities.decIndent();
								println("else ");
								FileUtilities.incIndent();
									println("AssociationEnd = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
								FileUtilities.decIndent();
							}
							if(isSubClass(theClass))
								if(RepeteadNeighbors.get(association.getTargetAE().cls()) > 1)
									println(association.getTargetAE().cls().name() + "End = extras().getString(" + association.getTargetAE().cls().name().toUpperCase() + "END);");
						FileUtilities.decIndent();
						println("}");
						alreadyAdded.add(association.getTargetAE().cls());
					}
				}
				if(isSuperClass(theClass)){//navegacao sub -> super (ToONE)
					for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
						if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super							
							println("if(extras().containsKey(" + theClass.name().toUpperCase() + "Object))");
							println("{");
						
							FileUtilities.incIndent();
								println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "((Integer)extras().getInt(" + theClass.name().toUpperCase() + "Object));");
								println("AssociationEnd = extras().getInt(" + theClass.name().toUpperCase() + "Association);");
							FileUtilities.decIndent();
							println("}");
							alreadyAdded.add(theClass);
						}
					}
				}
				if(isSubClass(theClass)){//navegacao super -> sub (ToMany)
					if(!alreadyAdded.contains(theClass.parents().iterator().next())){		
						println("if(extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Object))");
						println("{");
						FileUtilities.incIndent();
							println(theClass.parents().iterator().next().name().toLowerCase() + " = " + theClass.parents().iterator().next().name() + ".get" + theClass.parents().iterator().next().name() + "((Integer)extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Object));");
							println("AssociationEnd = extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Association);");
						FileUtilities.decIndent();
						println("}");
					}
				}
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("//camed from launcher therefore AssociationEnd = -1 (*) -> allInstances");
					println("AssociationEnd = ToMANY;");
				FileUtilities.decIndent();
			println("}");
//			println("setContentView(R.layout."+ theClass.name().toLowerCase() + "_layout_onepane);");
			
			println();
			println("if (getResources().getBoolean(R.bool.has_two_panes) && AssociationEnd != ToONE)");
			println("{");
			FileUtilities.incIndent();
				println("mTwoPane = true;");
				println("setContentView(R.layout." + theClass.name().toLowerCase() + "_layout_twopane);");
			FileUtilities.decIndent();
			println("}");
			println("else");
			FileUtilities.incIndent();
				println("setContentView(R.layout." + theClass.name().toLowerCase() + "_layout_onepane);");
			FileUtilities.decIndent();
			
			println();
			println("FragmentTransaction ft = getFragmentManager().beginTransaction();");
			println("if(savedInstanceState == null)");
			println("{");
			FileUtilities.incIndent();
			println("if (AssociationEnd != ToONE)");
			println("{");
			FileUtilities.incIndent();
				println("list_fragment = new ListFragmentController();");
				println("ft.add(R.id."+ theClass.name().toLowerCase() + "_list_container, list_fragment);");
			FileUtilities.decIndent();
			println("}");
				println("navigation_bar = new " + theClass.name() + "NavigationBarFragment();");
				println("ft.add(R.id."+ theClass.name().toLowerCase() + "_navigationbar_container, navigation_bar);");
				println("ft.commit();");
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("if (AssociationEnd != ToONE)");
				FileUtilities.incIndent();
					println("list_fragment = (ListFragmentController) getFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_list_container);");
				FileUtilities.decIndent();
				println("navigation_bar = (Fragment) getFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_navigationbar_container);");
				println("detail_fragment = (Fragment) getFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_detail_container);");
				println("showingDetail = savedInstanceState.getBoolean(SHOWINGDetail);");
				println("restarted = savedInstanceState.getBoolean(\"RESTARTED\");");
			FileUtilities.decIndent();
			println("}");
			println();
			println();
			println("if(ONCREATION)");
			FileUtilities.incIndent();
				println("Transactions.StartTransaction();");
			FileUtilities.decIndent();
			println();
			println("if(extras() != null)");
			println("{");
			FileUtilities.incIndent();
			
				alreadyAdded = new ArrayList<MClass>();
				RepeteadNeighbors = new HashMap<MClass, Integer>();
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					if(!alreadyAdded.contains(association.getTargetAE().cls())){
						RepeteadNeighbors.put(association.getTargetAE().cls(), 1);
						alreadyAdded.add(association.getTargetAE().cls());
					}
					else
						RepeteadNeighbors.put(association.getTargetAE().cls(), RepeteadNeighbors.get(association.getTargetAE().cls()) + 1);
				}
				
				alreadyAdded.clear();
//				boolean firts = true;
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					if(!alreadyAdded.contains(association.getTargetAE().cls())){
//						if(!firts)
//							print("else ");
						
						println("if(extras().containsKey(" + association.getTargetAE().cls().name().toUpperCase() + "Object))");
						println("{");
						
						FileUtilities.incIndent();
							if(is2Many(association, association.getSourceAE())){//source(theClass) -> 2MANY
								println(association.getTargetAE().cls().name().toLowerCase() + " = " + association.getTargetAE().cls().name() + ".get" + association.getTargetAE().cls().name() + "((Integer)extras().getInt(" + association.getTargetAE().cls().name().toUpperCase() + "Object));");
								println("if(ONCREATION)");
								println("{");
								FileUtilities.incIndent();
									println("startActivity_ToMANY(null, extras());");
									println("AssociationEnd = ToMANY;");
								FileUtilities.decIndent();
								println("}");
								println("else ");
								println("{");
								FileUtilities.incIndent();
									println("AssociationEnd = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
									//CUIDADO NA PROX LINHA
									println("startActivity_ToMANY((" + JavaTypes.javaInterfaceType(association.getSourceAE().getType()) + ") " + association.getTargetAE().cls().name().toLowerCase() + "." + association.getSourceAE().name() + "(), extras());");
								FileUtilities.decIndent();
								println("}");
								println(association.getTargetAE().cls().name() + ".getAccess().setChangeListener(this);");
							}else{//source(theClass) -> 2ONE
								println(association.getTargetAE().cls().name().toLowerCase() + " = " + association.getTargetAE().cls().name() + ".get" + association.getTargetAE().cls().name() + "((Integer)extras().getInt(" + association.getTargetAE().cls().name().toUpperCase() + "Object));");
								println("if(ONCREATION)");
								println("{");
								FileUtilities.incIndent();
									println("startActivity_ToMANY(null, extras());");
									println("AssociationEnd = ToMANY;");
								FileUtilities.decIndent();
								println("}");
								println("else ");
								println("{");
								FileUtilities.incIndent();
									println("AssociationEnd = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
									println("mTwoPane = false;");
								
									if(association.getSourceAE().cls() == theClass)
										println("clicked" + theClass.name() + " = " + association.getTargetAE().cls().name().toLowerCase() + "." + association.getSourceAE().name() + "();");
									else//para as hierarquias, fazemos cast da subClass
										println("clicked" + theClass.name() + " = (" + theClass.name() + ")" + association.getTargetAE().cls().name().toLowerCase() + "." + association.getSourceAE().name() + "();");
									println("startActivity_ToONE(clicked" + theClass.name() + ");");
								FileUtilities.decIndent();
								println("}");
//								println(association.getTargetAE().cls().name() + ".getAccess().setChangeListener(this);");
							}
							if(isSubClass(theClass))
								if(RepeteadNeighbors.get(association.getTargetAE().cls()) > 1)
									println(association.getTargetAE().cls().name() + "End = extras().getString(" + association.getTargetAE().cls().name().toUpperCase() + "END);");
						FileUtilities.decIndent();
						println("}");
						alreadyAdded.add(association.getTargetAE().cls());
//						firts = false;
					}
				}
				if(isSuperClass(theClass)){//navegacao sub -> super (ToONE)
					for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
						if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super
//							if(!firts)
//								print("else ");
							
							println("if(extras().containsKey(" + theClass.name().toUpperCase() + "Object))");
							println("{");
						
							FileUtilities.incIndent();
								println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "((Integer)extras().getInt(" + theClass.name().toUpperCase() + "Object));");
								println("AssociationEnd = extras().getInt(" + theClass.name().toUpperCase() + "Association);");
								println("mTwoPane = false;");
									
								if(x.parents().iterator().next() == theClass)
									println("clicked" + theClass.name() + " = " + theClass.name().toLowerCase() + ";");
								else//para as hierarquias, fazemos cast da subClass
									println("clicked" + theClass.name() + " = (" + theClass.name() + ")" + theClass.name().toLowerCase() + ";");
								println("startActivity_ToONE(clicked" + theClass.name() + ");");
								println(theClass.name() + ".getAccess().setChangeListener(this);");
							
							FileUtilities.decIndent();
							println("}");
							alreadyAdded.add(theClass);
						}
					}
				}
				if(isSubClass(theClass)){//navegacao super -> sub (ToMany)
					if(!alreadyAdded.contains(theClass.parents().iterator().next())){
//						if(!firts)
//							print("else ");
						
						println("if(extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Object))");
						println("{");
					
						FileUtilities.incIndent();
							println(theClass.parents().iterator().next().name().toLowerCase() + " = " + theClass.parents().iterator().next().name() + ".get" + theClass.parents().iterator().next().name() + "((Integer)extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Object));");
							println("AssociationEnd = extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Association);");
//							CUIDADO NA PROX LINHA
							println("startActivity_ToMANY(null" + ", extras());");
//							println(theClass.parents().iterator().next().name() + ".getAccess().setChangeListener(this);");
						FileUtilities.decIndent();
						println("}");
					}
				}
				
//				if(!firts){
//					println("else ");
//					println("{");
//					FileUtilities.incIndent();
//						println("startActivity_ToMANY(null, extras());");
//						println("AssociationEnd = ToMANY;");
//					FileUtilities.decIndent();
//					println("}");
//				}
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("//camed from launcher therefore AssociationEnd = -1 (*) -> allInstances");
					println("startActivity_ToMANY(null, extras());");
					println("AssociationEnd = ToMANY;");
				FileUtilities.decIndent();
			println("}");
			println();
			println("if(AssociationEnd != ToONE)");
			println("{");
			FileUtilities.incIndent();
				println(theClass.name() + ".getAccess().setChangeListener(list_fragment);");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onStart(MClass theClass){
		println("@Override");
		println("public void onStart()");
		println("{");
		FileUtilities.incIndent();
			println("super.onStart();");
			println("if(AssociationEnd != ToONE)");
			println("{");
			FileUtilities.incIndent();
				println("if (mTwoPane)");
				FileUtilities.incIndent();
					println("list_fragment.setActivatedLongClick(false);");
				FileUtilities.decIndent();
				println("else");
				FileUtilities.incIndent();
					println("list_fragment.setActivatedLongClick(true);");
				FileUtilities.decIndent();
				println("list_fragment.setActivateOnItemClick(true);");
//				println(theClass.name() + ".getAccess().setChangeListener(list_fragment);");
				println();
				println("if(list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
				println("{");
				FileUtilities.incIndent();
					println("clicked" + theClass.name() + " = (" + theClass.name() + ") list_fragment.getListAdapter().getItem(list_fragment.getSelectedPosition());");
//					println("list_fragment.setActivatedPosition(clicked" + theClass.name() + ");");
//					println("list_fragment.setSelection(clicked" + theClass.name() + ");");
					println();
				FileUtilities.decIndent();
				println("}");
				println();
				
//				println("if (showingDetail && !mTwoPane)");
//				println("{");
//				FileUtilities.incIndent();
//					println("list_fragment.hide();");
//					println("((DetailFragment) detail_fragment).show();");
//				FileUtilities.decIndent();
//				println("}");
//			FileUtilities.decIndent();
//			println("}");
//			println("else");
//			FileUtilities.incIndent();
//				println("showingDetail = true;");
//			FileUtilities.decIndent();
//			println();
//			println("if(AssociationEnd != ToONE)");
//			println("{");
//			FileUtilities.incIndent();
//				
//			FileUtilities.decIndent();
//			println("}");
//			println("if(AssociationEnd != ToONE)");
//			println("{");
//			FileUtilities.incIndent();
//				println("orientationSettings(clicked" + theClass.name() + ");");
//				println("((NavigationBarFragment)navigation_bar).setViewingObject(clicked" + theClass.name() + ");");
			FileUtilities.decIndent();
			println("}");
			println("((NavigationBarFragment)navigation_bar).setViewingObject(clicked" + theClass.name() + ");");
			println("orientationSettings(clicked" + theClass.name() + ");");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printActivity_onResume(MClass theClass){
		println("@Override");
		println("public void onResume()");
		println("{");
		FileUtilities.incIndent();
			println("super.onResume();");
			println("restarted = false;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onPause(MClass theClass){
		println("@Override");
		println("public void onPause()");
		println("{");
		FileUtilities.incIndent();
			println("super.onPause();");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onBackPressed(MClass theClass){
		println("@Override");
		println("public void onBackPressed()");
		println("{");
		FileUtilities.incIndent();
			println("if(AssociationEnd != ToONE && !mTwoPane && detail_fragment != null && showingDetail)");
			println("{");
			FileUtilities.incIndent();
				println("((DetailFragment) detail_fragment).hide();");
				println("((NavigationBarFragment) navigation_bar).show();");
				println("list_fragment.show();");
				println("showingDetail = false;");
			FileUtilities.decIndent();
			println("}");
			println("else");
			FileUtilities.incIndent();
				println("super.onBackPressed();");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onDestroy(MClass theClass){
		println("@Override");
		println("public void onDestroy()");
		println("{");
		FileUtilities.incIndent();
			println(theClass.name() + ".getAccess().removeChangeListener(list_fragment);");
			println("super.onDestroy();");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onItemSelected(MClass theClass){
		println("@Override");
		println("public void onItemSelected(int listPos, Object object, boolean mSinglePanelongclick)");
		println("{");
		FileUtilities.incIndent();
			println("if(object instanceof " + theClass.name() + ")");
			println("{");
			FileUtilities.incIndent();
				println("this.clicked" + theClass.name() + " = (" + theClass.name() + ") object;");
				println("((NavigationBarFragment)navigation_bar).setViewingObject(clicked" + theClass.name() + ");");
				println();
				println("if (mTwoPane)");
				println("{");
				FileUtilities.incIndent();
					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL, clicked" + theClass.name() + ");");
				FileUtilities.decIndent();
				println("}");
				println("else if(mSinglePanelongclick)");
				println("{");
				FileUtilities.incIndent();
					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL, clicked" + theClass.name() + ");");
//					println("showingDetail = true;");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onPrepareOptionsMenu(MClass theClass){
		println("@Override");
		println("public boolean onPrepareOptionsMenu(Menu menu)");
		println("{");
		FileUtilities.incIndent();
			println("super.onPrepareOptionsMenu(menu);");
			if(theClass.isAbstract()){
				println("menu.findItem(R.id.menu_new).setEnabled(false);");
				println("menu.findItem(R.id.menu_new).setEnabled(false);");
				println("menu.findItem(R.id.menu_edit).setEnabled(false);");
				println("menu.findItem(R.id.menu_edit).setEnabled(false);");
				println("menu.findItem(R.id.menu_delete).setEnabled(false);");
				println("menu.findItem(R.id.menu_delete).setEnabled(false);");
			}
			println("return true;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onOptionsItemSelected(MClass theClass){
		println("@Override");
		println("public boolean onOptionsItemSelected(MenuItem item)");
		println("{");
		FileUtilities.incIndent();
			println("switch (item.getItemId())");
			println("{");
			FileUtilities.incIndent();
				if(!theClass.isAbstract()){
					println("case R.id.menu_new:");
					FileUtilities.incIndent();
	//					println("((NavigationBarFragment) navigation_bar).hide();");
						println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_NEW, null);");
	//					println("showingDetail = true;");
						println("break;");
					FileUtilities.decIndent();
				}
				println("case R.id.menu_confirm_oncreation:");
				FileUtilities.incIndent();
					println("if(ONCREATION)");
					println("{");
					FileUtilities.incIndent();
						setActivityResult(theClass);
						println("finish();");
					FileUtilities.decIndent();
					println("}");
					println("break;");
				FileUtilities.decIndent();
				if(!theClass.isAbstract()){
					println("case R.id.menu_edit:");
					FileUtilities.incIndent();
						println("if (AssociationEnd == ToONE || list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
						println("{");
						FileUtilities.incIndent();
	//						println("((NavigationBarFragment) navigation_bar).hide();");
							println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_EDIT,	clicked" + theClass.name() + ");");
	//						println("showingDetail = true;");
						FileUtilities.decIndent();
						println("}");
						println("break;");
					FileUtilities.decIndent();
					println("case R.id.menu_delete:");
					FileUtilities.incIndent();
						println("if(Transactions.StartTransaction())");
						println("{");
						FileUtilities.incIndent();
							println("if (AssociationEnd == ToMANY && list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
							println("{");
							FileUtilities.incIndent();
								println("clicked" + theClass.name() + ".delete();");
								println("list_fragment.show();");
	//							println("list_fragment.setActivatedPosition(ListView.INVALID_POSITION);");
	//							println("list_fragment.setSelection(ListView.INVALID_POSITION);");
	//							println("getFragmentManager().beginTransaction().detach(detail_fragment).commit();");
							FileUtilities.decIndent();
							println("}");
							println("else");
							println("{");
							FileUtilities.incIndent();
								println("clicked" + theClass.name() + ".delete();");
								println("finish();");
							FileUtilities.decIndent();
							println("}");
							println("Transactions.StopTransaction();");
						FileUtilities.decIndent();
						println("}");
						println("break;");
					FileUtilities.decIndent();
				}
			FileUtilities.decIndent();
			println("}");
			println("return super.onOptionsItemSelected(item);");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	private void setActivityResult(MClass theClass){
		if(isSubClass(theClass)){
			List<MClass> alreadyAdded = new ArrayList<MClass>();
			Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				if(!alreadyAdded.contains(association.getTargetAE().cls())){
					RepeteadNeighbors.put(association.getTargetAE().cls(), 1);
					alreadyAdded.add(association.getTargetAE().cls());
				}
				else
					RepeteadNeighbors.put(association.getTargetAE().cls(), RepeteadNeighbors.get(association.getTargetAE().cls()) + 1);
			}
			
			alreadyAdded.clear();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				if(!alreadyAdded.contains(association.getTargetAE().cls())){
					println("if(" + association.getTargetAE().cls().name().toLowerCase() + " != null)");
					println("{");
					FileUtilities.incIndent();
					if(RepeteadNeighbors.containsKey(association.getTargetAE().cls()) && RepeteadNeighbors.get(association.getTargetAE().cls()).intValue() > 1){
						for(AssociationInfo x : AssociationInfo.getAssociationsInfo(association.getTargetAE().cls()))
							if(theClass.isSubClassOf(x.getTargetAE().cls())){
								println("if(" + association.getTargetAE().cls().name() + "End.equals(\"" + x.getTargetAE().cls() + "\"))");
								println("{");
								FileUtilities.incIndent();
									println("setResult(Activity.RESULT_OK, new Intent().putExtra(\"" + x.getTargetAE().cls().name() + "\", clicked" + theClass.name() + ".ID()));");
								FileUtilities.decIndent();
								println("}");
							}	
					}else
						println("setResult(Activity.RESULT_OK, new Intent().putExtra(\"" + association.getSourceAE().cls().name() + "\", clicked" + theClass.name() + ".ID()));");
					FileUtilities.decIndent();
					println("}");
					alreadyAdded.add(association.getTargetAE().cls());
				}
			}
		}else
			println("setResult(Activity.RESULT_OK, new Intent().putExtra(\"" + theClass.name() + "\", clicked" + theClass.name() + ".ID()));");
	}
	
	@Override
	public void printActivity_onActivityResult(MClass theClass){
		println("@Override");
		println("public void onActivityResult(int requestCode, int resultCode, Intent data)");
		println("{");
		FileUtilities.incIndent();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			println("if (requestCode == CREATION_CODE && resultCode == Activity.RESULT_OK && data.getExtras().containsKey(\"" + association.getTargetAE().cls().name() + "\"))");
			println("{");
			FileUtilities.incIndent();
				println("clicked" + theClass.name() + ".insertAssociation((" + association.getTargetAE().cls().name() + ") " + association.getTargetAE().cls().name() + ".get" + association.getTargetAE().cls().name() + "((Integer) data.getExtras().get(\"" + association.getTargetAE().cls().name() + "\")));");
				println("Transactions.StopTransaction();");
			FileUtilities.decIndent();
			println("}");
		}
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_onDetailOK(MClass theClass){
		if(!theClass.isAbstract()){
			println("@Override");
			println("public void onDetailOK(boolean isNew, " + theClass.name() + " new" + theClass.name() + ")");
			println("{");
	//		println("Log.i(\"carreguei no ok\", \"carreguei no ok\");");
			FileUtilities.incIndent();
				println("if(Transactions.StartTransaction())");
				println("{");
				FileUtilities.incIndent();
					println("if(isNew)");
					println("{");
					FileUtilities.incIndent();
						println(" new" + theClass.name() + ".insert();");
						println();
						for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
							println("if(" + association.getTargetAE().cls().name().toLowerCase() + " != null && !Transactions.isCanceled())");
							FileUtilities.incIndent();
								println("" + association.getTargetAE().cls().name().toLowerCase() + ".insertAssociation(new" + theClass.name() + ");");
							FileUtilities.decIndent();
						}
					FileUtilities.decIndent();
					println("}");
					println("else");
					println("{");
					FileUtilities.incIndent();
						println(" new" + theClass.name() + ".update();");
					FileUtilities.decIndent();
					println("}");
					println();
					println("if(!Transactions.StopTransaction())");
					FileUtilities.incIndent();
						println("Transactions.ShowErrorMessage(this);");
					FileUtilities.decIndent();
					println("else");
					println("{");
					FileUtilities.incIndent();
						println("clicked" + theClass.name() + " = new" + theClass.name() + ";");
	//					println("((NavigationBarFragment) navigation_bar).show();");
						println("if (mTwoPane)");
						FileUtilities.incIndent();
							println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL,clicked" + theClass.name() + ");");
						FileUtilities.decIndent();
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("((DetailFragment) detail_fragment).hide();");
							println("list_fragment.show();");
							println("showingDetail = false;");
						FileUtilities.decIndent();
						println("}");
						println("if(AssociationEnd == ToMANY)");
						println("{");
						FileUtilities.incIndent();
							println("list_fragment.setActivatedPosition(clicked" + theClass.name() + ");");
							println("list_fragment.setSelection(clicked" + theClass.name() + ");");
						FileUtilities.decIndent();
						println("}");
						
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("if(ONCREATION)");
					println("{");
					FileUtilities.incIndent();
						println("clicked" + theClass.name() + " = new" + theClass.name() + ";");
						println("clicked" + theClass.name() + ".insert();");
						
						setActivityResult(theClass);
						
						println("finish();");
					FileUtilities.decIndent();
					println("}");
					println("else");
					println("{");
					FileUtilities.incIndent();
						println("//concurrency error");
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	@Override
	public void printActivity_onDetailCancel(MClass theClass){
		if(!theClass.isAbstract()){
			println("@Override");
			println("public void onDetailCancel()");
			println("{");
			println("((NavigationBarFragment) navigation_bar).show();");
			FileUtilities.incIndent();
				println("if (mTwoPane)");
				println("{");
				FileUtilities.incIndent();
					println("if (clicked" + theClass.name() + " != null)");
					FileUtilities.incIndent();
						println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL,clicked" + theClass.name() + ");");
					FileUtilities.decIndent();
					println("else");
					FileUtilities.incIndent();
						println("getFragmentManager().beginTransaction().detach(detail_fragment).commit();");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("((DetailFragment) detail_fragment).hide();");
					println("list_fragment.show();");
					println("showingDetail = false;");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	@Override
	public void printActivity_addToList(MClass theClass){
		println("@Override");
		println("public void addToList(Class<?> caller, Object object,  Object neibor)");
		println("{");
		FileUtilities.incIndent();
			print("if(caller == " + theClass.name() + ".class" );
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass))
				print(" && " + association.getTargetAE().cls().name().toLowerCase() + " == null");
			println(")" );
			FileUtilities.incIndent();
				println("list_fragment.add(object);");
			FileUtilities.decIndent();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				print("if(caller == " + association.getTargetAE().cls().name() + ".class && " 
						+ association.getTargetAE().cls().name().toLowerCase() + " != null");
				if(is2Many(association, association.getSourceAE()))
					println(" && " + association.getTargetAE().cls().name().toLowerCase() + "." + association.getSourceAE().name() + "() == neibor)");
				else
					println(" && " + association.getTargetAE().cls().name().toLowerCase() + " == neibor)" );
				FileUtilities.incIndent();
					println("list_fragment.add(object);");
				FileUtilities.decIndent();
			}
		
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printActivity_propertyChange(MClass theClass){
		println("@Override");
		println("public void propertyChange(PropertyChangeEvent propertyChangeEvent)");
		println("{");
		FileUtilities.incIndent();
			println("//FALTA AKI");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	

//	******************** --- Activity - End --- *****************************
	
	
	
	
	
	
	
	
	
	
	
	
	@SuppressWarnings("unchecked")
	public <T> Collection<T> joinWithOutRepeated(Collection<T> c1, Collection<T> c2){
		for(Object o2 : c2)
			if(!c1.contains(o2))
				c1.add((T) o2);
		return c1;
	}
	
	
//	******************** --- Fragment - Detail - Start --- *****************************
	
	private List<MAttribute> getDetailViewAttributes(MClass theClass, boolean creation, boolean display){
//		--------------*************** CODIGO NOVO - START  ******************* ------------------
		List<MAttribute> inheritedUniqueAttributes = new ArrayList<MAttribute>();
		for (MClass theParentClass : theClass.allParents()){
			if(creation && theParentClass.isAnnotated() && theParentClass.getAnnotation("creation") != null)
				inheritedUniqueAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrdered(theParentClass.attributes(), theParentClass.getAnnotation("creation").getValues()));
			if(display && theParentClass.isAnnotated() && theParentClass.getAnnotation("display") != null)
				inheritedUniqueAttributes = (List<MAttribute>) joinWithOutRepeated(inheritedUniqueAttributes,ModelUtilities.annotationValuesToAttributeOrdered(theParentClass.attributes(), theParentClass.getAnnotation("display").getValues()));
		}
		List<MAttribute> finalAttributes = new ArrayList<MAttribute>();
		finalAttributes.addAll(inheritedUniqueAttributes);
		if(creation && theClass.isAnnotated() && theClass.getAnnotation("creation") != null)
			finalAttributes.addAll(ModelUtilities.annotationValuesToAttributeOrdered(theClass.attributes(), theClass.getAnnotation("creation").getValues()));
		if(display && theClass.isAnnotated() && theClass.getAnnotation("display") != null)
			finalAttributes = (List<MAttribute>) joinWithOutRepeated(inheritedUniqueAttributes,ModelUtilities.annotationValuesToAttributeOrdered(theClass.attributes(), theClass.getAnnotation("display").getValues()));
//		--------------*************** CODIGO NOVO - END  ******************* ------------------
		return finalAttributes;
	}
	
	@Override
	public void printDetailFragment_ClassHeader(MClass theClass, String layerName) {
		printFileHeader(theClass.name(), layerName);
		// visitAnnotations(e);

		printDetailFragment_Imports(theClass);

		print("public ");

		print("class " + theClass.name() + "DetailFragment");
		
		print(" extends Fragment implements PropertyChangeListener, DetailFragment");
		println("{");
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printDetailFragment_Imports(MClass theClass)
	{
		println("import " + basePackageName + ".R;");
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
		println("import " + basePackageName + "." + utilsLayerName + ".DetailFragment;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		println();

		Set<String> imports = new HashSet<String>();
		
		Set<String> androidClassTypes = new HashSet<String>();
		Set<Type> javaClassTypes = new HashSet<Type>();
		
//		--------------*************** CODIGO NOVO - START  ******************* ------------------
//		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);
//		--------------*************** CODIGO NOVO - END  ******************* ------------------
		
		List<MAttribute> inheritedUniqueAttributes = new ArrayList<MAttribute>();
		for (MClass theParentClass : theClass.allParents()){
			List<MAttribute> inheritedAttributes_temp = new ArrayList<MAttribute>();
			for(MAttribute attribute : theParentClass.attributes())
				inheritedAttributes_temp.add(attribute);
			inheritedUniqueAttributes.addAll(0, inheritedAttributes_temp);
		}
		List<MAttribute> AllAttributes = new ArrayList<MAttribute>();
		AllAttributes.addAll(inheritedUniqueAttributes);
		AllAttributes.addAll(theClass.attributes());
		
		for (MAttribute att : AllAttributes){
			String type = AndroidTypes.androidPrimitiveTypeToReadWidget(att.type());
			androidClassTypes.add(type);
			if(!type.equals(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type())))
				androidClassTypes.add(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type()));
			
			javaClassTypes.add(att.type());

			if(att.type().isEnum())
				imports.add("import " + basePackageName + "." + businessLayerName + "." + att.type().toString() + ";");
		}

		androidClassTypes.add("Activity");
		androidClassTypes.add("Fragment");
		androidClassTypes.add("FragmentTransaction");
		androidClassTypes.add("Bundle");
		androidClassTypes.add("LayoutInflater");
		androidClassTypes.add("View");
		androidClassTypes.add("View.OnClickListener");
		androidClassTypes.add("ViewGroup");
		androidClassTypes.add("InputMethodManager");
		androidClassTypes.add("Context");

		if(!androidClassTypes.contains("TextView"))
			androidClassTypes.add("TextView");
		
		imports.addAll(AndroidTypes.javaInAndroidImportDeclarations(javaClassTypes));
		imports.addAll(AndroidTypes.androidImportDeclarations(androidClassTypes));
//		System.out.println(theClass.name() + " - " + imports.toString());
		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
	}
	
	@Override
	public void printDetailFragment_Attributes(MClass theClass) {

		println("public static final String ARG_VIEW_DETAIL = \"detail\";");
		if(!theClass.isAbstract())
			println("public static final String ARG_VIEW_NEW = \"new\";");
		if(!theClass.isAbstract())
			println("public static final String ARG_VIEW_EDIT = \"edit\";");
		println("private Fragment fragment;");
		println("private View rootView = null;");
		println("private " + theClass.name() + " " + theClass.name().toLowerCase() + " = null;");
		println("private int " + theClass.name().toLowerCase() + "ID = 0;");
		println("private final String " + theClass.name().toUpperCase() + "ID = \"" + theClass.name().toUpperCase() + "ID\";");

		if(!theClass.isAbstract()){
	//		--------------*************** CODIGO NOVO - START  ******************* ------------------
			List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);
	//		--------------*************** CODIGO NOVO - END  ******************* ------------------
			
			for (MAttribute att : finalAttributes)
	//			if(att.name() != "ID")
					println("private " + AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type()) + " " + att.name().toLowerCase() + "View;");
		}
		println();
	}

	@Override
	public void printDetailFragment_DefaultConstructor(MClass theClass) {
		println("/**********************************************************************");
		println("* Default constructor");
		println("**********************************************************************/");
		println("public " + theClass.name() + "DetailFragment()");
		println("{");
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_onCreate(MClass theClass) {
		println("@Override");
		println("public void onCreate(Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onCreate(savedInstanceState);");
			println("if(savedInstanceState != null)");
		 	println("{");
		 	FileUtilities.incIndent();
		 		println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "(savedInstanceState.getInt(" + theClass.name().toUpperCase() + "ID));");
		 	FileUtilities.decIndent();
		 	println("}");
		 	println("else");
		 	println("{");
		 	FileUtilities.incIndent();
			 	println("if(getArguments() != null)");
			 	println("{");
			 	FileUtilities.incIndent();
			 		println("if (getArguments().containsKey(ARG_VIEW_DETAIL))");
			 		println("{");
			 		FileUtilities.incIndent();
			 			println(theClass.name().toLowerCase() + " = (" + theClass.name() + ") getArguments().getSerializable(ARG_VIEW_DETAIL);");
			 			println(theClass.name().toLowerCase() + "ID = " + theClass.name().toLowerCase() + ".ID();");
			 			println(theClass.name() + ".getAccess().setChangeListener(this);");
			 		FileUtilities.decIndent();
			 		println("}");
			 		if(!theClass.isAbstract()){
				 		println("if(getArguments().containsKey(ARG_VIEW_EDIT))");
				 		println("{");
						FileUtilities.incIndent();
				 			println(theClass.name().toLowerCase() + " = (" + theClass.name() + ") getArguments().getSerializable(ARG_VIEW_EDIT);");
				 			println(theClass.name().toLowerCase() + "ID = " + theClass.name().toLowerCase() + ".ID();");
				 			println(theClass.name() + ".getAccess().setChangeListener(this);");
				 		FileUtilities.decIndent();
				 		println("}");
				 		println("if(getArguments().containsKey(ARG_VIEW_NEW))");
				 		println("{");
				 		FileUtilities.incIndent();
				 			println("");
				 		FileUtilities.decIndent();
				 		println("}");
			 		}
			 	FileUtilities.decIndent();
				println("}");
		 	FileUtilities.decIndent();
		 	println("}");
		 	println("fragment = this;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printDetailFragment_onSaveInstanceState(MClass theClass){
		println("@Override");
		println("public void onSaveInstanceState(Bundle outState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onSaveInstanceState(outState);");
			println("if(" + theClass.name().toLowerCase() + " != null)");
			FileUtilities.incIndent();
				println("outState.putInt(" + theClass.name().toUpperCase() + "ID, " + theClass.name().toLowerCase() + ".ID());");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_onDestroy(MClass theClass) {
		println("@Override");
		println("public void onDestroy()");
		println("{");
		FileUtilities.incIndent();
			println("super.onDestroy();");
			println(theClass.name() + ".getAccess().removeChangeListener(this);");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_onActivityCreated(MClass theClass) {
		println("@Override");
		println("public void onActivityCreated(Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onActivityCreated(savedInstanceState);");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_onAttach(MClass theClass) {
		println("@Override");
		println("public void onAttach(Activity activity)");
		println("{");
		FileUtilities.incIndent();
			println("super.onAttach(activity);");
			println("if (!(activity instanceof Callbacks))");
		 	println("{");
		 	FileUtilities.incIndent();
		 		println("throw new IllegalStateException(\"Activity must implement fragment's callbacks.\");");
			FileUtilities.decIndent();
			println("}");

			println("mCallbacks = (Callbacks) activity;");
			
//			println("if(getArguments() != null)");
//		 	println("{");
//		 	FileUtilities.incIndent();
//		 		println("if (getArguments().containsKey(ARG_VIEW_DETAIL))");
//		 		println("{");
//		 		FileUtilities.incIndent();
//		 			println(theClass.name().toLowerCase() + " = (" + theClass.name() + ") getArguments().getSerializable(ARG_VIEW_DETAIL);");
//		 		FileUtilities.decIndent();
//		 		println("}");
//		 		println("if(getArguments().containsKey(ARG_VIEW_EDIT))");
//		 		println("{");
//				FileUtilities.incIndent();
//		 			println(theClass.name().toLowerCase() + " = (" + theClass.name() + ") getArguments().getSerializable(ARG_VIEW_EDIT);");
//		 		FileUtilities.decIndent();
//		 		println("}");
//		 		println("if(getArguments().containsKey(ARG_VIEW_NEW))");
//		 		println("{");
//		 		FileUtilities.incIndent();
//		 			println("");
//		 		FileUtilities.decIndent();
//		 		println("}");
//		 	FileUtilities.decIndent();
//		 	println("}");
			
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_onCreateView(MClass theClass) {
		println("@Override");
		println("public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("if(container == null && " + theClass.name().toLowerCase() + " == null)");
		 	println("{");
		 	FileUtilities.incIndent();
		 		println("return inflater.inflate(R.layout.default_blank_fragment, container, false);");
			FileUtilities.decIndent();
			println("}");
			
			println("if(getArguments() != null)");
		 	println("{");
		 	FileUtilities.incIndent();
		 		println("if (getArguments().containsKey(ARG_VIEW_DETAIL))");
		 		println("{");
		 		FileUtilities.incIndent();
		 			println("rootView = inflater.inflate(R.layout." + theClass.name().toLowerCase() + "_view_detail, container, false);");
		 			println("setViewDetailData();");
		 		FileUtilities.decIndent();
		 		println("}");
		 		if(!theClass.isAbstract()){
			 		println("if(getArguments().containsKey(ARG_VIEW_NEW) || getArguments().containsKey(ARG_VIEW_EDIT))");
			 		println("{");
					FileUtilities.incIndent();
						println("rootView = inflater.inflate(R.layout." + theClass.name().toLowerCase() + "_view_insertupdate, container, false);");
			 			println("setViewNewOrEditData();");
			 			println("rootView.findViewById(R.id.okButton).setOnClickListener(ClickListener);");
			 			println("rootView.findViewById(R.id.cancelButton).setOnClickListener(ClickListener);");
			 			println("if(getArguments().containsKey(ARG_VIEW_NEW))");
			 			println("{");
			 			FileUtilities.incIndent();
			 				println("((TextView) rootView.findViewById(R.id.okButton)).setText(\"Create\");");
			 			FileUtilities.decIndent();
			 			println("}");
			 			println("else");
			 			println("{");
			 			FileUtilities.incIndent();
			 				println("((TextView) rootView.findViewById(R.id.okButton)).setText(\"Update\");");
			 			FileUtilities.decIndent();
			 			println("}");
			 		FileUtilities.decIndent();
			 		println("}");
		 		}
		 	FileUtilities.decIndent();
		 	println("}");
		println("return rootView;");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printDetailFragment_VisibilityState(MClass theClass) {
		println("@Override");
		println("public void hide()");
		println("{");
		FileUtilities.incIndent();
			println("if(getView() != null)");
			FileUtilities.incIndent();
				println("((View) getView().getParent()).setVisibility(android.view.View.GONE);");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
		
		println("@Override");
		println("public void show()");
		println("{");
		FileUtilities.incIndent();
			println("if(getView() != null)");
			FileUtilities.incIndent();
			println("((View) getView().getParent()).setVisibility(android.view.View.VISIBLE);");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
		
		println("@Override");
		println("public boolean isGone()");
		println("{");
		FileUtilities.incIndent();
			println("if(((View) getView().getParent()).getVisibility() == android.view.View.GONE)");
			FileUtilities.incIndent();
				println("return true;");
			FileUtilities.decIndent();
			println("else");
			FileUtilities.incIndent();
				println("return false;");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printDetailFragment_SetInputMethod(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public void setInput()");
			println("{");
			FileUtilities.incIndent();
	//		--------------*************** CODIGO NOVO - START  ******************* ------------------
			List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
	//		--------------*************** CODIGO NOVO - END  ******************* ------------------
				for (MAttribute att : finalAttributes)
	//				if(att.name() != "ID")
						println(att.name().toLowerCase() + "View = (" + AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type()) + ") rootView.findViewById(R.id." + attributeBaseAncestor(theClass, att).name().toLowerCase() + "_insertupdate_" + att.name().toLowerCase() + "_value);");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	@Override
	public void printDetailFragment_setViewDetailData(MClass theClass) {
		println("public View setViewDetailData()");
		println("{");
		FileUtilities.incIndent();
			println("if (" + theClass.name().toLowerCase() + " != null)");
			println("{");
			FileUtilities.incIndent();
//			--------------*************** CODIGO NOVO - START  ******************* ------------------
			List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, false, true);
//			--------------*************** CODIGO NOVO - END  ******************* ------------------
				for (MAttribute att : finalAttributes)
					if(att.name() != "ID")
						println("((" + AndroidTypes.androidPrimitiveTypeToReadWidget(att.type()) + ") rootView.findViewById(R.id." + attributeBaseAncestor(theClass, att).name().toLowerCase() + "_detail_" + att.name().toLowerCase() + "_value))." + AndroidTypes.androidWidgetContentSetter(att.type(), theClass.name().toLowerCase() + "." + att.name() + "()") + ";");
			FileUtilities.decIndent();
			println("}");
			println("return rootView;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printDetailFragment_ActionViewDetail(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public void ActionViewDetail()");
			println("{");
			FileUtilities.incIndent();
				println("if(" + theClass.name().toLowerCase() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	@Override
	public void printDetailFragment_setViewNewOrEditData(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public View setViewNewOrEditData()");
			println("{");
			FileUtilities.incIndent();
				println("if (" + theClass.name().toLowerCase() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("setInput();");
	//				--------------*************** CODIGO NOVO - START  ******************* ------------------
					List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
	//				--------------*************** CODIGO NOVO - END  ******************* ------------------
					for (MAttribute att : finalAttributes)
	//					if(att.name() != "ID")
							println(att.name().toLowerCase() + "View." + AndroidTypes.androidWidgetContentSetter(att.type(), theClass.name().toLowerCase() + "." + att.name() + "()") + ";");
				FileUtilities.decIndent();
				println("}");
				println("return rootView;");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}

	@Override
	public void printDetailFragment_ActionViewNew(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public " + theClass.name() + " ActionViewNew()");
			println("{");
			FileUtilities.incIndent();
				println("rootView = this.getView();");
				println("setInput();");
				
	//			--------------*************** CODIGO NOVO - START  ******************* ------------------
				List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
	//			--------------*************** CODIGO NOVO - END  ******************* ------------------
				
				List<MAttribute> inheritedUniqueAttributes = new ArrayList<MAttribute>();
				for (MClass theParentClass : theClass.allParents()){
					List<MAttribute> inheritedAttributes_temp = new ArrayList<MAttribute>();
					for(MAttribute attribute : theParentClass.attributes())
						inheritedAttributes_temp.add(attribute);
					inheritedUniqueAttributes.addAll(0, inheritedAttributes_temp);
				}
				List<MAttribute> AllAttributes = new ArrayList<MAttribute>();
				AllAttributes.addAll(inheritedUniqueAttributes);
				AllAttributes.addAll(theClass.attributes());
				
				for (MAttribute att : AllAttributes)
	//				if(att.name() != "ID")
					if(finalAttributes.contains(att)){
						println(JavaInputValidation.inputValidation(att.type(), "temp_" + att.name(), att.name(), AndroidTypes.androidInputWidgetContentGetter(att.type(), att.name().toLowerCase() + "View"), "UtilNavigate.showWarning(getActivity(), ", true , getIndentSpace(), false));
						println();
					}else if(!att.name().equals("ID")){
						println(JavaInputValidation.inputValidation(att.type(), "temp_" + att.name(), att.name(), defaultValueType(att.type()), "UtilNavigate.showWarning(getActivity(), ", true , getIndentSpace(), true));
						println();
					}
	//			List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
	//			for (MClass theParentClass : theClass.allParents())
	//				for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
	//					inheritedAttributes.add(attribute);
				
				print("return new " + theClass.name() + "(");
				
				for (int i = 0; i < AllAttributes.size(); i++)
				{
	//				if(inheritedAttributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !inheritedAttributes.get(i).getName().equals("ID")){
	//					.contains na da :/ ???
	//					if(finalAttributes.contains(AllAttributes.get(i)))~
					if(AllAttributes.get(i).name() != "ID")
							print("temp_" + AllAttributes.get(i).name());
	//					else if(AllAttributes.get(i).name() != "ID")
	//						print(defaultValueType(AllAttributes.get(i).type()));
						
						if (i < AllAttributes.size() - 1 && AllAttributes.get(i).name() != "ID")
							print(", ");
	//				}
				}
	//			List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(theClass);
	//			if (inheritedAttributes.size() > 0 && attributes.size() > 0)
	//				print(", ");
	//			for (int i = 0; i < attributes.size(); i++)
	//			{
	//				if(attributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !attributes.get(i).getName().equals("ID")){
	//					print("temp_" + attributes.get(i).getName());
	//					if (i < attributes.size() - 1)
	//						print(", ");
	//				}
	//			}
				println(");");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	private String defaultValueType(Type oclType){
		
		if (oclType.isInteger())
			return "0";
		else if (oclType.isReal())
			return "0.0";
		else if (oclType.isBoolean())
			return "false";
		else if (oclType.isString())
			return "\"\"";
		else if (oclType.isEnum()){
			return oclType.toString() + ".valueOf(" + oclType.toString() + ".values()[0].toString())";
		}
		else if (oclType.isObjectType())
			if((oclType.toString().equals("Date")))
				return "";
			else
				return oclType.toString();
		else if (oclType.isTrueObjectType())
			return oclType.toString();
		else if (oclType.isTrueOclAny())
			return "null";
		else if (oclType.isVoidType())
			return "null";
		else if (oclType.isDate())
			return "";
		else
			return "null";
	}
	
	@Override
	public void printDetailFragment_ActionViewEdit(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public boolean ActionViewEdit()");
			println("{");
			FileUtilities.incIndent();
				println("if (" + theClass.name().toLowerCase() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("rootView = this.getView();");
	//				--------------*************** CODIGO NOVO - START  ******************* ------------------
					List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
	//				--------------*************** CODIGO NOVO - END  ******************* ------------------
					for (MAttribute att : finalAttributes)
	//					if(att.name() != "ID")
							println(JavaInputValidation.inputComparatorConditionSetter(att.type(), "temp_" + att.name(), theClass.name().toLowerCase() + "." + att.name() + "()", theClass.name().toLowerCase() + ".set" + capitalize(att.name()), AndroidTypes.androidInputWidgetContentGetter(att.type(), att.name().toLowerCase() + "View"), getIndentSpace()));
				
					println("return true;");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("return false;");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
			println();
		}
	}
	
	@Override
	public void printDetailFragment_InnerCallBackInterface(MClass theClass) {
		println("public interface Callbacks");
		println("{");
		if(!theClass.isAbstract()){
			FileUtilities.incIndent();
				println("public void onDetailOK(boolean isNew, " + theClass.name() + " new" + theClass.name() + ");");
				println("public void onDetailCancel();");
			FileUtilities.decIndent();
		}
		println("}");
		println();
		
	}

	@Override
	public void printDetailFragment_CallBackDeclaration(MClass theClass) {
		println("private Callbacks mCallbacks = new Callbacks()");
		println("{");
		if(!theClass.isAbstract()){
			FileUtilities.incIndent();
				println("public void onDetailOK(boolean isNew, " + theClass.name() + " new" + theClass.name() + ") {\t}");
				println("public void onDetailCancel() {\t}");
			FileUtilities.decIndent();
		}
		println("};");
		println();
	}

	@Override
	public void printDetailFragment_ScreenClickListeners(MClass theClass) {
		println("OnClickListener ClickListener = new OnClickListener()");
		println("{");
		FileUtilities.incIndent();
			println("@Override");
			println("public void onClick(View v)");
		 	println("{");
		 	FileUtilities.incIndent();
			if(!theClass.isAbstract()){
				println("if(v.getId() == R.id.okButton)");
				println("{");
				FileUtilities.incIndent();
					println("InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);");
					println("inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);");
					println("if(getArguments().containsKey(ARG_VIEW_NEW))");
					println("{");
					FileUtilities.incIndent();
						println(theClass.name().toLowerCase() + " = ActionViewNew();");
						println("if(" + theClass.name().toLowerCase() + " != null)");
						println("{");
						FileUtilities.incIndent();
							println("mCallbacks.onDetailOK(true, " + theClass.name().toLowerCase() + ");");
			 			FileUtilities.decIndent();
			 			println("}");
					FileUtilities.decIndent();
					println("}");
					println("if(getArguments().containsKey(ARG_VIEW_EDIT))");
					println("{");
					FileUtilities.incIndent();
						println("if(ActionViewEdit())");
		 				FileUtilities.incIndent();
		 					println("mCallbacks.onDetailOK(false, " + theClass.name().toLowerCase() + ");");
		 				FileUtilities.decIndent();
		 			FileUtilities.decIndent();
		 			println("}");		 			
				FileUtilities.decIndent();
				println("}");
				println("if(v.getId() == R.id.cancelButton)");
		 		println("{");
		 		FileUtilities.incIndent();
		 			println("InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);");
		 			println("inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);");
		 			println("mCallbacks.onDetailCancel();");
		 		FileUtilities.decIndent();
		 		println("}");
			}
		 	FileUtilities.decIndent();
		 	println("}");
		FileUtilities.decIndent();
		println("};");
		println();
	}

	@Override
	public void printDetailFragment_BusinessListeners(MClass theClass) {
		println("@Override");
		println("public void propertyChange(final PropertyChangeEvent event)");
		println("{");
		FileUtilities.incIndent();
			println("getActivity().runOnUiThread(new Runnable()");
			println("{");
			FileUtilities.incIndent();
				println("@Override");
				println("public void run()");
				println("{");
				FileUtilities.incIndent();
					println("switch(event.getCommandType())");
					println("{");
					FileUtilities.incIndent();
						println("case INSERT:");
						FileUtilities.incIndent();
							println("//no need to change anything");
						FileUtilities.decIndent();
						println("break;");
						
						println("case INSERT_ASSOCIATION:");
						FileUtilities.incIndent();
							println("//no need to change anything");
						FileUtilities.decIndent();
						println("break;");
						
						println("case UPDATE:");
						FileUtilities.incIndent();
							println("if(" + theClass.name().toLowerCase() + "ID == event.getOldObjectID())");
							FileUtilities.incIndent();
								println(theClass.name().toLowerCase() + " = (" + theClass.name() + ") event.getNewObject();");
								println(theClass.name().toLowerCase() + "ID = " + theClass.name().toLowerCase() + ".ID();");
								println("if (getArguments().containsKey(ARG_VIEW_DETAIL))");
								FileUtilities.incIndent();
									println("setViewDetailData();");
								FileUtilities.decIndent();
								if(!theClass.isAbstract()){
									println("else if(getArguments().containsKey(ARG_VIEW_EDIT))");
									FileUtilities.incIndent();
										println("setViewNewOrEditData();");
									FileUtilities.decIndent();
								}
							FileUtilities.decIndent();
						FileUtilities.decIndent();
						println("break;");
						
						println("case DELETE:");
						FileUtilities.incIndent();
							println("if(" + theClass.name().toLowerCase() + "ID == event.getOldObjectID())");
							println("{");
							FileUtilities.incIndent();
								println("FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();");
								println("ft.remove(fragment);");
								println("ft.commit();");
							FileUtilities.decIndent();
							println("}");
						FileUtilities.decIndent();
						println("break;");
						
						println("case DELETE_ASSOCIATION:");
						FileUtilities.incIndent();
							println("//no need to change anything");
						FileUtilities.decIndent();
						println("break;");
						
						println("default:");
						FileUtilities.incIndent();
						
						FileUtilities.decIndent();
						println("break;");
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("});");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
//	******************** --- Fragment - Detail - End --- *****************************
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	******************** --- Fragment - Navigation Bar - Start --- *****************************
	
	@Override
	public void printNavigationBarFragment_ClassHeader(MClass theClass, String layerName) {
		printFileHeader(theClass.name(), layerName);

		printNavigationBar_Imports(theClass);

		print("public ");

		print("class " + theClass.name() + "NavigationBarFragment");
		
		print(" extends Fragment implements PropertyChangeListener, NavigationBarFragment");
		println("{");
		println();
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printNavigationBar_Imports(MClass theClass)
	{
		println("import " + basePackageName + ".R;");
		println("import " + basePackageName + ".MasterActivity;");
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		println("import " + basePackageName + "." + utilsLayerName + ".NavigationBarFragment;");
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		//inheritance navigations - start
		if(isSubClass(theClass)){
			println("import " + basePackageName + "." + businessLayerName + "." + theClass.parents().iterator().next().name() + ";");
		}
		if(isSuperClass(theClass)){
			for(MClass sub : theClass.children()){
				println("import " + basePackageName + "." + businessLayerName + "." + sub.name() + ";");
			}
		}
		//inheritance navigations - end
				
		List<MClass> activityImport = new ArrayList<MClass>();
		
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			if(isSuperClass(association.getTargetAE().cls()))
				for(MClass subClass : getAllSubClasses(Arrays.asList(association.getTargetAE().cls())))
					if(!activityImport.contains(subClass))
						activityImport.add(subClass);
			if(!activityImport.contains(association.getTargetAE().cls()))
				activityImport.add(association.getTargetAE().cls());
		}
		//inheritance navigations - start
		if(isSubClass(theClass)){
			activityImport.add(theClass.parents().iterator().next());
		}
		if(isSuperClass(theClass)){
			for(MClass sub : theClass.children()){
				activityImport.add(sub);
			}
		}
		//inheritance navigations - end
		for (MClass clazz : activityImport)
			println("import " + basePackageName + "." + presentationLayerName + "." + clazz.name() + "." + clazz.name() + "Activity;");
		println();

		Set<String> classTypes = new HashSet<String>();

		
			
//		este e com associacoes
//		nao importa todas as associacoes sao do tipo view
//		for (MAttribute att : theClass.attributes())
//			classTypes.add(AndroidTypes.androidPrimitiveTypeToWidget(att.type()));
		classTypes.add("View");
		
		classTypes.add("Activity");
		classTypes.add("Fragment");
		classTypes.add("Color");
		classTypes.add("TransitionDrawable");
		classTypes.add("Bundle");
		classTypes.add("LayoutInflater");
		classTypes.add("TextView");
		classTypes.add("View.OnClickListener");
		classTypes.add("View.OnLongClickListener");
		classTypes.add("ViewGroup");
		
		Set<String> imports = AndroidTypes.androidImportDeclarations(classTypes);

		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
	}
	
	@Override
	public void printNavigationBarFragment_Attributes(MClass theClass) {
		println("private View rootView = null;");
		println("private " + theClass.name() + " clicked" + theClass.name() + " = null;");
		println("private int clicked" + theClass.name() + "ID;");
		println("private final String " + theClass.name().toUpperCase() + "ID = \"" + theClass.name().toUpperCase() + "ID\";");
		println("private TextView number_objects;");
		
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass))
			println("private View " + association.getTargetAE().name() + "View;");
		
		//inheritance navigations - start
		if(isSubClass(theClass))
			println("private View " + theClass.parents().iterator().next().name() + "View;");
		if(isSuperClass(theClass))
			for(MClass sub : theClass.children())
				println("private View " + sub.name() + "View;");
		//inheritance navigations - end
		
		println();
	}

	@Override
	public void printNavigationBarFragment_DefaultConstructor(MClass theClass) {
		println("/**********************************************************************");
		println("* Default constructor");
		println("**********************************************************************/");
		println("public " + theClass.name() + "NavigationBarFragment()");
		println("{");

		println("}");
		println();
	}

	@Override
	public void printNavigationBarFragment_onCreate(MClass theClass) {
		println("@Override");
		println("public void onCreate(Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onCreate(savedInstanceState);");
			println("if(savedInstanceState != null)");
			println("{");
			FileUtilities.incIndent();
				println("clicked" + theClass.name() + " = " + theClass.name() + ".get" + theClass.name() + "(savedInstanceState.getInt(" + theClass.name().toUpperCase() + "ID));");
			FileUtilities.decIndent();
			println("}");
			println(theClass.name() + ".getAccess().setChangeListener(this);");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printNavigationBarFragment_onSaveInstanceState(MClass theClass) {
		println("@Override");
		println("public void onSaveInstanceState(Bundle outState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onSaveInstanceState(outState);");
			println("if(clicked" + theClass.name() + " != null)");
			println("{");
			FileUtilities.incIndent();
				println("outState.putInt(" + theClass.name().toUpperCase() + "ID, clicked" + theClass.name() + ".ID());");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_onDestroy(MClass theClass) {
		println("@Override");
		println("public void onDestroy()");
		println("{");
		FileUtilities.incIndent();
			println("super.onDestroy();");
			println(theClass.name() + ".getAccess().removeChangeListener(this);");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printNavigationBarFragment_onActivityCreated(MClass theClass) {
		println("@Override");
		println("public void onActivityCreated(Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onActivityCreated(savedInstanceState);");
			println();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				if(association.getSourceAE().cls() == theClass)
					println(association.getTargetAE().name() + "View = (View) getActivity().findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ");");
				else
					println(association.getTargetAE().name() + "View = (View) getActivity().findViewById(R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ");");
				println(association.getTargetAE().name() + "View.setOnClickListener(ClickListener);");
				println(association.getTargetAE().name() + "View.setOnLongClickListener(LongClickListener);");
				println();
			}
			//inheritance navigations - start
			if(isSubClass(theClass)){
				println(theClass.parents().iterator().next().name() + "View = (View) getActivity().findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + theClass.parents().iterator().next().name().toLowerCase() + ");");
				println(theClass.parents().iterator().next().name() + "View.setOnClickListener(ClickListener);");
				println(theClass.parents().iterator().next().name() + "View.setOnLongClickListener(LongClickListener);");
				println();
			}
			if(isSuperClass(theClass)){
				for(MClass sub : theClass.children()){
					println(sub.name() + "View = (View) getActivity().findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + sub.name().toLowerCase() + ");");
					println(sub.name() + "View.setOnClickListener(ClickListener);");
					println(sub.name() + "View.setOnLongClickListener(LongClickListener);");
					println();
				}
			}
			//inheritance navigations - end
			
			println("setViewingObject(clicked" + theClass.name() + ");");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printNavigationBarFragment_onAttach(MClass theClass) {
		println("@Override");
		println("public void onAttach(Activity activity)");
		println("{");
		FileUtilities.incIndent();
			println("super.onAttach(activity);");
		FileUtilities.decIndent();
		println("}");
		println();
	}

	@Override
	public void printNavigationBarFragment_onCreateView(MClass theClass) {
		println("@Override");
		println("public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("rootView = inflater.inflate(R.layout." + theClass.name().toLowerCase() + "_view_navigationbar, container, false);");
			println("return rootView;");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_VisibilityState(MClass theClass) {
		println("@Override");
		println("public void hide()");
		println("{");
		FileUtilities.incIndent();
			println("if(getView() != null)");
			FileUtilities.incIndent();
				println("((View) getView().getParent()).setVisibility(android.view.View.GONE);");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
		
		println("@Override");
		println("public void show()");
		println("{");
		FileUtilities.incIndent();
			println("if(getView() != null)");
			FileUtilities.incIndent();
				println("((View) getView().getParent()).setVisibility(android.view.View.VISIBLE);");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
		
		println("@Override");
		println("public boolean isGone()");
		println("{");
		FileUtilities.incIndent();
			println("if(((View) getView().getParent()).getVisibility() == android.view.View.GONE)");
			FileUtilities.incIndent();
				println("return true;");
			FileUtilities.decIndent();
			println("else");
			FileUtilities.incIndent();
				println("return false;");
			FileUtilities.decIndent();
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_setViewingObject(MClass theClass) {
		println("public void setViewingObject(Object " + theClass.name().toLowerCase() + ")");
		println("{");
		FileUtilities.incIndent();
			println("if(" + theClass.name().toLowerCase() + " instanceof " + theClass.name() + ")");
			println("{");
			FileUtilities.incIndent();
				println("clicked" + theClass.name() + " = (" + theClass.name() + ") " + theClass.name().toLowerCase() + ";");
				println("if(" + theClass.name().toLowerCase() + " != null)");
				FileUtilities.incIndent();
					println("clicked" + theClass.name() + "ID = ((" + theClass.name() + ") " + theClass.name().toLowerCase() + ").ID();");
				FileUtilities.decIndent();
				println("refreshNavigationBar(clicked" + theClass.name() + ");");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_refreshNavigationBar(MClass theClass) {
		println("public void refreshNavigationBar(" + theClass.name() + " " + theClass.name().toLowerCase() + ")");
		println("{");
		FileUtilities.incIndent();
			println("if(" + theClass.name().toLowerCase() + " != null)");
			println("{");
			FileUtilities.incIndent();
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					if(association.getSourceAE().cls() == theClass)
						println("number_objects = (TextView) rootView.findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + "_numberobjects);");
					else
						println("number_objects = (TextView) rootView.findViewById(R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + "_numberobjects);");
					
					if(association.getTargetAE().isCollection()){
						println("if(" + theClass.name().toLowerCase() + "." + association.getTargetAE().name() + "().isEmpty())");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + association.getTargetAE().name() + "View, false);");
							println("setNumberAssociation(number_objects, 0);");
						FileUtilities.decIndent();
						println("}");
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + association.getTargetAE().name() + "View, true);");
							println("setNumberAssociation(number_objects, clicked" + theClass.name() + "." + association.getTargetAE().name() + "().size());");
						FileUtilities.decIndent();
						println("}");
						println();
					}else{
						println("if(" + theClass.name().toLowerCase() + "." + association.getTargetAE().name() + "() == null)");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + association.getTargetAE().name() + "View, false);");
							println("setNumberAssociation(number_objects, 0);");
						FileUtilities.decIndent();
						println("}");
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + association.getTargetAE().name() + "View, true);");
							println("setNumberAssociation(number_objects, 1);");
						FileUtilities.decIndent();
						println("}");
						println();
					}
				}
				//inheritance navigations - start
//				if(isSubClass(theClass)){
//					not needed -> always 1
//				}
				if(isSuperClass(theClass)){
					for(MClass sub : theClass.children()){
						println("number_objects = (TextView) rootView.findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + sub.name().toLowerCase() + "_numberobjects);");
						println("if(" + sub.name() + ".allInstances" + "().isEmpty())");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + sub.name() + "View, true);");
							println("setNumberAssociation(number_objects, 0);");
						FileUtilities.decIndent();
						println("}");
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + sub.name() + "View, true);");
							println("setNumberAssociation(number_objects, " + sub.name() + ".allInstances().size());");
						FileUtilities.decIndent();
						println("}");
						println();
					}
				}
				//inheritance navigations - end
				
				println("objectValidation();");
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("rootView.setEnabled(false);");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_prepareView(MClass theClass) {
		println("public void prepareView(View view, boolean enable)");
		println("{");
		FileUtilities.incIndent();
			println("if(enable)");
			println("{");
			FileUtilities.incIndent();
				println("view.setClickable(true);");
				println("view.setAlpha((float) 1);");
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("view.setClickable(false);");
				println("view.setAlpha((float) 0.5);");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_setNumberAssociation(MClass theClass) {
		println("public void setNumberAssociation(TextView view, int numberObjects)");
		println("{");
		FileUtilities.incIndent();
			println("view.setText(\"( \" + numberObjects + \" )\");");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_objectValidation(MClass theClass) {
		println("public void objectValidation()");
		println("{");
		FileUtilities.incIndent();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				println("if(clicked" + theClass.name() + ".has" + capitalize(association.getTargetAE().name()) + "())");
				println("{");
				FileUtilities.incIndent();
					println(association.getTargetAE().name() + "View.setBackgroundResource(R.drawable.navigationbar_selector);");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println(association.getTargetAE().name() + "View.setBackgroundResource(R.drawable.navigationbar_selector_error);");
				FileUtilities.decIndent();
				println("}");
			}
			//inheritance navigations - start
//			if(isSubClass(theClass)){
//				not needed -> always 1
//			}
//			if(isSuperClass(theClass)){
//				for(MClass sub : theClass.children()){
//					not needed -> it is not mandatory to have subs
//				}
//			}
			//inheritance navigations - end
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_ScreenClickListeners(MClass theClass) {
		printNavigationBarFragment_ClickListener(theClass);
		printNavigationBarFragment_LongClickListener(theClass);
	}
	
	private void printNavigationBarFragment_ClickListener(MClass theClass){
		println("OnClickListener ClickListener = new OnClickListener()");
		println("{");
		FileUtilities.incIndent();
			println("@Override");
			println("public void onClick(View view)");
			println("{");
			FileUtilities.incIndent();
				println("if(view.isClickable())");
				println("{");
				FileUtilities.incIndent();
					println("if(clicked" + theClass.name() + " != null)");
					println("{");
					FileUtilities.incIndent();
						for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
							if(association.getSourceAE().cls() == theClass)
								println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ")");
							else
								println("if(view.getId() == R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ")");
							println("{");
							FileUtilities.incIndent();
								MMultiplicity targetMultiplicity = association.getTargetAE().multiplicity();
								String upperRange = targetMultiplicity.toString();
								if(targetMultiplicity.toString().contains(".."))
									upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
								if(upperRange.equals("*"))
									upperRange = "-1";
								if(association.getSourceAE().cls() == theClass)
									println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAE().cls().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + association.getSourceAE().name().toUpperCase() + "_" + association.getTargetAE().name().toUpperCase() + "Association\", " + upperRange + "));");
								else
									println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAE().cls().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Association\", " + upperRange + "));");
							FileUtilities.decIndent();
							println("}");
						}
						//inheritance navigations - start
						if(isSubClass(theClass)){
							println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + theClass.parents().iterator().next().name().toLowerCase() + ")");
							println("{");
							FileUtilities.incIndent();
								MMultiplicity targetMultiplicity = new MMultiplicity();
								targetMultiplicity.addRange(1, 1);
								String upperRange = targetMultiplicity.toString();
								if(targetMultiplicity.toString().contains(".."))
									upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
								if(upperRange.equals("*"))
									upperRange = "-1";
								println("UtilNavigate.toActivity(getActivity(), " + theClass.parents().iterator().next().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.parents().iterator().next().name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.parents().iterator().next().name().toUpperCase() + "Association\", " + upperRange + "));");
							FileUtilities.decIndent();
							println("}");
						}
						if(isSuperClass(theClass)){
							for(MClass sub : theClass.children()){
								println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + sub.name().toLowerCase() + ")");
								println("{");
								FileUtilities.incIndent();
									MMultiplicity targetMultiplicity = new MMultiplicity();
									targetMultiplicity.addRange(0, MMultiplicity.MANY);
									String upperRange = targetMultiplicity.toString();
									if(targetMultiplicity.toString().contains(".."))
										upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
									if(upperRange.equals("*"))
										upperRange = "-1";
									println("UtilNavigate.toActivity(getActivity(), " + sub.name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", " + upperRange + "));");
								FileUtilities.decIndent();
								println("}");
							}
						}
						//inheritance navigations - end
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("};");
		println();
	}
	
	private void printNavigationBarFragment_LongClickListener(MClass theClass){
		Set<AssociationInfo> isNeighborSuper = new HashSet<AssociationInfo>();
		println("OnLongClickListener LongClickListener = new OnLongClickListener()");
		println("{");
		FileUtilities.incIndent();
			println("@Override");
			println("public boolean onLongClick(View view)");
			println("{");
			FileUtilities.incIndent();
				println("if(view.isLongClickable())");
				println("{");
				FileUtilities.incIndent();
					println("if(clicked" + theClass.name() + " != null)");
					println("{");
					FileUtilities.incIndent();
						for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
							if(association.getSourceAE().cls() == theClass)
								println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ")");
							else
								println("if(view.getId() == R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + association.getTargetAE().name().toLowerCase() + ")");
							println("{");
							FileUtilities.incIndent();
							if(isSuperClass(association.getTargetAE().cls())){
								isNeighborSuper.add(association);
								println("setSuperClassOffSpringsListeners(UtilNavigate.showInheritanceList(getActivity(), R.layout." + theClass.name().toLowerCase() + "_generalizationoptions_" + association.getTargetAE().cls().name().toLowerCase() + "_view), \"" + association.getTargetAE().cls().name() + "\");");
							}else
								println("UtilNavigate.toActivityForResult(getActivity(), " + association.getTargetAE().cls().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, MasterActivity.CREATION_CODE);");
							
							FileUtilities.decIndent();
							println("}");
						}
						//inheritance navigations - start
						if(isSubClass(theClass)){
							println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + theClass.parents().iterator().next().name().toLowerCase() + ")");
							println("{");
							FileUtilities.incIndent();
								println("UtilNavigate.showWarning(getActivity(), \"Action - Association creation\", \"this action is not define since is a navigation to a super class\");");
							FileUtilities.decIndent();
							println("}");
						}
						if(isSuperClass(theClass)){
							for(MClass sub : theClass.children()){
								println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + sub.name().toLowerCase() + ")");
								println("{");
								FileUtilities.incIndent();
									println("UtilNavigate.showWarning(getActivity(), \"Action - Association creation\", \"this action is not define since is a navigation to a sub class\");");
								FileUtilities.decIndent();
								println("}");
							}
						}
						//inheritance navigations - end
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
				println("return false;");
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("};");
		println();
		
		if(!isNeighborSuper.isEmpty())
			printNavigationBarFragment_setSuperClassOffSpringsListeners(isNeighborSuper, theClass);
	}

	private void printNavigationBarFragment_setSuperClassOffSpringsListeners(Set<AssociationInfo> supers, MClass theClass){
		List<MClass> AllNotDirectNeighbors = new ArrayList<MClass>();
		List<MClass> supers_temp = new ArrayList<MClass>();
		List<MClass> alreadyAdded = new ArrayList<MClass>();
		boolean isRepeteadNeighbor = false;
		for(AssociationInfo x : supers){
			supers_temp.add(x.getTargetAE().cls());
			if(!alreadyAdded.contains(x.getSourceAE().cls()))
				alreadyAdded.add(x.getSourceAE().cls());
			else
				isRepeteadNeighbor = true;
		}
		
		AllNotDirectNeighbors.addAll(getAllSubClasses(supers_temp));
		
		
		
		
		println("public void setSuperClassOffSpringsListeners(View view, final String AssociationSource)");
		println("{");
		FileUtilities.incIndent();
			println("if(view != null)");
			println("{");
			FileUtilities.incIndent();
			for(AssociationInfo x : supers){
				if(!x.getTargetAE().cls().isAbstract()){
					println("if(view.findViewById(R.id." + theClass.name().toLowerCase() + "_generalizationoptions_" + x.getTargetAE().name().toLowerCase() + ") != null)");
					println("{");
					FileUtilities.incIndent();
						println("view.findViewById(R.id." + theClass.name().toLowerCase() + "_generalizationoptions_" + x.getTargetAE().name().toLowerCase() + ").setOnClickListener(new OnClickListener()");
						println("{");
						FileUtilities.incIndent();
							println("@Override");
							println("public void onClick(View v)");
							println("{");
							FileUtilities.incIndent();
								println("UtilNavigate.toActivityForResult(getActivity(), " + x.getTargetAE().cls().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, MasterActivity.CREATION_CODE);");
							FileUtilities.decIndent();
							println("}");
						FileUtilities.decIndent();
						println("});");
					println("}");
					FileUtilities.decIndent();
				}
			}
			
			for(MClass subs : AllNotDirectNeighbors){
				if(!subs.isAbstract()){
					println("if(view.findViewById(R.id." + subs.parents().iterator().next().name().toLowerCase() + "_generalizationoptions_" + subs.name().toLowerCase() + ") != null)");
					println("{");
					FileUtilities.incIndent();
						println("view.findViewById(R.id." + subs.parents().iterator().next().name().toLowerCase() + "_generalizationoptions_" + subs.name().toLowerCase() + ").setOnClickListener(new OnClickListener()");
						println("{");
						FileUtilities.incIndent();
							println("@Override");
							println("public void onClick(View v)");
							println("{");
							FileUtilities.incIndent();
							if(isRepeteadNeighbor)
								println("UtilNavigate.toActivityForResult(getActivity(), " + subs.name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setBundles(UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", -1),UtilNavigate.setAssociationBundleArguments(\"" + theClass.name().toUpperCase() + "END\", AssociationSource)), MasterActivity.CREATION_CODE);");
							else
								println("UtilNavigate.toActivityForResult(getActivity(), " + subs.name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", -1), MasterActivity.CREATION_CODE);");
							FileUtilities.decIndent();
							println("}");
						FileUtilities.decIndent();
						println("});");
					FileUtilities.decIndent();
					println("}");
				}
			}
			FileUtilities.decIndent();
			println("}");
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printNavigationBarFragment_BusinessListeners(MClass theClass) {
		println("@Override");
		println("public void propertyChange(final PropertyChangeEvent event)");
		println("{");
		FileUtilities.incIndent();
			println("getActivity().runOnUiThread(new Runnable()");
			println("{");
			FileUtilities.incIndent();
				println("@Override");
				println("public void run()");
				println("{");
				FileUtilities.incIndent();
					println("switch(event.getCommandType())");
					println("{");
					FileUtilities.incIndent();
						println("case INSERT:");
						FileUtilities.incIndent();
							println("setViewingObject((" + theClass.name() + ") event.getNewObject());");
						FileUtilities.decIndent();
						println("break;");
						
						println("case INSERT_ASSOCIATION:");
						FileUtilities.incIndent();
							println("setViewingObject((" + theClass.name() + ") event.getNewObject());");
						FileUtilities.decIndent();
						println("break;");
						
						println("case UPDATE:");
						FileUtilities.incIndent();
							println("//no need to change anything");
						FileUtilities.decIndent();
						println("break;");
						
						println("case DELETE:");
						FileUtilities.incIndent();
							println("if(clicked" + theClass.name() + "ID == event.getOldObjectID())");
							FileUtilities.incIndent();
								println("setViewingObject(null);");
							FileUtilities.decIndent();
						FileUtilities.decIndent();
						println("break;");
						
						println("case DELETE_ASSOCIATION:");
						FileUtilities.incIndent();
							println("if(clicked" + theClass.name() + "ID == event.getOldObjectID())");
							FileUtilities.incIndent();
								println("setViewingObject((" + theClass.name() + ") event.getNewObject());");
							FileUtilities.decIndent();
						FileUtilities.decIndent();
						println("break;");
						
						println("default:");
						FileUtilities.incIndent();
						
						FileUtilities.decIndent();
						println("break;");
					FileUtilities.decIndent();
					println("}");
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("});");
		FileUtilities.decIndent();
		println("}");
		println();
	}
//	******************** --- Fragment - Navigation Bar - End --- *****************************
	
	
	
	
	
	
	
	
	
	
	

	
	
	
//	******************** --- ListViewHolder - Start --- *****************************
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printListViewHolder_ClassHeader(MClass theClass, String layerName)
	{
		printFileHeader(theClass.name(), layerName);
		// visitAnnotations(e);

		printListViewHolder_Imports(theClass);

		print("public ");

		print("class " + theClass.name() + "ListViewHolder");
		
		print(" implements ListViewHolder, Serializable");
		println("{");
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printListViewHolder_Imports(MClass theClass)
	{
		println("import java.io.Serializable;");
		println("import " + basePackageName + ".R;");
		println("import " + basePackageName + "." + utilsLayerName + ".ListViewHolder;");
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		println();

		Set<String> classTypes = new HashSet<String>();

		for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
			classTypes.add(AndroidTypes.androidPrimitiveTypeToWidget(att.type()));

		classTypes.add("Activity");
		classTypes.add("View");
		classTypes.add("View.OnClickListener");
		classTypes.add("ImageView");
		
		Set<String> imports = AndroidTypes.androidImportDeclarations(classTypes);

		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printListViewHolder_Attributes(MClass theClass)
	{
		println("private transient ViewHolder holder;");
		println("private final int layout = R.layout." + theClass.name().toLowerCase() + "_view_list;");
		println("private final int contractError = R.id.default_list_invalid;");
		
		for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
			println("private final int " + att.name() + " = R.id." +  attributeBaseAncestor(theClass, att).name().toLowerCase() + "_list_" + att.name().toLowerCase() + "_value;");
		
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printListViewHolder_ViewHolderInnerClass(MClass theClass)
	{
		println("private class ViewHolder");
		println("{");
		FileUtilities.incIndent();

			println("public ImageView errorIcon;");
			
			for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
				println("public " + AndroidTypes.androidPrimitiveTypeToWidget(att.type()) + " " + att.name() + "View;");
	
			println();
			
			println("public ViewHolder(View convertView)");
			println("{");
			FileUtilities.incIndent();
				
				println("this.errorIcon = (ImageView) convertView.findViewById(contractError);");
				for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
					println("this." + att.name() + "View = (" + AndroidTypes.androidPrimitiveTypeToWidget(att.type()) + ") convertView.findViewById(" + att.name() + ");" );
			
			FileUtilities.decIndent();
			FileUtilities.println("}");
		
		FileUtilities.decIndent();
		FileUtilities.println("}");
		println();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printListViewHolder_RequiredMethods(MClass theClass)
	{
		printListViewHolder_setViewHolderContent(theClass);
		printListViewHolder_setNewViewHolder(theClass);
		printListViewHolder_getViewHolder(theClass);
	}
	
	private void printListViewHolder_getViewHolder(MClass theClass)
	{
		println("@Override");
		println("public int getViewHolder()");
		println("{");
		incIndent();
			println("return this.layout;");
		decIndent();
		println("}");
	}
	
	private void printListViewHolder_setViewHolderContent(MClass theClass)
	{
		println("@Override");
		println("public View setViewHolderContent(View convertView, Object object)");
		println("{");
		FileUtilities.incIndent();
			println("if(object != null)");
			println("{");
			FileUtilities.incIndent();
			
				println("holder = (ViewHolder) convertView.getTag();");
				for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues())){
					println("if(holder." + att.name() + "View != null)" );
					FileUtilities.incIndent();
						println("holder." + att.name() + "View." + AndroidTypes.androidWidgetContentSetter(att.type(),  "((" + theClass.name() + ") object)." + att.name() + "()") + ";" );
					FileUtilities.decIndent();
				}

				println("if(((" + theClass.name() + ") object).isAssociationRestrictionsValid())");
				FileUtilities.incIndent();
					println("holder.errorIcon.setVisibility(View.INVISIBLE);");
				FileUtilities.decIndent();
				println("else");
				FileUtilities.incIndent();
					println("holder.errorIcon.setVisibility(View.VISIBLE);");
				FileUtilities.decIndent();
				
				println("holder.errorIcon.setOnClickListener(new OnClickListener()");
				println("{");
				FileUtilities.incIndent();
					println("@Override");
					println("public void onClick(View v)");
					println("{");
					FileUtilities.incIndent();
						println("//TO DO");
					FileUtilities.decIndent();
					FileUtilities.println("}");
				FileUtilities.decIndent();
				FileUtilities.println("});");
				
			FileUtilities.decIndent();
			FileUtilities.println("}");
			
			println("return convertView;");
		
		FileUtilities.decIndent();
		FileUtilities.println("}");
		println();
	}
	
	private void printListViewHolder_setNewViewHolder(MClass theClass)
	{
		println("@Override");
		println("public View setNewViewHolder(Activity context, View convertView)");
		println("{");
		FileUtilities.incIndent();

			println("convertView = context.getLayoutInflater().inflate(layout, null);");
			println("holder = new ViewHolder(convertView);");
			println("convertView.setTag(holder);");
			println("return convertView;");
		
		FileUtilities.decIndent();
		FileUtilities.println("}");
		println();
	}
	
	
//	******************** --- ListViewHolder - end --- *****************************

}
