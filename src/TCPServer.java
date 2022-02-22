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
        PrintWriter toServerRouter = null;
        BufferedReader fromServerRouter = null;

        // For every file sent to the server, connect to ServerRouter, accept message, return message, and close connection.
        for (int i = 0; i < 1; i++) {
            // Connect to the ServerRouter through a Socket. Also establish a writer and reader between the ServerRouter.
            try {
                socket = new Socket(serverRouterIP, port);
                toServerRouter = new PrintWriter(socket.getOutputStream(), true);
                fromServerRouter = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (UnknownHostException e) {
                System.err.println("Don't know about router: " + serverRouterIP);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to: " + serverRouterIP);
                System.exit(1);
            }

            // Initial communication with the ServerRouter. This just confirms the connection.
            toServerRouter.println(clientIP);
            String fromClient = fromServerRouter.readLine();
            System.out.println("ServerRouter: " + fromClient);

            // Begin accepting the file's data from the ServerRouter. Then, perform the correct service based on the file type.
            while ((fromClient = fromServerRouter.readLine()) != null) {
                System.out.println("Client said: " + fromClient);
                if (fromClient.equals("Bye."))
                    break;

                String fromServer = fromClient.toUpperCase();
                System.out.println("Server said: " + fromServer);
                toServerRouter.println(fromServer);
            }

            // Close the connection to the ServerRouter and its PrintWriter and BufferedReader.
            toServerRouter.close();
            fromServerRouter.close();
            socket.close();
        }
    }
}
