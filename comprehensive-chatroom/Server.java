
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	private static int LAST_HOUR = 60*60*1000;//set 
	private static int BLOCK_TIME = 60*1000;
	private static int server_port_no;
	private static int loginLimit = 3;
	public static ServerSocket listener;

	public static ArrayList<ClientHandler> clientList = new ArrayList<ClientHandler>();
	public static ArrayList<BanHandler> banList = new ArrayList<BanHandler>();
	
	
	public static void main(String[] args) {

		try{
			LoginLoad loginLoad = new LoginLoad();//load login information from .txt file
			loginLoad.load();
		} catch(Exception ex) {System.out.println("Fail to load username-password information!");}	

		server_port_no = Integer.parseInt(args[0]);
		try{
			System.out.println("the chat server is running!");
			listener = new ServerSocket(server_port_no);
			try {
				while(true){
					Socket clientSocket = listener.accept();
//					System.out.println(clientSocket);
					Thread t2 = new Thread(new ClientHandler (clientSocket));
					t2.start();
//					System.out.println("got a connection from user!");

				}
			} finally{
				listener.close();
			}
		} catch (Exception ex) {System.out.println("Server shutdown!");}
		
	}

	public static class BanHandler{
		InetAddress blockIP;
		String name;
		long banTime;
		
		public BanHandler (InetAddress inputblockIP, String inputname, long inputbanTime){
			blockIP = inputblockIP;
			name = inputname;
			banTime = inputbanTime;
		}
	}

	public static class ClientHandler implements Runnable {
		private BufferedReader reader;
		private PrintWriter writer;
		private Socket socket;
		public String name = null;
		public String username;
		public String password;
		public long loginTime;
		public long logoutTime;
		public boolean isOnline;

		public ClientHandler(Socket clientSocket) {//Constructor
			this.socket = clientSocket;
		}
		
		private static ArrayList <String> tobeBan = new ArrayList <String>();//list of possible block username
		@Override
		public void run(){
			boolean flag = true;
			
			
			try{
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream(),true);
				//to demonstrate whether the user passed authentication
				
				while(true){
					boolean flagLoginagain = true;
					
					writer.println("username");
					name = reader.readLine();
					writer.println("password");
					password = reader.readLine();
					
					Iterator <BanHandler> bancheckIterator = banList.iterator();
					while (bancheckIterator.hasNext()){
						BanHandler bancheckTemp = bancheckIterator.next();
						if (System.currentTimeMillis() - bancheckTemp.banTime >= BLOCK_TIME){
							bancheckIterator.remove(); 
						}
					}
					
					Iterator <BanHandler> banIterator = banList.iterator();
					while (banIterator.hasNext()){
						BanHandler banTemp = banIterator.next();						
						if (name.equals(banTemp.name) && socket.getInetAddress().equals(banTemp.blockIP)){
							writer.println("block "+(BLOCK_TIME+banTemp.banTime-System.currentTimeMillis()));
							flag = false;
						}						
					}					
					
					if (flag == false){
						break;
					}
					
					if (LoginLoad.username.contains(name)){

						if (LoginLoad.username.indexOf(name) == LoginLoad.password.indexOf(password)){

							Iterator <ClientHandler> clientIterator = clientList.iterator();
							while (clientIterator.hasNext()){
								ClientHandler clientTemp = clientIterator.next();				
								if (clientTemp.isOnline && name.equals(clientTemp.username)){
									this.writer.println("loginagain");
									flagLoginagain = false;
								} else if(!clientTemp.isOnline && name.equals(clientTemp.username)){
									clientIterator.remove();
								}
							}
							
							if (flagLoginagain == false) {
								continue;
							}
							writer.println("welcome");
							this.username = name;
							System.out.println("user \""+this.username+"\" is authenticated");
							this.loginTime = System.currentTimeMillis();
							this.isOnline = true;
							Server.clientList.add(this);
							break;
						}
						if (LoginLoad.username.indexOf(name) != LoginLoad.password.indexOf(password)){
							writer.println("wrong");
							//deal with three times block
							if (tobeBan.isEmpty()){
								tobeBan.add(name);
							} else if (tobeBan.get(tobeBan.size()-1).equals(name)){
								tobeBan.add(name);
							} else if (!tobeBan.get(tobeBan.size()-1).equals(name)) {
								tobeBan.clear();
							}
						}

					} else {	
						writer.println("wrong");
						tobeBan.clear();
					}
					if (tobeBan.size() == loginLimit){
						flag = false;
						writer.println("fail "+BLOCK_TIME);
						InetAddress blockIP = socket.getInetAddress();
						BanHandler banClient = new BanHandler (blockIP, name, System.currentTimeMillis());
						banList.add(banClient);
						tobeBan.clear();
						break;
					}										
				}


				while (flag){

					String input = reader.readLine();
					//if user has no input in 30 minutes, the user will be blocked				
					if (input.equals("forcelogout")) {
						System.out.println("user "+this.username+" is forced to logout");
						this.writer.println("timeout");
						this.isOnline = false;
						this.logoutTime = System.currentTimeMillis();
						this.socket.close();
						break;
					}				
					//implements the broadcast command
					else if (input.startsWith("broadcast ")){

						for (ClientHandler clientTemp : clientList){
							if (!clientTemp.username.equals(this.username)){
								clientTemp.writer.println("broadcast "+this.username+": "+input.substring(10));
							}
						}		
					} 
					//implements the logout command
					else if (input.equals("logout")){
						this.writer.println("logout");
						this.isOnline = false;
						this.logoutTime = System.currentTimeMillis();
						this.socket.close();					
					}
					//implements the whoelse command
					else if (input.equals("whoelse")){
						for (ClientHandler clientTemp : clientList){
							if (clientTemp.isOnline == true && !clientTemp.username.equals(this.username)){
								this.writer.println("else "+clientTemp.username);
							}
						}
					}
					//implements the wholasthr command
					else if (input.equals("wholasthr")){
						for (ClientHandler clientTemp : clientList){
							if (clientTemp.isOnline == true 
									&& !clientTemp.username.equals(this.username)) {
								this.writer.println("lasthr "+clientTemp.username);
							} else if (System.currentTimeMillis() - clientTemp.logoutTime <= LAST_HOUR 
									&& !clientTemp.username.equals(this.username)){
								this.writer.println("lasthr "+clientTemp.username);
							}
						}
					}
					//implements private messaging function
					else if (input.startsWith("message ")){

						int[] index = new int[2];//restore the location of username
						index[0] = 0;
						index[1] = 0;
						int k = 0;
						for (int m = 0; m < input.length(); m++){
							if (input.charAt(m) == ' ' && index[1] == 0){
								index[k] = m;
								k++;
							}
						}

						String tempUsername = input.substring(index[0]+1, index[1]);
						for (ClientHandler clientTemp : clientList){
							if (clientTemp.username.equals(tempUsername)){
								clientTemp.writer.println("message "+this.username+": "+input.substring(index[1]+1));
							}
						}

					} 
					
					else if (input.equals("help")) {
						this.writer.println("helpinfo");
					}
					
					else{
						this.writer.println("invalid");
					}
				}
			} catch (Exception e){
				if (this.username != null){
					this.isOnline = false;
					this.logoutTime = System.currentTimeMillis();
					System.out.println("user \""+this.username+"\" is disconnected");
				} else {System.out.print("");}
			
			} finally{
				try{
					socket.close();
				} catch(IOException e){
					System.out.println("fail to close a socket");
				}
			}			
		}

	}
}
