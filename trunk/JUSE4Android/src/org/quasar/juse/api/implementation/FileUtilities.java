package org.quasar.juse.api.implementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.quasar.use2android.api.DynamicJavaSourceCodeObject;

public abstract class FileUtilities
{
	private static final String	IDENTATOR	= "\t";
	private static int			fIndent		= 0;
	private static int			fIndentStep	= 1;
	private static String		buffer		= "";
	private static PrintWriter	fOut		= null;
	private static boolean		fileopened	= false;

	/***********************************************************
	 * @param directoryname
	 *            The name of the directory to be created
	 ***********************************************************/
	public static void createDirectory(String directoryname)
	{
		if (new File(directoryname).exists())
			return;
		else
			try
			{
				// Create multiple directories
				if ((new File(directoryname)).mkdirs())
					System.out.println("Directory: " + directoryname + " created!");
			}
			catch (Exception e)
			{
				System.out.println("ERROR: Package directories " + directoryname
								+ " could not be created. Check directory naming convention, priviledges or disk quota.");
				e.printStackTrace();
			}
	}

	/***********************************************************
	 * @param sourceFilename
	 * @param destFilename
	 * @throws IOException
	 ***********************************************************/
	@SuppressWarnings("resource")
	public static void copyFile(String sourceFilename, String destFilename)
	{
		File sourceFile = new File(sourceFilename);
		File destFile = new File(destFilename);

//		 System.out.println("Copying file " + sourceFilename + " to " + destFilename);

		if (!sourceFile.exists())
		{
			System.out.println("ERROR: Source file " + sourceFilename + " does not exist!");
			return;
		}

		try
		{
			if (!destFile.exists())
			{
				destFile.createNewFile();
			}
			FileChannel source = null;
			FileChannel destination = null;
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();

			if (destination != null && source != null)
				destination.transferFrom(source, 0, source.size());

			if (source != null)
				source.close();

			if (destination == null)
				System.out.println("ERROR: Destination file " + destFilename + " was not created!");
			else
				destination.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/***********************************************************
	 * @param directoryname
	 *            The name of the directory to be removed
	 ***********************************************************/
	public static void removeDirectory(String directoryname)
	{
		deleteFolder(new File(directoryname));
	}
	
	/***********************************************************
	 * @param directoryname
	 *            The name of the directory to be removed
	 ***********************************************************/
	public static void deleteFile(String filename)
	{
		File file = new File(filename);
		if(file.isFile() && file.exists())
			file.delete();
	}

	private static void deleteFolder(File folder)
	{
		File[] files = folder.listFiles();
		if (files != null)
		{ // some JVMs return null for empty dirs
			for (File f : files)
			{
				if (f.isDirectory())
				{
					deleteFolder(f);
				}
				else
				{
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/***********************************************************
	 * @param directoryname
	 *            The name of the directory where the file to open is placed
	 * @param classname
	 *            The name of the file to open
	 ***********************************************************/
	public static boolean openOutputFile(String directoryname, String filename)
	{
		String file = directoryname + "/" + filename;
		boolean result = true;
		try
		{
			if (fOut != null)
				fOut.close();
			File f = new File(file);
//			if (f.exists())
//			{
//				JFrame frame = new JFrame();
//				int answer = JOptionPane.showConfirmDialog(frame, "The file " + filename
//								+ " already exists!\nDo you want to overwrite it?", "WARNING", JOptionPane.YES_NO_OPTION);
//				frame.dispose();
//				if (answer == JOptionPane.YES_OPTION)
//					fOut = new PrintWriter(new FileWriter(file));
//				else
//					result = false;
//			}
//			else
				fOut = new PrintWriter(new FileWriter(file));
		}
		catch (IOException e)
		{
			createDirectory(directoryname);
			openOutputFile(directoryname, filename);
		}
		return result;
	}

	//not used
	/***********************************************************
	 * @param directoryname
	 *            The name of the directory where the file to open is placed
	 * @param classname
	 *            The name of the file to open
	 ***********************************************************/
	public static boolean openOutputForExistingFile(String directoryname, String filename)
	{
		String file = directoryname + "/" + filename;
		boolean result = true;
		if (fOut != null)
			fOut.close();
		try
		{
			File f = new File(file);
			fileopened = true;
			sourceCode = new StringBuffer();
			destinationfile = f;
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				sourceCode.append(line + "\r\n");
			}
			reader.close();
			
			fOut = new PrintWriter(new FileWriter(file));
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return result;
	}
	
	/***********************************************************
	* @param filename
	* @param oldString
	* @param newString
	***********************************************************/
	public static void replaceStringInFile(String filename, String oldString, String newString)
	{
		try
		{
			File file = new File(filename);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "", oldtext = "";
			while ((line = reader.readLine()) != null)
			{
				oldtext += line + "\r\n";
			}
			reader.close();

			String newtext = oldtext.replaceAll(oldString, newString);

			FileWriter writer = new FileWriter(filename);
			writer.write(newtext);
			writer.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	//not used
	/***********************************************************
	 * @param sourceFilename
	 * @param destFilename
	 * @throws IOException
	 ***********************************************************/
	@SuppressWarnings("resource")
	public static void copyAndCompileFile(String sourcePath, String sourceFile, String destPath, String filename)
	{
		FileUtilities.copyFile(sourcePath + sourceFile, destPath + filename + ".java");
		
//		String packag = "org" + destPath.split("org")[1];
//		packag = packag.replace("/" , ".");
//		System.out.println(packag);
		String fixedpath = sourcePath.replace("\\" , "/");
		sourcepath = fixedpath;
		System.out.println(fixedpath);
		try
		{
			File sourcefile = new File(destPath + filename + ".java");
//			File destinationfile = new File(path + destinationClass + ".java");
			filetocompile = fixedpath + filename;
			sourceCode = new StringBuffer();
			
			BufferedReader reader = new BufferedReader(new FileReader(sourcefile));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				sourceCode.append(line.toString() + "\r\n");
			}
			reader.close();
			
			doCompilation();
			filetocompile = "";
			sourceCode = null;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
//	estou a fazer isto mas penso que nao va utilizar no futuro e mais uma experienciazita - //not used
	/***********************************************************
	* @param path
	* @param name of source class
	* @param name of destination class
	* @param name of method
	* @param parameters of method
	***********************************************************/
	public static void openMethodForInput(String path, String sourceClass, String destinationClass, String methodname, Class<?>... param)
	{
//		String packag = "org" + path.split("org")[1];
//		packag = packag.replace("\\" , ".");
		String fixedpath = path.replace("\\" , "/");

		Class<?> fileClass = null;
		Method method = null;
		try {
			URL[] urls = new URL[1];
			try {
				urls[0] = new File(fixedpath + sourceClass).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			ClassLoader loader = URLClassLoader.newInstance(urls);
			try {
				fileClass = loader.loadClass("Database");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			method = fileClass.getDeclaredMethod(methodname, param);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		if(method != null){
			try
			{
				fileopened = true;
				forMethod = true;
				File sourcefile = new File(fixedpath + sourceClass + ".java");
				destinationfile = new File(fixedpath + destinationClass + ".java");
				filetocompile = fixedpath + destinationClass;
				sourcepath = fixedpath;
				
				BufferedReader reader = new BufferedReader(new FileReader(sourcefile));
				sourceCode = new StringBuffer();
				sourceCodeEnd = new StringBuffer();
				
				String line = "";
				boolean isLastViablePosition = false;
				StringBuffer endMethodCode = new StringBuffer();
				boolean starteddMethod = false;
				boolean endedMethod = false;
				int openbrakets = 0;
				int closebrakets = 0;
				int lastViablePositionIndex = 0;
				boolean endOfMethodLastChanges = false;
				
				if(param != null){
					
				}
				
				while ((line = reader.readLine()) != null)
				{
					//falta verificar os param
					if(!starteddMethod)
						sourceCode.append(line.toString() + "\r\n");
					
					if(!endedMethod && (line.toString().contains(method.getName()) || starteddMethod)){
						if(starteddMethod)
							sourceCode.append(line.toString() + "\r\n");
						for(Character x : line.toCharArray()){
							if(x == '{'){
								starteddMethod = true;
								openbrakets++;
							}
							if(x == '}')
								closebrakets++;
						}
						if(!method.getReturnType().equals(Void.TYPE))
							if(line.toString().contains("return")){
								endMethodCode = new StringBuffer();
								isLastViablePosition = true;
							}
						if(isLastViablePosition){
							endMethodCode.append(line.toString() + "\r\n");
							lastViablePositionIndex = sourceCode.lastIndexOf("return");
							
						}
					}
					
					//end of method
					if(starteddMethod && (openbrakets - closebrakets) == 0){
						endedMethod = true;
						if(!endOfMethodLastChanges){
							sourceCode.replace(lastViablePositionIndex, sourceCode.length(), " ");
							sourceCodeEnd.append(endMethodCode);
							endOfMethodLastChanges = true;
						}else
							sourceCodeEnd.append(line.toString() + "\r\n");
					}
				}
				reader.close();


			}
			catch (IOException ioe)
			{
				fileopened = false;
				filetocompile = "";
				sourcepath = "";
				sourceCode = null;
				ioe.printStackTrace();
			}
		}
	}
	
	private static boolean forMethod = false;
	private static File destinationfile;
	private static String filetocompile = "";
	private static StringBuffer sourceCode;
	private static StringBuffer sourceCodeEnd;
	private static String sourcepath;
	
	//not used
	private static void doCompilation (){
        /*Creating dynamic java source code file object*/
        SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject (filetocompile, sourceCode.toString()) ;
        JavaFileObject javaFileObjects[] = new JavaFileObject[]{fileObject} ;
 
        /*Instantiating the java compiler*/
        JavaCompiler compiler = null;
		try {
			compiler = (JavaCompiler) Class.forName("com.sun.tools.javac.api.JavacTool").newInstance();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e1) {
			e1.printStackTrace();
		}
        /**
         * Retrieving the standard file manager from compiler object, which is used to provide
         * basic building block for customizing how a compiler reads and writes to files.
         *
         * The same file manager can be reopened for another compiler task.
         * Thus we reduce the overhead of scanning through file system and jar files each time
         */
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
 
        /* Prepare a list of compilation units (java source code file objects) to input to compilation task*/
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFileObjects);
 
        /*Prepare any compilation options to be used during compilation*/
        //In this example, we are asking the compiler to place the output files under bin folder.
        String[] compileOptions = new String[]{"-d", sourcepath} ;
        Iterable<String> compilationOptionss = Arrays.asList(compileOptions);
 
        /*Create a diagnostic controller, which holds the compilation problems*/
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
 
        /*Create a compilation task from compiler by passing in the required input objects prepared above*/
        CompilationTask compilerTask = compiler.getTask(null, stdFileManager, diagnostics, compilationOptionss, null, compilationUnits) ;
 
        //Perform the compilation by calling the call method on compilerTask object.
        boolean status = compilerTask.call();
 
        if (!status){//If compilation error occurs
            /*Iterate through each compilation problem and print it*/
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()){
//                System.out.format("Error on line %d in %s", diagnostic.getLineNumber(), diagnostic);
            }
        }
        try {
            stdFileManager.close() ;//Close the file manager
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/***********************************************************
	 * Close the current opened file
	 ***********************************************************/
	public static void closeMethodForInput()
	{
		if (sourceCode != null)
		{
			sourceCode.append(sourceCodeEnd);
			
			FileWriter writer;
			try {
				writer = new FileWriter(destinationfile);
				writer.write(sourceCode.toString());
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			doCompilation();
			destinationfile = null;
			filetocompile = "";
			sourcepath = "";
			sourceCode = null;
			sourceCodeEnd = null;
			forMethod= false;
			
		}
		fileopened = false;
	}
	
	public static boolean contains(String content)
	{
		if (fileopened)
		{
			if(sourceCode.toString().contains(content))
				return true;
			else
				return false;
		}
		return false;
	}
	
	/***********************************************************
	 * Close the current opened file
	 ***********************************************************/
	public static void closeOutputForExistingFile()
	{
		if (fileopened)
		{
			FileWriter writer;
			try {
				writer = new FileWriter(destinationfile);
				writer.write(sourceCode.toString());
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			sourceCode = null;
			fileopened = false;
		}
	}
	
	/***********************************************************
	 * Close the current opened file
	 ***********************************************************/
	public static void closeOutputFile()
	{
		if (fOut != null)
		{
			fOut.flush();
			fOut.close();
		}
	}

	/***********************************************************
	 * @param slist
	 *            List of strings to be printed, followed by a newline
	 ***********************************************************/
	public static void println(String... slist)
	{
		if(fileopened){
			if(forMethod){
				incIndent();
				incIndent();
			}
			for (int i = 0; i < fIndent; i++)
				sourceCode.append(IDENTATOR );
			sourceCode.append(buffer);
			for (String s : slist)
				sourceCode.append(s);
			sourceCode.append("\n");
			buffer = "";
			if(forMethod){
				decIndent();
				decIndent();
			}
		}else{
			for (int i = 0; i < fIndent; i++)
				fOut.print(IDENTATOR);
			fOut.print(buffer);
			for (String s : slist)
				fOut.print(s);
			fOut.println();
			buffer = "";
		}
	}

	/***********************************************************
	 * @param s
	 *            String to be printed as a comment, followed by a newline
	 ***********************************************************/
	public static void printlnc(String s)
	{
		println("//" + IDENTATOR + s);
	}

	/***********************************************************
	 * @param s
	 *            String to be printed in the currently opened file (without newline)
	 ***********************************************************/
	public static void print(String s)
	{
		buffer += s;
	}

	/***********************************************************
	 * Increase the current identation level
	 ***********************************************************/
	public static void incIndent()
	{
		fIndent += fIndentStep;
	}

	/***********************************************************
	 * return the current identation level in a String format
	 ***********************************************************/
	public static String getIndentSpace(){
		String indent = "";
		for(int i = 0; i < fIndent; ++i)
			indent += "\t";
		return indent;
	}
	
	/***********************************************************
	 * Decrease the current identation level
	 ***********************************************************/
	public static void decIndent()
	{
		if (fIndent < fIndentStep)
			throw new RuntimeException("unbalanced indentation");
		fIndent -= fIndentStep;
	}

	/***********************************************************
	 * @param s
	 *            The original string
	 * @return The string s with its first letter capitalized
	 ***********************************************************/
	public static String capitalize(String s)
	{
		return s.toUpperCase().substring(0, 1) + s.substring(1);
	}

}