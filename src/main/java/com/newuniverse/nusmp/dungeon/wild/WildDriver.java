package com.newuniverse.nusmp.dungeon.wild;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.CarcassBlock;
import com.newuniverse.nusmp.dungeon.DungeonBlocks;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.dungeon.EcosystemData;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The food web of the Living Dungeon for creatures that are not of its own classes (docs/living_dungeon.md section 1). The real mobs of
 * Tensura and Ice and Fire are driven by brains and goals of their own, which are left alone; this drives them from outside, from one server
 * tick handler, with nothing but the public API of a mob:
 * <ul>
 *   <li><b>Hunger</b> falls with time, starvation hurts at zero, a kill or a meal feeds ({@link Dweller}).</li>
 *   <li><b>Hunting</b>: a hungry creature of tier 2 or more, with no target of its own, picks the nearest prey of a lower tier within
 *       16 blocks, walks it down, strikes it (reach check, cooldown) and is fed by the kill. It never goes for a player; whatever its own
 *       brain does about players, it keeps doing.</li>
 *   <li><b>Scavenging</b>: tier 1 (and the grazers) walk to a Carcass or a veil of Dungeon Mycelium within 10 blocks and eat it, and graze the
 *       magicules of the air.</li>
 *   <li><b>Resting</b>: a full apex predator with nothing to do goes to sleep (switched off with setNoAi) and wakes when it is hungry,
 *       hurt, or someone comes into its den. Nothing stays asleep when the Dungeon is switched off or the creature leaves it.</li>
 * </ul>
 * One sweep a tick over a tenth of the roster creatures near the players (staggered by entity id), every one inside a try block: a mob of
 * an unexpected shape costs a log line, never the server.
 */
public final class WildDriver {
    private WildDriver() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Ticks between two looks at one creature, creatures looked at in one cycle at the most, errands at once, ticks between two steps of an errand. */
    private static final int PASS = 10, MAX_CROWD = 160, MAX_ERRANDS = 48, ERRAND_STEP = 2;
    private static final double HUNT_SPEED = 1.15, FOOD_SPEED = 1.0;

    /** The roster creatures near a player of the Dungeon, collected once a cycle. */
    private static final List<Mob> crowd = new ArrayList<>();
    /** What each creature is doing right now (a hunt or a walk to food), by its UUID. */
    private static final Map<UUID, Errand> errands = new HashMap<>();
    /** Types whose attack threw: they are not asked to hunt again. */
    private static final Set<EntityType<?>> cannotHit = new HashSet<>();
    private static final Set<String> warned = new HashSet<>();

    /** One errand: a hunt (prey set) or a walk to food (food set). */
    private static final class Errand {
        final UUID prey;
        final BlockPos food;
        final HuntPlan plan = new HuntPlan();
        int age, repath, noPath;
        boolean fed;

        private Errand(UUID prey, BlockPos food) {
            this.prey = prey;
            this.food = food;
        }

        static Errand hunt(UUID prey) { return new Errand(prey, null); }

        static Errand forage(BlockPos food) { return new Errand(null, food); }

        boolean hunting() { return prey != null; }
    }

    // ---------------------------------------------------------------- events
    /** A creature joins a level: in the Dungeon it gets its record and stays; anywhere else a sleeper is woken. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof Mob m) || m instanceof DungeonCreature) return;
        boolean dungeon = LivingDungeon.in(e.getLevel()) && DungeonConfig.get(DungeonConfig.ENABLED);
        if (!dungeon) {
            if (m.isNoAi() && m.hasData(Dweller.DWELLER.get())) {
                Dweller w = m.getData(Dweller.DWELLER);
                if (w.asleep) wake(m, w, 0L);
            }
            return;
        }
        if (Wildlife.tierOf(m) == 0) return;
        Dweller w = m.getData(Dweller.DWELLER);
        if (!w.hasHome()) w.setHome(m.blockPosition());
        m.setPersistenceRequired();
    }

    /** Whatever kills in the Dungeon, if it is a creature of the roster, eats. */
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel d) || !LivingDungeon.in(d)) return;
        if (!(e.getSource().getEntity() instanceof Mob m) || m == victim || m instanceof DungeonCreature) return;
        if (!m.isAlive() || Wildlife.tierOf(m) == 0 || !m.hasData(Dweller.DWELLER.get())) return;
        Errand er = errands.get(m.getUUID());
        if (er != null && er.hunting() && victim.getUUID().equals(er.prey)) er.fed = true;          // the hunt must not feed it twice
        feed(d, m, m.getData(Dweller.DWELLER), WildRules.KILL_FEED, WildRules.KILL_MAGICULE);
    }

    /** Reads the roster once the tags are bound, so the log says at start-up which mods the Dungeon found. */
    public static void onServerStarted(ServerStartedEvent e) { Wildlife.roster(); }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel d = server.getLevel(LivingDungeon.DIMENSION);
        if (d == null) {
            crowd.clear();
            errands.clear();
            return;
        }
        int tick = server.getTickCount();
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) {
            if (tick % 100 == 7) {                                                                // switched off: nothing is left asleep
                crowd.clear();
                errands.clear();
                wakeAll(d);
            }
            return;
        }
        int slot = tick % PASS;
        if (slot == 0) refresh(d);
        if (!crowd.isEmpty()) sweep(d, slot);
        if (tick % ERRAND_STEP == 0 && !errands.isEmpty()) runErrands(d);
    }

    // ---------------------------------------------------------------- the sweep
    private static boolean member(Mob m) {
        return m.isAlive() && !(m instanceof DungeonCreature) && (Wildlife.tierOf(m) > 0 || (m.isNoAi() && m.hasData(Dweller.DWELLER.get())));
    }

    private static void refresh(ServerLevel d) {
        crowd.clear();
        if (d.players().isEmpty()) return;
        Set<Integer> seen = new HashSet<>();
        double reach = (DungeonConfig.get(DungeonConfig.SIM_RADIUS) + 1) * 16.0;
        for (ServerPlayer p : d.players()) {
            for (Mob m : d.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(reach, 400.0, reach), WildDriver::member)) {
                if (crowd.size() >= MAX_CROWD) return;
                if (seen.add(m.getId())) crowd.add(m);
            }
        }
    }

    private static void sweep(ServerLevel d, int slot) {
        long now = d.getGameTime();
        for (int i = 0; i < crowd.size(); i++) {
            Mob m = crowd.get(i);
            if (Math.floorMod(m.getId(), PASS) != slot || m.level() != d || !m.isAlive() || m.isRemoved()) continue;
            try {
                step(d, m, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("looking at", m.getType(), ex);
            }
        }
    }

    /** One look at one creature. */
    private static void step(ServerLevel d, Mob m, long now) {
        int tier = Wildlife.tierOf(m);
        if (tier == 0) {                                                                          // vetoed since it fell asleep: wake it
            if (m.isNoAi() && m.hasData(Dweller.DWELLER.get())) {
                Dweller w = m.getData(Dweller.DWELLER);
                if (w.asleep) wake(m, w, now);
            }
            return;
        }
        Dweller w = m.getData(Dweller.DWELLER);
        if (!w.hasHome()) w.setHome(m.blockPosition());
        if (kept(m)) {                                                                            // tamed, ridden or leashed: not ours to drive
            if (w.asleep) wake(m, w, now);
            return;
        }
        if (m.isNoAi() && !w.asleep) return;                                                     // switched off by somebody else

        boolean legend = Wildlife.isLegendary(m);
        if (w.appetite.advance(now, legend)) m.hurt(d.damageSources().starve(), WildRules.starveDamage(m.getMaxHealth()));
        if (!m.isAlive()) return;

        if (w.asleep) {
            if (WildRules.mustWake(w.appetite.hunger(), WildRules.hurtSince(w.sleepHealth, m.getHealth()), playerInDen(d, m, w))) wake(m, w, now);
            return;
        }

        boolean grazer = tier == 1 || Wildlife.isGrazer(m);
        if (grazer && now >= w.nextGraze) graze(d, m, w, now);
        if (m.getTarget() != null || errands.containsKey(m.getUUID())) return;                    // busy with its own business
        if (grazer) {
            if (!w.appetite.full() && now >= w.nextForage) forage(d, m, w, now);
        } else if (w.appetite.hungry()) {
            if (now >= w.nextHunt) hunt(d, m, w, tier, legend, now);
        } else if (WildRules.mayFallAsleep(tier >= 3, legend, w.appetite.hunger(), false, false, m.onGround(),
                playerWithin(d, m, WildRules.SLEEP_CLEAR), WildRules.justWoken(now, w.wokenAt))) {
            sleep(m, w);
        }
    }

    // ---------------------------------------------------------------- resting
    private static void sleep(Mob m, Dweller w) {
        m.getNavigation().stop();
        m.setNoAi(true);
        w.asleep = true;
        w.sleepHealth = m.getHealth();
    }

    private static void wake(Mob m, Dweller w, long now) {
        w.asleep = false;
        w.wokenAt = now;
        m.setNoAi(false);
    }

    /** With the Dungeon switched off, every sleeper near a player is woken. */
    private static void wakeAll(ServerLevel d) {
        long now = d.getGameTime();
        for (ServerPlayer p : d.players()) {
            for (Mob m : d.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(160.0, 400.0, 160.0), x -> x.isNoAi() && x.hasData(Dweller.DWELLER.get()))) {
                Dweller w = m.getData(Dweller.DWELLER);
                if (w.asleep) wake(m, w, now);
            }
        }
    }

    private static boolean playerWithin(ServerLevel d, Entity e, double radius) {
        double r2 = radius * radius;
        for (ServerPlayer p : d.players()) if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(e) <= r2) return true;
        return false;
    }

    /** Is a player inside the den: near the sleeper, or near the place it calls home? */
    private static boolean playerInDen(ServerLevel d, Mob m, Dweller w) {
        if (playerWithin(d, m, WildRules.DEN_RADIUS)) return true;
        if (!w.hasHome()) return false;
        BlockPos h = w.home();
        double r2 = (double) WildRules.DEN_RADIUS * WildRules.DEN_RADIUS;
        for (ServerPlayer p : d.players()) if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(h.getX() + 0.5, h.getY() + 0.5, h.getZ() + 0.5) <= r2) return true;
        return false;
    }

    /** Tamed, ridden, carrying a rider or on a leash: somebody else's. */
    private static boolean kept(Entity e) {
        return (e instanceof TamableAnimal t && t.isTame()) || e.isVehicle() || e.isPassenger() || (e instanceof Mob mob && mob.isLeashed());
    }

    // ---------------------------------------------------------------- eating
    static void feed(ServerLevel d, Mob m, Dweller w, int amount, float magicule) {
        w.appetite.feed(amount);
        if (magicule <= 0f) return;
        EcosystemData eco = LivingDungeon.eco(d, m.blockPosition());
        if (eco == null) return;
        eco.magicule += magicule;
        eco.clamp();
        LivingDungeon.touch(d, m.blockPosition());
    }

    /** The small things graze the magicules of the air, as the custom Dungeon slimes did. */
    private static void graze(ServerLevel d, Mob m, Dweller w, long now) {
        w.nextGraze = now + WildRules.GRAZE_INTERVAL;
        EcosystemData eco = LivingDungeon.eco(d, m.blockPosition());
        if (eco == null || eco.magicule < WildRules.GRAZE_MIN || w.appetite.hunger() >= 95) return;
        boolean slime = Wildlife.carcassKind(m) == 0;
        eco.magicule -= WildRules.grazeCost(slime);
        eco.clamp();
        LivingDungeon.touch(d, m.blockPosition());
        w.appetite.feed(WildRules.grazeGain(slime));
    }

    private static boolean edible(ServerLevel d, BlockPos p) {
        BlockState b = d.getBlockState(p);
        return b.is(DungeonBlocks.CARCASS.get()) || b.is(DungeonBlocks.DUNGEON_MYCELIUM.get());
    }

    private static void forage(ServerLevel d, Mob m, Dweller w, long now) {
        w.nextForage = now + 60;
        if (errands.size() >= MAX_ERRANDS) return;
        BlockPos at = m.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = WildRules.FOOD_RADIUS, h = WildRules.FOOD_HEIGHT;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -h; dy <= h; dy++) {
                    cursor.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (!edible(d, cursor)) continue;
                    double dist = cursor.distSqr(at);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        if (best != null) errands.put(m.getUUID(), Errand.forage(best));
    }

    // ---------------------------------------------------------------- hunting
    private static void hunt(ServerLevel d, Mob m, Dweller w, int tier, boolean legend, long now) {
        w.nextHunt = now + 40;
        if (cannotHit.contains(m.getType()) || errands.size() >= MAX_ERRANDS) return;
        float health = m.getMaxHealth();
        List<LivingEntity> found = d.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(WildRules.PREY_RADIUS),
                e -> isPrey(d, m, w, tier, legend, health, now, e));
        if (found.isEmpty()) return;
        found.sort(Comparator.comparingDouble(m::distanceToSqr));
        LivingEntity prey = found.get(0);
        errands.put(m.getUUID(), Errand.hunt(prey.getUUID()));
        aim(m, prey);
    }

    private static boolean isPrey(ServerLevel d, Mob m, Dweller w, int tier, boolean legend, float health, long now, LivingEntity e) {
        if (e == m || !e.isAlive() || e instanceof Player) return false;                          // players are never added to the menu here
        int preyTier = Wildlife.tierOf(e);
        if (preyTier == 0 || !WildRules.mayPrey(tier, legend, preyTier, Wildlife.isLegendary(e), health, e.getMaxHealth())) return false;
        if (e.getUUID().equals(w.avoid) && now < w.avoidUntil) return false;
        if (kept(e) || m.isAlliedTo(e)) return false;
        return !e.isInvulnerableTo(d.damageSources().mobAttack(m));
    }

    /** Tells the mob's own brain (when it has the memory) and the plain target slot who it is after; a brain of another shape just ignores it. */
    private static void aim(Mob m, LivingEntity prey) {
        try {
            m.setTarget(prey);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, prey);
        } catch (RuntimeException | LinkageError ignored) {
            // the walk and the strike below do not depend on it
        }
    }

    // ---------------------------------------------------------------- errands
    private static void runErrands(ServerLevel d) {
        Iterator<Map.Entry<UUID, Errand>> it = errands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Errand> entry = it.next();
            Entity who = d.getEntity(entry.getKey());
            if (!(who instanceof Mob m) || !m.isAlive() || m.isRemoved() || m.isNoAi() || kept(m)) {
                it.remove();
                continue;
            }
            Errand er = entry.getValue();
            boolean done;
            try {
                done = er.hunting() ? huntStep(d, m, er) : forageStep(d, m, er);
            } catch (RuntimeException | LinkageError ex) {
                warn("running an errand of", m.getType(), ex);
                done = true;
            }
            if (done) {
                finish(m, er);
                it.remove();
            }
        }
    }

    private static boolean huntStep(ServerLevel d, Mob m, Errand er) {
        Dweller w = m.getData(Dweller.DWELLER);
        Entity found = d.getEntity(er.prey);
        LivingEntity prey = found instanceof LivingEntity l && l.isAlive() ? l : null;
        boolean reach = prey != null && m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(prey.getBoundingBox());
        double dist = prey == null ? 0.0 : m.distanceToSqr(prey);
        switch (er.plan.step(ERRAND_STEP, dist, reach, prey != null, w.appetite.full())) {
            case DONE -> {
                boolean killed = found instanceof LivingEntity l && l.isDeadOrDying();
                if (killed && !er.fed && er.plan.strikes() > 0) feed(d, m, w, WildRules.KILL_FEED, WildRules.KILL_MAGICULE);
                return true;
            }
            case GIVE_UP -> {
                if (prey != null) {
                    w.avoid = prey.getUUID();
                    w.avoidUntil = d.getGameTime() + WildRules.AVOID_SPAN;
                }
                return true;
            }
            case STRIKE -> {
                aim(m, prey);
                m.getLookControl().setLookAt(prey, 60f, 60f);
                m.swing(InteractionHand.MAIN_HAND);
                try {
                    m.doHurtTarget(prey);
                } catch (RuntimeException | LinkageError ex) {
                    cannotHit.add(m.getType());                                                  // no attack damage, or an attack of another shape
                    warn("striking with", m.getType(), ex);
                    return true;
                }
            }
            case HOLD -> m.getLookControl().setLookAt(prey, 60f, 60f);
            default -> {
                m.getLookControl().setLookAt(prey, 30f, 30f);
                if (er.plan.wantsPath()) {
                    aim(m, prey);
                    er.plan.pathResult(m.getNavigation().moveTo(prey, HUNT_SPEED));
                }
            }
        }
        return false;
    }

    private static boolean forageStep(ServerLevel d, Mob m, Errand er) {
        Dweller w = m.getData(Dweller.DWELLER);
        er.age += ERRAND_STEP;
        if (er.age > 400 || w.appetite.full() || er.noPath >= 3 || !edible(d, er.food)) return true;
        double cx = er.food.getX() + 0.5, cy = er.food.getY(), cz = er.food.getZ() + 0.5;
        double dx = m.getX() - cx, dz = m.getZ() - cz, reach = m.getBbWidth() * 0.5 + 1.4;
        if (dx * dx + dz * dz <= reach * reach && Math.abs(m.getY() - cy) <= 2.5) {
            if (d.getBlockState(er.food).is(DungeonBlocks.CARCASS.get())) {
                CarcassBlock.nibble(d, er.food);
                feed(d, m, w, WildRules.CARCASS_FEED, WildRules.CARCASS_MAGICULE);
            } else {
                d.removeBlock(er.food, false);
                feed(d, m, w, WildRules.MYCELIUM_FEED, 0f);
            }
            return true;
        }
        er.repath -= ERRAND_STEP;
        if (er.repath <= 0) {
            er.repath = HuntPlan.REPATH;
            if (m.getNavigation().moveTo(cx, cy, cz, FOOD_SPEED)) er.noPath = 0;
            else er.noPath++;
        }
        return false;
    }

    /** The errand is over: a hunter lets go of its prey, so its own brain is not left chasing a dead or lost target. */
    private static void finish(Mob m, Errand er) {
        try {
            if (er.hunting()) {
                LivingEntity t = m.getTarget();
                if (t != null && t.getUUID().equals(er.prey)) {
                    m.setTarget(null);
                    m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
            }
            m.getNavigation().stop();
        } catch (RuntimeException | LinkageError ignored) {
            // nothing more to undo
        }
    }

    private static void warn(String what, EntityType<?> type, Throwable ex) {
        String key = what + " " + Wildlife.idOf(type) + " " + ex.getClass().getName();
        if (warned.add(key)) LOG.warn("Living Dungeon: trouble {} {} (this is logged once)", what, Wildlife.idOf(type), ex);
    }
}
