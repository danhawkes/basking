package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.SyncTask;
import co.arcs.groove.basking.task.SyncTask.Outcome;

public class SyncEvent {

    private SyncEvent() {
    }

    public static class Started extends TaskEvent.Started<SyncTask> {

        public Started(SyncTask task) {
            super(task);
        }
    }

    public static class ProgressChanged extends TaskEvent.ProgressChanged<SyncTask> {

        public ProgressChanged(SyncTask task, int progress, int total) {
            super(task, progress, total);
        }
    }

    public static class Finished extends TaskEvent.Finished<SyncTask> {

        public final Outcome outcome;

        public Finished(SyncTask task, Outcome outcome) {
            super(task);
            this.outcome = outcome;
        }
    }

    public static class FinishedWithError extends TaskEvent.Finished<SyncTask> {

        public final Exception e;

        public FinishedWithError(SyncTask task, Exception e) {
            super(task);
            this.e = e;
        }
    }
}
