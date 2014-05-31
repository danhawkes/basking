package co.arcs.groove.basking.task;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item;
import co.arcs.groove.basking.task.BuildSyncPlanTask.SyncPlan.Item.Action;
import co.arcs.groove.thresher.Song;

public class WriteCacheFileTask implements Task<Integer> {

    static final String CACHE_FILENAME = ".gssynccache";

    private final File syncDir;
    private final SyncPlan syncPlan;
    private final List<Song> downloadedSongs;

    public WriteCacheFileTask(File syncDir, SyncPlan syncPlan, List<Song> downloadedSongs) {
        this.syncDir = syncDir;
        this.syncPlan = syncPlan;
        this.downloadedSongs = downloadedSongs;
    }

    @Override
    public Integer call() throws Exception {

        // Filter out items that were deleted, or that failed to download
        Collection<Item> items = Collections2.filter(syncPlan.getItems(), new Predicate<Item>() {
            @Override
            public boolean apply(@Nullable Item item) {
                if (item.getAction() == Action.LEAVE) {
                    return true;
                }
                //noinspection RedundantIfStatement
                if (item.getAction() == Action.DOWNLOAD && downloadedSongs.contains(item.getSong())) {
                    return true;
                }
                return false;
            }
        });

        Files.write(generateCacheFileData(items), new File(syncDir, CACHE_FILENAME));

        return items.size();
    }

    private static byte[] generateCacheFileData(Collection<Item> items) {

        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append(item.getSong().getId());
            sb.append("|");
            sb.append(item.getFile().getName());
            sb.append("\n");
        }
        return sb.toString().getBytes(Charsets.UTF_8);
    }

    /**
     * Returns a new line processor for the cache file. The processor generates map of song IDs to
     * file names.
     */
    static LineProcessor<Map<Integer, String>> newCacheFileLineProcessor() {
        return new LineProcessor<Map<Integer, String>>() {

            @SuppressWarnings("CanBeFinal")
            Map<Integer, String> map = Maps.newHashMap();

            @Override
            public boolean processLine(String line) throws IOException {
                String[] split = line.split("\\|");
                map.put(Integer.valueOf(split[0]), split[1]);
                return true;
            }

            @Override
            public Map<Integer, String> getResult() {
                return map;
            }
        };
    }
}
