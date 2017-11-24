package project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;


public class peerProcess {
	
	public static void setCommonConfig() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(Constants.COMMONCFG));
		String configLine = null;

		while ((configLine = br.readLine()) != null) {

		String[] split = configLine.split(" ");
		Peer.getInstance().configProps.put(split[0], split[1]);

		}
		br.close();
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int currPeerId;
		if (args.length > 0) {
			try {
				currPeerId = Integer.parseInt(args[0]);
				System.out.printf("*Peer %d started successfully*", currPeerId);
				// Step 1: Initiate Logs
				Log.setUpSingleTonLog(currPeerId);
				Log.addLog("Success!!");
				// Step 2: Set up Config Information
				setCommonConfig();
					//insert variable as key and store its value....
					//commonInf.put(split[0], split[1]);				
				// Step 3: Set up Peer Information
				setPeerNeighbors(currPeerId);
				Peer.getInstance().peerID=currPeerId;
				// Step 4: initiate download-connections (create a server)
				// and evaluate pieces in it. -- in a method
				// if the download is done -- stop all the threads of download
				//syso the same.
				Server serverThread = new Server(Peer.getInstance().portNum, Peer.getInstance().neighbors.keySet());
				serverThread.start();
				Thread.sleep(1000);
				// Step 5: initiate uploading-thread 
				// ->always selects k+1 neighbors and sends data
				System.out.println("here");
				for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
					Client clientThread = new Client(neighbor.getValue().peerAddress, neighbor.getValue().peerPort);
					clientThread.start();
				}
				System.out.println("end");
			} catch (NumberFormatException e) {
		        System.err.println("Argument " + args[0] + " must be an integer.");
		        System.exit(1);
		    }
		} else {
			System.err.println("Please provide peer ID attribute to start");
			System.exit(1);
		}
	}

	private static void setPeerNeighbors(int currPeerId) {
		String row = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(Constants.PEERINFO));
			while((row = in.readLine()) != null) {
				 String[] tokens = row.split("\\s+");
			     Peer.getInstance().neighbors.put(new Integer(tokens[0]), 
			    		 new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			}
			Peer.getInstance().portNum = Peer.getInstance().neighbors.get(currPeerId).peerPort;
			Peer.getInstance().neighbors.remove(currPeerId);
			in.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

}
