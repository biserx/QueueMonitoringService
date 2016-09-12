package mr.scns.utils;

public class StringSubtraction {

	public static int sub(String a, String b) {
		int result = 0;		
		if (a.length() != b.length()) return result;
		
		for (int i = 0; i < a.length(); i++) {
			result *= 10;
			result += (a.charAt(i) - b.charAt(i));
		}

		return result;
		
	}

}
