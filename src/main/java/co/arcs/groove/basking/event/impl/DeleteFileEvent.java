package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.DeleteFileTask;

public class DeleteFileEvent {

    private DeleteFileEvent() {
    }

    public static class Started extends TaskEvent.Started<DeleteFileTask> {
        public Started(DeleteFileTask task) {
            super(task);
        }
    }

    public static class ProgressChanged extends TaskEvent.ProgressChanged<DeleteFileTask> {

        public ProgressChanged(DeleteFileTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class Finished extends TaskEvent.Finished<DeleteFileTask> {

        public Finished(DeleteFileTask task) {
            super(task);
        }
    }
}
