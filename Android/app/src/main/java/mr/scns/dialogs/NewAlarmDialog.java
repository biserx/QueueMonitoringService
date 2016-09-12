package mr.scns.dialogs;

import java.util.ArrayList;
import java.util.Locale;

import mr.scns.R;
import mr.scns.types.Window;
import mr.scns.utils.Constants;
import mr.scns.utils.DatabaseManager;
import mr.scns.utils.TicketRangeChecker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class NewAlarmDialog {

	DatabaseManager dbManager = null;
	
	public NewAlarmDialog(DatabaseManager manager) {
		dbManager = manager;
	}

	public static Dialog getDialog(final Activity activity, final int windowID, final  SparseArray<Window> windowsList) {
		
		final Dialog addAlarmDialog = new Dialog(activity);
		
		addAlarmDialog.setOwnerActivity(activity);
		addAlarmDialog.setTitle(R.string.dialogAddAlarmTitle);
		addAlarmDialog.setContentView(R.layout.dialog_alarm);

		// On save button
		addAlarmDialog.findViewById(R.id.dialogAddAlarm).setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				
				DatabaseManager dbManager = new DatabaseManager(activity.getApplicationContext());
				ArrayList<Window> matchingWindows = new ArrayList<Window>();

				Window tmpWindow;
				String ticket = ((EditText) addAlarmDialog.findViewById(R.id.dialogTicket)).getText().toString().toUpperCase(Locale.ENGLISH);;
				if (windowID != -1) {
					tmpWindow = windowsList.get(windowID);
					if (TicketRangeChecker.isInRange(tmpWindow.getMinimum(), ticket, tmpWindow.getMaximum()))
						matchingWindows.add(tmpWindow);
					
				} else {
					for (int i = 0; i < windowsList.size(); i++) {
						tmpWindow = windowsList.valueAt(i);
						if (TicketRangeChecker.isInRange(tmpWindow.getMinimum(), ticket, tmpWindow.getMaximum()))
							matchingWindows.add(tmpWindow);
					}	
				}

				if (matchingWindows.size() == 1) {
					Log.d(Constants.DEBUG_TAG, "One matching, check if not passed already...");
					if (TicketRangeChecker.isInRange(matchingWindows.get(0).getTicket(), ticket, matchingWindows.get(0).getMaximum())) {
						Log.d(Constants.DEBUG_TAG, "Is in range");
						dbManager.setAlarm(matchingWindows.get(0).getId(), ticket, 0);
						Log.d(Constants.DEBUG_TAG, "Done");
						Toast.makeText(activity.getApplicationContext(), activity.getResources().getString(R.string.alarmSuccessfullyAdded), Toast.LENGTH_SHORT).show(); 
						addAlarmDialog.dismiss();
					} else {
						Toast.makeText(activity.getApplicationContext(), activity.getResources().getString(R.string.ticketPassed), Toast.LENGTH_SHORT).show();	
					}
				} else if (matchingWindows.size() > 1) {
					// TODO If more than one window for ticket... Feature in feature
					Log.d(Constants.DEBUG_TAG, "More than one mathcing window for ticket? That shoudn't happend");
				} else {
					Toast.makeText(activity.getApplicationContext(), activity.getResources().getString(R.string.ticketInvalid), Toast.LENGTH_SHORT).show();
				}
				dbManager.close();
			}
		});
		
		// On cancel button
		addAlarmDialog.findViewById(R.id.dialogCancelAlarm).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addAlarmDialog.dismiss();
			}
		});
		
		return addAlarmDialog;
	}
}
