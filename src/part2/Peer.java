package part2;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Peer {
    int defaultPeerPort = 9876;
    String destinationIP;
    String serverRouterIP;
    int serverRouterPort;
    boolean waitingPeer;
    ArrayList<String> files = getFilesToSend();

    /**
     * A peer is an object that wants to communicate with another peer object. It does this by consulting with the
     * server routers. If the desired peer is online, begin communication; otherwise, wait for the peer to connect.
     * @param serverRouterIP IPv4 address of the peer's designated server router
     * @param serverRouterPort Port number that the peer's server router is listening on
     * @param destinationIP IPv4 address of the other peer
     * @throws IOException
     */
    public Peer(String serverRouterIP, int serverRouterPort, String destinationIP) throws IOException {
        this.serverRouterIP = serverRouterIP;
        this.serverRouterPort = serverRouterPort;
        this.destinationIP = destinationIP;
        registerWithServerRouter();
    }

    /**
     * Connect to the server router. The server router will search for the other peer. If that peer exists, this peer
     * will establish a TCP connection; otherwise, this peer will wait for the other peer to connect
     * @throws IOException
     */
    public void registerWithServerRouter() throws IOException {
        // Connect this peer to the server router
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

        // Confirm that the peer is connected to the server router
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

        // After searching for a connection, join space for communication with the other peer
        joinPeerCommunicationSpace(foundDestination);
    }

    /**
     * If a peer was found, connect to it; otherwise, establish a ServerSocket for the other peer to connect to.
     * Once a connection is established, begin sending data.
     * @param foundDestination
     * @throws IOException
     */
    public void joinPeerCommunicationSpace(boolean foundDestination) throws IOException {
        Socket peerSocket = null;
        if (foundDestination) {
            waitingPeer = false;
            System.out.println("Found destination peer. Attempting to establish connection at: " + this.destinationIP + ':' + this.defaultPeerPort + "\n");
            peerSocket = new Socket(this.destinationIP, this.defaultPeerPort);
        } else {
            waitingPeer = true;
            System.out.println("Destination peer not online. Waiting until they connect.");
            ServerSocket serverSocket = new ServerSocket(this.defaultPeerPort);
            System.out.println("Peer is listening on port: " + this.defaultPeerPort + "\n");
            peerSocket = serverSocket.accept();
        }
        DataInputStream dis = new DataInputStream(peerSocket.getInputStream());
        DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
        communicate(dis, dos);
    }

    /**
     * The Peers will communicate through this method. If this peer was waiting for the other peer to connect,
     * this peer will send data first and vice versa.
     * @param dis Data input stream used to receive data from the other peer.
     * @param dos Data output stream used to send data to the other peer.
     * @throws IOException
     */
    public void communicate(DataInputStream dis, DataOutputStream dos) throws IOException {
        HashMap<String, ArrayList<Long>> times = new HashMap<>();
            // If this peer was waiting for the other peer to connect, send data then accept data; otherwise, accept data then send data.
            long start = 0;
            long totalTime = 0;
            FileOutputStream fileOutputStream = null;
            int outgoingFileIndex = 0;
            int incomingFileIndex;
            byte[] fileContentBytes = new byte[0];
            if (waitingPeer) {
                dos.writeInt(files.size());
                incomingFileIndex = dis.readInt();
            } else {
                incomingFileIndex = dis.readInt();
                dos.writeInt(files.size());
            }
            do {
                if (waitingPeer) {
                    // Send outgoing file's name
                    if (outgoingFileIndex < files.size()) {
                        if (!times.containsKey(files.get(outgoingFileIndex))) {
                            times.put(files.get(outgoingFileIndex), new ArrayList<Long>());
                        }
                        File currentFile = new File("sendFiles/" + files.get(outgoingFileIndex));
                        FileInputStream fileInputStream = new FileInputStream(currentFile.getAbsolutePath());
                        fileContentBytes = new byte[(int) currentFile.length()];
                        fileInputStream.read(fileContentBytes);
                        pauseForPeer();
                        Common.sendData(dos, files.get(outgoingFileIndex).getBytes());
                    }
                    // Get incoming file's name
                    if (incomingFileIndex > 0) {
                        byte[] data = Common.getData(dis);
                        String incomingFileName = new String(data);
                        System.out.println("File incoming from peer: " + incomingFileName);
                        File downloadFile = new File("receiveFiles/" + incomingFileName);
                        fileOutputStream = new FileOutputStream(downloadFile);
                    }
                    // Send outgoing file's contents
                    if (outgoingFileIndex < files.size()) {
                        start = System.currentTimeMillis();
                        System.out.println("Sending file, " + files.get(outgoingFileIndex) + ", to Peer to download.");
                        Common.sendData(dos, fileContentBytes);
                        totalTime = System.currentTimeMillis() - start;
                        // Record the time it took to send the file
                        times.get(files.get(outgoingFileIndex)).add(totalTime);
                        System.out.println("Total Send Time: " + totalTime + " milliseconds\n");
                        outgoingFileIndex++;
                    }
                    // Get incoming file's contents
                    if (incomingFileIndex > 0) {
                        fileOutputStream.write(Common.getData(dis));
                        incomingFileIndex--;
                    }
                } else {
                    // Get incoming file's name
                    if (incomingFileIndex > 0) {
                        byte[] data = Common.getData(dis);
                        String incomingFileName = new String(data);
                        System.out.println("File incoming from peer: " + incomingFileName);
                        File downloadFile = new File("receiveFiles/" + incomingFileName);
                        fileOutputStream = new FileOutputStream(downloadFile);
                    }
                    // Send outgoing file's name
                    if (outgoingFileIndex < files.size()) {
                        if (!times.containsKey(files.get(outgoingFileIndex))) {
                            times.put(files.get(outgoingFileIndex), new ArrayList<Long>());
                        }
                        File currentFile = new File("sendFiles/" + files.get(outgoingFileIndex));
                        FileInputStream fileInputStream = new FileInputStream(currentFile.getAbsolutePath());
                        fileContentBytes = new byte[(int) currentFile.length()];
                        fileInputStream.read(fileContentBytes);
                        pauseForPeer();
                        Common.sendData(dos, files.get(outgoingFileIndex).getBytes());
                    }
                    // Get incoming file's contents
                    if (incomingFileIndex > 0) {
                        fileOutputStream.write(Common.getData(dis));
                        incomingFileIndex--;
                    }
                    // Send outgoing file's contents.
                    if (outgoingFileIndex < files.size()) {
                        start = System.currentTimeMillis();
                        System.out.println("Sending file, " + files.get(outgoingFileIndex) + ", to Peer to download.");
                        Common.sendData(dos, fileContentBytes);
                        totalTime = System.currentTimeMillis() - start;
                        // Record the time it took to send the file
                        times.get(files.get(outgoingFileIndex)).add(totalTime);
                        System.out.println("Total Send Time: " + totalTime + " milliseconds\n");
                        outgoingFileIndex++;
                    }
                }
            } while (outgoingFileIndex < files.size() || incomingFileIndex > 0);
        // Organize the file times in a CSV file
        createCsvFile(times);
    }

    /**
     * Pauses the Java process. This gives the other peer to catch up and remain in sync with this peer.
     */
    public static void pauseForPeer() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads each of the files names placed in filesToSend.txt these can be files of any type. The files must be
     * separated by newlines.
     *
     * @return an ArrayList of the files to be sent to ServerRouter
     */
    public static ArrayList<String> getFilesToSend() {
        String filename = "filesToSend.txt";
        ArrayList<String> fileList = new ArrayList<>();
        try {
            File filesToSend = new File(filename);
            BufferedReader filesToSendReader = new BufferedReader(new FileReader(filesToSend));

            String currentFile;

            while((currentFile = filesToSendReader.readLine()) != null) {
                fileList.add(currentFile);
            }
        } catch (IOException exception) {
            System.err.println("Unable to get files to send from file: " + filename);
        }
        return fileList;
    }

    /**
     * Creates a csv file with the times organized by filename.
     * @param times
     */
    public static void createCsvFile(HashMap<String, ArrayList<Long>> times) throws IOException {
        String fileContents = "";
        String currentLine = "";
        for (Map.Entry<String, ArrayList<Long>> entry : times.entrySet()) {
            currentLine = entry.getKey() + ",";
            for (long time : entry.getValue()) {
                currentLine += time + ",";
            }
            fileContents += currentLine.substring(0, currentLine.length() - 1) + "\n";
            currentLine = "";
        }
        FileWriter writer = new FileWriter(new File("timesCsv.csv"));
        writer.write(fileContents);
        writer.close();
    }
}