package co.arcs.groove.basking.task;

import java.io.File;
import java.io.IOException;

import co.arcs.groove.basking.Console;

public class DeleteSongTask extends AbstractSongTask {

	private final File file;

	public DeleteSongTask(File file) {
		this.file = file;
	}

	@Override
	public Void call() throws Exception {

		Console.log("Deleting '" + file.getAbsolutePath() + "'…");

		if (!file.delete()) {
			throw new IOException("Failed to delete orphaned file: " + file.getAbsolutePath());
		}
		return null;
	}
}