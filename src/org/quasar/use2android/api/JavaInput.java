package org.quasar.use2android.api;

import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.ocl.type.Type;

public class JavaInput {
	
	public static String inputTemporaryVariables(Type oclType, String newVariableName, String variableInputCode){
		StringBuffer outPutCode = new StringBuffer("");

		if (oclType.isInteger())
			outPutCode.append("int " + newVariableName + " = 0;\n");
			
		if (oclType.isReal())
			outPutCode.append("double " + newVariableName + " = 0;\n");
			
		if (oclType.isBoolean())
			outPutCode.append("boolean " + newVariableName + " = " + variableInputCode + ";\n");
		
		if (oclType.isEnum())
			outPutCode.append(oclType.toString() + " " + newVariableName + " = " + oclType.toString() + ".values()[" + variableInputCode + "];\n");
			
		
		if (oclType.isString())
			outPutCode.append("String " + newVariableName + " = " + variableInputCode + ";\n");


//		if (oclType.isDate() || (oclType.isObjectType() && oclType.toString().equals("CalendarDate")))
//			outPutCode.append("Date " + newVariableName + " = new Date(" + variableInputCode + ");\n");
		
		if (oclType.isObjectType() && !oclType.toString().equals("CalendarDate"))
			outPutCode.append(oclType.toString() + " " + newVariableName + " = " + variableInputCode + ";\n");
		
		return outPutCode.toString();
	}

	public static String inputValidation(MAttribute att, String newVariableName ,String attributeName, String variableInputCode, String ErrorMessageComponentHolder, boolean addReturnNullInError, String indentLevel, boolean commentedValidation, boolean isSpecialPrimitive){
		StringBuffer outPutCode = new StringBuffer("");
		String commented = "";
		if(commentedValidation)
			commented = "//";
		
		Type oclType;
		if(isSpecialPrimitive)
			oclType = att.owner().type();
		else
			oclType = att.type();	
		
		if (oclType.isInteger()){
			outPutCode.append(commented + "try{\n");
			outPutCode.append(indentLevel + commented + "\t" + newVariableName + " = Integer.parseInt(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + commented + "}catch(NumberFormatException e){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Number Format Error\", \"Error in input:\\n\" + " + variableInputCode + ");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
		if (oclType.isReal()){
			outPutCode.append(commented + "try{\n");
			outPutCode.append(indentLevel + commented + "\t" + newVariableName + " = Double.parseDouble(" + variableInputCode + ");\n");
			outPutCode.append(indentLevel + commented + "}catch(NumberFormatException e){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Number Format Error\", \"Error in input:\\n\" + " + variableInputCode + ");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
//		if (oclType.isBoolean())
//			outPutCode.append("boolean");
		
		//in Android the first value is the default value, therefore there is no need for no selection validation
		if (oclType.isEnum()){
//			outPutCode.append("if(" + variableInputCode + " == " + "){\n");
//			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Selection error\", \"Error in selection of " + attributeName + ":\\nA value must be selected\");\n");
//			if(addReturnNullInError)
//				outPutCode.append(indentLevel + commented + "\treturn null;\n");
//			outPutCode.append(indentLevel + commented + "}");
//			outPutCode.append(oclType.toString() + " " + newVariableName + " = " + variableInputCode + ";\n");
			
		}
		
		if (oclType.isString()){
//			outPutCode.append(indentLevel + "temp_" + attributeName + " = " + variableInputCode + ";\n");
			outPutCode.append(commented + "if (" + newVariableName + ".equals(\"\")){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Text input error\", \"Error in input of " + attributeName + ":\\nThe input must have some text\");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
		//for this case since we work with component based ui we believe that the date already comes in the right format
		//so we are going only to declare a new instance and put the necessary code for future possible checks
		if (oclType.isDate() || (oclType.isObjectType() && (oclType.toString().equals("CalendarDate") || oclType.toString().equals("CalendarTime")))){
			outPutCode.append(commented + newVariableName + " = " + variableInputCode + ";\n");
			
			commented = "//";
			outPutCode.append(indentLevel + commented + "if (" + newVariableName + "  something to check){\n");
			outPutCode.append(indentLevel + commented + "\t" + ErrorMessageComponentHolder + "\"Date input error\", \"Error in input of " + attributeName + ":\\n error specific message\");\n");
			if(addReturnNullInError)
				outPutCode.append(indentLevel + commented + "\treturn null;\n");
			outPutCode.append(indentLevel + commented + "}");
		}
		
		return outPutCode.toString();
	}
	
	public static String inputComparatorConditionSetter(MAttribute att, String newVariableName, String attributeGetter, String attributeSetter, String variableInputCode, String indentLevel, boolean isSpecialPrimitive){
		StringBuffer outPutCode = new StringBuffer("");
		Type oclType;
		if(isSpecialPrimitive)
			oclType = att.owner().type();
		else
			oclType = att.type();	
		
		if (oclType.isInteger()){
//			outPutCode.append("int " + newVariableName + " = Integer.parseInt(" + variableInputCode + ");\n");
			outPutCode.append("if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
		if (oclType.isReal()){
//			outPutCode.append("double " + newVariableName + " = Double.parseDouble(" + variableInputCode + ");\n");
			outPutCode.append("if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
		if (oclType.isBoolean()){
//			outPutCode.append("boolean " + newVariableName + " = " + variableInputCode + ";\n");
			outPutCode.append("if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}

		if (oclType.isEnum()){
			outPutCode.append("if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
			
		}
		
		if (oclType.isString()){
//			outPutCode.append("String " + newVariableName + " = " + variableInputCode + ";\n");
			outPutCode.append("if (!" + attributeGetter + ".equals(" + variableInputCode + "))\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
		}
		//for this case since we work with component based ui we believe that the date already comes in the right format
		//so we are going only to declare a new instance a put the necessary code for future possible checks
		if (oclType.isDate() || (oclType.isObjectType() && (oclType.toString().equals("CalendarDate") || oclType.toString().equals("CalendarTime")))){
//			outPutCode.append("Date " + newVariableName + " = new Date(" + variableInputCode + ");\n");
//			outPutCode.append(indentLevel + "if (" + attributeGetter + ".compareTo(" + newVariableName + ") != 0)\n");
//			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");
			outPutCode.append(newVariableName + " = " + variableInputCode + ";\n");
			outPutCode.append(indentLevel + "if (" + attributeGetter + " != " + newVariableName + ")\n");
			outPutCode.append(indentLevel + "\t" + attributeSetter + "(" + newVariableName + ");");

		}
		
		return outPutCode.toString();
	}
}
