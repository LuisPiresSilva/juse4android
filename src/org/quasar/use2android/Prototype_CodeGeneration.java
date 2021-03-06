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
	private static boolean	RUN_JAR = false;
	
	private static String	USE_PROJECT = "USE3.0.6";//fill here (check your USE version)
	private static String	JUSE4ANDROIDPROJECT = "JUSE4Android_git";//fill here (project name/folder)
	
	private static String	USE_BASE_DIRECTORY	= "";
	
	private static String	Authors				= "Lu�s Pires da Silva and Fernando Brito e Abreu";
	
	private static String	SOURCE_WORKSPACE	= "";
	private static String	TARGET_WORKSPACE	= "C:\\D\\WorkSpace\\Eclipse mestrado";//fill here
	
	private static String	AndroidProjectName	= "";//fill here
	private static String	ServerProjectName	= "";//fill here
	
	private final static String	BUSINESSLAYER_NAME		= "businessLayer";
	private final static String	PRESENTATIONLAYER_NAME	= "presentationLayer";
	private final static String	PERSISTENCELAYER_NAME	= "persistenceLayer";

	private final static String LIBRARY_DIRECTORY 	= "res/use2android/libs";
	private final static String	DB4O_CORE_JAR		= "db4o-8.0.249.16098-core-java5.jar";
	private final static String	DB4O_CS_JAR			= "db4o-8.0.249.16098-cs-java5.jar";
	private final static String	DB4O_OPTIONAL_JAR	= "db4o-8.0.249.16098-optional-java5.jar";
	
	private static String	MODEL_DIRECTORY		= "";//fill here
	private static String	MODEL_FILE			= ".use";//fill here
	private static String	TARGET_PACKAGE 		= "org.quasar.";//fill here (optional)
	
//	Server Information
	private static String	DATABASE	= "database";//fill here (optional)
	private static String	USER		= "db4o";//fill here if used
	private static String	PASS		= "db4o";//fill here if used
	private static String	PORT 		= "4444";//fill here if used
	private static String	IP			= "80.172.235.96";//fill here if used
	
	//used only in executable jar
	private static String getJarFolder() {
	    String name = Prototype_CodeGeneration.class.getName().replace('.', '/');
	    String s = Prototype_CodeGeneration.class.getResource("/" + name + ".class").toString();
	    s = s.replace('/', File.separatorChar);
	    s = s.substring(0, s.indexOf(".jar")+4);
	    s = s.substring(s.lastIndexOf(':')-1);
	    return s.substring(0, s.lastIndexOf(File.separatorChar)+1);
	}
	
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
		
		if(RUN_JAR){
			try {
				USE_BASE_DIRECTORY = URLDecoder.decode(getJarFolder() + USE_PROJECT, "utf-8");
				SOURCE_WORKSPACE = URLDecoder.decode(getJarFolder(), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			USE_BASE_DIRECTORY = new File(USE_BASE_DIRECTORY).getPath();
//			System.out.println(USE_BASE_DIRECTORY);
		}else{
			try {
				USE_BASE_DIRECTORY = URLDecoder.decode(getSourceFolder() + USE_PROJECT, "utf-8");
				SOURCE_WORKSPACE = URLDecoder.decode(getSourceFolder() + JUSE4ANDROIDPROJECT, "utf-8");
				if(TARGET_WORKSPACE.equals(""))
					TARGET_WORKSPACE = URLDecoder.decode(getSourceFolder(), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			USE_BASE_DIRECTORY = new File(USE_BASE_DIRECTORY).getPath();
		}
		
		
		PrototypeGeneratorFacade api = new PrototypeGeneratorFacade();
		
		if(args.length > 0 && !args[0].isEmpty() && !args[1].isEmpty()){
			try {
				TARGET_WORKSPACE	= URLDecoder.decode(args[0], "utf-8");
				MODEL_DIRECTORY = URLDecoder.decode(args[1].substring(0 ,args[1].lastIndexOf("\\")), "utf-8");
				MODEL_FILE = URLDecoder.decode(args[1].substring(args[1].lastIndexOf("\\") + 1, args[1].length()), "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
		}else{
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
								DATABASE, USER, PASS, PORT, IP);
		
		api.serverGeneration(Authors, ServerProjectName, SOURCE_WORKSPACE, TARGET_WORKSPACE, TARGET_PACKAGE, LIBRARY_DIRECTORY,
								DB4O_CORE_JAR,DB4O_CS_JAR,DB4O_OPTIONAL_JAR, DATABASE, USER, PASS, PORT, IP);
	}
}