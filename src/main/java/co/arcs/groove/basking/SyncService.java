package co.arcs.groove.basking;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

import co.arcs.groove.basking.task.BuildSyncPlanTask;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.basking.task.CreateDirectoriesTask;
import co.arcs.groove.basking.task.DeleteSongTask;
import co.arcs.groove.basking.task.DeleteTemporariesTask;
import co.arcs.groove.basking.task.DownloadSongTask;
import co.arcs.groove.basking.task.GeneratePlaylistsTask;
import co.arcs.groove.basking.task.GetSongsToSyncTask;
import co.arcs.groove.basking.task.GetSongsToSyncTask.SongToSync;
import co.arcs.groove.thresher.Client;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SyncService {

	private final Config config;
	private final File tempPath;
	private final Client client;

	public static final String FINISHED_FILE_EXTENSION = ".mp3";
	public static final String TEMP_FILE_EXTENSION_1 = ".tmp.1";
	public static final String TEMP_FILE_EXTENSION_2 = ".tmp.2";

	public SyncService(Config cli) {
		this.config = cli;
		this.tempPath = new File(cli.syncDir, ".gssync");
		this.client = new Client();
	}

	@SuppressWarnings("unchecked")
	public ListenableFuture<SyncOutcome> start() {

		final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors
				.newFixedThreadPool(config.numConcurrent));

		// Do some clean up before starting
		ListenableFuture<Void> createdRequiredDirectoriesFuture = (ListenableFuture<Void>) ((config.dryRun) ? Futures
				.immediateFuture(null) : exec.submit(new CreateDirectoriesTask(config.syncDir,
				tempPath)));

		ListenableFuture<Void> deletedTemporariesFuture = Futures.transform(
				createdRequiredDirectoriesFuture, new AsyncFunction<Void, Void>() {

					@Override
					public ListenableFuture<Void> apply(Void input) throws Exception {
						if (config.dryRun) {
							return Futures.immediateFuture(null);
						} else {
							return exec.submit(new DeleteTemporariesTask(tempPath));
						}
					}
				});

		// Obtain the user's library/favorites from the API
		ListenableFuture<List<SongToSync>> getSongsToSyncFuture = Futures.transform(
				deletedTemporariesFuture, new AsyncFunction<Void, List<SongToSync>>() {

					@Override
					public ListenableFuture<List<SongToSync>> apply(Void input) throws Exception {
						return exec.submit(new GetSongsToSyncTask(client, config.username,
								config.password));
					}
				});

		// Build a plan of what to do
		final ListenableFuture<SyncPlan> buildSyncPlanFuture = Futures.transform(
				getSongsToSyncFuture, new AsyncFunction<List<SongToSync>, SyncPlan>() {

					@Override
					public ListenableFuture<SyncPlan> apply(List<SongToSync> songToSync)
							throws Exception {
						return exec.submit(new BuildSyncPlanTask(config.syncDir, songToSync));
					}
				});

		// Schedule delete tasks. (These need to happen before the downloads as
		// the files may overlap)
		final ListenableFuture<List<Void>> deleteSongsFuture = Futures.transform(
				buildSyncPlanFuture, new AsyncFunction<SyncPlan, List<Void>>() {

					@Override
					public ListenableFuture<List<Void>> apply(SyncPlan syncPlan) throws Exception {
						List<ListenableFuture<Void>> taskFutures = Lists.newArrayList();
						if (!config.dryRun) {
							Iterable<Item> items = Iterables.filter(syncPlan.items,
									new Predicate<SyncPlan.Item>() {

										@Override
										public boolean apply(Item input) {
											return input.action == Action.DELETE;
										}
									});
							for (Item i : items) {
								taskFutures.add(exec.submit(new DeleteSongTask(i.file)));
							}
						}
						return Futures.allAsList(taskFutures);
					}
				});

		// Schedule download tasks
		final ListenableFuture<List<Void>> downloadSongsFuture = Futures.transform(
				Futures.allAsList(buildSyncPlanFuture, deleteSongsFuture),
				new AsyncFunction<List<Object>, List<Void>>() {

					@Override
					public ListenableFuture<List<Void>> apply(List<Object> input) throws Exception {
						SyncPlan syncPlan = buildSyncPlanFuture.get();
						List<ListenableFuture<Void>> taskFutures = Lists.newArrayList();
						if (!config.dryRun) {
							for (SyncPlan.Item item : syncPlan.items) {
								if (item.action == Action.DOWNLOAD) {
									taskFutures.add(exec.submit(new DownloadSongTask(client,
											item.songToSync, item.file, tempPath)));
								}
							}
						}
						return Futures.successfulAsList(taskFutures);
					}
				});

		// Schedule write playlist task
		final ListenableFuture<Void> generatePlaylistsFuture = Futures.transform(
				Futures.allAsList(buildSyncPlanFuture, downloadSongsFuture),
				new AsyncFunction<List<Object>, Void>() {

					@Override
					public ListenableFuture<Void> apply(List<Object> input) throws Exception {
						if (config.dryRun) {
							return Futures.immediateFuture(null);
						} else {
							SyncPlan syncPlan = (SyncPlan) input.get(0);
							return exec.submit(new GeneratePlaylistsTask(config.syncDir, syncPlan));
						}
					}
				});

		// Aggregate tasks to report metrics
		ListenableFuture<SyncOutcome> syncOutcomeFuture = Futures.transform(
				Futures.allAsList(deleteSongsFuture, downloadSongsFuture, generatePlaylistsFuture),
				new Function<List<Object>, SyncOutcome>() {

					@Override
					public SyncOutcome apply(List<Object> input) {
						int deleted = Futures.getUnchecked(deleteSongsFuture).size();
						int downloaded = Futures.getUnchecked(downloadSongsFuture).size();
						int failedToDownload = Futures.getUnchecked(buildSyncPlanFuture).download
								- downloaded;
						return new SyncOutcome(deleted, downloaded, failedToDownload);
					}
				});

		// Shut down executor on completion
		Futures.addCallback(syncOutcomeFuture, new FutureCallback<SyncOutcome>() {

			@Override
			public void onSuccess(SyncOutcome result) {
				exec.shutdown();
			}

			@Override
			public void onFailure(Throwable t) {
				exec.shutdown();
			}
		});

		return syncOutcomeFuture;
	}
}
