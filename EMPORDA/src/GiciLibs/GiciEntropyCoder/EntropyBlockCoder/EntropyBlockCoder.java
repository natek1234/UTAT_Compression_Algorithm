
package GiciEntropyCoder.EntropyBlockCoder;

import GiciStream.BitOutputStream;

import GiciEntropyCoder.Interface.EntropyCoder;
import GiciEntropyCoder.BlockAdaptiveCoder.BlockAdaptiveCoder;

import java.io.IOException;


/**
 * This Class implements a block adaptive encoder.
 *
 * The requirements for this coder are specifies in the CCSDS 123.0-R-1 standard.
 *
 * The methods of this class that perform coding, take their inputs as unsigned ints.
 * This means that the most significant bit will be treated as indicating magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class EntropyBlockCoder implements EntropyCoder {

	private final BlockAdaptiveCoder coder;


	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitInputStream</code> from which samples will be decoded.
	 * @param blockSize is used to split data into blocks (8, 16, 32 or 64).
	 * @param dynamicRange specifies how many bits of each sample are significant (from 2 to 16).
	 * @param referenceInterval Reference Sample Interval (up to 4096).
	 * @param bandSequential Sets the sample order to Band Sequential. The alternative is Band Interleaved.
	 * @param interleavingDepth The interleaving depth for the Band Interleaved order (up to the number of bands).
	 * @param verbose Whether to print progress messages.
	 */
	public EntropyBlockCoder(
		BitOutputStream bos,
		int blockSize,
		int dynamicRange,
		int referenceInterval,
		int restrictIdBits,
		boolean verbose)
	{
		if (blockSize != 8 && blockSize != 16 && blockSize != 32 && blockSize != 64) {
			throw new RuntimeException("Block Size must be 8, 16, 32 or 64");
		}
		if (dynamicRange < 2 || dynamicRange > 16) {
			throw new RuntimeException("Dynamic range must be between 2 and 16");
		}
		this.coder = new BlockAdaptiveCoder(bos, blockSize, dynamicRange, false, referenceInterval, 64, restrictIdBits);

	}

	@Override
	public void init(int z) {
		coder.init(z);
	}



	@Override
	public void update(int sample, int t, int z) {
		coder.update(sample, t, z);
	}



	@Override
	public void codeSample(int sample, int t, int z) throws IOException {
		coder.codeSample(sample, t, z);
	}



	@Override
	public void terminate() throws IOException {
		coder.terminate();
	}

}
