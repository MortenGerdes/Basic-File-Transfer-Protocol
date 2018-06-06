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
    private int clientPort;
    private int serverPort;
    private int B;
    private int W;
    private int timeout = 50;
    private int sendChance;
    private File file;
    private String ip;
    private String filename;
    private Random random;
    private List<DatagramPacket> packetList;
    private DatagramSocket clientSocket;
    private Thread receivingThread;

    public Client(String ip, int serverPort, int B, int W, int sendChance, String filename)
    {
        this.random = new Random();
        this.R = random.nextInt(100);
        this.serverPort = serverPort;
        this.B = B;
        this.W = W;
        this.sendChance = sendChance;
        this.filename = filename;
        this.ip = ip;
    }

    public void sendFile() throws IOException
    {
        clientSocket = new DatagramSocket(clientPort);
        InetAddress IPAddress = InetAddress.getByName("localhost"); // Change this with ip to connect to someone else
        file = new File(filename);
        packetList = new ArrayList<>();
        ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

        int wb = 0; //Sliding window index begin.
        int we = W - 1; // Sliding window index end.
        int amountOfPackets = (int) Math.ceil(file.length() / B);
        slidingWindow = new boolean[amountOfPackets + 1];
        long[] timeStamps = new long[amountOfPackets + 1];

        System.out.println(TAG + "Size of File: " + file.length() + "bytes");

        receivingThread = new Thread(() -> {
            receiveAcknowledgements(slidingWindow);
        });
        receivingThread.start();

        for(int i = 0; i < we; i++)//First off, we create every packet in our SlidingWindow.
        {
            boolean SlidingWindowLargerThanPacketRequired = (i >= Math.ceil(file.length() / B));
            if(SlidingWindowLargerThanPacketRequired)
            {
                break;
            }
            createDatagramPacket(IPAddress, fileBuffer, i);
            if(new Random().nextInt(100) < sendChance)
            {
                clientSocket.send(packetList.get(i));
            }
        }

        while(!allPacketsReceived)
        {
            for(int i = wb; i < we; i++)
            {
                boolean timedOutAndNotAcknowledged = System.currentTimeMillis() - timeStamps[i] > timeout && !slidingWindow[i];
                boolean notTimedOutAndNotAcknowledged = !slidingWindow[i] && System.currentTimeMillis() - timeStamps[i] < timeout;
                boolean SlidingWindowLargerThanPacketRequired = (i >= Math.ceil(file.length() / B));

                if(SlidingWindowLargerThanPacketRequired)
                {
                    break;
                }

                if(notTimedOutAndNotAcknowledged)
                {
                    //System.out.println(TAG + "Packet #" + i + " not yet acknowledged!");
                    continue;
                }

                if(timedOutAndNotAcknowledged)
                {
                    //System.out.println(TAG + "Packet #" + i + " timed out");
                    timeStamps[i] = System.currentTimeMillis();
                    System.out.println(TAG + "Resending packet #" + i + " to server.");
                    if(new Random().nextInt(100) < sendChance)
                    {
                        clientSocket.send(packetList.get(i));
                    }
                }
            }

            if(slidingWindow[wb])
            {
                wb++;
                we++;

                if(we > amountOfPackets + 1)
                {
                    we = amountOfPackets + 1;
                }
                createDatagramPacket(IPAddress, fileBuffer, we - 1);

                if(new Random().nextInt(100) < sendChance)
                {
                    clientSocket.send(packetList.get(packetList.size() - 1));
                }
            }

            allPacketsReceived = true;
            for(boolean b : slidingWindow)
            {
                if(!b)
                {
                    allPacketsReceived = false;
                }
            }
            if(allPacketsReceived)
            {
                System.out.println(TAG + "All packets sent and acknowledged!");
            }
        }
        receivingThread.interrupt();
        clientSocket.close();
    }

    private void createDatagramPacket(InetAddress IPAddress, ByteBuffer fileBuffer, int packetID)
    {
        byte[] sendData = new byte[2048 + B];
        int from = (packetID * B);
        int to = (((packetID + 1) * B));
        //int size = (int) (4 + 8 + 4 + Math.ceil(file.length() / B));

        ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
        packetBuffer.putInt(R);
        packetBuffer.putLong(file.length());
        packetBuffer.putInt(packetID);

        if(packetID == Math.ceil(file.length() / B))
        {
            //System.out.println(TAG + "Sends final packet #" + packetID + " from " + from + " to " + fileBuffer.array().length);
            long tempSize = (long) (Math.ceil(file.length() / B) * B);
            long uBytes = tempSize - file.length();

            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, from + (16 + Math.toIntExact(B - uBytes))));
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
            packetList.add(new DatagramPacket(packetBuffer.array(), packetBuffer.array().length, IPAddress, serverPort));
        } else
        {
            //System.out.println(TAG + "Created packet #" + packetID + " from " + from + " to " + to);
            System.out.println("Copying packet: " + packetID + "/" + Math.ceil(file.length() / B));
            packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
            packetList.add(new DatagramPacket(packetBuffer.array(), packetBuffer.array().length, IPAddress, serverPort));
        }
    }

    private void receiveAcknowledgements(boolean[] slidingWindow)
    {
        while(true)// This might break on Mac???
        {
            try
            {
                byte[] ackData = new byte[8];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                clientSocket.receive(ackPacket);
                //System.out.println(TAG + "Received some packet");
                ClientPacketDecoder pd = new ClientPacketDecoder(ByteBuffer.wrap(ackPacket.getData()));
                if(pd.getRandomNumber() == R)
                {
                    //System.out.println(TAG + "Received ack packet " + pd.getPacketID() + " from server.");
                    slidingWindow[pd.getPacketID()] = true;
                }
            }
            catch(SocketException e)
            {
                if(e.getMessage().toLowerCase().contentEquals("socket closed"))
                {
                    break;
                }
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
