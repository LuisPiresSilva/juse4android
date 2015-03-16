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

package org.quasar.juse.api.implementation;

import org.quasar.juse.api.JUSE_BasicFacade;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.tzi.use.config.Options;
import org.tzi.use.main.Session;
import org.tzi.use.main.shell.Shell;
import org.tzi.use.parser.use.USECompiler;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.ModelFactory;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MObjectState;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;
import org.tzi.use.util.Log;
import org.tzi.use.util.USEWriter;

/***********************************************************
 * @author fba
 * @version 1.0.4 - 25 de Abr de 2012
 * @version 1.0.6 - 25 de Abr de 2013
 ***********************************************************/
public class BasicFacade implements JUSE_BasicFacade
{
	private Session	session	= new Session();
	private MSystem	system	= null;
	private Shell	shell	= null;

	public BasicFacade()
	{
		System.out.println("\nj-use version 1.0.6, Copyright (C) 2012-2013 QUASAR research group");
	}

	/***********************************************************
	 * @return the system
	 ***********************************************************/
	public MSystem getSystem()
	{
		return system;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#initialize(java.lang.String[], java.lang.String, java.lang.String)
	 */
	public JUSE_BasicFacade initialize(String[] args, String useBaseDirectory, String modelDirectory)
	{
		// set System.out to the OldUSEWriter to protocol the output.
		System.setOut(USEWriter.getInstance().getOut());
		// set System.err to the OldUSEWriter to protocol the output.
		System.setErr(USEWriter.getInstance().getErr());

		String[] initialArgs = { "-H=" + useBaseDirectory };

		// Command line arguments or in Eclipse "Run As/Run Configurations/Arguments" are appended with
		// the initialArgs (USE instalation directory)

		String[] args2 = new String[args.length + 1];
		for (int i = 0; i < args.length; i++)
			args2[i] = args[i];
		args2[args.length] = initialArgs[0];

		// read and set global options, setup application properties
		Options.processArgs(args2);

		Options.doGUI = false;
		Options.doPLUGIN = false;

		// System.out.println("user.dir=" + System.getProperty("user.dir"));
		// System.out.println("lastDirectory=" + Options.getLastDirectory());

		// set current model directory
		if (modelDirectory != null)
		{
			System.setProperty("user.dir", modelDirectory);
			Options.setLastDirectory(modelDirectory);
		}
		// System.out.println("user.dir=" + System.getProperty("user.dir"));
		// System.out.println("lastDirectory=" + Options.getLastDirectory());

		// compile spec if filename given as command line argument
		if (Options.specFilename != null)
			compileSpecification(Options.specFilename);

		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#compileSpecification(java.lang.String)
	 */
	public MSystem compileSpecification(String specificationFilename)
	{
		MModel model = null;

		// compile spec if filename given as argument
		if (specificationFilename != null)
		{
			specificationFilename = System.getProperty("user.dir") + "/" + specificationFilename;

			FileInputStream specStream = null;
			try
			{
				System.out.println("\nCompiling specification " + specificationFilename);

				Log.verbose("compiling specification " + specificationFilename);
				specStream = new FileInputStream(specificationFilename);
				model = USECompiler.compileSpecification(specStream, specificationFilename, new PrintWriter(System.err),
								new ModelFactory());
			}
			catch (FileNotFoundException e)
			{
				Log.error("File `" + specificationFilename + "' not found.");
				System.exit(1);
			}
			finally
			{
				if (specStream != null)
					try
					{
						specStream.close();
					}
					catch (IOException ex)
					{
						// ignored
					}
			}

			// compile errors?
			if (model == null)
			{
				System.exit(1);
			}

			if (Options.compileOnly)
			{
				Log.verbose("no errors.");
				if (Options.compileAndPrint)
				{
					model = USECompiler.compileSpecification(specStream, specificationFilename, new PrintWriter(System.err),
									new ModelFactory());
				}
				System.exit(0);
			}

			// print some info about model
			Log.verbose(model.getStats());
		}

		else
		{
			model = new ModelFactory().createModel("empty model");
			Log.verbose("using empty model.");
		}
		// create system, session and shell
		system = new MSystem(model);

		session.setSystem(system);
		Shell.createInstance(session, null);
		shell = Shell.getInstance();

		return system;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#readSOIL(java.lang.String, boolean)
	 */
	public boolean readSOIL(String modelInstancesFilename, boolean verbose)
	{
		boolean result = false;
		if (system == null || system.model() == null)
		{
			System.out.println("Please compile the specification first!");
			return result;
		}

		modelInstancesFilename = System.getProperty("user.dir") + "/" + modelInstancesFilename;

		System.out.println("\nReading SOIL file " + modelInstancesFilename);

		// Unfortunately none of these 2 simple options work :( ...
		// command("open " + modelInstancesFilename);
		// shell.cmdRead(modelInstancesFilename, quiet);
		// command("info state");

		// ... so let's do it the hard way ...
		FileReader fr = null;
		try
		{
			fr = new FileReader(modelInstancesFilename);

			BufferedReader br = new BufferedReader(fr);
			String s = null;
			try
			{
				int line = 0;
				while ((s = br.readLine()) != null)
				{
					line++;
					if (verbose)
						System.out.println(s);
					else
						if (line % 500 == 0)
							System.out.print(".");

					shell.processLineSafely(s);
				}
				System.out.println("\n... finished reading " + line + " lines.\n");
				fr.close();
				result = true;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#dumpState(java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	public void dumpState(String author, String javaWorkspace, String cmdFile, boolean verbose)
	{
		if (system == null || system.model() == null)
		{
			System.out.println("Please compile the specification first!");
			return;
		}

		String targetDirectory = javaWorkspace + "/" + system.model().name() + "/data";

		MSystemState systemState = system.state();

		String command;

		if (!FileUtilities.openOutputFile(targetDirectory, cmdFile))
		{
			System.out.println("Dumping model snapshot to " + targetDirectory + "/" + cmdFile);
			return;
		}
		else
			System.out.println("Dumping model snapshot to " + targetDirectory + "/" + cmdFile);

		FileUtilities.println("-------------------------------------------------------------------");
		FileUtilities.println("-- author: " + author);
		FileUtilities.println("-------------------------------------------------------------------");
		FileUtilities.println("reset");
		FileUtilities.println();

		// generate regular objects
		int line = 0;
		for (MObject theObject : systemState.allObjects())
			if (!(theObject instanceof MLink))
			{
				line++;
				command = "!create " + theObject.name() + ": " + theObject.cls().name();
				FileUtilities.println(command);
				if (verbose)
					System.out.println(command);
			}
		System.out.println("\t- wrote " + line + " create object commands");

		FileUtilities.println("-------------------------------------------------------------------");

		line = 0;
		// generate regular link objects whose connected objects are regular objects
		for (MObject theObject : systemState.allObjects())
			if (theObject instanceof MLink)
			{
				MLink theLink = (MLink) theObject;

				if (!(theLink.linkedObjects().get(0) instanceof MLink) && !(theLink.linkedObjects().get(1) instanceof MLink))
				{
					line++;
					command = "!create " + theObject.name() + ": " + theObject.cls().name() + " between ("
									+ theLink.linkedObjects().get(0).name() + ", " + theLink.linkedObjects().get(1).name()
									+ ")";
					FileUtilities.println(command);
					if (verbose)
						System.out.println(command);
				}
			}

		// generate regular link objects whose connected objects are link objects
		for (MObject theObject : systemState.allObjects())
			if (theObject instanceof MLink)
			{
				MLink theLink = (MLink) theObject;

				if (theLink.linkedObjects().get(0) instanceof MLink || theLink.linkedObjects().get(1) instanceof MLink)
				{
					line++;
					command = "!create " + theObject.name() + ": " + theObject.cls().name() + " between ("
									+ theLink.linkedObjects().get(0).name() + ", " + theLink.linkedObjects().get(1).name()
									+ ")";
					FileUtilities.println(command);
					if (verbose)
						System.out.println(command);
				}
			}
		System.out.println("\t- wrote " + line + " create link object commands");

		FileUtilities.println("-------------------------------------------------------------------");

		line = 0;
		// generate regular links
		for (MLink theLink : systemState.allLinks())
			if (!(theLink instanceof MObject))
			{
				line++;
				command = "!insert (" + theLink.linkedObjects().get(0).name() + ", " + theLink.linkedObjects().get(1).name()
								+ ") into " + theLink.association().name();
				FileUtilities.println(command);
				if (verbose)
					System.out.println(command);
			}
		System.out.println("\t- wrote " + line + " insert link commands");

		FileUtilities.println("-------------------------------------------------------------------");

		line = 0;
		// set objects state
		for (MObject theObject : systemState.allObjects())
		{
			MObjectState objectState = theObject.state(systemState);
			for (MAttribute attribute : theObject.cls().allAttributes())
			{
				if (objectState.attributeValue(attribute).isDefined())
				{
					line++;
					command = "!set " + theObject.name() + "." + attribute.name() + " := "
									+ objectState.attributeValue(attribute);
					FileUtilities.println(command);
					if (verbose)
						System.out.println(command);
				}
			}
		}
		System.out.println("\t- wrote " + line + " set object state commands");

		FileUtilities.closeOutputFile();

		// print some info about snapshot
		// System.out.println("Specification " + system.model().name() + " snapshot (" + systemState.allObjects().size() +
		// " objects, "
		// + systemState.allLinks().size() + " links)");

		System.out.println("Model snapshot dump concluded!\n");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#command(java.lang.String)
	 */
	public void command(String commandLine)
	{
		shell.processLineSafely(commandLine);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quasar.juse.api.JUSE_BasicFacade#createShell()
	 */
	public void createShell()
	{
		Thread t = new Thread(shell);
		t.start();

		// wait on exit from shell (this thread never returns)
		try
		{
			t.join();
		}
		catch (InterruptedException ex)
		{
			// ignored
		}
	}
}