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

import java.io.*;
import java.util.*;

/**
 * Offers a ListIterator to write and read three-dimensional images in files using low memory
 * and abstracting pixel order and sample type.
 * @see RawImageIterator
 */
public class RawImage {

	//Mode

	/**
	 * Indicates that image can be read from a file
	 */
	public final static int READ = 1;

	/**
	 * Indicates that image can be write to a file.
	 */
	public final static int WRITE = 2;

	/**
	 * Indicates that image can be read or write to a file.
	 */
	public final static int RW = 3; //RW == READ | WRITE

	/**
	 * Represents the geometry of a image in BSQ mode.
	 * @see Geometry
	 */
	private int[] geo;

	/**
	 * Is the file that contain of will contain the image.
	 */
	private File file;

	/**
	 * represents the original pixel order of the image by the transformation of BSQ to
	 * the original pixel order.
	 */
	private int[] pixelOrder;

	/**
	 * Indicates the operations that can be done with file (see "Mode" above)
	 */
	private int mode;

	/**
	 * A list of all iterators built by this class.
	 */
	private Vector<RawImageIterator<?> > iterators;

	/**
	 * Constructor that obtain the necessary parameters.
	 * @param name is the file name that contains or will contain the image.
	 * @param geo is the geometry of the image in BSQ mode.
	 * @param pixelOrder represents the original pixel order of the image by the transformation of BSQ to
	 * the original pixel order.
	 * @param mode indicates the operations that can be done with file (read, write,...)
	 * @see Geometry
	 */
	public RawImage(String name, int[] geo, int[] pixelOrder, int mode) {
		this(new File(name), geo, pixelOrder, mode);
	}

	/**
	 * Constructor that obtain the necessary parameters.
	 * @param file represents the file that contains or will contain the image.
	 * @param geo is the geometry of the image in BSQ mode.
	 * @param pixelOrder represents the original pixel order of the image by the transformation of BSQ to
	 * the original pixel order.
	 * @param mode indicates the operations that can be done with file (read, write, ...)
	 * @see Geometry
	 */
	public RawImage(File file, int[] geo, int[] pixelOrder, int mode) {
		this.file = file;
		this.geo = geo;
		this.mode = mode;
		this.pixelOrder = pixelOrder;
		iterators = new Vector<RawImageIterator<?> >();
	}

	/**
	 * Built a ListIterator to do the desired operation with a three-dimensional image.
	 * @param t represents the type of array that will contain the pixels, for example, new int[0].
	 * @param pixelOrderTransfomation represents the pixel order transformation between original and desired pixel order of
	 * the image.
	 * @param mode indicates the operations (read, write, ...) that can be done with the iterator returned by this method. Note
	 * that this operations must be included in the available operations specified when the object was built.
	 * @param lossless indicates if the conversion of original to desired sample type has to be lossless. If it is true and occurs
	 * a problem of type conversion, a LackOfPrecisionError will be thrown.
	 * @return a iterator over all image.
	 * @exception IOException if an error occurs in read or write operations.
	 * @exception UnsupportedOperationException if mode is incompatible with mode specified when this object was built.
	 * @exception ClassCastException if the Type of T is not supported.
	 * @see Geometry
	 * @see TypeConverter
	 */
	public <T> ListIterator<T> getIterator(T t, int[] pixelOrderTransformation, int mode, boolean lossless) throws IOException, UnsupportedOperationException, IndexOutOfBoundsException, ClassCastException {
		if((mode & READ) > (this.mode & READ) || (mode & WRITE) > (this.mode & WRITE)) {
			throw new UnsupportedOperationException("The operation " + mode + " is not available in mode " + this.mode);
		}
		RawImageIterator<T> it = new RawImageIterator<T>(this, t, file, geo, pixelOrder, pixelOrderTransformation, mode, lossless);
		iterators.add(it);
		return it;
	}

	/**
	 * Built a ListIterator to do the desired operation with a band of three-dimensional image.
	 * @param t represents the type of array that will contain the pixels, for example, new int[0].
	 * @param pixelOrderTransfomation represents the pixel order transformation between original and desired pixel order of
	 * the image.
	 * @param mode indicates the operations (read, write, ...) that can be done with the iterator returned by this method. Note
	 * that this operations must be included in the available operations specified when the object was built.
	 * @param lossless indicates if the conversion of original to desired sample type has to be lossless. If it is true and occurs
	 * a problem of type conversion, a LackOfPrecisionError will be thrown.
	 * @param band is the index of the band.
	 * @return a iterator over all band.
	 * @exception IOException if an error occurs in read or write operations.
	 * @exception UnsupportedOperationException if mode is incompatible with mode specified when this object was built.
	 * @exception IndexOutOfBoundsException is the band is out of range of the bands that compose the image.
	 * @exception ClassCastException if the type of T is not supported.
	 * @see Geometry
	 * @see TypeConverter
	 */
	public <T> ListIterator<T> getIteratorByBand(T t, int[] pixelOrderTransformation, int mode, boolean lossless, int band) throws IOException, UnsupportedOperationException, IndexOutOfBoundsException, ClassCastException {
		if((mode & READ) > (this.mode & READ) || (mode & WRITE) > (this.mode & WRITE)) {
			throw new UnsupportedOperationException("The operation "+ mode +" is not available in mode " + this.mode);
		}
		RawImageIterator<T> it = new RawImageIterator<T>(this, t, file, geo, pixelOrder, pixelOrderTransformation, mode, lossless, band);
		iterators.add(it);
		return it;
	}

	/**
	 * @param it is a instance of RawImageIterator<T>. Is not checked that it has been built by this class.
	 * Close the file associated at this iterator.
	 * @exception ClassCastException if it is not a instance of RawImageIterator<T>
	 * @exception IOException if there are any problems closing the file associated.
	 */
	public <T> void close(ListIterator<T> it) throws ClassCastException, IOException {
		if(!(it instanceof RawImageIterator<?>)) {
			throw new ClassCastException("close need a RawImageIterator<T>");
		}
		RawImageIterator<T> iterator = (RawImageIterator<T>) it;
		iterators.remove(iterator);
		iterator.close();
	}

	/**
	 * Close all iterators used built by this instance.
	 * @exception Throwable if there are any problems closing this iterators.
	 */
	public void finalize() throws Throwable {
		try {
			while(!iterators.isEmpty()) {
				close(iterators.remove(0));
			}
		} finally {
			super.finalize();
		}
	}
}
