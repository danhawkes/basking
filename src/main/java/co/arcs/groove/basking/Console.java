package co.arcs.groove.basking;

import com.google.common.base.Splitter;

public class Console {

	public static void log(String message) {
		System.out.println(message);
	}

	public static void logIndent(String message) {
		Iterable<String> lines = Splitter.on('\n').split(message);
		for (String line : lines) {
			System.out.println("  " + line);
		}
	}
}
