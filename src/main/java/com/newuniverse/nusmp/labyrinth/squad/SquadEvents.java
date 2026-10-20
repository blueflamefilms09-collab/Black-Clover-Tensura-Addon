package com.newuniverse.nusmp.labyrinth.squad;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Why: three events keep the squads honest. A monster that joins the world with a squad id in its persistent data (a fresh spawn the Director stamped a moment ago, or
 * one that came back from a saved chunk) is attached to its squad, tagged and given the goals: this is where "tag the members" happens, and it is the SAME path for both,
 * so a reloaded squad is not a second-class squad. A squad member that dies is counted at once (not at the next think), which is what lets the morale rule see a leader fall
 * in the tick it happened. And the server stopping empties the registry, so nothing outlives a world. Each handler catches everything: a fault in the squad layer must never
 * stop a mob from spawning or dying.
 */
public final class SquadEvents {
    private SquadEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || !(e.getLevel() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            if (SquadManager.idOf(mob) == null) return;
            SquadManager.adopt(mob);
        } catch (Throwable t) {
            warn("join", t);
        }
    }

    public static void onDeath(LivingDeathEvent e) {
        try {
            if (e.getEntity().level().isClientSide() || !(e.getEntity() instanceof Mob mob) || !mob.getTags().contains(Squad.TAG)) return;
            Squad s = SquadManager.of(mob);
            if (s != null) s.memberDied(mob.getUUID());
        } catch (Throwable t) {
            warn("death", t);
        }
    }

    public static void onServerStopped(ServerStoppedEvent e) { SquadManager.reset(); }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The squad layer failed once in {} and goes on: {}", what, t.toString(), t);
    }
}
