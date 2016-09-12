package mr.scns.adapters;

import java.util.List;

import mr.scns.R;
import mr.scns.types.Window;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class WindowsListAdapter extends ArrayAdapter<Window>{
	
	Activity activity = null;
	int resource = 0;
	List<Window> data = null;
	public WindowsListAdapter(Activity activity, int resource, List<Window> windowsList) {
		super(activity.getApplicationContext(), resource, windowsList);
		this.activity = activity;
		this.resource = resource;
		this.data = windowsList; 
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		LayoutInflater inflater = activity.getLayoutInflater();
		row = inflater.inflate(resource, parent, false);

		TextView txtID = (TextView) row.findViewById(R.id.id);
		TextView txtTicket = (TextView) row.findViewById(R.id.ticket);
		ImageView imgAlarm = (ImageView) row.findViewById(R.id.alarm);
		TextView txtName = (TextView) row.findViewById(R.id.name);

		Window wRow = data.get(position);

		txtID.setText(String.valueOf(wRow.getId()));
		txtTicket.setText(wRow.getTicket());
		if (wRow.isAlarmSet()) imgAlarm.setImageResource(R.drawable.pic_alarm);
		txtName.setText(wRow.getName());

		return row;
	}

}
