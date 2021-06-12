
package GiciEntropyCoder.EntropyBlockCoder;

import GiciStream.BitInputStream;

import GiciEntropyCoder.Interface.EntropyDecoder;
import GiciEntropyCoder.BlockAdaptiveCoder.BlockAdaptiveDecoder;

import java.io.IOException;


/**
 * This Class implements a block adaptive decoder.
 *
 * The requirements for this coder are specifies in the CCSDS 123.0-R-1 standard.
 *
 * The methods of this class that perform decoding, will return their outputs as unsigned ints.
 * This means that the most significant bit will indicate magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class EntropyBlockDecoder implements EntropyDecoder {

	private final BlockAdaptiveDecoder decoder;

	/**
	 * Indicates if information about the process should be shown
	 */
	private boolean verbose = false;
	
	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitInputStream</code> from which samples will be decoded.
	 * @param blockSize is used to split data into blocks (8 or 16).
	 * @param dynamicRange specifies how many bits of each sample are significant (up to 16).
	 * @param referenceInterval Reference Sample Interval (up to 4096).
	 * @param bandSequential Sets the sample order to Band Sequential. The alternativew is Band Interleaved.
	 * @param interleavingDepth The interleaving depth for the Band Interleaved order (up to the number of bands).
	 * @param verbose Whether to print progress messages.
	 */
	public EntropyBlockDecoder(
		BitInputStream bis,
		int blockSize,
		int dynamicRange,
		int referenceInterval,
		int restrictIdBits,
		boolean verbose) {
		
		if (blockSize != 8 && blockSize != 16 && blockSize != 32 && blockSize != 64) {
			throw new RuntimeException("Block Size must be 8, 16, 32 or 64");
		}
		if (dynamicRange < 2 || dynamicRange > 16) {
			throw new RuntimeException("Dynamic range must be between 2 and 16");
		}
		this.decoder = new BlockAdaptiveDecoder(bis, blockSize, dynamicRange, false, referenceInterval, 64, restrictIdBits);

		this.verbose = verbose;
	}



	@Override
	public void init(int z) {
		decoder.init(z);
	}


	@Override
	public int decodeSample(int t, int z) throws IOException {
		
		return decoder.decodeSample(t, z);
	}


	@Override
	public void update(int sample, int t, int z) {
		decoder.update(sample, t, z);
	}


	@Override
	public void terminate() throws IOException {
		decoder.terminate();
	}

}
