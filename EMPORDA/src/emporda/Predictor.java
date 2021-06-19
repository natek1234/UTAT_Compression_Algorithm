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

import GiciMath.IntegerMath;
import GiciException.ParameterException;

/**
 * This class is a predictor of the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class Predictor {
	/**
	 * These variables are used as index for the arrays used to store the state of the
	 * predictor in every step of the compression
	 */
	private final int MIN = 0;
	private final int MAX = 1;
	private final int MID = 2;
	private final int N = 0;
	private final int W = 1;
	private final int NW = 2;
	private final int SPS = 0; /* scaled predicted sample */
	private final int PSV = 1; /* predicted sample value */

	/**
	 * The geometry of the image
	 */
	private int geo[];

	/**
	 * These are the arrays that maintain the state of the predictor in every step of the
	 * compression process
	 */
	private final int[] sample;
	private long[][] weightVector;
	private long[][] diffVector;
	private int[][] initializationTable;
	private int[] weightResolution;
	private int[] vectorsSize;
	private long totalTime = 0;
	/**
	 * It stores all the parameters set by the user
	 */
	private Parameters parameters;
	private boolean time;

	/**
	 * Constructor of Predictor. It receives the parameters needed for the 
	 * headers
	 *
	 * @param parameters all the information about the compression process
	 * 
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public Predictor(Parameters parameters, boolean time) throws ParameterException {

		this.parameters = parameters;
		sample = new int[3];
		weightResolution = new int[2];
		geo = parameters.getImageGeometry();

		weightResolution[MIN] = -1 << parameters.weightComponentResolution + 2;
		weightResolution[MAX] = (1 << parameters.weightComponentResolution + 2) - 1;
		
		if (geo[CONS.TYPE] != 3) {
			sample[MIN] = 0;
			sample[MAX] = (1 << parameters.dynamicRange) - 1;
			sample[MID] = 1 << (parameters.dynamicRange - 1);
		
		} else { 
			sample[MIN] = -1 << parameters.dynamicRange - 1;
			sample[MAX] = (1 << parameters.dynamicRange - 1) - 1;			
			sample[MID] = 0;
		}
		weightVector = new long[geo[CONS.BANDS]][];
		diffVector = new long[geo[CONS.BANDS]][];
		vectorsSize = new int[geo[CONS.BANDS]];
		initializationTable = parameters.getWeightInitTable();
		this.time = time;
	}

	/**
	 * Make the initializations needed before compression or decompression.
	 * 
	 * @param z is the number of the band
	 */
	private void init(int z) {
		vectorsSize[z] = parameters.numberPredictionBands < z ? parameters.numberPredictionBands : z;
		if (parameters.predictionMode == CONS.FULL_PRED_MODE) {
			vectorsSize[z] += 3;
		}	
		diffVector[z] = new long[vectorsSize[z]];
		weightVector[z] = new long[vectorsSize[z]];

		//weight initialization
		if (parameters.weightInitMethod == CONS.DEFAULT_WEIGHT_INIT) {
			// default weight initialization
			int i = 0;
			if (parameters.predictionMode == CONS.FULL_PRED_MODE) {
				weightVector[z][N] = 0;
				weightVector[z][W] = 0;
				weightVector[z][NW] = 0;
				i += 3;
			}
			if (i < vectorsSize[z]) {
				weightVector[z][i] = 7*(1 << parameters.weightComponentResolution - 3);
				i++;
			}
			for (; i < vectorsSize[z]; i++) {
				weightVector[z][i] = weightVector[z][i - 1] >> 3;
			}

		} else {
			//custom weight initialization
			int exponent = parameters.weightComponentResolution + 2 - parameters.weightInitResolution;
			int constantSum = exponent < 0 ? 0 : (1 << exponent) - 1;
			int constantProd = 1 << exponent + 1;
			for (int i = 0; i < vectorsSize[z]; i++) {
				weightVector[z][i] = constantProd * initializationTable[z][i] + constantSum;
			}
		}
	}

	/**
	 * Return the neighbor oriented sum for the sample s[y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is a band of the image
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param lineOffset is the position of line y in s
	 * @return the neighbor oriented sum for the sample s[y][x]
	 * of the band s.
	 */
	private int getNeighborOrientedSum(int s[][], int y, int x, int lineOffset) {
		if (y > 0 && x > 0 && x < geo[CONS.WIDTH] - 1) {
			return s[lineOffset][x - 1] + s[lineOffset - 1][x - 1] + s[lineOffset - 1][x] + s[lineOffset - 1][x + 1];
		} else if (y == 0 && x > 0) {
			return s[lineOffset][x - 1] << 2;
		} else if (y > 0 && x == 0) {
			return (s[lineOffset - 1][x] + s[lineOffset - 1][x + 1]) << 1;
		} else {
			return s[lineOffset][x - 1] + s[lineOffset - 1][x - 1] + (s[lineOffset - 1][x] << 1);
		}
	}

	/**
	 * Return the column oriented sum for the sample s[y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is a band of the image
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param lineOffset is the position of line y in s
	 * @return the column oriented sum for the sample s[y][x]
	 * of the band s.
	 */
	private int getColumnOrientedSum(int s[][], int y, int x, int lineOffset) {
		if (y > 0) {
			return s[lineOffset - 1][x] << 2;
		} else {
			return s[lineOffset][x - 1] << 2;
		}
	}

	/**
	 * Return the local sum for the sample s[y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is a band of the image
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param lineOffset is the position of line y in s
	 * @return the local sum for the sample s[y][x]
	 * of the band s.
	 */
	private int getLocalSum(int s[][], int y, int x, int lineOffset) {
		if (parameters.localSumMode == CONS.NEIGHBOR_ORIENTED_SUM) {
			return getNeighborOrientedSum(s, y, x, lineOffset);
		} else {
			return getColumnOrientedSum(s, y, x, lineOffset);
		}
	}

	/**
	 * Calculates the local difference vector under reduced prediction mode 
	 * for the sample s[z][y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is the image
	 * @param z is the band of the sample
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 */
	private void reducedDiffVector(int s[][][], int z, int y, int x, int bandOffset, int lineOffset) {
		for (int i = 0; i < vectorsSize[z]; i++) {
			diffVector[z][i] = (s[bandOffset - i - 1][lineOffset][x] << 2) - getLocalSum(s[bandOffset - i - 1], y, x, lineOffset);
		}
	}

	/**
	 * Calculates the local difference vector under full prediction mode 
	 * for the sample s[z][y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is the image
	 * @param z is the band of the sample
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 */
	private void fullDiffVector(int s[][][], int z, int y, int x, int bandOffset, int lineOffset) {
		if (y == 0) {
			diffVector[z][0] = 0;
			diffVector[z][1] = 0;
			diffVector[z][2] = 0;
		} else {
			int localSum = getLocalSum(s[bandOffset], y, x, lineOffset);
			diffVector[z][0] = (s[bandOffset][lineOffset - 1][x] << 2) - localSum;
			if (x == 0) {
				diffVector[z][1] = diffVector[z][0];
				diffVector[z][2] = diffVector[z][0];
			} else {
				diffVector[z][1] = (s[bandOffset][lineOffset][x - 1] << 2) - localSum;
				diffVector[z][2] = (s[bandOffset][lineOffset - 1][x - 1] << 2) - localSum;
			}
		}
		for (int i = 0; i < vectorsSize[z] - 3; i++) {
			diffVector[z][i + 3] = (s[bandOffset - i - 1][lineOffset][x] << 2) - getLocalSum(s[bandOffset - i - 1], y, x, lineOffset);
		}
	}

	/**
	 * Calculates the local sum for the sample s[z][y][x] of the band s.
	 * x and y cannot both be 0
	 * 
	 * @param s is the image
	 * @param z is the band of the sample
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 */
	private void calculateLocalDifference(int s[][][], int z, int y, int x, int bandOffset, int lineOffset) {
		if (parameters.predictionMode == CONS.FULL_PRED_MODE) {
			fullDiffVector(s, z, y, x, bandOffset, lineOffset);
		} else {
			reducedDiffVector(s, z, y, x, bandOffset, lineOffset);
		}
	}

	/**
	 * Calculates the prediction values.
	 * 
	 * @param s is the image
	 * @param z is the band of the sample
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 * @param returnValues is the param in which this function save
	 * the two values calculated by this function: scaled predicted
	 * sample value and predicted sample value (in this order).
	 */
	private void calculatePrediction(int[][][] s, int z, int y, int x, int bandOffset, int lineOffset, int[] returnValues) {
		int s_scaled, s_aprox;
		int localSum = 0;
		
		if (x + y == 0) {
			
			if(z == 0 || parameters.numberPredictionBands == 0) {
				s_scaled = sample[MID] << 1;
			} else {
				s_scaled = s[bandOffset - 1][lineOffset][x] << 1;
			}
				
		} else {						
			long d_aprox = 0;
			for (int i = 0; i < vectorsSize[z]; i++) {
				d_aprox += weightVector[z][i]*diffVector[z][i];
			}
			localSum = getLocalSum(s[bandOffset], y, x, lineOffset);
			/***************************************************************/
			/* this code is very sensitive to changes, it probably can be optimized, but be careful */
			long tmpValue = (localSum - (sample[MID] << 2));
			boolean sgn = tmpValue < 0;
			tmpValue = Math.abs(tmpValue) << parameters.weightComponentResolution;
			tmpValue = sgn ? -tmpValue : tmpValue;
			long tmp = IntegerMath.mod_R(d_aprox + tmpValue, parameters.registerSize);

			/*****************************************************************/
			
			tmp >>= parameters.weightComponentResolution + 1;
			tmp += (sample[MID] << 1) + 1;
			s_scaled = IntegerMath.clip(tmp, sample[MIN] << 1, (sample[MAX] << 1) + 1);
		}
		s_aprox = s_scaled >> 1;
		returnValues[SPS] = s_scaled;
		returnValues[PSV] = s_aprox;
	}


	/**
	 * Update the weight vector.
	 * x and y cannot both be 0 and in the reduced prediction mode
	 * z cannot be 0.
	 * 
	 * @param s is the image
	 * @param z is the band of the sample
	 * @param y is the row of the sample
	 * @param x is the column of the sample
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 * @param s_scaled is the scaled predicted sample value
	 */
	private void updateWeightVector(int[][][] s, int z, int y, int x, int s_scaled, int bandOffset, int lineOffset) {
		long scaled_error, scaling_exponent;
		
		scaled_error = (s[bandOffset][lineOffset][x] << 1) - s_scaled;
		
		long tmp = ((x + (y - 1) * geo[CONS.WIDTH]) >> parameters.tinc);
		scaling_exponent = IntegerMath.clip(parameters.vmin + tmp, 
				parameters.vmin, 
				parameters.vmax);
		scaling_exponent += parameters.dynamicRange - parameters.weightComponentResolution;
		
		for (int i = 0; i < vectorsSize[z]; i++) {
			tmp = (scaling_exponent < 0)
					? IntegerMath.positive_sign(scaled_error)*diffVector[z][i] << -scaling_exponent
					: IntegerMath.positive_sign(scaled_error)*diffVector[z][i] >> scaling_exponent;
			tmp = tmp + 1 >> 1;
			weightVector[z][i] = IntegerMath.clip(weightVector[z][i] + tmp, 
					weightResolution[MIN], 
					weightResolution[MAX]);
		}
	}

	/**
	 * Return the mapped residual of the sample s[z][y][x].
	 * 
	 * @param s is the sample value
	 * @param s_aprox is the predicted sample value
	 * @param s_scaled is the scaled predicted sample value
	 * @return the mapped residual of the sample s[z][y][x]
	 */
	private int getMappedResidual(int s, int s_aprox, int s_scaled) {
		int residual, minDifference;
		int mappedValue = 0;
		
		residual = s - s_aprox;
		minDifference = Math.min(s_aprox - sample[MIN], sample[MAX] - s_aprox);
		
		if (Math.abs(residual) > minDifference) {
			mappedValue = Math.abs(residual) + minDifference;
		} else if (s_scaled % 2 == 0 && residual >= 0 || s_scaled % 2 != 0 && residual <= 0) {
			mappedValue = Math.abs(residual) << 1;
		} else {
			mappedValue = (Math.abs(residual) << 1) - 1;
		}
		return mappedValue;
	}

	/**
	 * Return the original value of the sample.
	 * 
	 * @param mappedResidual is the mapped residual of a sample
	 * @param s_aprox is the predicted sample value
	 * @param s_scaled is the scaled predicted sample value
	 * @return the original value of the sample
	 */
	private int getSample(int mappedResidual, int s_aprox, int s_scaled) {
		int minDifference, residual;
		
		minDifference = Math.min(s_aprox - sample[MIN], sample[MAX] - s_aprox);
		
		if (mappedResidual > minDifference << 1) {
			if (minDifference == s_aprox - sample[MIN]) {
				residual = mappedResidual - minDifference;
			} else {
				residual = -mappedResidual + minDifference;
			}
		} else if (mappedResidual % 2 == 0) {
			residual = s_scaled % 2 == 0 ? mappedResidual >> 1 : -mappedResidual >> 1;
		} else {
			residual = s_scaled % 2 != 0 ? mappedResidual + 1 >> 1 : -mappedResidual - 1 >> 1;
		}
		return residual + s_aprox;
	}

	/**
	 * Compress the pixel of band z, line y and column x of the image s 
	 * and return the mapped residual.
	 * 
	 * @param s is the image
	 * @param z is the band of the image
	 * @param y is the line of the image
	 * @param x is the column of the image
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 * @return the mapped residual
	 */
	public int compress(int s[][][], int z, int y, int x, int bandOffset, int lineOffset) {
		long initTime = System.nanoTime();
		int s_scaled, s_aprox;
		int[] returnValues = new int[2];
		if(y == 0 && x == 0) {
			init(z);
			calculatePrediction(s, z, 0, 0, bandOffset, lineOffset, returnValues); 
			s_scaled = returnValues[0];
			s_aprox = returnValues[1];
			return getMappedResidual(s[bandOffset][lineOffset][0], s_aprox, s_scaled);
		}
		calculateLocalDifference(s, z, y, x, bandOffset, lineOffset);
		calculatePrediction(s, z, y, x, bandOffset, lineOffset, returnValues);
		s_scaled = returnValues[0];
		s_aprox = returnValues[1];
		updateWeightVector(s, z, y, x, s_scaled, bandOffset, lineOffset);
		totalTime += System.nanoTime() - initTime;
		return getMappedResidual(s[bandOffset][lineOffset][x], s_aprox, s_scaled);
	}

	/**
	 * Decompress the pixel of band z, line y and column x of the image s 
	 * and return it.
	 * 
	 * @param s is the image
	 * @param z is the band of the image
	 * @param y is the line of the image
	 * @param x is the column of the image
	 * @param bandOffset is the position of band z in s
	 * @param lineOffset is the position of line y in s
	 * @return the pixel value
	 */
	public int decompress(int s[][][], int z, int y, int x, int bandOffset, int lineOffset) {
		long initTime = System.nanoTime();
		int s_scaled, s_aprox;
		int[] returnValues = new int[2];
		
		if(y == 0 && x == 0) {
			init(z);
			calculatePrediction(s, z, 0, 0, bandOffset, lineOffset, returnValues);
			s_scaled = returnValues[0];
			s_aprox = returnValues[1];					
			return getSample(s[bandOffset][lineOffset][0], s_aprox, s_scaled); 
		}
		calculateLocalDifference(s, z, y, x, bandOffset, lineOffset);
		calculatePrediction(s, z, y, x, bandOffset, lineOffset, returnValues);
		s_scaled = returnValues[0];
		s_aprox = returnValues[1];
		s[bandOffset][lineOffset][x] = getSample(s[bandOffset][lineOffset][x], s_aprox, s_scaled);
		updateWeightVector(s, z, y, x, s_scaled, bandOffset, lineOffset);
		totalTime += System.nanoTime() - initTime;
		return s[bandOffset][lineOffset][x];
	}	

	/**
	 * Getter for weightVector
	 * @return weightVector
	 */
	public long[][] getWeightVector() {
		return weightVector;
	}
	
	public void end() {
		if(time) {
			System.out.println("time:" + totalTime/(double)1000000);
		}
	}
}
