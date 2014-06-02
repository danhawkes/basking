package co.arcs.groove.basking.task;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import co.arcs.groove.basking.Config;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import co.arcs.groove.basking.event.Events.SyncProcessProgressChangedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessStartedEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

public class SyncTask implements Task<SyncTask.Outcome> {

    public static class Outcome {

        private final int deleted;
        private final int downloaded;
        private final int failedToDownload;

        public Outcome(int deleted, int downloaded, int failedToDownload) {
            this.deleted = deleted;
            this.downloaded = downloaded;
            this.failedToDownload = failedToDownload;
        }

        public int getDeleted() {
            return deleted;
        }

        public int getDownloaded() {
            return downloaded;
        }

        public int getFailedToDownload() {
            return failedToDownload;
        }
    }

    private final Config config;
    private final EventBus bus;
    private final ListeningExecutorService exec;
    private final File tempPath;
    private final Client client;
    private final Semaphore concurrentJobsSemaphore;
    private static final int NUM_STEPS = 6;

    public SyncTask(EventBus bus, ListeningExecutorService exec, Config config) {
        this.bus = bus;
        this.exec = exec;
        this.config = config;
        this.tempPath = new File(config.syncDir, ".gssync");
        this.client = new Client();
        this.concurrentJobsSemaphore = new Semaphore(config.numConcurrent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Outcome call() throws Exception {

        bus.post(new SyncProcessStartedEvent(this, config));

        try {

            postProgressEvent(0);

            // Ensure sync and temp directories exist
            ListenableFuture<Void> createdRequiredDirectoriesFuture = (ListenableFuture<Void>) ((config.dryRun) ? Futures
                    .immediateFuture(null) : exec.submit(new CreateDirectoriesTask(config.syncDir,
                    tempPath)));

            // Do some clean up before starting
            ListenableFuture<Void> deletedTemporariesFuture = Futures.transform(
                    createdRequiredDirectoriesFuture,
                    new AsyncFunction<Void, Void>() {

                        @Override
                        public ListenableFuture<Void> apply(Void input) throws Exception {
                            if (config.dryRun) {
                                return Futures.immediateFuture(null);
                            } else {
                                return exec.submit(new DeleteTemporariesTask(tempPath));
                            }
                        }
                    }
            );

            // Obtain the user's library/favorites from the API
            ListenableFuture<Set<Song>> getSongsToSyncFuture = Futures.transform(
                    deletedTemporariesFuture,
                    new AsyncFunction<Void, Set<Song>>() {

                        @Override
                        public ListenableFuture<Set<Song>> apply(Void input) throws Exception {
                            postProgressEvent(1);
                            return exec.submit(new GetSongsToSyncTask(bus,
                                    client,
                                    config.username,
                                    config.password));
                        }
                    }
            );

            // Build a plan of what to do
            final ListenableFuture<SyncPlan> buildSyncPlanFuture = Futures.transform(
                    getSongsToSyncFuture,
                    new AsyncFunction<Set<Song>, SyncPlan>() {

                        @Override
                        public ListenableFuture<SyncPlan> apply(Set<Song> songs) throws Exception {
                            postProgressEvent(2);
                            return exec.submit(new BuildSyncPlanTask(bus, config.syncDir, songs));
                        }
                    }
            );

            // Schedule delete files task. (This needs to happen before
            // downloads as the files may overlap)
            final ListenableFuture<List<File>> deleteSongsFuture = Futures.transform(
                    buildSyncPlanFuture,
                    new AsyncFunction<SyncPlan, List<File>>() {

                        @Override
                        public ListenableFuture<List<File>> apply(SyncPlan syncPlan) throws Exception {
                            postProgressEvent(3);
                            List<SyncPlan.Item> deletePlanItems = Lists.newArrayList();
                            for (SyncPlan.Item item : syncPlan.getItems()) {
                                if (item.getAction() == Action.DELETE) {
                                    deletePlanItems.add(item);
                                }
                            }

                            if (deletePlanItems.size() == 0 || config.dryRun) {
                                return Futures.immediateFuture((List<File>) new ArrayList<File>());
                            } else {
                                return exec.submit(new DeleteFilesTask(bus, exec, deletePlanItems));
                            }
                        }
                    }
            );

            // Schedule download songs task
            final ListenableFuture<List<Song>> downloadSongsFuture = Futures.transform(Futures.allAsList(
                            buildSyncPlanFuture,
                            deleteSongsFuture), new AsyncFunction<List<Object>, List<Song>>() {

                        @Override
                        public ListenableFuture<List<Song>> apply(List<Object> input) throws Exception {
                            postProgressEvent(4);
                            SyncPlan syncPlan = (SyncPlan) input.get(0);
                            List<SyncPlan.Item> downloadPlanItems = Lists.newArrayList();
                            for (SyncPlan.Item item : syncPlan.getItems()) {
                                if (item.getAction() == Action.DOWNLOAD) {
                                    downloadPlanItems.add(item);
                                }
                            }

                            if (downloadPlanItems.size() == 0 || config.dryRun) {
                                return Futures.immediateFuture((List<Song>) new ArrayList<Song>());
                            } else {
                                return exec.submit(new DownloadSongsTask(bus,
                                        client,
                                        exec,
                                        tempPath,
                                        concurrentJobsSemaphore,
                                        downloadPlanItems));
                            }
                        }
                    }
            );

            // Schedule write playlists task
            final ListenableFuture<Void> generatePlaylistsFuture = Futures.transform(Futures.allAsList(
                            buildSyncPlanFuture,
                            downloadSongsFuture), new AsyncFunction<List<Object>, Void>() {

                        @Override
                        public ListenableFuture<Void> apply(List<Object> input) throws Exception {
                            postProgressEvent(5);
                            if (config.dryRun) {
                                return Futures.immediateFuture(null);
                            } else {
                                SyncPlan syncPlan = (SyncPlan) input.get(0);
                                List<Song> downloadedSongs = Lists.newArrayList((List<Song>) input.get(
                                        1));

                                return exec.submit(new GeneratePlaylistsTask(bus,
                                        config.syncDir,
                                        syncPlan,
                                        downloadedSongs));
                            }
                        }
                    }
            );

            // Schedule write cache file task
            final ListenableFuture<Integer> writeCacheFileFuture = Futures.transform(Futures.allAsList(
                            buildSyncPlanFuture,
                            downloadSongsFuture), new AsyncFunction<List<Object>, Integer>() {
                        @Override
                        public ListenableFuture<Integer> apply(List<Object> input) throws Exception {

                            if (config.dryRun) {
                                return Futures.immediateFuture(null);
                            } else {
                                SyncPlan syncPlan = (SyncPlan) input.get(0);
                                List<Song> downloadedSongs = Lists.newArrayList((List<Song>) input.get(
                                        1));

                                return exec.submit(new WriteCacheFileTask(config.syncDir,
                                        syncPlan,
                                        downloadedSongs));
                            }
                        }
                    }
            );

            // Aggregate tasks to report metrics
            ListenableFuture<Outcome> outcomeFuture = Futures.transform(Futures.allAsList(
                            deleteSongsFuture,
                            downloadSongsFuture,
                            generatePlaylistsFuture,
                            writeCacheFileFuture), new Function<List<Object>, Outcome>() {

                        @Override
                        public Outcome apply(List<Object> input) {
                            postProgressEvent(6);
                            List<File> deletedFiles = (List<File>) input.get(0);
                            List<Song> downloadedSongs = Lists.newArrayList((List<Song>) input.get(1));

                            int deletions = deletedFiles.size();
                            int totalDownloads = downloadedSongs.size();
                            downloadedSongs.retainAll(Collections.singleton(null));
                            int failedDownloads = downloadedSongs.size();
                            int successfulDownloads = totalDownloads - failedDownloads;

                            return new Outcome(deletions, successfulDownloads, failedDownloads);
                        }
                    }
            );

            Outcome outcome = outcomeFuture.get();
            bus.post(new SyncProcessFinishedEvent(this, config, outcome));
            return outcome;
        } catch (Exception e) {
            bus.post(new SyncProcessFinishedWithErrorEvent(this, config, e));
            throw e;
        }
    }

    private void postProgressEvent(int step) {
        bus.post(new SyncProcessProgressChangedEvent(SyncTask.this, config, step, NUM_STEPS));
    }
}
