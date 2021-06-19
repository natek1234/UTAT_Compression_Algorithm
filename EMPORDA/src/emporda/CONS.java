/*
 * EMPORDA Software - More than an implementation of MHDC Recommendation for Image Data Compression
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
 * http://sourceforge.net/projects/emporda
 * gici-info@deic.uab.es
 */

package emporda;

/**
 * CONS class of EMPORDA application. CONS defines all the constants
 * used in the whole program, and explains what means the information arrays
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class CONS {

	/* type constants */
	public static final int FULL_PRED_MODE = 0;         /* full prediction mode */
	public static final int REDUCED_PRED_MODE = 1;      /* reduced prediction mode */
	public static final int NEIGHBOR_ORIENTED_SUM = 0;  /* neighbor oriented sum */
	public static final int COLUMN_ORIENTED_SUM = 1;    /* column oriented sum */
	public static final int DEFAULT_WEIGHT_INIT = 0;    /* default weight initialization */
	public static final int CUSTOM_WEIGHT_INIT = 1;     /* custom weight initialization */
	public static final int SAMPLE_ADAPTIVE_ENCODER = 0; /* sample adaptive encoder */
	public static final int BLOCK_ADAPTIVE_ENCODER = 1; /* block adaptive encoder */
	public static final int BAND_INTERLEAVE = 0; /* band interleave order */
	public static final int BAND_SEQUENTIAL = 1; /* band sequential order */

	public static final int BYTE = 8; /* size of a byte in bits */
	public static final int SHORT = 16; /* size of a short in bits */

	/* size constants */
	public static final int GEO_SIZE = 6;        /* size of imageGeometry */
	public static final int IMAGE_OPTION_SIZE = 4; /* size of imageOption */
	public static final int IMAGE_FLAGS_SIZE = 3; /*size of imageFlags */
	public static final int ALG_OPTION_SIZE = 4;  /* size of algOption */
	public static final int WEIGHT_OPTION_SIZE = 7; /* size of weightOption */
	public static final int ENTROPY_OPTION_SIZE = 7; /* size of entropyOption */

	/* constants for the geometry */
	public static final int BANDS = 0;     /* number of bands */
	public static final int HEIGHT = 1;    /* image height */
	public static final int WIDTH = 2;     /* image width */
	public static final int TYPE = 3;      /* image sample type */
	public static final int ENDIANESS = 4; /* byte order of pixels */
	public static final int RGB = 5;       /* if image has header */

}
