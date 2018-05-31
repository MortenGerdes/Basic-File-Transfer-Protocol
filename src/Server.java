import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public Server(int port)
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
        int W = 6;
        int wb = 0;
        int we = W - 1;
        System.out.println(TAG + "Starting server...");
        running = true;
        nextPacketID = 0;
        FileOutputStream fos = null;

        try
        {
            fos = new FileOutputStream("test");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        while (running)
        {
            try
            {
                ServerPacketDecoder pd;
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                System.out.println(TAG + "Listening for packets");
                serverSocket.receive(receivedPacket);
                pd = new ServerPacketDecoder(ByteBuffer.wrap(receivedPacket.getData()));
                System.out.println(TAG + "Got Packet with ID " + (pd.getPacketID()));

                if (pd.getPacketID() >= wb && pd.getPacketID() <= we)
                {
                    System.out.println(TAG + "Received text: " + new String(pd.getData()));
                    if (pd.getPacketID() == Math.ceil(pd.getSizeOfData() / B))
                    { //Assuming no packet loss. This will not work with packet loss.
                        //fos.flush();
                        System.out.println(TAG + pd.getData().length);
                        fos.write(pd.getData());
                        running = false;
                    } else
                    {
                        fos.write(pd.getData());
                    }


                    if (pd.getPacketID() == wb)
                    {
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
        int clientRandomNumber = pd.getRandomNumber();
        int clientPort = packet.getPort();
        InetAddress clientIP = packet.getAddress();
        ClientIdentifier ci = new ClientIdentifier(clientRandomNumber, clientIP, clientPort);
        SlidingWindow sw;

        if(!clients.containsKey(ci))
        {
            clients.put(ci, new SlidingWindow((int) Math.ceil(pd.getData().length / B), W));
        }

        sw = clients.get(ci);
        if(sw.isPacketIDWithinWindow(pd.getPacketID()))
        {
            if(sw.isPacketAcknowledged(pd.getPacketID())) // Already got this packet
            {
                return;
            }

            sw.setPacketAcknowledged(packet, pd);
            if(pd.getPacketID() == sw.wb)// Latest packet we need to write, so we can push the window
            {
                File file = new File("" + clientRandomNumber + clientIP + clientPort);
                FileOutputStream fos = new FileOutputStream(file);

                int pastI = pd.getPacketID()-1;
                for(Integer i: sw.getPacketsInWindow().keySet())
                {
                    if(i != pastI+1){ break; }
                    fos.write(sw.getPacketsInWindow().get(i).getData());
                    sw.incrementBoundaries(); // TODO This will not work! We cannot edit this list while traversing it. Fix!
                }
            }

            //Send Acknowledge packet back to the client.
            ByteBuffer byteBuffer = ByteBuffer.allocate(8).putInt(pd.getRandomNumber());
            byteBuffer.putInt(pd.getPacketID());
            DatagramPacket ackPacket = new DatagramPacket(byteBuffer.array(), 8, packet
                    .getAddress(), packet.getPort());
            serverSocket.send(ackPacket);
        }
    }
}
