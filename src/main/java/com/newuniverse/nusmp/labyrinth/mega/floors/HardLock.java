package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the doors enforce the Directional Lock, but not everything goes through a door: an operator's teleport, a relic, a command block, an old Dungeon
 * Gate or Maw that now opens into the dungeon, or a bug in some future door would put a locked player on the road that is closed to them, and then the
 * lock would be a suggestion. This is the backstop the config calls {@code hardLock}: once a second a locked player who stands in a sector of the OTHER
 * road is told why and sent out of the dungeon with the existing {@link LabyrinthTravel#leaveAny}, which already knows how to take someone home from any
 * Labyrinth dimension (the origin they came from, else their bed or the world spawn). The decision is the pure {@link AlignmentRules#shouldCastOut} (so the
 * rule is tested without a game) applied to the polarity of the sector under the player's feet ({@code MegaDungeon.polarityAt}); a position in no sector
 * decides nothing. It never touches an operator when {@code opsBypass} is on and never a spectator. {@link #shouldCastOut} only decides and {@link #check}
 * acts, so a self-test can ask the question about a player that cannot be teleported. The expulsion is rate-limited per player (a failed teleport must not
 * repeat a message every second) and any exception is logged: the guard must never take the server tick down.
 */
public final class HardLock {
    private HardLock() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks before the same player is cast out again: long enough that a teleport that fails does not repeat its message and its log line every check, short enough that someone who is sent back in is out again at once. */
    private static final long COOLDOWN = 40L;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Should this player be cast out right now? No side effects. The cheap questions come first: it runs once a second for everybody in the dungeon, and almost nobody is locked. */
    public static boolean shouldCastOut(ServerPlayer p) {
        if (p == null || p.isSpectator()) return false;
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) return false;
        PlayerDungeonAlignment a = AlignmentAttachments.get(p);
        if (!a.locked()) return false;
        if (!FloorsConfig.get(FloorsConfig.HARD_LOCK) || !FloorsConfig.get(FloorsConfig.LOCK_ENABLED)) return false;
        if (LockPolicy.bypasses(p)) return false;
        Polarity here = MegaDungeon.polarityAt(level, p.blockPosition());
        return AlignmentRules.shouldCastOut(a.chosen(), a.locked(), here, true);
    }

    /** Casts the player out when they should be. True when they were. */
    public static boolean check(ServerPlayer p) {
        try {
            if (!shouldCastOut(p)) return false;
            long now = p.level().getGameTime();
            Long last = LAST.get(p.getUUID());
            if (last != null && now >= last && now - last < COOLDOWN) return false;
            LAST.put(p.getUUID(), now);
            Polarity here = MegaDungeon.polarityAt(p.level(), p.blockPosition());
            if (FloorsText.real(p)) {
                p.sendSystemMessage(FloorsText.text("cast_out", "You stand on the %s, but you are bound to the %s. The dungeon casts you out.", FloorsText.road(here), FloorsText.road(AlignmentAttachments.get(p).chosen()))
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            LabyrinthTravel.leaveAny(p);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The hard guard of the Directional Lock failed for {}: {}", p == null ? "?" : p.getGameProfile().getName(), e.toString());
            return false;
        }
    }

    /** Forgets what was done to one player (logout). */
    static void forget(UUID id) { LAST.remove(id); }

    /** Forgets everything (the server stopped). */
    static void clear() { LAST.clear(); }
}
