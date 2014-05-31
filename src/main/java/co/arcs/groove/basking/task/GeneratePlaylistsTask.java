package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.arcs.groove.basking.event.Events.GeneratePlaylistsFinishedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsStartedEvent;
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
        Collection<SyncPlan.Item> successfulItems = Collections2.filter(syncPlan.getItems(),
                new Predicate<SyncPlan.Item>() {

                    @Override
                    public boolean apply(Item item) {
                        return (item.getAction() != Action.DOWNLOAD) || (downloadedSongs.contains(
                                item.getSong()));
                    }
                }
        );

        // Separate out 'favorited' subset
        for (SyncPlan.Item item : successfulItems) {
            if ((item.getAction() == Action.DOWNLOAD) || (item.getAction() == Action.LEAVE)) {
                collectionItems.add(item);
                if (item.getSong().getUserData().isFavorited()) {
                    favoriteItems.add(item);
                }
            }
        }

        // Sort collections by date added
        Collections.sort(favoriteItems, new Comparator<SyncPlan.Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                return o2.getSong()
                        .getUserData()
                        .getTimeFavorited()
                        .compareTo(o1.getSong().getUserData().getTimeFavorited());
            }
        });
        Collections.sort(collectionItems, new Comparator<SyncPlan.Item>() {

            @Override
            public int compare(Item o1, Item o2) {
                return o2.getSong()
                        .getUserData()
                        .getTimeAdded()
                        .compareTo(o1.getSong().getUserData().getTimeAdded());
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

    /**
     * Writes a playlist file for the specified items.
     *
     * <p>Playlist generation events do not include information about which playlist is being
     * generated: progress is reported in terms of the total number of items in all playlists. This
     * method therefore takes two parameters - {@code startIndex} and {@code totalItems} that are
     * used to indicate the position of this sub-task relative to the larger process.</p>
     */
    private void writePlaylist(File playlistFile,
            List<SyncPlan.Item> items,
            int startIndex,
            int totalItems) throws IOException, UnsupportedTagException, InvalidDataException {

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n\n");

        int i = startIndex;

        for (SyncPlan.Item item : items) {
            writeItemToPlaylistBuffer(sb, item);
            bus.post(new GeneratePlaylistsProgressChangedEvent(this, ++i, totalItems));
        }

        Files.write(sb.toString(), playlistFile, Charsets.UTF_8);
    }

    private static void writeItemToPlaylistBuffer(StringBuilder sb, SyncPlan.Item item) {
        // Note: song length is specified as '-1' (indeterminate), as reading the real value from
        // the file is very slow (4-5 seconds on Android), and it's largely useless anyway.
        sb.append(String.format("#EXTINF:%d,%s - %s\n%s\n\n",
                -1,
                item.getSong().getName(),
                item.getSong().getArtistName(),
                item.getFile().getName()));
    }
}
