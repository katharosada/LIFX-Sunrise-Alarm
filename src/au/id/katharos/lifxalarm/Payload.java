package au.id.katharos.lifxalarm;

/**
 * Payload to attach to an {@link LIFXPacket}
 * 
 * Types of Payloads:
 * 	power state (on/off) (set and get same)
 * 	Bulb label (char[]) (set and get same)
 * 	Tags (unit64) (set and get same)
 * 	Tag labels (uint64, char[]) (set and get same)
 * 	SET light color (multi) 
 * 	SET dim 
 * 	Light status (response) 	
 */
public abstract class Payload {
	
	public static final Payload EMPTY = new Payload() {	
		@Override
		public int getlength() {
			return 0;
		}
		
		@Override
		public byte[] getBytes() {
			return new byte[0];
		}
	};
	
	public enum Type {
		NONE,
		POWER_STATE,
		SET_LIGHT_COLOR,
		SET_LIGHT_DIM,
		LIGHT_STATE;
	}
	protected Type type = Type.NONE;

	public Type getType() {
		return type;
	}
	
	public abstract byte[] getBytes();
	public abstract int getlength();

	// Construct a Payload object from an incoming packet.
	public static Payload construct(Type payloadType, byte[] payloadBytes) {
		switch (payloadType) {
			case NONE:
				return EMPTY;
			case POWER_STATE:
				return new PowerStatePayload(payloadBytes);
			case LIGHT_STATE:
				return EMPTY;
			case SET_LIGHT_COLOR:
				return EMPTY;
			case SET_LIGHT_DIM:
				return EMPTY;
			default:
				break;
		}
		return EMPTY;
	}
	
	public static short swap (short value)
	{
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;
		return (short) (b1 << 8 | b2 << 0);
	}
}
