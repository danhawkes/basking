package co.arcs.groove.basking;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.EventPoster;
import co.arcs.groove.basking.task.SyncTask;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import rx.Observable;
import rx.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncOperation {

    public static final String FINISHED_FILE_EXTENSION = ".mp3";
    public static final String TEMP_FILE_EXTENSION_1 = ".tmp.1";
    public static final String TEMP_FILE_EXTENSION_2 = ".tmp.2";

    private final Config config;
    private final PublishSubject<TaskEvent> subject = PublishSubject.create();
    private final EventBus bus;
    private final ListeningExecutorService exec;
    protected final EventPoster poster;
    private ListenableFuture<SyncTask.Outcome> syncOutcomeFuture;

    public SyncOperation(Config config) {
        this(config, new EventBus(SyncOperation.class.getName()));
    }

    @Deprecated
    public SyncOperation(Config config, final EventBus bus) {
        this.bus = checkNotNull(bus);
        this.config = checkNotNull(config);
        this.poster = new EventPoster() {
            @Override
            public void post(TaskEvent e) {
                subject.onNext(e);
                bus.post(e);
            }

            @Override
            public void postBusOnly(TaskEvent e) {
                bus.post(e);
            }

            @Override
            public void postCompleted() {
                subject.onCompleted();
            }

            @Override
            public void postError(Throwable t) {
                subject.onError(t);
            }
        };

        this.exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                config.numConcurrent + 3));
    }

    public final SyncOperation start() {
        syncOutcomeFuture = startFuture();
        return this;
    }

    protected ListenableFuture<Outcome> startFuture() {
        ListenableFuture<Outcome> future = exec.submit(new SyncTask(poster, exec, config));

        // Shut down executor on completion
        Futures.addCallback(future, new FutureCallback<Outcome>() {

            @Override
            public void onSuccess(Outcome outcome) {
                exec.shutdown();
            }

            @Override
            public void onFailure(Throwable t) {
                exec.shutdown();
            }
        }, MoreExecutors.sameThreadExecutor());

        return future;
    }

    @Nullable
    public ListenableFuture<Outcome> getFuture() {
        return syncOutcomeFuture;
    }

    @Deprecated
    public EventBus getEventBus() {
        return bus;
    }

    public Observable<TaskEvent> getObservable() {
        return subject.asObservable();
    }
}
