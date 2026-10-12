package com.newuniverse.nusmp.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Dungeon Maw (docs/living_dungeon.md section 2). Casting spends magicules into the air; the thickness of the air over a region of
 * 4x4 chunks is tracked ({@link #add}); when it crosses the threshold the ground there tears open into a mouth of dense mana wrapped in
 * roots. Walk in (through a gap you chop with an axe) and you fall into the Dungeon. When every root is gone, or the Maw's time is out,
 * it closes.
 */
public final class DungeonMaw {
    private DungeonMaw() {}

    /** Decay of the ambient density per tick (0.025 = 30 a minute). */
    private static final double DECAY = 0.025;

    private static long region(BlockPos p) { return ChunkPos.asLong(p.getX() >> 6, p.getZ() >> 6); }

    // ---------------------------------------------------------------- ambient magicule
    /** A caster spends magic here: the air of this region thickens. Opens a Maw if it has become too thick. */
    public static void add(ServerPlayer p, double amount) {
        if (amount <= 0 || LivingDungeon.in(p) || !DungeonConfig.get(DungeonConfig.ENABLED)) return;
        double threshold = DungeonConfig.get(DungeonConfig.MAW_THRESHOLD);
        if (threshold <= 0) return;
        MinecraftServer server = p.getServer();
        if (server == null || server.getLevel(LivingDungeon.DIMENSION) == null) return;
        DungeonSave s = DungeonSave.get(server);
        long now = p.level().getGameTime(), key = region(p.blockPosition());
        double[] a = s.ambient.computeIfAbsent(key, k -> new double[]{0, now});
        a[0] = Math.max(0, a[0] - DECAY * (now - a[1])) + amount;
        a[1] = now;
        s.setDirty();
        long cooldown = DungeonConfig.get(DungeonConfig.MAW_COOLDOWN_MINUTES) * 1200L;
        if (a[0] >= threshold && now - s.lastMaw >= cooldown && p.level() instanceof ServerLevel sl && open(sl, p.blockPosition())) {
            a[0] = threshold * 0.25;
            s.lastMaw = now;
        }
    }

    // ---------------------------------------------------------------- opening
    /** Tears a Maw open on the surface near this position. Returns false if no fitting ground was found. */
    public static boolean open(ServerLevel level, BlockPos near) {
        var rnd = level.getRandom();
        for (int tries = 0; tries < 12; tries++) {
            int x = near.getX() + rnd.nextInt(25) - 12, z = near.getZ() + rnd.nextInt(25) - 12;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(at.below());
            if (!below.getFluidState().isEmpty() || below.isAir() || !below.isFaceSturdy(level, at.below(), Direction.UP)) continue;
            if (!level.getBlockState(at).canBeReplaced() && !level.getBlockState(at).isAir()) continue;
            build(level, at);
            return true;
        }
        return false;
    }

    private static void build(ServerLevel level, BlockPos at) {
        MinecraftServer server = level.getServer();
        BlockState root = DungeonBlocks.MAW_ROOT.get().defaultBlockState();
        level.setBlock(at, DungeonBlocks.DUNGEON_MAW.get().defaultBlockState(), 3);
        List<Long> roots = new ArrayList<>();
        var rnd = level.getRandom();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int h = 1 + rnd.nextInt(3);
                for (int k = 0; k < h; k++) {
                    BlockPos r = at.offset(dx, k, dz);
                    BlockState cur = level.getBlockState(r);
                    if (cur.isAir() || cur.canBeReplaced() || cur.is(net.minecraft.tags.BlockTags.LEAVES)) { level.setBlock(r, root, 3); roots.add(r.asLong()); }
                }
            }
        }
        BlockPos cap = at.above();                                                  // a cap over the mouth: the roots must be cut to get in
        if (level.getBlockState(cap).isAir() || level.getBlockState(cap).canBeReplaced()) { level.setBlock(cap, root, 3); roots.add(cap.asLong()); }
        long now = level.getGameTime(), close = now + DungeonConfig.get(DungeonConfig.MAW_MINUTES) * 1200L;
        DungeonSave s = DungeonSave.get(server);
        s.maws.add(new DungeonSave.Maw(level.dimension().location().toString(), at.asLong(), close, roots.stream().mapToLong(Long::longValue).toArray()));
        s.setDirty();
        level.playSound(null, at, SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 2.5f, 0.6f);
        level.sendParticles(ParticleTypes.SCULK_SOUL, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 60, 1.5, 1.2, 1.5, 0.05);
        broadcast(server, Component.literal("The ground groans. A Dungeon Maw has torn open somewhere near (" + round(at.getX()) + ", " + round(at.getZ())
                + "). Cut its roots down before whatever lives below finds the way up.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private static int round(int v) { return Math.round(v / 100f) * 100; }

    // ---------------------------------------------------------------- closing
    /** Every second: Maws pulse; one whose roots are gone or whose time is out closes. */
    public static void tick(MinecraftServer server) {
        DungeonSave s = DungeonSave.get(server);
        if (s.maws.isEmpty()) return;
        boolean changed = false;
        for (Iterator<DungeonSave.Maw> it = s.maws.iterator(); it.hasNext(); ) {
            DungeonSave.Maw m = it.next();
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(m.dim())));
            if (level == null) continue;
            BlockPos at = BlockPos.of(m.pos());
            if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)) continue;                  // not loaded: it waits
            boolean gone = !level.getBlockState(at).is(DungeonBlocks.DUNGEON_MAW.get());
            int left = 0;
            for (long r : m.roots()) if (level.getBlockState(BlockPos.of(r)).is(DungeonBlocks.MAW_ROOT.get())) left++;
            if (gone || left == 0 || level.getGameTime() >= m.closeAt()) {
                boolean cut = left == 0 && !gone;
                close(server, level, m, cut ? "its roots were cut down" : "its time ran out");
                it.remove();
                changed = true;
                MawReward.onClosed(level, at, cut, m.closeAt(), level.getGameTime());       // 1.19.0: whoever cut the roots is rewarded
            } else if (level.getGameTime() % 100 < 20) {
                level.sendParticles(ParticleTypes.PORTAL, at.getX() + 0.5, at.getY() + 0.7, at.getZ() + 0.5, 12, 0.7, 0.7, 0.7, 0.4);
            }
        }
        if (changed) s.setDirty();
    }

    private static void close(MinecraftServer server, ServerLevel level, DungeonSave.Maw m, String why) {
        BlockPos at = BlockPos.of(m.pos());
        DungeonGuard.internal(() -> {                                               // a Maw opened with the admin command inside the Dungeon must still be able to close
            if (level.getBlockState(at).is(DungeonBlocks.DUNGEON_MAW.get())) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            for (long r : m.roots()) {
                BlockPos rp = BlockPos.of(r);
                if (level.getBlockState(rp).is(DungeonBlocks.MAW_ROOT.get())) level.setBlock(rp, Blocks.AIR.defaultBlockState(), 3);
            }
        });
        level.playSound(null, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 1.5f, 0.5f);
        level.sendParticles(ParticleTypes.SCULK_SOUL, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 40, 1.0, 1.0, 1.0, 0.05);
        broadcast(server, Component.literal("The Dungeon Maw near (" + round(at.getX()) + ", " + round(at.getZ()) + ") has closed: " + why + ".").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Closes every Maw at once (admin command). Returns how many. */
    public static int closeAll(MinecraftServer server) {
        DungeonSave s = DungeonSave.get(server);
        int n = s.maws.size();
        for (DungeonSave.Maw m : new ArrayList<>(s.maws)) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(m.dim())));
            if (level != null) {
                close(server, level, m, "an admin closed it");
                MawReward.onClosed(level, BlockPos.of(m.pos()), false, m.closeAt(), level.getGameTime());   // drops the credits, no reward
            }
        }
        s.maws.clear();
        s.setDirty();
        return n;
    }

    /** An ominous message to everyone, unless the config turns them off. */
    public static void broadcast(MinecraftServer server, Component msg) {
        if (DungeonConfig.get(DungeonConfig.BROADCAST)) server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
