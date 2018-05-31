import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        Server server = new Server(1338);
        Thread t = new Thread(server);
        t.start();

        Thread.sleep(2000);

        new Thread(() -> {
            Client client1 = new Client(1337, 1338);
            try
            {
                client1.sendFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            Client client2 = new Client(1339, 1338);
            try
            {
                client2.sendFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }).start();

        t.join();
    }
}
