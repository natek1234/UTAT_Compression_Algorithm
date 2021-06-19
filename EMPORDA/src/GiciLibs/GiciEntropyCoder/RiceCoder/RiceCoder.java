
package GiciEntropyCoder.RiceCoder;


import GiciEntropyCoder.Interface.EntropyCoder;
import GiciEntropyCoder.UnaryCoder.UnaryCoder;
import GiciStream.BitOutputStream;
import GiciMath.IntegerMath;

import java.io.IOException;


/**
 * This Class implements a Rice encoder.
 *
 * For details on Rice Coding see:
 * http://en.wikipedia.org/wiki/Golomb_Rice_code
 *
 * The methods of this class that perform coding, take their inputs as unsigned ints.
 * This means that the most significant bit will be treated as indicating magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class RiceCoder implements EntropyCoder {

	protected final BitOutputStream bos;
	protected final UnaryCoder unaryCoder;

	protected final int blockSize;

	protected final int dynamicRange;
	protected final int bitMask;

	protected final int idBits;
	protected final int numOptions;
	protected final int backupOption;

	protected int codingOption;
	protected int[] block = null;
	protected int blockCounter = 0;
	
	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitOutputStream</code> to which samples will be encoded.
	 * @param blockSize is used to split data into blocks when the <code>code</code> function is used.
	 * @param dynamicRange specifies how many bits of each sample are significant (up to 32).
	 * @param restrictIdBits When this flag true, then fewer than 3 id bits can be used.
	 */
	public RiceCoder(BitOutputStream bos, int blockSize, int dynamicRange, boolean restrictIdBits) {

		this.bos = bos;
		this.unaryCoder = new UnaryCoder(bos);

		if (blockSize < 1) {
			throw new RuntimeException("Block Size must be positive.");
		}

		this.blockSize = blockSize;

		if (dynamicRange < 1 || dynamicRange > 32) {
			throw new RuntimeException("Dynamic Range must be between 1 and 32.");
		}

		this.dynamicRange = dynamicRange;
		this.bitMask = (int) (1L << dynamicRange) - 1;

		int bits = IntegerMath.log2(dynamicRange - 1) + 1;
		this.idBits = restrictIdBits ? bits : Math.max(bits, 3);
		this.numOptions = 1 << idBits;
		this.backupOption = numOptions - 1;
		block = new int[blockSize];
		for(int i = 0; i < blockSize; i ++) {
			block[i] = 0;
		}
		
	}

	/**
	 * Encodes a sample, saving the given sample in a block, until the block is filled, 
	 * when the block is really coded
	 * 
	 * @param sample the sample to be coded
	 * @param t the sequence of the sample in the image
	 * @param z the band in which the sample belongs
	 * @throws IOException if there is a problem writing to the output file
	 */
	public void codeSample(int sample, int t, int z) throws IOException {
		block[blockCounter] = sample;
		blockCounter ++;
		if(blockCounter == blockSize) {
			blockCounter = 0;
			codeBlock(block);
			for(int i = 0; i < blockSize; i ++) {
				block[i] = 0;
			}
		}
	}

	/**
	 * See the general contract for the <code>codeBlock</code> method of the <code>BlockCoder</code> interface.
	 *
	 * First a copy of the input block is created, ignoring any bits outside of the <code>dynamicRange</code>.
	 * Then the optimal coding option for the block is determined and its id is written to the output stream.
	 * Finaly the block itself is coded.
	 *
	 * @param inBlock is the block to be coded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void codeBlock(int[] inBlock) throws IOException {

		int[] block = maskBlockBits(inBlock);

		findBestCodingOption(block);

		bos.write(idBits, codingOption);

		if (codingOption == backupOption) {
			backupBlock(block);
		} else {
			riceCodeBlock(block, codingOption);
		}
	}

	public void terminate() throws IOException {
		if (blockCounter > 0) {
			codeBlock(block);
			blockCounter = 0;
		}

		finish();
	}
	/**
	 * See the general contract for the <code>finish</code> method of the <code>BlockCoder</code> interface.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void finish() throws IOException {
		bos.flush();
	}


	/**
	 * All bits outside of the <code>dynamicRange</code> are set to zero.
	 *
	 * @param inBlock the block to be masked (remains unmodified).
	 *
	 * @returns the resulting block.
	 */
	protected int[] maskBlockBits(int[] inBlock) {

		int[] outBlock = new int[inBlock.length];

		for (int i = 0; i < inBlock.length; i++) {
			outBlock[i] = inBlock[i] & bitMask;
		}

		return outBlock;
	}


	/**
	 * Decides which coding option produces the smallest output for a given block.
	 * The result is assigned to <code>codingOption</code>.
	 *
	 * @param block contains the data on which the decision is based.
	 */
	protected void findBestCodingOption(int[] block) {

		// default to backup option
		codingOption = backupOption;
		long bestSize = block.length * dynamicRange;

		// examine sample split options
		for (int i = 0; i < numOptions - 1; i++) {
			long sum = 0 ;
			for( int sample : block) {
				sum += (sample & 0xffffffffL) >>> i ;
			}

			long size = sum + (i + 1) * block.length ;
			if (size < bestSize) {
				codingOption = i;
				bestSize = size;
			}
		}
	}


	/**
	 * Helper function.
	 * Sums a block of data, treating the values as unsigned ints.
	 *
	 * @param block is the block to be summed.
	 *
	 * @return the sum.
	 */
	protected long sumBlock(int[] block) {

		long sum = 0;
		for (int sample : block) {
			long value = sample & 0xffffffffL;
			sum += value;
		}
		return sum;
	}


	/**
	 * Implements the backup coding option.
	 * Writes exactly <code>dynamicRange</code> bits for each sample to the output stream.
	 *
	 * @param block is the block to be coded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	protected void backupBlock(int[] block) throws IOException {

		for (int value : block) {
			bos.write(dynamicRange, value);
		}
	}


	/**
	 * Implements the sample split coding option.
	 * Splits the samples at position <code>k</code>.
	 * Writes the Unary Codewords for the MSBs and appends the LSBs.
	 *
	 * @param block is the block to be coded.
	 * @param k is the position at which the samples are split.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	protected void riceCodeBlock(int[] block, int k) throws IOException {

		if (k < 0 || k > 31) {
			throw new RuntimeException("K must be between 0 and 31.");
		}
		for(int i = 0; i < blockSize; i ++) {
			unaryCoder.codeSample(block[i] >>> k);
		}
		for(int i = 0; i < blockSize; i ++) {
			bos.write(k, block[i]);
		}
	}

	@Override
	public void init(int z) {
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(int sample, int t, int z) {
		// TODO Auto-generated method stub
		
	}

}
