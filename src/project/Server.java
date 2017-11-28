
package project;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
						neighbor.setServerState(ScanState.SERVER_LISTEN);
					}
					case SERVER_LISTEN: {

						/**listen  for messages
					1. initial && if bitfield -> send interested/not interested, set initial = false;
					 2. unchoke message -> state = unchoke; set initial = false if it is true;
					listen for unchoke/choke msges
					state = unchoke or choke**/
						byte [] msg=new byte[5];
						in.read(msg);
						Constants.MsgType msgType=Constants.getMsgType(msg);

						if(neighbor.initial && msgType==Constants.MsgType.BITFIELD  )
						{

							if(compareBitField())
							{ 
								sendInterestedMessage();
							}
							else
							{
								sendNotInterestedMessage();
							}

							neighbor.initial=false;
						}

						if(msgType==Constants.MsgType.UNCHOKE)
						{
							neighbor.setServerState(Constants.ScanState.UNCHOKE);
							if(neighbor.initial)
							{
								neighbor.initial=false;
							}
						}
						if(msgType==Constants.MsgType.CHOKE)
						{
							neighbor.setServerState(Constants.ScanState.CHOKE);

						}


					}
					case UNCHOKE: {

						/**respond with request or not interested
						if request -> state = PIECE
						or send not interested and go to server_listen*/
						byte [] msg=new byte[5];
						in.read(msg);
						Constants.MsgType msgType=Constants.getMsgType(msg);
						
						if(compareBitField)
						{
							sendRequestMessage();
							neighbor.setServerState(Constants.ScanState.PIECE);
						}
							
						else
						{
							sendNotInterestedMessage();
							neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
						}
						
					}
					case PIECE: {
						
						/**listen for a piece msg;
						if piece -> update bit field
						->then update  updatePieceInfo in peer;
						send request again by putting state = unchoke;
						if choke occurs -go to server listen again**/
						byte [] msg=new byte[5];
						in.read(msg);
						Constants.MsgType msgType=Constants.getMsgType(msg);
						
						if(msgType==Constants.MsgType.PIECE)
						{
							updateBitField();
							updatePieceInfo();
						    neighbor.setServerState(Constants.ScanState.UNCHOKE);
						}
						if(msgType==Constants.MsgType.CHOKE)
						{
							neighbor.setServerState(Constants.ScanState.SERVER_LISTEN);
						}
					}
					case START: {
						byte[] handShakeMsg = new byte[32];
						System.out.println("SERVER:- server waiting to read");
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
						sendHandShake();
						neighbor.setServerState(ScanState.DONE_HAND_SHAKE);
						neighbor.setClientState(ScanState.DONE_HAND_SHAKE);
						break;
					}
					//case AFTER_HAND_SHAKE:{
					//If I receive bit field message -> update neighbors bit field
					// change state to SERVER_RXVED_BIT_FIELD
					//
					/*byte[] bitFieldMsg = new byte[9];
					in.read(bitFieldMsg);
					if (Peer.getInstance().validateBitFieldMsg(bitFieldMsg)) {
						//do something related to this
						System.out.println("SERVER:- Received bit field message ");
						neighbor.setState(ScanState.SERVER_RXVED_BIT_FIELD);
					}*/
					//break;
					//}
					/*case SERVER_RXVED_BIT_FIELD:{
					//if I have file 
					//send not interested
					//and state = SERVER_SENT_BIT_FIELD
					sendBitField();
					System.out.println("SERVER:- Sent Bit field ack");
					neighbor.setState(ScanState.SERVER_LISTEN);
					break;
					// neighbor.setState(ScanState.)
				}
				case SERVER_SENT_BIT_FIELD :{
					//wait for int/ not interested msg
					byte[] interestedMsg = new byte[10];
					in.read(interestedMsg);
					System.out.println("SERVER:- Received interested messgae- " + new String(interestedMsg));
					neighbor.setState(ScanState.SERVER_LISTEN);
					break;
				}*/
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

		//private void sendBitField(){
			//String bitField = "server123";
			//sendMessage(bitField);
	//	}
		
		
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
		private byte[] createPrefix(int len) {
			byte[] prefix = new byte[4];
			prefix[0] = (byte) ((len & 0xFF000000) >> 24);
			prefix[1] = (byte) ((len & 0x00FF0000) >> 16);
			prefix[2] = (byte) ((len & 0x0000FF00) >> 8);
			prefix[3] = (byte) (len & 0x000000FF);
	        return prefix;
	    }
		private byte[] msgWithoutPayLoad(MsgType msgType) {
			byte[] message = new byte[5];
			System.arraycopy(createPrefix(1), 0, message, 0, 4);
			message[4] = msgType.value;
			return message;
		}
		private void sendInterestedMessage()
		{
			sendMessage(msgWithoutPayLoad(Constants.MsgType.INTERESTED));
		}
		private void sendNotInterestedMessage()
		{
			sendMessage(msgWithoutPayLoad(Constants.MsgType.NOT_INTERESTED));
		}
		private void sendChokeMessage()
		{
			sendMessage(msgWithoutPayLoad(Constants.MsgType.CHOKE));
		}
		private void sendUnChokeMesssage()
		{
			sendMessage(msgWithoutPayLoad(Constants.MsgType.UNCHOKE));
		}
		
		private void sendRequestMessage()
		{
			sendMessage(msgWithPayLoad(Constants.MsgType.REQUEST, Peer.getInstance().bitField, null));
		}

		/*private String getMsgType (byte a[])
		{

			char []message=(new String(a)).toCharArray();

			switch(message[4])
			{
			case '0':
				return "CHOKE";
			case '1':
				return "UNCHOKE";

			case '2':
				return "INTERESTED";


			case '3':
				return "NOT INTERESTED";

			case '4':
				return "HAVE";

			case '5':
				return "BITFIELD";

			case '6':
				return "REQUEST";

			case '7':
				return "PIECE";

			default:
				return "INVALID MSG";


			}	
		}*/

	}

}
