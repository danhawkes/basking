package co.arcs.groove.basking.event;

import co.arcs.groove.basking.Config;
import co.arcs.groove.basking.event.EventLevels.HighLevelEvent;
import co.arcs.groove.basking.event.EventLevels.LowLevelEvent;
import co.arcs.groove.basking.event.EventLevels.MidLevelEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskFinishedEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskProgressChangedEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskStartedEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask;
import co.arcs.groove.basking.task.DeleteFileTask;
import co.arcs.groove.basking.task.DeleteFilesTask;
import co.arcs.groove.basking.task.DownloadSongTask;
import co.arcs.groove.basking.task.DownloadSongsTask;
import co.arcs.groove.basking.task.GeneratePlaylistsTask;
import co.arcs.groove.basking.task.GetSongsToSyncTask;
import co.arcs.groove.basking.task.SyncTask;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.thresher.Song;

public class Events {

    private Events() {
    }

    ///

    public static class BuildSyncPlanStartedEvent extends TaskStartedEvent<BuildSyncPlanTask> implements MidLevelEvent {
        public BuildSyncPlanStartedEvent(BuildSyncPlanTask task) {
            super(task);
        }
    }

    public static class BuildSyncPlanProgressChangedEvent extends TaskProgressChangedEvent<BuildSyncPlanTask> implements MidLevelEvent {
        public BuildSyncPlanProgressChangedEvent(BuildSyncPlanTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class BuildSyncPlanFinishedEvent extends TaskFinishedEvent<BuildSyncPlanTask> implements MidLevelEvent {

        private final int download;
        private final int delete;
        private final int leave;

        public BuildSyncPlanFinishedEvent(BuildSyncPlanTask task,
                int download,
                int delete,
                int leave) {
            super(task);
            this.download = download;
            this.delete = delete;
            this.leave = leave;
        }

        public int getToDownload() {
            return download;
        }

        public int getToDelete() {
            return delete;
        }

        public int getToLeave() {
            return leave;
        }
    }

    ///

    public static class DeleteFileStartedEvent extends TaskStartedEvent<DeleteFileTask> implements LowLevelEvent {

        public DeleteFileStartedEvent(DeleteFileTask task) {
            super(task);
        }
    }

    public static class DeleteFileProgressChangedEvent extends TaskProgressChangedEvent<DeleteFileTask> implements LowLevelEvent {

        public DeleteFileProgressChangedEvent(DeleteFileTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class DeleteFileFinishedEvent extends TaskFinishedEvent<DeleteFileTask> implements LowLevelEvent {

        public DeleteFileFinishedEvent(DeleteFileTask task) {
            super(task);
        }
    }

    ///

    public static class DeleteFilesStartedEvent extends TaskStartedEvent<DeleteFilesTask> implements MidLevelEvent {

        public DeleteFilesStartedEvent(DeleteFilesTask task) {
            super(task);
        }
    }

    public static class DeleteFilesProgressChangedEvent extends TaskProgressChangedEvent<DeleteFilesTask> implements MidLevelEvent {

        public DeleteFilesProgressChangedEvent(DeleteFilesTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class DeleteFilesFinishedEvent extends TaskFinishedEvent<DeleteFilesTask> implements MidLevelEvent {

        public DeleteFilesFinishedEvent(DeleteFilesTask task) {
            super(task);
        }
    }

    ///

    public static class DownloadSongStartedEvent extends TaskStartedEvent<DownloadSongTask> implements LowLevelEvent {

        private final Song song;

        public DownloadSongStartedEvent(DownloadSongTask task, Song song) {
            super(task);
            this.song = song;
        }

        public Song getSong() {
            return song;
        }
    }

    public static class DownloadSongProgressChangedEvent extends TaskProgressChangedEvent<DownloadSongTask> implements LowLevelEvent {

        private final Song song;

        public DownloadSongProgressChangedEvent(DownloadSongTask task,
                Song song,
                int progress,
                int total) {
            super(task, progress, total);
            this.song = song;
        }

        public Song getSong() {
            return song;
        }
    }

    public static class DownloadSongFinishedEvent extends TaskFinishedEvent<DownloadSongTask> implements LowLevelEvent {

        private final Song song;

        public DownloadSongFinishedEvent(DownloadSongTask task, Song song) {
            super(task);
            this.song = song;
        }

        public Song getSong() {
            return song;
        }
    }

    ///

    public static class DownloadSongsStartedEvent extends TaskStartedEvent<DownloadSongsTask> implements MidLevelEvent {

        public DownloadSongsStartedEvent(DownloadSongsTask task) {
            super(task);
        }
    }

    public static class DownloadSongsProgressChangedEvent extends TaskProgressChangedEvent<DownloadSongsTask> implements MidLevelEvent {

        public DownloadSongsProgressChangedEvent(DownloadSongsTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class DownloadSongsFinishedEvent extends TaskFinishedEvent<DownloadSongsTask> implements MidLevelEvent {

        public DownloadSongsFinishedEvent(DownloadSongsTask task) {
            super(task);
        }
    }

    ///

    public static class GeneratePlaylistsStartedEvent extends TaskStartedEvent<GeneratePlaylistsTask> implements MidLevelEvent {

        public GeneratePlaylistsStartedEvent(GeneratePlaylistsTask task) {
            super(task);
        }
    }

    public static class GeneratePlaylistsProgressChangedEvent extends TaskProgressChangedEvent<GeneratePlaylistsTask> implements MidLevelEvent {

        public GeneratePlaylistsProgressChangedEvent(GeneratePlaylistsTask task,
                int progress,
                int total) {
            super(task, progress, total);
        }
    }

    public static class GeneratePlaylistsFinishedEvent extends TaskFinishedEvent<GeneratePlaylistsTask> implements MidLevelEvent {

        public GeneratePlaylistsFinishedEvent(GeneratePlaylistsTask task) {
            super(task);
        }
    }

    ///

    public static class GetSongsToSyncStartedEvent extends TaskStartedEvent<GetSongsToSyncTask> implements MidLevelEvent {

        public GetSongsToSyncStartedEvent(GetSongsToSyncTask task) {
            super(task);
        }
    }

    public static class GetSongsToSyncProgressChangedEvent extends TaskProgressChangedEvent<GetSongsToSyncTask> implements MidLevelEvent {

        public GetSongsToSyncProgressChangedEvent(GetSongsToSyncTask task,
                int progress,
                int total) {
            super(task, progress, total);
        }
    }

    public static class GetSongsToSyncFinishedEvent extends TaskFinishedEvent<GetSongsToSyncTask> implements MidLevelEvent {

        public final int items;

        public GetSongsToSyncFinishedEvent(GetSongsToSyncTask task, int items) {
            super(task);
            this.items = items;
        }
    }

    ///

    public static class SyncProcessStartedEvent extends TaskStartedEvent<SyncTask> implements HighLevelEvent {

        private final Config config;

        public SyncProcessStartedEvent(SyncTask task, Config config) {
            super(task);
            this.config = config;
        }

        public Config getConfig() {
            return config;
        }
    }

    public static class SyncProcessProgressChangedEvent extends TaskProgressChangedEvent<SyncTask> implements HighLevelEvent {

        private final Config config;

        public SyncProcessProgressChangedEvent(SyncTask task,
                Config config,
                int progress,
                int total) {
            super(task, progress, total);
            this.config = config;
        }

        public Config getConfig() {
            return config;
        }
    }

    public static class SyncProcessFinishedEvent extends TaskFinishedEvent<SyncTask> implements HighLevelEvent {

        private final Config config;
        private final Outcome outcome;

        public SyncProcessFinishedEvent(SyncTask task, Config config, Outcome outcome) {
            super(task);
            this.config = config;
            this.outcome = outcome;
        }

        public Config getConfig() {
            return config;
        }

        public Outcome getOutcome() {
            return outcome;
        }
    }

    public static class SyncProcessFinishedWithErrorEvent extends TaskFinishedEvent<SyncTask> implements HighLevelEvent {

        private final Config config;
        private final Exception exception;

        public SyncProcessFinishedWithErrorEvent(SyncTask task, Config config, Exception e) {
            super(task);
            this.config = config;
            this.exception = e;
        }

        public Config getConfig() {
            return config;
        }

        public Exception getException() {
            return exception;
        }
    }
}
