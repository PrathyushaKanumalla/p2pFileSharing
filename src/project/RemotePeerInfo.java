package project;

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
	private ScanState state =  ScanState.DEFAULT;
	
	public synchronized ScanState getState() {
		return state;
	}

	public synchronized void setState(ScanState state) {
		this.state = state;
	}

	public RemotePeerInfo(Integer pId, String pAddress, String pPort) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
	}
}
