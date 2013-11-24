package co.arcs.groove.basking;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {

	@JsonProperty("syncDirectory")
	@Parameter(
			names = { "-dir", "--sync-dir" },
			required = true,
			description = "Directory to sync. Will be created if it does not already exist.")
	public File syncDir;

	@JsonProperty("username")
	@Parameter(
			names = { "-user", "--username" },
			description = "Grooveshark user name.",
			required = true)
	public String username;

	@JsonProperty("password")
	@Parameter(
			names = { "-pass", "--password" },
			description = "Grooveshark user password.",
			required = true)
	public String password;

	@JsonProperty("numConcurrentDownloads")
	@Parameter(
			validateWith = PositiveInteger.class,
			names = { "-num", "--num-concurrent" },
			description = "Number of concurrent downloads.")
	public int numConcurrent = 1;

	@JsonIgnore
	@Parameter(names = { "-cfg", "--config" }, description = "JSON configuration file to load.")
	public File configFile;

	@JsonProperty("dryRun")
	@Parameter(names = { "-dry", "--dry-run" }, description = "Do not modify the disk.")
	public boolean dryRun = false;

	@Parameter(names = { "-h", "--help" }, description = "Show this help.", help = true)
	public boolean help;
}
