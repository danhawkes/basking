package co.arcs.groove.basking.event;

/**
 * Event that's associated with a {@link co.arcs.groove.basking.task.Task}.
 *
 * @param <T>
 *         The task's type.
 */
public abstract class TaskEvent<T> {

    private final T task;

    public TaskEvent(T task) {
        this.task = task;
    }

    public T getTask() {
        return task;
    }

    public abstract static class TaskStartedEvent<T> extends TaskEvent<T> {

        public TaskStartedEvent(T task) {
            super(task);
        }
    }

    public abstract static class TaskProgressChangedEvent<T> extends TaskEvent<T> {

        private final int progress;
        private final int total;

        public TaskProgressChangedEvent(T task, int progress, int total) {
            super(task);
            this.progress = progress;
            this.total = total;
        }

        public int getProgress() {
            return progress;
        }

        public int getTotal() {
            return total;
        }

        public float getFraction() {
            return (float) progress / total;
        }

        public float getPercentage() {
            return getFraction() * 100.0f;
        }
    }

    public abstract static class TaskFinishedEvent<T> extends TaskEvent<T> {

        public TaskFinishedEvent(T task) {
            super(task);
        }
    }
}
