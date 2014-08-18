package co.arcs.groove.basking.task;

import co.arcs.groove.basking.event.TaskEvent;

/**
 * Wrapper to generify posting events to a bus/subject. To be removed and replaced with just a
 * subject upon eventual migration to RX.
 */
public interface EventPoster {

    void post(TaskEvent e);

    void postBusOnly(TaskEvent e);

    void postCompleted();

    void postError(Throwable t);
}
