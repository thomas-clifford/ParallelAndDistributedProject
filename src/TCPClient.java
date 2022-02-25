import javax.sound.sampled.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TCPClient {
    public static void main(String[] args) throws IOException {
        // Default socket information for TCP connection
        Socket socket = null;
        String client = InetAddress.getLocalHost().getHostAddress();
        String serverRouterIP = "10.0.0.9";
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
                System.out.println("Sending contents of " + filename + " to ServerRouter to convert to uppercase.");
                startTextFileSession(socket, client, serverIP, currentFile);
            } else if (filename.contains(".wav")) {
                System.out.println("Sending file, " + filename + " to ServerRouter to playback the audio and return a success message");
                startWavFileSession(socket, client, serverIP, currentFile);
            } else if (filename.contains(".mp4")) {
                System.out.println("Sending file, " + filename + " to ServerRouter to playback the video and return a success message");
                startMp4FileSession(socket, client, serverIP, currentFile);
            }
            long totalTime = System.currentTimeMillis() - start;
            times.get(filename).add(totalTime);
            System.out.println("Total Transfer Time: " + totalTime + " milliseconds\n");
            socket.close();
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
        PrintWriter toServerRouter = null;
        BufferedReader fromServerRouter = null;
        try {
            toServerRouter = new PrintWriter(socket.getOutputStream(), true);
            fromServerRouter = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about router: " + socket.getLocalAddress().getHostAddress());
            System.exit(1);
        } catch(IOException exception) {
            System.err.println("Couldn't get I/O for the connection to: " + socket.getLocalAddress().getHostAddress());
            System.exit(1);
        }

        // Initial communication with the ServerRouter. This just confirms the connection.
        toServerRouter.println(server);
        String fromServer = fromServerRouter.readLine();
        System.out.println("Message from ServerRouter: " + fromServer);
        toServerRouter.println(client);

        return new Object[]{toServerRouter, fromServerRouter};
    }

    /**
     * Begins sending information to the ServerRouter through a PrintWriter. Accepts information from the ServerRouter
     * through a BufferedReader.
     *
     * @param socket the socket that connects the client to the ServerRouter
     * @param client the IP of the client (the device running this program)
     * @param server the IP of the server (the device converting the text to uppercase)
     * @param currentFile the file that is sent to the server line by line
     * @throws IOException if handshake fails between client and server.
     */
    public static void startTextFileSession(Socket socket, String client, String server, File currentFile) throws IOException {
        BufferedReader fromFile = new BufferedReader(new FileReader(currentFile));

        // Writers and readers for sending messages to and from the ServerRouter
        Object[] writerAndReader = handshake(socket, client, server);
        PrintWriter toServerRouter = (PrintWriter) writerAndReader[0];
        BufferedReader fromServerRouter = (BufferedReader) writerAndReader[1];
        String fromServer;

        // Send the contents of the text file to the ServerRouter line by line. Receive the response as well.
        while ((fromServer = fromServerRouter.readLine()) != null) {
            System.out.println("Server: " + fromServer);
            String fromClient = fromFile.readLine();
            if (fromClient == null) {
                toServerRouter.println("Bye.");
                break;
            }
            System.out.println("Client: " + fromClient);
            toServerRouter.println(fromClient);
        }
        toServerRouter.close();
        fromServerRouter.close();
    }

    /**
     * Begins sending information to the ServerRouter through a PrintWriter. Accepts information from the ServerRouter
     * through a BufferedReader.
     *
     * @param socket the socket that connects the client to the ServerRouter
     * @param client the IP of the client (the device running this program)
     * @param server the IP of the server (the device operating on the wav file)
     * @param currentFile the file that is sent to the server for operation
     * @throws IOException if handshake fails between client and server.
     */
    public static void startWavFileSession(Socket socket, String client, String server, File currentFile) throws IOException {
        handshake(socket, client, server);
        BufferedInputStream inputStream;
        try {
            if (socket.isConnected()) {
                inputStream = new BufferedInputStream(socket.getInputStream());
                AudioInputStream ais = AudioSystem.getAudioInputStream(inputStream);
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, currentFile);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                // IF YOU WANT TO PLAY SOUND DIRECTLY FROM SPEAKERS COMMENT OUT THE TRY CATCH BLOCK ABOVE
                //  AND UNCOMMENT THE BELOW SECTION
//                Clip clip = AudioSystem.getClip();
//                clip.open(ais);
//                clip.start();
//
//                while (inputStream != null) {
//                    if (clip.isActive()) {
//                        System.out.println("********** Buffred *********" + inputStream.available());
//                    }
//                }
            }

        } catch (UnsupportedAudioFileException e) {
            System.err.println(e);
        }
    }

    /**
     * Begins sending information to the ServerRouter through a PrintWriter. Accepts information from the ServerRouter
     * through a BufferedReader.
     *
     * @param socket the socket that connects the client to the ServerRouter
     * @param client the IP of the client (the device running this program)
     * @param server the IP of the server (the device operating on the mp4 file)
     * @param currentFile the file that is sent to the server for operation
     * @throws IOException if handshake fails between client and server.
     */
    public static void startMp4FileSession(Socket socket, String client, String server, File currentFile) throws IOException {
        handshake(socket, client, server);

    }
}