import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.HashMap;

public class Server implements Runnable
{
    private static final String TAG = "[SERVER] ";
    private boolean running = false;
    private int B = 200;
    private int W = 6;
    private byte[] buf = new byte[4 + 8 + 4 + B];
    private HashMap<ClientIdentifier, SlidingWindow> clients;
    private HashMap<ClientIdentifier, String> clientFiles;
    private DatagramSocket serverSocket;

    public Server(int port)
    {
        try
        {
            serverSocket = new DatagramSocket(port);
            clients = new HashMap<>();
            clientFiles = new HashMap<>();
        }
        catch(SocketException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        System.out.println(TAG + "Starting server...");
        running = true;
        while(running)
        {
            try
            {
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                System.out.println(TAG + "Listening for packets");
                serverSocket.receive(receivedPacket);
                handleReceivedPacket(receivedPacket);
            }
            catch(IOException e)
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
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void handleReceivedPacket(DatagramPacket packet) throws IOException
    {
        ServerPacketDecoder pd = new ServerPacketDecoder(ByteBuffer.wrap(packet.getData()));
        System.out.println(TAG + "Got Packet with ID " + (pd.getPacketID()));
        int clientRandomNumber = pd.getRandomNumber();
        int clientPort = packet.getPort();
        InetAddress clientIP = packet.getAddress();
        ClientIdentifier ci = new ClientIdentifier(clientRandomNumber, clientIP, clientPort);
        SlidingWindow sw;
        Path path;

        if(!clients.containsKey(ci))
        {
            clients.put(ci, new SlidingWindow((int) Math.ceil(pd.getSizeOfData() / B) + 1, W));
        }

        if(!clientFiles.containsKey(ci))
        {
            clientFiles.put(ci, new String("" + ci.getClientRandomNumber() + "-" + ci.getClientPort()));
        }

        sw = clients.get(ci);
        path = Paths.get(clientFiles.get(ci));

        //System.out.println("Got packetID " + pd.getPacketID() + " and limits are " + sw.wb + " - " + sw.we);
        if(!sw.isPacketIDWithinWindow(pd.getPacketID()))
        {
            return;
        }
        /*if (sw.isPacketAcknowledged(pd.getPacketID())) // Already got this packet
        {
            sendAckknowledgeToClient(pd, packet.getAddress(), packet.getPort());
            return;
        }*/

        sw.setPacketAcknowledged(packet, pd);
        if(pd.getPacketID() != sw.wb)// Latest packet we need to write, so we can push the window
        {
            return;
        }
        int pastI = pd.getPacketID() - 1;
        for(Integer i : sw.getPacketsInWindow().keySet())
        {
            ServerPacketDecoder localPD = new ServerPacketDecoder(ByteBuffer.wrap(sw.getPacketsInWindow().get(i).getData()));

            if(localPD.getPacketID() != pastI + 1)
            {
                continue;
            }
            pastI = localPD.getPacketID();

            OpenOption oo = StandardOpenOption.CREATE_NEW;
            if(Files.exists(path))
            {
                oo = StandardOpenOption.APPEND;
            }
            System.out.println(TAG + "Writing packet with ID = " + localPD.getPacketID());
            Files.write(path, localPD.getData(), oo);
            sw.incrementBoundaries();
        }
        sendAckknowledgeToClient(pd, packet.getAddress(), packet.getPort());

        /**
         * We have to clean up after ourselves. Else the list get's proportionally large
         * with the amount of packets sent.
         */
        sw.getPacketsInWindow().entrySet().removeIf(entry -> !sw.isPacketIDWithinWindow(entry.getKey()));
    }

    public void sendAckknowledgeToClient(PacketDecoder pd, InetAddress ip, int port) throws IOException
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putInt(pd.getRandomNumber());
        byteBuffer.putInt(pd.getPacketID());
        DatagramPacket ackPacket = new DatagramPacket(byteBuffer.array(), 8, ip, port);
        //System.out.println(TAG + "Sending ack to client");
        serverSocket.send(ackPacket);
    }
}
