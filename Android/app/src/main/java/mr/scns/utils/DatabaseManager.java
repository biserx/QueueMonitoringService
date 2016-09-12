package mr.scns.utils;

import java.util.ArrayList;

import mr.scns.types.Alarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseManager  extends SQLiteOpenHelper  {

	private SQLiteDatabase database = getWritableDatabase();
	
	public DatabaseManager(Context context) {
		super(context, "SSmestaja_alarms.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE alarms (id INTEGER primary key, windowID int not null, ticket text not null, timestamp int not null);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS alarms");
		onCreate(db);
	}

	public void setAlarm(int windowID, String ticket, int timestamp) {
		if (ticket == null) return;
		
		if (getAlarm(ticket) == null) {
			ContentValues args = new ContentValues();
			args.put("windowID", windowID);
			args.put("ticket", ticket);
			args.put("timestamp", timestamp);
			database.insert("alarms", null, args);
		}
		
	}
	
	public int getAlarmsCount() {
		
		Cursor cursor = database.query("alarms", new String[] {"id", "windowID", "ticket", "timestamp"},null,null,null,null,null);
		
		return cursor.getCount();
	}
	
	public int getAlarmsCount(int windowID) {
		
		Cursor cursor = database.query("alarms", new String[] {"id", "windowID", "ticket", "timestamp"},"windowID = ?" ,new String[] { String.valueOf(windowID) },null,null,null);
		
		return cursor.getCount();
	}
	
	public ArrayList<Alarm> getAlarms(int windowID) {
		
		ArrayList<Alarm> alarms = new ArrayList<Alarm>();
		Cursor cursor = database.query("alarms", new String[] {"id", "windowID", "ticket", "timestamp"},"windowID = ?" ,new String[] { String.valueOf(windowID) },null,null,null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			while(true) {
				alarms.add(new Alarm(
						cursor.getInt(cursor.getColumnIndex("id")),
						cursor.getInt(cursor.getColumnIndex("windowID")),
						cursor.getString(cursor.getColumnIndex("ticket")),
						cursor.getInt(cursor.getColumnIndex("timestamp"))));
				
				if (!cursor.moveToNext()) break;
			}
		}
		
		return alarms;
	}
	
	public Alarm getAlarm(String ticket) {
		
		Cursor cursor = database.query("alarms", new String[] {"id", "windowID", "ticket", "timestamp"},"ticket = ?" ,new String[] { ticket },null,null,null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			return new Alarm(
					cursor.getInt(cursor.getColumnIndex("id")),
					cursor.getInt(cursor.getColumnIndex("windowID")),
					cursor.getString(cursor.getColumnIndex("ticket")),
					cursor.getInt(cursor.getColumnIndex("timestamp"))
							);
		}
		
		return null;		
	}
	
	public void deleteAlarm(int id) {
		database.delete("alarms", "id = ?", new String[] { String.valueOf(id) });
	}
	
	public void deleteAllAlarms() {
		database.execSQL("DELETE FROM alarms;");
	}

}
