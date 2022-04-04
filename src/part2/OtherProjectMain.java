package part2;

import java.io.IOException;

public class OtherProjectMain {
    private static String destinationIP = "10.0.0.116";
    public static void main(String[] args) throws IOException {
        new Peer(Common.serverRouter2IP, Common.serverRouter2Port, destinationIP);
    }
}