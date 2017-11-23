package project;

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
	public int numOfPrefferredNeighbors;
	public int unchokingInterval;
	public int optimisticUnchokingInterval;
	public String fileName;
	public int pieceSize;
	public static Map<Integer,String> peerInf=null;
	public static Map<String,String> commonInf=null;
	
	
	
}
