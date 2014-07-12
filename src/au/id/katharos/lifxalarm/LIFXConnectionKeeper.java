package au.id.katharos.lifxalarm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * Singleton class to manage the connection to the LIFX globes.
 * 
 * Note: I only have one globe so this code assume there is only one. In future
 * I should support multiple globes and show the connection state for each in the UI.
 */
public class LIFXConnectionKeeper {
	
	private static final int PORT = 56700;
	private static final int[] FIND_GATEWAY_RETRY_WAITS_MS = {200, 1000, 5000, 10000, 0};
	
	private static final int WAIT_FOR_RESPONSE_RETRIES = 3;
	private static final int WAIT_FOR_RESPONSE_RETRY_MS = 200;
	
	// Host name (ip address) of the gateway globe.
	private String hostname;
	private byte[] gatewayGlobeMacAddress;
	private boolean connected;
	
	private static LIFXConnectionKeeper INSTANCE = new LIFXConnectionKeeper();
	
	public static LIFXConnectionKeeper getInstance() {
		return INSTANCE;
	}
	
	private LIFXConnectionKeeper() {
		// Prevent construction;
	}
	
	/**
	 * Send a broadcast packet to find the mac address of the Gateway globe.
	 *
	 * The Gateway globe is the one you talk to to find and send commands to
	 * all the other globes.
	 */
	public void findGateway() {
		// Retry 3 times with backoff before giving up and showing a message.
		try {
			for (int retryMS : FIND_GATEWAY_RETRY_WAITS_MS) {
				if (findGatewayAttempt()) {
					break;
				}
				if (retryMS > 2000) {
					// TODO: Toast "Cannot find LIFX globe. Retrying in x seconds..."
				}
				Thread.sleep(retryMS);
			}
			if (!connected) {
				// Show failed connection in the UI
				// Toast goes here.
				// TODO: Toast "Failed to find LIFX globe."
			} else {
				// TODO: Toast "Connected!"
			}
		} catch (InterruptedException e) {
			// Should never happen... nothing is interrupting this thread and it's only for a second.
			e.printStackTrace();
		}
	}
	
	private boolean findGatewayAttempt() {
		Log.i("LIFXConnectionKeeper", "Searching for LIFX Globe");
		try {
			LIFXPacket packet = new LIFXPacket.Builder(LIFXPacket.Type.GET_PAN_GATEWAY).build();
    		
    		DatagramPacket initBroadcast = new DatagramPacket(
    				packet.getBytes(), packet.getLength(),
    				InetAddress.getByName("255.255.255.255"), 56700);
    		
			DatagramSocket udpSock = new DatagramSocket(PORT);
			udpSock.setBroadcast(true);
			udpSock.setReuseAddress(true);
			Log.i("Find Gateway", "Sending UDP Broadcast");
			udpSock.send(initBroadcast);

			// Three fast retries on the listening.
			udpSock.setSoTimeout(WAIT_FOR_RESPONSE_RETRY_MS);
			for (int i = 0; i < WAIT_FOR_RESPONSE_RETRIES; i++) {
				DatagramPacket reply = new DatagramPacket(new byte[256], 256);
				Log.i("Find Gateway", "Waiting to receive...");

				try {
					udpSock.receive(reply);
				} catch (SocketTimeoutException e) {
					Log.i("Find Gateway", "Timed out waiting to recieve.");
					continue;
				}

				LIFXPacket response = new LIFXPacket(reply.getData());

				if(response.getType() == LIFXPacket.Type.PAN_GATEWAY) {
					Log.i("Find Gateway", "Found Gateway at: " + reply.getAddress().getHostAddress());
					hostname = reply.getAddress().getHostAddress();
					gatewayGlobeMacAddress = response.getGatewayMacAddress();
					connected = true;
					break;
				} else {
					Log.i("Find Gateway", "This isn't the packet you're looking for.");
				}
			}

			udpSock.close();
			if (connected) {
				return true;
			}
		} catch (SocketException e) {
			Log.e("LIFX Alarm", "Unable to open UDP socket.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			Log.e("LIFX Alarm", "Cannot find LIFX device");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("LIFX Alarm", "Something went wrong...");
			e.printStackTrace();
		}
		return false;
	}

	public boolean isConnected() {
		return connected;
	}

	public byte[] getGatewayMac() {
		return gatewayGlobeMacAddress;
	}

	public LIFXPacket sendPacket(LIFXPacket packet, boolean expectResponse) {
		Socket sock = null;
		try {
			sock = new Socket(hostname, PORT);

			// Send packet
			sock.getOutputStream().write(packet.getBytes());
			
			if (!expectResponse) {
				return null;
			}
			
			// wait for response
			byte[] readBuffer = new byte[255];
			sock.getInputStream().read(readBuffer);
			
			LIFXPacket response = new LIFXPacket(readBuffer);
			Log.i("LIFX Alarm", response.toString());
			
			return response;
		} catch (UnknownHostException e) {
			connected = false;
			e.printStackTrace();
		} catch (IOException e) {
			connected = false;
			e.printStackTrace();
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
