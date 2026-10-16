package com.newuniverse.nusmp.boss.god;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Time Stasis Traps are the answer to dodge spam, and the owner capped them hard: at most {@value #MAX_TRAPS} at once, each alive
 * for 12 seconds, armed only after a visible warning, and a player can be caught by traps only once every 12 seconds. The rules live in
 * this pure ledger (no Minecraft types, unit tested); the glue draws the rings and freezes whoever the ledger says was caught.
 */
public final class TrapLedger {
    public static final int MAX_TRAPS = 6, LIFE_TICKS = 240, ARM_TICKS = 20, PLAYER_COOLDOWN = 240;
    public static final double RADIUS = 1.8, HEIGHT = 2.0;

    /** One trap. 'armedAt' is when it starts to catch; 'dieAt' when it is gone. */
    public record Trap(int id, double x, double y, double z, long bornAt, long armedAt, long dieAt) {
        public boolean live(long now) { return now < dieAt; }

        public boolean armed(long now) { return now >= armedAt && now < dieAt; }
    }

    private final List<Trap> traps = new ArrayList<>();
    private final CounterRules.Cooldowns<Object> caught = new CounterRules.Cooldowns<>();
    private int nextId = 1;

    /** Lays a trap now, or returns null when {@value #MAX_TRAPS} are already live. Expired traps are dropped first. */
    public Trap lay(double x, double y, double z, long now, int lifeTicks) {
        traps.removeIf(t -> !t.live(now));
        if (traps.size() >= MAX_TRAPS) return null;
        int life = Math.max(ARM_TICKS + 1, Math.min(LIFE_TICKS * 2, lifeTicks));
        Trap t = new Trap(nextId++, x, y, z, now, now + ARM_TICKS, now + life);
        traps.add(t);
        return t;
    }

    public List<Trap> live(long now) {
        traps.removeIf(t -> !t.live(now));
        return List.copyOf(traps);
    }

    public int liveCount(long now) { return live(now).size(); }

    /**
     * The armed trap that catches a body at (x, y, z) for 'player' now, or null: inside the radius, within the height, and the player
     * has not been caught in the last {@value #PLAYER_COOLDOWN} ticks. A catch removes that trap and starts the player's cooldown.
     */
    public Trap catchAt(Object player, double x, double y, double z, long now) {
        if (!caught.ready(player, now)) return null;
        for (Trap t : traps) {
            if (!t.armed(now)) continue;
            double dx = x - t.x(), dz = z - t.z();
            if (dx * dx + dz * dz > RADIUS * RADIUS || Math.abs(y - t.y()) > HEIGHT) continue;
            caught.use(player, now, PLAYER_COOLDOWN);
            traps.remove(t);
            return t;
        }
        return null;
    }

    /** Is a trap already lying within 'range' of this spot (a second one on the same dodger would be a waste)? */
    public boolean near(double x, double z, long now, double range) {
        for (Trap t : live(now)) if ((x - t.x()) * (x - t.x()) + (z - t.z()) * (z - t.z()) <= range * range) return true;
        return false;
    }

    public void clear() {
        traps.clear();
        caught.clear();
    }
}
