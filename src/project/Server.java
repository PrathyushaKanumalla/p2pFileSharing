package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;

import project.Constants.MsgType;
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
					neighbor.setServerState(ScanState.START);
				}
				while(true)
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
						System.out.println("SERVER:- SERVER LISTEN STATE ");
							byte[] msg = new byte[5];
							in.read(msg);
							if (neighbor.initial && msg[4] == MsgType.BITFIELD.value) {
								int bitFieldSize = Peer.getInstance().noOfPieces;
								byte[] bitField = new byte[bitFieldSize];
								in.read(bitField);
								Peer.getInstance().neighborsBitSet.put(neighbor.peerId, BitSet.valueOf(bitField));
								if (!Peer.getInstance().bitField.equals(bitField)) {
									sendInterested();
								} else {
									sendNotInterested();
								}
								neighbor.initial=false;
							} else if (msg[4] == MsgType.UNCHOKE.value) {
								if (neighbor.initial) 
									neighbor.initial = false;
								neighbor.setServerState(ScanState.UNCHOKE);
							} else if (msg[4] == MsgType.CHOKE.value) {
								neighbor.setServerState(ScanState.CHOKE);
							} else if (msg[4] == MsgType.HAVE.value) {
								byte[] pieceIndex = new byte[4];
								in.read(pieceIndex);
								Peer.getInstance().neighborsBitSet.get(neighbor.peerId).set(getPieceIndex(pieceIndex));
							}
							break;
					}
					case UNCHOKE: {
						/**respond with request or not interested
						if request -> state = PIECE
						or send not interested and go to server_listen*/
						System.out.println("SERVER:- UNCHOKE STATE ");
//						byte [] msg = new byte[5];
//						in.read(msg);
//						if (msg[4] == MsgType.UNCHOKE.value) {
						System.out.println("SERVER:- unchoke val ");
						byte[] pieceIndex = new byte[4];
						int genPieceindx = genPieceIndex();
						pieceIndex = createPrefix(genPieceindx);
						System.out.println("SERVER:- pieceindex val ");
						if (pieceIndex != null) {
							System.out.println("SERVER:- pieceindex val not null ");
							sendRequestMessage(pieceIndex);
							/**listen for a piece msg;
							if piece -> update bit field
							->then update  updatePieceInfo in peer;
							send request again by putting state = unchoke;
							if choke occurs -go to server listen again**/
							byte[] message = new byte[5];
							in.read(message);
							if (message[4] == MsgType.PIECE.value) {
								byte[] reqPieceIndex = new byte[4];
								in.read(reqPieceIndex);
								int reqPieceInd = getPieceIndex(reqPieceIndex);
								if (reqPieceInd != Peer.getInstance().noOfPieces-1) {
									byte[] piece = new byte[Peer.getInstance().noOfPieces];
									in.read(piece);
									Peer.getInstance().pieces[reqPieceInd].pieceInfo = piece;
								} else {
									byte[] piece = new byte[Peer.getInstance().excessPieceSize];
									in.read(piece, 5, Peer.getInstance().excessPieceSize);
									Peer.getInstance().pieces[reqPieceInd].pieceInfo = piece;
								}
								//update all neighbors
								for (RemotePeerInfo neighbor : Peer.getInstance().neighbors.values()) {
									neighbor.setUpdatePieceInfo(true);
									neighbor.piecesRxved.add(reqPieceIndex);
								}
								Peer.getInstance().bitField.set(reqPieceInd);
								neighbor.setServerState(Constants.ScanState.UNCHOKE);
								
							} else if (message[4] == MsgType.CHOKE.value) {
								System.out.println("SERVER:- choke sent val ");
								neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
								Peer.getInstance().requestedbitField.clear(genPieceindx);
							} else if (message[4] == MsgType.HAVE.value) {
								byte[] havePieceIndex = new byte[4];
								in.read(havePieceIndex);
								Peer.getInstance().neighborsBitSet.get(neighbor.peerId).set(getPieceIndex(havePieceIndex));
							}
						} else {
							System.out.println("SERVER:- unchke not interested val ");
							sendNotInterested();
							neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
						}
//						} else if (msg[4] == MsgType.HAVE.value) {
//							System.out.println("SERVER:- have val in choke state val ");
//							byte[] pieceIndex = new byte[4];
//							in.read(pieceIndex);
//							Peer.getInstance().neighborsBitSet.get(neighbor.peerId).set(getPieceIndex(pieceIndex));
//						}
						break;
					}
					case START: {
						byte[] handShakeMsg = new byte[32];
						System.out.println("SERVER:- server waiting to read");
						System.out.println("SERVER:- STARTE STATE ");
						in.read(handShakeMsg);
						if (Peer.getInstance().validateHandShakeMsg(handShakeMsg)) {
							System.out.printf("SERVER:- Received message: <%s> from client %d\n" ,new String(handShakeMsg), neighbor.peerId);
							System.out.println("SERVER:- Neighbor <" + neighbor.peerId +"> validated");
							//if not validated it does not proceed further
							neighbor.setServerState(ScanState.RXVED_HAND_SHAKE);
						}
						break;
					} 
					case RXVED_HAND_SHAKE: {
						System.out.println("SERVER:- Sent handShake message ");
						System.out.println("SERVER:- RCVED HAND SHAKE STATE ");
						sendHandShake();
						neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
						neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
						break;
					}
					case KILL:{
						System.out.println("SERVER:- KILL STATE ");
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

		private int genPieceIndex() {
			BitSet myBitfield = Peer.getInstance().bitField;
			BitSet neighborBitset = Peer.getInstance().neighborsBitSet.get(neighbor.peerId);
			int resultPieceIndex=-1;
			for(int i=0;i<Peer.getInstance().noOfPieces;i++){
				if(!myBitfield.get(i) && neighborBitset.get(i) && !Peer.getInstance().requestedbitField.get(i)){
					Peer.getInstance().requestedbitField.set(i);
					resultPieceIndex=i;
					break;
				}
			}
			
			return resultPieceIndex;
		}

		private void sendRequestMessage(byte[] pieceIndex) {
			sendMessage(msgWithPayLoad(MsgType.REQUEST, pieceIndex));
		}

		private void sendHandShake() {
			String handShake = Constants.HANDSHAKEHEADER + Constants.ZERO_BITS + Peer.getInstance().peerID;
			sendMessage(handShake.getBytes());
		}

		//send a message to the output stream
		public void sendMessage(byte[] msg)
		{
			try{
				out.write(msg);
				out.flush();
				System.out.printf("SERVER:- Sent message:<%s> to Client %d\n" ,msg, neighbor.peerId);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

		
		private void sendInterested(){
			sendMessage(msgWithoutPayLoad(MsgType.INTERESTED));
		}
		
		private void sendNotInterested(){
			sendMessage(msgWithoutPayLoad(MsgType.NOT_INTERESTED));
		}
		
		private byte[] msgWithPayLoad(MsgType msgType, byte[] payLoad) {
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
		
		private int getPieceIndex(byte[] pieceIndex) {
			int integerValue = 0;
	        for (int index = 0; index < 4; index++) {
	            int shift = (4 - 1 - index) * 8;
	            integerValue += (pieceIndex[index] & 0x000000FF) << shift;
	        }
	        return integerValue;
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
	}
}
