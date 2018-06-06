import java.nio.ByteBuffer;

/**
 * This class will handling the decoding of the acknowledgement packets sent from the server
 * to the client.
 */
public class ClientPacketDecoder implements PacketDecoder {
    ByteBuffer bb;

    public ClientPacketDecoder(ByteBuffer bb) {
        this.bb = bb;
    }

    public int getRandomNumber() {
        return bb.getInt(0);
    }

    public int getPacketID() {
        return bb.getInt(4);
    }
}
