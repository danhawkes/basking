package co.arcs.groove.basking.task;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import co.arcs.groove.basking.Console;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;
import co.arcs.groove.thresher.User;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class GetSongsToSyncTask implements Callable<List<GetSongsToSyncTask.SongToSync>> {

	public static class SongToSync {

		final Song song;
		final boolean favorite;

		public SongToSync(Song song, boolean favorite) {
			this.song = song;
			this.favorite = favorite;
		}
	}

	private final Client client;
	private final String username;
	private final String password;

	public GetSongsToSyncTask(Client client, String username, String password) {
		this.client = client;
		this.username = username;
		this.password = password;
	}

	@Override
	public List<GetSongsToSyncTask.SongToSync> call() throws Exception {
		Console.log("Getting songs to sync…");
		User user = client.login(username, password);
		ImmutableSet<Song> librarySongs = ImmutableSet.copyOf(user.library.get());
		ImmutableSet<Song> favoriteSongs = ImmutableSet.copyOf(user.favorites.get());
		Set<Song> nonfavoriteSongs = Sets.difference(librarySongs, favoriteSongs);
		List<GetSongsToSyncTask.SongToSync> songsToSync = Lists.newArrayList();
		for (Song s : favoriteSongs) {
			songsToSync.add(new SongToSync(s, true));
		}
		for (Song s : nonfavoriteSongs) {
			songsToSync.add(new SongToSync(s, false));
		}
		Console.logIndent("Found " + songsToSync.size() + " items");
		return songsToSync;
	}
}