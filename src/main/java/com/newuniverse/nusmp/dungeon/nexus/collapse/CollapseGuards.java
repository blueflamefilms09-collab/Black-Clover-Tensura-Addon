package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.Roster;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The mutated dungeon guards of the collapse (docs/living_dungeon.md section 8). At each guard wave a few real creatures of the Dungeon's
 * roster (tier 2, later some tier 3, see {@link GuardPlan}) step out of a ring of portal light at the exits: grown by attribute modifiers
 * by a finite amount, named "Mutated ...", outlined, and sent at the nearest player. They are capped at every level (alive at once, at one
 * exit, in all), never appear within five blocks of a player, never target creative or spectating players, and all of them are removed
 * when the collapse ends. Each carries the collapse's number in its persistent data, so a restart finds them again and a guard that
 * outlives its collapse is removed the moment its chunk loads.
 */
final class CollapseGuards {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The persistent-data key of a guard: the number of its collapse. */
    static final String KEY = "nusmp_collapse_guard";
    /** What its hunger is held at: above hungry (40), so the Dungeon's food web does not send it hunting or let it starve, and below full (80), so a tier 3 guard is never put to sleep. */
    private static final int HUNGER = 60;
    private static boolean rosterWarned;
    private static final double SIGHT = 40.0, SIGHT_SQ = SIGHT * SIGHT;
    private static final int AIM_EVERY = 10, TELEGRAPH_EVERY = 8, WAITS = 3, SWEEP_RADIUS = 64, SWEEP_HEIGHT = 24;

    private final ServerLevel level;
    private final CollapseState s;
    /** The guards of this collapse that are alive and loaded, with the exit each was made for (-1 for one found again after a restart). */
    private final Map<UUID, Integer> alive = new LinkedHashMap<>();
    private final List<Pending> pending = new ArrayList<>();

    /** A guard that has been announced by its ring of light and will stand there at `due`. */
    private record Pending(int exit, int x, int y, int z, long due, int tier, int waits) {}

    CollapseGuards(ServerLevel level, CollapseState s) {
        this.level = level;
        this.s = s;
    }

    int aliveCount() { return alive.size(); }

    int pendingCount() { return pending.size(); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    // ---------------------------------------------------------------- a wave
    /** Wave `index` is due: plan its guards (each announced by a ring of light before it stands there), up to every cap. */
    void wave(int index, long now, List<ServerPlayer> inside) {
        prune();
        int exits = s.exits.size();
        int[] atExit = new int[exits];
        for (int e : alive.values()) if (e >= 0 && e < exits) atExit[e]++;
        for (Pending p : pending) if (p.exit() < exits) atExit[p.exit()]++;
        int[] plan = GuardPlan.assign(index, exits, atExit, alive.size() + pending.size(), s.spawned + pending.size());
        if (plan.length == 0) return;
        RandomSource rnd = level.getRandom();
        int planned = 0;
        for (int k = 0; k < plan.length; k++) {
            int[] spot = spotFor(plan[k], s.spawned + pending.size() + k);
            if (spot == null) continue;
            pending.add(new Pending(plan[k], spot[0], spot[1], spot[2], now + GuardPlan.TELEGRAPH, GuardPlan.tier(index, rnd.nextInt(100)), 0));
            telegraph(spot[0], spot[1], spot[2]);
            planned++;
        }
        if (planned > 0) {
            int[] e0 = s.exits.get(0);
            level.playSound(null, new BlockPos(e0[0], e0[1], e0[2]), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.2f, 0.5f);
            for (ServerPlayer p : inside) CollapseUi.chat(p, CollapseText.GUARDS, ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
        }
    }

    /** A standing place on the ring round an exit, at least MIN_PLAYER_GAP from every player; null if four tries find none. */
    private int[] spotFor(int exit, int n) {
        int[] e = s.exits.get(exit);
        for (int attempt = 0; attempt < 4; attempt++) {
            int[] off = GuardPlan.offset(exit, n, attempt);
            int[] spot = CollapseGround.standingNear(level, e[0] + off[0], e[1], e[2] + off[1], 3, 4);
            if (spot != null && farFromPlayers(spot[0], spot[1], spot[2])) return spot;
        }
        return null;
    }

    private boolean farFromPlayers(int x, int y, int z) {
        double gap2 = GuardPlan.MIN_PLAYER_GAP * GuardPlan.MIN_PLAYER_GAP;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            double dx = p.getX() - (x + 0.5), dy = p.getY() - y, dz = p.getZ() - (z + 0.5);
            if (dx * dx + dy * dy + dz * dz < gap2) return false;
        }
        return true;
    }

    private void telegraph(int x, int y, int z) {
        level.sendParticles(ParticleTypes.PORTAL, x + 0.5, y + 1.0, z + 0.5, 24, 0.5, 0.9, 0.5, 0.35);
    }

    // ---------------------------------------------------------------- every tick
    void tick(long now) {
        if (!pending.isEmpty()) {
            for (Pending p : new ArrayList<>(pending)) {
                if (p.due() > now) {
                    if ((now + p.x()) % TELEGRAPH_EVERY == 0) telegraph(p.x(), p.y(), p.z());
                    continue;
                }
                pending.remove(p);
                stand(p, now);
            }
        }
        if (!alive.isEmpty() && (now + s.id) % AIM_EVERY == 0) aim();
    }

    /** The ring of light has burned its time: the guard steps out, unless a player has walked up to the spot (it waits a little, then gives up). */
    private void stand(Pending p, long now) {
        if (!CollapseGround.standable(level, p.x(), p.y(), p.z()) || !farFromPlayers(p.x(), p.y(), p.z())) {
            if (p.waits() + 1 < WAITS) pending.add(new Pending(p.exit(), p.x(), p.y(), p.z(), now + 20, p.tier(), p.waits() + 1));
            return;
        }
        if (s.spawned >= GuardPlan.MAX_SPAWNED || alive.size() >= GuardPlan.MAX_ALIVE) return;
        RandomSource rnd = level.getRandom();
        Roster roster = Wildlife.roster();
        Roster.Entry e = roster.pick(p.tier(), Roster.CITY, false, rnd.nextDouble());
        int tier = p.tier();
        if (e == null) {                                                                    // the roster has nothing of that tier: the other one serves
            tier = p.tier() == 3 ? 2 : 3;
            e = roster.pick(tier, Roster.CITY, false, rnd.nextDouble());
        }
        if (e == null) {
            if (!rosterWarned) {
                rosterWarned = true;
                LOG.warn("[nusmp] The roster has no creature of tier 2 or 3 (is Tensura installed?): the collapse brings no guards");
            }
            return;
        }
        Mob m = WildSpawn.spawnId(level, e.id(), new BlockPos(p.x(), p.y(), p.z()), rnd);
        if (m == null) return;
        try {
            mutate(m, tier);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not mutate a guard of the collapse", failure);
        }
        m.getPersistentData().putLong(KEY, s.id);
        alive.put(m.getUUID(), p.exit());
        s.spawned++;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x() + 0.5, p.y() + 1.0, p.z() + 0.5, 30, 0.4, 0.8, 0.4, 0.2);
        level.playSound(null, new BlockPos(p.x(), p.y(), p.z()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    // ---------------------------------------------------------------- the mutation
    private void mutate(Mob m, int tier) {
        GuardPlan.Mutation mu = GuardPlan.mutation(tier);
        grow(m, Attributes.MAX_HEALTH, "collapse_health", mu.health(), mu.healthCap());
        grow(m, Attributes.ATTACK_DAMAGE, "collapse_attack", mu.attack(), mu.attackCap());
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(id("collapse_pace"));
            speed.addPermanentModifier(new AttributeModifier(id("collapse_pace"), mu.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        m.setHealth(m.getMaxHealth());
        m.setCustomName(Component.literal("Mutated ").append(m.getType().getDescription()));
        m.setCustomNameVisible(true);
        m.setGlowingTag(true);
        pinHunger(m);
    }

    private static void pinHunger(Mob m) {
        try {
            if (m.hasData(Dweller.DWELLER.get())) m.getData(Dweller.DWELLER).appetite.set(HUNGER);
        } catch (RuntimeException | LinkageError ignored) {
            // a creature the food web does not know has nothing to pin
        }
    }

    /** Adds to an attribute by the share, but never past the cap and never when it is already past it. */
    private static void grow(Mob m, Holder<Attribute> attribute, String name, double share, double cap) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null) return;
        a.removeModifier(id(name));
        double add = GuardPlan.extra(a.getValue(), share, cap);
        if (add > 0.0) a.addPermanentModifier(new AttributeModifier(id(name), add, AttributeModifier.Operation.ADD_VALUE));
    }

    // ---------------------------------------------------------------- after a restart, and the upkeep
    /** A guard of this collapse that was loaded from disk (or found again): it counts again. */
    void adopt(UUID id) {
        if (alive.size() < GuardPlan.MAX_ALIVE + 4) alive.putIfAbsent(id, -1);
    }

    private void prune() {
        for (Iterator<Map.Entry<UUID, Integer>> it = alive.entrySet().iterator(); it.hasNext(); ) {
            Entity en = level.getEntity(it.next().getKey());
            if (!(en instanceof Mob m) || !m.isAlive() || m.isRemoved()) it.remove();
        }
    }

    /** Every few ticks: the guards that have lost their target take the nearest player (bounded: a handful of guards, a handful of players). */
    private void aim() {
        prune();
        for (UUID id : alive.keySet()) {
            if (!(level.getEntity(id) instanceof Mob m)) continue;
            pinHunger(m);                                                                   // it falls by one every five seconds: held, it never gets hungry or full
            ServerPlayer target = nearest(m);
            if (target == null) continue;
            try {
                if (m.getTarget() != target) {
                    m.setTarget(target);
                    m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                }
                m.getNavigation().moveTo(target, 1.15);
            } catch (RuntimeException | LinkageError ignored) {
                // a creature of an unexpected shape just keeps its own mind
            }
        }
    }

    /** The nearest player the guard may go for: alive, in survival or adventure, within sight, and not a Mantle of the Scavenger wearer for a tier 3 guard (they are ignored by tier 3). */
    private ServerPlayer nearest(Mob m) {
        ServerPlayer best = null;
        double bestD = SIGHT_SQ;
        int tier = Wildlife.tierOf(m);
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            double d = m.distanceToSqr(p);
            if (d >= bestD) continue;
            if (tier >= 3 && Relics.wears(p, Relic.MANTLE_OF_THE_SCAVENGER)) continue;
            bestD = d;
            best = p;
        }
        return best;
    }

    // ---------------------------------------------------------------- the end
    /** Removes every guard of this collapse that can be reached: the ones kept track of, and any more found near the exits by their mark. */
    void dismiss() {
        pending.clear();
        for (UUID id : alive.keySet()) {
            Entity en = level.getEntity(id);
            if (en != null) en.discard();
        }
        alive.clear();
        for (int[] e : s.exits) {
            AABB box = new AABB(e[0] - SWEEP_RADIUS, e[1] - SWEEP_HEIGHT, e[2] - SWEEP_RADIUS, e[0] + SWEEP_RADIUS + 1, e[1] + SWEEP_HEIGHT, e[2] + SWEEP_RADIUS + 1);
            for (Mob m : level.getEntitiesOfClass(Mob.class, box, x -> x.getPersistentData().contains(KEY) && x.getPersistentData().getLong(KEY) == s.id)) m.discard();
        }
    }
}
