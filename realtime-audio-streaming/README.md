# Realtime Audio Streaming

use command: “make”
to compile .java file to .class file

operations:
1. in one terminal, type in “java AudioReceiver”: listens on default port 5100 and plays audio from localhost; or “java AudioReceiver (IP address)” to play audio from the assigned IP address.

2. in another terminal, type in “java AudioSender” to send audio stream in real time to the Receiver.

The audio stream is send in UDP packets, each contains 1000 bytes of audio stream data.
