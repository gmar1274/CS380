package abc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/*
 * The server as a GUI
 */
public class ServerGUI extends JFrame implements ActionListener, WindowListener {
	private static final long	serialVersionUID	= 1L;
	// the stop and start buttons
	private JButton				connectButton;
	// JTextArea for the chat room and the events
	private JTextArea			serverTextField, event;
	// The port number
	private JTextField			tPortNumber;
	// my server
	private Server				server;
	private boolean				inBG;
	// server constructor that receive the port to listen to for connection as parameter
	ServerGUI(int port, boolean visible) {
		super("Server");
		server = new Server(port);
		inBG = !visible;
		new ServerRunning().start();
		setVisible(false);
	}
	ServerGUI(int port) {
		super("Server");
		// server = null;
		// in the NorthPanel the PortNumber the Start and Stop buttons
		JPanel north = new JPanel();
		north.add(new JLabel("Port number: "));
		tPortNumber = new JTextField("" + port);
		north.add(tPortNumber);
		// to stop or start the server, we start with "Start"
		connectButton = new JButton("Start");
		connectButton.addActionListener(this);
		north.add(connectButton);
		add(north, BorderLayout.NORTH);
		// the event and chat room
		JPanel center = new JPanel(new GridLayout(2, 1));
		serverTextField = new JTextArea(80, 80);
		serverTextField.setEditable(false);
		displayServerScreen("> Server is not currently listening.");
		center.add(new JScrollPane(serverTextField));
		event = new JTextArea(80, 80);
		event.setEditable(false);
		displayToEventLog("Events log.");
		center.add(new JScrollPane(event));
		add(center);
		// need to be informed when the user click the close button on the frame
		addWindowListener(this);
		setSize(400, 600);
		this.setLocationRelativeTo(null);
		setVisible(true);
	}
	// append message to the two JTextArea
	// position at the end
	void displayServerScreen(String str) {
		serverTextField.append(str + "\n");
	}
	void displayToEventLog(String str) {
		event.append(str + "\n");
	}
	// start or stop where clicked
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.connectButton && this.connectButton.getText().equalsIgnoreCase("Stop") && server != null) {
			server.disconnectClients();
			server = null;
			tPortNumber.setEditable(true);
			connectButton.setText("Start");
		} else {
			// OK start the server
			int port;
			try {
				port = Integer.parseInt(tPortNumber.getText().trim());
			} catch (Exception ee) {
				displayToEventLog("Invalid port number.");
				return;
			}
			// create a new Server
			server = new Server(port, this);
			// and start it as a thread
			new ServerRunning().start();
			this.displayServerScreen("> Server is listening on port: " + port);
			connectButton.setText("Stop");
			tPortNumber.setEditable(false);
		}
	}
	/*
	 * If the user click the X button to close the application connection with the server will be released and port freed.
	 */
	public void windowClosing(WindowEvent e) {
		if (server != null) {
			try {
				server.disconnectClients(); // close the connection
			} catch (Exception ee) {
				ee.printStackTrace();
			}
			server = null;
		}
		//FTP.main(null);
	}
	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	/*
	 * A thread to run the Server
	 */
	class ServerRunning extends Thread {
		public void run() {
			if (inBG) server.startServer();
			else {
				server.startServer();
				// if (inBG) appendEvent("Server crashed.");
			}
		}
	}
}
