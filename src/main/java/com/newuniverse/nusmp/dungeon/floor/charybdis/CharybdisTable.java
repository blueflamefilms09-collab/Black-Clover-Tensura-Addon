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
    /** Boss rework wave 1 (appended rows, see below): the storm of scale shards on marked circles, the current that drags the party in, the maw at the heart of a whirlpool, and Magic Interference (the scales' anti-magic). */
    public static final String SCALE_SHARD_STORM = "scale_shard_storm", TEMPEST_CURRENT = "tempest_current", WHIRLPOOL_MAW = "whirlpool_maw", MAGIC_INTERFERENCE = "magic_interference";

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

    /**
     * The stage of the fight in blocks: the radius of the ring the megalodons tear loose from, the outer radius and the height of the volume they are
     * looked for in, and how far over the floor the leviathan hovers. They are the numbers of the Charybdis Basin the fight was built for; the basin
     * was removed with the Living Dungeon (1.58.0) and the fight keeps its measure wherever the boss is called.
     */
    public static final int ARENA_R = 28, ARENA_OUTER = 32, ARENA_HEIGHT = 24, ARENA_HOVER = 5;

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
            new Spec(MAELSTROM, "Maelstrom of Scales", 60, 60, 1500, 0, 40, 5, true, true, 60),
            // boss rework wave 1 (docs/bosses/rework_wave1.md): appended, never reordered; the first OLD_MOVES rows are the table above
            new Spec(SCALE_SHARD_STORM, "Scale-Shard Storm", 40, 34, 300, 0, 36, 3, false, true, 14),
            new Spec(TEMPEST_CURRENT, "Tempest Current", 40, 28, 320, 6, 30, 3, false, true, 14),
            new Spec(WHIRLPOOL_MAW, "Whirlpool Maw", 60, 40, 420, 0, 14, 3, false, true, 20),
            new Spec(MAGIC_INTERFERENCE, "Magic Interference", 40, 30, 600, 0, 24, 2, false, true, 12));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    // ---------------------------------------------------------------- boss rework wave 1 (docs/bosses/rework_wave1.md): Tensura's Charybdis, appended
    /** The rows of the table before the rework (the setting bossRework1Moves off keeps only these). */
    public static final int OLD_MOVES = 7;
    public static final float SHARD = 9f, MAW = 20f, INTERFERENCE = 4f;
    /** A shard circle, the current's reach and the core it drags toward, the maw's whirlpool and its jaws, the field of Magic Interference (blocks). */
    public static final double SHARD_RADIUS = 2.6, CURRENT_RADIUS = 20.0, CURRENT_CORE = 4.0, CURRENT_SPEED = 0.16, MAW_RADIUS = 8.0, MAW_CORE = 3.2, INTERFERENCE_RADIUS = 16.0;
    /** Shard circles at most, ticks from the wind-up to the shards landing, the current's and the maw's ticks, blessings torn off one foe. */
    public static final int SHARD_SPOTS = 4, SHARD_LAND = 10, CURRENT_TICKS = 50, MAW_TICKS = 60, INTERFERENCE_STRIPS = 2;

    /** The warning the foes read on the action bar while a rework move winds up ("" for any other move). */
    public static String warning(String id) {
        return switch (id) {
            case SCALE_SHARD_STORM -> "Scale shards will rain on the marked circles: leave them!";
            case TEMPEST_CURRENT -> "The tide turns toward the leviathan: swim against the current!";
            case WHIRLPOOL_MAW -> "A whirlpool opens round the leviathan and its maw waits at the heart: get out of the ring!";
            case MAGIC_INTERFERENCE -> "The scales hum with Magic Interference: your blessings will be torn away inside the ring.";
            default -> "";
        };
    }

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
