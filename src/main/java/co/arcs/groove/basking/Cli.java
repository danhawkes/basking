package co.arcs.groove.basking;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import co.arcs.groove.basking.task.SyncTask.Outcome;

public class Cli {

    private static List<String> parseConfigFile(File file) throws IOException {
        List<String> configArgs = Lists.newArrayList();
        JsonNode root = new ObjectMapper().readTree(file);
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
        return configArgs;
    }

    public static void main(String[] argsArr) {

        Config config = new Config();
        JCommander jc = new JCommander(config);
        jc.setProgramName("basking");

        List<String> args = Lists.newArrayList(argsArr);

        // Attempt to read parameters from the config file
        int configFileParamIndex;
        if (((configFileParamIndex = args.indexOf(Config.CONFIG_ARG_SHORT)) > 0) || ((configFileParamIndex = args
                .indexOf(Config.CONFIG_ARG_LONG)) > 0)) {
            if (args.size() > configFileParamIndex) {
                // There's a valid config file parameter
                String configFileParamKey = args.get(configFileParamIndex);
                String configFileParamValue = args.get(configFileParamIndex + 1);
                try {
                    // Create synthetic arguments from the file
                    List<String> configFileParams = parseConfigFile(new File(configFileParamValue));

                    // Append them to the main args list. Also remove references to the config file.
                    args.remove(configFileParamKey);
                    args.remove(configFileParamValue);
                    args.addAll(configFileParams);
                } catch (IOException e) {
                    exit1(jc,
                            new ParameterException("Could not read file '" + configFileParamValue + "'."));
                    return;
                }
            }
        }

        // Parse
        try {
            jc.parse(args.toArray(new String[args.size()]));
        } catch (ParameterException e) {
            exit1(jc, e);
            return;
        }

        // Special cases
        if (config.help) {
            jc.usage();
            return;
        } else if (config.version) {
            try {
                Properties p = new Properties();
                InputStream is = Cli.class.getResourceAsStream(
                        "/META-INF/maven/co.arcs.groove/basking/pom.properties");
                p.load(is);
                String version = p.getProperty("version", null);
                System.out.println("basking v" + version);
            } catch (IOException e) {
                Throwables.propagate(e);
            }
            return;
        }

        new Runner(config).run();
    }

    private static void exit1(JCommander jc, Exception e1) {
        System.err.println(e1.getMessage());
        jc.usage();
        System.exit(1);
    }

    private static class Runner {

        private final Config config;

        public Runner(Config config) {
            this.config = config;
        }

        void run() {
            // Disable other logging
            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(rootLogger.getHandlers()[0]);

            SyncService syncService = new SyncService();

            ConsoleLogger consoleLogger = new ConsoleLogger();
            syncService.getEventBus().register(consoleLogger);

            ListenableFuture<Outcome> serviceOutcome = syncService.start(config);

            try {
                Outcome outcome = serviceOutcome.get();
                if (outcome.failedToDownload > 0) {
                    System.exit(1);
                }
            } catch (Throwable t) {
                System.exit(1);
            }
        }
    }
}
