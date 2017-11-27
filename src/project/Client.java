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
			if (neighbor.peerId < Peer.getInstance().peerID) {
				neighbor.setState(ScanState.CLIENT_START);
			}

			while(true)
			{
				switch (neighbor.getState()) {
				case CLIENT_START: {
					System.out.println("CLIENT:- the state is START");
					System.out.println("CLIENT:- sent handshake msg");
					sendHandShake();
					neighbor.setState(ScanState.CLIENT_SENT_HAND_SHAKE);
					//else states are modified by server
					break;
				}	
				case CLIENT_SENT_HAND_SHAKE: {
					byte[] handShakeMsg = new byte[32];
					in.read(handShakeMsg);
					//hand shake msg received
					if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
						System.out.println("CLIENT:- Neighbor <" + neighbor.peerId +"> validated");
						//if not validated it does not proceed further
						neighbor.setState(ScanState.CLIENT_RXVED_HAND_SHAKE);
					} //else continues to wait
					break;
				}
				case CLIENT_RXVED_HAND_SHAKE:{
					if (Peer.getInstance().hasCompleteFile()) {
						sendBitField();
						neighbor.setState(ScanState.CLIENT_SENT_BIT_FIELD);
					}
					break;
				}
				case CLIENT_SENT_BIT_FIELD:{
					//if bit field message sent wait for interested/not interested msg
					byte[] interestedMsg = new byte[10];
					in.read(interestedMsg);
					System.out.println("CLIENT:- Received interested messgae- " + new String(interestedMsg));
					neighbor.setState(ScanState.CLIENT_RXVED_INTERESTED);
					
					/*byte[] bitFieldMsg = new byte[9];
					in.read(bitFieldMsg);
					if (Peer.getInstance().validateBitFieldMsg(bitFieldMsg)) {
						//do something related to this
						System.out.println("CLIENT:- Received Neighbor Bit field");
						neighbor.setState(ScanState.CLIENT_RXVED_BIT_FIELD);
					}*/
					break;
				}
				case CLIENT_RXVED_BIT_FIELD:{
					sendInterested();
					neighbor.setState(ScanState.CLIENT_SENT_INTERESTED);
					break;

				}
				case CLIENT_SENT_INTERESTED:{
					//do something with interested.
				}
				default: {
					/*//Send the sentence to the server
						String message = "default_behavior_pratServer";
						sendMessage(message);
						//Receive the upperCase sentence from the server
						message = (String)in.readObject();
						//show the message to the user
						System.out.printf("CLIENT:- Received the message: <%s>\n", message);*/
					break;
				}
				}
			}
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		/*catch ( ClassNotFoundException e ) {
			System.err.println("Class not found");
		}*/ 
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

	private void sendBitField(){
		String bitField = "client123";
		sendMessage(bitField);
	}

	private void sendInterested(){
		String interestedMsg = "dummy text";
		sendMessage(interestedMsg);
	}
	void sendMessage(String msg)
	{
		try{
			out.write(msg.getBytes());
			out.flush();
			System.out.printf("CLIENT:- Message<"+msg+"> sent to %s:%s\n", neighbor.peerAddress, neighbor.peerPort);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	public byte[] getMessage(String messageType){
		byte[] res = messageType.getBytes();
		int length =messageType.length();
		//		byte[] temp = ByteBuffer.allocate(4).putInt(length).array();
		byte[] bytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte)(length >>> (i * 8));
		}

		byte[] combined = new byte[bytes.length + res.length];
		System.arraycopy(bytes,0,combined,0         ,bytes.length);
		System.arraycopy(res,0,combined,res.length,res.length);
		return combined;
	}


}
