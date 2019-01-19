import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner; 



class ClientHandler  extends Thread {  

	DataInputStream dis; 
	DataOutputStream dos; 
	Socket socket; 
	static boolean isStopped = false;
	String receivedMessage = "";
	String returnedMessage = "";

	//get put delete ls cd mkdir pwd quit
	public static final String GET_COMMAND = "get";
	public static final String PUT_COMMAND = "put";
	public static final String DELETE_COMMAND = "delete";
	public static final String LS_COMMAND = "ls";
	public static final String CD_COMMAND = "cd";
	public static final String MKDIR_COMMAND = "mkdir";
	public static final String PWD_COMMAND = "pwd";
	public static final String QUIT_COMMAND = "quit";

	public void getCommand() {

	}
	public void putCommand() {

	}
	public void deleteCommand() {

	}
	public void lsCommand() {

	}
	public void cdCommand() {

	}
	public void mkdirCommand() {

	}
	public void pwdCommand() {

	}
	public void quitCommand() throws IOException {

		returnedMessage = "You Entered quit command";
		dos.writeUTF(returnedMessage);
		System.out.println("myftpserver> Client " + this.socket + " sent a quit command");
		System.out.println("myftpserver> Closing this connection ..."); 
		isStopped = true;
		this.socket.close();
		System.out.println("myftpserver> Connection closed!");
		
	}	

	public ClientHandler(Socket socket, DataInputStream dis, DataOutputStream dos) { 
		this.socket = socket; 
		this.dis = dis; 
		this.dos = dos; 
	} 

	@Override
	public void run() {
		
		System.out.println("2");
		isStopped = false;

		try {
			dos.writeUTF("You are connected ..");
			System.out.println("3");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (!isStopped) {
			try {
				//dos.writeUTF("skip");
				System.out.println("4");
				receivedMessage = dis.readUTF(); 
				System.out.print("myftpserver> ");
				System.out.println(receivedMessage);
				isStopped = false;
				switch(receivedMessage) {
				case GET_COMMAND:
					returnedMessage = "You Entered get command";
					dos.writeUTF(returnedMessage);
					break;
				case PUT_COMMAND:
					returnedMessage = "You Entered put command";
					dos.writeUTF(returnedMessage);
					break;
				case DELETE_COMMAND:
					returnedMessage = "You Entered delete command";
					dos.writeUTF(returnedMessage);
					break;
				case LS_COMMAND:
					returnedMessage = "You Entered ls command";
					dos.writeUTF(returnedMessage);
					break;
				case CD_COMMAND:
					returnedMessage = "You Entered cd command";
					dos.writeUTF(returnedMessage);
					break;
				case MKDIR_COMMAND:
					returnedMessage = "You Entered mkdir command";
					dos.writeUTF(returnedMessage);
					break;
				case PWD_COMMAND:
					returnedMessage = "You Entered pwd command";
					dos.writeUTF(returnedMessage);
					break;
				case QUIT_COMMAND:
					quitCommand();
					break;
				default: 
					dos.writeUTF("Invalid input .."); 
					break; 
				}
			}catch ( IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("5");

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
				System.out.println("1");
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



