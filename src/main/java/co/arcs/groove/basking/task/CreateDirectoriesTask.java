package co.arcs.groove.basking.task;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class CreateDirectoriesTask implements Callable<Void> {

	private final File syncPath;
	private final File tempPath;

	public CreateDirectoriesTask(File syncPath, File tempPath) {
		this.syncPath = syncPath;
		this.tempPath = tempPath;
	}

	@Override
	public Void call() throws Exception {

		syncPath.mkdirs();
		tempPath.mkdirs();
		if (!syncPath.exists()) {
			throw new IOException("Sync path '" + syncPath.getAbsolutePath()
					+ "' does not exist and could not be created");
		}
		if (!syncPath.isDirectory()) {
			throw new IOException("Sync path '" + syncPath.getAbsolutePath()
					+ "' exists and is not a directory");
		}
		if (!tempPath.exists()) {
			throw new IOException("Temp path '" + tempPath.getAbsolutePath()
					+ "' does not exist and could not be created");
		}
		if (!tempPath.isDirectory()) {
			throw new IOException("Temp path '" + tempPath.getAbsolutePath()
					+ "' exists and is not a directory");
		}

		return null;
	}
}