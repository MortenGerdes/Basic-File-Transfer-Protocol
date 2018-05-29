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

public class Client
{
    private static final String TAG = "[CLIENT] ";

    boolean allPacketsReceived = false;
    private volatile boolean[] slidingWindow; // Securing thread safety
    private int R;
    private int ip;
    private int clientPort;
    private int serverPort;
    private int B = 4;
    private int W = 6;
    private int timeout = 50;
    private File file;
    private Random random;
    private List<DatagramPacket> packetList;
    private DatagramSocket clientSocket;
    private Thread receivingThread;

    public Client(int clientPort, int serverPort)
    {
        this.random = new Random();
        this.R = random.nextInt(100);
        this.ip = this.ip;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    public void sendFile() throws IOException
    {
        clientSocket = new DatagramSocket(clientPort);
        InetAddress IPAddress = InetAddress.getByName("localhost");
        file = new File("hello");
        packetList = new ArrayList<>();
        ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

        int wb = 0; //Sliding window index begin.
        int we = W - 1; // Sliding window index end.
        int amountOfPackets = (int) Math.ceil(file.length() / B);
        slidingWindow = new boolean[amountOfPackets + 1];
        long[] timeStamps = new long[amountOfPackets + 1];

        System.out.println(TAG + "Size of File: " + file.length() + "bytes");

        for (int i = 0; i < we; i++)//First off, we creates every packet in our SlidingWindow.
        {
            createDatagramPacket(IPAddress, fileBuffer, i);
        }

        receivingThread = new Thread(() -> {
            receiveAcknowledgements(slidingWindow);
        });
        receivingThread.start();

        while (!allPacketsReceived)
        {
            for (int i = wb; i < we; i++)
            {
                boolean timedOutAndNotAcknowledged = System.currentTimeMillis() - timeStamps[i] > timeout && !slidingWindow[i];
                boolean notTimedOutAndNotAcknowledged = !slidingWindow[i] && System.currentTimeMillis() - timeStamps[i] < timeout;

                System.out.println(TAG + "Sending packet #" + i + " to server.");
                if (notTimedOutAndNotAcknowledged)
                {
                    System.out.println(TAG + "Packet #" + i + " not yet acknowledged!");
                    continue;
                }

                if (timedOutAndNotAcknowledged)
                {
                    System.out.println(TAG + "Packet #" + i + " timed out");
                    timeStamps[i] = System.currentTimeMillis();
                    clientSocket.send(packetList.get(i));
                }
            }

            if (slidingWindow[wb])
            {
                wb++;
                we++;

                if (we > amountOfPackets+1)
                {
                    we = amountOfPackets+1;
                }
                createDatagramPacket(IPAddress, fileBuffer, we - 1);
            }

            allPacketsReceived = true;
            for (boolean b : slidingWindow)
            {
                if (!b)
                {
                    allPacketsReceived = false;
                }
            }
            if (allPacketsReceived)
                System.out.println(TAG + "All packets sent and acknowledged!");
        }
        receivingThread.interrupt();
        clientSocket.close();
    }

    private void createDatagramPacket(InetAddress IPAddress, ByteBuffer fileBuffer, int packetID)
    {
        byte[] sendData = new byte[(int) (4 + 8 + 4 + Math.ceil(file.length() / B))];
        int from = (packetID * B);
        int to = (((packetID + 1) * B));
        int size = (int) (4 + 8 + 4 + Math.ceil(file.length() / B));

        ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
        packetBuffer.putInt(R);
        packetBuffer.putLong(file.length());
        packetBuffer.putInt(packetID);

        if (packetID == Math.ceil(file.length() / B))
        {
            System.out.println(TAG + "Sends final packet #" + packetID + " from " + from + " to " + fileBuffer.array().length);
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, fileBuffer.array().length));
            packetList.add(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
        } else
        {
            System.out.println(TAG + "Created packet #" + packetID + " from " + from + " to " + to);
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
            packetList.add(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
        }
    }

    private void receiveAcknowledgements(boolean[] slidingWindow)
    {
        while(!allPacketsReceived)
        {
            try
            {
                byte[] ackData = new byte[8];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                clientSocket.receive(ackPacket);
                ClientPacketDecoder pd = new ClientPacketDecoder(ByteBuffer.wrap(ackPacket.getData()));
                if (pd.getRandomNumber() == R)
                {
                    System.out.println(TAG + "Received packet #" + pd.getPacketID() + " from server.");
                    slidingWindow[pd.getPacketID()] = true;
                }
            }
            catch (SocketException e)
            {
                if(e.getMessage().toLowerCase().contentEquals("socket closed"))
                {
                    break;
                }
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
