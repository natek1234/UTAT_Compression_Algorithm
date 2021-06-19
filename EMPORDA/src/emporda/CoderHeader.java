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
import GiciException.ParameterException;
import GiciMath.*;
import java.io.*;

/**
 * CoderHeader class of EMPORDA application. CoderHeader writes the headers as
 * it is told on the Recommended Standard MHDC-123 White Book.
 * <p>
 * 
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class CoderHeader {

	private BitOutputStream bos;
	private boolean debugMode = false;
	private long bitsWritten = 0;
	private int[] geo;
	private Parameters parameters;

	/**
	 * Constructor of CoderHeader. It receives the BitOutputStream, that will
	 * write all the information to the output file.
	 * 
	 * @param bos
	 *            the bit output stream
	 * @param debugMode
	 *            indicates whether to display debug information
	 * @param parameters
	 *            all the information about the coding process
	 */
	public CoderHeader(final BitOutputStream bos, boolean debugMode,
			Parameters parameters) {
		this.bos = bos;
		this.debugMode = debugMode;
		this.parameters = parameters;
		this.geo = parameters.getImageGeometry();
	}

	/**
	 * Writes the image information to the header of the compressed file
	 * 
	 * @throws IOException
	 *             if can not write information to the file
	 */
	public void imageHeader() throws IOException {
		int sampleType = (geo[CONS.TYPE] == 3 || geo[CONS.TYPE] == 1) ? 1 : 0;
		int bitsWritten = 0;

		if (debugMode) {
			System.out.println();
			System.out.println("Image Header");
			System.out.println("\t user defined data: 0 " +
					"(8 bits)");
			System.out.println("\t image width: " + geo[CONS.WIDTH] % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t image height: " + geo[CONS.HEIGHT] % (1 << 16) + 
					"(16 bits)");
			System.out.println("\t image bands: " + geo[CONS.BANDS] % (1 << 16) + 
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

		}
		bos.write(CONS.BYTE, 0); /* User defined data */
		bos.write(CONS.SHORT, geo[CONS.WIDTH]); /* width */
		bos.write(CONS.SHORT, geo[CONS.HEIGHT]); /* height */
		bos.write(CONS.SHORT, geo[CONS.BANDS]); /* bands */
		bitsWritten += CONS.BYTE + CONS.SHORT * 3;

		/* SampleType|00|Dynamic Range|Sample Encoding Order */
		bos.write(1, sampleType);
		bos.write(2, 0);
		bos.write(4, parameters.dynamicRange);
		bos.write(1, parameters.sampleEncodingOrder);
		bitsWritten += 1 + 2 + 4 + 1;

		/* Sub-frame interleaving depth */
		bos.write(CONS.SHORT, parameters.subframeInterleavingDepth);
		bitsWritten += CONS.SHORT;

		/* 00|Output word Size| ECT | PMF | ECMF */
		bos.write(2, 0);
		bos.write(3, parameters.outputWordSize);
		bos.write(1, parameters.entropyCoderType);
		bos.write(CONS.BYTE + 2, 0);
		bitsWritten += 2 + 3 + 1 + 1 + 1 + CONS.BYTE;

		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		this.bitsWritten += bitsWritten;
		bos.flush();
	}

	/**
	 * Writes information about the prediction process to the header of the
	 * compressed file
	 * 
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void predictorMetadata() throws IOException, ParameterException {
		int bitsWritten = 0;
		if (debugMode) {
			System.out.println("Predictor metadata");
			System.out.println("\t free space: " + 0 + 
					"(2 bits)");
			System.out.println("\t number prediction bands: " + parameters.numberPredictionBands + 
					"(4 bits)");
			System.out.println("\t prediction mode: " + parameters.predictionMode + 
					"(1 bit)");
			System.out.println("\t free space: " + 0 + 
					"(1 bit)");
			System.out.println("\t local sum mode: " + parameters.localSumMode + 
					"(1 bit)");
			System.out.println("\t free space: " + 0 + 
					"(1 bit)");
			System.out.println("\t register size: " + parameters.registerSize % (1 << 6) + 
					"(6 bits)");
			System.out.println("\t weight component resolution: " + (parameters.weightComponentResolution - 4) + 
					"(4 bit)");
			System.out.println("\t exponent change interval: " + (parameters.tinc - 4) + 
					"(4 bits)");
			System.out.println("\t weight update scaling exponent: " + (parameters.vmin + 6) + 
					"(4 bit)");
			System.out.println("\t weight update scaling exponent final parameter: " + (parameters.vmax + 6) + 
					"(4 bits)");
			System.out.println("\t free space: " + 0 + 
					"(1 bit)");
			System.out.println("\t weight initialization method: " + parameters.weightInitMethod + 
					"(1 bits)");
			System.out.println("\t weight initialization table flag: " + parameters.weightInitTableFlag + 
					"(1 bit)");
			System.out.println("\t weight initialization resolution: " + parameters.weightInitResolution + 
					"(5 bits)");

		}
		/* 00 | numBands | predictionMode | 0 */
		bos.write(2, 0);
		bos.write(4, parameters.numberPredictionBands);
		bos.write(1, parameters.predictionMode);
		bos.write(1, 0);
		bitsWritten += 2 + 4 + 1 + 1;

		/* localSumMode | 0 | RegisterSize */
		bos.write(1, parameters.localSumMode);
		bos.write(1, 0);
		bos.write(6, parameters.registerSize);
		bitsWritten += 1 + 1 + 6;

		/* weightComponentResolution | expChangeInterval */

		bos.write(4, parameters.weightComponentResolution - 4);
		bos.write(4, parameters.tinc - 4);
		bitsWritten += 4 + 4;

		/* weightUpdateScalExp | weightUpdateScalExpFinalParameter */

		bos.write(4, parameters.vmin + 6);
		bos.write(4, parameters.vmax + 6);
		bitsWritten += 4 + 4;

		/* 0 | weightInit | weightInitTableFlag | weightInitresol */
		bos.write(1, 0);
		bos.write(1, parameters.weightInitMethod);
		bos.write(1, parameters.weightInitTableFlag);
		bos.write(5, parameters.weightInitResolution);
		bos.flush();
		bitsWritten += 1 + 1 + 1 + 5;

		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		this.bitsWritten += bitsWritten;
		if (parameters.weightInitTableFlag == 1) {
			weightInitializationTable();
		}

	}

	/**
	 * Writes the weight initialization table to the header of the 
	 * compressed file
	 * 
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void weightInitializationTable() throws IOException,
			ParameterException {
		int zlength = geo[CONS.BANDS];
		int cont = 0;
		int sumBands = 3 * (1 - parameters.predictionMode);
		int bitsWritten = 0;
		int[][] weightInitTable = parameters.getWeightInitTable();

		if (debugMode) {
			System.out.println("coding weight initialization table ");
		}
		for (int z = 0; z < zlength; z++) {
			int nBands = Math.min(parameters.numberPredictionBands, z)
					+ sumBands;
			bitsWritten += parameters.weightInitResolution * nBands;
			for (int i = 0; i < nBands; i++) {
				bos.write(parameters.weightInitResolution, (weightInitTable[z][i] >= 0)?
							weightInitTable[z][i]: weightInitTable[z][i] + (1 << parameters.weightInitResolution));
				cont++;
			}
		}

		if (cont * parameters.weightInitResolution % 8 != 0) {
			bos.write(8 - (cont * parameters.weightInitResolution % 8), 0);
			bitsWritten += 8 - (cont * parameters.weightInitResolution % 8);
		}
		this.bitsWritten += bitsWritten;

		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		bos.flush();
	}

	/**
	 * Writes information about the entropy coder to the header of the
	 * compressed file
	 * 
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void entropyCoderMetadata() throws IOException, ParameterException {

		if (parameters.entropyCoderType == CONS.SAMPLE_ADAPTIVE_ENCODER) {
			sampleEntropyCoderMetadata();
		} else {
			blockEntropyCoderMetadata();
		}
	}

	/**
	 * Writes information about the sample entropy code to the header of
	 * the image file
	 * 
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void sampleEntropyCoderMetadata() throws IOException,
			ParameterException {
		int bitsWritten = 0;
		if (debugMode) {
			System.out.println("Sample entropy coder metadata");
			System.out.println("\t unary length limit: "
					+ (parameters.unaryLengthLimit % 32) + 
					"(5 bits)");
			System.out.println("\t rescaling counter size: " + (parameters.rescalingCounterSize - 4) + 
					"(3 bits)");
			System.out.println("\t initial count exponent: "
					+ (parameters.initialCountExponent % 8) + 
					"(3 bits)");
			System.out.println("\t accumulator initialization constant: " + (parameters.accInitConstant) + 
					"(4 bits)");
			System.out.println("\t accumulator initialization table flag: " + (parameters.accInitTableFlag) + 
					"(1 bit)");
		}
		/* UMAX | RCS */
		bos.write(5, parameters.unaryLengthLimit);
		bos.write(3, parameters.rescalingCounterSize - 4);
		bitsWritten += 5 + 3;

		/*
		 * Initial Count Exponent | Accumulator initialization Constant |
		 * Accumulator Initialization Table Flag
		 */

		bos.write(3, parameters.initialCountExponent);
		bos.write(4, parameters.accInitConstant);
		bos.write(1, parameters.accInitTableFlag);
		bitsWritten += 3 + 4 + 1;
		bos.flush();

		this.bitsWritten += bitsWritten;
		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		if (parameters.accInitTableFlag == 1) {
			accumulatorInitializationTable();
		}
	}

	/**
	 * Writes the accumulator initialization table to the header of the
	 * compressed file
	 * 
	 * @param accumulatorTable
	 *            the initialization table for the statistics of the entropy
	 *            coder
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void accumulatorInitializationTable() throws IOException,
			ParameterException {
		int[] accumulatorTable = parameters.getAccInitTable();
		int length = accumulatorTable.length;
		int bitsWritten = 0;

		if (debugMode) {
			System.out.println("coding accumulator initialization table ");
		}
		for (int i = 0; i < length; i++) {
			bos.write(4, accumulatorTable[i]);
			bitsWritten += 4;
		}
		/* we make sure an integer number of bytes is written */
		if (length % 2 == 1) {
			bos.write(4, 0);
			bitsWritten += 4;
		}
		this.bitsWritten += bitsWritten;
		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		bos.flush();

	}

	/**
	 * Writes information about the block entropy coder to the header of the
	 * image file
	 * 
	 * @param entropyOption
	 *            information about the entropy coder algorithm
	 * @throws IOException
	 *             if can not write information to the file
	 * @throws ParameterException
	 *             when an invalid parameter is detected
	 */
	public void blockEntropyCoderMetadata() throws IOException,
			ParameterException {

		int bitsWritten = 0, blockSize = 0;
        int restrictIdBits = 0;


		blockSize = IntegerMath.log2(parameters.blockSize >> 3);
        if(parameters.dynamicRange <= 4 && parameters.restrictIdBits == 1) {
            restrictIdBits = 1;
        } else {
            restrictIdBits = 0;
        }

		if (debugMode) {
			System.out.println("Block entropy coder metadata");
			System.out.println("\t free space: " + 0 + 
					"(1 bit)");
			System.out.println("\t block size: " + blockSize + 
					"(2 bits)");
			System.out.println("\t restrict set of code options: " + restrictIdBits + 
					"(1 bit)");
			System.out.println("\t reference sample interval: " + (parameters.referenceSampleInterval % (1 << 12)) + 
					"(12 bits)");
		}

		bos.write(1, 0);
		bos.write(2, blockSize);
		bitsWritten += 1 + 2;
        bos.write(1, restrictIdBits);
		bos.write(12, parameters.referenceSampleInterval);
		bitsWritten += 1 + 12;
		if (debugMode) {
			System.out.println("\twritten " + (bitsWritten / 8) + " bytes "
					+ " and " + (bitsWritten % 8) + " bits");
		}
		this.bitsWritten += bitsWritten;
		bos.flush();
	}

	public long getBitsWritten() {
		return bitsWritten;
	}
}
