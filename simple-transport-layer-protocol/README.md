# Simple TCP­-like transport-­layer protocol

MSS = 576, Timeout = 500ms initially

1. the proxy is invoked as:
./newudpl -o localhost:4119 -i localhost:9999 -p5000:6000 -L 20 -B 20 -d 1.5

2. The receiver program will be invoked as:
java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>
Ex: java receiver file1.txt 4119 localhost 9999 receiverLog.txt
or java receiver file1.txt 4119 localhost 9999 stdout

3. The sender program will be invoked as:
java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>
Ex: java sender file.txt localhost 5000 9999 senderLog.txt 1
or java sender file.txt localhost 5000 9999 stdout 1
(window size of 1 is set as default)

4. If the programs works out successfully,
“Delivery completed successfully” will be showed in the receiver’s terminal, with statistics on segment sent and retransmitted.







