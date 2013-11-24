package co.arcs.groove.basking;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.util.concurrent.ListenableFuture;

public class Cli {

	public static void main(String[] args) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		Config config = new Config();

		JCommander jc = new JCommander(config);
		jc.setProgramName("basking");

		try {
			// Do the initial parse. This will fail if any required parameters
			// are missing, but will nevertheless populate any that succeed.
			jc.parse(args);
		} catch (ParameterException e) {
			// Only consume the exception if the error is from too few
			// parameters, as that will be shown later if thers's still an issue
			// after loading the config file.
			if (!e.getMessage().startsWith("The following options are required")) {
				exit1(jc, e);
			}
		}
		if (config.help) {
			jc.usage();
			return;
		}
		// Load the config file, if present, and insert additional arguments
		if (config.configFile != null) {
			List<String> configArgs = Lists.newArrayList();
			try {
				JsonNode root = objectMapper.readTree(config.configFile);
				JsonNode numConcurrentDownloadsNode = root.get("numConcurrentDownloads");
				if (numConcurrentDownloadsNode != null) {
					configArgs.add("--num-concurrent");
					configArgs.add(numConcurrentDownloadsNode.asText());
				}
				JsonNode dryRunNode = root.get("dryRun");
				if (dryRunNode != null) {
					if (dryRunNode.asBoolean()) {
						configArgs.add("--dry-run");
					}
				}
				JsonNode syncDirectoryNode = root.get("syncDirectory");
				if (syncDirectoryNode != null) {
					configArgs.add("--sync-dir");
					configArgs.add(syncDirectoryNode.asText());
				}
				JsonNode usernameNode = root.get("username");
				if (usernameNode != null) {
					configArgs.add("--username");
					configArgs.add(usernameNode.asText());
				}
				JsonNode passwordNode = root.get("password");
				if (passwordNode != null) {
					configArgs.add("--password");
					configArgs.add(passwordNode.asText());
				}
				args = configArgs.toArray(new String[configArgs.size()]);
			} catch (JsonMappingException e1) {
				exit1(jc, e1);
				return;
			} catch (JsonParseException e1) {
				exit1(jc, e1);
				return;
			} catch (IOException e1) {
				exit1(jc, e1);
				return;
			}
		}
		// Parse again, hopefully with a complete set of arguments
		try {
			jc.parse(args);
			run(config);
		} catch (ParameterException e) {
			exit1(jc, e);
		}
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

	private static void printOutcome(SyncOutcome outcome) {
		System.out.println("Finished sync task. Downloaded " + outcome.downloaded + " of "
				+ (outcome.downloaded + outcome.failedToDownload) + " items.");
	}

	private static void exit1(JCommander jc, Exception e1) {
		System.err.println(e1.getMessage());
		jc.usage();
		System.exit(1);
	}
}
