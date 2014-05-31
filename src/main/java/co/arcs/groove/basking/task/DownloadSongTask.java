package co.arcs.groove.basking.task;

import com.belladati.httpclientandroidlib.HttpResponse;
import com.google.common.eventbus.EventBus;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v22Tag;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.event.Events.DownloadSongFinishedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongProgressChangedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongStartedEvent;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;

public class DownloadSongTask implements Task<Song> {

    private final EventBus bus;
    private final Client client;
    private final Song song;
    private final File syncFile;
    private final File tempPath;
    private final Semaphore concurrentJobsSemaphore;

    private static final int INPUT_BUFFER_LEN = 1024 * 64;

    public DownloadSongTask(EventBus bus,
            Client client,
            Song song,
            File syncFile,
            File tempPath,
            Semaphore concurrentJobsSemaphore) {
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

        try {
            bus.post(new DownloadSongStartedEvent(this, song));

            File tempFile = new File(tempPath,
                    syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_1);
            File tempFile2 = new File(tempPath,
                    syncFile.getName() + SyncService.TEMP_FILE_EXTENSION_2);

            // Download file
            InputStream is = null;
            FileOutputStream os = null;
            try {
                HttpResponse response = client.getStreamResponse(song);
                is = new BufferedInputStream(response.getEntity().getContent(), INPUT_BUFFER_LEN);
                os = new FileOutputStream(tempFile);

                bus.post(new DownloadSongProgressChangedEvent(this, song, 0, 1));

                byte[] buffer = new byte[INPUT_BUFFER_LEN];

                long len = response.getEntity().getContentLength();
                int read;
                int readTotal = 0;
                float lastReportedProgress = 0;

                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    readTotal += read;

                    // Only emit events for >1% change
                    float newProgress = (float) readTotal / len;
                    if (((newProgress - 0.01f) >= lastReportedProgress) || newProgress == 1.0f) {
                        bus.post(new DownloadSongProgressChangedEvent(this,
                                song,
                                readTotal,
                                (int) len));
                        lastReportedProgress = newProgress;
                    }
                }
                os.flush();
                os.getFD().sync();
            } finally {
                if (os != null) {
                    os.close();
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
            ID3v2 id3v2Tag;
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
                    id3v2Tag.setTitle(song.getName());
                    id3v2Tag.setArtist(song.getArtistName());
                    id3v2Tag.setAlbum(song.getAlbumName());
                    id3v2Tag.setYear(song.getYear() + "");
                    Utils.encodeId(song.getId(), id3v2Tag);
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

            bus.post(new DownloadSongFinishedEvent(this, song));
        } finally {
            concurrentJobsSemaphore.release();
        }

        return song;
    }
}
