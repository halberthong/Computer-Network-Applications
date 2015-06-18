import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xbill.DNS.*;

public class adns
{
	public static void main(String[] args) throws IOException
	{
		if(args.length != 2){
			System.out.println("Wrong format of command");
			System.exit(0);
		}
		String inputType = args[0];
		String host = args[1];
		Record[] record = new Lookup(host, Type.A).run();

		switch(inputType)
		{
			case "A":
				record = new Lookup(host, Type.A).run();
				for (int i = 0; i < record.length; i++) 
				{
					ARecord AData = (ARecord) record[i];
					System.out.println(AData);
				}
				break;
			case "CNAME":
				record = new Lookup(host, Type.CNAME).run();
				for (int i = 0; i < record.length; i++) 
				{
					CNAMERecord cnameData = (CNAMERecord) record[i];
					System.out.println(cnameData);
				}
				break;
			case "SRV":
				record = new Lookup(host, Type.SRV).run();
				if(record == null){
					System.out.println("No SRV record");
					break;
				}
				for (int i = 0; i < record.length; i++) 
				{
					SRVRecord srvData = (SRVRecord) record[i];
					System.out.println(srvData);
				}
				break;
			default :
				System.out.println("Doesn't support such type!");
		}
	}
}