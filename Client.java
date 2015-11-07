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
		this.userMaxAttempt = attempts+1;
		this.attempt=1;
	}
	Client(String server,int port,int attempts){
		this(server,port,null,null,attempts);
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
				JOptionPane.showMessageDialog(null,
				"Na cant send file too big." + fileArray.length + ". File has to be less than or equal to 1MB.\nYou have "+(this.getMaxAttemptAllowed()-this.getAttempt()+" attempts remaining."));
				this.fileTransferUnsuccessful();
				if (this.getAttempt() > this.getMaxAttemptAllowed()) {
					JOptionPane.showMessageDialog(null, "You have exceeded the maximum allowed attempts. Your connection with the server will be disconnected.");
					disconnect();
					
				}
				 return;
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
	 * Close all the open streams.
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
}