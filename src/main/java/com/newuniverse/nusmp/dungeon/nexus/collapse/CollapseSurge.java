package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultSave;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultTerrain;
import com.newuniverse.nusmp.dungeon.nexus.vault.WarpPlan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Spatial Surge of one collapse (the glue of {@link SurgePlan}): while the Dungeon comes down, the space inside it surges, and every
 * 12 to 20 seconds a tear opens under each player who is not anchored, shows for two seconds (a spiral of portal light, a sound, a line over
 * the hotbar) and throws them 8 to 24 blocks across the floor, dizzy for three seconds. The rules that keep it a cost in seconds and never a
 * danger are {@link SurgePlan}'s: it never carries a player more than ten blocks farther from the nearest way out, never onto ground the
 * stone has swept or is about to, never to a place they could not walk back from (a sealed Treasury), never within twenty seconds of the
 * end or near a Root Gate, and a Band of the Spatial Anchor ignores it. The place to land comes from the vault package's search for the
 * Warp Trap ({@link WarpPlan}); nothing is loaded, nothing is built. The Dungeon's hazards switch (config dungeon.hazards) turns it off
 * with the other spatial distortions.
 */
final class CollapseSurge {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();
    /** How many places a throw tests for a way back before it gives up (each test is a bounded walk). */
    private static final int WALK_TESTS = 3;

    private final ServerLevel level;
    private final CollapseState s;
    /** The clock of each player inside; one who leaves, dies or logs out loses theirs (and the tear that was open). */
    private final Map<UUID, SurgePlan.Clock> clocks = new HashMap<>();
    private int thrown, fizzled;

    CollapseSurge(ServerLevel level, CollapseState s) {
        this.level = level;
        this.s = s;
    }

    /** Players thrown so far, and tears that found no place to throw to. */
    int thrown() { return thrown; }

    int fizzled() { return fizzled; }

    /** Has this player a tear open under them (so their action bar is the tear's)? */
    boolean tearing(UUID id) {
        SurgePlan.Clock c = clocks.get(id);
        return c != null && c.open();
    }

    /** Forgets every clock (the collapse is over, or surges are off). */
    void clear() { clocks.clear(); }

    private static void warnOnce(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] The collapse's spatial surge could not {}; going on without it", what, failure);
    }

    /** Is the player wearing an attuned Band of the Spatial Anchor? (The relic code failing counts as not.) */
    private static boolean anchored(ServerPlayer p) {
        try {
            return Relics.anchored(p);
        } catch (RuntimeException | LinkageError unavailable) {
            return false;
        }
    }

    // ---------------------------------------------------------------- every few ticks
    /**
     * One look at the players inside, a few times a second: each is stepped on the clock of their own, and whatever it says is done.
     * `ways` are the Root Gates that stand (else the exits).
     */
    void tick(long now, List<ServerPlayer> inside, List<int[]> ways, CollapseSweep order) {
        if (!DungeonConfig.get(DungeonConfig.HAZARDS)) {
            clocks.clear();
            return;
        }
        long elapsed = s.elapsed(now);
        Set<UUID> here = new HashSet<>();
        for (ServerPlayer p : inside) {
            UUID u = p.getUUID();
            here.add(u);
            SurgePlan.Clock c = clocks.computeIfAbsent(u, k -> new SurgePlan.Clock(s.id * 31L + u.getMostSignificantBits() * 17L + u.getLeastSignificantBits()));
            boolean exempt = p.isCreative() || p.isSpectator() || !p.isAlive();
            try {
                SurgePlan.Act act = c.step(elapsed, exempt, !exempt && anchored(p), !exempt && SurgePlan.nearGate(ways, p.getX(), p.getY(), p.getZ()));
                act(p, c, act, ways, order, elapsed);
            } catch (RuntimeException | LinkageError failure) {
                warnOnce("play a surge for a player", failure);
                clocks.remove(u);
            }
        }
        clocks.keySet().retainAll(here);
    }

    private void act(ServerPlayer p, SurgePlan.Clock c, SurgePlan.Act act, List<int[]> ways, CollapseSweep order, long elapsed) {
        switch (act) {
            case TEAR -> {
                level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.5f);
                CollapseUi.bar(p, CollapseText.SURGE, ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
                spiral(p, c, elapsed);
            }
            case SPIRAL -> spiral(p, c, elapsed);
            case WARP -> fling(p, ways, order, elapsed);
            case HELD -> CollapseUi.bar(p, CollapseText.SURGE_HELD, ChatFormatting.GRAY);
            case CLOSE -> CollapseUi.bar(p, CollapseText.SURGE_CLOSED, ChatFormatting.GRAY);
            default -> { }
        }
    }

    /** A spiral of portal light winding in and up round the player as the throw comes near, and a ring of the reverse kind above. */
    private void spiral(ServerPlayer p, SurgePlan.Clock c, long elapsed) {
        double left = Math.max(0.0, Math.min(1.0, (c.due() - elapsed) / (double) SurgePlan.WARNING));
        double radius = 0.5 + 1.2 * left;
        double base = (elapsed % 30L) / 30.0 * Math.PI * 2.0;
        for (int i = 0; i < 6; i++) {
            double a = base + i * (Math.PI / 3.0), h = i * 0.35;
            level.sendParticles(ParticleTypes.PORTAL, p.getX() + Math.cos(a) * radius, p.getY() + 0.1 + h, p.getZ() + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + Math.cos(a + Math.PI) * radius * 0.6, p.getY() + 1.9 - h, p.getZ() + Math.sin(a + Math.PI) * radius * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ---------------------------------------------------------------- the throw
    /** Throws the player to a safe place 8 to 24 blocks away, if there is one; if not the tear closes on nothing. */
    private void fling(ServerPlayer p, List<int[]> ways, CollapseSweep order, long elapsed) {
        BlockPos to = destination(p, ways, order, elapsed);
        if (to == null) {
            fizzled++;
            level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.5, 0.1, 0.5, 0.02);
            CollapseUi.bar(p, CollapseText.SURGE_FIZZLED, ChatFormatting.GRAY);
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.6);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.6f);
        p.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SurgePlan.NAUSEA, 0, false, true));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        CollapseUi.bar(p, CollapseText.SURGE_LAND, ChatFormatting.DARK_PURPLE);
        thrown++;
    }

    /**
     * A standing place 8 to 24 blocks from the player, inside the collapsing ground and above its floor, that {@link SurgePlan} allows (not on
     * swept ground, not farther from the exits by more than ten blocks, not within six blocks of the old spot, on the level they stand on)
     * and that is not inside a hidden room and can be walked back from; null if the vault search's tries find none. Only loaded chunks are
     * read. (A player swimming is thrown to water without the walk test: water joins.)
     */
    private BlockPos destination(ServerPlayer p, List<int[]> ways, CollapseSweep order, long elapsed) {
        VaultSave sites = sites();
        if (sites == null) return null;                                                            // the hidden rooms cannot be told: no throw rather than one into a vault
        VaultTerrain terrain = new Terrain(level);
        boolean wet = level.getFluidState(p.blockPosition()).is(FluidTags.WATER);
        double before = SurgePlan.exitDistance(ways, p.getX(), p.getZ());
        int limit = CollapsePlan.warnLimit(elapsed, order.open());
        int px = p.getBlockX(), py = p.getBlockY(), pz = p.getBlockZ();
        SurgePlan.Ground ground = (gx, gy, gz) -> terrain.at(gx, gy, gz) == VaultTerrain.AIR && terrain.at(gx, gy + 1, gz) == VaultTerrain.AIR && VaultTerrain.solid(terrain.at(gx, gy - 1, gz));
        int walks = 0;
        for (WarpPlan.Offset o : WarpPlan.offsets(CollapsePlan.mix(s.id * 31L + p.getId(), elapsed), WarpPlan.TRIES)) {
            if (!SurgePlan.moveOk(o.dx(), o.dz())) continue;
            int x = px + o.dx(), z = pz + o.dz();
            if (!s.fp.contains(x, z)) continue;
            if (!SurgePlan.columnAllowed(order.indexOf(x, z), s.cursor, limit, order.open(), before, SurgePlan.exitDistance(ways, x + 0.5, z + 0.5))) continue;
            int y = WarpPlan.spotInColumn(terrain, x, py + o.dy(), z, wet);
            if (y == Integer.MIN_VALUE || !SurgePlan.spotAllowed(y, s.floorY, s.topY, x - px, y - py, z - pz)) continue;
            if (!wet && !CollapseGround.standable(level, x, y, z)) continue;
            if (sites.insideAnySite(x, y, z, 1)) continue;
            if (!wet) {                                                                            // a place the player can walk back from, not a sealed room or an island
                if (walks >= WALK_TESTS) break;                                                    // enough places tested: the tear closes on nothing
                walks++;
                if (!SurgePlan.connected(ground, x, y, z, px, py, pz)) continue;
            }
            return new BlockPos(x, y, z);
        }
        return null;
    }

    private VaultSave sites() {
        try {
            return VaultSave.get(level.getServer());
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("read where the hidden rooms are", failure);
            return null;
        }
    }

    /** The Dungeon as the vault search sees it: one classified block at a time, only from loaded chunks (the vault package's own adapter is not public). */
    private static final class Terrain implements VaultTerrain {
        private final ServerLevel level;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private int chunkX = Integer.MIN_VALUE, chunkZ;
        private LevelChunk chunk;

        Terrain(ServerLevel level) { this.level = level; }

        @Override
        public int at(int x, int y, int z) {
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return OTHER;
            int cx = x >> 4, cz = z >> 4;
            if (chunk == null || cx != chunkX || cz != chunkZ) {
                LevelChunk c = level.getChunkSource().getChunkNow(cx, cz);
                if (c == null) return UNKNOWN;
                chunk = c;
                chunkX = cx;
                chunkZ = cz;
            }
            return classify(chunk.getBlockState(pos.set(x, y, z)));
        }

        private static int classify(BlockState st) {
            if (st.isAir()) return AIR;
            FluidState f = st.getFluidState();
            boolean motion = st.blocksMotion();
            if (!f.isEmpty()) {
                if (f.is(FluidTags.LAVA)) return LAVA;
                if (f.is(FluidTags.WATER) && !motion) return WATER;
            }
            if (st.is(Blocks.STONE)) return STONE;
            if (st.is(Blocks.DEEPSLATE)) return DEEPSLATE;
            return motion ? OTHER : AIR;
        }
    }
}
