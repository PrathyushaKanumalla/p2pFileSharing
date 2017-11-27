package project;

public class Constants {

	public static final String LOGFILEPREFIX = "log_peer_";
	public static final String LOGFILESUFFIX = ".log";
	public static final String LOGTIMEFORMAT = "MMM dd,yyyy HH:mm:ss";
	
	public static final String COMMONCFG = "Common.cfg";
	public static final String PEERINFO = "PeerInfo.cfg";
	
	public static final String HANDSHAKEHEADER="P2PFILESHARINGPROJ";
	public static final String ZERO_BITS="0000000000";
	
	public static enum ScanState{
		CLIENT_START("CLIENT_START"),
		SERVER_START("START"), 
		SERVER_SENT_HAND_SHAKE("SENT_HAND_SHAKE"), 
		SERVER_RXVED_HAND_SHAKE("RXVED_HAND_SHAKE"), 
		SERVER_SENT_BIT_FIELD("SENT_BIT_FIELD"),
		SERVER_RXVED_BIT_FIELD("RXVED_BIT_FIELD"),
		SERVER_SENT_INTERESTED("SENT_INTERESTED"), 
		SERVER_RXVED_INTERESTED("SERVER_RXVED_INTERESTED"),
		CLIENT_SENT_HAND_SHAKE("CLIENT_SENT_HAND_SHAKE"), 
		CLIENT_RXVED_HAND_SHAKE("CLIENT_RXVED_HAND_SHAKE"), 
		CLIENT_SENT_BIT_FIELD("CLIENT_SENT_BIT_FIELD"), 
		CLIENT_RXVED_BIT_FIELD("CLIENT_RXVED_BIT_FIELD"),
		CLIENT_SENT_INTERESTED("CLIENT_SENT_INTERESTED"), 
		CLIENT_RXVED_INTERESTED("CLIENT_RXVED_INTERESTED"),
		DEFAULT("DEFAULT");
		
		ScanState(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}
}
