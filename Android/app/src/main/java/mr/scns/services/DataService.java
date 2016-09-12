package mr.scns.services;

import java.util.ArrayList;

import mr.scns.AlarmActivity;
import mr.scns.MainActivity;
import mr.scns.R.drawable;
import mr.scns.types.Alarm;
import mr.scns.utils.Constants;
import mr.scns.utils.DatabaseManager;
import mr.scns.utils.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class DataService extends AbstractService {
	
	private PendingIntent pi;
	private AlarmManager am;
	private BroadcastReceiver br = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
        	retrieveData();
        }
    };
	private NotificationManager mNotificationManager;

	private String lastXML;
	private int refreshInterval = 60;

	/*
	 * 
	 * 0 - Change timer interval
	 * 1 - Request for XML
	 * 2 - Alarm is set, so set the notification
	 * 3 - 
	 */
	@Override
	public void onReceiveMessage(Message msg) {
		switch(msg.what) {
			case 0:
				refreshInterval = msg.arg1;
				// TODO Will this work?
				am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + refreshInterval * Constants.ONE_SECOND, refreshInterval * Constants.ONE_SECOND, pi);
				Log.d(Constants.DEBUG_TAG, "New refresh interval: " + String.valueOf(refreshInterval));
				break;
			case 1:
				retrieveData();
				break;
			case 2:
		    	alarmIsSet();
			    break;

		}
	}

	@Override
	public void onStartService() {
		
    	registerReceiver(br, new IntentFilter("scns.ssmestaja.services"));
    	pi = PendingIntent.getBroadcast(this, 0, new Intent("scns.ssmestaja.services"), 0);
    	am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));
    	am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), refreshInterval * Constants.ONE_SECOND, pi);
    	
    	updateNotification();
	}

	@Override
	public void onClientConnected() {		
		
		send(Message.obtain(null, 0));
	}
	
	@Override
	public void onStopService() {
		if (this.mNotificationManager != null) 
			this.mNotificationManager.cancel(1);
		am.cancel(pi);
		unregisterReceiver(br);
	}

	private void alarmIsSet() {
		processData(lastXML);
		updateNotification();
	}

	private void retrieveData() {
		WindowsStateDownloader wsTask = new WindowsStateDownloader();
		wsTask.execute();
	}

	private void processData(String xml) {
		try {
			XMLParser parser = new XMLParser();
			Document doc = parser.getDomElement(xml); // getting DOM element
			NodeList nl = null;
			
			nl = doc.getElementsByTagName(Constants.KEY_WINDOW);
			
			// looping through all item nodes <item>
			for (int i = 0; i < nl.getLength(); i++) {
				Element e = (Element) nl.item(i);
				
				//String ticket = parser.getValue(e, Constants.KEY_TICKET);
				try {
					int windowID = Integer.valueOf(parser.getAttribute(e, Constants.ATTR_WINDOW_ID));
					String ticket = parser.getValue(e, Constants.KEY_TICKET);
					notifyAlarms(windowID, ticket);
				} catch (Exception ee) {}
			}
			lastXML = xml;

			send(Message.obtain(null, 1, xml));
		} catch (Exception e) {
			Log.d(Constants.DEBUG_TAG, "DataService processData error");
			e.printStackTrace();
			return;
		}
	}
	
	private void notifyAlarms(int windowID, String ticket) {

		DatabaseManager dbManager = new DatabaseManager(getApplicationContext());
		
		ArrayList<Alarm> alarms = dbManager.getAlarms(windowID);
		if (alarms.size() > 0) {
			for(int i = 0; i < alarms.size(); i++) {
				Alarm alarm = alarms.get(i);
				//if (alarm.getTicket().equals(ticket)) {
				Log.d(Constants.DEBUG_TAG, "Compare: '" + ticket + "' to: '" + alarm.getTicket() + "' => " + String.valueOf(alarm.getTicket().compareTo(ticket)));
				if (alarm.getTicket().compareTo(ticket) <= 0) {
					dbManager.deleteAlarm(alarm.getID());
					updateNotification();
					makeAlarm();
				}
			}
		}
		dbManager.close();
		
	}
	
	private void makeAlarm() {
		Intent i = new Intent(getApplicationContext(), AlarmActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		getApplication().startActivity(i);
	}

	private void updateNotification() {
		DatabaseManager dbManager = new DatabaseManager(getApplicationContext());
		int _alarmsCount = dbManager.getAlarmsCount();

		if (_alarmsCount == 0) {
			if (this.mNotificationManager != null) {
				this.mNotificationManager.cancel(1);
				this.mNotificationManager = null;
			}
		} else {
			if (this.mNotificationManager == null) {
				this.mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				
				Notification not = new Notification(drawable.ic_launcher, "SCNS alarm manager started", System.currentTimeMillis());
			    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), Notification.FLAG_ONGOING_EVENT);        
			    not.flags = Notification.FLAG_ONGOING_EVENT;
			    not.setLatestEventInfo(this, "SCNS", "SCNS alarms", contentIntent);
			    this.mNotificationManager.notify(1, not);
			}
		}
		
		dbManager.close();
	}

	class WindowsStateDownloader extends AsyncTask<Integer, Integer, String> {
		@Override
		protected String doInBackground(Integer... arg0) {
			XMLParser parser = new XMLParser();
			String xml = parser.getXmlFromUrl(Constants.URL);
			return xml;
		}
		 @Override
		 protected void onPostExecute(String xml) {
			 if (xml != null) {		
				 Log.d(Constants.DEBUG_TAG, "XML = OK!");
				 processData(xml);
			 } else {
				 Log.d(Constants.DEBUG_TAG, "XML = null?");
				 send(Message.obtain(null, 2));
			 }
			 super.onPostExecute(xml);
		 }
	}
}
