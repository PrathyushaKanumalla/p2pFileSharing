
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
import java.util.concurrent.ThreadLocalRandom;

import project.Constants.ScanState;

/** Defining Schedulers, variables, choke list, unchoke list */
public class peerProcess {
	private static ScheduledExecutorService determinekscheduler = Executors.newScheduledThreadPool(5);
	private static ScheduledExecutorService optimisticallyscheduler = Executors.newScheduledThreadPool(5);
	private static ScheduledExecutorService shutdownscheduler = Executors.newScheduledThreadPool(5);
	private static boolean stateCheck = true;
	private static boolean initialFlow = true;
	private static List<Integer> unchokeList =  Collections.synchronizedList(new ArrayList<>());
	private static List<Integer> chokeList =  Collections.synchronizedList(new ArrayList<>());
	/** Reading Common Configuration File to get piece information*/
	public static void setCommonConfig() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(Constants.COMMONCFG));
		String configLine = null;

		while ((configLine = br.readLine()) != null) {
			String[] split = configLine.split(" ");
			Peer.getInstance().configProps.put(split[0], split[1]);
		}
		int fileSize = Integer.parseInt(Peer.getInstance().configProps.get("FileSize"));
		int pieceSize = Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"));

		int noOfPieces=0;

		if (fileSize % pieceSize ==0) {
			noOfPieces = fileSize/pieceSize;
		} else {
			noOfPieces = fileSize/pieceSize;
			int excess = fileSize - pieceSize * noOfPieces;
			Peer.getInstance().excessPieceSize = excess;
			noOfPieces+=1;
		}
		Peer.getInstance().noOfPieces = noOfPieces;
		int i;
		for (i = 0; i < Peer.getInstance().noOfPieces -1 ;i++){
			Peer.getInstance().fileBitfield.set(i);
		}
		int tempPieces = Peer.getInstance().excessPieceSize;
		while (tempPieces > 0){
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
				/** Step 1: Initiate Logs */
				Log.setUpSingleTonLog(currPeerId);
				/** Step 2: Set up Config Information
				//insert variable as key and store its value....*/
				setCommonConfig();

				int k = Integer.parseInt(Peer.getInstance().configProps.get("NumberOfPreferredNeighbors"));
				int p =Integer.parseInt(Peer.getInstance().configProps.get("UnchokingInterval"));
				int m = Integer.parseInt(Peer.getInstance().configProps.get("OptimisticUnchokingInterval"));

				/** Step 3: Set up Peer Information */
				setPeerNeighbors(currPeerId);
				/** Step 4 Start up client and server process*/
				for (RemotePeerInfo neighbor : Peer.getInstance().neighbors.values()) {
					if (neighbor.peerId > Peer.getInstance().peerID) {
						neighbor.setServerState(ScanState.START);
					}
					if (neighbor.peerId < Peer.getInstance().peerID) {
						neighbor.setClientState(ScanState.START);
					}
				}
				/** Step 5 Start server thread*/
				Server serverThread = new Server();
				serverThread.start();
				/** Step 6 Begin clien thread*/
				for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
					Client clientThread = new Client(neighbor.getValue());
					Peer.getInstance().neighborThreads.put(neighbor.getKey(), clientThread);
					if (neighbor.getKey() < Peer.getInstance().peerID) {
						clientThread.start();
					}
				}
				Thread.sleep(1000);
				/**Determine K neighbours*/
				determineKPreferred(k,p);
				/** Determine Optimistically Unchoked Peer **/
				determineOptimisticallyUnchokedPeer(m);
				/** Shutdown Scheduler */
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
	/** Setup peer neighbours for each peer */
	private static void setPeerNeighbors(int currPeerId) {
		Peer.getInstance().peerID = currPeerId;
		String row = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(Constants.PEERINFO));
			while ((row = in.readLine()) != null) {
				String[] tokens = row.split("\\s+");
				Integer peerId = new Integer(tokens[0]);
				Peer.getInstance().downloadTime.put(peerId, (double) 0);
				if (peerId == currPeerId) {
					Peer.getInstance().pieces = new Receivedpieces[Peer.getInstance().noOfPieces];
					Peer.getInstance().portNum = tokens[2];
					Peer.getInstance().setBitField(new BitSet(Peer.getInstance().noOfPieces));
					if (tokens[3].equals("1")) {
						Peer.getInstance().hasCompletefile = true;
						Peer.getInstance().getBitField().flip(0, Peer.getInstance().noOfPieces);
						String fileName = "./peer_" + Peer.getInstance().peerID + File.separator 
								+ Peer.getInstance().configProps.get("FileName");

						FileInputStream fis = new FileInputStream(new File(fileName));

						for (int i = 0; i < Peer.getInstance().noOfPieces-1; i ++) {
							byte[] piece = new byte[Integer.parseInt(Peer.getInstance().configProps.get("PieceSize"))];
							fis.read(piece, 0, Integer.parseInt(Peer.getInstance().configProps.get("PieceSize")));
							Peer.getInstance().pieces[i] = new Receivedpieces(piece);
						}
						byte[] piece = new byte[Peer.getInstance().excessPieceSize];
						fis.read(piece, 0, Peer.getInstance().excessPieceSize);
						Peer.getInstance().pieces[Peer.getInstance().noOfPieces-1] = new Receivedpieces(piece);
						fis.close();
					}
				} else {
					Peer.getInstance().neighbors.put(peerId, 
							new RemotePeerInfo(peerId, tokens[1], tokens[2]));
					BitSet bset = new BitSet(Peer.getInstance().noOfPieces);
					Peer.getInstance().getNeighborsBitSet().put(peerId, bset);

				}
			}
			System.out.println("PEER PROCESS - My bit field " + Peer.getInstance().getBitField());
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** Shut down scheduler */
	public static void shutdownChecker(){
		final Runnable checkShutdownChecker = new Runnable(){
			public void run(){
				BitSet myBitfield = Peer.getInstance().getBitField();
				int noOfPieces = Peer.getInstance().noOfPieces;
				boolean compareFlag = true;
				for (int i = 0; i < noOfPieces;i++){
					if (!myBitfield.get(i)) {
						compareFlag=false;
						break;
					}
				}
				/** Compare if all peers got the message */
				if (compareFlag){
					boolean shutdown = true;
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						int peerNeighborId = neighbor.getKey();
						BitSet neighborBitset = Peer.getInstance().getNeighborsBitSet().get(peerNeighborId);
						if (neighbor.getValue().isUpdatePieceInfo()) {
							shutdown=false;
							break;
						}
						for (int i = 0 ; i < noOfPieces;i++) {
							System.out.println("neighbor val -> shere -> "+neighborBitset.get(i) + "index -> "+i + "neighbor id "+neighbor.getKey());
							if (!neighborBitset.get(i)) {
								shutdown=false;
								break;
							}
						}
					}
					/**Save Files before shut down */
					if(shutdown){
						try {
							String path =  "./peer_" + Peer.getInstance().peerID ;
							File directoryFolder = new File(path);
							if (!directoryFolder.exists())
								directoryFolder.mkdir();
							String fileName = path + File.separator + Peer.getInstance().configProps.get("FileName");
							File dateFile = new File(fileName);
							FileOutputStream fos = new FileOutputStream(dateFile);
							for (Receivedpieces piece : Peer.getInstance().pieces) {
								fos.write(piece.pieceInfo);
							}
							Log.addLog(String.format("Peer %s has downloaded the complete file", Peer.getInstance().peerID));
							fos.flush();
							fos.close();
							for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
								RemotePeerInfo info = neighbor.getValue();
								System.out.println("PEERPROCESS - killing the peer " + info.peerId);
								info.setClientState(ScanState.KILL);
								info.setServerState(ScanState.KILL);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						shutdownscheduler.shutdown();
						optimisticallyscheduler.shutdown();
						determinekscheduler.shutdown();
					}
				}
			}
		};
		shutdownscheduler.scheduleAtFixedRate(checkShutdownChecker, 10, 10, SECONDS);

	}

	public static int previouslyOptimisticallyPeer;

	public static void determineOptimisticallyUnchokedPeer(int m){
		final Runnable determineOUnchokedPeer = new Runnable(){
			public void run(){
				int size = chokeList.size();
				if (size != 0) {
					int randIndex = ThreadLocalRandom.current().nextInt(0, size);
					int peer = chokeList.remove(randIndex);
					if (peer != previouslyOptimisticallyPeer) {
						System.out.println("new peer is selected to unchoke -> "+peer);
						Log.addLog(String.format("Peer %d has the optimistically unchoked neighbor %d", Peer.getInstance().peerID, peer));
						RemotePeerInfo peerInfo = Peer.getInstance().neighbors.get(peer);
						peerInfo.isOptimisticallyChosen=true;
						peerInfo.setClientState(ScanState.UNCHOKE);
						if (previouslyOptimisticallyPeer != 0){
							System.out.println("sending choke to previously choked -> "+ previouslyOptimisticallyPeer);
							RemotePeerInfo tempInfo = Peer.getInstance().neighbors.get(previouslyOptimisticallyPeer);
							tempInfo.isOptimisticallyChosen=false;
							tempInfo.setClientState(ScanState.CHOKE);
						}
						previouslyOptimisticallyPeer = peer;
					}
				} else if(previouslyOptimisticallyPeer != 0) {
					System.out.println("in else block sending choke to previously choked -> "+ previouslyOptimisticallyPeer);
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
				if(stateCheck){
					boolean isFlag = true;
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						RemotePeerInfo val = neighbor.getValue();
						if(val.getClientState()!=ScanState.UPLOAD_START){
							isFlag = false;
						}
					}
					if (isFlag) {
						System.out.println("All clients are in UPLOAD START state");
						stateCheck=false;
					}
				}
				else{
					List<Integer> peerList = new ArrayList<>(); 
					peerList = Peer.getInstance().interestedInMe;
					Map<Integer, Double> peerVsDownrate = new HashMap<Integer, Double>();
					for (Entry<Integer, RemotePeerInfo> neighbor : Peer.getInstance().neighbors.entrySet()) {
						if (peerList.contains(neighbor.getKey())) {
							double dwnldTime = Peer.getInstance().downloadTime.get(neighbor.getKey());
							System.out.println("The download rate of the peer is - " + dwnldTime + " for peer - " + neighbor.getKey());
							peerVsDownrate.put(neighbor.getKey(), dwnldTime);
						}
					}
					peerVsDownrate = MapSortByValue.sortByValue(peerVsDownrate);
					if (peerList.size() > 0) {
						Iterator<Integer> iterator = peerList.iterator();
						int count =0;
						if (initialFlow){
							while (iterator.hasNext()){
								int prefPeer = iterator.next();
								if (count < k){
									unchokeList.add(prefPeer);
									RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(prefPeer);
									if (prefPeerInfo.alreadyChoked) {
										prefPeerInfo.alreadyChoked = false;
										if (!prefPeerInfo.isOptimisticallyChosen) {
											prefPeerInfo.setClientState(ScanState.UNCHOKE);
										}
									}
								} else {
									chokeList.add(prefPeer);
									RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(prefPeer);
									if (!prefPeerInfo.alreadyChoked) {
										if (!prefPeerInfo.isOptimisticallyChosen){
											prefPeerInfo.setClientState(ScanState.CHOKE);
										}
									}
								}
								count++;
							}
							initialFlow= false;
						} else {
							Iterator<Integer> itr = peerVsDownrate.keySet().iterator();
							List<Integer> tempList = new ArrayList<Integer>();
							while (count<k && itr.hasNext()) {
								int prefPeer = itr.next();
								tempList.add(prefPeer);
								if (!unchokeList.isEmpty() && unchokeList.contains(prefPeer)) {
									int idx = unchokeList.indexOf(prefPeer);
									unchokeList.remove(idx);
								} else {
									Peer.getInstance().neighbors.get(prefPeer).setClientState(ScanState.UNCHOKE);
								}
								count++;
							}
							for (int i=0;i<unchokeList.size();i++) {
								RemotePeerInfo prefPeerInfo = Peer.getInstance().neighbors.get(unchokeList.get(i));
								prefPeerInfo.setClientState(ScanState.CHOKE);
							}

							unchokeList.clear();
							unchokeList = tempList;
						}
					}
				}
				Log.addLog(String.format("Peer %s has the preferred neighbors %s", Peer.getInstance().peerID, unchokeList));
			}
		};
		determinekscheduler.scheduleAtFixedRate(kNeighborDeterminer, p, p, SECONDS);
	}

}
