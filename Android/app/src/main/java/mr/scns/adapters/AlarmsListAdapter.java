package mr.scns.adapters;

import java.util.Calendar;
import java.util.List;

import mr.scns.interfaces.ListRefresher;
import mr.scns.R;
import mr.scns.types.Alarm;
import mr.scns.utils.Constants;
import mr.scns.utils.DatabaseManager;
import mr.scns.utils.StringSubtraction;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmsListAdapter extends ArrayAdapter<Alarm>{

	final Activity activity;
	int resource = 0;
	List<Alarm> data = null;
	double statistic;
	String currentTicket = null;

	public AlarmsListAdapter(Activity activity, int resource, List<Alarm> alarmsList, String currentTicket, double statistic) {
		super(activity.getApplicationContext(), resource, alarmsList);
		this.activity = activity;
		this.resource = resource;
		this.data = alarmsList; 
		this.currentTicket = currentTicket;
		this.statistic = statistic;
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		LayoutInflater inflater = activity.getLayoutInflater();
		row = inflater.inflate(resource, parent, false);

		TextView txtID = (TextView) row.findViewById(R.id.id);
		TextView txtTicket = (TextView) row.findViewById(R.id.ticket);
		ImageView imgDelAlarm = (ImageView) row.findViewById(R.id.delAlarm);
		TextView txtPassTime = (TextView) row.findViewById(R.id.expectedPassTimeValue);

		final Alarm alarm = data.get(position);

		txtID.setText(String.valueOf(alarm.getID()));
		txtTicket.setText(alarm.getTicket());
		imgDelAlarm.setImageResource(R.drawable.pic_delete_alarm);

		imgDelAlarm.setOnClickListener(new OnClickListener() {

		@Override
		public void onClick(View v) {
			DatabaseManager dbManager = new DatabaseManager(activity.getApplicationContext());
			dbManager.deleteAlarm(alarm.getID());
			dbManager.close();
			Toast.makeText(activity.getApplicationContext(), activity.getResources().getString(R.string.alarmDeleted), Toast.LENGTH_SHORT).show();
			ListRefresher lr = (ListRefresher) activity;
			lr.refreshAlarmsList();
		}
		});

		if (currentTicket != null && statistic != 0) {
			Calendar cal = Calendar.getInstance();
			long targetTime = (long) System.currentTimeMillis() / Constants.ONE_SECOND;
			int delta = StringSubtraction.sub(alarm.getTicket(),currentTicket);
			targetTime += delta / statistic  * Constants.ONE_HOUR;
			targetTime *= Constants.ONE_SECOND;
			cal.setTimeInMillis(targetTime);

			if (cal.get(Calendar.MINUTE) < 10) // Adds zero if theres less than 10 minutes
				txtPassTime.setText(cal.get(Calendar.HOUR_OF_DAY) + ":0" + cal.get(Calendar.MINUTE)+"h");
			else
				txtPassTime.setText(cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) +"h");
		} else {
			txtPassTime.setText(getContext().getResources().getString(R.string.na));
		}
		return row;
	}

}
