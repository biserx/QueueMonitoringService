package mr.scns;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import mr.scns.adapters.AlarmsListAdapter;
import mr.scns.dialogs.NewAlarmDialog;
import mr.scns.interfaces.ListRefresher;
import mr.scns.services.DataService;
import mr.scns.services.ServiceManager;
import mr.scns.R;
import mr.scns.types.Alarm;
import mr.scns.types.Window;
import mr.scns.utils.Constants;
import mr.scns.utils.DatabaseManager;
import mr.scns.utils.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class DetailsActivity extends Activity implements ListRefresher {

	private Window window = null;
	
	private ServiceManager service;
	
	private ImageView imgMenu;
	private ImageView imgAddAlarm;
	private TextView txtInfo;
	private ListView lstAlarms;
	private TextView lblNoAlarms;
	
	private String startXML = null;	
	private String currentTicket = null;
	private double statisticValue = 0;
	
	private ArrayAdapter<Alarm> adapter;

	/*
	 * 0 - Bound to service
	 * 1 - XML from server
	 * 2 - No data received
	 * 3 - Refresh alarms list
	 */
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1:
					processData((String)msg.obj);
					break;
				case 2:
					noDataReceived();
					break;
				case 3:
					refreshAlarmsList();
					break;
				default:
					super.handleMessage(msg);
			} 
		}
	};

	/*
	 * Shows add new alarm dialog if user touch the Add alarm icon
	 */
	OnClickListener imgAddAlarm_OnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showAlarmDialog(window); 
		}
	};

	/*
	 * Shows the menu for users that don't have menu button
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
		setContentView(R.layout.activity_details);

		window = (Window) getIntent().getSerializableExtra(Constants.IE_WINDOW);
		startXML = getIntent().getExtras().getString(Constants.IE_XML);
		if (startXML == null) 
			System.exit(-1);
		
		processData(startXML);
		
		configureUI();
		
		service =  new ServiceManager(this,  DataService.class, handler);
		service.start();

		refreshAlarmsList();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.unbind();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.actionAddAlarm:
				showAlarmDialog(window);
				return true;
			case R.id.actionRefresh:
				try {
					Log.d(Constants.DEBUG_TAG, "Refresh clicked");
					service.send(Message.obtain(null, 1));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				return true;
			case R.id.actionSettings:
				Intent in = new Intent(getApplicationContext(), SettingsActivity.class);
				startActivity(in);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		 }
	}

	private void processData(String xml) {
		setNotification(0);
		
		try {
			if (window.getId() != 0) {
				
				XMLParser parser = new XMLParser();
				Document doc = parser.getDomElement(xml); // getting DOM element
				
				NodeList root = doc.getElementsByTagName(Constants.KEY_ROOT);
				Long lastUpdateTimestamp = Long.parseLong(parser.getAttribute((Element)root.item(0), Constants.ATTR_TIMESTAMP));
				if (lastUpdateTimestamp + Constants.OBSOLETE_DATA_DELAY < System.currentTimeMillis()/1000) {
					setNotification(2);
				}

				TextView name =  (TextView) findViewById(R.id.name);
				TextView ticket =  (TextView) findViewById(R.id.ticket);
				TextView statistic =  (TextView) findViewById(R.id.statistic);
				TextView queue = (TextView) findViewById(R.id.queue);
				TextView timestamp =  (TextView) findViewById(R.id.timestamp);

				NodeList nl = doc.getElementsByTagName(Constants.KEY_WINDOW);
				// looping through all item nodes <item>
				for (int i = 0; i < nl.getLength(); i++) {
					Element e = (Element) nl.item(i);
					
					if (Integer.parseInt(parser.getAttribute(e, Constants.ATTR_WINDOW_ID)) == window.getId()) {
						
						name.setText(Html.fromHtml(window.getName()).toString());
						
						this.currentTicket = parser.getValue(e, Constants.KEY_TICKET);
						ticket.setText(this.currentTicket);

						Long lastChangeTimestamp = Long.parseLong(parser.getValue(e, Constants.KEY_TIMESTAMP));
						
						if (lastChangeTimestamp + Constants.ONE_DAY  > System.currentTimeMillis() / Constants.ONE_SECOND) {
							
							double statisticValue = Double.parseDouble(parser.getValue(e, Constants.KEY_STATISTIC));
							if (statisticValue > 0) {
								statistic.setText("~" + String.valueOf(statisticValue));
								this.statisticValue = statisticValue;
							} else {
								statistic.setText(getResources().getString(R.string.na));
							}
							
							queue.setText(parser.getValue(e, Constants.KEY_QUEUE));
							
							Calendar cal = Calendar.getInstance();
							cal.setTimeInMillis(lastChangeTimestamp * Constants.ONE_SECOND);
	
							if (cal.get(Calendar.MINUTE) < 10) // Dodaje nulu ako ima manje od 10min
								timestamp.setText(cal.get(Calendar.HOUR_OF_DAY)+":0"+cal.get(Calendar.MINUTE) + "h");
							else
								timestamp.setText(cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE) + "h");
						} else {
							timestamp.setText(getResources().getString(R.string.na));
							statistic.setText(getResources().getString(R.string.na));
						}
						
						break;
					}
				}
		
	
				
			}
		} catch (Exception e) {
			Log.d(Constants.DEBUG_TAG, "DetailsActivity processData error");
			e.printStackTrace();
			return;
		}
	}

	private void noDataReceived() {
		setNotification(3);
	}

	private void showAlarmDialog(Window window) {
		SparseArray<Window> helperList = new SparseArray<Window>();
		helperList.append(window.getId(), window);
		
		Dialog addAlarmDialog = NewAlarmDialog.getDialog(this, window.getId(), helperList);
		
		addAlarmDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				refreshAlarmsList();
			}
		});
		
		if (window.getName() != null) {
			((TextView)addAlarmDialog.findViewById(R.id.windowLabel)).setText(window.getName());
			((TextView)addAlarmDialog.findViewById(R.id.windowLabel)).setVisibility(View.VISIBLE);
			((TextView)addAlarmDialog.findViewById(R.id.windowLabelTitle)).setVisibility(View.VISIBLE);
		}
		
		addAlarmDialog.show();
	}

	// for Alarms list adapter
	@Override
	public void refreshAlarmsList() {
		List<Alarm> alarmsList = new ArrayList<Alarm>();
		DatabaseManager dbManager = new DatabaseManager(getApplicationContext());
		alarmsList = dbManager.getAlarms(window.getId());
		
		//if (dbManager.getAlarmsCount(windowID) > 0) {
		if (alarmsList.size() > 0) {
			adapter = new AlarmsListAdapter(this, R.layout.list_item_alarm, alarmsList, currentTicket, statisticValue);
			lstAlarms.setAdapter(adapter);
			lblNoAlarms.setVisibility(View.GONE);
			lstAlarms.setVisibility(View.VISIBLE);
		} else {
			lblNoAlarms.setVisibility(View.VISIBLE);
			lstAlarms.setVisibility(View.GONE);
		}
		dbManager.close();
		
		try {
			if (service != null) service.send(Message.obtain(null, 2));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}

	private void configureUI() {
		txtInfo = (TextView) findViewById(R.id.info);
		lblNoAlarms = (TextView) findViewById(R.id.lblNoAlarms);
		imgAddAlarm = (ImageView) findViewById(R.id.add_alarm);
		lstAlarms = (ListView) findViewById(R.id.alarms);
		imgMenu = (ImageView) findViewById(R.id.menu);
		imgMenu.setOnClickListener(imgMenu_OnClickListener);
		imgAddAlarm.setOnClickListener(imgAddAlarm_OnClickListener);
		txtInfo.setVisibility(View.GONE);
	}
	
	private void setNotification(int id) {
		TextView txtInfo = (TextView) findViewById(R.id.info);
		switch(id){
		case 0:
			txtInfo.setVisibility(View.GONE);
			break;
		case 1:
			txtInfo.setText(R.string.dataGathering);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.info));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 2:
			txtInfo.setText(R.string.oldData);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.info));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 3:
			txtInfo.setText(R.string.noData);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.warning));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		}
	}
}
