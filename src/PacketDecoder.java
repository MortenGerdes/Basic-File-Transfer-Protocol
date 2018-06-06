/**
 * Interface to make our methods, handling decoding, more flexible.
 */
public interface PacketDecoder {
    int getRandomNumber();

    int getPacketID();
}
