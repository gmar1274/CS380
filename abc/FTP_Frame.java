package abc;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import java.awt.TextArea;
import java.awt.Label;
import java.awt.Button;
import java.awt.TextField;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.ButtonGroup;
import javax.swing.JSeparator;
import javax.swing.JCheckBox;

public class FTP_Frame extends JFrame implements ActionListener {
	private JPanel					contentPane;
	private boolean					login;
	// private Socket socket;
	private TextField				server_textArea;
	private TextField				port_textArea;
	private JRadioButtonMenuItem	client_radio;
	private JRadioButtonMenuItem	server_radio;
	private TextField				username_textArea;
	private TextArea				clientScreen;
	private TextArea				serverScreen;
	private Client					client;
	private JButton					login_btn;
	private JButton					uploadFile_btn;
	private final ButtonGroup		buttonGroup	= new ButtonGroup();
	private JSeparator				separator;
	private JCheckBox				controlledFail;
	public JCheckBox getControlledFail() {
		return this.controlledFail;
	}
	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FTP_Frame frame = null;
					if (args.length > 0) {
						frame = new FTP_Frame(args[0]);
					} else {
						frame = new FTP_Frame(null);
					}
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * Create the frame.
	 */
	public FTP_Frame(String msg) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// /close streams
				if (client != null) client.disconnect();
			}
		});
		setResizable(false);
		setSize(new Dimension(600, 360));
		this.setLocationRelativeTo(null);
		login = false;
		setTitle("JFileTransferProtocol");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// setBounds(100, 100, 600, 350);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		JPanel panel = new JPanel();
		panel.setBounds(0, 52, 586, 210);
		contentPane.add(panel);
		panel.setLayout(null);
		clientScreen = new TextArea();
		clientScreen.setFont(new Font("Arial", Font.PLAIN, 18));
		clientScreen.setBackground(new Color(0, 0, 0));
		clientScreen.setForeground(new Color(0, 204, 0));
		clientScreen.setText("JFTP >: ");
		clientScreen.setEditable(false);
		clientScreen.setBounds(0, 0, 586, 210);
		panel.add(clientScreen);
		serverScreen = new TextArea();
		serverScreen.setForeground(new Color(0, 204, 0));
		serverScreen.setBackground(new Color(0, 0, 0));
		serverScreen.setVisible(false);
		serverScreen.setEditable(false);
		serverScreen.setText("Server>: ");
		serverScreen.setBounds(0, 0, 586, 207);
		panel.add(serverScreen);
		// ////////////
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, 584, 30);
		contentPane.add(menuBar);
		JMenu file_menu = new JMenu("File");
		menuBar.add(file_menu);
		JMenu edit_menu = new JMenu("Edit");
		menuBar.add(edit_menu);
		client_radio = new JRadioButtonMenuItem("Act as client");
		client_radio.addActionListener(this);
		buttonGroup.add(client_radio);
		client_radio.setSelected(true);
		edit_menu.add(client_radio);
		server_radio = new JRadioButtonMenuItem("Act as server");
		server_radio.addActionListener(this);
		buttonGroup.add(server_radio);
		// ////////
		edit_menu.add(server_radio);
		separator = new JSeparator();
		edit_menu.add(separator);
		controlledFail = new JCheckBox("Controlled fail");
		controlledFail.addActionListener(this);
		edit_menu.add(controlledFail);
		// ////////
		Label server_label = new Label("Server:");
		server_label.setBounds(0, 30, 40, 22);
		contentPane.add(server_label);
		server_textArea = new TextField();
		server_textArea.setFont(new Font("Arial", Font.PLAIN, 14));
		server_textArea.setText("localhost");
		server_textArea.setBounds(45, 30, 150, 25);
		contentPane.add(server_textArea);
		Label port_label = new Label("Port:");
		port_label.setBounds(250, 30, 30, 22);
		contentPane.add(port_label);
		port_textArea = new TextField();
		port_textArea.setFont(new Font("Arial", Font.PLAIN, 14));
		port_textArea.setText("23");
		port_textArea.setBounds(280, 30, 70, 25);
		contentPane.add(port_textArea);
		Label username_label = new Label("Username:");
		username_label.setBounds(356, 31, 62, 22);
		contentPane.add(username_label);
		username_textArea = new TextField();
		username_textArea.setFont(new Font("Arial", Font.PLAIN, 14));
		username_textArea.setText("Anon");
		username_textArea.setBounds(418, 30, 160, 22);
		contentPane.add(username_textArea);
		login_btn = new JButton("Login");
		login_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton b = (JButton) e.getSource();
				if (b.getText().contains("Log Off")) {
					client.disconnect();
					client = null;
					server_textArea.setEnabled(true);
					port_textArea.setEnabled(true);
					uploadFile_btn.setEnabled(false);
					username_textArea.setEnabled(true);
					b.setText("Login");
					return;
				} else {
					login();
				}
			}
		});
		login_btn.setBounds(10, 273, 200, 50);
		contentPane.add(login_btn);
		uploadFile_btn = new JButton("Upload File");
		uploadFile_btn.setEnabled(false);
		uploadFile_btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!login) { return; }
				client.uploadFile();
			}
		});
		uploadFile_btn.setBounds(218, 273, 200, 50);
		contentPane.add(uploadFile_btn);
	}
	/*
	 * LOGIN ATTEMPTS: default 3.
	 */
	private void login() {
		if (this.server_textArea.getText().length() <= 0 || this.port_textArea.getText().length() <= 0 || this.username_textArea.getText().length() <= 0) { return; }
		final FTP_Frame ftp_frame = this;
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					client = new Client(server_textArea.getText(), username_textArea.getText(), JOptionPane.showInputDialog("Enter your password:"),
					Integer.parseInt(port_textArea.getText()), 3, ftp_frame);
					if (!client.start() && !client.authenticate()) { return; }
					if (client.getSocket().isConnected()) {
						login = true;
						login_btn.setText("Log Off");
						port_textArea.setEnabled(false);
						server_textArea.setEnabled(false);
						uploadFile_btn.setEnabled(true);
						username_textArea.setEnabled(false);
					}
					// check usetrname and password after connection to server
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}
	public void displayToClientScreen(String msg) {
		clientScreen.setText(this.clientScreen.getText() + "\nJFTP >: " + msg);
	}
	public void actionPerformed(ActionEvent e) {
		try {
			JRadioButtonMenuItem b = (JRadioButtonMenuItem) e.getSource();
			if (b == this.client_radio && !b.isSelected()) {
				this.dispose();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						new FTP_Frame(null);
					}
				});
			} else if (b == this.server_radio && !b.isSelected()) {
				this.dispose();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						new FTP_Server();
					}
				});
			}
		} catch (Exception ee) {
			// is checkbox
			JCheckBox v = (JCheckBox) e.getSource();
			if (!v.isSelected()) client.sendMessage(new NetworkMessage(NetworkMessage.ERROR, this));
		}
	}
}