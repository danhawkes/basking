package co.arcs.groove.basking.task;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.event.impl.BuildSyncPlanEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.thresher.Song;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

public class BuildSyncPlanTask implements Task<SyncPlan> {

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
			public final Song song;

			public Item(File file, Action action, @Nullable Song songToSync) {
				this.file = file;
				this.action = action;
				this.song = songToSync;
			}
		}
	}

	private final EventBus bus;
	private final File syncPath;
	private final Set<Song> songs;

	public BuildSyncPlanTask(EventBus bus, File syncPath, Set<Song> songs) {
		this.bus = bus;
		this.syncPath = syncPath;
		this.songs = songs;
	}

	@Override
	public SyncPlan call() throws Exception {

		bus.post(new BuildSyncPlanEvent.Started(this));

		List<Task<Void>> items = Lists.newArrayList();

		// Only deal with mp3 files
		List<File> files = Lists.newArrayList(syncPath.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(SyncService.FINISHED_FILE_EXTENSION);
			}
		}));

		int progress = 0;
		bus.post(new BuildSyncPlanEvent.ProgressChanged(this, progress++, files.size()));

		ImmutableList.Builder<SyncPlan.Item> syncPlanItems = ImmutableList.builder();

		// For existing files…
		out: for (File f : files) {
			bus.post(new BuildSyncPlanEvent.ProgressChanged(this, progress++, files
					.size()));
			long id = Utils.decodeId(f);
			if (id == -1) {
				// …if not a managed file: delete
				items.add(new DeleteFileTask(bus, f));
				syncPlanItems.add(new Item(f, Action.DELETE, null));
			} else {
				Iterator<Song> iterator = songs.iterator();
				Song song = null;
				while (iterator.hasNext()) {
					song = iterator.next();
					if (song.id == id) {
						// …if song already in library: ignore
						syncPlanItems.add(new Item(f, Action.LEAVE, song));
						// Remove from sync list. Anything left in this list
						// after the loop is not in the library and needs
						// downloading.
						iterator.remove();
						continue out;
					}
				}
				// …if a managed file, but not in library: delete
				items.add(new DeleteFileTask(bus, f));
				syncPlanItems.add(new Item(f, Action.DELETE, song));
			}
		}

		for (Song song : songs) {
			syncPlanItems.add(new Item(new File(syncPath, Utils.getDiskName(song)),
					Action.DOWNLOAD, song));
		}

		SyncPlan syncPlan = new SyncPlan(syncPlanItems.build());

		bus.post(new BuildSyncPlanEvent.Finished(this, syncPlan.download, syncPlan.delete, syncPlan.leave));

		return syncPlan;
	}
}