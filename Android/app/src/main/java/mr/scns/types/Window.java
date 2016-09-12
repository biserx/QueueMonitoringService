package mr.scns.types;

import java.io.Serializable;

public class Window implements Serializable {

	private static final long serialVersionUID = -7970135215395659390L;

	private int id;
	private String name;
	private String ticket;
	private boolean alarmSet;
	private String minimum;
	private String maximum;

	public Window(int id, String name, String min, String max) {
		super();
		this.id = id;
		this.name = name;
		this.minimum = min;
		this.maximum = max;
	}

	public void setAlarmSet(boolean alarmSet) {
		this.alarmSet = alarmSet;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public void setMinimum(String minimum) {
		this.minimum = minimum;
	}

	public void setMaximum(String maximum) {
		this.maximum = maximum;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTicket() {
		return ticket;
	}

	public boolean isAlarmSet() {
		return alarmSet;
	}
	
	public String getMinimum() {
		return minimum;
	}

	public String getMaximum() {
		return maximum;
	}
	
}
