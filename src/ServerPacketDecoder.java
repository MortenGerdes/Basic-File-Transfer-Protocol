import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * This class handles the decoding of packets sent from the client to the server.
 * It has some default packet protocol, which it assumes as a pre-condition.
 */
public class ServerPacketDecoder implements PacketDecoder {
    ByteBuffer bb;

    public ServerPacketDecoder(ByteBuffer bb) {
        this.bb = bb;
    }

    public int getRandomNumber() {
        return bb.getInt(0);
    }

    public long getSizeOfData() {
        return bb.getLong(4);
    }

    public int getPacketID() {
        return bb.getInt(12);
    }

    public byte[] getData() {
        return Arrays.copyOfRange(bb.array(), 16, bb.array().length);
    }
}
