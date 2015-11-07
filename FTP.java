import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

public class FTP extends JFrame {
	FTP() {
		this.setSize(600, 80);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(this.EXIT_ON_CLOSE);
		this.setTitle("FTP Upload");
		this.setResizable(false);
		this.add(new FTPMainMenu(this));
		this.setVisible(true);
	}
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new FTP();
			}
		});
	}
	public class FTPMainMenu extends JPanel implements ActionListener {
		private JFrame		frame;
		private JButton		server;
		private JButton		client;
		private String		destanation, sender;
		private JTextPane	text;
		private String		host;
		private int			portNumber;
		private String		password;
		FTPMainMenu(JFrame frame) {
			password = "" + this.hashCode();
			text = new JTextPane();
			text.setEditable(false);
			this.destanation = null;
			this.sender = null;
			this.frame = frame;
			this.setLayout(new FlowLayout());
			addButtons();
			this.setVisible(true);
		}
		private void addButtons() {
			client = new JButton("<html><font size=4>Client: File Transfer Protocol </html>");
			client.addActionListener(this);
			server = new JButton("<html><font size=4>Server: File Transfer Protocol");
			server.addActionListener(this);
			this.add(client);
			this.add(server);
			this.repaint();
		}
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == this.client) {
				this.frame.dispose();
				ClientGUI clientGUI = new ClientGUI("localhost", 23);//default value
				Thread serverThread = new Thread() {
					public void start() {
						new ServerGUI(23, false);//default value will be changed if user decides to change it in the clintGUI class
					}
				};
				serverThread.start();
			} else if (e.getSource() == this.server) {
				// act as reciever
				this.frame.dispose();
				ServerGUI serverGUI = new ServerGUI(23);
				Thread clientThread = new Thread() {
					public void start() {
						new Client("localhost",23,3);//default value will be changed if user decides to change it in the clintGUI class
					}
				};
				clientThread.start();
			}
		}
	}
}
