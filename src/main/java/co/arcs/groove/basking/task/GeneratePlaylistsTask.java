package co.arcs.groove.basking.task;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import co.arcs.groove.basking.Console;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;

import com.beust.jcommander.internal.Lists;
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

		List<SyncPlan.Item> collectionItems = Lists.newArrayList();
		List<SyncPlan.Item> favoriteItems = Lists.newArrayList();

		for (SyncPlan.Item item : syncPlan.items) {
			if ((item.action == Action.DOWNLOAD) || (item.action == Action.LEAVE)) {
				collectionItems.add(item);
				if (item.songToSync.favorite) {
					favoriteItems.add(item);
				}
			}
		}

		Collections.sort(favoriteItems, new Comparator<SyncPlan.Item>() {

			@Override
			public int compare(Item o1, Item o2) {
				return o2.songToSync.song.timeFavorited.compareTo(o1.songToSync.song.timeFavorited);
			}
		});
		Collections.sort(collectionItems, new Comparator<SyncPlan.Item>() {

			@Override
			public int compare(Item o1, Item o2) {
				// Favorites are a sub-set of the collection, so some items in
				// the collection do not have 'timeAdded' timestamps.
				String t2 = (o2.songToSync.song.timeAdded != null) ? o2.songToSync.song.timeAdded
						: o2.songToSync.song.timeFavorited;
				String t1 = (o1.songToSync.song.timeAdded != null) ? o1.songToSync.song.timeAdded
						: o1.songToSync.song.timeFavorited;
				return t2.compareTo(t1);
			}
		});

		writePlaylist(new File(syncPath, "GS Favorites.m3u"), favoriteItems);
		writePlaylist(new File(syncPath, "GS Collection.m3u"), collectionItems);

		return null;
	}

	private static void writePlaylist(File playlistFile, List<SyncPlan.Item> items)
			throws IOException, UnsupportedTagException, InvalidDataException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("#EXTM3U\n\n");
		for (SyncPlan.Item item : items) {
			writeToPlaylist(sb, item);
		}
		Files.write(sb.toString(), playlistFile, Charsets.UTF_8);
	}

	private static void writeToPlaylist(StringBuilder sb, SyncPlan.Item item)
			throws UnsupportedTagException, InvalidDataException, IOException {
		
		long len = new Mp3File(item.file.getAbsolutePath()).getLengthInSeconds();
		sb.append(String.format("#EXTINF:%d,%s - %s\n%s\n\n", len, item.songToSync.song.name,
				item.songToSync.song.artistName, item.file.getName()));
	}
}
