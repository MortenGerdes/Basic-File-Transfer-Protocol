import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("Duplicates")
public class FixedServer implements Runnable {

    private static final String TAG = "[SERVER] ";
    private boolean running = false;
    private int B;
    private int W;
    private int nextPacketID;
    private byte[] buf;
    private HashMap<ClientIdentifier, SlidingWindow> clients;
    private HashMap<ClientIdentifier, String> clientFiles;
    private DatagramSocket serverSocket;

    public FixedServer(int port, int B, int W) throws IOException {
        try {
            serverSocket = new DatagramSocket(port);
            clients = new HashMap<>();
            clientFiles = new HashMap<>();
            this.B = B;
            this.W = W;
            this.buf = new byte[4 + 8 + 4 + this.B];
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(TAG + "Starting server...");
        running = true;
        while (running) {
            try {
                System.out.println(TAG + "Listening for packets");

                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                serverSocket.receive(receivedPacket);

                ServerPacketDecoder pd = new ServerPacketDecoder(ByteBuffer.wrap(receivedPacket.getData()));

                int clientRandomNumber = pd.getRandomNumber();
                int amountOfPackets = ((Double) Math.ceil(pd.getSizeOfData() / (double) (B))).intValue();
                System.out.println(TAG + "Data: " + new String(receivedPacket.getData().length + ":" + buf.length));

                int clientPort = receivedPacket.getPort();
                InetAddress clientIP = receivedPacket.getAddress();

                ClientIdentifier ci = new ClientIdentifier(clientRandomNumber, clientIP, clientPort);

                System.out.println(TAG + "Got Packet with ID " + (pd.getPacketID()) + " from client " + clientRandomNumber);

                if (!clients.containsKey(ci)) {
                    clients.put(ci, new SlidingWindow(amountOfPackets, W));
                    System.out.println("Datasize: " + pd.getSizeOfData() + "; " + ((Double) Math.ceil(pd.getSizeOfData() / (double) (B))).intValue());
                }

                if (!clientFiles.containsKey(ci)) {
                    clientFiles.put(ci, new String("" + ci.getClientRandomNumber() + "-" + ci.getClientPort() + ".jpg"));
                }

                SlidingWindow sw = clients.get(ci);
                Path path = Paths.get(clientFiles.get(ci));

                if (!sw.isPacketIDWithinWindow(pd.getPacketID())) {
                    System.err.println(TAG + "Packet #" + pd.getPacketID() + " not within");
                    if (sw.isPacketAcknowledged(pd.getPacketID())) {
                        byte[] toClientBytes = ByteBuffer.allocate(8)
                                .putInt(pd.getRandomNumber())
                                .putInt(pd.getPacketID())
                                .array();

                        DatagramPacket ackPacket = new DatagramPacket(toClientBytes, toClientBytes.length, clientIP, clientPort);
                        System.out.println(TAG + "Sending ack for packet #" + pd.getPacketID() + " to client");
                        serverSocket.send(ackPacket);
                    } else {
                        System.out.println(TAG + "packet #" + pd.getPacketID() + " not acknowledged!");
                    }
                    continue;
                }

                sw.setPacketAcknowledged(receivedPacket, pd);

                byte[] toClientBytes = ByteBuffer.allocate(8)
                        .putInt(pd.getRandomNumber())
                        .putInt(pd.getPacketID())
                        .array();

                DatagramPacket ackPacket = new DatagramPacket(toClientBytes, toClientBytes.length, clientIP, clientPort);
                System.out.println(TAG + "Sending ack to client");
                serverSocket.send(ackPacket);

                if (pd.getPacketID() == sw.wb)// Latest packet we need to write, so we can push the window
                {
                    int pastI = pd.getPacketID() - 1;
                    for (Integer i : sw.getPacketsInWindow().keySet()) {
                        ByteBuffer bb = ByteBuffer.wrap(sw.getPacketsInWindow().get(i).getData());
                        int R = bb.getInt(0);
                        long fileSize = bb.getLong(4);
                        int packetID = bb.getInt(12);
                        if (packetID != pastI + 1) {
                            continue;
                        }
                        pastI = packetID;

                        byte[] data = Arrays.copyOfRange(bb.array(), 16, bb.array().length);

                        if (packetID == amountOfPackets - 1) {
                            long tempSize = (long) ((Math.ceil(fileSize / B) + 1) * B);
                            long uBytes = tempSize - fileSize;

                            data = Arrays.copyOfRange(data, 0, Math.toIntExact(B - uBytes));
                        }

                        System.out.println("Packet size: " + bb.array().length + ", randID: " + R + ", fileSize: " + fileSize + ", packetIndex: " + packetID);

                        OpenOption oo = StandardOpenOption.CREATE_NEW;
                        if (Files.exists(path)) {
                            oo = StandardOpenOption.APPEND;
                        }
                        System.out.println(TAG + "Writing packet with ID = " + packetID);
                        Files.write(path, data, oo);

                        //System.out.println("Incrementing sliding windows for #" + ci.getClientRandomNumber());
                        //sw.incrementBoundaries();

                        //sw.getPacketsInWindow().remove(i); // TODO Remember to clean up after you.
                    }
                }

                if (sw.isPacketAcknowledged(pd.getPacketID())) {
                    System.out.println(TAG + "Incrementing sliding windows for client #" + clientRandomNumber);
                    sw.incrementBoundaries();
                }

                if (!ci.isComplete()) {
                    if (sw.areAllReceived()) {
                        ci.setComplete(true);
                        System.out.println(TAG + "All packets have been received from client #" + ci.getClientRandomNumber());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocket.close();
    }
}
