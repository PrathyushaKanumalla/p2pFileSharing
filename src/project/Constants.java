package project;

public class Constants {

	public static final String LOGFILEPREFIX = "log_peer_";
	public static final String LOGFILESUFFIX = ".log";
	public static final String LOGTIMEFORMAT = "MMM dd,yyyy HH:mm:ss";
	
	public static final String COMMONCFG = "Common.cfg";
	public static final String PEERINFO = "PeerInfo.cfg";
	
	public static final  byte[] HANDSHAKEHEADER = "P2PFILESHARINGPROJ".getBytes();
	public static final byte[] ZERO_BITS = {0,0,0,0,0,0,0,0,0,0};
}
