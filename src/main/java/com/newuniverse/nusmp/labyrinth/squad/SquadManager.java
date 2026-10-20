package com.newuniverse.nusmp.labyrinth.squad;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Kind;
import com.newuniverse.nusmp.labyrinth.squad.RoleMotion.Move;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: the squads of the world live here, and the whole cost of all of them is this one tick handler. Every squad thinks once every {@code thinkEveryTicks} (10) and
 * each squad's pass is offset by a hash of its id, so a world with forty squads does four of them a tick instead of forty every tenth tick. After the pass, the members
 * that could not be given goals (the goal fields are unreadable) are steered here from the very same pure decisions, so a squad degrades to "coordinated without goals"
 * and never to "uncoordinated". A squad with nobody left, or one that has been empty for half a minute, is forgotten (the cleaning on death and unload the brief asks
 * for); beyond {@code maxSquads} the oldest idle ones go first. Everything is in memory: a restart drops every squad, and the mobs that come back from saved chunks are
 * adopted again from their persistent role tags by {@link #adopt}. A fault in one squad is logged once and only that squad is dropped; it never reaches the server tick.
 */
public final class SquadManager {
    private SquadManager() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Squad> SQUADS = new LinkedHashMap<>();
    private static final long FORGET_AFTER = 600L;
    private static boolean warned;
    private static long thinks;

    // ---------------------------------------------------------------- the registry
    static Squad get(UUID id) { return id == null ? null : SQUADS.get(id); }

    /** The squad a mob belongs to, read from its persistent tag; null when it has none or the squad is not in memory. */
    public static Squad of(Mob mob) {
        UUID id = idOf(mob);
        return id == null ? null : SQUADS.get(id);
    }

    static UUID idOf(Mob mob) {
        CompoundTag d = mob.getPersistentData();
        if (!d.contains(Squad.ID)) return null;
        try {
            return UUID.fromString(d.getString(Squad.ID));
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    public static int count() { return SQUADS.size(); }

    public static List<Squad> all() { return new ArrayList<>(SQUADS.values()); }

    /** A new squad (a shell: the members attach as they join the world). */
    static Squad create(UUID id, long seed, int size, SquadPlan.Plan plan, boolean armed, long now) {
        Squad s = new Squad(id, seed, size, plan, armed, now);
        SQUADS.put(id, s);
        trim();
        return s;
    }

    static void remove(UUID id) { SQUADS.remove(id); }

    /** Forgets every squad (server stop, a self-test's clean-up). */
    public static void reset() {
        SQUADS.clear();
        warned = false;
        thinks = 0L;
    }

    /** Caps the number of squads: the ones that have been empty longest go first. */
    private static void trim() {
        int max = SquadConfig.get(SquadConfig.MAX_SQUADS);
        if (SQUADS.size() <= max) return;
        Iterator<Squad> it = SQUADS.values().iterator();
        while (SQUADS.size() > max && it.hasNext()) {
            Squad s = it.next();
            if (s.memberCount() == 0 || SQUADS.size() > max + 8) it.remove();
        }
    }

    // ---------------------------------------------------------------- joining and adopting
    /**
     * A mob with a squad tag has joined the world (a fresh spawn of the Director, or a mob back from a saved chunk): it is attached to its squad, which is made from its
     * tags when the server has forgotten it, tagged, and given the goals. Returns the squad, or null when the mob has no valid tag.
     */
    public static Squad adopt(Mob mob) {
        UUID id = idOf(mob);
        if (id == null) return null;
        Squad s = SQUADS.get(id);
        CompoundTag d = mob.getPersistentData();
        if (s == null) {
            long now = mob.level().getGameTime();
            s = create(id, d.getLong(Squad.SEED), Math.max(SquadPlan.MIN_SIZE, d.getInt(Squad.SIZE)), null, false, now);
        }
        Squad.Member m = s.attach(mob, SquadRole.fromId(d.getString(Squad.ROLE)), d.getInt(Squad.SLOT));
        m.goals = GoalInstaller.install(mob, id);
        if (!s.armed()) s.layout();
        return s;
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        if (SQUADS.isEmpty()) return;
        try {
            tick(MegaDungeon.level(e.getServer()));
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The squad handler failed once and goes on: {}", t.toString(), t);
            }
        }
    }

    private static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        int every = Math.max(2, SquadConfig.get(SquadConfig.TICK_EVERY));
        Iterator<Squad> it = SQUADS.values().iterator();
        while (it.hasNext()) {
            Squad s = it.next();
            if (Math.floorMod(s.id().hashCode(), every) != now % every) {
                continue;
            }
            try {
                thinks++;
                s.think(level, now);
                drive(level, s, now);
                if (s.memberCount() == 0 && now - s.lastAliveAt() > FORGET_AFTER) it.remove();
                else if (s.aliveCount() == 0 && s.memberCount() > 0 && now - s.lastAliveAt() > FORGET_AFTER) it.remove();
            } catch (RuntimeException | LinkageError fault) {
                if (!warned) {
                    warned = true;
                    LOG.warn("[nusmp] A squad failed once and was dropped: {}", fault.toString(), fault);
                }
                it.remove();
            }
        }
    }

    /** One think pass of one squad now (the self-test and the admin command), exactly as the tick makes it. */
    public static void thinkNow(ServerLevel level, Squad s) {
        long now = level.getGameTime();
        s.think(level, now);
        drive(level, s, now);
    }

    public static long thinks() { return thinks; }

    // ---------------------------------------------------------------- members without goals
    /** Steers the members that have no goals from the same decisions the goals use, and casts for a healer; the members with goals are left to their goals. */
    private static void drive(ServerLevel level, Squad s, long now) {
        for (Mob mob : s.alive()) {
            Squad.Member me = s.member(mob.getUUID());
            if (me == null || me.goals) continue;
            if (me.fleeing(now)) {
                MoraleFleeGoal.flee(mob, s.target());
                continue;
            }
            RoleMotion.Ctx c = s.contextFor(mob, now);
            if (c == null) continue;
            if (c.target() == null) me.waited = 0;
            else if (!c.tankEngaged()) me.waited += 10;
            LivingEntity t = s.target();
            if (t != null && mob.getTarget() == null && (mob.hasLineOfSight(t) || mob.distanceToSqr(t) < 16.0 * 16.0)) aim(mob, t);
            Optional<Move> m = RoleMotion.decide(c);
            if (m.isPresent()) {
                Move mv = m.get();
                if (mv.kind() == Kind.ABORT) me.flip = !me.flip;
                else if (mv.kind() == Kind.HOLD) mob.getNavigation().stop();
                else if (mv.to() != null) {
                    double y = mv.kind() == Kind.FLANK || mv.kind() == Kind.INTERCEPT ? (t != null ? t.getY() : mob.getY()) : mob.getY();
                    mob.getNavigation().moveTo(mv.to().x(), y, mv.to().z(), mv.speed());
                }
            }
            if (me.role == SquadRole.SUPPORT) SupportCast.tick(level, s, mob, me, now);
        }
    }

    // ---------------------------------------------------------------- aiming
    /** Tells a mob who it is after, in the plain slot and in the brain when it has one (a mob without a brain simply has no such memory). */
    static void aim(Mob m, LivingEntity target) {
        try {
            m.setTarget(target);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } catch (RuntimeException | LinkageError ignored) {
            // the plain slot was enough
        }
    }
}
