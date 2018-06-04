import java.net.InetAddress;
import java.util.Objects;

public class ClientIdentifier {
    private int clientRandomNumber;
    private InetAddress clientIP;
    private int clientPort;
    private boolean complete;

    public ClientIdentifier(int clientRandomNumber, InetAddress clientIP, int clientPort) {
        this.clientRandomNumber = clientRandomNumber;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
        this.complete = false;
    }

    public int getClientRandomNumber() {
        return clientRandomNumber;
    }

    public InetAddress getClientIP() {
        return clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdentifier that = (ClientIdentifier) o;
        return clientRandomNumber == that.clientRandomNumber &&
                clientPort == that.clientPort &&
                Objects.equals(clientIP, that.clientIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientRandomNumber, clientIP, clientPort);
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean isComplete){
        this.complete = isComplete;
    }
}
