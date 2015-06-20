import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class bfclient {

	public static class NodeInfo {// description of IP address and port number
		InetAddress IP;
		int port;

		public NodeInfo(InetAddress a, int b) {
			IP = a;
			port = b;
		}

		@Override
		public int hashCode() {
			return IP.hashCode() + port;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (!(o instanceof NodeInfo)) {
				return false;
			} else {
				NodeInfo nodeInfo = (NodeInfo) o;
				return IP.equals(nodeInfo.IP) && port == nodeInfo.port;
			}
		}
	}

	// costs from host to every nodes and the next hop
	public static class SelfValue {
		double cost;
		NodeInfo nextHop;

		public SelfValue(double a, NodeInfo b) {
			cost = a;
			nextHop = b;
		}

		public void changeCost(double c) {
			cost = c;
		}
	}

	// what to do if timeout
	public static class ReSendVector implements Runnable {

		@Override
		public void run() {
			try {
				sendVector();
			} catch (UnknownHostException e) {
				System.out.println("Unable to get local IP address!");
			}
		}
	}

	public static class CloseNeighbor implements Runnable {

		NodeInfo closeNodeInfo;

		public CloseNeighbor(NodeInfo nodeInfo) {
			closeNodeInfo = nodeInfo;
		}

		@Override
		public void run() {
			try {
				linkDown(closeNodeInfo, false);
				if(neighborCostBackup.containsKey(closeNodeInfo)){
					neighborCostBackup.remove(closeNodeInfo);
				}
			} catch (UnknownHostException e) {
				System.out.println("Unable to get local IP address!");
			}
		}
	}

	// definition of class variables
	public static int localport;
	public static int timeout;
	public static int MSS = 1000;
	public static DatagramSocket socket;
	public static DatagramSocket sendSocket;

	public static HashMap<NodeInfo, HashMap<NodeInfo, Double>> routerTable = new HashMap<NodeInfo, HashMap<NodeInfo, Double>>();

	public static HashMap<NodeInfo, SelfValue> selfVector = new HashMap<NodeInfo, SelfValue>();

	public static HashMap<NodeInfo, Double> neighborCost = new HashMap<NodeInfo, Double>();

	public static HashMap<NodeInfo, Double> neighborCostBackup = new HashMap<NodeInfo, Double>();

	public static HashMap<NodeInfo, ScheduledFuture<?>> timeToShutNeighborDown = new HashMap<NodeInfo, ScheduledFuture<?>>();

	public static ScheduledThreadPoolExecutor timer;

	public static int maxNeighborNum = 30;

	public static ScheduledFuture<?> timeToSendVector;

	public static void main(String[] args) throws UnknownHostException {

		try {
			localport = Integer.parseInt(args[0]);
			timeout = Integer.valueOf(args[1]);
			socket = new DatagramSocket(localport);
			sendSocket = new DatagramSocket();

			timer = new ScheduledThreadPoolExecutor(maxNeighborNum);

			int inputLen = args.length;

			for (int i = 2; i < inputLen; i += 3) {

				InetAddress IP = InetAddress.getByName(args[i]);
				int port = Integer.parseInt(args[(i + 1)]);
				double cost = Double.valueOf(args[(i + 2)]);

				NodeInfo newNode = new NodeInfo(IP, port);
				// write the initial info into selfTable
				selfVector.put(newNode, new SelfValue(cost, newNode));
				neighborCost.put(newNode, cost);

				// set the time for shutdown a neighbor
				timeToShutNeighborDown.put(newNode, timer.schedule(
						new CloseNeighbor(newNode), 3 * timeout,
						TimeUnit.SECONDS));
			}
		} catch (Exception ex) {
			System.out.println("Wrong format of input");
			System.exit(1);
			// ex.printStackTrace();
		}

		Thread t = new Thread(new receiveData());
		t.start();
		sendVector();
		userInput();

	}

	public static class receiveData implements Runnable {

		public void run() {
			try {
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[MSS],
							MSS);
					socket.receive(packet);
					byte[] recvByte = packet.getData();

					String[] recvData = new String(recvByte).split(" ");
					if (recvData[0].equals("1") || recvData[0].equals("2")) {
						// System.out.println(recvData[2]);
						InetAddress IP = InetAddress.getByName(recvData[1]);
						int port = Integer.parseInt(recvData[2].trim());

						NodeInfo nodeInfo = new NodeInfo(IP, port);
						if (recvData[0].equals("1")) {
							linkDown(nodeInfo, false);

						} else {
							linkUp(nodeInfo, false);
						}
					} else {
						InetAddress remoteIP = InetAddress
								.getByName(recvData[0]);
						int remotePort = Integer.parseInt(recvData[1]);
						int inputLen = recvData.length - 1;

						for (int i = 2; i < inputLen; i += 3) {// receive the
																// router vector
																// from neighbor
							InetAddress IP = InetAddress.getByName(recvData[i]);
							int port = Integer.parseInt(recvData[(i + 1)]);
							double cost = Double.valueOf(recvData[(i + 2)]);

							// change the routerTable based on the vector
							// receives
							if (routerTable.containsKey(new NodeInfo(remoteIP,
									remotePort))) {

								routerTable.get(
										new NodeInfo(remoteIP, remotePort))
										.put(new NodeInfo(IP, port), cost);

							} else {
								HashMap<NodeInfo, Double> temp = new HashMap<NodeInfo, Double>();
								temp.put(new NodeInfo(IP, port), cost);
								routerTable.put(new NodeInfo(remoteIP,
										remotePort), temp);

							}
						}
						changeSelf(new NodeInfo(remoteIP, remotePort));

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
				// System.out.println("Unable to receive UDP packets!");
			}
		}
	}

	public static void changeSelf(NodeInfo nodeInfo)
			throws UnknownHostException {

		// timer issue
		if (timeToShutNeighborDown.containsKey(nodeInfo)) {
			timeToShutNeighborDown.get(nodeInfo).cancel(true);
		}
		timeToShutNeighborDown.put(nodeInfo, timer.schedule(new CloseNeighbor(
				nodeInfo), 3 * timeout, TimeUnit.SECONDS));

		HashMap<NodeInfo, Double> tempVector = new HashMap<NodeInfo, Double>();
		tempVector = routerTable.get(nodeInfo);
		NodeInfo selfInfo = new NodeInfo(InetAddress.getLocalHost(), localport);
		

		if (!neighborCostBackup.containsKey(nodeInfo)){
			boolean flag = false;
			// decide whether to send vectors to neighbors
			// determine whether it's a new neighbor
			if (!neighborCost.containsKey(nodeInfo) && !nodeInfo.equals(selfInfo)) {

				double cost = tempVector.get(selfInfo);
				selfVector.put(nodeInfo, new SelfValue(cost, nodeInfo));
				neighborCost.put(nodeInfo, cost);
				flag = true;
			}

			for (Entry<NodeInfo, Double> entry : tempVector.entrySet()) {

				NodeInfo key = (NodeInfo) entry.getKey();
				Double val = (Double) entry.getValue();

				if (selfVector.containsKey(key)) {
					if (!selfVector.get(key).nextHop.equals(nodeInfo)) {
						// the nextHop of this key is not nodeInfo
						// System.out.println("2");
						double oldCost = selfVector.get(key).cost;
						double newCost = val + neighborCost.get(nodeInfo);
						if (newCost < oldCost) {
							selfVector.put(key, new SelfValue(newCost, nodeInfo));
							flag = true;
						}

					} else if (selfVector.get(key).nextHop.equals(nodeInfo)) {
						// the nextHop of this key is nodeInfo

						if (val == Double.POSITIVE_INFINITY
								&& selfVector.get(key).cost != Double.POSITIVE_INFINITY) {
							flag = true;
						}

						double tempCost = Double.POSITIVE_INFINITY;
						NodeInfo tempInfo = nodeInfo;

						for (NodeInfo srcNode : routerTable.keySet()) {
							for (NodeInfo destNode : routerTable.get(srcNode)
									.keySet()) {
								if (selfVector.containsKey(destNode)
										&& destNode.equals(key) && neighborCost.containsKey(srcNode)) {
									double newCost = neighborCost.get(srcNode)
											+ routerTable.get(srcNode)
											.get(destNode);

									if (newCost < tempCost) {
										tempCost = newCost;
										tempInfo = srcNode;
										flag = true;
									}
								}
							}
						}

						if (neighborCost.containsKey(key)) {
							if (neighborCost.get(key) < tempCost) {
								tempCost = neighborCost.get(key);
								tempInfo = key;
								flag = true;
							}
						}
						selfVector.put(key, new SelfValue(tempCost, tempInfo));

					}
				} else if (!selfVector.containsKey(key) && !key.equals(selfInfo)) {
					double cost = val + neighborCost.get(nodeInfo);
					selfVector.put(key, new SelfValue(cost, nodeInfo));
					flag = true;
				}

			}

			if (neighborCost.size() == 0){
				for (NodeInfo tempNode : selfVector.keySet()){
					NodeInfo nextHop = selfVector.get(tempNode).nextHop;
					selfVector.put(tempNode, new SelfValue(Double.POSITIVE_INFINITY, nextHop));
					flag = true;
				}
			}
			// System.out.println(flag);
			if (flag) {
				sendVector();
			}
		}
	}
	
	private static void userInput() {

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			while (true) {

				String line = stdIn.readLine();
				if (line.equals("SHOWRT")) {
					showTable();
				} else if (line.equals("CLOSE")) {
					System.exit(0);
					break;
				} else if (line.startsWith("LINKDOWN")) {
					String[] temp = line.split(" ");
					InetAddress removeIP = InetAddress.getByName(temp[1]);
					int removePort = Integer.parseInt(temp[2]);
					NodeInfo nodeInfo = new NodeInfo(removeIP, removePort);
					if (neighborCost.containsKey(nodeInfo)) {
						linkDown(nodeInfo, true);
					} else {
						System.out.println("No such link!");
					}
//					System.out.println("Quitting LINKDOWN");
				} else if (line.startsWith("LINKUP")) {
					String[] temp = line.split(" ");
					InetAddress removeIP = InetAddress.getByName(temp[1]);
					int removePort = Integer.parseInt(temp[2]);
					NodeInfo nodeInfo = new NodeInfo(removeIP, removePort);
					if (neighborCostBackup.containsKey(nodeInfo)) {
						linkUp(nodeInfo, true);
					} else {
						System.out.println("No such link is shutdown before!");
					}
				} else if (line.equals("SHOWNEIGHBOR")){
					showNeighbor();
				}else {
					System.out.println("Wrong command!");
				}
			}
		} catch (Exception ex) {
			System.out.println("Wrong command!");
		}
	}
	private static void showNeighbor(){
		int i = 0;
		System.out.println("Current neighbor list is: ");
		for (Entry<NodeInfo, Double> entry : neighborCost.entrySet()) {
			
			i++;
			NodeInfo key1 = (NodeInfo) entry.getKey();
			InetAddress remoteIP = key1.IP;
			int remotePort = key1.port;
			
			System.out.println(// print the current selfTable
					"Neighbor #"+i+": "+ remoteIP.getHostAddress() + ":" + remotePort);
		}
	}

	// command related functions
	private static void showTable() {
		String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
		System.out.println(timeStamp + " Distance vector list is:");

		for (Entry<NodeInfo, SelfValue> entry : selfVector.entrySet()) {
			
			NodeInfo key = (NodeInfo) entry.getKey();
			SelfValue val = (SelfValue) entry.getValue();

			System.out.println(// print the current selfTable
					"Destination = " + key.IP.getHostAddress() + ":" + key.port
							+ ", Cost = " + val.cost + ", Link = ("
							+ val.nextHop.IP.getHostAddress() + ":"
							+ val.nextHop.port + ")");
		}
	}

	private static void linkDown(NodeInfo nodeInfo, boolean command)
			throws UnknownHostException {

		if (neighborCost.containsKey(nodeInfo)) {

			// timer issue
			timeToShutNeighborDown.get(nodeInfo).cancel(true);
			timeToShutNeighborDown.remove(nodeInfo);

			// backup and delete
			double temp = neighborCost.get(nodeInfo);
			neighborCostBackup.put(nodeInfo, temp);
			neighborCost.remove(nodeInfo);
			routerTable.remove(nodeInfo);
			boolean flag = false;
			
			for (Entry<NodeInfo, SelfValue> entry : selfVector.entrySet()) {
				
				NodeInfo key = (NodeInfo) entry.getKey();
				SelfValue val = (SelfValue) entry.getValue();
				
//				System.out.println(val.nextHop.port);
				
				if (val.nextHop.equals(nodeInfo)) {
					flag = true;
					val.cost = Double.POSITIVE_INFINITY;
					selfVector.put(key, val);

					for (NodeInfo srcNode : routerTable.keySet()) {
						for (NodeInfo destNode : routerTable.get(srcNode)
								.keySet()) {
							if (destNode.equals(key)) {
								double newCost = neighborCost.get(srcNode)
										+ routerTable.get(srcNode)
												.get(destNode);
								if (newCost < val.cost) {
									selfVector.put(destNode, new SelfValue(
											newCost, srcNode));
								}
							}
						}
					}
				}
			}

//			System.out.println(neighborCost.size());
			for (NodeInfo neighborNode : neighborCost.keySet()) {
				double newCost = neighborCost.get(neighborNode);
				if (newCost < selfVector.get(neighborNode).cost) {
					selfVector.put(neighborNode, new SelfValue(newCost,
							neighborNode));
					flag = true;
				}
			}
//			System.out.println(neighborCost.size());
			if (neighborCost.size() == 0){
				for (NodeInfo tempNode : selfVector.keySet()){
					NodeInfo nextHop = selfVector.get(tempNode).nextHop;
					selfVector.put(tempNode, new SelfValue(Double.POSITIVE_INFINITY, nextHop));
				}
			}
	
			if (command) {
				sendLinkChange(nodeInfo, 1);
			}
			if (flag) {
				sendVector();
			}
		}
	}

	private static void linkUp(NodeInfo nodeInfo, boolean command)
			throws UnknownHostException {

		// timer issue
		timeToShutNeighborDown.put(nodeInfo, timer.schedule(new CloseNeighbor(
				nodeInfo), 3 * timeout, TimeUnit.SECONDS));

		double temp = neighborCostBackup.get(nodeInfo);
		neighborCost.put(nodeInfo, temp);
		neighborCostBackup.remove(nodeInfo);

		double oldCost = selfVector.get(nodeInfo).cost;
		double newCost = neighborCost.get(nodeInfo);

		boolean flag = false;
		if (newCost < oldCost) {
			selfVector.put(nodeInfo, new SelfValue(newCost, nodeInfo));
			flag = true;
		}
		if (command) {
			sendLinkChange(nodeInfo, 2);
		}
		if (flag) {
			sendVector();
		}
	}

	public static void sendLinkChange(NodeInfo nodeInfo, int command)
			throws UnknownHostException {
		// send to its neighbor the message of linkdown or linkup

		String messageString = "";
		if (command == 1) { // link down
			messageString += "1";
		} else {
			messageString += "2";
		}

		String localIP = InetAddress.getLocalHost().getHostAddress();
		messageString += " " + localIP + " " + Integer.toString(localport);

		byte[] message = stringToByteArray(messageString);
		DatagramPacket packet = new DatagramPacket(message, message.length,
				nodeInfo.IP, nodeInfo.port);
		try {
			sendSocket.send(packet);
		} catch (IOException e) {
			System.out.println("Unable to send the UDP packet!");
		}
	}

	public static void sendVector() throws UnknownHostException {

		// timer issue
		if (timeToSendVector != null) {
			timeToSendVector.cancel(true);
		}
				
		String vector = "";
		String localIP = InetAddress.getLocalHost().getHostAddress();
		vector += localIP + " " + Integer.toString(localport) + " ";
		// System.out.println(localIP);
		
		for (Entry<NodeInfo, Double> entry1 : neighborCost.entrySet()) {

			NodeInfo key1 = (NodeInfo) entry1.getKey();
			InetAddress remoteIP = key1.IP;
			int remotePort = key1.port;

			for (Entry<NodeInfo, SelfValue> entry2 : selfVector.entrySet()) {

				NodeInfo key2 = (NodeInfo) entry2.getKey();
				SelfValue val2 = (SelfValue) entry2.getValue();
				NodeInfo nextHop = val2.nextHop;

				if (nextHop.equals(key1) && !key2.equals(key1)) {// solving
																	// poison
																	// reverse
					vector += key2.IP.getHostAddress() + " "
							+ Integer.toString(key2.port) + " "
							+ Double.POSITIVE_INFINITY + " ";
				} else {
					vector += key2.IP.getHostAddress() + " "
							+ Integer.toString(key2.port) + " "
							+ Double.toString(val2.cost) + " ";
				}

			}
			byte[] byteArrayVector = stringToByteArray(vector);

			DatagramPacket packet = new DatagramPacket(byteArrayVector,
					byteArrayVector.length, remoteIP, remotePort);
			try {
				sendSocket.send(packet);
			} catch (IOException e) {
				System.out.println("Unable to send the UDP packet!");
			}
		}

		// timer issue
		timeToSendVector = timer.scheduleAtFixedRate(new ReSendVector(),
				timeout, timeout, TimeUnit.SECONDS);
	}

	public static byte[] stringToByteArray(String s) {
		byte[] byteArray = new byte[s.length()];
		for (int i = 0; i < s.length(); i++) {
			byteArray[i] = (byte) s.charAt(i);
		}
		return byteArray;
	}

}
