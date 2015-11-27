import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.charset.Charset;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/*
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener, WindowListener {
	private static final long serialVersionUID = 1L;
	private JLabel hashLabel, asciiLabel;
	private JTextField textField, hashField;
        private JPasswordField passwordField;
	private JTextField textFieldServer, textFieldPort;
	private JButton	login, logout, uploadFile, uploadKey;
	private JTextArea clientDisplayScreen;
	// if it is for connection
	private boolean	connected, keySelected = false;
	// the Client object
	private Client client;
	// the default port number
	private int portNumber;
	private String defaultHost;
        private JCheckBox hash, ascii;
        
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
                
                //label = new JLabel("Enter your username below", SwingConstants.CENTER);
		//northPanel.add(label);
		textField = new JTextField("Username");
		northPanel.add(textField);
		add(northPanel, BorderLayout.NORTH);
                
		//passLabel = new JLabel("Enter your password below", SwingConstants.CENTER);
		//northPanel.add(passLabel);
		passwordField = new JPasswordField("Password");
		northPanel.add(passwordField);
		add(northPanel, BorderLayout.NORTH);
                
                //northPanel.add(passwordField);
                //add(northPanel, BorderLayout.NORTH);
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
		uploadKey = new JButton("Select Key");
		uploadKey.addActionListener(this);
                hash = new JCheckBox();
		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(uploadFile);
                southPanel.add(uploadKey);
		//add(southPanel, BorderLayout.SOUTH);
                
                hashField = new JTextField("0  ");
                hashField.setEnabled(false);
                hashLabel = new JLabel("Failed hash");
                hash = new JCheckBox();
                hash.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        hashField.setEnabled(!hashField.isEnabled());
                    }
                });
                
                asciiLabel = new JLabel("ASCII armor");
                ascii = new JCheckBox();
                ascii.setSelected(true);
                
                JPanel southPanel2 = new JPanel();
                southPanel.add(hashLabel);
                southPanel.add(hash);
                southPanel.add(hashField);
                
                southPanel.add(asciiLabel);
                southPanel.add(ascii);
                add(southPanel, BorderLayout.SOUTH);
                
		setSize(650, 500);
		this.setLocationRelativeTo(null);
		this.addWindowListener(this);
		setVisible(true);
		//textField.requestFocus();
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
		//label.setText("Enter your username below");
		textField.setText("Username");
                passwordField.setText("Password");
		// reset port number and host name as a construction time
		textFieldPort.setText("" + portNumber);
		textFieldServer.setText(defaultHost);
		textField.removeActionListener(this);
                passwordField.removeActionListener(this);
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
			try {
                            client.uploadFile((hash.isSelected() ? Integer.parseInt(hashField.getText().trim()) : 0), ascii.isSelected(), true);
                        } catch (Exception ee) {
                            client.uploadFile(0, ascii.isSelected(), true);
                        }
			return;
		}
                if (object == uploadKey) {
                    FTP.uploadKey();
                    keySelected = true;
                    if(logout.isEnabled())
                        uploadFile.setEnabled(true);
                }
		// ok it is coming from the JTextField
		if (connected) {
			client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, textField.getText()));
			return;
		}
		if (object == login) {
                        
			// ok it is a connection request
			String username = textField.getText().trim();
                        String password = passwordField.getText().trim();
			// empty username ignore it
			if (username.length() == 0 || password.length() == 0) 
                            return;                       
			// empty serverAddress ignore it
			String server = textFieldServer.getText().trim();
			if (server.length() == 0) return;
			// empty or invalid port number, ignore it
			String portNumber = textFieldPort.getText().trim();
			if (portNumber.length() == 0) 
                            return;			
                        
                        try {
				this.portNumber = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}
			
                        // try creating a new Client with GUI
			client = authenticate(username, password, client, server);
                        if(client == null) {
                            displayToClientScreen("\n> Invalid username/password");
                            return;
                        }
                            
			// test if we can start the Client
                        if (!client.start()) 
                            return;
                        
			setStateOfLabelsTo(false);
			connected = true;
			// disable login button
			login.setEnabled(false);
			// enable the 2 buttons
			logout.setEnabled(true);
                        if(keySelected)
                            uploadFile.setEnabled(true);
		}
	}
        
        private Client authenticate(String username, String password, Client client, String server) {
            int index = -1;
            for(int i = 0; i < FTP.USERNAMES.length; i++) {
                if(FTP.USERNAMES[i].equals(username)) {
                    index = i;
                    break;
                }
            }
            if(index == -1)
                return null;
            String send = FTP.verifyUsernameAndPassword(username, FTP.SALTS[index], password);
            return new Client(server, this.portNumber, send, this, 3);
        }
        
	public void setStateOfLabelsTo(boolean b) {
                this.textField.setVisible(b);
                this.passwordField.setVisible(b);
		//this.label.setVisible(b);
                //System.out.println("HERE");
		textFieldServer.setEditable(b);
		textFieldPort.setEditable(b);
                textField.setEditable(b);
                passwordField.setEditable(b);
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
