package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The read side of the merged magics, shared by the server thread and the client thread of one JVM: the current definitions, their
 * cached blends, and the profiles registered in {@link MagicProfiles}. The server fills it from the world's SavedData
 * ({@link MergedMagicData}); a client fills it from the sync packet ({@link MergedSync}). The store inside is never modified after it is
 * published (a change publishes a fresh copy), so reading needs no lock. In an integrated server both sides publish equal copies.
 */
public final class MergedRegistry {
    private MergedRegistry() {}

    private static volatile MergeStore store = new MergeStore();
    private static final Map<String, MergedBlend> BLENDS = new ConcurrentHashMap<>();
    private static volatile Map<Integer, MergedDef> byKey = Map.of();          // the id keys of the published store (the effect layer asks for one per instance per frame)

    /** Publishes a new set of definitions: the profiles of removed ones go, those of new / changed ones are registered. */
    public static synchronized void replace(MergeStore next) {
        MergeStore old = store;
        Set<String> keep = new HashSet<>();
        for (MergedDef d : next.all()) {
            keep.add(d.id());
            MergedDef was = old.byId(d.id());
            if (was == null || !was.equals(d)) { BLENDS.remove(d.id()); MagicProfiles.register(new MergedProfile(d)); }
        }
        for (MergedDef d : old.all()) if (!keep.contains(d.id())) { BLENDS.remove(d.id()); MagicProfiles.unregister(d.id()); }
        Map<Integer, MergedDef> keys = new java.util.HashMap<>();
        for (MergedDef d : next.all()) keys.putIfAbsent(d.key(), d);          // first wins, as the store's own lookup does
        byKey = keys;
        store = next;
    }

    /** Publishes the definitions of encoded lines (the sync packet). */
    public static void replaceFrom(List<String> lines) { replace(MergeStore.decode(lines)); }

    /** No merged magics (a world was left). */
    public static void clear() { replace(new MergeStore()); BLENDS.clear(); }

    public static List<MergedDef> all() { return store.all(); }
    public static MergedDef byId(String id) { return store.byId(id); }
    public static MergedDef bySlot(int slot) { return store.bySlot(slot); }
    public static MergedDef byKey(int key) { return byKey.get(key); }
    public static int size() { return store.size(); }

    /** A number that changes whenever the definition of this slot changes (the page-list version of its book): -1 when the slot is empty. */
    public static int version(int slot) {
        MergedDef d = store.bySlot(slot);
        return d == null ? -1 : d.hashCode();
    }

    /** The blend of a definition (palette, soul, hit effects), cached; never null. */
    public static MergedBlend blend(MergedDef def) {
        MergedBlend b = BLENDS.get(def.id());
        if (b != null && b.def.equals(def)) return b;
        b = MergedBlend.of(def, parent -> {
            MagicProfile p = MagicProfiles.of(parent);
            return new MergedBlend.Part(parent, p.soulType(), p.hit() == null ? "PIERCE" : p.hit().name(), p.color());
        });
        BLENDS.put(def.id(), b);
        return b;
    }
}
