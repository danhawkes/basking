package co.arcs.groove.basking.event;

import com.google.common.eventbus.Subscribe;

public interface EventListener {

	// High-level events

	@Subscribe
	void onTaskStarted(TaskEvent.Started task);

	@Subscribe
	void onTaskProgressChanged(TaskEvent.ProgressChanged task, int percent);

	@Subscribe
	void onTaskFinished(TaskEvent.Finished task);
}
