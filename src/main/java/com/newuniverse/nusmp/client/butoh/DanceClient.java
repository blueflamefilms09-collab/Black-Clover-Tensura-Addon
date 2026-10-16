package com.newuniverse.nusmp.client.butoh;

import com.newuniverse.nusmp.butoh.DanceChoreographer;
import com.newuniverse.nusmp.butoh.DanceStatePayload;
import com.newuniverse.nusmp.client.CastAnimClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Why: the server tells every client only the sequence seed and start tick of a dance; this class is the client half that turns them into
 * body animation. Each client tick it walks the same {@link DanceChoreographer} sequence for every dancer it can see and, when the move
 * changes, hands the move's clip to the mod's own cast animation player ({@link CastAnimClient}; Player Animator / ParCool are not used).
 * The clips are looping and crossfade, so the dance flows from move to move. Entries are dropped when the dancer disappears, so nothing
 * piles up.
 */
public final class DanceClient {
    private DanceClient() {}

    private static final class Dancer {
        final DanceChoreographer.Cursor cursor;
        final long startTick;
        int lastIndex = -1;
        int age;

        Dancer(long seed, long startTick) {
            this.cursor = new DanceChoreographer.Cursor(seed);
            this.startTick = startTick;
        }
    }

    private static final Map<Integer, Dancer> DANCERS = new HashMap<>();

    /** Wires the client listeners (NeoForge.EVENT_BUS). */
    public static void register(IEventBus bus) {
        bus.addListener(DanceClient::onClientTick);
        bus.addListener(DanceClient::onLoggingOut);
    }

    /** True while this client believes the entity is dancing (used by the renderer for the ripple and after-image timing). */
    public static boolean dancing(int entityId) { return DANCERS.containsKey(entityId); }

    public static void receive(DanceStatePayload p) {
        if (p.on()) {
            DANCERS.put(p.entity(), new Dancer(p.seed(), p.startTick()));
        } else if (DANCERS.remove(p.entity()) != null) {
            CastAnimClient.receive(p.entity(), "");
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) { DANCERS.clear(); }

    private static void onClientTick(ClientTickEvent.Post e) {
        if (DANCERS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { DANCERS.clear(); return; }
        long now = mc.level.getGameTime();
        for (Iterator<Map.Entry<Integer, Dancer>> it = DANCERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Dancer> en = it.next();
            Entity ent = mc.level.getEntity(en.getKey());
            if (ent == null || !ent.isAlive()) { it.remove(); continue; }
            Dancer d = en.getValue();
            DanceChoreographer.Step step = d.cursor.at((int) Math.max(0, now - d.startTick));
            d.age++;
            // a new move, or a few ticks after the start (the page's own release clip may have just taken the arms): play the move's clip
            if (step.index() != d.lastIndex || d.age == 4) {
                d.lastIndex = step.index();
                CastAnimClient.receive(en.getKey(), step.move().clip);
            }
        }
    }
}
