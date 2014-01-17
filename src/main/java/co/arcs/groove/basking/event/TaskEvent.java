package co.arcs.groove.basking.event;

import co.arcs.groove.basking.task.Task;

public abstract class TaskEvent<T extends Task<?>> {

	public final T task;

	public TaskEvent(T task) {
		this.task = task;
	}

	public abstract static class Started<T extends Task<?>> extends TaskEvent<T> {

		public Started(T task) {
			super(task);
		}
	}

	public abstract static class ProgressChanged<T extends Task<?>> extends TaskEvent<T> {

		public final int progress;
		public final int total;
		public final float fraction;
		public final float percentage;

		public ProgressChanged(T task, int progress, int total) {
			super(task);
			this.progress = progress;
			this.total = total;
			this.fraction = ((float) progress / total);
			this.percentage = fraction * 100.0f;
		}
	}

	public abstract static class Finished<T extends Task<?>> extends TaskEvent<T> {

		public Finished(T task) {
			super(task);
		}
	}
}
