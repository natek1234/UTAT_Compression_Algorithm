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

import GiciStream.BitInputStream;
import GiciEntropyCoder.Interface.EntropyDecoder;
import GiciEntropyCoder.EntropyIntegerCoder.EntropyIntegerDecoder;
import GiciEntropyCoder.EntropyBlockCoder.EntropyBlockDecoder;
import GiciException.ParameterException;
import GiciFile.RawImage.OrderConverter;
import GiciFile.RawImage.RawImage;
import GiciFile.RawImage.RawImageIterator;

import java.io.*;


/**
 * Decoder class of EMPORDA application. Decoder is a decoder of the Recommended Standard MHDC-123 White Book.
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class Decoder {

	private final File file;
	private final FileInputStream fileStream;
	private final BitInputStream bis;
	private EntropyDecoder ed;
	private Parameters parameters = null;
	private Predictor predictor = null;
	private boolean debugMode = false;
	private int[] savedPixelOrder = null;
	private String outputFile = null;
	private int[] pixelOrderTransformation = null;
	private int sampleOrder;
	private int pixelFormat;
	private boolean time;
	/**
	 * Constructor of Decoder. It receives the name of the input file.
	 *
	 * @param inputFile the file where is saved the image that is going
	 * to be decompressed
	 * @throws FileNotFoundException when something goes wrong and writing must be stopped
	 */
	public Decoder (String inputFile, String outputFile, boolean debugMode, int sampleOrder, int pixelFormat, 
			boolean time) throws FileNotFoundException {
		
		file = new File(inputFile);
		fileStream = new FileInputStream(file);
		bis = new BitInputStream( new BufferedInputStream( fileStream ) );
		this.sampleOrder = sampleOrder;
		this.debugMode = debugMode;
		this.outputFile = outputFile;
		this.pixelFormat = pixelFormat;
		this.time = time;
	}

	/**
	 * Reads the header with all the needed information for  the decompression process.
	 *
	 * @return object with all the information about the compression process
	 * @throws IOException when something goes wrong and writing must be stopped
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public Parameters readHeader(String optionString) 
			throws IOException, ParameterException {

		parameters = new Parameters(optionString, null, false, debugMode, false);
		return readHeader();
	}

	/**
	 * Reads the header with all the needed information for  the decompression process.
	 *
	 * @return object with all the information about the compression process
	 * @throws IOException when something goes wrong and writing must be stopped
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public Parameters readHeader(FileInputStream optionFile) 
			throws IOException, ParameterException {

		parameters = new Parameters(optionFile, null, false, debugMode, false);
		return readHeader();
	}

	/**
	 * Reads the header with all the needed information for  the decompression process.
	 *
	 * @return object with all the information about the compression process
	 * @throws IOException when something goes wrong and writing must be stopped
	 * @throws ParameterException when an invalid parameter is detected
	 */
	public Parameters readHeader() 
			throws IOException, ParameterException {

		
		DecoderHeader dh = new DecoderHeader(bis, parameters, debugMode);
		
		dh.readImageHeader();
		dh.finish();
		
		switch(sampleOrder) {
			case 0: //BSQ
				savedPixelOrder = OrderConverter.DIM_TRANSP_IDENTITY;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_IDENTITY;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BSQ_TO_BIL;
				}
				break;
			case 1: //BIL
				savedPixelOrder = OrderConverter.DIM_TRANSP_BSQ_TO_BIL;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIL_TO_BSQ;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_IDENTITY;
				}
				break;
			case 2: //BIP
				savedPixelOrder = OrderConverter.DIM_TRANSP_BSQ_TO_BIP;
				if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIP_TO_BSQ;
				}else {
					pixelOrderTransformation = OrderConverter.DIM_TRANSP_BIP_TO_BIL;
				}
				break;
		}
		
		return parameters;
	}

	/**
	 * Compiles all the information needed to create the entropy decoder,
	 * and creates it.
	 * @param verbose indicates whether to display information
	 * @throws ParameterException 
	 */
	private void startDecoder(boolean verbose) throws ParameterException {
	
		if(verbose) {
                	System.out.println("Z: " + parameters.getImageGeometry()[CONS.BANDS]
				+ ", Y: " + parameters.getImageGeometry()[CONS.HEIGHT]
				+ ", X: " + parameters.getImageGeometry()[CONS.WIDTH]);
                }
	
		if (parameters.entropyCoderType == CONS.SAMPLE_ADAPTIVE_ENCODER) {
			try {
				ed = new EntropyIntegerDecoder(
				bis,
				parameters.initialCountExponent,  // initial count exponent
				parameters.accInitConstant,  // accumulator init constant
				parameters.rescalingCounterSize,  // rescaling counter size
				parameters.dynamicRange,     // dynamic range
				parameters.unaryLengthLimit, // unaly length limit
				parameters.getAccInitTable(),
				parameters.getImageGeometry()[CONS.BANDS]);
			} catch (ParameterException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			if(verbose) {
				System.out.println("Starting sample adaptive decoder");
			}
		} else {
			
			ed = new EntropyBlockDecoder(
				bis,
				parameters.blockSize,  // block size
				parameters.dynamicRange,    // dynamic range
				parameters.referenceSampleInterval, // reference sample interval
				parameters.restrictIdBits,
				verbose);
			if(verbose) {
				System.out.println("Starting block adaptive decoder");
			}
		}
		predictor = new Predictor(parameters, time);
	}

	
	/**
	 * Runs the EMPORDA decoder algorithm to decompress the image.
	 *
	 * @return the whole image decoded
	 * @throws IOException when something goes wrong and compression must be stopped
	 */
	public void decode(boolean verbose) throws IOException {
		int readBytes = bis.available();
		
		try {
			startDecoder(verbose);
		} catch (ParameterException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (parameters.sampleEncodingOrder == CONS.BAND_SEQUENTIAL) {
			decodeBSQ(verbose);
		} else {
			decodeBI(verbose);
		}
		ed.terminate();

		if (verbose) {
			System.out.println("\rRead " + readBytes + " bytes            ");
		}
	}
	
	/**
	 * Decodes an image in BSQ order
	 *
	 * @params img the whole image
	 * @params bands the number of bands of the image
	 * @params height the number of lines of one band of the image
	 * @params width the number of samples of one line of the image
	 * @throws IOException if can not read file information
	 */
	private void decodeBSQ(boolean verbose) throws IOException {

		int[] imageGeometry = parameters.getImageGeometry();
		int bands = imageGeometry[CONS.BANDS];
		int height = imageGeometry[CONS.HEIGHT];
		int width = imageGeometry[CONS.WIDTH];
		int imageBands[][][] = new int[parameters.numberPredictionBands + 1][height][width];
		int[] geometry = parameters.getImageGeometry();
		if(pixelFormat > 0) {
			geometry[CONS.TYPE] = pixelFormat; 
		}
		try {
			RawImage image = new RawImage(outputFile, geometry, savedPixelOrder, RawImage.WRITE);
			RawImageIterator<int[]> it = (RawImageIterator<int[]>) image.getIterator(new int[0], pixelOrderTransformation, RawImage.WRITE, true);
			for (int z = 0; z < bands; z ++) {
				if (verbose) {
					System.out.print("\rDecoding band: " + z);
				}
				ed.init(z);
				for (int y = 0; y < height; y ++) {
					for (int x = 0; x < width; x ++) {
						imageBands[parameters.numberPredictionBands][y][x] = ed.decodeSample(y*width + x, z);
						ed.update(imageBands[parameters.numberPredictionBands][y][x], y*width + x, z);
						imageBands[parameters.numberPredictionBands][y][x] = predictor.decompress(imageBands, z, y, x, parameters.numberPredictionBands, y);
					}
				}
				prepareBands(imageBands, it);
			}
			predictor.end();
			image.close(it);
			if (verbose) {
				System.out.print("\rDecoding image finished");
			}
		}catch(UnsupportedOperationException e) {
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}catch(IndexOutOfBoundsException e) {
			e.printStackTrace();
		}catch(ClassCastException e) {
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}
	}
	
	/**
	 * Write the next band of the image.
	 * @param band is the band to write in the output file.
	 * @param it the BSQ iterator over the image
	 */
	private void writeBand(int[][] band, RawImageIterator<int[]> it) {
		int height =  parameters.getImageGeometry()[CONS.HEIGHT];

		for(int i = 0; i < height; i ++) {
			it.next();
			it.set(band[i]);
		}
	}
	
	/**
	 * Save the first band and reordered bands for prediction
	 * @param bands an array with the prediction bands
	 * @param it the BSQ iterator over the image
	 */
	private void prepareBands(int[][][] bands, RawImageIterator<int[]> it) {
		int[][] tmpBand = bands[0];
		writeBand(bands[parameters.numberPredictionBands], it);		
		for(int i = 0; i < parameters.numberPredictionBands; i ++) {
			bands[i] = bands[i + 1];
		}
		bands[parameters.numberPredictionBands] = tmpBand;
	}
	
	/**
	 * Decodes an image in BSQ order
	 *
	 * @params img the whole image
	 * @params bands the number of bands of the image
	 * @params height the number of lines of one band of the image
	 * @params width the number of samples of one line of the image
	 * @params M the encoding interleaving value
	 * @throws IOException if can not read file informationn
	 */
	private void decodeBI(boolean verbose) throws IOException {

		int[] imageGeometry = parameters.getImageGeometry();
		int bands = imageGeometry[CONS.BANDS];
		int height = imageGeometry[CONS.HEIGHT];
		int width = imageGeometry[CONS.WIDTH];
		int M = parameters.subframeInterleavingDepth;
		int imageBands[][][] = new int[bands][2][width];
		int[] geometry = parameters.getImageGeometry();
		if(pixelFormat > 0) {
			geometry[CONS.TYPE] = pixelFormat; 
		}
		try {
			RawImage image = new RawImage(outputFile, geometry, savedPixelOrder, RawImage.WRITE);
			RawImageIterator<int[]> it = (RawImageIterator<int[]>) image.getIterator(new int[0], pixelOrderTransformation, RawImage.WRITE, true);
			int auxValue = (bands % M == 0) ?
					bands / M :
					bands / M + 1;
			
			for (int y = 0; y < height; y++) {
				if (verbose && height % 10 == 0) {
					System.out.print("\rDecoding rows: " + y + " to " + Math.min(y+10, height));
				}
				for (int i = 0; i < auxValue; i++) {
					for (int x = 0; x < width; x++) {
						for (int z = i * M; z < Math.min((i+1) * M, bands); z++) {
							if (x == 0 && y == 0) {
								ed.init(z);
							}
							imageBands[z][1][x] = ed.decodeSample(y*width + x, z);
							ed.update(imageBands[z][1][x], y*width + x, z);
							imageBands[z][1][x] = predictor.decompress(imageBands, z, y, x, z, 1);
						}
					}
				}
				prepareLines(imageBands, it);
			}
			predictor.end();
			if (verbose) {
				System.out.print("\rDecoding image finished");
			}
			image.close(it);
		}catch(UnsupportedOperationException e) {
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}catch(IndexOutOfBoundsException e) {
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}catch(ClassCastException e) {
			throw new Error("Unexpected exception ocurred "+e.getMessage());
		}
	}
	
	/**
	 * Try to write a line of all bands and reordered lines for prediction
	 * @param bands is the array with the last lines loaded
	 * @param it the BIL iterator over the image
	 */
	private void prepareLines(int[][][] bands, RawImageIterator<int[]> it) {
		int numBands = parameters.getImageGeometry()[CONS.BANDS];
		int width = bands[0][0].length;
		for(int i = 0; i < numBands; i ++) {
			for(int x = 0; x < width; x ++) {
				bands[i][0][x] = bands[i][1][x];
			}
		}
		for(int i = 0; i < numBands; i ++) {
			it.next();
			it.set(bands[i][1]);
		}
		
	}
}
