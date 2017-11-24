package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Server extends Thread{

	private int portNum;
	private Set<Integer> neighbors;
	
	Server (String portNum, Set<Integer> set) {
		this.portNum = new Integer(portNum);
		this.neighbors = set;		
	}

	public void run(){
		System.out.println("The server is running."); 
		ServerSocket listener = null;

		try {
			listener = new ServerSocket(portNum);
			for (Integer clientPeerId : neighbors) {
				new Handler(listener.accept(),clientPeerId).start();
				System.out.println("Client "  + clientPeerId + " is connected!");
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
		private String MESSAGE;    //uppercase message send to the client
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
						System.out.println("Receive message: " + message + " from client " + clientNum);
						//Capitalize all letters in the message
						MESSAGE = message.toUpperCase();
						//send MESSAGE back to the client
						sendMessage("sridhar's server");
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + clientNum);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + clientNum);
				}
			}
		}

		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				//System.out.println("Send message: " + msg + " to Client " + clientNum);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}

}
