package co.arcs.groove.basking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.CharStreams;

import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Locale;

import co.arcs.groove.basking.event.Events.BuildSyncPlanFinishedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanProgressChangedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanStartedEvent;
import co.arcs.groove.basking.event.Events.DeleteFilesFinishedEvent;
import co.arcs.groove.basking.event.Events.DeleteFilesStartedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongFinishedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongProgressChangedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongsStartedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsStartedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncFinishedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncStartedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import co.arcs.groove.basking.event.Events.SyncProcessStartedEvent;
import co.arcs.groove.thresher.GroovesharkException.InvalidCredentialsException;
import co.arcs.groove.thresher.GroovesharkException.RateLimitedException;
import co.arcs.groove.thresher.GroovesharkException.ServerErrorException;

import static org.fusesource.jansi.Ansi.ansi;

public class ConsoleLogger {

    private final ObjectMapper objectMapper;
    private final PrintStream out;

    public ConsoleLogger() {
        objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        out = System.out;
        AnsiConsole.systemInstall();
    }

    // Whole sync process

    @Subscribe
    public void onEvent(SyncProcessStartedEvent e) {
        ansiPrintLogo();
        ansiPrintPrimaryLn("Starting sync…");

        // Print config, obfuscating password
        try {
            Config config = new Config(e.getConfig());
            if (config.password != null) {
                config.password = "******";
            }
            ansiPrintSecondaryLn(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e2) {
            throw new RuntimeException(e2);
        }
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedEvent e) {
        String message;
        if (e.getOutcome().getFailedToDownload() == 0) {
            message = "Sync finished successfully.";
        } else {
            message = String.format(Locale.US,
                    "Sync finished with errors: %d item(s) could not be downloaded.",
                    e.getOutcome().getFailedToDownload());
        }
        ansiPrintPrimaryLn(message);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent event) {
        String message;
        if (event.getException().getCause() instanceof InvalidCredentialsException) {
            message = "Username/password combination is not valid.";
        } else if (event.getException() instanceof ServerErrorException) {
            message = "Server is not responding as expected. If this error persists, please file a bug at https://github.com/danhawkes/basking/issues.";
        } else if (event.getException() instanceof RateLimitedException) {
            message = "Your IP has been rate-limited! Please try again later.";
        } else {
            message = event.getException().toString();
        }
        ansiPrintErrorLn(message);
    }

    // Query API for user information

    @Subscribe
    public void onEvent(GetSongsToSyncStartedEvent e) {
        ansiPrintPrimaryLn("Retrieving user data…");
    }

    @Subscribe
    public void onEvent(GetSongsToSyncProgressChangedEvent e) {
        ansiPrintProgress(e.getFraction());
    }

    @Subscribe
    public void onEvent(GetSongsToSyncFinishedEvent e) {
        ansiPrintSecondaryLn("Found " + e.items + " items.");
    }

    // Build sync plan

    @Subscribe
    public void onEvent(BuildSyncPlanStartedEvent e) {
        ansiPrintPrimaryLn("Building sync plan…");
    }

    @Subscribe
    public void onEvent(BuildSyncPlanProgressChangedEvent e) {
        ansiPrintProgress(e.getFraction());
    }

    @Subscribe
    public void onEvent(BuildSyncPlanFinishedEvent e) {
        ansiPrintSecondaryLn(String.format(Locale.US,
                "Download %d, delete %d, and leave %d.",
                e.getToDownload(),
                e.getToDelete(),
                e.getToLeave()));
    }

    // Delete files

    @Subscribe
    public void onEvent(DeleteFilesStartedEvent e) {
        ansiPrintPrimaryLn("Deleting files…");
        ansiPrintProgress(0.0f);
    }

    @Subscribe
    public void onEvent(DeleteFilesFinishedEvent e) {
        ansiPrintProgress(1.0f);
    }

    // Download song

    @Subscribe
    public void onEvent(DownloadSongsStartedEvent e) {
        ansiPrintPrimaryLn("Downloading songs…");
    }

    @Subscribe
    public void onEvent(DownloadSongProgressChangedEvent e) {
        ansiPrintProgress(e.getFraction());
        out.print(ansi().a(" ").a(e.getSong().getArtistName() + " - " + e.getSong().getName()));
    }

    @Subscribe
    public void onEvent(DownloadSongFinishedEvent e) {
        out.print(ansi().newline());
        needNewline = false;
    }

    // Generate playlist

    @Subscribe
    public void onEvent(GeneratePlaylistsStartedEvent e) {
        ansiPrintPrimaryLn("Generating playlists…");
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsProgressChangedEvent e) {
        ansiPrintProgress(e.getFraction());
    }

    // Printers

    private boolean needNewline;

    void ansiPrintPrimaryLn(String message) {
        ansiPrintNewlineIfReqd();
        out.println(ansi().newline()
                .bold()
                .fg(Color.BLUE)
                .a("\u27F6")
                .reset()
                .bold()
                .a("  ")
                .a(message)
                .reset()
                .newline());
        out.flush();
    }

    void ansiPrintErrorLn(String message) {
        ansiPrintNewlineIfReqd();
        out.println(ansi().newline()
                .bold()
                .fg(Color.RED)
                .a("\u27F6")
                .reset()
                .bold()
                .a("  ")
                .a(message)
                .reset()
                .newline());
        out.flush();
    }

    void ansiPrintSecondaryLn(String message) {
        ansiPrintNewlineIfReqd();
        out.print(ansi().a(message).newline());
        out.flush();
    }

    void ansiPrintProgress(float fraction) {
        ansiPrintProgress(fraction, true);
    }

    void ansiPrintProgress(float fraction, boolean carriageReturn) {
        if (carriageReturn) {
            out.print(ansi().a('\r'));
        }
        out.print(ansi().a(ProgressBar.print(fraction)));
        out.flush();
        needNewline = true;
    }

    private void ansiPrintLogo() {
        try {
            InputStream stream = ConsoleLogger.class.getClassLoader()
                    .getResourceAsStream("logo.txt");
            String s = CharStreams.toString(new InputStreamReader(stream));
            out.print(ansi().newline().bold().fg(Color.BLUE).a(s).reset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ansiPrintNewlineIfReqd() {
        if (needNewline) {
            out.print(ansi().newline());
            needNewline = false;
        }
    }

    static class ProgressBar {

        private static final int BAR_LENGTH = 30;
        private static final String BAR_FILLER = Strings.repeat("=", BAR_LENGTH);
        private static final String EMPTY_FILLER = Strings.repeat(" ", BAR_LENGTH);
        private static final String FORMAT = "[%s%s] %3d%%";

        static String print(float fraction) {

            String full = BAR_FILLER.substring(0, (int) (fraction * BAR_FILLER.length()));
            String empty = EMPTY_FILLER.substring(full.length(), BAR_FILLER.length());

            return String.format(Locale.US, FORMAT, full, empty, (int) (fraction * 100.0f));
        }
    }
}
