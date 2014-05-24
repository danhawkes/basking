package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.GeneratePlaylistsTask;

public class GeneratePlaylistsEvent {

    private GeneratePlaylistsEvent() {
    }

    public static class Started extends TaskEvent.Started<GeneratePlaylistsTask> {
        public Started(GeneratePlaylistsTask task) {
            super(task);
        }
    }

    public static class ProgressChanged extends TaskEvent.ProgressChanged<GeneratePlaylistsTask> {

        public ProgressChanged(GeneratePlaylistsTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class Finished extends TaskEvent.Finished<GeneratePlaylistsTask> {

        public Finished(GeneratePlaylistsTask task) {
            super(task);
        }
    }
}
