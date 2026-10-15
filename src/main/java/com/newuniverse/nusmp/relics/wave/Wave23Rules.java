package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;
import java.util.Set;

/**
 * Why: Relics of Many Worlds, part 4, wave 23 (three longbows, a lute-crossbow, four shields, a lyre, a ring and a horn): the pure rules, with no
 * Minecraft imports, so they can be tested alone. Every number the twelve relics use lives here, and every tabletop rule becomes a game rule
 * on the same scale as the earlier waves: 5 ft = 1 block, 1 round = 6 s (120 ticks), a short rest = a few minutes, a long rest = a Minecraft day.
 * Dice are not rolled here: callers pass a roll in (0.0 to 1.0, or 1 to 4) so the maths stays deterministic.
 */
public final class Wave23Rules {
    private Wave23Rules() {}

    public static final long DAY = 24000L;
    /** A creature with at least this much maximum health counts as a boss: it resists the long effects, and is never dragged or sentenced. */
    public static final double BOSS_HEALTH = 100.0;

    public static boolean isBoss(double maxHealth) { return maxHealth >= BOSS_HEALTH; }

    /** The Minecraft day a game time falls in (a long rest ends when the day number changes). */
    public static long dayOf(long gameTime) { return Math.floorDiv(Math.max(0L, gameTime), DAY); }

    // ---------------------------------------------------------------- Divine Mercy
    /** The +3 bonus of the card, added to every arrow that lands. */
    public static final float MERCY_BONUS = 3.0f;
    /** 2d8 radiant (average 9) and 4d8 against a fiend or an undead (average 18), scaled to this game's damage: 7 and 14. */
    public static final float RADIANT = 7.0f, RADIANT_FIEND = 14.0f;
    /** The Sentence: a creature of at most this many hit points can be sentenced; it takes this share of its maximum health unless it saves. */
    public static final double SENTENCE_MAX_HEALTH = 100.0, SENTENCE_SHARE = 0.55, SENTENCE_SAVE_CHANCE = 0.35;
    public static final int SENTENCE_ARMED_TICKS = 600, SENTENCE_COOLDOWN = 40;

    public static float radiant(boolean fiendOrUndead) { return fiendOrUndead ? RADIANT_FIEND : RADIANT; }

    public static boolean canSentence(double victimMaxHealth) { return victimMaxHealth > 0 && victimMaxHealth <= SENTENCE_MAX_HEALTH; }

    /** True if the creature resists the sentence (a Constitution save, DC 15: a roll under the chance saves). */
    public static boolean sentenceSaved(double roll) { return roll < SENTENCE_SAVE_CHANCE; }

    /** What the Sentence deals: a share of the creature's maximum health, halved by a save, never under 4. Nothing here kills at once: it is a heavy blow. */
    public static float sentenceDamage(double victimMaxHealth, boolean saved) {
        double d = Math.max(4.0, victimMaxHealth * SENTENCE_SHARE);
        return (float) (saved ? d * 0.5 : d);
    }

    private static final String[] FIEND_WORDS = {"zombie", "skeleton", "wither", "undead", "wraith", "lich", "ghost", "spectre", "phantom", "drowned", "husk", "stray",
            "demon", "devil", "fiend", "imp", "daemon", "vampire", "ghoul", "spirit", "death", "reaper", "specter", "zoglin", "piglin_brute_undead"};

    /** Whether an entity type path ("zombie", "tensura:demon_lord") names a fiend or an undead. (Vanilla's undead tag is checked by the caller as well.) */
    public static boolean isFiendOrUndead(String entityPath) {
        if (entityPath == null) return false;
        String p = entityPath.toLowerCase(Locale.ROOT);
        for (String w : FIEND_WORDS) if (p.contains(w)) return true;
        return false;
    }

    // ---------------------------------------------------------------- Embrasure: two uses, then a short rest
    public static final int PROTECT_USES = 2, PROTECT_TICKS = 120, PROTECT_REST_TICKS = 4800;
    public static final float PROTECT_ABSORB = 4.0f;

    /** The "Protect me!" counter: uses so far, and when the short rest that clears them ends (0 = no rest running). */
    public record Charges(int used, long restAt) {
        public static final Charges NONE = new Charges(0, 0L);

        /** The counter as it stands at {@code now}: a rest that has run its course clears the uses. */
        public Charges at(long now) { return restAt > 0 && now >= restAt ? NONE : this; }

        public boolean canUse(long now) { return at(now).used < PROTECT_USES; }

        /** The counter after one use; the second use starts the rest. */
        public Charges use(long now) {
            Charges c = at(now);
            int u = Math.min(PROTECT_USES, c.used + 1);
            return new Charges(u, u >= PROTECT_USES ? now + PROTECT_REST_TICKS : c.restAt);
        }

        public long restLeft(long now) { return Math.max(0L, at(now).restAt - now); }
    }

    // ---------------------------------------------------------------- The Dragon's Spirit
    public static final int SIGHT_TICKS = 300, SIGHT_COOLDOWN = 3600, SIGHT_RANGE = 48, CLAW_TARGETS = 3, CLAW_RANGE = 40;
    /** 30 ft of truesight is six blocks. */
    public static final double SIGHT_RADIUS = 6.0;
    public static final float CLAW_DAMAGE = 5.0f, STRIKE_DAMAGE = 14.0f;
    /** The spirit is 100 ft long (20 blocks) and 5 ft wide (one block). */
    public static final double STRIKE_LENGTH = 20.0, STRIKE_HALF_WIDTH = 0.5, STRIKE_SAVE_CHANCE = 0.40;
    public static final double DRAGON_HAZARD_RESIST = 0.15;

    /** 4d6 force damage: half as much on a save. */
    public static float strikeDamage(boolean saved) { return saved ? STRIKE_DAMAGE * 0.5f : STRIKE_DAMAGE; }

    public static boolean strikeSaved(double roll) { return roll < STRIKE_SAVE_CHANCE; }

    /** Blocks a revealing arrow lets the archer sense as traps (the paths of their block ids). */
    private static final Set<String> TRAP_BLOCKS = Set.of("tripwire_hook", "tripwire", "stone_pressure_plate", "oak_pressure_plate", "spruce_pressure_plate",
            "birch_pressure_plate", "jungle_pressure_plate", "acacia_pressure_plate", "dark_oak_pressure_plate", "mangrove_pressure_plate", "cherry_pressure_plate",
            "bamboo_pressure_plate", "crimson_pressure_plate", "warped_pressure_plate", "light_weighted_pressure_plate", "heavy_weighted_pressure_plate",
            "polished_blackstone_pressure_plate", "dispenser", "dropper", "tnt", "trapped_chest", "sculk_sensor", "calibrated_sculk_sensor", "sculk_shrieker",
            "trial_spawner", "spawner", "cobweb", "pointed_dripstone", "magma_block", "wither_rose");

    public static boolean isTrapBlock(String blockPath) { return blockPath != null && TRAP_BLOCKS.contains(blockPath); }

    // ---------------------------------------------------------------- The Reaver
    public static final float REAVER_BONUS = 2.0f, BARB_DAMAGE = 6.0f, BARB_EXTRA = 3.5f, IMPALE_DAMAGE = 7.0f;
    /** 50 ft normal range (10 blocks), 100 ft long range (20 blocks). */
    public static final double BARB_NORMAL = 10.0, BARB_LONG = 20.0;
    /** "Large or smaller": no more than 10 ft (two blocks) in either measure. */
    public static final double LARGE_MAX = 2.0, BARB_SAVE_CHANCE = 0.30, BARB_STOP = 1.6;
    public static final int BARB_USES = 2, BARB_COOLDOWN = 40, BARB_CHAIN_TICKS = 40;

    public static boolean largeOrSmaller(double width, double height) { return Math.max(width, height) <= LARGE_MAX; }

    /** Whether the chain drags the target to the bow: a creature of Large size or less, not a boss, that fails its Strength save (DC 17). */
    public static boolean pulled(double width, double height, double maxHealth, double roll) {
        return largeOrSmaller(width, height) && !isBoss(maxHealth) && roll >= BARB_SAVE_CHANCE;
    }

    /** Damage of the barbed arrow: its own blow, the extra 1d6 once it is embedded, and the impaling 2d6 if the target was dragged in. */
    public static float barbDamage(double distance, boolean impaled) {
        double base = distance > BARB_NORMAL ? BARB_DAMAGE * 0.75 : BARB_DAMAGE;       // beyond the normal range the shot is a long one: disadvantage, a smaller blow
        return (float) (base + BARB_EXTRA + (impaled ? IMPALE_DAMAGE : 0.0));
    }

    // ---------------------------------------------------------------- Warlute
    public static final int CHORD_USES = 3, CHORD_TICKS = 1200, RHYTHM_WINDOW = 120, CHORD_COOLDOWN = 60;
    public static final double CHORD_RANGE = 24.0;

    /** Rhythm of Battle: a blow with the lute within the last round (6 s) before the chord makes the chord one step stronger. */
    public static boolean rhythm(long lastBlowAt, long now) { return lastBlowAt > 0 && now >= lastBlowAt && now - lastBlowAt <= RHYTHM_WINDOW; }

    /** The amplifier of the chord's strength and speed: 0, or 1 on the beat. */
    public static int chordAmp(boolean onBeat) { return onBeat ? 1 : 0; }

    /** A creature can be given a chord once per long rest. {@code storedDayPlusOne} is the day of its last chord plus one (0 = never), so a fresh creature is allowed. */
    public static boolean chordAllowed(long storedDayPlusOne, long today) { return storedDayPlusOne != today + 1; }

    // ---------------------------------------------------------------- Aegis of the Abyss
    public static final int AEGIS_ARMOR = 3, HUNGER_USES = 2;
    public static final double AEGIS_TOUGHNESS = 2.0, SPELLGUARD = 0.25, HUNGER_RADIUS = 2.0, SHATTER = 90.0;

    /** The share of a spell the shield swallows (all of it, up to the 90 the card names). */
    public static float absorbed(float damage) { return (float) Math.min(Math.max(0f, damage), SHATTER); }

    public static boolean shatters(float damage) { return damage > SHATTER; }

    /** Hearts of absorption a swallowed spell turns into: half its damage, at most 20 health points. */
    public static float absorbGain(float swallowed) { return Math.min(20f, Math.max(0f, swallowed) * 0.5f); }

    /** Uses of Arcane Hunger left in the day the counter belongs to (the counter resets when the day number changes). */
    public static int hungerLeft(long counterDay, int used, long now) { return counterDay != dayOf(now) ? HUNGER_USES : Math.max(0, HUNGER_USES - used); }

    private static final String[] SPELL_WORDS = {"magic", "holy", "curse", "elemental", "spell", "lightning", "thunder", "fire_ball", "fireball", "wither", "sonic", "dragon_breath"};

    /** Whether a damage type path is a spell (a ranged spell attack the Aegis may swallow, and Spellguard softens). */
    public static boolean isSpell(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        for (String w : SPELL_WORDS) if (p.contains(w)) return true;
        return false;
    }

    // ---------------------------------------------------------------- Celestial Support
    public static final int CELESTIAL_ARMOR = 2, RIPOSTE_COOLDOWN = 100, DEATH_SAVE_COOLDOWN = 6000;
    public static final double CRITICAL_SHARE = 0.35;
    public static final float RIPOSTE_DAMAGE = 8.0f;

    /** A blow counts as a critical hit when it takes at least this share of the target's maximum health. */
    public static boolean isCritical(double damage, double maxHealth) { return maxHealth > 0 && damage >= maxHealth * CRITICAL_SHARE; }

    /** The +2 on death saving throws: a blow that would drop the wielder to zero leaves one half-heart instead (the caller checks the five-minute rest). */
    public static float deathSave(float damage, float health) { return damage >= health ? Math.max(0f, health - 1f) : damage; }

    // ---------------------------------------------------------------- Hero's Shield
    public static final int HERO_ARMOR = 3;
    public static final double HERO_TOUGHNESS = 2.0, NONMAGIC_RESIST = 0.25, HERO_KNOCKBACK = 0.3;
    /** 30/90 ft: six blocks normal, eighteen long. */
    public static final double THROW_NORMAL = 6.0, THROW_LONG = 18.0;
    public static final float THROW_DAMAGE = 8.0f;
    public static final int THROW_COOLDOWN = 50;

    public static float throwDamage(double distance) { return (float) (distance > THROW_NORMAL ? THROW_DAMAGE * 0.7 : THROW_DAMAGE); }

    private static final Set<String> WEAPON_PATHS = Set.of("player_attack", "mob_attack", "mob_attack_no_aggro", "arrow", "thrown", "trident", "sting", "mace_smash", "spear",
            "cactus_attack", "stalagmite_attack");

    /** A blow of a nonmagical weapon: a blade, a fist, an arrow, a thrown thing. */
    public static boolean isWeaponPath(String path) { return path != null && WEAPON_PATHS.contains(path) && !isSpell(path); }

    // ---------------------------------------------------------------- Thunder Shield
    public static final int THUNDER_ARMOR = 3;
    public static final double THUNDER_RADIUS = 12.0;

    public static boolean isLightning(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("lightning") || p.contains("thunder") || p.contains("electr") || p.contains("shock");
    }

    // ---------------------------------------------------------------- Bewitching Lyre
    public static final int LYRE_CHARGES = 8, LYRE_COOLDOWN = 200, BEWITCH_TICKS = 160, PERFORM_HOLD = 60, PERFORM_COOLDOWN = 600, AUDIENCE_MIN = 12;
    public static final double LYRE_RADIUS = 10.0, AUDIENCE_RADIUS = 12.0, BEWITCH_FAIL_CHANCE = 0.5, BEWITCH_FAIL_BOSS = 0.2, PERFORM_SUCCESS = 0.70;

    /** Whether a foe fails its save against the enchantment: always, when a charge is spent on it, else by chance (less for a boss). */
    public static boolean bewitched(boolean chargeSpent, boolean boss, double roll) {
        return chargeSpent || roll < (boss ? BEWITCH_FAIL_BOSS : BEWITCH_FAIL_CHANCE);
    }

    /** Charges left after spending one on each of {@code foes} (one charge covers the whole chord): a spend needs at least one charge. */
    public static int lyreAfter(int charges, boolean spend) { return spend && charges > 0 ? charges - 1 : charges; }

    public static boolean audienceEnough(int willingCreatures) { return willingCreatures >= AUDIENCE_MIN; }

    /** The Performance check (DC 15): enough listeners and a good roll bring every charge back. */
    public static boolean performed(int willingCreatures, double roll) { return audienceEnough(willingCreatures) && roll < PERFORM_SUCCESS; }

    // ---------------------------------------------------------------- Eye of the Deep One
    public static final double BLAST_RANGE = 20.0, GRAPPLE_RANGE = 4.0, EYE_KNOCKBACK = 0.3;
    public static final float BLAST_DAMAGE = 5.0f, BLAST_BONUS = 2.0f;
    public static final int BLAST_COOLDOWN = 40, GRAPPLE_COOLDOWN = 300, GRAPPLE_TICKS = 80;

    public static final int BOON_WARD = 1, BOON_MIND = 2, BOON_BLAST = 3, BOON_ARMOR = 4;

    /** The boon the d4 after a long rest picks (1 to 4; anything else is clamped). */
    public static int boon(int d4) { return Math.max(1, Math.min(4, d4)); }

    public static String boonName(int boon) {
        return switch (boon) {
            case BOON_WARD -> "Warded Mind (spells hit you 10% softer)";
            case BOON_MIND -> "Sharpened Mind (+10% magicule regeneration)";
            case BOON_BLAST -> "Twisted Blast (Eldritch Blast deals 2 more)";
            case BOON_ARMOR -> "Abyssal Hide (+1 armour)";
            default -> "none";
        };
    }

    /**
     * The d4 is rolled again when a long rest passed (a new day) since the last roll; a player who never sleeps gets a new one every two days.
     * {@code storedDayPlusOne} is the day of the last roll plus one (0 = never rolled).
     */
    public static boolean rerollDue(long storedDayPlusOne, long today, boolean slept) {
        if (storedDayPlusOne <= 0) return true;
        long last = storedDayPlusOne - 1;
        return (slept && today > last) || today - last >= 2;
    }

    // ---------------------------------------------------------------- Horn of Leroy
    /** 500 ft = 100 blocks; 6 hours of a 24-hour day = 6000 ticks; the horn crumbles and is whole again after five days. */
    public static final double HORN_RADIUS = 100.0;
    public static final int HORN_TICKS = 6000, HORN_KILLS = 8, HORN_COOLDOWN = (int) (5 * DAY), CHARISMA_CAP = 5;
    public static final double CHARISMA_LUCK = 0.5;

    /** The battle is won when the hornblower and the fighters felled at least this many hostile creatures before the horn's time ran out. */
    public static boolean victory(int kills) { return kills >= HORN_KILLS; }

    /** Charisma earned so far (each won battle +1, at most five) as a Luck bonus. */
    public static double charismaLuck(int charisma) { return Math.max(0, Math.min(CHARISMA_CAP, charisma)) * CHARISMA_LUCK; }

    public static int charismaAfterVictory(int charisma) { return Math.min(CHARISMA_CAP, Math.max(0, charisma) + 1); }
}
