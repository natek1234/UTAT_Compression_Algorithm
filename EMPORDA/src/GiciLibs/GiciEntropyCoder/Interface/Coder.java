
package GiciEntropyCoder.Interface;

import java.io.IOException;


/**
 * This is the interface for classes that implement offline encoding functionality. Offline in this case means that all
 * the data to be encoded is available before the encoding process begins.
 */
public interface Coder {

	/**
	 * Encodes <code>data.length</code> samples.
	 *
	 * @param data An array containing the data to be encoded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void code(int[] data) throws IOException;

}
