package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.gen.StairLinks;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.travel.StairTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Why: this is the waypoint engine (the owner's "Anime-Style Return Portal &amp; Last Location Waypoint Engine"). WHEN it is saved: every two seconds while a player stands still on solid
 * ground in a playable floor of the Mega-Dungeon (not on a stair pad, which would send them straight back out; not in a pocket realm, which is not a place to return to), and at the moment they
 * use a Return Crystal. Sampling instead of hooking each exit is deliberate: there are many ways out (the Crystal, the stairs, the Gate Key, death, a command, logging off) and a sampled
 * waypoint is right for all of them, never more than two seconds old. WHERE it goes back to: the exact block, provided the floor is still a place of the player's own party (the dungeon is
 * instanced per party, so a waypoint on another party's floor, or on a floor that was regenerated away, is not honoured); otherwise the floor's arrival pad, and if even that is gone the
 * player is told. Server thread only.
 */
public final class WaypointService {
    private WaypointService() {}

    /** Persistent data key of the next game time the Crystal / the Gate Key may be used. */
    public static final String CRYSTAL_UNTIL = "nusmp_rift_crystal_until", KEY_UNTIL = "nusmp_rift_key_until";
    private static final int SAVE_EVERY = 40;

    public static Waypoint get(ServerPlayer p) {
        return Waypoint.load(p.getPersistentData().getCompound(Waypoint.KEY));
    }

    public static void clear(ServerPlayer p) { p.getPersistentData().remove(Waypoint.KEY); }

    /** The waypoint of the player's present position, or null when they do not stand somewhere a waypoint is wanted. */
    static Waypoint sample(ServerPlayer p) {
        if (!Labyrinth.inMega(p.level()) || !p.onGround() || !p.isAlive() || p.isSpectator()) return null;
        if (RiftRules.slotAt(p.getX(), p.getZ()) >= 0) return null;                          // inside a pocket realm: not a place to come back to
        MegaData data = MegaData.get(p.getServer());
        Sector s = data.sectorAt(p.blockPosition());
        if (s == null || !s.isFloor() || !SectorGenerator.playable(s)) return null;
        if (onAPad(p, s)) return null;
        return new Waypoint(p.level().dimension().location().toString(), s.id, Waypoint.levelOf(s.firstFloor()), s.firstFloor(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(),
                p.level().getGameTime());
    }

    private static boolean onAPad(ServerPlayer p, Sector s) {
        for (BlockPos pad : new BlockPos[]{s.stairForward, s.stairBack}) {
            if (pad != null && StairLinks.onPad(p.getX(), p.getY(), p.getZ(), pad.getX(), pad.getY(), pad.getZ())) return true;
            if (pad != null && Math.abs(p.getX() - pad.getX()) < 5 && Math.abs(p.getZ() - pad.getZ()) < 5 && Math.abs(p.getY() - pad.getY()) < 4) return true;
        }
        return false;
    }

    /** Saves the player's waypoint now (when there is a place to save); true when it did. */
    public static boolean save(ServerPlayer p) {
        Waypoint w = sample(p);
        if (w == null) return false;
        p.getPersistentData().put(Waypoint.KEY, w.save());
        return true;
    }

    /** Called every server tick for each player: the periodic save. */
    public static void tick(ServerPlayer p) {
        if (!RiftConfig.get(RiftConfig.WAYPOINT) || (p.tickCount + p.getId()) % SAVE_EVERY != 0) return;
        save(p);
    }

    /** What a return can do for a waypoint: the exact block, the floor's arrival pad, or nothing. */
    public enum Outcome { EXACT, PAD, GONE }

    /** Decides without moving anybody (so the self-test can ask). */
    public static Outcome outcome(MinecraftServer server, ServerPlayer p, Waypoint w) {
        if (w == null || server == null) return Outcome.GONE;
        MegaData data = MegaData.get(server);
        Sector s = data.sector(w.sector());
        if (s == null || !SectorGenerator.playable(s) || !MegaDungeon.mayEnter(p, s.polarity)) return Outcome.GONE;
        if (PartyService.instanced() && s.party != PartyService.partyOf(p)) return Outcome.GONE;
        if (s.containsRegion(BlockPos.containing(w.x(), w.y(), w.z()))) return Outcome.EXACT;
        return SectorGenerator.padOf(s) != null ? Outcome.PAD : Outcome.GONE;
    }

    /** Puts the player back at their waypoint (from Floor 0 or from anywhere). Returns true when they were moved. Tells them why when not. */
    public static boolean returnToWaypoint(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        Waypoint w = get(p);
        if (w == null || mega == null) {
            say(p, "waypoint.none", "You have no waypoint: you have not stood anywhere in the dungeon yet.", ChatFormatting.GRAY);
            return false;
        }
        Outcome o = outcome(server, p, w);
        if (o == Outcome.GONE) {
            say(p, "waypoint.gone", "Your waypoint is gone: that floor is not open to you any more. Take the Gate again.", ChatFormatting.RED);
            return false;
        }
        Sector s = MegaData.get(server).sector(w.sector());
        double x = w.x(), y = w.y(), z = w.z();
        float yaw = w.yaw(), pitch = w.pitch();
        if (o == Outcome.PAD) {
            BlockPos pad = SectorGenerator.padOf(s);
            x = pad.getX() + 0.5;
            y = pad.getY();
            z = pad.getZ() + 0.5;
        }
        mega.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        if (!Labyrinth.in(p)) LabyrinthTravel.arrive(p, mega, BlockPos.containing(x, y, z), null);       // remembers the way home, stamps the gate cooldown
        p.getPersistentData().putLong(StairTravel.UNTIL, mega.getGameTime() + 100L);                     // a stair pad nearby must not take them at once
        p.teleportTo(mega, x, y, z, yaw, pitch);
        p.resetFallDistance();
        mega.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 1.2f);
        say(p, o == Outcome.EXACT ? "waypoint.back" : "waypoint.pad", o == Outcome.EXACT ? "You step out where you left off." : "Your place was lost; you stand at the floor's arrival.",
                ChatFormatting.AQUA);
        return true;
    }

    /** The Return Crystal: remember where the player stands, then take them to Floor 0. Returns false when it could not (not in the dungeon, off cooldown ...). */
    public static boolean crystal(ServerPlayer p) {
        if (!Labyrinth.inMega(p.level()) || RiftRules.slotAt(p.getX(), p.getZ()) >= 0) {
            say(p, "crystal.where", "The crystal only answers inside the dungeon's floors.", ChatFormatting.GRAY);
            return false;
        }
        if (!cool(p, CRYSTAL_UNTIL, RiftConfig.get(RiftConfig.CRYSTAL_COOLDOWN))) return false;
        if (!save(p)) say(p, "crystal.last", "You are not standing steady enough to mark this place; the last mark stands.", ChatFormatting.GRAY);
        else com.newuniverse.nusmp.compat.maps.MapShare.returnWaypoint(p, get(p));                       // compat-maps: a clickable waypoint line for the saved place (opt-in, rate limited)
        if (!HubTravel.enabled() || !HubTravel.available(p.getServer())) {
            say(p, "crystal.nohub", "Floor 0 does not answer here.", ChatFormatting.GRAY);
            return false;
        }
        HubTravel.returnToPlaza(p);
        return true;
    }

    /** The Gate Key: from anywhere outside the dungeon's floors, straight back to the waypoint. */
    public static boolean gateKey(ServerPlayer p) {
        if (Labyrinth.inMega(p.level())) {
            say(p, "key.here", "You are already in the dungeon.", ChatFormatting.GRAY);
            return false;
        }
        if (get(p) == null) {
            say(p, "waypoint.none", "You have no waypoint: you have not stood anywhere in the dungeon yet.", ChatFormatting.GRAY);
            return false;
        }
        if (!cool(p, KEY_UNTIL, RiftConfig.get(RiftConfig.GATE_COOLDOWN))) return false;
        if (returnToWaypoint(p)) return true;
        p.getPersistentData().remove(KEY_UNTIL);                              // nothing happened: the key is not spent
        return false;
    }

    /** Checks and starts a cooldown kept in the player's persistent data (game time); tells the player how long is left when it is not over. */
    static boolean cool(ServerPlayer p, String key, int seconds) {
        long now = p.level().getGameTime(), until = p.getPersistentData().getLong(key);
        if (now < until) {
            say(p, "cooldown", "It is still quiet: %s seconds.", ChatFormatting.GRAY, (until - now + 19) / 20);
            return false;
        }
        p.getPersistentData().putLong(key, now + seconds * 20L);
        return true;
    }

    static void say(ServerPlayer p, String key, String english, ChatFormatting colour, Object... args) {
        if (p.connection == null) return;
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift." + key, english, args).withStyle(colour), true);
    }
}
