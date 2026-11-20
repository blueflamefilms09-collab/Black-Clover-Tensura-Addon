package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilEntities;
import com.newuniverse.nusmp.devil.DevilNpcs;
import com.newuniverse.nusmp.devil.DevilRituals;
import com.newuniverse.nusmp.devil.DevilRitualRun;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Why: the server side of the trial fight (docs/devil_contract_phase2.md). {@link #start} spawns the devil as a {@link DevilTrialEntity} in front of the caller, {@link #report} turns "impressed" and "won" into the
 * contract offer, {@link #check} (called by the entity once a second) removes it when its caller is gone, dead, far or out of time, and {@link #end} removes it when the contract is signed or the player leaves.
 * Where it is kept: in the caller's live contract ({@code DevilContract.trial}, a UUID, never saved), not in a map. The server-wide count looks at the players who are online, never at the world.
 *
 * <p>Cost (rule 11): at most one entity per player and {@code trial.max} on the server (default 4), each for at most {@code trial.seconds} (default 150); one count over the online players per start; one check per entity
 * per second; a few chat lines to the caller. Nothing is scanned.
 */
public final class Trials {
    private Trials() {}

    /** The farthest the caller may be from the devil before the trial ends (blocks). */
    public static final double MAX_DISTANCE = 48.0;

    private static DevilTrialEntity entityOf(MinecraftServer server, UUID id) {
        if (server == null || id == null) return null;
        for (ServerLevel l : server.getAllLevels()) {
            if (l.getEntity(id) instanceof DevilTrialEntity e && !e.isRemoved()) return e;
        }
        return null;
    }

    /** How many trials run on the server (a stale pointer is cleared). */
    static int live(MinecraftServer server) {
        int n = 0;
        for (ServerPlayer q : server.getPlayerList().getPlayers()) {
            DevilContract c = DevilStore.peek(q);
            if (c == null || c.trial == null) continue;
            if (entityOf(server, c.trial) != null) n++;
            else c.trial = null;
        }
        return n;
    }

    public static boolean running(MinecraftServer server, DevilContract c) { return c != null && c.trial != null && entityOf(server, c.trial) != null; }

    /** Spawns the devil three or four blocks in front of the circle. False (nothing spawned) when the server is at its cap or there is no room. */
    static boolean start(ServerPlayer p, DevilContract c, WorldDevil d, DevilRitualRun r) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        end(server, c);
        if (live(server) >= Phase2Config.trialMax()) return false;
        ServerLevel sl = p.serverLevel();
        double x = 0, z = 0;
        boolean found = false;
        for (int i = 0; i < 3 && !found; i++) {
            double dist = i == 0 ? 4.0 : i == 1 ? 3.0 : 2.4;
            x = r.x + r.faceX * dist;
            z = r.z + r.faceZ * dist;
            found = sl.noCollision(new AABB(x - 0.5, r.y, z - 0.5, x + 0.5, r.y + 2.1, z + 0.5));
        }
        if (!found) return false;
        DevilTrialEntity e = DevilEntities.TRIAL.get().create(sl);
        if (e == null) return false;
        e.moveTo(x, DevilNpcs.standY(sl, x, r.y, z), z, (float) Math.toDegrees(Math.atan2(x - r.x, r.z - z)), 0f);
        e.init(p, d, Phase2Config.trialHealthScale());
        if (!sl.addFreshEntity(e)) return false;
        c.trial = e.getUUID();
        c.trialResult = Phase2Rules.TRIAL_NONE;
        c.trialDevil = d.id();
        return true;
    }

    /** Removes the trial devil, if any, and forgets it. */
    public static void end(MinecraftServer server, DevilContract c) {
        if (c == null) return;
        DevilTrialEntity e = entityOf(server, c.trial);
        if (e != null) e.discard();
        c.trial = null;
    }

    /** The entity says what the fight reached: the caller gets the offer (again, with the terms that result unlocks). */
    static void report(DevilTrialEntity e, int result) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        ServerPlayer p = e.challenger() == null ? null : sl.getServer().getPlayerList().getPlayer(e.challenger());
        DevilContract c = p == null ? null : DevilStore.peek(p);
        WorldDevil d = WorldDevil.byId(e.devilId());
        if (c == null || d == null || !e.getUUID().equals(c.trial)) return;
        c.trialResult = Math.max(c.trialResult, result);
        c.trialDevil = d.id();
        if (result >= Phase2Rules.TRIAL_WON) c.trial = null;
        Phase2Text.say(p, result >= Phase2Rules.TRIAL_WON ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE, result >= Phase2Rules.TRIAL_WON
                ? Phase2Text.t("trial.won", "You beat %s. It kneels. A dominance contract is possible now, if your existence points allow it.", d.name())
                : Phase2Text.t("trial.impressed", "%s is impressed. \"Enough.\" It offers a contract. Keep fighting if you want to beat it and take all of its power.", d.name()));
        DevilRituals.offerAfterTrial(p, c, d, e.position());
    }

    /** Once a second from the entity: ends the trial when it no longer makes sense. */
    static void check(DevilTrialEntity e) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        MinecraftServer server = sl.getServer();
        ServerPlayer p = e.challenger() == null ? null : server.getPlayerList().getPlayer(e.challenger());
        DevilContract c = p == null ? null : DevilStore.peek(p);
        long now = p == null ? 0L : DevilContracts.gameTime(p);
        String why = null;
        if (!Phase2Config.trial() || p == null || c == null || !e.getUUID().equals(c.trial) || !p.isAlive() || p.serverLevel() != sl) why = "gone";
        else if (p.distanceToSqr(e) > MAX_DISTANCE * MAX_DISTANCE) why = "far";
        else if (e.tickCount > Phase2Config.trialTicks()) why = "time";
        else if (e.result() >= Phase2Rules.TRIAL_IMPRESSED && !DevilRituals.offerIsOpen(c, now)) why = "offer";
        if (why == null) return;
        if (p != null && c != null && e.getUUID().equals(c.trial)) {
            c.trial = null;
            if (e.result() == Phase2Rules.TRIAL_NONE) {
                c.trialResult = Phase2Rules.TRIAL_NONE;
                Phase2Text.say(p, ChatFormatting.GRAY, Phase2Text.t("trial.left", "%s is not impressed. It leaves. The ritual is spent.", e.devilId().isEmpty() ? "The devil" : nameOf(e.devilId())));
            }
        }
        e.discard();
    }

    private static String nameOf(String id) {
        WorldDevil d = WorldDevil.byId(id);
        return d == null ? id : d.name();
    }
}
