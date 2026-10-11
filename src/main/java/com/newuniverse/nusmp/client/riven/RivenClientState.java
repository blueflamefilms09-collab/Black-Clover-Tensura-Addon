package com.newuniverse.nusmp.client.riven;

import com.newuniverse.nusmp.entity.riven.RivenStatePayload;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** What the boss bar knows about Marquis Remake: the last payload, interpolated on the client clock. */
public final class RivenClientState {
    private RivenClientState() {}

    public static final class State {
        public int phase = 1, story, castTicks, maxConstructs;
        public float hp = 1f, hpShown = 1f;
        public String cast = "", combatType = "", grimoire = "", status = "";
        public long castStart = Long.MIN_VALUE, castEnd = Long.MIN_VALUE, spawnedAt;
        public String lastCast = "";
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    public static State get(int entity) { return STATES.get(entity); }

    public static void accept(RivenStatePayload p) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        State s = STATES.computeIfAbsent(p.entity(), k -> { State n = new State(); n.spawnedAt = now; n.hp = n.hpShown = p.hp(); return n; });
        s.phase = p.phase();
        s.hp = p.hp();
        s.story = p.story();
        s.maxConstructs = p.maxConstructs();
        s.combatType = p.combatType();
        s.grimoire = p.grimoire();
        s.status = p.status();
        if (!p.cast().isEmpty()) {
            if (!p.cast().equals(s.cast) || now > s.castEnd) s.castStart = now;
            s.cast = p.cast();
            s.lastCast = p.cast();
            s.castEnd = now + Math.max(1, p.castTicks());
        } else s.cast = "";
        if (STATES.size() > 8) STATES.keySet().removeIf(id -> mc.level != null && mc.level.getEntity(id) == null);
    }
}
