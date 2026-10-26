package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Kind;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Mode;

import java.util.List;
import java.util.Locale;

/**
 * Why: the numbers of Solomon, Lord of Sorcery (docs/veldanava_skills.md) in one place that names no Minecraft type, so the skill, the events, the client's pillar drawing, the unit tests and the CI
 * self-test read the same table. Ten modes like every Veldanava skill: four toggles (Infinite Grimoire, Omnipotent Magic, Mana Skin, Elemental Sovereignty), five actives (Grand Spell Copy, Arcane
 * Convergence, Spell Chain, Mana Zone Erasure and the held Realm of Magic) and the mastery active The 72 Pillars. Costs are in Magicule; Realm of Magic's cost is per second held. The order of the modes is
 * the order of the owner's skill file (the held Realm is mode 6, the mastery mode is last).
 */
public final class SolomonRules {
    private SolomonRules() {}

    public static final JudgmentRules.Spec SOLOMON = new JudgmentRules.Spec("solomon", "Solomon", "Lord of Sorcery", List.of(
            new Mode(0, "infinite_grimoire", "Infinite Grimoire", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(1, "omnipotent_magic", "Omnipotent Magic", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(2, "mana_skin", "Mana Skin", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(3, "elemental_sovereignty", "Elemental Sovereignty", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(4, "grand_spell_copy", "Grand Spell Copy", Kind.PRESS, 4500, 3000, 30, 18, false),
            new Mode(5, "arcane_convergence", "Arcane Convergence", Kind.CROUCH_PRESS, 2000, 1200, 20, 12, false),
            new Mode(6, "realm_of_magic", "Realm of Magic", Kind.HELD, 2500, 1500, 90, 60, true),
            new Mode(7, "spell_chain", "Spell Chain", Kind.PRESS, 12000, 8500, 60, 40, false),
            new Mode(8, "mana_zone_erasure", "Mana Zone Erasure", Kind.CROUCH_PRESS, 15000, 10000, 90, 60, false),
            new Mode(9, "the_72_pillars", "The 72 Pillars", Kind.PRESS, 80000, 80000, 240, 240, false)));

    /** Solomon's skill code in the player's mask (bits 5..7), see {@link JudgmentRules#code}. */
    public static final int CODE = 4;

    // ---------------------------------------------------------------- Infinite Grimoire (toggle 0)
    /** Share added to the maximum Magicule and to the regeneration (normal / mastered), as an ADD_MULTIPLIED_TOTAL modifier: +1.0 doubles. */
    public static final double MAX_MP_BONUS = 1.0, MAX_MP_BONUS_MASTERED = 1.5, REGEN_BONUS = 2.0, REGEN_BONUS_MASTERED = 3.0;
    /** The share of the usual chant time that is left (normal / mastered) and the shortest chant a grimoire cast can have (ticks). */
    public static final double CHANT_FACTOR = 0.5, CHANT_FACTOR_MASTERED = 0.35;
    public static final int CHANT_MIN_TICKS = 2;
    /** Mastered Infinite Grimoire: every spell costs this share of its price. */
    public static final double MASTERED_SPELL_COST = 0.5;

    // ---------------------------------------------------------------- Omnipotent Magic (toggle 1)
    /** The share of an enemy's magic blow that is foreseen and so not taken (normal / mastered). */
    public static final double FORESIGHT = 0.25, FORESIGHT_MASTERED = 0.40;
    /** The presence sense: reach (blocks), the most creatures marked at once and how long the mark lasts (ticks; it is renewed once a second). */
    public static final double PRESENCE_RADIUS = 24.0;
    public static final int PRESENCE_MAX = 8, PRESENCE_TICKS = 40, FORESIGHT_MESSAGE_GAP = 60;

    // ---------------------------------------------------------------- Mana Skin (toggle 2)
    /** One Hit Point is worth this many Magicule. */
    public static final double MP_PER_HP = 10.0;

    /** The outcome of a lethal blow on a Mana Skin: the damage that is left and the Magicule it cost. */
    public record Skin(float damage, double cost) {}

    /**
     * A blow that would kill (the damage is at least the health plus the absorption) is paid for in Magicule, 10 for each point: all of it when the pool allows, else as much of it as the pool pays for
     * (the rest hurts). A blow that does not kill, or a blow on a wearer with no Magicule, is left as it is.
     */
    public static Skin manaSkin(float damage, float health, float absorption, double magicule) {
        if (damage <= 0f || magicule <= 0.0 || damage < health + Math.max(0f, absorption)) return new Skin(Math.max(0f, damage), 0.0);
        double need = damage * MP_PER_HP;
        if (magicule >= need) return new Skin(0f, need);
        float covered = (float) (magicule / MP_PER_HP);
        return new Skin(Math.max(0f, damage - covered), magicule);
    }

    // ---------------------------------------------------------------- Elemental Sovereignty (toggle 3)
    /** The elemental projectiles one wearer absorbs a second at most, the Magicule each gives (share of the maximum, and its cap). */
    public static final int ABSORB_PER_SECOND = 10;
    public static final double ABSORB_SHARE = 0.01, ABSORB_CAP = 5000.0;

    private static final String[] HAZARD_WORDS = {"fire", "flame", "lava", "burn", "heat", "magma", "hot_floor", "inferno", "freez", "frost", "ice", "cold", "snow", "poison", "toxic", "venom"};
    private static final String[] PROJECTILE_WORDS = {"fire", "flame", "ball", "bolt", "lightning", "thunder", "ice", "frost", "water", "wind", "earth", "stone", "rock", "magic", "mana", "spell", "beam", "blast", "meteor", "skull", "shot"};

    /** True for a damage type path of an environmental or elemental hazard Elemental Sovereignty shrugs off (fire, lava, freezing, poison). */
    public static boolean hazard(String damageTypePath) {
        if (damageTypePath == null) return false;
        String s = damageTypePath.toLowerCase(Locale.ROOT);
        for (String w : HAZARD_WORDS) if (s.contains(w)) return true;
        return false;
    }

    /** True for a projectile entity type path that is an elemental one (not an arrow, a trident, a snowball, an egg or a pearl). */
    public static boolean elementalProjectile(String typePath) {
        if (typePath == null) return false;
        String s = typePath.toLowerCase(Locale.ROOT);
        if (s.contains("arrow") || s.contains("trident") || s.contains("snowball") || s.contains("egg") || s.contains("pearl") || s.contains("firework") || s.contains("potion") || s.contains("bottle")) return false;
        for (String w : PROJECTILE_WORDS) if (s.contains(w)) return true;
        return false;
    }

    /** The Magicule one absorbed projectile gives a wearer whose maximum is {@code maxMagicule}. */
    public static double absorbGain(double maxMagicule) { return Math.max(0.0, Math.min(ABSORB_CAP, maxMagicule * ABSORB_SHARE)); }

    // ---------------------------------------------------------------- Grand Spell Copy (4) and Spell Chain (7)
    /** How far the copied spell may have been cast from the caster (blocks), how old it may be (ticks), the size of the server's cast log, the spells of one player kept for the chain, and the spells a chain fires. */
    public static final double COPY_RANGE = 40.0;
    public static final int COPY_WINDOW_TICKS = 600, LOG_SIZE = 64, OWN_LOG = 3, CHAIN_COUNT = 3, FREE_COOLDOWN_SECONDS = 3;

    // ---------------------------------------------------------------- Arcane Convergence (5)
    /** The reach (blocks), the most projectiles / clouds taken at once, the Magicule each gives, and the share of the maximum one press can give back at most. */
    public static final double CONVERGENCE_RADIUS = 30.0, CONVERGENCE_CAP_SHARE = 0.25;
    public static final int CONVERGENCE_MAX_PROJECTILES = 64, CONVERGENCE_MAX_CLOUDS = 32;
    public static final double GAIN_PROJECTILE = 250.0, GAIN_CLOUD = 400.0, GAIN_FIELD = 1500.0;

    /** The Magicule a convergence gives: each projectile (or bolt), cloud and field is worth its price, the whole never above the share of the maximum. */
    public static double convergenceGain(int projectiles, int clouds, int fields, double maxMagicule) {
        double raw = Math.max(0, projectiles) * GAIN_PROJECTILE + Math.max(0, clouds) * GAIN_CLOUD + Math.max(0, fields) * GAIN_FIELD;
        return Math.min(raw, Math.max(0.0, maxMagicule) * CONVERGENCE_CAP_SHARE);
    }

    // ---------------------------------------------------------------- Realm of Magic (6)
    /** The radius, the longest it stands (ticks), the second between pulses, the enemies a pulse touches, the redraw interval and the grace after the last pulse; what a pulse does to an enemy. */
    public static final double REALM_RADIUS = 40.0;
    public static final int REALM_MAX_TICKS = 1200, REALM_PULSE_TICKS = 20, REALM_MAX_ENEMIES = 24, REALM_FX_EVERY_TICKS = 80, REALM_GRACE_TICKS = 40, REALM_SILENCE_TICKS = 30, REALM_LOCK_TICKS = 40;

    /** The chant of a grimoire cast: one tick inside an allied Realm, else the usual time cut by Infinite Grimoire (never below {@link #CHANT_MIN_TICKS}). */
    public static int chantTicks(int base, boolean infinite, boolean mastered, boolean realm) {
        if (realm) return 1;
        if (!infinite) return base;
        double f = mastered ? CHANT_FACTOR_MASTERED : CHANT_FACTOR;
        return Math.min(base, Math.max(CHANT_MIN_TICKS, (int) Math.round(base * f)));
    }

    /** The price of a grimoire spell: nothing inside an allied Realm, half once Infinite Grimoire is mastered, else as it was. */
    public static double spellCost(double base, boolean infiniteMastered, boolean realm) {
        if (realm) return 0.0;
        return infiniteMastered ? base * MASTERED_SPELL_COST : base;
    }

    // ---------------------------------------------------------------- Mana Zone Erasure (8)
    /** The reach, the most projectiles (including bolts), clouds, summons and casters one erasure touches, and how long the casters stutter (ticks; the Balance Law limits it per target). */
    public static final double ERASURE_RADIUS = 40.0;
    public static final int ERASURE_MAX_PROJECTILES = 96, ERASURE_MAX_CLOUDS = 32, ERASURE_MAX_SUMMONS = 24, ERASURE_MAX_CASTERS = 24, ERASURE_STUTTER_TICKS = 60;

    // ---------------------------------------------------------------- The 72 Pillars (9)
    /** The pillars, the spread of the packet and the pulse of the server, the ticks the pillars of one target are spread over, the time a pillar takes to fall (the telegraph), the targets marked at most. */
    public static final int PILLARS = 72, PILLAR_PULSE_TICKS = 4, PILLAR_SPAN_TICKS = 108, PILLAR_DELAY_TICKS = 12, PILLAR_TARGETS_MAX = 8, PILLAR_ELEMENTS = 8;
    /** The reach in which targets are marked (blocks), where the pillars land round a target (blocks), the radius of one impact, the base damage of a pillar, the most pillars that hit one creature in one cast, creatures one impact touches. */
    public static final double PILLAR_RANGE = 60.0, PILLAR_SPREAD = 6.0, PILLAR_IMPACT_RADIUS = 2.8, PILLAR_DAMAGE = 70.0;
    public static final int PILLAR_MAX_HITS = 10, PILLAR_IMPACT_MAX_CREATURES = 12;
    /** Terrain: the most blocks one cast removes, the most one pillar removes, the crater radius, and the hardness above which a block is left alone. */
    public static final int TERRAIN_MAX_BLOCKS = 600, TERRAIN_PER_PILLAR = 10;
    public static final double CRATER_RADIUS = 2.0, TERRAIN_MAX_HARDNESS = 50.0;

    /** How many of the 72 pillars fall on target {@code index} of {@code targets} (the first {@code 72 % targets} targets get one more). */
    public static int pillarsOnTarget(int index, int targets) {
        int t = Math.max(1, Math.min(PILLAR_TARGETS_MAX, targets));
        if (index < 0 || index >= t) return 0;
        return PILLARS / t + (index < PILLARS % t ? 1 : 0);
    }

    /** The tick (counted from the cast) at which pillar {@code k} of the {@code m} on one target starts to fall: spread evenly over {@link #PILLAR_SPAN_TICKS}. It lands {@link #PILLAR_DELAY_TICKS} later. */
    public static int pillarSpawnTick(int k, int m) {
        if (m <= 0) return 0;
        return (int) ((long) Math.max(0, Math.min(k, m - 1)) * PILLAR_SPAN_TICKS / m);
    }

    /** The element (0..7: flame, tide, gale, stone, storm, frost, light, dark) of pillar {@code k}; the seed turns the order round so two casts differ. */
    public static int pillarElement(long seed, int k) { return Math.floorMod((int) ((seed >>> 20) & 0xFFFF) + k, PILLAR_ELEMENTS); }

    /**
     * Where pillar {@code k} lands relative to its target, in blocks {dx, dz}: every fourth pillar lands on the target itself, the others scatter over a disc of {@code spread} blocks. A pure function of
     * the seed and the index, so the server's impacts and the client's picture are the same without any extra packet.
     */
    public static double[] pillarOffset(long seed, int k, double spread) {
        if (k % 4 == 0) return new double[]{0.0, 0.0};
        long h = mix(seed ^ (k * 0x9E3779B97F4A7C15L));
        double angle = ((h >>> 11) & 0xFFFFF) / (double) 0x100000 * Math.PI * 2.0;
        double dist = Math.sqrt(((mix(h) >>> 11) & 0xFFFFF) / (double) 0x100000) * Math.max(0.0, spread);
        return new double[]{Math.cos(angle) * dist, Math.sin(angle) * dist};
    }

    /** The seed of a packet: random bits above, the number of pillars on this target (1..72) in the low 16 bits. */
    public static long packSeed(long random, int pillars) { return ((random << 20) & ~0xFFFFFL) | (pillars & 0xFFFF); }

    /** The number of pillars a packet's seed names. */
    public static int pillarsOfSeed(long seed) { return (int) (seed & 0xFFFF); }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The damage of one pillar's impact on a creature, before the Balance Law: the base, never negative. */
    public static double pillarDamage(int hitsSoFar) { return hitsSoFar >= PILLAR_MAX_HITS ? 0.0 : PILLAR_DAMAGE; }
}
