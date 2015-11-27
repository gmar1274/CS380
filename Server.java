import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
    
    public static String[] HASHES = new String[]{"L6SMvA=="};
        // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread>	clientThreadArrayList;
    private ServerGUI serverGUI;
    // to display time
    private SimpleDateFormat sdateFormat;
    // the port number to listen for connection
    private int port;
    private boolean	keepGoing;
    private ServerSocket serverSocket;
    public static boolean asciiArmored, listen, aunthenticated;
    
    /*
     * server constructor that receive the port to listen to for connection as parameter in console
     */
    public Server(int port) {
	this(port, null);
    }
    
    public Server(int port, ServerGUI sg) {
	// GUI or not
	this.serverGUI = sg;
	// the port
	this.port = port;
	// to display hh:mm:ss
	sdateFormat = new SimpleDateFormat("HH:mm:ss");
	// ArrayList for the Client list
	clientThreadArrayList = new ArrayList<ClientThread>();
        listen = false;
        aunthenticated = false;
    }
    
    /**
     * MAIN LOOP CONNECTION FOR THE SERVER.
     **/
    public void startServer() {
	keepGoing = true;
	/* create socket server and wait for connection requests */
	try {
            // the socket used by the server
            serverSocket = new ServerSocket(port);
            // infinite loop to wait for connections
            // while (keepGoing) {
            while (true) {
                Socket socket = serverSocket.accept(); // accept connection
                System.out.println(socket.getInetAddress());
		if (!keepGoing) 
                    break;
		ClientThread client = new ClientThread(socket); // make a thread of it
		clientThreadArrayList.add(client); // save it in the ArrayList
		client.start();
            }
            // I was asked to stop
            try {
		disconnectClients(serverSocket);
            } catch (Exception e) {
		e.printStackTrace();
            }
	} catch (IOException e) {}
    }
    
    public void disconnectClients() {
	this.disconnectClients(serverSocket);
    }
    
    private void disconnectClients(ServerSocket serverSocket) {
        try {
            this.keepGoing = false;
            for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
                ClientThread ct = clientThreadArrayList.get(i);
		try {
                    serverGUI.displayServerScreen("> Server disconnceted with " + ct.username + ".");                  
                    ct.closeStreams();
		} catch (Exception e) {
                    serverGUI.displayToEventLog("Exception closing the server and clients: " + e);
		}
            }
            serverSocket.close();
            serverGUI.displayServerScreen("> Sever stopped listening.");
	} catch (Exception ee) {}
    }
    
    /*
     * Display an event (not a message) to the console or the GUI
     */
    public void displayToServerLog(String msg) {
        String time = sdateFormat.format(new Date()) + " " + msg;
	if (serverGUI == null) 
            System.out.println(time);
	else 
            serverGUI.displayToEventLog(time);
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
	for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
            ClientThread ct = clientThreadArrayList.get(i);
            // found it
            if (ct.id == id) {
		clientThreadArrayList.remove(i);
		return;
            }
	}
    }
    
    /** One instance of this thread will run for each client */
    public class ClientThread extends Thread {
        // the socket where to listen/talk
	private Socket socket;
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private int id;
	private String username;
	private NetworkMessage networkMessage;
	// the date it connected
	private String date;
	// Constructor
	
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            // Creating Data Streams
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
		sInput = new ObjectInputStream(socket.getInputStream());
		username = (String) sInput.readObject();
                if(!authorize(username)) {
                    sendToClient(FTP.stringToByteArray("Invalid username/password"), NetworkMessage.STRING);
                    closeStreams();
                    return;
                }
		if (serverGUI != null) 
                    serverGUI.displayServerScreen("> " + username + " just connected.");
		else 
                    System.out.println("> " + username + " just connected.");
		sendToClient(FTP.stringToByteArray("Server says: Hello " + username.substring(0, username.indexOf(":")) + "!!!"), 
                             NetworkMessage.STRING);
            } catch (IOException e) {
                serverGUI.displayToEventLog("Exception creating new Input/output Streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
		e.printStackTrace();
            }
            date = new Date().toString();
	}
        
        private boolean authorize(String usernameAndPass) {
            System.out.println(usernameAndPass);
            int index = 0;
            for(int i = 0; i < FTP.USERNAMES.length; i++) {
                if(FTP.USERNAMES[i].equals(usernameAndPass.substring(0, usernameAndPass.indexOf(":")))) {
                    index = i;
                    break;
                }
            }
            if(HASHES[index].equals(usernameAndPass.substring(usernameAndPass.indexOf(":")+1))) {
                aunthenticated = true;
                return true;
            }
            return false;
        }
        
        public void run() {
            listenForClients();
	}
        
	/**
	 * THE MAIN LISTENING LOOP
	 */
	public void listenForClients() {
            // to loop until LOGOUT
            ArrayList<byte[]> byteList = new ArrayList<byte[]>();
            while (true) {                       
                try {
                    //System.out.println(networkMessage);
                    networkMessage = (NetworkMessage) sInput.readObject();                   
		} catch (IOException e) {
                    if (this.socket.isClosed() && aunthenticated) {
                        displayToServerLog(username + " connection has been terminated. " + e);
                    } 
                    else 
                        displayToServerLog(username + " File transfer was unsuccesful: " + e);
                    break;
		} catch (ClassNotFoundException e2) {
                    break;
		}
		switch (networkMessage.getType()) {
                    // case NetworkMessage.MESSAGE:
                    // broadcast(username + ": " + networkMessage.getMessage());
                    // break;
                    case NetworkMessage.LOGOUT:
                        displayToServerLog(username + " disconnected.");
			this.closeStreams();
			return;
                    case NetworkMessage.LISTEN:
                        listen = true;
                        asciiArmored = (networkMessage.getByteArray()[0] == (byte)1);
                        break;
                    case NetworkMessage.UPLOADFILE:
			if(listen) {
                            byte[] fileByteArray = networkMessage.getByteArray();
                            try {
                                byte[] add = recieveFinishedDataPiece(fileByteArray, FTP.keyFile);
                                if(add == null) {
                                    listen = false;
                                    closeStreams();
                                    System.out.println("EMPTY");
                                    if (serverGUI != null) {
                                        serverGUI.displayServerScreen("> There was an error transferring the file.");
                                    }
                                    else
                                        System.out.println("> There was an error transferring the file.");
                                    byteList.clear();
                                    sendToClient(new byte[0], NetworkMessage.FAILED_TRANSMISSION);
                                    break;
                                }
                                byteList.add(add);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (serverGUI != null) 
                                serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fileByteArray));
                            else 
                                System.out.println("> File byte array: " + Arrays.toString(fileByteArray));
                            break;
                        }
                    case NetworkMessage.LASTPACKETSENT:
                        byte[] fByteArray = networkMessage.getByteArray();
			if (serverGUI != null) 
                            serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fByteArray));
                        else {
                            System.out.println("> File byte array: " + Arrays.toString(fByteArray));
                            System.out.println("> File has been succesfully recieved.");
			}
			sendToClient(FTP.stringToByteArray(sdateFormat.format(new Date()) + " File successfully transfered."), 
                                     NetworkMessage.STRING);
                        try {
                            //The program knows that this is the last packet to be received for this file
                            //transfer. This packet contains an ecrypted byte array that represents the
                            //file name of the file uploaded from the client.
                            byteListToFile(byteList, 
                                           new String(FTP.encryptDecrypt(fByteArray, 
                                                                         FTP.fileToByteArray(FTP.keyFile)), 
                                                      "UTF-8"));
                            byteList.clear();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        listen = false;
                        break;
                        
		}
            }
            closeStreams();
	}
		
        // try to close everything
	private void closeStreams() {
            aunthenticated = false;
            try {
                if (sOutput != null) {
                    //True to close connection
                    sOutput.writeBoolean(true);
                    sOutput.close();
                }
            } 
            catch (Exception e) {}
            try {
				if (sInput != null) sInput.close();
			} catch (Exception e) {};
			try {
				if (socket != null) 
                                    socket.close();
			} catch (Exception e) {
				serverGUI.displayToEventLog("Connection to sever closed.");
			}
		}
		/*
		 * Write a String to the Client output stream
		 */
		private boolean sendToClient(byte[] msg, int type) {
			
                        // if Client is still connected send the message to it
			if (!socket.isConnected()) {
				closeStreams();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(new NetworkMessage(type, msg));
			}
			// if an error occurs, do not abort just inform the user
			catch (IOException e) {
				displayToServerLog("Error sending message to " + username);
				displayToServerLog(e.toString());
			}
			return true;
		}
	}
        
    public static byte[] recieveFinishedDataPiece(byte[] data, File key) throws Exception {       
        try {
            //Converts the sent byte array to a string, which is decoded and turned into
            //a new byte array.
            System.out.println("SERVER: " + asciiArmored);
            int decodeLength = 0;
            byte[] decrypt, decode;
            if(asciiArmored) {           
                String string = new String(data, "UTF-8");
                decode = FTP.decodeBase64(string);
                decrypt = FTP.encryptDecrypt(decode, FTP.fileToByteArray(key));
                decodeLength = decode.length;
            }
            else {
                decrypt = FTP.encryptDecrypt(data, FTP.fileToByteArray(key));
            } 

            //The byte array is decrypted.
            //This is the byte array that will store the originally sent data in the
            //chunk.
            byte[] finishedData = new byte[decrypt.length - 4];

            //This stores the hash that came with the chunk.
            byte[] hash = new byte[4];

            //Fills both of these arrays with their proper values.
            for(int i = 0; i < finishedData.length; i++)
                finishedData[i] = decrypt[i];
            for(int i = 0; i < hash.length; i++)
                hash[i] = decrypt[(decrypt.length-4) + i];
            
            for(int j = 0; j < finishedData.length; j++)
                System.out.print((j == 0 ? "SERVER: [" : "") + (int)finishedData[j] + (j == finishedData.length-1 ? "]\n" : ", "));
            //If the hash included with the chunk does not equal the hashed version
            //of the finished data, then the file was sent incorrectly, and null
            //is returned.
            if(!FTP.compareByteArrays(FTP.hash(finishedData, FTP.fileToByteArray(key), true), 
                                  hash)) {
                
                //System.out.println("FAILED. finishedData = " + Arrays.toString(decode));
                return null;
            }
            //System.out.println("SUCCESS! finishedData = " + Arrays.toString(finishedData));
            //The data originally put in the chunk is returned.
            return finishedData;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void byteListToFile(ArrayList<byte[]> byteList, String fileName) throws Exception  {
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        for(int i = 0; i < byteList.size(); i++) {
            if(byteList.get(i) != null) {
                for(int j = 0; j < byteList.get(i).length; j++)
                    bytes.add(byteList.get(i)[j]);
            }
        }
        
        byte[] finalBytes = new byte[bytes.size()];
        
        for(int i = 0; i < finalBytes.length; i++)
            finalBytes[i] = bytes.get(i);
        
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(finalBytes);
        fos.close();
    }
}
