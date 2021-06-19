/*
 * GICI Library -
 * Copyright (C) 2007  Group on Interactive Coding of Images (GICI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Group on Interactive Coding of Images (GICI)
 * Department of Information and Communication Engineering
 * Autonomous University of Barcelona
 * 08193 - Bellaterra - Cerdanyola del Valles (Barcelona)
 * Spain
 *
 * http://gici.uab.es
 * gici-info@deic.uab.es
 */
package GiciParser;
import GiciException.*;

import java.lang.reflect.*;


/**
 * Arguments parser definition (useful for extending in other classe). This class analyses a string of arguments and extract and check its validity. Use example:
 *
 *
 * import java.lang.reflect.*;
 *
 * public class CoderParser extends ArgumentsParser{
 * 	//ARGUMENTS SPECIFICATION
 * 	String[][] coderArguments = {
 * 		{"-ct", "--colourTransformType", "{int}", "if 3 first components are RGB: 1 if WT is 1 or 0, 2 if WT is 2. Otherwise 0.", "0", "1",
 * 			"Colour transform type:\n    0- No colour transform\n    1- Reversible Colour Transform\n    2- Irreversible Colour Transform"
 * 		}
 * 	};
 * 	//ARGUMENTS VARIABLES
 * 	int CTType = -1;
 *
 * 	public CoderParser(String[] arguments) throws ParameterException, ErrorException{
 * 		try{
 * 			Method m = this.getClass().getMethod("parseArgument", new Class[] {int.class, String[].class});
 * 			parse(coderArguments, arguments, this, m);
 * 		}catch(NoSuchMethodException e){
 * 			throw new ErrorException("Coder parser error invoking parse function.");
 * 		}
 * 	}
 * 	public void parseArgument(int argFound, String[] options) throws ParameterException{
 * 		switch(argFound){
 * 		case  0: //-c  --colourTransformType
 * 			CTType = parseIntegerPositive(options);
 * 			break;
 * 		}
 * 	}
 * 	public int getCTType(){
 * 		return(CTType);
 * 	}
 * }
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */

/**
 * FIXME!!! the correct way to do this would had been to use an abstract function (aka pure virtual) in the base
 * class forcing all descendents of it to implement parseArgument. Then in ArgParser one could call that function. Of course
 * ArgParser must be an abstract class!
 * 
 * Here it is, and without reflection...
 * 
 * public abstract class ArgumentsParser{
 *  abstract parseArgument(int argFound, String[] options) throws ParameterException;
 *  
 * 	ArgumentsParser(String[] arguments, String[][] Arguments) {
 * 		parse(Arguments, arguments);
 *  }
 * }
 * 
 * public class CoderParser extends ArgumentsParser{
 * 	//ARGUMENTS SPECIFICATION
 * 	String[][] Arguments = {
 * 		{"-ct", "--colourTransformType", "{int}", "if 3 first components are RGB: 1 if WT is 1 or 0, 2 if WT is 2. Otherwise 0.", "0", "1",
 * 			"Colour transform type:\n    0- No colour transform\n    1- Reversible Colour Transform\n    2- Irreversible Colour Transform"
 * 		}
 * 	};
 * 	//ARGUMENTS VARIABLES
 * 	int CTType = -1;
 *
 *  CoderParser(String[] arguments) {
 *  	super(arguments, Arguments);
 *  }
 *
 * 	public void parseArgument(int argFound, String[] options) throws ParameterException{
 * 		switch(argFound){
 * 		case  0: //-c  --colourTransformType
 * 			CTType = parseIntegerPositive(options);
 * 			break;
 * 		}
 * 	}
 * 	public int getCTType(){
 * 		return(CTType);
 * 	}
 * }
 */

public class ArgumentsParser {

	/**
	 * Arguments specificiation. The array describes argument, explain what is used and its default parameters. First index of array is argument; second specifies:<br>
	 *   <ul>
	 *     <li> 0- short argument specification
	 *     <li> 1- long argument specification
	 *     <li> 2- parsing specification of argument ({} indicates mandatority, [] optionality) -string only showed to the user-
	 *     <li> 3- default values
	 *     <li> 4- minimum number of argument ocurrences (if argument is non mandatory it must be 1 or greater)
	 *     <li> 5- maximum number of argument ocurrences (0 indicates infinite)
	 *     <li> 6- explanation
	 *   </ul>
	 * <p>
	 * String arguments.
	 */
	String[][] argumentsSpecification = null;
	
	/**
	 * Receives the arguments string and parses all the arguments. It calls a method passed to class passing to it the number of argument and its options (this method can use parseInteger,... functions to extract arguments values).
	 *
	 * @param argumentsSpecification defined in this class
	 * @param args the array of strings passed at the command line
	 * @param parseClass class where the invocated method is
	 * @param parseArgument method to invoke for each argument
	 *
	 * @throws ParameterException when an invalid parsing is detected
	 * @throws ErrorException when some problem with method invocation occurs
	 */
	public void parse(String[][] argumentsSpecification, String[] args, Object parseClass, Method parseArgument) throws ParameterException, ErrorException{
		//Copy parameters
		this.argumentsSpecification = argumentsSpecification;

		//Initalizations
		int argNum = 0;
		int[] argsNumFounds = new int[argumentsSpecification.length];

		//Arguments parsing
		for(int i = 0; i < argumentsSpecification.length; i++){
			argsNumFounds[i] = 0;
		}
		while(argNum < args.length){
			int argFound = argFind(args[argNum]);
			if (argFound != -1) {
				argsNumFounds[argFound]++;
				int argOptions = argNum + 1;
				while (argOptions < args.length) {
					if (argFind(args[argOptions]) != -1) {
						break;
					} else {
						argOptions++;
					}
				}
				
				int numOptions = argOptions - argNum;
				String[] options = new String[numOptions];
				System.arraycopy(args, argNum, options, 0, numOptions);
				argNum = argOptions;
				
				try {
					parseArgument.invoke(parseClass, argFound, options);
				} catch(IllegalAccessException e) {
					throw new ErrorException("Illegal access invocating parse argument function.");
				} catch(InvocationTargetException e) {					
					throw new ParameterException("Wrong parameters for argument \"" + options[0] + "\": " 
							+ e.getCause().getLocalizedMessage());
				}
			} else {
				throw new ParameterException("Argument \"" + args[argNum] + "\" unrecognized.");
			}
		}

		//Check number of arguments occurrences
		for(int i = 0; i < argumentsSpecification.length; i++){
			int minArgsOccurs = 0;
			int maxArgsOccurs = 0;
			try{
				minArgsOccurs = Integer.parseInt(argumentsSpecification[i][4]);
				maxArgsOccurs = Integer.parseInt(argumentsSpecification[i][5]);
			}catch(NumberFormatException e){
				throw new ErrorException("Malformed arguments specification.");
			}
			if(argsNumFounds[i] < minArgsOccurs){
				throw new ParameterException("Argument \"" + argumentsSpecification[i][0] + "\" must must appear " + minArgsOccurs + " times minimum  (\"-h\" displays help).");
			}
			if(maxArgsOccurs > 0){
				if(argsNumFounds[i] > maxArgsOccurs){
					throw new ParameterException("Argument \"" + argumentsSpecification[i][0] + "\" can appear " + maxArgsOccurs + " times maximum (\"-h\" displays help).");
				}
			}
		}
	}

	/**
	 * Finds the argument string in arguments specification array.
	 *
	 * @param arg argument to find out in argumentsSpecification
	 * @return the argument index of argumentsSpecification (-1 if it doesn't exist)
	 */
	protected int argFind(String arg){
		int argFound = 0;
		boolean found = false;

		while((argFound < argumentsSpecification.length) && !found){
			if((arg.compareTo(argumentsSpecification[argFound][0]) == 0) || (arg.compareTo(argumentsSpecification[argFound][1]) == 0)){
				found = true;
			}else{
				argFound++;
			}
		}
		return(found ? argFound: -1);
	}

	/**
	 * This function shows arguments information to console.
	 */
	public void showArgsInfo(){
		System.out.println("Arguments specification: ");
		for(int numArg = 0; numArg < argumentsSpecification.length; numArg++){
			char beginMandatory = '{', endMandatory = '}';
			if(argumentsSpecification[numArg][4].compareTo("0") == 0){
				//No mandatory argument
				beginMandatory = '[';
				endMandatory = ']';
			}
			System.out.print("\n" + beginMandatory + " ");
			System.out.print("{" + argumentsSpecification[numArg][0] + "|" + argumentsSpecification[numArg][1] + "} " + argumentsSpecification[numArg][2]);
			System.out.println(" " + endMandatory);
			System.out.println("  Explanation:\n    " + argumentsSpecification[numArg][6]);
			System.out.println("  Default value: " + argumentsSpecification[numArg][3]);
			String maxOccurences = "";
			if(argumentsSpecification[numArg][5].compareTo("0") == 0){
				maxOccurences = "inf";
			}else{
				maxOccurences = argumentsSpecification[numArg][5];
			}
			System.out.println("  Minimum/Maximum occurences: " + argumentsSpecification[numArg][4] + "/" + maxOccurences);
		}
	}

	/**
	 * This function shows arguments information to console using a formatted table in latex (useful to write manuals in latex).
	 */
	public void showArgsInfoLatexTable(){
		for(int numArg = 0; numArg < argumentsSpecification.length; numArg++){
			System.out.println("\\begin{center}\\begin{tabular}{|rr|rl|rl|}");
			String longParam = argumentsSpecification[numArg][1].replace("-","$-$");
			String shortParam = argumentsSpecification[numArg][0].replace("-","$-$");
			String paramArguments = argumentsSpecification[numArg][2].replace("{","$\\{$").replace("}","$\\}$").replace("[","$[$").replace("]","$]$");
			String paramMandatory;
			if(argumentsSpecification[numArg][4].compareTo("0") == 0){
				paramMandatory = "No";
			}else{
				paramMandatory = "Yes";
			}
			String paramReps;
			if(argumentsSpecification[numArg][5].compareTo("0") == 0){
				paramReps = "$\\infty$";
			}else{
				paramReps = argumentsSpecification[numArg][5];
			}
			String paramExplanation = argumentsSpecification[numArg][6].replace("\n","\\newline").replace("    "," ");
			String paramDefault = argumentsSpecification[numArg][3];

			System.out.println("\\hline\n\\multicolumn{2}{|l|}{\\textbf{" + longParam + "}} & \\multicolumn{4}{|l|}{" + paramArguments + "} \\\\\n\\cline{3-6}");
			System.out.println("\\multicolumn{2}{|l|}{\\textbf{" + shortParam + "}} & \\emph{Mandatory:} & " + paramMandatory + " & \\emph{Max reps:} & " + paramReps + " \\\\\n\\hline");
			System.out.println("\\emph{Explanation:} & \\multicolumn{5}{|p{12cm}|}{" + paramExplanation + "} \\\\\n\\hline");
			System.out.println("\\emph{Default:} & \\multicolumn{5}{|p{12cm}|}{" + paramDefault + "} \\\\\n\\hline");
			System.out.println("\\end{tabular}\\end{center}");
		}
	}

	/////////////////////
	//PARSING FUNCTIONS//
	/////////////////////
	//These functions receives a string array that contains in first position the argument and then their options and return the corresponding value//

	public boolean parseBoolean(String[] options) throws ParameterException{
		boolean value = false;

		if(options.length == 2){
			try{
				int readValue = Integer.parseInt(options[1]);
				if((readValue < 0) || (readValue > 1)){
					throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" must be 0 or 1.");
				}else{
					value = readValue == 0 ? false: true;
				}
			}catch(NumberFormatException e){
				throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}

	public int parseIntegerPositive(String[] options) throws ParameterException{
		int value = 0;

		if(options.length == 2){
			try{
				value = Integer.parseInt(options[1]);
				if(value < 0){
					throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is must be a positive integer.");
				}
			}catch(NumberFormatException e){
				throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}
	
	public int parseInteger(String[] options) throws ParameterException{
		int value = 0;

		if(options.length == 2){
			try{
				value = Integer.parseInt(options[1]);
			}catch(NumberFormatException e){
				throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}

	public long parseLongPositive(String[] options) throws ParameterException{
		long value = 0;

		if(options.length == 2){
			try{
				value = Long.parseLong(options[1]);
				if(value < 0){
					throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is must be a positive long.");
				}
			}catch(NumberFormatException e){
				throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is not a parsable long.");
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}

	public float parseFloatPositive(String[] options) throws ParameterException{
		float value = 0F;

		if(options.length == 2){
			try{
				value = Float.parseFloat(options[1]);
				if(value < 0){
					throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is must be a positive float.");
				}
			}catch(NumberFormatException e){
				throw new ParameterException("\"" + options[1] + "\" of argument \"" + options[0] + "\" is not a parsable float.");
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}

	public String parseString(String[] options) throws ParameterException{
		String value = "";

		if(options.length == 2){
			value = options[1];
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}
	
	public String[] parseStrings(String[] options) throws ParameterException{
		String[] value = null;

		if(options.length == 2){
			value = options[1].split(",");
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one option. Try \"-h\" to display help.");
		}
		return(value);
	}

	String[] parseStringArray(String[] options, int numOptions) throws Exception{
		String[] value = new String[numOptions];

		if(options.length == numOptions + 1){
			for (int i = 0; i < numOptions; i++) {
				value[i] = options[1 + i];
			}
		}else{
			throw new Exception("Argument \"" + options[0] + "\" takes " + numOptions + " option. Try \"-h\" to display help.");
		}
		return(value);
	}
	
	public boolean[] parseBooleanArray(String[] options, int numOptions) throws ParameterException{
		boolean[] value = null;

		if(options.length == numOptions+1){
			value =  new boolean[options.length - 1];
			int[] readValue = new int[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					readValue[numOption - 1] = Integer.parseInt(options[numOption]);
					if((readValue[numOption - 1] < 0) || (readValue[numOption - 1] > 1)){
						throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" must be 0 or 1.");
					}else{
						value[numOption - 1] = readValue[numOption - 1] == 0 ? false: true;
					}
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes " + numOptions +" options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public int[] parseIntegerArray(String[] options) throws ParameterException{
		int[] value = null;

		if(options.length >= 2){
			value = new int[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Integer.parseInt(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one or more options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public int[] parseIntegerArray(String[] options, int numOptions) throws ParameterException{
		int[] value = null;

		if(options.length == numOptions+1){
			value = new int[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Integer.parseInt(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable integer.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes " + numOptions +" options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public long[] parseLongArray(String[] options) throws ParameterException{
		long[] value = null;

		if(options.length >= 2){
			value = new long[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Long.parseLong(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable long.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one or more options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public long[] parseLongArray(String[] options, int numOptions) throws ParameterException{
		long[] value = null;

		if(options.length == numOptions+1){
			value = new long[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Long.parseLong(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable long.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes " + numOptions +" options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public float[] parseFloatArray(String[] options) throws ParameterException{
		float[] value = null;

		if(options.length >= 2){
			value = new float[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Float.parseFloat(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable float.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes one or more options. Try \"-h\" to display help.");
		}
		return(value);
	}

	public float[] parseFloatArray(String[] options, int numOptions) throws ParameterException{
		float[] value = null;

		if(options.length == numOptions+1){
			value = new float[options.length - 1];
			for(int numOption = 1; numOption < options.length; numOption++){
				try{
					value[numOption - 1] = Float.parseFloat(options[numOption]);
				}catch(NumberFormatException e){
					throw new ParameterException("\"" + options[numOption] + "\" of argument \"" + options[0] + "\" is not a parsable float.");
				}
			}
		}else{
			throw new ParameterException("Argument \"" + options[0] + "\" takes " + numOptions + " options. Try \"-h\" to display help.");
		}
		return(value);
	}

}

