/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encryption;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
/**
 *
 * @author Stephen
 */
public class Encryption {

    /*This static String array stores our usernames, salts, and hashed passwords 
      in the format: <username>:<salt>:<hashed password>
      The username portion of the project is incomplete at the moment.*/
    String[] USERNAMES = new String[]{"sdthomas92:f7d3f5ty2s:<password>"};
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //The program contains hashed passwords from each member that are hard-coded
        //Before sending, the username/password must be correct
        //(stored as "<username>:<salt>:<hashed password>" in the code)
        //This string is sent to the reciever for verification (in the form of: <username>:<hashed password>)
        //Using the salt given
        //If the username/password is not correct, the user is prompted that it is invalid
        //Otherwise, the file can be sent

        //Break up data (File object) into chunks (1024 length byte array)
        //Each chunk is hashed, and the hash is merged with the chunk (now 1028 bytes in length).
        //The chunks are then encrypted (given a key via File object from the professor)
        //The encrypted chunks are encoded in BASE64
        //The chunks are sent to reciver one by one.
        
        //The receiver decodes each chunk from BASE64
        //The chunks are decrypted
        //The first 1024 bytes of the chunk is hashed, and compared with the associated hash the chunk was sent with
        //If the sent hash does not match the hashed chunk, an error has occured,
        //the reciver tells the sender to resend.
        //If all the chunks are properly received, the chunks are put together into a single byte array (without the hashes).
        //This byte array is turned into a File object.
        
        File inputFile = new File("test.txt");
        File keyFile = new File("key.txt");
        
        sendFile(inputFile, keyFile);
    }
    
    public static String verifyUsernameAndPassword(String username, String salt, String password) throws Exception {
        byte[] hash = hash(password.getBytes("UTF-8"), salt.getBytes("UTF-8"));
        String hashPassword = encodeBase64(hash);
        String result = username + ":" + salt + ":" + hashPassword;
        //Send result to the receiver, the receiver checks the USERNAMES array to
        //see if the result matches with any of its contents. If so, the user is
        //verified.
        return result;
    }
    
    /**
     * This method sends a given file to the receiver with a given key used to
     * encrypt it. The file is broken up into 1024 byte chunks, and each chunk
     * is hashed. Each chunk and hash are merged together, encrypted, encoded in
     * Base 64, and sent to the receiver chunk by chunk.
     * 
     * @param inputFile
     * @param keyFile 
     */
    public static void sendFile(File inputFile, File keyFile) {       
        byte[] file, key;
        try {
            //Converts both the inputFile and keyFile into byte arrays.
            file = fileToByteArray(inputFile);
            key = fileToByteArray(keyFile);
            
            //For every kilobyte in the file, a chunk is made, hashed, and these
            //are both merged into a byte array of length 1028, encrypted, encoded 
            //in Base 64, and sent.
            for(int i = 0; i < file.length; i += 1024) {
                //Creates a kilobyte byte array for the current file chunk. If
                //this is the last iteration and this file chunk is less than a
                //kilobyte in length (the remander of the file), the byte array
                //length is adjusted accordingly.
                byte[] send = new byte[(file.length - i) < 1024 ? (file.length - i) + 4 : 1028];
                
                //The new byte array is filled with the chunk's contents.
                for(int j = 0; j < send.length-4; j++)
                    send[j] = file[i+j];
               
                //The hash is generated (4 bytes in length).
                byte[] hash = hash(send, key);
                
                //The hash is put in the last remaining 4 bytes of the byte array to be sent.
                for(int j = 0; j < hash.length; j++)
                    send[1024 + j] = hash[j];
                
                //The byte array is encrypted.
                byte[] encrypt = encryptDecrypt(send, key);
                
                String encode = encodeBase64(encrypt);
                
                byte[] finalArray = encode.getBytes("UTF-8");
                /*Send the resulting byte array to the reciever. This byte 
                  array represents the encrypted chunk and hash, and decoded in
                  base 64. Have the receiver recieve the chunks and store all 
                  the byte array results in a single byte array and send to the
                  "recieveFinishedDataPiece" method.
                */
            }
        }
        catch (Exception e){
            System.out.println("File or key don't exist.");
        }      
    }
    
    /**
     * This method should be run every time a chunk is received by the receiver.
     * The chunk (byte[] data) is converted to the original byte array that represented
     * the data that was originally passed in the "sentFile" method. The returned
     * byte arrays from this method should all be added to a byte list, which is
     * then converted to a File object, and this File object should be identical
     * to original sent File. If a byte array is received by this method and the
     * hash that it is sent with is not equal to the hashed "finishedData" byte
     * array, then there was an error with the data transmitted, and the method returns
     * null. If at any time this method returns null, send an error to the sender
     * that the file was not properly received.
     * 
     * @param data The file chunk that was sent.
     * @param key The file that represents the key.
     * @return The decrypted, decoded data chunk (1024 bytes in length); null if
     * the hashes didn't match.
     * @throws Exception If there was an error getting a String from the base 64 encoded
     * byte array, or if the key file cannot be converted to a byte array.
     */
    public static byte[] recieveFinishedDataPiece(byte[] data, File key) throws Exception {
        //Converts the sent byte array to a string, which is decoded and turned into
        //a new byte array.
        byte[] decode = decodeBase64(new String(data, "UTF-8"));
        
        //The byte array is decrypted.
        byte[] decrypt = encryptDecrypt(decode, fileToByteArray(key));
        //This is the byte array that will store the originally sent data in the
        //chunk.
        byte[] finishedData = new byte[data.length - 4];
        
        //This stores the hash that came with the chunk.
        byte[] hash = new byte[4];
        
        //Fills both of these arrays with their proper values.
        for(int i = 0; i < finishedData.length; i++)
            finishedData[i] = decrypt[i];
        for(int i = 0; i < hash.length; i++)
            hash[i] = decrypt[(data.length-4) + i];
        //If the hash included with the chunk does not equal the hashed version
        //of the finished data, then the file was sent incorrectly, and null
        //is returned.
        if(!compareByteArrays(hash(finishedData, fileToByteArray(key)), 
                              hash))
            return null;
        //The data originally put in the chunk is returned.
        return finishedData;
    }
    
    /**
     * THIS METHOD IS INCOMPLETE, AND NEEDS TO BE FINISHED.
     * @param data
     * @return 
     */
    public static String encodeBase64(byte[] data) {
        return null;
    }
 
        /**
     * THIS METHOD IS INCOMPLETE, AND NEEDS TO BE FINISHED.
     * @param data
     * @return 
     */
    public static byte[] decodeBase64(String data) {
        return null;
    }
    
    /**
     * Compares two byte arrays. If they are not equal, the method returns
     * false, otherwise, it returns true.
     * @param a
     * @param b
     * @return 
     */
    public static boolean compareByteArrays(byte[] a, byte[] b) {
        if(a.length != b.length)
            return false;
        for(int i = 0; i < a.length; i++) {
            if(a[i] != b[i])
                return false;
        }
        return true;
    }
    
    /**
     * This method can encrypt or decrypt byte arrays. If the input is in
     * plaintext, the result will be ciphertext, and vice versa.
     * @param in The byte array that is supposed to be encrypted or decrypted. 
     *           If this method is used to encrypt, this would be the plaintext 
     *           after being converted to a byte array. If this method is used 
     *           to decrypt, this would be the ciphertext after being converted 
     *           to a byte array.
     * @param key The byte array which represents the key.
     * @return The byte array that is either encrypted or decrypted, depending
     *         on whether or not plaintext or ciphertext is used as "in".
     */
    public static byte[] encryptDecrypt(byte[] in, byte[] key) throws Exception {
        byte[] out = new byte[in.length];
        for(int i = 0; i < out.length; i++)
           out[i] = (byte)((int)in[i] ^ (int)key[i % key.length]);
        return out;
    }
    
    /**
     * This method converts a file into a byte array. This is used in the sending
     * process, when the file to be sent and the key are converted into byte
     * arrays.
     * @param f
     * @return
     * @throws Exception 
     */
    public static byte[] fileToByteArray(File f) throws Exception {
        if(!f.exists())
            return null;
        Path p = Paths.get(f.getPath());
        return Files.readAllBytes(p);
    }
    
    /**
     * This method uses a given byte array and salt (also a byte array) to
     * generate a hash code. The hash code that's generated is four bytes
     * long (32 bits). The actual hash algorithm (in the method innerHash) is
     * repeated numerous times (depending on the salt given).
     * @param input
     * @param salt
     * @return 
     */
    public static byte[] hash(byte[] input, byte[] salt) {
        byte[] data = new byte[4];
        for(int i = 0; i < data.length; i++)
            data[i] = input[i % input.length];     
        for(int i = 0; i < salt.length; i++) {
            //Performs the actual hash algorithm multiple times (hashes
            //the hashed byte array again).
            byte[] temp = innerHash(data, salt[(i+i)%2]);
            for(int j = 0; j < data.length; j++)
                data[j] = temp[j % temp.length];
        }
        return data;      
    }
    
    /**
     * The actual hash algorithm that takes a byte array of length 4, and produces
     * a hashed version of it (also 4 bytes long). Each individual byte undergoes
     * a different computation, involving bit-shifts, XOR operations, OR operations,
     * additions, and more.
     * @param data
     * @param salt
     * @return 
     */
    public static byte[] innerHash(byte[] data, Byte salt) {
        if(data.length != 4)
            return null;
        byte[] result = new byte[4];
        byte sum0 = (byte)(((int)data[0] + byteArrayToInt(data)) % 256);
        byte bitShift3 = (byte)(data[2] >> 3);
        byte saltXOR = (byte)(data[1] ^ salt);
        byte sum3 = (byte)(((int)data[3] + (int)bitShift3) % 256);
        
        result[0] = (byte)(((int)sum3 + (int)saltXOR) % 256);
        result[1] = (byte)(((int)sum0 + (int)bitShift3) % 256);
        result[2] = (byte)(sum0 << 2);
        result[3] = (byte)((sum0 | (byte)(salt >> 2)) << 2);
        
        //printByteArray(result);
        //System.out.println();
        return result;
    }
    
    /**
     * Converts a byte array to an integer. This method is used by the
     * hashing algorithm in order to add multiple values together.
     * @param data
     * @return 
     */
    public static int byteArrayToInt(byte[] data) {
         if(data.length != 4)
            return 0;
         int counter = 31;
         int result = 0;
         for(int i = 0; i < data.length; i++) {
             for(int j = 7; j >= 0; j--) {
                 if(isBitOne(data[i], j))
                     result += Math.pow(2, counter);
                 counter--;
             }
         }
         return result;
    }
    
    /**
     * This method determines if a certain bit in a byte is 0 or 1. This method
     * is used by the byteArrayToInt method, which is used in the hash algorithm.
     * @param a
     * @param pos
     * @return 
     */
    public static boolean isBitOne(byte a, int pos) {
        if((int)(a >> pos) % 2 == 0)
            return false;
        return true;
    }
    
    /**
     * This method prints out a byte array. It is used for debugging/testing, and
     * won't be used in the final project.
     * @param b 
     */
    public static void printByteArray(byte[] b) {
        for(int i = 0; i < b.length; i++) 
            System.out.println(Integer.toBinaryString(b[i] & 255 | 256).substring(1));
    }
    
}