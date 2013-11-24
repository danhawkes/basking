package co.arcs.groove.basking.task;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import co.arcs.groove.basking.Console;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

public class GeneratePlaylistsTask implements Callable<Void> {

	private final File syncPath;
	private final SyncPlan syncPlan;

	public GeneratePlaylistsTask(File syncPath, SyncPlan syncPlan) {
		this.syncPath = syncPath;
		this.syncPlan = syncPlan;
	}

	@Override
	public Void call() throws Exception {

		Console.log("Generating playlists…");

		StringBuilder collectionStringBuilder = new StringBuilder();
		StringBuilder favoritesStringBuilder = new StringBuilder();

		favoritesStringBuilder.append("#EXTM3U\n");
		collectionStringBuilder.append("#EXTM3U\n");

		for (SyncPlan.Item item : syncPlan.items) {
			if ((item.action == Action.DOWNLOAD) || (item.action == Action.LEAVE)) {
				if (item.songToSync.favorite) {
					writeToPlaylist(favoritesStringBuilder, item);
				}
				writeToPlaylist(collectionStringBuilder, item);
			}
		}

		Files.write(favoritesStringBuilder.toString(), new File(syncPath, "GS Favorites.m3u"),
				Charsets.UTF_8);
		Files.write(collectionStringBuilder.toString(), new File(syncPath, "GS Collection.m3u"),
				Charsets.UTF_8);

		return null;
	}

	private static void writeToPlaylist(StringBuilder sb, SyncPlan.Item item)
			throws UnsupportedTagException, InvalidDataException, IOException {
		Mp3File mp3File = new Mp3File(item.file.getAbsolutePath());
		long length = mp3File.getLengthInSeconds();
		sb.append(String.format("#EXTINF:%d,%s - %s\n%s\n", length, item.songToSync.song.name,
				item.songToSync.song.artistName, item.file.getName()));
	}
}
