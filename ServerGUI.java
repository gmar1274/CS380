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
	private boolean	inBG;
	// server constructor that receive the port to listen to for connection as parameter
	ServerGUI(int port, boolean visible) {
		super("Server");
		server = new Server(port);
		
		inBG=true;
		new ServerRunning().start();
		setVisible(false);
	}
	ServerGUI(int port) {
		super("Server");
		server = null;
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
		setServerTextField("> Server is not currently listening.");
		center.add(new JScrollPane(serverTextField));
		event = new JTextArea(80, 80);
		event.setEditable(false);
		appendEvent("Events log.");
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
	void setServerTextField(String str) {
		serverTextField.append(str + "\n");
	}
	void appendEvent(String str) {
		event.append(str + "\n");
	}
	// start or stop where clicked
	public void actionPerformed(ActionEvent e) {
		// if running we have to stop
		if (server != null) {
			server.stop();
			server = null;
			tPortNumber.setEditable(true);
			connectButton.setText("Start");
			return;
		}
		// OK start the server
		int port;
		try {
			port = Integer.parseInt(tPortNumber.getText().trim());
		} catch (Exception ee) {
			appendEvent("Invalid port number.");
			return;
		}
		// create a new Server
		server = new Server(port, this);
		// and start it as a thread
		new ServerRunning().start();
		this.setServerTextField("> Server is listening on port: " + port);
		connectButton.setText("Stop");
		tPortNumber.setEditable(false);
	}
	// entry point to start the Server
	public static void main(String[] arg) {
		// start server default port 23
		new ServerGUI(23);
	}
	/*
	 * If the user click the X button to close the application connection with the server will be released and port freed.
	 */
	public void windowClosing(WindowEvent e) {
		if (server != null) {
			try {
				server.stop(); // close the connection
			} catch (Exception ee) {
				ee.printStackTrace();
			}
			server = null;
		}
		System.exit(0);
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
			if(inBG)server.startInBackground();
			else server.start();
			connectButton.setText("Start");
			tPortNumber.setEditable(true);
			if(inBG)appendEvent("Server crashed.");
			server = null;
		}
	}
}
