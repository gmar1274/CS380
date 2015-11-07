import java.net.*;
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
	private ObjectOutputStream	sOutput;	// to write on the socket
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
		this.userMaxAttempt = attempts;
		this.attempt=1;
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
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			displayToClientScreen("Error. Data stream could not be establish.\n" + e);
			return false;
		}
		// creates the Thread to listen from the server
		new ListenFromServer().start();
		try {
			sOutput.writeObject(username);
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
		else clientGUI.appendToTextArea("\n" + msg); // append to the ClientGUI display field.
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
			if (fileArray.length > Math.pow(2, 10)) {
				JOptionPane.showMessageDialog(null,
				"Na cant send file too big." + fileArray.length + ". File has to be less than or equal to " + Math.pow(2, 10)+"\nYou have "+(this.getMaxAttemptAllowed()-this.getAttempt()+" remaining.");
				this.fileTransferUnsuccessful();
				if (this.getAttempt() > this.getMaxAttemptAllowed()) disconnect();
				else return;
			}
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			bis.read(fileArray, 0, fileArray.length);
			this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, fileArray));
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
			sOutput.writeObject(msg);
		} catch (IOException e) {
			displayToClientScreen("Exception writing to server: " + e);
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
	// public static void main(final String[] args) {
	// SwingUtilities.invokeLater(new Runnable() {
	// public void run() {
	// // default values
	// int portNumber = 1500;
	// String serverAddress = "localhost";
	// String userName = "Anonymous";
	// switch (args.length) {
	// case 3:
	// serverAddress = args[2];
	// case 2:
	// try {
	// portNumber = Integer.parseInt(args[1]);
	// } catch (Exception e) {
	// System.out.println("Invalid port number.");
	// System.out.println("Usage is: > java Client username portNumber serverAddress");
	// return;
	// }
	// case 1:
	// userName = args[0];
	// case 0:
	// break;
	// default:
	// System.out.println("Usage is: > java Client username portNumber {serverAddress}");
	// return;
	// }
	// Client client = new Client(serverAddress, portNumber, userName);
	// // test if we can start the connection to the Server
	// if (!client.start()) return;
	// // wait for messages from user
	// Scanner scan = new Scanner(System.in);
	// while (true) {
	// // read message from user
	// String msg = scan.nextLine();
	// // logout if message is LOGOUT
	// if (msg.equalsIgnoreCase("LOGOUT")) {
	// client.sendMessage(new NetworkMessage(NetworkMessage.LOGOUT, ""));
	// // break to do the disconnect
	// break;
	// }
	// // message WhoIsIn
	// else if (msg.equalsIgnoreCase("WHOISIN")) {
	// client.sendMessage(new NetworkMessage(NetworkMessage.UPLOADFILE, ""));
	// } else { // default to ordinary message
	// client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, msg));
	// }
	// }
	// // done disconnect
	// client.disconnect();
	// }
	// });
	// }
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
						clientGUI.appendToTextArea("\n> " + msg);
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
}