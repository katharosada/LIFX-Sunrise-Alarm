package au.id.katharos.lifxalarm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Payload for a Set light color packet.
 *
 * Format:
 * byte stream;        // Unknown, potential "streaming" mode toggle? Set to 0x00 for now.
 * uint16 hue;         // LE NOTE: Wraps around at 0xff 0xff back to 0x00 0x00
 *                     // which is a primary red colour.
 * uint16 saturation;  // LE
 * uint16 brightness;  // LE
 * uint16 kelvin;      // LE i.e. colour temperature (whites wheel in apps)
 * uint32 fade_time;   // LE Length of fade action, I've not worked out what the time unit is.
 */
public class SetLightColorPayload extends Payload {

	private static final int LENGTH = 13;
	private static final int MAX_SHORT = (1 << 16) - 1;
	
	private int hue;
	private int saturation;
	private int brightness;

	private int fade_time;

	/**
	 * Construct a packet payload to set the light color/brightness to the given values.
	 * 
	 * @param hue A number in the range 0-360 indicating the hue.
	 * @param saturation 0 to 100
	 * @param brightness 0 to 100
	 * @param time A 4-byte int, units unknown.
	 */
	public SetLightColorPayload(int hue, int saturation, int brightness, int time) {
		type = Payload.Type.SET_LIGHT_COLOR;
		this.hue = hue;
		this.saturation = saturation;
		this.brightness = brightness;
		this.fade_time = time; // units??
	}
		
	@Override
	public byte[] getBytes() {
		
		ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		// stream
		buffer.put((byte) 0);
		
		// colour, etc.
		buffer.putShort((short) (hue / 360.0 * MAX_SHORT));
		buffer.putShort((short) (saturation / 100.0 * MAX_SHORT));
		buffer.putShort((short) (brightness / 100.0 * MAX_SHORT));
		buffer.putShort((short) 0);
		
		// fade time (4 bytes)
		buffer.putInt(fade_time);

		return buffer.array();
	}

	@Override
	public int getlength() {
		return LENGTH;
	}
	
	int getBrightness() {
		return brightness;
	}

}
