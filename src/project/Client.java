package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import project.Constants.MsgType;
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
				neighbor.setClientState(ScanState.START);
			}

			while(true)
			{
				if (neighbor.isUpdatePieceInfo()) {
					/**send have message to this neighbor
					receive interested msg - set to false
					update interested list**/
					for (byte[] pieceIndex : neighbor.piecesRxved) {
						sendHaveMsg(pieceIndex);
						byte[] interestedMsg = new byte[9];
						in.read(interestedMsg);
						if (!Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
							Peer.getInstance().interestedInMe.add(neighbor.peerId);
						}
						neighbor.piecesRxved.remove(pieceIndex);
					}
					if (neighbor.piecesRxved.isEmpty())
						neighbor.setUpdatePieceInfo(false);;
				}
				switch (neighbor.getClientState()) {
					case START: {
						System.out.println("CLIENT== MODE-START- sent handshake msg");
						sendHandShake();
						neighbor.setClientState(ScanState.SENT_HAND_SHAKE);
						break;
					}	
					case SENT_HAND_SHAKE: {
						byte[] handShakeMsg = new byte[32];
						in.read(handShakeMsg);
						if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
							System.out.println("CLIENT== MODE-SENT_HAND_SHAKE - Neighbor <" + neighbor.peerId +"> validated");
							neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
							neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
						}
						break;
					}
					case DONE_HAND_SHAKE:{
						if (Peer.getInstance().hasCompleteFile()) {
							sendBitField();
							neighbor.setClientState(ScanState.SENT_BIT_FIELD);
						} else {
							neighbor.setClientState(ScanState.UPLOAD_START);
						}
						break;
					}
					case SENT_BIT_FIELD:{
						/**if bit field message sent wait for interested/not interested msg*/
						byte[] interestedMsg = new byte[10];
						in.read(interestedMsg);
						System.out.println("CLIENT:- Received interested messgae- " + new String(interestedMsg));
						if (!Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
							Peer.getInstance().interestedInMe.add(neighbor.peerId);
						}
						neighbor.setClientState(ScanState.UPLOAD_START);
						break;
					}
					case UNCHOKE: {
						/**if this neighbor is selected as preferred neighbor
						send unchoke msg to the neighbor
						change state to RXVE_REQUEST**/
						sendUnchokeMsg();
						neighbor.setClientState(ScanState.RXVE_REQUEST);
					}
					case RXVE_REQUEST: {
						/** if pref neighbors changed -> state to choke in the scheduler
						rxve request msg
						change state to PIECE**/
					}
					case PIECE: {
						/**if pref neighbors changed -> state to choke in the scheduler
						send peice msg
						wait to receive either request or not interested.
						in the mean time if the neighbbor is choked it should be handled.
						if request received -> go to piece again ;
						if not interested - > go to UPLOAD_START state.**/
					}
					case CHOKE: {
						/**if pref neighbors changed -> state to choke in the scheduler
						expect nothing.
						change to UPLOCAD_START**/
					}
					default: {
						/*Send the sentence to the server
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

	private void sendUnchokeMsg() {
		sendMessage(msgWithoutPayLoad(MsgType.UNCHOKE));
	}

	private void sendHaveMsg(byte[] pieceIndex) {
		sendMessage(msgWithPayLoad(MsgType.HAVE, pieceIndex, null));
	}

	private void sendHandShake() {
		String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
		sendMessage(handShake.getBytes());
	}

	private void sendBitField(){
		sendMessage(msgWithPayLoad(MsgType.BITFIELD, Peer.getInstance().bitField, null));
	}

	private byte[] msgWithPayLoad(MsgType msgType, byte[] field, byte[] payLoad) {
		if (payLoad != null) {
			byte[] result = new byte[field.length + payLoad.length];
	        System.arraycopy(field, 0, result, 0, field.length);
	        System.arraycopy(payLoad, 0, result, field.length, payLoad.length);
	        field = result;
		}
		int length = field.length;
		byte[] message = new byte[5+length];
		System.arraycopy(createPrefix(length), 0, message, 0, 4);
		message[4] = msgType.value;
		System.arraycopy(field, 0, message, 5, field.length);
		return message;
	}

	private byte[] msgWithoutPayLoad(MsgType msgType) {
		byte[] message = new byte[5];
		System.arraycopy(createPrefix(1), 0, message, 0, 4);
		message[4] = msgType.value;
		return message;
	}
	
	public byte[] createPrefix(int len) {
		byte[] prefix = new byte[4];
		prefix[0] = (byte) ((len & 0xFF000000) >> 24);
		prefix[1] = (byte) ((len & 0x00FF0000) >> 16);
		prefix[2] = (byte) ((len & 0x0000FF00) >> 8);
		prefix[3] = (byte) (len & 0x000000FF);
        return prefix;
    }

	private void sendInterested(){
		String interestedMsg = "dummy text";
		sendMessage(interestedMsg.getBytes());
	}
	void sendMessage(byte[] msg)
	{
		try{
			out.write(msg);
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
