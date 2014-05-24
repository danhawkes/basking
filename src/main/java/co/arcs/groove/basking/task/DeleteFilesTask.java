package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.File;
import java.util.List;

import co.arcs.groove.basking.event.impl.DeleteFilesEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;

public class DeleteFilesTask implements Task<List<File>> {

    private final EventBus bus;
    private final List<SyncPlan.Item> items;
    private final ListeningExecutorService executor;

    public DeleteFilesTask(EventBus bus, ListeningExecutorService executor, List<Item> items) {
        this.bus = bus;
        this.items = items;
        this.executor = executor;
    }

    @Override
    public List<File> call() throws Exception {

        bus.post(new DeleteFilesEvent.Started(this));

        List<ListenableFuture<File>> deleteFutures = Lists.newArrayList();
        for (Item item : items) {
            deleteFutures.add(executor.submit(new DeleteFileTask(bus, item.file)));
        }
        List<File> result = Futures.allAsList(deleteFutures).get();

        bus.post(new DeleteFilesEvent.Finished(this));

        return result;
    }
}
