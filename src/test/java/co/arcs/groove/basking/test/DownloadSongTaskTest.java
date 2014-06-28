package co.arcs.groove.basking.test;

import com.belladati.httpclientandroidlib.HttpEntity;
import com.belladati.httpclientandroidlib.HttpResponse;
import com.google.common.eventbus.EventBus;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.arcs.groove.basking.task.DownloadSongTask;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.GroovesharkException;
import co.arcs.groove.thresher.Song;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadSongTaskTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    public File targetFile;
    public File tempDir;

    @Before
    public void setup() throws IOException, GroovesharkException {
        targetFile = new File(tempFolder.getRoot(), "test.mp3");
        tempDir = tempFolder.newFolder();
    }

    /**
     * This test is intended to make sure that download song task stops when its thread is
     * interrupted. The reason it might not is that it spends a significant amount of its time in
     * code that does not automatically get an InterruptedException when the thread is interrupted
     * (see {@link Thread#interrupt()}), so cannot just rely on the IO stream's interruptible
     * behaviour to bail out of the loop.
     */
    @Test
    public void futureCancellationInterruptsTask() throws ExecutionException, InterruptedException, TimeoutException, IOException, GroovesharkException {

        // Endless input stream. Supplies large amounts of data to ensure it fills the task's input
        // buffer quickly.
        InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                return 1;
            }

            @Override
            public int read(byte[] bytes, int i, int i2) throws IOException {
                return 10000;
            }
        };

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(is);

        HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(entity);

        Client client = mock(Client.class);
        when(client.getStreamResponse((Song) anyObject())).thenReturn(response);

        final DownloadSongTask task = new DownloadSongTask(new EventBus(),
                client,
                mock(Song.class),
                targetFile,
                tempDir,
                new Semaphore(1));

        final CountDownLatch latch = new CountDownLatch(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    task.call();
                } catch (InterruptedException e) {
                    // The task stopped itself
                    latch.countDown();
                } catch (Exception e) {
                    fail("Task threw unexpected exception");
                }
            }
        });
        t.start();

        // Wait for download to get started, then interrupt it
        Thread.sleep(1000);
        t.interrupt();

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Timed out waiting for task to handle interrupt");
        }
    }
}
