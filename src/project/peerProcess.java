
package project;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
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
	private static ScheduledExecutorService determinekscheduler = Executors.newScheduledThreadPool(5);
	private static ScheduledExecutorService optimisticallyscheduler = Executors.newScheduledThreadPool(5);
	private static ScheduledExecutorService shutdownscheduler = Executors.newScheduledThreadPool(5);
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
//			String test = Peer.getInstance().configProps.get("FileSize");
//			int val = Integer.parseInt(test);
			
		}
		int fileSize = Integer.parseInt(Peer.getInstance().configProps.get("FileSize"));
		int pieceSize = Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"));
		
		int noOfPieces=0;
		
		if(fileSize % pieceSize ==0){
			noOfPieces = fileSize/pieceSize;
		}
		else{
			noOfPieces = fileSize/pieceSize;
			int excess = fileSize - pieceSize * noOfPieces;
			Peer.getInstance().excessPieceSize = excess;
			System.out.println("excess size- "+excess);
			noOfPieces+=1;
		}
		Peer.getInstance().noOfPieces = noOfPieces;
		int i;
		for(i=0;i<Peer.getInstance().noOfPieces -1 ;i++){
			Peer.getInstance().fileBitfield.set(i);
		}
		int tempPieces = Peer.getInstance().excessPieceSize;
		while(tempPieces>0){
			Peer.getInstance().fileBitfield.set(i);
			i++;
			tempPieces--;
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

				Server serverThread = new Server();
				serverThread.start();
				for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
					Client clientThread = new Client(neighbor.getValue());
					Peer.getInstance().neighborThreads.put(neighbor.getKey(), clientThread);
					clientThread.start();
				}
				Thread.sleep(1000);
				determineKPreferred(k,p);
//				determineOptimisticallyUnchokedPeer(m);
				shutdownChecker();
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
			//Peer.getInstance().bitField = new BitSet(Peer.getInstance().noOfPieces);
			
//			for(int i=0;i<Peer.getInstance().noOfPieces;i++){
//				Peer.getInstance().bitField.clear(i);
//			}
			//Peer.getInstance().bitField.clear(0, Peer.getInstance().noOfPieces);
//			System.out.println("my bitfield-> "+Peer.getInstance().bitField.toString());
//			for(int i=0;i<Peer.getInstance().noOfPieces;i++){
//				System.out.println(Peer.getInstance().bitField.get(i));
//			}
			while((row = in.readLine()) != null) {
				 String[] tokens = row.split("\\s+");
				 Integer peerId = new Integer(tokens[0]);
			     if (peerId == currPeerId) {
			    	 Peer.getInstance().pieces = new Receivedpieces[Peer.getInstance().noOfPieces];
			    	 Peer.getInstance().portNum = tokens[2];
			    	 Peer.getInstance().setBitField(new BitSet(Peer.getInstance().noOfPieces));
			    	 if (tokens[3].equals("1")) {
			    		 Peer.getInstance().hasCompletefile=true;
			    		 System.out.println("my bitfield-> "+Peer.getInstance().getBitField().toString());
//			    		 System.out.println(Peer.getInstance().noOfPieces);
				    	 Peer.getInstance().getBitField().flip(0, Peer.getInstance().noOfPieces);
				    	 for(int i=0;i<Peer.getInstance().noOfPieces;i++){
								System.out.println(Peer.getInstance().getBitField().get(i));
							}
				    	 System.out.println("my bitfield-> "+Peer.getInstance().getBitField().toString());
				    	 String fileName = "./peer_" + Peer.getInstance().peerID + File.separator 
				    			 + Peer.getInstance().configProps.get("FileName");
				    	 
				    	 fileName = Peer.getInstance().configProps.get("FileName");
				    	 FileInputStream fis = new FileInputStream(new File(fileName));
				    	 
				    	 for (int i = 0; i < Peer.getInstance().noOfPieces-1; i ++) {
				    		 byte[] piece = new byte[Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"))];
				    		 fis.read(piece, 0, Integer.parseInt(Peer.getInstance().configProps.get("PieceSize")));
//				    		 System.out.println("i val-> "+ i);
//				    		 System.out.println(new String(piece));
				    		 Peer.getInstance().pieces[i] = new Receivedpieces(piece);
				    	 }
				    	 byte[] piece = new byte[Peer.getInstance().excessPieceSize];
			    		 fis.read(piece, 0, Peer.getInstance().excessPieceSize);
			    		 
//			    		 System.out.println("here");
//			    		 System.out.println(new String(piece));
			    		 
//			    		 Peer.getInstance().pieces = new Receivedpieces[Peer.getInstance().noOfPieces];
			    		 Peer.getInstance().pieces[Peer.getInstance().noOfPieces-1] = new Receivedpieces(piece);
			    		 System.out.println("pieces info");
			    		 for(int i=0;i<Peer.getInstance().pieces.length;i++){
			    			 System.out.println(i);
			    			 System.out.println(Peer.getInstance().pieces[i]);
			    		 }
			    		 fis.close();
				     }
			     } else {
			    	 Peer.getInstance().neighbors.put(peerId, 
				    		 new RemotePeerInfo(peerId, tokens[1], tokens[2]));
			    	 BitSet bset = new BitSet(Peer.getInstance().noOfPieces);
//			    	 bset.flip(0, Peer.getInstance().noOfPieces);
			    	
			    	 
			    	 Peer.getInstance().neighborsBitSet.put(peerId, bset);
			    	 
			     }
			}
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
//			System.err.println(e.getMessage());
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
	
	public static void shutdownChecker(){
		final Runnable checkShutdownChecker = new Runnable(){
			public void run(){
				System.out.println("Shutdown scheduler started");
				BitSet myBitfield = Peer.getInstance().getBitField();
				BitSet filebitfield = Peer.getInstance().fileBitfield;
				int noOfPieces = Peer.getInstance().noOfPieces;
				for(int i=0;i<noOfPieces;i++){
					System.out.println("***");
					System.out.println(myBitfield.get(i));
					System.out.println(filebitfield.get(i));
				}
				boolean compareFlag = true;
				for(int i=0;i<noOfPieces;i++){
					if(!myBitfield.get(i)){
						compareFlag=false;
						break;
					}
				}
				if(compareFlag){
					System.out.println("Peer bitset and file bit set are equal");
					boolean shutdown = true;
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						int peerNeighborId = neighbor.getKey();
						BitSet neighborBitset = Peer.getInstance().neighborsBitSet.get(peerNeighborId);
//						boolean compareNCheckFlag = true;
						for(int i=0;i<noOfPieces;i++){
							System.out.println("neighbor val -> shere -> "+neighborBitset.get(i));
							if(!neighborBitset.get(i) ){
//								compareNCheckFlag=false;
								shutdown=false;
								break;
							}
						}
						
//						if(compareNCheckFlag){
//							shutdown= true;
//							break;
//						}
					}
					if(shutdown){
						

						try {
							String fileName =  Peer.getInstance().configProps.get("FileName");
							File dateFile = new File(fileName);
							FileOutputStream fos = new FileOutputStream(dateFile);
							for (Receivedpieces piece : Peer.getInstance().pieces) {
								fos.write(piece.pieceInfo);
							}
							
				            fos.flush();
				            fos.close();
				            for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
								RemotePeerInfo info = neighbor.getValue();
								info.setClientState(ScanState.KILL);
								info.setServerState(ScanState.KILL);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						for (int index = 0; index < Peer.getInstance().pieces.length - 1; index++) {
							
						}
						shutdownscheduler.shutdown();
						optimisticallyscheduler.shutdown();
						determinekscheduler.shutdown();
						
					}
				}
			}
		};
		shutdownscheduler.scheduleAtFixedRate(checkShutdownChecker, 3, 3, SECONDS);
		
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
		optimisticallyscheduler.scheduleAtFixedRate(determineOUnchokedPeer, m, m, SECONDS);
	}
	
	public static void determineKPreferred( int k,  int p) throws IOException{
		final Runnable kNeighborDeterminer = new Runnable() {
            public void run() {
            	System.out.println("determine scheduler start");
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
					System.out.println("determine logic started");

					List<Integer> peerList = new ArrayList<>(); 
					peerList = Peer.getInstance().interestedInMe;
					
					
					
					Map<Integer, Double> peerVsDownrate = new HashMap<Integer, Double>();
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						if(peerList.contains(neighbor.getKey())){
							peerVsDownrate.put(neighbor.getKey(), neighbor.getValue().downRate);
						}
					}
					peerVsDownrate = MapSortByValue.sortByValue(peerVsDownrate);
					if(peerList.size()>0){
						System.out.println("peer list is not null");

						Iterator<Integer> iterator = peerList.iterator();
						int count =0;
						if(initialFlow){
							System.out.println("initial flow started");
//							System.out.println("iterator val "+ iterator.next());
//							System.out.println("iterator val "+ iterator.hasNext());
//							System.out.println("count val "+ count);
//							System.out.println("k val "+ k);
							while(count<k && iterator.hasNext()){
								System.out.println("while loop started");
								int prefPeer = iterator.next();
								unchokeList.add(prefPeer);
								RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(prefPeer);
								prefPeerInfo.setClientState(ScanState.UNCHOKE);
								count++;
								System.out.println("unchoke set");
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
								System.out.println("CHOKED HERE *******");
								prefPeerInfo.setClientState(ScanState.CHOKE);
							}
							
							unchokeList.clear();
							unchokeList = tempList;
							for(int i=0;i<unchokeList.size();i++){
								RemotePeerInfo prefPeer = Peer.getInstance().neighbors.get(unchokeList.get(i));
								prefPeer.setClientState(ScanState.UNCHOKE);
							}
						}
					}
				}
            }
		};
//		final ScheduledFuture<?> kNeighborDeterminerHandle = scheduler.scheduleAtFixedRate(kNeighborDeterminer, p, p, SECONDS);
		determinekscheduler.scheduleAtFixedRate(kNeighborDeterminer, p, p, SECONDS);
	}

}
