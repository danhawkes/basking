package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.SyncTask;

public class SyncTaskEvent {

	private SyncTaskEvent() {
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

		public Finished(SyncTask task) {
			super(task);
		}
	}
}
