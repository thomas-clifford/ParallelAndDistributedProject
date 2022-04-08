package part2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Common {
    public static String serverRouter1IP = "10.0.0.94";
    public static String serverRouter2IP = "10.0.0.162";
    public static int serverRouter1Port = 5555;
    public static int serverRouter2Port = 5556;

    public static byte[] getData(DataInputStream dis) {
        int dataLength = 0;
        try {
            dataLength = dis.readInt();
            byte[] data = new byte[dataLength];
            dis.readFully(data, 0, dataLength);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static void sendData(DataOutputStream dos, byte[] data) {
        try {
            dos.writeInt(data.length);
            dos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
