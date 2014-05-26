package co.arcs.groove.basking.task;

import java.util.concurrent.Callable;

/**
 * A unit of work within the sync process. Is actually just a {@link
 * java.util.concurrent.Callable}.
 */
public interface Task<T> extends Callable<T> {

}
