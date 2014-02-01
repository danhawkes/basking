package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.DeleteFilesTask;

public class DeleteFilesEvent {

	private DeleteFilesEvent() {
	}

	public static class Started extends TaskEvent.Started<DeleteFilesTask> {
		public Started(DeleteFilesTask task) {
			super(task);
		}
	}

	public static class ProgressChanged extends TaskEvent.ProgressChanged<DeleteFilesTask> {

		public ProgressChanged(DeleteFilesTask task, int progress, int total) {
			super(task, progress, total);
		}
	}

	public static class Finished extends TaskEvent.Finished<DeleteFilesTask> {

		public Finished(DeleteFilesTask task) {
			super(task);
		}
	}
}
