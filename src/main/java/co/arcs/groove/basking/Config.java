package co.arcs.groove.basking;

import java.io.File;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class Config {

	@Argument(
			metaVar = "<dir>",
			usage = "Directory to sync. Will be created if it does not already exist.",
			index = 0,
			required = true)
	public File syncPath;

	@Argument(
			metaVar = "<username>",
			usage = "Grooveshark user name",
			index = 1,
			required = true)
	public String username;

	@Argument(
			metaVar = "<password>",
			usage = "Grooveshark user password",
			index = 2,
			required = true)
	public String password;

	@Option(
			name = "-n",
			aliases = { "--num-concurrent" },
			usage = "Number of concurrent downloads. Defaults to 1.")
	public int concurrentDownloads = 1;

	@Option(name = "-d", aliases = { "--dry-run" }, usage = "Do not modify the disk")
	public boolean dryRun = false;

	@Option(name = "-h", aliases = { "--help" }, usage = "Show this help page")
	public boolean help = false;
}