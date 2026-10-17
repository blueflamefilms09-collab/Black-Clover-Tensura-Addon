package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: "the air of the Holy floors is dense with Light Rukh" should be something the player SEES the moment they arrive, before they cast anything, so
 * the flow has a light ambient: every two seconds a few Rukh of the flow's colour drift around each player who stands in a place that has a flow (Light
 * Rukh on the Holy road by default, Dark Rukh if the owner mirrors the Abyssal road). It is deliberately small and cheap: it runs on the server tick but does
 * work only on every 40th tick, looks only at the players of the Mega-Dungeon's own level (never the other dimensions), sends at most
 * {@code ambientCount} Rukh per player through {@code RukhFx} (whose per-tick budget drops the excess when a hundred players stand in one room), and uses no
 * vanilla particle. Spectators and the dead see nothing. Switching {@code ambient} off removes only this; the boost and the rejection are unaffected.
 */
public final class FlowAir {
    private FlowAir() {}

    /** Ticks between two puffs for one player. */
    static final int PERIOD_TICKS = 40;
    /** How far from the player the Rukh appear, in blocks. */
    private static final double SPREAD = 2.5;

    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            MinecraftServer server = e.getServer();
            if (server.getTickCount() % PERIOD_TICKS != 0) return;
            if (!FlowConfig.get(FlowConfig.ENABLED) || !FlowConfig.get(FlowConfig.AMBIENT)) return;
            int count = FlowConfig.get(FlowConfig.AMBIENT_COUNT);
            ServerLevel mega = MegaDungeon.level(server);
            if (count <= 0 || mega == null) return;
            for (ServerPlayer p : mega.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                Alignment flow = FlowEvents.flowAt(mega, FlowEvents.posOf(p));
                if (flow == Alignment.NEUTRAL) continue;
                RukhFx.burst(mega, FlowEvents.pointOf(p).add(0.0, 1.0, 0.0), Rejection.rukh(flow), count, SPREAD);
            }
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }
}
