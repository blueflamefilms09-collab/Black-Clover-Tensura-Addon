package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.block.NUBlocks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 0.47 Kotodama "Devour" and the Zagred boss's void flood: underworld sludge that spreads over the ground and devours it.
 * <ul>
 *   <li><b>Spread:</b> a flood fill over surface blocks (solid, open above) from the centre, a few blocks a tick, out to the
 *       radius. Each block is swallowed into {@code underworld_matter}; small plants on top are eaten too. Air, fluids, block
 *       entities (chests, furnaces...) and unbreakable blocks are left alone.</li>
 *   <li><b>Contact:</b> every half second, everyone standing in it (not the owner, not allies) takes darkness damage, spiritual
 *       damage, and has magicules drained to the owner (Energy Drain).</li>
 *   <li><b>End:</b> when the time is up or the owner is gone, everything devoured comes back exactly as it was, unless the
 *       server's griefBlocks rule is on (then the ground stays eaten). A server stop puts everything back at once.</li>
 * </ul>
 */
public final class UnderworldMatter {
    private UnderworldMatter() {}

    static final class Session {
        final ServerLevel level;
        final LivingEntity owner;
        final BlockPos center;
        final int radius;
        final long until;
        final Map<BlockPos, BlockState> eaten = new LinkedHashMap<>();
        final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> seen = new HashSet<>();

        Session(ServerLevel level, LivingEntity owner, BlockPos center, int radius, long until) {
            this.level = level; this.owner = owner; this.center = center; this.radius = radius; this.until = until;
        }
    }

    private static final List<Session> SESSIONS = new ArrayList<>();
    static final int MAX_RADIUS = 24;

    /** Floods sludge out from near 'at'. Returns false if there is no ground to take. */
    public static boolean start(ServerLevel level, LivingEntity owner, BlockPos at, int radius, int ticks) {
        BlockPos c = surface(level, at);
        if (c == null) return false;
        Session s = new Session(level, owner, c, Math.min(MAX_RADIUS, radius), level.getGameTime() + ticks);
        s.frontier.add(c);
        s.seen.add(c);
        SESSIONS.add(s);
        VfxSpawn.send(level, VfxShape.KOTO_SLUDGE, c.getCenter().add(0, 0.55, 0), c.getCenter(), KotodamaWords.VIOLET, Math.min(ticks, 200), s.radius / 4f);
        level.playSound(null, c, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 2f, 0.5f);
        return true;
    }

    /** The nearest surface block at or below 'at' (looking up to 6 down and 3 up). */
    static BlockPos surface(ServerLevel level, BlockPos at) {
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos p = at.offset(0, dy, 0);
            if (takeable(level, p)) return p;
        }
        return null;
    }

    /** A surface block the sludge may swallow: solid ground with room above, no block entity, breakable, not a fluid. */
    static boolean takeable(ServerLevel level, BlockPos p) {
        if (!level.isLoaded(p)) return false;
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.is(NUBlocks.UNDERWORLD_MATTER.get()) || !s.getFluidState().isEmpty() || s.hasBlockEntity()) return false;
        if (s.getDestroySpeed(level, p) < 0 || !s.isCollisionShapeFullBlock(level, p)) return false;
        BlockState up = level.getBlockState(p.above());
        return up.isAir() || up.canBeReplaced() && up.getFluidState().isEmpty() && !up.hasBlockEntity();
    }

    /** True if an active session placed matter here (the block uses this to tell leftovers from live sludge). */
    public static boolean owns(ServerLevel level, BlockPos p) {
        for (Session s : SESSIONS) if (s.level == level && s.eaten.containsKey(p)) return true;
        return false;
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (SESSIONS.isEmpty()) return;
        for (Session s : new ArrayList<>(SESSIONS)) {
            long now = s.level.getGameTime();
            boolean ownerGone = s.owner == null || s.owner.isRemoved() || !s.owner.isAlive();
            if (now >= s.until || ownerGone) { end(s); continue; }
            spread(s, 6 + s.radius / 2);
            if (now % 10 == 0) touch(s);
        }
    }

    static void spread(Session s, int budget) {
        BlockState matter = NUBlocks.UNDERWORLD_MATTER.get().defaultBlockState();
        while (budget-- > 0 && !s.frontier.isEmpty()) {
            BlockPos p = s.frontier.poll();
            if (!takeable(s.level, p)) continue;
            BlockPos up = p.above();
            BlockState above = s.level.getBlockState(up);
            if (!above.isAir()) { s.eaten.put(up.immutable(), above); s.level.setBlock(up, Blocks.AIR.defaultBlockState(), 3); }
            s.eaten.put(p.immutable(), s.level.getBlockState(p));
            s.level.setBlock(p, matter, 3);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos n = p.offset(dx, dy, dz);
                double hx = n.getX() - s.center.getX(), hz = n.getZ() - s.center.getZ();
                if (hx * hx + hz * hz > s.radius * s.radius || !s.seen.add(n)) continue;
                s.frontier.add(n);
            }
        }
    }

    static void touch(Session s) {
        for (LivingEntity t : KotodamaWords.foes(s.owner, s.center.getCenter(), s.radius + 2)) {
            if (!s.level.getBlockState(t.blockPosition().below()).is(NUBlocks.UNDERWORLD_MATTER.get())
                    && !s.level.getBlockState(t.blockPosition()).is(NUBlocks.UNDERWORLD_MATTER.get())) continue;
            KotodamaWords.hurt(s.owner, t, 3f);
            KotodamaWords.spirit(s.owner, t, 1);
            EnergyBridge.drain(t, s.owner, 0.02);
        }
    }

    /** Puts back everything the session devoured (or leaves it eaten under griefBlocks), newest first. */
    static void end(Session s) {
        SESSIONS.remove(s);
        boolean keep = NUConfig.GRIEF.get();
        List<Map.Entry<BlockPos, BlockState>> list = new ArrayList<>(s.eaten.entrySet());
        for (int k = list.size() - 1; k >= 0; k--) {
            BlockPos p = list.get(k).getKey();
            BlockState now = s.level.getBlockState(p), was = list.get(k).getValue();
            if (now.is(NUBlocks.UNDERWORLD_MATTER.get())) s.level.setBlock(p, keep ? Blocks.AIR.defaultBlockState() : was, 3);
            else if (now.isAir() && !keep && !was.is(NUBlocks.UNDERWORLD_MATTER.get())) s.level.setBlock(p, was, 3);   // a devoured plant
        }
    }

    /** Ends every session this entity owns (the boss dying, a player logging out). */
    public static void endAll(LivingEntity owner) {
        for (Session s : new ArrayList<>(SESSIONS)) if (s.owner == owner) end(s);
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        for (Session s : new ArrayList<>(SESSIONS)) end(s);
    }
}
