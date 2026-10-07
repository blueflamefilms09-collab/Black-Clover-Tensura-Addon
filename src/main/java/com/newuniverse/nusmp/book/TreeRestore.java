package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 0.48 fix: the World Tree's real trees are taken back by this saved list, so they also go away after a server restart (they used
 * to stay when the restart came before the spell ended). A block is put back when it is still the same <i>kind</i> of block the
 * tree placed (leaves change their distance state as the tree settles; comparing the exact state left those leaves behind).
 * Anything built in the meantime, a different block, is never touched.
 */
public final class TreeRestore extends SavedData {
    private record Undo(BlockPos pos, BlockState placed, BlockState before) {}
    private record Job(ResourceKey<Level> dim, long due, List<Undo> blocks) {}

    private final List<Job> jobs = new ArrayList<>();

    public static TreeRestore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TreeRestore::new, TreeRestore::load, null), "nusmp_world_trees");
    }

    /** Schedules a grown tree to be taken back at game time 'due'. */
    public void add(ServerLevel level, long due, Map<BlockPos, BlockState> placed, Map<BlockPos, BlockState> before) {
        List<Undo> list = new ArrayList<>();
        for (var e : placed.entrySet()) list.add(new Undo(e.getKey().immutable(), e.getValue(), before.get(e.getKey())));
        jobs.add(new Job(level.dimension(), due, list));
        setDirty();
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 9) return;
        TreeRestore data = get(server);
        if (data.jobs.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<Job> it = data.jobs.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            if (now < j.due()) continue;
            ServerLevel level = server.getLevel(j.dim());
            if (level != null) undo(level, j);
            it.remove();
            data.setDirty();
        }
    }

    static void undo(ServerLevel level, Job j) {
        Vec3 at = null;
        for (Undo u : j.blocks()) {
            if (u.before() == null) continue;
            BlockState now = level.getBlockState(u.pos());
            if (!now.is(u.placed().getBlock())) continue;                     // replaced by someone: leave it
            level.setBlock(u.pos(), u.before(), 2 | 16);
            if (at == null) at = Vec3.atBottomCenterOf(u.pos());
        }
        if (at != null) VfxSpawn.send(level, VfxShape.TREE_ROOTS, at, at.add(0, 1, 0), WorldTreeBook.EMERALD, 30, 2f);
    }

    private static TreeRestore load(CompoundTag tag, HolderLookup.Provider provider) {
        TreeRestore r = new TreeRestore();
        HolderGetter<Block> blocks = provider.lookupOrThrow(Registries.BLOCK);
        ListTag list = tag.getList("Jobs", Tag.TAG_COMPOUND);
        for (int k = 0; k < list.size(); k++) {
            CompoundTag t = list.getCompound(k);
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim")));
            List<Undo> undo = new ArrayList<>();
            ListTag bl = t.getList("Blocks", Tag.TAG_COMPOUND);
            for (int b = 0; b < bl.size(); b++) {
                CompoundTag u = bl.getCompound(b);
                undo.add(new Undo(BlockPos.of(u.getLong("Pos")), NbtUtils.readBlockState(blocks, u.getCompound("Placed")),
                        NbtUtils.readBlockState(blocks, u.getCompound("Before"))));
            }
            r.jobs.add(new Job(dim, t.getLong("Due"), undo));
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Job j : jobs) {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", j.dim().location().toString());
            t.putLong("Due", j.due());
            ListTag bl = new ListTag();
            for (Undo u : j.blocks()) {
                if (u.before() == null) continue;
                CompoundTag c = new CompoundTag();
                c.putLong("Pos", u.pos().asLong());
                c.put("Placed", NbtUtils.writeBlockState(u.placed()));
                c.put("Before", NbtUtils.writeBlockState(u.before()));
                bl.add(c);
            }
            t.put("Blocks", bl);
            list.add(t);
        }
        tag.put("Jobs", list);
        return tag;
    }
}
