package co.arcs.groove.basking.task;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import co.arcs.groove.basking.Log;
import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.basking.task.GetSongsToSyncTask.SongToSync;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class BuildSyncPlanTask implements Callable<SyncPlan> {

	public static class SyncPlan {

		public final ImmutableList<Item> items;
		public final int download;
		public final int delete;
		public final int leave;

		public SyncPlan(List<Item> items) {
			this.items = ImmutableList.copyOf(items);
			int delete = 0;
			int download = 0;
			int leave = 0;
			for (Item item : items) {
				if (item.action == Action.DELETE) {
					delete++;
				} else if (item.action == Action.DOWNLOAD) {
					download++;
				} else if (item.action == Action.LEAVE) {
					leave++;
				}
			}
			this.download = download;
			this.delete = delete;
			this.leave = leave;
		}

		public static class Item {

			public static enum Action {
				DOWNLOAD, DELETE, LEAVE
			}

			public final File file;
			public final Action action;
			public final SongToSync songToSync;

			public Item(File file, Action action, @Nullable SongToSync songToSync) {
				this.file = file;
				this.action = action;
				this.songToSync = songToSync;
			}
		}
	}

	private final File syncPath;
	private final List<SongToSync> songsToSync;

	public BuildSyncPlanTask(File syncPath, List<SongToSync> songsToSync) {
		this.syncPath = syncPath;
		this.songsToSync = songsToSync;
	}

	@Override
	public SyncPlan call() throws Exception {
		List<AbstractSongTask> items = Lists.newArrayList();

		// Only deal with mp3 files
		List<File> files = Lists.newArrayList(syncPath.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(SyncService.FINISHED_FILE_EXTENSION);
			}
		}));

		int delete = 0;
		int download = 0;
		int leave = 0;
		ImmutableList.Builder<SyncPlan.Item> syncPlan = ImmutableList.builder();

		// For existing files…
		out: for (File f : files) {
			long id = Utils.decodeId(f);
			if (id == -1) {
				// …if not a managed file: delete
				items.add(new DeleteSongTask(f));
				syncPlan.add(new Item(f, Action.DELETE, null));
				delete++;
			} else {
				Iterator<SongToSync> iterator = songsToSync.iterator();
				SongToSync songToSync = null;
				while (iterator.hasNext()) {
					songToSync = iterator.next();
					if (songToSync.song.id == id) {
						// …if song already in library: ignore
						syncPlan.add(new Item(f, Action.LEAVE, songToSync));
						leave++;
						// Remove from sync list. Anything left in this list
						// after the loop is not in the library and needs
						// downloading.
						iterator.remove();
						continue out;
					}
				}
				// …if a managed file, but not in library: delete
				items.add(new DeleteSongTask(f));
				delete++;
				syncPlan.add(new Item(f, Action.DELETE, songToSync));
			}
		}

		for (SongToSync songToSync : songsToSync) {
			syncPlan.add(new Item(new File(syncPath, Utils.getDiskName(songToSync.song)),
					Action.DOWNLOAD, songToSync));
			download++;
		}

		Log.d(String.format(Locale.US,
				"…built sync plan: Will download %d items, delete %d, and leave %d untouched",
				download, delete, leave));

		return new SyncPlan(syncPlan.build());
	}
}