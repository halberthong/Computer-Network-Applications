**Simple TCP­-like transport-­layer protocol**

A. Brief Description

The packets has 5 files in total:
i) Contains 2 .java file.
sender.java
receiver.java

ii) Testing file
file.txt

iii) makefile: Input “make” in the terminal under the path of the folder and all the .java files will be complied. 

iv) readme

B. Development Environment

JAVA 1.7

C Instructions

MSS = 576, Timeout = 500ms initially

1. the proxy is invoked as:
./newudpl -o localhost:4119 -i localhost:9999 -p5000:6000 -L 20 -B 20 -d 1.5

2. The receiver program will be invoked as:
java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>
Ex: java receiver file1.txt 4119 localhost 9999 receiverLog.txt
or java receiver file1.txt 4119 localhost 9999 stdout

Notes:
i) If running the programs in two machines, you could use the remote user’s IP(it could be proxy or sender).
ii) If you are receiving a picture, you should change the filename as file.jpg(or some other types of pictures)

3.The sender program will be invoked as:
java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>
Ex: java sender file.txt localhost 5000 9999 senderLog.txt 1
or java sender file.txt localhost 5000 9999 stdout 1
(window size of 1 is set as default)

Notes:
i) Here “window_size” is set as its default value 1.
ii) If running the programs in two machines, you could use the remote user’s IP(it could be proxy or receiver).
iii) If the filename is “stdout”, the log file will not only be generated in the folder but the contents will be showed in the terminal.

4 If the programs works out successfully,
“Delivery completed successfully” will be showed in the receiver’s terminal.

Delivery completed successfully
Total bytes sent = 3930
Segments sent = 22
Segments retransmitted = 15

will be showed in the sender’s information

5 In the final, normally, 3 file will be generated in the folder: 
file.txt: It is exactly what the receiver get from the sender.
file1.txt: The output file from receiver.
senderLog.txt: Record the log information of the sender.
receiverLog.txt: Record the log information of the receiver.





