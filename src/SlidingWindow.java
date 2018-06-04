import java.net.DatagramPacket;
import java.util.HashMap;

public class SlidingWindow {
    int W;
    int wb;
    int we;

    private boolean[] data;
    private HashMap<Integer, DatagramPacket> packetsInWindow;

    public SlidingWindow(int dataSize, int windowSize) {
        this.data = new boolean[dataSize];
        this.W = windowSize;
        this.packetsInWindow = new HashMap<>();
        this.wb = 0;
        this.we = windowSize - this.wb;
    }

    public void incrementBoundaries() {
        wb = wb + 1;
        we = we + 1;

        //packetsInWindow.remove(0); // We don't need this packet anymore
    }

    public void incrementBoundaries(int i) {
        for (int j = 0; j < i; j++) {
            incrementBoundaries();
        }
    }

    public void setPacketAcknowledged(int index) {
        data[index] = true;
    }

    public void setPacketAcknowledged(DatagramPacket packet, PacketDecoder pd) {
        data[pd.getPacketID()] = true;

        if (!packetsInWindow.containsKey(pd.getPacketID())) {
            packetsInWindow.put(pd.getPacketID(), packet);
        }
    }

    public boolean isPacketAcknowledged(int index) {
        return data[index];
    }

    public boolean isPacketIDWithinWindow(int packetID) {
        if (wb <= packetID && we > packetID) {
            return true;
        }
        return false;
    }

    public boolean[] getReceivedFlags() {
        return data;
    }

    public HashMap<Integer, DatagramPacket> getPacketsInWindow() {
        return packetsInWindow;
    }

    public boolean areAllReceived(){
        boolean tmp = true;
        for (boolean datum : data) {
            if (!datum) {
                tmp = false;
                break;
            }
        }
        return tmp;
    }
}
