import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("Duplicates")
public class FixedClient {

    private static final String TAG = "[CLIENT] ";

    boolean allPacketsReceived = false;
    private volatile boolean[] slidingWindow; // Securing thread safety
    private int R;
    private int serverPort;
    private int B;
    private int W;
    private int timeout = 50;
    private int sendChance = 100;
    private String filename;
    private File file;
    private Random random;
    private List<DatagramPacket> packetList;
    private DatagramSocket clientSocket;
    private Thread receivingThread;

    public FixedClient(int serverPort, int B, int W, String filename) {
        this.random = new Random();
        this.R = random.nextInt(100);
        this.serverPort = serverPort;
        this.B = B;
        this.W = W;
        this.filename = filename;
    }

    public void sendFile() throws IOException {
        clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        file = new File(filename);
        packetList = new ArrayList<>();
        ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

        int wb = 0; //Sliding window index begin.
        int we = W - 1; // Sliding window index end.
        System.out.println("file length: " + file.length() + "; fileBuffer: " + fileBuffer.array().length);
        int amountOfPackets = (int) Math.ceil(file.length() / B) + 1;
        System.out.println("amountofpackets: " + amountOfPackets);
        slidingWindow = new boolean[amountOfPackets];
        long[] timeStamps = new long[amountOfPackets];

        System.out.println(TAG + "Size of File: " + file.length() + "bytes");

        receivingThread = new Thread(() -> {
            receiveAcknowledgements(slidingWindow);
        });
        receivingThread.start();

        for (int i = 0; i <= we; i++)//First off, we create every packet in our SlidingWindow.
        {
            createDatagramPacket(IPAddress, fileBuffer, i,amountOfPackets);
        }

        while (!allPacketsReceived) {
            for (int i = wb; i <= Math.min(we, amountOfPackets - 1); i++)
            {
                if (timeStamps[i] != 0) {
                    //System.out.println("i: " + i + "; timestamp length: " + timeStamps.length + "; slidingwindow length: " + slidingWindow.length);
                    if (slidingWindow[i]){
                        //System.out.println(TAG + "Packet #" + i + " is acknowledged!");
                        continue;
                    }

                    boolean timedOutAndNotAcknowledged = System.currentTimeMillis() - timeStamps[i] > timeout;
                    boolean notTimedOutAndNotAcknowledged = System.currentTimeMillis() - timeStamps[i] < timeout;
                    if (notTimedOutAndNotAcknowledged)
                    {
                        //System.out.println(TAG + "Packet #" + i + " not yet acknowledged!");
                        continue;
                    }
                    if (timedOutAndNotAcknowledged) {
                        System.out.println(TAG + "Packet #" + i + " timed out");
                        //System.out.println(TAG + "Resending packet #" + i + " to server.");
                    }
                }

                timeStamps[i] = System.currentTimeMillis();
                clientSocket.send(packetList.get(i));
                System.out.println(TAG + "Sending packet #" + i + " to server.");
            }

            if (slidingWindow[wb]) {
                wb++;
                we++;

                if (we < amountOfPackets) {
                    createDatagramPacket(IPAddress, fileBuffer, we, amountOfPackets);
                }
            }

            allPacketsReceived = true;
            for (boolean b : slidingWindow) {
                if (!b) {
                    allPacketsReceived = false;
                    break;
                }
            }
            if (allPacketsReceived) {
                System.out.println(TAG + "All packets sent and acknowledged!");
            }
        }
        receivingThread.interrupt();
        clientSocket.close();
    }

    private void receiveAcknowledgements(boolean[] slidingWindow) {
        while (true)// This might break on Mac???
        {
            byte[] ackData = new byte[8];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            try {
                clientSocket.receive(ackPacket);
                //System.out.println(TAG + "Received some packet");
                ClientPacketDecoder pd = new ClientPacketDecoder(ByteBuffer.wrap(ackPacket.getData()));
                if (pd.getRandomNumber() == R) {
                    System.out.println(TAG + "Received ack packet " + pd.getPacketID() + " from server.");
                    slidingWindow[pd.getPacketID()] = true;
                }
            } catch (SocketException e) {
                if (allPacketsReceived) {
                    break;
                }
                //System.err.println("Socket closed: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDatagramPacket(InetAddress IPAddress, ByteBuffer fileBuffer, int packetID, int amountOfPackets) {
        int from = (packetID * B);
        int to = from + B;

        ByteBuffer packetBuffer = ByteBuffer.allocate(4 + 8 + 4 + B)
                .putInt(R)
                .putLong(file.length())
                .putInt(packetID);

        if (packetID == amountOfPackets - 1) {
            long tempSize = (long) ((Math.ceil(file.length() / B) + 1) * B);
            long uBytes = tempSize - file.length();

            System.out.println(TAG + "Sends final packet #" + packetID + " from " + from + " to " + (from + Math.toIntExact(B - uBytes)));

            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, from + Math.toIntExact(B - uBytes)));
            packetList.add(new DatagramPacket(packetBuffer.array(), packetBuffer.array().length, IPAddress, serverPort));
            System.out.println(new String(Arrays.copyOfRange(fileBuffer.array(), from, from + Math.toIntExact(B - uBytes))));
        } else {
            System.out.println(TAG + "Created packet #" + packetID + " from " + from + " to " + to + "; fileSize: " + fileBuffer.array().length);
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
            packetList.add(new DatagramPacket(packetBuffer.array(), packetBuffer.array().length, IPAddress, serverPort));
        }
    }
}
