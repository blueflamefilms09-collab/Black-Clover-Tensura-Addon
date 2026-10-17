package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

/**
 * Why: every number that makes the Labyrinth Guardian a different fight in sector 1 and in sector 30 lives here, as plain arithmetic with
 * no Minecraft types, so a JUnit test can pin each curve and each safety cap and nobody has to read the entity to know what a theme trait
 * does. The entity ({@link LabyrinthGuardianEntity}) only reads the answers: health, damage, armour and size by the sector's threat; which
 * of the nine traits the sector's theme gives it; how that trait is tuned in the first and in the second phase; and when a lost Mana Core
 * may be forged again.
 * <p>
 * Why the curves look like this. Health is {@code base * (1 + perThreat * (threat - 1))} (the brief's formula), but the vanilla max-health
 * attribute stops at 1024, so {@link #health} clamps there and {@link #damageDivisor} turns the rest into a damage divisor (the same
 * trick the Blood-Red Commander uses): a threat-20 Guardian still takes proportionally longer to kill than a threat-5 one. Damage and
 * armour grow slower than health on purpose (a boss that scales everything at the same rate stops being dodgeable and becomes a
 * stat check): damage by a gentler per-threat rate, armour by a flat step with a hard ceiling (30 is the attribute's own). Its body is COLOSSAL
 * from the first sector ({@link #BASE_SCALE}, 1.8 times a Floor Warden: the owner's brief calls the Guardian "the colossal boss that gates every
 * tenth floor"), grows 2 % of its base size per threat and stops at a quarter more, so a later Guardian looks bigger without ever outgrowing its
 * hall. Traits are a table, not code: a
 * theme maps to ONE trait, each trait is five numbers (cooldown, range, power, duration, count) and the second phase is one function
 * that strengthens them, then the hard caps clamp the result, so "blindness two seconds at most" and "a root lasts one second at most" are
 * facts of this file that a test enforces for every trait in both phases, not promises scattered through the entity.
 */
public final class GuardianScaling {
    private GuardianScaling() {}

    // ---------------------------------------------------------------- curve constants (the config defaults repeat these)
    public static final double BASE_HEALTH = 400.0, HEALTH_PER_THREAT = 0.35, DAMAGE_PER_THREAT = 0.12;
    public static final double BASE_DAMAGE = 14.0, MAX_DAMAGE = 150.0;
    public static final double BASE_ARMOR = 12.0, ARMOR_PER_THREAT = 0.75, MAX_ARMOR = 30.0;
    /** The Guardian's base body size (the SCALE attribute): colossal, 1.8 times a Floor Warden. The config key {@code guardianScale} repeats it. */
    public static final double BASE_SCALE = 1.8;
    /**
     * The config's range, 0.5 to 4: a server that finds a colossal boss too much may halve a Warden's size, and nobody may make it larger than 4 (a
     * 5.6 x 12.8 block body is the most the 26 block high halls hold). The default is {@link #BASE_SCALE}.
     */
    public static final double MIN_BASE_SCALE = 0.5, MAX_SCALE = 4.0;
    /** Growth per level of threat, as a share of the base size, and the most it may grow in all (a quarter more). */
    public static final double SCALE_PER_THREAT = 0.02, MAX_SCALE_GROWTH = 1.25;
    /** The vanilla max-health attribute's ceiling: a Guardian's raw health above it becomes a damage divisor. */
    public static final double HEALTH_CAP = 1024.0;
    /** Below this share of its health the Guardian is in its second phase (two Living Armors step out, the trait gets stronger). */
    public static final double PHASE_TWO_AT = 0.5;
    /** A corrupted sector must not overflow a double: threat is clamped into 1..MAX_THREAT. */
    public static final int MAX_THREAT = 1000;

    public static int clampThreat(int threat) { return Math.max(1, Math.min(MAX_THREAT, threat)); }

    /** Health the formula asks for: {@code base * (1 + perThreat * (threat - 1))}, never below 1. */
    public static double rawHealth(double base, double perThreat, int threat) {
        return Math.max(1.0, base * (1.0 + Math.max(0.0, perThreat) * (clampThreat(threat) - 1)));
    }

    /** Health the entity gets: the formula, but never above the attribute's ceiling. */
    public static double health(double base, double perThreat, int threat) { return Math.min(HEALTH_CAP, rawHealth(base, perThreat, threat)); }

    /** Incoming damage is divided by this (1 or more): the part of {@link #rawHealth} the attribute cannot hold. */
    public static double damageDivisor(double base, double perThreat, int threat) { return Math.max(1.0, rawHealth(base, perThreat, threat) / HEALTH_CAP); }

    /** Melee damage: {@code BASE_DAMAGE * (1 + perThreat * (threat - 1))}, at most {@link #MAX_DAMAGE}. */
    public static double attackDamage(double perThreat, int threat) {
        return Math.min(MAX_DAMAGE, BASE_DAMAGE * (1.0 + Math.max(0.0, perThreat) * (clampThreat(threat) - 1)));
    }

    /** Armour points: a flat step per threat from the Warden's 12 up to the attribute's 30. */
    public static double armor(int threat) { return Math.min(MAX_ARMOR, BASE_ARMOR + ARMOR_PER_THREAT * (clampThreat(threat) - 1)); }

    /**
     * Body size (the SCALE attribute) of a Guardian whose base size is {@code base} (the config; clamped into {@link #MIN_BASE_SCALE}..{@link #MAX_SCALE},
     * a NaN reads as {@link #BASE_SCALE}): the base at threat 1, {@link #SCALE_PER_THREAT} of it more per level, at most {@link #MAX_SCALE_GROWTH} times the
     * base, and never above {@link #MAX_SCALE}.
     */
    public static double scale(double base, int threat) {
        double b = Double.isNaN(base) ? BASE_SCALE : Math.max(MIN_BASE_SCALE, Math.min(MAX_SCALE, base));
        return Math.min(MAX_SCALE, b * Math.min(MAX_SCALE_GROWTH, 1.0 + SCALE_PER_THREAT * (clampThreat(threat) - 1)));
    }

    /** Is the Guardian in its second phase (strictly under half health)? */
    public static boolean phaseTwo(double health, double maxHealth) { return maxHealth > 0.0 && health < maxHealth * PHASE_TWO_AT; }

    // ---------------------------------------------------------------- the traits
    /** What the sector's theme gives the Guardian. The nine themes of 1.54.0 have one each, same names; the two roads of 1.55.0 borrow (see {@link #traitOf}). */
    public enum Trait { GRAVITY, BLOOD, SPATIAL, FLAME, FROST, VERDANT, STORM, SHADOW, ASTRAL }

    /**
     * One trait's tuning. {@code cooldown} ticks between uses (0 = passive or continuous), {@code range} blocks, {@code power} the share of the
     * Guardian's attack damage one use deals, {@code duration} ticks of whatever it applies (a slow, a root, a blindness, a trail's life),
     * {@code count} a trait-specific whole number (see the table in {@link #base}).
     */
    public record TraitSpec(int cooldown, double range, double power, int duration, int count) {}

    /**
     * The trait of a theme. The nine themes of 1.54.0 each have the trait of their own name; the two roads of 1.55.0 borrow one: the Holy road's
     * {@code radiant} sector has the falling stars (ASTRAL: a rain of light) and the Abyssal road's {@code abyss} sector the gravity wells (GRAVITY:
     * the pull of the deep). They are matched by id, not by constant, so this class compiles and behaves the same with or without them in the enum;
     * an unknown future theme gets GRAVITY, the baseline.
     */
    public static Trait traitOf(RegionTheme theme) {
        if (theme == null) return Trait.GRAVITY;
        switch (theme.id()) {
            case "radiant": return Trait.ASTRAL;
            case "abyss": return Trait.GRAVITY;
            // the themes of 1.59.0 borrow the trait that fits their element
            case "feywild_grove": return Trait.VERDANT;
            case "dwarven_forgehold": return Trait.FLAME;
            case "crystal_geode": return Trait.ASTRAL;
            case "sunken_temple": return Trait.FROST;
            case "holy_church": return Trait.ASTRAL;
            case "celestial_court": return Trait.ASTRAL;
            case "sky_citadel": return Trait.STORM;
            case "mountain_monastery": return Trait.FROST;
            case "cursed_archive": return Trait.VERDANT;
            case "necropolis": return Trait.FROST;
            case "vampire_castle": return Trait.BLOOD;
            case "haunted_manor": return Trait.SHADOW;
            case "fungal_underdark": return Trait.SHADOW;
            case "infernal_foundry": return Trait.FLAME;
            case "drowned_crypt": return Trait.GRAVITY;
            case "spider_warrens": return Trait.VERDANT;
            default: break;
        }
        try { return Trait.valueOf(theme.name()); } catch (IllegalArgumentException unmapped) { return Trait.GRAVITY; }
    }

    /**
     * The first-phase tuning. The meaning of {@code count} and {@code range} per trait:
     * <ul>
     * <li>GRAVITY: a Gravity Well every 10 s pulls players within 10 blocks in and slows them 2 s; {@code count} extra blocks on its Ground Slam radius; {@code power} the well's damage.</li>
     * <li>BLOOD: no cooldown; {@code power} the share of its attack damage that becomes absorption on each of ITS OWN hits; {@code count} the ceiling of that absorption in percent of max health.</li>
     * <li>SPATIAL: a blink behind a target every 8 s, at most {@code range} blocks; {@code power} the share of its attack damage of the strike that follows.</li>
     * <li>FLAME: a trail of fire cells where it walked (no blocks: damage zones and particles); {@code range} half width, {@code duration} life of a cell, {@code count} most cells alive, {@code power} damage per second in the trail.</li>
     * <li>FROST: every two seconds players within {@code range} get Slowness for {@code duration}; {@code count} is the Slowness amplifier.</li>
     * <li>VERDANT: every 12 s one player within {@code range} is rooted for {@code duration} (through {@code BalanceLaw.beginControl}); {@code count} 1 target.</li>
     * <li>STORM: after every Ground Slam lightning arcs from it to the nearest player within {@code range} and on to {@code count} more, {@code power} per hop (each hop weaker).</li>
     * <li>SHADOW: every 10 s players within {@code range} are blinded for {@code duration}.</li>
     * <li>ASTRAL: every 11 s {@code count} stars fall on a ring of radius {@code range} round it after {@code duration} ticks, {@code power} damage each; damage only, no blocks, no push.</li>
     * </ul>
     */
    static TraitSpec base(Trait t) {
        return switch (t) {
            //                       cooldown range power duration count
            case GRAVITY -> new TraitSpec(200, 10.0, 0.35, 40, 2);
            case BLOOD -> new TraitSpec(0, 0.0, 0.25, 0, 10);
            case SPATIAL -> new TraitSpec(160, 14.0, 0.60, 0, 0);
            case FLAME -> new TraitSpec(0, 0.9, 0.25, 80, 24);
            case FROST -> new TraitSpec(40, 9.0, 0.0, 60, 0);
            case VERDANT -> new TraitSpec(240, 14.0, 0.0, 20, 1);
            case STORM -> new TraitSpec(0, 8.0, 0.60, 0, 3);
            case SHADOW -> new TraitSpec(200, 6.0, 0.0, 40, 0);
            case ASTRAL -> new TraitSpec(220, 7.0, 0.50, 20, 8);
        };
    }

    /** Hard ceilings, enforced on both phases and tested: blindness 2 s and 6 blocks, a root 1 s, a blink never more often than every 5 s, power at most 1.5. */
    public static final int SHADOW_MAX_TICKS = 40, ROOT_MAX_TICKS = 20, BLINK_MIN_COOLDOWN = 100, MIN_ACTIVE_COOLDOWN = 40;
    public static final double SHADOW_MAX_RANGE = 6.0, BLINK_MAX_RANGE = 16.0, MAX_POWER = 1.5;

    /** The tuning of a trait in a phase: the second phase is stronger (more power and reach, faster, one more of whatever counts), then the caps clamp. */
    public static TraitSpec spec(Trait t, boolean phaseTwo) {
        TraitSpec b = base(t);
        int cooldown = b.cooldown();
        double range = b.range(), power = b.power();
        int duration = b.duration(), count = b.count();
        if (phaseTwo) {
            if (cooldown > 0) cooldown = (int) Math.round(cooldown * 0.7);
            range = range * 1.2;
            power = power * 1.4;
            switch (t) {
                case GRAVITY, FLAME, STORM, ASTRAL -> count += 2;
                case FROST -> count += 1;
                case BLOOD -> count += 5;
                default -> { }
            }
        }
        if (cooldown > 0) cooldown = Math.max(MIN_ACTIVE_COOLDOWN, cooldown);
        power = Math.min(MAX_POWER, power);
        switch (t) {
            case SHADOW -> { range = Math.min(SHADOW_MAX_RANGE, range); duration = Math.min(SHADOW_MAX_TICKS, duration); }
            case VERDANT -> duration = Math.min(ROOT_MAX_TICKS, duration);
            case SPATIAL -> { cooldown = Math.max(BLINK_MIN_COOLDOWN, cooldown); range = Math.min(BLINK_MAX_RANGE, range); }
            default -> { }
        }
        return new TraitSpec(cooldown, range, power, duration, count);
    }

    /** The colour of a theme in the Mana Core's tooltip (RGB). A fixed table: the client has no access to the theme data files. */
    public static int themeColor(RegionTheme theme) {
        if (theme == null) return 0x9AA0B4;
        return switch (theme.id()) {                                                     // by id with a default: a theme added later never breaks the build
            case "gravity" -> 0x8E8AA8;
            case "blood" -> 0xC83A3A;
            case "spatial" -> 0xB070F0;
            case "flame" -> 0xF08A2A;
            case "frost" -> 0x8FD8F0;
            case "verdant" -> 0x6CC060;
            case "storm" -> 0x4FC8C0;
            case "shadow" -> 0x6A5C9A;
            case "astral" -> 0xF0D878;
            case "radiant" -> 0xFFF1C2;
            case "abyss" -> 0x38596E;
            case "feywild_grove" -> 0xD98BE8;
            case "dwarven_forgehold" -> 0xC98A3C;
            case "crystal_geode" -> 0xC9A0FF;
            case "sunken_temple" -> 0x3FB8B0;
            case "holy_church" -> 0xE3D3A8;
            case "celestial_court" -> 0x7FE0E6;
            case "sky_citadel" -> 0xBFD8F2;
            case "mountain_monastery" -> 0xB8844C;
            case "cursed_archive" -> 0x5B3A7A;
            case "necropolis" -> 0xC9B27A;
            case "vampire_castle" -> 0x7A1230;
            case "haunted_manor" -> 0x6E8A82;
            case "fungal_underdark" -> 0x6A4C93;
            case "infernal_foundry" -> 0xC2452D;
            case "drowned_crypt" -> 0x2E6F80;
            case "spider_warrens" -> 0x9FA3B8;
            default -> 0x9AA0B4;
        };
    }

    // ---------------------------------------------------------------- the lost core
    public static final int TICKS_PER_MINUTE = 20 * 60;

    /** Ticks still to wait before the altar may forge a core again: 0 = ready. {@code lastForgeTick} below 0 means it never forged (ready). */
    public static long reforgeWaitTicks(long lastForgeTick, long nowTick, int minutes) {
        if (lastForgeTick < 0) return 0L;
        long due = lastForgeTick + (long) Math.max(0, minutes) * TICKS_PER_MINUTE;
        return Math.max(0L, due - nowTick);
    }

    /**
     * May a player re-forge the Mana Core at a spent altar? Only when the altar really is spent ({@code altarDone}), the sector's door is still
     * sealed ({@code sectorGuardianDead}: a core that already opened it has nothing left to do), no core of this sector can be found
     * ({@code coreExists}: an item entity near the altar, or a player's inventory) and the wait is over. This is the anti soft-lock for a core
     * lost to lava, the void or a log-out; it is never a way to farm more than one core at a time.
     */
    public static boolean canReforge(boolean altarDone, boolean sectorGuardianDead, boolean coreExists, long lastForgeTick, long nowTick, int minutes) {
        return altarDone && sectorGuardianDead && !coreExists && reforgeWaitTicks(lastForgeTick, nowTick, minutes) == 0L;
    }
}
