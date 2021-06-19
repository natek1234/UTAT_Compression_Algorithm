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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;


/**
 * This Class implements a FilterInputStream for reading from the output one bit at a time.
 */
public class BitInputStream extends FilterInputStream {

	private static final int BITS_PER_BYTE = 8;

	private static final int bmask[] = {
		0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff,
		0x1ff,0x3ff,0x7ff,0xfff,0x1fff,0x3fff,0x7fff,0xffff,
		0x1ffff,0x3ffff,0x7ffff,0xfffff,0x1fffff,0x3fffff,
		0x7fffff,0xffffff,0x1ffffff,0x3ffffff,0x7ffffff,
		0xfffffff,0x1fffffff,0x3fffffff,0x7fffffff,0xffffffff
	};

	private int buffer = 0;
	private int bitCount = 0;


	/**
	 * Create a bit-at-a-time stream that reads from supplied input stream.
	 *
	 * @param in is the stream from which bits are read.
	 */
	public BitInputStream(InputStream in) {
		super(in);
	}


	/**
	 * Returns the number of bits requested as the rightmost bits in the
	 * returned value.
	 *
	 * @param howManyBits is the number of bits to read and return (0-32).
	 *
	 * @return the value read. Only rightmost <code>howManyBits</code>
	 * are valid.
	 * 
	 * @throws IOException if there are not enough bits left.
	 */
	public int read(int howManyBits) throws IOException {

		if (howManyBits > 32 || howManyBits < 0) {
			throw new RuntimeException("BitInputStream can only return from 0 to 32 bits.");
		}

		int retval = 0;

		while (howManyBits > bitCount) {
			retval |= buffer << (howManyBits - bitCount);
			howManyBits -= bitCount;

			buffer = in.read();
			if (buffer == -1) {
				throw new EOFException();
			}

			bitCount = BITS_PER_BYTE;
		}

		if (howManyBits > 0) {
			retval |= buffer >>> (bitCount - howManyBits);
			buffer &= bmask[bitCount - howManyBits];
			bitCount -= howManyBits;
		}

		return retval;
	}


	public int read() throws IOException {
		return read(8);
	}


	public int read(byte[] values, int off, int len) throws IOException {

		int i;
		for (i = off; i < off + len; i++) {
			try {
				values[i] = (byte) read();
			} catch (EOFException eof) {
				if( i == off ) {
					return -1 ;
				}
				break;
			}
		}
		return i - off;
	}


	public int read(byte[] values) throws IOException {
		return read(values, 0, values.length);
	}

}
