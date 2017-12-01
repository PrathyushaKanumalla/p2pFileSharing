package project;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Peer {
	
	private Peer() {}
	
	private static class PeerSingletonHelper {
		private static final Peer peer = new Peer();
	}
	
	public static Peer getInstance() {
		return PeerSingletonHelper.peer;
	}

	public int peerID;
	private BitSet bitField = new BitSet();
	public BitSet requestedbitField = new BitSet();
	public BitSet fileBitfield= new BitSet();
	public int noOfPieces;
	public Map<String, String> configProps = Collections.synchronizedMap(new HashMap<>());
	public Map<Integer, RemotePeerInfo> neighbors = Collections.synchronizedMap(new HashMap<Integer, RemotePeerInfo>());
	public List<Integer> interestedInMe = Collections.synchronizedList(new ArrayList<>());
	public String portNum;
	public Receivedpieces[] pieces;
	public int excessPieceSize = 0;
	public Map<Integer, BitSet> neighborsBitSet = Collections.synchronizedMap(new HashMap<>());
	public boolean hasCompletefile = false;
	public boolean stopped= false;
	public Map<Integer, Client> neighborThreads = Collections.synchronizedMap(new HashMap<>());
	
	synchronized boolean validateHandShakeMsg(byte[] handShakeMsg) {
		String message = new String(handShakeMsg);
		if (message.startsWith(Constants.HANDSHAKEHEADER) &&
				neighbors.containsKey(Integer.parseInt(message.substring(28, 32)))) {
			return true;
		}
		return false;
	}
	
	public synchronized BitSet getBitField() {
		return bitField;
	}

	public synchronized void setBitField(BitSet bitField) {
		this.bitField = bitField;
	}

	public boolean hasCompleteFile() {
		// TODO Auto-generated method stub
		
		return Peer.getInstance().hasCompletefile;
	}

	public synchronized RemotePeerInfo search(InetAddress inetAddress, int port) {
		//System.out.println(System.currentTimeMillis() + " received this Inet address - "+inetAddress.getHostAddress());
		for (RemotePeerInfo neighbor : neighbors.values()) {
			if (neighbor.peerAddress.equals(inetAddress.getHostAddress())) {
				return neighbor;
			}
		}
		return null;
	}
}
