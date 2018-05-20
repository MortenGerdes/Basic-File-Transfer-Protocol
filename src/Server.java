/**
 * Created by jackzet on 19/05/2018.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Server implements Runnable
{

	private boolean running = false;
	private int B = 4;
	private int nextPacketID;
	private byte[] buf = new byte[4+8+4+B];
	private ArrayList<Client> clients;
	private DatagramSocket serverSocket;

	public Server(int port)
	{
		try
		{
			serverSocket = new DatagramSocket(port);
			clients = new ArrayList<>();
		}
		catch(SocketException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		running = true;
		nextPacketID = 0;
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream("test.txt");
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		while(running)
		{
			try
			{
				PacketDecoder pd;
				DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
				serverSocket.receive(receivedPacket);
				pd = new PacketDecoder(ByteBuffer.wrap(receivedPacket.getData()));

				if(pd.getPacketID() == nextPacketID)
				{
					fos.write(pd.getData());
					//fos.flush(); // Is this needed??
				}

				if(pd.getPacketID() == Math.ceil(pd.getSizeOfData()/B)) //Assuming no packet loss. This will not work with packet loss.
				{
					running = false;
				}

				// Create a packet, we send back to the client to confirm the server received it.
				// The packet will contain the packetID of the received packet.
				DatagramPacket ackpacket = new DatagramPacket(ByteBuffer.allocate(4).putInt(pd.getPacketID()).array(), 4, receivedPacket
						.getAddress(), receivedPacket.getPort());
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void stop()
	{
		try
		{
			serverSocket.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}
}
