package mr.scns;

import java.text.SimpleDateFormat;
import java.util.Date;

import mr.scns.services.DataService;
import mr.scns.services.ServiceManager;
import mr.scns.R;
import mr.scns.utils.Constants;
import mr.scns.utils.SettingsManager;
import mr.scns.utils.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SettingsActivity extends Activity {
	private ServiceManager service;
	private SettingsManager settings;
	private TextView interval;
	private TextView lastUpdate;

	/*
	 * 0 - Bound to service
	 * 1 - XML from server
	 * 2 - No data received
	 * 3 - Refresh list alarms icons
	 */
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		switch (msg.what) {
			case 1:
				processData((String)msg.obj);
				break;
			default:
				super.handleMessage(msg);
		}
		}
	};
	
	private OnClickListener chkRinging_Click = new OnClickListener() {
		@Override
		public void onClick(View v) {
			settings.setRingingEnabled(((CheckBox)findViewById(R.id.chkRinging)).isChecked());
		}
	};
	
	private OnClickListener chkVibrating_Click = new OnClickListener() {
		@Override
		public void onClick(View v) {
			settings.setVibrationEnabled(((CheckBox)findViewById(R.id.chkVibrating)).isChecked());
		}
	};
	
	private OnClickListener btnBack_Click = new OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				service.send(Message.obtain(null, 0, settings.getRefreshInterval(), 0));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			finish();
		}
	};
	
	private OnSeekBarChangeListener intervalBar_Changed = new OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			settings.setRefreshInterval(seekBar.getProgress() + 10);
		}
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
		}
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			StringBuilder sb = new StringBuilder(15);
			sb.append(progress + 10);
			sb.append(" ");
			sb.append(getResources().getString(R.string.seconds));
			interval.setText(sb.toString());
		}
	};
	
	/*
	 * Will display menu (for users that don't have menu button)
	 */
	OnClickListener imgMenu_OnClickListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			openOptionsMenu();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		service =  new ServiceManager(this,  DataService.class, handler);
		settings = new SettingsManager(getApplicationContext());
		
		//TextView file = (TextView) findViewById(R.id.txtFilePath);
		// Button btnFile = (Button) findViewById(R.id.btnChooseFile);
		ImageView imgMenu = (ImageView) findViewById(R.id.menu);
		CheckBox ringing = (CheckBox) findViewById(R.id.chkRinging);
		CheckBox vibrating = (CheckBox) findViewById(R.id.chkVibrating);
		SeekBar intervalBar = (SeekBar) findViewById(R.id.intervalBar);
		lastUpdate = (TextView) findViewById(R.id.lastServerUpdate);
		interval = (TextView) findViewById(R.id.lblInterval);
		
		Button btnBack = (Button) findViewById(R.id.btnSaveSettings);
		
		imgMenu.setOnClickListener(imgMenu_OnClickListener);
		ringing.setOnClickListener(chkRinging_Click);
		vibrating.setOnClickListener(chkVibrating_Click);
		intervalBar.setOnSeekBarChangeListener(intervalBar_Changed);
		btnBack.setOnClickListener(btnBack_Click);
		
		//file.setText(settings.getRingingTone());
		ringing.setChecked(settings.getRingingEnabled());
		vibrating.setChecked(settings.getVibrationEnabled());
		intervalBar.setProgress(settings.getRefreshInterval() - 10);
		
		lastUpdate.setText(getResources().getString(R.string.lastServerUpdate));
		
		StringBuilder sb = new StringBuilder(15);
		sb.append(settings.getRefreshInterval());
		sb.append(" ");
		sb.append(getResources().getString(R.string.seconds));
		interval.setText(sb.toString());
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (service.isRunning()) service.start();
	}

	@Override
	protected void onStop() {
		service.unbind();
		super.onStop();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		 switch (item.getItemId()) {
		 case R.id.action_about:
				
				dialog.setIcon(android.R.drawable.ic_dialog_info);
				dialog .setTitle(getResources().getString(R.string.about_title));
		        dialog.setMessage(getResources().getString(R.string.about_body));
		        dialog.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		             @Override
		             public void onClick(DialogInterface dialog, int which) {
		            	 dialog.dismiss();
		            }
		        });
		        dialog.show();
		    	return true;
		 case R.id.action_eula:
		 		
				dialog.setIcon(android.R.drawable.ic_dialog_info);
				dialog .setTitle(getResources().getString(R.string.eula_title));
		        dialog.setMessage(getResources().getString(R.string.eula_body));
		        dialog.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
		             @Override
		             public void onClick(DialogInterface dialog, int which) {
		            	 dialog.dismiss();
		            }
		        });
		        dialog.show();
			 	return true;
		 	default:
	            return super.onOptionsItemSelected(item);
		 }
	}

	private void processData(String xml) {
		try {
			XMLParser parser = new XMLParser();
			Document doc = parser.getDomElement(xml); // getting DOM element
			NodeList root = doc.getElementsByTagName(Constants.KEY_ROOT);
			Long lastUpdateTimestamp = Long.parseLong(parser.getAttribute((Element)root.item(0), Constants.ATTR_TIMESTAMP));
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy");
			lastUpdate.setText(getResources().getString(R.string.lastServerUpdate) + "\n" + sdf.format(new Date(lastUpdateTimestamp * Constants.ONE_SECOND)).toString());
		} catch (Exception e) {
			Log.d(Constants.DEBUG_TAG, "SettingsActivity processData error");
			e.printStackTrace();
			return;
		}
		
	
	}
	
}
