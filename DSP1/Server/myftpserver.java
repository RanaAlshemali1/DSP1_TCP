import java.io.BufferedInputStream;
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
	String currentDir = System.getProperty("user.dir");

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
	public void getCommand(String fileDirName, DataOutputStream dos) throws IOException {
			File file = new File(fileDirName);
			if(file.exists()) {
				//get file size
				long fileSize = file.length();
				//send file size
				System.out.println("Gotcha!");
				dos.writeBytes(fileSize + "\n");
				byte[] buffer = new byte[8192];
				try {
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
					int count = 0;
					while((count = in.read(buffer)) > 0)
						dos.write(buffer, 0, count);
					returnedMessage = "Successfully get: "+ fileDirName;
					in.close();	
					dos.writeUTF(fileDirName);
					System.out.println("myftpserver> " + fileDirName);
				} 
				catch(Exception e) {
					System.out.println("transfer error: " + fileDirName);
				}		
		}
		
			
	}


	// ------------------- PUT FILE FROM USER - RECEIVE FROM USER--------------------
	public void putCommand(DataOutputStream dos) throws IOException {
		returnedMessage = "You Entered put command";
		dos.writeUTF(returnedMessage);
	}


	public void deleteCommand(String fileDirName, DataOutputStream dos) throws IOException {
		File file = new File(fileDirName);
		if(file.exists()){
			boolean result = file.delete();
			if(result){
				returnedMessage = "Successfully deleted: "+ fileDirName;	
			}
			else{
				returnedMessage = "Failed deleting: "+ fileDirName;
			}
		}else{
			returnedMessage = "File does not exist";
		}
		System.out.println(returnedMessage);
		System.out.println("myftpserver> ");
		dos.writeUTF(returnedMessage);
		
	}
		

	public void lsCommand(DataOutputStream dos) throws IOException {
		 
		currentDir = System.getProperty("user.dir");
		File folder = new File(currentDir);
		File[] listOfFiles = folder.listFiles();

		if(listOfFiles != null && listOfFiles.length == 0){
			dos.writeUTF(LS_NO_SUBDIR);
		}else { 
			String filesList = "";
			for (int i = 0; i < listOfFiles.length; i++) {
				if(i ==0) {
					filesList = listOfFiles[i].getName();
				}else {
					filesList += " \n" + listOfFiles[i].getName();
				}
			}
			dos.writeUTF(filesList);
			System.out.println("myftpserver> " + filesList);
		}
	}

	public void cdCommand(String fileDirName, DataOutputStream dos) throws IOException {
		currentDir = System.getProperty("user.dir");
		String splitDirectories[] = currentDir.split("/");
		if(fileDirName.equals("..")) {
			//cd .. select substring of currentDir: currentDir -last file - '/'
			currentDir = currentDir.substring(0 , currentDir.length() - splitDirectories[splitDirectories.length - 1].length() - 1);
			System.out.println(currentDir);
			//cd. 
		} else if(fileDirName.equals(".")){
			currentDir = currentDir;
			//cd
		} else {
			currentDir = currentDir + "/" + fileDirName;
		}
		System.setProperty("user.dir", currentDir);
		System.out.println("myftpserver> ");
		dos.writeUTF("OK!!");
	}
		

	public void mkdirCommand(String fileDirName, DataOutputStream dos) throws IOException {
		
		
		File file = new File(fileDirName);
		if(!file.exists()){
			boolean result = file.mkdir();
			if(result){
				returnedMessage = "Successfully created: "+ fileDirName;	
			}
			else{
				returnedMessage = "Failed creating: "+ fileDirName;
			}
		}else{
			returnedMessage = "File already exists";
		}
		System.out.println(returnedMessage);
		System.out.println("myftpserver> ");
		dos.writeUTF(returnedMessage);
		
	}

	public void pwdCommand(DataOutputStream dos) throws IOException { 
		currentDir = System.getProperty("user.dir");
		dos.writeUTF(currentDir);
		System.out.println("myftpserver> ");
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
				}
				
				else {
					command = receivedMessage;
				}
				System.out.println("myftpserver> "+receivedMessage);

				isStopped = false;

				switch(command) {
				case GET_COMMAND:
					getCommand(fileDirName,dos);
					break;
				case PUT_COMMAND: 
					putCommand(dos); 
					break;
				case DELETE_COMMAND:
					deleteCommand(fileDirName, dos);
					break;
				case LS_COMMAND:
					lsCommand(dos);
					break;
				case CD_COMMAND:
					cdCommand(fileDirName, dos);
					break;
				case MKDIR_COMMAND:
					mkdirCommand(fileDirName, dos);
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
