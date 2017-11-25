package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import project.Constants.ScanState;

public class Client extends Thread {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	RemotePeerInfo neighbor;

	public Client(RemotePeerInfo value) {
		this.neighbor = value;
	}

	public void run()
	{
		try{
			//create a socket to connect to the server
			System.out.println("*The Client is running*");
			requestSocket = new Socket(neighbor.peerAddress, new Integer(neighbor.peerPort));
			System.out.printf("*My Client Connected to %s in port %s*", neighbor.peerAddress, neighbor.peerPort);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			//sending handshake message

			//handShake = "handshakeSent";
			//sendMessage(handShake);
			//get Input from standard input
			//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			while(true)
			{
				switch (neighbor.getState()) {
					case START: {
						if (neighbor.peerId < Peer.getInstance().peerID) {
							sendHandShake();
							neighbor.setState(ScanState.SENT_HAND_SHAKE);
						} //else states are modified by server
						break;
					}	
					case SENT_HAND_SHAKE: {
						byte[] handShakeMsg = new byte[32];
						if ((in.read(handShakeMsg)) > 0) {
							//hand shake msg received
							byte[] peerId =  Arrays.copyOfRange(handShakeMsg, 28, 32);
							if (Peer.getInstance().neighbors.containsKey(peerId)) {
								System.out.println("CLIENT:- Neighbor <" + peerId + "> validated");
								//if not validated it does not proceed further
								neighbor.setState(ScanState.RXVED_HAND_SHAKE);
							}
						} //else continues to wait
						break;
					}
					default: {
						//Send the sentence to the server
						String message = "default_behavior_pratServer";
						sendMessage(message);
						//Receive the upperCase sentence from the server
						message = (String)in.readObject();
						//show the message to the user
						System.out.printf("CLIENT:- Received the message: <%s>\n", message);
						break;
					}
				}
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

	private void sendHandShake() {
		String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
		sendMessage(handShake);
	}

	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg.getBytes());
			out.flush();
			System.out.printf("CLIENT:- Message<"+msg+"> sent to %s:%s\n", neighbor.peerAddress, neighbor.peerPort);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

}
