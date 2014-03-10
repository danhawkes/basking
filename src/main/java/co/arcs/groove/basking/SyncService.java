package co.arcs.groove.basking;

import java.util.concurrent.Executors;

import co.arcs.groove.basking.task.SyncTask;
import co.arcs.groove.basking.task.SyncTask.Outcome;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SyncService {

	private final EventBus bus;
	private final Config config;

	public static final String FINISHED_FILE_EXTENSION = ".mp3";
	public static final String TEMP_FILE_EXTENSION_1 = ".tmp.1";
	public static final String TEMP_FILE_EXTENSION_2 = ".tmp.2";

	public SyncService(Config config) {
		this(config, new EventBus(SyncService.class.getName()));
	}

	public SyncService(Config config, EventBus bus) {
		this.config = config;
		this.bus = bus;
	}

	public EventBus getEventBus() {
		return bus;
	}

	public Config getConfig() {
		return config;
	}

	public ListenableFuture<SyncTask.Outcome> start() {

		final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors
				.newFixedThreadPool(config.numConcurrent + 3));

		ListenableFuture<SyncTask.Outcome> syncOutcomeFuture = exec.submit(new SyncTask(bus, exec,
				config));

		// Shut down executor on completion
		Futures.addCallback(syncOutcomeFuture, new FutureCallback<Outcome>() {

			@Override
			public void onSuccess(Outcome result) {
				exec.shutdown();
			}

			@Override
			public void onFailure(Throwable t) {
				exec.shutdown();
			}
		}, MoreExecutors.sameThreadExecutor());

		return syncOutcomeFuture;
	}
}
