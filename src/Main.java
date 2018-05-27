import java.io.IOException;

public class Main
{

	public static void main(String[] args) throws IOException, InterruptedException
	{
		Server server = new Server(1338);
		Thread t = new Thread(server);
		t.start();

		Thread.sleep(2000);

		Client client = new Client(1337, 1338);
		client.connect();
		t.join();
	}
}
