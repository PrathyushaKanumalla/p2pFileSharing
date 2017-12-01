package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import project.Constants.MsgType;
import project.Constants.ScanState;

public class Server extends Thread{

	public void run(){
		System.out.print("*The Server is running*"); 
		ServerSocket listener = null;

		try {
			listener = new ServerSocket(Integer.parseInt(Peer.getInstance().portNum));
			for (Integer clientPeerId : Peer.getInstance().neighbors.keySet()) {
//			while (true) {
				new Handler(listener.accept(), Peer.getInstance().neighbors.get(clientPeerId)).start();
				//System.out.println("*My Server Connected to "  + clientPeerId + " *");
//			}
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
	private class Handler extends Thread {
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
				System.out.print("*In handler ");
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				this.neighbor = Peer.getInstance().search(connection.getInetAddress(), connection.getPort());
				System.out.print(" with neighbor " + neighbor.peerId);
				if (neighbor == null) {
					System.out.println("neighbor is null");
				}
				//else {
				while(!Peer.getInstance().stopped)
				{
					/** if i receive have msg
					send interested or not interested as response; 
					 **/
					//show the message to the user
					switch (neighbor.getServerState()) {
					case DONE_HAND_SHAKE: {
						//go to SERVER_LISTEN
						System.out.println("SERVER:- DONEHANDSHAKE STATE ");
						neighbor.setServerState(ScanState.SERVER_LISTEN);
					}
					case SERVER_LISTEN: {
	
							/**listen  for messages
						1. initial && if bitfield -> send interested/not interested, set initial = false;
						 2. unchoke message -> state = unchoke; set initial = false if it is true;
						listen for unchoke/choke msges
						state = unchoke or choke**/
//						System.out.println("SERVER:- SERVER LISTEN STATE ");
							if(in.available() >0){
								byte[] msg = new byte[5];
								in.read(msg);
								if (neighbor.initial && msg[4] == MsgType.BITFIELD.value) {
									int bitFieldSize = Peer.getInstance().noOfPieces;
									byte[] bitField = new byte[bitFieldSize];
//									while(in.available()<0){}
									in.read(bitField);
									System.out.println("SERVER:- received bit field message from " + neighbor.peerId +
											" and bitfield is " + BitSet.valueOf(bitField));
									Peer.getInstance().getNeighborsBitSet().put(neighbor.peerId, BitSet.valueOf(bitField));
									if (!Peer.getInstance().getBitField().equals(bitField)) {
										sendInterested();
									} else {
										sendNotInterested();
									}
									neighbor.initial=false;
								} else if (msg[4] == MsgType.UNCHOKE.value) {
									System.out.println("SERVER:- received unchoke message from " + neighbor.peerId);
									if (neighbor.initial) 
										neighbor.initial = false;
									byte[] pieceIndex = new byte[4];
									int genPieceindx = genPieceIndex();
									if(genPieceindx == -1){
										sendNotInterested();
										break;
									}
									pieceIndex = createPrefix(genPieceindx);
									if (pieceIndex != null) {
//										System.out.println("SERVER:- pieceindex req "+ genPieceindx);
										neighbor.startTime.put(neighbor.peerId, System.currentTimeMillis());
										sendRequestMessage(pieceIndex);
										System.out.println("SERVER:- SENT this request message of piece- " +genPieceindx +" to peer id " +neighbor.peerId );
									}
									
									//neighbor.setServerState(ScanState.UNCHOKE);
								} else if (msg[4] == MsgType.CHOKE.value) {
									System.out.println("SERVER:- received choke message from " + neighbor.peerId);
									//neighbor.setServerState(ScanState.CHOKE);
								} else if (msg[4] == MsgType.HAVE.value) {
									System.out.println("SERVER:- HAVE MESSGAGE IN SERVER_LISTEN BLOCK");
									byte[] pieceIndex = new byte[4];
//									while(in.available()<0){}
									in.readFully(pieceIndex);
									System.out.println("SERVER:- Received HAVE INDEX "+ getPieceIndex(pieceIndex) 
												+ " from peer id "+ neighbor.peerId);
									int gotThisPeerIndex = getPieceIndex(pieceIndex);
									Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).set(gotThisPeerIndex);
									System.out.println("SERVER:- neighbor " + neighbor.peerId + " & bitset is - "+ Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId));
									BitSet myBitfield = Peer.getInstance().getBitField();
									BitSet neighborBitset = Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId);
									boolean sendInterested = false;
									for(int i = 0;i < Peer.getInstance().noOfPieces;i++){
										if(!myBitfield.get(i) && neighborBitset.get(i)){
											sendInterested = true;
											break;
										}
									}
									if (sendInterested) 
										sendInterested();
									else
										sendNotInterested();
//									System.out.println("neighbor bitset -> "+ neighbor.peerId);
//									for(int i=0;i<Peer.getInstance().noOfPieces;i++){
//										System.out.println(Peer.getInstance().neighborsBitSet.get(neighbor.peerId).get(i));
//									}
								} else if (msg[4] == MsgType.PIECE.value) {
									//System.out.println("PIECE 2nd time here");
									byte[] reqPieceIndex = new byte[4];
								//	while(in.available()<0){}
									in.read(reqPieceIndex);
									int reqPieceInd = getPieceIndex(reqPieceIndex);
									System.out.println("SERVER:- Received this piece message - " + reqPieceInd + " from peer " + neighbor.peerId);
//									System.out.println("REQ INDEX -> "+ reqPieceInd);
									//while(in.available()<0){}
									int pieceSize = 0;
									if (reqPieceInd != Peer.getInstance().noOfPieces-1) {
										pieceSize = Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"));
										byte[] piece = new byte[pieceSize];
										in.readFully(piece);
										System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
												 " from peer " + neighbor.peerId );
//										System.out.println("for not LAST ONE -> "+ new String(piece));
//										System.out.println("Piece Len-> "+ piece.length);
										Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
									} else {
										pieceSize = Peer.getInstance().excessPieceSize;
										byte[] piece = new byte[pieceSize];
										in.readFully(piece);
										System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
												 " from peer " + neighbor.peerId );
//										System.out.println("last index val");
//										System.out.println("for LAST ONE -> "+ piece.length);
										Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
									}
									if (neighbor.startTime.containsKey(neighbor.peerId)) {
										Long endTime = System.currentTimeMillis();
										Long downtime = endTime - neighbor.startTime.get(neighbor.peerId);
										System.out.println("downtime here **** -> "+downtime);
										System.out.println("pieceSize here **** -> "+pieceSize);
										System.out.println("download rate is **** -> "+ (long)(pieceSize/downtime));
										Peer.getInstance().downloadTime.put(neighbor.peerId, (long)(pieceSize)/downtime);
									}
									pieceSize = 0;
									Peer.getInstance().getBitField().set(reqPieceInd);
									//update all neighbors
									System.out.println("SERVER:- My bit field " + Peer.getInstance().getBitField());
									for (RemotePeerInfo neighbor : Peer.getInstance().neighbors.values()) {
										System.out.println("SERVER:- sending have message " + reqPieceInd + " to -> "+ neighbor.peerId);
										//Peer.getInstance().neighborThreads.get(neighbor.peerId).sendHaveMsg(reqPieceIndex);
										if (!neighbor.isUpdatePieceInfo())
											neighbor.setUpdatePieceInfo(true);
//										neighbor.getPiecesRxved().add(reqPieceIndex);
										neighbor.prxd.put(reqPieceInd, reqPieceIndex);
										System.out.println("SERVER:- done  have message " + reqPieceInd + "to -> "+ neighbor.peerId);
									}
//									System.out.println(reqPieceInd + "---> to update bitfield after receive");
									//neighbor.setServerState(Constants.ScanState.UNCHOKE);
									byte[] pieceIndex = new byte[4];
									int genPieceindx = genPieceIndex();
									if(genPieceindx == -1){
										sendNotInterested();
										break;
									}
									pieceIndex = createPrefix(genPieceindx);
									if (pieceIndex != null) {
//										System.out.println("SERVER:- pieceindex req "+ genPieceindx);
										sendRequestMessage(pieceIndex);
										System.out.println("SERVER:- SENT this request message of piece- " +genPieceindx +" to peer id " +neighbor.peerId );
									}
									
								}
							}
							break;
					}
					case UNCHOKE: {
						/**respond with request or not interested
						if request -> state = PIECE
						or send not interested and go to server_listen*/
//						System.out.println("SERVER:- UNCHOKE STATE ");
//						byte [] msg = new byte[5];
//						in.read(msg);
//						if (msg[4] == MsgType.UNCHOKE.value) {
//						System.out.println("SERVER:- unchoke val ");
						if (in.available() > 0 ) {
							byte[] message = new byte[5];
							System.out.println("SERVER:_POSSIBLE_HAVE_BLOCK");
							in.read(message);
							System.out.println("SERVER:-HAVE_BLOCK_message-type - "+message[4]);
							if (message[4] == MsgType.HAVE.value) {
								System.out.println("SERVER:- HAVE MESSGAGE IN UNCHOKE BLOCK-1");
								byte[] havePieceIndex = new byte[4];
								in.read(havePieceIndex);
								System.out.println("SERVER:- Received HAVE INDEX "+ getPieceIndex(havePieceIndex) 
								+ " from peer id "+ neighbor.peerId);
								Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).set(getPieceIndex(havePieceIndex));
								System.out.println("SERVER:- neighbor " + neighbor.peerId + " & bitset is - "+ Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId));
								BitSet myBitfield = Peer.getInstance().getBitField();
								BitSet neighborBitset = Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId);
								boolean sendInterested = false;
								for(int i = 0;i < Peer.getInstance().noOfPieces;i++){
									if(!myBitfield.get(i) && neighborBitset.get(i)){
										sendInterested = true;
										break;
									}
								}
								if (sendInterested) 
									sendInterested();
								else
									sendNotInterested();
							}
						}	else {
							System.out.println("SERVER:- POSSIBLE_NOT_HAVE_BLOCK");
						}
						
						
						byte[] pieceIndex = new byte[4];
						int genPieceindx = genPieceIndex();
						if(genPieceindx == -1){
							neighbor.setServerState(ScanState.SERVER_LISTEN);
							sendNotInterested();
							break;
						}
						pieceIndex = createPrefix(genPieceindx);
//						System.out.println("SERVER:- pieceindex val "+ new String(pieceIndex));
						if (pieceIndex != null) {
//							System.out.println("SERVER:- pieceindex req "+ genPieceindx);
							sendRequestMessage(pieceIndex);
							System.out.println("SERVER:- SENT this request message of piece- " +genPieceindx +" to peer id " +neighbor.peerId );
//							System.out.println("SERVER:- sent pieceindex req");
							/**listen for a piece msg;
							if piece -> update bit field
							->then update  updatePieceInfo in peer;
							send request again by putting state = unchoke;
							if choke occurs -go to server listen again**/
							byte[] message = new byte[5];
//							while(in.available()<0){
//								
//							}
//							if(in.available() >0){
							while(in.available()<=0){}
							in.read(message);
//							System.out.println("MESSAGE LENGTH"+message.length);
//							System.out.println("Message type --> "+ new String(message));
							if (message[4] == MsgType.PIECE.value) {
								//System.out.println("PIECE 2nd time here");
								byte[] reqPieceIndex = new byte[4];
							//	while(in.available()<0){}
								in.read(reqPieceIndex);
								int reqPieceInd = getPieceIndex(reqPieceIndex);
								System.out.println("SERVER:- Received this piece message - " + reqPieceInd + " from peer " + neighbor.peerId);
//								System.out.println("REQ INDEX -> "+ reqPieceInd);
								//while(in.available()<0){}
								if (reqPieceInd != Peer.getInstance().noOfPieces-1) {
									byte[] piece = new byte[Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"))];
									in.readFully(piece);
									System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
											 " from peer " + neighbor.peerId );
//									System.out.println("for not LAST ONE -> "+ new String(piece));
//									System.out.println("Piece Len-> "+ piece.length);
									Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
								} else {
									byte[] piece = new byte[Peer.getInstance().excessPieceSize];
									in.readFully(piece);
									System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
											 " from peer " + neighbor.peerId );
//									System.out.println("last index val");
//									System.out.println("for LAST ONE -> "+ piece.length);
									Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
								}
								Peer.getInstance().getBitField().set(reqPieceInd);
								//update all neighbors
								System.out.println("SERVER:- My bit field " + Peer.getInstance().getBitField());
								for (RemotePeerInfo neighbor : Peer.getInstance().neighbors.values()) {
									//neighbor.getPiecesRxved().add(reqPieceIndex);
									//neighbor.setUpdatePieceInfo(true);
//										neighbor.getPiecesRxved().add(reqPieceIndex);
//										neighbor.setUpdatePieceInfo(true);
									System.out.println("SERVER:- sending have message " + reqPieceInd + " to -> "+ neighbor.peerId);
									Peer.getInstance().neighborThreads.get(neighbor.peerId).sendHaveMsg(pieceIndex);
									//ScanState prevState = neighbor.getClientState();
									//neighbor.getPiecesRxved().add(reqPieceIndex);
									//neighbor.setClientState(ScanState.HAVE);
									/*while (Peer.getInstance().neighborThreads.get(neighbor.peerId).isPause()) {
									}*/
									/*Peer.getInstance().neighborThreads.get(neighbor.peerId).setHavePiece(reqPieceIndex);
									Peer.getInstance().neighborThreads.get(neighbor.peerId).setPause(true);*/
									/*while (Peer.getInstance().neighborThreads.get(neighbor.peerId).isPause()) {
									}*/
									
									
									//sendHaveMsg(reqPieceIndex);
									
									System.out.println("SERVER:- done  have message " + reqPieceInd + "to -> "+ neighbor.peerId);
									//neighbor.setClientState(prevState);
//									System.out.println("sending have updare "+ reqPieceIndex);
								}
//								System.out.println(reqPieceInd + "---> to update bitfield after receive");
								neighbor.setServerState(Constants.ScanState.UNCHOKE);
								
							} else if (message[4] == MsgType.CHOKE.value) {
								System.out.println("SERVER:- Received Choke message from peer " + neighbor.peerId);
								neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
								Peer.getInstance().requestedbitField.clear(genPieceindx);
							} else if (message[4] == MsgType.HAVE.value) {
								System.out.println("SERVER:- HAVE MESSGAGE IN UNCHOKE BLOCK-2");
								byte[] havePieceIndex = new byte[4];
//								while(in.available()<0){}
								in.read(havePieceIndex);
								System.out.println("SERVER:- Received HAVE INDEX "+ getPieceIndex(havePieceIndex) 
								+ " from peer id "+ neighbor.peerId);
								Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).set(getPieceIndex(havePieceIndex));
								System.out.println("SERVER:- neighbor " + neighbor.peerId + " & bitset is - "+ Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId));
								BitSet myBitfield = Peer.getInstance().getBitField();
								BitSet neighborBitset = Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId);
								boolean sendInterested = false;
								for(int i = 0;i < Peer.getInstance().noOfPieces;i++){
									if(!myBitfield.get(i) && neighborBitset.get(i)){
										sendInterested = true;
										break;
									}
								}
								if (sendInterested) 
									sendInterested();
								else
									sendNotInterested();
//								System.out.println("HAVE PIECE INDEX ---> "+ getPieceIndex(havePieceIndex));
//								System.out.println("peer id for have message ---> "+ neighbor.peerId);
//								System.out.println("neighbor bit set  ---> "+ Peer.getInstance().neighborsBitSet.get(neighbor.peerId));
							}
//							}
						} else {
							sendNotInterested();
							neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
						}
						break;
					}
					case START: {
						byte[] handShakeMsg = new byte[32];
//						System.out.println("SERVER:- server waiting to read");
//						System.out.println("SERVER:- STARTE STATE ");
						if(in.available()>0){
						in.read(handShakeMsg);
							if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
								System.out.printf("SERVER:- Received message: <%s> from client %d\n" ,new String(handShakeMsg), neighbor.peerId);
								System.out.println("SERVER:- Neighbor <" + neighbor.peerId +"> validated");
								//if not validated it does not proceed further
								neighbor.setServerState(ScanState.RXVED_HAND_SHAKE);
							}
						}
						break;
					} 
					case RXVED_HAND_SHAKE: {
						sendHandShake();
						System.out.println("SERVER:- RCVED HAND SHAKE STATE ----sent hand shake message to " + neighbor.peerId);
						neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
						neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
						break;
					}
					case KILL:{
						System.out.println("SERVER:- KILL STATE ");
						Peer.getInstance().stopped=true;
						interrupt();
						break;
					}
					default: 
						break;
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
		
		/*public synchronized void sendHaveMsg(byte[] pieceIndex) {
			try {
				sendMessage(msgWithPayLoad(MsgType.HAVE, pieceIndex));
				byte[] responseMsg = new byte[5];
				while(in.available()<=0){}
				in.read(responseMsg);
				if (responseMsg[4] == MsgType.INTERESTED.value && !Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
					System.out.println("SERVER:- received interested message from peer " + neighbor.peerId);
					Peer.getInstance().interestedInMe.add(neighbor.peerId);
				} else if (responseMsg[4] == MsgType.NOT_INTERESTED.value){
					System.out.println("SERVER:- received not interested message from peer " + neighbor.peerId);
					if (Peer.getInstance().interestedInMe.contains(neighbor.peerId)) {
						Peer.getInstance().interestedInMe.remove(Peer.getInstance().interestedInMe.indexOf(neighbor.peerId));
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}*/
		

		private synchronized int genPieceIndex() {
			List<Integer> possibleRequests = new ArrayList<>();
			for(int i = 0;i < Peer.getInstance().noOfPieces;i++){
				if(!Peer.getInstance().getBitField().get(i) && Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).get(i) 
						&& !Peer.getInstance().requestedbitField.get(i)){
					possibleRequests.add(i);
				}
			}
			if (possibleRequests.isEmpty())
				return -1;
			Random rand = new Random();
			int randomIndex = rand.nextInt(possibleRequests.size());
			System.out.println("SERVER:- Request index genrated random for piece - " + possibleRequests.get(randomIndex));
			Peer.getInstance().requestedbitField.set(possibleRequests.get(randomIndex));
			return possibleRequests.get(randomIndex);
		}

		private synchronized void sendRequestMessage(byte[] pieceIndex) {
			sendMessage(msgWithPayLoad(MsgType.REQUEST, pieceIndex));
		}

		private synchronized void sendHandShake() {
			String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
			sendMessage(handShake.getBytes());
		}

		//send a message to the output stream
		public synchronized void sendMessage(byte[] msg)
		{
			try{
				out.write(msg);
				out.flush();
				System.out.printf("SERVER:- Sent message:<%s> to Client %d\n" ,print(msg), neighbor.peerId);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

		
		private synchronized void sendInterested(){
			System.out.println("SERVER:- SENT interested msg to peer " + neighbor.peerId);
			sendMessage(msgWithoutPayLoad(MsgType.INTERESTED));
		}
		
		private synchronized void sendNotInterested(){
			System.out.println("SERVER:- SENT not interested msg to peer " + neighbor.peerId);
			sendMessage(msgWithoutPayLoad(MsgType.NOT_INTERESTED));
		}
		
		private synchronized byte[] msgWithPayLoad(MsgType msgType, byte[] payLoad) {
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
		
		private synchronized int getPieceIndex(byte[] pieceIndex) {
			int integerValue = 0;
	        for (int index = 0; index < 4; index++) {
	            int shift = (4 - 1 - index) * 8;
	            integerValue += (pieceIndex[index] & 0x000000FF) << shift;
	        }
	        return integerValue;
		}

		private synchronized byte[] msgWithoutPayLoad(MsgType msgType) {
			byte[] message = new byte[5];
			System.arraycopy(createPrefix(1), 0, message, 0, 4);
			message[4] = msgType.value;
			return message;
		}
		
		public synchronized byte[] createPrefix(int len) {
			return new byte[] {
		            (byte)(len >>> 24),
		            (byte)(len >>> 16),
		            (byte)(len >>> 8),
		            (byte)len};
	    }
		
		private synchronized String print(byte[] msg) {
			StringBuilder sb = new StringBuilder();
			for (byte b : msg) {
				sb.append(b);
			}
			return sb.toString();
		}
	}
}