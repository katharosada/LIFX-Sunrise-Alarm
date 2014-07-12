package au.id.katharos.lifxalarm;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * A time-picker widget to set the alarm time.
 */
public class CustomTimePickerDialog extends TimePickerDialog {

    private static class Listener implements TimePickerDialog.OnTimeSetListener {

    	private Activity sourceActivity; 
    	
    	public Listener(Activity sourceActivity) {
			this.sourceActivity = sourceActivity; 
		}
    	
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			 Log.i("Time Picker Dialog", "The time is set to: " + hourOfDay + ":" + minute);
		     Intent intent = new Intent(sourceActivity, AlarmActivity.class);
		     PendingIntent alarmIntent = PendingIntent.getActivity(sourceActivity, 0, intent, 0);
		     
		     AlarmManager alarmMgr = (AlarmManager)sourceActivity.getSystemService(Context.ALARM_SERVICE);
		     
		     // Set the alarm to start at the given time.
		     Calendar calendar = Calendar.getInstance();
		     calendar.setTimeInMillis(System.currentTimeMillis());
		     calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		     calendar.set(Calendar.MINUTE, minute);
		     calendar.set(Calendar.SECOND, 0);
		     
		     Calendar currentTime = Calendar.getInstance();
		     currentTime.setTimeInMillis(System.currentTimeMillis());
		     
		     if (calendar.compareTo(currentTime) < 1) {
		    	 // This calendar is before the current time, increase by a day.
		    	 calendar.add(Calendar.DATE, 1);
		     }

		     // With setInexactRepeating(), you have to use one of the AlarmManager interval
		     // constants--in this case, AlarmManager.INTERVAL_DAY.
		     alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
		     
		     // Find difference between scheduled time and now
		     long millis = calendar.getTimeInMillis() - System.currentTimeMillis();
		     
		     int minutes = (int) (1 + millis / 1000 / 60);
		     Log.i("Time Picker Dialog", "Schedlued intent for " + minutes + "min from now.");
		     
		     int hours = minutes / 60;
		     minutes = minutes % 60; 
		     Toast.makeText(sourceActivity.getApplicationContext(), 
		    		 		"Sunrise scheduled for " + hours + " hours and " + minutes + " minutes from now.",
		    		 		Toast.LENGTH_LONG).show();
		}
    }

	public CustomTimePickerDialog(Activity sourceActivity, int hourOfDay, int minute,
			boolean is24HourView) {
		super(sourceActivity, THEME_DEVICE_DEFAULT_DARK, new Listener(sourceActivity), hourOfDay, minute, is24HourView);
	}
	
	@Override
	protected void onStop() {
		// Do nothing - We're overriding this function so it doesn't call onTimeSet twice.
	}
}
