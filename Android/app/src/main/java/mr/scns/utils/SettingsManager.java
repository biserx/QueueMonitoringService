package mr.scns.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class SettingsManager {
	SharedPreferences preferences;
	public SettingsManager(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public boolean getEULARead() {
		boolean result =  preferences.getBoolean("eula", false);
		Editor editor = preferences.edit();
		editor.putBoolean("eula", true);
		editor.commit();
		return result;
	}
	
	public void setRingingEnabled(boolean enabled) {
		Editor editor = preferences.edit();
		editor.putBoolean("ringing", enabled);
		editor.commit();
	}

	public boolean getRingingEnabled() {
		return preferences.getBoolean("ringing", true);
	}
	
	public void setRingingTone(String tone) {
		Editor editor = preferences.edit();
		editor.putString("tone", tone);
		editor.commit();
	}
	
	public String getRingingTone() {
		return preferences.getString("tone", null);
	}
	
	public void setVibrationEnabled(boolean enabled) {
		Editor editor = preferences.edit();
		editor.putBoolean("vibration", enabled);
		editor.commit();
	}

	public boolean getVibrationEnabled() {
		return preferences.getBoolean("vibration", true);
	}
	
	public int getRefreshInterval() {
		return preferences.getInt("refresh", 60);
	}
	
	public void setRefreshInterval(int refreshInterval) {
		Editor editor = preferences.edit();
		editor.putInt("refresh", refreshInterval);
		editor.commit();
	}
	
}
