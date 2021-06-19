
package GiciEntropyCoder.BlockAdaptiveCoder;

import GiciEntropyCoder.Interface.BlockCoder;
import GiciEntropyCoder.RiceCoder.RiceCoder;

import GiciEntropyCoder.BlockAdaptiveCoder.CodingType;
import static GiciEntropyCoder.BlockAdaptiveCoder.CodingType.*;

import GiciStream.BitOutputStream;

import java.io.IOException;


/**
 * This Class implements the block adaptive encoder.
 *
 * The requirements for this coder are specifies in the CCSDS 121.0-B-2 standard.
 * Missing features: 1) the preprocessor 2) reference samples
 *
 * The methods of this class that perform coding, take their inputs as unsigned ints.
 * This means that the most significant bit will be treated as indicating magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class BlockAdaptiveCoder extends RiceCoder implements BlockCoder {

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
		 * Must be called whenever a block is written.
		 */
		public void increment() {
			rOffset = (rOffset + 1) % r;
			sOffset = (sOffset + 1) % s;
		}

		/**
		 * Returns the offset in blocks from the last reference sample.
		 */
		public int rOffset() { return rOffset; }

		/**
		 * Returns the offset in blocks from the the begining of the segment.
		 */
		public int sOffset() { return sOffset; }
	}

	private final BlockCounter blockCounter;


	/**
	 * This class is employed to write zero blocks.
	 */
	private class ZeroBlock {
		int numZeroBlocks = 0;
		final int rosCode = 4;

		/**
		 * Increments the number of zero blocks that have accumulated.
		 */
		public void addBlock() {
			numZeroBlocks++;
		}

		/**
		 * Writes a single zero block with a code corresponding to the number of accumulated blocks.
		 *
		 * @throws IOException if an IO error prevents the process from completing.
		 */
		public void flushBlocks() throws IOException {
			if (numZeroBlocks == 0) {
				return;
			}

			if (numZeroBlocks <= rosCode) {
				numZeroBlocks--;
			}

			writeZeroBlock();
		}

		/**
		 * Writes a zero block with a Remainder-of-Segment code.
		 *
		 * @throws IOException if an IO error prevents the process from completing.
		 */
		public void flushRosBlock() throws IOException {
			if(numZeroBlocks <= rosCode) {
				flushBlocks();
				return;
			}

			numZeroBlocks = rosCode;

			writeZeroBlock();
		}

		void writeZeroBlock() throws IOException {
			writeId(ZERO_BLOCK, 0);
			unaryCoder.codeSample(numZeroBlocks);
			numZeroBlocks = 0;
		}
	}

	private final ZeroBlock zeroBlock;


	/**
	 * This class implements the second extension coding logic.
	 */
	private class SecondExtension {
		long[] secExtBlock;

		/**
		 * Uses the unaryCoder to write the converted block to output.
		 *
		 * @throws IOException if an IO error prevents the process from completing.
		 */
		public void codeBlock() throws IOException {
			for (long value : secExtBlock)
			{
				unaryCoder.codeSample((int) value);
			}
		}

		/**
		 * Converts a passed block into second extension representation and stores it in the field secExtBlock.
		 *
		 * @param block the block to be converted.
		 */
		public void convertToSecExt(int[] block) {
			int len = (block.length + 1) / 2;
			secExtBlock = new long[len];

			boolean odd = (block.length % 2) == 1;

			int i = 0;

			if (odd) {
				secExtBlock[0] = transform(0, block[0]);
				i++;
			}

			for (; i < block.length; i += 2) {
				secExtBlock[(i+1)/2] = transform(block[i], block[i+1]);
			}
		}

		/**
		 * Returns the converted block.
		 */
		public long[] getBlock() {
			return secExtBlock;
		}

		/**
		 * Takes two samples and transforms them into a second extension cadeword.
		 *
		 * @param i0 the first sample
		 * @param i1 the second sample
		 *
		 * @returns the codeword
		 */
		long transform(int i0, int i1) {
			long d0 = i0 & 0xffffffffL;
			long d1 = i1 & 0xffffffffL;
			return (d0 + d1) * (d0 + d1 + 1) / 2 + d1;
		}
	}

	private final SecondExtension secExt;


	private final boolean referenceSamples;


	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitOutputStream</code> to which samples will be encoded.
	 * @param blockSize is used to split data into blocks when the <code>code</code> function is used.
	 * @param dynamicRange specifies how many bits of each sample are significant (up to 32).
	 * @param referenceSamples Whether reference samples are inserted. (currently not supported)
	 * @param r Reference Sample Interval (up to 4096).
	 * @param s Segment Size.
	 */
	public BlockAdaptiveCoder(BitOutputStream bos, int blockSize, int dynamicRange,
			boolean referenceSamples, int r, int s, int restrictIdBits) {

		super(bos, blockSize, dynamicRange, restrictIdBits == 1 && dynamicRange <= 4);

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
	 * See the general contract for the <code>codeBlock</code> method of the <code>BlockCoder</code> interface.
	 *
	 * First a copy of the input block is created, ignoring any bits outside of the <code>dynamicRange</code>.
	 * Then the optimal coding option for the block is determined and the block itself is coded.
	 * Since zero blocks get accumulated, none or more than one block might be written to the output stream.
	 *
	 * @param inBlock is the block to be coded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void codeBlock(int[] inBlock) throws IOException {

		int[] block = maskBlockBits(inBlock);

		findBestCodingOption(block);

		// Zero blocks
		if (codingType == ZERO_BLOCK) {
			zeroBlock.addBlock();
		} else {
			zeroBlock.flushBlocks();

			// Write block
			writeId(codingType, codingOption);

			switch(codingType) {

				case SECOND_EXT :
					secExt.codeBlock();
				break;

				case BACKUP :
					backupBlock(block);
				break;

				case SAMPLE_SPLIT :
					riceCodeBlock(block, codingOption);
				break;
				
			default:
				throw new Error();
			}
		}

		blockCounter.increment();

		// End-of-Segment reached
		if (blockCounter.sOffset() == 0 || blockCounter.rOffset() == 0) {
			zeroBlock.flushRosBlock();
		}
	}


	/**
	 * See the general contract for the <code>finish</code> method of the <code>BlockCoder</code> interface.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void finish() throws IOException {

		zeroBlock.flushRosBlock();
		bos.flush();
	}


	/**
	 * Decides which coding option produces the smallest output for a given block.
	 * The result is assigned to <code>codingType</code> and <code>codingOption</code>.
	 *
	 * @param block contains the data on which the decision is based.
	 */
	protected void findBestCodingOption(int[] block) {

		// default to backup option
		codingType = BACKUP;
		codingOption = backupOption;
		long bestSize = block.length * dynamicRange + idBits;
		long size;


		long sum = sumBlock(block);

		// check for zero block
		if (sum == 0) {
			codingType = ZERO_BLOCK;
			return;
		}

		// examine second extension option
		secExt.convertToSecExt(block);
		long[] secExtBlock = secExt.getBlock();

		long secSum = 0;
		for (long value : secExtBlock) {
			if (value < 0 || value > bestSize) {
				secSum = bestSize;
				break;
			}
			secSum += value;
		}

		size = secSum + secExtBlock.length + idBits + 1;
		if (size < bestSize) {
			codingType = SECOND_EXT;
			bestSize = size;
		}

		// examine sample split options
		for (int i = 0; i < numOptions - 2; i++) {
			sum = 0 ;
			for( int sample : block) {
				sum += (sample & 0xffffffffL) >>> i ;
			}
			size = sum + (i + 1) * block.length + idBits;
			if (size < bestSize) {
				codingType = SAMPLE_SPLIT;
				codingOption = i;
				bestSize = size;
			}
		}

	}


	/**
	 * Writes the id that corresponds to <code>codingType</code> and <code>codingOption</code>.
	 *
	 * @param type the coding type
	 * @param k when the coding type is sample split this is the split position.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void writeId(CodingType type, int k) throws IOException {

		switch(type) {

			case ZERO_BLOCK :
				bos.write(idBits + 1, 0);
			break;

			case SECOND_EXT :
				bos.write(idBits + 1, 1);
			break;

			case BACKUP :
				bos.write(idBits, 0xffffff);
			break;

			case SAMPLE_SPLIT :
				bos.write(idBits, k + 1);
			break;

		}
	}


}
