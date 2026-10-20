package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Wave 6: every number of the ten armours and robes in one place (pure, no Minecraft types), with the small formulas that carry the
 * tabletop-to-game translations: dice become the pool gains, "advantage on saves" becomes a chance, a "saving throw" becomes the
 * distance from the flash. The Minecraft glue reads these; the tests pin them.
 */
public final class Wave6Tune {
    private Wave6Tune() {}

    // ---------------------------------------------------------------- the flags the relics carry (read with RelicFlags.has(player, key))
    public static final String F_SKY = "w6_sky", F_SLIME = "w6_slime_skin", F_DAWN = "w6_dawn", F_ASTRAL = "w6_astral", F_SHADOW = "w6_shadow",
            F_NEKET = "w6_neket", F_PLOT = "w6_plot", F_ZAR = "w6_zar";

    // ---------------------------------------------------------------- the ids of the ten relics
    public static final String ID_SKY = "skycrystal_armor", ID_SLIME = "slime_armour", ID_DAWN = "armor_of_the_defiant_dawn", ID_ASTRAL = "suit_of_the_astral_overseer",
            ID_SHADOW = "shadow_robes", ID_MIST = "mistwalker_robe", ID_NEKET = "nekets_practical_mage_robe", ID_PLOT = "armor_of_plot",
            ID_ZAR = "zar_sancuis_plate_of_the_red_prophet", ID_COAT = "skitchs_smuggling_coat";

    // ---------------------------------------------------------------- Skycrystal Armor
    public static final int SKY_MAX = 10, SKY_ABSORB_ELEMENTS = 1, SKY_SHIELD = 1, SKY_GLOBE = 6, SKY_INVULN = 9;
    public static final double SKY_WARD_FACTOR = 0.75;       // magic damage taken x0.75 ("advantage on saves against spells")
    public static final double SKY_EFFECT_SAVE = 0.30;       // chance a harmful magical effect is shrugged off
    public static final int SKY_ABSORB_COOLDOWN = 100;       // ticks between two Spell Absorptions
    public static final int GLOBE_TICKS = 200, INVULN_TICKS = 100;
    public static final double GLOBE_RADIUS = 5.0;

    /** 1d8+2 at dawn. */
    public static int skyDawnGain(IntUnaryOperator roll) { return Wave6Pool.dice(1, 8, roll) + 2; }

    /** Spending the last charge: a d10; on a 10 the armour regains 1d4+1 at once. */
    public static int skyLastChargeGain(IntUnaryOperator roll) { return roll.applyAsInt(10) == 9 ? Wave6Pool.dice(1, 4, roll) + 1 : 0; }

    /** An absorbed spell gives charges equal to its "level": one per 5 damage, 1 to 4. */
    public static int absorbGain(float damage) { return Math.max(1, Math.min(4, (int) Math.ceil(damage / 5.0))); }

    // ---------------------------------------------------------------- Slime Armour
    public static final int SLIME_USES = 3, SLIME_TICKS = 1200, SLIME_EXTEND_MAX = 5;
    public static final double SLASH_FACTOR = 0.65, BLUNT_FACTOR = 1.30;
    public static final double SLIME_SCALE = -0.55;           // ADD_MULTIPLIED_TOTAL on the scale attribute: x0.45
    public static final double SLIME_KNOCKBACK = 0.6;

    // ---------------------------------------------------------------- Armor of the Defiant Dawn
    public static final int DAWN_MAX = 3, DAWN_FLASH_COOLDOWN = 40;
    public static final double DAWN_REVEAL_RADIUS = 16.0, DAWN_FLASH_RADIUS = 6.0, DAWN_CLOSE = 3.0;
    public static final float DAWN_FLASH_DAMAGE = 6f;
    public static final int DAWN_BLIND_TICKS = 80;

    /** 1d3 at dawn. */
    public static int dawnGain(IntUnaryOperator roll) { return Wave6Pool.dice(1, 3, roll); }

    /** The "saving throw" of the flash: the close, and always the undead ("disadvantage"), fail it. */
    public static boolean failsSave(boolean undead, double dist) { return undead || dist <= DAWN_CLOSE; }

    /** "4d8 radiant, half on a save": the full flash for those who fail, half for the rest; the undead take half again on top. */
    public static float flashDamage(boolean undead, double dist) {
        float d = DAWN_FLASH_DAMAGE * (undead ? 1.5f : 1f);
        return failsSave(undead, dist) ? d : d * 0.5f;
    }

    /** Blinded only when the save failed. */
    public static int flashBlind(boolean undead, double dist) { return failsSave(undead, dist) ? DAWN_BLIND_TICKS : 0; }

    // ---------------------------------------------------------------- Suit of the Astral Overseer
    public static final int ASTRAL_VOID_USES = 2, ASTRAL_VOID_COOLDOWN = 20;
    public static final float FLY_SPEED = 0.10f, WALK_FLY_SPEED = 0.05f;
    public static final float FLIGHT_EXHAUSTION = 0.22f;      // per half second of flight (about the cost of sprinting)
    public static final int FLIGHT_FOOD_MIN = 4;
    /** 1.99.0, THE HUNGER BAN: flight costs magicules now (the two constants above are the old food cost, unused). Share of the maximum pool per half second of flight (about 14 minutes from a full pool), and the share needed to take off. */
    public static final double FLIGHT_MANA = 0.0006, FLIGHT_MANA_MIN = 0.01;

    // ---------------------------------------------------------------- Shadow Robes
    public static final int SHADOW_USES = 2, DIM_MAX = 7, SHADOW_RANGE = 12, SHADOW_MIN = 2;
    public static final int CLOUD_PULSES = 3, CLOUD_PERIOD = 20, CLOUD_WITHER_TICKS = 60, CLOUD_CAP = 24;
    public static final float CLOUD_PULSE_DAMAGE = 2f;
    public static final double CLOUD_RADIUS = 2.5;
    public static final double DRAG_RADIUS = 3.0;

    /** "Dim light or darkness": block-or-sky light of 7 or less. */
    public static boolean isDim(int light) { return light <= DIM_MAX; }

    /** The farthest distance (blocks along the look ray) that {@code valid} accepts, scanning down from {@code max}; -1 when none does. */
    public static int pickDistance(int max, int min, IntPredicate valid) {
        for (int d = max; d >= min; d--) if (valid.test(d)) return d;
        return -1;
    }

    // ---------------------------------------------------------------- Mistwalker Robe
    public static final int MIST_MAX = 5, MIST_STEP = 1, MIST_LEVITATE = 1, MIST_GASEOUS = 2, MIST_FAR = 4, MIST_WIND = 5;
    public static final double MISTY_RANGE = 6.0, FAR_RANGE = 14.0;

    /** 1d4+1 at dawn. */
    public static int mistGain(IntUnaryOperator roll) { return Wave6Pool.dice(1, 4, roll) + 1; }

    /** Spending the last charge: a d20, a 1 and the mist slips ("the robe teleports away", here: it throws the wearer a short way instead, never the item). */
    public static boolean mistMisfire(IntUnaryOperator roll) { return roll.applyAsInt(20) == 0; }

    // ---------------------------------------------------------------- Neket's Practical Mage Robe
    public static final int MAGE_MAX = 5, MAGE_TICKS = 9600, MAGE_STORE_MANA = 8;
    public static final double MAGE_ARMOR = 4.0;

    // ---------------------------------------------------------------- Armor of Plot
    public static final int PLOT_MAX = 3, PLOT_GRACE_TICKS = 100;
    public static final double PLOT_LUCK = -3.0;

    /** "When hit, negates all damage... at the last second": a few percent, rising as the story gets desperate (hp 100% -> 6%, hp 0% -> 33%). */
    public static double plotDodge(float hp, float maxHp) {
        double lost = maxHp <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, 1.0 - hp / maxHp));
        return 0.06 + 0.27 * lost;
    }

    /** Health left after the death save: a third of the bar, at least 6. */
    public static float plotSaveHealth(float maxHp) { return Math.max(6f, maxHp / 3f); }

    // ---------------------------------------------------------------- Z'ar Sancuis
    public static final int ZAR_USES = 2, ZAR_WINDOW = 120, ZAR_ALLY_RANGE = 18;
    public static final float VENGEANCE_HEAL = 2f;
    public static final int VENGEANCE_COOLDOWN = 10;
    public static final float GUIDANCE_BONUS = 3f;
    public static final int GUIDANCE_COOLDOWN = 60;
    public static final double GUIDANCE_RADIUS = 6.0;

    // ---------------------------------------------------------------- Skitch's Smuggling Coat
    public static final int DEEP_ROWS = 6, SEAM_ROWS = 3;

    // ---------------------------------------------------------------- Timelessness
    /** Remaining ticks of a time-based affliction after one Timelessness pass: halved; 0 means it is lifted. */
    public static int timeless(int remaining) { return remaining <= 40 ? 0 : remaining / 2; }
    public static final int TIMELESS_PERIOD = 100;
}
