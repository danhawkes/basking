package co.arcs.groove.basking.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import org.apache.http.HttpResponse;

import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.event.impl.DownloadSongEvent;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

import com.google.common.eventbus.EventBus;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v22Tag;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;

public class DownloadSongTask implements Task<Song> {

	private final EventBus bus;
	private final Client client;
	public final Song song;
	private final File syncFile;
	private final File tempPath;
	private final Semaphore concurrentJobsSemaphore;

	public DownloadSongTask(EventBus bus, Client client, Song song, File syncFile, File tempPath, Semaphore concurrentJobsSemaphore) {
		this.bus = bus;
		this.client = client;
		this.song = song;
		this.syncFile = syncFile;
		this.tempPath = tempPath;
		this.concurrentJobsSemaphore = concurrentJobsSemaphore;
	}

	@Override
	public Song call() throws Exception {
		
		concurrentJobsSemaphore.acquire();
		
		bus.post(new DownloadSongEvent.Started(this));
		
		File tempFile = new File(tempPath, syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_1);
		File tempFile2 = new File(tempPath, syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_2);

		// Download file
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			HttpResponse response = client.getStreamResponse(song);
			is = response.getEntity().getContent();
			fos = new FileOutputStream(tempFile);

			byte[] buffer = new byte[1024 * 1024];
			long len = response.getEntity().getContentLength();
			
			int read = 0;
			int readTotal = 0;
			bus.post(new DownloadSongEvent.ProgressChanged(this, 0, (int) len));
			while ((read = is.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
				readTotal += read;
				bus.post(new DownloadSongEvent.ProgressChanged(this, readTotal, (int) len));
			}
			fos.flush();
			fos.getFD().sync();
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
				id3v2Tag.setTitle(song.name);
				id3v2Tag.setArtist(song.artistName);
				id3v2Tag.setAlbum(song.albumName);
				id3v2Tag.setYear(song.year + "");
				Utils.encodeId(song.id, id3v2Tag);
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

		bus.post(new DownloadSongEvent.Finished(this));
		
		concurrentJobsSemaphore.release();
		
		return song;
	}
}