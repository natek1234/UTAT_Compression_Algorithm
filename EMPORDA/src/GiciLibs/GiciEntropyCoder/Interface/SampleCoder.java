
package GiciEntropyCoder.Interface;

import java.io.IOException;


/**
 * This is the interface for classes that implement online encoding functionality. Online in this case means that the
 * data to be encoded becomes available incrementaly.
 * Samples are encoded one at a time.
 *
 * This interface extends the <code>Coder</code> interface. Naturaly any encoder that can be used online can also be
 * used offline.
 */
public interface SampleCoder extends Coder {

	/**
	 * Encodes the next sample.
	 *
	 * @param sample The next sample to be encoded.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void codeSample(int sample) throws IOException;


	/**
	 * Ensures that the encoding process has finished correctly.
	 * The classes that implement this interface might be doing any number of things behind the scenes (such as
	 * buffering or preprocessing). This function must allways be called after the last <code>codeSample</code> call to
	 * ensure that all data has been written to the output stream.
	 *
	 * @throws IOException if an IO error prevents the process from completing.
	 */
	void finish() throws IOException;

}
