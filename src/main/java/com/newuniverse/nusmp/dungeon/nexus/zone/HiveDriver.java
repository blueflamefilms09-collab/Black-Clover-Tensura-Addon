package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveCues.Call;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Cue;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Foe;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Order;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the swarm of the Tempest Mirage from outside (docs/living_dungeon.md section 5, zone 3). The members are real mobs of Tensura driven by
 * brains of their own, so this does what dungeon/wild/WildDriver does: one handler on the server tick, with nothing but the public API of a mob
 * (a target, the navigation, a strike). One {@link HiveTactics} per village keeps the shared picture: every tenth tick the driver tells it what
 * the members see, asks for orders, and over the next ticks carries them out, a few members at a time. A change of the hive's state is
 * announced (a horn, a line on the action bar, the colour over its banner), the tactics decide who may strike and when, and what the swarm
 * does to one player is capped by {@link HiveRules.Budget}, so it is never an unavoidable stun-lock. The swarm is capped at
 * {@link HiveWaves#CAP}, comes in waves while players are in the village, leaves no loot and no carcass, and dissolves with the mirage when the
 * Nexus collapses. Nothing but the marks on the entities is saved: a restart finds the members again as they load.
 */
public final class HiveDriver {
    private HiveDriver() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Ticks between two applications of one member's order; how far a member sees (blocks); the most sight lines cast in one thought. */
    private static final int APPLY = 4, MAX_LOS = 24;
    private static final double SIGHT = 20.0;
    /** The most members a collapsing village dissolves at once. */
    private static final int DISSOLVE_BATCH = 4;

    /** One village's swarm: its tactics, the members that are loaded, the orders in force. */
    private static final class Colony {
        final long key;
        final TempestMiragePlan.Layout layout;
        final HiveTactics tactics;
        final Map<UUID, Kind> members = new LinkedHashMap<>();
        final Map<Integer, Order> orders = new HashMap<>();
        final Map<UUID, double[]> walks = new HashMap<>();
        long lastWave = Long.MIN_VALUE, dismissedAt = -1;
        boolean marked;

        Colony(long key, TempestMiragePlan.Layout layout) {
            this.key = key;
            this.layout = layout;
            ZonePlan p = layout.plan();
            this.tactics = new HiveTactics(layout.rally().x() + 0.5, layout.rally().z() + 0.5, p.centreX() + 0.5, p.centreZ() + 0.5, TempestMiragePlan.A, TempestMiragePlan.B);
        }
    }

    private static final Map<Long, Colony> COLONIES = new LinkedHashMap<>();
    private static final Map<UUID, HiveRules.Budget> BUDGETS = new HashMap<>();
    /** Types whose attack threw: they are left to their own brains. */
    private static final Set<EntityType<?>> CANNOT_HIT = new HashSet<>();
    private static final Set<String> WARNED = new HashSet<>();

    private static Colony colony(long key) {
        Colony c = COLONIES.get(key);
        if (c == null) {
            c = new Colony(key, TempestMiragePlan.layout(NexusCells.cellX(key), NexusCells.cellZ(key)));
            COLONIES.put(key, c);
        }
        return c;
    }

    // ---------------------------------------------------------------- events
    /** A member joins the Dungeon's level, new or loaded from disk: it is taken in (or, if its village has collapsed, it is not). */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel l) || !LivingDungeon.in(l) || !HiveMutant.is(e.getEntity())) return;
        long key = HiveMutant.colonyOf(e.getEntity());
        try {
            Colony existing = COLONIES.get(key);
            if ((existing != null && existing.dismissedAt >= 0) || NexusZones.stateOf(l.getServer(), NexusCells.cellX(key), NexusCells.cellZ(key)) == NexusState.COLLAPSED) {
                e.setCanceled(true);                                              // the mirage is gone, and what it made with it
                return;
            }
            colony(key).members.put(e.getEntity().getUUID(), HiveMutant.kindOf(e.getEntity()));
        } catch (RuntimeException | LinkageError ex) {
            warn("taking in a member", ex);
        }
    }

    /** A member leaves the level (it died, or its chunk was unloaded): it is no longer the driver's to run. */
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (!HiveMutant.is(e.getEntity())) return;
        Colony c = COLONIES.get(HiveMutant.colonyOf(e.getEntity()));
        if (c == null) return;
        c.members.remove(e.getEntity().getUUID());
        c.walks.remove(e.getEntity().getUUID());
        c.orders.remove(e.getEntity().getId());
    }

    /** A member falls: the hive hears its cry. */
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !HiveMutant.is(m) || !(m.level() instanceof ServerLevel l)) return;
        Colony c = COLONIES.get(HiveMutant.colonyOf(m));
        if (c != null) {
            c.tactics.died(m.getId(), l.getGameTime());
            c.members.remove(m.getUUID());
            c.orders.remove(m.getId());
        }
        l.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.6, m.getZ(), 10, 0.4, 0.5, 0.4, 0.04);
    }

    /** Loot-free: a mutant leaves nothing behind, nor a carcass (the swarm is not part of the food web). Register before the Dungeon's own drop handler. */
    public static void onDrops(LivingDropsEvent e) {
        if (HiveMutant.is(e.getEntity())) e.setCanceled(true);
    }

    /**
     * Two things happen here to a blow. One that a mutant lands on a player is capped (each blow a share of the victim's health, the blows of a
     * second together a smaller one), and a slime's blow may slow them a little, once in a while. And one that a player lands on a mutant is the
     * hive's pain: the hive is told who struck and from how far.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        Entity source = e.getSource().getEntity();
        if (source == null || !(victim.level() instanceof ServerLevel l) || !LivingDungeon.in(l)) return;
        try {
            if (victim instanceof ServerPlayer p && HiveMutant.is(source)) {
                if (BUDGETS.size() > 64) BUDGETS.clear();
                float allowed = (float) BUDGETS.computeIfAbsent(p.getUUID(), k -> new HiveRules.Budget()).take(l.getGameTime(), e.getAmount(), p.getMaxHealth());
                if (allowed <= 0f) {
                    e.setCanceled(true);
                    return;
                }
                e.setAmount(allowed);
                Colony c = COLONIES.get(HiveMutant.colonyOf(source));
                if (c != null && HiveMutant.kindOf(source) == Kind.SLIME && c.tactics.mayRoot(p.getId(), l.getGameTime())) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
                }
            } else if (source instanceof ServerPlayer p && HiveMutant.is(victim)) {
                Colony c = COLONIES.get(HiveMutant.colonyOf(victim));
                if (c != null) c.tactics.hurt(victim.getId(), p.getId(), p.getX(), p.getZ(), Math.sqrt(p.distanceToSqr(victim)), l.getGameTime());
            }
        } catch (RuntimeException | LinkageError ex) {
            warn("judging a blow of the swarm", ex);
        }
    }

    /** The Nexus falls: the mirage unravels, the swarm dissolves with it (a few at a time), nothing more is made. */
    public static void onCollapse(DungeonCollapseEvent e) {
        if (e.zone() != NexusZone.TEMPEST_MIRAGE) return;
        ServerLevel l = e.level();
        int cx = Math.floorDiv(e.centre().getX(), NexusZone.CELL), cz = Math.floorDiv(e.centre().getZ(), NexusZone.CELL);
        long key = NexusCells.key(cx, cz);
        Colony c = colony(key);
        if (c.dismissedAt >= 0) return;
        c.dismissedAt = l.getGameTime();
        Component line = Component.literal(HiveCues.UNRAVELS).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        for (ServerPlayer p : viewers(l, c)) p.displayClientMessage(line, true);
    }

    public static void onServerStopping(ServerStoppingEvent e) { clear(); }

    /** Forgets everything held about a world: the colonies, the budgets, and the timers of the fake houses and of the gates (the whole zone's statics). */
    private static void clear() {
        COLONIES.clear();
        BUDGETS.clear();
        CANNOT_HIT.clear();
        WARNED.clear();
        TempestIllusions.clear();
        TempestPassages.clear();
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (COLONIES.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == null) {
            clear();
            return;
        }
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        long now = level.getGameTime();
        for (Colony c : new ArrayList<>(COLONIES.values())) {                       // a copy: a collapsed colony removes itself
            try {
                tick(level, c, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("running the swarm", ex);
            }
        }
    }

    private static void tick(ServerLevel level, Colony c, long now) {
        List<Mob> mobs = new ArrayList<>(c.members.size());
        for (Iterator<Map.Entry<UUID, Kind>> it = c.members.entrySet().iterator(); it.hasNext(); ) {
            Entity en = level.getEntity(it.next().getKey());
            if (en instanceof Mob m && m.isAlive() && !m.isRemoved()) mobs.add(m);
            else it.remove();
        }
        if (c.dismissedAt < 0 && now % 100 == Math.floorMod(c.key, 100L)
                && NexusZones.stateOf(level.getServer(), NexusCells.cellX(c.key), NexusCells.cellZ(c.key)) == NexusState.COLLAPSED) {
            c.dismissedAt = now;                                                  // the mirage fell while nobody told this colony (a restart in the middle of the collapse): it goes now
        }
        if (c.dismissedAt >= 0) {
            dissolve(level, c, mobs, now);
            return;
        }
        mobs.removeIf(HiveDriver::kept);                                          // a tamed, ridden or leashed member is its keeper's to command, not the hive's
        if (mobs.isEmpty()) return;
        List<ServerPlayer> viewers = viewers(level, c), foes = foesOf(viewers);
        if (now % HiveTactics.THINK == Math.floorMod(c.key, (long) HiveTactics.THINK)) think(level, c, mobs, foes, viewers, now);
        for (Mob m : mobs) {
            if ((m.getId() + now) % APPLY != 0) continue;
            Order o = c.orders.get(m.getId());
            if (o == null) continue;
            try {
                apply(level, c, m, o, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("running a member of the swarm", ex);
            }
        }
        if (now % 10 == 5) banner(level, c, viewers, now);
    }

    /** Tamed, ridden, carrying a rider or on a leash: somebody else's (the same rule the Dungeon's food web keeps for its creatures). */
    private static boolean kept(Entity e) {
        return (e instanceof TamableAnimal t && t.isTame()) || e.isVehicle() || e.isPassenger() || (e instanceof Mob mob && mob.isLeashed());
    }

    /** May the swarm fight this player? Alive, and neither spectating nor in creative mode: those are never its foes (and never spring its traps). */
    static boolean fair(ServerPlayer p) { return p != null && p.isAlive() && !p.isSpectator() && !p.isCreative(); }

    /** The players in the village who can see its signs: alive, not spectating, inside its volume (a creative player sees the horns and the banner, but is not fought). */
    private static List<ServerPlayer> viewers(ServerLevel level, Colony c) {
        ZonePlan p = c.layout.plan();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer pl : level.players()) if (pl.isAlive() && !pl.isSpectator() && p.contains(pl.getBlockX(), pl.getBlockY(), pl.getBlockZ(), 2)) out.add(pl);
        return out;
    }

    /** The ones of them that the swarm may fight. */
    private static List<ServerPlayer> foesOf(List<ServerPlayer> viewers) {
        List<ServerPlayer> out = new ArrayList<>(viewers.size());
        for (ServerPlayer pl : viewers) if (fair(pl)) out.add(pl);
        return out;
    }

    private static Foe foeOf(ServerPlayer p) { return new Foe(p.getId(), p.getX(), p.getZ(), HiveRules.power(p.getMaxHealth(), p.getArmorValue())); }

    /** The hive thinks: what its members see is put on the board, and the orders come back. */
    private static void think(ServerLevel level, Colony c, List<Mob> mobs, List<ServerPlayer> foes, List<ServerPlayer> viewers, long now) {
        int casts = 0;
        for (Mob m : mobs) {
            ServerPlayer seen = null;
            double best = SIGHT * SIGHT;
            for (ServerPlayer p : foes) {
                double dx = p.getX() - m.getX(), dz = p.getZ() - m.getZ(), d = dx * dx + dz * dz;
                if (d < best && Math.abs(p.getY() - m.getY()) <= 12.0) {
                    best = d;
                    seen = p;
                }
            }
            if (seen == null || casts >= MAX_LOS) continue;
            casts++;
            if (m.hasLineOfSight(seen)) c.tactics.sight(m.getId(), foeOf(seen), now);
        }
        List<Unit> units = new ArrayList<>(mobs.size());
        for (Mob m : mobs) {
            BlockPos home = HiveMutant.homeOf(m);
            units.add(new Unit(m.getId(), c.members.get(m.getUUID()), m.getX(), m.getZ(), m.getHealth() / Math.max(1.0f, m.getMaxHealth()), home.getX() + 0.5, home.getZ() + 0.5));
            HiveMutant.pinHunger(m);
        }
        Order[] orders = c.tactics.think(now, units);
        c.orders.clear();
        for (int i = 0; i < orders.length; i++) c.orders.put(units.get(i).id(), orders[i]);
        Cue cue = c.tactics.takeCue();
        if (cue != null) announce(level, c, cue, viewers);
        boolean marked = c.tactics.markedSpot(now) != null;
        if (marked && !c.marked) {
            Component line = Component.literal(HiveCues.MARKED).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
            for (ServerPlayer p : viewers) p.displayClientMessage(line, true);
        }
        c.marked = marked;
    }

    // ---------------------------------------------------------------- carrying out an order
    private static void apply(ServerLevel level, Colony c, Mob m, Order o, long now) {
        Kind kind = c.members.get(m.getUUID());
        if (kind == null) return;
        if (o.heal() && m.getHealth() < m.getMaxHealth()) m.heal(m.getMaxHealth() * 0.004f);
        double floor = c.layout.plan().floorY + 1.0;
        switch (o.act()) {
            case WANDER -> {
                if (m.getNavigation().isDone() && m.getTarget() == null) walk(c, m, o.goalX(), floor, o.goalZ(), o.speed(), now);
            }
            case MOVE -> {
                release(m);
                walk(c, m, o.goalX(), floor, o.goalZ(), o.speed(), now);
            }
            case HOLD -> {
                release(m);
                m.getNavigation().stop();
            }
            case ENGAGE -> {
                Entity found = o.target() >= 0 ? level.getEntity(o.target()) : null;
                ServerPlayer target = found instanceof ServerPlayer sp && fair(sp) ? sp : null;
                engage(c, m, kind, o, target, floor, now);
            }
        }
    }

    /** Walks to a point: a new path only when the goal has moved, the old one is done, or it has gone stale; never to a place it is already at. */
    private static void walk(Colony c, Mob m, double x, double y, double z, double speed, long now) {
        double[] last = c.walks.get(m.getUUID());
        boolean fresh = last == null || Math.hypot(last[0] - x, last[1] - z) > 2.5 || now - (long) last[2] >= 20;
        if (!fresh && !m.getNavigation().isDone()) return;
        if (m.distanceToSqr(x, y, z) < 2.0) {
            m.getNavigation().stop();
            return;
        }
        try {
            m.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);                   // a walk of its own brain's would undo this one
        } catch (RuntimeException | LinkageError ignored) {
            // a mob without a brain has no walk to erase
        }
        m.getNavigation().moveTo(x, y, z, speed);
        c.walks.put(m.getUUID(), new double[]{x, z, now});
    }

    private static void engage(Colony c, Mob m, Kind kind, Order o, ServerPlayer target, double floor, long now) {
        if (target == null) {
            release(m);
            walk(c, m, o.goalX(), floor, o.goalZ(), o.speed(), now);
            return;
        }
        double reach = kind.reach + target.getBbWidth() * 0.5, dist = Math.sqrt(m.distanceToSqr(target));
        if (!o.strike()) {                                                             // no turn of its own: it holds its place in the formation
            if (m.getTarget() instanceof Player) release(m);
            walk(c, m, o.goalX(), target.getY(), o.goalZ(), o.speed(), now);
            m.getLookControl().setLookAt(target, 30f, 30f);
            return;
        }
        aim(m, target);
        m.getLookControl().setLookAt(target, 40f, 40f);
        if (dist > reach + 0.3) {
            m.getNavigation().moveTo(target, o.speed());
            return;
        }
        if (!c.tactics.mayStrike(m.getId(), target.getId(), now)) return;
        if (target.getLastHurtByMob() == m && target.tickCount - target.getLastHurtByMobTimestamp() < 10) {      // its own brain has just struck: that was its blow
            c.tactics.struck(m.getId(), kind, target.getId(), now);
            return;
        }
        if (CANNOT_HIT.contains(m.getType())) return;
        m.swing(InteractionHand.MAIN_HAND);
        try {
            m.doHurtTarget(target);
        } catch (RuntimeException | LinkageError ex) {
            CANNOT_HIT.add(m.getType());                                                // no attack damage, or an attack of another shape
            warn("striking with a member of the swarm", ex);
        }
        c.tactics.struck(m.getId(), kind, target.getId(), now);
    }

    /** Tells a mob's own brain (when it has the memory) and the plain target slot who it is after; a brain of another shape ignores it. */
    static void aim(Mob m, LivingEntity prey) {
        try {
            m.setTarget(prey);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, prey);
        } catch (RuntimeException | LinkageError ignored) {
            // the walk and the strike do not depend on it
        }
    }

    /** Lets go of whoever the brain was after, so it does not chase the player on its own while it has been ordered elsewhere. */
    private static void release(Mob m) {
        try {
            if (m.getTarget() == null) return;
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (RuntimeException | LinkageError ignored) {
            // nothing more to undo
        }
    }

    // ---------------------------------------------------------------- telegraphing
    private static SoundEvent sound(Call call) {
        return switch (call) {
            case ALARM, RETREAT -> SoundEvents.RAID_HORN.value();
            case CHARGE -> SoundEvents.RAVAGER_ROAR;
            case CLOSE_IN -> SoundEvents.BELL_BLOCK;
            case REGROUP -> SoundEvents.BELL_RESONATE;
            default -> null;
        };
    }

    private static float pitch(Call call) {
        return switch (call) {
            case ALARM -> 0.8f;
            case RETREAT -> 1.5f;
            case CHARGE -> 0.8f;
            case CLOSE_IN -> 0.5f;
            default -> 1.2f;
        };
    }

    /** A change of the hive's state: a horn over the village, a line to the players in it, a burst of colour over the banner. */
    private static void announce(ServerLevel level, Colony c, Cue cue, List<ServerPlayer> viewers) {
        ZonePlan.Spot r = c.layout.rally(), s = c.layout.standard();
        SoundEvent sound = sound(HiveCues.call(cue.from(), cue.to()));
        if (sound != null) level.playSound(null, new BlockPos(r.x(), r.y() + 6, r.z()), sound, SoundSource.HOSTILE, 4.0f, pitch(HiveCues.call(cue.from(), cue.to())));
        String line = HiveCues.line(cue.from(), cue.to());
        if (!line.isEmpty()) {
            Component text = Component.literal(line).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            for (ServerPlayer p : viewers) p.displayClientMessage(text, true);
        }
        int[] col = HiveCues.colour(cue.to());
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(col[0] / 255f, col[1] / 255f, col[2] / 255f), 2.4f);
        level.sendParticles(dust, s.x() + 0.5, s.y() + 12.5, s.z() + 0.5, 40, 1.2, 1.5, 1.2, 0.0);
    }

    /** The colour over the banner says which state the hive is in; a mark over the place it has learned a foe shoots from. */
    private static void banner(ServerLevel level, Colony c, List<ServerPlayer> viewers, long now) {
        if (viewers.isEmpty()) return;
        int[] col = HiveCues.colour(c.tactics.state());
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(col[0] / 255f, col[1] / 255f, col[2] / 255f), 2.0f);
        ZonePlan.Spot s = c.layout.standard();
        double[] spot = c.tactics.markedSpot(now);
        for (ServerPlayer p : viewers) {
            level.sendParticles(p, dust, true, s.x() + 0.5, s.y() + 12.5, s.z() + 0.5, 6, 0.3, 0.8, 0.3, 0.0);
            if (spot != null && now % 20 == 5) level.sendParticles(p, ParticleTypes.ANGRY_VILLAGER, true, spot[0], s.y() + 1.5, spot[1], 3, 0.3, 0.4, 0.3, 0.0);
        }
    }

    // ---------------------------------------------------------------- bringing the swarm in
    /** How many mutants of this village are alive in the loaded world. */
    public static int alive(ServerLevel level, ZonePlan plan, long key) {
        AABB box = new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
        return level.getEntitiesOfClass(Mob.class, box, m -> HiveMutant.is(m) && HiveMutant.colonyOf(m) == key && m.isAlive()).size();
    }

    private static boolean loaded(ServerLevel level, ZonePlan plan) {
        for (int[] ch : plan.chunks()) if (level.getChunkSource().getChunkNow(ch[0], ch[1]) == null) return false;
        return true;
    }

    private static boolean seenBy(Mob m, List<ServerPlayer> players) {
        for (ServerPlayer p : players) if (p.distanceToSqr(m) < 56.0 * 56.0 && p.hasLineOfSight(m)) return true;
        return false;
    }

    /** The first wave, when the village is built: nobody is in it yet. Any member left from an earlier village in this place goes first. */
    public static void populate(ServerLevel level, TempestMiragePlan.Layout l) {
        ZonePlan plan = l.plan();
        long key = NexusCells.key(plan.cellX, plan.cellZ);
        AABB box = new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
        for (Mob old : level.getEntitiesOfClass(Mob.class, box, m -> HiveMutant.is(m) && HiveMutant.colonyOf(m) == key)) old.discard();
        Colony c = colony(key);
        c.members.clear();
        c.orders.clear();
        c.dismissedAt = -1;
        c.lastWave = level.getGameTime();
        int made = spawn(level, c, HiveWaves.FIRST, List.of());
        LOG.info("[nusmp] The Tempest Mirage of cell ({}, {}) was filled with a swarm of {}", plan.cellX, plan.cellZ, made);
    }

    /** About once a second while players are in the village: a wave if one is due (every chunk of the village loaded, so the count is true). */
    public static void waves(ServerLevel level, TempestMiragePlan.Layout l, List<ServerPlayer> inside, long now) {
        ZonePlan plan = l.plan();
        long key = NexusCells.key(plan.cellX, plan.cellZ);
        Colony c = colony(key);
        if (c.dismissedAt >= 0 || !loaded(level, plan)) return;
        if (!DungeonConfig.get(DungeonConfig.LIFE)) return;                      // the Dungeon's switch for births is off: the swarm is not refilled
        int present = 0;
        for (ServerPlayer p : inside) if (fair(p)) present++;                    // a creative player brings no wave
        int n = HiveWaves.due(alive(level, plan, key), now, c.lastWave, present);
        if (n <= 0) return;
        c.lastWave = now;
        spawn(level, c, n, inside);
    }

    /** `count` mutants at the village's swarm spots, none within sight of a player. Returns how many came. */
    private static int spawn(ServerLevel level, Colony c, int count, List<ServerPlayer> inside) {
        ZonePlan plan = c.layout.plan();
        List<ZonePlan.Spot> spots = plan.spawns(ZonePlan.ROLE_SWARM);
        if (spots.isEmpty() || count <= 0) return 0;
        double[][] at = new double[spots.size()][];
        for (int i = 0; i < at.length; i++) at[i] = new double[]{spots.get(i).x() + 0.5, spots.get(i).z() + 0.5};
        double[][] players = new double[inside.size()][];
        for (int i = 0; i < players.length; i++) players[i] = new double[]{inside.get(i).getX(), inside.get(i).getZ()};
        int made = 0;
        for (int index : HiveWaves.pickSpots(at, players, count, c.key ^ level.getGameTime())) {
            ZonePlan.Spot s = spots.get(index);
            Mob m = HiveMutant.create(level, HiveWaves.kindFor(level.getRandom().nextDouble()), new Vec3(s.x() + 0.5, s.y(), s.z() + 0.5), c.key, new BlockPos(s.x(), s.y(), s.z()), level.getRandom().nextDouble());
            if (m == null) continue;
            if (!level.noCollision(m.getBoundingBox()) || seenBy(m, inside)) {
                m.discard();
                continue;
            }
            if (HiveMutant.add(level, m)) made++;
        }
        return made;
    }

    /**
     * A fake house has dissolved: its swarm comes out of it at once, on the one who sprang it, and the hive is on that player. Returns how many
     * came. The cap holds: a trap may bring a few over it, never more than {@link HiveWaves#OVERFLOW}.
     */
    public static int ambush(ServerLevel level, TempestMiragePlan.Layout l, TempestMiragePlan.Fake f, ServerPlayer cause, int players) {
        ZonePlan plan = l.plan();
        long key = NexusCells.key(plan.cellX, plan.cellZ), now = level.getGameTime();
        Colony c = colony(key);
        int n = HiveWaves.ambushSize(alive(level, plan, key), players), made = 0;
        for (int i = 0; i < n * 2 && made < n; i++) {
            ZonePlan.Spot s = f.ambush().get(i % f.ambush().size());
            Mob m = HiveMutant.create(level, HiveWaves.kindFor(level.getRandom().nextDouble()), new Vec3(s.x() + 0.5, s.y(), s.z() + 0.5), key, new BlockPos(s.x(), s.y(), s.z()), level.getRandom().nextDouble());
            if (m == null) continue;
            if (!level.noCollision(m.getBoundingBox())) {
                m.discard();
                continue;
            }
            if (!HiveMutant.add(level, m)) continue;
            if (fair(cause)) aim(m, cause);
            made++;
        }
        if (fair(cause)) c.tactics.ambush(foeOf(cause), now);
        Cue cue = c.tactics.takeCue();
        if (cue != null) announce(level, c, cue, viewers(level, c));
        return made;
    }

    // ---------------------------------------------------------------- the end
    /** A collapsing village's swarm goes a few at a time in a puff of haze; when none are left the colony is forgotten. */
    private static void dissolve(ServerLevel level, Colony c, List<Mob> mobs, long now) {
        if (mobs.isEmpty()) {
            COLONIES.remove(c.key);
            return;
        }
        if (now % 5 != 0) return;
        int n = 0;
        for (Mob m : mobs) {
            if (n++ >= DISSOLVE_BATCH) break;
            level.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 0.8, m.getZ(), 14, 0.4, 0.6, 0.4, 0.05);
            c.members.remove(m.getUUID());
            m.discard();
        }
    }

    private static void warn(String what, Throwable ex) {
        if (WARNED.add(what + ex.getClass().getName())) LOG.warn("[nusmp] Trouble {} (this is logged once)", what, ex);
    }
}
