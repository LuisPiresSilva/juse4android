/***********************************************************
 * Filename: MainExample.java
 * Created:  24 de Mar de 2012
 ***********************************************************/
package org.quasar.use2android;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.quasar.use2android.api.implementation.PrototypeGeneratorFacade;

/***********************************************************
 * @author fba 24 de Mar de 2012
 * 
 ***********************************************************/
public final class Prototype_CodeGeneration
{
	//note: change the RUN_JAR boolean to false if working in a java environment (i.e. this class is the main class)
	//change it to true before exporting/creating the executable jar file
	private static boolean	RUN_JAR = true;
	
	private static String	USE_PROJECT = "USE3.0.6";
	private static String	JUSE4ANDROIDPROJECT = "JUSE4Android";
	
	private static String	USE_BASE_DIRECTORY	= "";
	
	private static String	Authors				= "Luís Pires da Silva and Fernando Brito e Abreu";
	
	private static String	SOURCE_WORKSPACE	= "";
	private static String	TARGET_WORKSPACE	= "";
	
	private static String	AndroidProjectName	= "";
	private static String	ServerProjectName	= "";
	
	private final static String	BUSINESSLAYER_NAME		= "businessLayer";
	private final static String	PRESENTATIONLAYER_NAME	= "presentationLayer";
	private final static String	PERSISTENCELAYER_NAME	= "persistenceLayer";

	private final static String LIBRARY_DIRECTORY 	= "res/use2android/libs";
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
	
	//used only in executable jar
	private static String getJarFolder() {
	    String name = Prototype_CodeGeneration.class.getName().replace('.', '/');
	    String s = Prototype_CodeGeneration.class.getResource("/" + name + ".class").toString();
	    s = s.replace('/', File.separatorChar);
	    s = s.substring(0, s.indexOf(".jar")+4);
	    s = s.substring(s.lastIndexOf(':')-1);
	    return s.substring(0, s.lastIndexOf(File.separatorChar)+1);
	}
	
	//used only in executable jar
	private static String getSourceFolder() {
	    String name = Prototype_CodeGeneration.class.getName().replace('.', '/');
	    String s = Prototype_CodeGeneration.class.getResource("/" + name + ".class").toString();
	    s = s.replace('/', File.separatorChar);
	    s = s.substring(0, s.indexOf(JUSE4ANDROIDPROJECT)+4);
	    s = s.substring(s.lastIndexOf(':')-1);
	    return s.substring(0, s.lastIndexOf(File.separatorChar)+1);
	} 
	/***********************************************************
	 * @param args
	 * @throws InterruptedException
	 ***********************************************************/
	public static void main(String[] args) throws InterruptedException
	{	
		
		
		
		
		PrototypeGeneratorFacade api = new PrototypeGeneratorFacade();
		if(RUN_JAR){
			try {
				USE_BASE_DIRECTORY = URLDecoder.decode(getJarFolder() + USE_PROJECT, "utf-8");
				SOURCE_WORKSPACE = URLDecoder.decode(getJarFolder(), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			USE_BASE_DIRECTORY = new File(USE_BASE_DIRECTORY).getPath();

			if(args.length > 0 && !args[0].isEmpty() && !args[1].isEmpty()){
				TARGET_WORKSPACE		= args[0];
				MODEL_DIRECTORY = args[1].substring(0 ,args[1].lastIndexOf("\\"));
				MODEL_FILE = args[1].substring(args[1].lastIndexOf("\\") + 1, args[1].length());
				if(args.length > 2 && !args[2].equals("empty"))
					AndroidProjectName = args[2];
				if(args.length > 3 && !args[3].equals("empty"))
					ServerProjectName = args[3];
				if(args.length > 4 && !args[4].equals("empty"))
					USER = args[4];
				if(args.length > 5 && !args[5].equals("empty"))
					PASS = args[5];
				if(args.length > 6 && !args[6].equals("empty"))
					PORT = args[6];
				if(args.length > 7 && !args[7].equals("empty"))
					IP = args[7];
				
				api.initialize(new String[0], USE_BASE_DIRECTORY, MODEL_DIRECTORY);
			}
		}else{
			try {
				USE_BASE_DIRECTORY = URLDecoder.decode(getSourceFolder() + USE_PROJECT, "utf-8");
				SOURCE_WORKSPACE = URLDecoder.decode(getSourceFolder() + JUSE4ANDROIDPROJECT, "utf-8");
				TARGET_WORKSPACE = URLDecoder.decode(getSourceFolder(), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			USE_BASE_DIRECTORY = new File(USE_BASE_DIRECTORY).getPath();

			
			
			
			
			api.initialize(args, USE_BASE_DIRECTORY, MODEL_DIRECTORY);
		}
		
		System.out.println("\nUse model to Android tool for USE version 3.0.6, Copyright (C) 2012-2013 QUASAR Group\n");
		
		System.out.println("Choosed Settings:");
		System.out.println("\tTarget path: " + TARGET_WORKSPACE);
		System.out.println("\tUSE Model: " + MODEL_FILE);
		System.out.println("\n");
		
		api.compileSpecification(MODEL_FILE);
		
		api.androidGeneration(Authors, AndroidProjectName, SOURCE_WORKSPACE, TARGET_WORKSPACE, TARGET_PACKAGE, BUSINESSLAYER_NAME, 
								PRESENTATIONLAYER_NAME, PERSISTENCELAYER_NAME, LIBRARY_DIRECTORY, DB4O_CORE_JAR,DB4O_CS_JAR,DB4O_OPTIONAL_JAR,
								USER, PASS, PORT, IP);
		
		api.serverGeneration(Authors, ServerProjectName, SOURCE_WORKSPACE, TARGET_WORKSPACE, TARGET_PACKAGE, LIBRARY_DIRECTORY,
								DB4O_CORE_JAR,DB4O_CS_JAR,DB4O_OPTIONAL_JAR, USER, PASS, PORT, IP);
	}
}