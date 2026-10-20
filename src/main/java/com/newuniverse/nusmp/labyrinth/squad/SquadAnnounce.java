package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import com.newuniverse.nusmp.labyrinth.squad.SquadMind.Cause;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the owner wants the System to say what is coming ("Squad: 6: 2 tank, 1 melee, ..."), and a squad whose nerve broke should be told too, because the fight changes
 * (the survivors run or go wild). Both are one pop-up line of the System panel ({@link SystemSync#toast}), the same channel the floor quests and the shadow extraction use,
 * sent to the players within 64 blocks of the squad. The lang keys are in {@code tools/lang/squad_loot.json}; a stand-in player of a self-test has no connection and is
 * skipped by the sender, never an error.
 */
final class SquadAnnounce {
    private SquadAnnounce() {}

    static final String KEY_FORMED = "message.nusmp.squad.formed";
    static final String KEY_LEADER = "message.nusmp.squad.broken.leader", KEY_LOSSES = "message.nusmp.squad.broken.losses", KEY_LAST = "message.nusmp.squad.broken.last";

    /** "Squad: N: roles" for the players near the squad's mobs. */
    static void formed(ServerLevel level, Squad squad, V2 at) {
        if (!SquadConfig.get(SquadConfig.ANNOUNCE)) return;
        tellNear(level, at, KEY_FORMED, squad.describe());
    }

    static void broken(ServerLevel level, Squad squad, Cause cause, V2 at) {
        if (!SquadConfig.get(SquadConfig.ANNOUNCE)) return;
        String key = cause == Cause.LEADER_DOWN ? KEY_LEADER : cause == Cause.LAST_STANDING ? KEY_LAST : KEY_LOSSES;
        tellNear(level, at, key, Integer.toString(squad.aliveCount()));
    }

    private static void tellNear(ServerLevel level, V2 at, String key, String arg) {
        try {
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || new V2(p.getX(), p.getZ()).dist(at) > 64.0) continue;
                SystemSync.toast(p, ToastKind.INFO, key, arg);
            }
        } catch (RuntimeException unreachable) {
            // a line of news must never undo a squad
        }
    }
}
