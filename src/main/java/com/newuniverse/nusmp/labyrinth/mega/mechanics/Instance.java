package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Why: one mechanic that is ACTIVE somewhere: the runtime of a definition at an anchor in an area, with everything the Director must be able to clean up. It owns the players
 * currently inside, the props and monsters it placed, its counters and timed flags (the reflect, shield, shared-damage ... rules the event handlers consult), the delayed and
 * repeating effect calls, the sustained runs, and the journals of the blocks it changed. {@link #update} is called every tick with the players of the area: it asks
 * {@link Triggers} whether the mechanic fires, runs due effects through the primitive registry ({@link Prims}), ticks the sustained runs and lets {@link Counters} decide
 * whether it was beaten. {@link #end} restores every block, removes every monster and prop and clears every flag, whatever the reason (beaten, floor left, server stopping,
 * admin clear): that is the one cleanup path, so there is nothing to forget. Nothing here is saved except the journals (a mirror in {@link MechanicsData}); an instance is
 * rebuilt from the plan when a player comes back.
 */
public final class Instance {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();

    public enum State { ARMED, SOLVED, ENDED }

    public final String key;
    public final MechanicDef def;
    public final ServerLevel level;
    public final Sector sector;
    public final Room room;
    public final Area area;
    public final BlockPos anchor;
    public final int floor;
    public final long seed;
    public final double difficulty;
    public final SplittableRandom rng;
    public State state = State.ARMED;
    public boolean admin;
    public long born;

    /** The players of the area at the last update. */
    public final List<ServerPlayer> inside = new ArrayList<>();
    final Set<UUID> lastInside = new HashSet<>();
    final Map<UUID, BlockPos> entry = new HashMap<>();
    public final List<Prop> props = new ArrayList<>();
    public final List<UUID> mobs = new ArrayList<>();
    public UUID leader;
    public final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, double[]> flags = new HashMap<>();
    final List<Run> running = new ArrayList<>();
    private final List<Pending> pending = new ArrayList<>();
    private final List<Journal> kept = new ArrayList<>();
    final Map<String, Journal> named = new HashMap<>();
    final Journal propJournal;
    /** Players in the downed state (uuid -> {until tick, revive progress}); see {@link Rules}. */
    final Map<UUID, long[]> downed = new HashMap<>();
    private final Set<UUID> told = new HashSet<>();
    final Triggers.Mem trig = new Triggers.Mem();
    final Counters.Mem ctr = new Counters.Mem();
    boolean fired;
    long lastDamageAt = Long.MIN_VALUE / 2, lastNoiseAt = Long.MIN_VALUE / 2;
    int kills;
    boolean pendingFire;
    private final Set<String> broken = new HashSet<>();
    private List<Mob> mobCache = List.of();
    private long mobCacheAt = Long.MIN_VALUE;
    private final String mobTag;
    long lastEmpty;

    private static final class Pending {
        long at;
        final int idx;
        int left;
        final int gap;

        Pending(long at, int idx, int left, int gap) {
            this.at = at;
            this.idx = idx;
            this.left = left;
            this.gap = gap;
        }
    }

    public Instance(String key, MechanicDef def, ServerLevel level, Sector sector, Room room, Area area, BlockPos anchor, int floor, long seed, double difficulty) {
        this.key = key;
        this.def = def;
        this.level = level;
        this.sector = sector;
        this.room = room;
        this.area = area;
        this.anchor = anchor;
        this.floor = floor;
        this.seed = seed;
        this.difficulty = difficulty;
        this.rng = Seeds.rng(seed);
        this.born = clock();
        this.mobTag = "nusmp_mech_" + Integer.toHexString(key.hashCode());
        this.propJournal = new Journal(level);
        this.lastEmpty = born;
    }

    private long now = Long.MIN_VALUE;
    /** How many times something in this instance threw (the self-tests demand 0). */
    public int errors;

    /** The instance's clock: the tick of its last update (so a self-test can drive time by hand), else the level's game time. */
    public long clock() { return now == Long.MIN_VALUE ? level.getGameTime() : now; }

    /** The primitives that threw and were switched off for this instance. */
    public Set<String> brokenOps() { return broken; }

    /** The tag on every monster this instance spawned. */
    public String mobTag() { return mobTag; }

    public boolean active() { return state != State.ENDED; }

    // ---------------------------------------------------------------- flags (timed rules the event handlers read)
    public void setFlag(String name, double value, long untilTick) { flags.put(name, new double[] {value, untilTick}); }

    /** The value of a flag, 0 when absent or expired. */
    public double flag(String name) {
        double[] f = flags.get(name);
        if (f == null) return 0.0;
        if (f[1] < clock()) { flags.remove(name); return 0.0; }
        return f[0];
    }

    public boolean hasFlag(String name) { return flag(name) != 0.0; }

    public void clearFlag(String name) { flags.remove(name); }

    // ---------------------------------------------------------------- set-up
    /** Called once when the first player enters: places the props and wardens the counter and trigger need, and tells the players. */
    public void activate() {
        try {
            Props.setup(this);
        } catch (RuntimeException e) {
            warn("setup", e);
        }
    }

    // ---------------------------------------------------------------- the tick
    /** Every tick with the players of the area (already filtered: in the area, in this level). */
    public void update(List<ServerPlayer> players, long now) {
        if (state == State.ENDED) return;
        this.now = now;
        inside.clear();
        inside.addAll(players);
        for (ServerPlayer p : players) {
            entry.putIfAbsent(p.getUUID(), p.blockPosition());
            if (told.add(p.getUUID())) Messages.entered(p, this);
        }
        try {
            Rules.tickDowned(this, now);
            Triggers.observe(this, now);
            if (state == State.ARMED && (pendingFire || Triggers.poll(this, now))) {
                pendingFire = false;
                fire(now);
            }
            runPending(now);
            tickRunning(now);
            if (state == State.ARMED) Counters.poll(this, now);
        } catch (RuntimeException e) {
            warn("update", e);
        }
        lastInside.clear();
        for (ServerPlayer p : players) lastInside.add(p.getUUID());
        if (!players.isEmpty()) lastEmpty = now;
    }

    /** Queues every effect of the definition (honouring delay and repeats). */
    public void fire(long now) {
        fired = true;
        for (int i = 0; i < def.effects().size(); i++) {
            MechanicDef.Effect e = def.effects().get(i);
            pending.add(new Pending(now + e.delay(), i, e.times() - 1, e.every()));
        }
    }

    private void runPending(long now) {
        if (pending.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        for (Pending p : pending) if (p.at <= now) due.add(p);
        for (Pending p : due) {
            pending.remove(p);
            execute(p.idx, now);
            if (p.left > 0) {
                p.left--;
                p.at = now + p.gap;
                pending.add(p);
            }
        }
    }

    /** Runs one effect line now (after its condition and chance). */
    void execute(int idx, long now) {
        MechanicDef.Effect e = def.effects().get(idx);
        if (broken.contains(e.op())) return;
        if (!e.cond().isEmpty() && !Cond.test(e.cond(), counters)) return;
        if (e.chance() < 1.0 && rng.nextDouble() >= e.chance()) return;
        Prim prim = Prims.get(e.op());
        OpCatalog cat = MechanicLibrary.catalog();
        if (prim == null || cat == null) return;
        Run r = new Run(this, e, cat.op(e.op()), now);
        try {
            prim.fire(r);
            r.duration = Math.max(0, prim.duration(r));
        } catch (RuntimeException | LinkageError t) {
            warn("effect " + e.op(), t);
            broken.add(e.op());
            r.restoreBlocks();
            return;
        }
        if (r.duration > 0) running.add(r);
        else if (r.hasJournal()) kept.add(r.takeJournal());
    }

    private void tickRunning(long now) {
        if (running.isEmpty()) return;
        for (Run r : new ArrayList<>(running)) {
            if (now - r.startedAt >= r.duration) { endRun(r); continue; }
            Prim p = Prims.get(r.eff.op());
            if (p == null) continue;
            try { p.tick(r); } catch (RuntimeException t) { warn("tick " + r.eff.op(), t); broken.add(r.eff.op()); endRun(r); }
        }
    }

    private void endRun(Run r) {
        running.remove(r);
        Prim p = Prims.get(r.eff.op());
        try { if (p != null) p.end(r); } catch (RuntimeException t) { warn("end " + r.eff.op(), t); }
        r.restoreBlocks();
    }

    // ---------------------------------------------------------------- events routed here by the Director
    public void onKill(LivingEntity victim) {
        UUID id = victim.getUUID();
        boolean mine = mobs.remove(id);
        if (mine || (area.contains(victim) && MegaDungeon.isDungeonMonster(victim))) kills++;
        Counters.onMobDeath(this, victim, mine);
        Triggers.onKill(this);
    }

    public void onDamage(ServerPlayer p, float amount) {
        lastDamageAt = clock();
        Triggers.onDamage(this, p, amount);
    }

    public void onDeath(ServerPlayer p) { Triggers.onDeath(this, p); }

    public void onItemUse(ServerPlayer p, net.minecraft.world.item.ItemStack stack) { Triggers.onItemUse(this, p, stack); }

    /** A right click on a block; true when it was one of this instance's props (the caller cancels the vanilla action). */
    public boolean onUse(ServerPlayer p, BlockPos pos) {
        Prop prop = null;
        for (Prop x : props) if (x.pos().equals(pos)) { prop = x; break; }
        if (prop == null) return false;
        switch (prop.kind()) {
            case "chest" -> { return false; }                                   // an ordinary reward chest: vanilla opens it
            case "cursed_chest" -> { Props.curse(this, p); return false; }      // it opens, and the curse comes with the loot
            case "mimic" -> { Props.mimic(this, p, prop); return true; }
            default -> { }
        }
        Triggers.onUse(this, p, prop);
        Counters.onUse(this, p, prop);
        return true;
    }

    // ---------------------------------------------------------------- results
    /** The mechanic was beaten: pay the reward, lift the effects (cages, doors, running runs), stop firing. */
    public void solve(ServerPlayer by) {
        if (state != State.ARMED) return;
        state = State.SOLVED;
        pending.clear();
        try {
            Rewards.give(this, by);
        } catch (RuntimeException e) {
            warn("reward", e);
        }
        stopEffects();
        Messages.solved(this);
        if (!admin) MechanicsData.get(level.getServer()).markSolved(key);
    }

    /** Ends every running effect and restores the blocks of effects (not of the props). */
    void stopEffects() {
        for (Run r : new ArrayList<>(running)) endRun(r);
        for (Journal j : kept) j.restore();
        kept.clear();
        for (Journal j : named.values()) j.restore();
        named.clear();
        flags.clear();
    }

    /** The one cleanup: effects, props, monsters, flags. Safe to call twice. */
    public void end() {
        if (state == State.ENDED) return;
        state = State.ENDED;
        pending.clear();
        stopEffects();
        propJournal.restore();
        for (UUID id : new ArrayList<>(mobs)) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) e.discard();
        }
        mobs.clear();
        props.clear();
        inside.clear();
        MechanicsData.get(level.getServer()).forget(this);
    }

    /** Every journal this instance holds, for the SavedData mirror. */
    List<Journal> journals() {
        List<Journal> out = new ArrayList<>(kept);
        out.addAll(named.values());
        out.add(propJournal);
        for (Run r : running) if (r.journalOrNull() != null) out.add(r.journalOrNull());
        return out;
    }

    // ---------------------------------------------------------------- monsters
    /** The live monsters this instance may act on: its own and the dungeon's in the area (near the players for a floor-wide mechanic). */
    public List<Mob> mobsInArea() {
        long now = clock();
        if (mobCacheAt == now) return mobCache;
        List<Mob> out = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        java.util.function.Predicate<Mob> mine = m -> m.isAlive() && (m.getTags().contains(mobTag) || MegaDungeon.isDungeonMonster(m) || m instanceof com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity);
        if (area.kind == Area.Kind.FLOOR) {
            for (ServerPlayer p : inside) {
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), mine)) if (seen.add(m.getUUID())) out.add(m);
            }
        } else {
            for (Mob m : level.getEntitiesOfClass(Mob.class, area.aabb(), mine)) if (area.contains(m) && seen.add(m.getUUID())) out.add(m);
        }
        mobCache = out;
        mobCacheAt = now;
        return out;
    }

    /** The monsters this instance itself spawned that are still alive. */
    public List<Mob> ownMobs() {
        List<Mob> out = new ArrayList<>();
        for (UUID id : mobs) if (level.getEntity(id) instanceof Mob m && m.isAlive()) out.add(m);
        return out;
    }

    private void warn(String what, Throwable e) {
        errors++;
        if (WARNED.add(def.id() + ":" + what)) LOG.warn("[nusmp] mechanic {} failed in {}: {}", def.id(), what, e.toString());
    }
}
