/*
 * GICI Library -
 * Copyright (C) 2011  Group on Interactive Coding of Images (GICI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Group on Interactive Coding of Images (GICI)
 * Department of Information and Communication Engineering
 * Autonomous University of Barcelona
 * 08193 - Bellaterra - Cerdanyola del Valles (Barcelona)
 * Spain
 *
 * http://gici.uab.es
 * gici-info@deic.uab.es
 */
package GiciFile.RawImage;

import java.nio.*;
import GiciException.LackOfPrecisionError;

/**
 * This class cast an array of elements represented in byte[] to int[], long[] or float[] and vice versa.
 * @see Geometry
 */
public class TypeConverter <T> {

	//Type

	/**
	 * Indicates that the conversion is to or from int[]
	 */
	public final static int INT_ARRAY = 0;

	/**
	 * Indicates that the conversion is to or from long[]
	 */
	public final static int LONG_ARRAY = 1;

	/**
	 * Indicates that the conversion is to or from float[]
	 */
	public final static int FLOAT_ARRAY = 2;

	/**
	 * Represents the geometry of a image in BSQ mode.
	 * @see Geometry
	 */
	private final int[] geo;

	/**
	 * Indicates if the conversion of original to desired sample type has to be lossless.
	 */
	private final boolean lossless;

	/**
	 * Indicates that the conversion is to or from this type (see "Type" above)
	 */
	private final int type;

	/**
	 * The size of the original type of pixels.
	 */
	private final int size;
	
	/**
	 * The byte order of original type of pixels.
	 */
	private final ByteOrder byteOrder;

	/**
	 * Constructor that obtain the necessary parameters.
	 * @param t represents the type of array that the conversion is to or from, for example, new int[0].
	 * @param geo is the geometry of the image in BSQ mode.
	 * @param lossless indicates if the conversion of original to desired sample type has to be lossless. If it is true and occurs
	 * a problem of type conversion, a LackOfPrecisionError will be thrown.
	 * @exception ClassCastException if the Type of T is not supported.
	 * @see Geometry
	 */
	public TypeConverter (T t, int[] geo, boolean lossless) throws ClassCastException {

		if (t instanceof int[]) {
			type = INT_ARRAY;
		} else if(t instanceof long[]) {
			type = LONG_ARRAY;
		} else if(t instanceof float[]) {
			type = FLOAT_ARRAY;
		} else {
			throw new ClassCastException("The only types accepted are int[], long[] and float[]");
		}

		this.geo = geo;
		this.lossless = lossless;

		int[] sizeTable = {1/* boolean */, 1/* byte */, 2/* char */, 2/* short */, 4/* int */, 8/* long */, 4/* float */, 8/* double */};
		size = sizeTable[geo[Geometry.SAMPLE_TYPE]];

		if (geo[Geometry.BYTE_ORDER] == Geometry.BIG_ENDIAN) {
			byteOrder = ByteOrder.BIG_ENDIAN;
		} else {
			assert (geo[Geometry.BYTE_ORDER] == Geometry.LITTLE_ENDIAN);
			byteOrder = ByteOrder.LITTLE_ENDIAN;
		}
	}

	/**
	 * Test if d is integer.
	 * @param d a real number.
	 * @return if d is integer.
	 */
	private static boolean isInteger(double d) {
		long l = (long) d;
		return l == d;
	}

	/**
	 * Test if d fits in float.
	 * @param d a real number.
	 * @return if d is f fits in float.
	 */
	private static boolean fitsInFloat(double d) {
		float f = (float) d;
		return f == d;
	}

	/**
	 * Test if l fits in float.
	 * @param l an integer number.
	 * @return if l is f fits in float.
	 */
	private static boolean fitsInFloat(long l) {
		float f = l;
		return (long) f == l;
	}

	/**
	 * Test if l fits in double.
	 * @param l an integer number.
	 * @return if l is f fits in double.
	 */
	private static boolean fitsInDouble(long l) {
		double d = l;
		return (long) d == l;
	}

	/**
	 * Cast a int[], long[] or float[] to an array of elements represented in byte[].
	 * @param t is the array that will be cast 
	 * @return a byte[] with the elements.
	 */
	public byte[] TtoByte(T t) {
		switch(type) {
		case INT_ARRAY:
		{
			int[] imageSamples = (int[]) t;
			final ByteBuffer buffer = ByteBuffer.allocate(size * imageSamples.length).order(byteOrder);

			switch(geo[Geometry.SAMPLE_TYPE]) {
			case Geometry.BOOLEAN:
				for(int i=0;i<imageSamples.length;i++) {
					buffer.put((byte)(imageSamples[i] == 0 ? 0 : 1));
				}
				break;
			case Geometry.U_BYTE:
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < 0 || imageSamples[i] > 255) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						buffer.put((byte)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						byte out = (byte) (Math.max(Math.min(imageSamples[i], 255), 0));
						buffer.put(out);
					}
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer charBuffer = buffer.asCharBuffer();

				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Character.MIN_VALUE  || imageSamples[i] > Character.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						charBuffer.put((char)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						char out = (char) Math.max(Math.min(imageSamples[i], Character.MAX_VALUE), Character.MIN_VALUE);
						charBuffer.put(out);
					}
				}
				break;
			case Geometry.SHORT:
				ShortBuffer shortBuffer = buffer.asShortBuffer();

				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Short.MIN_VALUE  || imageSamples[i] > Short.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						shortBuffer.put((short)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						short out = (short) Math.max(Math.min(imageSamples[i], Short.MAX_VALUE), Short.MIN_VALUE);
						shortBuffer.put(out);
					}
				}
				break;
			case Geometry.INT:
				IntBuffer intBuffer = buffer.asIntBuffer();
				intBuffer.put(imageSamples);
				break;
			case Geometry.LONG:
				LongBuffer longBuffer = buffer.asLongBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					longBuffer.put((long)imageSamples[i]);
				}
				break;
			case Geometry.FLOAT:
				FloatBuffer floatBuffer = buffer.asFloatBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					if(!fitsInFloat(imageSamples[i]) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					floatBuffer.put((float)imageSamples[i]);
				}
				break;
			case Geometry.DOUBLE:
				DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					doubleBuffer.put((double)imageSamples[i]);
				}
				break;
			}

			return buffer.array();
		}
		case LONG_ARRAY:
		{
			long[] imageSamples = (long[]) t;
			final ByteBuffer buffer = ByteBuffer.allocate(size * imageSamples.length).order(byteOrder);

			switch(geo[Geometry.SAMPLE_TYPE]) {
			case Geometry.BOOLEAN:
				for(int i=0;i<imageSamples.length;i++) {
					buffer.put((byte)(imageSamples[i] == 0 ? 0 : 1));
				}
				break;
			case Geometry.U_BYTE:
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < 0 || imageSamples[i] > 255) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						buffer.put((byte)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						byte out = (byte) (Math.max(Math.min(imageSamples[i], 255), 0));
						buffer.put(out);
					}
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer charBuffer = buffer.asCharBuffer();

				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Character.MIN_VALUE  || imageSamples[i] > Character.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						charBuffer.put((char)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						char out = (char) Math.max(Math.min(imageSamples[i], Character.MAX_VALUE), Character.MIN_VALUE);
						charBuffer.put(out);
					}
				}
				break;
			case Geometry.SHORT:
				ShortBuffer shortBuffer = buffer.asShortBuffer();

				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Short.MIN_VALUE  || imageSamples[i] > Short.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						shortBuffer.put((short)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						short out = (short) Math.max(Math.min(imageSamples[i], Short.MAX_VALUE), Short.MIN_VALUE);
						shortBuffer.put(out);
					}
				}
				break;
			case Geometry.INT:
				IntBuffer intBuffer = buffer.asIntBuffer();

				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Integer.MIN_VALUE  || imageSamples[i] > Integer.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						intBuffer.put((int)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						int out = (int) Math.max(Math.min(imageSamples[i], Integer.MAX_VALUE), Integer.MIN_VALUE);
						intBuffer.put(out);
					}
				}
				break;
			case Geometry.LONG:
				buffer.asLongBuffer().put(imageSamples);
				break;
			case Geometry.FLOAT:
				FloatBuffer floatBuffer = buffer.asFloatBuffer();

				for(int i=0;i<imageSamples.length;i++) {
					if(!fitsInFloat(imageSamples[i]) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					floatBuffer.put((float)imageSamples[i]);
				}

				break;
			case Geometry.DOUBLE:
				DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();

				for(int i=0;i<imageSamples.length;i++) {
					if(!fitsInDouble(imageSamples[i]) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					doubleBuffer.put((double)imageSamples[i]);
				}

				break;
			}

			return buffer.array();
		}
		case FLOAT_ARRAY:
		{
			float[] imageSamples = (float[]) t;
			final ByteBuffer buffer = ByteBuffer.allocate(size * imageSamples.length).order(byteOrder);

			switch(geo[Geometry.SAMPLE_TYPE]) {
			case Geometry.BOOLEAN:
				for(int i=0;i<imageSamples.length;i++) {
					buffer.put((byte)(imageSamples[i] == 0 ? 0 : 1));
				}
				break;
			case Geometry.U_BYTE:
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < 0 || imageSamples[i] > 255) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						if(!isInteger(imageSamples[i])) {
							throw new LackOfPrecisionError("Lack of precision with one pixel");
						}
						buffer.put((byte)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						byte out = (byte) (Math.max(Math.min(imageSamples[i], 255), 0));
						buffer.put(out);
					}
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer charBuffer = buffer.asCharBuffer();
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Character.MIN_VALUE  || imageSamples[i] > Character.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						if(!isInteger(imageSamples[i])) {
							throw new LackOfPrecisionError("Lack of precision with one pixel");
						}
						charBuffer.put((char)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						char out = (char) Math.max(Math.min(imageSamples[i], Character.MAX_VALUE), Character.MIN_VALUE);
						charBuffer.put(out);
					}
				}

				break;
			case Geometry.SHORT:
				ShortBuffer shortBuffer = buffer.asShortBuffer();
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Short.MIN_VALUE  || imageSamples[i] > Short.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						if(!isInteger(imageSamples[i])) {
							throw new LackOfPrecisionError("Lack of precision with one pixel");
						}
						shortBuffer.put((short)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						short out = (short) Math.max(Math.min(imageSamples[i], Short.MAX_VALUE), Short.MIN_VALUE);
						shortBuffer.put(out);
					}
				}

				break;
			case Geometry.INT:
				IntBuffer intBuffer = buffer.asIntBuffer();
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Integer.MIN_VALUE  || imageSamples[i] > Integer.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						if(!isInteger(imageSamples[i])) {
							throw new LackOfPrecisionError("Lack of precision with one pixel");
						}
						intBuffer.put((int)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						int out = (int) Math.max(Math.min(imageSamples[i], Integer.MAX_VALUE), Integer.MIN_VALUE);
						intBuffer.put(out);
					}
				}

				break;
			case Geometry.LONG:
				LongBuffer longBuffer = buffer.asLongBuffer();
				if(lossless) {
					for(int i=0;i<imageSamples.length;i++) {
						if(imageSamples[i] < Long.MIN_VALUE  || imageSamples[i] > Long.MAX_VALUE ) {
							throw new LackOfPrecisionError("Pixel out of range");
						}
						if(!isInteger(imageSamples[i])) {
							throw new LackOfPrecisionError("Lack of precision with one pixel");
						}
						longBuffer.put((long)imageSamples[i]);
					}
				} else {
					for(int i=0;i<imageSamples.length;i++) {
						long out = (long) Math.max(Math.min(imageSamples[i], Long.MAX_VALUE), Long.MIN_VALUE);
						longBuffer.put(out);
					}
				}
				break;
			case Geometry.FLOAT:
				FloatBuffer floatBuffer = buffer.asFloatBuffer();
				floatBuffer.put(imageSamples);
				break;
			case Geometry.DOUBLE:
				DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					doubleBuffer.put((double)imageSamples[i]);
				}
				break;
			}

			return buffer.array();
		}
		
		}

		return null;
	}

	/**
	 * Cast an array of elements represented in byte[] to int[], long[] or float[].
	 * @param b is the array that will be cast 
	 * @return a T[] with the elements.
	 */
	@SuppressWarnings("unchecked")
	public T bytetoT(byte b[]) {
		ByteBuffer buffer = ByteBuffer.wrap(b).order(byteOrder);

		final int sampleType = geo[Geometry.SAMPLE_TYPE];

		switch(type) {
		case INT_ARRAY:
			int[] line = new int[b.length/size];

			switch(sampleType){
			case Geometry.BOOLEAN:
				for(int i=0;i<line.length;i++) {
					line[i] = buffer.get() == 0 ? 0 : 1;
				}
				break;
			case Geometry.U_BYTE:
				for(int i=0;i<line.length;i++) {
					line[i] = buffer.get();
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer cb = buffer.asCharBuffer();
				for(int i=0;i<line.length;i++) {
					line[i] = cb.get() & 0xffff;
				}
				break;
			case Geometry.SHORT:
				ShortBuffer sb = buffer.asShortBuffer();
				for(int i=0;i<line.length;i++) {
					line[i] = sb.get();
				}
				break;
			case Geometry.INT:
				buffer.asIntBuffer().get(line);
				break;
			case Geometry.LONG:
				LongBuffer lb = buffer.asLongBuffer();
				for(int i=0;i<line.length;i++) {
					long l = lb.get();
					if(l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					line[i] = (int) l;
				}
				break;
			case Geometry.FLOAT:
				FloatBuffer fb = buffer.asFloatBuffer();
				for(int i=0;i<line.length;i++) {
					float f = fb.get();
					if(f < Integer.MIN_VALUE || f > Integer.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					if(!isInteger(f) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					line[i] = (int) f;
				}
				break;
			case Geometry.DOUBLE:
				DoubleBuffer db = buffer.asDoubleBuffer();
				for(int i=0;i<line.length;i++) {
					double d = db.get();
					if(d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					if(!isInteger(d) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					line[i] = (int) d;
				}
				break;
			}

			return (T) line;


		case LONG_ARRAY:

			long[] longSamples = new long[b.length/size];

			switch(sampleType){
			case Geometry.BOOLEAN:
				for(int i=0;i<longSamples.length;i++) {
					longSamples[i] = buffer.get() == 0 ? 0 : 1;
				}
				break;
			case Geometry.U_BYTE:
				for(int i=0;i<longSamples.length;i++) {
					longSamples[i] = buffer.get();
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer cb = buffer.asCharBuffer();
				for(int i=0;i<longSamples.length;i++) {
					longSamples[i] = cb.get() & 0xffff;
				}
				break;
			case Geometry.SHORT:
				ShortBuffer sb = buffer.asShortBuffer();
				for(int i=0;i<longSamples.length;i++) {
					longSamples[i] = sb.get();
				}
				break;
			case Geometry.INT:
				IntBuffer ib = buffer.asIntBuffer();
				for(int i=0;i<longSamples.length;i++) {
					longSamples[i] = ib.get();
				}
				break;
			case Geometry.LONG:
				buffer.asLongBuffer().get(longSamples);
				break;
			case Geometry.FLOAT:
				FloatBuffer fb = buffer.asFloatBuffer();
				for(int i=0;i<longSamples.length;i++) {
					float f = fb.get();
					if(f < Long.MIN_VALUE || f > Long.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					if(!isInteger(f) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					longSamples[i] = (long) f;
				}
				break;
			case Geometry.DOUBLE:
				DoubleBuffer db = buffer.asDoubleBuffer();
				for(int i=0;i<longSamples.length;i++) {
					double d = db.get();
					if(d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					if(!isInteger(d) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					longSamples[i] = (long) d;
				}
				break;
			}

			return (T) longSamples;

		case FLOAT_ARRAY:
			float[] imageSamples = new float[b.length/size];

			switch(sampleType){
			case Geometry.BOOLEAN:
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = buffer.get() == 0 ? 0.0F : 1.0F;
				}
				break;
			case Geometry.U_BYTE:
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = buffer.get();
				}
				break;
			case Geometry.U_SHORT:
				CharBuffer cb = buffer.asCharBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = cb.get() & 0xffff;
				}
				break;
			case Geometry.SHORT:
				ShortBuffer sb = buffer.asShortBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = sb.get();
				}
				break;
			case Geometry.INT:
				IntBuffer ib = buffer.asIntBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = ib.get();
				}
				break;
			case Geometry.LONG:
				LongBuffer lb = buffer.asLongBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					imageSamples[i] = lb.get();
				}
				break;
			case Geometry.FLOAT:
				buffer.asFloatBuffer().get(imageSamples);
				break;
			case Geometry.DOUBLE:
				DoubleBuffer db = buffer.asDoubleBuffer();
				for(int i=0;i<imageSamples.length;i++) {
					double d = db.get();
					if(d < -Float.MAX_VALUE || d > Float.MAX_VALUE) {
						throw new LackOfPrecisionError("Pixel out of range");
					}
					if(!fitsInFloat(d) && lossless) {
						throw new LackOfPrecisionError("Lack of precision with one pixel");
					}
					imageSamples[i] = (float) d;
				}
				break;
			}

			return (T) imageSamples;


		default:
			return null;
		}


	}

}
