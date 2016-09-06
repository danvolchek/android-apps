package com.volchekD.LaundryApp;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Alarm extends BroadcastReceiver {
	private boolean set;
	private long when;

	private List<AlarmListener> listeners = new ArrayList<>();

	public void addListener(AlarmListener toAdd) {
		listeners.add(toAdd);
	}

	public Alarm() {
		set = false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		Toast.makeText(context, "Check Your Laundry", Toast.LENGTH_LONG).show();
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

		v.vibrate(new long[] {0, 500, 500, 500, 500, 500}, -1);

		CancelAlarm(context);

		for (AlarmListener hl : listeners)
			hl.alarmFinished();
	}

	public void SetAlarm(long time, Context context) {
		if (!set) {
			set = true;
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, Alarm.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
			am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, pi);
			when = System.currentTimeMillis() + time;
			Toast.makeText(context, "Set Alarm for: " + time / 60000 + " min", Toast.LENGTH_SHORT).show();
		} else
			Toast.makeText(context, "Alarm already set for: " + (when - System.currentTimeMillis()) / 60000 + " min", Toast.LENGTH_SHORT).show();

	}

	public void CancelAlarm(Context context) {
		if (set) {
			Intent intent = new Intent(context, Alarm.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(sender);
			Toast.makeText(context, "Alarm Canceled", Toast.LENGTH_SHORT).show();
		}
		set = false;
	}

}