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
	public byte[] bitField;
	public Map<String, String> configProps = Collections.synchronizedMap(new HashMap<>());
	public Map<Integer, RemotePeerInfo> neighbors = Collections.synchronizedMap(new HashMap<Integer, RemotePeerInfo>());
	public String portNum;
	
	synchronized boolean validateHandShakeMsg(byte[] handShakeMsg) {
		String message = new String(handShakeMsg);
		if (message.startsWith(Constants.HANDSHAKEHEADER) &&
				neighbors.containsKey(Integer.parseInt(message.substring(28, 32)))) {
			return true;
		}
		return false;
	}

	public boolean validateBitFieldMsg(byte[] bitFieldMsg) {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean hasCompleteFile() {
		// TODO Auto-generated method stub
		return false;
	}
}
