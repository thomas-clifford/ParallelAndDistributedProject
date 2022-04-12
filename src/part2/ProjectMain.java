package part2;

import java.io.IOException;

public class ProjectMain {
    private static String destinationIP = "10.0.0.8";
    public static void main(String[] args) throws IOException {
        new Peer(Common.serverRouter1IP, Common.serverRouter1Port, destinationIP);
    }
}
