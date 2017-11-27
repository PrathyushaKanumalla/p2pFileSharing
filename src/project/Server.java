
package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map.Entry;

import project.Constants.ScanState;

public class Server extends Thread{

	public void run(){
		System.out.print("*The Server is running*"); 
		ServerSocket listener = null;

		try {
			listener = new ServerSocket(Integer.parseInt(Peer.getInstance().portNum));
			for (Integer clientPeerId : Peer.getInstance().neighbors.keySet()) {
				new Handler(listener.accept(),Peer.getInstance().neighbors.get(clientPeerId)).start();
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
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private RemotePeerInfo neighbor;

		public Handler(Socket connection, RemotePeerInfo neighbor) {
			this.connection = connection;
			this.neighbor = neighbor;
		}

		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				if (neighbor.peerId > Peer.getInstance().peerID) {
					neighbor.setState(ScanState.SERVER_START);
				}
				while(true)
				{
					//show the message to the user
					switch (neighbor.getState()) {
					case SERVER_START: {
						byte[] handShakeMsg = new byte[32];
						System.out.println("SERVER:- server waiting to read");
						in.read(handShakeMsg);
						if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
							System.out.printf("SERVER:- Received message: <%s> from client %d\n" ,new String(handShakeMsg), neighbor.peerId);
							System.out.println("SERVER:- Neighbor <" + neighbor.peerId +"> validated");
							//if not validated it does not proceed further
							neighbor.setState(ScanState.SERVER_RXVED_HAND_SHAKE);
						}
						break;
					} 
				case SERVER_RXVED_HAND_SHAKE: {
					System.out.println("SERVER:- Sent handShake message ");
					sendHandShake();
					neighbor.setState(ScanState.SERVER_SENT_HAND_SHAKE);
					break;
				}
				case SERVER_SENT_HAND_SHAKE:{
					//receive bit field message
					byte[] bitFieldMsg = new byte[9];
					in.read(bitFieldMsg);
					if (Peer.getInstance().validateBitFieldMsg(bitFieldMsg)) {
						//do something related to this
						System.out.println("SERVER:- Received bit field message ");
						neighbor.setState(ScanState.SERVER_RXVED_BIT_FIELD);
					}
					break;
				}
				case SERVER_RXVED_BIT_FIELD:{
					sendBitField();
					System.out.println("SERVER:- Sent Bit field ack");
					neighbor.setState(ScanState.SERVER_SENT_BIT_FIELD);
					break;
					// neighbor.setState(ScanState.)
				}
				case SERVER_SENT_BIT_FIELD :{
					byte[] interestedMsg = new byte[10];
					in.read(interestedMsg);
					System.out.println("SERVER:- Received interested messgae- " + new String(interestedMsg));
					neighbor.setState(ScanState.SERVER_RXVED_INTERESTED);
					break;
				}
				default: {
					/*String message = null;
							//receive the message sent from the client
							if ((message = (String)in.readObject()) != null ) {
								System.out.printf("SERVER:- Received message: <%s> from client %d\n" , message, neighbor.peerId);
								message = "Prathyusha Server Received " + message;
								//send MESSAGE back to the client
								sendMessage(message);
							}*/
					break;
				}
				}
			}
		}
		catch(IOException ioException){
			System.out.printf("Disconnect with Client %d\n" , neighbor.peerId);
		}/* catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				connection.close();
			}
			catch(IOException ioException){
				System.out.printf("Disconnect with Client %d\n" , neighbor.peerId);
			}
		}
	}

	private void sendHandShake() {
		String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
		sendMessage(handShake);
	}

	//send a message to the output stream
	public void sendMessage(String msg)
	{
		try{
			out.write(msg.getBytes());
			out.flush();
			System.out.printf("SERVER:- Sent message:<%s> to Client %d\n" ,msg, neighbor.peerId);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	private void sendBitField(){
		String bitField = "server123";
		sendMessage(bitField);
	}

}

}
