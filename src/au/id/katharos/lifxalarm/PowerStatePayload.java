package au.id.katharos.lifxalarm;

/**
 * Payload for a power state packet, indicating if the light is currently
 * on or off.
 */
public class PowerStatePayload extends Payload {

	private byte[] data = new byte[2];
	
	public PowerStatePayload(boolean powerState) {
		type = Payload.Type.POWER_STATE;
		if (powerState) {
			data[0] = (byte) 0xFF;
			data[1] = (byte) 0xFF;
		}
	}
	
	public PowerStatePayload(byte[] data) {
		this.data = data;
	}

	public boolean isOn() {
		return data[1] == (byte) 0xFF;
	}

	@Override
	public byte[] getBytes() {
		return data;
	}

	@Override
	public int getlength() {
		return 2;
	}
}
