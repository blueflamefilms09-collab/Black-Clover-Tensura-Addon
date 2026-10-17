package com.newuniverse.nusmp.arc.world;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererDirector;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianAltarBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.seal.PeakAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: every tenth floor of the Mega-Dungeon ends in a hall with an altar that calls the Labyrinth Guardian, and the owner wants the characters of the arcs to be
 * met THERE too, not only in an arena you are carried to. This class is the glue between the altar and the pure choice ({@link FloorBossChoice}): when the altar is
 * about to call its Guardian it first asks {@link #host}; if the floor's coin says so and the roster has a boss that may stand on this floor and road, fits the
 * sector's theme and is open to the party that stands there, that boss is made in the Guardian's place and the altar waits for ITS death instead. The plain
 * Guardian stays the answer whenever anything is missing or goes wrong (the safe fallback that always works), and an exception never reaches the altar.
 * The hosted boss keeps the Guardian's whole contract so that nothing downstream can tell the difference: it carries the three tags the Guardian carries and the
 * persistent sector id (so the Metal Vessel of the Rukh flow and the guaranteed gear piece find it), it grows with the floor a little ({@link FloorScale}), it is
 * pulled back to its hall when it is led out of it, and when it dies the altar is told (so the Mana Core is forged and {@code MegaDungeon.guardianDefeated} runs
 * exactly as for the Guardian). A boss the Dungeon Master sends wandering has a second chore here: its arena (the ground its fight is anchored to) follows it, since
 * the fight framework pulls a boss back to the spot it was made at, and a wanderer is meant to walk away from it.
 */
public final class ArcFloorBosses {
    private ArcFloorBosses() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The tag of a boss that stands in a Guardian's place, and the persistent key that holds its altar's position. */
    public static final String TAG = "nusmp_arc_floor_boss", ANCHOR_KEY = "nusmp_arc_floor_anchor";
    /** How far from its altar a hosted boss may stray before it is carried back (the Guardian's own hall half width plus its slack). */
    public static final double HALL_RADIUS = 28.0;
    /** Blocks round the altar whose survival players count as the party for the story gate. */
    public static final double PARTY_RADIUS = 64.0;

    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "arc_floor_health"),
            DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "arc_floor_damage");

    /** Hosted bosses by entity UUID to their altar (packed position); only loaded ones, rebuilt from the tag when a chunk loads. Server thread only, cleared when it stops. */
    private static final Map<UUID, Long> HOSTED = new HashMap<>();
    /** Wandering bosses of the arcs (the framework bodies the Dungeon Master walks through its halls). */
    private static final Set<UUID> WANDERING = new LinkedHashSet<>();

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.LOWEST, ArcFloorBosses::onDeath);
        bus.addListener(ArcFloorBosses::onJoin);
        bus.addListener(ArcFloorBosses::onLeave);
        bus.addListener(ArcFloorBosses::onTick);
        bus.addListener(ArcFloorBosses::onStopped);
    }

    // ================================================================ the altar's question
    /**
     * Called by the Guardian Altar the moment it would call its Guardian. Returns the boss of the arcs that now stands in the hall (already in the level, tagged,
     * scaled), or null when the plain Guardian should be called. Never throws.
     */
    public static Entity host(ServerLevel sl, Sector sector, BlockPos altar) {
        try {
            MinecraftServer server = sl.getServer();
            if (server == null || sector == null || !ArcConfig.floorBosses()) return null;
            int floor = sector.lastFloor();
            List<ArcCharacter> standing = ArcRoster.floorBosses(floor, sector.polarity);
            if (standing.isEmpty()) return null;
            List<ServerPlayer> party = partyNear(sl, altar);
            FloorBossChoice.Request req = new FloorBossChoice.Request(MegaData.get(server).seed, sector.id, floor, sector.polarity,
                    sector.theme == null ? "" : sector.theme.id(), ArcConfig.floorBossChance(), true, standing, c -> ArcStory.gateOpen(party, c));
            ArcCharacter c = FloorBossChoice.choose(req);
            if (c == null) return null;
            Vec3 at = Vec3.atBottomCenterOf(altar.above());
            float yaw = sl.getRandom().nextFloat() * 360f;
            ZogratisBoss boss = make(sl, c, at, yaw, party);
            if (boss == null) return null;
            dress(boss, c, sector, floor, altar);
            HOSTED.put(boss.getUUID(), altar.asLong());
            LOG.info("[nusmp] {} stands in the Guardian's place on floor {} (sector {})", c.id(), floor, sector.id);
            return boss;
        } catch (RuntimeException e) {
            LOG.error("[nusmp] an arc boss could not take the Guardian's place; the Guardian is called instead: {}", e.toString());
            return null;
        }
    }

    private static List<ServerPlayer> partyNear(ServerLevel sl, BlockPos altar) {
        List<ServerPlayer> out = new ArrayList<>();
        double r2 = PARTY_RADIUS * PARTY_RADIUS;
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            if (p.distanceToSqr(altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5) <= r2) out.add(p);
        }
        return out;
    }

    /** Makes the boss through the door that fits it: the core's spawner for the arcs' own bosses, ours for the seven older ones. */
    static ZogratisBoss make(ServerLevel sl, ArcCharacter c, Vec3 at, float yaw, List<ServerPlayer> party) {
        if (ExistingBosses.owns(c.id())) return ExistingBossSpawner.make(sl, c, at, yaw, false).boss();
        ArcSpawner.Result r = ArcSpawner.summon(sl, c, at, yaw, party, true);
        return r.outcome() == ArcSpawner.Outcome.SPAWNED ? r.boss() : null;
    }

    /** The Guardian's contract: its tags and its sector, and the floor's growth. */
    private static void dress(ZogratisBoss boss, ArcCharacter c, Sector sector, int floor, BlockPos altar) {
        boss.addTag(PeakAltarBlockEntity.BOSS_TAG);
        boss.addTag(LabyrinthGuardianEntity.TAG);
        boss.addTag(LabyrinthGuardianEntity.SECTOR_TAG);
        boss.addTag(TAG);
        boss.getPersistentData().putInt(LabyrinthGuardianEntity.SECTOR_KEY, sector.id);
        boss.getPersistentData().putLong(ANCHOR_KEY, altar.asLong());
        double perFloor = ArcWorldConfig.floorScale();
        grow(boss, Attributes.MAX_HEALTH, HEALTH_ID, FloorScale.healthFactor(c, floor, perFloor));
        grow(boss, Attributes.ATTACK_DAMAGE, DAMAGE_ID, FloorScale.damageFactor(c, floor, perFloor));
        boss.setHealth(boss.getMaxHealth());                                         // a new boss starts at full health (the heal guard only blocks one that is fighting)
    }

    private static void grow(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double factor) {
        AttributeInstance a = e.getAttribute(attribute);
        if (a == null || !(factor > 1.0)) return;
        a.addOrReplacePermanentModifier(new AttributeModifier(id, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ================================================================ its death, told to the altar
    private static void onDeath(LivingDeathEvent e) {
        try {
            Entity dead = e.getEntity();
            if (!(dead.level() instanceof ServerLevel sl) || !dead.getTags().contains(TAG) || !dead.getPersistentData().contains(ANCHOR_KEY)) return;
            BlockPos anchor = BlockPos.of(dead.getPersistentData().getLong(ANCHOR_KEY));
            HOSTED.remove(dead.getUUID());
            if (sl.isLoaded(anchor) && sl.getBlockEntity(anchor) instanceof GuardianAltarBlockEntity altar) altar.onBossDied(dead.getUUID());
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the altar could not be told that its hosted boss died (it will see the body on its next poll): {}", ex.toString());
        }
    }

    // ================================================================ tracking, the leash, the wanderers' arena
    private static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ZogratisBoss boss)) return;
            if (boss.getTags().contains(TAG) && boss.getPersistentData().contains(ANCHOR_KEY)) HOSTED.put(boss.getUUID(), boss.getPersistentData().getLong(ANCHOR_KEY));
            if (boss.getTags().contains(WandererDirector.TAG)) WANDERING.add(boss.getUUID());
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] could not track a boss of the arcs: {}", ex.toString());
        }
    }

    private static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        UUID id = e.getEntity().getUUID();
        HOSTED.remove(id);
        WANDERING.remove(id);
    }

    private static void onStopped(ServerStoppedEvent e) {
        HOSTED.clear();
        WANDERING.clear();
    }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 0 || (HOSTED.isEmpty() && WANDERING.isEmpty())) return;
        try {
            ServerLevel level = MegaDungeon.level(server);
            if (level == null) return;
            for (Map.Entry<UUID, Long> h : new ArrayList<>(HOSTED.entrySet())) {
                if (!(level.getEntity(h.getKey()) instanceof ZogratisBoss boss) || !boss.isAlive()) continue;
                BlockPos altar = BlockPos.of(h.getValue());
                Vec3 home = Vec3.atBottomCenterOf(altar.above());
                double dx = boss.getX() - home.x, dz = boss.getZ() - home.z;
                if (dx * dx + dz * dz > HALL_RADIUS * HALL_RADIUS || Math.abs(boss.getY() - home.y) > 24.0) {
                    boss.teleportTo(home.x, home.y, home.z);                           // a boss led out of its hall is carried back, like the Guardian
                    boss.getNavigation().stop();
                }
            }
            for (UUID id : new ArrayList<>(WANDERING)) {
                if (level.getEntity(id) instanceof ZogratisBoss boss && boss.isAlive() && !boss.fightActive()) boss.setArena(boss.blockPosition());
            }
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the floor boss upkeep failed: {}", ex.toString());
        }
    }

    /** How many hosted bosses are tracked right now (the self-test and the status command). */
    public static int hostedCount() { return HOSTED.size(); }

    /** How many wandering bosses of the arcs are tracked right now. */
    public static int wanderingCount() { return WANDERING.size(); }
}
