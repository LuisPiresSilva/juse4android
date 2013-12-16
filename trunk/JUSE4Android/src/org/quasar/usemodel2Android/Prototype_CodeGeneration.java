/***********************************************************
 * Filename: MainExample.java
 * Created:  24 de Mar de 2012
 ***********************************************************/
package org.quasar.usemodel2Android;

import java.io.IOException;

import org.quasar.juse.api.JUSE_PrototypeGeneratorFacade;
import org.quasar.use2android.api.implementation.PrototypeGeneratorFacade;

/***********************************************************
 * @author fba 24 de Mar de 2012
 * 
 ***********************************************************/
public final class Prototype_CodeGeneration
{
	private final static String	USE_BASE_DIRECTORY	= "C:\\D\\WorkSpace\\Eclipse mestrado\\USE 3.0.6";
	
	private static String	Authors				= "Luís Pires da Silva and Fernando Brito e Abreu";
	
	private static String	JAVA_WORKSPACE				= "";
	
	private static String	AndroidProjectName	= "";
	private static String	ServerProjectName	= "";
	
	private final static String	BUSINESSLAYER_NAME		= "businessLayer";
	private final static String	PRESENTATIONLAYER_NAME	= "presentationLayer";
	private final static String	PERSISTENCELAYER_NAME	= "persistenceLayer";

	private final static String LIBRARY_DIRECTORY 	= "lib";
	private final static String	DB4O_CORE_JAR		= "db4o-8.0.249.16098-core-java5.jar";
	private final static String	DB4O_CS_JAR			= "db4o-8.0.249.16098-cs-java5.jar";
	private final static String	DB4O_OPTIONAL_JAR	= "db4o-8.0.249.16098-optional-java5.jar";
	
	private static String	MODEL_DIRECTORY		= "";
	private static String	MODEL_FILE			= "";
	private static String	TARGET_PACKAGE 		= "org.quasar.";
	
//	Server Information
	private static String	USER	= "db4o";
	private static String	PASS	= "db4o";
	private static String	PORT 	= "4444";	
	private static String	IP		= "0.0.0.0";
	
	
	/***********************************************************
	 * @param args
	 * @throws InterruptedException
	 ***********************************************************/
	public static void main(String[] args) throws InterruptedException
	{	 		
		PrototypeGeneratorFacade api = new PrototypeGeneratorFacade();
		if(args.length > 0 && !args[0].isEmpty() && !args[1].isEmpty()){
			JAVA_WORKSPACE		= args[0];
			MODEL_DIRECTORY = args[1].substring(0 ,args[1].lastIndexOf("\\"));
			MODEL_FILE = args[1].substring(args[1].lastIndexOf("\\") + 1, args[1].length());
			if(!args[2].equals("empty"))
				AndroidProjectName = args[2];
			if(!args[3].equals("empty"))
				ServerProjectName = args[3];
			if(!args[4].isEmpty())
				USER = args[4];
			if(!args[5].isEmpty())
				PASS = args[5];
			System.out.println(args.length);
			if(!args[6].isEmpty())
				PORT = args[6];

			IP = args[7];
			api.initialize(new String[0], USE_BASE_DIRECTORY, MODEL_DIRECTORY);
		}else
			api.initialize(args, USE_BASE_DIRECTORY, MODEL_DIRECTORY);

		api.compileSpecification(MODEL_FILE);
		
		api.androidGeneration(Authors, AndroidProjectName, JAVA_WORKSPACE, TARGET_PACKAGE, BUSINESSLAYER_NAME, 
								PRESENTATIONLAYER_NAME, PERSISTENCELAYER_NAME, LIBRARY_DIRECTORY, DB4O_CORE_JAR,DB4O_CS_JAR,DB4O_OPTIONAL_JAR,
								USER, PASS, PORT, IP);
		
		api.serverGeneration(Authors, ServerProjectName, JAVA_WORKSPACE, TARGET_PACKAGE, LIBRARY_DIRECTORY,
								DB4O_CORE_JAR,DB4O_CS_JAR,DB4O_OPTIONAL_JAR, USER, PASS, PORT, IP);
	}
}