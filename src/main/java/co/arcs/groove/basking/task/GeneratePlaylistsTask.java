package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.arcs.groove.basking.event.impl.Events.GeneratePlaylistsFinishedEvent;
import co.arcs.groove.basking.event.impl.Events.GeneratePlaylistsProgressChangedEvent;
import co.arcs.groove.basking.event.impl.Events.GeneratePlaylistsStartedEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.thresher.Song;

public class GeneratePlaylistsTask implements Task<Void> {

    private final EventBus bus;
    private final File syncPath;
    private final SyncPlan syncPlan;
    private final List<Song> downloadedSongs;

    public GeneratePlaylistsTask(EventBus bus,
            File syncPath,
            SyncPlan syncPlan,
            List<Song> downloadedSongs) {
        this.bus = bus;
        this.syncPath = syncPath;
        this.syncPlan = syncPlan;
        this.downloadedSongs = downloadedSongs;
    }

    @Override
    public Void call() throws Exception {

        bus.post(new GeneratePlaylistsStartedEvent(this));

        List<SyncPlan.Item> collectionItems = Lists.newArrayList();
        List<SyncPlan.Item> favoriteItems = Lists.newArrayList();

        // Filter out planned items that failed
        Collection<SyncPlan.Item> successfulItems = Collections2.filter(syncPlan.items,
                new Predicate<SyncPlan.Item>() {

                    @Override
                    public boolean apply(Item item) {
                        return (item.action != Action.DOWNLOAD) || (downloadedSongs.contains(item.song));
                    }
                }
        );

        // Separate out 'favorited' subset
        for (SyncPlan.Item item : successfulItems) {
            if ((item.action == Action.DOWNLOAD) || (item.action == Action.LEAVE)) {
                collectionItems.add(item);
                if (item.song.getUserData().isFavorited()) {
                    favoriteItems.add(item);
                }
            }
        }

        // Sort collections by date added
        Collections.sort(favoriteItems, new Comparator<SyncPlan.Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                return o2.song.getUserData().getTimeFavorited().compareTo(o1.song.getUserData().getTimeFavorited());
            }
        });
        Collections.sort(collectionItems, new Comparator<SyncPlan.Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                return o2.song.getUserData().getTimeAdded().compareTo(o1.song.getUserData().getTimeAdded());
            }
        });

        int totalItems = favoriteItems.size() + collectionItems.size();
        bus.post(new GeneratePlaylistsProgressChangedEvent(this, 0, totalItems));

        writePlaylist(new File(syncPath, "GS Favorites.m3u"), favoriteItems, 0, totalItems);
        writePlaylist(new File(syncPath, "GS Collection.m3u"),
                collectionItems,
                favoriteItems.size(),
                totalItems);

        bus.post(new GeneratePlaylistsFinishedEvent(this));

        return null;
    }

    // TODO startIndex and totalItems are a hack
    private void writePlaylist(File playlistFile,
            List<SyncPlan.Item> items,
            int startIndex,
            int totalItems) throws IOException, UnsupportedTagException, InvalidDataException {

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n\n");

        int i = startIndex;

        for (SyncPlan.Item item : items) {
            writeToPlaylist(sb, item);
            bus.post(new GeneratePlaylistsProgressChangedEvent(this, ++i, totalItems));
        }

        Files.write(sb.toString(), playlistFile, Charsets.UTF_8);
    }

    private static void writeToPlaylist(StringBuilder sb,
            SyncPlan.Item item) throws UnsupportedTagException, InvalidDataException, IOException {

        long len = new Mp3File(item.file.getAbsolutePath()).getLengthInSeconds();
        sb.append(String.format("#EXTINF:%d,%s - %s\n%s\n\n",
                len,
                item.song.getName(),
                item.song.getArtistName(),
                item.file.getName()));
    }
}
