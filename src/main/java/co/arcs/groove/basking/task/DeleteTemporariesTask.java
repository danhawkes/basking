package co.arcs.groove.basking.task;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import co.arcs.groove.basking.SyncService;

public class DeleteTemporariesTask implements Task<Void> {

	private final File tempPath;

	public DeleteTemporariesTask(File tempPath) {
		this.tempPath = tempPath;
	}

	@Override
	public Void call() throws Exception {

		File[] tempFiles = tempPath.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(SyncService.TEMP_FILE_EXTENSION_1)
						|| arg1.endsWith(SyncService.TEMP_FILE_EXTENSION_2);
			}
		});
		for (File f : tempFiles) {
			if (!f.delete()) {
				throw new IOException("Could not delete temp file: '" + f.getAbsolutePath() + "'");
			}
		}
		return null;
	}
}