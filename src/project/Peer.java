package project;

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
	
	
	
	
	
}
