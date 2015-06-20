# Distributed Bellman-Ford Router

A Instructions

The sender program will be invoked as:
localport timeout [ipaddress1 port1 weight1 ipaddress2 port2 weight2 …]
Example: java bfclient 4115 3 128.59.229.226 4116 5.0

B Notifications

1) the default environment of Linux can only return the local IP address as 127.0.1.1, this might result in error.
2) IP address can only be entered manually, i.e. “localhost” does not work.
3) Maximum number of neighbors allowed is specified in the field “MAX_NEIGHBORS”. Default value is 30.
4) Maximum size (in byte) of routing table allowed is specified in the field “MSS”. Default value is 1000.

C Advanced functions

1) Poisoned reverse problem is resolved.

2) new command: SHOWNEIGHBOR to display the current neighbor of the node.

D Inter-client Communications Protocol
The format of UPDATE message sending among nodes is: [source-address source-port destination1-address destination1-port cost1 …]
