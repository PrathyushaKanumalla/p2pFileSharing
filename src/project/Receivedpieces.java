package project;

public class Receivedpieces {
	
	byte[] pieceInfo;
	
	public Receivedpieces() {
		pieceInfo = new byte[Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"))];
	}
	
}
