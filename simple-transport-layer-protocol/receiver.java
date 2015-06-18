import java.io.*;
import java.net.*;
import java.sql.Timestamp;
public class receiver {
	private static String filename;
	private static int listening_port;
	private static InetAddress sender_IP;
	private static int sender_port;

	private static final int MSS = 576;
	private static final int headerSize = 20;

	private static String log_filename;
	private static int seqFlag = 0;
	private static PrintStream outprint;
	private static FileOutputStream fileout;

	public static void main(String[] args) throws IOException {

		try {
			filename = args[0];
			listening_port = Integer.parseInt(args[1]);
			sender_IP = InetAddress.getByName(args[2]);
			sender_port = Integer.parseInt(args[3]);
			log_filename = args[4];

		} catch (Exception ex){
			System.out.println("Wrong format of input");
		}

		if (log_filename.equals("stdout")){
			outprint = System.out;
		} else {
			try{
				outprint = new PrintStream(log_filename);
			} catch (FileNotFoundException e) {
				System.out.println("Unable to write logfile");
			}
		}

		try{
			fileout = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to write file");
		}
		receiver recv = new receiver();
		recv.run();
		outprint.close();
		fileout.close();
		System.exit(0);
	}

	private void run(){

		try{
			DatagramSocket recvSocket = new DatagramSocket(listening_port);
			while(true){
				DatagramPacket packet = new DatagramPacket(new byte[MSS], MSS);
				recvSocket.receive(packet);

				byte[] receivedData = packet.getData();

				byte[] originalChecksum = new byte[2];
				originalChecksum[0] = receivedData[16];
				originalChecksum[1] = receivedData[17];
				receivedData[16] = 0b0000_0000;
				receivedData[17] = 0b0000_0000;

				byte[] ACK = new byte[1];
				ACK[0] = 0b0000_0001;
				byte[] NAK = new byte[1];
				NAK[0] = 0b0000_0000;

				byte[] newChecksum = CRC(receivedData);
				if (newChecksum[0] == originalChecksum[0] && newChecksum[1] == originalChecksum[1]){
					int seqNum = byteToInt(receivedData, 4, 7);

					if (seqNum == seqFlag){
						seqFlag++;
						DatagramPacket feedback = new DatagramPacket(ACK, 1, sender_IP, sender_port);
						recvSocket.send(feedback);
						int lastSize = 0;
						int packetSize = MSS - headerSize;
						if ((receivedData[13] & 1) == 1){
							for (int i = headerSize; i < receivedData.length; i++){
								if (receivedData[i] != 0){
									lastSize++;
								} else{
									break;
								}
							}
							packetSize = lastSize;
						}

						byte[] sourcePkt = new byte[packetSize];
						System.arraycopy(receivedData, headerSize, sourcePkt, 0, packetSize);
						fileout.write(sourcePkt);
						writeLog(seqNum, 0, String.format("%8s", Integer.toBinaryString(receivedData[13] & 0xff)).replace(' ', '0'));
						writeLog(seqNum, 1, "");
						if ((receivedData[13] & 1) == 1){
							break;
						}
					} else {
						continue;
					}
				} else {
					DatagramPacket feedback = new DatagramPacket(NAK, 1, sender_IP, sender_port);
					recvSocket.send(feedback);
					writeLog(0, 2, "");
					continue;
				}
			}
			recvSocket.close();
			System.out.println("Delivery completed successfully");
		} catch (Exception ex){
			System.out.println("File transmission failed");
			System.exit(0);
		}
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
		byte[] checksum = intToByte(crc, 2);
		return checksum;
	}

	private static byte[] intToByte( int input, int size){

		byte[] output = new byte[size];
		for (int i = 0; i < size; i++) {
			output[i] = (byte) (input >>> (i * 8));
		}
		return output;
	}

	private static int byteToInt( byte[] input, int start, int end){

		int output = 0;
		for (int i = end; i >= start; i--) {
			int j = i -start;
			output += (int)(input[i] & 0xff) << (j*8);
		}
		return output;
	}

	public static void writeLog(int SeqNum, int flag, String FLAG) throws UnknownHostException{

		//<filename>  <listening_port>  <sender_IP>  <sender_port>  <log_filename>
		String logString;
		if (flag == 0){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+sender_IP+" "+sender_port+", "+InetAddress.getLocalHost().getHostAddress()+":"+		listening_port+", SEQ "+SeqNum+", ACK#, Flags "+FLAG;

		} else if (flag == 1){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+InetAddress.getLocalHost().getHostAddress()+":"+listening_port+
					", "+sender_IP+":"+sender_port+", ACK "+SeqNum;
		} else if (flag == 2){
			String timeStamp = new Timestamp(System.currentTimeMillis()).toString();
			logString = timeStamp+", "+InetAddress.getLocalHost().getHostAddress()+":"+listening_port+
					", "+sender_IP+":"+sender_port+", NAK";
		} else{return;}
		outprint.println(logString);

	}

}
