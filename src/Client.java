import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by jackzet on 19/05/2018.
 */
public class Client {

    int R;
    int ip;
    int port;
    int B = 4;
    File file;
    Random random;

    public Client(int ip, int port){
        random = new Random();
        R = random.nextInt(100);
        ip = this.ip;
        port = this.port;
    }

    public void connect() throws SocketException, UnknownHostException {
        BufferedReader inFromUser =
                new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        byte[] sendData = new byte[1024];
        file = new File("hello");

        ByteBuffer bb = ByteBuffer.wrap(sendData);
        bb.putInt(R);
        bb.putInt((int) file.length());
        bb.putInt((int) file.length() / B);
    }




}
