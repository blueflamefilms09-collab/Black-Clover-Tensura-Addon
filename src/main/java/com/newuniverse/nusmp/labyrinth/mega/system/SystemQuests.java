package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is the System's rulebook in motion: the lifecycle of the floor quests, from the moment a player steps onto a floor to the moment the quest pays or
 * fails. The pure parts (which quest, how big, the state machine, the reward) are in {@link QuestRoller}, {@link QuestProgress} and {@link RewardRoller}; this
 * class is the glue that feeds them what the game sees. It is the {@link FloorListener} the floor tracker calls: on a NEW depth it finishes the old quest (a
 * counting quest still ACTIVE fails, a "reach the next floor" quest still ACTIVE succeeds), rolls the new one (never on a boss floor, never twice for the same
 * depth: {@code maxFloor} remembers the deepest, so walking back up and down cannot farm quests) and pops up the news. Between floors it is fed by bounded work
 * only: every five ticks of a player with an ACTIVE quest (the clock, and the room they stand in), and by the rare events (a kill, a hit). Rooms count after 40
 * ticks inside with no hostile alive in them; magic damage is recognised by {@link MagicDamage} and the tag {@code nusmp:mega_system_magic}; everything is
 * counted on the quest's own floor only, so wandering back down to rest cannot spoil a quest and cannot advance it. Server thread only; per-player scratch state
 * (the room timer, the last known floor) lives in maps that are cleared when the server stops.
 */
public final class SystemQuests implements FloorListener {
    private static final Logger LOG = LogUtils.getLogger();

    /** The one listener instance {@code SystemRegistry.init} registers with {@code MegaDungeon.addFloorListener}. */
    public static final SystemQuests LISTENER = new SystemQuests();

    /** Damage types a datapack adds to the ones the "no magic damage" quests recognise. */
    public static final TagKey<DamageType> MAGIC_TAG = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "mega_system_magic"));

    /** The entity tag the Dungeon Master puts on the creatures of its encounters (the ambush quest counts them). */
    public static final String AMBUSH_TAG = "nusmp_dm";

    /** Work is done every this many ticks per player. */
    static final int STEP = 5;

    private static final Map<UUID, RoomDwell> DWELL = new HashMap<>();
    private static final Map<UUID, Integer> LAST_FLOOR = new HashMap<>();

    private SystemQuests() {}

    // ================================================================ access
    public static SystemState state(ServerPlayer p) { return p.getData(SystemAttachments.STATE); }

    /** The last floor the floor tracker (or a sync) saw this player on; 0 when none. */
    public static int lastFloor(ServerPlayer p) { return LAST_FLOOR.getOrDefault(p.getUUID(), 0); }

    /** Does the System cover this road? Holy always; Abyssal only when the config says so. */
    public static boolean covers(Polarity pol) {
        return pol == Polarity.HOLY || (pol == Polarity.ABYSSAL && SystemConfig.get(SystemConfig.ABYSSAL_TOO));
    }

    public static boolean covers(int floor) { return covers(Polarity.ofFloor(floor)); }

    /**
     * The floor the player counts as standing on: the floor of the room they are in; in a doorway cap or the air beside the rooms (no room at all) the last
     * floor they entered, so a step through a cap never reads as "back on the first floor of the sector" (the floor tracker keeps the last floor in a gap in the
     * same way). A room that was never banded (a 1.54.0 save) answers with the first floor of its sector, as {@link MegaDungeon#floorAt} does.
     */
    public static int currentFloor(ServerLevel level, ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        Room r = MegaDungeon.roomAt(level, pos);
        if (r != null && r.floor != 0) return r.floor;
        if (r == null) {
            int last = lastFloor(p);
            if (last != 0) return last;
        }
        return MegaDungeon.floorAt(level, pos);
    }

    /** Is this damage magic: the datapack tag, or a damage type id the pure {@link MagicDamage} calls magic? */
    public static boolean isMagicDamage(DamageSource s) {
        if (s == null) return false;
        if (s.is(MAGIC_TAG)) return true;
        return s.typeHolder().unwrapKey().map(k -> MagicDamage.isMagicId(k.location().toString())).orElse(false);
    }

    // ================================================================ the floor
    @Override
    public void onFloorEntered(ServerPlayer player, int floor, int previous) {
        try {
            enter(player, floor, previous);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] System floor handler failed: {}", e.toString());
        }
    }

    /**
     * The player stepped onto {@code floor}. Finishes the old quest if this is the next floor, remembers the depth, and on a depth never reached before rolls
     * the new quest (unless it is a boss floor or the chance said no). Public for the self-test.
     */
    public static void enter(ServerPlayer p, int floor, int previous) {
        if (!SystemConfig.enabled() || floor == 0) return;
        LAST_FLOOR.put(p.getUUID(), floor);
        if (!covers(floor)) {
            SystemSync.request(p);
            return;
        }
        SystemState s = state(p);
        int depth = FloorMap.threat(floor);
        QuestProgress q = s.quest();
        if (q != null && QuestProgress.isNextFloor(q.floor(), floor)) apply(p, s, q, q.onNextFloor());
        if (depth > s.maxFloor()) {
            s.setMaxFloor(depth);
            SystemSync.toast(p, ToastKind.INFO, SystemText.TOAST_FLOOR_REACHED, FloorMap.label(floor));
            startFor(p, s, floor, depth);
        }
        SystemSync.request(p);
    }

    /** Rolls the quest of a newly reached floor (none on a boss floor; the chance may also say none). The old quest, finished or not, is replaced. */
    private static void startFor(ServerPlayer p, SystemState s, int floor, int depth) {
        ServerLevel level = p.serverLevel();
        Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
        int per = sector == null ? FloorMap.DEFAULT_PER_REGION : sector.perRegion;
        if (FloorMap.isDecennial(floor, per)) {
            s.setQuest(null);
            return;
        }
        long seed = QuestRoller.seed(p.getUUID(), floor);
        if (!QuestRoller.hasQuest(seed, SystemConfig.get(SystemConfig.QUEST_CHANCE))) {
            s.setQuest(null);
            return;
        }
        QuestTemplate t = QuestRoller.pick(SystemData.quests(), seed, depth, s.lastKindOrNull(),
                new QuestRoller.FloorFacts(countableRooms(sector, floor), SystemConfig.get(SystemConfig.AMBUSH_QUESTS)));
        if (t == null) {
            s.setQuest(null);
            return;
        }
        begin(p, s, t, floor, depth);
    }

    /** Starts a quest of this template on this floor (replacing the current one) and pops up the news. */
    public static QuestProgress begin(ServerPlayer p, SystemState s, QuestTemplate t, int floor, int depth) {
        QuestProgress q = QuestProgress.start(t, floor, QuestRoller.target(t, depth));
        s.setQuest(q);
        s.setLastKind(t.kind());
        DWELL.remove(p.getUUID());
        SystemSync.toast(p, ToastKind.INFO, SystemText.TOAST_QUEST_ARRIVED);
        SystemSync.request(p);
        return q;
    }

    /** How many rooms of this floor a player could clear (neither safe nor corridors). A position outside any sector assumes a typical floor. */
    static int countableRooms(Sector sector, int floor) {
        if (sector == null) return 8;
        int first = sector.firstFloor(), n = 0;
        for (Room r : sector.rooms) {
            if (r.kind.isSafe() || r.kind.isCirculation()) continue;
            if ((r.floor != 0 ? r.floor : first) == floor) n++;
        }
        return n;
    }

    // ================================================================ what a change means
    /** Acts on a state-machine change: the pop-ups, the reward, the quest hook, the sync. Returns whether anything changed. */
    static boolean apply(ServerPlayer p, SystemState s, QuestProgress q, QuestProgress.Change c) {
        switch (c) {
            case PROGRESSED -> SystemSync.request(p);
            case COMPLETED -> {
                s.countDone();
                SystemSync.toast(p, ToastKind.SUCCESS, SystemText.TOAST_QUEST_COMPLETE);
                SystemRewards.grant(p, s, q);
                NusmpEvents.fire(p, "mega.system_quest", q.id());
                SystemSync.request(p);
            }
            case FAILED -> {
                s.countFailed();
                SystemSync.toast(p, ToastKind.FAILURE, SystemText.TOAST_QUEST_FAILED);
                SystemSync.request(p);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ================================================================ feeding: time and rooms
    /** Player tick: every five ticks, the extraction hint, and for an ACTIVE quest the clock and the room the player stands in. */
    public static void tickPlayer(ServerPlayer p) {
        if (p.tickCount % STEP != 0 || !SystemConfig.enabled()) return;
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) return;
        if (p.tickCount % (STEP * 2) == 0) ShadowExtraction.notifyReady(p);
        if (!p.hasData(SystemAttachments.STATE)) return;
        SystemState s = state(p);
        QuestProgress q = s.quest();
        if (q == null || !q.isActive()) {
            DWELL.remove(p.getUUID());
            return;
        }
        BlockPos pos = p.blockPosition();
        if (currentFloor(level, p) != q.floor()) {
            DWELL.remove(p.getUUID());
            return;
        }
        if (q.kind().timed()) {
            if (apply(p, s, q, q.tickTime(STEP)) && !q.isActive()) return;
            if (p.tickCount % 100 == 0) SystemSync.request(p);       // the client counts down on its own; this keeps its clock honest
        }
        if (q.kind() == QuestKind.CLEAR_ROOMS) dwell(p, s, q, level, pos);
    }

    private static void dwell(ServerPlayer p, SystemState s, QuestProgress q, ServerLevel level, BlockPos pos) {
        Room room = MegaDungeon.roomAt(level, pos);
        int id = room != null && !room.kind.isSafe() && !room.kind.isCirculation() ? room.id : RoomDwell.NONE;
        RoomDwell d = DWELL.computeIfAbsent(p.getUUID(), k -> new RoomDwell());
        int ready = d.step(id, STEP);
        if (ready == RoomDwell.NONE || room == null || q.counted().contains(ready)) return;
        if (!roomIsClear(level, room)) {
            d.retry(20);
            return;
        }
        apply(p, s, q, q.countRoom(ready));
    }

    /** Is no hostile creature (other than a shadow) alive inside the room? One bounded entity query, made only when the player has stood in the room long enough. */
    static boolean roomIsClear(ServerLevel level, Room room) {
        return level.getEntitiesOfClass(Mob.class, AABB.of(room.box), m -> ShadowMinions.isHostile(m) && m.isAlive() && !ShadowMinions.isMinion(m)).isEmpty();
    }

    // ================================================================ feeding: kills and damage
    /** A hostile creature fell to this player (or their shadow): counts for a kill quest on its floor. */
    public static void killed(ServerPlayer p, LivingEntity dead) {
        if (!SystemConfig.enabled() || !ShadowMinions.isHostile(dead) || ShadowMinions.isMinion(dead) || !p.hasData(SystemAttachments.STATE)) return;
        SystemState s = state(p);
        QuestProgress q = s.quest();
        if (q == null || !q.isActive() || q.kind() != QuestKind.KILL_N) return;
        if (MegaDungeon.floorAt(p.serverLevel(), dead.blockPosition()) != q.floor()) return;
        apply(p, s, q, q.onKill());
    }

    /** A creature of the Dungeon Master's ambushes fell: counts for the ambush quest of every player near it on that floor. */
    public static void ambushKilled(ServerLevel level, LivingEntity dead) {
        if (!SystemConfig.enabled() || !dead.getTags().contains(AMBUSH_TAG)) return;
        int floor = MegaDungeon.floorAt(level, dead.blockPosition());
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(dead) > 48.0 * 48.0 || !p.hasData(SystemAttachments.STATE)) continue;
            SystemState s = state(p);
            QuestProgress q = s.quest();
            if (q == null || !q.isActive() || q.kind() != QuestKind.SURVIVE_AMBUSH || q.floor() != floor) continue;
            apply(p, s, q, q.onAmbushKill());
        }
    }

    /** The player took damage: fails a "no damage" quest, and a "no magic damage" quest when it was magic. Counted only on the quest's own floor. */
    public static void damaged(ServerPlayer p, DamageSource source, float amount) {
        if (amount <= 0.0f || !SystemConfig.enabled() || !p.hasData(SystemAttachments.STATE)) return;
        SystemState s = state(p);
        QuestProgress q = s.quest();
        if (q == null || !q.isActive()) return;
        if (currentFloor(p.serverLevel(), p) != q.floor()) return;
        QuestProgress.Change c = q.onDamage();
        if (c == QuestProgress.Change.NONE && isMagicDamage(source)) c = q.onMagicDamage();
        apply(p, s, q, c);
    }

    // ================================================================ admin and tests
    /** Completes the active quest as if it had been done (pays the reward). Returns false if there is no active quest. */
    public static boolean complete(ServerPlayer p) {
        SystemState s = state(p);
        QuestProgress q = s.quest();
        return q != null && apply(p, s, q, q.forceComplete());
    }

    /** Fails the active quest. Returns false if there is no active quest. */
    public static boolean fail(ServerPlayer p) {
        SystemState s = state(p);
        QuestProgress q = s.quest();
        return q != null && apply(p, s, q, q.forceFail());
    }

    /** Removes the player's quest without paying or failing it. */
    public static void clear(ServerPlayer p) {
        state(p).setQuest(null);
        DWELL.remove(p.getUUID());
        SystemSync.request(p);
    }

    /**
     * Starts a quest on the floor the player is on (or floor 1 when that is unknown), replacing the current one: the given template, or a fresh draw when
     * {@code template} is null. Returns the quest, or null when nothing could be rolled.
     */
    public static QuestProgress roll(ServerPlayer p, QuestTemplate template) {
        SystemState s = state(p);
        ServerLevel level = p.serverLevel();
        int floor = MegaDungeon.in(level) ? MegaDungeon.floorAt(level, p.blockPosition()) : 0;
        if (floor == 0) floor = lastFloor(p);
        if (floor == 0) floor = 1;
        int depth = FloorMap.threat(floor);
        QuestTemplate t = template;
        if (t == null) {
            Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
            long seed = QuestRoller.mix(QuestRoller.seed(p.getUUID(), floor) ^ level.getGameTime());
            t = QuestRoller.pick(SystemData.quests(), seed, depth, s.lastKindOrNull(), new QuestRoller.FloorFacts(Math.max(1, countableRooms(sector, floor)), true));
        }
        return t == null ? null : begin(p, s, t, floor, depth);
    }

    // ================================================================ lifecycle of the per-player scratch state
    /** A player came back (login, respawn, a new dimension): their stat modifiers are re-applied and the window is rebuilt. */
    public static void refresh(ServerPlayer p) {
        if (SystemConfig.enabled()) SystemStats.apply(p);
        SystemSync.requestNow(p);
    }

    /** A player left: their scratch state goes. */
    public static void forget(UUID player) {
        DWELL.remove(player);
        LAST_FLOOR.remove(player);
        SystemSync.forget(player);
    }

    /** Server stop. */
    public static void clearAll() {
        DWELL.clear();
        LAST_FLOOR.clear();
        SystemSync.clear();
    }
}
