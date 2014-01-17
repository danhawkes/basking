package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.GetSongsToSyncTask;

public class GetSongsToSyncEvent {

	private GetSongsToSyncEvent() {
	}

	public static class Started extends TaskEvent.Started<GetSongsToSyncTask> {
		public Started(GetSongsToSyncTask task) {
			super(task);
		}
	}

	public static class ProgressChanged extends TaskEvent.ProgressChanged<GetSongsToSyncTask> {

		public ProgressChanged(GetSongsToSyncTask task, int progress, int total) {
			super(task, progress, total);
		}
	}

	public static class Finished extends TaskEvent.Finished<GetSongsToSyncTask> {

		public final int items;
		
		public Finished(GetSongsToSyncTask task, int items) {
			super(task);
			this.items = items;
		}  
	}
}
