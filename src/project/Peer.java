package project;

import java.util.Collections;
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
	

	public int peerID;
	public Map<String, String> configProps = Collections.synchronizedMap(new HashMap<>());
	public Map<Integer, RemotePeerInfo> neighbors = Collections.synchronizedMap(new HashMap<Integer, RemotePeerInfo>());
	public static String portNum;
}
