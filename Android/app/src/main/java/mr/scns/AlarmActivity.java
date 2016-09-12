package mr.scns;

import mr.scns.R;
import mr.scns.utils.SettingsManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;

public class AlarmActivity extends Activity {
	
	private static Ringtone r = null;
	private static Vibrator vb = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_alarm);
		SettingsManager settings = new SettingsManager(getApplicationContext());
		
		if (settings.getRingingEnabled()) {
			Uri notification = null;
			String file = settings.getRingingTone();
			if (file != null)
				notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			else 
				notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

			r = RingtoneManager.getRingtone(this.getApplicationContext(), notification);
			r.play();
		}
		
		if (settings.getVibrationEnabled()) {
			vb = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
			vb.vibrate(new long[] {100, 400,100,400,100,1200},-1);
		}
		
		PowerManager pm = (PowerManager)this.getApplicationContext().getSystemService(Context.POWER_SERVICE); 
		final WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TRAININGCOUNTDOWN");
		wl.acquire(); 
   	 	wl.release();
   	 	
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog .setTitle(this.getResources().getString(R.string.alarm));
        dialog.setMessage(this.getResources().getString(R.string.alarmText));
        dialog.setButton(this.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
            	 if (r != null) r.stop();
            	 if (vb != null) vb.cancel();
            	 
            	 dialog.dismiss();
            	 finish();
             }
        });
        
		dialog.show();
	}
}
