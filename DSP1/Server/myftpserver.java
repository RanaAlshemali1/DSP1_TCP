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
class ClientHandler extends Thread {

	DataInputStream dis;
	DataOutputStream dos;
	Socket socket;
	static boolean isStopped = false;
	String receivedMessage = "";
	String returnedMessage = "";
	String currentDir = System.getProperty("user.dir");

	// List of commands: get put delete ls cd mkdir pwd quit
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

	public static final String INVALID_INPUT = "Invalid input ..";

	// 8 methods, each for one of the 8 commands
	// --------------------- GET FILE TO USER - SEND TO USER
	// ------------------------
	public void getCommand(String fileDirName, DataOutputStream dos) throws IOException {
		File file = new File(fileDirName);
		// get file size
		if (file.exists()) {
			returnedMessage = "getCommand";
			dos.writeUTF(returnedMessage);
			long fileSize = file.length();
			double sizeOfFiles = 8.0 * 1024;
			int chunks = (int) Math.ceil(fileSize / sizeOfFiles);
			// send file size
			dos.writeUTF(String.valueOf(chunks));
			byte[] buffer = new byte[(int) sizeOfFiles];
			try (FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis)) {
				int count = 0;
				while ((count = bis.read(buffer)) > 0)
					dos.write(buffer, 0, count);
			}
			returnedMessage = fileDirName + " is downloaded successfully";
			dos.writeUTF(returnedMessage);
			System.out.println(returnedMessage);
		} else {
			returnedMessage = "File does not exist";
			dos.writeUTF(returnedMessage);
			System.out.println(returnedMessage);
		}
		
	}

	// ------------------- PUT FILE FROM USER - RECEIVE FROM
	// USER--------------------
	public void putCommand(String fileDirName, DataOutputStream dos) throws IOException {
		String receivedMessage = dis.readUTF();
		if (receivedMessage.equals("putCommand")) {
			String chunks = dis.readUTF();
			FileOutputStream f = new FileOutputStream(new File(fileDirName));
			int count = 0;
			byte[] buffer = new byte[8 * 1024];
			int chunk_num = 0;
			int chunksInt = Integer.parseInt(chunks);
			while (chunk_num < chunksInt) {
				count = dis.read(buffer);
				f.write(buffer, 0, count);
				chunk_num += 1;
			}

			returnedMessage = fileDirName + " is uploaded successfully";
			System.out.println(returnedMessage);
			
			dos.writeUTF(returnedMessage);
		} else {
			System.out.println( receivedMessage);
		}

	}

	public void deleteCommand(String fileDirName, DataOutputStream dos) throws IOException {
		File file = new File(fileDirName);
		if (file.exists()) {
			boolean result = file.delete();
			if (result) {
				returnedMessage = "Successfully deleted: " + fileDirName;
			} else {
				returnedMessage = "Failed deleting: " + fileDirName;
			}
		} else {
			returnedMessage = "File does not exist";
		}
		System.out.println(returnedMessage); 
		dos.writeUTF(returnedMessage);

	}

	public void lsCommand(DataOutputStream dos) throws IOException {

		currentDir = System.getProperty("user.dir");
		File folder = new File(currentDir);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null && listOfFiles.length == 0) {
			dos.writeUTF(LS_NO_SUBDIR);
		} else {
			String filesList = "";
			for (int i = 0; i < listOfFiles.length; i++) {
				if (i == 0) {
					filesList = listOfFiles[i].getName();
				} else {
					filesList += " \n" + listOfFiles[i].getName();
				}
			}
			dos.writeUTF(filesList); 
		}
	}

	public void cdCommand(String fileDirName, DataOutputStream dos) throws IOException {
		currentDir = System.getProperty("user.dir");
		String splitDirectories[] = currentDir.split("/");
		if (fileDirName.equals("..")) {  
			// cd .. select substring of currentDir: currentDir -last file - '/'
			currentDir = currentDir.substring(0,
					currentDir.length() - splitDirectories[splitDirectories.length - 1].length() - 1);
			// cd.
			System.setProperty("user.dir", currentDir); 
			dos.writeUTF("Directory is changed to " + currentDir);
			System.out.println("Directory is changed to " + currentDir);
			
		} else if (fileDirName.equals(".")) {
			currentDir = currentDir;
			// cd
			System.setProperty("user.dir", currentDir); 
			dos.writeUTF("Directory is changed to " + currentDir);
			System.out.println("Directory is changed to " + currentDir);
			
		} else if (fileDirName.contains("/") && !(fileDirName.equals("../"))) {
			File f = new File(fileDirName);
			if (f.exists() && f.isDirectory()) {
				currentDir = fileDirName;
				System.setProperty("user.dir", currentDir); 
				dos.writeUTF("Directory is changed to " + currentDir);
				System.out.println("Directory is changed to " + currentDir);
				
			} else {
				dos.writeUTF("Directory does not exist");
				System.out.println(currentDir + " does not exist");
			}
		} else {
			File f = new File(currentDir + "/" +fileDirName);
			if (f.exists() && f.isDirectory() && !(fileDirName.equals("../"))) {
				currentDir = currentDir + "/" + fileDirName;
				System.setProperty("user.dir", currentDir); 
				dos.writeUTF("Directory is changed to " + currentDir);
				System.out.println("Directory is changed to " + currentDir);
			} else {
				dos.writeUTF("Directory does not exist");
				System.out.println( currentDir + " does not exist");
			}
		}

	}

	public void mkdirCommand(String fileDirName, DataOutputStream dos) throws IOException {

		currentDir = System.getProperty("user.dir");
		File file = new File(currentDir + "/" + fileDirName);
		if (!file.exists()) {
			boolean result = file.mkdir();
			if (result) {
				returnedMessage = "Successfully created: " + fileDirName;
			} else {
				returnedMessage = "Failed creating: " + fileDirName;
			}
		} else {
			returnedMessage = "File already exists";
		}
		System.out.println(returnedMessage); 
		dos.writeUTF(returnedMessage);

	}

	public void pwdCommand(DataOutputStream dos) throws IOException {
		currentDir = System.getProperty("user.dir");
		dos.writeUTF(currentDir); 
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
				// dos.writeUTF("skip");
				receivedMessage = dis.readUTF();
				String command = "";
				String fileDirName = "";

				if (receivedMessage.contains(" ")) {
					String[] splittedCommand = receivedMessage.split(" ");
					command = splittedCommand[0];
					fileDirName = splittedCommand[1];
				}

				else {
					command = receivedMessage;
				}
				System.out.println("myftpserver> " + receivedMessage);

				isStopped = false;

				switch (command) {
				case GET_COMMAND:
					getCommand(fileDirName, dos);
					break;
				case PUT_COMMAND:
					putCommand(fileDirName, dos);
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
				System.out.println("myftpserver> ");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			this.dis.close();
			this.dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

public class myftpserver {

	static int serverPort = 8080;
	static ServerSocket serverSocket = null;
	static boolean isStopped = false;

	public static void main(String args[]) throws IOException {

		// Scanner scanner = new Scanner(System.in);
		// System.out.print("myftpserver> Enter port number: ");
		// String clientPortString = scanner.nextLine();
		String clientPortString = args[0];
		serverPort = Integer.valueOf(clientPortString);
		serverSocket = new ServerSocket(serverPort);
		System.out.println("myftpserver> Waiting for a Client to connect ..");
		// continuously listen for clients requests
		while (!isStopped) {
			Socket socket = null;

			try {
				// incoming client requests are received using this socket object
				socket = serverSocket.accept();
				System.out.println("myftpserver> A new client is connected: " + socket);

				// input and output streams
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

				System.out.println("myftpserver> This Client is being assigned to a new thread");

				Thread thread = new ClientHandler(socket, dis, dos);
				thread.start();

			} catch (Exception e) {
				socket.close();
				System.out.println("Error.. can't establish a connection ..");
				e.printStackTrace();
			}

		}
	}
}
