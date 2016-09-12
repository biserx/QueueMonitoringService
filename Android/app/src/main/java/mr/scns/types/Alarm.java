package mr.scns.types;

public class Alarm {
	int id = 0;
	int windowsID = 0;
	String ticket = null;
	int timestamp;

	public Alarm(int windowsID, String ticket, int timestamp) {
		super();
		this.windowsID = windowsID;
		this.ticket = ticket;
		this.timestamp = timestamp;
	}

	public Alarm(int id, int windowsID, String ticket, int timestamp) {
		super();
		this.id = id;
		this.windowsID = windowsID;
		this.ticket = ticket;
		this.timestamp = timestamp;
	}

	public int getID() {
		return id;
	}
	
	public int getWindowsID() {
		return windowsID;
	}

	public String getTicket() {
		return ticket;
	}

	public int getTimestamp() {
		return timestamp;
	}
	
}