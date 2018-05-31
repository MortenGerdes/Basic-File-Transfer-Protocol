import java.nio.ByteBuffer;
import java.util.Arrays;

public class ClientPacketDecoder implements PacketDecoder
{
	ByteBuffer bb;

	public ClientPacketDecoder(ByteBuffer bb)
	{
		this.bb = bb;
	}

	public int getRandomNumber()
	{
		return bb.getInt(0);
	}

	public int getPacketID()
	{
		return bb.getInt(4);
	}
}
