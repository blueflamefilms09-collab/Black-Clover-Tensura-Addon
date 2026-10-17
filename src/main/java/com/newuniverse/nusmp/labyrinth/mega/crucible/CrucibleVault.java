package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Crucible must wake up exactly when a person walks into the vault, and not before: this is its ticker. Every ten ticks it looks at the
 * survival players in the Mega-Dungeon; the first one found inside a vault box whose Crucible is UNOPENED rolls the offer ({@link CrucibleOffer#roll}),
 * and while a vault is ROLLED it keeps its pedestals showing the saved offer (a regenerated sector, an admin's {@code /data}), which is the "re-entry
 * re-reads the saved ids to repair a pedestal" rule. It also holds the two housekeeping jobs that keep the saved data honest: a vault found CHOSEN with
 * no catastrophe running (a pick that was cut short: the hook marks the vault RESOLVED as soon as it is done, so CHOSEN only outlives a failure in the middle of
 * a pick) is RESOLVED and the next sector released, so the dungeon is never held back by a vault that stopped half way, and a vault
 * that is CHOSEN or RESOLVED is checked once per server session for a stale preview on a pedestal and emptied. The same tick drives the running
 * catastrophes ({@link CatastropheRunner#tick}); with nothing running and nobody in the dungeon the whole handler is two comparisons. Creative players do not
 * wake the Crucible (an operator can look at a vault without spending it; {@code /multiverse mega crucible roll} is the admin's way).
 */
public final class CrucibleVault {
    private CrucibleVault() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Sector ids whose finished vault was already checked for a stale preview in this server session. */
    private static final Set<Integer> TIDIED = new HashSet<>();

    /** The server tick: the catastrophes every tick, the vaults every ten. */
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        try {
            CatastropheRunner.tick(server);
            int t = server.getTickCount();
            if (t % 10 != 0 || !ThemeConfig.get(ThemeConfig.CRUCIBLE_ENABLED)) return;
            ServerLevel level = MegaDungeon.level(server);
            if (level == null) return;
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) return;
            MegaData data = MegaData.get(server);
            if (t % 100 == 0) sweepChosen(level, data);
            for (ServerPlayer p : players) visit(level, data, p, p.blockPosition(), t);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] mega crucible: the vault tick failed: {}", ex.toString());       // a tick handler never throws
        }
    }

    /**
     * One player standing at {@code pos}: when that is inside a sector's vault box (and the player is neither a spectator nor in creative mode) the vault
     * reacts to its phase: an UNOPENED one rolls its offer, a ROLLED one re-shows it every two seconds, a finished one is tidied once per session. The ticker
     * calls this with each player's block position; a self-test calls it with a fake player, which no level lists and whose own position is not a real one.
     * A player in a ROLLED vault also reads, on the action bar, the relic they are looking at and where it leads ({@link CrucibleOffer#inspect}).
     * Returns the sector whose vault the player is in, or null.
     */
    static Sector visit(ServerLevel level, MegaData data, ServerPlayer p, BlockPos pos, int tick) {
        if (p.isSpectator() || p.isCreative()) return null;
        Sector s = data.sectorAt(pos);
        if (s == null || !CruciblePedestals.inBox(s, pos)) return null;
        switch (Phase.of(s.crucible.phase.name())) {
            case UNOPENED -> CrucibleOffer.roll(level, s);
            case ROLLED -> {
                if (tick % 40 == 0) CrucibleOffer.show(level, s);
                CrucibleOffer.inspect(s, p);
            }
            case CHOSEN, RESOLVED -> { if (TIDIED.add(s.id)) CrucibleOffer.clearAll(level, s, true); }
        }
        return s;
    }

    /** A vault that is CHOSEN while no catastrophe runs for it (a pick cut short by a failure) is RESOLVED and the listeners hear it. */
    static void sweepChosen(ServerLevel level, MegaData data) {
        for (Sector s : List.copyOf(data.sectors())) {
            if (Phase.of(s.crucible.phase.name()) != Phase.CHOSEN || CatastropheRunner.active(s.id)) continue;
            s.crucible.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.afterRestart(Phase.CHOSEN).name());
            data.setDirty();
            LOG.info("[nusmp] mega crucible: the catastrophe of sector {} did not survive the restart; its vault is resolved", s.id);
            MegaDungeon.crucibleResolved(level, s.id);
        }
    }

    /**
     * Puts a vault back to UNOPENED: its catastrophe is stopped (its spirit let go), the saved offer, choice and catastrophe are cleared and every pedestal is
     * emptied and marked fresh. The next survival player to enter rolls a new offer. The admin command and the self-tests use it.
     */
    static void reset(ServerLevel level, Sector s) {
        CatastropheRunner.stop(level, s.id);
        Sector.Crucible c = s.crucible;
        c.phase = Sector.Crucible.Phase.valueOf(CrucibleRules.Phase.UNOPENED.name());
        c.offered.clear();
        c.chosen = "";
        c.chooser = null;
        c.catastrophe = "";
        c.since = 0L;
        CrucibleOffer.clearAll(level, s, false);
        TIDIED.remove(s.id);
        MegaData.get(level.getServer()).setDirty();
    }

    /** The server is stopping: forget the running catastrophes and the session's housekeeping (what was CHOSEN is resolved at the next start). */
    public static void onServerStopping(ServerStoppingEvent e) {
        CatastropheRunner.clear();
        TIDIED.clear();
        CrucibleOffer.clearWarnings();
    }
}
