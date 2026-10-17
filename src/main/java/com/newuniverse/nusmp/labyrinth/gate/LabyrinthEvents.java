package com.newuniverse.nusmp.labyrinth.gate;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game-bus side of the gate package. The void rescue is a player tick, not a dimension tick, because only players are caught (a mob that
 * falls off the Spire is gone) and because a {@code PlayerTickEvent.Post} every 10 ticks on a few players costs nothing; it fires at y &lt; 1,
 * well above the y = -64 where the game's own void damage starts, so the player is back on the pad before the first tick of it. A death inside
 * the Labyrinth forgets the stored origin and pad ({@link LabyrinthTravel#forget}) so the respawn is an ordinary one (bed or world spawn) and
 * nothing later tries to "return" a player who is already home. Login and respawn need no handler: the origin lives in persistent data and is read only
 * when a gate or the exit command asks for it. The same tick carries a player out of the dimension of the removed Living Dungeon (1.58.0;
 * {@link LabyrinthTravel#evacuate}), which a world that remembers it keeps registered but no gate leads out of.
 */
public final class LabyrinthEvents {
    private LabyrinthEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !GateRules.checkDue(p.tickCount)) return;
        if (Labyrinth.inRemovedDungeon(p.level())) {
            LabyrinthTravel.evacuate(p);
            return;
        }
        if (!Labyrinth.in(p)) return;
        if (GateRules.falling(p.getY(), p.isSpectator(), LabyrinthConfig.get(LabyrinthConfig.VOID_RESCUE))) LabyrinthTravel.rescue(p);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Labyrinth.in(p)) LabyrinthTravel.forget(p);
    }
}
