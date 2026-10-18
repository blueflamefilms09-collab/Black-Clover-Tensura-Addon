package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: the SP Book is ONE registered item whose data component {@code nusmp:sp_kind} says which primer it is, and this table is the one place that knows the five
 * kinds, so the use logic, the creative tab, the dungeon drops, the model overrides (a sprite per kind) and the Black Market's shelves cannot disagree about
 * what a kind is worth. It is a closed enum in code, not a datapack list, on purpose: every kind needs its own sprite, name and flavour text in the resource
 * pack, which a datapack cannot add, whereas the NUMBERS (how many Skill Points a kind gives) live here as plain constants that the pure unit tests pin. The class
 * has no Minecraft type in it so the table and the drop pick are tested without a server. {@link #amount} is the guaranteed Skill Points of the kind; for the
 * gamble kind it is the FLOOR of the roll ({@link JackInTheBox}), which is exactly what the {@code nusmp:sp_amount} component of such a stack holds. The order of
 * the constants is the creative-tab order and the numbering of {@link #index}, which the item model's override list ({@code models/item/sp_book.json}) is written
 * against: new kinds are only ever appended at the end, never inserted and never renumbered (the same discipline as a saved format).
 *
 * <p>The two drop weights say how likely a kind is among the SP Books that fall in the Mega-Dungeon: an ordinary Abyssal monster mostly drops the cheap primers,
 * a decennial Guardian mostly drops the good ones, so killing the boss feels like more than a bigger roll of the same dice. Their means are about 3.9 SP for a
 * monster's book and about 5.8 SP for a Guardian's (see {@link #meanDrop} and the unit test), which at the default chances is roughly one SP per six to seven
 * Abyssal kills and about twelve SP per Guardian (two books on average): a route costs tens of SP, so the drops are a steady trickle and the shop is still the
 * way to buy a whole build.
 */
public enum SpKind {
    /** The cheapest primer: +2 SP. */
    BEGINNER_ARCHERY("beginner_archery", 2, 30, 5),
    /** +3 SP. */
    ELEMENTAL_PRIMER("elemental_primer", 3, 25, 15),
    /** +5 SP. */
    MAGIC_BOOSTER("magic_booster", 5, 20, 35),
    /** The gamble: 1 to 9 SP, a rare 20 ({@link JackInTheBox}). {@link #amount} is its floor. */
    JACK_IN_THE_BOX("jack_in_the_box", 1, 20, 20),
    /** The best primer: +10 SP. Dungeon loot only (the shop does not stock it). */
    GREATER_PRIMER("greater_primer", 10, 5, 25);

    private final String id;
    private final int amount, monsterWeight, bossWeight;

    SpKind(String id, int amount, int monsterWeight, int bossWeight) {
        this.id = id;
        this.amount = amount;
        this.monsterWeight = monsterWeight;
        this.bossWeight = bossWeight;
    }

    /** The lower-case snake id stored in the {@code nusmp:sp_kind} component and used in lang keys and model file names. */
    public String id() { return id; }

    /** The guaranteed Skill Points; for {@link #JACK_IN_THE_BOX} the floor of the gamble. */
    public int amount() { return amount; }

    /** Does this kind roll its SP instead of giving a fixed number? */
    public boolean isGamble() { return this == JACK_IN_THE_BOX; }

    /** 1-based position in the table, the number the item model's {@code nusmp:sp_kind} override predicates compare against. Never renumbered. */
    public int index() { return ordinal() + 1; }

    /**
     * The number the client's item property {@code nusmp:sp_kind} answers for this kind: {@link #index} divided by ten, so 0.1 to 0.5 (the game clamps a registered
     * property to 0..1, which is why it is not the plain index). The item model's overrides test "at least {@code modelValue() - 0.05}", a threshold half a step below
     * the value, so no float rounding can ever make a kind miss its own sprite.
     */
    public float modelValue() { return index() / 10f; }

    /** The weight of this kind among the SP Books an ordinary Abyssal monster drops. */
    public int monsterWeight() { return monsterWeight; }

    /** The weight of this kind among the SP Books a decennial Guardian drops. */
    public int bossWeight() { return bossWeight; }

    /** The kind with this id (case-sensitive, as stored), or null for null, empty and unknown ids. */
    public static SpKind byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (SpKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }

    /** The sum of the drop weights of every kind, for a boss or for a monster. */
    public static int totalWeight(boolean boss) {
        int sum = 0;
        for (SpKind k : values()) sum += boss ? k.bossWeight : k.monsterWeight;
        return sum;
    }

    /** The mean Skill Points of one drop of the given source: the weighted mean of the kinds' guaranteed amounts, with the gamble counted at its real mean. */
    public static double meanDrop(boolean boss) {
        double sum = 0;
        for (SpKind k : values()) sum += (boss ? k.bossWeight : k.monsterWeight) * (k.isGamble() ? JackInTheBox.mean() : k.amount);
        return sum / totalWeight(boss);
    }

    /** Picks the kind of one dropped SP Book by the weights of the source. Never null (the table is not empty and every total is positive). */
    public static SpKind pickDrop(Dice dice, boolean boss) {
        int r = dice.nextInt(totalWeight(boss));
        for (SpKind k : values()) {
            r -= boss ? k.bossWeight : k.monsterWeight;
            if (r < 0) return k;
        }
        return values()[values().length - 1];
    }
}
