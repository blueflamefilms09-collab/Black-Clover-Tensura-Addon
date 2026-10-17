package com.newuniverse.nusmp.arc.spade;

/**
 * Why: "kneel" is the one thing everybody remembers about Lucifero, and as a fight rule it has to be a demand the players can understand, meet and
 * fail without being cheated. The King orders the room to bow: whoever is crouching (holding sneak) when the weight falls has bowed of their own will and
 * is spared, whoever holds anti-magic is not forced (the gravity cannot get a grip on it), whoever stands inside a pocket of still air (the ultimate's
 * version) is spared, and everybody else is crushed to their knees. This class holds those rules, and the factor each outcome puts on the damage, as
 * pure logic so they are tested without a game. A player's lag is allowed for: crouching a few ticks before the fall still counts.
 */
public final class BowRules {
    private BowRules() {}

    /** What happened to one foe when the weight fell. */
    public enum Verdict {
        /** Crouching when it fell: bowed by choice. */
        BOWED,
        /** Holding anti-magic: the gravity finds nothing to grip. */
        UNBOWED,
        /** Standing in a pocket of still air. */
        POCKET,
        /** None of the above: crushed to the knees. */
        CRUSHED
    }

    /** Ticks of lag allowed between the last tick a foe was seen crouching and the fall. */
    public static final int DEFAULT_GRACE = 4;

    /** Did the foe bow: crouching at the fall, or within {@code grace} ticks before it? {@code lastCrouch} is -1 when never seen crouching. */
    public static boolean bowed(long fall, long lastCrouch, int grace) {
        return lastCrouch >= 0 && fall - lastCrouch <= Math.max(0, grace) && lastCrouch <= fall;
    }

    /** The verdict, the best excuse first: anti-magic, then a pocket, then a bow. */
    public static Verdict verdict(boolean antiMagic, boolean inPocket, boolean bowed) {
        if (antiMagic) return Verdict.UNBOWED;
        if (inPocket) return Verdict.POCKET;
        if (bowed) return Verdict.BOWED;
        return Verdict.CRUSHED;
    }

    /** The factor on the crushing damage for an ordinary Bow: nothing for anyone spared, all of it for the crushed. */
    public static double bowFactor(Verdict v) { return v == Verdict.CRUSHED ? 1.0 : 0.0; }

    /**
     * The factor on the damage of Kneel Before the King: the crushed take it all; anti-magic cuts it to 40% (the same cut as Dante's collapse); a pocket
     * leaves a quarter of it; a crouch alone does not help (the ultimate asks for more than a bow).
     */
    public static double kneelFactor(Verdict v) {
        return switch (v) {
            case UNBOWED -> 0.40;
            case POCKET -> 0.25;
            default -> 1.0;
        };
    }

    /** Is a foe pinned to the floor by this verdict in the ultimate? Only the crushed (the ultimate does not look at crouching at all). */
    public static boolean pinnedByKneel(Verdict v) { return v == Verdict.CRUSHED; }

    /** Is a foe pinned by an ordinary Bow? Only the crushed. */
    public static boolean pinnedByBow(Verdict v) { return v == Verdict.CRUSHED; }
}
