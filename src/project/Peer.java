package project;

import java.util.HashMap;
import java.util.Map;

public class Peer {
	
	private Peer() {}
	
	private static class PeerSingletonHelper {
		private static final Peer peer = new Peer();
	}
	
	public static Peer getInstance() {
		return PeerSingletonHelper.peer;
	}
	
	public static Map<Integer,String> getPeerInf() {
		return peerInf;
	}
	public static void setPeerInf(Map<Integer,String> peerInf) {
		Peer.peerInf = peerInf;
	}

	public static Map<String,String> getCommonInf() {
		return commonInf;
	}

	public static void setCommonInf(Map<String,String> commonInf) {
		Peer.commonInf = commonInf;
	}

	public int peerID;
	private static Map<Integer,String> peerInf=null;
	private static Map<String,String> commonInf=null;
	
}
