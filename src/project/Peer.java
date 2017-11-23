package project;

public class Peer {
	
	private Peer() {}
	
	private static class SingletonPeerHelper {
		private static final Peer myPeer = new Peer();
	}
	
	private static Peer getInstance() {
		return SingletonPeerHelper.myPeer;
	}
	
	public int peerID;
	public int numOfPrefferredNeighbors;
	public int unchokingInterval;
	public int optimisticUnchokingInterval;
	public String fileName;
	public int pieceSize;
	
	
	
	
	
}
