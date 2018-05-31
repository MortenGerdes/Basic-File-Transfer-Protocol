import java.net.InetAddress;

public class ClientIdentifier
{
    private int clientRandomNumber;
    private InetAddress clientIP;
    private int clientPort;

    public ClientIdentifier(int clientRandomNumber, InetAddress clientIP, int clientPort)
    {
        this.clientRandomNumber = clientRandomNumber;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
    }

    public int getClientRandomNumber()
    {
        return clientRandomNumber;
    }

    public InetAddress getClientIP()
    {
        return clientIP;
    }

    public int getClientPort()
    {
        return clientPort;
    }
}
