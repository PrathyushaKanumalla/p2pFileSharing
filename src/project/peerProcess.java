
package project;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import project.Constants.ScanState;


public class peerProcess {
	private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
	private static boolean stateCheck = true;
	private static boolean initialFlow = true;
	private static List<Integer> unchokeList =  Collections.synchronizedList(new ArrayList<>());
			
	public static void setCommonConfig() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(Constants.COMMONCFG));
		String configLine = null;

		while ((configLine = br.readLine()) != null) {

			String[] split = configLine.split(" ");
			Peer.getInstance().configProps.put(split[0], split[1]);
			
			int fileSize = Integer.parseInt(Peer.getInstance().configProps.get("FileSize"));
			int pieceSize = Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"));
			
			int noOfPieces=0;
			if(fileSize % pieceSize ==0){
				noOfPieces = fileSize/pieceSize;
			}
			else{
				noOfPieces = fileSize/pieceSize;
				int excess = fileSize - fileSize * noOfPieces;
				Peer.getInstance().excessPieceSize = excess;
				noOfPieces+=1;
			}
			Peer.getInstance().noOfPieces = noOfPieces;
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
				int k = Integer.parseInt(Peer.getInstance().configProps.get("NumberOfPreferredNeighbors"));
				int p =Integer.parseInt(Peer.getInstance().configProps.get("UnchokingInterval"));
				int m = Integer.parseInt(Peer.getInstance().configProps.get("OptimisticUnchokingInterval"));
				
				
				setPeerNeighbors(currPeerId);
				// Step 4: initiate download-connections (create a server)
				// and evaluate pieces in it. -- in a method
				// if the download is done -- stop all the threads of download
				//syso the same.
				// Step 5: initiate uploading-thread 
				// ->always selects k+1 neighbors and sends data
				determineKPreferred(k,p);
				Server serverThread = new Server();
				serverThread.start();
				for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
					Client clientThread = new Client(neighbor.getValue());
					clientThread.start();
				}
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
		Peer.getInstance().peerID = currPeerId;
		String row = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(Constants.PEERINFO));
			while((row = in.readLine()) != null) {
				 String[] tokens = row.split("\\s+");
				 Integer peerId = new Integer(tokens[0]);
			     if (peerId == currPeerId) {
			    	 Peer.getInstance().portNum = tokens[2];
			     } else {
			    	 Peer.getInstance().neighbors.put(peerId, 
				    		 new RemotePeerInfo(peerId, tokens[1], tokens[2]));
			     }
			     if (tokens[3].equals("1")) {
			    	 Peer.getInstance().bitField.flip(0, Peer.getInstance().noOfPieces);
			     }
			}
			in.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	//Currently not needed but we may need in future, otherwise delete/comment the below method 
	public static int countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}
	
	public static int previouslyOptimisticallyPeer;
	
	public static void determineOptimisticallyUnchokedPeer(int m){
		final Runnable determineOUnchokedPeer = new Runnable(){
			public void run(){
				List<Integer> chokeList = new ArrayList<Integer>();
				for(int i=0;i<Peer.getInstance().interestedInMe.size();i++){
					if(!unchokeList.contains(Peer.getInstance().interestedInMe.get(i))){
						chokeList.add(Peer.getInstance().interestedInMe.get(i));
					}
				}
				int size = chokeList.size();
				if(size!=0){
					int randIndex = ThreadLocalRandom.current().nextInt(0, size);
                    int peer = chokeList.remove(randIndex);
                    //no need to check for null
                    if(peer!=previouslyOptimisticallyPeer){
                    	RemotePeerInfo peerInfo = Peer.getInstance().neighbors.get(peer);
                    	peerInfo.isOptimisticallyChosen=true;
                    	peerInfo.setClientState(ScanState.UNCHOKE);
                    	if(previouslyOptimisticallyPeer!=0){
                    		RemotePeerInfo tempInfo = Peer.getInstance().neighbors.get(previouslyOptimisticallyPeer);
                    		tempInfo.isOptimisticallyChosen=false;
                    		tempInfo.setClientState(ScanState.CHOKE);
                    	}
                    	previouslyOptimisticallyPeer = peer;
                    }
				}
				else if(previouslyOptimisticallyPeer!=0){
					RemotePeerInfo tempInfo = Peer.getInstance().neighbors.get(previouslyOptimisticallyPeer);
					tempInfo.isOptimisticallyChosen=false;
					tempInfo.setClientState(ScanState.CHOKE);
					previouslyOptimisticallyPeer = 0;
				}
			}
		};
		scheduler.scheduleAtFixedRate(determineOUnchokedPeer, m, m, SECONDS);
	}
	
	public static void determineKPreferred(int k, int p) throws IOException{
		final Runnable kNeighborDeterminer = new Runnable() {
            public void run() {
				if(stateCheck){
					boolean isFlag = true;
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						RemotePeerInfo val = neighbor.getValue();
						if(val.getClientState()!=ScanState.UPLOAD_START){
							isFlag = false;
						}
					}
					if(isFlag){
						System.out.println("All clients are in UPLOAD START state");
						stateCheck=false;
					}
				}
				else{
					List<Integer> peerList = Peer.getInstance().interestedInMe;
					Map<Integer, Double> peerVsDownrate = new HashMap<Integer, Double>();
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						if(peerList.contains(neighbor.getKey())){
							peerVsDownrate.put(neighbor.getKey(), neighbor.getValue().downRate);
						}
					}
					peerVsDownrate = MapSortByValue.sortByValue(peerVsDownrate);
					if(peerList!=null){
						Iterator<Integer> iterator = peerList.iterator();
						int count =0;
						if(initialFlow){
							while(count<k && iterator.hasNext()){
								int prefPeer = iterator.next();
								unchokeList.add(prefPeer);
								RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(prefPeer);
								prefPeerInfo.setClientState(ScanState.UNCHOKE);
								count++;
							}
							initialFlow= false;
						}
						else{
							List<Integer> tempList = new ArrayList<Integer>();
							while(count<k && iterator.hasNext()){
								int prefPeer = iterator.next();
								tempList.add(prefPeer);
								if(unchokeList.contains(prefPeer)){
									unchokeList.remove(prefPeer);
								}
								count++;
							}
							for(int i=0;i<unchokeList.size();i++){
								RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(unchokeList.get(i));
								prefPeerInfo.setClientState(ScanState.CHOKE);
							}
							unchokeList.clear();
							unchokeList = tempList;
						}
					}
				}
            }
		};
		final ScheduledFuture<?> kNeighborDeterminerHandle = scheduler.scheduleAtFixedRate(kNeighborDeterminer, p, p, SECONDS);
	}

}
