package co.arcs.groove.basking.test;

import com.google.common.base.Charsets;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.task.BuildSyncPlanTask;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.EventPoster;
import co.arcs.groove.thresher.Song;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSyncPlanTaskTest {

    private static final Range<Integer> FILES_PRESENT = Range.closedOpen(0, 1000);

    private static final Range<Integer> FILES_WANTED = Range.closedOpen(50, 1040);

    /** Files in the sync directory that are not part of the sync. */
    private static final int FILES_PRESENT_UNMANAGED = 500;

    private static final int EXPECTED_LEAVE = sizeOf(FILES_PRESENT.intersection(FILES_WANTED));
    private static final int EXPECTED_DOWNLOAD = sizeOf(FILES_WANTED) - EXPECTED_LEAVE;
    private static final int EXPECTED_DELETE = sizeOf(FILES_PRESENT) - EXPECTED_LEAVE;

    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private Set<Song> songs = new HashSet<Song>();

    @Before
    public void setup() throws IOException, InvalidDataException, UnsupportedTagException, NotSupportedException {

        // A managed file
        String mp3File = BuildSyncPlanTaskTest.class.getClassLoader()
                .getResource("test.mp3")
                .getPath();

        // An un-managed file
        byte[] unmanagedFile = "test".getBytes();

        // Create present, managed files
        for (int i : iterable(FILES_PRESENT)) {
            File f = new File(tempDir.getRoot(), genManagedFileName(i));
            Mp3File mp3 = new Mp3File(mp3File);
            ID3v2 tag = mp3.getId3v2Tag();
            Utils.encodeId(i, tag);
            mp3.save(f.getAbsolutePath());
        }

        // Create present, unmanaged files
        for (int i = 0; i < FILES_PRESENT_UNMANAGED; i++) {
            File f = new File(tempDir.getRoot(), genUnmanagedFileName());
            Files.write(unmanagedFile, f);
        }

        // Create a sync cache file
        File cacheFile = tempDir.newFile(BuildSyncPlanTask.CACHE_FILENAME);
        StringBuilder sb = new StringBuilder();
        for (int i : iterable(FILES_PRESENT)) {
            sb.append(String.format("%d|%s\n", i, genManagedFileName(i)));
        }
        Files.write(sb.toString().getBytes(Charsets.UTF_8), cacheFile);

        // Wanted files
        songs = new HashSet<Song>();
        for (int i : iterable(FILES_WANTED)) {
            Song s = mock(Song.class);
            when(s.getId()).thenReturn(i);
            songs.add(s);
        }
    }

    private static String genManagedFileName(int i) {
        return String.format("file %04d.mp3", i);
    }

    private static String genUnmanagedFileName() {
        int d = (int) (Math.random() * Integer.MAX_VALUE);
        return String.format("unmanaged %d.txt", d);
    }

    private void deleteSyncCacheFile() {
        assertTrue(new File(tempDir.getRoot(), BuildSyncPlanTask.CACHE_FILENAME).delete());
    }

    private static Iterable<Integer> iterable(Range<Integer> range) {
        return ContiguousSet.create(range, DiscreteDomain.integers());
    }

    private static int sizeOf(Range<Integer> range) {
        return ContiguousSet.create(range, DiscreteDomain.integers()).size();
    }

    @Test
    public void withSyncCache() throws Exception {
        SyncPlan syncPlan = new BuildSyncPlanTask(mock(EventPoster.class), tempDir.getRoot(), songs).call();
        assertEquals(EXPECTED_LEAVE, syncPlan.getToLeave());
        assertEquals(EXPECTED_DOWNLOAD, syncPlan.getToDownload());
        assertEquals(EXPECTED_DELETE, syncPlan.getToDelete());
    }

    @Test
    public void withoutSyncCache() throws Exception {
        deleteSyncCacheFile();
        SyncPlan syncPlan = new BuildSyncPlanTask(mock(EventPoster.class), tempDir.getRoot(), songs).call();
        assertEquals(EXPECTED_LEAVE, syncPlan.getToLeave());
        assertEquals(EXPECTED_DOWNLOAD, syncPlan.getToDownload());
        assertEquals(EXPECTED_DELETE, syncPlan.getToDelete());
    }

    @Test
    public void withCacheIsFaster() throws Exception {

        long t0 = System.currentTimeMillis();
        new BuildSyncPlanTask(mock(EventPoster.class), tempDir.getRoot(), songs).call();
        long dtWithCache = System.currentTimeMillis() - t0;

        // Delete cache file before continuing
        deleteSyncCacheFile();

        long t1 = System.currentTimeMillis();
        new BuildSyncPlanTask(mock(EventPoster.class), tempDir.getRoot(), songs).call();
        long dtWithoutCache = System.currentTimeMillis() - t1;

        // This is a conservative estimate for when running on an SSD.
        assertTrue(String.format(
                "Expected task with sync cache to take <= 1/2 the time of regular task. Actual times were %d and %d ms.",
                dtWithCache,
                dtWithoutCache), dtWithCache <= (dtWithoutCache / 2));
    }
}
