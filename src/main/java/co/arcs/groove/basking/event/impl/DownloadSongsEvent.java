package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.DownloadSongsTask;

public class DownloadSongsEvent {

    private DownloadSongsEvent() {
    }

    public static class Started extends TaskEvent.Started<DownloadSongsTask> {
        public Started(DownloadSongsTask task) {
            super(task);
        }
    }

    public static class ProgressChanged extends TaskEvent.ProgressChanged<DownloadSongsTask> {

        public ProgressChanged(DownloadSongsTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class Finished extends TaskEvent.Finished<DownloadSongsTask> {

        public Finished(DownloadSongsTask task) {
            super(task);
        }
    }
}
