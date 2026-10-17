package com.newuniverse.nusmp.arc.zagred;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: every number of Zagred's fight in one pure place. The Words have sizes (how wide a ring, how many swords, how many letters, how many pages he
 * calls) that grow with the framework's power steps and with the number of fighters, and the boss class, the telegraph and the tests must all agree on them
 * or a telegraph would draw a ring the blow does not match. A number lives here once; the boss class only asks.
 *
 * <p>"Stage" is the framework's power step (0 at 20%, 1 at 40%, 2 at 60%, 3 at 80%, 4 once the Gates are open), see {@link PowerCurve#stage}. The damage
 * numbers are BASES: the boss law (power tuning, the victim's EP, the balance law's cap of a sliver of a player's health) is applied afterwards by
 * {@code ZogratisBoss.bossDamage}, so none of these is a number a player ever sees.
 */
public final class ZagredMath {
    private ZagredMath() {}

    // ---------------------------------------------------------------- base damage (before the boss law)
    public static final float CLAW = 15f, DEVIL_CLAW = 20f, HALT = 6f, FALL = 11f, TRIDENT = 13f, SWORD = 6.5f, SEAL = 5f, LETTER = 9f, BANISH = 6f,
            RULE_BREAK = 3f, VERDICT_FALL = 11f, VERDICT_SWORDS = 10f, VERDICT_HALT = 6f, VERDICT_SEAL = 5f, VERDICT_COWER = 6f, LAST_LETTER_FACTOR = 1.3f;

    /** The act (1 to 5) of a power value: I the Reader, II the Speaker, III the Judge, IV the Author, V the Heart (the Gates). */
    public static int act(int power) { return PowerCurve.stage(power) + 1; }

    /** The name of an act, for the boss bar and the chat. */
    public static String actName(int act) {
        return switch (act) {
            case 1 -> "the Reader";
            case 2 -> "the Speaker";
            case 3 -> "the Judge";
            case 4 -> "the Author";
            default -> "the Heart";
        };
    }

    // ---------------------------------------------------------------- Halt
    /** The inner edge of the Halt band: standing this close to him is safe (a melee fighter never has to run). */
    public static double haltCore() { return 3.5; }

    /** The outer edge of the Halt band, 9 blocks at the start and 13 at the Gates: further out than this is safe. */
    public static double haltOuter(int stage) { return 9 + Math.max(0, Math.min(4, stage)); }

    /** Ticks of the bind before the control law shortens it (the law caps it at three seconds for a player and halves it on a repeat). */
    public static int haltBindTicks() { return 30; }

    // ---------------------------------------------------------------- Fall
    /** How many rings the crushing wave has (eight, up to twelve at the Gates). */
    public static int fallRings(int stage) { return 8 + Math.max(0, Math.min(4, stage)); }

    /** The radius of the k-th ring (k from 0): the wave moves out 1.6 blocks every two ticks. */
    public static double fallRingRadius(int k) { return 2.0 + 1.6 * Math.max(0, k); }

    /** Half the width of the band a ring hurts in. */
    public static double fallBand() { return 1.3; }

    /** The highest a foe's feet may be above the floor and still be hit by the wave (a jump clears it). */
    public static double fallHeight() { return 0.8; }

    /** Blocks around him in which fliers are thrown to the ground. */
    public static double fallPullReach(int stage) { return 16 + Math.max(0, Math.min(4, stage)); }

    // ---------------------------------------------------------------- Swords, Trident, Seal
    /** How many swords fall: eight with one cast slot, fourteen with four. */
    public static int swordCount(int castSlots) { return 6 + 2 * Math.max(1, Math.min(4, castSlots)); }

    /** The radius of the field the swords are scattered over, round the marked foe. */
    public static double swordSpread(int castSlots) { return 3.5 + 1.2 * Math.max(1, Math.min(4, castSlots)); }

    /** The radius of one sword's impact. */
    public static double swordImpact() { return 1.8; }

    /** Half the width of the trident's lane. */
    public static double tridentHalfWidth() { return 1.3; }

    /** How far the trident flies. */
    public static double tridentLength() { return 30; }

    /** Half the width of the seal's chain. */
    public static double sealHalfWidth() { return 0.9; }

    // ---------------------------------------------------------------- Redact
    /** Letters of one sentence: five, six from the Judge, seven from the Author, nine at the Gates. */
    public static int redactLetters(int stage) {
        int s = Math.max(0, Math.min(4, stage));
        return 5 + (s >= 2 ? 1 : 0) + (s >= 3 ? 1 : 0) + (s >= 4 ? 2 : 0);
    }

    /** Sentences written at once: one, two at the Gates. */
    public static int sentences(boolean gatesOpen) { return gatesOpen ? 2 : 1; }

    /** Ticks between two letters going off. */
    public static int letterGap() { return 6; }

    /** The blast radius of a letter; the last one is larger. */
    public static double letterRadius(boolean last) { return last ? 4.0 : 2.5; }

    // ---------------------------------------------------------------- Banish, Shatter, Reverse
    public static double banishRadius() { return 8.0; }

    /** Foes (players and their summons) within seven blocks of him that make a crowd. */
    public static int crowdSize() { return 4; }

    public static double crowdRadius() { return 7.0; }

    public static int shatterTicks() { return 50; }

    public static double shatterRadius() { return 14.0; }

    public static int reverseTicks() { return 80; }

    /** The share of a blow that comes back on the one who struck while Reverse holds (the same half the players' word turns back). */
    public static float reflectShare() { return 0.5f; }

    // ---------------------------------------------------------------- Overwrite
    public static int ruleTicks() { return 240; }

    public static int stones() { return 5; }

    public static int stonesNeeded() { return 3; }

    /** Ticks he is staggered when three stones cut the sentence short. */
    public static int staggerAfterStones() { return 40; }

    // ---------------------------------------------------------------- the ward
    /** Layers of the ward: three, four from the Author, none at the Gates (the heart takes its place). */
    public static int wardLayers(int stage) {
        int s = Math.max(0, Math.min(4, stage));
        return s >= 4 ? 0 : s == 3 ? 4 : 3;
    }

    /** The share of his maximum health one layer holds. */
    public static float wardLayerShare() { return 0.045f; }

    // ---------------------------------------------------------------- the pages (grimoire daemons)
    /** How many players the packs are scaled for: 1.0 for a pair, 0.6 + 0.4 each, at most 1.5. */
    public static double partyScale(int players) { return Math.min(1.5, 0.6 + 0.4 * Math.max(1, players)); }

    /** Lesser pages alive at most: 3, 4, 6, 8 by act II to V, scaled by the party; none in the first act. */
    public static int lesserCap(int stage, int players) {
        int base = switch (Math.max(0, Math.min(4, stage))) { case 0 -> 0; case 1 -> 3; case 2 -> 4; case 3 -> 6; default -> 8; };
        return base == 0 ? 0 : Math.max(1, (int) Math.round(base * partyScale(players)));
    }

    /** Greater pages (the turrets) alive at most: from the Judge on. */
    public static int greaterCap(int stage, int players) {
        int base = switch (Math.max(0, Math.min(4, stage))) { case 0, 1 -> 0; case 2 -> 1; case 3 -> 2; default -> 3; };
        return base == 0 ? 0 : Math.max(1, (int) Math.round(base * partyScale(players)));
    }

    /** Arch daemons (the anchors): one from the Author, two at the Gates. */
    public static int archCap(int stage) { return stage >= 4 ? 2 : stage >= 3 ? 1 : 0; }

    /** Pages one cast of Call the Pages brings (before the caps). */
    public static int pagesPerCall(int stage) { return stage <= 1 ? 2 : stage == 2 ? 3 : 4; }

    // ---------------------------------------------------------------- the rule's punishment
    /** Ticks of the rule breaker's grace: the rule bites after a full second of breaking it. */
    public static int ruleGraceTicks() { return 20; }

    // ---------------------------------------------------------------- the threat swap
    /** Ticks between two looks at who has hurt him most (from the Judge on he turns on the top of the list). */
    public static int threatSwapTicks() { return 160; }
}
