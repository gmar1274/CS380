import static ftp.FTP.encodeBase64;
import static ftp.FTP.hash;
import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {
    
        private final int ATTEMPT_CAP = 3;
        private static final String key = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private ObjectInputStream	sInput;	// to read from the socket
	private ObjectOutputStream	sOutput;	// to write on the socket
	private Socket			socket;
	private ClientGUI		clientGUI;
	private String			server, username;
	private int			port;
	private int			attempts, userMaxAttempt, fail;
        private boolean asciiArmor, failed;
        private File sendingFile;
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
		this.userMaxAttempt = attempts + 1;
		this.attempts = 0;
                failed = false;
	}
	Client(String server, int port, int attempts) {
		this(server, port, null, null, attempts);
	}
	public boolean start() {
            // try to connect to the server
            try {
                socket = new Socket(server, port);
            } catch (Exception e) {
		e.printStackTrace();
                displayToClientScreen("Cannot establish a connection to server: " + 
                                      server + " on port: " + port + "\n" + e);
		return false;
            }
            String msg = "Connection accepted " + socket.getInetAddress() + 
                         ":" + socket.getPort();
            //displayToClientScreen(msg);
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
		if (clientGUI == null) 
                    System.out.println(msg); // println in console mode
		else 
                    clientGUI.displayToClientScreen("\n" + msg); // append to the ClientGUI display field.
	}
        
	public void uploadFile(int failAttempts, boolean ascii, boolean first) {
            asciiArmor = ascii;
            fail = failAttempts;
            try {
                File file;
                if(first) {
                    String fileName = null;
                    JFileChooser fc = new JFileChooser();
                    if (fc.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) 
                        fileName = fc.getSelectedFile().getAbsolutePath();
                    else 
                        return;
                    file = new File(fileName);
                    sendingFile = file;
                }
                else
                    file = sendingFile;
		// send file
                failed = false;
		byte[] fileArray = new byte[(int) file.length()];
		if (fileArray.length > Math.pow(2, 30)) {
                    JOptionPane.showMessageDialog(null, "File too large. Must be less than or equal to 1 GB.");
                    return;
		}
                sendFile(file, FTP.keyFile, fail--, ascii);
            } catch (Exception e) {
		e.printStackTrace();
            }
	}
       
	/*
	 * To send a message to the server
	 */
	void sendMessage(NetworkMessage msg) {
		try {
			sOutput.writeObject(msg);
		} catch (Exception e) {
			displayToClientScreen("Exception writing to server: " + e);
		}
	}
	/*
	 * Close all the open streams.
	 */
	public void disconnect() {
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

	/****
	 ** MAIN LOOP THAT SERVER LISTENS TO CLIENT
	 ****/
	private class ListenFromServer extends Thread {
            public void run() {
                while (true) {
                    try {
                        NetworkMessage nm = (NetworkMessage) sInput.readObject();
                        if(nm.getType() == NetworkMessage.STRING) {
                            String msg = new String(nm.getByteArray(), "UTF-8");
                            if (clientGUI == null) {
                                System.out.println(msg);
                                System.out.print("> ");
                            } 
                            else {
                                clientGUI.displayToClientScreen("\n> " + msg);
                            }
                        }
                        else if(nm.getType() == NetworkMessage.FAILED_TRANSMISSION) {
                            failed = true;
                            displayToClientScreen("Failed transmission, attempting to send again...");
                            attempts++;
                            if(attempts < ATTEMPT_CAP)
                                uploadFile(fail, asciiArmor, false);
                            else
                                disconnect();
                        }
                    } catch (IOException e) {
			clientGUI.setStateOfLabelsTo(true);
                        //displayToClientScreen("\n> Connection with the server has been terminated.");
			if (clientGUI != null) 
                            clientGUI.connectionFailed();
			else disconnect();
                            break;
                    } catch (ClassNotFoundException e) {
			e.printStackTrace();
                    }
		}
            }
	}
    public void sendFile(File inputFile, File keyFile, int failAttempts, boolean ascii) {       
        try {
            this.sOutput.writeObject(new NetworkMessage(NetworkMessage.LISTEN, new byte[]{(byte)(ascii ? 1 : 0)}));
        } catch (Exception e) {}
        byte[] file, key;
        try {
            //Converts both the inputFile and keyFile into byte arrays.
            file = FTP.fileToByteArray(inputFile);
            key = FTP.fileToByteArray(keyFile);
            
            //For every kilobyte in the file, a chunk is made, hashed, and these
            //are both merged into a byte array of length 1028, encrypted, encoded 
            //in Base 64, and sent.
            for(int i = 0; i < file.length; i += 1024) {
                if(!failed) {
                    //Creates a kilobyte byte array for the current file chunk. If
                    //this is the last iteration and this file chunk is less than a
                    //kilobyte in length (the remander of the file), the byte array
                    //length is adjusted accordingly.
                    byte[] send = new byte[(file.length - i) < 1024 ? (file.length - i) + 4 : 1028];

                    //The new byte array is filled with the chunk's contents.
                    byte[] sendHash = new byte[send.length - 4]; 
                    for(int j = 0; j < sendHash.length; j++) {
                        send[j] = file[i+j];
                        sendHash[j] = file[i+j];
                    }

                    //The hash is generated (4 bytes in length).
                    byte[] hash = FTP.hash(sendHash, key, (failAttempts <= 0));


                    //The hash is put in the last remaining 4 bytes of the byte array to be sent.
                    for(int j = 0; j < hash.length; j++)
                        send[((file.length - i) < 1024 ? (file.length - i): 1024) + j] = hash[j];

                    //The byte array is encrypted.
                    byte[] encrypt = FTP.encryptDecrypt(send, key);
                    System.out.println("CLIENT: " + ascii);
                    if(ascii) {

                        //The byte array is encoded in Base64
                        String encode = FTP.encodeBase64(encrypt);

                        //The encoded string is converted into a byte array representation. This
                        //is what will be sent to the receiver.
                        byte[] finalArray = encode.getBytes(Charset.forName("UTF-8"));

                        //finalArray is sent to the receiver.
                        for(int j = 0; j < sendHash.length; j++)
                            System.out.print((j == 0 ? "CLIENT: [" : "") + (int)sendHash[j] + (j == sendHash.length-1 ? "]\n" : ", "));
                        this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, finalArray));
                    }
                    else {
                        this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, encrypt));
                    }
                }
                
            }
            if(!failed)
                this.sOutput.writeObject(new NetworkMessage(NetworkMessage.LASTPACKETSENT, 
                                                            FTP.encryptDecrypt(inputFile.getName().getBytes(Charset.forName("UTF-8")), 
                                                                               key)));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
