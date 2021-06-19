
package GiciEntropyCoder.RiceCoder;


import GiciEntropyCoder.Interface.EntropyDecoder;
import GiciEntropyCoder.UnaryCoder.UnaryDecoder;
import GiciStream.BitInputStream;
import GiciMath.IntegerMath;

import java.io.IOException;


/**
 * This Class implements a Rice decoder.
 *
 * For details on Rice Coding see:
 * http://en.wikipedia.org/wiki/Golomb_Rice_code
 *
 * The methods of this class that perform decoding, will return their outputs as unsigned ints.
 * This means that the most significant bit will indicate magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class RiceDecoder implements EntropyDecoder {

	protected final BitInputStream bis;
	protected final UnaryDecoder unaryDecoder;

	protected final int blockSize;
	protected final int dynamicRange;

	protected final int idBits;
	protected final int numOptions;
	protected final int backupOption;

	protected int codingOption;
	protected int blockCounter;
	protected int[] block;

	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitInputStream</code> from which samples will be decoded.
	 * @param blockSize is used to split data into blocks when the <code>decode</code> function is used.
	 * @param dynamicRange specifies how many bits of each sample are significant (up to 32).
	 * @param restrictIdBits When this flag true, then fewer than 3 id bits can be used.
	 */
	public RiceDecoder(BitInputStream bis, int blockSize, int dynamicRange, boolean restrictIdBits) {

		this.bis = bis;
		this.unaryDecoder = new UnaryDecoder(bis);

		if (blockSize < 1) {
			throw new RuntimeException("Block Size must be positive.");
		}

		this.blockSize = blockSize;

		if (dynamicRange < 1 || dynamicRange > 32) {
			throw new RuntimeException("Dynamic Range must be between 1 and 32.");
		}

		this.dynamicRange = dynamicRange;
		
		int bits = IntegerMath.log2(dynamicRange - 1) + 1;
		this.idBits = restrictIdBits ? bits : Math.max(bits, 3);
		this.numOptions = 1 << idBits;
		this.backupOption = numOptions - 1;
		block = new int[blockSize];
		blockCounter = 0;
	}


	/**
	 * See the general contract for the <code>decodeBlock</code> method of the <code>BlockDecoder</code> interface.
	 *
	 * First an id is read and parsed to determine what coding option was used.
	 * Then the block is decoded using that coding option.
	 *
	 * @param block is the array to which decoded samples will be written.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void decodeBlock(int[] block) throws IOException {

		readId();

		if (codingOption == backupOption) {
			restoreBlock(block);
		} else {
			riceDecodeBlock(block, codingOption);
		}
	}


	/**
	 * Implements the backup coding option.
	 * Reads exactly <code>dynamicRange</code> bits for each sample from the input stream.
	 *
	 * @param block is the array to which decoded samples will be written.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	protected void restoreBlock(int[] block) throws IOException {

		for (int i = 0; i < block.length; i++) {
			block[i] = bis.read(dynamicRange);
		}
	}


	/**
	 * Implements the sample split coding option.
	 * Splits the samples at position <code>k</code>.
	 * Reads the Unary Codewords for the MSBs and appends the LSBs.
	 *
	 * @param block is the array to which decoded samples will be written.
	 * @param k is the position at which the samples are split.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	protected void riceDecodeBlock(int[] block, int k) throws IOException {
		int tmp = 0;
		if (k < 0 || k > 31) {
			throw new RuntimeException("K must be between 0 and 31.");
		}

		for (int i = 0; i < block.length; i++) {
			block[i] = unaryDecoder.decodeSample() << k;
		}
		for (int i = 0; i < block.length; i++) {
			tmp = bis.read(k);
			block[i] |= tmp;
		}
	}


	/**
	 * Determines which coding option is used on the next block, by reading its id.
	 * Assigns the result to codingOption.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void readId() throws IOException {
		codingOption = bis.read(idBits);
	}


	@Override
	public void init(int z) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Decodes a sample of the compressed file
	 * 
	 * @param t the sequence of the sample in the original image
	 * @param z the band in which the sample belongs
	 * @throws IOException if there is a problem reading to the output file
	 */
	@Override
	public int decodeSample(int t, int z) throws IOException {
		if(blockCounter == 0) {
			decodeBlock(block);
			blockCounter = blockSize;
		}
		blockCounter --;
		return block[blockSize - blockCounter - 1];
	}


	@Override
	public void update(int sample, int t, int z) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void terminate() throws IOException {
		bis.close();
		
	}

}
