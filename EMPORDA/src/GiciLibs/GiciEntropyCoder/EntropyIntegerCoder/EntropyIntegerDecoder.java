/*
 * GiciLibs - EntropyIntegerCoder an implementation of MHDC Recommendation for Image Data Compression
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
package GiciEntropyCoder.EntropyIntegerCoder;

import GiciEntropyCoder.Interface.EntropyDecoder;
import GiciException.ParameterException;
import java.io.IOException;
import GiciMath.IntegerMath;
import GiciStream.BitInputStream;

/**
 * Decoder class of the EntropyIntegerCoder. EntropyIntegerDecoder is a coder of
 * the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class EntropyIntegerDecoder implements EntropyDecoder {

	private BitInputStream bis = null;

	/**
	 * the accumulator array, one element of the array for one band
	 */
	private int[] accumulator;

	/**
	 * the counter array, one element of the array for one band
	 */
	private int[] counter;

	/**
	 * number of bits read of the stream
	 */
	private int numBitsRead;

	/**
	 * all the options needed for the encoding process
	 */
	private final int initialCountExponent;
	private final int accumulatorInitConstant;
	private final int rescalingCounterSize;

	private final int dynamicRange;
	private final int unaryLengthLimit;

	/**
	 * array with the initial accumulator values
	 */
	private int[] accumulatorTable = null;

	/**
	 * Constructor.
	 *
	 * @param bis the bit input stream
	 * @param initialCountExponent Initial Count Exponent
	 * @param accumulatorInitConstant Accumulator Initialization Constant
	 * @param rescalingCounterSize Rescaling Counter Size
	 * @param dynamicRange DynamicRange
	 * @param unaryLengthLimit Unary Length Limit
	 * @param accumulatorTable the table with the initial values of the accumulator
	 * @param bands is the number of bands of the image.
	 */
	public EntropyIntegerDecoder(
		BitInputStream bis,
		int initialCountExponent,
		int accumulatorInitConstant,
		int rescalingCounterSize,
		int dynamicRange,
		int unaryLengthLimit,
		int[] accumulatorTable,
		int bands)
	{
		this.bis = bis;
		this.numBitsRead = 0;

		this.initialCountExponent = initialCountExponent;
		this.accumulatorInitConstant = accumulatorInitConstant;
		this.rescalingCounterSize = rescalingCounterSize;
		this.dynamicRange = dynamicRange;
		this.unaryLengthLimit = unaryLengthLimit;
		this.accumulatorTable = accumulatorTable;

		accumulator = new int[bands];
		counter = new int[bands];

	}

	/**
	 * Initializes the statistics
	 *
	 * @param z the band number
	 */
	public void init(int z) {
		int accInit = 0;
		
		counter[z] = 1 << initialCountExponent;
		if (accumulatorInitConstant < 15) {
			accInit = accumulatorInitConstant;
		} else if(accumulatorInitConstant == 15) {
			accInit = accumulatorTable[z];
			
		} else {
			try {
				throw new ParameterException("PARAMS ERROR: ACCUMULATOR_INITIALIZATION_CONSTANT has been " + 
							"set to an invalid value: " + accumulatorInitConstant);
			} catch (ParameterException e) {
				e.printStackTrace();
			}
		}
		accumulator[z] = (3 * (1 << (accInit + 6)) - 49) * counter[z];
		accumulator[z] >>= 7;
	}

	/**
	 * Updates the statistics for every sample
	 *
	 * @param sample the sample that has been coded
	 * @param t the number of the sample in the encoding sequence
	 * @param z the band of the sample
	 */
	public void update(int sample, int t, int z) {

		int limit = (1 << rescalingCounterSize) - 1;

		if (t > 0) {
			if (counter[z] < limit) {
				accumulator[z] += sample;
				counter[z]++;

			} else {
				accumulator[z] = accumulator[z] + sample + 1 >> 1;
				counter[z] = counter[z] + 1 >> 1;
			}
		}
	}

	/**
	 * Decodes a sample
	 *
	 * @param t the number of the sample in the encoding sequence
	 * @param z the band of the sample
	 * @return the symbol that has been just decoded
	 * @throws IOException if can not read file information
	 */
	public int decodeSample(int t, int z) throws IOException {

		int decodedSample = 0;

		if (t == 0) {
			decodedSample = bis.read(dynamicRange);
			numBitsRead += dynamicRange;
			return decodedSample;
		}

		int buffer = -1, u_z = 0;
		int k_z = IntegerMath.log2((accumulator[z] + (49*counter[z] >> 7)) / counter[z]);
		 

		k_z = (k_z < 0) ? 0 : k_z;
		k_z = (k_z > dynamicRange - 2) ? dynamicRange - 2 : k_z;
		
		for (; u_z < unaryLengthLimit && buffer != 1; u_z ++) {
			buffer = bis.read(1);
		}
		numBitsRead += u_z;
		if (buffer == 1 && u_z > 0) {
			u_z--;
		} 
		
		if (u_z < unaryLengthLimit) {
			int leastSignificantBits = bis.read(k_z);
			decodedSample = (u_z << k_z) + leastSignificantBits;
			numBitsRead += k_z;
			
		} else {
			decodedSample = bis.read(dynamicRange);
			numBitsRead += dynamicRange;
		}
		return decodedSample;
	}

	/**
	 * Finishes the decoding process
	 * 
	 * @throws IOException if the input file can not be closed
	 */
	public void terminate() throws IOException {
		bis.close();
	}
	
	/**
	 * Finishes the decoding process
	 *
	 * @param verbose if output information of the encoding process must be shown
	 * or not
	 * @throws IOException if the input file can not be closed
	 */
	public void terminate(boolean verbose) throws IOException {
		bis.close();
		if(verbose) {
			System.out.println("read: " + numBitsRead);
		}
	}
}
