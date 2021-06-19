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

import GiciEntropyCoder.Interface.EntropyCoder;
import GiciException.ParameterException;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import GiciMath.IntegerMath;
import GiciStream.BitOutputStream;

/**
 * Coder class of the EntropyIntegerCoder. EntropyIntegerCoder is a coder of
 * the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class EntropyIntegerCoder implements EntropyCoder {

	private final int BITCOST_SIZE = 64;
	
	private BitOutputStream bos = null;

	/**
	 * the accumulator array, one element of the array for one band
	 */
	private int[] accumulator;

	/**
	 * the counter array, one element of the array for one band
	 */
	private int[] counter;

	/**
	 * number of bits written to the stream
	 */
	private long numBitsWritten;

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
	 * Bit-cost file
	 */
	private DataOutputStream bitCostDos = null;
	
	/**
	 * Bit cost mode
	 */
	private int bitCost;
	
	private long totalBitsCoded = 0;
	/**
	 * Constructor.
	 *
	 * @param bos the bit output stream
	 * @param initialCountExponent Initial Count Exponent
	 * @param accumulatorInitConstant Accumulator Initialization Constant
	 * @param rescalingCounterSize Rescaling Counter Size
	 * @param dynamicRange DynamicRange
	 * @param unaryLengthLimit Unary Length Limit
	 * @param bandSequential Sets the sample order to Band Sequential. The alternativew is Band Interleaved.
	 * @param interleavingDepth The interleaving depth for the Band Interleaved order (up to the number of bands).
	 * @param accumulatorTable the table with the initial values of the accumulator
	 * @param verbose Whether to print progress messages.
	 * @param bands is the number of bands of the image.
	 * @param numBitsWritten the number of bits written to the file at this moment.
	 * @param bitCost is the bit cost mode
	 * @param outputFile file to save bit cost (it can be null if bitCost is 0)
	 */
	public EntropyIntegerCoder(
		BitOutputStream bos,
		int initialCountExponent,
		int accumulatorInitConstant,
		int rescalingCounterSize,
		int dynamicRange,
		int unaryLengthLimit,
		int[] accumulatorTable,
		int bands, 
		long numBitsWritten, 
		int bitCost,
		String outputFile)
	{
		this.bos = bos;

		this.initialCountExponent = initialCountExponent;
		this.accumulatorInitConstant = accumulatorInitConstant;
		this.rescalingCounterSize = rescalingCounterSize;
		this.dynamicRange = dynamicRange;
		this.unaryLengthLimit = unaryLengthLimit;
	
		this.accumulatorTable = accumulatorTable;
		
		this.numBitsWritten = numBitsWritten;
		
		this.bitCost = bitCost;
		try {
			if(bitCost > 0) {
				File f = new File(outputFile + ".bitCost");
				FileOutputStream fs = new FileOutputStream(f);
				bitCostDos = new DataOutputStream( new BufferedOutputStream(fs));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
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
		accumulator[z] = (3 * (1 << accInit + 6) - 49) * counter[z];
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
	 * Encodes a sample
	 *
	 * @param sample the sample that is going to be encoded
	 * @param t the sample number of the sequence
	 * @param z the band number
	 * @throws IOException if can not write information to the file
	 */
	public void codeSample(int sample, int t, int z) throws IOException {

		int k_z = 0;
		int u_z = 0;
		int bitsCoded = 0;
		if (t == 0) {
			if(sample > 1 << dynamicRange) {
				try {
					throw new ParameterException("PARAMS ERROR: dynamic range too small for this image: " + sample);
				} catch (ParameterException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			bos.write(dynamicRange, sample);
			bitsCoded = dynamicRange;
		} else {

			k_z = IntegerMath.log2((accumulator[z] + (49*counter[z] >> 7))/ counter[z]);
			k_z = (k_z < 0) ? 0 : k_z;
			k_z = (k_z > dynamicRange-2) ? dynamicRange-2 : k_z;
			u_z = sample >> k_z;
			u_z = (u_z < 0) ? 0: u_z;
			if (u_z < unaryLengthLimit) {
				bos.write(u_z, 0);
				bos.write(1, 1);
				bos.write(k_z, sample);
				bitsCoded = k_z + 1 + u_z; 
			} else {
				bos.write(unaryLengthLimit, 0);
				bos.write(dynamicRange, sample);
				bitsCoded = dynamicRange + unaryLengthLimit;
			}
		}
		numBitsWritten += bitsCoded;
		totalBitsCoded += bitsCoded;
		if(bitCostDos != null) {
			switch(bitCost) {
			case 1:
				bitCostDos.writeLong(bitsCoded);
				break;
			case 2:
				if(t != 0) {
					bitCostDos.writeLong(u_z);
					bitCostDos.writeLong(k_z);
				}else {
					bitCostDos.writeLong(-1);
				}
				break;
			}
				
		}
	}

	/**
	 * Ends the encoding process
	 *
	 * @param verbose if output information of the encoding process must be shown
	 * or not
	 * @throws IOException if can not write information to the file
	 */
	public void terminate() throws IOException {
		terminate(false);
	}
	
	/**
	 * Ends the encoding process
	 *
	 * @param verbose if output information of the encoding process must be shown
	 * or not
	 * @throws IOException if can not write information to the file
	 */
	public void terminate(boolean verbose) throws IOException {
		bos.flush();
		if(bitCostDos != null) {
			bitCostDos.flush();
		}
		if(verbose) {
			System.out.println("total bits written: " + numBitsWritten);
		}
	}
	
	/**
	 * Getter for the accumulator
	 * @return accumulator
	 */
	public int[] getAccumulator() {
		return accumulator;
	}
	
	/**
	 * Getter for the counter
	 * @return counter
	 */
	public int[] getCounter() {
		return counter;
	}

}
