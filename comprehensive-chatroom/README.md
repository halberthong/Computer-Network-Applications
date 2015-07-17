# Comprehensive Chatroom
This program builds a comprehensive server for messaging, 
including elementary security features and current state of the chat­room.
This will require that various processes in different machines are able to work with each other,
and recuperate in case of asynchronous messaging and failure.
This chat program is based on a client server model consisting of one chat server and multiple chat clients over TCP connections.
The server is mainly used to authenticate the chat clients and direct the chat messages to one another.
Besides, the server also has to support certain commands that the clients can use.

(a) code description:
My source code includes Server.java, LoginLoad.java and Client.java.

1. BLOCK_TIME and LAST_HOUR is defined as class variable in Server.java; TIME_OUT is defined as class variable in Client.java; the path for user_txt is defined in LoginLoad.java, which default path is "./user_pass.txt".

2. Server.java implements function of handling client's request and maintain multi-threaded process, each thread handle the issue of one client. There are two internal class in Server: BanHandler that blocks user from 3 times consecutive wrong login; ClientHandler that implements Runnable and handles client's request.

3. The main() function in Client.java implements connecting to Server through Socket and receiving message from Server. There are two internal class in Client: UserOutput that implements Runnable and control the output stream from client to server; StateCheck that implements Runnable and send message to Server to disconnect when user is inactive for more than TIME_OUT.

4. LoginLoad.java load the username-password information from user_pass.txt, in order to authenticate user.

5. all functions required is realized, including username-password login, whoelse, wholasthr, broadcast, message and logout.

(b) development environment:
Tested in java 1.8 and 1.7 (java 1.6 should work)

(c) instruction of how to run the code
1. enter "make" to invoke makefile that generate .class file and be ready run.

2. At server terminal, input "java Server [port_number]", the port_number can be any available
port number.
3. At client terminal, input "java Server [server_ip_address] [server_port_number]", after authentication.

(d) sample command
all commands are identical to the samples on Programming Assignment 1, you can enter "help" for full command instruction.

(e) additional functionalities
1. If you enter "help", screen will display a list of command instruction.
2. If the message is broadcast or private to one user, the message will display with current time.
3. All useful state will be displayed on Server screen. For example: "user "columbia" is authenticated", "user "seas" is disconnected" etc.
4. If a username is blocked for a host, the Server will tell the user how much time left of blocking

If you have any question, please feel free to contact me: ah3209@columbia.edu
