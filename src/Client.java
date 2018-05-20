import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by jackzet on 19/05/2018.
 */
public class Client
{
	int R;
	int ip;
	int port;
	int B = 4;
	File file;
	Random random;

	public Client(int port)
	{
		random = new Random();
		R = random.nextInt(100);
		ip = this.ip;
		port = this.port;
	}

	public void connect() throws IOException
	{
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");

		byte[] sendData = new byte[4+8+4+26];
		file = new File("hello");
		ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

		System.out.println("Size of File: " + file.length() + "bytes");
		for(int i = 0; i < Math.ceil(file.length()/B)+1; i++)
		{
			int from = (int) (i*B);
			int to = (int) ((i+1)*B)-1;
			int size = (int) (4 + 8 + 4 + Math.ceil(file.length()/B));

			ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
			packetBuffer.putInt(R);
			packetBuffer.putInt((int) file.length());
			packetBuffer.putInt((int) file.length() / B);

			if(i == Math.ceil(file.length()/B))
			{
				System.out.println("Sends final packet from " + from + " to " + fileBuffer.array().length);
				packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, fileBuffer.array().length));
				clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, port));
			}
			else
			{
				packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
				System.out.println("Sends packet from " + from + " to " + to);
				clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, port));
			}
		}
	}
}
