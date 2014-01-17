package co.arcs.groove.basking.event.impl;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask;

public class BuildSyncPlanEvent {

	private BuildSyncPlanEvent() {
	}

	public static class Started extends TaskEvent.Started<BuildSyncPlanTask> {
		public Started(BuildSyncPlanTask task) {
			super(task);
		}
	}

	public static class ProgressChanged extends TaskEvent.ProgressChanged<BuildSyncPlanTask> {

		public ProgressChanged(BuildSyncPlanTask task, int progress, int total) {
			super(task, progress, total);
		}
	}

	public static class Finished extends TaskEvent.Finished<BuildSyncPlanTask> {

		public final int download;
		public final int delete;
		public final int leave;

		public Finished(BuildSyncPlanTask task, int download, int delete, int leave) {
			super(task);
			this.download = download;
			this.delete = delete;
			this.leave = leave;
		}
	}
}
