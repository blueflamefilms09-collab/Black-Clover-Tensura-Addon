package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a right-click is the only thing that starts a rest, and the answer to "is the block I clicked a rest anchor?" must be a hash lookup, not
 * a walk over the several hundred markers a sector keeps (a hundred rooms with a dozen markers each). The anchors (seat, hearth, bed) are
 * indexed by cell the first time a sector is asked about and the index is rebuilt whenever the sector's marker list no longer looks like the one
 * it was built from (another list instance, a different length, a different first or last marker: a regenerated sector), so a stale index can
 * never hand out a seat that is gone. Markers are the generator's saved data, so nothing here is saved; the cache is dropped when the server
 * stops. Server thread only.
 */
final class RestAnchors {
    private RestAnchors() {}

    private record Index(Sector owner, int size, long first, long last, Map<Long, Marker> byCell) {}

    private static final Map<Integer, Index> CACHE = new HashMap<>();

    /** The rest anchor (seat, hearth or bed marker) standing in {@code cell}, or null. */
    static Marker at(Sector sector, BlockPos cell) {
        if (sector == null || cell == null) return null;
        return index(sector).byCell().get(cell.asLong());
    }

    /** Every rest anchor of the sector (for the admin command). */
    static Collection<Marker> all(Sector sector) { return sector == null ? List.of() : index(sector).byCell().values(); }

    /** Forgets everything (the server stopped, or a test wants a clean slate). */
    static void clear() { CACHE.clear(); }

    private static Index index(Sector sector) {
        List<Marker> markers = sector.markers;
        int size = markers.size();
        long first = size == 0 ? 0L : markers.get(0).pos().asLong();
        long last = size == 0 ? 0L : markers.get(size - 1).pos().asLong();
        Index cached = CACHE.get(sector.id);
        if (cached != null && cached.owner() == sector && cached.size() == size && cached.first() == first && cached.last() == last) return cached;
        Map<Long, Marker> byCell = new HashMap<>();
        for (Marker m : new ArrayList<>(markers)) if (RestRules.isAnchor(m.kind())) byCell.putIfAbsent(m.pos().asLong(), m);
        Index built = new Index(sector, size, first, last, byCell);
        CACHE.put(sector.id, built);
        return built;
    }
}
