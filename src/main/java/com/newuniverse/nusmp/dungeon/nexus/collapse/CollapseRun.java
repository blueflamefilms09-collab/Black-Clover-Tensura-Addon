package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.EcosystemData;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One collapse that is under way, in the world: the {@link CollapseState} that is saved, the {@link CollapseRunner} that plays its clock,
 * the hands ({@link CollapseWorld}), the frenzied horde ({@link CollapseGuards}), the spatial surges ({@link CollapseSurge}) and everything
 * else that needs a ServerLevel. {@link CollapseManager} ticks it once a tick; the heavy part is bounded by the runner's budgets, the surges
 * are looked at a few times a second, the rest happens once a second.
 *
 * <p>The opening (exits, Root Gates, the broadcast, the air) is done on the first tick, saved as {@code started}, and never repeated. The
 * ending removes the horde, the Root Gates this collapse placed and any falling block still in the air, lets go of the chunks it held and
 * forgets the saved state. Everything the collapse owns is cleared there, whether it ends by its own clock or because an admin stopped it
 * or its Nexus was reset.
 */
final class CollapseRun implements CollapseRunner.Events {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int SECOND = 20, HOLD_EVERY = 100, FAILURES = 100, WIPE_PATIENCE = 6000, SURGE_EVERY = 4;
    /**
     * Whether the wipe asks for its chunks to be loaded (region tickets that expire by themselves, from the moment the wipe starts to its
     * end) so that the Nexus is gone even when everyone has left. false: the wipe waits for a player to bring the chunks back instead.
     * Not "force-loading per tick": a few dozen tickets, renewed every five seconds, for the few seconds the wipe takes.
     */
    private static final boolean HOLD_CHUNKS = true;
    /** The pocket's ring of candidate exits: how far out, how many candidates, how many to keep, how far apart they must be. */
    private static final int RING_RADIUS = 14, RING_COUNT = 8, RING_KEEP = 2, RING_GAP = 12;

    final ServerLevel level;
    final CollapseState s;
    private final CollapseSave save;
    final CollapseGuards guards;
    final CollapseSurge surge;
    /** The chunks the ground of the collapse is in (the air is held at zero in these), and those plus the exits' (held loaded for the wipe). */
    private final List<int[]> air = new ArrayList<>(), held = new ArrayList<>();
    private final Set<UUID> told = new HashSet<>();
    private final Map<UUID, Long> warnedAt = new HashMap<>();
    private CollapseSweep order;
    private CollapseRunner runner;
    private CollapseWorld world;
    private int failures, partFailures;
    private boolean done, sawCollapsed;
    private long now, wipeSince = -1L;

    CollapseRun(ServerLevel level, CollapseState s, CollapseSave save) {
        this.level = level;
        this.s = s;
        this.save = save;
        this.guards = new CollapseGuards(level, s);
        this.surge = new CollapseSurge(level, s);
    }

    boolean done() { return done; }

    BlockPos centre() { return new BlockPos(s.centreX, s.centreY, s.centreZ); }

    /** Is this block within the collapse or a short way round it (the 16 blocks round the footprint are where the dust and the rubble are)? */
    boolean covers(BlockPos p) {
        return p.getY() >= s.floorY - 8 && p.getY() <= s.topY + 16 && s.fp.contains(p.getX(), p.getZ(), 16);
    }

    // ---------------------------------------------------------------- one tick
    void tick(long gameTime) {
        if (done) return;
        now = gameTime;
        if (now < s.startTick) s.startTick = now;                                       // a clock that ran back (a copied world): the countdown starts from here
        try {
            if (!s.started) open();
            if (runner == null) build();
            world.begin(CollapsePlan.stageAt(s.elapsed(now)));
            DungeonGuard.internal(() -> runner.tick(now, world, this));
            failures = 0;
        } catch (RuntimeException | LinkageError failure) {
            if (failures++ % 50 == 0) LOG.warn("[nusmp] The Treasury Collapse at ({}, {}) failed this tick", s.centreX, s.centreZ, failure);
            if (failures >= FAILURES) abort("it failed " + FAILURES + " ticks in a row");
            return;
        }
        save.setDirty();
        try {
            guard(() -> guards.tick(now), "the horde");                                     // a failure of one part never stops the others, nor the wipe
            if (Math.floorMod(now + s.id, (long) SURGE_EVERY) == 0L && CollapsePlan.stageAt(s.elapsed(now)) != CollapsePlan.Stage.WIPE) guard(() -> surge.tick(now, insidePlayers(), ways(), order), "the surges");
            if (Math.floorMod(now + s.id, (long) SECOND) == 0L) second();
            if (!done && CollapsePlan.stageAt(s.elapsed(now)) == CollapsePlan.Stage.WIPE) {
                if (wipeSince < 0L) wipeSince = now;
                if (now % HOLD_EVERY == 0L) hold();
                if (runner.ended()) end("finished");
                else if (now - wipeSince > WIPE_PATIENCE) abort("its wipe did not finish in " + WIPE_PATIENCE / 20 + " seconds");
            }
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Treasury Collapse at ({}, {}) failed in its upkeep", s.centreX, s.centreZ, failure);
        }
    }

    /** Runs one part of the upkeep; if it throws the failure is logged (now and then, not every tick) and the rest goes on. */
    private void guard(Runnable part, String what) {
        try {
            part.run();
        } catch (RuntimeException | LinkageError failure) {
            if (partFailures++ % 100 == 0) LOG.warn("[nusmp] The Treasury Collapse at ({}, {}) failed in {}", s.centreX, s.centreZ, what, failure);
        }
    }

    /** Makes the order, the sweep and the hands from the saved state (after the exits are known), and works out which chunks are the collapse's. */
    private void build() {
        order = s.order();
        runner = new CollapseRunner(s, order, s.sweep(order));
        world = new CollapseWorld(level, s.nexus, order);
        air.clear();
        air.addAll(s.fp.chunks());
        held.clear();
        held.addAll(CollapseExits.held(s.fp, s.exits));
    }

    // ---------------------------------------------------------------- the opening
    /** The first tick: the exits, the Root Gates, the air, the broadcast. Saved as started first, so whatever goes wrong below is not run twice. */
    private void open() {
        s.started = true;
        List<int[]> exits = s.nexus ? nexusExits() : pocketExits();
        s.exits.clear();
        s.gates.clear();
        for (int[] e : exits) {
            s.exits.add(e);
            s.gates.add(new int[0]);
        }
        build();
        boolean gates = tryGates();
        pinAir();
        DungeonMaw.broadcast(level.getServer(), Component.literal(CollapseText.BROADCAST).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        level.playSound(null, centre(), SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 4.0f, 0.5f);
        for (ServerPlayer p : insidePlayers()) {
            CollapseUi.title(p, CollapseText.TITLE, CollapseText.TITLE_SUB, 60, ChatFormatting.DARK_RED, ChatFormatting.BOLD);
            CollapseUi.chat(p, CollapseText.GASP, ChatFormatting.GRAY);
            CollapseUi.chat(p, gates ? CollapseText.OPENED : CollapseText.NO_EXIT, ChatFormatting.YELLOW);
            told.add(p.getUUID());
            s.inside.add(p.getUUID());
        }
        LOG.info("[nusmp] A Treasury Collapse began at ({}, {}, {}): the {}{}, {} exit(s)", s.centreX, s.centreY, s.centreZ, s.zone.label, s.nexus ? "" : " (a pocket outside any Nexus)", s.exits.size());
    }

    private List<int[]> nexusExits() {
        NexusInfo info = CollapseHooks.containing(level, centre());
        return info != null ? CollapseHooks.exitsOf(info) : CollapseExits.fromPlan(null, null, null, s.fp, s.floorY);
    }

    /** Up to two standing places on a ring round the centre of a pocket, far enough apart; none if the ground round it has no floor to stand on. */
    private List<int[]> pocketExits() {
        List<int[]> found = new ArrayList<>();
        for (int[] c : CollapseExits.ring(s.centreX, s.centreZ, RING_RADIUS, RING_COUNT, (int) s.id)) {
            int[] spot = CollapseGround.standingNear(level, c[0], s.centreY, c[1], 4, 6);
            if (spot != null && CollapseExits.farEnough(found, spot[0], spot[2], RING_GAP)) found.add(spot);
            if (found.size() >= RING_KEEP) break;
        }
        return found;
    }

    /** Sets a Root Gate at every exit that has none yet (and whose chunk is loaded). True if the collapse has at least one gate standing after this. */
    private boolean tryGates() {
        for (int i = 0; i < s.exits.size(); i++) {
            if (s.gates.get(i).length != 0) continue;
            final int[] exit = s.exits.get(i);
            int[] g = DungeonGuard.internalGet(() -> CollapseGates.place(level, exit));
            if (g.length != 0) s.gates.set(i, g);
        }
        return !CollapseGates.standing(s.gates).isEmpty();
    }

    // ---------------------------------------------------------------- once a second
    private void second() {
        long elapsed = s.elapsed(now);
        CollapsePlan.Stage stage = CollapsePlan.stageAt(elapsed);
        if (s.nexus && !watchNexus()) return;
        pinAir();
        tryGates();
        List<ServerPlayer> in = insidePlayers();
        greet(in);
        if (stage == CollapsePlan.Stage.WIPE) return;
        long left = CollapsePlan.remaining(elapsed);
        List<int[]> ways = ways();
        int open = order.open(), limit = Math.min(open, CollapsePlan.warnLimit(elapsed, open));
        for (ServerPlayer p : in) {
            int i = order.indexOf(p.getBlockX(), p.getBlockZ());
            if (CollapseRules.frontAhead(stage, i, s.cursor, limit)) {
                warn(p);
                continue;
            }
            if (surge.tearing(p.getUUID())) continue;                                    // the tear's own warning holds the bar for its two seconds
            int near = CollapseExits.nearest(ways, p.getX(), p.getZ());
            if (near < 0) {
                CollapseUi.bar(p, CollapseText.bar(left, Double.NaN, ""), ChatFormatting.GOLD);
                continue;
            }
            int[] w = ways.get(near);
            double dx = w[0] + 0.5 - p.getX(), dz = w[2] + 0.5 - p.getZ();
            CollapseUi.bar(p, CollapseText.bar(left, Math.sqrt(dx * dx + dz * dz), CollapseText.direction(dx, dz)), left < 600L ? ChatFormatting.RED : ChatFormatting.GOLD);
        }
        warnedAt.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    /** The ways out that count: the Root Gates that stand, else the exits themselves. */
    private List<int[]> ways() {
        List<int[]> ways = CollapseGates.standing(s.gates);
        return ways.isEmpty() ? s.exits : ways;
    }

    /** The personal warning: the front is about to come down where this player stands. */
    private void warn(ServerPlayer p) {
        CollapseUi.bar(p, CollapseText.WARN, ChatFormatting.RED, ChatFormatting.BOLD);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState()), p.getX(), p.getY() + 2.6, p.getZ(), 14, 0.6, 0.3, 0.6, 0.02);
        Long last = warnedAt.get(p.getUUID());
        if (last == null || now - last >= 60L) {
            warnedAt.put(p.getUUID(), now);
            level.playSound(null, p.blockPosition(), SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.AMBIENT, 1.6f, 0.5f);
        }
    }

    /** A player who has just come into the collapse is told where things stand; one who left is forgotten. */
    private void greet(List<ServerPlayer> in) {
        Set<UUID> here = new HashSet<>();
        boolean gates = !CollapseGates.standing(s.gates).isEmpty();
        for (ServerPlayer p : in) {
            here.add(p.getUUID());
            if (told.add(p.getUUID()) && !s.inside.contains(p.getUUID())) CollapseUi.chat(p, gates ? CollapseText.OPENED : CollapseText.NO_EXIT, ChatFormatting.YELLOW);
        }
        told.retainAll(here);
        s.inside.clear();
        s.inside.addAll(here);
    }

    /**
     * Is the Nexus still marked collapsed in the Nexus save? Not yet (or the mark was lost): it is marked now. Marked once and not any more:
     * an admin reset or rebuilt it, and the collapse ends. False when it has ended.
     */
    private boolean watchNexus() {
        NexusState st = CollapseHooks.stateOf(level.getServer(), s.cellX, s.cellZ);
        switch (CollapseRules.watch(sawCollapsed, st)) {
            case KEEP -> {
                sawCollapsed = true;
                return true;
            }
            case MARK -> {
                CollapseHooks.markCollapsed(level, centre());
                return true;
            }
            default -> {
                abort("its Nexus was reset or rebuilt (it is now " + st + ")");
                return false;
            }
        }
    }

    // ---------------------------------------------------------------- air and chunks
    /** Biomass and oxygen of the chunks of the collapse are held at zero (the ecosystem lets the air creep back every few seconds). */
    private void pinAir() {
        for (int[] c : air) {
            LevelChunk ch = level.getChunkSource().getChunkNow(c[0], c[1]);
            if (ch == null) continue;
            EcosystemData e = ch.getData(LivingDungeon.ECOSYSTEM);
            if (e.biomass != 0f || e.oxygen != 0f) {
                e.biomass = 0f;
                e.oxygen = 0f;
                ch.setUnsaved(true);
            }
        }
    }

    /** For the wipe only: the chunks of the collapse are asked to stay (or come) loaded for a little while, so that the Nexus is gone even if everyone left. */
    private void hold() {
        if (!HOLD_CHUNKS) return;
        ServerChunkCache cache = level.getChunkSource();
        BlockPos at = centre();
        for (int[] c : held) cache.addRegionTicket(TicketType.PORTAL, new ChunkPos(c[0], c[1]), 1, at);
    }

    private void release() {
        ServerChunkCache cache = level.getChunkSource();
        BlockPos at = centre();
        for (int[] c : held) cache.removeRegionTicket(TicketType.PORTAL, new ChunkPos(c[0], c[1]), 1, at);
    }

    // ---------------------------------------------------------------- players
    List<ServerPlayer> insidePlayers() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            if (CollapseRules.inside(s.fp, s.floorY, s.topY, p.getBlockX(), p.getBlockY(), p.getBlockZ())) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- the runner's events
    @Override
    public void stage(CollapsePlan.Stage to) {
        switch (to) {
            case ROLLING -> level.playSound(null, centre(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 3.0f, 0.5f);
            case FINAL -> level.playSound(null, centre(), SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 3.0f, 0.4f);
            case WIPE -> level.playSound(null, centre(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.AMBIENT, 4.0f, 0.4f);
            default -> { }
        }
    }

    @Override
    public void line(int index, long ticksLeft) {
        String text = CollapseText.mark(index);
        for (ServerPlayer p : insidePlayers()) {
            if (index >= 5) CollapseUi.title(p, text, "", 18, ChatFormatting.RED, ChatFormatting.BOLD);
            else CollapseUi.chat(p, text, index >= 3 ? ChatFormatting.RED : ChatFormatting.YELLOW);
        }
    }

    @Override
    public void crack(int x, int z) {
        double y = s.floorY + (s.topY - s.floorY) * 0.75;
        level.playSound(null, x + 0.5, y, z + 0.5, SoundEvents.STONE_BREAK, SoundSource.AMBIENT, 1.4f, 0.5f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState()), x + 0.5, y, z + 0.5, 6, 0.4, 0.3, 0.4, 0.02);
    }

    @Override
    public void dust(int x, int z) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState()), x + 0.5, s.topY - 1.0, z + 0.5, 18, 0.5, 0.4, 0.5, 0.02);
    }

    @Override
    public void wave(int index) { guards.wave(index, now, insidePlayers()); }

    @Override
    public void expel(boolean first) {
        if (first) {
            guards.dismiss();
            hold();
        }
        CollapseExpel.run(level, insidePlayers());
    }

    // ---------------------------------------------------------------- the end
    /** Ends the collapse before its time (an admin, a reset Nexus, repeated failures): nothing is restored, everything the collapse owns is cleared. */
    void abort(String why) {
        LOG.info("[nusmp] The Treasury Collapse at ({}, {}) was ended early: {}", s.centreX, s.centreZ, why);
        end("ended early");
    }

    private void end(String how) {
        if (done) return;
        done = true;
        try {
            guards.dismiss();
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not remove the horde of a Treasury Collapse", failure);
        }
        surge.clear();
        try {
            CollapseGates.remove(level, s.gates);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not remove the Root Gates of a Treasury Collapse", failure);
        }
        try {
            clearDebris();
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not clear the falling blocks of a Treasury Collapse", failure);
        }
        try {
            release();
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not let go of the chunks of a Treasury Collapse", failure);
        }
        save.remove(s.id);
        LOG.info("[nusmp] The Treasury Collapse at ({}, {}) {}", s.centreX, s.centreZ, how);
    }

    /** Every falling block still in the air above the ground of the collapse is taken away (there are never more than a couple of hundred). */
    private void clearDebris() {
        AABB box = new AABB(s.fp.minX() - 2, s.floorY - 2, s.fp.minZ() - 2, s.fp.maxX() + 3, s.topY + 12, s.fp.maxZ() + 3);
        for (FallingBlockEntity f : level.getEntitiesOfClass(FallingBlockEntity.class, box)) f.discard();
    }

    // ---------------------------------------------------------------- a look
    String describe() {
        long at = now == 0L ? level.getGameTime() : now;
        long elapsed = s.elapsed(at);
        String line = CollapseText.status(CollapsePlan.stageAt(elapsed), CollapsePlan.remaining(elapsed), runner == null ? 0 : runner.percent());
        return "#" + s.id + " " + s.zone.label + (s.nexus ? "" : " (pocket)") + " at (" + s.centreX + ", " + s.centreY + ", " + s.centreZ + "): " + line + "; "
                + s.exits.size() + " exit(s), " + CollapseGates.standing(s.gates).size() + " Root Gate(s), " + guards.aliveCount() + " of the horde alive of " + s.spawned + " born" + (guards.pendingCount() > 0 ? ", " + guards.pendingCount() + " on the way" : "")
                + ", " + surge.thrown() + " player(s) thrown by surges" + (surge.fizzled() > 0 ? " (" + surge.fizzled() + " tear(s) found no place)" : "")
                + (runner == null ? "" : ", " + runner.skipped() + " column(s) passed over")
                + (world == null ? "" : ", " + world.started() + " falling block(s) started" + (world.refused() > 0 ? " (" + world.refused() + " refused by the Dungeon's guard)" : ""));
    }
}
