package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: the Jack-in-the-Box is the one SP Book the owner wants to be a gamble ("Jack-in-the-Box" on the Black Market's shelf: the cheap book with a chance of a
 * jackpot), and a gamble is only fair and only fun if its odds are written down once and tested. This pure table is that: a floor (the guaranteed minimum, which
 * is what the stack's {@code nusmp:sp_amount} component holds, 1 by default), a 5% chance of a jackpot of 20 Skill Points, and otherwise 1 to 9 Skill Points
 * with the low numbers likelier than the high ones, weighted so that the whole box pays about 5.1 SP on average (the Magic Booster's price class) while any single
 * opening can be anywhere from the floor to the jackpot. The weights are a table of nine integers, not a formula, so the shape of the curve can be read and
 * changed in one place; the dice are the repo's {@link Dice} so a test replays any sequence. It has no Minecraft type in it and knows nothing of the item: the
 * item calls {@link #roll}, the self-test and the unit test call the same method.
 */
public final class JackInTheBox {
    private JackInTheBox() {}

    /** The smallest payout of a stack made without an explicit floor. */
    public static final int FLOOR = 1;
    /** The highest ordinary payout (the table covers 1 to this). */
    public static final int TOP = 9;
    /** The jackpot, in Skill Points. */
    public static final int JACKPOT = 20;
    /** The chance of the jackpot on any opening. */
    public static final double JACKPOT_CHANCE = 0.05;

    /** Relative weights of the payouts 1 to 9; the sum is 100, so they read as percentages of the 95% that is not the jackpot. */
    private static final int[] WEIGHTS = {14, 14, 14, 14, 12, 10, 8, 8, 6};

    /**
     * One opening. The first draw decides the jackpot, the second picks the ordinary payout. The result is never below {@code floor} (clamped to 1..9, so a hand-made
     * stack with a silly floor cannot turn the box into a fixed giver of thousands) and is exactly {@link #JACKPOT} on a jackpot.
     */
    public static int roll(int floor, Dice dice) {
        if (dice.next() < JACKPOT_CHANCE) return JACKPOT;
        int min = Math.max(FLOOR, Math.min(TOP, floor));
        int r = dice.nextInt(100);
        int payout = TOP;
        for (int i = 0; i < WEIGHTS.length; i++) {
            r -= WEIGHTS[i];
            if (r < 0) {
                payout = i + 1;
                break;
            }
        }
        return Math.max(min, payout);
    }

    /** Is this payout the jackpot? */
    public static boolean isJackpot(int payout) { return payout >= JACKPOT; }

    /** The exact mean payout of {@link #roll} with the default floor. */
    public static double mean() {
        double ordinary = 0;
        int total = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            ordinary += (double) (i + 1) * WEIGHTS[i];
            total += WEIGHTS[i];
        }
        return JACKPOT_CHANCE * JACKPOT + (1.0 - JACKPOT_CHANCE) * ordinary / total;
    }
}
