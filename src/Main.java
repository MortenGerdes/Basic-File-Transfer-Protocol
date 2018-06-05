import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException, InterruptedException {


        if (args.length <= 0) {
            System.out.println("You have to pass arguments to the program");
            System.out.println("1. arg: c or s");
        }

        if (args[0].equals("c")) {
            int serverPort = Integer.parseInt(args[1]);
            int B = Integer.parseInt(args[2]);
            int W = Integer.parseInt(args[3]);
            int sendChance = Integer.parseInt(args[4]);
            String filename = args[5];

            new Thread(() -> {
                //Client client1 = new Client(clientPort, serverPort, B, W, filename);
                Client client1 = new Client(serverPort, B, W, sendChance, filename);
                try {
                    client1.sendFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();
        }

        if (args[0].equals("s")) {
            int port = Integer.parseInt(args[1]);
            int B = Integer.parseInt(args[2]);
            int W = Integer.parseInt(args[3]);

            /*Server server = new Server(port, B, W);*/
            Server server = new Server(port, B, W);
            Thread t = new Thread(server);
            t.start();
        }

    }

}