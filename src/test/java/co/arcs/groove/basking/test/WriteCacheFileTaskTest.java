package co.arcs.groove.basking.test;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.basking.task.WriteCacheFileTask;
import co.arcs.groove.thresher.Song;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WriteCacheFileTaskTest {

    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void generatesExpectedNumberOfItems() throws Exception {

        ImmutableList.Builder<Item> itemsBuilder = ImmutableList.builder();
        List<Song> downloadedSongs = Lists.newArrayList();

        for (int i = 0; i < 5; i++) {
            Song s = mockSongWithId(i);
            if (i < 3) {
                downloadedSongs.add(s);
            }
            itemsBuilder.add(new Item(mockFileWithSongId(i), Action.DOWNLOAD, s));
        }
        for (int i = 5; i < 10; i++) {
            itemsBuilder.add(new Item(mockFileWithSongId(i), Action.LEAVE, mockSongWithId(i)));
        }
        for (int i = 10; i < 15; i++) {
            itemsBuilder.add(new Item(mockFileWithSongId(i), Action.DELETE, null));
        }
        SyncPlan plan = new SyncPlan(itemsBuilder.build());

        Integer writtenItems = new WriteCacheFileTask(tempDir.getRoot(),
                plan,
                downloadedSongs).call();

        assertThat(writtenItems, equalTo(8));
    }

    private static Song mockSongWithId(int i) {
        Song s = mock(Song.class);
        when(s.getId()).thenReturn(i);
        return s;
    }

    private File mockFileWithSongId(int i) {
        return new File(tempDir.getRoot(), Integer.toString(i));
    }
}
