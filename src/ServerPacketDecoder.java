import java.nio.ByteBuffer;
import java.util.Arrays;

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
