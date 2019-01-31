import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class myftp {

	static int clientPort = 1234;
	static Socket clientSocket = null;
	static String hostName = "localhost";
	static boolean isStopped = false;
	static DataInputStream dis;
	static DataOutputStream dos; 
	


	public static final String GET_COMMAND = "get";
	public static final String PUT_COMMAND = "put";
	public static final String DELETE_COMMAND = "delete";

	public static final String LS_COMMAND = "ls";
	public static final String LS_NO_SUBDIR = "There are no files or subdirectories";

	public static final String CD_COMMAND = "cd";
	public static final String MKDIR_COMMAND = "mkdir";
	public static final String PWD_COMMAND = "pwd";

	public static final String QUIT_COMMAND = "quit"; 
	public static final String QUIT_COMMAND_MESSAGE = "Connection closed!"; 

	// 8 methods, each for one of the 8 commands
	// --------------------- GET FILE FROM SERVER - RECEIVE FROM SERVER ------------------------
	public static void getCommand(String fileDirName, DataInputStream dis) throws IOException {

		//get file size
		String fileSize = dis.readLine();
		FileOutputStream f = new FileOutputStream(new File(fileDirName));
		int count = 0;
		byte[] buffer = new byte[8192];
		long bytesReceived = 0;
		while(bytesReceived < Integer.parseInt(fileSize)) {
			count = dis.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		};
		f.close();
	}

	// ------------------- PUT FILE TO SERVER - SEND TO SERVER--------------------
	public static void putCommand(String fileDirName,DataInputStream dis) throws IOException {
		File file = new File(fileDirName);
		if(file.exists()) {
			//get file size
			long fileSize = file.length();
			//send file size
			dos.writeBytes(fileSize + "\n");
			byte[] buffer = new byte[8192];
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			int count = 0;
			while((count = in.read(buffer)) > 0)
			dos.write(buffer, 0, count);
			in.close();
		}
			else{
				System.out.println("transfer error: " + fileDirName);
			}

	}
	
	

	public static void deleteCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF(); 
		System.out.println("myftp> "+ receivedMessage);
	}

	public static void lsCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();  
		System.out.println(receivedMessage); 
	}

	public static void cdCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF(); 
		System.out.println("myftp> "+ receivedMessage);
	}

	public static void mkdirCommand(DataInputStream dis) throws IOException {
		
		String receivedMessage = dis.readUTF(); 
		System.out.println("myftp> "+ receivedMessage);
	}

	public static void pwdCommand(DataInputStream dis) throws IOException { 
		String receivedMessage = dis.readUTF();  
		System.out.println(receivedMessage);
	}

	public static void quitCommand(DataInputStream dis) throws IOException { 
		String receivedMessage = dis.readUTF(); 
		System.out.println("Closing this connection .."); 
		//clientSocket.close();  
		System.out.println(receivedMessage); 
		//break; 
		//System.exit(0);
		isStopped = true;
	}	

	public static void invalidInput(DataInputStream dis) throws IOException { 
		String receivedMessage = dis.readUTF(); 
		System.out.println("myftp> "+ receivedMessage); 
	}

	public static void main(String args[]) throws IOException  {

		try { 
			Scanner scanner = new Scanner(System.in);
			//System.out.print("myftp> Enter machine name: ");
			//hostName = scanner.nextLine();
			hostName = args[0];
			//System.out.print("myftp> Enter port number: ");
			//String clientPortString = scanner.nextLine();
			String clientPortString = args[1];
			clientPort = Integer.valueOf(clientPortString); 
			
			Socket clientSocket = new Socket(hostName, clientPort);

			dis = new DataInputStream(clientSocket.getInputStream()); 
			dos = new DataOutputStream(clientSocket.getOutputStream()); 

			System.out.print("myftp> "); 
			System.out.println(dis.readUTF());

			while (!isStopped) {
				isStopped = false; 
				System.out.print("myftp> ");
				String sentMessage = scanner.nextLine();
				//String sentMessage = args[0];
				dos.writeUTF(sentMessage);

				String command = "";
				String fileDirName = "";

				if(sentMessage.contains(" ")) {
					String[] splittedCommand = sentMessage.split(" ");
					command = splittedCommand[0];
					fileDirName = splittedCommand[1];
				}else {
					command = sentMessage;
				}

				switch(command) {
				case GET_COMMAND:
					getCommand(fileDirName, dis);
					break;
				case PUT_COMMAND: 
					putCommand(fileDirName, dis); 
					break;
				case DELETE_COMMAND:
					deleteCommand(dis);
					break;
				case LS_COMMAND:
					lsCommand(dis);
					break;
				case CD_COMMAND:
					cdCommand(dis);
					break;
				case MKDIR_COMMAND:
					mkdirCommand(dis);
					break;
				case PWD_COMMAND:
					pwdCommand(dis);
					break;
				case QUIT_COMMAND:
					quitCommand(dis);
					break;
				default: 
					invalidInput(dis);
					break; 
				}  
			}  

			dis.close(); 
			dos.close(); 

		}catch(Exception e){ 
			e.printStackTrace(); 
		} 
	}
}