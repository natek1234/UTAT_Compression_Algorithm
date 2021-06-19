/*
 * EMPORDA Software - More than an implementation of MHDC Recommendation for  Image Data Compression
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
 * GNU General Public License for  more details.
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
import GiciStream.BitOutputStream;

import GiciEntropyCoder.Interface.EntropyCoder;
import GiciEntropyCoder.EntropyIntegerCoder.EntropyIntegerCoder;
import GiciEntropyCoder.EntropyBlockCoder.EntropyBlockCoder;
import GiciException.LackOfPrecisionError;
import GiciException.ParameterException;
import GiciFile.RawImage.*;

import java.io.*;
import java.util.*;


/**
 * Coder class of EMPORDA application. This is class uses the predictor and the type of coder chosen 
 * in order to encode the image
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class Coder {

	private final File file;
	private final FileOutputStream fileStream;
	private final BitOutputStream bos;
	private EntropyCoder ec;

	private Parameters parameters;
	private int[] geo;
	private Predictor predictor;
	private int[] originalPixelOrder;
	private int[] pixelOrderTransformation;
	private String inputFile;
	private String outputFile;
	private long numBitsWritten = 0;

	/**
	 * This variable indicates if debug information must be shown or not
	 */
	private boolean debugMode = false;
	private boolean deltaMode;
	private RawImageIterator<int[]> deltaIterator;
	private RawImage deltaFile;
	private int[][] deltaFrame;
	private int deltaCounter = 1;
	private boolean saveState = false;
	private int bitCost = 0;
	private boolean time;
	
	/**
	 * Constructor of Coder. It receives the name of the output file and
	 * the parameters needed for the headers.
	 *
	 * @param outputFile the file where the result of the compressing will be saved
	 * @param inputFile the file that contain the image
	 * @param sampleOrder is the sample order of the image in the input file.
	 * @param parameters all the information about the compression process
	 * @param debugMode indicates if debug information must be shown
	 * @throws IOException when something goes wrong and writing must be stopped
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public Coder(String outputFile, String inputFile, int sampleOrder, final Parameters parameters, 
			boolean debugMode, boolean time) throws IOException, ParameterException {
		this.outputFile = outputFile;
		this.time = time;
		file = new File(outputFile);
		fileStream = new FileOutputStream(file);
		bos = new BitOutputStream( new BufferedOutputStream(fileStream));
		this.inputFile = inputFile;
		switch(sampleOrder) {
			case 0: //BSQ
				originalPixelOrder = OrderConverter.DIM_TRANSP_IDENTITY;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_IDENTITY;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BSQ_TO_BIL;
				}
				break;
			case 1: //BIL
				originalPixelOrder = OrderConverter.DIM_TRANSP_BSQ_TO_BIL;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIL_TO_BSQ;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_IDENTITY;
				}
				break;
			case 2: //BIP
				originalPixelOrder = OrderConverter.DIM_TRANSP_BSQ_TO_BIP;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIP_TO_BSQ;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIP_TO_BIL;
				}
				break;
		}
		this.parameters = parameters;
		geo = parameters.getImageGeometry();
		this.predictor = new Predictor(parameters, time);
		this.debugMode = debugMode;
	}

	/**
     * Sets deltaMode, and if it is set to true, then prepares
     * all the necessary things in order to generate a file
     * of the differences.
     * @param deltaMode the boolean indicating if deltaMode is set or not
     */
    public void setDeltaMode(boolean deltaMode) {
    	this.deltaMode = deltaMode;
    	if(this.deltaMode) {
    		int[] deltaImageGeo = new int[6];
    		deltaImageGeo[0] = geo[0];
    		deltaImageGeo[1] = geo[1];
    		deltaImageGeo[2] = geo[2];
    		
    		if (parameters.dynamicRange < 8) {
    			deltaImageGeo[3] = 1;
    		} else if (parameters.dynamicRange < 16) {
    			deltaImageGeo[3] = 3;
    		} else {
    			deltaImageGeo[3] = 4;
    		}
    		 // the differences may be bigger than original samples
    		deltaImageGeo[4] = geo[4];
    		deltaImageGeo[5] = geo[5];
    		
    		deltaFile = new RawImage(outputFile + ".delta", deltaImageGeo, OrderConverter.DIM_TRANSP_IDENTITY, RawImage.WRITE);
            try {
				deltaIterator = (RawImageIterator<int[]>) deltaFile.getIterator(new int[0], OrderConverter.DIM_TRANSP_IDENTITY, RawImage.WRITE, true);
				
            } catch (Exception e) {
				e.printStackTrace();
			}
            deltaCounter = 1;
            deltaFrame = new int[geo[CONS.HEIGHT]][geo[CONS.WIDTH]];
    	}
    }
	/**
	 * Writes the header with all the needed information for the decompression process.
	 *
	 * @param parameters all information about the compression process
	 * @throws IOException when something goes wrong and writing must be stopped
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public void writeHeader(final Parameters parameters) throws IOException, ParameterException {
		
		CoderHeader ch = new CoderHeader(bos, debugMode, parameters);
		ch.imageHeader();
		ch.predictorMetadata();
		ch.entropyCoderMetadata();
		numBitsWritten = ch.getBitsWritten();
		ch = null;
	}

	/**
	 * Compiles all the information needed to create the entropy coder,
	 * and creates it.
	 * @param verbose indicates whether to display information
	 * @param  
	 */
	private void startCoder(boolean verbose) {

		if (parameters.entropyCoderType == CONS.SAMPLE_ADAPTIVE_ENCODER) {
			try {
				ec = new EntropyIntegerCoder(
					bos,
					parameters.initialCountExponent,
					parameters.accInitConstant,
					parameters.rescalingCounterSize,
					parameters.dynamicRange,
					parameters.unaryLengthLimit,
					parameters.getAccInitTable(),
					parameters.getImageGeometry()[CONS.BANDS], 
					numBitsWritten,
					bitCost,
					outputFile
					);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			if(verbose || debugMode) {
				System.out.println("Starting sample adaptive coder");
			}
		} else {
			ec =  new EntropyBlockCoder(
				bos,
				parameters.blockSize,
				parameters.dynamicRange,
				parameters.referenceSampleInterval,
				parameters.restrictIdBits,
				verbose);
			if(verbose || debugMode) {
				System.out.println("Starting block adaptive coder");
			}
		}
	}

	/**
	 * Runs the EMPORDA coder algorithm to compress the image.
	 * 
	 * @param verbose indicates whether to display information
	 *
	 * @throws IOException when something goes wrong and compression must be stopped
	 */
	public void code(boolean verbose) throws IOException {

		startCoder(verbose);
		if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
			codeBSQ(verbose);
		} else {
			codeBI(verbose);
		}

		ec.terminate();

		int oddBytes = (int) (file.length() % parameters.outputWordSize);
		if (oddBytes != 0) {
			byte b[] = new byte[parameters.outputWordSize - oddBytes];
			Arrays.fill(b, (byte)0);
			fileStream.write(b);
		}
		fileStream.close();

		if (verbose) {
			System.out.println("\rWritten " + file.length() + " bytes  ");
		}

	}
	
	/**
	 * Encodes an image in BSQ order
	 *
	 * @param verbose indicates whether to display information
	 * @throws IOException if can not write information to the file
	 */
	private void codeBSQ(boolean verbose) throws IOException {
		int bands[][][] = new int[parameters.numberPredictionBands + 1][][];
		int bandSize = geo[CONS.HEIGHT]*geo[CONS.WIDTH];
		int value;
		if(verbose || debugMode) {
			System.out.println("Coding BSQ");
		}
		try {
			RawImage image = new RawImage(inputFile, geo, originalPixelOrder, RawImage.READ);
			RawImageIterator<int[]> it = (RawImageIterator<int[]>) image.getIterator(new int[0], pixelOrderTransformation, RawImage.READ, true);
	
			if(debugMode) {
				System.err.println("debugInfo: RawImage created");
			}
			for (int z = 0; z < geo[CONS.BANDS]; z ++) {
				if (verbose) {
					System.out.print("\rCoding band: " + z);
				}
				ec.init(z);
				prepareBands(z, bands, it, geo[CONS.HEIGHT]);
				for (int y = 0; y < geo[CONS.HEIGHT]; y ++) {
					for (int x = 0; x < geo[CONS.WIDTH]; x ++) {
						value = predictor.compress(bands, z, y, x, parameters.numberPredictionBands, y);
						ec.codeSample(value, y*geo[CONS.WIDTH] + x, z);
						ec.update(value, y*geo[CONS.WIDTH] + x, z);
						deltaSaveSample(value, y, x, deltaCounter == bandSize,
								z == geo[CONS.BANDS] - 1);
					}
				}
			}
			predictor.end();
			ec.terminate();
			image.close(it);
			if(saveState) {
				saveAppState();
			}
			if (verbose) {
				System.out.print("\rCoding image finished");
			}
		} catch(Exception e) {
			if(debugMode) {
				e.printStackTrace();
			}
			
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}
	}
	
	/**
	 * Stores the state of the weight vector used
	 * by the predictor in a file
	 * @throws IOException
	 */
	private void saveAppState() throws IOException {
		Parameters newState = new Parameters(parameters);
		long[][] weightVector = predictor.getWeightVector();
		int[] accTable = null;
		int[] counter = null;
		try {
			 accTable = ((EntropyIntegerCoder) ec).getAccumulator();
			 counter = ((EntropyIntegerCoder) ec).getCounter();
		} catch(ClassCastException e) {
			// this means that ec is not an instance of EntropyIntegerCoder.
			accTable = null;
		}
		// now newState has been set exactly to values for the parameters
		// used at the beginning of the process. Now we need to store
		// the final state of the application.
		if(weightVector != null) {
			newState.weightInitResolution = (parameters.weightComponentResolution + 3)/2;
			newState.setWeightInitTable(calculateWeightFactors(weightVector));
			newState.weightInitMethod = 1;
			int[] geo = parameters.getImageGeometry();
			newState.vmin = IntegerMath.clip(newState.vmin+(geo[1]*geo[2]-geo[2])/newState.tinc, newState.vmin, newState.vmax);
		}
		/*if(counter != null) {
			int tmp = IntegerMath.log2(counter[0]);
			newState.initialCountExponent = (tmp < newState.rescalingCounterSize) ? tmp : tmp-1;
			parameters.initialCountExponent = newState.initialCountExponent;
		}
		if(accTable != null) {
			newState.setAccInitTable(calculateAccFactors(accTable));
			newState.accInitTableFlag = 1;
			newState.accInitConstant = 15;
		}*/
		// and at the end, we store the values of the parameters
		// to a file
		newState.saveOptions(outputFile + ".state");
	}

	/**
	 * Calculates the factors needed in custom initalization mode
	 * for the accumulator, in order to recover the state of the 
	 * accumulator
	 * @param accTable
	 * @return factors
	 */
	private int[] calculateAccFactors(int[] accTable) {
		int[] factors = new int[accTable.length];
		int initCounter = 1 << parameters.initialCountExponent;
		for(int i = 0; i < accTable.length; i ++) {
			factors[i] = (int) Math.round(Math.log((accTable[i]*Math.pow(2,7)/initCounter + 49)/(double) 3)/Math.log(2) - 6);
			factors[i] = IntegerMath.clip(factors[i], 0, parameters.dynamicRange-2);
			/*System.out.println("ACC: Converting " + accTable[i] + " to " + 
					factors[i] + " and recovered " + 
					(((3*(1 << (factors[i] + 6)) - 49)*initCounter) >> 7));*/
		}
		return factors;
	}
	
	/**
	 * Calculates the factors needed in custom initalization mode
	 * in order to recover weightVector
	 * @param weightVector
	 * @return factors
	 */
	private int[][] calculateWeightFactors(long[][] weightVector) {
		int[][] factors = new int[weightVector.length][];
		/*int exponent = parameters.weightComponentResolution + 2 - parameters.weightInitResolution;
		int constantSum = exponent < 0 ? 0 : (1 << exponent) - 1;
		int constantProd = 1 << (exponent + 1);*/

		for(int i = 0; i < weightVector.length; i ++) {
			factors[i] = new int[weightVector[i].length];
			for(int j = 0; j < weightVector[i].length; j ++) {
				int tmp = ((int) weightVector[i][j]) >> parameters.weightComponentResolution - (parameters.weightComponentResolution +3)/2+3;
				factors[i][j] = IntegerMath.clip(tmp, (-1 << (parameters.weightComponentResolution +3)/2-1), (1 << (parameters.weightComponentResolution +3)/2-1)-1);
				/*System.out.println("WEIGHTS: Converting " + weightVector[i][j] + " to " + 
						factors[i][j] + " and recovered " + 
						((factors[i][j] << parameters.weightComponentResolution + 3 - (parameters.weightComponentResolution +3)/2)
						+ (1 << parameters.weightComponentResolution + 2 - (parameters.weightComponentResolution +3)/2)-1));*/
				//factors[i][j] =  (int)weightVector[i][j];
				/*factors[i][j] = (int) Math.ceil((weightVector[i][j] - constantSum)/(double) constantProd);
				System.out.println("WEIGHTS: Converting " + weightVector[i][j] + " to " + 
						factors[i][j] + " and recovered " + 
						(constantProd*factors[i][j] + constantSum));*/
				/*System.out.println("WEIGHTS: Converting " + weightVector[i][j] + " to " + 
						factors[i][j]);*/
			}
		}
		return factors;
	}
	
	/**
	 * Reads the next band of the image from the input file.
	 * @param it the BSQ iterator over the image
	 * @return the next band of the image
	 */
	private void readBand(RawImageIterator<int[]> it, int[][] band) {
		for(int i=0;i<geo[CONS.HEIGHT];i++) {
			band[i] = it.next();
		}
	}
	
	/**
	 * Tries to read the band z of the image, and reorders all the bands
	 * in memory.
	 * @param z the band to be read
	 * @param bands an array with all the bands needed in the compression process
	 * @param it the BSQ iterator over the image
	 * @param height is the height of image.
	 */
	private void prepareBands(int z, int[][][] bands, RawImageIterator<int[]> it, int height) {
		bands[0] = null;
		for(int i = 0; i < parameters.numberPredictionBands; i ++) {
			bands[i] = bands[i + 1];
		}
		bands[parameters.numberPredictionBands] = new int[height][];
		readBand(it, bands[parameters.numberPredictionBands]);
	}

	/**
	 * Encodes an image in BI order
	 *
	 * @param verbose indicates whether to display information
	 * @throws IOException if can not write information to the file
	 */
	private void codeBI(boolean verbose) throws IOException {

		int M = parameters.subframeInterleavingDepth;
		int auxValue = (geo[CONS.BANDS] % M == 0) ?
				geo[CONS.BANDS] / M :
					geo[CONS.BANDS] / M + 1;

		int value;
		int bands[][][] = new int[geo[CONS.BANDS]][2][];
		int bandSize = geo[CONS.HEIGHT]*geo[CONS.WIDTH];
		
		if(verbose || debugMode) {
			System.out.println("Coding BI");
		}
		try {
			RawImage image = new RawImage(inputFile, geo, originalPixelOrder, RawImage.READ);
			RawImageIterator<int[]> it = (RawImageIterator<int[]>) image.getIterator(new int[0], pixelOrderTransformation, RawImage.READ, true);
			for (int y = 0; y < geo[CONS.HEIGHT]; y++) {
				prepareLines(y, bands, it);
				if (verbose && geo[CONS.HEIGHT] % 10 == 0) {
					System.out.print("\rCoding rows: " + y + " to " + Math.min(y+10, geo[CONS.HEIGHT]));
				}
				for (int i = 0; i < auxValue; i++) {
					for (int x = 0; x < geo[CONS.WIDTH]; x++) {
						for (int z = i * M; z < Math.min((i+1) * M, geo[CONS.BANDS]); z++) {
							if (x == 0 && y == 0) {
								ec.init(z);
							}
							value = predictor.compress(bands, z, y, x, z, 1);
							
							ec.codeSample(value, y*geo[CONS.WIDTH] + x, z);
							ec.update(value, y*geo[CONS.WIDTH] + x, z);
							deltaSaveSample(value, y, x, deltaCounter == bandSize, 
									z == geo[CONS.BANDS] - 1 && y == geo[CONS.HEIGHT] - 1 &&
									x == geo[CONS.WIDTH]- 1 );
							
						}
					}
				}
			}
			predictor.end();
			ec.terminate();
			image.close(it);
			if(saveState) {
				saveAppState();
			}
			if (verbose || debugMode) {
				System.out.print("\rCoding image finished");
			}
		} catch(Exception e) {
			if(debugMode) {
				e.printStackTrace();
			}
			
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}
	}
	
	/**
	 * Tries to read the line y of all bands and reorders the lines for prediction
	 * @param y is the line of the image that will be loaded for all bands
	 * @param bands is the array with the last lines loaded
	 * @param it the BIL iterator over the image
	 */
	private void prepareLines(int y, int[][][] bands, RawImageIterator<int[]> it) {
		if(y != 0) {
			for(int i=0;i<geo[CONS.BANDS];i++) {
				bands[i][0] = bands[i][1];
			}
		}
		for(int i=0;i<geo[CONS.BANDS];i++) {
			bands[i][1] = it.next();
		}
		
	}
	
	/**
	 * Stores a sample to deltaFrame, and if it is filled, then the frame
	 * is written to the file
	 * @param sample value of the difference
	 * @param y line index of the residual
	 * @param x row index of the residual
	 * @param filledBand indicates if this is the last value for the frame, or not
	 * @param last indicates if the current sample is the last sample to be compressed
	 * @throws ClassCastException
	 * @throws IOException
	 */
	private void deltaSaveSample(int sample, int y, int x, boolean filledBand, boolean last) throws ClassCastException, IOException {
		if (deltaMode) {
			deltaFrame[y][x] = sample;
			if (filledBand) {
				saveDeltaBand(deltaFrame, last);
				deltaCounter = 0;
			}
			deltaCounter ++;
		}
	}
    /**
     * Saves a band to the deltaFile
     * @param band the band that is going to be stored to the file
     * @param last if it is the last band or not
     * @throws ClassCastException
     * @throws IOException
     */
    private void saveDeltaBand(int[][] band, boolean last) throws ClassCastException, IOException {
    	int height = geo[CONS.HEIGHT];
    	int width = geo[CONS.WIDTH];
    	if (deltaMode) {
			for(int i = 0; i < height; i ++) {
				// for(int x = 0; x < width;  x ++) {
				// 	System.out.println(band[i][x]);
				// }
				deltaIterator.next();
				deltaIterator.set(band[i]);
			}
			if (last) {
	    		deltaFile.close(deltaIterator);
	    	}
    	}
    }

    /**
     * Sets if a weights file must be created or not
     * @param weights 
     */
	public void setSaveState(boolean saveState) {
		this.saveState  = saveState;
	}
	
	/**
	 * Setter for bitCost
	 * @param bitCost
	 */
	public void setBitCost(int bitCost) {
		this.bitCost  = bitCost;
	}
	
}
