package org.quasar.use2android.api.implementation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.quasar.juse.api.implementation.AssociationInfo;
import org.quasar.juse.api.implementation.AssociationKind;
import org.quasar.juse.api.implementation.AttributeInfo;
import org.quasar.juse.api.implementation.JavaTypes;
import org.quasar.juse.api.implementation.ModelUtilities;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MInvalidModelException;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MOperation;
import org.tzi.use.uml.mm.ModelFactory;
import org.tzi.use.uml.ocl.expr.VarDecl;
import org.tzi.use.uml.ocl.type.EnumType;
import org.tzi.use.uml.ocl.type.Type;
import org.tzi.use.uml.ocl.type.TypeFactory;
import org.tzi.use.util.StringUtil;

public class AndroidPersistenceVisitor extends PersistenceVisitor{

	private MModel			model;
	private String			author;
	private String			basePackageName;
	private String			businessLayerName;
	private String			persistenceLayerName;
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
	public AndroidPersistenceVisitor(MModel model, String author, String basePackageName, String businessLayerName,
					String persistenceLayerName)
	{
		this.model = model;
		this.author = author;
		this.basePackageName = basePackageName;
		this.businessLayerName = businessLayerName;
		this.persistenceLayerName = persistenceLayerName;
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
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printAttributes(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printAttributes()
	{
		println("private static ObjectContainer oc = null;");
		println("private static Context context;");
		println("private final static String DataBaseName = \"Database\";");
		println("private final static String DataBaseExtension = \".db4o\";");
		println();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.use.api.implementation.IJavaVisitor#printClassHeader(org.tzi.use.uml.mm.MClass)
	 */
	@Override
	public void printClassHeader(String name, String layerName, String businessLayer, List<MClass> domainClasses)
	{
		printFileHeader(name, layerName);

		printImports(businessLayer, domainClasses);
		
		print("public class Database");
		println("{");
	}

	/***********************************************************
	 * @param theClass
	 ***********************************************************/
	private void printImports(String businessLayer, List<MClass> domainClasses)
	{

		Set<String> imports = new HashSet<String>();
		imports.add("import java.io.File;");
		imports.add("import java.io.IOException;");
		imports.add("import java.util.Collection;");
		imports.add("import java.util.HashSet;");
		imports.add("import java.util.Set;");
		imports.add("import android.content.Context;");
		
		for(MClass cls : domainClasses)
			imports.add("import " + basePackageName + "." + businessLayer + "." + cls.name() + ";");
		
		imports.add("import com.db4o.Db4oEmbedded;");
		imports.add("import com.db4o.ObjectContainer;");
		imports.add("import com.db4o.ObjectSet;");
		imports.add("import com.db4o.config.AndroidSupport;");
		imports.add("import com.db4o.config.EmbeddedConfiguration;");
		imports.add("import com.db4o.cs.Db4oClientServer;");
		imports.add("import com.db4o.cs.config.ClientConfiguration;");
		imports.add("import com.db4o.query.Query;");
		
		for (String importDeclaration : imports)
			println(importDeclaration);
		println();
	}

	@Override
	public void printAllInstances()
	{
		println();
		println("/***********************************************************");
		println("* @return all instances of the given class");
		println("***********************************************************/");
		print("public synchronized static <T, Y> Set<T> allInstances(Class<Y> prototype)");
		println("{");
		incIndent();
		println("return new HashSet<T>((Collection<? extends T>)OpenDB().query(prototype));");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* @return all instances of the given class in an ordered (by modification) manner");
		println("***********************************************************/");
		print("public synchronized static <T, Y> ObjectSet<T> allInstancesOrdered(Class<Y> prototype)");
		println("{");
		incIndent();
		println("return (ObjectSet<T>) OpenDB().query(prototype);");
		decIndent();
		println("}");
		println();
	}

	@Override
	public void printDefaultDBMethods() {
		println();
		println("/***********************************************************");
		println("* Create, open and close the database");
		println("***********************************************************/");
		print("public synchronized static ObjectContainer OpenDB()");
		println("{");
		incIndent();
			println("try{");
			incIndent();
				println("if (oc == null || oc.ext().isClosed())");
				incIndent();
					println("oc = Db4oEmbedded.openFile(dbConfig(), db4oDBFullPath(context));");
				decIndent();
				println("return oc;");
			decIndent();
			println("} catch (Exception ie) {");
			incIndent();
				println("return null;");
			decIndent();
			println("}");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* return the app context");
		println("***********************************************************/");
		print("public synchronized Context getContext()");
		println("{");
		incIndent();
			println("return context;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* sets the app context");
		println("***********************************************************/");
		print("public synchronized static void setContext(Context context)");
		println("{");
		incIndent();
			println("Database.context = context;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns the database android path");
		println("***********************************************************/");
		print("public static String db4oDBFullPath(Context ctx)");
		println("{");
		incIndent();
			println("return ctx.getDir(\"data\", Context.MODE_PRIVATE) + \"/\" + DataBaseName + DataBaseExtension;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* Deletes the database");
		println("***********************************************************/");
		print("public static synchronized void DeleteDB()");
		println("{");
		incIndent();
			println("new File(db4oDBFullPath(context)).delete();");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* Closes the database");
		println("***********************************************************/");
		print("public synchronized static void close()");
		println("{");
		incIndent();
			println("if (oc != null)");
			incIndent();
				println("oc.close();");
			decIndent();
		decIndent();
		println("}");
		println();
		
	}

	@Override
	public void printDefaultDBConfigMethods(List<MClass> list) {
		int updateDepth = 1;
		//preciso de ver se preciso de fazer para as hierarquias - ou seja - se o updateDepth é maior que 2

		println("/***********************************************************");
		println("* returns the server database configuration");
		println("***********************************************************/");
		print("public static ClientConfiguration dbServerConfig()");
		println("{");
		incIndent();
			println("ClientConfiguration configuration = Db4oClientServer.newClientConfiguration();");
			for(MClass theClass : list){
				updateDepth = 1;
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
				for(MAttribute att : theClass.allAttributes())
					if(att.type().isObjectType())
						updateDepth = 2;
				println("configuration.common().objectClass(" + theClass.name() + ".class).objectField(\"ID\").indexed(true);");
				println("configuration.common().objectClass(" + theClass.name() + ".class).updateDepth(" + updateDepth + ");");
			}
			
			println("return configuration;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns the local database configuration");
		println("***********************************************************/");
		print("public static EmbeddedConfiguration dbConfig()");
		println("{");
		incIndent();
			println("EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();");
			println("configuration.common().add(new AndroidSupport());");
			println("configuration.file().lockDatabaseFile(false);");
			for(MClass theClass : list){
				updateDepth = 1;
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
				for(MAttribute att : theClass.allAttributes())
					if(att.type().isObjectType())
						updateDepth = 2;
				println("configuration.common().objectClass(" + theClass.name() + ".class).objectField(\"ID\").indexed(true);");
				println("configuration.common().objectClass(" + theClass.name() + ".class).updateDepth(" + updateDepth + ");");
			}
			println("return configuration;");
		decIndent();
		println("}");
		println();
		
	}

	@Override
	public void printGetters() {
		println("/***********************************************************");
		println("* returns an object based on type (class), a field name and respective object constraint");
		println("***********************************************************/");
		print("public synchronized static <T> T get(Class<T> c, String fieldName, Object constraint)");
		println("{");
		incIndent();
			println("Query q = OpenDB().query();");
			println("q.constrain(c);");
			println("q.descend(fieldName).constrain(constraint);");
			println("ObjectSet<T> result = q.execute();");
			println("if (result.hasNext())");
			incIndent();
				println("return result.next();");
			decIndent();
			println("return null;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns an object based on type (class), a field name and respective string constraint");
		println("***********************************************************/");
		print("public synchronized static <T> T get(Class<T> c, String fieldName, String constraint)");
		println("{");
		incIndent();
			println("Query q = OpenDB().query();");
			println("q.constrain(c);");
			println("q.descend(fieldName).constrain(constraint);");
			println("ObjectSet<T> result = q.execute();");
			println("if (result.hasNext())");
			incIndent();
				println("return result.next();");
			decIndent();
			println("return null;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns an object based on type (class), and constraint based on the field 'ID' ");
		println("***********************************************************/");
		print("public synchronized static <T> T get(Class<T> c, int constraint)");
		println("{");
		incIndent();
			println("Query q = OpenDB().query();");
			println("q.constrain(c);");
			println("q.descend(\"ID\").constrain(constraint);");
			println("ObjectSet<T> result = q.execute();");
			println("if (result.hasNext())");
			incIndent();
				println("return result.next();");
			decIndent();
			println("return null;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns an object based on type (class), constraint based on the field 'ID', and session");
		println("***********************************************************/");
		print("public synchronized static <T> T get(ObjectContainer oc, Class<T> c, int constraint)");
		println("{");
		incIndent();
			println("Query q = oc.query();");
			println("q.constrain(c);");
			println("q.descend(\"ID\").constrain(constraint);");
			println("ObjectSet<T> result = q.execute();");
			println("if (result.hasNext())");
			incIndent();
				println("return result.next();");
			decIndent();
			println("return null;");
		decIndent();
		println("}");
		println();
		
		println("/***********************************************************");
		println("* returns an object based on an example object");
		println("***********************************************************/");
		print("public static Object get(Object object)");
		println("{");
		incIndent();
			println("return OpenDB().queryByExample(object);");
		decIndent();
		println("}");
		println();
		
	}

}
