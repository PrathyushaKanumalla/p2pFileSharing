package project;

import java.io.File;

public class Constants {

	public static final String LOGFILEPREFIX = "log_peer_";
	public static final String LOGFILESUFFIX = ".log";
	public static final String LOGTIMEFORMAT = "MMM dd,yyyy HH:mm:ss";

	public static final String COMMONCFG = "Common.cfg";
	public static final String PEERINFO = "PeerInfo.cfg";

	public static final String HANDSHAKEHEADER = "P2PFILESHARINGPROJ";
	public static final String ZERO_BITS = "0000000000";
	public static final String ECLIPLSE_FILE_PREFIX = System.getProperty("user.dir") + File.separator + "src" + File.separator +"project" + File.separator ;
	public static final String FILE_PREFIX = System.getProperty("user.dir") + File.separator + "project" + File.separator ;
	//public static final FILE_PREFIX = ECLIPLSE_FILE_PREFIX;

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
		CHOKE("CHOKE"),
		KILL("KILL"),
		HAVE("HAVE");

		ScanState(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}

	public enum MsgType{
		CHOKE((byte)0), 
		UNCHOKE((byte)1), 
		INTERESTED((byte)2), 
		NOT_INTERESTED((byte)3), 
		HAVE((byte)4), 
		BITFIELD((byte)5), 
		REQUEST((byte)6), 
		PIECE((byte)7);
		byte value = -1;
		private MsgType(byte n){
			this.value = n;
		}
	}

}
