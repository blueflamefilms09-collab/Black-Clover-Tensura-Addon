package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: a monster's Howl is the DanMachi way of saying "you are not ready for me": it paralyses adventurers of a lower Level with fear, and Welf's anti-magic
 * sword negates it in the story. The mod has no Falna, so the rule is built from the Dungeon Level (see {@link DungeonLevel}) and the anti-magic that the
 * boss framework already knows: a fighter is afraid when they stand inside the ring, their Level is below the monster's and they hold no anti-magic.
 * The fear is a slowing and weakening, never a hard stop (a hard stop would have to go through the balance law's control budget); how strong it is grows with
 * how far below the monster the fighter is. Pure, so the rule is tested and the boss class only applies the result.
 */
public final class HowlMath {
    private HowlMath() {}

    /** Base ticks of fear, before the balance law caps it for the victim. */
    public static final int BASE_FEAR_TICKS = 50;

    /** The ring of a Howl: the move's reach, half a block wider for every power step. */
    public static double radius(double reach, int power) { return reach + 0.5 * PowerCurve.stage(power); }

    /** Ticks of fear before the cap: 50 at the opening, 6 more for every power step. */
    public static int fearTicks(int power) { return BASE_FEAR_TICKS + 6 * PowerCurve.stage(power); }

    /**
     * Is this fighter frightened by the Howl? Only inside the ring, only below the monster's Level, never with anti-magic in hand, and never when they are
     * not a real fighter (a creative player or a spectator).
     */
    public static boolean afraid(int monsterLevel, int fighterLevel, boolean antiMagic, boolean realFighter, double distance, double radius) {
        return realFighter && !antiMagic && distance <= radius && DungeonLevel.frightens(monsterLevel, fighterLevel);
    }

    /** The Slowness amplifier of the fear: II (1) one Level below, III (2) two or three below, IV (3) four or more. */
    public static int slowAmplifier(int monsterLevel, int fighterLevel) {
        int gap = Math.max(1, monsterLevel - fighterLevel);
        return Math.min(3, 1 + gap / 2);
    }
}
