public class SlidingWindow
{
    int W;
    int wb = 0;
    int we = W - 1;

    boolean[] data;
    public SlidingWindow(int dataSize, int windowSize)
    {
        this.data = new boolean[dataSize];
        this.W = windowSize;
    }

    public void incrementBoundaries()
    {
        wb = wb+1;
        we = we+1;
    }

    public void setPacketAcknowledged(int index, boolean flag)
    {
        data[index] = flag;
    }

    public boolean isPacketAcknowledged(int index)
    {
        return data[index];
    }

    public boolean[] getAllData()
    {
        return data;
    }
}
