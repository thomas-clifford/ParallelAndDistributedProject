package part2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Peer {
    int defualtPeerPort = 9876;
    String destinationIP;
    String serverRouterIP;
    int serverRouterPort;

    public Peer(String serverRouterIP, int serverRouterPort, String destinationIP) throws IOException {
        this.serverRouterIP = serverRouterIP;
        this.serverRouterPort = serverRouterPort;
        this.destinationIP = destinationIP;
        registerWithServerRouter();
    }

    public void registerWithServerRouter() throws IOException {
        // Register this peer with the server router
        DataInputStream dis = null;
        DataOutputStream dos = null;
        Socket socket = null;
        try {
            socket = new Socket(this.serverRouterIP, this.serverRouterPort);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Confirm that we are connected to the server router
        byte[] data = ("Hello from " + InetAddress.getLocalHost().getHostAddress()).getBytes();
        assert dos != null;
        Common.sendData(dos, data);
        data = Common.getData(dis);
        System.out.println(new String(data));

        // Ask server router to search for peer.
        System.out.println("Attempting to find destination peer: " + this.destinationIP);
        Common.sendData(dos, this.destinationIP.getBytes());
        data = Common.getData(dis);
        String destinationCoordination = new String(data);
        boolean foundDestination = destinationCoordination.contains("found peer at: ");
        socket.close();

        // After searching for a connection, join space for communication with peer
        joinPeerCommunicationSpace(foundDestination);
    }

    public void joinPeerCommunicationSpace(boolean foundDestination) throws IOException {
        ServerSocket serverSocket = null;
        Socket peerSocket = null;
        if (foundDestination) {
            System.out.println("Found destination peer. Attempting to establish connection at: " + this.destinationIP + ':' + this.defualtPeerPort);
            peerSocket = new Socket(this.destinationIP, this.defualtPeerPort);
        } else {
            System.out.println("Destination peer not online. Waiting until they connect.");
            serverSocket = new ServerSocket(this.defualtPeerPort);
            System.out.println("Peer is listening on port: " + this.defualtPeerPort);
            peerSocket = serverSocket.accept();
        }
        // What if we make the communication method similar to the SThread class. Begin communicating with a single
        // peer while simultaneously searching for another peer to connect to.
        DataInputStream dis = new DataInputStream(peerSocket.getInputStream());
        DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
        communicate(dis, dos);
    }

    public void communicate(DataInputStream dis, DataOutputStream dos) {
        // TODO establish a universal communication method for peers. This will have to implement threading. The goal is to wait for a message, send a response, and begin waiting again.
        for (int i = 0; i < 3; i++) {
            byte[] data = new byte[0];
            try {
                data = ("From " + InetAddress.getLocalHost().getHostAddress() + ": Hello World").getBytes();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            Common.sendData(dos, data);
            data = Common.getData(dis);
            System.out.println(new String(data));
        }
    }
}