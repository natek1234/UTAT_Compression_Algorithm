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
package GiciException;

/**
 * This class is an exception for error caused by lack of precision. It must be used only when program execution can't continue.<br>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class LackOfPrecisionError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor. The default message "Lack of precision error" will be passed through Error class.
	 */
	public LackOfPrecisionError() {
		super("Lack of precision error");
	}

	/**
	 * Constructor with message. The message will be passed through Error class.
	 *
	 * @param message an string that contains the message exception
	 */
	public LackOfPrecisionError(String message) {
		super(message);
	}
}
