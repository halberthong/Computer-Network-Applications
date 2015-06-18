JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) -classpath $(CLASSPATH) $(JFLAGS) $*.java

CLASSES = \
	adns.java

CLASSPATH = org.xbill.dns_2.1.7.jar
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
