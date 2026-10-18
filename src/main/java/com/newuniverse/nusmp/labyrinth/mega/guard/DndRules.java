package com.newuniverse.nusmp.labyrinth.mega.guard;

import java.util.List;

/**
 * Why: the D&D layer of the Decennial bosses (legendary resistances, legendary actions, lair actions, a mythic second life, a telegraphed execution) is only fair if its numbers are facts that a
 * test can pin, so every rule that is arithmetic and not the game lives here as a plain function with no Minecraft type: the saving throw (d20 + a bonus against a fixed difficulty class, a natural
 * 20 always saves and a natural 1 always fails, as at the table), which profile a floor gets, how many legendary points a round holds, which legendary action is taken next, who may be
 * the target of the Word of Ending, and how much of a magical blow is eaten. The entity side ({@link DndBoss}) only asks these questions and does the world part. Every cap is here, so
 * a datapack that writes a silly number into a profile is clamped (at most five resistances, at most five legendary points) instead of obeyed.
 */
public final class DndRules {
    private DndRules() {}

    /** One round of the boss's turn order, in ticks (6 s): the legendary points come back at its start. */
    public static final int ROUND_TICKS = 120;
    /** The least gap between two legendary actions (the boss acts between the party's turns, not in a burst). */
    public static final int LEGENDARY_GAP = 15;
    /** Lair actions happen "on initiative count 20" of every round of this length (10 s). */
    public static final int LAIR_TICKS = 200;
    /** The difficulty class of every save the boss asks for. */
    public static final int DC = 15;
    /** The Word of Ending can only name a player whose health is at or under this share (the absolute execution threshold), takes this long to be spoken, and rests this long. */
    public static final double EXECUTION_THRESHOLD = 0.35;
    public static final int EXECUTION_CAST_TICKS = 60, EXECUTION_REST_TICKS = 1200;
    /** The Word is broken when the boss takes this share of its maximum health while speaking it. */
    public static final double EXECUTION_INTERRUPT = 0.03;
    /** Share of a magical blow that the boss eats (Acnologia's absorption) and turns into a shield. */
    public static final double ABSORB_SHARE = 0.25;
    /** Share of its health the boss returns to when its mythic trait spends itself. */
    public static final double MYTHIC_RESTORE = 0.5;
    public static final int MAX_RESISTANCES = 5, MAX_POINTS = 5;

    /** The legendary actions the code knows (a profile naming another is refused) with their cost in points. */
    public static final List<String> LEGENDARY_IDS = List.of("tail_sweep", "blink", "cantrip", "wing_buffet", "frightful_presence", "magic_drain");
    public static final List<String> LAIR_IDS = List.of("tremor", "anti_magic_field", "searing_light");

    /** A saving throw: true when {@code d20 + bonus >= dc}; a natural 20 always saves, a natural 1 never does. */
    public static boolean save(int dc, int d20, int bonus) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= dc;
    }

    /** A player's saving bonus: a point per four armour points and one per point of Luck, from 0 to 8. */
    public static int saveBonus(double armor, double luck) {
        return (int) Math.max(0, Math.min(8, Math.floor(Math.max(0.0, armor) / 4.0) + Math.floor(Math.max(0.0, luck))));
    }

    /** Which of the ten profiles of a side a floor gets: five tiers of 200 floors, two bosses to a tier (the first and the second hundred floors); 0 to 9. A floor past 1000 gets the last. */
    public static int profileIndex(int floor) {
        int f = Math.max(1, Math.abs(floor));
        int tier = Math.min(4, (f - 1) / 200);
        int variant = f > 1000 ? 1 : ((f - 1) % 200) / 100;
        return tier * 2 + variant;
    }

    /** The side of a floor: "holy" above Floor 0, "evil" below it; the hub itself reads as holy. */
    public static String side(int floor) { return floor < 0 ? "evil" : "holy"; }

    /** The id of the profile a floor gets: {@code holy_t3b}. */
    public static String idForFloor(int floor) {
        int index = profileIndex(floor);
        return side(floor) + "_t" + (index / 2 + 1) + (index % 2 == 0 ? "a" : "b");
    }

    public static int clampResistances(int n) { return Math.max(0, Math.min(MAX_RESISTANCES, n)); }

    /** Legendary points per round: the profile's, one more after the mythic change, clamped. */
    public static int points(int profilePoints, boolean mythic) { return Math.max(1, Math.min(MAX_POINTS, profilePoints + (mythic ? 1 : 0))); }

    /**
     * The next legendary action to take: the first affordable one after {@code last} in the profile's order, wrapping round (so the boss cycles through its list instead of repeating
     * its first move); -1 when it can afford none. {@code costs} are the points of each action in the profile's order.
     */
    public static int nextAction(int[] costs, int points, int last) {
        int n = costs.length;
        for (int k = 1; k <= n; k++) {
            int i = Math.floorMod(last + k, n);
            if (costs[i] <= points) return i;
        }
        return -1;
    }

    /** May the Word of Ending name a player with this health? (Alive, and at or under the threshold.) */
    public static boolean executionTarget(double health, double maxHealth) { return health > 0.0 && maxHealth > 0.0 && health <= maxHealth * EXECUTION_THRESHOLD; }

    /** Does the Word still land at the end of the cast? The target must still be in the threshold, in sight, and the cast not broken by damage. */
    public static boolean executionLands(double health, double maxHealth, boolean inSight, double damageTakenWhileSpeaking, double bossMaxHealth) {
        return executionTarget(health, maxHealth) && inSight && damageTakenWhileSpeaking < bossMaxHealth * EXECUTION_INTERRUPT;
    }

    /** The part of a magical blow the boss eats: {@code {what still hurts, what becomes shield}}. */
    public static float[] absorb(float amount, boolean magical) {
        if (!magical || amount <= 0f) return new float[] {amount, 0f};
        float eaten = (float) (amount * ABSORB_SHARE);
        return new float[] {amount - eaten, eaten};
    }

    /** The phase from health: 1 above half, 2 under half, 3 once the mythic trait has been spent. */
    public static int phase(double health, double maxHealth, boolean mythicUsed) {
        if (mythicUsed) return 3;
        return maxHealth > 0 && health < maxHealth * GuardianScaling.PHASE_TWO_AT ? 2 : 1;
    }
}
