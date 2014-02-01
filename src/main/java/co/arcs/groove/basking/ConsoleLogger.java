package co.arcs.groove.basking;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;

import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;

import co.arcs.groove.basking.event.impl.BuildSyncPlanEvent;
import co.arcs.groove.basking.event.impl.DeleteFileEvent;
import co.arcs.groove.basking.event.impl.DownloadSongEvent;
import co.arcs.groove.basking.event.impl.GeneratePlaylistsEvent;
import co.arcs.groove.basking.event.impl.GetSongsToSyncEvent;
import co.arcs.groove.basking.event.impl.SyncTaskEvent;
import co.arcs.groove.thresher.Song;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.CharStreams;

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
	public void onEvent(SyncTaskEvent.Started e) {
		ansiPrintLogo();
		ansiPrintPrimaryLn("Starting sync…");
		
		// Print config, obfuscating password
		try {
			Config config = new Config(e.task.config);
			if (config.password != null) {
				config.password = "******";
			}
			ansiPrintSecondaryLn(objectMapper.writeValueAsString(config));
		} catch (JsonProcessingException e2) {
			throw new RuntimeException(e2);
		}
	}

	@Subscribe
	public void onEvent(SyncTaskEvent.Finished e) {

		ansiPrintPrimaryLn("Finished sync.");
	}

	// Query API for use information

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.Started e) {
		ansiPrintPrimaryLn("Retrieving user data…");
	}

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.ProgressChanged e) {
		ansiPrintProgress(e.fraction);
	}

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.Finished e) {
		ansiPrintSecondaryLn("Found " + e.items + " items.");
	}

	// Build sync plan

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.Started e) {
		ansiPrintPrimaryLn("Building sync plan…");
	}

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.ProgressChanged e) {
		ansiPrintProgress(e.fraction);
	}

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.Finished e) {
		ansiPrintSecondaryLn(String.format(Locale.US, "Download %d, delete %d, and leave %d.",
				e.download, e.delete, e.leave));
	}

	// Delete files

	boolean startedDeletion;

	@Subscribe
	public void onEvent(DeleteFileEvent.Started e) {
		if (!startedDeletion) {
			ansiPrintPrimaryLn("Deleting files…");
			startedDeletion = true;
		}
	}

	@Subscribe
	public void onEvent(DeleteFileEvent.Finished e) {
		ansiPrintProgress(1.0f);
	}

	// Download song

	boolean startedDownload;

	Map<Song, Float> downloadingSongs = Maps.newLinkedHashMap();

	@Subscribe
	public void onEvent(DownloadSongEvent.Started e) {
		if (!startedDownload) {
			ansiPrintPrimaryLn("Downloading songs…");
			startedDownload = true;
		}
	}

	@Subscribe
	public void onEvent(DownloadSongEvent.ProgressChanged e) {
		ansiPrintProgress(e.fraction);
		out.print(ansi().a(" ").a(e.task.song.artistName + " - " + e.task.song.name));
	}

	@Subscribe
	public void onEvent(DownloadSongEvent.Finished e) {
		out.print(ansi().newline());
		needNewline = false;
	}

	// Generate playlist

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.Started e) {
		ansiPrintPrimaryLn("Generating playlists…");
	}

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.ProgressChanged e) {
		ansiPrintProgress(e.fraction);
	}

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.Finished e) {
	}

	//

	@Subscribe
	public void onEvent(DeadEvent e) {
		System.err.println("Dead event: " + e.getEvent().toString());
	}

	// Printers

	boolean needNewline;

	void ansiPrintPrimaryLn(String message) {
		ansiPrintNewlineIfReqd();
		out.println(ansi().newline().bold().fg(Color.BLUE).a("\u27F6").reset().bold().a("  ")
				.a(message).reset().newline());
		out.flush();
	}

	void ansiPrintSecondary(String message) {
		ansiPrintNewlineIfReqd();
		out.print(ansi().a(message));
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
			InputStream stream = ConsoleLogger.class.getClassLoader().getResourceAsStream(
					"logo.txt");
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
