package co.arcs.groove.basking.task;

import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;

import co.arcs.groove.basking.event.impl.DownloadSongsEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

import com.beust.jcommander.internal.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DownloadSongsTask implements Task<List<Song>> {

	private final EventBus bus;
	private final Client client;
	private final List<SyncPlan.Item> syncPlanItems;
	private final File tempPath;
	private final Semaphore concurrentJobsSemaphore;
	private final ListeningExecutorService executor;

	public DownloadSongsTask(EventBus bus, Client client, ListeningExecutorService executor,
			File tempPath, Semaphore concurrentJobsSemaphore, List<SyncPlan.Item> syncPlanItems) {
		this.bus = bus;
		this.client = client;
		this.syncPlanItems = syncPlanItems;
		this.tempPath = tempPath;
		this.concurrentJobsSemaphore = concurrentJobsSemaphore;
		this.executor = executor;
	}

	@Override
	public List<Song> call() throws Exception {

		bus.post(new DownloadSongsEvent.Started(this));

		List<ListenableFuture<Song>> downloadFutures = Lists.newArrayList();
		for (Item item : syncPlanItems) {
			downloadFutures.add(executor.submit(new DownloadSongTask(bus, client, item.song,
					item.file, tempPath, concurrentJobsSemaphore)));
		}
		List<Song> result = Futures.successfulAsList(downloadFutures).get();

		bus.post(new DownloadSongsEvent.Finished(this));

		return result;
	}
}
