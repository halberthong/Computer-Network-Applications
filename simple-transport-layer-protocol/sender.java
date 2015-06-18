
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.net.*;
import java.sql.Timestamp;

public class sender {

	// initial definition of variables
	private static String filename = null;
	private static InetAddress remote_IP;
	private static int remote_port;
	private static int ack_port_num;
	private static String log_filename;

	private static int window_size;
	private static final int MSS = 576;
	private static final int headerSize = 20;
	private static byte[] header = new byte[headerSize];
	private static int RTT = 1000;
	private static int timeInterval = 1000;
	private static int DevRTT = 0;
	private static PrintStream outprint;
		
	public static void main(String[] args) {
		try {
			filename = args[0];
			remote_IP = InetAddress.getByName(args[1]);
			remote_port = Integer.parseInt(args[2]);
			ack_port_num = Integer.parseInt(args[3]);
			log_filename = args[4];
			window_size = Integer.parseInt(args[5]);
			// sender senderObject = new sender();
			window_size = 1;// default

		} catch (Exception ex) {
			System.out.println("Wrong format of input");
		}

		sender send = new sender();
		byte[] sourceFile = send.getBytesfromFile();

		if (log_filename.equals("stdout")){
			outprint = System.out;
		} else {
			try{
				outprint = new PrintStream(log_filename);
			} catch (FileNotFoundException e) {
				System.out.println("Unable to write logfile");
			}
		}
		send.buildHeader();
		send.run(sourceFile);
		
		outprint.close();
		System.exit(0);
	}
	
	private void run(byte[] sourceFile){
		
		//separate the big file into small pieces, which is the size of MSS
		int totalLen = sourceFile.length;
		int totalSegment = 0;
		int retransmitSegment = 0;
		int i = 0;// sequence number
		
		while (true){
			
			byte[] headerTemp = header;
			
			byte[] SeqNum = new byte[4];
			SeqNum = intToByte(i, 4);
			headerTemp[4] = SeqNum[0];
			headerTemp[5] = SeqNum[1];
			headerTemp[6] = SeqNum[2];
			headerTemp[7] = SeqNum[3];
			byte[] packets = new byte[MSS];
			
			if ((totalLen - i*(MSS-headerSize)) > (MSS-headerSize)){
				if (i == 0){
					headerTemp[13] = 0b0000_0010;
				} else {
					headerTemp[13] = 0b0001_0000;
				}	
				System.arraycopy(headerTemp, 0, packets, 0, headerSize);
				System.arraycopy(sourceFile, i*(MSS-headerSize), packets, headerSize, (MSS-headerSize));	
//				System.out.format("%x\n", packets[i][4]);
			
			} else if ((totalLen - i*(MSS-headerSize) <= (MSS-headerSize))) {
				if (i == 0){
					headerTemp[13] = 0b0000_0011;
				} else{
					headerTemp[13] = 0b0001_0001;
				}
				System.arraycopy(headerTemp, 0, packets, 0, headerSize);
				System.arraycopy(sourceFile, i*(MSS-headerSize), packets, headerSize, (totalLen-(i*(MSS-headerSize))));		
			}
			
			byte[] checksum = CRC(packets);
			packets[16] = checksum[0];
			packets[17] = checksum[1];
			int temp[] = sendPacket(packets, i);// send packets
			totalSegment += temp[0];
			retransmitSegment += temp[1];
			if (packets[13] == 0b0000_0011 || packets[13] == 0b0001_0001){
				break;
			}
			i++;
		}
		totalLen = totalLen + i*headerSize;
		System.out.println("Delivery completed successfully");
		System.out.println("Total bytes sent = "+totalLen);
		System.out.println("Segments sent = "+totalSegment);
		System.out.println("Segments retransmitted = "+retransmitSegment);
	}

	private static int[] sendPacket(byte[] packets, int j){
		long startingTime, endingTime;
		int segmentNum = 0;
		int retransmitNum = 0;
		try{
			DatagramSocket sendSocket = new DatagramSocket(ack_port_num);
			DatagramPacket packet = new DatagramPacket(packets, packets.length, remote_IP, remote_port);
			sendSocket.send(packet);
			segmentNum++;
			startingTime = System.currentTimeMillis();
			writeLog(j, 0, 0, String.format("%8s", Integer.toBinaryString(packets[13] & 0xff)).replace(' ', '0'));
			byte[] incoming = new byte[1];
			
			sendSocket.setSoTimeout(timeInterval);
			while(true) {
				DatagramPacket getack = new DatagramPacket(incoming, incoming.length);
				try {
					sendSocket.receive(getack);
					if(incoming[0] == 0b0000_0001){
						endingTime = System.currentTimeMillis();
						sendSocket.close();
						writeLog(0, j, 1, "");
						break;
					}
					else{// retransmit because of NAK
						sendSocket.send(packet);
						segmentNum++;
						retransmitNum++;
						startingTime = System.currentTimeMillis();
						sendSocket.setSoTimeout(timeInterval);
						writeLog(0, j, 2, String.format("%8s", Integer.toBinaryString(packets[13] & 0xff)).replace(' ', '0'));
						writeLog(j, 0, 0, String.format("%8s", Integer.toBinaryString(packets[13] & 0xff)).replace(' ', '0'));
						continue;
					}

				} catch (SocketTimeoutException e) {
					// re-send
					sendSocket.send(packet);
					segmentNum++;
					retransmitNum++;
					startingTime = System.currentTimeMillis();
					writeLog(j, 0, 0, String.format("%8s", Integer.toBinaryString(packets[13] & 0xff)).replace(' ', '0'));
					continue;
				}

			}	
			estimateRTT(startingTime, endingTime);
		} catch (Exception ex){
			//			System.out.println("Fail to create DatagramSocket");
			ex.printStackTrace();
		}
		int[] temp = {segmentNum, retransmitNum};
		return temp;	
	}

	private byte[] getBytesfromFile() {
		byte[] sourceFile = null;
		try{
			@SuppressWarnings("resource")
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			sourceFile = new byte[(int) file.length()];
			file.read(sourceFile);
		} catch(Exception e) {
			System.out.println("Fail to read the file");
			System.exit(0);
		}
		return sourceFile;
	}

	private void buildHeader() {

		byte[] SP = new byte[2];
		byte[] DP = new byte[2];
		byte[] winSize = new byte[2];
		
		SP = intToByte(ack_port_num, 2);// Source Port
		DP = intToByte(remote_port, 2);// Destination Port
		winSize = intToByte(window_size, 2);
		header[0] = SP[0];
		header[1] = SP[1];
		header[2] = DP[0];
		header[3] = DP[1];
		header[14] = winSize[0];
		header[15] = winSize[1];
	}
	
	private static byte[] intToByte( int input, int size){
		
		byte[] output = new byte[size];
		for (int i = 0; i < size; i++) {
			output[i] = (byte) (input >>> (i * 8));
		}
		return output;
	}
	
	public static byte[] CRC (byte[] input){ 

		int crc = 0xFFFF;          // initial value
		int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12) 
//		1 + x + x^5 + x^12 + x^16 is irreducible polynomial.

		byte[] bytes = input;

		for (byte b : bytes) {
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b   >> (7-i) & 1) == 1);
				boolean c15 = ((crc >> 15    & 1) == 1);
				crc <<= 1;
				if (c15 ^ bit) crc ^= polynomial;
			}
		}

		crc &= 0xffff;
//		System.out.println("CRC16-CCITT = " + Integer.toHexString(crc));
		byte[] checksum = intToByte(crc, 2);
		return checksum;
	}
	
	private static void estimateRTT(long initialTime, long endingTime){
		double alpha = 0.125;
		double beta = 0.25;
		RTT = (int)((1-alpha)*RTT+alpha*(int)(endingTime - initialTime));
		DevRTT = (int)((1-beta)*DevRTT + beta*(int)Math.abs((endingTime - initialTime) - RTT));
		timeInterval = RTT + 4 * DevRTT;
	}
	
	public static void writeLog(int SeqNum, int ACK, int flag, String FLAG) throws UnknownHostException{

		//timestamp, source, destination, Sequence #, ACK #, and the flags
		String logString;
		if (flag == 0){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+InetAddress.getLocalHost().getHostAddress()+":"+
					ack_port_num+", "+remote_IP+":"+remote_port+", SEQ "+SeqNum+", ACK#"+
					", RTT "+RTT+"ms, flags "+FLAG;

		} else if (flag == 1){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+remote_IP+":"+remote_port+", "
					+InetAddress.getLocalHost().getHostAddress()+":"+ack_port_num+", SEQ#"+", ACK "+ACK+
					", RTT "+RTT+"ms"; 
		} else if (flag == 2){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+remote_IP+":"+remote_port+", "
					+InetAddress.getLocalHost().getHostAddress()+":"+ack_port_num+", SEQ#"+", NAK "+ACK+
					", RTT "+RTT+"ms";
		} else {return;}
		//		System.out.println(logString);
		outprint.println(logString);

	}
}