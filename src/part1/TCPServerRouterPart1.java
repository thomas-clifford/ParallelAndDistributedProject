package part1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServerRouterPart1 {
    public static void main(String[] args) throws IOException {
        Object [][] routingTable = new Object [10000][2]; // routing table
        int routerIndex = 0; // index in the routing table

        Socket clientSocket = null; // socket for the thread
        final int PORT = 5555; // port number
        ServerSocket serverSocket = null; // server socket for accepting connections
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("ServerRouter is Listening on port: " + PORT);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + PORT);
            System.exit(1);
        }

        // Creating threads with accepted connections
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                SThreadPart1 clientThread = new SThreadPart1(routingTable, clientSocket, routerIndex); // creates a thread with a random port
                clientThread.start(); // starts the thread
                routerIndex++; // increments the index
                System.out.println("ServerRouter connected with Client/Server: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("Client/Server failed to connect.");
                // closing connections
                clientSocket.close();
                serverSocket.close();
                System.exit(1);
            }
        }
    }
}