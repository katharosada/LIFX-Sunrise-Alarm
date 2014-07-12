package au.id.katharos.lifxalarm;

import java.util.Calendar;

import android.app.TimePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import au.id.katharos.lifxalarm.LIFXPacket.Type;

/**
 * This is the initial screen where you can set the alarm time and any other
 * settings.
 */
public class MainActivity extends FragmentActivity {

	private LIFXConnectionKeeper connKeeper;
	private boolean powerState;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void showTimePickerDialog(View view) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        TimePickerDialog timePicker = new CustomTimePickerDialog(this, hour, minute,
        		DateFormat.is24HourFormat(this));
        timePicker.show();
    }
    
    public void togglePower(View view) {
    	Log.v("Main Activity", "The button was clicked");
    	findViewById(R.id.power_toggle).setEnabled(false);
    	
    	new SetPowerTask().execute(!powerState);
    }
    
    public void setColor(View view) {
    	int hue = 0;
    	int sat = 100;
    	int brightness = 100;
    	if (view.getId() == R.id.red_button) {
    		hue = 0;
    	} else if (view.getId() == R.id.green_button) {
    		hue = 120;
    	} else if (view.getId() == R.id.blue_button) {
    		hue = 240;
    	} else if (view.getId() == R.id.white_button) {
    		hue = 30;
    		sat = 10;
    		brightness = 90;
    	}
    	SetLightColorPayload payload = new SetLightColorPayload(hue, sat, brightness, 2);
    	Log.i("Send Payload", payload.toString());
    	
    	new SetColorTask().execute(payload);
    }
    
    private class SetColorTask extends AsyncTask<SetLightColorPayload, Void, LIFXPacket> {

		@Override
		protected LIFXPacket doInBackground(SetLightColorPayload... payload) {
			if (connKeeper == null || !connKeeper.isConnected()) {
				connKeeper = LIFXConnectionKeeper.getInstance();
				connKeeper.findGateway();
			}
			// Don't bother sending if the connection didn't succeed (it was already retried).
			if (connKeeper.isConnected()) {
	    		// Use the gateway globe, since I only have one.
	    		// In future, we'll need a better way of selecting the globe in the UI.
				byte[] mac = connKeeper.getGatewayMac();

				LIFXPacket setColor = new LIFXPacket.Builder(Type.SET_LIGHT_COLOR)
				.setGlobeMac(mac)
				.setPayload(payload[0])
				.build();

				return connKeeper.sendPacket(setColor, false);
			}
			return null;
		}
    }
    
    private class SetPowerTask extends AsyncTask<Boolean, Void, LIFXPacket> {
    	
    	@Override
    	protected LIFXPacket doInBackground(Boolean... turnOn) {
    		if (connKeeper == null) {
    			connKeeper = LIFXConnectionKeeper.getInstance();
    			connKeeper.findGateway();
    		}
    		// Use the gateway globe, since I only have one.
    		// In future, we'll need a better way of selecting the globe in the UI.
    		byte[] mac = connKeeper.getGatewayMac();
    		
    		PowerStatePayload payload = new PowerStatePayload(!powerState);
    		
    		LIFXPacket turnOff = new LIFXPacket.Builder(Type.SET_POWER_STATE)
    			.setGlobeMac(mac)
    			.setPayload(payload)
    			.build();
    		
    		LIFXPacket result = connKeeper.sendPacket(turnOff, true);

    		return result;
    	}
    	
    	@Override
    	protected void onPostExecute(LIFXPacket response) {
    		powerState = ((PowerStatePayload) response.getPayload()).isOn();
    		findViewById(R.id.power_toggle).setEnabled(true);
    		Button power_toggle = ((Button) findViewById(R.id.power_toggle));
    		power_toggle.setText(powerState ? R.string.power_toggle_off : R.string.power_toggle_on);
    	}
    }
}
    