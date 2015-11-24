package abc;

public class Base64Converter{
    /**
     * Used to convert a number into a Base64 character
     */
    private final String key = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    
    /**
     * Default Constructor
     */
    public Base64Converter(){
    }

    /**
     * Decodes a Base64 string and turns it into a byte array
     * @param input A Base64 encoded string
     * @return byte[] The byte equivalent of the Base64 string
     */
    public byte[] decode(String input){ 
	
	/*Throws exception if the string isn't a valid Base64String*/
	if(input.length() % 4 != 0){
	    throw new IllegalArgumentException("This is not a valid base64 string.");
	}
	StringBuffer bitString = new StringBuffer(); // Bit representation of the input string
	byte[] decodedArray; // Stores the output byte array

	for(int a = 0; a < input.length(); a++){
	    if(input.charAt(a) == '='){
		bitString.append((a + 1) == input.length() ? "000000" : "0000"); // Appends 6 zeros if there are two equals, and 4 zeros if there is one
	    }
	    else{
	        bitString.append(toBits((key.indexOf(input.charAt(a))), 6));    
	    }
	}
	decodedArray = new byte[bitString.length() / 8 - (input.indexOf('=') > 0 ? 1 : 0)]; 
	for(int a = 0, b = 0; b < decodedArray.length; a += 8, b++){
	    int tempValue = (int)bitsToChar(bitString.substring(a, a + 8), false);

	    if(tempValue > 127){
	        tempValue -= 256;
	    }
	    decodedArray[b] = (byte)tempValue;
	}
	return decodedArray;
    }

    /**
     * Encodes a byte array into a Base64 string
     * @param input the byte array to be converted
     * @return String the Base64 string
     */
    public String encode(byte[] input){
	StringBuffer bitString = new StringBuffer(), // Bit string representation of the byte array
	    encodedString = new StringBuffer(); // Base64 representation of the byte array

	/* Creates a bit string represenation of the byte array */
	for(byte a : input){
	    int temp = a;

	    if(temp < 0){
	        temp  = (a + 256);
	    }
	    bitString.append(toBits(temp, 8));
	}
	/* Appends zeroes so the bit string can be properly converted to Base64 */
	while(bitString.length() % 6 != 0){
	    bitString.append("0");
	}
	/* Takes 6 bits at a time from the string, converts them into a Base64 character, and appends them to output string */ 
	for(int a = 0; a < bitString.length(); a += 6){
	    encodedString.append(bitsToChar(bitString.substring(a, a + 6), true));
	}
	/* Appends equal signs to the output string to make it a valid Base64 string */
	while(encodedString.length() % 4 > 0){
	    encodedString.append("=");
	}
	return encodedString.toString();
    }

    /**
     * Converts a byte to a bit string equivalent
     * @param input A byte to be converted
     * @param bitLength length of the bit string
     * @return String bit string conversion of the input
     */
    private String toBits(int input, int bitLength){
        StringBuffer output = new StringBuffer();

	output.append(input & 1);
	
	while(input > 0){
	input >>= 1;
	output.append(input & 1);
	}
	/* Removes a leading zero that makes the output one greater than the bitlength */
	if(output.charAt(output.length() - 1) == '0'){
	    output = new StringBuffer(output.substring(0, output.length() - 1));
	}
	/* Appends 0's until the output is equal to the bitlength */
	while(output.length() < bitLength){
	    output.append("0");
	}
	return reverseString(output.toString());
    }

    /**
     * Converts a bit string into an ASCII or Base64 character
     * @param bits String to be converted
     * @param isBase64 Decides which conversion method to use
     * @return String the character representation of the bit string
     */
    private char bitsToChar(String bits, boolean isBase64){
        int value = 0;
	int bitCount = isBase64 ? 6 : 8;
	
	bits = reverseString(bits);
        for(int a = 0; a < bitCount; a++){
	    if(bits.charAt(a) == '1'){
		value += Math.pow(2, a);
	    }
	}
	return isBase64 ? key.charAt(value) : (char)value;
    }
    
    /**
     * Takes a string and reverses it
     * @param input String to be reversed
     * @return String reversed string
     */
    private String reverseString(String input){
	return new StringBuffer(input).reverse().toString();
    }
}
