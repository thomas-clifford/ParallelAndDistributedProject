# ParallelAndDistributedProject

How to run:

filesToSend.txt contains a list of the files that the client will send to the server. Each file is separated by a newline character.

In part1.TCPServerPart1.java, line 20 is a for loop. This loop accepts a connection for every new file in filesToSend. 
The number in the for loop should correspond to the number of files in filesToSend.txt. 
I have not found a way to automate this yet, so that is why it is hard coded. For instance, if there are 3 files in filesToSend.txt:

for (int i = 0; i < 3; i++)

Each of the 3 programs should be run on their own machine. This way, each of the socket connections have their own IP address.

Start part1.TCPServerRouterPart1.java. This will facilitate connections between the server and the router
Start part1.TCPServerPart1.java. This will accept connection from part1.TCPClientPart1.java through part1.TCPServerRouterPart1.javva
Start part1.TCPClientPart1.java. This will send data to part1.TCPServerPart1.java through part1.TCPServerRouterPart1.java.


