import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	private ArrayList<byte[]>		byteList;
	// a unique ID for each connection
	private static int				uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread>	clientThreadArrayList;
	private ServerGUI				serverGUI;
	// to display time
	private SimpleDateFormat		sdateFormat;
	// the port number to listen for connection
	private int						port;
	private boolean					keepGoing;
	private ServerSocket			serverSocket;
	/*
	 * server constructor that receive the port to listen to for connection as parameter in console
	 */
	public Server(int port) {
		this(port, null);
	}
	public Server(int port, ServerGUI sg) {
		byteList = new ArrayList<byte[]>();
		// GUI or not
		this.serverGUI = sg;
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdateFormat = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		clientThreadArrayList = new ArrayList<ClientThread>();
	}
	/**
	 * MAIN LOOP CONNECTION FOR THE SERVER.
	 **/
	public void startServer() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			serverSocket = new ServerSocket(port);
			// infinite loop to wait for connections
			// while (keepGoing) {
			while (true) {
				Socket socket = serverSocket.accept(); // accept connection
				if (!keepGoing) break;
				ClientThread client = new ClientThread(socket); // make a thread of it
				clientThreadArrayList.add(client); // save it in the ArrayList
				client.start();
			}
			// I was asked to stop
			try {
				disconnectClients(serverSocket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {}
	}
	public void disconnectClients() {
		this.disconnectClients(serverSocket);
	}
	private void disconnectClients(ServerSocket serverSocket) {
		try {
			this.keepGoing = false;
			for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
				ClientThread ct = clientThreadArrayList.get(i);
				try {
					serverGUI.displayServerScreen("> Server disconnceted with " + ct.username + ".");
					ct.closeStreams();
				} catch (Exception e) {
					serverGUI.displayToEventLog("Exception closing the server and clients: " + e);
				}
			}
			serverSocket.close();
			serverGUI.displayServerScreen("> Sever stopped listening.");
		} catch (Exception ee) {
			// nothing
		}
	}
	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	public void displayToServerLog(String msg) {
		String time = sdateFormat.format(new Date()) + " " + msg;
		if (serverGUI == null) System.out.println(time);
		else serverGUI.displayToEventLog(time);
	}
	/*
	 * to broadcast a message to all Clients
	 */
	// private synchronized void broadcast(String message) {
	// // // add HH:mm:ss to the message
	// String time = sdateFormat.format(new Date());
	// String messageLf = time + " " + message;
	// // display message on console or GUI
	// if (serverGUI == null) System.out.println(messageLf);
	// else serverGUI.displayServerScreen(messageLf); // append in the room window
	// // we loop in reverse order in case we would have to remove a Client
	// // because it has disconnected
	// for ( int i = clientThreadArrayList.size() - 1 ; i >= 0 ; --i) {
	// ClientThread ct = clientThreadArrayList.get(i);
	// // try to write to the Client if it fails remove it from the list
	// if (!ct.writeToClientScreen(messageLf)) {
	// clientThreadArrayList.remove(i);
	// displayToServerLog("\nDisconnected Client " + ct.username + " removed from list.");
	// }
	// }
	// }
	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
			ClientThread ct = clientThreadArrayList.get(i);
			// found it
			if (ct.id == id) {
				clientThreadArrayList.remove(i);
				return;
			}
		}
	}
	/** One instance of this thread will run for each client */
	public class ClientThread extends Thread {
		// the socket where to listen/talk
		private Socket				socket;
		private ObjectInputStream	readFromClientStream;
		private ObjectOutputStream	writeToClientStream;
		private int					id;
		private String				username;
		private NetworkMessage		networkMessage;
		// the date it connected
		private String				date;
		// Constructor
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			// Creating Data Streams
			try {
				// create output first
				writeToClientStream = new ObjectOutputStream(socket.getOutputStream());
				readFromClientStream = new ObjectInputStream(socket.getInputStream());
				username = (String) readFromClientStream.readObject();
				if (serverGUI != null) serverGUI.displayServerScreen("> " + username + " just connected.");
				else System.out.println("> " + username + " just connected.");
				writeToClientScreen("Server says: hello " + username + "!!!");
			} catch (IOException e) {
				serverGUI.displayToEventLog("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			date = new Date().toString();
		}
		public void run() {
			listenForClients();
		}
		/**
		 * THE MAIN LISTENING LOOP
		 */
		public void listenForClients() {// to loop until LOGOUT
			while (true) {
				try {
					networkMessage = (NetworkMessage) readFromClientStream.readObject();
				} catch (IOException e) {
					if (this.socket.isClosed()) {
						displayToServerLog(username + " connection has been terminated. " + e);
					} else displayToServerLog(username + " File transfer was unsuccesful: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}
				switch (networkMessage.getType()) {
				// case NetworkMessage.MESSAGE:
				// broadcast(username + ": " + networkMessage.getMessage());
				// break;
					case NetworkMessage.LOGOUT:
						displayToServerLog(username + " disconnected.");
						this.closeStreams();
						return;
					case NetworkMessage.UPLOADFILE:
						byte[] fileByteArray = networkMessage.getByteArray();
						byteList.add(recieveFinishedDataPiece(fileByteArray, FTP.KEYFILE));
						if (serverGUI != null) serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fileByteArray));
						else System.out.println("> File byte array: " + Arrays.toString(fileByteArray));
						
						///Assume it finished 
						break;
					case NetworkMessage.LASTPACKETSENT:
						byte[] fByteArray = networkMessage.getByteArray();
						if (serverGUI != null) serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fByteArray));
						else {
							System.out.println("> File byte array: " + Arrays.toString(fByteArray));
							System.out.println("> File has been succesfully recieved.");
						}
						writeToClientScreen(sdateFormat.format(new Date()) + " File successfully transfered.");
						break;
				}
			}
			closeStreams();
		}
		// try to close everything
		private void closeStreams() {
			try {
				if (writeToClientStream != null) writeToClientStream.close();
			} catch (Exception e) {}
			try {
				if (readFromClientStream != null) readFromClientStream.close();
			} catch (Exception e) {};
			try {
				if (socket != null) socket.close();
			} catch (Exception e) {
				serverGUI.displayToEventLog("Connection to sever closed.");
			}
		}
		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeToClientScreen(String msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				closeStreams();
				return false;
			}
			// write the message to the stream
			try {
				writeToClientStream.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				displayToServerLog("Error sending message to " + username);
				displayToServerLog(e.toString());
			}
			return true;
		}
	}
	// //////////////////
	/**
	 * This method should be run every time a chunk is received by the receiver. The chunk (byte[] data) is converted to the original byte array that
	 * represented the data that was originally passed in the "sentFile" method. The returned byte arrays from this method should all be added to a byte list,
	 * which is then converted to a File object, and this File object should be identical to original sent File. If a byte array is received by this method and
	 * the hash that it is sent with is not equal to the hashed "finishedData" byte array, then there was an error with the data transmitted, and the method
	 * returns null. If at any time this method returns null, send an error to the sender that the file was not properly received.
	 * 
	 * @param data
	 *            The file chunk that was sent.
	 * @param key
	 *            The file that represents the key.
	 * @return The decrypted, decoded data chunk (1024 bytes in length); null if the hashes didn't match.
	 * @throws Exception
	 *             If there was an error getting a String from the base 64 encoded byte array, or if the key file cannot be converted to a byte array.
	 */
	private byte[] recieveFinishedDataPiece(byte[] data, File key) {
		// Converts the sent byte array to a string, which is decoded and turned into
		// a new byte array.
		byte[] decode;
		byte[] finishedData = null;
		try {
			decode = FTP.decodeBase64(new String(data, "UTF-8"));
			// The byte array is decrypted.
			byte[] decrypt = FTP.encryptDecrypt(decode, FTP.fileToByteArray(key));
			// This is the byte array that will store the originally sent data in the
			// chunk.
			finishedData = new byte[data.length - 4];
			// This stores the hash that came with the chunk.
			byte[] hash = new byte[4];
			// Fills both of these arrays with their proper values.
			for ( int i = 0 ; i < decrypt.length ; i++)
				finishedData[i] = decrypt[i];
			for ( int i = 0 ; i < hash.length ; i++)
				hash[i] = decrypt[(data.length - 4) + i];
			// If the hash included with the chunk does not equal the hashed version
			// of the finished data, then the file was sent incorrectly, and null
			// is returned.
			if (!FTP.compareByteArrays(FTP.hash(finishedData, FTP.fileToByteArray(key)), hash)) return null;
			// The data originally put in the chunk is returned.
		} catch (Exception e) {
			e.printStackTrace();
		}
		return finishedData;
	}
}