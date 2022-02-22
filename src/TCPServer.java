import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.*;

public class TCPServer {
    public static void main(String[] args) throws IOException {
        // Basic socket information for TCP connection
        Socket socket = null;
        String serverRouterIP = "10.0.0.116";
        String clientIP = "10.0.0.116";
        int port = 5555;

        // Writers and readers for sending messages to and from the ServerRouter
        DataOutputStream toServerRouter = null;
        DataInputStream fromServerRouter = null;

        // For every file sent to the server, connect to ServerRouter, accept message, return message, and close connection.
        for (int i = 0; i < 3; i++) {
            // Connect to the ServerRouter through a Socket. Also establish a writer and reader between the ServerRouter.
            try {
                socket = new Socket(serverRouterIP, port);
                toServerRouter = new DataOutputStream(socket.getOutputStream());
                fromServerRouter = new DataInputStream(socket.getInputStream());
            } catch (UnknownHostException e) {
                System.err.println("Don't know about router: " + serverRouterIP);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to: " + serverRouterIP);
                System.exit(1);
            }

            // Initial communication with the ServerRouter. This just confirms the connection.
            byte[] clientName = clientIP.getBytes();
            toServerRouter.writeInt(clientName.length);
            toServerRouter.write(clientName);

            int clientResponseLength = fromServerRouter.readInt();
            byte[] clientResponse = new byte[clientResponseLength];
            fromServerRouter.readFully(clientResponse, 0, clientResponseLength);
            String fromClient = new String(clientResponse);
            System.out.println("ServerRouter: " + fromClient);

            byte[] clientMessage = null;
            boolean isTextFile = false;

            String fileName = "";
            for (int j = 0; j < 3; j++) {
                int clientMessageLength = fromServerRouter.readInt();

                clientMessage = new byte[clientMessageLength];
                fromServerRouter.readFully(clientMessage, 0, clientMessage.length);
                fromClient = new String(clientMessage);

                if (j == 1 && fromClient.substring(fromClient.lastIndexOf('.')).equals(".txt")) {
                    isTextFile = true;
                }
                if (j == 1) {
                    fileName = fromClient;
                    System.out.println("Receiving the following file from client: " + fromClient);
                }
                if (j == 0 || j > 1 && isTextFile) {
                    System.out.println("Client said: " + fromClient);
                    byte[] serverResponse = fromClient.toUpperCase().getBytes();
                    System.out.println("Server said: " + new String(serverResponse));
                    toServerRouter.writeInt(serverResponse.length);
                    toServerRouter.write(serverResponse);
                }
            }
            if (!isTextFile) {
                String successMessage = "Successfully downloaded the file.";
                byte[] serverResponse = successMessage.getBytes();
                System.out.println("Server said: " + successMessage);
                toServerRouter.writeInt(serverResponse.length);
                toServerRouter.write(serverResponse);
            }

            File downloadFile = new File(fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(downloadFile);
            fileOutputStream.write(clientMessage);
            fileOutputStream.close();

            // Close the connection to the ServerRouter and its PrintWriter and BufferedReader.
            toServerRouter.close();
            fromServerRouter.close();
            socket.close();
        }
    }
}
