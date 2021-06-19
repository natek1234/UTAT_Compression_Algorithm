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
 * This class provide the mechanisms necessarys to do the conversion between the diferents modes
 * of ordering the pixels of three-dimensional image.
 */
public class OrderConverter {
	
	/**
	 * Represents the conversion of BIL to BSQ mode
	 */
	public static final int[] DIM_TRANSP_BIL_TO_BSQ = {1, 0, 2};

	/**
	 * Represents the conversion of BIP to BSQ mode
	 */
	public static final int[] DIM_TRANSP_BIP_TO_BSQ = {2, 0, 1};

	/**
	 * Represents the conversion of BSQ to BIL mode
	 */
	public static final int[] DIM_TRANSP_BSQ_TO_BIL = DIM_TRANSP_BIL_TO_BSQ;

	/**
	 * Represents the conversion of BSQ to BIP mode
	 */
	public static final int[] DIM_TRANSP_BSQ_TO_BIP = {1, 2, 0};

	/**
	 * Represents the conversion of BIL to BIP mode
	 */
	public static final int[] DIM_TRANSP_BIL_TO_BIP = {0, 2, 1};

	/**
	 * Represents the conversion of BIL to BIP mode
	 */
	public static final int[] DIM_TRANSP_BIP_TO_BIL = DIM_TRANSP_BIL_TO_BIP;

	/**
	 * Represents the absence of conversion.
	 */
	public static final int[] DIM_TRANSP_IDENTITY = {0, 1, 2};

	/**
	 * Represents the length of the three dimensions of the image expressed with
	 * the original pixel order of the image.
	 */
	private int[] size = null;

	/**
	 * Represents the pixel order transformation between original and final pixel order of
	 * the image.
	 */
	private int[] pixelOrder = null;

	/**
	 * Constructor that obtain the necessary parameters to do the conversion between modes.
	 * @param geo is the geometry of the image in BSQ mode.
	 * @param originalPixelOrder represents the original pixel order of the image by the transformation of BSQ to
	 * the original pixel order.
	 * @param pixelOrderTransfomation represents the pixel order transformation between original and final pixel order of
	 * the image.
	 * @see Geometry
	 */
	public OrderConverter(int[] geo, int[] originalPixelOrder, int[] pixelOrderTransformation) {
		size = new int[3];
		size[Geometry.Z_SIZE] = geo[originalPixelOrder[Geometry.Z_SIZE]];
		size[Geometry.Y_SIZE] = geo[originalPixelOrder[Geometry.Y_SIZE]];
		size[Geometry.X_SIZE] = geo[originalPixelOrder[Geometry.X_SIZE]];
		
		pixelOrder = new int[3];
		pixelOrder[Geometry.Z_SIZE] = pixelOrderTransformation[Geometry.Z_SIZE];
		pixelOrder[Geometry.Y_SIZE] = pixelOrderTransformation[Geometry.Y_SIZE];
		pixelOrder[Geometry.X_SIZE] = pixelOrderTransformation[Geometry.X_SIZE];
	}

	/**
	 * Convert the absolute position of a pixel in desired pixel order in the corresponding
	 * position in the original pixel order.
	 * @param position is the absolute position of the pixel in original pixel order.
	 * @return the absolute position of the pixel in final pixel order.
	 */
	public int getAddress(int position) {
		int aux[] = new int[3];
		int tmp;
		aux[pixelOrder[Geometry.Z_SIZE]] = position / (size[pixelOrder[Geometry.X_SIZE]]*size[pixelOrder[Geometry.Y_SIZE]]);
		tmp = position % (size[pixelOrder[Geometry.X_SIZE]]*size[pixelOrder[Geometry.Y_SIZE]]);
		aux[pixelOrder[Geometry.Y_SIZE]] = tmp/size[pixelOrder[Geometry.X_SIZE]];
		aux[pixelOrder[Geometry.X_SIZE]] = tmp % size[pixelOrder[Geometry.X_SIZE]];
		return (aux[Geometry.Z_SIZE]*size[Geometry.Y_SIZE]+aux[Geometry.Y_SIZE])*size[Geometry.X_SIZE]+aux[Geometry.X_SIZE];
	}

	/**
	 * Return the distance (in pixels) in the original byte order between consecutive pixels in the same line of 
	 * desired pixel order. In other words, if we consider the desired pixel order and x and x+1 are consecutive 
	 * pixels in the same line (in this order), this function returns getAddress(x+1) - getAddress(x).
	 * @return the distance (in pixels) in the original byte order between consecutive pixels in the same line of 
	 * original pixel order if image have more than one pixel per line (in original pixel order), or 0 in another case.
	 */
	public int getColumnOffset() {
		if(size[pixelOrder[Geometry.X_SIZE]] == 1) {
			return 0;
		}
		return getAddress(1);
	}

}
