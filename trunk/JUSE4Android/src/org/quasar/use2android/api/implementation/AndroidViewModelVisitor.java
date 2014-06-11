package org.quasar.use2android.api.implementation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import org.quasar.use2android.api.AndroidWidgetPreference;
import org.quasar.use2android.api.AndroidWidgetsTypes;
import org.quasar.use2android.api.JavaInput;

import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAssociationClass;
import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MMultiplicity;
import org.tzi.use.uml.ocl.type.Type;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

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
		if(association.getKind() == AssociationKind.MANY2MANY || ((association.getKind() == AssociationKind.ONE2MANY
				|| association.getKind() == AssociationKind.MEMBER2MEMBER || association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER) && end.isCollection()))
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
		println("import android.support.v4.app.FragmentActivity;");
		println("import android.content.Intent;");
		println("import android.graphics.drawable.AnimationDrawable;");
		println("import android.os.Bundle;");
		println("import android.view.Menu;");
		println("import android.view.MenuInflater;");
		println("import android.view.MenuItem;");
		println("import android.widget.ImageView;");
		println();
		println("public class MasterActivity extends FragmentActivity");
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
		    		println("{");
					FileUtilities.incIndent();
						println("ONCREATION = true;");
						println("getActionBar().setIcon(R.drawable.ic_android_mode_write);");
					FileUtilities.decIndent();
					println("}");
					println("else");
					FileUtilities.incIndent();
						println("getActionBar().setIcon(R.drawable.ic_android_mode_read);");
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
					println("if(menu.size() == 0)");
					println("{");
					FileUtilities.incIndent();
						println("if(ONCREATION)");
						FileUtilities.incIndent();
							println("menuInflater.inflate(R.menu.menu_write, menu);");
						FileUtilities.decIndent();
						println("else");
						FileUtilities.incIndent();
							println("menuInflater.inflate(R.menu.menu_read, menu);");
						FileUtilities.decIndent();
					println("}");
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
						println("ServerActions.sendChanges(this);");
						println("break;");
					FileUtilities.decIndent();
					println("case R.id.download:");
					FileUtilities.incIndent();
						println("ServerActions.getChanges(this);");
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

//		comando para TESTE - START
		println("import " + basePackageName + "." + utilsLayerName + ".Command;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandTargetLayer;");
//		comando para TESTE - END	
		
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
//		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".ListFragmentController;");
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".ListAdapter;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		if(ModelUtilities.hasAssociations(theClass))
			println("import " + basePackageName + "." + utilsLayerName + ".NavigationBarFragment;");
		println("import " + basePackageName + "." + utilsLayerName + ".DetailFragment;");
//		println("import " + basePackageName + "." + utilsLayerName + ".Utils;");
					
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		
		List<MClass> alreadyAdded = new ArrayList<MClass>();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			if(!alreadyAdded.contains(association.getTargetAEClass())){
				println("import " + basePackageName + "." + businessLayerName + "." + association.getTargetAEClass().name() + ";");
				alreadyAdded.add(association.getTargetAEClass());
				if(ModelUtilities.isAssociativeClass(theClass) && association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
					println("import " + basePackageName + "." + presentationLayerName + "." + association.getTargetAEClass().name() + "." + association.getTargetAEClass().name() + "Activity;");
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
			String type = AndroidTypes.androidPrimitiveTypeToReadWidget(att.type(), AndroidWidgetPreference.NORMAL);
			androidClassTypes.add(type);
			if(!type.equals(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL)))
				androidClassTypes.add(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL));
			
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
		println("private String AssociationEndMultiplicityKey = \"AssociationEndMultiplicityKey\";");
		println("private int AssociationEndMultiplicity;");
		println("private String AssociationEndNameKey = \"AssociationEndNamekey\";");
		println("private String AssociationEndName;");
		println();
		if(ModelUtilities.hasAssociations(theClass))
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
//		Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			if(!alreadyAdded.contains(association.getTargetAEClass())){
				println("private " + association.getTargetAEClass().name() + " " + association.getTargetAEClass().name().toLowerCase() + ";");
				println("private final String " + association.getTargetAEClass().name().toUpperCase() + "Object = \"" + association.getTargetAEClass().name().toUpperCase() + "Object\";");
				alreadyAdded.add(association.getTargetAEClass());
//				RepeteadNeighbors.put(association.getTargetAEClass(), 1);
			}
//				RepeteadNeighbors.put(association.getTargetAEClass(), RepeteadNeighbors.get(association.getTargetAEClass()) + 1);
//			println("private final String " + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association = \"" + association.getTargetAE().name().toUpperCase() +  "_" + association.getSourceAE().name().toUpperCase() + "Association\";");
//			if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
//				println("private final String " + association.getTargetAEClass().name() + "Association = \"" + association.getTargetAEClass().name() + "Association\";");
//			else
			println("private final String " + association.getName() + "Association = \"" + association.getName() + "Association\";");
		}
		if(ModelUtilities.isAssociativeClass(theClass))
			println("private final String " + theClass.name() + "Association = \"" + theClass.name() + "Association\";");
		
		
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
//			for(MClass repeteadNeighbor : RepeteadNeighbors.keySet())
//				if(RepeteadNeighbors.get(repeteadNeighbor) > 1){
//					println("private String " + repeteadNeighbor.name() + "End;");
//					println("private final String " + repeteadNeighbor.name().toUpperCase() + "END = \"" + repeteadNeighbor.name().toUpperCase() + "\";");
//				}
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
			println("if(" + theClass.name().toLowerCase() + " != null)");
			FileUtilities.incIndent();
				println("UtilNavigate.replaceFragment(this, detail_fragment, R.id." + theClass.name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(View," + theClass.name().toLowerCase() + ".ID()));");
			FileUtilities.decIndent();
			println("else");
			FileUtilities.incIndent();
				println("UtilNavigate.replaceFragment(this, detail_fragment, R.id." + theClass.name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(View,0));");
			FileUtilities.decIndent();
			println();
		println("if(!mTwoPane && AssociationEndMultiplicity != ToONE)");
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
//				println("if((!showingDetail || detail_fragment == null) && clicked" + theClass.name() + " != null)");
//				println("{");
//				FileUtilities.incIndent();
//					println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL, clicked" + theClass.name() + ");");
//				FileUtilities.decIndent();
//				println("}");
//				println("showingDetail = true;");
//			FileUtilities.decIndent();
//			println("}");
			println("if(!mTwoPane && AssociationEndMultiplicity != ToONE)");
			println("{");
				FileUtilities.incIndent();
				println("if(detail_fragment != null)");
				println("{");
				FileUtilities.incIndent();
					println("if(showingDetail)");
					println("{");
					FileUtilities.incIndent();
						println("list_fragment.hide();");
						println("((DetailFragment) detail_fragment).show();");
					FileUtilities.decIndent();
					println("}");
					println("else");
					println("{");
					FileUtilities.incIndent();
						println("list_fragment.show();");
						println("((DetailFragment) detail_fragment).hide();");
					FileUtilities.decIndent();
					println("}");
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
			println("if(AssociationEndMultiplicity != ToONE)");
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
			
//				List<MClass> alreadyAdded = new ArrayList<MClass>();
//				Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
//				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
//					if(!alreadyAdded.contains(association.getTargetAEClass())){
//						RepeteadNeighbors.put(association.getTargetAEClass(), 1);
//						alreadyAdded.add(association.getTargetAEClass());
//					}
//					else
//						RepeteadNeighbors.put(association.getTargetAEClass(), RepeteadNeighbors.get(association.getTargetAEClass()) + 1);
//				}
				
//				alreadyAdded.clear();
				boolean hasAssocaitions = false;
				boolean isFirts = true;//novo
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					hasAssocaitions = true;
//					if(!alreadyAdded.contains(association.getTargetAEClass())){
						String id = association.getName() + "Association";
//						if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
//							id = association.getTargetAEClass().name() + "Association";
//						novo
						if(isFirts){
							println("if(extras().containsKey(" + id + ")");
//							println("if(extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object)");
							isFirts = false;
						}else
							println(" || extras().containsKey(" + id + ")");
//							println(" || extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object)");
					
//						println("if(extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object))");
//						println("{");
//						FileUtilities.incIndent();
//							if(is2Many(association, association.getSourceAE())){//source(theClass) -> 2MANY
//								println(association.getTargetAEClass().name().toLowerCase() + " = " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer)extras().getInt(" + association.getTargetAEClass().name().toUpperCase() + "Object));");
//								println("if(ONCREATION)");
//								FileUtilities.incIndent();
//									println("AssociationEndMultiplicity = ToMANY;");
//								FileUtilities.decIndent();
//								println("else ");
//								FileUtilities.incIndent();
//									println("AssociationEndMultiplicity = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
//								FileUtilities.decIndent();
//							}else{//source(theClass) -> 2ONE
//								println(association.getTargetAEClass().name().toLowerCase() + " = " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer)extras().getInt(" + association.getTargetAEClass().name().toUpperCase() + "Object));");
//								println("if(ONCREATION)");
//								FileUtilities.incIndent();
//									println("AssociationEndMultiplicity = ToMANY;");
//								FileUtilities.decIndent();
//								println("else ");
//								FileUtilities.incIndent();
//									println("AssociationEndMultiplicity = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
//								FileUtilities.decIndent();
//							}
//							if(isSubClass(theClass))
//								if(RepeteadNeighbors.get(association.getTargetAEClass()) > 1)
//									println(association.getTargetAEClass().name() + "End = extras().getString(" + association.getTargetAEClass().name().toUpperCase() + "END);");
//						FileUtilities.decIndent();
//						println("}");
//						if(association.getKind() != AssociationKind.MEMBER2ASSOCIATIVE)
//							alreadyAdded.add(association.getTargetAEClass());
//					}
				}
				if(isSuperClass(theClass)){//navegacao sub -> super (ToONE)
//					for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
//						if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super
							println(" || extras().containsKey(" + theClass.name().toUpperCase() + "Association)");
//							println(" || extras().containsKey(" + theClass.name().toUpperCase() + "Object)");//novo
//							println("if(extras().containsKey(" + theClass.name().toUpperCase() + "Object))");
//							println("{");
//						
//							FileUtilities.incIndent();
//								println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "((Integer)extras().getInt(" + theClass.name().toUpperCase() + "Object));");
//								println("AssociationEndMultiplicity = extras().getInt(" + theClass.name().toUpperCase() + "Association);");
//							FileUtilities.decIndent();
//							println("}");
//							alreadyAdded.add(theClass);
//						}
//					}
				}
				if(isSubClass(theClass)){//navegacao super -> sub (ToMany)
//					if(!alreadyAdded.contains(theClass.parents().iterator().next())){
						println(" || extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Association)");
//						println(" || extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Object)");//novo
//						println("if(extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Object))");
//						println("{");
//						FileUtilities.incIndent();
//							println(theClass.parents().iterator().next().name().toLowerCase() + " = " + theClass.parents().iterator().next().name() + ".get" + theClass.parents().iterator().next().name() + "((Integer)extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Object));");
//							println("AssociationEndMultiplicity = extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Association);");
//						FileUtilities.decIndent();
//						println("}");
//					}
				}
				if(ModelUtilities.isAssociativeClass(theClass))
					println(" || extras().containsKey(" + theClass.name() + "Association)");
				//novo - start
				if(hasAssocaitions){
					println(")");
					println("{");
				}
				FileUtilities.incIndent();
					println("if(ONCREATION)");
					FileUtilities.incIndent();
						println("AssociationEndMultiplicity = ToMANY;");
					FileUtilities.decIndent();
					println("else ");
					FileUtilities.incIndent();
						println("AssociationEndMultiplicity = extras().getInt(AssociationEndMultiplicityKey);");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
				if(hasAssocaitions)
					println("}");
				//novo - end
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("//camed from launcher therefore AssociationEndMultiplicity = -1 (*) -> allInstances");
				println("AssociationEndMultiplicity = ToMANY;");
			FileUtilities.decIndent();
			println("}");
			
			println();
			println("if (getResources().getBoolean(R.bool.has_two_panes) && AssociationEndMultiplicity != ToONE)");
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
			println("FragmentTransaction ft = getSupportFragmentManager().beginTransaction();");
			println("if(savedInstanceState == null)");
			println("{");
			FileUtilities.incIndent();
			println("if (AssociationEndMultiplicity != ToONE)");
			println("{");
			FileUtilities.incIndent();
				println("list_fragment = new ListFragmentController();");
				println("ft.add(R.id."+ theClass.name().toLowerCase() + "_list_container, list_fragment);");
			FileUtilities.decIndent();
			println("}");
			if(ModelUtilities.hasAssociations(theClass)){
				println("navigation_bar = new " + theClass.name() + "NavigationBarFragment();");
				println("ft.add(R.id."+ theClass.name().toLowerCase() + "_navigationbar_container, navigation_bar);");
			}
				println("ft.commit();");
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("if (AssociationEndMultiplicity != ToONE)");
				FileUtilities.incIndent();
					println("list_fragment = (ListFragmentController) getSupportFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_list_container);");
				FileUtilities.decIndent();
				if(ModelUtilities.hasAssociations(theClass))
					println("navigation_bar = (Fragment) getSupportFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_navigationbar_container);");
				println("detail_fragment = (Fragment) getSupportFragmentManager().findFragmentById(R.id."+ theClass.name().toLowerCase() + "_detail_container);");
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
			
//				alreadyAdded = new ArrayList<MClass>();
//				RepeteadNeighbors = new HashMap<MClass, Integer>();
//				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
//					if(!alreadyAdded.contains(association.getTargetAEClass())){
//						RepeteadNeighbors.put(association.getTargetAEClass(), 1);
//						alreadyAdded.add(association.getTargetAEClass());
//					}
//					else
//						RepeteadNeighbors.put(association.getTargetAEClass(), RepeteadNeighbors.get(association.getTargetAEClass()) + 1);
//				}
				
//				alreadyAdded.clear();
//				boolean firts = true;
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
//					if(!alreadyAdded.contains(association.getTargetAEClass())){
						String id = association.getName() + "Association";
//						if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
//							id = association.getTargetAEClass().name() + "Association";
//						if(!firts)
//							print("else ");
						
						println("if(extras().containsKey(" + id + "))");
//						println("if(extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object))");
						println("{");
						
						FileUtilities.incIndent();
							if(is2Many(association, association.getSourceAE())){//source(theClass) -> 2MANY

									println(association.getTargetAEClass().name().toLowerCase() + " = " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer)extras().getInt(" + association.getTargetAEClass().name().toUpperCase() + "Object));");
								println("AssociationEndName = extras().getString(" + id + ");");//novo
								println("if(ONCREATION)");
//								println("{");
								FileUtilities.incIndent();
									println("startActivity_ToMANY(null, extras());");
//									println("AssociationEndMultiplicity = ToMANY;");
								FileUtilities.decIndent();
//								println("}");
								println("else ");
//								println("{");
								FileUtilities.incIndent();
//									println("AssociationEndMultiplicity = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
									//CUIDADO NA PROX LINHA
								if(ModelUtilities.isAssociativeClass(theClass) && association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)	
									println("startActivity_ToMANY((" + "Set<" + theClass.name() + ">) " + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAEClass().nameAsRolename() + "(), extras());");
								else
									println("startActivity_ToMANY((" + JavaTypes.javaInterfaceType(association.getSourceAE().getType()) + ") " + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAE().name() + "(), extras());");

								FileUtilities.decIndent();
//								println("}");
								println(association.getTargetAEClass().name() + ".getAccess().setChangeListener(this);");
							}else{//source(theClass) -> 2ONE
								println(association.getTargetAEClass().name().toLowerCase() + " = " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer)extras().getInt(" + association.getTargetAEClass().name().toUpperCase() + "Object));");
								println("AssociationEndName = extras().getString(" + id + ");");//novo
								println("if(ONCREATION)");
//								println("{");
								FileUtilities.incIndent();
									println("startActivity_ToMANY(null, extras());");
//									println("AssociationEndMultiplicity = ToMANY;");
								FileUtilities.decIndent();
//								println("}");
								println("else ");
								println("{");
								FileUtilities.incIndent();
									
//									println("AssociationEndMultiplicity = extras().getInt(" + association.getTargetAE().name().toUpperCase() + "_" + association.getSourceAE().name().toUpperCase() + "Association);");
									println("mTwoPane = false;");
								
//									if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
//										println("clicked" + theClass.name() + " = (" + theClass.name() + ")" + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAE().name().toLowerCase() + "();");
//									else if(association.getSourceAE().cls() == theClass)
									if(ModelUtilities.isAssociativeClass(theClass) && association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
										println("clicked" + theClass.name() + " = " + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAEClass().name().toLowerCase() + "();");
									else//para as hierarquias, fazemos cast da subClass
										println("clicked" + theClass.name() + " = (" + theClass.name() + ")" + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAE().name() + "();");
									println("startActivity_ToONE(clicked" + theClass.name() + ");");
								FileUtilities.decIndent();
								println("}");
//								println(association.getTargetAEClass().name() + ".getAccess().setChangeListener(this);");
							}
//							proximas 3 linhas sao antigas
//							if(isSubClass(theClass))
//								if(RepeteadNeighbors.get(association.getTargetAEClass()) > 1)
//									println(association.getTargetAEClass().name() + "End = extras().getString(" + association.getTargetAEClass().name().toUpperCase() + "END);");
						FileUtilities.decIndent();
						println("}");
//						alreadyAdded.add(association.getTargetAEClass());
//						firts = false;
//					}
				}
				if(isSuperClass(theClass)){//navegacao sub -> super (ToONE)
//					for(MClass x : getAllSubClasses(Arrays.asList(theClass))){
//						if(x.parents().iterator().next() == theClass && !alreadyAdded.contains(theClass)){//if is direct super
//							if(!firts)
//								print("else ");
							
							println("if(extras().containsKey(" + theClass.name().toUpperCase() + "Association))");
//							println("if(extras().containsKey(" + theClass.name().toUpperCase() + "Object))");
							println("{");
						
							FileUtilities.incIndent();
								println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "((Integer)extras().getInt(" + theClass.name().toUpperCase() + "Object));");
								println("AssociationEndName = extras().getString(" + theClass.name().toUpperCase() + "Association);");//novo
//								println("AssociationEndMultiplicity = extras().getInt(" + theClass.name().toUpperCase() + "Association);");
								println("mTwoPane = false;");
									
//								if(x.parents().iterator().next() == theClass)
									println("clicked" + theClass.name() + " = " + theClass.name().toLowerCase() + ";");
//								else//para as hierarquias, fazemos cast da subClass
//									println("clicked" + theClass.name() + " = (" + theClass.name() + ")" + theClass.name().toLowerCase() + ";");
								println("startActivity_ToONE(clicked" + theClass.name() + ");");
								println(theClass.name() + ".getAccess().setChangeListener(this);");
							
							FileUtilities.decIndent();
							println("}");
//							alreadyAdded.add(theClass);
//						}
//					}
				}
				if(isSubClass(theClass)){//navegacao super -> sub (ToMany)
//					if(!alreadyAdded.contains(theClass.parents().iterator().next())){
//						if(!firts)
//							print("else ");
					
						println("if(extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Association))");
//						println("if(extras().containsKey(" + theClass.parents().iterator().next().name().toUpperCase() + "Object))");
						println("{");
					
						FileUtilities.incIndent();
							println(theClass.parents().iterator().next().name().toLowerCase() + " = " + theClass.parents().iterator().next().name() + ".get" + theClass.parents().iterator().next().name() + "((Integer)extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Object));");
							println("AssociationEndName = extras().getString(" + theClass.parents().iterator().next().name().toUpperCase() + "Association);");//novo
//							println("AssociationEndMultiplicity = extras().getInt(" + theClass.parents().iterator().next().name().toUpperCase() + "Association);");
//							CUIDADO NA PROX LINHA
							println("startActivity_ToMANY(null" + ", extras());");
//							println(theClass.parents().iterator().next().name() + ".getAccess().setChangeListener(this);");
						FileUtilities.decIndent();
						println("}");
//					}
				}
				//writes the MEMBERTOMEMBER association in the associative class
				if(ModelUtilities.isAssociativeClass(theClass)){
					println("if(extras().containsKey(" + theClass.name() + "Association))");
					println("{");
					FileUtilities.incIndent();
						for (AssociationInfo association : AssociationInfo.getAssociationsInfo(theClass)){
							if(association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
								println("if(extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object))");
								FileUtilities.incIndent();
									println(association.getTargetAEClass().name().toLowerCase() + " = " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer)extras().getInt(" + association.getTargetAEClass().name().toUpperCase() + "Object));");
								FileUtilities.decIndent();
							}
						}
						println("AssociationEndName = extras().getString(" + theClass.name() + "Association);");//novo
						println("if(ONCREATION)");
						println("{");
						FileUtilities.incIndent();
							println("startActivity_ToMANY(null, extras());");
							for (AssociationInfo association : AssociationInfo.getAssociationsInfo(theClass)){
								if(association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
									println("if(extras().containsKey(" + association.getTargetAEClass().name().toUpperCase() + "Object))");
									FileUtilities.incIndent();
										println("UtilNavigate.showWarning(this, \"Action - Association between associative members\", \"You are trying to associate a " + association.getTargetAEClass().name() + " to a " + ModelUtilities.getOtherMember(association.getAssociationClass(), association.getTargetAEClass()).name() + "\\nBefore you can complete this action (association to " + ModelUtilities.getOtherMember(association.getAssociationClass(), association.getTargetAEClass()).name() + ") you must firts select or create a " + association.getAssociationClass().name() + " in order to make of the " + ModelUtilities.getOtherMember(association.getAssociationClass(), association.getTargetAEClass()).name() + " a " + ModelUtilities.getOtherMemberAssociation(association.getAssociationClass(), association.getTargetAEClass()).nameAsRolename() + " in the " + association.getTargetAEClass().name() + "\");");
									FileUtilities.decIndent();
								}
							}
						FileUtilities.decIndent();
						println("}");
					FileUtilities.decIndent();
					println("}");
				}
				
//				if(!firts){
//					println("else ");
//					println("{");
//					FileUtilities.incIndent();
//						println("startActivity_ToMANY(null, extras());");
//						println("AssociationEndMultiplicity = ToMANY;");
//					FileUtilities.decIndent();
//					println("}");
//				}
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("//camed from launcher therefore AssociationEndMultiplicity = -1 (*) -> allInstances");
					println("startActivity_ToMANY(null, extras());");
					println("AssociationEndMultiplicity = ToMANY;");
				FileUtilities.decIndent();
			println("}");
			println();
			println("if(AssociationEndMultiplicity != ToONE)");
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
			println("if(AssociationEndMultiplicity != ToONE)");
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
//			println("if(AssociationEndMultiplicity != ToONE)");
//			println("{");
//			FileUtilities.incIndent();
//				
//			FileUtilities.decIndent();
//			println("}");
//			println("if(AssociationEndMultiplicity != ToONE)");
//			println("{");
//			FileUtilities.incIndent();
//				println("orientationSettings(clicked" + theClass.name() + ");");
//				println("((NavigationBarFragment)navigation_bar).setViewingObject(clicked" + theClass.name() + ");");
			FileUtilities.decIndent();
			println("}");
			if(ModelUtilities.hasAssociations(theClass))
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
//			comando para TESTE - START
			println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "Activity.class, CommandType.READ, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//			comando para TESTE - END	
			println("if(ONCREATION)");
			FileUtilities.incIndent();
				println("Transactions.StartTransaction();");
			FileUtilities.decIndent();
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
			println("if(AssociationEndMultiplicity != ToONE && !mTwoPane && detail_fragment != null && showingDetail)");
			println("{");
			FileUtilities.incIndent();
				println("((DetailFragment) detail_fragment).hide();");
				if(ModelUtilities.hasAssociations(theClass))
					println("((NavigationBarFragment) navigation_bar).show();");
				println("list_fragment.show();");
				println("showingDetail = false;");
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				println("setResult(Activity.RESULT_CANCELED);");
//				comando para TESTE - START
				println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "Activity.class, CommandType.BACK, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//				comando para TESTE - END	
				println("super.onBackPressed();");
			FileUtilities.decIndent();
			println("}");
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
				if(ModelUtilities.hasAssociations(theClass))
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
				println("menu.findItem(R.id.menu_new).setVisible(false);");
				println("menu.findItem(R.id.menu_edit).setEnabled(false);");
				println("menu.findItem(R.id.menu_edit).setVisible(false);");
				println("menu.findItem(R.id.menu_delete).setEnabled(false);");
				println("menu.findItem(R.id.menu_delete).setVisible(false);");
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
						println("if(list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
						println("{");
						FileUtilities.incIndent();
							setActivityResult(theClass);
						FileUtilities.decIndent();
						println("}");
						println("else");
						FileUtilities.incIndent();
							println("UtilNavigate.showWarning(this, \"Wrong action\", \"Error - In order to confirm a " + theClass.name() + " you must have a " + theClass.name() + " selected\\nSolution - In order to finish the association action\\n\\t1 - select a " + theClass.name() + "\\n\\t2 - if no " + theClass.name() + " is available create a new one by means of the plus icon\");");
						FileUtilities.decIndent();
					FileUtilities.decIndent();
					println("}");
					println("break;");
				FileUtilities.decIndent();
				if(!theClass.isAbstract()){
					println("case R.id.menu_edit:");
					FileUtilities.incIndent();
						println("if (AssociationEndMultiplicity == ToONE || list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
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
						println("if(clicked" + theClass.name() + " != null)");
						println("{");
						FileUtilities.incIndent();
							println("if(Transactions.StartTransaction())");
							println("{");
							FileUtilities.incIndent();
								println("if (AssociationEndMultiplicity == ToMANY && list_fragment.getSelectedPosition() != ListView.INVALID_POSITION)");
								println("{");
								FileUtilities.incIndent();
									println("clicked" + theClass.name() + ".delete();");
									println("list_fragment.show();");
		//							println("list_fragment.setActivatedPosition(ListView.INVALID_POSITION);");
		//							println("list_fragment.setSelection(ListView.INVALID_POSITION);");
		//							println("getSupportFragmentManager().beginTransaction().detach(detail_fragment).commit();");
								FileUtilities.decIndent();
								println("}");
								println("else");
								println("{");
								FileUtilities.incIndent();
									println("clicked" + theClass.name() + ".delete();");
									println("finish();");
								FileUtilities.decIndent();
								println("}");
								println("clicked" + theClass.name() + " = null;");
								println("if(!Transactions.StopTransaction())");
								FileUtilities.incIndent();
									println("Transactions.ShowErrorMessage(this);");
								FileUtilities.decIndent();
							FileUtilities.decIndent();
							println("}");
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
//		if(isSubClass(theClass)){
			List<MClass> alreadyAdded = new ArrayList<MClass>();
			Map<MClass, Integer> RepeteadNeighbors = new HashMap<MClass, Integer>();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				if(!alreadyAdded.contains(association.getTargetAEClass())){
					RepeteadNeighbors.put(association.getTargetAEClass(), 1);
					alreadyAdded.add(association.getTargetAEClass());
				}
				else
					RepeteadNeighbors.put(association.getTargetAEClass(), RepeteadNeighbors.get(association.getTargetAEClass()) + 1);
			}
			
			alreadyAdded.clear();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				if(!alreadyAdded.contains(association.getTargetAEClass())){
					println("if(" + association.getTargetAEClass().name().toLowerCase() + " != null)");
					println("{");
					FileUtilities.incIndent();
					if(RepeteadNeighbors.containsKey(association.getTargetAEClass()) && RepeteadNeighbors.get(association.getTargetAEClass()).intValue() > 1){
						for(AssociationInfo x : AssociationInfo.getAssociationsInfo(association.getTargetAEClass()))
							if(theClass.isSubClassOf(x.getTargetAEClass())){
								println("if(AssociationEndName.equals(" + x.getName() + "Association))");
//								println("if(" + association.getTargetAEClass().name() + "End.equals(\"" + x.getTargetAEClass() + "\"))");
								println("{");
								FileUtilities.incIndent();
									println("setResult(Activity.RESULT_OK, new Intent().putExtra(" + x.getName() + "Association, clicked" + theClass.name() + ".ID()));");
									println("finish();");
								FileUtilities.decIndent();
								println("}");
							}
					}else
						if(ModelUtilities.isAssociativeClass(theClass) && association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
							println("if(extras().containsKey(" + theClass.name() + "Association))");
							println("{");
							FileUtilities.incIndent();
								println("UtilNavigate.toActivityForResult(this, " + ModelUtilities.getOtherMember((MAssociationClass) theClass, association.getTargetAEClass()).name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", -1, \"" + ModelUtilities.getOtherMemberAssociation((MAssociationClass) theClass, association.getTargetAEClass()).name() + "_" + theClass.name() + "Association\", \"" + ModelUtilities.getOtherMemberAssociation((MAssociationClass) theClass, association.getTargetAEClass()).name() + "_" + theClass.name() + "Association\"), MasterActivity.CREATION_CODE);");
							FileUtilities.decIndent();
							println("}");
							println("else");
							println("{");
							FileUtilities.incIndent();
								println("setResult(Activity.RESULT_OK, new Intent().putExtra(" + association.getName() + "Association, clicked" + theClass.name() + ".ID()));");
								println("finish();");
							FileUtilities.decIndent();
							println("}");
						}else{
							println("setResult(Activity.RESULT_OK, new Intent().putExtra(" + association.getName() + "Association, clicked" + theClass.name() + ".ID()));");
							println("finish();");
						}
//						println("setResult(Activity.RESULT_OK, new Intent().putExtra(\"" + association.getSourceAE().cls().name() + "\", clicked" + theClass.name() + ".ID()));");

					FileUtilities.decIndent();
					println("}");
					alreadyAdded.add(association.getTargetAEClass());
				}
			}
//		}else
//			println("setResult(Activity.RESULT_OK, new Intent().putExtra(\"" + theClass.name() + "\", clicked" + theClass.name() + ".ID()));");
	}
	
	@Override
	public void printActivity_onActivityResult(MClass theClass){
		println("@Override");
		println("public void onActivityResult(int requestCode, int resultCode, Intent data)");
		println("{");
		FileUtilities.incIndent();
		for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
			println("if (requestCode == CREATION_CODE && resultCode == Activity.RESULT_OK && data.getExtras().containsKey(" + association.getName() + "Association))");
			println("{");
			FileUtilities.incIndent();
				println("clicked" + theClass.name() + ".insertAssociation((" + association.getTargetAEClass().name() + ") " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer) data.getExtras().get(" + association.getName() + "Association))" +
						", " + association.getName() + "Association);");
				if(ModelUtilities.isAssociativeClass(theClass) && association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
					for (AssociationInfo otherMember : AssociationInfo.getAllAssociationsInfo(theClass))
						if(otherMember.getKind() == AssociationKind.ASSOCIATIVE2MEMBER && otherMember.getTargetAEClass() != association.getTargetAEClass()){
							println("if(" + otherMember.getTargetAEClass().name().toLowerCase() + " != null){");
							FileUtilities.incIndent();
								println("setResult(Activity.RESULT_OK, new Intent().putExtra(" + otherMember.getName() + "Association, clicked" + theClass.name() + ".ID()));");
								println("finish();");
							FileUtilities.decIndent();
							println("}else");
							FileUtilities.incIndent();
								println("if(!Transactions.StopTransaction())");
								FileUtilities.incIndent();
									println("Transactions.ShowErrorMessage(this);");
								FileUtilities.decIndent();
							FileUtilities.decIndent();
						}
				}else{
					println("if(!Transactions.StopTransaction())");
					FileUtilities.incIndent();
						println("Transactions.ShowErrorMessage(this);");
					FileUtilities.decIndent();
				}
			FileUtilities.decIndent();
			println("}");
//			println("if (requestCode == CREATION_CODE && resultCode == Activity.RESULT_OK && data.getExtras().containsKey(\"" + association.getTargetAEClass().name() + "\"))");
//			println("{");
//			FileUtilities.incIndent();
//				println("clicked" + theClass.name() + ".insertAssociation((" + association.getTargetAEClass().name() + ") " + association.getTargetAEClass().name() + ".get" + association.getTargetAEClass().name() + "((Integer) data.getExtras().get(\"" + association.getTargetAEClass().name() + "\")));");
//				println("Transactions.StopTransaction();");
//			FileUtilities.decIndent();
//			println("}");
		}
			println("if (requestCode == CREATION_CODE && resultCode == Activity.RESULT_CANCELED)");
			println("{");
			FileUtilities.incIndent();
				println("if (!ONCREATION)");
				println("{");
				FileUtilities.incIndent();
					println("if(!Transactions.StopTransaction())");
					FileUtilities.incIndent();
						println("Transactions.ShowErrorMessage(this);");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("}");
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
//						DEPOIS VOLTAR A POR
						for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
							println("if(" + association.getTargetAEClass().name().toLowerCase() + " != null && AssociationEndName.equals(" + association.getName() + "Association) && !Transactions.isCanceled())");
							if(association.getKind() == AssociationKind.MEMBER2MEMBER){
								println("{");
								FileUtilities.incIndent();
								List<AttributeInfo> attributes = AttributeInfo.getAttributesInfo(ModelUtilities.getAssociativeClass(association.getTargetAEClass(), association.getSourceAEClass()));
								List<String> orderedMembers = new ArrayList<String>();
								for (int i = 0; i < attributes.size(); i++)
									if (attributes.get(i).getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
										if(attributes.get(i).getType().toString().equals(association.getSourceAEClass().name()))
											orderedMembers.add(theClass.name());
										else
											orderedMembers.add(association.getTargetAEClass().name().toLowerCase());
											

								if(orderedMembers.get(0).equals(theClass.name())){
									orderedMembers.set(0, "new" + orderedMembers.get(0));
									orderedMembers.set(1, orderedMembers.get(1).toLowerCase());
								}else{
									orderedMembers.set(1, "new" + orderedMembers.get(1));
									orderedMembers.set(0, orderedMembers.get(0).toLowerCase());
								}
									println("new " + ModelUtilities.getAssociativeClass(association.getTargetAEClass(), association.getSourceAEClass()).name() + "(" + orderedMembers.get(0) + ", " + orderedMembers.get(1) + ").insert();");
									println("list_fragment.add(new" + theClass.name() + ");");
								FileUtilities.decIndent();
								println("}");
							}else{
								println("{");
								FileUtilities.incIndent();
									println("" + association.getTargetAEClass().name().toLowerCase() + ".insertAssociation(new" + theClass.name() + ", " + association.getName() + "Association);");
									println("list_fragment.add(new" + theClass.name() + ");");
								FileUtilities.decIndent();
								println("}");
							}
//							println("if(" + association.getTargetAEClass().name().toLowerCase() + " != null && !Transactions.isCanceled())");
//							FileUtilities.incIndent();
//								println("" + association.getTargetAEClass().name().toLowerCase() + ".insertAssociation(new" + theClass.name() + ");");
//							FileUtilities.decIndent();
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
//						println("if (mTwoPane)");
//						FileUtilities.incIndent();
							println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL,clicked" + theClass.name() + ");");
//						FileUtilities.decIndent();
//						println("else");
//						println("{");
//						FileUtilities.incIndent();
//							println("((DetailFragment) detail_fragment).hide();");
//							println("list_fragment.show();");
//							println("showingDetail = false;");
//						FileUtilities.decIndent();
//						println("}");
						println("if(AssociationEndMultiplicity == ToMANY)");
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
						
//						println("finish();");
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
			if(ModelUtilities.hasAssociations(theClass))
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
						println("getSupportFragmentManager().beginTransaction().detach(detail_fragment).commit();");
					FileUtilities.decIndent();
				FileUtilities.decIndent();
				println("}");
				println("else if(AssociationEndMultiplicity != ToONE)");
				println("{");
				FileUtilities.incIndent();
					println("((DetailFragment) detail_fragment).hide();");
					println("list_fragment.show();");
					println("showingDetail = false;");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("if (clicked" + theClass.name() + " != null)");
					FileUtilities.incIndent();
						println("setDetailFragment(" + theClass.name() + "DetailFragment.ARG_VIEW_DETAIL,clicked" + theClass.name() + ");");
					FileUtilities.decIndent();
					println("else");
					FileUtilities.incIndent();
						println("getSupportFragmentManager().beginTransaction().detach(detail_fragment).commit();");
					FileUtilities.decIndent();
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
				print(" && " + association.getTargetAEClass().name().toLowerCase() + " == null");
			println(")" );
			FileUtilities.incIndent();
				println("list_fragment.add(object);");
			FileUtilities.decIndent();
			for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
				print("if(caller == " + association.getTargetAEClass().name() + ".class && " 
						+ association.getTargetAEClass().name().toLowerCase() + " != null");
				if(is2Many(association, association.getSourceAE()))
					println(" && " + association.getTargetAEClass().name().toLowerCase() + "." + association.getSourceAE().name() + "() == neibor)");
				else
					println(" && " + association.getTargetAEClass().name().toLowerCase() + " == neibor)" );
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
//		comando para TESTE - START
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".Command;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandTargetLayer;");
//		comando para TESTE - END	
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
		println("import " + basePackageName + "." + utilsLayerName + ".DetailFragment;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeEvent;");
		println("import " + basePackageName + "." + utilsLayerName + ".PropertyChangeListener;");
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		if(ModelUtilities.isAssociativeClass(theClass)){
			for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
				if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
					println("import " + basePackageName + "." + businessLayerName + "." + ass.getTargetAEClass().name() + ";");
					println("import " + basePackageName + "." + presentationLayerName + "." + ass.getTargetAEClass().name() + "." + ass.getTargetAEClass().name() + "DetailFragment;");
				}
		}
		List<MClass> owners = ModelUtilities.getAttributeObjectTypeOwners(theClass);
		for(MClass owner : owners)
			println("import " + basePackageName + "." + presentationLayerName + "." + owner.name() + "." + owner.name() + "Activity;");

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
		
		if(ModelUtilities.isSpecialPrimitive(theClass)){
			if(theClass.name().equals("CalendarDate"))
				androidClassTypes.add("DatePicker");
			if(theClass.name().equals("CalendarTime"))
				imports.add("import " + basePackageName + "." + "customViews" + "." + "CustomTimePicker" + ";");
		}
		
		for (MAttribute att : AllAttributes){
			String type = AndroidTypes.androidPrimitiveTypeToReadWidget(att.type(), AndroidWidgetPreference.NORMAL);
			androidClassTypes.add(type);
			if(!type.equals(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL)))
				androidClassTypes.add(AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL));
			
			javaClassTypes.add(att.type());

			if(att.type().isEnum())
				imports.add("import " + basePackageName + "." + businessLayerName + "." + att.type().toString() + ";");
			if(att.type().isObjectType()){
				imports.add("import " + basePackageName + "." + presentationLayerName + "." + att.type().toString() + "." + att.type().toString() + "DetailFragment;");
				imports.add("import " + basePackageName + "." + businessLayerName + "." + att.type().toString() + ";");
			}
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
		println("public String ARG_VIEW = \"\";");
		println("private Fragment fragment;");
		if(ModelUtilities.isAssociativeClass(theClass)){
			for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
				if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
					println("private Fragment fragment" + ass.getTargetAEClass().name() + ";");
		}
		println("private View rootView = null;");
		println("private View activeView = null;");
		println("private " + theClass.name() + " " + theClass.name().toLowerCase() + " = null;");
		println("private int " + theClass.name().toLowerCase() + "ID = 0;");
		println("private final String " + theClass.name().toUpperCase() + "ID = \"" + theClass.name().toUpperCase() + "ID\";");

		if(!theClass.isAbstract() && !ModelUtilities.isSpecialPrimitive(theClass)){
	//		--------------*************** CODIGO NOVO - START  ******************* ------------------
			List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);
	//		--------------*************** CODIGO NOVO - END  ******************* ------------------
			
			for (MAttribute att : finalAttributes)
	//			if(att.name() != "ID")
				if(att.type().isObjectType())
					println("private " + att.type().toString() + "DetailFragment " + att.name().toLowerCase() + "View;");
				else
					println("private " + AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL) + " " + att.name().toLowerCase() + "View;");
		}else if(ModelUtilities.isSpecialPrimitive(theClass)){
			println("private " + AndroidTypes.androidPrimitiveTypeToWriteWidget(theClass.type(), AndroidWidgetPreference.NORMAL) + " " + theClass.name().toLowerCase() + "View;");
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
		 		println("ARG_VIEW = savedInstanceState.getString(\"ARG_VIEW\");");
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
//					comando para TESTE - START
					println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "DetailFragment.class, CommandType.READ, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//					comando para TESTE - END	
			 		FileUtilities.incIndent();
			 			println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "(getArguments().getInt(ARG_VIEW_DETAIL));");
	 					println("if(" + theClass.name().toLowerCase() + " != null)");
	 					FileUtilities.incIndent();
	 						println(theClass.name().toLowerCase() + "ID = " + theClass.name().toLowerCase() + ".ID();");
	 					FileUtilities.decIndent();
			 			if(ModelUtilities.isAssociativeClass(theClass)){
			 				for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
			 					if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
			 						println("fragment" + ass.getTargetAEClass().name() + " = new " + ass.getTargetAEClass().name() + "DetailFragment();");
			 					
			 			}
			 			println("ARG_VIEW = ARG_VIEW_DETAIL;");
			 		FileUtilities.decIndent();
			 		println("}");
			 		if(!theClass.isAbstract()){
				 		println("if(getArguments().containsKey(ARG_VIEW_EDIT))");
				 		println("{");
//						comando para TESTE - START
						println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "DetailFragment.class, CommandType.WRITE, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//						comando para TESTE - END	
						FileUtilities.incIndent();
				 			println(theClass.name().toLowerCase() + " = " + theClass.name() + ".get" + theClass.name() + "(getArguments().getInt(ARG_VIEW_EDIT));");
				 			println("if(" + theClass.name().toLowerCase() + " != null)");
			 				FileUtilities.incIndent();
					 			println(theClass.name().toLowerCase() + "ID = " + theClass.name().toLowerCase() + ".ID();");
					 		FileUtilities.decIndent();
					 		println("ARG_VIEW = ARG_VIEW_EDIT;");
				 		FileUtilities.decIndent();
				 		println("}");
				 		println("if(getArguments().containsKey(ARG_VIEW_NEW))");
				 		println("{");
//						comando para TESTE - START
						println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "DetailFragment.class, CommandType.WRITE, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//						comando para TESTE - END	
				 		FileUtilities.incIndent();
				 			println("ARG_VIEW = ARG_VIEW_NEW;");
				 		FileUtilities.decIndent();
				 		println("}");
			 		}
//		 			Zona nova - START
		 			if(!theClass.isAbstract()){
		 				List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);			 						
		 				for (MAttribute att : finalAttributes)
		 					if(att.type().isObjectType())
		 						println(att.name() + "View = new " + att.type().toString() + "DetailFragment();");

		 			}
//		 			Zona nova - END
			 	FileUtilities.decIndent();
				println("}");
		 	FileUtilities.decIndent();
		 	println("}");
		 	println("fragment = this;");
		 	println(theClass.name() + ".getAccess().setChangeListener(this);");
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
			println("{");
			FileUtilities.incIndent();
				println("outState.putInt(" + theClass.name().toUpperCase() + "ID, " + theClass.name().toLowerCase() + ".ID());");
				println("outState.putString(\"ARG_VIEW\", ARG_VIEW);");
			FileUtilities.decIndent();
			println("}");
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
		List<MClass> owners = ModelUtilities.getAttributeObjectTypeOwners(theClass);
		String condition = null;
		for(int i = 0;i < owners.size();++i){
			if(i == 0)
				if(owners.size() == 1)
					condition = "activity.getClass() != " + owners.get(i).name() + "Activity.class";
				else
					condition = "(activity.getClass() != " + owners.get(i).name() + "Activity.class";
			else if(i == owners.size() - 1)
				condition = condition + " && activity.getClass() != " + owners.get(i).name() + "Activity.class)";
			else
				condition = condition + " && activity.getClass() != " + owners.get(i).name() + "Activity.class";
		}
		println("@Override");
		println("public void onAttach(Activity activity)");
		println("{");
		FileUtilities.incIndent();
			println("super.onAttach(activity);");
			println("if(getArguments() == null || !getArguments().containsKey(ARG_VIEW_DETAIL))");
		 	println("{");
		 	FileUtilities.incIndent();
		 		if(condition != null)
		 			println("if (" + condition + " && !(activity instanceof Callbacks))");
		 		else
		 			println("if (!(activity instanceof Callbacks))");
			 	println("{");
			 	FileUtilities.incIndent();
			 		println("throw new IllegalStateException(\"Activity must implement fragment's callbacks.\");");
				FileUtilities.decIndent();
				println("}");
				if(condition != null){
		 			println("if (" + condition + ")");
					FileUtilities.incIndent();
						println("mCallbacks = (Callbacks) activity;");
					FileUtilities.decIndent();
				}else
		 			println("mCallbacks = (Callbacks) activity;");
			FileUtilities.decIndent();
			println("}");
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
		 			println("if (!getArguments().containsKey(\"IsChildFragment\"))");
		 			FileUtilities.incIndent();
		 				println("rootView = inflater.inflate(R.layout." + theClass.name().toLowerCase() + "_view_detail, container, false);");
		 			FileUtilities.decIndent();
		 			println("setViewDetailData();");
//				 	Zona nova - START
//				 	if(!theClass.isAbstract()){
//				 		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);			 						
//				 		for (MAttribute att : finalAttributes)
//				 			if(att.type().isObjectType()){
//				 				println(att.name() + "View.setViewDetailData();");
//				 			}
//				 	}
//				 	Zona nova - END
		 		FileUtilities.decIndent();
		 		println("}");
		 		if(!theClass.isAbstract()){
			 		println("if(getArguments().containsKey(ARG_VIEW_NEW) || getArguments().containsKey(ARG_VIEW_EDIT))");
			 		println("{");
					FileUtilities.incIndent();
						println("if (!getArguments().containsKey(\"IsChildFragment\"))");
			 			FileUtilities.incIndent();
			 				println("rootView = inflater.inflate(R.layout." + theClass.name().toLowerCase() + "_view_insertupdate, container, false);");
			 			FileUtilities.decIndent();
			 			println("setViewNewOrEditData();");
//					 	Zona nova - START
//					 	if(!theClass.isAbstract()){
//					 		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);			 						
//					 		for (MAttribute att : finalAttributes)
//					 			if(att.type().isObjectType()){
//					 				println(att.name() + "View.setViewNewOrEditData();");
//					 			}
//					 	}
//					 	Zona nova - END
					 	println("if (!getArguments().containsKey(\"IsChildFragment\"))");
					 	println("{");
			 			FileUtilities.incIndent();
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
	public void printDetailFragment_onViewCreated(MClass theClass) {
		println("@Override");
		println("public void onViewCreated(View view, Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onViewCreated(view, savedInstanceState);");
			println("if(savedInstanceState != null)");
			println("{");
			FileUtilities.incIndent();
				if(ModelUtilities.isAssociativeClass(theClass)){
					for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
						if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
							println("fragment" + ass.getTargetAEClass().name() + " = getChildFragmentManager().findFragmentById(R.id." + ass.getTargetAEClass().name().toLowerCase() + "_detail_container);");
							println("if(fragment" + ass.getTargetAEClass().name() + " == null)");
							FileUtilities.incIndent();
								println("fragment" + ass.getTargetAEClass().name() + " = new " + ass.getTargetAEClass().name() + "DetailFragment();");
							FileUtilities.decIndent();
						}
				}
//			 	Zona nova - START
			 	if(!theClass.isAbstract()){
			 		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);			 						
			 		for (MAttribute att : finalAttributes)
			 			if(att.type().isObjectType()){
			 				println(att.name() + "View" + " = (" + att.type().toString() + "DetailFragment) getChildFragmentManager().findFragmentById(R.id." + att.type().toString().toLowerCase() + "_detail_container);");
							println("if(" + att.name() + "View" + " == null)");
							FileUtilities.incIndent();
								println(att.name() + "View" + " = new " + att.type().toString() + "DetailFragment();");
							FileUtilities.decIndent();
			 			}
			 	}
//			 	Zona nova - END
			FileUtilities.decIndent();
			println("}");
			println("else");
			println("{");
			FileUtilities.incIndent();
				if(!theClass.isAbstract()){
			 		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, true);			 						
			 		for (MAttribute att : finalAttributes)
			 			if(att.type().isObjectType()){
							println(att.name() + "View" + " = new " + att.type().toString() + "DetailFragment();");
			 			}
			 	}
				println("if(getArguments().containsKey(ARG_VIEW_DETAIL))");
				println("{");
				FileUtilities.incIndent();
				if(ModelUtilities.isAssociativeClass(theClass)){
					for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
						if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
							println("if(" + theClass.name().toLowerCase() + " != null && " + theClass.name().toLowerCase() + "." + ass.getTargetAE().nameAsRolename() + "() != null)");
							FileUtilities.incIndent();
								println("UtilNavigate.addFragment(fragment, fragment" + ass.getTargetAEClass().name() + ", R.id." + ass.getTargetAEClass().name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, " + theClass.name().toLowerCase() + "." + ass.getTargetAE().nameAsRolename() + "().ID()));");
							FileUtilities.decIndent();
							println("else");
							FileUtilities.incIndent();
								println("UtilNavigate.addFragment(fragment, fragment" + ass.getTargetAEClass().name() + ", R.id." + ass.getTargetAEClass().name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, 0));");
							FileUtilities.decIndent();
						}
				}
//			 	Zona nova - START
			 	if(!theClass.isAbstract()){
			 		List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, false, true);			 						
			 		for (MAttribute att : finalAttributes)
			 			if(att.type().isObjectType()){
			 				println("if(" + theClass.name().toLowerCase() + " != null && " + theClass.name().toLowerCase() + "." + att.name() + "() != null)");
							FileUtilities.incIndent();
								println("UtilNavigate.replaceFragment(fragment, " + att.name() + "View" + ", R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + ", UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, " + theClass.name().toLowerCase() + "." + att.name() + "().ID(), true, R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + "));");
							FileUtilities.decIndent();
							println("else");
							FileUtilities.incIndent();
								println("UtilNavigate.replaceFragment(fragment, " + att.name() + "View" + ", R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + ", UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, 0, true, R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + "));");
							FileUtilities.decIndent();
			 			}
			 	}

				FileUtilities.decIndent();
				println("}");
				if(!theClass.isAbstract()){
					println("if(getArguments().containsKey(ARG_VIEW_EDIT))");
					println("{");
					FileUtilities.incIndent();
						List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);			 						
				 		for (MAttribute att : finalAttributes)
				 			if(att.type().isObjectType()){
				 				println("if(" + theClass.name().toLowerCase() + " != null && " + theClass.name().toLowerCase() + "." + att.name() + "() != null)");
								FileUtilities.incIndent();
									println("UtilNavigate.replaceFragment(fragment, " + att.name() + "View" + ", R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + ", UtilNavigate.setFragmentBundleArguments(ARG_VIEW_EDIT, " + theClass.name().toLowerCase() + "." + att.name() + "().ID(), true, R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + "));");
								FileUtilities.decIndent();
								println("else");
								FileUtilities.incIndent();
									println("UtilNavigate.replaceFragment(fragment, " + att.name() + "View" + ", R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + ", UtilNavigate.setFragmentBundleArguments(ARG_VIEW_EDIT, 0, true, R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + "));");
								FileUtilities.decIndent();
				 			}
				 	FileUtilities.decIndent();
					println("}"); 
								
					println("if(getArguments().containsKey(ARG_VIEW_NEW))");
					println("{");
					FileUtilities.incIndent();
				 		for (MAttribute att : finalAttributes)
				 			if(att.type().isObjectType()){
								println("UtilNavigate.replaceFragment(fragment, " + att.name() + "View" + ", R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + ", UtilNavigate.setFragmentBundleArguments(ARG_VIEW_NEW, 0, true, R.id." + att.type().toString().toLowerCase() + "_detail_container_" + att.name().toLowerCase() + "));");
				 			}
					FileUtilities.decIndent();
					println("}");
				}
//			 	Zona nova - END
			FileUtilities.decIndent();
			println("}");
			
		FileUtilities.decIndent();
		println("}");
		println();
	}
	
	@Override
	public void printDetailFragment_replaceObject(MClass theClass) {
		println("public void replaceObject(final String ARG_VIEW, final " + theClass.name() + " new" + theClass.name() + ")");
		println("{");
		FileUtilities.incIndent();
			println(" getActivity().runOnUiThread(new Runnable() {");
			FileUtilities.incIndent();
				println("public void run() {");
				FileUtilities.incIndent();
					println(theClass.name().toLowerCase() + " = new" + theClass.name() + ";");
					println(theClass.name().toLowerCase() + "ID = new" + theClass.name() + ".ID();");
					
					println("if(ARG_VIEW.equals(ARG_VIEW_DETAIL))");
					println("{");
					FileUtilities.incIndent();
						println("fragment.onSaveInstanceState(UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, new" + theClass.name() + ".ID()));");
						println("setViewDetailData();");
					if(!theClass.isAbstract())
						for(MAttribute att : theClass.allAttributes())
							if(att.type().isObjectType())
								println(att.name() + "View.replaceObject(ARG_VIEW, " + theClass.name().toLowerCase() + "." + att.name().toLowerCase() + "());");
					FileUtilities.decIndent();
					println("}");
					if(!theClass.isAbstract()){
						println("if(ARG_VIEW.equals(ARG_VIEW_EDIT))");
						println("{");
						FileUtilities.incIndent();
							println("fragment.onSaveInstanceState(UtilNavigate.setFragmentBundleArguments(ARG_VIEW_EDIT, new" + theClass.name() + ".ID()));");
							println("setViewNewOrEditData();");
							for(MAttribute att : theClass.allAttributes())
								if(att.type().isObjectType())
									println(att.name() + "View.replaceObject(ARG_VIEW, " + theClass.name().toLowerCase() + "." + att.name().toLowerCase() + "());");
						FileUtilities.decIndent();
						println("}");
						
						println("if(ARG_VIEW.equals(ARG_VIEW_NEW))");
						println("{");
						FileUtilities.incIndent();
							println("fragment.onSaveInstanceState(UtilNavigate.setFragmentBundleArguments(ARG_VIEW_NEW, 0));");
						FileUtilities.decIndent();
						println("}");
					}
				FileUtilities.decIndent();
				println("}");
			FileUtilities.decIndent();
			println("});");
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
	public void printDetailFragment_confirmActiveView(MClass theClass){
		println("private void confirmActiveView()");
		println("{");
		FileUtilities.incIndent();
			println("activeView = rootView;");
			println("if(getArguments() != null && getArguments().containsKey(\"IsChildFragment\"))");
			FileUtilities.incIndent();
				println("activeView = getParentFragment().getView().findViewById(getArguments().getInt(\"container\"));");
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
			if(!ModelUtilities.isSpecialPrimitive(theClass)){
		//		--------------*************** CODIGO NOVO - START  ******************* ------------------
				List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
		//		--------------*************** CODIGO NOVO - END  ******************* ------------------
					for (MAttribute att : finalAttributes)
		//				if(att.name() != "ID")
						if(!att.type().isObjectType())
							println(att.name().toLowerCase() + "View = (" + AndroidTypes.androidPrimitiveTypeToWriteWidget(att.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + attributeBaseAncestor(theClass, att).name().toLowerCase() + "_insertupdate_" + att.name().toLowerCase() + "_value);");
			}else{
				println(theClass.name().toLowerCase() + "View = (" + AndroidTypes.androidPrimitiveTypeToWriteWidget(theClass.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + theClass.name().toLowerCase() + "_insertupdate_" + theClass.name().toLowerCase() + "_value);");
			}
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
			println("confirmActiveView();");
			if(ModelUtilities.isSpecialPrimitive(theClass)){
				println(theClass.name().toLowerCase() + "View = (" + AndroidTypes.androidPrimitiveTypeToWriteWidget(theClass.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + theClass.name().toLowerCase() + "_detail_" + theClass.name().toLowerCase() + "_value);");
				if(theClass.name().equals("CalendarTime")){
					println(theClass.name().toLowerCase() + "View.setIs24HourView(true);");
				}
			}
			println("if (" + theClass.name().toLowerCase() + " != null)");
			println("{");
			FileUtilities.incIndent();
			if(ModelUtilities.isSpecialPrimitive(theClass)){
				println("((" + AndroidTypes.androidPrimitiveTypeToReadWidget(theClass.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + theClass.name().toLowerCase() + "_detail_" + theClass.name().toLowerCase() + "_value))." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, theClass.type(), theClass.name().toLowerCase(), AndroidWidgetPreference.NORMAL) + ";");
			}else{
	//			--------------*************** CODIGO NOVO - START  ******************* ------------------
				List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, false, true);
	//			--------------*************** CODIGO NOVO - END  ******************* ------------------
				for (MAttribute att : finalAttributes)
					if(att.name() != "ID" && !att.type().isObjectType()){
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date"))){
							println("if(" + theClass.name().toLowerCase() + "." + att.name() + "() != null" + ")");
							println("{");
							FileUtilities.incIndent();
								println("((" + AndroidTypes.androidPrimitiveTypeToReadWidget(att.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + attributeBaseAncestor(theClass, att).name().toLowerCase() + "_detail_" + att.name().toLowerCase() + "_value))." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, att.type(), theClass.name().toLowerCase() + "." + att.name() + "()", AndroidWidgetPreference.NORMAL) + ";");
							FileUtilities.decIndent();
							println("}");
						}else
							println("((" + AndroidTypes.androidPrimitiveTypeToReadWidget(att.type(), AndroidWidgetPreference.NORMAL) + ") activeView.findViewById(R.id." + attributeBaseAncestor(theClass, att).name().toLowerCase() + "_detail_" + att.name().toLowerCase() + "_value))." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, att.type(), theClass.name().toLowerCase() + "." + att.name() + "()", AndroidWidgetPreference.NORMAL) + ";");
					}
			}
//			if(ModelUtilities.isAssociativeClass(theClass)){
// 				for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
// 					if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
// 						println("UtilNavigate.addFragment(fragment, fragment" + ass.getTargetAEClass().name() + ", R.id." + ass.getTargetAEClass().name().toLowerCase() + "_detail_container, UtilNavigate.setFragmentBundleArguments(ARG_VIEW_DETAIL, " + theClass.name().toLowerCase() + "." + ass.getTargetAE().name() + "()));");
// 					}	
//			}
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
				println("confirmActiveView();");
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
				println("confirmActiveView();");
				println("if (" + theClass.name().toLowerCase() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("setInput();");
					if(ModelUtilities.isSpecialPrimitive(theClass)){
						println(theClass.name().toLowerCase() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.WRITE_WIDGET, theClass.type(), theClass.name().toLowerCase(), AndroidWidgetPreference.NORMAL) + ";");
					}else{
		//				--------------*************** CODIGO NOVO - START  ******************* ------------------
						List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
		//				--------------*************** CODIGO NOVO - END  ******************* ------------------
						for (MAttribute att : finalAttributes)
		//					if(att.name() != "ID")
							if(!att.type().isObjectType()){
								if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date"))){
									println("if(" + theClass.name().toLowerCase() + "." + att.name() + "() != null" + ")");
									println("{");
									FileUtilities.incIndent();
										println(att.name().toLowerCase() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.WRITE_WIDGET, att.type(), theClass.name().toLowerCase() + "." + att.name() + "()", AndroidWidgetPreference.NORMAL) + ";");
									FileUtilities.decIndent();
									println("}");
								}else
									println(att.name().toLowerCase() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.WRITE_WIDGET, att.type(), theClass.name().toLowerCase() + "." + att.name() + "()", AndroidWidgetPreference.NORMAL) + ";");
							}
					}
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
				println("confirmActiveView();");
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
				
//				System.out.println(finalAttributes.toString());
//				if(ModelUtilities.isSpecialPrimitive(theClass)){
//					
//				}else{
					for (MAttribute att : AllAttributes)
		//				if(att.name() != "ID")
						if(finalAttributes.contains(att)){
							if(att.type().isObjectType())
								println(att.type().toString() + " temp_" + att.name() + " = " + att.name() + "View.ActionViewNew();");
							else{
								println(JavaInput.inputTemporaryVariables(att.type(), "temp_" + att.name(), AndroidTypes.androidInputWidgetContentGetter(AndroidWidgetsTypes.WRITE_WIDGET, att, "View", AndroidWidgetPreference.NORMAL, ModelUtilities.isSpecialPrimitive(theClass))));
//								if (att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("CalendarDate")))
//									println(JavaInput.inputValidation(att.type(), "temp_" + att.name(), att.name(), AndroidTypes.androidInputWidgetContentGetter(AndroidWidgetsTypes.WRITE_WIDGET, att, "View", AndroidWidgetPreference.NORMAL, ModelUtilities.isSpecialPrimitive(theClass)), "UtilNavigate.showWarning(getActivity(), ", true , getIndentSpace(), true));
//								else
									println(JavaInput.inputValidation(att, "temp_" + att.name(), att.name(), AndroidTypes.androidInputWidgetContentGetter(AndroidWidgetsTypes.WRITE_WIDGET, att, "View", AndroidWidgetPreference.NORMAL, ModelUtilities.isSpecialPrimitive(theClass)), "UtilNavigate.showWarning(getActivity(), ", true , getIndentSpace(), false, ModelUtilities.isSpecialPrimitive(theClass)));
							}
							println();
						}
//				}
//					else if(!att.name().equals("ID")){
//						println(JavaInputValidation.inputValidation(att.type(), "temp_" + att.name(), att.name(), defaultValueType(att.type()), "UtilNavigate.showWarning(getActivity(), ", true , getIndentSpace(), true));
//						println();
//					}
	//			List<AttributeInfo> inheritedAttributes = new ArrayList<AttributeInfo>();
	//			for (MClass theParentClass : theClass.allParents())
	//				for(AttributeInfo attribute : AttributeInfo.getAttributesInfo(theParentClass))
	//					inheritedAttributes.add(attribute);
				
				print("return new " + theClass.name() + "(");
				boolean hasAssociative = false;
				if(ModelUtilities.isAssociativeClass(theClass)){
					hasAssociative = true;
					boolean firts = true;// there are always only two
					for(AttributeInfo att : AttributeInfo.getAttributesInfo(theClass))
						if(att.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
							if(firts){
								print("null, ");
//								print(theClass.name().toLowerCase() + "." + att.getName().toLowerCase() + "() == null ? null : " + theClass.name().toLowerCase() + "." + att.getName().toLowerCase() + "(), ");
								firts = false;
							}else
								print("null");
						
				}
				for (int i = 0; i < AllAttributes.size(); i++) {
	//			if(inheritedAttributes.get(i).getKind().toString().equals(AssociationKind.NONE.toString()) && !inheritedAttributes.get(i).getName().equals("ID")){
	//				.contains na da :/ ???
	//					if(finalAttributes.contains(AllAttributes.get(i)))~
					if(AllAttributes.get(i).name() != "ID")
						if(finalAttributes.contains(AllAttributes.get(i))){
							if(hasAssociative){
								hasAssociative = false;
								print(", ");
							}
							print("temp_" + AllAttributes.get(i).name());
						}
	//					else if(AllAttributes.get(i).name() != "ID")
	//						print(defaultValueType(AllAttributes.get(i).type()));
						
					if (finalAttributes.contains(AllAttributes.get(i)) && i < AllAttributes.size() - 1 && AllAttributes.get(i).name() != "ID")
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
	
	@Override
	public void printDetailFragment_ActionViewEdit(MClass theClass) {
		if(!theClass.isAbstract()){
			println("public boolean ActionViewEdit()");
			println("{");
			FileUtilities.incIndent();
				println("if (" + theClass.name().toLowerCase() + " != null)");
				println("{");
				FileUtilities.incIndent();
					println("confirmActiveView();");
//						--------------*************** CODIGO NOVO - START  ******************* ------------------
						List<MAttribute> finalAttributes = getDetailViewAttributes(theClass, true, false);
		//				--------------*************** CODIGO NOVO - END  ******************* ------------------
						for (MAttribute att : finalAttributes)
							if(att.type().isObjectType())
								println(att.name() + "View.ActionViewEdit();");
							else
								println(JavaInput.inputComparatorConditionSetter(att, "temp_" + att.name(), theClass.name().toLowerCase() + "." + att.name() + "()", theClass.name().toLowerCase() + ".set" + capitalize(att.name()), AndroidTypes.androidInputWidgetContentGetter(AndroidWidgetsTypes.WRITE_WIDGET, att, "View", AndroidWidgetPreference.NORMAL, ModelUtilities.isSpecialPrimitive(theClass)), getIndentSpace(), ModelUtilities.isSpecialPrimitive(theClass)));
					
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
					println("if(getActivity().getCurrentFocus() != null)");
		 			FileUtilities.incIndent();
		 				println("inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);");
		 			FileUtilities.decIndent();
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
		 			println("if(getActivity().getCurrentFocus() != null)");
		 			FileUtilities.incIndent();
		 				println("inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);");
		 			FileUtilities.decIndent();
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
						if(ModelUtilities.isAssociativeClass(theClass)){
							println("if(event.getNewNeibor() != null)");
							println("{");
							FileUtilities.incIndent();
				 				for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
				 					if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
				 						println("if(event.getNewNeibor().getClass() == " + ass.getTargetAEClass().name() + ".class)");
				 						FileUtilities.incIndent();
				 							println("if(fragment" + ass.getTargetAEClass().name() + " != null)");
				 							FileUtilities.incIndent();
				 								println("((" + ass.getTargetAEClass().name() + "DetailFragment) fragment" + ass.getTargetAEClass().name() + ").replaceObject(ARG_VIEW, (" + ass.getTargetAEClass().name() + ") event.getNewNeibor());");
				 							FileUtilities.decIndent();
				 						FileUtilities.decIndent();
				 					}
			 				FileUtilities.decIndent();
							println("}");
			 			}
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
								println("FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();");
								println("ft.remove(fragment);");
								println("ft.commit();");
							FileUtilities.decIndent();
							println("}");
						FileUtilities.decIndent();
						println("break;");
						
						println("case DELETE_ASSOCIATION:");
						FileUtilities.incIndent();
						if(ModelUtilities.isAssociativeClass(theClass)){
							println("if(event.getNewNeibor() != null)");
							println("{");
							FileUtilities.incIndent();
				 				for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(theClass))
				 					if(ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
				 						println("if(event.getNewNeibor().getClass() == " + ass.getTargetAEClass().name() + ".class)");
				 						FileUtilities.incIndent();
				 						println("if(fragment" + ass.getTargetAEClass().name() + " != null)");
			 								FileUtilities.incIndent();
			 									println("((" + ass.getTargetAEClass().name() + "DetailFragment) fragment" + ass.getTargetAEClass().name() + ").replaceObject(ARG_VIEW, null);");
			 								FileUtilities.decIndent();
			 							FileUtilities.decIndent();
				 					}
				 			FileUtilities.decIndent();
							println("}");
			 			}
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
//		comando para TESTE - START
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".Command;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandTargetLayer;");
//		comando para TESTE - END	
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
			if(isSuperClass(association.getTargetAEClass()))
				for(MClass subClass : getAllSubClasses(Arrays.asList(association.getTargetAEClass())))
					if(!activityImport.contains(subClass))
						activityImport.add(subClass);
			if(!activityImport.contains(association.getTargetAEClass()))
				activityImport.add(association.getTargetAEClass());
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
		
		for (AssociationInfo ass : AssociationInfo.getAllAssociationsInfo(theClass))
			if(ass.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
				println("private View " + ass.getTargetAEClass().nameAsRolename() + "View;");
			else
				println("private View " + ass.getTargetAE().name() + "View;");
		
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
				String targetRole = association.getTargetAE().name();
				if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
					targetRole = association.getTargetAEClass().nameAsRolename();
				
				if(association.getSourceAEClass() == theClass)
					println(targetRole + "View = (View) getActivity().findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole.toLowerCase() + ");");
				else
					println(targetRole + "View = (View) getActivity().findViewById(R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + ");");
				println(targetRole + "View.setOnClickListener(ClickListener);");
				println(targetRole + "View.setOnLongClickListener(LongClickListener);");
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
	public void printNavigationBarFragment_onViewCreated(MClass theClass) {
		println("@Override");
		println("public void onViewCreated(View view, Bundle savedInstanceState)");
		println("{");
		FileUtilities.incIndent();
			println("super.onViewCreated(view, savedInstanceState);");
			
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
			
			println("if(" + theClass.name().toLowerCase() + " == null)");
			println("{");
			FileUtilities.incIndent();
				println("refreshNavigationBar(null);");
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
					String targetRole = association.getTargetAE().name();
					if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
						targetRole = association.getTargetAEClass().nameAsRolename();
					if(association.getSourceAE().cls() == theClass)
						println("number_objects = (TextView) rootView.findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole.toLowerCase() + "_numberobjects);");
					else
						println("number_objects = (TextView) rootView.findViewById(R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + "_numberobjects);");
					
					if(association.getKind() != AssociationKind.ASSOCIATIVE2MEMBER && association.getTargetAE().isCollection()){
						println("if(" + theClass.name().toLowerCase() + "." + targetRole + "().isEmpty())");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + targetRole + "View, false);");
							println("setNumberAssociation(number_objects, 0);");
						FileUtilities.decIndent();
						println("}");
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + targetRole + "View, true);");
							println("setNumberAssociation(number_objects, clicked" + theClass.name() + "." + targetRole + "().size());");
						FileUtilities.decIndent();
						println("}");
						println();
					}else{
						println("if(" + theClass.name().toLowerCase() + "." + targetRole + "() == null)");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + targetRole + "View, false);");
							println("setNumberAssociation(number_objects, 0);");
						FileUtilities.decIndent();
						println("}");
						println("else");
						println("{");
						FileUtilities.incIndent();
							println("prepareView(" + targetRole + "View, true);");
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
				for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
					String targetRole = association.getTargetAE().name();
					if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
						targetRole = association.getTargetAEClass().nameAsRolename();
					if(association.getSourceAE().cls() == theClass)
						println("number_objects = (TextView) rootView.findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole.toLowerCase() + "_numberobjects);");
					else
						println("number_objects = (TextView) rootView.findViewById(R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + "_numberobjects);");
					
					println("prepareView(" + targetRole + "View, false);");
					println("setNumberAssociation(number_objects, 0);");
					println(targetRole + "View.setBackgroundResource(R.drawable.navigationbar_selector);");
					println();
					
				}
				//inheritance navigations - start
	//			if(isSubClass(theClass)){
	//				not needed -> always 1
	//			}
				if(isSuperClass(theClass)){
					for(MClass sub : theClass.children()){
						println("number_objects = (TextView) rootView.findViewById(R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + sub.name().toLowerCase() + "_numberobjects);");
						println("prepareView(" + sub.name() + "View, true);");
						println("setNumberAssociation(number_objects, 0);");
						println();
					}
				}
				//inheritance navigations - end
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
			for (AssociationInfo ass : AssociationInfo.getAllAssociationsInfo(theClass)){
				String targetRole = ass.getTargetAE().name();
				if(ass.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
					targetRole = ass.getTargetAEClass().nameAsRolename().toLowerCase();
				println("if(clicked" + theClass.name() + ".valid" + capitalize(targetRole) + "())");
				println("{");
				FileUtilities.incIndent();
					println(targetRole + "View.setBackgroundResource(R.drawable.navigationbar_selector);");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println(targetRole + "View.setBackgroundResource(R.drawable.navigationbar_selector_error);");
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
//			comando para TESTE - START
			println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "NavigationBarFragment.class, CommandType.READ, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//			comando para TESTE - END	
				println("if (getActivity().getIntent().getAction().equals(\"WRITE\"))");
				println("{");
				FileUtilities.incIndent();
					println("UtilNavigate.showWarning(getActivity(), \"Navigation Bar Warning\", \"You are in CREATION MODE.\\nPlease finish this action (object association) before trying to further navigate.\\n\\nNOTE: The upper Android icon will turn green when this action is completed or canceled\");");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("if(view.isClickable())");
					println("{");
					FileUtilities.incIndent();
						println("if(clicked" + theClass.name() + " != null)");
						println("{");
						FileUtilities.incIndent();
							for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
								String targetRole = association.getTargetAE().name().toLowerCase();
								if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
									targetRole = association.getTargetAEClass().nameAsRolename().toLowerCase();
								if(association.getSourceAE().cls() == theClass)
									println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
								else
									println("if(view.getId() == R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
								println("{");
								FileUtilities.incIndent();
									MMultiplicity targetMultiplicity = association.getTargetAE().multiplicity();
									String upperRange = targetMultiplicity.toString();
									if(targetMultiplicity.toString().contains(".."))
										upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
									if(upperRange.equals("*"))
										upperRange = "-1";
									if(association.getKind() == AssociationKind.ASSOCIATIVE2MEMBER)
										upperRange = "1";
	//								if(association.getSourceAE().cls() == theClass)
	//									println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + association.getSourceAE().name().toUpperCase() + "_" + association.getTargetAE().name().toUpperCase() + "Association\", " + upperRange + "));");
	//								else
	//									println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Association\", " + upperRange + "));");
	//------------------------------last 4 lines commented, next 4 lines added									
									if(association.getSourceAE().cls() == theClass)
										println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", " + upperRange + ", \"" + association.getName() + "Association\", \"" + association.getName() + "Association\"));");
									else
										println("UtilNavigate.toActivity(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", " + upperRange + ", \"" + association.getName() + "Association\", \"" + association.getName() + "Association\"));");
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
	//								println("UtilNavigate.toActivity(getActivity(), " + theClass.parents().iterator().next().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.parents().iterator().next().name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.parents().iterator().next().name().toUpperCase() + "Association\", " + upperRange + "));");
	//------------------------------last line commented, next line added								
									println("UtilNavigate.toActivity(getActivity(), " + theClass.parents().iterator().next().name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.parents().iterator().next().name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", " + upperRange + ", \"" + theClass.parents().iterator().next().name().toUpperCase() + "Association\", \"" + theClass.parents().iterator().next().name().toUpperCase() + "Association\"));");
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
	//									println("UtilNavigate.toActivity(getActivity(), " + sub.name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", " + upperRange + "));");
	//----------------------------------last line commented, next line added									
										println("UtilNavigate.toActivity(getActivity(), " + sub.name() + "Activity.class, MasterActivity.ACTION_MODE_READ, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", " + upperRange + ", \"" + theClass.name().toUpperCase() + "Association\", \"" + theClass.name().toUpperCase() + "Association\"));");
									FileUtilities.decIndent();
									println("}");
								}
							}
							//inheritance navigations - end
						FileUtilities.decIndent();
						println("}");
					FileUtilities.decIndent();
					println("}");
					println("else");
					println("{");
					FileUtilities.incIndent();
					for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
						String targetRole = association.getTargetAE().name().toLowerCase();
						if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
							targetRole = association.getTargetAEClass().nameAsRolename().toLowerCase();
						if(association.getSourceAE().cls() == theClass)
							println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
						else
							println("if(view.getId() == R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
						println("{");
						FileUtilities.incIndent();	
							println("if(clicked" + theClass.name() + " != null)");
							FileUtilities.incIndent();
								println("UtilNavigate.showWarning(getActivity(), \"Navigation Bar Warning\", \"The selected " + theClass.name() + " does not have any " + association.getTargetAE().nameAsRolename() + ".\\nYou can do a longclick to add new " + association.getTargetAE().nameAsRolename() + " to this " + theClass.name() + "\");");
							FileUtilities.decIndent();
							println("else");
							FileUtilities.incIndent();
								println("UtilNavigate.showWarning(getActivity(), \"Navigation Bar Warning\", \"There is not any " + theClass.name() + " selected.\\nFirts you must select an " + theClass.name() + "\");");
							FileUtilities.decIndent();

						FileUtilities.decIndent();
						println("}");
					}
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
//			comando para TESTE - START
			println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "NavigationBarFragment.class, CommandType.WRITE, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//			comando para TESTE - END
				println("if (getActivity().getIntent().getAction().equals(\"WRITE\"))");
				println("{");
				FileUtilities.incIndent();
					println("UtilNavigate.showWarning(getActivity(), \"Navigation Bar Warning\", \"You are in CREATION MODE.\\nPlease finish this action (object association) before trying to further navigate.\\n\\nNOTE: The upper Android icon will turn green when this action is completed or canceled\");");
				FileUtilities.decIndent();
				println("}");
				println("else");
				println("{");
				FileUtilities.incIndent();
					println("if(view.isLongClickable())");
					println("{");
					FileUtilities.incIndent();
						println("if(clicked" + theClass.name() + " != null)");
						println("{");
						FileUtilities.incIndent();
							for (AssociationInfo association : AssociationInfo.getAllAssociationsInfo(theClass)){
								String targetRole = association.getTargetAE().name().toLowerCase();
								if(association.getKind() == AssociationKind.MEMBER2ASSOCIATIVE)
									targetRole = association.getTargetAEClass().nameAsRolename().toLowerCase();
								if(association.getSourceAE().cls() == theClass)
									println("if(view.getId() == R.id." + theClass.name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
								else
									println("if(view.getId() == R.id." + associationTargetBaseAncestor(theClass, association).name().toLowerCase() + "_navigationbar_association_" + targetRole + ")");
								println("{");
								FileUtilities.incIndent();
								if(isSuperClass(association.getTargetAEClass())){
									isNeighborSuper.add(association);
	//								println("setSuperClassOffSpringsListeners(UtilNavigate.showInheritanceList(getActivity(), R.layout." + theClass.name().toLowerCase() + "_generalizationoptions_" + association.getTargetAEClass().name().toLowerCase() + "_view), \"" + association.getTargetAEClass().name() + "\");");
	//------------------------------last line commented, next line added
									println("setSuperClassOffSpringsListeners(UtilNavigate.showInheritanceList(getActivity(), R.layout." + theClass.name().toLowerCase() + "_generalizationoptions_" + targetRole + "_view), \"" + association.getName() + "Association" + "\");");
								}else
	//								println("UtilNavigate.toActivityForResult(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, MasterActivity.CREATION_CODE);");
									if(association.getKind() == AssociationKind.MEMBER2MEMBER)
										println("UtilNavigate.toActivityForResult(getActivity(), " + ModelUtilities.getAssociativeClass(association.getTargetAEClass(), association.getSourceAEClass()).name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", -1, \"" + association.getName() + "Association\", \"" + association.getName() + "Association" + "\"), MasterActivity.CREATION_CODE);");
									else
										println("UtilNavigate.toActivityForResult(getActivity(), " + association.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + associationTargetBaseAncestor(theClass, association).name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", -1, \"" + association.getName() + "Association\", \"" + association.getName() + "Association" + "\"), MasterActivity.CREATION_CODE);");
	
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
						println("}else");
						FileUtilities.incIndent();
							println("UtilNavigate.showWarning(getActivity(), \"Navigation Bar Warning\", \"There is not any " + theClass.name() + " selected.\\nFirts you must select an " + theClass.name() + "\");");
						FileUtilities.decIndent();
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
			supers_temp.add(x.getTargetAEClass());
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
				if(!x.getTargetAEClass().isAbstract()){
					String targetRole = x.getTargetAE().name().toLowerCase();
					if(model.getAssociationClassesOnly().contains(x.getTargetAEClass()))
						targetRole = x.getTargetAEClass().nameAsRolename().toLowerCase();
					println("if(view.findViewById(R.id." + theClass.name().toLowerCase() + "_generalizationoptions_" + targetRole + ") != null)");
					println("{");
					FileUtilities.incIndent();
						println("view.findViewById(R.id." + theClass.name().toLowerCase() + "_generalizationoptions_" + targetRole + ").setOnClickListener(new OnClickListener()");
						println("{");
						FileUtilities.incIndent();
							println("@Override");
							println("public void onClick(View v)");
							println("{");
							FileUtilities.incIndent();
//								println("UtilNavigate.toActivityForResult(getActivity(), " + x.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, MasterActivity.CREATION_CODE);");
							println("UtilNavigate.toActivityForResult(getActivity(), " + x.getTargetAEClass().name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", -1, AssociationSource, AssociationSource), MasterActivity.CREATION_CODE);");
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
//							if(isRepeteadNeighbor)
//								println("UtilNavigate.toActivityForResult(getActivity(), " + subs.name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setBundles(UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", -1),UtilNavigate.setAssociationBundleArguments(\"" + theClass.name().toUpperCase() + "END\", AssociationSource)), MasterActivity.CREATION_CODE);");
//							else
//								println("UtilNavigate.toActivityForResult(getActivity(), " + subs.name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"" + theClass.name().toUpperCase() + "Association\", -1), MasterActivity.CREATION_CODE);");
//----------------------------last 4 lines commented, next line added
								println("UtilNavigate.toActivityForResult(getActivity(), " + subs.name() + "Activity.class, MasterActivity.ACTION_MODE_WRITE, UtilNavigate.setActivityBundleArguments(\"" + theClass.name().toUpperCase() + "Object\", clicked" + theClass.name() + ".ID(), \"AssociationEndMultiplicityKey\", -1, AssociationSource, AssociationSource), MasterActivity.CREATION_CODE);");
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
//		comando para TESTE - START
		println("import " + basePackageName + "." + utilsLayerName + ".Transactions;");
		println("import " + basePackageName + "." + utilsLayerName + ".Command;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandType;");
		println("import " + basePackageName + "." + utilsLayerName + ".CommandTargetLayer;");
//		comando para TESTE - END	
		println("import " + basePackageName + "." + MainMemoryClass + ";");
		println("import " + basePackageName + "." + utilsLayerName + ".ListViewHolder;");
		println("import " + basePackageName + "." + utilsLayerName + ".UtilNavigate;");
		println("import " + basePackageName + "." + businessLayerName + "." + theClass.name() + ";");
		
		Set<String> classTypes = new HashSet<String>();

		if(ModelUtilities.isAssociativeClass(theClass)){
			for(MClass cls : ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(theClass)){
				for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues()))
					classTypes.add(AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST));
//				println("import " + basePackageName + "." + businessLayerName + "." + cls.name() + ";");
			}
		}else
			for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
				classTypes.add(AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST));

		println();
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
		
		if(ModelUtilities.isAssociativeClass(theClass)){
			println("private final int associativeDivider = R.id.default_association_associative_class_list_divider;");
			for(MClass cls : ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(theClass))
				if(!ModelUtilities.isSpecialPrimitive(cls)){
					for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues()))
						if(att.type().isObjectType())
							println("private final int " + attributeBaseAncestor(cls, att).name() + "_" + att.name() + " = R.id." +  att.type().toString().toLowerCase() + "_list_" +  att.type().toString().toLowerCase() + "_value;");
						else
							println("private final int " + attributeBaseAncestor(cls, att).name() + "_" + att.name() + " = R.id." +  attributeBaseAncestor(cls, att).name().toLowerCase() + "_list_" + att.name().toLowerCase() + "_value;");
				}else
					println("private final int " + cls.name() + "_list"  + " = R.id." +  cls.name().toLowerCase() + "_list_" + cls.name().toLowerCase() + "_value;");

		}else if(ModelUtilities.isSpecialPrimitive(theClass))
			println("private final int " + theClass.name() + "_list"  + " = R.id." +  theClass.name().toLowerCase() + "_list_" + theClass.name().toLowerCase() + "_value;");
		else
			for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
				if(att.type().isObjectType())
					println("private final int " + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + " = R.id." +  att.type().toString().toLowerCase() + "_list_" +  att.type().toString().toLowerCase() + "_value;");
				else
					println("private final int " + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + " = R.id." +  attributeBaseAncestor(theClass, att).name().toLowerCase() + "_list_" + att.name().toLowerCase() + "_value;");
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
			
			if(ModelUtilities.isAssociativeClass(theClass)){
				println("public ImageView associativeDividerIcon;");
				for(MClass cls : ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(theClass))
					if(!ModelUtilities.isSpecialPrimitive(cls)){
						for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues()))
							println("public " + AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST) + " " + attributeBaseAncestor(cls, att).name() + "_" + att.name() + "View;");
					}else
						println("public " + AndroidTypes.androidPrimitiveTypeToWidget(cls.type(), AndroidWidgetPreference.SMALLEST) + " " + cls.name() + "_list_" + cls.name() + "View;");
			}else if(ModelUtilities.isSpecialPrimitive(theClass))
				println("public " + AndroidTypes.androidPrimitiveTypeToWidget(theClass.type(), AndroidWidgetPreference.SMALLEST) + " " + theClass.name() + "_list_" + theClass.name() + "View;");
			else
				for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
					println("public " + AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST) + " " + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + "View;");
			
			println();
			
			println("public ViewHolder(View convertView)");
			println("{");
			FileUtilities.incIndent();
				println("this.errorIcon = (ImageView) convertView.findViewById(contractError);");
				if(ModelUtilities.isAssociativeClass(theClass)){
					println("this.associativeDividerIcon = (ImageView) convertView.findViewById(associativeDivider);");
					for(MClass cls : ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(theClass))
						if(!ModelUtilities.isSpecialPrimitive(cls)){
							for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues()))
								println("this." + attributeBaseAncestor(cls, att).name() + "_" + att.name() + "View = (" + AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST) + ") convertView.findViewById(" + attributeBaseAncestor(cls, att).name() + "_" + att.name() + ");" );
						}else
							println("this." + cls.name() + "_list_" + cls.name() + "View = (" + AndroidTypes.androidPrimitiveTypeToWidget(cls.type(), AndroidWidgetPreference.SMALLEST) + ") convertView.findViewById(" + cls.name() + "_list);" );
				}else if(ModelUtilities.isSpecialPrimitive(theClass))
					println("this." + theClass.name() + "_list_" + theClass.name() + "View = (" + AndroidTypes.androidPrimitiveTypeToWidget(theClass.type(), AndroidWidgetPreference.SMALLEST) + ") convertView.findViewById(" + theClass.name() + "_list);" );
				else
					for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues()))
						println("this." + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + "View = (" + AndroidTypes.androidPrimitiveTypeToWidget(att.type(), AndroidWidgetPreference.SMALLEST) + ") convertView.findViewById(" + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + ");" );
				
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
		println("public View setViewHolderContent(final View convertView, final Object object)");
		println("{");
		FileUtilities.incIndent();
			println("if(object != null)");
			println("{");
			FileUtilities.incIndent();
			
				println("holder = (ViewHolder) convertView.getTag();");
				if(ModelUtilities.isAssociativeClass(theClass)){
					for(MClass cls : ModelUtilities.getAssociativeClassTree_WithOutAssociativeClasses(theClass))
						if(!ModelUtilities.isSpecialPrimitive(cls)){
							for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(cls.allAttributes(), cls.getAnnotation("list").getValues())){
								println("if(holder." + attributeBaseAncestor(cls, att).name() + "_" + att.name() + "View != null)");
								FileUtilities.incIndent();
									List<String> objectGetterCode = getAssociativeGettersCode(theClass, cls);
									for(int i = 0; i < objectGetterCode.size();++i)
										if(i == 0)
											if(att.type().isDate() || (att.type().isObjectType() && (att.type().toString().equals("CalendarDate") || att.type().toString().equals("CalendarTime"))))
												if(objectGetterCode.get(i).contains(cls.name().toLowerCase()))
													print("if(((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + "."  + att.name() + "() != null" );
												else
													print("if(((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + "."  + att.name() + "() != null" );
											else
												print("if(((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null" );
										else
	//										if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
	//											print(" && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + "." + cls.name().toLowerCase() + "()." + att.name() + "() != null" );
	//										else
												print(" && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null" );
										println(")");
									FileUtilities.incIndent();
										println("holder." + attributeBaseAncestor(cls, att).name() + "_" + att.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, att.type(),  "((" + theClass.name() + ") object)" + objectGetterCode.get(objectGetterCode.size() - 1) + "." + att.name() + "()", AndroidWidgetPreference.SMALLEST) + ";" );
									FileUtilities.decIndent();
									println("else" );
									FileUtilities.incIndent();
										println("holder." + attributeBaseAncestor(cls, att).name() + "_" + att.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, att.type(),  "\"null\"", AndroidWidgetPreference.SMALLEST) + ";" );
									FileUtilities.decIndent();
								FileUtilities.decIndent();
							}
						}else{
							println("if(holder." + cls.name() + "_list_" + cls.name() + "View != null)");
							FileUtilities.incIndent();
								List<String> objectGetterCode = getAssociativeGettersCode(theClass, cls);
								for(int i = 0; i < objectGetterCode.size();++i)
									if(i == 0)
										print("if(((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null" );
									else
										print(" && ((" + theClass.name() + ") object)" + objectGetterCode.get(i) + " != null" );
									println(")");
								FileUtilities.incIndent();
									println("holder." + cls.name() + "_list_" + cls.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, cls.type(),  "((" + theClass.name() + ") object)" + objectGetterCode.get(objectGetterCode.size() - 1), AndroidWidgetPreference.SMALLEST) + ";" );
								FileUtilities.decIndent();
								println("else" );
								FileUtilities.incIndent();
									println("holder." + cls.name() + "_list_" + cls.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, cls.type(),  "\"null\"", AndroidWidgetPreference.SMALLEST) + ";" );
								FileUtilities.decIndent();
							FileUtilities.decIndent();
						}
				}else if(ModelUtilities.isSpecialPrimitive(theClass)){
					println("if(holder." + theClass.name() + "_list_" + theClass.name() + "View != null)" );
					FileUtilities.incIndent();
						println("holder." + theClass.name() + "_list_" + theClass.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, theClass.type(),  "((" + theClass.name() + ") object)", AndroidWidgetPreference.SMALLEST) + ";" );
					FileUtilities.decIndent();
				}else
					for (MAttribute att : ModelUtilities.annotationValuesToAttributeOrdered(theClass.allAttributes(), theClass.getAnnotation("list").getValues())){
						print("if(holder." + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + "View != null" );
						if(att.type().isDate() || (att.type().isObjectType() && att.type().toString().equals("Date")))
							print(" && ((" + theClass.name() + ") object)." + att.name() + "() != null");
						println(")");
						FileUtilities.incIndent();
							println("holder." + attributeBaseAncestor(theClass, att).name() + "_" + att.name() + "View." + AndroidTypes.androidWidgetContentSetter(AndroidWidgetsTypes.READ_WIDGET, att.type(),  "((" + theClass.name() + ") object)." + att.name() + "()", AndroidWidgetPreference.SMALLEST) + ";" );
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
//						comando para TESTE - START
						println("Transactions.addSpecialCommand(new Command(" + theClass.name() + "ListViewHolder.class, CommandType.READ, CommandTargetLayer.VIEW, 0, null, null, null, 0, null, null));");
//						comando para TESTE - END						
						println("String message = \"\";");
						for(AssociationInfo ass : AssociationInfo.getAllAssociationsInfo(theClass)){
							MAssociationEnd targetAE = ass.getTargetAE();

							String targetRole = targetAE.name();
				
							MMultiplicity targetMultiplicity = targetAE.multiplicity();
							
							String upperRange = targetMultiplicity.toString();
							String lowerRange = targetMultiplicity.toString();
							if(targetMultiplicity.toString().contains("..")){
								upperRange = targetMultiplicity.toString().split("\\.\\.")[1];
								lowerRange = targetMultiplicity.toString().split("\\.\\.")[0];
							}
							if(upperRange.equals("*")){
								upperRange = "-1";
								if(lowerRange.equals("*"))
									lowerRange = "0";
							}
							switch (ass.getKind())
							{
								case ASSOCIATIVE2MEMBER:
									println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "() == null)");
									FileUtilities.incIndent();
										println("message += \"This " + theClass.name() + " must have at least 1 " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
									FileUtilities.decIndent();
									break;
								case MEMBER2ASSOCIATIVE:
									if(!upperRange.equals("-1") && !upperRange.equals("1")){
										println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() > " + upperRange + ")");
										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " can only have a maximun of " + upperRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
									}
									if(!lowerRange.equals("0")){
										if(lowerRange.equals("1") && upperRange.equals("1"))
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "() == null)");
										else
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() < " + lowerRange + ")");

										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " must have at least " + lowerRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
											
									}
									break;
								case MEMBER2MEMBER:
//									if(!upperRange.equals("-1") && !upperRange.equals("1")){
//										println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() > " + upperRange + ")");
//										FileUtilities.incIndent();
//											println("message += \"This " + theClass.name() + " can only have a maximun of " + upperRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
//										FileUtilities.decIndent();
//									}
//									if(!lowerRange.equals("0")){
//										if(lowerRange.equals("1") && upperRange.equals("1"))
//											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "() == null)");
//										else
//											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() < " + lowerRange + ")");
//
//										FileUtilities.incIndent();
//											println("message += \"This " + theClass.name() + " must have at least " + lowerRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
//										FileUtilities.decIndent();
//											
//									}
									break;
								case ONE2ONE:
									println("message += \"This " + theClass.name() + " must have at least " + lowerRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
									break;
								case ONE2MANY:
									if(!upperRange.equals("-1") && !upperRange.equals("1")){
										println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() > " + upperRange + ")");
										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " can only have a maximun of " + upperRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
									}
									if(!lowerRange.equals("0")){
										if(lowerRange.equals("1") && upperRange.equals("1"))
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "() == null)");
										else
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() < " + lowerRange + ")");

										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " must have at least " + lowerRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
											
									}									break;
								case MANY2MANY:
									if(!upperRange.equals("-1") && !upperRange.equals("1")){
										println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() > " + upperRange + ")");
										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " can only have a maximun of " + upperRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
									}
									if(!lowerRange.equals("0")){
										if(lowerRange.equals("1") && upperRange.equals("1"))
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "() == null)");
										else
											println("if(((" + theClass.name() + ") object)." + ass.getTargetAE().nameAsRolename().toLowerCase() + "().size() < " + lowerRange + ")");

										FileUtilities.incIndent();
											println("message += \"This " + theClass.name() + " must have at least " + lowerRange + " " + ass.getTargetAE().nameAsRolename().toLowerCase() + "\\n\";");
										FileUtilities.decIndent();
											
									}									break;
								default:
									break;
							}
						}
						println("if(convertView.isActivated())");
						FileUtilities.incIndent();
							println("UtilNavigate.showWarning(" + MainMemoryClass + ".getActiveActivity(), \"Constraints not met\", message);");
						FileUtilities.decIndent();
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
	
	public List<String> getAssociativeGettersCode(MClass caller, MClass target){
		List<AssociationInfo> objectGetter = getAssociativeGetter(caller, target, new ArrayList<AssociationInfo>());
		List<String> result = new ArrayList<String>();
		String temp = "";
		for(int  i = objectGetter.size() - 1;i >= 0;i--){
			temp = temp + "." + objectGetter.get(i).getTargetAE() + "()";

			result.add(temp);
		}
		return result;
		
	}
	
	public List<AssociationInfo> getAssociativeGetter(MClass caller, MClass target, List<AssociationInfo> list){
		for(AssociationInfo ass : AssociationInfo.getAssociationsInfo(caller)){
			if(ModelUtilities.isAssociativeClass(ass.getTargetAEClass()) && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
				getAssociativeGetter(ass.getTargetAEClass(), target, list);
				if(list.size() > 0 && list.get(0).getTargetAEClass() == target)
					list.add(ass);
			}
			if(ass.getTargetAEClass() == target && ass.getKind() == AssociationKind.ASSOCIATIVE2MEMBER){
//				if(list.size() > 0)
//					list.add(list.get(list.size() - 1) + "." + ass.getTargetAE().name().toLowerCase() + "()");
//				else
					list.add(ass);
				return list;
			}
			
		}
		return list;
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
