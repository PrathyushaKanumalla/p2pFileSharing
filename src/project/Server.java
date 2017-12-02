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
				new Handler(listener.accept(), Peer.getInstance().neighbors.get(clientPeerId)).start();
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
				if (neighbor.getServerState().equals(ScanState.START))
					Log.addLog(String.format("Peer %d is connected from Peer %d", Peer.getInstance().peerID, neighbor.peerId));
				if (neighbor == null) {
					System.out.println("neighbor is null");
				}
				
				while (!Peer.getInstance().stopped) {
					/** if i receive have msg
					send interested or not interested as response; 
					 **/
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
							if (in.available() > 0) {
								byte[] msg = new byte[5];
								in.read(msg);
								if (neighbor.initial && msg[4] == MsgType.BITFIELD.value) {
									int bitFieldSize = Peer.getInstance().noOfPieces;
									byte[] bitField = new byte[bitFieldSize];
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
									Log.addLog(String.format("Peer %s is unchoked by %s", Peer.getInstance().peerID, neighbor.peerId));
									if (neighbor.initial) 
										neighbor.initial = false;
									byte[] pieceIndex = new byte[4];
									int genPieceindx = genPieceIndex();
									if (genPieceindx == -1) {
										sendNotInterested();
										break;
									}
									pieceIndex = createPrefix(genPieceindx);
									if (pieceIndex != null) {
										neighbor.startTime.put(neighbor.peerId, System.currentTimeMillis());
										sendRequestMessage(pieceIndex);
										neighbor.setStoredIndex(genPieceindx);
										System.out.println("SERVER:- SENT this request message of piece- " +genPieceindx +" to peer id " +neighbor.peerId );
									}
								} else if (msg[4] == MsgType.CHOKE.value) {
									Log.addLog(String.format("Peer %s is choked by %s", Peer.getInstance().peerID, neighbor.peerId));
									System.out.println("SERVER:- received choke message from " + neighbor.peerId);
									System.out.println("stored index after choke - "+neighbor.getStoredIndex());
									if (neighbor.getStoredIndex() != -1) {
										Peer.getInstance().getRequestedbitField().clear(neighbor.getStoredIndex());
										System.out.println("SERVER:- My current request bit field - " + Peer.getInstance().getRequestedbitField());
										neighbor.setStoredIndex(-1);
									}
								} else if (msg[4] == MsgType.HAVE.value) {
									System.out.println("SERVER:- HAVE MESSGAGE IN SERVER_LISTEN BLOCK");
									byte[] pieceIndex = new byte[4];
									in.readFully(pieceIndex);
									System.out.println("SERVER:- Received HAVE INDEX "+ getPieceIndex(pieceIndex) 
											+ " from peer id "+ neighbor.peerId);
									int gotThisPeerIndex = getPieceIndex(pieceIndex);
									Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).set(gotThisPeerIndex);
									System.out.println("SERVER:- neighbor " + neighbor.peerId + " & bitset is - "+ Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId));
									Log.addLog(String.format("Peer %d received the 'have' message from %d for the piece %d", 
											Peer.getInstance().peerID, neighbor.peerId, gotThisPeerIndex));
									BitSet myBitfield = Peer.getInstance().getBitField();
									BitSet neighborBitset = Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId);
									boolean sendInterested = false;
									for (int i = 0;i < Peer.getInstance().noOfPieces;i++) {
										if (!myBitfield.get(i) && neighborBitset.get(i)) {
											sendInterested = true;
											break;
										}
									}
									if (sendInterested) 
										sendInterested();
									else
										sendNotInterested();
								} else if (msg[4] == MsgType.PIECE.value) {
									byte[] reqPieceIndex = new byte[4];
									in.read(reqPieceIndex);
									int reqPieceInd = getPieceIndex(reqPieceIndex);
									System.out.println("SERVER:- Received this piece message - " + reqPieceInd + " from peer " + neighbor.peerId);
									int pieceSize = 0;
									if (reqPieceInd != Peer.getInstance().noOfPieces-1) {
										pieceSize = Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"));
										byte[] piece = new byte[pieceSize];
										in.readFully(piece);
										System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
												" from peer " + neighbor.peerId );
										Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
									} else {
										pieceSize = Peer.getInstance().excessPieceSize;
										byte[] piece = new byte[pieceSize];
										in.readFully(piece);
										System.out.println("SERVER - length of " + reqPieceInd + " is " + piece.length + 
												" from peer " + neighbor.peerId );
										Peer.getInstance().pieces[reqPieceInd] = new Receivedpieces(piece);
									}
									neighbor.setStoredIndex(-1);
									Log.addLog(String.format("Peer %d has downloaded the piece %d from %d", 
											Peer.getInstance().peerID, reqPieceInd, neighbor.peerId));
									if (neighbor.startTime.containsKey(neighbor.peerId)) {
										Long endTime = System.currentTimeMillis();
										Long downtime = endTime - neighbor.startTime.get(neighbor.peerId);
										System.out.println("downtime here **** -> "+downtime);
										System.out.println("pieceSize here **** -> "+pieceSize);
										if (downtime != 0) {
											System.out.println("download rate is **** -> "+ (double)(pieceSize/downtime));
											Peer.getInstance().downloadTime.put(neighbor.peerId, (double)(pieceSize)/downtime);
										} else {
											System.out.println("download rate is set to max value");
											Peer.getInstance().downloadTime.put(neighbor.peerId, Double.MAX_VALUE);
										}
									}
									pieceSize = 0;
									Peer.getInstance().getBitField().set(reqPieceInd);
									//update all neighbors
									System.out.println("SERVER:- My bit field " + Peer.getInstance().getBitField());
									for (RemotePeerInfo neighbor : Peer.getInstance().neighbors.values()) {
										System.out.println("SERVER:- sending have message " + reqPieceInd + " to -> "+ neighbor.peerId);
										if (!neighbor.isUpdatePieceInfo())
											neighbor.setUpdatePieceInfo(true);
										neighbor.prxd.put(reqPieceInd, reqPieceIndex);
										System.out.println("SERVER:- done  have message " + reqPieceInd + "to -> "+ neighbor.peerId);
									}
									byte[] pieceIndex = new byte[4];
									int genPieceindx = genPieceIndex();
									if (genPieceindx == -1) {
										sendNotInterested();
										break;
									}
									pieceIndex = createPrefix(genPieceindx);
									if (pieceIndex != null) {
										sendRequestMessage(pieceIndex);
										neighbor.setStoredIndex(genPieceindx);
										System.out.println("SERVER:- SENT this request message of piece- " +genPieceindx +" to peer id " +neighbor.peerId );
									}
								}
							}
							break;
						}
					case START: {
						byte[] handShakeMsg = new byte[32];
						if (in.available() > 0) {
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
						Peer.getInstance().neighborThreads.get(neighbor.peerId).start();
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
			}
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

		private synchronized int genPieceIndex() {
			List<Integer> possibleRequests = new ArrayList<>();
			for (int i = 0;i < Peer.getInstance().noOfPieces;i++) {
				if (!Peer.getInstance().getBitField().get(i) && Peer.getInstance().getNeighborsBitSet().get(neighbor.peerId).get(i) 
						&& !Peer.getInstance().getRequestedbitField().get(i)) {
					possibleRequests.add(i);
				}
			}
			if (possibleRequests.isEmpty())
				return -1;
			Random rand = new Random();
			int randomIndex = rand.nextInt(possibleRequests.size());
			System.out.println("SERVER:- Request index genrated random for piece - " + possibleRequests.get(randomIndex));
			Peer.getInstance().getRequestedbitField().set(possibleRequests.get(randomIndex));
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