/**
 * Created by jackzet on 19/05/2018.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Server implements Runnable {

    private boolean running = false;
    private int B = 4;
    private int nextPacketID;
    private byte[] buf = new byte[4 + 8 + 4 + B];
    private ArrayList<Client> clients;
    private DatagramSocket serverSocket;
    private static final String TAG = "[SERVER] ";

    public Server(int port) {
        try {
            serverSocket = new DatagramSocket(port);
            clients = new ArrayList<>();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(TAG + "Starting server...");
        running = true;
        nextPacketID = 0;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("test");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int W = 6;

        int wb = 0;
        int we = W - 1;

        while (running) {
            try {
                PacketDecoder pd;
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                System.out.println(TAG + "Listening for packets");
                serverSocket.receive(receivedPacket);
                pd = new PacketDecoder(ByteBuffer.wrap(receivedPacket.getData()));
                System.out.println(TAG + "Got Packet with ID " + (pd.getPacketID()));

                if (pd.getPacketID() >= wb && pd.getPacketID() <= we) {
                    System.out.println(TAG + "Received text: " + new String(pd.getData()));
                    if (pd.getPacketID() == Math.ceil(pd.getSizeOfData() / B)) { //Assuming no packet loss. This will not work with packet loss.
                        System.out.println(TAG + "went here 2");
                        //fos.flush();
                        System.out.println(TAG + pd.getData().length);
                        fos.write(pd.getData());
                        running = false;
                    } else {
                        System.out.println(TAG + "went here 1");
                        fos.write(pd.getData());
                    }

                    ByteBuffer byteBuffer = ByteBuffer.allocate(8).putInt(pd.getRandomNumber());
                    byteBuffer.putInt(pd.getPacketID());
                    DatagramPacket ackpacket = new DatagramPacket(byteBuffer.array(), 8, receivedPacket
                            .getAddress(), receivedPacket.getPort());
                    serverSocket.send(ackpacket);

                    if (pd.getPacketID() == wb) {
                        wb++;
                        we++;
                    }
                }

                /*if (pd.getPacketID() == nextPacketID) {
                    System.out.println(TAG + "went here 1");
                    nextPacketID++;
                    fos.write(pd.getData());
                }

                if (pd.getPacketID() == Math.ceil(pd.getSizeOfData() / B)) //Assuming no packet loss. This will not work with packet loss.
                {
                    System.out.println(TAG + "went here 2");
                    fos.flush();
                    running = false;
                }

                // Create a packet, we send back to the client to confirm the server received it.
                // The packet will contain the packetID of the received packet.

                DatagramPacket ackpacket = new DatagramPacket(ByteBuffer.allocate(4).putInt(pd.getPacketID()).array(), 4, receivedPacket
                        .getAddress(), receivedPacket.getPort());
                serverSocket.send(ackpacket);*/

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stop();
    }

    public void stop() {
        try {
            running = false;
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
