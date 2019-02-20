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
import java.util.HashMap;
import java.util.Scanner;

public class myftp {

	static HashMap<Integer, Boolean> commandIDList = new HashMap<Integer, Boolean>();

	static int nport = 8080;
	static int tport = 8000;
	static Socket nSocket = null;
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

	public static final String TERMINATE_COMMAND = "terminate";

	public static void main(String args[]) throws IOException {

		try {
			Scanner scanner = new Scanner(System.in);
			// System.out.print("myftp> Enter machine name: ");
			// hostName = scanner.nextLine();
			hostName = args[0];
			// System.out.print("myftp> Enter port number: ");
			// String clientPortString = scanner.nextLine();
			String nPortString = args[1];
			String tPortString = args[2];
			nport = Integer.valueOf(nPortString);
			tport = Integer.valueOf(tPortString);

			// Socket clientSocket = new Socket(hostName, nPort);
			nSocket = new Socket(hostName, nport);

			dis = new DataInputStream(nSocket.getInputStream());
			dos = new DataOutputStream(nSocket.getOutputStream());

			System.out.print("myftp N1> ");
			System.out.println(dis.readUTF());

			while (!isStopped) {
				dos.flush();
				isStopped = false;
				System.out.print("myftp N> ");
				String sentMessage = scanner.nextLine();
				// String sentMessage = args[0];

				String arg1 = "";
				String arg2 = "";
				String arg3 = "";

				if (sentMessage.contains("&")) {
					String[] splittedCommand = sentMessage.split(" ");
					arg1 = splittedCommand[0];
					arg2 = splittedCommand[1];
					arg3 = splittedCommand[2];
				} else if (sentMessage.contains(" ")) {
					String[] splittedCommand = sentMessage.split(" ");
					arg1 = splittedCommand[0];
					arg2 = splittedCommand[1];
				} else {
					arg1 = sentMessage;
				}

				switch (arg1) {
				case GET_COMMAND:
					if (arg3.equals("&")) {
						Thread tThread = new Thread(new getPutThread(arg1, arg2, sentMessage, nSocket));
						tThread.start();
					} else {
						getCommand(arg2, dis, dos, sentMessage);
					}
					break;
				case PUT_COMMAND:
					if (arg3.equals("&")) {
						Thread tThread = new Thread(new getPutThread(arg1, arg2, sentMessage, nSocket));
						tThread.start();
					} else {
						putCommand(arg2, dis, dos, sentMessage);
					}
					break;
				case DELETE_COMMAND:
					dos.writeUTF(sentMessage);
					deleteCommand(dis);
					break;
				case LS_COMMAND:
					dos.writeUTF(sentMessage);
					lsCommand(dis);
					break;
				case CD_COMMAND:
					dos.writeUTF(sentMessage);
					cdCommand(dis);
					break;
				case MKDIR_COMMAND:
					dos.writeUTF(sentMessage);
					mkdirCommand(dis);
					break;
				case PWD_COMMAND:
					dos.writeUTF(sentMessage);
					pwdCommand(dis);
					break;
				case QUIT_COMMAND:
					dos.writeUTF(sentMessage);
					quitCommand(dis);
					break;
				case TERMINATE_COMMAND:
					Socket tSocket = new Socket(hostName, tport);
					DataOutputStream tDos = new DataOutputStream(tSocket.getOutputStream());
					System.out.println("myftp T> In Terminate");
					tDos.writeUTF("terminate");
					// send the command ID to the server
					int commandId = Integer.parseInt(arg2);
					commandIDList.remove(commandId);
					tDos.writeInt(commandId);
					tDos.flush();
					tDos.close();
					tSocket.close();
					System.out.println("myftp T> T DONE");
					break;
				default:
					dos.writeUTF(sentMessage);
					invalidInput(dis);
					break;
				}
			}

		} catch (Exception e) {
			System.out.println("Error while connecting to the server");
			e.printStackTrace();
		}
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// ****************************************************************************************

	// 8 methods, each for one of the 8 commands
	// --------------------- GET FILE FROM SERVER - RECEIVE FROM SERVER ------------------------
	public static void getCommand(String fileDirName, DataInputStream dis, DataOutputStream dos, String sentMessage)
			throws IOException {
		dos.writeUTF(sentMessage);
		String receivedMessage = dis.readUTF();
		if (receivedMessage.equals("getCommand")) {

			// get the command ID from the server
			int commandID = dis.readInt();
			commandIDList.put(commandID, true);
			System.out.println("Get command ID is: " + commandID);

			// --------------------------------------------

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
					dis.read(buffer);
					fos.write(buffer);
				} else if (i == numOfIterations + 1 && needRemaining) {
					byte[] buffer0 = new byte[remaining];
					readBytes = dis.read(buffer0);
					fos.write(buffer0);

				}
			}

			fos.close();
			// ----------- END: NEW METHOD OF RECEIVING FILES -----------
			
			// --------------------------------------------
			dos.writeInt(12);

			receivedMessage = dis.readUTF();
			System.out.println(receivedMessage);
			commandIDList.remove(commandID);

		} else {

			System.out.println(receivedMessage);
		}
	}

	// ------------------- PUT FILE TO SERVER - SEND TO SERVER--------------------
	public static void putCommand(String fileDirName, DataInputStream dis, DataOutputStream dos, String sentMessage)
			throws IOException {
		dos.writeUTF(sentMessage);
		File file = new File(fileDirName);
		if (file.exists()) {
			String returnedMessage = "putCommand";
			dos.writeUTF(returnedMessage);

			// get the command ID from the server
			int commandID = dis.readInt();
			commandIDList.put(commandID, true);
			System.out.println("Put command ID is: " + commandID);

			// get file size
			// --------------------------------------------
			
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
				if (i <= numOfIterations) {
					fin.read(buffer);
					dos.write(buffer);
				} else if (i == numOfIterations + 1 && needRemaining) {
					byte[] buffer0 = new byte[remaining];
					fin.read(buffer0);
					dos.write(buffer0);
				}
			}
			fin.close();
			// ----------- START: NEW METHOD OF SENDING FILES -----------
			
			int x = dis.readInt();
			//System.out.println(x);
			// --------------------------------------------
			
			String receivedMessage = dis.readUTF();
			System.out.println(receivedMessage);
			commandIDList.remove(commandID);
			
		} else {
			String returnedMessage = "File does not exist";
			System.out.println(returnedMessage);
			dos.writeUTF(returnedMessage);
		}

	}

	public static void deleteCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	public static void lsCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	public static void cdCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	public static void mkdirCommand(DataInputStream dis) throws IOException {

		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	public static void pwdCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	public static void quitCommand(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println("Closing this connection ..");
		// clientSocket.close();
		System.out.println(receivedMessage);
		// dis.close(); //******************************
		// dos.close();
		nSocket.close();

		isStopped = true;
	}

	public static void invalidInput(DataInputStream dis) throws IOException {
		String receivedMessage = dis.readUTF();
		System.out.println(receivedMessage);
	}

	// ****************************************************************************************
	// ****************************************************************************************
	// ****************************************************************************************

	// the & thread: when user add & to the command, the get and put are sent and received in a new thread
	static class getPutThread extends Thread {

		DataInputStream nDis;
		DataOutputStream nDos;
		String command;
		String fileName;
		String sentMessage;
		Socket tSocket;

		public static final String GET_COMMAND = "get";
		public static final String PUT_COMMAND = "put";

		public getPutThread(String command, String fileName, String sentMessage, Socket tSocket) {

			this.command = command;
			this.fileName = fileName;
			this.sentMessage = sentMessage;
			this.tSocket = tSocket;
			//System.out.println("In Const");
		}

		@Override
		public void run() {
			try {
				nDis = new DataInputStream(tSocket.getInputStream());
				nDos = new DataOutputStream(tSocket.getOutputStream());

				switch (command) {
				case GET_COMMAND:
					getCommand(fileName, nDis, nDos, sentMessage);
					break;
				case PUT_COMMAND:
					putCommand(fileName, nDis, nDos, sentMessage);
					break;
				default:
					invalidInput(nDis);
					break;
				}
				// dos.flush();
				// dos.close();
				// dis.close();
				// tSocket.close();
				System.out.print("myftp &> ");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// --------------------- GET FILE FROM SERVER - RECEIVE FROM SERVER
		// ------------------------
		public static void getCommand(String fileDirName, DataInputStream dis, DataOutputStream dos, String sentMessage)
				throws IOException {
			dos.writeUTF(sentMessage);
			String receivedMessage = dis.readUTF();
			if (receivedMessage.equals("getCommand")) {

				// get the command ID from the server
				int commandID = dis.readInt();
				commandIDList.put(commandID, true);
				System.out.println("Get command ID is: " + commandID);
				System.out.print("myftp g&> ");

				// --------------------------------------------
				//dos.flush();
				
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
				
				dos.writeInt(13);
				
				if (commandIDList.containsKey(commandID)) {
					receivedMessage = dis.readUTF();
					System.out.println(receivedMessage);
					commandIDList.remove(commandID);
				} else {
					file.delete();
					receivedMessage = dis.readUTF();
					System.out.println(receivedMessage);
					commandIDList.remove(commandID);
				}

			} else {

				System.out.println(receivedMessage);
			}

		}

		// ------------------- PUT FILE TO SERVER - SEND TO SERVER--------------------
		public static void putCommand(String fileDirName, DataInputStream dis, DataOutputStream dos, String sentMessage)
				throws IOException {
			dos.writeUTF(sentMessage);
			File file = new File(fileDirName);
			if (file.exists()) {
				String returnedMessage = "putCommand";
				dos.writeUTF(returnedMessage);

				// get the command ID from the server
				int commandID = dis.readInt();
				commandIDList.put(commandID, true);
				System.out.println("Put command ID is: " + commandID);
				System.out.print("myftp p&> ");

				// get file size --------------------------------------------
				//dos.flush();
				
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
						dos.writeUTF("\n");
						dos.flush();
						break;
					}
				}
				fin.close();
				dos.flush(); 
				// ----------- END: NEW METHOD OF SENDING FILES -----------

				int x = dis.readInt();
				//System.out.println(x);
				// --------------------------------------------
				
				if (commandIDList.containsKey(commandID)) {
					String receivedMessage = dis.readUTF();
					System.out.println(receivedMessage);
					commandIDList.remove(commandID);
				} else {

					String receivedMessage = dis.readUTF();
					System.out.println(receivedMessage);
					commandIDList.remove(commandID);
				}

			} else {
				String returnedMessage = "File does not exist";
				System.out.println(returnedMessage);
				dos.writeUTF(returnedMessage);
			}

		}

		public static void invalidInput(DataInputStream dis) throws IOException {
			String receivedMessage = dis.readUTF();
			System.out.println(receivedMessage);
		}
	}

}
