package au.id.katharos.lifxalarm;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import au.id.katharos.lifxalarm.LIFXPacket.Type;

/**
 * Timer to control the Sunrise sequence of colours and brightness.
 */
public class SunriseCounter extends CountDownTimer {
	
	private static final int TOTAL_SUNRISE_TIME_MS = 1200 * 1000; // 20 min total sequence time.
	private static final int UPDATE_INTERVAL_MS = 5000; // update very 5 sec 
	
	private byte[] mac;
	private LIFXConnectionKeeper connKeeper;
	private Handler updateNotify;
	private int steps;
	private int currentStep;

	/**
	 * Sends a packet to set the color to the give values.
	 */
	private class ColorAction {
		
		final LIFXPacket packet;
		
		public ColorAction(int hue, int sat, int brightness, int time) {
			// Build LIFX Packet ready to send
			SetLightColorPayload payload = new SetLightColorPayload(hue, sat, brightness, time);
    		
    		packet = new LIFXPacket.Builder(Type.SET_LIGHT_COLOR)
				.setGlobeMac(mac)
				.setPayload(payload)
				.build();
		}
		
		public void execute() {
			new SendPacket().execute(packet);
		}
	}
	
	// Invariant: stops and offsets must be the same length  
	List<ColorAction> stops = new LinkedList<ColorAction>();
	List<Integer> offsets = new LinkedList<Integer>();
	
	public SunriseCounter(LIFXConnectionKeeper connKeeper, Handler updateNotify) {
		super(TOTAL_SUNRISE_TIME_MS, UPDATE_INTERVAL_MS);
		this.connKeeper = connKeeper;
		this.updateNotify = updateNotify;
		
		try {
			// Set up the connection and get the light into the right starting state.
			new SunriseSequenceTask().execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Construct light sequence
		//
		// Dawn light follows a sigmoid function:
		// brightness(t) = 1 / (1 + e^-t)
		// (where the middle is t=0)
		// For us, t = [x,0] and brightness = [0,100] so
		// brightness(t) = 100 / (1 + e^(f(t-(x/2))
		// where f is some ratio based on the total time (for now 10/x)
		
		// Start dim red
		double F = 5 / (TOTAL_SUNRISE_TIME_MS / 2.0); // want the t to be roughly -5 -> 5 
		
		for (int t = TOTAL_SUNRISE_TIME_MS; t > 0; t -= UPDATE_INTERVAL_MS) {
			int bright = (int) (100 / (1 + Math.exp(F * (t - TOTAL_SUNRISE_TIME_MS / 2))));
			// Hue is a function of brightness, such that dim = red, bright = yellow
			int hue = Math.min(60, bright * 120 / 100);
			// Saturation is also a function of brightness, such that dim = full, bright = low
			int sat = Math.max(100 - bright, 20);
			Log.i("SunriseCounter", "Setting brightness: " + bright + ", hue: " + hue);
			ColorAction stop = new ColorAction(hue, sat, bright, 200000);
			offsets.add(t); 
			stops.add(stop); 
		}
		steps = stops.size();
		currentStep = 0;
	}

	@Override
	public void onFinish() {
		Log.i("SunriseCounter", "Finished - the Sun is up!");
		Log.i("SunriseCounter", "There were " + offsets.size() + " offsets left.");
		Log.i("SunriseCounter", "There were " + stops.size() + " stops left.");
	}

	@Override
	public void onTick(long millisUntilFinished) {
		Log.i("Sunrise", "TICK! Time is : " + millisUntilFinished + " offset is: " + offsets.get(0));
		while (!offsets.isEmpty() && millisUntilFinished < offsets.get(0)) {
			Log.i("Sunrise", "Setting brightness to: " + ((SetLightColorPayload) stops.get(0).packet.getPayload()).getBrightness());
			
			offsets.remove(0);
			ColorAction action = stops.remove(0);
			action.execute();
			
			currentStep += 1;
			int progress = 1000 * currentStep / steps;
			updateNotify.dispatchMessage(Message.obtain(updateNotify, 0, progress, 0));
		}
	}
	
	private class SendPacket extends AsyncTask<LIFXPacket, Void, Void> {

		@Override
		protected Void doInBackground(LIFXPacket... params) {
			connKeeper.sendPacket(params[0], false);
			return null;
		}	
	}

	private class SunriseSequenceTask extends AsyncTask<Void, Void, LIFXPacket> {

		@Override
		protected LIFXPacket doInBackground(Void... params) {

			Log.i("Background Thread", "Starting Rainbow Dance!!");
			if (connKeeper == null) {
				connKeeper = LIFXConnectionKeeper.getInstance();
			}
			if (!connKeeper.isConnected()) {
				connKeeper.findGateway();
				if (!connKeeper.isConnected()) {
					throw new RuntimeException("Can't connect to the light! Everyone panic!");
				}
			}
    		// Use the gateway globe, since I only have one.
    		// In future, we'll need a better way of selecting the globe in the UI.
			mac = connKeeper.getGatewayMac();
			
			// Starting color (red with 0 brightness)
			SetLightColorPayload setColorPayload = new SetLightColorPayload(0, 80, 0, 2);
			
			// Make the light dim. (doesn't actually work if the light is off.)
			LIFXPacket setColor = new LIFXPacket.Builder(Type.SET_LIGHT_COLOR)
				.setGlobeMac(mac)
				.setPayload(setColorPayload)
				.build();
			connKeeper.sendPacket(setColor, false);
			
			// Turn on the light
    		PowerStatePayload turnOnPayload = new PowerStatePayload(true);
    		LIFXPacket turnOn = new LIFXPacket.Builder(Type.SET_POWER_STATE)
    			.setGlobeMac(mac)
    			.setPayload(turnOnPayload)
    			.build();
    		connKeeper.sendPacket(turnOn, true);
			
			// Make it dim again straight away just in case it didn't before.
    		connKeeper.sendPacket(setColor, false);

			return null;
		}
	}
}
