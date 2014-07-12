package au.id.katharos.lifxalarm;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;


/**
 * An LIFX packet to send.
 * Thanks to the protocol spec here: https://github.com/magicmonkey/lifxjs/blob/master/Protocol.md
 * 
 * Rough structure of a packet: 
 * uint16 size;              // Little Endian
 * uint16 protocol;
 * uint32 reserved1;         // Always 0x0000
 * byte   target_mac_address[6];
 * 
 * uint16 reserved2;         // Always 0x00
 * byte   site[6];           // MAC address of gateway PAN controller bulb
 * uint16 reserved3;         // Always 0x00
 * uint64 timestamp;
 * uint16 packet_type;       // Little Endian
 * uint16 reserved4;         // Always 0x0000
 * 
 * varies payload;           // Documented separately per packet type.
 */
public class LIFXPacket {

	public enum Type {
		GET_PAN_GATEWAY((byte) 0x02),
		PAN_GATEWAY((byte) 0x03),
		
		GET_POWER_STATE((byte) 0x14),
		SET_POWER_STATE((byte) 0x15, Payload.Type.POWER_STATE),
		POWER_STATE((byte) 0x16, Payload.Type.POWER_STATE),
		
		SET_LIGHT_COLOR((byte) 0x66, Payload.Type.SET_LIGHT_COLOR);
		
		
		private final byte code;
		private final Payload.Type payloadType;
		
		private static final Map<Byte, Type> lookup = new HashMap<Byte, LIFXPacket.Type>();
		static {
			for (Type t : Type.values()) {
				lookup.put(t.code, t);
			}
		}
		public static Type get(byte b) {
			return lookup.get(b);
		}
		
		private Type(byte code) {
			this.code = code;
			this.payloadType = Payload.Type.NONE;
		}
		
		private Type(byte code, Payload.Type payloadType) {
			this.code = code;
			this.payloadType = payloadType;
		}
	}
	
	private Type type;
	private int protocol;
	private byte[] targetGlobeMac;
	private byte[] gatewayGlobeMac;
	
	private Payload payload;

	
	private LIFXPacket(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public Payload getPayload() {
		return payload;
	}

	/**
	 * Construct from a received packet. 
	 */
	public LIFXPacket(byte[] bytes) {
		int len = bytes[0] & 0xFF;
		ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, len);
		
		// Skip the length
		buffer.position(2);
		
		// protocol
		buffer.getShort();
		
		// Skip 4 (reserved 1)
		buffer.position(buffer.position() + 4);
		
		targetGlobeMac = new byte[6];
		buffer.get(targetGlobeMac);
		
		// Skip 2 (reserved 2)
		buffer.position(buffer.position() + 2);
	
		gatewayGlobeMac = new byte[6];
		buffer.get(gatewayGlobeMac);
		
		// Skip 2 (reserved 3)
		buffer.position(buffer.position() + 2);
		
		// Skip 8 (timestamp)
		buffer.position(buffer.position() + 8);
		
		// Packet type
		byte t = buffer.get();
		type = Type.get(t);
		buffer.get();
		
		// Skip 2 (reserved 4)
		buffer.position(buffer.position() + 2);
		
		// payload is whatever is left
		byte[] payloadBytes = new byte[buffer.remaining()];
		buffer.get(payloadBytes);
		payload = Payload.construct(type.payloadType, payloadBytes);

		if (buffer.remaining() != 0) {
			Log.wtf("LIFXPacket builder", "Something's wrong with the bytes...");
		}
	}
	
	public int getLength() {
		return 36 + payload.getlength();
	}
	
	public byte[] getBytes() {
		// 2400 0034 00000000 000000000000 0000 000000000000 0000 4b15125300000000 0200 0000 (36 bytes)
		ByteBuffer buffer = ByteBuffer.allocate(36 + payload.getlength());
		// LE uint16 size (this works as long as size is never >255 bytes)
		buffer.put((byte) (36 + payload.getlength()));
		buffer.put((byte) 0);
		
		// unit16 protocol // wtf is this anyway?
		buffer.put((byte) 0);
		buffer.put((byte) 0x34);
		
		// skip 4 bytes for reserved1
		buffer.position(buffer.position() + 4);
		
		// byte[6] target mac address
		buffer.put(targetGlobeMac);
		
		// reserved 2
		buffer.position(buffer.position() + 2);
		
		// byte[6] gateway mac address
		buffer.put(gatewayGlobeMac);
		
		// reserved 3
		buffer.position(buffer.position() + 2);
		
		// timestamp (who cares? just leave it blank)
		buffer.position(buffer.position() + 8);
		
		// LE packet type 
		buffer.put(type.code);
		buffer.put((byte) 0);
		
		// reserved 4
		buffer.position(buffer.position() + 2);

		// payload
		buffer.put(payload.getBytes());
		
		if (buffer.remaining() != 0) {
			Log.wtf("LIFXPacket builder", "Something's wrong with the bytes...");
		}
		
		Log.i("LIFXPacket builder", "Packet: " + bytesToHex(buffer.array()));
		return buffer.array();
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static String byteToHex(byte b) {
		int v = b & 0xFF;
		char[] hexChars = new char[2];
		hexChars[0] = hexArray[v >>> 4];
		hexChars[1] = hexArray[v & 0x0F];
		
		return String.valueOf(hexChars);
	}
	
	private String formatMac(byte[] mac) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			builder.append(byteToHex(mac[i]));
			if (i < mac.length -1 ) {
				builder.append(':');
			}
		}
		return builder.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Type: " + type + "\n");
		builder.append("Protocol: " + protocol + "\n");
		builder.append("Target Globe: " + formatMac(targetGlobeMac) + "\n");
		builder.append("Gateway Globe: " + formatMac(targetGlobeMac) + "\n");
		builder.append("Payload: " + bytesToHex(payload.getBytes()));
		
		return builder.toString();
	}
	
	public static class Builder {
		
		LIFXPacket packet;
		
		public Builder(Type type) {
			packet = new LIFXPacket(type);
			packet.payload = Payload.EMPTY;
			packet.gatewayGlobeMac = new byte[6];
			packet.targetGlobeMac = new byte[6];
		}
		
		public Builder setGlobeMac(byte[] mac) {
			packet.gatewayGlobeMac = mac;
			packet.targetGlobeMac = mac;
			return this;
		}
		
		public Builder setPayload(Payload payload) {
			packet.payload = payload;
			return this;
		}
		
		public LIFXPacket build() {
			return packet;
		}
	}

	public byte[] getGatewayMacAddress() {
		return gatewayGlobeMac;
	}
}
