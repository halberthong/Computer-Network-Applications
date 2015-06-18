import java.io.*;
import java.util.*;

public class LoginLoad {
	
	public static ArrayList<String> username = new ArrayList<String>();
	public static ArrayList<String> password = new ArrayList<String>();
	
	
	public void load() throws Exception{
		String path = "./user_pass.txt";// path of user_pass.txt
		Scanner textReader = new Scanner(new File(path));
		
		int i = 0;
		while(textReader.hasNextLine()){
			String loginInfo = textReader.next();
			if (i%2 == 0){
				username.add(loginInfo);
			} else {
				password.add(loginInfo);
			}
			i++;
		}
		
		textReader.close();
	}	
}
