package com.newuniverse.nusmp.entity.veldanava.pocket;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.veldanava.skills.IsrafilRules;
import com.newuniverse.nusmp.entity.veldanava.skills.IsrafilState;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentDamage;
import com.newuniverse.nusmp.entity.veldanava.skills.TemporalDebt;
import com.newuniverse.nusmp.entity.veldanava.skills.TrialFx;
import com.newuniverse.nusmp.entity.veldanava.skills.VeldanavaGuard;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Why: Dimensional Labyrinth (Israfil) is a real pocket dimension. The target (and, with the skill mastered, up to two creatures next to it) is taken out of the world into {@code nusmp:trial}: a
 * sealed maze of black stone under a ceiling, dimly lit from the floor, where the walls NEVER stand still. Every few seconds a wall that is shut opens and a wall on the new loop shuts (see
 * {@link PocketMaze}: the maze stays one connected maze with a single way between two places, so it can never trap anyone for good), after a warning in the form of a flash between the two walls. A
 * glowing exit of crying obsidian waits in the maze and moves every few shifts; reaching it lets the creature out at once. When the time runs out, or the caster dies or leaves, everyone is put back
 * where they stood. The caster stays outside and sees how many are held.
 *
 * <p>Server side only; the caster, the members and the sessions are all keyed by UUID. Rules that keep it fair and safe: a boss is never taken in (it is caged in the old way), no one can be taken
 * in again for thirty seconds after they leave, no block can be broken, placed or blown up there, no teleport (ender pearl, chorus fruit) works from inside, a wall never closes on someone standing in
 * it, a player who logs out inside is put back on the next login, and the whole thing is undone on server stop.
 *
 * <p>Cost (rule 11): at most {@link IsrafilRules#POCKET_SLOTS} mazes on a server and one per caster; opening paints a maze of at most 45 x 45 x 6 blocks once (about 12,000 block reads, 6,500 writes
 * without neighbour updates) and forces at most nine chunks, closing does the same in reverse and unforces them; while it stands, one wall moves every 1.8 to 3 seconds (24 block writes, 2 effect
 * packets), members (at most three) are looked at five times a second and hurt once a second, and the caster gets one action-bar line a second. A server with no maze open pays one empty-map check
 * per tick.
 */
public final class PocketLabyrinth {
    private PocketLabyrinth() {}

    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "trial"));

    private static final PerfProbe.Section SECTION = PerfProbe.section("s.veldanava.pocket");
    private static final int FLOOR_Y = 64, HEIGHT = 4, SPACING = 128, ORIGIN_X = 16, ORIGIN_Z = 16;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String RETURN = "nusmp_trial_return", IMMUNE_UNTIL = "nusmp_trial_pocket_until";

    /** Where a creature stood before it was taken in. */
    private record Return(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {}

    private static final class Member {
        final Return back;
        final long endsAt;

        Member(Return back, long endsAt) { this.back = back; this.endsAt = endsAt; }
    }

    private static final class Session {
        final UUID caster;
        final int slot, originX, originZ;
        final boolean mastered;
        final PocketMaze maze;
        final Random rnd;
        final Map<UUID, Member> members = new LinkedHashMap<>();
        int exit, shifts;
        long nextShift, applyAt;
        PocketMaze.Change planned;
        boolean entered, painted, over;

        Session(UUID caster, int slot, boolean mastered, long seed) {
            this.caster = caster;
            this.slot = slot;
            this.mastered = mastered;
            this.originX = slot * SPACING + ORIGIN_X;
            this.originZ = ORIGIN_Z;
            this.maze = PocketMaze.generate(IsrafilRules.pocketCells(mastered), seed);
            this.rnd = new Random(seed ^ 0x5DEECE66DL);
            this.exit = maze.farthest(0);
        }
    }

    private static final Map<UUID, Session> BY_CASTER = new HashMap<>();
    private static final Map<UUID, Session> BY_MEMBER = new HashMap<>();
    private static final boolean[] USED = new boolean[IsrafilRules.POCKET_SLOTS];
    private static boolean moving;                 // our own teleports must not be cancelled

    public static void register(IEventBus bus) {
        bus.addListener(PocketLabyrinth::onServerTick);
        bus.addListener(PocketLabyrinth::onTeleport);
        bus.addListener(PocketLabyrinth::onDeath);
        bus.addListener(PocketLabyrinth::onLogout);
        bus.addListener(PocketLabyrinth::onLogin);
        bus.addListener(PocketLabyrinth::onServerStopping);
        bus.addListener(PocketLabyrinth::onBreak);
        bus.addListener(PocketLabyrinth::onPlace);
        bus.addListener(PocketLabyrinth::onExplosion);
    }

    public static boolean inPocket(Entity e) { return e.level().dimension() == DIMENSION; }

    /** True when the server has the trial dimension (a data pack can remove it; then the old cage is used). */
    public static boolean available(MinecraftServer server) { return server != null && server.getLevel(DIMENSION) != null; }

    public static boolean holds(UUID caster) { return BY_CASTER.containsKey(caster); }

    public static int sessions() { return BY_CASTER.size(); }

    // ================================================================ opening
    /** Can this creature be pulled in right now? A failure reason, or null. */
    public static String refusal(LivingEntity t) {
        if (BalanceLaw.isBoss(t)) return t.getDisplayName().getString() + " is too vast to be pulled in.";
        if (VeldanavaGuard.entrapmentImmune(t)) return t.getDisplayName().getString() + " slips out of the labyrinth.";
        if (BY_MEMBER.containsKey(t.getUUID()) || BY_CASTER.containsKey(t.getUUID()) || inPocket(t)) return t.getDisplayName().getString() + " is already inside a labyrinth.";
        if (t.getPersistentData().getLong(IMMUNE_UNTIL) > t.level().getGameTime()) return t.getDisplayName().getString() + " has only just left a labyrinth.";
        if (t instanceof ServerPlayer sp && (sp.hasDisconnected() || sp.isSpectator())) return t.getDisplayName().getString() + " cannot be reached.";
        return null;
    }

    /**
     * Opens the labyrinth round {@code target} (and, mastered, the others next to it). The creatures are taken in after a short flash of 20 ticks. Returns a failure text, or null when it opened.
     */
    public static String open(ServerPlayer caster, LivingEntity target, boolean mastered) {
        MinecraftServer server = caster.getServer();
        if (server == null || server.getLevel(DIMENSION) == null) return "The labyrinth will not open here (the nusmp:trial dimension is missing).";
        if (inPocket(caster) || BY_CASTER.containsKey(caster.getUUID()) || BY_MEMBER.containsKey(caster.getUUID())) return "Your labyrinth still stands.";
        String why = refusal(target);
        if (why != null) return why;
        int slot = freeSlot();
        if (slot < 0) return "Too many labyrinths stand on this server.";

        List<LivingEntity> picked = new ArrayList<>();
        picked.add(target);
        int most = IsrafilRules.pocketMembers(mastered);
        if (most > 1) {
            double r = IsrafilRules.POCKET_GATHER_RADIUS;
            List<LivingEntity> near = new ArrayList<>(target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r),
                    x -> x != target && x != caster && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(caster) && x.distanceToSqr(target) <= r * r && refusal(x) == null));
            near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(target)));
            for (LivingEntity x : near) if (picked.size() < most) picked.add(x);
        }

        Session s = new Session(caster.getUUID(), slot, mastered, caster.getRandom().nextLong());
        USED[slot] = true;
        BY_CASTER.put(caster.getUUID(), s);
        for (LivingEntity e : picked) BY_MEMBER.put(e.getUUID(), s);                                   // reserved now, so no one else can take them
        ServerLevel here = caster.serverLevel();
        for (LivingEntity e : picked) TrialFx.pocket(here, e.position(), 2.4f, 25);
        SpellRuntime.later(here, 20, () -> enter(server, s, caster, picked));
        return null;
    }

    private static int freeSlot() {
        for (int k = 0; k < USED.length; k++) if (!USED[k]) return k;
        return -1;
    }

    private static void enter(MinecraftServer server, Session s, ServerPlayer caster, List<LivingEntity> picked) {
        ServerLevel pocket = server.getLevel(DIMENSION);
        if (s.over || BY_CASTER.get(s.caster) != s) return;
        if (pocket == null || !caster.isAlive() || caster.hasDisconnected()) { abort(s); return; }
        setForced(pocket, s, true);
        paintAll(pocket, s, false);
        s.painted = true;
        long now = server.overworld().getGameTime();
        double[] c = s.maze.centre(0);
        int k = 0;
        moving = true;
        try {
            for (LivingEntity e : picked) {
                BY_MEMBER.remove(e.getUUID());
                if (!e.isAlive() || e.level().isClientSide || (e instanceof ServerPlayer sp0 && sp0.hasDisconnected())) continue;
                Vec3 at = new Vec3(s.originX + c[0] + (k == 1 ? 1.0 : k == 2 ? -1.0 : 0.0), FLOOR_Y, s.originZ + c[1]);
                Return back = new Return(e.level().dimension(), e.position(), e.getYRot(), e.getXRot());
                boolean player = e instanceof ServerPlayer;
                int ticks = IsrafilRules.pocketTicks(player, s.mastered);
                Entity moved;
                if (e instanceof ServerPlayer sp) {
                    remember(sp, back);
                    sp.teleportTo(pocket, at.x, at.y, at.z, 0f, 0f);
                    moved = sp;
                } else {
                    moved = e.changeDimension(new DimensionTransition(pocket, at, Vec3.ZERO, 0f, 0f, DimensionTransition.DO_NOTHING));
                }
                if (moved == null) continue;
                moved.fallDistance = 0.0f;
                s.members.put(moved.getUUID(), new Member(back, now + ticks));
                BY_MEMBER.put(moved.getUUID(), s);
                moved.getPersistentData().putLong(IMMUNE_UNTIL, now + ticks + IsrafilRules.POCKET_IMMUNE_TICKS);
                if (moved instanceof LivingEntity le) TemporalDebt.add(le, IsrafilRules.POCKET_DEBT);
                if (moved instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("You are inside " + caster.getName().getString()
                        + "'s Dimensional Labyrinth. The walls never stand still - find the crying-obsidian floor to get out.").withStyle(ChatFormatting.AQUA), false);
                k++;
            }
        } finally {
            moving = false;
        }
        if (s.members.isEmpty()) { end(server, s, Ending.EMPTY); return; }
        s.entered = true;
        s.nextShift = now + IsrafilRules.pocketShiftTicks(s.mastered);
        TrialFx.pocket(pocket, new Vec3(s.originX + c[0], FLOOR_Y, s.originZ + c[1]), 3.0f, 30);
    }

    /** The caster could not finish the cast: free what was reserved. */
    private static void abort(Session s) {
        BY_CASTER.remove(s.caster, s);
        BY_MEMBER.values().removeIf(x -> x == s);
        USED[s.slot] = false;
        s.over = true;
    }

    private static void remember(ServerPlayer p, Return back) {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", back.dim().location().toString());
        t.putDouble("X", back.pos().x); t.putDouble("Y", back.pos().y); t.putDouble("Z", back.pos().z);
        p.getPersistentData().put(RETURN, t);
    }

    // ================================================================ the walls
    private static final Block[] FLOOR = {Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES};

    private static BlockState wallState(int x, int z) {
        boolean pillar = x % PocketMaze.PITCH == 0 && z % PocketMaze.PITCH == 0;
        return (pillar ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
    }

    /** The block at a maze column on the floor layer: the exit's crying obsidian, the lanterns, or the checkered floor. */
    private static BlockState floorState(Session s, int x, int z) {
        PocketMaze maze = s.maze;
        int cell = maze.cellAt(x, z);
        int mx = x % PocketMaze.PITCH, mz = z % PocketMaze.PITCH;
        if (cell == s.exit && mx != 0 && mz != 0) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        if (cell >= 0 && mx == 2 && mz == 2 && maze.column(cell) % 2 == 0 && maze.row(cell) % 2 == 0) return Blocks.SEA_LANTERN.defaultBlockState();
        int parity = cell < 0 ? 0 : (maze.column(cell) + maze.row(cell)) & 1;
        return FLOOR[parity].defaultBlockState();
    }

    /** What belongs at height {@code y} (-1 floor, 0 .. HEIGHT - 1 walls, HEIGHT ceiling) of a column. */
    private static BlockState want(Session s, int x, int y, int z, boolean erase) {
        if (erase) return Blocks.AIR.defaultBlockState();
        if (y < 0) return floorState(s, x, z);
        if (y >= HEIGHT) return Blocks.BLACKSTONE.defaultBlockState();
        return s.maze.solid(x, z) ? wallState(x, z) : Blocks.AIR.defaultBlockState();
    }

    private static void put(ServerLevel w, BlockPos.MutableBlockPos m, int x, int y, int z, BlockState state) {
        m.set(x, y, z);
        if (w.getBlockState(m) != state) w.setBlock(m, state, FLAGS);
    }

    /** Paints (or, when {@code erase}, empties) the whole maze: floor, walls and ceiling. */
    private static void paintAll(ServerLevel w, Session s, boolean erase) {
        int size = s.maze.size();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++)
                for (int y = -1; y <= HEIGHT; y++) put(w, m, s.originX + x, FLOOR_Y + y, s.originZ + z, want(s, x, y, z, erase));
    }

    /** Re-paints the three columns of one wall. */
    private static void paintGap(ServerLevel w, Session s, int edge) {
        int[] g = s.maze.gap(edge);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = g[0]; x <= g[2]; x++)
            for (int z = g[1]; z <= g[3]; z++)
                for (int y = 0; y < HEIGHT; y++) put(w, m, s.originX + x, FLOOR_Y + y, s.originZ + z, want(s, x, y, z, false));
    }

    /** Re-paints the floor of one cell (the exit marker moves). */
    private static void paintFloor(ServerLevel w, Session s, int cell) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x0 = s.maze.column(cell) * PocketMaze.PITCH, z0 = s.maze.row(cell) * PocketMaze.PITCH;
        for (int x = x0 + 1; x < x0 + PocketMaze.PITCH; x++)
            for (int z = z0 + 1; z < z0 + PocketMaze.PITCH; z++) put(w, m, s.originX + x, FLOOR_Y - 1, s.originZ + z, floorState(s, x, z));
    }

    private static void setForced(ServerLevel w, Session s, boolean on) {
        int size = s.maze.size();
        for (int cx = s.originX >> 4; cx <= (s.originX + size) >> 4; cx++)
            for (int cz = s.originZ >> 4; cz <= (s.originZ + size) >> 4; cz++) w.setChunkForced(cx, cz, on);
    }

    private static Vec3 gapCentre(Session s, int edge) {
        int[] g = s.maze.gap(edge);
        return new Vec3(s.originX + (g[0] + g[2] + 1) / 2.0, FLOOR_Y + 1.0, s.originZ + (g[1] + g[3] + 1) / 2.0);
    }

    /** True when somebody stands in (or within a body's width of) the three columns of a wall. */
    private static boolean occupied(Session s, int edge, List<Vec3> who) {
        int[] g = s.maze.gap(edge);
        double x0 = s.originX + g[0] - 0.9, x1 = s.originX + g[2] + 1 + 0.9, z0 = s.originZ + g[1] - 0.9, z1 = s.originZ + g[3] + 1 + 0.9;
        for (Vec3 p : who) if (p.x >= x0 && p.x <= x1 && p.z >= z0 && p.z <= z1) return true;
        return false;
    }

    private static List<Vec3> positions(ServerLevel pocket, Session s) {
        List<Vec3> out = new ArrayList<>(s.members.size());
        for (UUID id : s.members.keySet()) {
            Entity e = pocket.getEntity(id);
            if (e != null) out.add(e.position());
        }
        return out;
    }

    // ================================================================ living in the maze
    public static void onServerTick(ServerTickEvent.Post e) {
        if (BY_CASTER.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            tick(e.getServer());
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Session s : new ArrayList<>(BY_CASTER.values())) {
            if (s.over || !s.entered) continue;
            ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
            ServerLevel pocket = server.getLevel(DIMENSION);
            if (caster == null || !caster.isAlive() || pocket == null) { end(server, s, Ending.CASTER_GONE); continue; }
            if (s.planned != null) {
                if (now >= s.applyAt) applyShift(pocket, s, now);
            } else if (now >= s.nextShift) {
                planShift(pocket, s, now);
            }
            if (now % 5 == 0) watch(server, pocket, s, caster, now);
        }
    }

    private static void planShift(ServerLevel pocket, Session s, long now) {
        List<Vec3> who = positions(pocket, s);
        PocketMaze.Change c = s.maze.plan(s.rnd, edge -> !occupied(s, edge, who));
        s.nextShift = now + IsrafilRules.pocketShiftTicks(s.mastered);
        if (c == null) return;
        s.planned = c;
        s.applyAt = now + IsrafilRules.POCKET_WARN_TICKS;
        TrialFx.shift(pocket, gapCentre(s, c.closed()), gapCentre(s, c.opened()));                 // the flash that says "this wall goes, that wall opens"
    }

    private static void applyShift(ServerLevel pocket, Session s, long now) {
        PocketMaze.Change c = s.planned;
        s.planned = null;
        if (occupied(s, c.closed(), positions(pocket, s))) return;                                   // someone stepped in: this shift is skipped, the maze is unchanged
        s.maze.apply(c);
        paintGap(pocket, s, c.opened());
        paintGap(pocket, s, c.closed());
        if (++s.shifts % IsrafilRules.POCKET_EXIT_EVERY == 0) moveExit(pocket, s);
    }

    private static void moveExit(ServerLevel pocket, Session s) {
        int old = s.exit, cells = s.maze.cellCount();
        int pick = s.rnd.nextInt(cells);
        if (pick == old) pick = (pick + 1) % cells;
        s.exit = pick;
        paintFloor(pocket, s, old);
        paintFloor(pocket, s, pick);
        double[] c = s.maze.centre(pick);
        TrialFx.pocket(pocket, new Vec3(s.originX + c[0], FLOOR_Y, s.originZ + c[1]), 1.6f, 20);
    }

    /** Five times a second: time up, the exit, the edge of the maze; once a second: the toll and the caster's line. */
    private static void watch(MinecraftServer server, ServerLevel pocket, Session s, ServerPlayer caster, long now) {
        double lo = -2.0, hi = s.maze.size() + 2.0;
        for (UUID id : new ArrayList<>(s.members.keySet())) {
            Member m = s.members.get(id);
            if (m == null) continue;
            Entity e = pocket.getEntity(id);
            if (!(e instanceof LivingEntity le) || !e.isAlive() || e.level() != pocket) { forget(s, id); continue; }   // dead, offline (the login puts a player back) or gone by its own means
            if (now >= m.endsAt) { release(server, pocket, s, id, Ending.FADED, caster); continue; }
            double x = e.getX() - s.originX, z = e.getZ() - s.originZ;
            int cell = s.maze.cellAt(x, z);
            if (cell == s.exit && e.getY() < FLOOR_Y + 2.0) { release(server, pocket, s, id, Ending.ESCAPED, caster); continue; }
            if (x < lo || z < lo || x > hi || z > hi || e.getY() < FLOOR_Y - 3.0 || e.getY() > FLOOR_Y + HEIGHT + 1.0) {
                double[] c = s.maze.centre(0);
                IsrafilState.place(e, pocket, new Vec3(s.originX + c[0], FLOOR_Y, s.originZ + c[1]));      // back to the start of the maze
            } else if (now % 20 == 0) {
                JudgmentDamage.share(caster, le, IsrafilRules.POCKET_PULSE_SHARE);
            }
        }
        if (s.over) return;
        if (s.members.isEmpty()) { end(server, s, Ending.EMPTY); return; }
        if (now % 20 == 0) {
            long left = 0;
            for (Member m : s.members.values()) left = Math.max(left, m.endsAt - now);
            caster.displayClientMessage(Component.literal("Dimensional Labyrinth: " + s.members.size() + " held, " + (left + 19) / 20 + " s").withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** Drops a member without moving it (it died, left by itself, or logged out). */
    private static void forget(Session s, UUID id) {
        s.members.remove(id);
        BY_MEMBER.remove(id, s);
    }

    // ================================================================ leaving
    enum Ending { FADED, ESCAPED, CASTER_GONE, EMPTY, STOPPED, BROKEN }

    /** Puts one member back where it stood. */
    private static void release(MinecraftServer server, ServerLevel pocket, Session s, UUID id, Ending why, ServerPlayer caster) {
        Member m = s.members.remove(id);
        BY_MEMBER.remove(id, s);
        if (m == null) return;
        Entity e = pocket == null ? null : pocket.getEntity(id);
        if (e == null || !e.isAlive()) return;
        ServerLevel to = server.getLevel(m.back.dim());
        if (to == null) to = server.overworld();
        Vec3 p = m.back.pos();
        moving = true;
        try {
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(to, p.x, p.y, p.z, m.back.yRot(), m.back.xRot());
                sp.getPersistentData().remove(RETURN);
                sp.fallDistance = 0.0f;
            } else {
                e.changeDimension(new DimensionTransition(to, p, Vec3.ZERO, m.back.yRot(), m.back.xRot(), DimensionTransition.DO_NOTHING));
            }
        } finally {
            moving = false;
        }
        TrialFx.pocket(to, p, 1.8f, 20);
        if (why == Ending.ESCAPED && caster != null) caster.displayClientMessage(Component.literal(e.getDisplayName().getString() + " found the way out of your labyrinth.").withStyle(ChatFormatting.GRAY), true);
    }

    /** Ends a labyrinth: everyone is put back, the maze is taken down and the room is free again. */
    private static void end(MinecraftServer server, Session s, Ending why) {
        if (s.over) return;
        s.over = true;
        ServerLevel pocket = server.getLevel(DIMENSION);
        ServerPlayer caster = server.getPlayerList().getPlayer(s.caster);
        for (UUID id : new ArrayList<>(s.members.keySet())) release(server, pocket, s, id, why, caster);
        BY_CASTER.remove(s.caster, s);
        BY_MEMBER.values().removeIf(x -> x == s);
        if (pocket != null && s.painted) {
            paintAll(pocket, s, true);
            setForced(pocket, s, false);
        }
        USED[s.slot] = false;
    }

    /** Ends the labyrinth of one caster (the skill was forgotten). */
    public static void endOf(MinecraftServer server, UUID caster) {
        Session s = BY_CASTER.get(caster);
        if (s != null && server != null) end(server, s, Ending.BROKEN);
    }

    /** The Grand Trumpet: every labyrinth whose caster (not {@code keep}) stands within {@code radius} of a point ends. Returns how many. */
    public static int endNear(ServerLevel level, Vec3 centre, double radius, UUID keep) {
        MinecraftServer server = level.getServer();
        int n = 0;
        for (Session s : new ArrayList<>(BY_CASTER.values())) {
            if (s.caster.equals(keep)) continue;
            ServerPlayer c = server.getPlayerList().getPlayer(s.caster);
            if (c == null || c.level() != level || c.position().distanceToSqr(centre) > radius * radius) continue;
            end(server, s, Ending.BROKEN);
            n++;
        }
        return n;
    }

    // ================================================================ events
    public static void onTeleport(EntityTeleportEvent e) {
        if (moving || BY_MEMBER.isEmpty() || !BY_MEMBER.containsKey(e.getEntity().getUUID()) || !inPocket(e.getEntity())) return;
        e.setCanceled(true);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (BY_MEMBER.isEmpty() && BY_CASTER.isEmpty()) return;
        UUID id = e.getEntity().getUUID();
        MinecraftServer server = e.getEntity().getServer();
        Session own = BY_CASTER.get(id);
        if (own != null && server != null) { end(server, own, Ending.CASTER_GONE); return; }
        Session s = BY_MEMBER.get(id);
        if (s != null) {
            forget(s, id);
            e.getEntity().getPersistentData().remove(RETURN);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        MinecraftServer server = e.getEntity().getServer();
        Session own = BY_CASTER.get(id);
        if (own != null && server != null) { end(server, own, Ending.CASTER_GONE); return; }
        Session s = BY_MEMBER.get(id);
        if (s != null) forget(s, id);                                                                  // the login sends them back (their return place stays in their data)
    }

    /** Back in the world with the maze gone: put them where they stood (or at the world spawn). */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !inPocket(p) || BY_MEMBER.containsKey(p.getUUID())) return;
        sendBack(p);
    }

    private static void sendBack(ServerPlayer p) {
        CompoundTag t = p.getPersistentData().getCompound(RETURN);
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel level = null;
        if (t.contains("Dim")) {
            ResourceLocation rl = ResourceLocation.tryParse(t.getString("Dim"));
            if (rl != null) level = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        }
        moving = true;
        try {
            if (level != null && level.dimension() != DIMENSION) p.teleportTo(level, t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), p.getYRot(), p.getXRot());
            else {
                BlockPos spawn = server.overworld().getSharedSpawnPos();
                p.teleportTo(server.overworld(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
        } finally {
            moving = false;
        }
        p.getPersistentData().remove(RETURN);
        p.fallDistance = 0.0f;
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        for (Session s : new ArrayList<>(BY_CASTER.values())) end(e.getServer(), s, Ending.STOPPED);
        BY_CASTER.clear();
        BY_MEMBER.clear();
        java.util.Arrays.fill(USED, false);
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level l && l.dimension() == DIMENSION) e.setCanceled(true);
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof Level l && l.dimension() == DIMENSION) e.setCanceled(true);
    }

    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel().dimension() == DIMENSION) e.getAffectedBlocks().clear();
    }

    /**
     * The CI server smoke test (israfil.pocket): paints a mastered maze in a free room of the real dimension, compares every block with the walls, shifts it thirty times (moving the exit as the game
     * does) comparing again each time, takes it down and checks that nothing is left and the chunks are no longer forced. Returns a failure text, or null when all held.
     */
    public static String check(MinecraftServer server) {
        ServerLevel pocket = server.getLevel(DIMENSION);
        if (pocket == null) return "the nusmp:trial dimension is not loaded";
        int slot = freeSlot();
        if (slot < 0) return "no free room for the test";
        USED[slot] = true;
        Session s = new Session(UUID.randomUUID(), slot, true, 20240601L);
        try {
            setForced(pocket, s, true);
            paintAll(pocket, s, false);
            s.painted = true;
            String bad = compare(pocket, s, false);
            if (bad != null) return "after painting: " + bad;
            Random rnd = new Random(77);
            for (int i = 0; i < 30; i++) {
                PocketMaze.Change c = s.maze.plan(rnd, e -> true);
                if (c == null) return "shift " + i + " found no wall to move";
                s.maze.apply(c);
                paintGap(pocket, s, c.opened());
                paintGap(pocket, s, c.closed());
                if (!s.maze.perfect()) return "the maze broke at shift " + i;
                if (i % 7 == 6) moveExit(pocket, s);
            }
            bad = compare(pocket, s, false);
            if (bad != null) return "after 30 shifts: " + bad;
            return null;
        } finally {
            paintAll(pocket, s, true);
            setForced(pocket, s, false);
            USED[slot] = false;
        }
    }

    /** The first block of the maze that is not what it should be (or an air block that is not air when erased), or null. */
    private static String compare(ServerLevel w, Session s, boolean erase) {
        int size = s.maze.size();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++)
                for (int y = -1; y <= HEIGHT; y++) {
                    m.set(s.originX + x, FLOOR_Y + y, s.originZ + z);
                    if (w.getBlockState(m) != want(s, x, y, z, erase)) return "block " + x + "," + y + "," + z + " is " + w.getBlockState(m).getBlock().getDescriptionId();
                }
        return null;
    }

    /** Tests only: the maze a caster's labyrinth is made of. */
    static PocketMaze mazeOf(UUID caster) {
        Session s = BY_CASTER.get(caster);
        return s == null ? null : s.maze;
    }
}
