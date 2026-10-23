package com.newuniverse.nusmp.spiritblades;

import java.util.Map;
import java.util.UUID;

/**
 * Why: the numbers and decisions of the two Wind Spirit blades (Spirit of Zephyr, Spirit of Boreas) in one pure class (no game class), so a test can pin every cap. The blades are creative-display
 * items with no special magic: the sword is a netherite-level sword whose hits throw a few leaf-green sparkles, the bow is a vanilla bow whose arrows leave a short glowing trail. Both effects are
 * drawn from the server with ordinary particle packets that only reach players near the spot, and both are capped here: bursts per tick over the whole server, one burst per attacker every few
 * ticks, particles per burst, arrows trailed per owner and over the server, how long and how often an arrow is trailed. Nothing here allocates per tick beyond the particle call itself.
 */
public final class SpiritBladesRules {
    private SpiritBladesRules() {}

    // ---------------------------------------------------------------- the sword
    /** The sword's numbers: the base damage added to netherite's (3 + 4 = 7, a netherite sword's) and the attack speed modifier (-2.4 = 1.6 swings a second, a netherite sword's). */
    public static final float SWORD_BASE_DAMAGE = 3.0f, SWORD_SPEED = -2.4f;
    /** Sparkles in one hit burst, bursts per server tick (all attackers together) and the least ticks between two bursts of one attacker. */
    public static final int HIT_PARTICLES = 6, HIT_BURSTS_PER_TICK = 4, HIT_GAP_TICKS = 4;

    // ---------------------------------------------------------------- the bow
    /** The bow's durability: vanilla's bow (384). Its draw time, power and arrows are vanilla's: the class is a plain BowItem. */
    public static final int BOW_DURABILITY = 384;
    /** Arrows trailed at once per owner and over the whole server, ticks between two trail puffs, how long an arrow is trailed and particles per puff; puffs per server tick. */
    public static final int TRAIL_PER_OWNER = 4, TRAIL_GLOBAL = 32, TRAIL_EVERY = 3, TRAIL_MAX_AGE = 40, TRAIL_PARTICLES = 2, TRAIL_PUFFS_PER_TICK = 8;
    /** An arrow slower than this squared speed has stopped (stuck in a block or ground): its trail ends. */
    public static final double TRAIL_MIN_SPEED_SQ = 0.04;

    /** May another arrow be trailed, given the arrows already trailed for this owner and over the server? */
    public static boolean canTrack(int ownerCount, int globalCount) { return ownerCount < TRAIL_PER_OWNER && globalCount < TRAIL_GLOBAL; }

    /** Is this arrow, {@code age} ticks after the shot, due a puff of trail now? (The first puff is at once, then every {@link #TRAIL_EVERY} ticks, until {@link #TRAIL_MAX_AGE}.) */
    public static boolean puffDue(int age) { return age >= 0 && age <= TRAIL_MAX_AGE && age % TRAIL_EVERY == 0; }

    /** Is the trail over: too old, or the arrow has stopped? */
    public static boolean trailOver(int age, double speedSq) { return age > TRAIL_MAX_AGE || (age > 2 && speedSq < TRAIL_MIN_SPEED_SQ); }

    /** The worst number of trail particles the server can send in one tick: puffs per tick times particles per puff. */
    public static int worstTrailParticlesPerTick() { return TRAIL_PUFFS_PER_TICK * TRAIL_PARTICLES; }

    /** The worst number of hit particles the server can send in one tick: bursts per tick times particles per burst. */
    public static int worstHitParticlesPerTick() { return HIT_BURSTS_PER_TICK * HIT_PARTICLES; }

    // ---------------------------------------------------------------- the per-attacker gap
    /** The most attackers remembered (a map keyed by UUID, trimmed when it grows; also cleared when a player leaves). */
    public static final int MAX_REMEMBERED = 256;

    /**
     * May this attacker throw a hit burst at game tick {@code now}? At least {@link #HIT_GAP_TICKS} after its last burst. Records the burst when yes. The map is bounded: past
     * {@link #MAX_REMEMBERED} entries the entries older than the gap are dropped (a full-map scan at most once per overflow, never per hit).
     */
    public static boolean hitBurstAllowed(Map<UUID, Long> last, UUID attacker, long now) {
        Long prev = last.get(attacker);
        if (prev != null && now - prev < HIT_GAP_TICKS && now >= prev) return false;
        if (last.size() >= MAX_REMEMBERED) last.values().removeIf(t -> now - t >= HIT_GAP_TICKS || t > now);
        if (last.size() >= MAX_REMEMBERED) return false;
        last.put(attacker, now);
        return true;
    }
}
