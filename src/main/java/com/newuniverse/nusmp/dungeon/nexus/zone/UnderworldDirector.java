package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.sentient.SentientMode;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.entity.avatar.AvatarEntities;
import com.newuniverse.nusmp.entity.avatar.DevilAvatarBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The creatures of the Underworld Gate: the floor boss, a Devil Avatar (Lucifero's or Megicula's, see {@link #populateAvatar}) on the way in front
 * of the Gate; a Gatekeeper (a greater daemon of Tensura, buffed and named) in the vestibule in front of the Treasury's gate; lesser daemons at
 * their posts on the floor, and Dark Disciples (the Zogratis family's followers, each channelling a share of Lucifero, Megicula or Beelzebub, the
 * share rising toward the Gate) that come back slowly, up to a cap, while players are in the hall. While the Sentient Dungeon is on, the lesser
 * daemons and the Disciples are not placed here and no Disciple comes back: the rooms of the Dungeon bring Type C's low-tier Devils and Disciples
 * out of their own walls ({@link UnderworldRules#staticPopulation}). The creatures are marked with scoreboard tags, so every one of them is
 * found again after a restart and removed when the Nexus collapses. Nothing here is saved by itself: the hall is rebuilt by populate, and
 * everything else is read off the live creatures. Server thread only.
 */
final class UnderworldDirector {
    private UnderworldDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    static final String TAG = "nusmp_underworld", TAG_KEEPER = "nusmp_underworld_keeper", TAG_DEVIL = "nusmp_underworld_devil", TAG_DISCIPLE = "nusmp_underworld_disciple",
            TAG_AVATAR = "nusmp_underworld_avatar";
    private static final ResourceLocation HEALTH = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_keeper_health"),
            DAMAGE = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_keeper_damage"),
            KNOCK = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_keeper_knockback"),
            ARMOUR = ResourceLocation.fromNamespaceAndPath("nusmp", "underworld_keeper_armour");

    /** The bar of each Gatekeeper that is alive, by its UUID. */
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    /** When the next Disciple may come, and how many have come, by cell. */
    private static final Map<Long, Long> NEXT_DISCIPLE = new HashMap<>();
    private static final Map<Long, Integer> COUNT = new HashMap<>();
    private static boolean sorry;

    // ---------------------------------------------------------------- finding them
    private static AABB box(ZonePlan plan) {
        return new AABB(plan.footprint.minX(), plan.floorY - 2, plan.footprint.minZ(), plan.footprint.maxX() + 1, plan.topY + 2, plan.footprint.maxZ() + 1);
    }

    /** The loaded creatures of the hall that carry this tag. */
    static List<LivingEntity> mine(ServerLevel level, ZonePlan plan, String tag) {
        return level.getEntitiesOfClass(LivingEntity.class, box(plan), e -> e.getTags().contains(tag));
    }

    // ---------------------------------------------------------------- making the hall's creatures
    /** Called once when the hall is built: the Gatekeeper, the Devils at their posts, the first Disciples. Whatever an earlier hall left is removed first. */
    static void populate(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        clear(level, plan);
        try {
            spawnKeeper(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Gatekeeper of the Underworld Gate of cell ({}, {}) could not be made", cellX, cellZ, failure);
        }
        if (!staticPopulation()) return;                                  // the Sentient Dungeon brings the Devils and Disciples out of the walls
        try {
            spawnDevils(level, plan);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The Devils of the Underworld Gate of cell ({}, {}) could not be made", cellX, cellZ, failure);
        }
        long key = NexusCells.key(cellX, cellZ);
        int want = UnderworldRules.initialDisciples(1);
        for (int i = 0; i < want; i++) {
            try {
                spawnDisciple(level, plan, cellX, cellZ, key);
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A Dark Disciple of the Underworld Gate of cell ({}, {}) could not be made", cellX, cellZ, failure);
                break;
            }
        }
    }

    /** Is the hall's own static population on (the Sentient Dungeon off, or its class not there)? */
    private static boolean staticPopulation() {
        boolean sentient = false;
        try {
            sentient = SentientMode.active();
        } catch (LinkageError | RuntimeException noSentientDungeon) { /* the Sentient Dungeon is not in this build: the hall is the hall of 1.21.0 */ }
        return UnderworldRules.staticPopulation(sentient);
    }

    /**
     * The floor boss: one Devil Avatar on the way in front of the Gate (the plan's avatar spot), facing the way in, its arena there and its
     * collapse centred on the middle of the hall, tagged like every creature of the hall so a rebuild or a collapse removes it. Never two:
     * if one of this hall is already standing, nothing is made. Called once when the hall is built, after {@link #populate}.
     */
    static void populateAvatar(ServerLevel level, ZonePlan plan, int cellX, int cellZ) {
        List<ZonePlan.Spot> spots = plan.spawns(UnderworldGatePlan.ROLE_AVATAR);
        if (spots.isEmpty() || !mine(level, plan, TAG_AVATAR).isEmpty()) return;
        ZonePlan.Spot s = spots.get(0), mid = plan.centre();
        float yaw = 0f;
        List<ZonePlan.Spot> in = plan.spawns(ZonePlan.ROLE_ARRIVAL);
        if (!in.isEmpty()) yaw = (float) Math.toDegrees(Math.atan2(-(in.get(0).x() - s.x()), in.get(0).z() - s.z()));
        DevilAvatarBoss boss = AvatarEntities.spawn(level, new Vec3(s.x() + 0.5, s.y(), s.z() + 0.5), yaw, UnderworldRules.avatarVariant(cellX, cellZ),
                new BlockPos(s.x(), s.y(), s.z()), new BlockPos(mid.x(), mid.y(), mid.z()), false);
        if (boss == null) {
            LOG.warn("[nusmp] The Devil Avatar of the Underworld Gate of cell ({}, {}) could not be made", cellX, cellZ);
            return;
        }
        boss.addTag(TAG);
        boss.addTag(TAG_AVATAR);
    }

    private static Mob spawnType(ServerLevel level, String id, ZonePlan.Spot at) {
        EntityType<?> type = Wildlife.typeOf(id);
        if (type == null) {
            if (!sorry) {
                sorry = true;
                LOG.warn("[nusmp] {} is not registered (Tensura is not installed?): the Underworld Gate has no Devils", id);
            }
            return null;
        }
        return WildSpawn.spawnType(level, type, new BlockPos(at.x(), at.y(), at.z()), level.getRandom(), MobSpawnType.EVENT, false);
    }

    private static void spawnKeeper(ServerLevel level, ZonePlan plan) {
        List<ZonePlan.Spot> spots = plan.spawns(UnderworldGatePlan.ROLE_GATEKEEPER);
        if (spots.isEmpty()) return;
        Mob keeper = spawnType(level, "tensura:greater_daemon", spots.get(0));
        if (keeper == null) return;
        keeper.addTag(TAG);
        keeper.addTag(TAG_KEEPER);
        keeper.setCustomName(Component.literal(UnderworldText.GATEKEEPER));
        keeper.setCustomNameVisible(true);
        buff(keeper, Attributes.MAX_HEALTH, HEALTH, UnderworldRules.KEEPER_HEALTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        buff(keeper, Attributes.ATTACK_DAMAGE, DAMAGE, UnderworldRules.KEEPER_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        buff(keeper, Attributes.KNOCKBACK_RESISTANCE, KNOCK, UnderworldRules.KEEPER_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE);
        buff(keeper, Attributes.ARMOR, ARMOUR, UnderworldRules.KEEPER_ARMOUR, AttributeModifier.Operation.ADD_VALUE);
        keeper.setHealth(keeper.getMaxHealth());
    }

    private static void buff(Mob m, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a != null) a.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void spawnDevils(ServerLevel level, ZonePlan plan) {
        int made = 0;
        for (ZonePlan.Spot post : plan.spawns(ZonePlan.ROLE_ELITE)) {
            if (made >= UnderworldRules.LESSER_DAEMONS) break;
            Mob devil = spawnType(level, "tensura:lesser_daemon", post);
            if (devil == null) continue;
            devil.addTag(TAG);
            devil.addTag(TAG_DEVIL);
            made++;
        }
    }

    /** One Dark Disciple at a spot well away from every player; false if none could be made. */
    private static boolean spawnDisciple(ServerLevel level, ZonePlan plan, int cellX, int cellZ, long key) {
        List<ZonePlan.Spot> all = plan.spawns(ZonePlan.ROLE_MINION), loaded = new ArrayList<>();
        for (ZonePlan.Spot s : all) if (level.getChunkSource().getChunkNow(s.x() >> 4, s.z() >> 4) != null) loaded.add(s);
        if (loaded.isEmpty()) return false;
        double[][] spots = new double[loaded.size()][2];
        for (int i = 0; i < spots.length; i++) {
            spots[i][0] = loaded.get(i).x() + 0.5;
            spots[i][1] = loaded.get(i).z() + 0.5;
        }
        List<double[]> who = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator()) who.add(new double[]{p.getX(), p.getZ()});
        int n = COUNT.getOrDefault(key, 0);
        int pick = UnderworldRules.pickSpot(spots, who.toArray(new double[0][]), n * 7919L + level.getGameTime() / 20);
        if (pick < 0) return false;
        ZonePlan.Spot s = loaded.get(pick);
        float share = UnderworldRules.fraction(UnderworldGatePlan.depth(cellX, cellZ, s.x() + 0.5, s.z() + 0.5));
        Devil devil = UnderworldRules.channel(n);
        Entity made = UnderworldDisciples.spawn(level, new Vec3(s.x() + 0.5, s.y(), s.z() + 0.5), devil, share);
        if (made == null) return false;
        made.addTag(TAG);
        made.addTag(TAG_DISCIPLE);
        if (made instanceof Mob m) m.setPersistenceRequired();
        COUNT.put(key, n + 1);
        return true;
    }

    // ---------------------------------------------------------------- once a second while players are in the hall
    /** Keeps the Gatekeeper at its post and its bar up, tops up the Devils' hunger, brings a Disciple back now and then. */
    static void tick(ServerLevel level, ZonePlan plan, int cellX, int cellZ, List<ServerPlayer> inside) {
        long key = NexusCells.key(cellX, cellZ), now = level.getGameTime();
        List<LivingEntity> keepers = new ArrayList<>(), devils = new ArrayList<>();
        int disciples = 0;
        for (LivingEntity e : mine(level, plan, TAG)) {                  // one look at the hall, sorted by tag
            if (e.getTags().contains(TAG_KEEPER)) keepers.add(e);
            else if (e.getTags().contains(TAG_DEVIL)) devils.add(e);
            else if (e.getTags().contains(TAG_DISCIPLE)) {
                disciples++;
                feed(e);                                                 // a lesser daemon standing in for a Disciple is of the food web too
            }
        }
        for (LivingEntity keeper : keepers) {
            if (keeper instanceof Mob m && m.isAlive()) holdPost(plan, m);
            watchBar(level, keeper);
        }
        for (Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator(); it.hasNext(); ) {         // a Gatekeeper that fell (or was removed) takes its bar with it
            Map.Entry<UUID, ServerBossEvent> e = it.next();
            Entity gone = level.getEntity(e.getKey());
            if (gone == null || !gone.isAlive()) {
                e.getValue().removeAllPlayers();
                it.remove();
            }
        }
        for (LivingEntity devil : devils) feed(devil);
        if (!staticPopulation()) {                                        // the rooms of the Dungeon bring the Disciples, not the hall
            NEXT_DISCIPLE.remove(key);
            return;
        }
        int cap = UnderworldRules.discipleCap(inside.size());
        int delay = UnderworldRules.respawnDelay(disciples, cap);
        if (delay < 0) {
            NEXT_DISCIPLE.remove(key);                                    // full: the clock starts again when a Disciple falls
            return;
        }
        Long due = NEXT_DISCIPLE.get(key);
        if (due == null) {
            NEXT_DISCIPLE.put(key, now + delay);
        } else if (now >= due) {
            NEXT_DISCIPLE.put(key, now + delay);
            try {
                spawnDisciple(level, plan, cellX, cellZ, key);
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] A Dark Disciple of the Underworld Gate of cell ({}, {}) could not be made", cellX, cellZ, failure);
            }
        }
    }

    /** The Gatekeeper walks back to its post if it has wandered off without a target, and is put there if it cannot find the way. */
    private static void holdPost(ZonePlan plan, Mob keeper) {
        List<ZonePlan.Spot> spots = plan.spawns(UnderworldGatePlan.ROLE_GATEKEEPER);
        if (spots.isEmpty()) return;
        ZonePlan.Spot post = spots.get(0);
        double dx = keeper.getX() - (post.x() + 0.5), dy = keeper.getY() - post.y(), dz = keeper.getZ() - (post.z() + 0.5);
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (!UnderworldRules.shouldReturn(dist, keeper.getTarget() != null)) return;
        if (dist > 40.0 || Math.abs(dy) > 12.0) keeper.moveTo(post.x() + 0.5, post.y(), post.z() + 0.5, keeper.getYRot(), 0f);
        else keeper.getNavigation().moveTo(post.x() + 0.5, post.y(), post.z() + 0.5, 1.0);
    }

    /** A Devil of lesser rank is a member of the Dungeon's food web: it is kept neither hungry (it would hunt) nor full (it would sleep), and never starves. */
    private static void feed(LivingEntity devil) {
        if (!(devil instanceof Mob m) || !m.hasData(Dweller.DWELLER.get())) return;
        Dweller w = m.getData(Dweller.DWELLER);
        if (UnderworldRules.needsFeeding(w.appetite.hunger())) w.appetite.set(UnderworldRules.KEEP_FULL);
        if (w.asleep) {
            w.asleep = false;
            m.setNoAi(false);
        }
    }

    /** The Gatekeeper's bar: shown to the players near it, with its health. */
    private static void watchBar(ServerLevel level, LivingEntity keeper) {
        ServerBossEvent bar = BARS.get(keeper.getUUID());
        if (!keeper.isAlive()) {
            if (bar != null) {
                bar.removeAllPlayers();
                BARS.remove(keeper.getUUID());
            }
            return;
        }
        if (bar == null) {
            bar = new ServerBossEvent(Component.literal(UnderworldText.GATEKEEPER), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(keeper.getUUID(), bar);
        }
        bar.setProgress(Math.max(0f, Math.min(1f, keeper.getHealth() / keeper.getMaxHealth())));
        if (keeper instanceof Mob km && km.getTarget() != null && level.getGameTime() % 40 < 20)          // 1.35.0: a fighting Gatekeeper wears the ring of void glyphs
            com.newuniverse.nusmp.entity.ZagredFx.ring(level, keeper.position(), 5f, 24, 0xFF8A4CFF);
        double range2 = UnderworldRules.BAR_RANGE * UnderworldRules.BAR_RANGE;
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(keeper) <= range2) bar.addPlayer(p);
            else bar.removePlayer(p);
        }
    }

    // ---------------------------------------------------------------- bars
    /** Is any Gatekeeper's bar up? */
    static boolean hasBars() { return !BARS.isEmpty(); }

    /**
     * Keeps the bars honest between the hall's once-a-second looks, which stop when nobody is inside: a player who has left the hall, the Dungeon
     * or the game is taken off a bar (it would stay on their screen), and every player is when the Gatekeeper is gone or its chunk is unloaded.
     */
    static void sweepBars(ServerLevel level) {
        double range2 = UnderworldRules.BAR_RANGE * UnderworldRules.BAR_RANGE;
        for (Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ServerBossEvent> entry = it.next();
            Entity keeper = level.getEntity(entry.getKey());
            ServerBossEvent bar = entry.getValue();
            if (keeper == null || !keeper.isAlive()) {
                bar.removeAllPlayers();
                it.remove();
                continue;
            }
            for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) if (p.isRemoved() || p.level() != level || p.distanceToSqr(keeper) > range2) bar.removePlayer(p);
        }
    }

    // ---------------------------------------------------------------- the end
    /** Removes every creature the hall made (the Nexus collapsed, or is being made again) and puts their bars away. */
    static void clear(ServerLevel level, ZonePlan plan) {
        for (LivingEntity e : mine(level, plan, TAG)) {
            if (e.getTags().contains(TAG_AVATAR) && e.isDeadOrDying()) continue;       // its own fall started this collapse: the body stays for its death throes
            ServerBossEvent bar = BARS.remove(e.getUUID());
            if (bar != null) bar.removeAllPlayers();
            e.discard();
        }
        long key = NexusCells.key(plan.cellX, plan.cellZ);
        NEXT_DISCIPLE.remove(key);
        COUNT.remove(key);
    }

    /** Forgets everything held about a world (it is closing): the bars and the clocks. */
    static void forgetAll() {
        for (Iterator<ServerBossEvent> it = BARS.values().iterator(); it.hasNext(); ) {
            it.next().removeAllPlayers();
            it.remove();
        }
        NEXT_DISCIPLE.clear();
        COUNT.clear();
        sorry = false;
    }
}
