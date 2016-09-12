
package mr.scns;

import java.util.ArrayList;
import java.util.List;

import mr.scns.adapters.WindowsListAdapter;
import mr.scns.dialogs.NewAlarmDialog;
import mr.scns.services.DataService;
import mr.scns.services.ServiceManager;
import mr.scns.R;
import mr.scns.types.Window;
import mr.scns.utils.Constants;
import mr.scns.utils.DatabaseManager;
import mr.scns.utils.SettingsManager;
import mr.scns.utils.StrictModeWrapper;
import mr.scns.utils.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	/* TO-DO: 
	 * - Settings activity
	 * 
	 * - Sound alarm
	 * - Dialog to stop sound alarm
	 * 
	 */

	/*
	 * Variables
	 */
	private String lastXML = null;
	
	private SparseArray<Window> windowsList = new SparseArray<Window>();
	List<Window> helperList = new ArrayList<Window>();
	//private List<Window> windowsList = new ArrayList<Window>();
	
	private WindowsConfigurationDownloader wcTask;
	private ServiceManager service;
	private SettingsManager settings;
	
	private ArrayAdapter<Window> adapter;
	
	private ListView lstWindows;
	private ImageView imgMenu;
	
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
			case 0:
				try {
					service.send(Message.obtain(null, 0, settings.getRefreshInterval(), 0));
					service.send(Message.obtain(null, 1));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case 1:
				processData((String)msg.obj);
				break;
			case 2:
				noDataReceived();
				break;
			case 3:
				// No need to do something, because case 1 is called before this
				break;
			default:
				super.handleMessage(msg);
		}
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
	
	/*
	 * On touch event on some of the window in windows list 
	 * will open new activity with detail of choosen window
	 */
	OnItemClickListener lstWindows_OnItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int windowID = Integer.parseInt( ((TextView) view.findViewById(R.id.id)).getText().toString() );
			Intent in = new Intent(getApplicationContext(), DetailsActivity.class);
			
			in.putExtra(Constants.ATTR_WINDOW_ID, windowID);
			
			//Window tmpWindow = null;
			
			//for (int j = 0; j < windowsList.size(); j ++) {
			//	if (windowsList.get(j).getId() == windowID) {
			//		tmpWindow = windowsList.get(j);
			//		break;
			//	}
			//}
			
			in.putExtra(Constants.IE_WINDOW, windowsList.get(windowID));
			in.putExtra(Constants.IE_XML,lastXML);
			startActivity(in);
		}
	};

	/*
	 * Long touch on list with all windows
	 * Will display menu with two items: Detail, Add alarm
	 */
	OnItemLongClickListener lstWindows_OnItemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, final View arg1, int arg2, long arg3) {
		// TO-DO:
		// Fix getter
		final int pass_arg2 = arg2;
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		alertDialogBuilder.setItems(new String[] {"Detalji", "Postavi alarm"}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
					case 0:
						Intent in = new Intent(getApplicationContext(), DetailsActivity.class);
						in.putExtra(Constants.IE_WINDOW,  helperList.get(pass_arg2));
						in.putExtra(Constants.IE_XML,lastXML);
						startActivity(in);
						break;
					case 1:
						showAlarmDialog(helperList.get(pass_arg2).getId());
						break;
				}
			}
		});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
		return false;
		}
	};
	
	private void showEULA() {
		AlertDialog dialog = new AlertDialog.Builder(this).create();
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
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		StrictModeWrapper.permitAll();
		settings = new SettingsManager(getApplicationContext());
		adapter = new WindowsListAdapter(MainActivity.this, R.layout.list_item_window, helperList);
		service =  new ServiceManager(this,  DataService.class, handler);
		
		configureUI();	
		if (!settings.getEULARead())
			showEULA();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (service.isRunning()) service.start();
	}
	
	@Override
	protected void onResume() {
		if (windowsList.size() == 0) {
			wcTask = new WindowsConfigurationDownloader();
			wcTask.execute();
		}
		super.onResume();
	}

	@Override
	protected void onStop() {
		service.unbind();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DatabaseManager dbManager = new DatabaseManager(getApplicationContext());
		if (dbManager.getAlarmsCount() > 0)
			service.unbind();
		else
			service.stop();
		dbManager.close();
		
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
				showAlarmDialog(-1);
		    	return true;
		 	case R.id.actionRefresh:
		 		setNotification(2);
	        	try {
	        		if (windowsList.size() == 0) {
	        			wcTask = new WindowsConfigurationDownloader();
	        			wcTask.execute();
	        		}
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
		
		if (windowsList.size() <= 0) 
			return;
		
		try {
			XMLParser parser = new XMLParser();
			Document doc = parser.getDomElement(xml); // getting DOM element
			DatabaseManager dbManager = new DatabaseManager(getApplicationContext());
					
			NodeList root = doc.getElementsByTagName(Constants.KEY_ROOT);
			Long lastUpdateTimestamp = Long.parseLong(parser.getAttribute((Element)root.item(0), Constants.ATTR_TIMESTAMP));
			if (lastUpdateTimestamp + Constants.OBSOLETE_DATA_DELAY < System.currentTimeMillis()/1000) {
				setNotification(3);
			 } else {
				 setNotification(0);
			 }
			
			
			NodeList nl = doc.getElementsByTagName(Constants.KEY_WINDOW);
			// looping through all item nodes <item>
			
			for (int i = 0; i < nl.getLength(); i++) {
				
				Element e = (Element) nl.item(i);
				
				int windowID = Integer.parseInt(parser.getAttribute(e, Constants.ATTR_WINDOW_ID));
				
				String ticket = parser.getValue(e, Constants.KEY_TICKET);
	
				windowsList.get(windowID).setTicket(ticket);
				if (dbManager.getAlarmsCount(windowID) > 0) {
					windowsList.get(windowID).setAlarmSet(true); 
				} else {
					windowsList.get(windowID).setAlarmSet(false);
				}
			}
	
			helperList.clear();
			for (int i = 0; i < windowsList.size(); i++) {
				helperList.add(windowsList.valueAt(i));
			}
			
			lastXML = new String(xml);
			adapter.notifyDataSetChanged();
			dbManager.close();
			
		} catch (Exception e) {
			Log.d(Constants.DEBUG_TAG, "MainActivity processData error");
			e.printStackTrace();
			return;
		}
	}

	private void noDataReceived() {
		setNotification(4);
	}

	private void showAlarmDialog(int windowID) {
	
		Dialog addAlarmDialog;
		if (windowID != -1)  {
			addAlarmDialog = NewAlarmDialog.getDialog(this, windowID, windowsList);
			((TextView)addAlarmDialog.findViewById(R.id.windowLabel)).setText(windowsList.get(windowID).getName());
			((TextView)addAlarmDialog.findViewById(R.id.windowLabel)).setVisibility(View.VISIBLE);
			((TextView)addAlarmDialog.findViewById(R.id.windowLabelTitle)).setVisibility(View.VISIBLE);
		} else { 
			addAlarmDialog = NewAlarmDialog.getDialog(this, -1, windowsList);
		}
		
		addAlarmDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				processData(lastXML);
				//adapter.notifyDataSetChanged();
				try {
					service.send(Message.obtain(null, 2));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				Log.d(Constants.DEBUG_TAG, "Back button pressed");
			}
		});
		
		addAlarmDialog.show();
		
	}


	private void configureUI() {
		imgMenu = (ImageView) findViewById(R.id.menu);
		imgMenu.setOnClickListener(imgMenu_OnClickListener);
		lstWindows = (ListView) findViewById(R.id.windowsList);
		lstWindows.setOnItemLongClickListener(lstWindows_OnItemLongClickListener);
		lstWindows.setOnItemClickListener(lstWindows_OnItemClickListener);
		lstWindows.setAdapter(adapter);        
	}
	
	private void setNotification(int id) {
		TextView txtInfo = (TextView) findViewById(R.id.info);
		switch(id){
		case 0:
			txtInfo.setVisibility(View.GONE);
			break;
		case 1:
			txtInfo.setText(R.string.init);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.info));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 2:
			txtInfo.setText(R.string.dataGathering);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.info));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 3:
			txtInfo.setText(R.string.oldData);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.info));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 4:
			txtInfo.setText(R.string.noData);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.warning));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		case 5:
			txtInfo.setText(R.string.invalidData);
			txtInfo.setTextColor(getResources().getColor(R.color.textShine));
			txtInfo.setBackgroundColor(getResources().getColor(R.color.warning));
			txtInfo.setVisibility(View.VISIBLE);
			break;
		}
 		
	}

	class WindowsConfigurationDownloader extends AsyncTask<Integer, Integer, Integer> {

		@Override
		protected void onPreExecute() {
			// Initializing..
			setNotification(1);
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Integer... arg0) {
			
			if (windowsList.size() > 0) 
				return null;
			
			XMLParser parser = new XMLParser();
			String xml = parser.getXmlFromUrl(Constants.CONFIG_URL);
			
			if (xml == null) {
				//publishProgress(4);
				return 4;
			}

			windowsList.clear();
			
			try {
			
				Document doc = parser.getDomElement(xml); // getting DOM element
			
				NodeList nl = doc.getElementsByTagName(Constants.KEY_WINDOW);
				
				for (int i = 0; i < nl.getLength(); i++) {
					Element e = (Element) nl.item(i);
					
					int id = Integer.parseInt(parser.getAttribute(e, Constants.ATTR_WINDOW_ID));
					String name = Html.fromHtml(parser.getValue(e, Constants.KEY_NAME)).toString();
					String minimum = Html.fromHtml(parser.getValue(e, Constants.KEY_MIN)).toString();
					String maximum = Html.fromHtml(parser.getValue(e, Constants.KEY_MAX)).toString();
					
					windowsList.append(id, new Window(id, name, minimum, maximum));
				}

			} catch (Exception e) {
				e.printStackTrace();
				//publishProgress(5);
				return 5;
			}
			
			if (windowsList.size() == 0) 
				return null;
			//publishProgress(0);
			return 0;
			
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			switch (result) {
				case 0: // Start service and remove any notification
					//adapter.notifyDataSetChanged();
					service.start();
					setNotification(2);
					break;
				case 4: // no data received
					setNotification(4);
					break;
				case 5: // fucking error happend while parsing xml
					setNotification(5);
					break;
			}
			super.onPostExecute(result);
		}
	}
}
