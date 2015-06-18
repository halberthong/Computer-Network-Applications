# adns
The program consists of 2 files:
1. adns.java
2. org.xbill.dns_2.1.7.jar

This program allows you to retrive DNS infomation.

use command: “make”
to compile .java file to .class file

then use command: java -classpath org.xbill.dns_2.1.7.jar:. adns type domain
For example: java -classpath org.xbill.dns_2.1.7.jar:. adns CNAME www.cs.columbia.edu

The default DNS port is 53
