
package GiciEntropyCoder.BlockAdaptiveCoder;

import GiciEntropyCoder.Interface.BlockDecoder;
import GiciEntropyCoder.RiceCoder.RiceDecoder;

import GiciEntropyCoder.BlockAdaptiveCoder.CodingType;
import static GiciEntropyCoder.BlockAdaptiveCoder.CodingType.*;

import GiciStream.BitInputStream;

import java.io.IOException;


/**
 * This Class implements a block adaptive decoder.
 *
 * The requirements for this coder are specifies in the CCSDS 121.0-B-2 standard.
 * Missing features: 1) the preprocessor 2) reference samples
 *
 * The methods of this class that perform decoding, will return their outputs as unsigned ints.
 * This means that the most significant bit will indicate magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class BlockAdaptiveDecoder extends RiceDecoder implements BlockDecoder {

	private CodingType codingType;

	/**
	 * This class is employed, to determine how many blocks away the End-ofSegment and the next Reference sample are.
	 */
	private class BlockCounter {
		int r, rOffset = 0;
		int s, sOffset = 0;

		/**
		 * Constructor.
		 *
		 * @param r is the reference sample interval.
		 * @param s is the segment size.
		 */
		public BlockCounter(int r, int s) {
			this.r = r;
			this.s = s;
		}

		/**
		 * Must be called whenever a block is read.
		 */
		public void increment() {
			rOffset = (rOffset + 1) % r;
			sOffset = (sOffset + 1) % s;
		}

		/**
		 * Returns the distance in blocks to the next reference sample.
		 */
		public int rDistance() { return r - rOffset; }

		/**
		 * Returns the distance in blocks to the end of the segment.
		 */
		public int sDistance() { return s - sOffset; }
	}

	private final BlockCounter blockCounter;


	/**
	 * This class is employed to read zero blocks.
	 */
	private class ZeroBlock {
		int numZeroBlocks = 0;
		int rosCode = 4;

		/**
		 * Atempts to return a zero block.
		 *
		 * @params block if zero blocks are available this block will be filled with zeros.
		 *
		 * @return true if a block was available, false if not.
		 */
		boolean getBlock(int[] block) {
			if (numZeroBlocks == 0) {
				return false;
			}

			for (int i = 0; i < block.length; i++) {
				block[i] = 0;
			}

			numZeroBlocks--;
			return true;
		}

		/**
		 * Reads a zero block from input.
		 * Sets the value of numZeroBlocks.
		 *
		 * @throws IOException if an IO error prevents the process from completing.
		 */
		void readBlocks() throws IOException {
			numZeroBlocks = unaryDecoder.decodeSample();

			if (numZeroBlocks == rosCode) {
				numZeroBlocks = Math.min(
					blockCounter.rDistance(),
					blockCounter.sDistance());
			}
			else if (numZeroBlocks < rosCode) {
				numZeroBlocks++;
			}
		}
	}

	private final ZeroBlock zeroBlock;


	/**
	 * This class implements the second extension decoding logic.
	 */
	private class SecondExtension {
		long[] secExtBlock;
		int b, ms;

		/**
		 * Reads a second extension block and inverts the conversion.
		 *
		 * @param block where the decoded block will be stored.
		 *
		 * @throws IOException if an IO error prevents the process from completing.
		 */
		public void decodeBlock(int[] block) throws IOException {
			int len = (block.length + 1) / 2;
			secExtBlock = new long[len];

			for (int i = 0; i < len; i++) {
				secExtBlock[i] = unaryDecoder.decodeSample();
			}

			convertFromSecExt(block);
		}

		/**
		 * Inverts the second extension conversion.
		 * The second extension block is originally stored in the field secExtBlock.
		 *
		 * @param block the array where the decoded block will be saved.
		 */
		void convertFromSecExt(int[] block) {
			boolean odd = (block.length % 2) == 1;

			int i = 0;

			if (odd) {
				transform((int) secExtBlock[0]);
				block[0] = (int) (secExtBlock[0] - ms);
				i++;
			}

			for (; i < block.length; i += 2) {
				int m = (int) secExtBlock[(i+1)/2];
				transform(m);
				block[i+1] = m - ms;
				block[i] = b - block[i+1];
			}
		}

		/**
		 * Takes a second extension cadeword and sets the values of b and ms apropriately.
		 * The value of the two samples that were used to produce the codeword can be inferred from these two values.
		 *
		 * @param sample the codeword
		 */
		void transform(int sample) {
			b = 0;
			ms = 0;
			long value = sample & 0xffffffffL;
			while (value > b + ms) {
				b++;
				ms += b;
			}
		}
	}

	private final SecondExtension secExt;


	private final boolean referenceSamples;


	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitInputStream</code> from which samples will be decoded.
	 * @param blockSize is used to split data into blocks when the <code>decode</code> function is used.
	 * @param dynamicRange specifies how many bits of each sample are significant (up to 32).
	 * @param referenceSamples Whether reference samples are inserted. (currently not supported)
	 * @param r Reference Sample Interval (up to 4096).
	 * @param s Segment Size.
	 */
	public BlockAdaptiveDecoder(BitInputStream bis, int blockSize, int dynamicRange,
			boolean referenceSamples, int r, int s, int restrictIdBits) {

		super(bis, blockSize, dynamicRange, restrictIdBits == 1 && dynamicRange <= 4);

		if (referenceSamples) {
			throw new RuntimeException("Inserting Reference Samples is not yet supported.");
		}

		this.referenceSamples = referenceSamples;

		if (r < 1 || r > 4096) {
			throw new RuntimeException("Reference Sample Interval should be between 1 and 4096.");
		}

		if (s < 1) {
			throw new RuntimeException("Segment Size should be positive. Recommended value is 64.");
		}

		this.blockCounter = new BlockCounter(r, s);

		this.zeroBlock = new ZeroBlock();
		this.secExt = new SecondExtension();
	}


	/**
	 * See the general contract for the <code>decodeBlock</code> method of the <code>BlockDecoder</code> interface.
	 *
	 * First an id is read and parsed to determine what coding option was used.
	 * Then the block is decoded using that coding option.
	 * Since zero blocks get accumulated, blocks might not be read from the input stream.
	 *
	 * @param block is the array to which decoded samples will be written.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void decodeBlock(int[] block) throws IOException {

		// Return a zero block if available
		if (zeroBlock.getBlock(block)) {
			blockCounter.increment();
			return;
		}

		// Read block
		readId();
		
		switch(codingType) {

			case ZERO_BLOCK :
				zeroBlock.readBlocks();
				zeroBlock.getBlock(block);
			break;

			case SECOND_EXT :
				secExt.decodeBlock(block);
			break;

			case SAMPLE_SPLIT :
				riceDecodeBlock(block, codingOption);
			break;

			case BACKUP :
				restoreBlock(block);
			break;

		}

		blockCounter.increment();
	}


	/**
	 * Determines which coding option is used on the next block, by reading its id.
	 * Assigns the result to codingOption.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void readId() throws IOException {

		int id = bis.read(idBits);
		if (id == 0) {
			id = bis.read(1);
			if (id == 0) {
				codingType = ZERO_BLOCK;
			}
			else {
				codingType = SECOND_EXT;
			}
		}
		else {
			if (id == backupOption) {
				codingType = BACKUP;
			}
			else {
				codingType = SAMPLE_SPLIT;
				codingOption = id - 1;
			}
		}
	}

}
