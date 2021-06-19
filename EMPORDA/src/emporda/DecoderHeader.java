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

import GiciStream.*;
import GiciMath.*;
import java.io.*;


/**
 * DecoderHeader class of EMPORDA application. DecoderHeader reads the headers
 * as it is told on the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class DecoderHeader {

	private BitInputStream bis = null;
	private boolean debugMode = false;
	private Parameters parameters = null;
	private int bitsRead = 0;

	/**
	 * Constructor of DecoderHeader. It receives the BitIntputStream, that
	 * will read all the information to the input file.
	 *
	 * @param bis the bit input stream
	 */
	public DecoderHeader(final BitInputStream bis, Parameters parameters, boolean debugMode) {

		this.bis = bis;
		this.parameters = parameters;
		this.debugMode = debugMode;
	}
	/**
	 * Reads all the header of the compressed image, and saves all the parameters to an instance
	 * of the class Parameters
	 * @throws IOException
	 */
	public void readImageHeader() throws IOException {
		imageHeader();
		predictorMetadata();
		entropyCoderMetadata();
	}
	/**
	 * Reads the header relative to image information.
	 *
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public void imageHeader() throws IOException {

		int bitsRead = 0;
		int[] imageGeometry = parameters.getImageGeometry();
		int sampleType = 0;
		
		bis.read(CONS.BYTE); /*User defined data*/
		imageGeometry[CONS.WIDTH] = bis.read(CONS.SHORT);
		imageGeometry[CONS.WIDTH] = (imageGeometry[CONS.WIDTH] == 0)
									? (1 << 16) : imageGeometry[CONS.WIDTH]; /*X Value*/
		imageGeometry[CONS.HEIGHT] = bis.read(CONS.SHORT);
		imageGeometry[CONS.HEIGHT] = (imageGeometry[CONS.HEIGHT] == 0)
									? (1 << 16) : imageGeometry[CONS.HEIGHT]; /*Y Value*/
		imageGeometry[CONS.BANDS] = bis.read(CONS.SHORT);
		imageGeometry[CONS.BANDS] = (imageGeometry[CONS.BANDS] == 0)
									? (1 << 16) : imageGeometry[CONS.BANDS]; /*Z Value*/
		bitsRead += CONS.BYTE + CONS.SHORT*3;
		
		/* SampleType|00|Dynamic Range|Sample Encoding Order */
		sampleType = bis.read(1);
		imageGeometry[CONS.TYPE] = (sampleType == 0) ? 2 : 3; 
		bis.read(2);
		parameters.setImageGeometry(imageGeometry);
		parameters.dynamicRange = bis.read(4);
		parameters.dynamicRange = (parameters.dynamicRange == 0) ? (1 << 4) : parameters.dynamicRange;

		/* Sample order */
		parameters.sampleEncodingOrder = bis.read(1);
		bitsRead += 1 + 2 + 4 + 1;
		/* Sub-frame interleaving depth */
		parameters.subframeInterleavingDepth = bis.read(CONS.SHORT);
		if(parameters.sampleEncodingOrder == 0) {
			parameters.subframeInterleavingDepth = (parameters.subframeInterleavingDepth == 0) ? (1 << CONS.SHORT) : parameters.subframeInterleavingDepth;
		}
		bitsRead += CONS.SHORT;
		
		/* 00|Output word Size| ECT | PMF | ECMF */
		bis.read(2);
		parameters.outputWordSize = bis.read(3);
		parameters.outputWordSize = (parameters.outputWordSize == 0) ? (1 << 3) : parameters.outputWordSize;        
		parameters.entropyCoderType = bis.read(1);
		bis.read(1);  // this reads the deprecated predictor metadata flag
		bis.read(1);  // this reads the deprecated entropy coder metadata flag 
		bitsRead += 2 + 3 + 1 + 1 + 1;
		/*Reserved*/
		bis.read(CONS.BYTE);
		bitsRead += CONS.BYTE;

		if (debugMode) {
			System.out.println();
			System.out.println("Image Header");
			System.out.println("\t user defined data: 0 " +
					"(8 bits)");
			System.out.println("\t image width: " + imageGeometry[CONS.WIDTH] % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t image height: " + imageGeometry[CONS.HEIGHT] % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t image bands: " + imageGeometry[CONS.BANDS] % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t sample type: " + sampleType + 
					"(1 bit)");
			System.out.println("\t free space: " + 0 + 
					"(2 bits)");
			System.out.println("\t dynamic range: " + parameters.dynamicRange % (1 << 4) + 
					"(4 bits)");
			System.out.println("\t sample encoding order: " + parameters.sampleEncodingOrder + 
					"(1 bit)");
			System.out.println("\t subframe interleaving depth: " + parameters.subframeInterleavingDepth % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t free space: " + 0 + 
					"(2 bits)");
			System.out.println("\t output word size: " + parameters.outputWordSize % (1 << 3) + 
					"(3 bit)");
			System.out.println("\t entropy coder type: " + parameters.entropyCoderType + 
					"(1 bit)");
			System.out.println("\t free space: " + 0 + 
					"(10 bits)");
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
		
		this.bitsRead += bitsRead;
	}



	/**
	 * Reads the header relative to the predictor.
	 *
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public void predictorMetadata()  throws IOException {
		int bitsRead = 0;
		
		/* 00 | numBands | predictionMode | 0 */
		bis.read(2);
		parameters.numberPredictionBands = bis.read(4);
		parameters.predictionMode = bis.read(1);
		bis.read(1);

		/* localSumMode | 0 | RegisterSize */
		parameters.localSumMode = bis.read(1);
		bis.read(1);
		parameters.registerSize = bis.read(6);
		parameters.registerSize = (parameters.registerSize == 0) ? (1 << 6) : parameters.registerSize;
		bitsRead += 2 + 4 + 1 + 1 + 1 + 1 + 6;
		
		/* weightComponentResolution | expChangeInterval */

		parameters.weightComponentResolution = bis.read(4) + 4;
		parameters.tinc = bis.read(4) + 4;

		/* weightUpdateScalExp | weightUpdateScalExpFinalParameter */
		parameters.vmin = bis.read(4) - 6;
		parameters.vmax = bis.read(4) - 6;
		/* 0 | weightInit | weightInitTableFlag | weightInitresol */
		bis.read(1);
		bitsRead += 4 + 4 + 4 + 4 + 1;
		
		parameters.weightInitMethod = bis.read(1);
		parameters.weightInitTableFlag = bis.read(1);
		parameters.weightInitResolution = bis.read(5);
		bitsRead += 1 + 1 + 5;
		
		if(debugMode) {
			System.out.println("Predictor metadata");
			System.out.println("\t free space: " + 0  
					+ "(2 bits)");
			System.out.println("\t number prediction bands: " +  parameters.numberPredictionBands
					+ "(4 bits)");
			System.out.println("\t prediction mode: " + parameters.predictionMode
					+ "(1 bit)");
			System.out.println("\t free space: " + 0
					+ "(1 bit)");
			System.out.println("\t local sum mode: " + parameters.localSumMode
					+ "(1 bit)");
			System.out.println("\t free space: " + 0
					+ "(1 bit)");
			System.out.println("\t register size: " + parameters.registerSize % (1 << 6)
					+ "(6 bits)");
			System.out.println("\t weight component resolution: " + (parameters.weightComponentResolution - 4)
					+ "(4 bit)");
			System.out.println("\t exponent change interval: " + (parameters.tinc - 4)
			        + "(4 bits)");
			System.out.println("\t weight update scaling exponent: " + (parameters.vmin + 6)
					+ "(4 bit)");
			System.out.println("\t weight update scaling exponent final parameter: " + (parameters.vmax + 6)
			        + "(4 bits)");
			System.out.println("\t free space: " + 0
					+ "(1 bit)");
			System.out.println("\t weight initialization method: " + parameters.weightInitMethod
			        + "(1 bits)");
			System.out.println("\t weight initialization table flag: " + parameters.weightInitTableFlag
					+ "(1 bit)");
			System.out.println("\t weight initialization resolution: " + parameters.weightInitResolution
			        + "(5 bits)");
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
		this.bitsRead += bitsRead;
		if (parameters.weightInitTableFlag == 1) {
			parameters.setWeightInitTable(weightInitializationTable());
		}
	}


	/**
	 * Reads the header relative to initialization table.
	 *
	 * @return the initialization table of the compression algorithm
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public int[][] weightInitializationTable() throws IOException {
		
		int numBands = parameters.getImageGeometry()[CONS.BANDS];
		int[][] initializationTable = new int[numBands][];
		int sumBands = 3 * (1 - parameters.predictionMode);
		int cont = 0;
		int bitsRead = 0;
		if (debugMode) {
			System.out.println("decoding weight initialization table ");
		}
		for (int z = 0; z < numBands; z++) {

			int numIt = Math.min(parameters.numberPredictionBands, z) + sumBands;
			bitsRead += numIt*parameters.weightInitResolution;
			initializationTable[z] = new int[numIt];

			for (int i = 0; i < numIt; i++) {

				initializationTable[z][i] = bis.read(parameters.weightInitResolution);
				initializationTable[z][i] = (initializationTable[z][i] < (1 << (parameters.weightInitResolution - 1)))?
											initializationTable[z][i]: initializationTable[z][i] - (1 << parameters.weightInitResolution);
				cont++;
			}
		}
		if (cont*parameters.weightInitResolution % 8 != 0) {
			bis.read(8 - (cont * parameters.weightInitResolution % 8));
			bitsRead += 8 - (cont * parameters.weightInitResolution % 8);
		}
		this.bitsRead += bitsRead;
		if (debugMode) {
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
		return initializationTable;
	}

	/**
	 * Reads the header relative to the entropy coder.
	 *
	 * @return the initialization table for the statistics of the entropy coder
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public int[] entropyCoderMetadata() throws IOException {
		int[] accumulatorTable = null;
		
		if (parameters.entropyCoderType == CONS.SAMPLE_ADAPTIVE_ENCODER){
			sampleEntropyCoderMetadata() ;
		}
		else {
			blockEntropyCoderMetadata();
		}

		return accumulatorTable;
	}

	/**
	 * Reads the header relative to the entropy coder.
	 *
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public void sampleEntropyCoderMetadata() throws IOException {
		int bitsRead = 0;
		
		
		/* UMAX | RCS */
		parameters.unaryLengthLimit = bis.read(5);
		parameters.unaryLengthLimit = (parameters.unaryLengthLimit == 0) ? 32 : parameters.unaryLengthLimit;
		parameters.rescalingCounterSize = bis.read(3) + 4;
		bitsRead += 5 + 3;
		
		/* ICE | AITF | AIC */
		parameters.initialCountExponent = bis.read(3);
		parameters.initialCountExponent = (parameters.initialCountExponent == 0) ? 8 : parameters.initialCountExponent;
		parameters.accInitConstant = bis.read(4);
		parameters.accInitTableFlag = bis.read(1);
		bitsRead += 3 + 4 + 1;
		
		this.bitsRead += bitsRead;
		if (debugMode) {
			System.out.println("Sample entropy coder metadata");
			System.out.println("\t unary length limit: " + (parameters.unaryLengthLimit % 32) 
					+ "(5 bits)");
			System.out.println("\t rescaling counter size: " +  (parameters.rescalingCounterSize - 4)
					+ "(3 bits)");
			System.out.println("\t initial count exponent: " + (parameters.initialCountExponent % 8)
					+ "(3 bits)");
			System.out.println("\t accumulator initialization constant: " + (parameters.accInitConstant)
					+ "(4 bits)");
			System.out.println("\t accumulator initialization table flag: " + (parameters.accInitTableFlag)
			        + "(1 bit)");
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
		
		if (parameters.accInitTableFlag == 1) {
			parameters.setAccInitTable(getAccInitTable(parameters.getImageGeometry()[CONS.BANDS]));
		}
	}

	/**
	 * Reads the header relative to the accumulator table.
	 *
	 * @param nbands the number of bands of the image
	 * @return the initialization table for the statistics of the entropy coder
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public int[] getAccInitTable(int nBands) throws IOException {
		int[] accumulatorTable = new int[nBands];
		int bitsRead = 0;
		
		for (int i = 0; i < nBands; i++) {
			accumulatorTable[i] = bis.read(4);
			bitsRead += 4;
		}
		if (nBands % 2 == 1) {
			bis.read(4);
			bitsRead += 4;
		}
		this.bitsRead += bitsRead;
		
		if (debugMode) {
			System.out.println("coding accumulator initialization table ");
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
		return accumulatorTable;
	}

	/**
	 * Reads the header relative to the block entropy coder.
	 *
	 * @throws IOException when something goes wrong and writing must be stopped
	 */
	public void blockEntropyCoderMetadata() throws IOException
	{
		int bitsRead = 0, blockSize;
		
		bis.read(1);
		blockSize = bis.read(2);
		parameters.blockSize = 8 << blockSize;
		parameters.restrictIdBits = bis.read(1);
		bitsRead += 1 + 2 + 1;
		parameters.referenceSampleInterval = bis.read(12);
		parameters.referenceSampleInterval = (parameters.referenceSampleInterval == 0) ? 4096 : parameters.referenceSampleInterval;
		bitsRead += 12;
		
		this.bitsRead += bitsRead;
		
		if (debugMode) {
			System.out.println("Block entropy coder metadata");
			System.out.println("\t free space: " + 0 
					+ "(1 bit)");
			System.out.println("\t block size: " +  blockSize
					+ "(2 bits)");
			System.out.println("\t restrict set of code options: " + parameters.restrictIdBits + 
					"(1 bit)");
			System.out.println("\t reference sample interval: " + (parameters.referenceSampleInterval % (1 << 12))
					+ "(12 bits)");
			System.out.println("\tread " + (bitsRead/8) + " bytes " + " and " + (bitsRead % 8) + " bits");
		}
	}

	/**
	 * Finishes the header reading process and shows how many bits have been read if necessary
	 */
	public void finish() {
		if(debugMode) {
			System.out.println("debug_info: total bits read: " + bitsRead);
		}
	}
}

