package co.arcs.groove.basking.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.ExecutionException;

import co.arcs.groove.basking.Config;
import co.arcs.groove.basking.SyncOperation;
import co.arcs.groove.basking.task.SyncTask.Outcome;

import static org.junit.Assert.assertEquals;

public class SyncOperationTest {

    private static final String USERNAME = "jka32muwfhqt3jf4qbubc8dp@mailinator.com";
    private static final String PASSWORD = "jka32muwfhqt3jf4qbubc8dp";

    @SuppressWarnings("CanBeFinal") @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void sync() throws InterruptedException, ExecutionException {

        Config config = new Config(USERNAME, PASSWORD, tempDir.getRoot());
        Outcome outcome = new SyncOperation(config).start().getFuture().get();
        assertEquals(0, outcome.getFailedToDownload());
        assertEquals(0, outcome.getDeleted());
        assertEquals(2, outcome.getDownloaded());
    }
}
