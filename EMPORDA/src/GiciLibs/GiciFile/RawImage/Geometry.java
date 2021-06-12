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

/**
 * Represents the geometry of three-dimensional image in BSQ mode.
 */
public class Geometry {
	//Geometry

	/**
	 * Is the position in which be saved the number of bands of the image in BSQ mode.
	 */
	public final static int Z_SIZE = 0;
	
	/**
	 * Is the position in which be saved the number of lines of the image in BSQ mode.
	 */
	public final static int Y_SIZE = 1;
	
	/**
	 * Is the position in which be saved the number of columns of the image in BSQ mode.
	 */
	public final static int X_SIZE = 2;
	
	/**
	 * Is the position in which be saved the sample type, see "sample" below.
	 */
	public final static int SAMPLE_TYPE = 3;
	
	/**
	 * Is the position in which be saved the byte order, see "byte-order" below.
	 */
	public final static int BYTE_ORDER = 4;
	
	/**
	 * Indicates the number of elements in the geometry. Is useful to memory allocation for arrays
	 * that contain the geometry values.
	 */
	public final static int GEO_SIZE = 5;

	//Sample
	
	/**
	 * Indicates that pixels have two possible values: false (0) or true (other values). Needs 1 byte.
	 */
	public final static int BOOLEAN = 0;
	
	/**
	 * Indicates that pixels are (unsigned) integers in range [0, 255]. Needs 1 byte.
	 * @see Boolean
	 */
	public final static int U_BYTE = 1;
	
	/**
	 * Indicates that pixels are (unsigned) integers in range [0, Character.MAX_VALUE]. Needs 2 bytes.
	 * @see Character
	 */
	public final static int U_SHORT = 2;
	
	/**
	 * Indicates that pixels are integers in range [Short.MIN_VALUE, Short.MAX_VALUE]. Needs 2 bytes.
	 * @see Short
	 */
	public final static int SHORT = 3;
	
	/**
	 * Indicates that pixels are integers in range [Integer.MIN_VALUE, Integer.MAX_VALUE]. Needs 4 bytes.
	 * @see Integer
	 */
	public final static int INT = 4;
	
	/**
	 * Indicates that pixels are integers in range [Long.MIN_VALUE, Long.MAX_VALUE]. Needs 8 bytes.
	 * @see Long
	 */
	public final static int LONG = 5;
	
	/**
	 * Indicates that pixels are real numbers with simple precision. Needs 4 bytes.
	 * @see Float
	 */
	public final static int FLOAT = 6;
	
	/**
	 * Indicates that pixels are real numbers with double precision. Needs 8 bytes.
	 * @see Double
	 */
	public final static int DOUBLE = 7;

	//Byte-Order

	/**
	 * Indicates that pixels are saved in big endian byte-order.
	 */
	public final static int BIG_ENDIAN = 0;

	/**
	 * Indicates that pixels are saved in little endian byte-order.
	 */
	public final static int LITTLE_ENDIAN = 1;

	/**
	 * The default constructor is overwritten because it is not necessary create an object of this class.
	 */
	private Geometry() {}
}
