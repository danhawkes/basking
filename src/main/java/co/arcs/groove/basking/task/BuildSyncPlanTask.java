package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import co.arcs.groove.basking.SyncService;
import co.arcs.groove.basking.Utils;
import co.arcs.groove.basking.event.Events.BuildSyncPlanFinishedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanProgressChangedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanStartedEvent;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.thresher.Song;

/**
 * Builds a plan of what needs to be done to synchronise the contents of the sync directory with a
 * collection of songs.
 */
public class BuildSyncPlanTask implements Task<SyncPlan> {

    public static class SyncPlan {

        private final ImmutableList<Item> items;
        private final int download;
        private final int delete;
        private final int leave;

        public SyncPlan(List<Item> items) {
            this.items = ImmutableList.copyOf(items);
            int delete = 0;
            int download = 0;
            int leave = 0;
            for (Item item : items) {
                if (item.action == Action.DELETE) {
                    delete++;
                } else if (item.action == Action.DOWNLOAD) {
                    download++;
                } else if (item.action == Action.LEAVE) {
                    leave++;
                }
            }
            this.download = download;
            this.delete = delete;
            this.leave = leave;
        }

        public ImmutableList<Item> getItems() {
            return items;
        }

        public int getToDownload() {
            return download;
        }

        public int getToDelete() {
            return delete;
        }

        public int getToLeave() {
            return leave;
        }

        public static class Item {

            public static enum Action {
                DOWNLOAD, DELETE, LEAVE
            }

            private final File file;
            private final Action action;
            private final Song song;

            public Item(File file, Action action, @Nullable Song songToSync) {
                this.file = file;
                this.action = action;
                this.song = songToSync;
            }

            public File getFile() {
                return file;
            }

            public Action getAction() {
                return action;
            }

            public Song getSong() {
                return song;
            }
        }
    }

    public static final String CACHE_FILENAME = WriteCacheFileTask.CACHE_FILENAME;
    private final EventBus bus;
    private final File syncDir;
    private final Set<Song> songs;

    /**
     * Creates a new sync plan task.
     *
     * @param bus
     *         The bus on which to post events.
     * @param syncDir
     *         The directory to be synchronised.
     * @param songs
     *         The songs that {@code syncDir} should contain.
     */
    public BuildSyncPlanTask(EventBus bus, File syncDir, Set<Song> songs) {
        this.bus = bus;
        this.syncDir = syncDir;
        this.songs = songs;
    }

    @Override
    public SyncPlan call() throws Exception {

        bus.post(new BuildSyncPlanStartedEvent(this));

        SyncPlan syncPlan = buildSyncPlanUsingCache(songs);

        if (syncPlan == null) {
            syncPlan = buildSyncPlanByAnalysingFiles(songs);
        }

        bus.post(new BuildSyncPlanFinishedEvent(this,
                syncPlan.download,
                syncPlan.delete,
                syncPlan.leave));

        return syncPlan;
    }

    private SyncPlan buildSyncPlanByAnalysingFiles(Set<Song> songs) {

        // Get a list of all non-hidden mp3 files
        List<File> files = Lists.newArrayList(syncDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String arg1) {
                return !arg1.startsWith(".") && arg1.endsWith(SyncService.FINISHED_FILE_EXTENSION);
            }
        }));

        int progress = 0;
        bus.post(new BuildSyncPlanProgressChangedEvent(this, progress++, files.size()));

        Map<Integer, File> existingSongs = Maps.newHashMap();
        for (File f : files) {
            // This operation takes far longer than everything else, so is used as the measure of progress
            Integer id = Utils.decodeId(f);
            if (id != Utils.ID_NONE) {
                existingSongs.put(id, f);
            }
            bus.post(new BuildSyncPlanProgressChangedEvent(this, progress++, files.size()));
        }

        Map<Integer, Song> wantedSongs = Maps.newHashMap();
        for (Song s : songs) {
            wantedSongs.put(s.getId(), s);
        }

        Builder<Item> syncPlanItems = ImmutableList.builder();

        for (Integer id : wantedSongs.keySet()) {
            if (existingSongs.containsKey(id)) {
                // Wanted song already exists, so leave as is
                syncPlanItems.add(new Item(existingSongs.get(id),
                        Action.LEAVE,
                        wantedSongs.get(id)));
                existingSongs.remove(id);
            } else {
                // Wanted song is absent, so download
                syncPlanItems.add(new Item(new File(syncDir,
                        Utils.getDiskName(wantedSongs.get(id))),
                        Action.DOWNLOAD,
                        wantedSongs.get(id)
                ));
            }
        }

        // Remaining songs are present but unwanted, so delete
        for (Integer id : existingSongs.keySet()) {
            syncPlanItems.add(new Item(existingSongs.get(id), Action.DELETE, null));
        }
        return new SyncPlan(syncPlanItems.build());
    }

    /**
     * Reads the cache file and returns a sync plan based on its contents.
     *
     * @return A sync plan, or null if the cache file does not exist.
     */
    @Nullable
    private SyncPlan buildSyncPlanUsingCache(Collection<Song> songs) throws IOException {
        File cacheFile = new File(syncDir, CACHE_FILENAME);
        if (cacheFile.exists()) {

            ImmutableList.Builder<SyncPlan.Item> items = ImmutableList.builder();

            InputStream is = new BufferedInputStream(new FileInputStream(cacheFile));
            Map<Integer, String> cacheMap = CharStreams.readLines(new InputStreamReader(is),
                    WriteCacheFileTask.newCacheFileLineProcessor());
            is.close();

            int progress = 0;
            bus.post(new BuildSyncPlanProgressChangedEvent(this, 0, songs.size()));

            for (Song song : songs) {
                if (cacheMap.containsKey(song.getId())) {
                    // Wanted song is in cache, so leave as is
                    items.add(new Item(new File(syncDir, cacheMap.get(song.getId())),
                            Action.LEAVE,
                            song));
                    cacheMap.remove(song.getId());
                } else {
                    // Wanted song is absent, so download
                    items.add(new Item(new File(syncDir, Utils.getDiskName(song)),
                            Action.DOWNLOAD,
                            song));
                }
                bus.post(new BuildSyncPlanProgressChangedEvent(this, ++progress, songs.size()));
            }

            // Unwanted stuff that's in the cache should be removed
            for (String fileName : cacheMap.values()) {
                items.add(new Item(new File(syncDir, fileName), Action.DELETE, null));
            }

            return new SyncPlan(items.build());
        } else {
            return null;
        }
    }
}
