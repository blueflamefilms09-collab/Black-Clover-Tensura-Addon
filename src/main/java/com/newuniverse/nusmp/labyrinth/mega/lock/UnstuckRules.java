package com.newuniverse.nusmp.labyrinth.mega.lock;

/**
 * Why: whatever the dungeon does, a player must always be able to say "I am stuck" and be taken somewhere safe: a room the generator closed, a hole the pit trap left, a
 * lock whose rune they cannot find, a spawn in a room they cannot leave. {@code /dungeon unstuck} is that way out, and because a free teleport out of a fight would be an
 * exploit, it has a COOLDOWN and a COST (experience levels), and it refuses while hostile creatures are close, unless the player is really stuck. "Really stuck" is a fact
 * the escape rules know (the player stands inside a lock that is closed), and then the cost, the cooldown and the hostile test do not apply: nobody pays to be released from
 * something the dungeon did to them. The decisions are pure, so the cooldown arithmetic, the refusals and the choice of destination are lines in {@code UnstuckRulesTest}, and
 * the game class ({@link UnstuckService}) only gathers the facts and moves the player.
 */
public final class UnstuckRules {
    private UnstuckRules() {}

    /** The settings. {@code cooldownSeconds} per player, {@code xpLevels} paid by the player who types the command, {@code hostileRadius} in blocks (0 = no test). */
    public record Config(int cooldownSeconds, int xpLevels, int hostileRadius, int partyCooldownSeconds) {
        public static final Config DEFAULT = new Config(300, 3, 8, 600);

        public Config clamped() {
            return new Config(Math.max(0, Math.min(86400, cooldownSeconds)), Math.max(0, Math.min(100, xpLevels)), Math.max(0, Math.min(64, hostileRadius)),
                    Math.max(0, Math.min(86400, partyCooldownSeconds)));
        }
    }

    /** Why a use is refused. */
    public enum Refusal { NONE, COOLDOWN, NO_XP, HOSTILE_NEAR, NOT_IN_DUNGEON }

    /**
     * May the command be used now?
     *
     * @param inDungeon  the player is in the Mega-Dungeon
     * @param sealed     the player stands inside a closed lock (free, no cooldown, no hostile test)
     * @param ownLevels  the player's experience levels (creative players pass a large number)
     * @param hostileNear a hostile creature is within the configured radius
     * @param party      the party version (its own, longer cooldown)
     */
    public static Refusal check(Config config, long now, long lastUse, boolean inDungeon, boolean sealed, int ownLevels, boolean hostileNear, boolean party) {
        Config c = config.clamped();
        if (!inDungeon) return Refusal.NOT_IN_DUNGEON;
        if (sealed) return Refusal.NONE;
        long cd = (party ? c.partyCooldownSeconds() : c.cooldownSeconds()) * 20L;
        if (cd > 0 && lastUse > Long.MIN_VALUE / 4 && now - lastUse < cd) return Refusal.COOLDOWN;
        if (c.hostileRadius() > 0 && hostileNear) return Refusal.HOSTILE_NEAR;
        if (c.xpLevels() > 0 && ownLevels < c.xpLevels()) return Refusal.NO_XP;
        return Refusal.NONE;
    }

    /** The levels the player pays: none when stuck for real, else the configured cost. */
    public static int cost(Config config, boolean sealed) { return sealed ? 0 : config.clamped().xpLevels(); }

    /** Seconds of cooldown left (0 = ready). */
    public static long cooldownLeftSeconds(Config config, long now, long lastUse, boolean party) {
        Config c = config.clamped();
        long cd = (party ? c.partyCooldownSeconds() : c.cooldownSeconds()) * 20L;
        if (cd <= 0 || lastUse <= Long.MIN_VALUE / 4) return 0;
        long left = cd - (now - lastUse);
        return left <= 0 ? 0 : (left + 19) / 20;
    }

    /** Where the player is taken, best first. */
    public enum Destination { LAST_SAFE, NEAREST_SAFE, ARRIVAL_STAIRS, FORWARD_STAIRS, HUB }

    /**
     * Picks the destination from what is known: the room the player last stood in that is safe (a stairwell, a safe room) if it is still in the same sector as they
     * stand in, else the nearest safe room by doorways, else the arrival stairwell of the floor, else the forward stairwell, else out to the Hub (the dungeon is in a state
     * nobody can walk in).
     */
    public static Destination choose(boolean lastSafeUsable, boolean nearestSafeFound, boolean arrivalKnown, boolean forwardKnown) {
        if (lastSafeUsable) return Destination.LAST_SAFE;
        if (nearestSafeFound) return Destination.NEAREST_SAFE;
        if (arrivalKnown) return Destination.ARRIVAL_STAIRS;
        if (forwardKnown) return Destination.FORWARD_STAIRS;
        return Destination.HUB;
    }
}
