package part1;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TCPClientPart1 {
    public static void main(String[] args) throws IOException {
        // Default socket information for TCP connection
        Socket socket = null;
        String client = InetAddress.getLocalHost().getHostAddress();
        String serverRouterIP = "10.0.0.116";
        String serverIP = "10.0.0.114";
        int port = 5555;

        // Variables for recording times in a csv
        HashMap<String, ArrayList<Long>> times = new HashMap<>();

        // For every file sent to the ServerRouter, connect to ServerRouter, send message, and close connection.
        ArrayList<String> filesToSend = getFilesToSend();
        int iteration = 1;
        for (String filename : filesToSend) {
            System.out.println("Iteration: " + iteration);
            if (!times.containsKey(filename)) {
                times.put(filename, new ArrayList<Long>());
            }
            ArrayList<Long> timesForFile = new ArrayList<>();
            // Current file to be sent to the ServerRouter
            File currentFile = new File("testFiles/" + filename);

            // Connect to the ServerRouter through a Socket. Also, establish a writer and reader between the ServerRouter
            try {
                socket = new Socket(serverRouterIP, port);
            } catch (UnknownHostException e) {
                System.err.println("Don't know about router: " + serverRouterIP);
                System.exit(1);
            } catch(IOException exception) {
                System.err.println("Couldn't get I/O for the connection to: " + serverRouterIP);
                System.exit(1);
            }

            // Begin sending the file's data to the ServerRouter. The returned value should depend on the type of the file.
            long start = System.currentTimeMillis();
            if (filename.contains(".txt")) {
                System.out.println("Sending contents of " + filename + " to ServerRouter to download and return the message in uppercase.");
                executeTCPConnection(socket, client, serverIP, currentFile);
            } else if (filename.contains(".wav")) {
                System.out.println("Sending file, " + filename + " to ServerRouter to download and return a success message");
                executeTCPConnection(socket, client, serverIP, currentFile);
            } else if (filename.contains(".mp4")) {
                System.out.println("Sending file, " + filename + " to ServerRouter to download and return a success message");
                executeTCPConnection(socket, client, serverIP, currentFile);
            }
            long totalTime = System.currentTimeMillis() - start;
            times.get(filename).add(totalTime);
            System.out.println("Total Transfer Time: " + totalTime + " milliseconds\n");
            socket.close();
            iteration++;
        }
        createCsvFile(times);
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

    /**
     * Creates the PrintWriter and BufferedReader for communication between client and server. At the very least,
     * these will be used to confirm a connection between two. This method completes the handshake section of the
     * TCP process.
     *
     * @param socket the socket that connects the client to the ServerRouter
     * @param client the IP of the client (the device running this program)
     * @param server the IP of the server (the device operating on the wav file)
     * @return an array with the PrintWriter at index 0 and the BufferedReader at index 1
     */
    public static Object[] handshake(Socket socket, String client, String server) throws IOException {
        DataOutputStream toServerRouter = null;
        DataInputStream fromServerRouter = null;
        try {
            toServerRouter = new DataOutputStream(socket.getOutputStream());
            fromServerRouter = new DataInputStream(socket.getInputStream());
        } catch (UnknownHostException e) {
            System.err.println("Don't know about router: " + socket.getLocalAddress().getHostAddress());
            System.exit(1);
        } catch(IOException exception) {
            System.err.println("Couldn't get I/O for the connection to: " + socket.getLocalAddress().getHostAddress());
            System.exit(1);
        }

        // Initial communication with the ServerRouter. This just confirms the connection.
        byte[] serverName = server.getBytes();
        toServerRouter.writeInt(serverName.length);
        toServerRouter.write(serverName);

        int serverResponseLength = fromServerRouter.readInt();
        byte[] serverResponse = new byte[serverResponseLength];
        fromServerRouter.readFully(serverResponse, 0, serverResponseLength);
        String fromServer = new String(serverResponse);
        System.out.println("Message from ServerRouter: " + fromServer);

        byte[] clientName = client.getBytes();
        toServerRouter.writeInt(clientName.length);
        toServerRouter.write(clientName);

        return new Object[]{toServerRouter, fromServerRouter};
    }

    /**
     * Completes a TCP Connection with the ServerRouter. Begins with a handshake, receives a confirmation message,
     * sends data from the file, and receives a response.
     *
     * @param socket the socket that connects the client to the ServerRouter
     * @param client the IP of the client (the device running this program)
     * @param server the IP of the server (the device operating on the file)
     * @param currentFile the file that is sent to the server through an input stream
     * @throws IOException if handshake fails between client and server.
     */
    public static void executeTCPConnection(Socket socket, String client, String server, File currentFile) throws IOException {
        // Writers and readers for sending messages to and from the ServerRouter
        Object[] writerAndReader = handshake(socket, client, server);
        DataOutputStream toServerRouter = (DataOutputStream) writerAndReader[0];
        DataInputStream fromServerRouter = (DataInputStream) writerAndReader[1];
        String fromServer;

        int serverResponseLength = fromServerRouter.readInt();
        byte[] serverResponse = new byte[serverResponseLength];
        fromServerRouter.readFully(serverResponse, 0, serverResponseLength);
        fromServer = new String(serverResponse);

        System.out.println("Server: " + fromServer);

        FileInputStream fileInputStream = new FileInputStream(currentFile.getAbsolutePath());

        String fileName = currentFile.getName();
        byte[] fileNameBytes = fileName.getBytes();

        byte[] fileContentBytes = new byte[(int)currentFile.length()];
        fileInputStream.read(fileContentBytes);

        if (fileName.substring(fileName.lastIndexOf('.')).equals(".txt")) {
            System.out.println("Client: " + new String(fileContentBytes));
        }

        toServerRouter.writeInt(fileNameBytes.length);
        toServerRouter.write(fileNameBytes);

        toServerRouter.writeInt(fileContentBytes.length);
        toServerRouter.write(fileContentBytes);

        serverResponseLength = fromServerRouter.readInt();
        serverResponse = new byte[serverResponseLength];
        fromServerRouter.readFully(serverResponse, 0, serverResponseLength);
        fromServer = new String(serverResponse);

        System.out.println("Server: " + fromServer);

        toServerRouter.close();
        fromServerRouter.close();
    }
}