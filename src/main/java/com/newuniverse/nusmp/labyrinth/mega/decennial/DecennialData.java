package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Why: what the Decennial feature must remember across a restart is small and specific: which halls it has built (or is part way through building) and where, which of their interactive elements
 * the party has already broken (so a reload does not put a tombstone back that was smashed), whether the entry is sealed right now, which boss belongs to which hall, and which bosses have already
 * paid their item (a boss pays once per hall, whatever happens). It is one saved file, {@code data/nusmp_decennial.dat} in the OVERWORLD's storage (the always-loaded place the other mega
 * records use). It adds nothing to {@code Sector}, whose save format belongs to the generator. An old world has no such file, which reads as "no hall built, nobody paid", exactly right.
 * Server thread only.
 */
public final class DecennialData extends SavedData {
    public static final String NAME = "nusmp_decennial";

    /**
     * One hall. {@code key} is {@code s<sector id>} for a hall in a floor of the Mega-Dungeon and {@code x<n>} for a hall an admin stood up alone; {@code cursor} is how many plan cells are placed (a
     * hall that is not {@code built} resumes there); {@code broken} holds the ids of the elements the party has broken; {@code sealed} says the entry bars are in place right now.
     */
    public static final class Site {
        public final String key;
        public int sectorId = -1, level, cx, y0, cz, size, rot, cursor;
        public String theme = "", dim = "";
        public boolean built, sealed, defeated, standalone, gateOpen;
        public long sealedAt;
        public UUID boss;
        public final Set<Integer> broken = new TreeSet<>();

        public Site(String key) { this.key = key; }

        public ArenaFrame frame() { return new ArenaFrame(cx, y0, cz, size, rot); }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("key", key);
            t.putInt("sector", sectorId);
            t.putInt("level", level);
            t.putString("theme", theme);
            t.putString("dim", dim);
            t.putIntArray("frame", new int[] {cx, y0, cz, size, rot});
            t.putInt("cursor", cursor);
            t.putBoolean("built", built);
            t.putBoolean("sealed", sealed);
            t.putBoolean("defeated", defeated);
            t.putBoolean("standalone", standalone);
            t.putBoolean("gateOpen", gateOpen);
            t.putLong("sealedAt", sealedAt);
            if (boss != null) t.putUUID("boss", boss);
            t.putIntArray("broken", broken.stream().mapToInt(Integer::intValue).toArray());
            return t;
        }

        public static Site load(CompoundTag t) {
            Site s = new Site(t.getString("key"));
            s.sectorId = t.contains("sector") ? t.getInt("sector") : -1;
            s.level = t.getInt("level");
            s.theme = t.getString("theme");
            s.dim = t.getString("dim");
            int[] f = t.getIntArray("frame");
            if (f.length == 5) {
                s.cx = f[0];
                s.y0 = f[1];
                s.cz = f[2];
                s.size = f[3];
                s.rot = f[4];
            }
            s.cursor = t.getInt("cursor");
            s.built = t.getBoolean("built");
            s.sealed = t.getBoolean("sealed");
            s.defeated = t.getBoolean("defeated");
            s.standalone = t.getBoolean("standalone");
            s.gateOpen = t.getBoolean("gateOpen");
            s.sealedAt = t.getLong("sealedAt");
            s.boss = t.hasUUID("boss") ? t.getUUID("boss") : null;
            for (int id : t.getIntArray("broken")) s.broken.add(id);
            return s;
        }
    }

    private final Map<String, Site> sites = new LinkedHashMap<>();
    private final Set<String> paid = new HashSet<>();
    private int nextStandalone;

    public DecennialData() {}

    public static DecennialData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DecennialData::new, DecennialData::load, null), NAME);
    }

    // ---------------------------------------------------------------- sites
    public Collection<Site> sites() { return Collections.unmodifiableCollection(sites.values()); }

    public Site site(String key) { return sites.get(key); }

    public Site siteOfSector(int sectorId) { return sectorId < 0 ? null : sites.get("s" + sectorId); }

    public static String keyOfSector(int sectorId) { return "s" + sectorId; }

    /** Adds a site (replacing one with the same key) and marks the file dirty. */
    public void put(Site s) {
        sites.put(s.key, s);
        setDirty();
    }

    public Site remove(String key) {
        Site s = sites.remove(key);
        if (s != null) setDirty();
        return s;
    }

    public String newStandaloneKey() {
        String k;
        do { k = "x" + nextStandalone++; } while (sites.containsKey(k));
        setDirty();
        return k;
    }

    /** The site whose footprint holds the column in {@code dim}, or null. */
    public Site siteAt(String dim, int x, int z) {
        for (Site s : sites.values()) {
            if (s.size > 0 && s.dim.equals(dim) && Math.abs(x - s.cx) <= s.size / 2 && Math.abs(z - s.cz) <= s.size / 2) return s;
        }
        return null;
    }

    public List<Site> unfinished() {
        List<Site> out = new ArrayList<>();
        for (Site s : sites.values()) if (!s.built) out.add(s);
        return out;
    }

    // ---------------------------------------------------------------- the memory of who has paid
    /** True the first time a hall's boss claims its item. */
    public boolean claimLoot(String key) {
        boolean first = paid.add(key);
        if (first) setDirty();
        return first;
    }

    public boolean paid(String key) { return paid.contains(key); }

    // ---------------------------------------------------------------- save
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Site s : sites.values()) list.add(s.save());
        tag.put("sites", list);
        ListTag p = new ListTag();
        for (String k : paid) p.add(StringTag.valueOf(k));
        tag.put("paid", p);
        tag.putInt("nextStandalone", nextStandalone);
        return tag;
    }

    public static DecennialData load(CompoundTag tag, HolderLookup.Provider registries) {
        DecennialData d = new DecennialData();
        ListTag list = tag.getList("sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Site s = Site.load(list.getCompound(i));
            if (!s.key.isEmpty()) d.sites.put(s.key, s);
        }
        ListTag p = tag.getList("paid", Tag.TAG_STRING);
        for (int i = 0; i < p.size(); i++) d.paid.add(p.getString(i));
        d.nextStandalone = tag.getInt("nextStandalone");
        return d;
    }
}
