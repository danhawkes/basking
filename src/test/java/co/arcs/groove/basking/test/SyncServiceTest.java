package co.arcs.groove.basking.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import co.arcs.groove.basking.Config;
import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.task.SyncTask.Outcome;

public class SyncServiceTest {

	private static final String USERNAME = "jka32muwfhqt3jf4qbubc8dp@mailinator.com";
	private static final String PASSWORD = "jka32muwfhqt3jf4qbubc8dp";

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Test
	public void syncTest() throws InterruptedException, ExecutionException {

		Config config = new Config(USERNAME, PASSWORD, tempDir.getRoot());
		Outcome outcome = new SyncService().start(config).get();
		assertEquals(0, outcome.deleted);
		assertEquals(2, outcome.downloaded);
		assertEquals(0, outcome.failedToDownload);
	}
}
