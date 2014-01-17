package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.DownloadSongTask;

public class DownloadSongEvent {

	private DownloadSongEvent() {
	}

	public static class Started extends TaskEvent.Started<DownloadSongTask> {
		public Started(DownloadSongTask task) {
			super(task);
		}
	}

	public static class ProgressChanged extends TaskEvent.ProgressChanged<DownloadSongTask> {

		public ProgressChanged(DownloadSongTask task, int progress, int total) {
			super(task, progress, total);
		}
	}

	public static class Finished extends TaskEvent.Finished<DownloadSongTask> {

		public Finished(DownloadSongTask task) {
			super(task);
		}
	}
}
