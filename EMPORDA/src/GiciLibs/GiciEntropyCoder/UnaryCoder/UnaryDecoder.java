
package GiciEntropyCoder.UnaryCoder;

import GiciEntropyCoder.Interface.SampleDecoder;

import GiciStream.BitInputStream;
import java.io.IOException;


/**
 * This Class implements a Unary decoder.
 *
 * For details on its function see:
 * http://en.wikipedia.org/wiki/Unary_coding
 *
 * The methods of this class that perform decoding, will return their outputs as unsigned ints.
 * This means that the most significant bit will indicate magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class UnaryDecoder implements SampleDecoder {

	private final BitInputStream bis;


	/**
	 * Constructor.
	 *
	 * @param bis is a <code>BitInputStream</code> from which samples will be decoded.
	 */
	public UnaryDecoder(BitInputStream bis) {
		this.bis = bis;
	}


	/**
	 * See the general contract for the <code>decode</code> method of the <code>Decoder</code> interface.
	 *
	 * To decode the samples, <code>decodeSample</code> is called for each one.
	 *
	 * @param data is the array to which decoded samples will be written.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void decode(int[] data) throws IOException {

		for (int i = 0; i < data.length; i++) {
			data[i] = decodeSample();
		}
	}


	/**
	 * See the general contract for the <code>codeSample</code> method of the <code>SampleCoder</code> interface.
	 *
	 * Reads a Unary Codeword from the input stream and returns the decoded sample.
	 *
	 * @return the value of the decoded sample.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public int decodeSample() throws IOException {

		long value = 0;

		while (bis.read(1) == 0) {
			value++;
		}
		return (int) value;
	}

}
