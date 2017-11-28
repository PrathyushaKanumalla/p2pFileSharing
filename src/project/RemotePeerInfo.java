package project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import project.Constants.ScanState;

/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

public class RemotePeerInfo {
	public int peerId;
	public String peerAddress;
	public String peerPort;
	public byte[] peerBitFieldInfo;
	public boolean flag;
	private ScanState clientState =  ScanState.DEFAULT;
	private ScanState serverState =  ScanState.DEFAULT;
	private boolean updatePieceInfo = false;
	public List<byte[]> piecesRxved = Collections.synchronizedList(new ArrayList<>());

	public RemotePeerInfo(Integer pId, String pAddress, String pPort) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
	}
	
	public synchronized ScanState getClientState() {
		return clientState;
	}

	public synchronized void setClientState(ScanState clientState) {
		this.clientState = clientState;
	}

	public ScanState getServerState() {
		return serverState;
	}

	public void setServerState(ScanState serverState) {
		this.serverState = serverState;
	}
	
	public synchronized boolean isUpdatePieceInfo() {
		return updatePieceInfo;
	}

	public synchronized void setUpdatePieceInfo(boolean updatePieceInfo) {
		this.updatePieceInfo = updatePieceInfo;
	}

}
