package com.newuniverse.nusmp.relics.wave;

/**
 * The Cape of Nine Lives' state: how many lives the cat spirit has left and how feline the wearer has become. Pure, no Minecraft imports;
 * the game side keeps it in the cape's own custom data.
 *
 * @param left lives left, 9 down to 0 (at 0 the cape is spent: the "Feline Fate")
 * @param cat  how many times it has saved the wearer since the curse was last lifted, 0 to 9
 */
public record Wave2Lives(int left, int cat) {
    public static final int MAX = 9;

    public Wave2Lives {
        left = Math.max(0, Math.min(MAX, left));
        cat = Math.max(0, Math.min(MAX, cat));
    }

    public static Wave2Lives fresh() { return new Wave2Lives(MAX, 0); }

    public boolean alive() { return left > 0; }

    /** One life used: the wearer is a little more of a cat. */
    public Wave2Lives afterSave() { return new Wave2Lives(left - 1, cat + 1); }

    /** The ninth life has just gone. */
    public boolean justSpent(Wave2Lives before) { return before.left() > 0 && left == 0; }

    /** Remove Curse: the cat traits go, the spent lives do not come back. */
    public Wave2Lives cured() { return new Wave2Lives(left, 0); }

    /** A wish (a Nether Star): nine lives and no cat traits. */
    public Wave2Lives wished() { return fresh(); }

    /** Half a heart off the maximum health for each cat trait, at most four hearts. */
    public double healthPenalty() { return cat == 0 ? 0.0 : -0.5 * Math.min(8, cat); }

    /** Night eyes come with the first whisker. */
    public boolean catEyes() { return cat >= 1; }

    /** From the fourth trait the wearer starts knocking things over. */
    public boolean knocksThingsOver() { return cat >= 4; }

    /** The cat's paw: 20% at four traits, 5% more with each further one. */
    public double knockChance() { return knocksThingsOver() ? 0.20 + 0.05 * (cat - 4) : 0.0; }
}
