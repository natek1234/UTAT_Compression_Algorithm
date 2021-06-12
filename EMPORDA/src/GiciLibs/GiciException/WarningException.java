/*
 * GICI Library -
 * Copyright (C) 2007  Group on Interactive Coding of Images (GICI)
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
 * This class is an exception for warning messages. It must be used only when program execution can continue.<br>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.1
 */
public class WarningException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * An error code.
	 *<p>
	 * Values are usually defined by the application which uses this class. If this parameter is not set, the value is -1.
	 */
	int errorCode;


	/**
	 * Default constructor. When an exception without any message is thrown, a default message will be used.
	 */
	public WarningException(){
		super("Warning exception.");
		this.errorCode = -1;
	}

	/**
	 * Constructor with message. The message will be passed through Exception class.
	 *
	 * @param message an string that contains the message exception
	 */
	public WarningException(String message){
		super(message);
		this.errorCode = -1;
	}

	/**
	 * Constructor with error code and message. The message will be passed through the Exception class.
	 *
	 * @param message an string that contains the message exception
	 * @param errorCode the error code
	 */
	public WarningException(String message, int errorCode){
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Returns the error code of this exception.
	 *
	 * @return errorCode defined in {@link #errorCode}
	 */
	public int getErrorCode(){
		return(errorCode);
	}

}
