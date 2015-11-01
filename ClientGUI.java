import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/*
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener {
	private static final long	serialVersionUID	= 1L;
	// will first hold "Username:", later on "Enter message"
	private JLabel				label;
	// to hold the Username and later on the messages
	private JTextField			textField;
	// to hold the server address an the port number
	private JTextField			textFieldServer, textFieldPort;
	// to Logout and get the list of the users
	private JButton				login, logout, whoIsIn;
	// for the chat room
	private JTextArea			textArea;
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
		// the server name anmd the port number
		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		// the two JTextField with default value for server address and port number
		textFieldServer = new JTextField(host);
		textFieldPort = new JTextField("" + port);
		textFieldPort.setHorizontalAlignment(SwingConstants.RIGHT);
		serverAndPort.add(new JLabel("Server Address:  "));
		serverAndPort.add(textFieldServer);
		serverAndPort.add(new JLabel("Port Number:  "));
		serverAndPort.add(textFieldPort);
		serverAndPort.add(new JLabel(""));
		// adds the Server an port field to the GUI
		northPanel.add(serverAndPort);
		// the Label and the TextField
		label = new JLabel("Enter your username below", SwingConstants.CENTER);
		northPanel.add(label);
		textField = new JTextField("Anonymous");
		northPanel.add(textField);
		add(northPanel, BorderLayout.NORTH);
		// The CenterPanel which is the chat room
		textArea = new JTextArea("Login to the server.", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		centerPanel.add(new JScrollPane(textArea));
		textArea.setEditable(false);
		textArea.setBackground(Color.black);
		textArea.setForeground(Color.green);
		add(centerPanel, BorderLayout.CENTER);
		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false); // you have to login before being able to logout
		whoIsIn = new JButton("Who's connected");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false); // you have to login before being able to Who is in
		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		add(southPanel, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(550, 500);
		this.setLocationRelativeTo(null);
		setVisible(true);
		textField.requestFocus();
	}
	// called by the Client to append text in the TextArea
	void appendToTextArea(String str) {
		textArea.append(str);
	//	textArea.setCaretPosition(textArea.getText().length() - 1);
	}
	// called by the GUI is the connection failed
	// we reset our buttons, label, textfield
	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		label.setText("Enter your username below");
		textField.setText("Anonymous");
		// reset port number and host name as a construction time
		textFieldPort.setText("" + portNumber);
		textFieldServer.setText(defaultHost);
		textFieldServer.setEditable(false);
		textFieldPort.setEditable(false);
		textField.removeActionListener(this);
		connected = false;
	}
	/*
	 * Button or JTextField clicked
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == logout) {
			client.sendMessage(new NetworkMessage(NetworkMessage.LOGOUT, ""));
			return;
		}
		// if it the who is in button
		if (o == whoIsIn) {
			client.sendMessage(new NetworkMessage(NetworkMessage.WHOISIN, ""));
			return;
		}
		// ok it is coming from the JTextField
		if (connected) {
			client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, textField.getText()));
			textField.setText("");
			return;
		}
		if (o == login) {
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
			client = new Client(server, this.portNumber, username, this);
			// test if we can start the Client
			if (!client.start()) return;
			textField.setText("");
			label.setText("Submit message by pressing enter.");
			connected = true;
			// disable login button
			login.setEnabled(false);
			// enable the 2 buttons
			logout.setEnabled(true);
			whoIsIn.setEnabled(true);
			// disable the Server and Port JTextField
			textFieldServer.setEditable(false);
			textFieldPort.setEditable(false);
			// Action listener for when the user enter a message
			textField.addActionListener(this);
		}
	}
	// to start the whole thing the server
	public static void main(String[] args) {
		new ClientGUI("localhost", 23);
	}
}
