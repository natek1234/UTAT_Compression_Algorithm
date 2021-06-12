
package GiciEntropyCoder.Interface;

import java.io.IOException;


/**
 * This is the interface for classes that implement online decoding functionality. Online in this case means that the
 * encoded data becomes available incrementaly.
 * Samples are decoded one block at a time.
 *
 * This interface extends the <code>Decoder</code> interface. Naturaly any encoder that can be used online can also be
 * used offline.
 */
public interface BlockDecoder {

	/**
	 * Decodes the next block of samples.
	 *
	 * @param block An array where decoded samples will be placed.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void decodeBlock(int[] block) throws IOException;

}
