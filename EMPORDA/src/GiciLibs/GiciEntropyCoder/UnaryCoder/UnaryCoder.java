
package GiciEntropyCoder.UnaryCoder;

import GiciEntropyCoder.Interface.SampleCoder;

import GiciStream.BitOutputStream;
import java.io.IOException;


/**
 * This Class implements a Unary encoder.
 *
 * For details on Unary Coding see:
 * http://en.wikipedia.org/wiki/Unary_coding
 *
 * The methods of this class that perform coding, take their inputs as unsigned ints.
 * This means that the most significant bit will be treated as indicating magnitude instead of sign.
 * The above will not matter for emporda, where the maximum dynamic range is 16 bits.
 */
public class UnaryCoder implements SampleCoder {

	private final BitOutputStream bos;


	/**
	 * Constructor.
	 *
	 * @param bos is a <code>BitOutputStream</code> to which samples will be encoded.
	 */
	public UnaryCoder(BitOutputStream bos) {
		this.bos = bos;
	}


	/**
	 * See the general contract for the <code>code</code> method of the <code>Coder</code> interface.
	 *
	 * To encode the samples, <code>codeSample</code> is called on each one.
	 *
	 * @param data is the array of samples to be encoded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void code(int[] data) throws IOException {

		for (int sample : data) {
			codeSample(sample);
		}
		finish();
	}


	/**
	 * See the general contract for the <code>codeSample</code> method of the <code>SampleCoder</code> interface.
	 *
	 * Writes the Unary Codeword for a sample to the output stream.
	 *
	 * @param sample is the value to be encoded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void codeSample(int sample) throws IOException {

		long value = sample & 0xffffffffL;
		
		for (long i = 0; i < value; i++) {
			bos.write(1, 0);
		}
		bos.write(1, 1);
	}


	/**
	 * See the general contract for the <code>finish</code> method of the <code>SampleCoder</code> interface.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	public void finish() throws IOException {
		bos.flush();
	}

}
