package project;

import java.util.HashMap;

public class Peer {
	
	private Peer() {}
	
	private static class PeerSingletonHelper {
		private static final Peer peer = new Peer();
	}
	
	public static Peer getInstance() {
		return PeerSingletonHelper.peer;
	}
	
	public int peerID;
	
	
}
