
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class Client {

	private static BufferedReader reader,stdIn;
	private static PrintWriter writer;
	private static int server_port_no;
	private static InetAddress server_address;
	private static long lastActiveTime;
	private static int TIME_OUT = 30*60*1000;// set client inactive time
	private static boolean flag = false;
	Scanner sc = new Scanner(System.in);
	
	public void displayTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		Date currentTime = new Date();
		String dateString = formatter.format(currentTime);
		System.out.print(dateString+"  ");
	}


	@SuppressWarnings("deprecation")
	private void run(InetAddress server_ip_address, int server_ip_port) {

		// Make connection and initialize streams
		
		Thread t1 = new Thread();//thread for user input
		Thread t2 = new Thread();//thread for checking whether the user is inactive for TIME_OUT
		
		try{
			Socket socket = new Socket(server_ip_address, server_ip_port);
//			System.out.println(socket);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);
			stdIn = new BufferedReader(new InputStreamReader(System.in));
			
			// Process all messages from server, according to the protocol.
			while (true) {
				
				String line = reader.readLine();
				
				if (line.startsWith("username")) {
					System.out.print("Username:");
					writer.println(stdIn.readLine());
					
				} else if(line.startsWith("password")){
					System.out.print("Password:");
					writer.println(stdIn.readLine());
					
				} else if (line.startsWith("wrong")) {
					System.out.println("Wrong username or password");	
					
				} else if (line.startsWith("welcome")) {
					System.out.println("Welcome to simple chat server!");
					System.out.println("Command: ");
					t1 = new Thread(new userOutput());
					t1.start();
					
					t2 = new Thread(new stateCheck());
					t2.start();
					
					flag = true;
					lastActiveTime = System.currentTimeMillis();
					
				} else if (line.startsWith("broadcast ")) {
					displayTime();
					System.out.println(line.substring(10));
					
				} else if (line.startsWith("message ")) {
					displayTime();
					System.out.println(line.substring(8));
					
				} else if (line.startsWith("fail")) {
					long banTime= Long.parseLong(line.substring(5));
					System.out.println("Login failed, you are blocked to use this username for "+(banTime/1000)+" seconds");
					socket.close();
					
				} else if (line.equals("logout")) {
					System.out.println("Logout successfully");
					
				} else if (line.startsWith("else ")) {
					System.out.println(line.substring(5));
					
				} else if (line.startsWith("lasthr ")) {
					System.out.println(line.substring(7));
					
				} else if (line.startsWith("block ")) {
					long banTime= Long.parseLong(line.substring(6));
					long remainBanTime = banTime/1000;					
					System.out.println("You are blocked to use this username for "+remainBanTime+" seconds");	
				
				} else if (line.equals("timeout")) {
					System.out.println("Due to long time of no input command, you are forced to log out!");
					
				} else if (line.equals("invalid")) {
					System.out.println("Invalid input, enter \"help\" for command instruction.");
					
				} else if (line.equals("loginagain")) {
					System.out.println("The username is used by others, please choose another username.");
					
				} else if (line.equals("helpinfo")) {
					System.out.println("-------------------------------command instruction------------------------------");
					System.out.println("whoelse:                  Name(s) of other connected user(s)");
					System.out.println("wholasthr:                Name(s) of user(s) that connected within the last hour");
					System.out.println("broadcast <message>:      Broadcasts <message> to all connected users");
					System.out.println("message <user> <message>: Private <message> to a <user>");
					System.out.println("logout:                   Log out this user");
					System.out.println("help:                     Displays help information");
					System.out.println("--------------------------------------------------------------------------------");
				}
			}

		} catch(Exception ex){
			System.out.println("Unable to connect server!");
			t1.stop();
			t2.stop();
		}

	} 

	//start the client program
	public static void main(String[] args) {
		try {
			Client.server_address = InetAddress.getByName(args[0]);
			Client.server_port_no = Integer.parseInt(args[1]);
			Client client = new Client();
			client.run(server_address, server_port_no);
		} catch (Exception ex){
			System.out.println("Wrong format of server's IP address or port number");
		}

	}
	
	//handle user input
	public class userOutput implements Runnable{
		public void run(){
			try{
				while (true){
					
					String userOut = stdIn.readLine();
					
					if (flag && !userOut.isEmpty()){
						lastActiveTime = System.currentTimeMillis();
						writer.println(userOut);
					} else {continue;}	
				}
			} catch (Exception ex){System.out.println("Unable to connect server!");}
		}
	}
	
	//handle user inactivity
	public class stateCheck implements Runnable{
		public void run(){
			try{
				while (true){				
					if (flag && System.currentTimeMillis() - lastActiveTime > TIME_OUT){
						writer.println("forcelogout");
					}
				}
			} catch (Exception ex){System.out.println("Error!");}
		}
	}
}
