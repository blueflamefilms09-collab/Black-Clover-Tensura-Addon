package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * The Charybdis Spawn's numbers (pure, unit tested): its build, the base damage of each move and every move's row as a {@link MoveSpec} the
 * scheduler can read without Minecraft. {@code CharybdisSpawnBoss} turns each row into a {@code Move} by adding its telegraph and its code,
 * looked up by id.
 * <p>
 * The shape of the fight (section 2, Type B of the design: "a floating, aquatic leviathan that summons smaller megalodons; it fires dense
 * magicule scales that players must parry or deflect"): a slow leviathan that orbits its dais and answers with a fan of glowing scales
 * (the signature: raise a shield, parry, or strike a scale and it flies back for a real wound), calls megalodons out of the walls, drags
 * foes into a whirlpool, sweeps its tentacles up close, dives with its beak and crushes a marked patch of the floor. Every blow that matters
 * is shown first; only the ultimate, the Maelstrom of Scales, leaves it exposed.
 */
public final class CharybdisTable {
    private CharybdisTable() {}

    public static final String SCALE_VOLLEY = "scale_volley", TENTACLE_SWEEP = "tentacle_sweep", WHIRLPOOL = "whirlpool", BEAK_LUNGE = "beak_lunge",
            SUMMON = "summon_megalodons", TIDAL_CRUSH = "tidal_crush", MAELSTROM = "maelstrom_of_scales";

    /** The scoreboard tags: every floor boss, this boss, and its megalodons. */
    public static final String TAG_FLOOR_BOSS = "nusmp_floor_boss", TAG_BOSS = "nusmp_charybdis", TAG_MINION = "nusmp_charybdis_minion";

    /** Its build: a big, slow, tough leviathan. The hitbox is square (Minecraft's are), so the long body trails out of it. */
    public static final double HEALTH = 1350, ATTACK = 25, SPEED = 0.30, ARMOR = 7, TOUGHNESS = 4;
    public static final float WIDTH = 3.4f, HEIGHT = 2.8f, DAMAGE_SCALE = 1.05f, MELEE_FACTOR = 0.75f;

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float SCALE = 5.5f, SWEEP = 15f, CRUSH = 22f, LUNGE = 20f;

    /** At most this many megalodons at once, and how many one cast brings. */
    public static final int MINION_CAP = 4, MINIONS_PER_CAST = 2;

    /** The most one blow of a megalodon may take of a player's health (the share of their maximum), and at least this many points so a weak player is still hurt. */
    public static final float MINION_SHARE = 0.18f, MINION_FLOOR = 3f;

    /** What a megalodon's blow of 'incoming' damage does to a victim with that much health: never more than the share, never less than the floor unless the blow was smaller. */
    public static float capMinion(float incoming, float victimMax) {
        if (incoming <= 0f || victimMax <= 0f) return Math.max(0f, incoming);
        return Math.min(incoming, Math.max(MINION_FLOOR, MINION_SHARE * victimMax));
    }

    /** A player this near the dais (blocks, along the floor) wakes the leviathan; the basin is 28 blocks across its water, so it is the middle of it. */
    public static final double SURFACE_RANGE = 24;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's Move): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(SCALE_VOLLEY, "Magicule Scale Volley", 20, 26, 130, 5, 40, 7, false, false, 12),
            new Spec(TENTACLE_SWEEP, "Tentacle Sweep", 20, 18, 90, 0, 7.5, 6, false, false, 24),
            new Spec(WHIRLPOOL, "Whirlpool", 20, 30, 260, 0, 30, 4, false, true, 14),
            new Spec(BEAK_LUNGE, "Beak Lunge", 40, 28, 200, 6, 28, 4, false, false, 30),
            new Spec(SUMMON, "Summon Megalodons", 40, 32, 520, 0, 48, 3, false, false, 14),
            new Spec(TIDAL_CRUSH, "Tidal Crush", 40, 46, 230, 0, 30, 4, false, false, 20),
            new Spec(MAELSTROM, "Maelstrom of Scales", 60, 60, 1500, 0, 40, 5, true, true, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }

    // ---------------------------------------------------------------- the scales
    /** Blocks a tick a scale flies, how long it lives (6 s), and the hits one volley may land on one victim. */
    public static final double SCALE_SPEED = 0.5;
    public static final int SCALE_LIFE = 120, HITS_PER_VICTIM = 2;

    /** Scales in one fan: 5 up to 59% power, 7 from 60%, 9 at the Gates. A Maelstrom fires two fans. */
    public static int fanCount(int power) { return power >= 100 ? 9 : power >= 60 ? 7 : 5; }

    /** Ticks of Slowness a scale that lands gives, and its level (0 = Slowness I; II once the Gates are open). */
    public static int slowTicks(int power) { return power >= 100 ? 80 : 60; }

    public static int slowLevel(int power) { return power >= 100 ? 1 : 0; }

    // ---------------------------------------------------------------- the surfacing (the basin's side of the fight)
    /** Ticks of bubbles before it surfaces (3 s), ticks it takes to rise (3 s), and how far it rises. */
    public static final int BUBBLE_TICKS = 60, RISE_TICKS = 60;
    public static final double RISE_BLOCKS = 6.0;
}
