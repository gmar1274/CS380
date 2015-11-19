package ftp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/*
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener, WindowListener {
	private static final long	serialVersionUID	= 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel				label;
	// to hold the Username and later on the messages
	private JTextField			textField;
	// to hold the server address an the port number
	private JTextField			textFieldServer, textFieldPort;
	// to Logout and get the list of the users
	private JButton				login, logout, uploadFile;
	// for the chat room
	private JTextArea			clientDisplayScreen;
	// if it is for connection
	private boolean				connected;
	// the Client object
	private Client				client;
	// the default port number
	private int					portNumber;
	private String				defaultHost;
	// Constructor connection receiving a socket number
	ClientGUI(String host, int port) {
		super("Client");
		portNumber = port;
		defaultHost = host;
		// The NorthPanel with:
		JPanel northPanel = new JPanel(new GridLayout(3, 1));
		// the server name and the port number
		JPanel serverAndPortPanel = new JPanel(new GridLayout(1, 5, 1, 3));
		// the two JTextField with default value for server address and port number
		textFieldServer = new JTextField(host);
		textFieldPort = new JTextField("" + port);
		textFieldPort.setHorizontalAlignment(SwingConstants.RIGHT);
		serverAndPortPanel.add(new JLabel("Server Address:  "));
		serverAndPortPanel.add(textFieldServer);
		serverAndPortPanel.add(new JLabel("Port Number:  "));
		serverAndPortPanel.add(textFieldPort);
		serverAndPortPanel.add(new JLabel(""));
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPortPanel);
		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		textField = new JTextField("Anonymous");
		northPanel.add(textField);
		add(northPanel, BorderLayout.NORTH);
		// The CenterPanel which is the chat room
		clientDisplayScreen = new JTextArea("Login to the server.", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		centerPanel.add(new JScrollPane(clientDisplayScreen));
		clientDisplayScreen.setEditable(false);
		clientDisplayScreen.setBackground(Color.black);
		clientDisplayScreen.setForeground(Color.green);
		add(centerPanel, BorderLayout.CENTER);
		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false); // you have to login before being able to logout
		uploadFile = new JButton("Upload File");
		uploadFile.addActionListener(this);
		uploadFile.setEnabled(false); // you have to login before being able to Who is in
		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(uploadFile);
		add(southPanel, BorderLayout.SOUTH);
		setSize(550, 500);
		this.setLocationRelativeTo(null);
		this.addWindowListener(this);
		setVisible(true);
		textField.requestFocus();
	}
	// called by the Client to append text in the TextArea
	void displayToClientScreen(String str) {
		clientDisplayScreen.append(str);
	}
	// called by the GUI is the connection failed ie server disconeccted.
	// we reset our buttons, label, textfield
	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		uploadFile.setEnabled(false);
		label.setText("Enter your username below");
		textField.setText("Anonymous");
		// reset port number and host name as a construction time
		textFieldPort.setText("" + portNumber);
		textFieldServer.setText(defaultHost);
		textField.removeActionListener(this);
		connected = false;
	}
	public void actionPerformed(ActionEvent e) {
		Object object = e.getSource();
		if (object == logout) {
			this.setStateOfLabelsTo(true);
			client.sendMessage(new NetworkMessage(NetworkMessage.LOGOUT));// tell server client wants to disconnect
			return;
		}
		if (object == uploadFile) {
			client.uploadFile();
			return;
		}
		// ok it is coming from the JTextField
		if (connected) {
			client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, textField.getText()));
			return;
		}
		if (object == login) {
			// ok it is a connection request
			String username = textField.getText().trim();
			// empty username ignore it
			if (username.length() == 0) return;
			// empty serverAddress ignore it
			String server = textFieldServer.getText().trim();
			if (server.length() == 0) return;
			// empty or invalid port number, ignore it
			String portNumber = textFieldPort.getText().trim();
			if (portNumber.length() == 0) return;
			try {
				this.portNumber = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}
			// try creating a new Client with GUI
			client = new Client(server, this.portNumber, username, this, 3);
			// test if we can start the Client
			if (!client.start()) return;
			setStateOfLabelsTo(false);
			connected = true;
			// disable login button
			login.setEnabled(false);
			// enable the 2 buttons
			logout.setEnabled(true);
			uploadFile.setEnabled(true);
		}
	}
	private void setStateOfLabelsTo(boolean b) {
		this.textField.setVisible(b);
		this.label.setVisible(b);
		textFieldServer.setEditable(b);
		textFieldPort.setEditable(b);
	}
	/*
	 * If the user click the X button to close the application connection with the server will be released and port freed.
	 */
	public void windowClosing(WindowEvent e) {
		if (client != null) {
			try {
				client.disconnect(); // close the connection
			} catch (Exception ee) {
				ee.printStackTrace();
			}
			client = null;
		}
		 FTP.main(null);
	}
	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public static void main(String[] args) {
		ClientGUI c = new ClientGUI("localhost", 23);
	}
}
