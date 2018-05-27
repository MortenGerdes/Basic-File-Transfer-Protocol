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
	private int R;
	private int ip;
	private int clientPort;
    private int serverPort;
	private int B = 4;
	private File file;
	private Random random;

	public Client(int clientPort, int serverPort)
	{
		this.random = new Random();
		this.R = random.nextInt(100);
		this.ip = this.ip;
		this.clientPort = clientPort;
		this.serverPort = serverPort;
	}

	public void connect() throws IOException
	{
		DatagramSocket clientSocket = new DatagramSocket(clientPort);
		InetAddress IPAddress = InetAddress.getByName("localhost");

		file = new File("hello");
		ByteBuffer fileBuffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));

		System.out.println("Size of File: " + file.length() + "bytes");
		for(int i = 0; i < Math.ceil(file.length()/B)+1; i++)
		{
			byte[] sendData = new byte[(int) (4 + 8 + 4 + Math.ceil(file.length()/B))];
			int from = (int) (i*B);
			int to = (int) (((i+1)*B));
			int size = (int) (4 + 8 + 4 + Math.ceil(file.length()/B));

			ByteBuffer packetBuffer = ByteBuffer.wrap(sendData);
			packetBuffer.putInt(R);
			packetBuffer.putLong(file.length());
			packetBuffer.putInt(i);

			if(i == Math.ceil(file.length()/B))
			{
				System.out.println("Sends final packet from " + from + " to " + fileBuffer.array().length);
				packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, fileBuffer.array().length));
				clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
			}
			else
			{
				packetBuffer.put(Arrays.copyOfRange(fileBuffer.array(), from, to));
				System.out.println("Sends packet from " + from + " to " + to);
				clientSocket.send(new DatagramPacket(packetBuffer.array(), size, IPAddress, serverPort));
			}
		}
		clientSocket.close();
	}
}
