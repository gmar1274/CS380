import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	// a unique ID for each connection
	private static int			uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<CThread>	clientThreadArrayList;
	private ServerGUI			serverGUI;
	// to display time
	private SimpleDateFormat	sdateFormat;
	// the port number to listen for connection
	private int					port;
	// the boolean that will be turned of to stop the server
	private boolean				keepGoing;
	/*
	 * server constructor that receive the port to listen to for connection as parameter in console
	 */
	public Server(int port) {
		this(port, null);
	}
	public Server(int port, ServerGUI sg) {
		// GUI or not
		this.serverGUI = sg;
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdateFormat = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		clientThreadArrayList = new ArrayList<CThread>();
	}
	public void startInBackground() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
			// infinite loop to wait for connections
			while (keepGoing) {
				Socket socket = serverSocket.accept(); // accept connection
				if (!keepGoing) break;
				CThread t = new CThread(socket); // make a thread of it
				clientThreadArrayList.add(t); // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
					CThread tc = clientThreadArrayList.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {e.printStackTrace();}
		}
		// something went bad
		catch (IOException e) {
			String msg = sdateFormat.format(new Date()) + " Exception on new ServerSocket: " + e;
			display(msg);
		}
	}
	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
			// infinite loop to wait for connections
			while (keepGoing) {
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");
				Socket socket = serverSocket.accept(); // accept connection
				// if I was asked to stop
				if (!keepGoing) break;
				CThread t = new CThread(socket); // make a thread of it
				clientThreadArrayList.add(t); // save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
					CThread tc = clientThreadArrayList.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
			String msg = sdateFormat.format(new Date()) + " Exception on new ServerSocket: " + e;
			display(msg);
		}
	}
	/*
	 * For the GUI to stop the server
	 */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	private void display(String msg) {
		String time = sdateFormat.format(new Date()) + " " + msg;
		if (serverGUI == null) System.out.println(time);
		else serverGUI.appendEvent(time);
	}
	/*
	 * to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss to the message
		String time = sdateFormat.format(new Date());
		String messageLf = time + " " + message;
		// display message on console or GUI
		if (serverGUI == null) System.out.println(messageLf);
		else serverGUI.setServerTextField(messageLf); // append in the room window
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for ( int i = clientThreadArrayList.size() - 1 ; i >= 0 ; --i) {
			CThread ct = clientThreadArrayList.get(i);
			// try to write to the Client if it fails remove it from the list
			if (!ct.writeMsg(messageLf)) {
				clientThreadArrayList.remove(i);
				display("\nDisconnected Client " + ct.username + " removed from list.");
			}
		}
	}
	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
			CThread ct = clientThreadArrayList.get(i);
			// found it
			if (ct.id == id) {
				clientThreadArrayList.remove(i);
				return;
			}
		}
	}
	/*
	 * To run as a console application just open a console window and: &gt; java Server &gt; java Server portNumber If the port number is not specified 1500 is
	 * used
	 */
	public static void main(String[] args) {
		// start server on port 23 unless a PortNumber is specified
		int portNumber = 23;
		switch (args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				} catch (Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: java Server portNumber");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: java Server portNumber");
				return;
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}
	/** One instance of this thread will run for each client */
	public class CThread extends Thread {
		// the socket where to listen/talk
		Socket				socket;
		ObjectInputStream	sInput;
		ObjectOutputStream	sOutput;
		int					id;
		String				username;
		NetworkMessage		cm;
		// the date it connected
		String				date;
		// Constructore
		CThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			// Creating Data Streams
			try {
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());
				username = (String) sInput.readObject();
				display(username + " just connected.");
				writeMsg("Server says hello " + username + "!!!");
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			date = new Date().toString();
		}
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while (keepGoing) {
				// read a String (which is an object)
				try {
					cm = (NetworkMessage) sInput.readObject();
				} catch (IOException e) {
					display(username + "Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}
				String message = cm.getMessage();
				// Switch on the type of message receive
				switch (cm.getType()) {
					case NetworkMessage.MESSAGE:
						broadcast(username + ": " + message);
						break;
					case NetworkMessage.LOGOUT:
						display(username + " disconnected.");
						keepGoing = false;
						break;
					case NetworkMessage.WHOISIN:
						writeMsg("List of the users connected at " + sdateFormat.format(new Date()));
						// scan al the users connected
						for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
							CThread ct = clientThreadArrayList.get(i);
							writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
						}
						break;
				}
			}
			remove(id);
			close();
		}
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if (sOutput != null) sOutput.close();
			} catch (Exception e) {}
			try {
				if (sInput != null) sInput.close();
			} catch (Exception e) {};
			try {
				if (socket != null) socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if (!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}
