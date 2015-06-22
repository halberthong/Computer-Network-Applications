import javax.sound.sampled.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioSender {
    // record duration, in milliseconds
    static InetAddress remoteIP;
	static int remotePort = 5100;
	static int blockSize = 1000;
	private static DatagramSocket socket;
	private AudioInputStream ulawStream;
 
    // the line from which audio data is captured
    TargetDataLine line;
 
    AudioFormat getAudioFormat() 
    {
        float sampleRate = 8000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        return format;
    }
    
    void start() {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
 
            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                System.exit(0);
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();   // start capturing
  
            AudioInputStream ais = new AudioInputStream(line);
            System.out.println("Audio is on!");
            System.out.println("Please use \"Ctrl+C\" to terminate audio streaming.");
            
            AudioFormat.Encoding encoding = AudioFormat.Encoding.ULAW;
			ulawStream = AudioSystem.getAudioInputStream(encoding, ais);
			
			byte[] sendBytes = new byte[blockSize];
			while(true){
				try
	            {
	                if(ulawStream.available() > blockSize)
	                {
	                    ulawStream.read(sendBytes, 0, blockSize);
	                    sendPacket(sendBytes);
	                }
	            }
	            catch (IOException e) {System.out.println(e);}
			}
 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    void sendPacket(byte[] sendBytes) throws Exception{
		DatagramPacket sendpacket = new DatagramPacket
				(sendBytes, sendBytes.length, remoteIP, remotePort);
		socket.send(sendpacket);
	}
    
    public static void main(String[] args) {
    	
    	try {
    		if(args.length > 0){
    			remoteIP = InetAddress.getByName(args[0]);
    		}else remoteIP = InetAddress.getByName("127.0.0.1");
    		
    		socket = new DatagramSocket(5000);
		} catch(Exception e){
			System.out.println("Wrong format of input!");
		}
    	
        final AudioSender recorder = new AudioSender();
        
        // start recording
        recorder.start();
    }
}