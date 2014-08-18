package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import co.arcs.groove.basking.event.Events.DownloadSongsFinishedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongsStartedEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

/**
 * Task that downloads a set of songs.
 *
 * <p>Unlike other tasks, this will succeed even if one or more of its subtasks fail. The rationale
 * for this is that it's common for individual downloads to fail because of transient API issues or
 * connectivity interruptions. The sync process (including updating playlists and caches) is allowed
 * to continue as some progress is better than no progress.</p>
 *
 * <p>A collection of the successfully downloaded songs is returned.</p>
 */
public class DownloadSongsTask implements Task<List<Song>> {

    private final EventPoster bus;
    private final Client client;
    private final List<SyncPlan.Item> syncPlanItems;
    private final File tempPath;
    private final Semaphore concurrentJobsSemaphore;
    private final ListeningExecutorService executor;

    public DownloadSongsTask(EventPoster bus,
            Client client,
            ListeningExecutorService executor,
            File tempPath,
            Semaphore concurrentJobsSemaphore,
            List<SyncPlan.Item> syncPlanItems) {
        this.bus = bus;
        this.client = client;
        this.syncPlanItems = syncPlanItems;
        this.tempPath = tempPath;
        this.concurrentJobsSemaphore = concurrentJobsSemaphore;
        this.executor = executor;
    }

    @Override
    public List<Song> call() throws Exception {

        bus.post(new DownloadSongsStartedEvent(this));

        List<ListenableFuture<Song>> downloadFutures = Lists.newArrayList();
        for (Item item : syncPlanItems) {
            downloadFutures.add(executor.submit(new DownloadSongTask(bus,
                    client,
                    item.getSong(),
                    item.getFile(),
                    tempPath,
                    concurrentJobsSemaphore)));
        }

        List<Song> result = Lists.newArrayList(Futures.successfulAsList(downloadFutures).get());
        result.removeAll(Collections.singleton(null));

        bus.post(new DownloadSongsFinishedEvent(this));

        return result;
    }
}
