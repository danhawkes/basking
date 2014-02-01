package co.arcs.groove.basking.task;

import java.io.File;
import java.io.IOException;

import co.arcs.groove.basking.event.impl.DeleteFileEvent;

import com.google.common.eventbus.EventBus;

public class DeleteFileTask implements Task<File> {

	private final EventBus bus;
	public final File file;

	public DeleteFileTask(EventBus bus, File file) {
		this.bus=bus;
		this.file = file;
	}

	@Override 
	public File call() throws Exception {

		bus.post(new DeleteFileEvent.Started(this));

		if (!file.delete()) {
			throw new IOException("Failed to delete orphaned file: " + file.getAbsolutePath());
		}
		
		bus.post(new DeleteFileEvent.Finished(this));
		
		return file;
	}
}