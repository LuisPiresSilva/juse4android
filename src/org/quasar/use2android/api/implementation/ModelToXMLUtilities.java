package org.quasar.use2android.api.implementation;

public class ModelToXMLUtilities {

	public ModelToXMLUtilities(){
		
	}
	
	private String intent = "\t";
	private int totalXMLs = 0;
	private int generatedXMLs = 0;
	private int copiedXMLs = 0;

	public void addOneToGenerated(){
		totalXMLs += 1;
		generatedXMLs += 1;
	}
	
	public void addOneToCopied(){
		totalXMLs += 1;
		copiedXMLs += 1;
	}
	
	public void addToCopied(int number){
		totalXMLs += number;
		copiedXMLs += number;
	}
	
	public String toString(String projectName){
		return  intent + intent + intent + totalXMLs + " XML files added to the " + projectName + " project:\n"
				+ intent + intent + intent + intent + "Generated: " + generatedXMLs + "\n"
				+ intent + intent + intent + intent +"Copied: " + copiedXMLs; 
	}
}
