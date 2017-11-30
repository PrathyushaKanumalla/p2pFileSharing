package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;

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
			System.out.println("TEST 1");
			System.out.println(neighbor.peerAddress);
			System.out.println(new Integer(neighbor.peerPort));
			requestSocket = new Socket(neighbor.peerAddress, new Integer(neighbor.peerPort));
			System.out.println("TEST 2");
			System.out.printf("*My Client Connected to %s in port %s*", neighbor.peerAddress, neighbor.peerPort);
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
//			System.out.println("TEST 1");
			in = new ObjectInputStream(requestSocket.getInputStream());
//			System.out.println("TEST2");
			//sending handshake message

			//handShake = "handshakeSent";
			//sendMessage(handShake);
			//get Input from standard input
			//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			if (neighbor.peerId < Peer.getInstance().peerID) {
				neighbor.setClientState(ScanState.START);
			}
			
			while(!Peer.getInstance().stopped)
			{
				
				/*if (neighbor.isUpdatePieceInfo()) {
					*//**send have message to this neighbor
					receive interested msg - set to false
					update interested list**//*
//					Iterator<byte[]> iterator = neighbor.piecesRxved.iterator();
//					System.out.println("RECEIVED HAVE IN CLIENT "+ neighbor.piecesRxved.size());
//					while(iterator.hasNext()){
//						byte[] tempPIndx = iterator.next();
//						System.out.println("have peice index here in client -> "+getPieceIndex(tempPIndx));
//						sendHaveMsg(tempPIndx);
//						byte[] responseMsg = new byte[9];
//						in.read(responseMsg);
//						if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
//							Peer.getInstance().interestedInMe.add(neighbor.peerId);
//						} 
//						iterator.remove();
//					}
//					synchronized(this) {
					for (int i=0;i<neighbor.getPiecesRxved().size();i++) {
						byte[] pieceIndex = neighbor.getPiecesRxved().get(i);
						if(pieceIndex.length >2){		
							sendHaveMsg(pieceIndex);
							byte[] responseMsg = new byte[9];
							in.read(responseMsg);
							if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
								Peer.getInstance().interestedInMe.add(neighbor.peerId);
							} 
//							neighbor.getPiecesRxved().remove(pieceIndex);
							
							//neighbor.piecesRxved.add(i, new byte[0]);
						}
					}
					
					
					System.out.println("CLIENT:- Have message sent");
					if (neighbor.getPiecesRxved().isEmpty())
						neighbor.setUpdatePieceInfo(false);
//				}
					
				}*/
				switch (neighbor.getClientState()) {
					case START: {
						System.out.println("CLIENT== MODE-START- sent handshake msg");
						sendHandShake();
						neighbor.setClientState(ScanState.SENT_HAND_SHAKE);
						break;
					}	
					case SENT_HAND_SHAKE: {
						byte[] handShakeMsg = new byte[32];
						if(in.available() >0){
							in.read(handShakeMsg);
							if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
								System.out.println("CLIENT== MODE-SENT_HAND_SHAKE - Neighbor <" + neighbor.peerId +"> validated");
								neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
								neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
							}
						}
						break;
					}
					case DONE_HAND_SHAKE:{
						System.out.println("CLIENT:- DONE HAND SHAKE STATE REACHED");
						if (Peer.getInstance().hasCompleteFile()) {
//							System.out.println("have complete file");
							sendBitField();
							neighbor.setClientState(ScanState.SENT_BIT_FIELD);
						} else {
							neighbor.setClientState(ScanState.UPLOAD_START);
						}
						break;
					}
					case SENT_BIT_FIELD:{
						/**if bit field message sent wait for interested/not interested msg*/
//						System.out.println("CLIENT:- STATE SENT BIT FIELD");
						if(in.available() >0){
							byte[] responseMsg = new byte[10];
							in.read(responseMsg);
							System.out.println("CLIENT:- Received interested messgae- " + new String(responseMsg));
							if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
//								System.out.println("got interested message");
								Peer.getInstance().interestedInMe.add(neighbor.peerId);
							} // if not interested do nothing
							neighbor.setClientState(ScanState.UPLOAD_START);
						}
						break;
					}
					case UNCHOKE: {
						System.out.println("CLIENT:- UNCHOKE STATE REACHED");
						/**if this neighbor is selected as preferred neighbor
						send unchoke msg to the neighbor
						change state to RXVE_REQUEST**/
						sendUnchokeMsg();
						neighbor.setClientState(ScanState.RXVE_REQUEST);
						break;
					}
					case RXVE_REQUEST: {
						/** if pref neighbors changed -> state to choke in the scheduler
						rxve request msg
						if pref neighbors changed -> state to choke in the scheduler
						send peice msg
						change state to PIECE**/
						if(in.available() >0){
							System.out.println("CLIENT:- RXVE REQUEST STATE REACHED");
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
//							System.out.println("CLIENT:- Received interested messgae- " + new String(responseMsg));
							if (responseMsg[4] == MsgType.REQUEST.value) {
								System.out.println("CLIENT:- REQUST VAL GOT STATE ");
								neighbor.setClientState(ScanState.PIECE);
							} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								System.out.println("CLIENT:- NOT INTERRSTDDDDD here GOT STATE ");
								neighbor.setClientState(ScanState.UPLOAD_START);
							}
						}
						
						break;
						
					}
					case PIECE: {
						/**
						wait to receive either request or not interested.
						in the mean time if the neighbor is choked it should be handled.
						if request received -> go to piece again ;
						if not interested - > go to UPLOAD_START state.**/
						System.out.println("CLIENT:- PIECE STATE REACHED");
//						if(in.available() >0){
//						while (in.available() < 0) {
//						}
							//System.out.println("here");
							byte[] pieceIndex = new byte[4];
							in.read(pieceIndex);
//							System.out.println(new String(pieceIndex));
							System.out.println("&&&&&&&&&&&&" +getPieceIndex(pieceIndex));
							//System.out.println("piece index" + new String(pieceIndex));
							sendPieceMsg(pieceIndex);
							//System.out.println("send piece message");
							
							
							while (in.available() < 0) {
							}
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							if (responseMsg[4] == MsgType.REQUEST.value) {
								neighbor.setClientState(ScanState.PIECE);
							}  else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								neighbor.setClientState(ScanState.UPLOAD_START);
							}
//						}
						break;
					}
					case CHOKE: {
						/**if pref neighbors changed -> state to choke in the scheduler
						expect nothing.
						change to UPLOCAD_START**/
						System.out.println("CLIENT:- CHOKE STATE REACHED");
						sendChokeMsg();
						neighbor.setClientState(ScanState.UPLOAD_START);
						break;
					}
					case KILL:{
						System.out.println("CLIENT:- KILL STATE REACHED");
						Peer.getInstance().stopped=true;
						interrupt();
						break;
					}
					default: {
						/*Send the sentence to the server
							String message = "default_behavior_pratServer";
							sendMessage(message);
							//Receive the upperCase sentence from the server
							message = (String)in.readObject();
							//show the message to the user
							System.out.printf("CLIENT:- Received the message: <%s>\n", message);*/
//						System.out.println("CLIENT: DEFAULT "+neighbor.getClientState());
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

	private  void sendChokeMsg() {
		sendMessage(msgWithoutPayLoad(MsgType.CHOKE));
	}

	private  void sendPieceMsg(byte[] pieceIndex) {
		byte[] piece = Peer.getInstance().pieces[getPieceIndex(pieceIndex)].pieceInfo;
		byte[] result = new byte[piece.length+4];
		//System.out.println(new String(pieceIndex));
		System.arraycopy(pieceIndex, 0, result, 0, 4);
	//	System.out.println("before -->" +new String(result));
		System.arraycopy(piece, 0, result, 4, piece.length);
		//System.out.println("after -->" +new String(result));
//		System.out.println("sent length->>> "+piece.length);
		sendMessage(msgWithPayLoad(MsgType.PIECE, result));		
	}

	private  int getPieceIndex(byte[] pieceIndex) {
		int integerValue = 0;
        for (int index = 0; index < 4; index++) {
            int shift = (4 - 1 - index) * 8;
            integerValue += (pieceIndex[index] & 0x000000FF) << shift;
        }
        return integerValue;
	}

	private  void sendUnchokeMsg() {
		sendMessage(msgWithoutPayLoad(MsgType.UNCHOKE));
	}
	

	public  void sendHaveMsg(byte[] pieceIndex) {
		try {
			sendMessage(msgWithPayLoad(MsgType.HAVE, pieceIndex));
			byte[] responseMsg = new byte[5];
			while(in.available()<=0){}
			System.out.println("CLIENT:- input buffer has " + in.available());
			in.read(responseMsg);
			System.out.println("CLIENT:- received this type - " + responseMsg[4]);
			if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
				Peer.getInstance().interestedInMe.add(neighbor.peerId);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private  void sendHandShake() {
		String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
		sendMessage(handShake.getBytes());
	}

	private  void sendBitField(){
		sendMessage(msgWithPayLoad(MsgType.BITFIELD, Peer.getInstance().getBitField().toByteArray()));
	}
	
	private  byte[] msgWithPayLoad(MsgType msgType, byte[] payLoad) {
		/*if (payLoad != null) {
			byte[] result = new byte[payLoad.length];
	        System.arraycopy(field, 0, result, 0, field.length);
	        System.arraycopy(payLoad, 0, result, field.length, payLoad.length);
	        field = result;
		}*/
		int length = payLoad.length;
		
		byte[] message = new byte[5+length];
		System.arraycopy(createPrefix(length+1), 0, message, 0, 4);
		message[4] = msgType.value;
		System.arraycopy(payLoad, 0, message, 5, payLoad.length);
		return message;
	}

	private  byte[] msgWithoutPayLoad(MsgType msgType) {
		byte[] message = new byte[5];
		System.arraycopy(createPrefix(1), 0, message, 0, 4);
		message[4] = msgType.value;
		return message;
	}
	
	public  byte[] createPrefix(int len) {
		byte[] prefix = new byte[4];
		prefix[0] = (byte) ((len & 0xFF000000) >> 24);
		prefix[1] = (byte) ((len & 0x00FF0000) >> 16);
		prefix[2] = (byte) ((len & 0x0000FF00) >> 8);
		prefix[3] = (byte) (len & 0x000000FF);
        return prefix;
    }

	 void  sendMessage(byte[] msg)
	{
		try{
//			System.out.println("final length ---> "+msg.length);
//			System.out.println("final messge to sent --> "+ new String(msg));
			out.write(msg);
			out.flush();
			//System.out.printf("CLIENT:- Message<"+msg+"> sent to %s:%s\n", neighbor.peerAddress, neighbor.peerPort);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	public  byte[] getMessage(String messageType){
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
