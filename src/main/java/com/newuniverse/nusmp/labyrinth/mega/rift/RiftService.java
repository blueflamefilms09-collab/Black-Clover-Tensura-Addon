package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaConfig;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import com.newuniverse.nusmp.labyrinth.mega.travel.StairTravel;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the server half of the rifts: it opens them (by themselves near players on the floors, by a relic, by an admin), keeps the list of the ones that are open, decides what stepping into
 * each kind does, and closes them. SPAWNING: every {@code checkSeconds} each player on a playable floor rolls once for a rift near them (a shortcut, or rarely a pocket); the spot is a
 * floor cell 8 to 22 blocks away, inside the same floor and clear of walls, never close to a stair pad or in a safe room, and no further rift opens while {@code maxNear} are already open within
 * 48 blocks, so the floors are not carpeted. SHORTCUT: the jump is rolled from the rift's seed (3 to 5 floors), held at the next boss floor ({@link RiftRules#shortcutTarget}) so a rift can
 * never skip a Decennial Boss's seal; if the destination floor is not built yet the rift asks the generator for it (the floors behind it, one at a time) and shows itself dim ("unstable") until it is,
 * exactly as the stairs say "still forming"; since 1.94.0 a rift that has waited {@link RiftRules#FORM_PATIENCE_TICKS} settles for the farthest floor that IS built, so it is never a wall. The
 * Directional Lock is asked first. The player arrives on the destination floor's arrival pad, together with party mates standing within six blocks. POCKET: allocates a cell of the pocket field
 * ({@link PocketRealm}), builds the realm and takes the player in. RETURN: the player's own waypoint ({@link WaypointService}). EXIT: back to where the pocket was entered. 1.94.0 also
 * announces a new rift to the delvers near it, gives every open rift a faint chime and a rising wisp of light, lets a right click step through, and never lets an error in a rift's tick
 * reach the server's tick loop. Server thread only.
 */
public final class RiftService {
    private static final Logger LOG = LogUtils.getLogger();

    /** 1.94.0: an open rift is its dimension and its position (two dimensions share coordinates; a Return Portal of the Hub used to hold back the spawner of the dungeon's first floor). A null dimension is a legacy entry that counts everywhere. */
    private record Key(ResourceKey<Level> dim, long pos) {}

    private static final Set<Key> OPEN = new HashSet<>();                       // the rifts that are loaded and open
    private static final String NEXT_ROLL = "nusmp_rift_next";

    private RiftService() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RiftService::onServerTick);
        gameBus.addListener(RiftService::onStopped);
    }

    private static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) { OPEN.clear(); }

    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null || mega.players().isEmpty()) return;
        for (ServerPlayer p : new ArrayList<>(mega.players())) {
            try {
                WaypointService.tick(p);
                if ((server.getTickCount() + p.getId()) % 20 == 0) roll(mega, p);
            } catch (RuntimeException ex) {                                         // one player's failure must not stop the others' rolls
                LOG.warn("[nusmp] The rift spawner failed for {} (further detail at debug): {}", p.getGameProfile().getName(), ex.toString());
                LOG.debug("rift spawner", ex);
            }
        }
    }

    // ---------------------------------------------------------------- spawning
    private static void roll(ServerLevel mega, ServerPlayer p) {
        if (!RiftConfig.get(RiftConfig.ENABLED) || p.isSpectator() || !p.onGround()) return;
        long now = mega.getGameTime();
        var data = p.getPersistentData();
        if (now < data.getLong(NEXT_ROLL)) return;
        data.putLong(NEXT_ROLL, now + RiftConfig.get(RiftConfig.CHECK_SECONDS) * 20L);
        rollNow(mega, p, null);
    }

    /** The gate of a roll at the player's feet (why a rift can or cannot open here). {@code force} is an admin: the switch and standing on the ground do not count. */
    public static RiftRules.RollGate gateFor(ServerLevel mega, ServerPlayer p, boolean force) {
        MegaData md = MegaData.get(mega.getServer());
        BlockPos pos = p.blockPosition();
        Sector s = md.sectorAt(pos);
        return RiftRules.rollGate(force || RiftConfig.get(RiftConfig.ENABLED), p.isSpectator(), force || p.onGround(), RiftRules.slotAt(p.getX(), p.getZ()) >= 0, s != null, s != null && s.isFloor(),
                s != null && SectorGenerator.playable(s), s != null && MegaDungeon.inSafeZone(mega, pos), nearCount(mega, pos, RiftRules.HINT_RADIUS), RiftConfig.get(RiftConfig.MAX_NEAR));
    }

    /**
     * One roll for a new rift near {@code p}: with {@code forced == null} the natural roll (the chances of the config), else an admin's (that kind, at once). Returns null when a rift opened, else the
     * short reason it did not (a {@link RiftRules.RollGate} name in lower case, {@code chance}, {@code boss_floor}, {@code dead_end}, {@code no_spot}, {@code blocked}, {@code kind}).
     */
    public static String rollNow(ServerLevel mega, ServerPlayer p, RiftType forced) { return rollNow(mega, p, forced, null); }

    /**
     * 1.99.0 (the exclusive tombs): the same roll with a way to ask for a MEGA-RARE rift. {@code megaPocket} is null for the roll the game makes (a rift that is going to open may be mega-rare by the
     * table of {@code pocket/RiftTiers}: depth, Luck and the player's pity), "" for an admin's mega-rare rift to a random tomb, or the id of a tomb. Everything else is as before.
     */
    public static String rollNow(ServerLevel mega, ServerPlayer p, RiftType forced, String megaPocket) {
        boolean force = forced != null || megaPocket != null;
        RiftRules.RollGate gate = gateFor(mega, p, force);
        if (gate != RiftRules.RollGate.OK) return gate.name().toLowerCase(Locale.ROOT);
        MegaData md = MegaData.get(mega.getServer());
        Sector s = md.sectorAt(p.blockPosition());
        var rnd = mega.getRandom();
        boolean pocket, shortcut;
        if (force) {
            pocket = megaPocket != null || forced == RiftType.POCKET;
            shortcut = megaPocket == null && forced == RiftType.SHORTCUT;
            if (!pocket && !shortcut) return "kind";
        } else {
            pocket = RiftConfig.get(RiftConfig.POCKETS) && RiftRules.rolls(RiftConfig.get(RiftConfig.POCKET_CHANCE), rnd.nextDouble());
            shortcut = !pocket && !s.isBossFloor() && RiftRules.rolls(RiftConfig.get(RiftConfig.SHORTCUT_CHANCE), rnd.nextDouble());
            if (!pocket && !shortcut) return "chance";
        }
        if (shortcut) {
            if (s.isBossFloor()) return "boss_floor";
            Sector tail = md.chainTail(s.party, s.polarity);
            if (!RiftRules.canLeadAnywhere(s.chainIndex, tail == null ? s.chainIndex : tail.chainIndex, MegaConfig.get(MegaConfig.EXPANSION))) return "dead_end";   // nothing beyond and nothing will be built: a tear to nowhere is a wall
        }
        BlockPos at = findSpotNear(mega, s, p.getX(), p.getY(), p.getZ());
        if (at == null) return "no_spot";
        com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDef tomb = megaPocket != null ? com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts.pickForced(megaPocket, rnd)
                : force ? null : com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts.rollNatural(p, s.firstFloor(), rnd);                  // 1.99.0: a rift that is going to open may lead to an exclusive tomb
        if (megaPocket != null && tomb == null) return "unknown_pocket";
        DimensionalRiftBlockEntity be = open(mega, at, pocket || tomb != null ? RiftType.POCKET : RiftType.SHORTCUT, rnd.nextLong());
        if (be == null) return "blocked";
        if (tomb != null) {
            com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts.aim(be, tomb);
            com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts.announce(mega, at, tomb, p);
        } else {
            if (!pocket) aimShortcut(md, s, be);
            announce(mega, at, be.type(), p);
        }
        if (!force) com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts.opened(p, tomb != null);                                            // the pity moves only when a rift really opened
        return null;
    }

    /**
     * 1.94.0: tells the delvers within {@link RiftRules#HINT_RADIUS} blocks that a rift opened, and where: the action bar line it always had, a chat line with the distance and the compass point, the System's
     * pop-up, and the map mods' waypoint line (opt-in, rate limited). Before this only the one who happened to be rolled for saw one line on the action bar, and a rift lives four minutes: it was
     * found by chance or not at all.
     */
    public static void announce(ServerLevel level, BlockPos at, RiftType type, ServerPlayer finder) {
        boolean pocket = type == RiftType.POCKET;
        double r2 = (double) RiftRules.HINT_RADIUS * RiftRules.HINT_RADIUS;
        for (ServerPlayer q : new ArrayList<>(level.players())) {
            double dx = at.getX() + 0.5 - q.getX(), dy = at.getY() - q.getY(), dz = at.getZ() + 0.5 - q.getZ();
            if (q.isSpectator() || dx * dx + dy * dy + dz * dz > r2) continue;
            q.displayClientMessage(Component.translatableWithFallback(pocket ? "message.nusmp.mega.rift.opened.pocket" : "message.nusmp.mega.rift.opened.shortcut",
                    pocket ? "The air tears open nearby: a hidden realm hums behind it." : "A tear in the air opens nearby. It leads further on.").withStyle(pocket ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA), true);
            if (SystemSync.hasConnection(q)) {
                q.sendSystemMessage(Component.translatableWithFallback("message.nusmp.mega.rift.hint",
                        "[SYSTEM] A rift opened %s blocks to the %s. Walk into the tear, or right-click it, to step through; it closes in a few minutes.", (int) Math.round(Math.sqrt(dx * dx + dz * dz)),
                        RiftRules.compass(dx, dz)).withStyle(pocket ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
                SystemSync.toast(q, ToastKind.INFO, SystemText.TOAST_RIFT_NEAR);
            }
            com.newuniverse.nusmp.compat.maps.MapShare.rift(q, at, pocket);                                // compat-maps: a clickable waypoint line for the rift (opt-in, rate limited)
        }
        if (finder != null) NusmpEvents.fire(finder, "mega.rift", pocket ? "pocket" : "shortcut");
    }

    /**
     * 1.94.0: the hint an open rift gives every two seconds: a faint chime for whoever is within reach of hearing it, and a wisp of Rukh light that rises from the tear and is sent to delvers up to 64 blocks
     * away (a normal particle packet stops at 32), so a tear can be found by following a faint column of light in the dark. Cheap: nothing is sent unless a delver is near.
     */
    public static void beacon(ServerLevel level, DimensionalRiftBlockEntity be) {
        if (RiftRules.openness(be.age(), be.left()) < 0.2f) return;
        BlockPos at = be.getBlockPos();
        double x = at.getX() + 0.5, y = at.getY(), z = at.getZ() + 0.5;
        boolean hear = false;
        boolean dark = be.type() == RiftType.POCKET;
        for (ServerPlayer q : level.players()) {
            double d2 = q.distanceToSqr(x, y, z);
            if (q.isSpectator() || d2 > 64.0 * 64.0) continue;
            if (d2 <= 20.0 * 20.0) hear = true;
            level.sendParticles(q, dark ? RukhParticles.dark() : RukhParticles.light(), true, x, y + 1.6, z, 4, 0.2, 1.5, 0.2, 0.01);
        }
        if (hear) level.playSound(null, at, dark ? SoundEvents.AMETHYST_CLUSTER_STEP : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, dark ? 0.5f : 0.9f);
    }

    /** A free cell 8 to 22 blocks from (px, py, pz) inside the floor {@code s}: open ground, away from the stair pads and the safe rooms; null when 24 tries find none. */
    static BlockPos findSpotNear(ServerLevel level, Sector s, double px, double py, double pz) {
        var rnd = level.getRandom();
        for (int i = 0; i < 24; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = 8 + rnd.nextDouble() * 14;
            int x = (int) Math.floor(px + Math.cos(a) * d), z = (int) Math.floor(pz + Math.sin(a) * d);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos feet = new BlockPos(x, (int) Math.floor(py) + dy, z);
                if (!s.containsRegion(feet) || !standable(level, feet)) continue;
                if (MegaDungeon.inSafeZone(level, feet)) continue;
                if (nearPad(s, feet)) continue;
                return feet;
            }
        }
        return null;
    }

    private static boolean nearPad(Sector s, BlockPos p) {
        for (BlockPos pad : new BlockPos[]{s.stairForward, s.stairBack, s.arrival}) if (pad != null && pad.distSqr(p) < 36) return true;
        return false;
    }

    /** A cell a rift may stand in: solid below, three cells of air above, and air on the four sides at the feet (an open spot, not a corridor corner). */
    static boolean standable(ServerLevel level, BlockPos feet) {
        if (!level.getBlockState(feet.below()).isSolidRender(level, feet.below())) return false;
        for (int i = 0; i < 3; i++) if (!level.getBlockState(feet.above(i)).isAir()) return false;
        for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST})
            if (!level.getBlockState(feet.relative(d)).isAir()) return false;
        return true;
    }

    static int nearCount(ServerLevel level, BlockPos at, int radius) {
        int n = 0;
        long r2 = (long) radius * radius;
        for (Key k : OPEN) if ((k.dim() == null || k.dim() == level.dimension()) && BlockPos.of(k.pos()).distSqr(at) <= r2) n++;
        return n;
    }

    /** Opens a rift of {@code type} with its feet cell at {@code at} (server), or null when the cell is not free. */
    public static DimensionalRiftBlockEntity open(ServerLevel level, BlockPos at, RiftType type, long seed) {
        BlockState now = level.getBlockState(at);
        if (!now.isAir() && !now.canBeReplaced()) return null;
        level.setBlock(at, RiftRegistry.RIFT.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(at);
        if (!(be instanceof DimensionalRiftBlockEntity r)) return null;
        int base = RiftConfig.get(RiftConfig.RIFT_SECONDS) * 20;
        int life = switch (type) {
            case POCKET -> base * 2;
            case RETURN -> RiftConfig.get(RiftConfig.PORTAL_SECONDS) * 20;
            case EXIT -> 20 * 60 * RiftConfig.get(RiftConfig.POCKET_MINUTES) + 400;
            default -> base;
        };
        r.init(type, seed, life);
        OPEN.add(new Key(level.dimension(), at.asLong()));
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.4f, 0.5f);
        burst(level, at, type);
        return r;
    }

    /** Called when a rift's block entity is loaded again after a restart: it counts as open once more. */
    public static void noteLoaded(Level level, BlockPos at) { OPEN.add(new Key(level.dimension(), at.asLong())); }

    /** Called when a rift's block entity goes (it collapsed, or its chunk unloaded): it is no longer counted. */
    public static void noteGone(Level level, BlockPos at) { OPEN.remove(new Key(level.dimension(), at.asLong())); }

    /** The forms from before 1.94.0 (no dimension: the entry counts in every dimension); kept so nothing that called them breaks. */
    public static void noteLoaded(BlockPos at) { OPEN.add(new Key(null, at.asLong())); }

    public static void noteGone(BlockPos at) { OPEN.remove(new Key(null, at.asLong())); }

    /** Decides where a shortcut leads, from the floor it stands on. */
    public static void aimShortcut(MegaData md, Sector from, DimensionalRiftBlockEntity be) {
        int jump = RiftRules.jumpFor(be.seed(), RiftConfig.get(RiftConfig.JUMP_MIN), RiftConfig.get(RiftConfig.JUMP_MAX));
        int target = RiftRules.shortcutTarget(from.chainIndex, jump, c -> isBoss(md, from.party, from.polarity, c));
        be.aimShortcut(from.party, from.polarity, from.chainIndex, target);
    }

    static boolean isBoss(MegaData md, int party, Polarity road, int chain) {
        Sector s = md.chainSector(party, road, chain);
        return s != null ? s.isBossFloor() : FloorMap.isBossFloor(FloorMap.floorOfSector(road, chain));
    }

    // ---------------------------------------------------------------- stability: is the destination there yet?
    public static void refreshStability(ServerLevel level, DimensionalRiftBlockEntity be) { refreshStability(level, be, true); }

    /**
     * Is the floor behind a shortcut built? Yes: the tear is steady. No: it is dim, asks the generator for the floor after the highest one that exists (when {@code expand}), and counts how long it has
     * waited; after {@link RiftRules#FORM_PATIENCE_TICKS} it settles for the farthest floor between its own and its target that IS built ({@link RiftRules#forming}), so the delver who walks in is
     * never left standing at a wall. {@code expand} is false in the self-test, which must not grow the dungeon.
     */
    public static void refreshStability(ServerLevel level, DimensionalRiftBlockEntity be, boolean expand) {
        MegaData md = MegaData.get(level.getServer());
        if (be.targetChain() <= be.fromChain()) {                           // nowhere to go (it was opened on a boss floor): stays unstable and goes out by itself
            be.setStable(false);
            return;
        }
        int party = be.party();
        Polarity road = be.polarity();
        java.util.function.IntPredicate built = c -> {
            Sector x = md.chainSector(party, road, c);
            return x != null && SectorGenerator.playable(x) && SectorGenerator.padOf(x) != null;
        };
        boolean ready = built.test(be.targetChain());
        int nearer = ready ? -1 : RiftRules.settleTarget(be.fromChain(), be.targetChain() - 1, built);
        switch (RiftRules.forming(ready, be.waited(), nearer)) {
            case READY -> {
                be.setStable(true);
                return;
            }
            case SETTLE -> {
                int was = be.targetChain(), waitedTicks = be.waited();
                be.retarget(nearer);
                LOG.info("[nusmp] A shortcut rift at {} waited {} s for chain {} and settles for chain {}.", be.getBlockPos().toShortString(), waitedTicks / 20, was, nearer);
                for (ServerPlayer q : level.players())
                    if (q.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY(), be.getBlockPos().getZ() + 0.5) <= 12.0 * 12.0)
                        WaypointService.say(q, "shortcut.settled", "The tear steadies: it leads %s floors on.", ChatFormatting.AQUA, nearer - be.fromChain());
                return;
            }
            default -> {
                be.setStable(false);
                be.waited(be.waited() + 20);
            }
        }
        if (!expand || be.age() < be.nextExpand()) return;
        be.nextExpand(be.age() + 100);
        int h = be.targetChain() - 1;                                       // the highest floor that exists below the target: ask for the one after it
        Sector from = null;
        for (; h >= be.fromChain(); h--) {
            from = md.chainSector(be.party(), be.polarity(), h);
            if (from != null) break;
        }
        if (from != null && SectorGenerator.playable(from) && !from.isBossFloor()) {
            String why = ExpansionQueue.get().tryNext(level, from);
            if (why != null) LOG.debug("[nusmp] A shortcut rift could not queue the floor after chain {}: {}", from.chainIndex, why);
        }
    }

    // ---------------------------------------------------------------- stepping in
    /** The persistent-data key of the game time before which a player is not taken by a tear again (package-visible: the self-test lets the cooldown pass). */
    static final String TOUCH = "nusmp_rift_touch";

    /**
     * 1.94.0: every five ticks (from the rift's tick): which of {@code candidates} stand in the tear. It used to ask the level for the players in a box, which cannot see a stand-in that is not in the
     * level's entity list; it asks {@link RiftRules#inTear} of each candidate instead (the same box), so the self-test drives the very code the tick does.
     */
    public static void step(ServerLevel level, DimensionalRiftBlockEntity be, Collection<ServerPlayer> candidates) {
        BlockPos pos = be.getBlockPos();
        for (ServerPlayer p : new ArrayList<>(candidates)) {                // a copy: a pocket takes players out of the list it came from
            if (!p.isAlive() || p.isSpectator() || p.level() != level) continue;
            if (RiftRules.inTear(pos.getX(), pos.getY(), pos.getZ(), p.getX(), p.getY(), p.getZ())) touch(level, p, be);
        }
    }

    /** A player is inside the tear (every five ticks): carries out what this kind of rift does. */
    public static void touch(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        long now = level.getGameTime();
        if (now < p.getPersistentData().getLong(TOUCH)) return;
        p.getPersistentData().putLong(TOUCH, now + 60L);
        act(level, p, be);
    }

    /** A right click on the tear: the same as stepping in (and the same short cooldown, so a double click does not go through twice). */
    public static void enter(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        if (be.age() <= RiftRules.GROW_TICKS / 2) return;                   // still opening
        touch(level, p, be);
    }

    /** The admin's way in: no cooldown, no waiting for the tear to open. */
    public static void forceEnter(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        p.getPersistentData().putLong(TOUCH, level.getGameTime() + 60L);
        act(level, p, be);
    }

    private static void act(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        try {
            switch (be.type()) {
                case SHORTCUT -> shortcut(level, p, be);
                case POCKET -> PocketRealm.enter(level, p, be);
                case RETURN -> WaypointService.returnToWaypoint(p);
                case EXIT -> PocketRealm.leave(level, p);
            }
        } catch (RuntimeException | LinkageError ex) {
            failed(level, be, ex);
            WaypointService.say(p, "failed", "The tear shudders and lets go of you. (The error is in the server log.)", ChatFormatting.RED);
        }
    }

    /** An error inside a rift: logged, and after {@link RiftRules#FAIL_LIMIT} of them the rift closes itself instead of failing every tick. */
    public static void failed(ServerLevel level, DimensionalRiftBlockEntity be, Throwable ex) {
        int n = be.failed(ex);
        LOG.warn("[nusmp] A {} rift at {} failed ({} of {}): {}", be.type().id(), be.getBlockPos().toShortString(), n, RiftRules.FAIL_LIMIT, ex.toString());
        LOG.debug("rift failure", ex);
        if (n >= RiftRules.FAIL_LIMIT) be.close();
    }

    private static void shortcut(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        MegaData md = MegaData.get(level.getServer());
        if (!MegaDungeon.mayEnter(p, be.polarity())) return;                   // the lock says why
        if (!be.stable()) {
            if (be.targetChain() <= be.fromChain()) WaypointService.say(p, "shortcut.none", "This tear leads nowhere: a boss floor's seal stands in the way.", ChatFormatting.GRAY);
            else WaypointService.say(p, "shortcut.forming", "The tear ripples: the floor behind it is still forming. Wait here.", ChatFormatting.GRAY);
            return;
        }
        Sector t = md.chainSector(be.party(), be.polarity(), be.targetChain());
        BlockPos pad = t == null ? null : SectorGenerator.padOf(t);
        if (pad == null) {
            WaypointService.say(p, "shortcut.lost", "The tear collapses: the floor behind it is gone.", ChatFormatting.RED);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : level.players())
            if (o != p && o.distanceToSqr(p) <= 36 && !o.isSpectator() && PartyService.partyOf(o) == PartyService.partyOf(p) && MegaDungeon.mayEnter(o, be.polarity())) group.add(o);
        for (ServerPlayer m : group) {
            LabyrinthTravel.arrive(m, level, pad, null);
            m.getPersistentData().putLong(StairTravel.UNTIL, level.getGameTime() + 100L);
            title(m, Component.translatableWithFallback("message.nusmp.mega.rift.floor", "Floor %s", FloorMap.label(t.firstFloor())),
                    Component.translatableWithFallback("message.nusmp.mega.rift.jump", "You fall through the tear: %s floors on.", be.targetChain() - be.fromChain()));
            NusmpEvents.fire(m, "mega.rift_jump", FloorMap.label(t.firstFloor()));
        }
        be.close();                                                         // a tear is used up by being used
    }

    public static void title(ServerPlayer p, Component title, Component subtitle) {
        if (p.connection == null || p instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(6, 30, 10));
        p.connection.send(new ClientboundSetTitleTextPacket(title.copy().withStyle(ChatFormatting.WHITE)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    // ---------------------------------------------------------------- closing
    /** The rift ran out (or was used up): it gutters out with a puff and the block goes. */
    public static void collapse(ServerLevel level, BlockPos pos, DimensionalRiftBlockEntity be) {
        OPEN.remove(new Key(level.dimension(), pos.asLong()));
        burst(level, pos, be.type());
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.2f, 0.6f);
        if (level.getBlockState(pos).is(RiftRegistry.RIFT.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    static void burst(ServerLevel level, BlockPos at, RiftType type) {
        level.sendParticles(type == RiftType.POCKET ? RukhParticles.dark() : RukhParticles.light(), at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 24, 0.5, 0.9, 0.5, 0.02);
    }

    /** Every rift that is open now (position longs), for the admin list. */
    public static List<BlockPos> openRifts() {
        List<BlockPos> out = new ArrayList<>();
        for (Key k : OPEN) out.add(BlockPos.of(k.pos()));
        return out;
    }

    /** 1.94.0: the open rifts of one dimension (positions). */
    public static List<BlockPos> openRifts(Level level) {
        List<BlockPos> out = new ArrayList<>();
        for (Key k : OPEN) if (k.dim() == null || k.dim() == level.dimension()) out.add(BlockPos.of(k.pos()));
        return out;
    }

    /** 1.94.0: the nearest loaded rift of {@code level} within {@code maxDist} blocks of {@code from}, or null (the admin commands ask). */
    public static DimensionalRiftBlockEntity nearest(ServerLevel level, BlockPos from, double maxDist) {
        DimensionalRiftBlockEntity best = null;
        double bd = maxDist * maxDist;
        for (BlockPos r : openRifts(level)) {
            double d = r.distSqr(from);
            if (d <= bd && level.isLoaded(r) && level.getBlockEntity(r) instanceof DimensionalRiftBlockEntity be) {
                best = be;
                bd = d;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- the Return Portal on Floor 0
    /** Opens a Return Portal three blocks in front of the player (anywhere outside the dungeon's floors). Returns false when there is no room or no waypoint. */
    public static boolean openReturnPortal(ServerPlayer p) {
        if (WaypointService.get(p) == null) {
            WaypointService.say(p, "waypoint.none", "You have no waypoint: you have not stood anywhere in the dungeon yet.", ChatFormatting.GRAY);
            return false;
        }
        ServerLevel level = p.serverLevel();
        var look = p.getDirection();
        for (int dist = 3; dist >= 2; dist--) {
            BlockPos feet = p.blockPosition().relative(look, dist);
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos at = feet.above(dy);
                if (level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir() && level.getBlockState(at.above(2)).isAir()) {
                    DimensionalRiftBlockEntity be = open(level, at, RiftType.RETURN, level.getRandom().nextLong());
                    if (be != null) return true;
                }
            }
        }
        WaypointService.say(p, "portal.room", "There is no room to open a portal here.", ChatFormatting.GRAY);
        return false;
    }

    /** Is this level the Mega-Dungeon? (A helper for the pocket code, which must not read a sector from another dimension's coordinates.) */
    static boolean inMega(Level level) { return Labyrinth.inMega(level); }
}
