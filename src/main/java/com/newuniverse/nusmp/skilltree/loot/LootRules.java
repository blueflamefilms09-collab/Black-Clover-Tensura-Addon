package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: the owner wants the Skill Books to be a REWARD OF THE DEEP: the hidden knowledge of the Abyssal descent, trickling out of its monsters at a very low chance
 * and falling out of its decennial Guardians at a high one, and nowhere else (the Holy floors have their own loot, the other dimensions none of this). A rule that
 * rare and that easy to get subtly wrong (a Holy floor that drops, a boss that drops nothing, a chance read as a percent instead of a fraction) must be one small
 * pure function, not scattered through event handlers, so every entry point that can add a book (the global loot modifier for the monsters that roll a loot table,
 * the death listener for the Guardian that does not, the self-test, the unit test) asks {@link #decide} the same question with the same facts and gets the same
 * answer. The facts are a {@link Victim} (where, what, killed by whom: all read from the live game by the glue code and passed in as plain values, so this class
 * needs no server) and the {@link Chances} (the server's config); the answer is a {@link Plan}: whether a route book is added and how many SP Books. It decides only
 * WHETHER; which route and which kinds are picked by {@link RoutePick} and the SP Book kinds (gone in 1.96.0), so the odds of the
 * drop and the shape of the drop can be tuned and tested apart. The order of the dice draws (route book first, then the SP Books) is part of the contract because
 * the unit tests replay scripted dice.
 *
 * <p>The decennial rule reuses the dungeon's own pure {@link FloorMap} (a boss floor is a non-zero multiple of ten) and {@link Polarity} instead of copying them, so
 * the skill books follow the dungeon if its floor model ever changes. The brief and the design say the books fall on ABYSSAL floors only and that "on Holy floors
 * nothing drops"; the owner's own words ("Abyssal monsters, Decennial bosses") can also be read as including the Holy bosses, so that single case is a config switch
 * ({@link Chances#holyBosses}, default off, the design) rather than a hard-wired no.
 */
public final class LootRules {
    private LootRules() {}

    /** What the dying thing is, as far as the drop is concerned. */
    public enum Kind {
        /** Anything that does not pay (an NPC, a pet, a boss that is not a sector Guardian, a servant of a player...). */
        OTHER,
        /** An ordinary hostile monster of the dungeon: a vanilla-style {@code Enemy} or one of the Dungeon Master's creatures, neither a boss nor anyone's servant. */
        MONSTER,
        /** The Guardian of a sector (the colossal boss that gates a floor). */
        GUARDIAN
    }

    /**
     * The facts of one death.
     *
     * @param inMega     did it die in the Mega-Dungeon dimension
     * @param playerKill was it killed (or last hurt) by a player
     * @param kind       what it was
     * @param polarity   the road of the sector it stood in ({@code ABYSSAL}, {@code HOLY}) or null outside any sector; for a Guardian the road of the sector it guards
     * @param floor      the signed floor it stood on; for a Guardian the last floor of the sector it guards (the boss floor); 0 when unknown
     */
    public record Victim(boolean inMega, boolean playerKill, Kind kind, Polarity polarity, int floor) {}

    /**
     * The odds, from the server config. Probabilities are clamped to 0..1 (NaN becomes 0), the Guardian's SP Book count to 0..MAX_BOSS_SP with
     * {@code bossSpMax >= bossSpMin}, so no config value can make the rule misbehave. The Guardian ceiling is {@value LootRules#MAX_BOSS_SP}.
     *
     * @param abyssalBook chance that an Abyssal monster adds a route book
     * @param abyssalSp   chance that an Abyssal monster adds one SP Book
     * @param bossBook    chance that a decennial Guardian adds a route book
     * @param bossSpMin   fewest SP Books a decennial Guardian always adds
     * @param bossSpMax   most SP Books a decennial Guardian always adds
     * @param holyBosses  do the Guardians of Holy boss floors drop too (the design says no)
     */
    public record Chances(double abyssalBook, double abyssalSp, double bossBook, int bossSpMin, int bossSpMax, boolean holyBosses) {
        public Chances {
            abyssalBook = unit(abyssalBook);
            abyssalSp = unit(abyssalSp);
            bossBook = unit(bossBook);
            bossSpMin = Math.max(0, Math.min(MAX_BOSS_SP, bossSpMin));
            bossSpMax = Math.max(bossSpMin, Math.min(MAX_BOSS_SP, bossSpMax));
        }

        /** The numbers of the design: 0.4% and 4% for the monsters, 30% and 1 to 3 SP Books for the Guardian, Holy bosses off. */
        public static final Chances DEFAULT = new Chances(0.004, 0.04, 0.30, 1, 3, false);

        private static double unit(double p) { return p > 0.0 ? Math.min(1.0, p) : 0.0; }
    }

    /** The most SP Books one Guardian can add (a config sanity ceiling). */
    public static final int MAX_BOSS_SP = 10;

    /**
     * What to add to the drops.
     *
     * @param routeBook add one route book
     * @param spBooks   add this many SP Books
     * @param boss      the drop comes from a decennial Guardian (the SP Books then use the Guardian's weights)
     */
    public record Plan(boolean routeBook, int spBooks, boolean boss) {
        /** Nothing to add. */
        public static final Plan NOTHING = new Plan(false, 0, false);

        public boolean isEmpty() { return !routeBook && spBooks <= 0; }
    }

    /** Decides one death. Pure; the order of the draws is: the route book, then the SP Books. */
    public static Plan decide(Victim v, Chances c, Dice dice) {
        if (v == null || c == null || dice == null || !v.inMega() || !v.playerKill()) return Plan.NOTHING;
        switch (v.kind()) {
            case MONSTER: {
                if (v.polarity() != Polarity.ABYSSAL) return Plan.NOTHING;
                boolean book = dice.chance(c.abyssalBook());
                int sp = dice.chance(c.abyssalSp()) ? 1 : 0;
                return new Plan(book, sp, false);
            }
            case GUARDIAN: {
                if (!FloorMap.isBossFloor(v.floor())) return Plan.NOTHING;
                boolean road = v.polarity() == Polarity.ABYSSAL || (v.polarity() == Polarity.HOLY && c.holyBosses());
                if (!road) return Plan.NOTHING;
                boolean book = dice.chance(c.bossBook());
                int sp = c.bossSpMin() + dice.nextInt(c.bossSpMax() - c.bossSpMin() + 1);
                return new Plan(book, sp, true);
            }
            default:
                return Plan.NOTHING;
        }
    }
}
