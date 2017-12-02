package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import project.Constants.MsgType;
import project.Constants.ScanState;

public class Client extends Thread {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	private RemotePeerInfo neighbor;
	public synchronized RemotePeerInfo getNeighbor() {
		return neighbor;
	}

	public synchronized void setNeighbor(RemotePeerInfo neighbor) {
		this.neighbor = neighbor;
	}

	public Client(RemotePeerInfo value) {
		this.neighbor = value;
	}

	public void run()
	{
		try{
			System.out.println("*The Client is running*");
			System.out.println(getNeighbor().peerAddress);
			System.out.println(new Integer(getNeighbor().peerPort));
			requestSocket = new Socket(getNeighbor().peerAddress, new Integer(getNeighbor().peerPort));
			if (neighbor.getClientState().equals(ScanState.START))
				Log.addLog(String.format("Peer %d makes a connection to Peer %d", Peer.getInstance().peerID, neighbor.peerId));
			System.out.printf("*My Client Connected to %s in port %s*", getNeighbor().peerAddress, getNeighbor().peerPort);
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			byte[] globalPieceIndex = createPrefix(-1);
			while(!Peer.getInstance().stopped)
			{
				if (getNeighbor().isUpdatePieceInfo()) {
					/**send have message to this neighbor
					receive interested msg - set to false
					update interested list**/
					for(int id: getNeighbor().prxd.keySet()){
						sendHaveMsg(getNeighbor().prxd.get(id));
						getNeighbor().prxd.remove(id);
					}
					if (getNeighbor().prxd.isEmpty())
						getNeighbor().setUpdatePieceInfo(false);
				}
				switch (getNeighbor().getClientState()) {
					case START: {
						System.out.println("CLIENT== MODE-START- sent handshake msg");
						sendHandShake();
						getNeighbor().setClientState(ScanState.SENT_HAND_SHAKE);
						break;
					}
					case SENT_HAND_SHAKE: {
						byte[] handShakeMsg = new byte[32];
						if (in.available() > 0){
							in.read(handShakeMsg);
							if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
								System.out.println("CLIENT== MODE-SENT_HAND_SHAKE - Neighbor <" + getNeighbor().peerId +"> validated");
								neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
								neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
							}
						}
						break;
					}
					case DONE_HAND_SHAKE:{
						System.out.println("CLIENT:- DONE HAND SHAKE STATE REACHED");
						if (Peer.getInstance().hasCompleteFile()) {
							sendBitField();
							neighbor.setClientState(ScanState.SENT_BIT_FIELD);
						} else {
							neighbor.setClientState(ScanState.UPLOAD_START);
						}
						break;
					}
					case UPLOAD_START: {
						if (in.available() > 0) {
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
								System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
								Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
							} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
								if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
								}
							}
						}
						break;
					}
					case SENT_BIT_FIELD:{
						/**if bit field message sent wait for interested/not interested msg*/
						if (in.available() > 0) {
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
								System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
								Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
							} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
								if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
								}
							}
							getNeighbor().setClientState(ScanState.UPLOAD_START);
						}
						break;
					}
					case UNCHOKE: {
						System.out.println("CLIENT:- UNCHOKE STATE REACHED");
						/**if this neighbor is selected as preferred neighbor
						send unchoke msg to the neighbor
						change state to RXVE_REQUEST**/
						if (in.available() > 0) {
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							 if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
									Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
											Peer.getInstance().peerID, neighbor.peerId));
									Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
								} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
									Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
											Peer.getInstance().peerID, neighbor.peerId));
									System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
									if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
										Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
									}
									break;
								}
						}
						sendUnchokeMsg();
						getNeighbor().setClientState(ScanState.RXVE_REQUEST);
						break;
					}
					case RXVE_REQUEST: {
						/** if pref neighbors changed -> state to choke in the scheduler
						rxve request msg
						if pref neighbors changed -> state to choke in the scheduler
						send peice msg
						change state to PIECE**/
						if (in.available() > 0) {
							System.out.println("CLIENT:- RXVE REQUEST STATE REACHED");
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							if (responseMsg[4] == MsgType.REQUEST.value) {
								in.read(globalPieceIndex);
								System.out.println("CLIENT:- received request message from peer " + getNeighbor().peerId);
								sendPieceMsg(globalPieceIndex);
							} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
								if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
								}
								break;
							} else if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
								Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
								Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
							} 
						}
						break;
						
					}
					case CHOKE: {
						/**if pref neighbors changed -> state to choke in the scheduler
						expect nothing.
						change to UPLOCAD_START**/
						if (in.available() > 0) {
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							 if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
								 Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
											Peer.getInstance().peerID, neighbor.peerId));
								 System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
								Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
							} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
								Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
										Peer.getInstance().peerID, neighbor.peerId));
								System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
								if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
							}
								}
						}
						System.out.println("CLIENT:- CHOKE STATE REACHED");
						sendChokeMsg();
						getNeighbor().setClientState(ScanState.UPLOAD_START);
						break;
					}
					case KILL:{
						System.out.println("in kill state client "+getNeighbor().peerId);
						System.out.println("in.availbale "+in.available());
						if (in.available() > 0) {
							byte[] responseMsg = new byte[5];
							in.read(responseMsg);
							 if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
									System.out.println("CLIENT:- received interested message from peer " + getNeighbor().peerId);
									Peer.getInstance().interestedInMe.add(getNeighbor().peerId);
									Log.addLog(String.format("Peer %d received the 'interested' message from %d", 
											Peer.getInstance().peerID, neighbor.peerId));
								} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value) {
									Log.addLog(String.format("Peer %d received the 'not interested' message from %d", 
											Peer.getInstance().peerID, neighbor.peerId));
									System.out.println("CLIENT:- received not interested message from peer " + getNeighbor().peerId);
									if (Peer.getInstance().interestedInMe.contains(getNeighbor().peerId)) {
										Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(getNeighbor().peerId));
									}
								}
						}
						System.out.println("CLIENT:- KILL STATE REACHED");
						Peer.getInstance().stopped=true;
						interrupt();
						break;
					}
					default: {
						break;
					}
				}
			}
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
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

	private synchronized void sendChokeMsg() {
		sendMessage(msgWithoutPayLoad(MsgType.CHOKE));
	}
	
	private synchronized void sendPieceMsg(byte[] pieceIndex) {
		byte[] piece = Peer.getInstance().pieces[getPieceIndex(pieceIndex)].pieceInfo;
		byte[] result = new byte[piece.length+4];
		System.arraycopy(pieceIndex, 0, result, 0, 4);
		System.arraycopy(piece, 0, result, 4, piece.length);
		sendMessage(msgWithPayLoad(MsgType.PIECE, result));		
	}

	private synchronized int getPieceIndex(byte[] pieceIndex) {
		int integerValue = 0;
        for (int index = 0; index < 4; index++) {
            int shift = (4 - 1 - index) * 8;
            integerValue += (pieceIndex[index] & 0x000000FF) << shift;
        }
        return integerValue;
	}

	private synchronized void sendUnchokeMsg() {
		sendMessage(msgWithoutPayLoad(MsgType.UNCHOKE));
	}
	

	public synchronized void sendHaveMsg(byte[] pieceIndex) {
		sendMessage(msgWithPayLoad(MsgType.HAVE, pieceIndex));
	}

	private synchronized void sendHandShake() {
		String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
		sendMessage(handShake.getBytes());
	}

	private synchronized void sendBitField(){
		sendMessage(msgWithPayLoad(MsgType.BITFIELD, Peer.getInstance().getBitField().toByteArray()));
	}
	
	private synchronized  byte[] msgWithPayLoad(MsgType msgType, byte[] payLoad) {
		int length = payLoad.length;
		
		byte[] message = new byte[5+length];
		System.arraycopy(createPrefix(length+1), 0, message, 0, 4);
		message[4] = msgType.value;
		System.arraycopy(payLoad, 0, message, 5, payLoad.length);
		return message;
	}

	private synchronized  byte[] msgWithoutPayLoad(MsgType msgType) {
		byte[] message = new byte[5];
		System.arraycopy(createPrefix(1), 0, message, 0, 4);
		message[4] = msgType.value;
		return message;
	}
	
	public synchronized byte[] createPrefix(int len) {
		byte[] prefix = new byte[4];
		prefix[0] = (byte) ((len & 0xFF000000) >> 24);
		prefix[1] = (byte) ((len & 0x00FF0000) >> 16);
		prefix[2] = (byte) ((len & 0x0000FF00) >> 8);
		prefix[3] = (byte) (len & 0x000000FF);
        return prefix;
    }

	synchronized void sendMessage(byte[] msg)
	{
		try{
			System.out.println("CLIENT:- sent message to server -  to peer " +neighbor.peerId);
			out.write(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	public synchronized byte[] getMessage(String messageType){
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
