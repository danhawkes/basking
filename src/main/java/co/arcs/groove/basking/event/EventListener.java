package co.arcs.groove.basking.event;

import com.google.common.eventbus.Subscribe;

import co.arcs.groove.basking.event.TaskEvent.TaskFinishedEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskProgressChangedEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskStartedEvent;

public interface EventListener {

    // High-level events

    @Subscribe
    void onTaskStarted(TaskStartedEvent task);

    @Subscribe
    void onTaskProgressChanged(TaskProgressChangedEvent task, int percent);

    @Subscribe
    void onTaskFinished(TaskFinishedEvent task);
}
