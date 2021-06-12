/*
 * EMPORDA Software - More than an implementation of MHDC Recommendation for Image Data Compression
 * Copyright (C) 2011  Group on Interactive Coding of Images (GICI)
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
 * http://sourceforge.net/projects/emporda
 * gici-info@deic.uab.es
 */
package emporda;

import GiciException.*;
import GiciParser.*;


/**
 * Arguments parser for EMPORDA application (extended from ArgumentsParser).
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class Parser extends BaseArgumentsParser {

	//ARGUMENTS SPECIFICATION
	private String[][] coderArguments = {

			{"-c", "--compress", "", "", "1", "1",
				"Compress de input image."
			},

			{"-d", "--decompress", "", "", "1", "1",
				"Decompress de input image."
			},

			{"-i", "--inputImage", "{string}", "", "1", "1",
				"It must be a raw image"
			},

			{"-o", "--outputFile", "{string}", "", "1", "1",
				"Output file.\n"
				+ "COMPRESSING: Output file name without extension.\n"
				+ "DECOMPRESSING: Output image file WITH extension."
			},

			{"-ig", "--inputImageGeometry", "{int int int int int}", "", "1", "1",
				"Geometry of raw image data. Only nessesary if image is raw data. Parameters are:\n" + 
				"1- zSize (number of image components)\n2- ySize (image height)\n" + 
				"3- xSize (image width)\n4- data type. Possible values are:\n      " + 
				"1- unsigned int (1 byte)\n      2- unsigned int (2 bytes)\n      " + 
				"3- signed int (2 bytes)\n" + 
				"5- 1 if 3 first components are RGB, " +
				"0 otherwise\n" 

			},
			{"-so", "--sample-order", "{int}", "", "0", "1",
				"The sample order in which the image will be loaded or saved. \n"
				+ "0.- BSQ, 1.- BIL, 2.- BIP. This value is 0 by default. "
			},
			{"-v", "--verbose", "", "", "0", "1",
				"Displays information about the progress in the task done by the program."
			},
			{"-e", "--endianess", "", "", "0", "1",
				"Is used to tell the program the image's endianess in compression and decompression modes. \n"
				+ "0 BIG_ENDIAN, 1 LITTLE_ENDIAN. This value is 0 by default"
			},
			{"-h", "--help", "", "", "0", "1",
				"Displays help and exits program."
			},
			{"-f", "--option-file", "{String}", "", "0", "1",
				"sets the option file for the compression process"
			},
			{"-dbg", "--debug-mode", "", "", "0", "1", 
				"sets debug mode, in which the program outputs useful information for debugging tasks"
			},
			{"-os", "--option-string", "{String}", "", "0", "1", 
				"Sets options from command line. It is equivalent to -f."
			},
			{"-delta","--delta-file", "", "", "0", "1",
				"It is used in compression mode to ask the program to generate" +
				" a none coded file of the differences. Useful to obtain statistics of " +
				"the compression."
			},
			{"-format", "--pixel-format", "{int}", "", "0", "1", 
				"It is used in decompression mode, to set how the pixels of the " +
						"restored image must be saved. Possibilities: \n" +
						"1- unsigned int (1 byte)\n" +
						"2- unsigned int (2 bytes)\n" + 
						"3- signed int (2 bytes)\n" 
			},
			{"-ss", "--save-state", "", "", "0", "1", 
				"If set, emporda creates a file outputImage.state " + 
						"in which all the variables used by the application " +  
						"are stored in a file. It must be considered that in some " +
						"cases it is not possible to save the same value for a parameter."
			},
			{"-bc", "--bit-cost", "{int}", "", "0", "1",
				"If set, emporda creates a file outputImage.bitCost " + 
						"in which it is written Possibilites: \n" +
						"0- do nothing (default)"+
						"1- the length of each codeword in outputImage\n" +
						"2- pairs (u_z(t),k_z(t)) if t != 0 and -1 if t=0\n"+
						"(only works in compression mode and for the sample adaptive coder)."
			},
			{"-P", "--pedantic", "", "", "0", "1", 
				"If set, emporda does not execute if non sense sets of parameters " +
						"are found. It only can be used in compression mode."
							
			},
			{"-t", "--time", "", "", "0", "1", 
				"If set, emporda outputs the time used in the predictor."
			}
	};

	//ARGUMENTS VARIABLES + Default Values
	private int action = 0; // Indicates if the program compress 0 or decompress 1
	private String inputFile = ""; // Input File
	private String outputFile = ""; // Output File
	private int[] imageGeometry = null; // Image geometry
	private boolean verbose = false; // The program shows information about the compression
	private String optionFile = null; //option file
	private String optionString = null; //option string
	private int sampleOrder = 0;     // sample order of the image
	private int endianess = 0; // endianess of the image decompressed
	private boolean debugMode = false; //debug mode
	private boolean deltaMode = false; //delta mode
	private int pixelFormat = 0; // pixel format
	private boolean saveState = false; // weight file
	private int bitCost = 0; //bit cost
	private boolean pedantic = false; // pedantic mode
	private boolean time = false; // pedantic mode
	
	/** Receives program arguments and parses it, setting to arguments variables.
	 *
	 * @param arguments the array of strings passed at the command line
	 *
	 * @throws ParameterException when an invalid parsing is detected
	 * @throws ErrorException when some problem with method invocation occurs
	 */
	public Parser(String[] arguments) throws ParameterException, ErrorException {

		parse(coderArguments, arguments);

	}

	/**
	 * Parse an argument using parse functions from super class and put its value/s to the desired variable.
	 * This function is called from parse function of the super class.
	 *
	 * @param argFound number of parameter (the index of the array coderArguments)
	 * @param options the command line options of the argument
	 *
	 * @throws ParameterException when some error about parameters passed (type, number of params, etc.) occurs
	 */
	public void parseArgument(int argFound, String[] options) throws ParameterException {

		switch(argFound) {

		case 0: // -c --compress
			action = 0;
			coderArguments[1][4] = "0";
			coderArguments[1][5] = "0";
			break;

		case 1: // -d -decompress 
			action = 1;
			coderArguments[0][4] = "0";
			coderArguments[0][5] = "0";
			coderArguments[4][4] = "0";
			coderArguments[4][5] = "0";
			break;

		case  2: //-i  --inputImage
			inputFile = parseString(options);
			break;

		case  3: //-o  --outputFile
			outputFile = parseString(options);
			break;

		case  4: //-ig  --inputImageGeometry
			int[] tmpGeometry = parseIntegerArray(options, 5);
			imageGeometry = new int[6];
			imageGeometry[0] = tmpGeometry[0];
			imageGeometry[1] = tmpGeometry[1];
			imageGeometry[2] = tmpGeometry[2];
			imageGeometry[3] = tmpGeometry[3];
			imageGeometry[4] = 0; // it is set with -e option
			imageGeometry[5] = tmpGeometry[4];

			coderArguments[1][4] = "0";
			coderArguments[1][5] = "0";
			break;

		case 5: //-so --sample-order
			sampleOrder = parseInteger(options);
			break;

		case 6: //-v --verbose
			verbose = true;
			break;

		case 7: // -e --endianess
			endianess = parseInteger(options);
			break;

		case 8: //-h  --help
			System.out.println("Emporda");
			showArgsInfo();
			System.exit(0);
			break;

		case 9: //-f --option-file
			optionFile = parseString(options);
			if(optionString != null) {
				throw new ParameterException("Parameters -os and -f are not compatible");
			}
			break;
			
		case 10: //dbg --debug-mode
			debugMode = true;
			break;
		case 11: //-os --option-string
			optionString = parseString(options);
			if(optionFile != null) {
				throw new ParameterException("Parameters -os and -f are not compatible");
			}
			break;
		case 12: //-delta, --delta-file
			deltaMode = true;
			break;
			
		case 13: //-format, --pixel-format
			pixelFormat = parseInteger(options);
			break;
		case 14: // -ss, --save-state
			saveState = true;
			break;
		case 15: // -bc, --bit-cost
			bitCost = parseInteger(options);
			break;
		case 16: // -P, --pedantic
			pedantic = true;
			break;
		case 17: // -t, --time
			time = true;
			break;
			
		default:
			throw new ParameterException("Unknown Parameter ");
		}

	}


	/**
	 * get action.
	 * 
	 * @return action tells if compression or decompression must be done
	 */
	public int getAction() {

		return action;
	}

	/**
	 * get input file.
	 *
	 * @return the input file, from where the data will be read
	 */
	public String getInputFile() {

		return inputFile;
	}

	/** 
	 * get output file.
	 *
	 * @return the output file, where the generated data will be saved
	 */
	public String getOutputFile() {

		return outputFile;
	}

	/**
	 * get the image geometry.
	 * 
	 * @return an array with the image geometry information
	 * @throws ParameterException when it is set in decompression mode
	 */
	public int[] getImageGeometry() {
		if(action == 1) {
			if (imageGeometry == null) {
				return null;
			} else {
				new ParameterException("Option -ig can only be used in compression mode").printStackTrace();
			}
		}
		
		try {

			if ((imageGeometry[CONS.BANDS] <= 0) || (imageGeometry[CONS.HEIGHT] <= 0)
					|| (imageGeometry[CONS.WIDTH] <= 0)) {

				throw new WarningException("Image dimensions in \".raw\" or \".img\" data files"
						+ " must be positive (\"-h\" displays help).");
			}

			if ((imageGeometry[CONS.TYPE] < 1) || (imageGeometry[CONS.TYPE] > 3)) {

				throw new WarningException("Invalid pixel type");
			}   

			if ((imageGeometry[CONS.RGB] != 0) && (imageGeometry[CONS.RGB] != 1)) {

				throw new WarningException("Image RGB specification in \".raw\" or \".img\" data must "
						+ "be between 0 or 1 (\"-h\" displays help).");
			}
			
		} catch (WarningException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}	
		return imageGeometry;
	}

	/**
	 * get sampleOrder.
	 *
	 * @return the sample order of the image.
	 */
	public int getSampleOrder() {

		try {
			if (sampleOrder < 0 || sampleOrder > 2) {

				throw new WarningException("Sample Order must be 0, 1 or 2");
			}
		} catch (WarningException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}	

		return sampleOrder;
	}

	/**
	 * get endianess.
	 *
	 * @return 0 if image must be saved in BIG_ENDIAN byte order, 
	 * 1 if LITTLE_ENDIAN.
	 */
	public int getEndianess() {

		return endianess;
	}

	/**
	 * get verbose.
	 *
	 * @return true if output information must be given, false otherwise
	 */
	public boolean getVerbose() {

		return verbose;
	}

	/**
	 * get optionFile.
	 *
	 * @return the file where can be defines parameters for the algorithms
	 */
	public String getOptionFile() {

		return optionFile;
	}
	/**
	 * get debugMode.
	 *
	 * @return the value which indicates if debug mode is used
	 */
	public boolean getDebugMode() {
		return debugMode;
	}
	/**
	 * get optionString.
	 *
	 * @return string that has values for the parameters used in the
	 * application
	 */
	public String getOptionString() {

		return optionString;
	}
	
	/**
	 * Returns a boolean that indicates if a file with the differences must
	 * be generated or not (in compression mode)
	 * @return deltaMode if delta file must be generated or not
	 */
	public boolean getDeltaMode() {
		if (action == 1 && deltaMode == true) {
			System.err.println(new ParameterException("Option -delta can only be set in compression mode").getMessage());
			System.exit(-1);
		}
		return deltaMode;
	}

	/**
	 * get pixelFormat
	 * @return pixelFormat an integer indicating in which format the pixels
	 * of the decompressed image will be stored
	 * @throws WarningException if the value set for pixel format is not valid
	 * @throws ParameterException if pixelFormat is set in compression mode
	 */
	public int getPixelFormat()  {
		try {
			if ((pixelFormat < 0 || pixelFormat > 3 ) && action == 1) {
				throw new WarningException("pixel format (-format) must be a value between 1 and 3." + 
											" Value 0 is ignored.");
			}
			// this option can not be set in compression mode
			if (action == 0 && pixelFormat > 0) {
				throw new ParameterException("option -format must be set in decompression mode only");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}	
		return pixelFormat;
	}
	
	/**
	 * Getter for saveState
	 * @return saveState
	 */
	public boolean getSaveState() {
		return saveState;
	}
	
	/**
	 * Getter for bitCost
	 * @return bitCost
	 */
	public int getBitCost() {
		if (action == 1 && (bitCost >= 1 || bitCost <= 2)) {
			System.err.println(new ParameterException("Option -bitCost can only be set in compression mode").getMessage());
			System.exit(-1);
		}
		if(bitCost < 0 || bitCost > 2) {
			System.err.println(new ParameterException("bitCost value must be between 0 a 2").getMessage());
			System.exit(-1);
		}
		return bitCost;
	}
	
	/**
	 * Getter for pedantic
	 * @return pedantic
	 */
	public boolean getPedantic() {
		return pedantic;
	}
	
	/**
	 * Getter for time
	 * @return time
	 */
	public boolean getTime() {
		return time;
	}
	
}

