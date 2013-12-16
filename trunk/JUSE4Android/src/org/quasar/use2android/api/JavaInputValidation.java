package org.quasar.use2android.api;

import org.tzi.use.uml.ocl.type.Type;

public class JavaInputValidation {

	public static String inputValidation(Type oclType, String newVariableName ,String attributeName, String variableInputCode, String ErrorMessageComponentHolder, boolean addReturnNullInError, String indentLevel, boolean commentedValidation){
		StringBuffer outPutCode = new StringBuffer("");
		String commented = "";
		if(commentedValidation)
			commented = "//";
		
		if (oclType.isInteger()){
			outPutCode.append("int " + newVariableName + " = 0;\n");
			outPutCode.append(indentLevel + commented + "try{\n");
			outPutCode.append(indentLevel + commented + "\t" + newVariableName + " = Integer.parseInt(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + commented + "}catch(NumberFormatException e){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Number Format Error\", \"Error in input:\\n\" + " + variableInputCode + ");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
		if (oclType.isReal()){
			outPutCode.append("double " + newVariableName + " = 0;\n");
			outPutCode.append(indentLevel + commented + "try{\n");
			outPutCode.append(indentLevel + commented + "\t" + newVariableName + " = Double.parseDouble(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + commented + "}catch(NumberFormatException e){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Number Format Error\", \"Error in input:\\n\" + " + variableInputCode + ");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
//		if (oclType.isBoolean())
//			outPutCode.append("boolean");
		
		if (oclType.isEnum())
			outPutCode.append(oclType.toString() + " " + newVariableName + " = " + variableInputCode + ";\n");
		
		if (oclType.isString()){
			outPutCode.append("String " + newVariableName + " = " + variableInputCode + ";\n");
//			outPutCode.append(indentLevel + "temp_" + attributeName + " = " + variableInputCode + ";\n");
			outPutCode.append(indentLevel + commented + "if (" + newVariableName + ".equals(\"\")){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Text input error\", \"Error in input of " + attributeName + ":\\nThe input must have some text\");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
		//for this case since we work with component based ui we believe that the date already comes in the right format
		//so we are going only to declare a new instance a put the necessary code for future possible checks
		if (oclType.isDate() || (oclType.isObjectType() && oclType.toString().equals("Date"))){
			outPutCode.append("Date " + newVariableName + " = new Date(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + "//if (" + newVariableName + "  something to check){\n");
			outPutCode.append(indentLevel + "//\t" + ErrorMessageComponentHolder + "\"Date input error\", \"Error in input of " + attributeName + ":\\n error specific message\");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + "//\treturn null;\n");
			outPutCode.append(indentLevel + "//}");
		}
		
		return outPutCode.toString();
	}
	
	public static String inputComparatorConditionSetter(Type oclType, String newVariableName, String attributeGetter, String attributeSetter, String variableInputCode, String indentLevel){
		StringBuffer outPutCode = new StringBuffer("");
		
		if (oclType.isInteger()){
			outPutCode.append("int " + newVariableName + " = Integer.parseInt(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + "if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
		if (oclType.isReal()){
			outPutCode.append("double " + newVariableName + " = Double.parseDouble(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + "if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
//		if (oclType.isBoolean())
//			outPutCode.append("boolean");
		if (oclType.isString()){
			outPutCode.append("String " + newVariableName + " = " + variableInputCode + ";\n");
			outPutCode.append(indentLevel + "if (!" + attributeGetter + ".equals(" + variableInputCode + "))\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
		//for this case since we work with component based ui we believe that the date already comes in the right format
		//so we are going only to declare a new instance a put the necessary code for future possible checks
		if (oclType.isDate() || (oclType.isObjectType() && oclType.toString().equals("Date"))){
			outPutCode.append("Date " + newVariableName + " = new Date(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + "if (" + attributeGetter + ".compareTo(" + newVariableName + ") != 0)\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");

		}
		
		return outPutCode.toString();
	}
}
