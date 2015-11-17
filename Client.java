import java.lang.reflect.Array;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {
	private ObjectInputStream	sInput;	// to read from the socket
	private ObjectOutputStream	sendToServerOutputStream;	// to write on the socket
	private Socket				socket;
	private ClientGUI			clientGUI;
	private String				server, username;
	private int					port;
	private int					attempt, userMaxAttempt;
	/*
	 * Constructor called by console mode server: the server address port: the port number username: the username
	 */
	Client(String server, int port, String username, int attempts) {
		// which calls the GUI set to null since its in console mode.
		this(server, port, username, null, attempts);
	}
	/*
	 * Constructor call when used from a GUI in console mode the ClienGUI parameter is null
	 */
	Client(String server, int port, String username, ClientGUI cg, int attempts) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.clientGUI = cg;// can be null
		this.userMaxAttempt = attempts + 1;
		this.attempt = 1;
	}
	Client(String server, int port, int attempts) {
		this(server, port, null, null, attempts);
	}
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} catch (Exception e) {
			displayToClientScreen("Cannot establish a connection to server: " + server + " on port: " + port + "\n" + e);
			return false;
		}
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		displayToClientScreen(msg);
		/* Creating both Data Stream */
		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sendToServerOutputStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			displayToClientScreen("Error. Data stream could not be establish.\n" + e);
			return false;
		}
		// creates the Thread to listen from the server
		new ListenFromServer().start();
		try {
			sendToServerOutputStream.writeObject(username);
		} catch (IOException er) {
			displayToClientScreen("Exception doing login : " + er);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}
	/*
	 * To send a message to the console or the GUI
	 */
	private void displayToClientScreen(String msg) {
		if (clientGUI == null) System.out.println(msg); // println in console mode
		else clientGUI.displayToClientScreen("\n" + msg); // append to the ClientGUI display field.
	}
	public void uploadFile() {
		try {
			String fileName = null;
			JFileChooser fc = new JFileChooser();
			if (fc.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) fileName = fc.getSelectedFile().getAbsolutePath();
			else return;
			File file = new File(fileName);
			// send file
			byte[] fileArray = new byte[(int) file.length()];
			if (fileArray.length > Math.pow(2, 20)) {
				JOptionPane.showMessageDialog(null, "Na cant send file too big." + fileArray.length + ". File has to be less than or equal to 1MB.\nYou have "
				+ (this.getMaxAttemptAllowed() - this.getAttempt() + " attempts remaining."));
				this.fileTransferUnsuccessful();
				if (this.getAttempt() > this.getMaxAttemptAllowed()) {
					JOptionPane
					.showMessageDialog(null, "You have exceeded the maximum allowed attempts. Your connection with the server will be disconnected.");
					disconnect();
				}
				return;
			}
			sendFile(file, FTP.KEYFILE); // send file
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// private void sendBytes(File file, byte[] fileArray) {
	// try {
	// BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	// // send 1KB at a time
	// bis.read(fileArray, 0, fileArray.length);
	// int remaining = 0;
	// System.out.println("size: "+fileArray.length);
	// for ( int i = 0 ; i < fileArray.length ; i += Math.pow(2, 10)) {
	// System.out.println("i:: "+i);
	// byte[] a = new byte[(int) Math.pow(2, 10)];
	//
	// a = Arrays.copyOfRange(fileArray, i, i + (int) Math.pow(2, 10));
	// if (a.length <= 0) break; // this will happen if the bounds i to 2^20 exceeds fileArray's bounds
	// else this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, a));
	// remaining = i;
	// }
	// // send remaining bytes
	// byte[] r = new byte[(int) Math.pow(2, 10)];
	// for ( int i = fileArray.length - remaining ; i < r.length ; ++i) {
	// if (i > fileArray.length) r[i] = 0;// padding
	// else r[i] = fileArray[i];// send remaining bytes
	// }
	// this.sOutput.writeObject(new NetworkMessage(NetworkMessage.LASTPACKETSENT, r));
	// } catch (Exception e) {// just sending bytes
	// this.displayToClientScreen("> Unsuccessful file transfer.");
	// this.disconnect();
	// }
	// }
	public int getMaxAttemptAllowed() {
		return this.userMaxAttempt;
	}
	/*
	 * To send a message to the server
	 */
	void sendMessage(NetworkMessage msg) {
		try {
			sendToServerOutputStream.writeObject(msg);
		} catch (IOException e) {
			displayToClientScreen("Exception writing to server: " + e);
		}
	}
	/*
	 * Close all the open streams.
	 */
	public void disconnect() {
		try {
			if (sInput != null) sInput.close();
		} catch (Exception e) {}
		try {
			if (sendToServerOutputStream != null) sendToServerOutputStream.close();
		} catch (Exception e) {}
		try {
			if (socket != null) socket.close();
		} catch (Exception e) {}
		if (clientGUI != null) clientGUI.connectionFailed();
	}
	public void fileTransferUnsuccessful() {
		++this.attempt;
	}
	public int getAttempt() {
		return this.attempt;
	}
	/****
	 ** MAIN LOOP THAT SERVER LISTENS TO CLIENT
	 ****/
	private class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					String msg = (String) sInput.readObject();
					if (clientGUI == null) {
						System.out.println(msg);
						System.out.print("> ");
					} else {
						clientGUI.displayToClientScreen("\n> " + msg);
					}
				} catch (IOException e) {
					displayToClientScreen("\n> Connection with the server has been terminated.");
					if (clientGUI != null) clientGUI.connectionFailed();
					else disconnect();
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}
	// /////////////////////////////////////////////////////
	// * This static String array stores our usernames, salts, and hashed passwords in the format: <username>:<salt>:<hashed password> The username portion of
	// the
	// * project is incomplete at the moment.
	// */
	/**
	 * @param args
	 *            the command line arguments
	 */
	// public static void main(String[] args) throws IOException {
	// // The program contains hashed passwords from each member that are hard-coded
	// // Before sending, the username/password must be correct
	// // (stored as "<username>:<salt>:<hashed password>" in the code)
	// // This string is sent to the reciever for verification (in the form of: <username>:<hashed password>)
	// // Using the salt given
	// // If the username/password is not correct, the user is prompted that it is invalid
	// // Otherwise, the file can be sent
	// // Break up data (File object) into chunks (1024 length byte array)
	// // Each chunk is hashed, and the hash is merged with the chunk (now 1028 bytes in length).
	// // The chunks are then encrypted (given a key via File object from the professor)
	// // The encrypted chunks are encoded in BASE64
	// // The chunks are sent to reciver one by one.
	// // The receiver decodes each chunk from BASE64
	// // The chunks are decrypted
	// // The first 1024 bytes of the chunk is hashed, and compared with the associated hash the chunk was sent with
	// // If the sent hash does not match the hashed chunk, an error has occured,
	// // the reciver tells the sender to resend.
	// // If all the chunks are properly received, the chunks are put together into a single byte array (without the hashes).
	// // This byte array is turned into a File object.
	// File inputFile = new File("test.txt");
	// File keyFile = new File("key.txt");
	// sendFile(inputFile, keyFile);
	// }
	public String verifyUsernameAndPassword(String username, String salt, String password) throws Exception {
		byte[] hash = FTP.hash(password.getBytes("UTF-8"), salt.getBytes("UTF-8"));
		String hashPassword = FTP.encodeBase64(hash);
		String result = username + ":" + salt + ":" + hashPassword;
		// Send result to the receiver, the receiver checks the USERNAMES array to
		// see if the result matches with any of its contents. If so, the user is
		// verified.
		return result;
	}
	/**
	 * This method sends a given file to the receiver with a given key used to encrypt it. The file is broken up into 1024 byte chunks, and each chunk is
	 * hashed. Each chunk and hash are merged together, encrypted, encoded in Base 64, and sent to the receiver chunk by chunk.
	 * 
	 * @param inputFile
	 * @param keyFile
	 */
	public void sendFile(File inputFile, File keyFile) {
		byte[] file, key;
		ArrayList<byte[]> byteList = new ArrayList<byte[]>();
		try {
			// Converts both the inputFile and keyFile into byte arrays.
			file = FTP.fileToByteArray(inputFile);
			key = FTP.fileToByteArray(keyFile);
			// For every kilobyte in the file, a chunk is made, hashed, and these
			// are both merged into a byte array of length 1028, encrypted, encoded
			// in Base 64, and sent.
			for ( int i = 0 ; i < file.length ; i += 1024) {
				// Creates a kilobyte byte array for the current file chunk. If
				// this is the last iteration and this file chunk is less than a
				// kilobyte in length (the remander of the file), the byte array
				// length is adjusted accordingly.
				byte[] send = new byte[(file.length - i) < 1024 ? (file.length - i) + 4 : 1028];
				// The new byte array is filled with the chunk's contents.
				for ( int j = 0 ; j < send.length - 4 ; j++)
					send[j] = file[i + j];
				// The hash is generated (4 bytes in length).
				byte[] hash = FTP.hash(send, key);
				// The hash is put in the last remaining 4 bytes of the byte array to be sent.
				for ( int j = 0 ; j < hash.length ; j++)
					send[((file.length - i) < 1024 ? (file.length - i) : 1024) + j] = hash[j];
				// The byte array is encrypted.
				byte[] encrypt = FTP.encryptDecrypt(send, key);
				String encode = FTP.encodeBase64(encrypt);
				byte[] finalArray = encode.getBytes("UTF-8");
				byteList.add(finalArray);
				sendToServerOutputStream.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, finalArray));
				System.out.println("CLIENT BYTE: "+Arrays.toString(finalArray));
				/*
				 * Send the resulting byte array to the reciever. This byte array represents the encrypted chunk and hash, and decoded in base 64. Have the
				 * receiver recieve the chunks and store all the byte array results in a single byte array and send to the "recieveFinishedDataPiece" method.
				 */
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("File or key don't exist.");
		}
	}
	
}