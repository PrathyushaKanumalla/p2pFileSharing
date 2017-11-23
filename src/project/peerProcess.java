package project;

import java.io.BufferedReader;
<<<<<<< HEAD
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
=======
import java.io.FileReader;
import java.util.Map.Entry;
>>>>>>> 6fdf4281beed81a1d407eb8ca3ca954fc2d854e4

public class peerProcess {
	
	//private final static String peerInfoFile = "PeerInfo.cfg";
	//private final static String commonFile = "Common.cfg";
	
	public static void setCommonConfig() throws IOException
	{
		FileInputStream fis;
		File file=new File(Constants.COMMONCFG);
		fis = new FileInputStream(new File(Constants.COMMONCFG));
		
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	 
		String configLine = null;
		
		while ((configLine = br.readLine()) != null) {
			
			String[] split = configLine.split(" ");
			Peer.getCommonInf().put(split[0], split[1]);
		}
		br.close();
	
		
	}
	
	
	public static void main(String[] args) throws IOException {
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
				
				// Step 4: initiate download-connections (create a server)
				// and evaluate pieces in it. -- in a method
				// if the download is done -- stop all the threads of download
				//syso the same.
				// Step 5: initiate uploading-thread 
				// ->always selects k+1 neighbors and sends data
				
			
				
				setPeerNeighbors(currPeerId);

				
			
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
			Peer.getInstance().neighbors.remove(currPeerId);
			in.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

}
