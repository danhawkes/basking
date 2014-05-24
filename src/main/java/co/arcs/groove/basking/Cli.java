package co.arcs.groove.basking;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import co.arcs.groove.basking.task.SyncTask.Outcome;

public class Cli {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

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

        if (config.version) {
            try {
                Properties p = new Properties();
                InputStream is = Cli.class.getResourceAsStream(
                        "/META-INF/maven/co.arcs.groove/basking/pom.properties");
                p.load(is);
                String version = p.getProperty("version", null);
                System.out.println("basking v" + version);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
            new Cli(config);
        } catch (ParameterException e) {
            exit1(jc, e);
        }
    }

    private static void exit1(JCommander jc, Exception e1) {
        System.err.println(e1.getMessage());
        jc.usage();
        System.exit(1);
    }

    private final SyncService syncService;
    private final ConsoleLogger consoleLogger;

    public Cli(Config config) {
        consoleLogger = new ConsoleLogger();

        syncService = new SyncService();
        syncService.getEventBus().register(consoleLogger);

        ListenableFuture<Outcome> serviceOutcome = syncService.start(config);

        try {
            Outcome outcome = serviceOutcome.get();
            if (outcome.failedToDownload > 0) {
                System.exit(1);
            }
        } catch (Throwable t) {
            System.err.println(t);
            System.exit(1);
        }
    }
}
