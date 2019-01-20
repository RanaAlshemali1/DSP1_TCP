import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner; 


// Each client will be handled here for multothreding purposes
class ClientHandler  extends Thread {  

	DataInputStream dis; 
	DataOutputStream dos; 
	Socket socket; 
	static boolean isStopped = false;
	String receivedMessage = "";
	String returnedMessage = ""; 

	//List of commands: get put delete ls cd mkdir pwd quit
	public static final String GET_COMMAND = "get";
	public static final String PUT_COMMAND = "put";
	public static final String DELETE_COMMAND = "delete";

	public static final String LS_COMMAND = "ls";
	public static final String LS_NO_SUBDIR = "There are no file or subdirectory";

	public static final String CD_COMMAND = "cd";
	public static final String MKDIR_COMMAND = "mkdir";
	public static final String PWD_COMMAND = "pwd";

	public static final String QUIT_COMMAND = "quit";
	public static final String QUIT_COMMAND_MESSAGE = "Connection closed!"; 

	public static final String INVALID_INPUT = "Invalid input ..";

	// 8 methods, each for one of the 8 commands
	// --------------------- GET FILE TO USER - SEND TO USER ------------------------
	public void getCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered get command";
		dos.writeUTF(returnedMessage);
	}

	// ------------------- PUT FILE FROM USER - RECEIVE FROM USER--------------------
	public void putCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered put command";
		dos.writeUTF(returnedMessage);
	}

	public void deleteCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered delete command";
		dos.writeUTF(returnedMessage);
	}

	public void lsCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered ls command";
		dos.writeUTF(returnedMessage);
	}

	public void cdCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered cd command";
		dos.writeUTF(returnedMessage);
	}

	public void mkdirCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered mkdir command";
		dos.writeUTF(returnedMessage);
	}

	public void pwdCommand(DataOutputStream dos) throws IOException { 
		returnedMessage = "You Entered pwd command";
		dos.writeUTF(returnedMessage);
	}

	public void quitCommand(DataOutputStream dos) throws IOException { 
		System.out.println("myftpserver> Client " + this.socket + " sent a quit command");
		System.out.println("myftpserver> Closing this connection ..."); 
		dos.writeUTF(QUIT_COMMAND_MESSAGE);
		isStopped = true;
		this.socket.close();
		System.out.println("myftpserver> " + QUIT_COMMAND_MESSAGE);
	}	

	public ClientHandler(Socket socket, DataInputStream dis, DataOutputStream dos) { 
		this.socket = socket; 
		this.dis = dis; 
		this.dos = dos; 
	} 

	@Override
	public void run() {

		isStopped = false;

		try {
			dos.writeUTF("You are connected .."); 
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// keep listening for the client's commands
		while (!isStopped) {
			try {
				//dos.writeUTF("skip"); 
				receivedMessage = dis.readUTF(); 
				String command = "";
				String fileDirName = "";

				if(receivedMessage.contains(" ")) {
					String[] splittedCommand = receivedMessage.split(" ");
					command = splittedCommand[0];
					fileDirName = splittedCommand[1];
				}else {
					command = receivedMessage;
				}
				System.out.println("myftpserver> "+receivedMessage);

				isStopped = false;

				switch(command) {
				case GET_COMMAND:
					getCommand(dos);
					break;
				case PUT_COMMAND: 
					putCommand(dos); 
					break;
				case DELETE_COMMAND:
					deleteCommand(dos);
					break;
				case LS_COMMAND:
					lsCommand(dos);
					break;
				case CD_COMMAND:
					cdCommand(dos);
					break;
				case MKDIR_COMMAND:
					mkdirCommand(dos);
					break;
				case PWD_COMMAND:
					pwdCommand(dos);
					break;
				case QUIT_COMMAND:
					quitCommand(dos);
					break;
				default: 
					dos.writeUTF(INVALID_INPUT); 
					break; 
				}
			}catch ( IOException e) {
				e.printStackTrace();
			}
		} 

		try { 
			this.dis.close();
			this.dos.close();
		}catch ( IOException e) {
			e.printStackTrace();
		}
	}
} 
public class myftpserver {

	static int serverPort = 8080;
	static ServerSocket serverSocket = null;
	static boolean isStopped = false;

	public static void main(String args[]) throws IOException{

		Scanner scanner = new Scanner(System.in);
		System.out.print("myftpserver> Enter port number: ");
		String clientPortString = scanner.nextLine();
		serverPort = Integer.valueOf(clientPortString); 
		serverSocket = new ServerSocket(serverPort);
		System.out.println("myftpserver> Waiting for a Client to connect ..");
		// continuously listen for clients requests
		while (!isStopped) {
			Socket socket = null;

			try { 
				// incoming client requests are received using this socket object
				socket = serverSocket.accept();
				System.out.println("myftpserver> A new client is connected: "+ socket);

				// input and output streams
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

				System.out.println("myftpserver> This Client is being assigned to a new thread");

				Thread thread = new ClientHandler(socket, dis, dos); 
				thread.start();

			} catch (Exception e) {
				socket.close();
				e.printStackTrace();
			}

		}
	}
}



