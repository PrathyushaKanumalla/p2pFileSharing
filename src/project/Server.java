
package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;

public class Server extends Thread{

	private int portNum;
	
	Server (String portNum) {
		this.portNum = new Integer(portNum);
	}

	public void run(){
		System.out.print("*The Server is running*"); 
		ServerSocket listener = null;

		try {
			listener = new ServerSocket(portNum);
			for (Integer clientPeerId : Peer.getInstance().neighbors.keySet()) {
				new Handler(listener.accept(),clientPeerId).start();
				System.out.println("*My Server Connected to "  + clientPeerId + " *");
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}  finally {
			try {
				listener.close();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}

	}

	/**
	 * A handler thread class.  Handlers are spawned from the listening
	 * loop and are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {
		private String message;    //message received from the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int clientNum;
		
		public Handler(Socket connection, int clientNum) {
			this.connection = connection;
			this.clientNum = clientNum;
		}
		
		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				try{
					while(true)
					{
						//receive the message sent from the client
						message = (String)in.readObject();
						//show the message to the user
						System.out.printf("SERVER:- Received message: <%s> from client %s\n" ,message, clientNum);
						//send MESSAGE back to the client
						sendMessage("Prathyusha Server Received");
						for (Entry<Integer, RemotePeerInfo> peer : Peer.getInstance().neighbors.entrySet()) {
							peer.getValue().flag = true;
						}
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.printf("Disconnect with Client %s\n" ,clientNum);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.printf("Disconnect with Client %s\n" , clientNum);
				}
			}
		}

		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				System.out.printf("SERVER:- Sent message:<%s> to Client %s\n" ,msg, clientNum);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}

}
