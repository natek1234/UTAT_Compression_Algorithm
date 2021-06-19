package GiciMath;

//import java.math.BigInteger;

/**
 * 
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class IntegerMath {

	/**
	 * Returns the clipping of the integer value to the range [min, max].
	 * @param value the value that we want to clip
	 * @param min the minimum value of the range
	 * @param max the maximum value of the range
	 * @return the clipped value to the range [min,max]
	 */
	public static int clip(long value, int min, int max) {
		return (value < min) ? min : ((value > max) ? max : (int)value);
	}

	/**
	 * Returns the value of value operated in R size register
	 * @param value that will be operated
	 * @param R register size
	 * @return value operated in R size register
	 */
	public static long mod_R(long value, int R) {
		//long mask = ((long) -1) << (R);
		long mask = (R < 64)? ((long) - 1) << R: 0; 
		long mask2 = ((long) -1) << (R - 1);
		long signExtension =  (((value & mask2) << (64 - R)) >> (64 - R));
		long register = (value & ~mask) | signExtension;

		return register;
	}

	/**
	 * Returns the value of value operated in R size register
	 * @param value that will be operated
	 * @param R register size
	 * @return value operated in R size register
	 */
	/*public static long mod_R(long value, int R) {
		BigInteger bi = BigInteger.valueOf(value);
		BigInteger register = BigInteger.valueOf(1).shiftLeft(R - 1);
		BigInteger result = bi.add(register);
		result = result.mod(register.shiftLeft(1));
		result = result.subtract(register);
		
		return result.longValue();
	}*/
	
	/**
	 * Returns the sign of the integer value:
	 * 		-1 if value < 0
	 * 		1 in other case
	 * Notes that this function don't have exactly the same behavior that Math.signum()
	 * @param value the value whose sign we want to calculate
	 * @return the sign of value
	 */
	public static int positive_sign(long value) {
		return value < 0 ? -1 : 1;
	}

	/**
	 * Returns the truncated base 2 logarithm of the positive integer value.
	 * @param value a strictly positive value whose logarithm we want to calculate
	 * @return the truncated base 2 logarithm of value
	 */
	public static int log2(int value) {
		int counter = 0;

		while ((value >>= 1) > 0) {
			counter++;
		}
		return counter;
	}


	/**
	 * Returns the reverse of a two complement number.
	 * @param value an integer value in two complement
	 * @param bits the number of bits 
	 * @return value in normal binary form
	 */
	public static int complement2ToInt(int value, int bits) {
		if (value == 0) {
			return 0;
		}
		int newValue = value - 1;
		for (int i = 0; i < bits; i++) {
			newValue ^= (1 << i); 
		}
		newValue = (newValue > 1 << (bits - 1)) ? newValue - (1 << bits) : newValue;

		return newValue;
	}

	/**
	 * Returns the two complement of an integer.
	 * @param value an integer value
	 * @param bits the number of bits 
	 * @return the two complement of value 	 
	 */
	public static int intToComplement2(int value, int bits) {
		int newValue = (value < 0) ? value + (1 << bits) : value;
		for (int i = 0; i < bits; i++) {
			newValue ^= (1 << i); 
		}

		return newValue + 1;
	}

}
