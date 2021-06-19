/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * BitInputStream and BitOutputStream are slight modifications of the similarly
 * named classes authored by Professor Owen Astrachan from Duke University.
 */

package GiciStream;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;


/**
 * This Class implements a FilterOutputStream for writing to the output one bit at a time.
 */
public class BitOutputStream extends FilterOutputStream {

	private static final int BITS_PER_BYTE = 8;

	private static final int bmask[] = {
		0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff,
		0x1ff,0x3ff,0x7ff,0xfff,0x1fff,0x3fff,0x7fff,0xffff,
		0x1ffff,0x3ffff,0x7ffff,0xfffff,0x1fffff,0x3fffff,
		0x7fffff,0xffffff,0x1ffffff,0x3ffffff,0x7ffffff,
		0xfffffff,0x1fffffff,0x3fffffff,0x7fffffff,0xffffffff
	};

	private int buffer = 0;
	private int bitsToGo = BITS_PER_BYTE;


	/**
	 * Create a bit-at-a-time stream that writes to the supplied output stream.
	 *
	 * @param out is the output stream to which bits are written.
	 */
	public BitOutputStream(OutputStream out) {
		super(out);
	}


	/**
	 * Flushes bits not yet written. Either this function or
	 * <code>close</code> must be called to ensure that all bits will be
	 * written.
	 *
	 * @throws IOException if there's a problem writing bits.
	 */
	public void flush() throws IOException {

		if (bitsToGo != BITS_PER_BYTE) {
			out.write(buffer << bitsToGo);
			buffer = 0;
			bitsToGo = BITS_PER_BYTE;
		}

		out.flush();
	}


	/**
	 * Releases system resources associated with the stream and flushes bits
	 * not yet written. Either this function or <code>flush</code> must be
	 * called to ensure that all bits will be written.
	 *
	 * @throws IOException if close fails.
	 */
	public void close() throws IOException {
		flush();
		out.close();
	}


	/**
	 * Write specified number of bits from value to the stream.
	 *
	 * @param howManyBits is number of bits to write (0-32).
	 * @param value is the source of bits. Rightmost bits are written.
	 *
	 * @throws IOException if there's an I/O problem writing bits
	 */
	public void write(int howManyBits, int value) throws IOException {

		if (howManyBits > 32 || howManyBits < 0) {
			throw new RuntimeException("BitInputStream can only write from 0 to 32 bits.");
		}

		value &= bmask[howManyBits]; // only right most bits valid

		while (howManyBits >= bitsToGo) {
			buffer = (buffer << bitsToGo) | (value >>> (howManyBits - bitsToGo));
			out.write(buffer);

			value &= bmask[howManyBits - bitsToGo];
			howManyBits -= bitsToGo;
			bitsToGo = BITS_PER_BYTE;
			buffer = 0;
		}

		if (howManyBits > 0) {
			buffer = (buffer << howManyBits) | value;
			bitsToGo -= howManyBits;
		}
	}


	public void write(int value) throws IOException {
		write(value, 8);
	}


	public void write(byte[] values, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			write(values[i]);
		}
	}


	public void write(byte[] values) throws IOException {
		write(values, 0, values.length);
	}

}
