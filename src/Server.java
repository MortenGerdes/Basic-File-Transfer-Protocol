import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class Server implements Runnable
{

    private boolean running = false;
    private int B = 4;
    private int W = 6;
    private int nextPacketID;
    private byte[] buf = new byte[4 + 8 + 4 + B];
    private HashMap<ClientIdentifier, SlidingWindow> clients;
    private DatagramSocket serverSocket;
    private static final String TAG = "[SERVER] ";
    File file = new File("output");
    FileWriter fw = new FileWriter(file);
    BufferedWriter bw = new BufferedWriter(fw);

    public Server(int port) throws IOException
    {
        try
        {
            serverSocket = new DatagramSocket(port);
            clients = new HashMap<>();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        System.out.println(TAG + "Starting server...");
        running = true;
        while (running)
        {
            try
            {
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                System.out.println(TAG + "Listening for packets");
                serverSocket.receive(receivedPacket);
                System.out.println(TAG + "Got packet");
                handleReceivedPacket(receivedPacket);

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

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        stop();
    }

    public void stop()
    {
        try
        {
            running = false;
            serverSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public  void handleReceivedPacket(DatagramPacket packet) throws IOException
    {
        ServerPacketDecoder pd = new ServerPacketDecoder(ByteBuffer.wrap(packet.getData()));
        System.out.println(TAG + "Got Packet with ID " + (pd.getPacketID()));
        int clientRandomNumber = pd.getRandomNumber();
        int clientPort = packet.getPort();
        InetAddress clientIP = packet.getAddress();
        ClientIdentifier ci = new ClientIdentifier(clientRandomNumber, clientIP, clientPort);
        SlidingWindow sw;

        if(!clients.containsKey(ci))
        {
            System.out.println("Created SW with datasize = " + Math.ceil(pd.getSizeOfData() / B)+1);
            clients.put(ci, new SlidingWindow((int) Math.ceil(pd.getSizeOfData() / B)+1, W));
        }

        sw = clients.get(ci);
        System.out.println("Got packetID " + pd.getPacketID() + " and limits are " + sw.wb + " - " + sw.we);
        if(sw.isPacketIDWithinWindow(pd.getPacketID()))
        {
            if(sw.isPacketAcknowledged(pd.getPacketID())) // Already got this packet
            {
                sendAckknowledgeToClient(pd, packet.getAddress(), packet.getPort());
                return;
            }

            sw.setPacketAcknowledged(packet, pd);
            if(pd.getPacketID() == sw.wb)// Latest packet we need to write, so we can push the window
            {
                //File file = new File("" + clientRandomNumber + clientIP + clientPort);
                int pastI = pd.getPacketID()-1;
                for(Integer i: sw.getPacketsInWindow().keySet())
                {
                    if(i != pastI+1)
                    {
                        continue;
                    }
                    ServerPacketDecoder localPD = new ServerPacketDecoder(ByteBuffer.wrap(sw.getPacketsInWindow().get(i).getData()));
                    pastI = i;
                    bw.write(new String(localPD.getData()));
                    sw.incrementBoundaries();
                }
                bw.flush();
            }
            sendAckknowledgeToClient(pd, packet.getAddress(), packet.getPort());
        }
    }

    public void sendAckknowledgeToClient(PacketDecoder pd, InetAddress ip, int port) throws IOException
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putInt(pd.getRandomNumber());
        byteBuffer.putInt(pd.getPacketID());
        DatagramPacket ackPacket = new DatagramPacket(byteBuffer.array(), 8, ip, port);
        System.out.println(TAG + "Sending ack to client");
        serverSocket.send(ackPacket);
    }
}
