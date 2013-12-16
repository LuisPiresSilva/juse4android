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

package org.quasar.juse.api;

import org.tzi.use.uml.sys.MSystem;


/***********************************************************
* @author fba
* 19 de Abr de 2012
*
***********************************************************/
/***********************************************************
* @author fba
* 18 de Mai de 2013
*
***********************************************************/
public interface JUSE_BasicFacade
{
	/***********************************************************
	 * @param args
	 *            Arguments passed either at command line or in Eclipse "Run As/Run Configurations/Arguments"
	 * @param useBaseDirectory
	 *            Directory where USE is installed (Unix syntax)
	 * @param modelDirectory
	 *            Directory where the model to be compiled is
	 ***********************************************************/
	public JUSE_BasicFacade initialize(String[] args, String useBaseDirectory, String modelDirectory);

	/**********************************************************
	 * @param specificationFilename
	 *            The specification filename (*.use file)
	 * @return system the model and its state
	 ***********************************************************/
	public MSystem compileSpecification(String specificationFilename);

	/***********************************************************
	 * Processes a SOIL file
	 * @param modelInstancesFilename
	 *            The model instances filename (*.soil file)
	 * @param verbose
	 *            If true sends commands to console as well
	 * @return
	 * 				True if it was able to read the file correctly
	 ***********************************************************/
	public boolean readSOIL(String modelInstancesFilename, boolean verbose);

	/***********************************************************
	 * Generates a CMD file containg objects, their setup and links among them
	 * @param author
	 *            The author of the specification
	 * @param javaWorkspace
	 *            Workspace directory where the generated commands file is to be created
	 * @param cmdFile
	 *            Name of the commands file
	 * @param verbose
	 *            If true sends commands to console as well
	 ***********************************************************/
	public void dumpState(String author, String javaWorkspace, String cmdFile, boolean verbose);
	
	/***********************************************************
	 * @param commandLine
	 *            Command line to be processed by shell
	 ***********************************************************/
	public void command(String commandLine);

	/***********************************************************
	 * Run USE shell; thread gets hung until the shell exits This should be the last call in the program.
	 ***********************************************************/
	public void createShell();
}