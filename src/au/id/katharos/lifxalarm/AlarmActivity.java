package au.id.katharos.lifxalarm;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

/**
 * View for when the alarm is active and the Sunrise sequence is underway.
 * 
 * Shows a progress bar, and a 'dismiss' button to abort the sequence.
 */
public class AlarmActivity extends Activity {

	private SunriseTask sunriseTask;
	private SunriseCounter sunriseCounter;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getWindow().addFlags(
	    		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
	            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
	            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
	            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	    setContentView(R.layout.activity_alarm);

	    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.sunrise_progress);
	    progressBar.setMax(1000);
	    Callback callback = new Callback() {
			
			@Override
			public boolean handleMessage(Message msg) {
				progressBar.setProgress(msg.arg1);
				return true;
			}
		};

	    Handler handler = new Handler(callback);
	    sunriseTask = new SunriseTask(handler);
	    sunriseTask.execute();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void dismiss(View view) {
    	Log.i("Alarm Activity", "Dissmissing alarm.");
    	sunriseTask.cancel(true);
    	sunriseCounter.cancel();
    }
    
    private class SunriseTask extends AsyncTask<Void, Void, Void> {
    	
    	private Handler handler;
    	private LIFXConnectionKeeper connKeeper;
    	
    	private SunriseTask(Handler handler) {
			super();
			this.handler = handler;
		}
    	
    	@Override
    	protected Void doInBackground(Void... steps) {
    		Log.i("Background Thread", "Background LIFX Request started");
        
    		if (connKeeper == null) {
    			connKeeper = LIFXConnectionKeeper.getInstance();
    			connKeeper.findGateway();
    			if (!connKeeper.isConnected()) {
    				// For testing, we're going to assume that we connected fine, even though we probably didn't.
    				// throw new RuntimeException("We didn't manage to connect! Everyone panic!");
    				// TODO: Handle this better - with Toast... and a notification or something.
    			}
    		}
    		return null;
    	}

    	@Override
    	protected void onPostExecute(Void response) {
    		if (sunriseCounter != null) {
    			sunriseCounter.cancel();
    		}
    		sunriseCounter = new SunriseCounter(connKeeper, handler);
    		sunriseCounter.start();
    	}
    }	
}
