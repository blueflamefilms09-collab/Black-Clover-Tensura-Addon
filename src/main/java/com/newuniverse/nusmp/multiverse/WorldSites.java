package com.newuniverse.nusmp.multiverse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Where the Grimoire Towers and Library Ruins are. They are features (not structures), so vanilla /locate can't find them:
 * each one records itself when it generates (worldgen threads queue it; the server thread saves it), and every Grimoire Altar
 * a player uses is recorded as a tower too, so towers from before this version are found once someone visits them.
 */
public final class WorldSites extends SavedData {
    public enum Kind { TOWER, RUINS }

    public record Site(Kind kind, ResourceKey<Level> dimension, BlockPos pos) {}

    private static final ConcurrentLinkedQueue<Site> PENDING = new ConcurrentLinkedQueue<>();
    private final List<Site> sites = new ArrayList<>();

    /** Safe from worldgen threads. */
    public static void record(Kind kind, ResourceKey<Level> dimension, BlockPos pos) { PENDING.add(new Site(kind, dimension, pos.immutable())); }

    public static WorldSites get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldSites::new, WorldSites::load, null), "nusmp_world_sites");
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        WorldSites ws = get(event.getServer());
        Site s;
        while ((s = PENDING.poll()) != null) ws.add(s);
    }

    private void add(Site s) {
        for (Site o : sites) if (o.kind == s.kind && o.dimension.equals(s.dimension) && o.pos.distSqr(s.pos) < 32 * 32) return;   // same site
        sites.add(s);
        setDirty();
    }

    /** Nearest known site of this kind in the dimension. */
    public Optional<Site> nearest(Kind kind, ResourceKey<Level> dimension, BlockPos from) {
        Site best = null;
        double bestD = Double.MAX_VALUE;
        for (Site s : sites) {
            if (s.kind != kind || !s.dimension.equals(dimension)) continue;
            double d = s.pos.distSqr(from);
            if (d < bestD) { bestD = d; best = s; }
        }
        return Optional.ofNullable(best);
    }

    public int count(Kind kind) { int n = 0; for (Site s : sites) if (s.kind == kind) n++; return n; }

    private static WorldSites load(CompoundTag tag, HolderLookup.Provider provider) {
        WorldSites ws = new WorldSites();
        for (Tag t : tag.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Kind kind;
            try { kind = Kind.valueOf(c.getString("Kind")); } catch (IllegalArgumentException e) { continue; }
            ResourceLocation dim = ResourceLocation.tryParse(c.getString("Dim"));
            if (dim == null) continue;
            ws.sites.add(new Site(kind, ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(c.getLong("Pos"))));
        }
        return ws;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Site s : sites) {
            CompoundTag c = new CompoundTag();
            c.putString("Kind", s.kind.name());
            c.putString("Dim", s.dimension.location().toString());
            c.putLong("Pos", s.pos.asLong());
            list.add(c);
        }
        tag.put("Sites", list);
        return tag;
    }
}
