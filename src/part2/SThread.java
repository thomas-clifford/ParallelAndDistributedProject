package part2;

import java.io.*;
import java.net.*;
import java.lang.Exception;


public class SThread extends Thread {
	private Object [][] routingTable; // routing table
	private DataOutputStream out; // writers (for writing back to the machine and to destination)
	private DataInputStream in; // reader (for reading from the machine connected to)
	private String destination, addr; // communication strings
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

		// If the server router (SR) is connected to another SR, the other SR will ask this SR to search for the peer;
		// otherwise, the SR is connected to a peer.
		if (dataStr.contains("search routing table for destination: ")) {
			isServerRouter = true;
			destination = dataStr.substring(dataStr.lastIndexOf(":") + 2);
		} else {
			System.out.println("From peer: " + dataStr);
			// Confirm connection with ServerRouter with peer
			data = "From server router: successfully connected.".getBytes();
			Common.sendData(out, data);
			// Get destination from peer
			data = Common.getData(in);
			destination = new String(data);
			System.out.println("Attempting to connect to peer: " + destination);
		}

		// Search routing table for destination peer.
		boolean foundDestination = false;
		for (Object[] route : routingTable) {
			if (destination.equals((String) route[0])) {
				foundDestination = true;
			}
		}

		// Only add the connected machine's IP address to the routing table if it is not another Server Router.
		if (!isServerRouter) {
			routingTable[ind][0] = addr; // IP addresses
			routingTable[ind][1] = this.toClient; // sockets for communication
		}

		// If the other peer is found within the peer's network, connect them; otherwise, ask the other network's server router if the peer exists.
		if (foundDestination) {
			System.out.println("Found destination peer through server router");
			if (isServerRouter) {
				// The other peer is connected to the other server router. Give the initial server router the peer's information
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
				// The other peer is not connected to the initial peer's server router. Consult with the other server router.
				System.out.println("Could not find destination through server router. " +
						"Attempting to find destination from peer's server router");
				coordinateWithServerRouter();
			}
		}
	}

	/**
	 * If this server router did not find the destination peer, ask the other server router if the peer is connected.
	 * If it is, get its IPv4 address. If it isn't, tell the initial peer to wait for the destination peer.
	 */
	public void coordinateWithServerRouter() {
		try {
			// Connect to the OTHER server router
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

			// Tell the other server router to search for the destination peer's address
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			byte[] data = ("search routing table for destination: " + destination).getBytes();
			Common.sendData(dos, data);
			data = Common.getData(dis);
			String returnMessage = new String(data);

			// If the other SR found the peer, return success message; otherwise, tell the peer to wait.
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