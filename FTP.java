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
		private void uploadFile() {
			try {
				JOptionPane.showMessageDialog(null, "Select your file to upload");
				String fileName = null;
				JFileChooser fc = new JFileChooser();
				if (fc.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) fileName = fc.getSelectedFile().getAbsolutePath();
				else return;
				File f = new File(fileName);
				System.out.println(fileName);
				// openConnections();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// private void openConnections() {
		// host = "localhost";
		// portNumber = Integer.parseInt(JOptionPane.showInputDialog("Port number: "));
		// Thread thread = new Thread() {
		// public void run() {
		// server(host, password, portNumber);
		// }
		// };
		// thread.start();
		// client();
		// }
		// private void client() {
		// try {} catch (Exception e) {
		// client();
		// }
		// try {
		// while (true) {
		// Socket socket = new Socket(host, portNumber);
		// text.setText(text.getText() + "\nCreating socket to '" + host + "' on port " + portNumber);
		// BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		// PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		// text.setText(text.getText() + "\nServer says:" + br.readLine());
		// BufferedReader userInputBR = new BufferedReader(new InputStreamReader(System.in));
		// String userInput = userInputBR.readLine();
		// out.println(userInput);
		// text.setText(text.getText() + "\nserver says:" + br.readLine());
		// if ("exit".equalsIgnoreCase(userInput)) {
		// socket.close();
		// break;
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// private void server(String host, String password, int portNumber) {
		// text.setText(text.getText() + "\nCreating server socket on port " + portNumber);
		// ServerSocket serverSocket;
		// try {
		// serverSocket = new ServerSocket(portNumber);
		// while (true) {
		// frame.add(text);
		// frame.repaint();
		// Socket socket = serverSocket.accept();
		// OutputStream os = socket.getOutputStream();
		// PrintWriter pw = new PrintWriter(os, true);
		// text.setText(text.getText() + "\nWhat's you name?");
		// BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		// String str = br.readLine();
		// text.setText(text.getText() + "\nHello, " + str);
		// pw.close();
		// socket.close();
		// text.setText(text.getText() + "\nJust said hello to:" + str);
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		public void actionPerformed(ActionEvent e) {
			// uploadFile();
			if (e.getSource() == this.client) {
				this.frame.dispose();
				ClientGUI c = new ClientGUI("localhost", 23);
				Thread t2 = new Thread() {
					public void start() {
						new ServerGUI(23,false);
					}
				};
				t2.start();
			} else if (e.getSource() == this.server) {
				// act as reciever
			}
		}
	}
}
