/*
 * GiciLibs - EntropyIntegerCoder an implementation of MHDC Recommendation for Image Data Compression
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
package GiciEntropyCoder.EntropyIntegerCoder;

/**
 * COD_CONS class of EntropyIntegerCoder library. COD_CONS defines all the constants
 * used in the library, and explains what means the information arrays
 * <p>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class COD_CONS
{
	public static final int SAMPLE_ADAPTIVE_ENCODER = 0;  /* sample adaptive encoder */
	public static final int BLOCK_ADAPTIVE_ENCODER = 1;  /* block adaptive encoder */

	/* order of the samples */
	public static final int BI = 0; /* band interleaved order */
	public static final int BSQ = 1; /* band sequential order */

	public static final int OPTIONS_SIZE = 11; /* size of the options array */

	public static final int DR = 0; /* dynamic range */
	public static final int ICE = 1;/* initial count exponent */
	public static final int AIC = 2; /* accumulator initialization constant */
	public static final int RCS = 3; /* rescaling counter size */
	public static final int UMAX = 4; /* unary length limit */
	public static final int OWS = 5; /* output word size */
	public static final int BS = 6; /* block size */
	public static final int RSI = 7; /* reference sample interval */
	public static final int SEO = 8; /* sample encoding order */
	public static final int SFID = 9; /* subframe interleaving depth */
	public static final int ECT = 10; /* entropy coder type */
}
