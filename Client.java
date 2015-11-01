import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {
	private ObjectInputStream	sInput;	// to read from the socket
	private ObjectOutputStream	sOutput;	// to write on the socket
	private Socket				socket;
	private ClientGUI			clientGUI;
	private String				server, username;
	private int					port;
	/*
	 * Constructor called by console mode server: the server address port: the port number username: the username
	 */
	Client(String server, int port, String username) {
		// which calls the GUI set to null since its in console mode.
		this(server, port, username, null);
	}
	/*
	 * Constructor call when used from a GUI in console mode the ClienGUI parameter is null
	 */
	Client(String server, int port, String username, ClientGUI cg) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.clientGUI = cg;// can be null
		// this.startServer();
	}
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} catch (Exception e) {
			display("Cannot establish a connection to server: " + server + " on port: " + port + "\n" + e);
			return false;
		}
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
		/* Creating both Data Stream */
		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			display("Error. Data stream could not be establish.\n" + e);
			return false;
		}
		// creates the Thread to listen from the server
		new ListenFromServer().start();
		try {
			sOutput.writeObject(username);
		} catch (IOException er) {
			display("Exception doing login : " + er);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}
	/*
	 * To send a message to the console or the GUI
	 */
	private void display(String msg) {
		if (clientGUI == null) System.out.println(msg); // println in console mode
		else clientGUI.appendToTextArea("\n" + msg); // append to the ClientGUI display field.
	}
	/*
	 * To send a message to the server
	 */
	void sendMessage(NetworkMessage msg) {
		try {
			sOutput.writeObject(msg);
		} catch (IOException e) {
			display("Exception writing to server: " + e);
		}
	}
	/*
	 * When something goes wrong Close the Input/Output streams and disconnect not much to do in the catch clause
	 */
	private void disconnect() {
		try {
			if (sInput != null) sInput.close();
		} catch (Exception e) {}
		try {
			if (sOutput != null) sOutput.close();
		} catch (Exception e) {}
		try {
			if (socket != null) socket.close();
		} catch (Exception e) {}
		if (clientGUI != null) clientGUI.connectionFailed();
	}
	/*
	 * To start the Client in console mode use one of the following command > java Client > java Client username > java Client username portNumber > java Client
	 * username portNumber serverAddress at the console prompt If the portNumber is not specified 1500 is used If the serverAddress is not specified "localHost"
	 * is used If the username is not specified "Anonymous" is used > java Client is equivalent to > java Client Anonymous 1500 localhost are eqquivalent In
	 * console mode, if an error occurs the program simply stops when a GUI id used, the GUI is informed of the disconnection
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// default values
				int portNumber = 1500;
				String serverAddress = "localhost";
				String userName = "Anonymous";
				switch (args.length) {
					case 3:
						serverAddress = args[2];
					case 2:
						try {
							portNumber = Integer.parseInt(args[1]);
						} catch (Exception e) {
							System.out.println("Invalid port number.");
							System.out.println("Usage is: > java Client username portNumber serverAddress");
							return;
						}
					case 1:
						userName = args[0];
					case 0:
						break;
					default:
						System.out.println("Usage is: > java Client username portNumber {serverAddress}");
						return;
				}
				Client client = new Client(serverAddress, portNumber, userName);
				// test if we can start the connection to the Server
				if (!client.start()) return;
				// wait for messages from user
				Scanner scan = new Scanner(System.in);
				System.exit(0);
				while (true) {
					// read message from user
					String msg = scan.nextLine();
					// logout if message is LOGOUT
					if (msg.equalsIgnoreCase("LOGOUT")) {
						client.sendMessage(new NetworkMessage(NetworkMessage.LOGOUT, ""));
						// break to do the disconnect
						break;
					}
					// message WhoIsIn
					else if (msg.equalsIgnoreCase("WHOISIN")) {
						client.sendMessage(new NetworkMessage(NetworkMessage.WHOISIN, ""));
					} else { // default to ordinary message
						client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, msg));
					}
				}
				// done disconnect
				client.disconnect();
			}
		});
	}
	// private void startServer() {
	// Thread t = new Thread() {
	// public void start() {
	// Server serv = new Server(port);
	// serv.start();
	// System.out.println("here at port "+port);
	// }
	// };
	// t.start();
	// }
	/*
	 * a class that waits for the message from the server and append them to the JTextArea if we have a GUI or simply System.out.println() it in console mode
	 */
	class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					String msg = (String) sInput.readObject();
					// if console mode print the message and add back the prompt
					if (clientGUI == null) {
						System.out.println(msg);
						System.out.print("> ");
					} else {
						clientGUI.appendToTextArea("\n> " + msg);
					}
				} catch (IOException e) {
					display("\n> Connection with the server has been terminated.");
					if (clientGUI != null) clientGUI.connectionFailed();
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}
}