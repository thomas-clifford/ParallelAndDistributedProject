package part2;

import java.io.*;
import java.net.*;
import java.lang.Exception;


public class SThread extends Thread {
	private Object [][] routingTable; // routing table
	private DataOutputStream out, outTo; // writers (for writing back to the machine and to destination)
	private DataInputStream in; // reader (for reading from the machine connected to)
	private String destination, addr; // communication strings
	private Socket outSocket; // socket for communicating with a destination
	private int ind; // index in the routing table
	private boolean isServerRouter;
	private int serverRouterPort;
	private Socket toClient;

	// Constructor
	SThread(Object [][] Table, Socket toClient, int index, int port) throws IOException {
		out = new DataOutputStream(toClient.getOutputStream());
		in = new DataInputStream(toClient.getInputStream());
		routingTable = Table;
		addr = toClient.getInetAddress().getHostAddress();
		ind = index;
		serverRouterPort = port;
		this.toClient = toClient;
	}

	// Run method (will run for each machine that connects to the ServerRouter)
	public void run() {
		// Initial sends/receives
		byte[] data = Common.getData(in);
		String dataStr = new String(data);

		if (!dataStr.contains("search routing table for destination: ")) {
			System.out.println("From peer: " + dataStr);
			// Confirm connection with ServerRouter with peer
			data = "From server router: successfully connected.".getBytes();
			Common.sendData(out, data);
			// Get destination from peer
			data = Common.getData(in);
			destination = new String(data);
			System.out.println("Attempting to connect to peer: " + destination);
		} else {
			isServerRouter = true;
			destination = dataStr.substring(dataStr.lastIndexOf(":") + 2);
		}

		// Search routing table for destination IP
		boolean foundDestination = false;
		for (Object[] route : routingTable) {
			if (destination.equals((String) route[0])) {
				foundDestination = true;
				outSocket = (Socket) route[1]; // gets the socket for communication from the table
				try {
					outTo = new DataOutputStream(outSocket.getOutputStream()); // assigns a writer
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (!isServerRouter) {
			routingTable[ind][0] = addr; // IP addresses
			routingTable[ind][1] = this.toClient; // sockets for communication
		}

		// If the other peer is found within the peer's network, connect them. Otherwise, ask the other network's server router if the peer exists.
		if (foundDestination) {
			System.out.println("Found destination peer through server router");
			if (isServerRouter) {
				// The other peer is connected to the other server router. Give the server router the peer's information
				System.out.println("Sending the destination back to the server router for confirmation.");
				data = destination.getBytes();
			} else {
				data = ("found peer at: " + destination).getBytes();
			}
			Common.sendData(this.out, data);
		} else {
			if (isServerRouter) {
				System.out.println("Could not find destination peer. Telling other server the peer is not connected.");
				// The other peer is not connected to either server router. Tell the peer to wait for connection.
				data = "could not find destination peer.".getBytes();
				Common.sendData(out, data);
			} else {
				System.out.println("Could not find destination through server router. " +
						"Attempting to find destination from peer's server router");
				coordinateWithServerRouter();
			}
		}
	}

	public void coordinateWithServerRouter() {
		try {
			int otherServerRouterPort;
			String otherServerRouterIP;
			if (serverRouterPort == Common.serverRouter1Port) {
				otherServerRouterIP = Common.serverRouter2IP;
				otherServerRouterPort = Common.serverRouter2Port;
			} else {
				otherServerRouterIP = Common.serverRouter1IP;
				otherServerRouterPort = Common.serverRouter1Port;
			}
			Socket socket = new Socket(otherServerRouterIP, otherServerRouterPort);
			// Should we identify ourselves as a fellow server router?
			// This way, the other server router doesn't try to collect information from us as if we are a peer.
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			byte[] data = ("search routing table for destination: " + destination).getBytes();
			Common.sendData(dos, data);
			data = Common.getData(dis);
			String returnMessage = new String(data);
			if (returnMessage.equals("could not find destination peer.")) {
				System.out.println("Did not find destination peer. Telling peer to wait.");
				data = ("did not find peer at: " + destination).getBytes();
			} else {
				System.out.println("Found destination peer. Telling peer to connect.");
				data = ("found peer at: " + destination).getBytes();
			}
			Common.sendData(this.out, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}