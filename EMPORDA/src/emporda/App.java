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

import java.io.FileInputStream;
/**
 * Main class of EMPORDA application of the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class App {

	/**
	 * Main function.
	 *
	 * @params args input command line
	 */
	public static void main(final String[] args) {
		Parser parser = null;
		try {
			parser = new Parser(args);
			
		} catch (ErrorException e) {
			System.err.println("RUN ERROR:");
			System.err.println("Please report this error (specifying "
					   + "image type and parameters) to: gici-dev@abra.uab.es");
			System.exit(-1);

		} catch (ParameterException e) {
			System.err.println("ARGUMENTS ERROR: " + e.getMessage());
			System.exit(-1);
		}
		if (parser.getAction() == 0) {
			compress(parser);
		} else {
			decompress(parser);
		}
	}

	/**
	 * Runs all the compression and coding process.
	 *
	 * @params parser the command line options of the user
	 */
	public static void compress(final Parser parser) {

		int sampleOrder = parser.getSampleOrder();
		String inputFile = parser.getInputFile();
		String outputFile = parser.getOutputFile();
		boolean verbose = parser.getVerbose();
		boolean debugMode = parser.getDebugMode();
		boolean deltaMode = parser.getDeltaMode(); //if it has been set, an exception is thrown
		int[] geo = null;
		Parameters parameters = null;
		Coder encoder = null;
		int format = parser.getPixelFormat(); //if it has been set, an exception is thrown
		
		geo = parser.getImageGeometry();
		geo[CONS.ENDIANESS] = parser.getEndianess(); 

		try {
			
			if(debugMode) {
				System.out.println("debug info: loading parameters");
			}
			if(parser.getOptionFile() == null) {
				parameters = new Parameters(parser.getOptionString(), geo, true, debugMode, parser.getPedantic());
			} else {
				parameters = new Parameters(new FileInputStream(parser.getOptionFile()), geo, true, debugMode, parser.getPedantic());
			}

			if (parser.getPedantic()) {
				if (geo[0] <= parameters.numberPredictionBands) {
					System.err.println("Pedantic: this image has " + geo[0] + " bands and the number of bands " +
							"used in the prediction process is " + parameters.numberPredictionBands + ", in fact, " +
							" the number of bands of the image must be bigger than the number of bands used in the prediction");
					System.exit(-1);
				}
			}
			if(debugMode) {
				System.out.println("debug info: starting coder");
			}
			encoder = new Coder(outputFile, inputFile, sampleOrder, parameters, debugMode, parser.getTime());
			encoder.setDeltaMode(parser.getDeltaMode());
			encoder.setSaveState(parser.getSaveState());
			encoder.setBitCost(parser.getBitCost());
			if(debugMode) {
				System.out.println("debug info: writting image header");
			}
			encoder.writeHeader(parameters);
			if(debugMode) {
				System.out.println("debug info: coding image");
			}
			encoder.code(verbose);
			encoder = null;
			parameters = null;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		if (verbose || debugMode) {
			System.out.println("Compression process ended successfully");
		}
	}

	/**
	 * Runs the decoding and decompression process.
	 *
	 * @params parser the command line options of the user
	 */
	public static void decompress(final Parser parser) {

		int sampleOrder = parser.getSampleOrder();
		String inputFile = parser.getInputFile();
		String outputFile = parser.getOutputFile();
		String optionFile = parser.getOptionFile();
		String optionString = parser.getOptionString();
		boolean verbose = parser.getVerbose();
		boolean debugMode = parser.getDebugMode();
		int[] geo = parser.getImageGeometry();
		Parameters parameters = null;
		Decoder decoder = null;
		boolean deltaMode = parser.getDeltaMode(); // used to avoid this boolean being set
		
		try {
			
			if(debugMode) {
				System.out.println("debug info: starting decoder");
			}
			
			decoder = new Decoder(inputFile, outputFile, debugMode, sampleOrder, parser.getPixelFormat(), parser.getTime());
			
			if (debugMode) {
				System.out.println("debug info: reading image header and loading parameters");
			}
			if(optionFile == null) {
				parameters = decoder.readHeader(optionString);
			} else {
				parameters = decoder.readHeader(new FileInputStream(optionFile));
			}
			geo = parameters.getImageGeometry();
			geo[CONS.ENDIANESS] = parser.getEndianess();

			if(debugMode) {
				System.out.println("debug info: decoding image");
			}
			decoder.decode(verbose);
			decoder = null;

			
			parameters = null;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		if (verbose || debugMode) {
			System.out.println("Decompression process ended succesfully");
		}
	}
	
}
