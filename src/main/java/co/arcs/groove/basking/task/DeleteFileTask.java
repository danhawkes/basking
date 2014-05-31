package co.arcs.groove.basking.task;

import com.google.common.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

import co.arcs.groove.basking.event.Events.DeleteFileFinishedEvent;
import co.arcs.groove.basking.event.Events.DeleteFileStartedEvent;

public class DeleteFileTask implements Task<File> {

    private final EventBus bus;
    private final File file;

    public DeleteFileTask(EventBus bus, File file) {
        this.bus = bus;
        this.file = file;
    }

    @Override
    public File call() throws Exception {

        bus.post(new DeleteFileStartedEvent(this));

        if (!file.delete()) {
            throw new IOException("Failed to delete orphaned file: " + file.getAbsolutePath());
        }

        bus.post(new DeleteFileFinishedEvent(this));

        return file;
    }
}
