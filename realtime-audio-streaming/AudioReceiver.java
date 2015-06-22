import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import javax.sound.sampled.*;

public class AudioReceiver {
	
	int listeningPort = 5100;
	DatagramSocket socket;
	DatagramPacket packet;
	final int blockSize = 1000;
	static SourceDataLine line = null;
	AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

	AudioFormat PCMFormat = new AudioFormat(8000, 16, 1, true, false);
	static AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);
	
	
	public static void main(String[] args) throws Exception{
		AudioReceiver ar = new AudioReceiver();
		ar.start();
		ar.receivePacket();
	}
	
	public void start(){
		try {
			socket = new DatagramSocket(listeningPort);
			System.out.println("Receiving audio streaming");
			System.out.println("Please use \"Ctrl+C\" to terminate audio streaming.");
		} catch (SocketException e) {
			System.out.println("Unable to create socket!");
		}	
	}
	
	void receivePacket() throws Exception{
		
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, PCMFormat);
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(PCMFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		line.start();
		byte [] recvPacket = new byte[blockSize];
		while(true){
			packet = new DatagramPacket(new byte[blockSize], blockSize);
			socket.receive(packet);
			recvPacket = packet.getData();
			play(recvPacket);
		}
	}
	
	public static void play(byte[] data) {
    	InputStream in = new ByteArrayInputStream(data);
        AudioInputStream ulawSt = new AudioInputStream(in, ulawFormat, data.length);          
        AudioInputStream pcmSt = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ulawSt);
        
        int	bytesLeft = 0;
        byte[]	buffer = new byte[1000];
        while (bytesLeft != -1)
        {
            try
            {
                bytesLeft = pcmSt.read(buffer, 0, buffer.length);
            }
            catch (Exception e) { e.printStackTrace(); }
            if (bytesLeft >= 0)
                line.write(buffer, 0, bytesLeft);
        }
    }
	
}
