package abc;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;
import java.awt.ScrollPane;
import java.awt.Dimension;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.Color;
import java.awt.Label;
import java.awt.TextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.ServerSocket;

public class FTP_Server extends JFrame {
	private ServerSocket	serverSocket;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FTP_Server frame = new FTP_Server();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	private JTextArea	serverScreen;
	private TextField	portText;
	private JButton		btnListen;
	/**
	 * Create the frame.
	 */
	public FTP_Server() {
		setTitle("Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 600);
		this.setLocationRelativeTo(null);
		getContentPane().setLayout(null);
		serverScreen = new JTextArea();
		serverScreen.setLineWrap(true);
		serverScreen.setForeground(new Color(0, 255, 0));
		serverScreen.setEditable(false);
		serverScreen.setBackground(new Color(0, 0, 0));
		serverScreen.setText("Server >:");
		ScrollPane s = new ScrollPane();
		s.setBounds(10, 80, 460, 400);
		s.add(serverScreen);
		getContentPane().add(s);
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, 480, 50);
		getContentPane().add(menuBar);
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		Label label = new Label("Port:");
		label.setBounds(10, 50, 30, 22);
		getContentPane().add(label);
		portText = new TextField();
		portText.setText("23");
		portText.setBounds(37, 52, 100, 22);
		getContentPane().add(portText);
		btnListen = new JButton("Start listening");
		btnListen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (portText.getText().length() <= 0) return;
				JButton b = (JButton) e.getSource();
				try {
					if (b.getText().contains("Stop Listening")) {
						b.setText("Start listening");
						serverSocket.close();
						portText.setEnabled(true);
						displayToServer("Sever stopped listening on port: " + serverSocket.getLocalPort() + ".");
						return;
					}
					// //was here
					serverSocket = new ServerSocket(Integer.parseInt(portText.getText()));
					btnListen.setText("Stop Listening");
					portText.setEnabled(false);
					displayToServer("Server is listening on port: " + serverSocket.getLocalPort() + ".");
				} catch (IOException e1) {
					displayToServer("" + e1);
				}
			}
		});
		btnListen.setBounds(171, 51, 130, 23);
		getContentPane().add(btnListen);
	}
	private void displayToServer(String msg) {
		this.serverScreen.setText(this.serverScreen.getText() + "\nServer >: " + msg);
	}
}
