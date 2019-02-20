
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;

public class myftpserver {

	static int nport = 8080;
	static int tport = 8000;
	static int commandIDSeed = 100;
	static HashMap<Integer, Boolean> commandIDList = new HashMap<Integer, Boolean>();
	static ServerSocket nServerSocket = null;
	static ServerSocket tServerSocket = null;
	static boolean isStopped = false;

	public static void main(String args[]) throws IOException {

		String nPortString = args[0];
		String tPortString = args[1];
		nport = Integer.valueOf(nPortString);
		tport = Integer.valueOf(tPortString);

		nServerSocket = new ServerSocket(nport);
		tServerSocket = new ServerSocket(tport);
 

		System.out.println("myftpserver> Waiting for a Client to connect ..");
		// continuously listen for clients requests
		while (!isStopped) {
			Socket nSocket = null;

			try {
				// incoming client requests are received using this socket object
				nSocket = nServerSocket.accept();
				System.out.println("myftpserver> A new client is connected: " + nSocket);

				// input and output streams
				DataInputStream dis = new DataInputStream(nSocket.getInputStream());
				DataOutputStream dos = new DataOutputStream(nSocket.getOutputStream());

				System.out.println("myftpserver> This Client is being assigned to a new thread");

				Thread nThread = new ClientHandler(nSocket, dis, dos);
				nThread.start();

				Thread tThread = new clinetTerminate(tport, tServerSocket);
				tThread.start();

			} catch (Exception e) {
				nSocket.close();
				System.out.println("Error.. can't establish a connection ..");
				e.printStackTrace();
			}

		}
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// ****************************************************************************************

	// the terminate thread - keep listening for incoming clients connecting via tport
	static class clinetTerminate extends Thread {

		int tport;
		ServerSocket tServerSocket;

		public clinetTerminate(int tport, ServerSocket tServerSocket) {
			this.tport = tport;
			this.tServerSocket = tServerSocket;
		}

		@Override
		public void run() {
			try {
				System.out.println("myftpserver> Terminate Started");

				while (true) {
					Socket tSocket = tServerSocket.accept();
					DataInputStream dis = new DataInputStream(tSocket.getInputStream());
					DataOutputStream dos = new DataOutputStream(tSocket.getOutputStream());
					// get the word "terminate" from client
					String command = dis.readUTF();
					// get the command ID from the client to terminate it
					int commandID = dis.readInt(); 
					
					// check if command Id is in the list
					if (commandIDList.containsKey(commandID)) {
						// if found, remove it
						commandIDList.remove(commandID);
						System.out.println("Command ID is found");
					} else if (!commandIDList.containsKey(commandID)) {
						System.out.println("Command ID is NOT found");
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// ****************************************************************************************

	// Each client will be handled here for multithreading purposes
	static class ClientHandler extends Thread {

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
		// --------------------- GET FILE TO USER - SEND TO USER ------------------------
		public void getCommand(String fileDirName, DataOutputStream dos) throws IOException {

			File file = new File(fileDirName);
			// get file size
			if (file.exists()) {
				String returnedMessage = "getCommand";
				dos.writeUTF(returnedMessage);

				// generate a command Id to send to the client
				int commandID = commandIDSeed++;
				commandIDList.put(commandID, true);
				System.out.println("Get command ID is: " + commandID);
				dos.writeInt(commandID);

				// -----------------------  get file size ---------------------
				// ----------- START: NEW METHOD OF SENDING FILES -----------
				FileInputStream fin = new FileInputStream(file);
				long fileSize = file.length();
				int bufferSize = 0;

				int numOfIterations = 0;
				int remaining = 0;
				boolean needRemaining = false;

				if (fileSize < 1024) {
					bufferSize = (int) fileSize;
					numOfIterations = 1;
				} else {
					numOfIterations = (int) (fileSize / 1024);
					remaining = (int) (fileSize % 1024);
					bufferSize = 1024;
				}

				if ((int) (fileSize % 1024) != 0) {
					needRemaining = true;
				} else {
					needRemaining = false;
				}

				byte[] buffer = new byte[bufferSize];
				dos.writeLong(fileSize);

				for (int i = 1; i <= numOfIterations + 1; i++) {
					// if command is not terminated
					if (commandIDList.containsKey(commandID)) { 
						if (i <= numOfIterations) {
							fin.read(buffer);
							dos.write(buffer);
						} else if (i == numOfIterations + 1 && needRemaining) {
							byte[] buffer0 = new byte[remaining];
							fin.read(buffer0);
							dos.write(buffer0);
						} 
						
					} else {
						// if terminated, clear the dos buffer
						dos.writeUTF("\n");
						dos.flush();
						break;
					}
				}
				
				fin.close(); 
				// ----------- END: NEW METHOD OF SENDING FILES -----------
				
				int x = dis.readInt();
				//System.out.println(x);

				// --------------------------------------------
				if (commandIDList.containsKey(commandID)) {
					returnedMessage = fileDirName + " is downloaded successfully";
					commandIDList.remove(commandID);
					dos.writeUTF(returnedMessage);
					System.out.println(returnedMessage);
				} else {
					// if command is terminated, delete the partially existing file, and remove the command ID from list
					returnedMessage = fileDirName + " is terminated successfully";
					commandIDList.remove(commandID);
					System.out.println(returnedMessage);
					dos.writeUTF(returnedMessage);
				}

			} else {
				returnedMessage = "File does not exist";
				dos.writeUTF(returnedMessage);
				System.out.println(returnedMessage);
			}

		}

		// ------------------- PUT FILE FROM USER - RECEIVE FROM USER--------------------
		public void putCommand(String fileDirName, DataOutputStream dos, DataInputStream dis) throws IOException {
			String receivedMessage = dis.readUTF();
			if (receivedMessage.equals("putCommand")) {

				// generate a command Id to send to the client
				int commandID = commandIDSeed++;
				commandIDList.put(commandID, true);
				System.out.println("Put command ID is: " + commandID);
				dos.writeInt(commandID);
				// --------------------------------------------
				// dos.flush();
				
				// ----------- START: NEW METHOD OF RECEIVING FILES -----------
				File file = new File(fileDirName);
				Files.deleteIfExists(file.toPath());
				FileOutputStream fos = new FileOutputStream(file);

				int readBytes = 0;
				long fileSize = dis.readLong();
				int bufferSize = 0;

				int numOfIterations = 0;
				int remaining = 0;
				boolean needRemaining = false;

				if (fileSize < 1024) {
					bufferSize = (int) fileSize;
					numOfIterations = 1;
				} else {
					numOfIterations = (int) (fileSize / 1024);
					remaining = (int) (fileSize % 1024);
					bufferSize = 1024;
				}

				if ((int) (fileSize % 1024) != 0) {
					needRemaining = true;
				} else {
					needRemaining = false;
				}

				byte[] buffer = new byte[bufferSize];

				for (int i = 1; i <= numOfIterations + 1; i++) { 
						if (i <= numOfIterations) {
							int x = dis.read(buffer);
							if (x < bufferSize) { 
								break;
							}
							fos.write(buffer);
						} else if (i == numOfIterations + 1 && needRemaining) {
							byte[] buffer0 = new byte[remaining];
							readBytes = dis.read(buffer0);
							fos.write(buffer0);
						}  
				}

				fos.close();
				dos.flush();

				// ----------- END: NEW METHOD OF RECEIVING FILES -----------
				// --------------------------------------------
				
				dos.writeInt(10);
				
				if (commandIDList.containsKey(commandID)) {
					returnedMessage = fileDirName + " is uploaded successfully";
					commandIDList.remove(commandID);
					System.out.println(returnedMessage);
					dos.writeUTF(returnedMessage);
				} else {
					// if command is terminated, delete the partially existing file, and remove the command ID from list
					file.delete();
					returnedMessage = fileDirName + " is terminated successfully";
					commandIDList.remove(commandID);
					System.out.println(returnedMessage);
					dos.writeUTF(returnedMessage);
				}

			} else {
				System.out.println(receivedMessage);
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
				File f = new File(currentDir + "/" + fileDirName);
				if (f.exists() && f.isDirectory() && !(fileDirName.equals("../"))) {
					currentDir = currentDir + "/" + fileDirName;
					System.setProperty("user.dir", currentDir);
					dos.writeUTF("Directory is changed to " + currentDir);
					System.out.println("Directory is changed to " + currentDir);
				} else {
					dos.writeUTF("Directory does not exist");
					System.out.println(currentDir + " does not exist");
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
			this.dos.close();
			this.dis.close();
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
					dos.flush();
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
						putCommand(fileDirName, dos, dis);
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

}
