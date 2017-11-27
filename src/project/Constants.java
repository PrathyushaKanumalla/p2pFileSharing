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
		START("START"),
		SENT_HAND_SHAKE("SENT_HAND_SHAKE"),
		DONE_HAND_SHAKE("DONE_HAND_SHAKE"),
		SENT_BIT_FIELD("SENT_BIT_FIELD"),
		RXVED_HAND_SHAKE("RXVED_HAND_SHAKE"),
		UPLOAD_START("UPLOAD_START"),
		UNCHOKE("UNCHOKE"),
		RXVE_REQUEST("RXVE_REQUEST"),
		PIECE("PIECE"),
		DEFAULT("DEFAULT"), 
		SERVER_LISTEN("SERVER_LISTEN"),
		CHOKE("CHOKE");
		
		ScanState(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}
}
