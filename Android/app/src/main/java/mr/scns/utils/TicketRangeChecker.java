package mr.scns.utils;

public class TicketRangeChecker {

public static boolean isInRange(String min, String ticket, String max) {
	
		if (ticket.length() == 0) return false;
		
		if (min.length() > ticket.length()) return false;
		if (ticket.length() > max.length()) return false;
		
		if (min.compareTo(ticket) >= 0) return false;
		if (max.compareTo(ticket) < 0) return false;
		return true;
	}
	
}
