package co.arcs.groove.basking;

public class Log {

	public static void d(String message) {
		System.out.println(message);
	}

	public static void e(String message) {
		System.err.println(message);
	}

	public static void e(Throwable t) {
		System.err.println(t);
	}

}
