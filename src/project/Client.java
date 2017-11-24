package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends Thread {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	String handShake;
	String neighborAddr;
	String neighborPort;
	
	public Client(String neighborAddr, String neighborPort) {
		this.neighborAddr = neighborAddr;
		this.neighborPort = neighborPort;
	}

	public void run()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket(neighborAddr, new Integer(neighborPort));
			System.out.printf("Connected to %s in port %s", neighborAddr, neighborPort);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			//sending handshake message
			
			handShake = "handshakeSent";
			sendMessage(handShake);
			System.out.printf("handshake message sent", neighborAddr, neighborPort);
			//get Input from standard input
			//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			while(true)
			{
				//System.out.print("Hello, please input a sentence: ");
				//read a sentence from the standard input
				//message = bufferedReader.readLine();
				//Send the sentence to the server
				message = "Prathyusha";
				sendMessage(message);
				//Receive the upperCase sentence from the server
				MESSAGE = (String)in.readObject();
				//show the message to the user
				//System.out.println("Receive message: " + MESSAGE);
			}
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		} 
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
}
