import com.sun.tools.jdi.Packet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by jackzet on 19/05/2018.
 */
public class Client {
    private int R;
    private int ip;
    private int clientPort;
    private int serverPort;
    private int B = 4;
    private int W = 6;
    private int timeout = 50;
    private File file;
    private Random random;

    private static final String TAG = "[CLIENT] "; 


    public Client(int clientPort, int serverPort) {
        this.random = new Random();
        this.R = random.nextInt(100);
        this.ip = this.ip;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        DatagramSocket clientSocket = new DatagramSocket(clientPort);
        InetAddress IPAddress = InetAddress.getByName("localhost");

        file = new File("hello");
        ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
        boolean[] slidingWindow = new boolean[(int) Math.ceil(file.length() / B) + 1];
        long[] timeStamps = new long[(int) Math.ceil(file.length() / B) + 1];
        boolean allPacketsReceived = false;
        int wb = 0;
        int we = W - 1;

        for (int i = 0; i < W; i++) {
            slidingWindow[i] = false;
        }

        System.out.println(TAG + "Size of File: " + file.length() + "bytes");

        List<DatagramPacket> packetList = new ArrayList<>();
        for (int i = 0; i < we; i++) {
            createDatagramPacket(IPAddress, fileBuffer, packetList, i);
        }

        while (!allPacketsReceived) {
            for (int i = wb; i < we; i++) {
                System.out.println(TAG + "Sending packet #" + i + " to server.");
                //clientSocket.send(packetList.get(i));
                if (System.currentTimeMillis() - timeStamps[i] > 50 && !slidingWindow[i]) {
                    System.out.println(TAG + "Packet #" + i + " timed out");
                    timeStamps[i] = System.currentTimeMillis();
                    clientSocket.send(packetList.get(i));
                } else if (!slidingWindow[i] && System.currentTimeMillis() - timeStamps[i] < 50) {
                    System.out.println(TAG + "Packet #" + i + " not yet acknowledged!");
                }
            }

            byte[] ackData = new byte[8];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            clientSocket.receive(ackPacket);
            ClientPacketDecoder pd = new ClientPacketDecoder(ByteBuffer.wrap(ackPacket.getData()));
            if (pd.getRandomNumber() == R) {
                System.out.println(TAG + "Received packet #" + pd.getPacketID() + " from server.");
                slidingWindow[pd.getPacketID()] = true;
            }

            if (slidingWindow[wb]) {
                wb++;
                we++;

                if (we > 7) {
                    we = 7;
                }
                createDatagramPacket(IPAddress, fileBuffer, packetList, we - 1);
            }

            allPacketsReceived = true;
            for (boolean b : slidingWindow) {
                if (!b) {
                    allPacketsReceived = false;
                }
            }
            if (allPacketsReceived)
                System.out.println(TAG + "All packets sent and acknowledged!");
        }

        /*for (int i = 0; i < Math.ceil(file.length() / B) + 1; i++) {
            byte[] sendData = new byte[(int) (4 + 8 + 4 + Math.ceil(file.length() / B))];
            byte[] ackData = new byte[4];
            int from = (int) (i * B);
            int to = (int) (((i + 1) * B));
            int size = (int) (4 + 8 + 4 + Math.ceil(file.length() / B));

            ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
            packetBuffer.putInt(R);
            packetBuffer.putLong(file.length());
            packetBuffer.putInt(i);

            if (i == Math.ceil(file.length() / B)) {
                System.out.println(TAG + "Sends final packet from " + from + " to " + fileBuffer.array().length);
                packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, fileBuffer.array().length));
                clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));


                System.currentTimeMillis();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                PacketDecoder pd = new PacketDecoder(ByteBuffer.wrap(ackPacket.getData()));
                clientSocket.receive(ackPacket);

            } else {
                if (slidingWindow[wb] == true) {
                    wb++;
                    we++;
                }

                if (i < we + 1) {
                    packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
                    System.out.println(TAG + "Sends packet from " + from + " to " + to);
                    clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
                    timeStamps[i] = System.currentTimeMillis();

                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);

                    clientSocket.receive(ackPacket);
                    slidingWindow[i] = true;

                    if (System.currentTimeMillis() - timeStamps[i] > 50 && slidingWindow[i] == false) {
                        clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
                    }


                } else {

                }
            }


        }*/
        clientSocket.close();
    }

    private void createDatagramPacket(InetAddress IPAddress, ByteBuffer fileBuffer, List<DatagramPacket> packetList, int i) {
        byte[] sendData = new byte[(int) (4 + 8 + 4 + Math.ceil(file.length() / B))];
        int from = (int) (i * B);
        int to = (int) (((i + 1) * B));
        int size = (int) (4 + 8 + 4 + Math.ceil(file.length() / B));

        ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
        packetBuffer.putInt(R);
        packetBuffer.putLong(file.length());
        packetBuffer.putInt(i);

        if (i == Math.ceil(file.length() / B)) {
            System.out.println(TAG + "Sends final packet #" + i + " from " + from + " to " + fileBuffer.array().length);
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, fileBuffer.array().length));
            packetList.add(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
        } else {
            System.out.println(TAG + "Created packet #" + i + " from " + from + " to " + to);
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
            packetList.add(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
        }
    }


}
