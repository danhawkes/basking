package co.arcs.groove.basking.task;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.eventbus.EventBus;

import java.util.Set;

import co.arcs.groove.basking.event.impl.GetSongsToSyncEvent;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;
import co.arcs.groove.thresher.User;

public class GetSongsToSyncTask implements Task<Set<Song>> {

    private final EventBus bus;
    private final Client client;
    private final String username;
    private final String password;

    public GetSongsToSyncTask(EventBus bus, Client client, String username, String password) {
        this.bus = bus;
        this.client = client;
        this.username = username;
        this.password = password;
    }

    @Override
    public Set<Song> call() throws Exception {

        bus.post(new GetSongsToSyncEvent.Started(this));
        bus.post(new GetSongsToSyncEvent.ProgressChanged(this, 0, 3));

        User user = client.login(username, password);

        bus.post(new GetSongsToSyncEvent.ProgressChanged(this, 1, 3));

        // The library.get() response contains favorited songs that do not have
        // the 'favorited' property set. To work around this, favorites are
        // removed from the library set and replaced with instances from the
        // favorites set. The result is that all songs within the resulting set
        // are 'collected', and some are 'favorited'.
        ImmutableSet<Song> library = ImmutableSet.copyOf(user.library.get());

        bus.post(new GetSongsToSyncEvent.ProgressChanged(this, 2, 3));

        ImmutableSet<Song> favorites = ImmutableSet.copyOf(user.favorites.get());

        SetView<Song> nonFavorites = Sets.difference(library, favorites);
        SetView<Song> all = Sets.union(nonFavorites, favorites);

        bus.post(new GetSongsToSyncEvent.ProgressChanged(this, 3, 3));

        bus.post(new GetSongsToSyncEvent.Finished(this, all.size()));

        return Sets.newHashSet(all);
    }
}
