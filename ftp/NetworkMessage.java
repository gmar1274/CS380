import java.io.*;

/*
 * This class defines the different type of messages that will be exchanged between the Clients and the Server. When talking from a Java Client to a Java Server
 * a lot easier to pass Java objects, no need to count bytes or to wait for a line feed at the end of the frame
 */
public class NetworkMessage implements Serializable {
	protected static final long	serialVersionUID	= 1112122200L;
	// The different types of message sent by the Client
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server
	static final int ERROR = -1 , 
                         UPLOADFILE = 0, 
                         MESSAGE = 1, 
                         LOGOUT = 2, 
                         LASTPACKETSENT = 3,
                         LOGIN = 4;
	private int type;
	private byte[] fileArray;
	private String message;
	NetworkMessage(int type){//for logout
		this.type = type;
	}
	NetworkMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	NetworkMessage(int type,byte[] filearray){
		this.type=type;
		this.fileArray=filearray;
	}
	int getType() {
		return type;
	}
	public byte[] getByteArray(){return fileArray;} 
	String getMessage() {
		return message;
	}
}
