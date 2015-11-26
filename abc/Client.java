package abc;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.Charset;
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
	private ObjectInputStream	readFromServerInputStream;	// to read from the socket
	private ObjectOutputStream	sendToServerOutputStream;	// to write on the socket
	private Socket				socket;
	// private ClientGUI clientGUI;
	private String				server, username, password;
	private int					port;
	private int					attempt, userMaxAttempt;
	private FTP_Frame			frame;
	Client(String server, String user, String pwrd, int port, int attempts, FTP_Frame f) {
		this.frame = f;
		// Client(String server, int port, String username, ClientGUI cg, int attempts) {
		this.server = server;
		this.port = port;
		this.username = user;
		this.password = pwrd;
		// this.clientGUI = cg;// can be null
		this.userMaxAttempt = attempts + 1;
		this.attempt = 1;
	}
	public Socket getSocket() {
		return this.socket;
	}
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} catch (Exception e) {
			frame.displayToClientScreen("Cannot establish a connection to server: " + server + " on port: " + port + "\n" + e);
			return false;
		}
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		frame.displayToClientScreen(msg);
		/* Creating both Data Stream */
		try {
			readFromServerInputStream = new ObjectInputStream(socket.getInputStream());
			sendToServerOutputStream = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			frame.displayToClientScreen("Error. Data stream could not be establish.\n" + e);
			return false;
		}
		// creates the Thread to listen from the server
		new ListenFromServer().start();
		try {
			sendToServerOutputStream.writeObject(username);
		} catch (IOException er) {
			frame.displayToClientScreen("Exception doing login : " + er);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
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
			sendFile(file, FTP.keyFile); // send file
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
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
			frame.displayToClientScreen("Exception writing to server: " + e);
		}
	}
	/*
	 * Close all the open streams.
	 */
	public void disconnect() {
		try {
			sendToServerOutputStream.writeObject(new NetworkMessage(NetworkMessage.LOGOUT));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if (readFromServerInputStream != null) readFromServerInputStream.close();
		} catch (Exception e) {}
		try {
			if (sendToServerOutputStream != null) sendToServerOutputStream.close();
		} catch (Exception e) {}
		try {
			if (socket != null) socket.close();
		} catch (Exception e) {}
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
					String msg = (String) readFromServerInputStream.readObject();
					frame.displayToClientScreen(msg);
				} catch (IOException e) {
					frame.displayToClientScreen("Connection with the server has been terminated.");
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
	public String verifyUsernameAndPassword(String username, String salt, String password) throws Exception {
		byte[] hash = FTP.hash(password.getBytes("UTF-8"), salt.getBytes("UTF-8"),!frame.getControlledFail().isSelected());
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
				byte[] hash = FTP.hash(send, key, !frame.getControlledFail().isSelected());
				// The hash is put in the last remaining 4 bytes of the byte array to be sent.
				for ( int j = 0 ; j < hash.length ; j++)
					send[((file.length - i) < 1024 ? (file.length - i) : 1024) + j] = hash[j];
				// The byte array is encrypted.
				byte[] encrypt = FTP.encryptDecrypt(send, key);
				String encode = FTP.encodeBase64(encrypt);
				byte[] finalArray = encode.getBytes("UTF-8");
				byteList.add(finalArray);
				sendToServerOutputStream.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, finalArray));
				// System.out.println("CLIENT BYTE: " + Arrays.toString(finalArray));
				/*
				 * Send the resulting byte array to the reciever. This byte array represents the encrypted chunk and hash, and decoded in base 64. Have the
				 * receiver recieve the chunks and store all the byte array results in a single byte array and send to the "recieveFinishedDataPiece" method.
				 */
			}
			sendToServerOutputStream.writeObject(new NetworkMessage(NetworkMessage.LASTPACKETSENT, 
             FTP.encryptDecrypt(inputFile.getName().getBytes(Charset.forName("UTF-8")), 
                                key)));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("File or key don't exist.");
		}
	}
	public boolean authenticate() {
		// send through socket to server if username and password is correct
		return true;
	}
}