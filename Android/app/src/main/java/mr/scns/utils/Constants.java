package mr.scns.utils;

public class Constants {

	public static final String URL = "http://www.scns.rs/adRedova/data.xml";
	public static final String CONFIG_URL = "http://www.scns.rs/adRedova/config.xml";

	public static final int ONE_SECOND = 1000; // miliseconds
	public static final int ONE_HOUR = 3600; // miliseconds
	public static final int OBSOLETE_DATA_DELAY = 300; // seconds
	public static final int ONE_DAY = 86400; // seconds

	public static final int CONNECTION_TIMEOUT = 3;
	public static final int SOCKET_TIMEOUT = 5;

	// XML node keys
	public static final String KEY_ROOT = "sluzba-smestaja";
	
	public static final String ATTR_TIMESTAMP = "timestamp";
	
	public static final String ATTR_WINDOW_ID = "id"; 
	public static final String ATTR_WINDOW_NAME = "name";
	
	public static final String KEY_NAME = "name";
	public static final String KEY_MIN = "min";
	public static final String KEY_MAX = "max";
	
	public static final String KEY_WINDOW = "window";
	
	public static final String KEY_TICKET = "ticket";
	public static final String KEY_STATISTIC = "statistic";
	public static final String KEY_QUEUE = "queue";
	public static final String KEY_TIMESTAMP = "timestamp";

	public static final String IE_WINDOW = "configuration";
	public static final String IE_XML= "xml";

	public static final String DEBUG_TAG = "SCNS_SSmestaja";

}
