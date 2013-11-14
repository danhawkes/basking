package co.arcs.groove.basking.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import co.arcs.groove.basking.Log;
import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.task.GetSongsToSyncTask.SongToSync;
import co.arcs.groove.thresher.Client;

import com.google.common.io.ByteStreams;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v22Tag;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;

public class DownloadSongTask extends AbstractSongTask {

	private final Client client;
	private final SongToSync songToSync;
	private final File syncFile;
	private final File tempPath;

	public DownloadSongTask(Client client, SongToSync songToSync, File syncFile, File tempPath) {
		this.client = client;
		this.songToSync = songToSync;
		this.syncFile = syncFile;
		this.tempPath = tempPath;
	}

	@Override
	public Void call() throws Exception {

		File tempFile = new File(tempPath, syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_1);
		File tempFile2 = new File(tempPath, syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_2);

		// Download file
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = client.getStream(songToSync.song);
			fos = new FileOutputStream(tempFile);
			ByteStreams.copy(is, fos);
			fos.getFD().sync();
		} catch (IOException e) {
			throw e;
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (is != null) {
				is.close();
			}
		}

		// Remove id3v1 tag
		Mp3File mp3File = new Mp3File(tempFile.getAbsolutePath());
		if (mp3File.hasId3v1Tag()) {
			mp3File.removeId3v1Tag();
		}

		// Get/create id3v2 tag
		ID3v2 id3v2Tag = null;
		if (mp3File.hasId3v2Tag()) {
			id3v2Tag = mp3File.getId3v2Tag();
		} else {
			id3v2Tag = new ID3v23Tag();
		}
		Exception err = null;
		while (true) {
			// Try saving twice; once with old tag data, and again after
			// discarding corrupt data and starting over
			try {
				id3v2Tag.setTitle(songToSync.song.name);
				id3v2Tag.setArtist(songToSync.song.artistName);
				id3v2Tag.setAlbum(songToSync.song.albumName);
				id3v2Tag.setYear(songToSync.song.year + "");
				Utils.encodeId(songToSync.song.id, id3v2Tag);
				mp3File.setId3v2Tag(id3v2Tag);
				mp3File.save(tempFile2.getAbsolutePath());
				break;
			} catch (NotSupportedException e) {
				if (err != null) {
					e.initCause(err);
					throw e;
				} else {
					// Discard old tag if obsolete
					id3v2Tag = new ID3v22Tag();
					err = e;
				}
			}
		}

		// Delete temp 1 file
		if (!tempFile.delete()) {
			throw new IOException("Failed to delete temp file: " + tempFile.getAbsolutePath());
		}

		// Move temp 2 file to sync directory
		if (!tempFile2.renameTo(syncFile)) {
			throw new IOException("Failed to move temp file: " + tempFile2.getAbsolutePath());
		}

		Log.d("…downloaded: '" + syncFile.getName() + "'");

		return null;
	}
}