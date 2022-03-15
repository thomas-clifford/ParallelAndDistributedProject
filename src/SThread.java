import java.io.*;
import java.net.*;
import java.lang.Exception;


public class SThread extends Thread {
	private Object [][] routingTable; // routing table
	private DataOutputStream out, outTo; // writers (for writing back to the machine and to destination)
	private DataInputStream in; // reader (for reading from the machine connected to)
	private String inputLine, outputLine, destination, addr; // communication strings
	private Socket outSocket; // socket for communicating with a destination
	private int ind; // index in the routing table

	// Constructor
	SThread(Object [][] Table, Socket toClient, int index) throws IOException {
		out = new DataOutputStream(toClient.getOutputStream());
		in = new DataInputStream(toClient.getInputStream());
		routingTable = Table;
		addr = toClient.getInetAddress().getHostAddress();
		routingTable[index][0] = addr; // IP addresses
		routingTable[index][1] = toClient; // sockets for communication
		ind = index;
	}

	// Run method (will run for each machine that connects to the ServerRouter)
	public void run() {
		try {
			// Initial sends/receives
			int destinationLength = in.readInt();
			byte[] destinationBytes = new byte[destinationLength];
			in.readFully(destinationBytes, 0, destinationLength);
			destination = new String(destinationBytes); // initial read (the destination for writing)
			System.out.println("Forwarding to " + destination);
			String confirmationMessage = "Connected to the router.";
			out.writeInt(confirmationMessage.length()); // confirmation of connection
			out.write(confirmationMessage.getBytes());

			// waits 10 seconds to let the routing table fill with all machines' information
			try{
				Thread.currentThread().sleep(10000);
			} catch(InterruptedException ie) {
				System.out.println("Thread interrupted");
			}

			// loops through the routing table to find the destination
			boolean foundDestination = false;
			for (Object[] route : routingTable) {
				if (destination.equals((String) route[0])) {
					foundDestination = true;
					outSocket = (Socket) route[1]; // gets the socket for communication from the table
					outTo = new DataOutputStream(outSocket.getOutputStream()); // assigns a writer
				}
			}
			if (foundDestination) {
				System.out.println("Found destination: " + destination);
			}
			int messageLength;
			do {
				try {
					boolean isTextFile = false;
					messageLength = in.readInt();
					byte[] message = new byte[messageLength];
					in.readFully(message, 0, messageLength);
					String messageStr = new String(message);
					if (messageStr.contains(".") && !messageStr.substring(messageStr.lastIndexOf('.')).equals(".txt")) {
						isTextFile = true;
					}
					if (!isTextFile) {
						System.out.println("Client/Server said: " + messageStr);
					}
					outTo.writeInt(messageLength);
					outTo.write(message, 0, messageLength);
				} catch (EOFException e) {
					break;
				}

			} while(messageLength > 0);


		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}