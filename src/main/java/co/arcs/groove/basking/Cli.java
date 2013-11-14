package co.arcs.groove.basking;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.common.util.concurrent.ListenableFuture;

public class Cli {

	public static void main(String[] args) {
		Config config = new Config();
		CmdLineParser parser = new CmdLineParser(config);
		try {
			parser.parseArgument(args);
			if (config.help) {
				printUsage(parser, null);
				return;
			}
			run(config);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.out.println();
			printUsage(parser, e);
		}
	}

	private static void printUsage(CmdLineParser parser, CmdLineException e) {
		System.out.println("NAME");
		System.out.println();
		System.out.println("basking -- Sync a grooveshark library to disk");
		System.out.println();
		System.out.println("SYNOPSIS");
		System.out.println();
		parser.printSingleLineUsage(System.out);
		System.out.println();
		System.out.println();
		System.out.println("DESCRIPTION");
		System.out.println();
		parser.printUsage(System.out);
	}

	private static void printOutcome(SyncOutcome outcome) {
		System.out.println("Finished sync task. Downloaded " + outcome.downloaded + " of "
				+ (outcome.downloaded + outcome.failedToDownload) + " items.");
	}

	public static void run(Config config) {
		ListenableFuture<SyncOutcome> serviceOutcome = new SyncService(config).start();
		try {
			SyncOutcome outcome = serviceOutcome.get();
			printOutcome(outcome);
			if (outcome.failedToDownload > 0) {
				System.exit(1);
			}
		} catch (Throwable t) {
			System.err.println(t);
			System.exit(1);
		}
	}
}
