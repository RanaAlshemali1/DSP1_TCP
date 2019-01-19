import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class myftp {

	static int clientPort = 8080;
	static Socket clientSocket = null;
	static String hostName = "localhost";
	static boolean isStopped = false;
	
	public static final String QUIT_COMMAND = "quit";
	
	public static void main(String args[]) throws IOException  {
		
		try {
			System.out.println("1");
			Scanner scanner = new Scanner(System.in);
			System.out.print("myftp> Enter port number: ");
			String clientPortString = scanner.nextLine();
			clientPort = Integer.valueOf(clientPortString); 
			Socket clientSocket = new Socket(hostName, clientPort);
			
			DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); 
	        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream()); 
	         
	         while (!isStopped) {
	        	 isStopped = false;
	        	 System.out.println("2");
	        	 System.out.print("myftp> "); 
	        	 System.out.println(dis.readUTF());
	        	 System.out.print("myftp> "); 
	        	 String sentMessage = scanner.nextLine();
	        	 dos.writeUTF(sentMessage);
	        	 
	        	 if (sentMessage.equals(QUIT_COMMAND)) {
	        		 System.out.println("myftp> Closing this connection : " + clientSocket); 
	        		 //clientSocket.close(); 
	                 System.out.println("myftp> Connection closed!"); 
	                 //break; 
	        		 //System.exit(0);
	                 isStopped = true;
	        	 }
	        	 
	        	 System.out.println("3");
	        	 /*String receivedMessage = dis.readUTF(); 
	        	 if(!(receivedMessage.equals("skip"))){
	        		 System.out.print("myftp> "); 
	        		 System.out.println("**********"); 
	        	 } */
	         }
	         //System.out.println("4");
	         //scanner.close(); 
	         
	         dis.close(); 
	         dos.close(); 
	         
		}catch(Exception e){ 
            e.printStackTrace(); 
        } 
	}
}
