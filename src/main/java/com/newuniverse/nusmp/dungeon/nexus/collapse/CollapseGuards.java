package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.sentient.FloorKind;
import com.newuniverse.nusmp.dungeon.sentient.FloorRoster;
import com.newuniverse.nusmp.dungeon.sentient.WallBirth;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.Roster;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The frenzied horde of the collapse (docs/living_dungeon.md section 8). At each wave the Dungeon births creatures out of its own walls:
 * round the ways out and round the player who is farthest from all of them ({@link GuardPlan#assign}), the wall cracking for two seconds
 * first (the sentient package's {@link WallBirth}, which plays the picture and adds what the factory made; it changes no block; the shares
 * of a wave are handed to it one a tick). The creatures are those of the floor's own spawn list ({@link FloorRoster}), or, where a floor has
 * none (the Monarch's Vault, the Mirage, the Golden City), real creatures of the Dungeon's roster, tier 2 or 3, as the guards of 1.21.0
 * were. Each is frenzied: quicker, harder hitting, glowing, named "Frenzied ...", and goes for any player inside the collapsing ground, at
 * any distance.
 *
 * <p>They bypass the room caps of the sentient Dungeon (they do not carry its birth tag, so its driver never steers them) and keep a hard
 * ceiling of their own at every level, held in the {@link HordeLedger}: alive at once, near one exit or one player, born in all. They are
 * never put within five blocks of a player, never go for creative or spectating players, a wave with nobody inside to go for is not sent,
 * a creature that is not placed is not counted, and all of them are removed when the collapse ends. Each carries the collapse's number in
 * its persistent data, so a restart finds them again and one that outlives its collapse is removed the moment its chunk loads. Where no
 * wall can be found for a birth, a ring of portal light at the exit brings the creature as the 1.21.0 guards stepped out.
 */
final class CollapseGuards {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();
    /** The persistent-data key of a creature of the horde: the number of its collapse. */
    static final String KEY = "nusmp_collapse_guard";
    /** Its scoreboard tag. (Not the sentient Dungeon's birth tag: the driver of the room births must not steer it.) */
    static final String TAG = "nusmp_collapse_horde";
    /** What its hunger is held at: above hungry (40), so the Dungeon's food web does not send it hunting or let it starve, and below full (80), so a tier 3 creature is never put to sleep. */
    private static final int HUNGER = 60;
    private static final int AIM_EVERY = 10, TELEGRAPH_EVERY = 8, WAITS = 3, SWEEP_RADIUS = 64, SWEEP_HEIGHT = 24, STRIKE_EVERY = 40, SLACK = 100;

    private final ServerLevel level;
    private final CollapseState s;
    /** Who is alive, what is on its way, how many were made: the books the ceilings are held in. */
    private final HordeLedger book;
    /** When each creature may strike next, and the kinds whose attack threw (they are left to their own brains). */
    private final Map<UUID, Long> nextStrike = new HashMap<>();
    private final Set<EntityType<?>> cannotStrike = new HashSet<>();
    /** The kind of floor this collapse is on, read at each wave (null if it could not be). */
    private FloorKind kind;
    private boolean closed;

    CollapseGuards(ServerLevel level, CollapseState s) {
        this.level = level;
        this.s = s;
        this.book = new HordeLedger(s);
    }

    int aliveCount() { return book.alive(); }

    /** Creatures on the way: shares of a wave not yet handed to the walls, ring births waiting for their light, and wall births still cracking. */
    int pendingCount() { return book.coming(); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    private static void warnOnce(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] The collapse's horde could not {}; going on without it", what, failure);
    }

    // ---------------------------------------------------------------- a wave
    /** Wave `index` is due: plan its creatures (exits and the one player to follow), up to every ceiling, and queue them for the walls. */
    void wave(int index, long now, List<ServerPlayer> inside) {
        if (closed) return;
        prune(now);
        List<ServerPlayer> who = targets();
        if (who.isEmpty()) return;                                                          // nobody inside to go for: the wave is not sent
        int exits = s.exits.size();
        double[] px = new double[who.size()], pz = new double[who.size()];
        for (int i = 0; i < px.length; i++) {
            px[i] = who.get(i).getX();
            pz[i] = who.get(i).getZ();
        }
        int pick = GuardPlan.targetPlayer(index, px, pz, s.exitXs(), s.exitZs());
        ServerPlayer follow = pick >= 0 ? who.get(pick) : null;
        int coming = book.coming();
        int near = follow == null ? 0 : nearCount(follow) + book.comingPlayer();
        int[] plan = GuardPlan.assign(index, exits, book.load(exits), follow != null, near, book.alive() + coming, s.spawned + coming);
        if (plan.length == 0) return;
        int[] perExit = new int[exits];
        int perPlayer = 0;
        for (int slot : plan) {
            if (slot == GuardPlan.PLAYER) perPlayer++;
            else if (slot >= 0 && slot < exits) perExit[slot]++;
        }
        kind = floorKind();
        for (int e = 0; e < exits; e++) {
            if (perExit[e] == 0) continue;
            int[] at = s.exits.get(e);
            book.queue(new HordeLedger.Job(e, at[0], at[1], at[2], perExit[e], index));
        }
        if (perPlayer > 0 && follow != null) book.queue(new HordeLedger.Job(GuardPlan.PLAYER, follow.getBlockX(), follow.getBlockY(), follow.getBlockZ(), perPlayer, index));
        BlockPos where = exits > 0 ? new BlockPos(s.exits.get(0)[0], s.exits.get(0)[1], s.exits.get(0)[2]) : follow == null ? null : follow.blockPosition();
        if (where != null) level.playSound(null, where, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.2f, 0.5f);
        for (ServerPlayer p : inside) CollapseUi.chat(p, CollapseText.GUARDS, ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
    }

    /** The floor's kind at the middle of the collapse; null if it cannot be told (the fallback roster serves). */
    private FloorKind floorKind() {
        try {
            return FloorKind.at(level, new BlockPos(s.centreX, s.centreY, s.centreZ));
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("tell which floor it is on", failure);
            return null;
        }
    }

    /**
     * Hands `k` births of one slot to the walls round `around`; what the walls cannot give (no loaded wall with a place to stand beside it, or
     * no wall birth at all) comes from a ring of portal light instead.
     */
    private void bear(int slot, BlockPos around, int k, int wave, long now) {
        boolean player = slot == GuardPlan.PLAYER;
        HordeLedger.Batch batch = new HordeLedger.Batch(slot, now + GuardPlan.TELEGRAPH + SLACK);
        int got = 0;
        try {
            got = WallBirth.birth(level, around, k, player ? GuardPlan.PLAYER_MIN : GuardPlan.EXIT_MIN, player ? GuardPlan.PLAYER_MAX : GuardPlan.EXIT_MAX,
                    GuardPlan.TELEGRAPH, (lvl, spot) -> make(lvl, spot, GuardPlan.tier(wave, lvl.getRandom().nextInt(100))), m -> claim(m, batch));
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("birth creatures from the walls", failure);
        }
        got = Math.max(0, Math.min(k, got));
        batch.left += got;
        if (got > 0) book.addBatch(batch);
        if (k > got) ring(slot, around, k - got, wave, now);
    }

    /** A wall birth has come out: it is counted and frenzied, unless a ceiling is reached by now or the collapse is over (then it is removed again). */
    private void claim(Mob m, HordeLedger.Batch batch) {
        batch.left--;
        register(m, batch.slot);
    }

    /** Counts a creature the collapse has just got into the world and makes it frenzied: false (and the creature gone) if a ceiling is reached or the collapse is over. */
    private boolean register(Mob m, int slot) {
        if (closed || !book.admit(m.getUUID(), slot)) {
            m.discard();
            return false;
        }
        try {
            frenzy(m);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("make a creature frenzied", failure);
        }
        m.getPersistentData().putLong(KEY, s.id);
        m.addTag(TAG);
        m.setPersistenceRequired();
        return true;
    }

    /** How many of the horde are within NEAR_PLAYER blocks of this player. */
    private int nearCount(ServerPlayer p) {
        int n = 0;
        double r2 = GuardPlan.NEAR_PLAYER * GuardPlan.NEAR_PLAYER;
        for (UUID u : book.ids()) if (level.getEntity(u) instanceof Mob m && m.distanceToSqr(p) <= r2) n++;
        return n;
    }

    // ---------------------------------------------------------------- making one
    /** The factory of the walls: a creature of the floor's own spawn list, else of the roster; positioned and not yet added. Null (nothing is born) if it would crowd a player or a ceiling is reached. */
    private Mob make(ServerLevel lvl, BlockPos spot, int tier) {
        if (closed || book.full() || !farFromPlayers(spot.getX(), spot.getY(), spot.getZ())) return null;
        RandomSource rnd = lvl.getRandom();
        Mob m = null;
        if (kind != null) {
            try {
                m = FloorRoster.create(lvl, kind, rnd, spot);
            } catch (RuntimeException | LinkageError failure) {
                warnOnce("take a creature from the floor's spawn list", failure);
            }
        }
        return m != null ? m : rosterMob(lvl, tier, spot, rnd);
    }

    /** A real creature of the Dungeon's roster of that tier (the other tier if it has none), as the guards of 1.21.0 were, made but not added. */
    private Mob rosterMob(ServerLevel lvl, int tier, BlockPos spot, RandomSource rnd) {
        Roster roster = Wildlife.roster();
        int place = DungeonTier.at(spot.getX(), spot.getY(), spot.getZ()).ordinal();
        Roster.Entry e = roster.pick(tier, place, false, rnd.nextDouble());
        if (e == null) e = roster.pick(tier == 3 ? 2 : 3, place, false, rnd.nextDouble());          // the roster has nothing of that tier: the other one serves
        if (e == null) {
            if (WARNED.add("no roster")) LOG.warn("[nusmp] The roster has no creature of tier 2 or 3 (is Tensura installed?): the collapse brings no horde where its floor has no spawn list");
            return null;
        }
        EntityType<?> type = Wildlife.typeOf(e.id());
        return type == null ? null : unadded(lvl, type, spot, rnd);
    }

    /** Creates, places and finalizes a creature the way the Dungeon's own births do (an event spawn, so its spawn hook leaves it alone), without adding it. */
    private static Mob unadded(ServerLevel lvl, EntityType<?> type, BlockPos at, RandomSource rnd) {
        try {
            Entity made = type.create(lvl);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);       // (room is checked where it is placed: WallBirth shifts a creature off its wall, the ring checks its own)
            m.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            return m;
        } catch (RuntimeException | LinkageError failure) {
            warnOnce("make a " + Wildlife.idOf(type), failure);
            return null;
        }
    }

    // ---------------------------------------------------------------- the ring of light (when no wall can birth one)
    /** Announces `n` creatures on the ring round `around` (an exit, or a player), each by a ring of portal light; one that finds no place is not made. */
    private void ring(int slot, BlockPos around, int n, int wave, long now) {
        for (int k = 0; k < n; k++) {
            int[] spot = ringSpot(slot, around, s.spawned + book.coming() + k);
            if (spot == null) continue;
            book.addRing(new HordeLedger.Ring(slot, spot[0], spot[1], spot[2], now + GuardPlan.TELEGRAPH, GuardPlan.tier(wave, level.getRandom().nextInt(100)), 0));
            telegraph(spot[0], spot[1], spot[2]);
        }
    }

    /** A standing place on the ring round `around`, at least MIN_PLAYER_GAP from every player; null if four tries find none. */
    private int[] ringSpot(int slot, BlockPos around, int n) {
        int push = slot == GuardPlan.PLAYER ? GuardPlan.PLAYER_RING : 0;
        for (int attempt = 0; attempt < 4; attempt++) {
            int[] off = GuardPlan.offset(slot, n, attempt);
            int[] spot = CollapseGround.standingNear(level, around.getX() + off[0] + Integer.signum(off[0]) * push, around.getY(), around.getZ() + off[1] + Integer.signum(off[1]) * push, 3, 4);
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
        HordeLedger.Job job = book.nextJob();                                                    // one share of a wave a tick
        if (job != null && !closed) bear(job.slot(), new BlockPos(job.x(), job.y(), job.z()), job.k(), job.wave(), now);
        book.expire(now);                                                                        // births that never came out are forgotten
        for (HordeLedger.Ring p : book.rings()) {
            if (p.due() > now) {
                if ((now + p.x()) % TELEGRAPH_EVERY == 0) telegraph(p.x(), p.y(), p.z());
                continue;
            }
            book.removeRing(p);
            stand(p, now);
        }
        if (book.alive() > 0 && (now + s.id) % AIM_EVERY == 0) aim(now);
    }

    /** The ring of light has burned its time: the creature steps out, unless a player has walked up to the spot (it waits a little, then gives up). */
    private void stand(HordeLedger.Ring p, long now) {
        if (closed) return;
        if (!CollapseGround.standable(level, p.x(), p.y(), p.z()) || !farFromPlayers(p.x(), p.y(), p.z())) {
            if (p.waits() + 1 < WAITS) book.addRing(new HordeLedger.Ring(p.slot(), p.x(), p.y(), p.z(), now + 20, p.tier(), p.waits() + 1));
            return;
        }
        BlockPos at = new BlockPos(p.x(), p.y(), p.z());
        Mob m = make(level, at, p.tier());
        if (m == null) return;
        if (!level.noCollision(m.getBoundingBox()) || !level.addFreshEntity(m)) {                // no room, or refused: it is not placed, so it is not counted
            m.discard();
            return;
        }
        if (!register(m, p.slot())) return;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x() + 0.5, p.y() + 1.0, p.z() + 0.5, 30, 0.4, 0.8, 0.4, 0.2);
        level.playSound(null, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    // ---------------------------------------------------------------- the frenzy
    /** Speed and attack grown by a finite share, sight lengthened so it can find a player across the hall, outlined and named. The modifiers live and die with the creature. */
    private void frenzy(Mob m) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(id("collapse_frenzy_speed"), GuardPlan.FRENZY_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        grow(m, Attributes.ATTACK_DAMAGE, "collapse_frenzy_attack", GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP);
        AttributeInstance sight = m.getAttribute(Attributes.FOLLOW_RANGE);
        if (sight != null) sight.addOrReplacePermanentModifier(new AttributeModifier(id("collapse_frenzy_sight"), GuardPlan.FRENZY_SIGHT, AttributeModifier.Operation.ADD_VALUE));
        Component base = m.hasCustomName() ? m.getCustomName() : m.getType().getDescription();       // an Aqua Slime stays an Aqua Slime
        m.setCustomName(Component.literal("Frenzied ").append(base));
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
    /** A creature of this collapse that was loaded from disk (or found again): it counts again. */
    void adopt(UUID id) { book.adopt(id); }

    private void prune(long now) {
        book.retain(u -> level.getEntity(u) instanceof Mob m && m.isAlive() && !m.isRemoved());
        book.expire(now);
    }

    /** Every few ticks: each creature goes for the nearest player inside the collapsing ground, at any distance (bounded: two dozen creatures, a handful of players). */
    private void aim(long now) {
        prune(now);
        if (book.alive() == 0) return;
        List<ServerPlayer> who = targets();
        for (UUID u : book.ids()) {
            if (!(level.getEntity(u) instanceof Mob m)) continue;
            pinHunger(m);                                                                        // it falls by one every five seconds: held, it never gets hungry or full
            ServerPlayer target = nearest(m, who);
            if (target == null) continue;
            try {
                if (m.getTarget() != target) {
                    m.setTarget(target);
                    m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                }
                if (m.getNavigation().isDone() || Math.floorMod(m.getId() + now / AIM_EVERY, 2L) == 0L) m.getNavigation().moveTo(target, 1.15);     // a new path every other look
                strike(m, target, now);
            } catch (RuntimeException | LinkageError ignored) {
                // a creature of an unexpected shape just keeps its own mind
            }
        }
        nextStrike.keySet().removeIf(u -> !book.has(u));
    }

    /** Strikes once in two seconds when in reach, unless the creature's own brain has just done so (as WildDriver and the Monarch's driver do). */
    private void strike(Mob m, ServerPlayer target, long now) {
        if (now < nextStrike.getOrDefault(m.getUUID(), 0L) || cannotStrike.contains(m.getType())) return;
        if (!m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(target.getBoundingBox())) return;
        nextStrike.put(m.getUUID(), now + STRIKE_EVERY);
        if (target.getLastHurtByMob() == m && target.tickCount - target.getLastHurtByMobTimestamp() < 30) return;
        m.swing(InteractionHand.MAIN_HAND);
        try {
            m.doHurtTarget(target);
        } catch (RuntimeException | LinkageError failure) {
            cannotStrike.add(m.getType());
            LOG.warn("[nusmp] {} could not strike for the collapse; its own brain will have to", m.getType().getDescriptionId(), failure);
        }
    }

    /** The players the horde may go for: alive, in survival or adventure, inside the collapsing ground. */
    private List<ServerPlayer> targets() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            boolean in = CollapseRules.inside(s.fp, s.floorY, s.topY, p.getBlockX(), p.getBlockY(), p.getBlockZ());
            if (CollapseRules.isTarget(p.isAlive(), p.isSpectator(), p.isCreative(), in)) out.add(p);
        }
        return out;
    }

    /** The nearest of them, however far (a tier 3 creature leaves a wearer of the Mantle of the Scavenger alone). */
    private static ServerPlayer nearest(Mob m, List<ServerPlayer> who) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        int tier = Wildlife.tierOf(m);
        for (ServerPlayer p : who) {
            double d = m.distanceToSqr(p);
            if (d >= bestD) continue;
            if (tier >= 3 && Relics.wears(p, Relic.MANTLE_OF_THE_SCAVENGER)) continue;
            bestD = d;
            best = p;
        }
        return best;
    }

    // ---------------------------------------------------------------- the end
    /** Removes every creature of this collapse that can be reached: the ones kept track of, and any more found by their mark near the exits and over the ground. Nothing is born after this. */
    void dismiss() {
        closed = true;
        book.closeBirths();
        for (UUID u : book.ids()) {
            Entity en = level.getEntity(u);
            if (en != null) en.discard();
        }
        book.clear();
        for (int[] e : s.exits) sweep(new AABB(e[0] - SWEEP_RADIUS, e[1] - SWEEP_HEIGHT, e[2] - SWEEP_RADIUS, e[0] + SWEEP_RADIUS + 1, e[1] + SWEEP_HEIGHT, e[2] + SWEEP_RADIUS + 1));
        sweep(new AABB(s.fp.minX() - 8, s.floorY - 8, s.fp.minZ() - 8, s.fp.maxX() + 9, s.topY + 16, s.fp.maxZ() + 9));
    }

    private void sweep(AABB box) {
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, x -> x.getPersistentData().contains(KEY) && x.getPersistentData().getLong(KEY) == s.id)) m.discard();
    }
}
