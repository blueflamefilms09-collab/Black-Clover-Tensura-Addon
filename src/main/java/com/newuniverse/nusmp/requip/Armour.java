package com.newuniverse.nusmp.requip;

import java.util.List;

/**
 * Why: Requip: The Knight (Erza Scarlet's Equip Magic) is one Ultimate skill with fourteen armours, and everything that differs between the armours (what it looks like, what it adds to the
 * wearer, what it resists, what it costs, its two special attacks, the weapon it puts in the hand) has to live in ONE table that the server, the client painter, the documentation and the
 * tests all read. This is that table. It has no Minecraft classes in it, so the numbers are unit-tested ({@code ArmourTableTest}) instead of being checked by hand in game.
 * <p>
 * Mode order is the skill's mode order and is append-only (a saved skill stores the worn armour as this ordinal): new armours go at the END, before nothing. The skill adds one more mode after
 * the last armour, "Dismiss", which is not an armour.
 */
public enum Armour {
    HEART_KREUZ("heart_kreuz", "Heart Kreuz Armor", 0xE0405A, 0xFF8AA0, Particle.HEART, 0.0,
            new Stats(0.10, 2.0, 6.0, 2.0, 0.05, 0.10), Flight.NONE, 0.0, null,
            Resist.of(0.15, 0.10, 0.10, 0.0, 0.15, 0.15, 0.0),
            new Cost(0.030, 0.0020), 0.0,
            new Attack("cross_slash", "Kreuz Cross", Kind.CONE, Element.NONE, 14, 5.5, 0, 0.030, 50, 0.0),
            new Attack("kreuz_ward", "Kreuz Ward", Kind.GUARD, Element.NONE, 0, 0, 0, 0.040, 240, 0.0)),

    HEAVENS_WHEEL("heavens_wheel", "Heaven's Wheel Armor", 0xC8D8F0, 0xA0D0FF, Particle.SWORD, 0.0,
            new Stats(0.20, 3.0, 4.0, 1.0, 0.10, 0.0), Flight.GLIDE, 0.9, null,
            Resist.of(0.10, 0.0, 0.0, 0.0, 0.25, 0.0, 0.0),
            new Cost(0.045, 0.0030), 0.0,
            new Attack("sword_rain", "Sword Rain", Kind.VOLLEY, Element.NONE, 9, 28, 10, 0.050, 160, 0.0),
            new Attack("telekinesis", "Telekinesis", Kind.LIFT, Element.NONE, 8, 24, 0, 0.040, 120, 0.0)),

    FLAME_EMPRESS("flame_empress", "Flame Empress Armor", 0xE8501A, 0xFFB040, Particle.FLAME, 0.05,
            new Stats(0.05, 4.0, 8.0, 3.0, 0.0, 0.20), Flight.NONE, 0.0, "flame_empress_blade",
            Resist.of(0.10, 0.80, 0.0, 0.0, 0.0, 0.0, 0.0),
            new Cost(0.040, 0.0030), 0.0,
            new Attack("flame_slash", "Empress Flame Slash", Kind.LINE, Element.FIRE, 18, 14, 0, 0.050, 100, 0.0),
            new Attack("inferno_burst", "Inferno Burst", Kind.BURST, Element.FIRE, 14, 7, 0, 0.070, 220, 0.0)),

    LIGHTNING_EMPRESS("lightning_empress", "Lightning Empress Armor", 0xF4E060, 0xFFFFA0, Particle.SPARK, 0.10,
            new Stats(0.30, 3.0, 5.0, 2.0, 0.15, 0.0), Flight.NONE, 0.0, "lightning_empress_halberd",
            Resist.of(0.05, 0.0, 0.80, 0.0, 0.0, 0.0, 0.0),
            new Cost(0.040, 0.0030), 0.0,
            new Attack("thunder_dash", "Thunder Dash", Kind.DASH, Element.LIGHTNING, 14, 10, 0, 0.050, 80, 0.0),
            new Attack("thunderstorm", "Thunderstorm", Kind.VOLLEY, Element.LIGHTNING, 12, 18, 6, 0.070, 220, 0.0)),

    SEA_EMPRESS("sea_empress", "Sea Empress Armor", 0x2AA8C8, 0x80F0FF, Particle.WATER, 0.10,
            new Stats(0.10, 3.0, 7.0, 2.0, 0.05, 0.10), Flight.NONE, 0.0, "sea_empress_trident",
            Resist.of(0.05, 0.30, 0.0, 0.90, 0.0, 0.0, 0.0),
            new Cost(0.040, 0.0030), 0.0,
            new Attack("tidal_wave", "Tidal Wave", Kind.LINE, Element.WATER, 15, 16, 0, 0.050, 110, 0.0),
            new Attack("whirlpool", "Whirlpool", Kind.PULL, Element.WATER, 10, 8, 0, 0.060, 200, 0.0)),

    BLACK_WING("black_wing", "Black Wing Armor", 0x3A2A58, 0xA060FF, Particle.DARK, 0.20,
            new Stats(0.35, 4.0, 4.0, 1.0, 0.10, 0.0), Flight.GLIDE, 1.2, "black_wing_blade",
            Resist.of(0.0, 0.0, 0.0, 0.0, 0.10, 0.0, 0.0),
            new Cost(0.045, 0.0035), 0.0,
            new Attack("gale_slash", "Gale Slash", Kind.LINE, Element.WIND, 16, 16, 0, 0.050, 100, 0.0),
            new Attack("dark_dive", "Dark Dive", Kind.DASH, Element.DARK, 18, 12, 0, 0.060, 160, 0.0)),

    CLEAR_HEART("clear_heart", "Clear Heart Clothing", 0xF0F0FF, 0xFFFFFF, Particle.CLOUD, 0.0,
            new Stats(0.45, 2.0, 1.0, 0.0, 0.45, 0.0), Flight.NONE, 0.0, null,
            Resist.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new Cost(0.025, 0.0020), 0.0,
            new Attack("flurry", "Flurry", Kind.FLURRY, Element.NONE, 5, 6, 6, 0.040, 80, 0.0),
            new Attack("phantom_step", "Phantom Step", Kind.DASH, Element.NONE, 6, 8, 0, 0.030, 70, 0.0)),

    NAKAGAMI("nakagami", "Nakagami Armor", 0xF0F4FF, 0x60C0FF, Particle.RUNE, 0.25,
            new Stats(0.05, 5.0, 10.0, 4.0, 0.0, 0.30), Flight.NONE, 0.0, "nakagami_edge",
            Resist.of(0.0, 0.30, 0.30, 0.0, 0.0, 0.60, 0.0),
            new Cost(0.120, 0.0120), 0.0,
            new Attack("null_cut", "Null Cut", Kind.NULLIFY, Element.ANTI_MAGIC, 20, 6, 0, 0.150, 200, 0.0),
            new Attack("anti_magic_field", "Anti-Magic Field", Kind.FIELD, Element.ANTI_MAGIC, 8, 10, 0, 0.200, 600, 0.0)),

    GRAND_CHARIOT("grand_chariot", "Heaven's Wheel Armor: Grand Chariot", 0xFFE8A0, 0xFFD060, Particle.SWORD, 0.40,
            new Stats(0.25, 4.0, 5.0, 2.0, 0.10, 0.0), Flight.GLIDE, 1.0, null,
            Resist.of(0.15, 0.05, 0.05, 0.0, 0.35, 0.05, 0.0),
            new Cost(0.080, 0.0050), 0.0,
            new Attack("grand_sword_rain", "Grand Sword Rain", Kind.VOLLEY, Element.NONE, 10, 34, 24, 0.120, 300, 0.0),
            new Attack("chariot_charge", "Chariot Charge", Kind.DASH, Element.NONE, 22, 16, 0, 0.060, 120, 0.0)),

    ADAMANTINE("adamantine", "Adamantine Armor", 0x607068, 0x90FFC0, Particle.SPARK, 0.10,
            new Stats(-0.10, 3.0, 16.0, 8.0, -0.10, 1.0), Flight.NONE, 0.0, null,
            Resist.of(0.50, 0.20, 0.0, 0.0, 0.50, 0.20, 0.50),
            new Cost(0.050, 0.0030), 0.0,
            new Attack("adamant_smash", "Adamant Smash", Kind.BURST, Element.NONE, 16, 5, 0, 0.050, 140, 0.0),
            new Attack("reflect_stance", "Reflect Stance", Kind.GUARD, Element.NONE, 0, 0, 0, 0.060, 300, 0.0)),

    PURGATORY("purgatory", "Purgatory Armor", 0x2A1A22, 0xFF3A1A, Particle.FLAME, 0.40,
            new Stats(-0.05, 9.0, 6.0, 3.0, -0.05, 0.50), Flight.NONE, 0.0, "purgatory_mace",
            Resist.of(0.20, 0.40, 0.0, 0.0, 0.0, 0.0, 0.0),
            new Cost(0.080, 0.0040), 1.0,
            new Attack("purgatory_smash", "Purgatory Smash", Kind.BURST, Element.FIRE, 40, 7, 0, 0.100, 260, 0.15),
            new Attack("hellfire_cataclysm", "Hellfire Cataclysm", Kind.BURST, Element.FIRE, 80, 12, 0, 0.250, 900, 0.30)),

    FLIGHT_ARMOR("flight_armor", "Flight Armor", 0x70A8E0, 0xC0F0FF, Particle.CLOUD, 0.0,
            new Stats(0.30, 2.0, 3.0, 0.0, 0.10, 0.0), Flight.HOVER, 1.5, null,
            Resist.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new Cost(0.035, 0.0030), 0.0,
            new Attack("aerial_boost", "Aerial Boost", Kind.DASH, Element.WIND, 8, 14, 0, 0.040, 80, 0.0),
            new Attack("wing_blast", "Wing Blast", Kind.CONE, Element.WIND, 12, 8, 0, 0.040, 90, 0.0)),

    WIND_GOD("wind_god", "Wind God Armor", 0x58D8A0, 0xC0FFE0, Particle.CLOUD, 0.25,
            new Stats(0.40, 3.0, 3.0, 1.0, 0.20, 0.0), Flight.HOVER, 1.8, null,
            Resist.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.10, 0.0),
            new Cost(0.050, 0.0040), 0.0,
            new Attack("cyclone_slash", "Cyclone Slash", Kind.BURST, Element.WIND, 15, 7, 0, 0.060, 150, 0.0),
            new Attack("gale_rush", "Gale Rush", Kind.DASH, Element.WIND, 20, 18, 0, 0.060, 130, 0.0)),

    FAIRY_ARMOR("fairy_armor", "Fairy Armor", 0xF8B8E8, 0xFFE0A0, Particle.GLINT, 0.25,
            new Stats(0.20, 3.0, 7.0, 3.0, 0.10, 0.0), Flight.GLIDE, 1.0, null,
            Resist.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.35, 0.0),
            new Cost(0.060, 0.0040), 0.0,
            new Attack("fairy_radiance", "Fairy Radiance", Kind.HEAL, Element.LIGHT, 10, 10, 0, 0.080, 300, 0.0),
            new Attack("fairy_burst", "Fairy Burst", Kind.BURST, Element.LIGHT, 20, 9, 0, 0.100, 240, 0.0));

    /** What an armour adds to the wearer: fractions are ADD_MULTIPLIED_BASE, the rest ADD_VALUE. */
    public record Stats(double speed, double attack, double armor, double toughness, double attackSpeed, double knockbackResistance) {}

    /** The damage classes a resistance is given for. */
    public enum DamageClass { PHYSICAL, FIRE, LIGHTNING, WATER, PROJECTILE, MAGIC, EXPLOSION }

    /** Fractions (0..0.9) of each damage class that the armour takes off. */
    public record Resist(double physical, double fire, double lightning, double water, double projectile, double magic, double explosion) {
        public static Resist of(double physical, double fire, double lightning, double water, double projectile, double magic, double explosion) {
            return new Resist(physical, fire, lightning, water, projectile, magic, explosion);
        }

        public double of(DamageClass c) {
            return switch (c) {
                case PHYSICAL -> physical;
                case FIRE -> fire;
                case LIGHTNING -> lightning;
                case WATER -> water;
                case PROJECTILE -> projectile;
                case MAGIC -> magic;
                case EXPLOSION -> explosion;
            };
        }
    }

    /** equip: share of max Aura (else Magicule) spent to put the armour on; upkeep: share of it per second the armour stays on. */
    public record Cost(double equip, double upkeep) {}

    public enum Flight { NONE, GLIDE, HOVER }

    /** The shape of a special attack (the server code in {@code skill.ErzaRequipAttacks} has one branch per kind). */
    public enum Kind { CONE, LINE, BURST, DASH, VOLLEY, LIFT, PULL, FLURRY, GUARD, NULLIFY, FIELD, HEAL }

    public enum Element { NONE, FIRE, LIGHTNING, WATER, WIND, DARK, LIGHT, ANTI_MAGIC }

    /** The theme particle of the swap flash and of the attacks (mapped to a real particle by the client-safe server code). */
    public enum Particle { HEART, SWORD, FLAME, SPARK, WATER, DARK, CLOUD, RUNE, GLINT }

    /**
     * One special attack.
     *
     * @param raw        base damage (HP) before the EP scaling of the balance law; for HEAL the base healing
     * @param range      reach, radius or dash length in blocks
     * @param count      swords for a VOLLEY, strikes for a FLURRY (0 otherwise)
     * @param cost       share of max Aura (else Magicule) the attack spends
     * @param cooldown   ticks
     * @param selfDamage share of the wearer's current health the attack takes (never to death)
     */
    public record Attack(String id, String name, Kind kind, Element element, double raw, double range, int count, double cost, int cooldown, double selfDamage) {}

    private final String id, displayName;
    private final int theme, glow;
    private final Particle particle;
    private final double unlock;
    private final Stats stats;
    private final Flight flight;
    private final double thrust;
    private final String weapon;
    private final Resist resist;
    private final Cost cost;
    private final double upkeepSelfDamage;
    private final Attack primary, secondary;

    Armour(String id, String displayName, int theme, int glow, Particle particle, double unlock, Stats stats, Flight flight, double thrust, String weapon,
           Resist resist, Cost cost, double upkeepSelfDamage, Attack primary, Attack secondary) {
        this.id = id; this.displayName = displayName; this.theme = theme; this.glow = glow; this.particle = particle; this.unlock = unlock;
        this.stats = stats; this.flight = flight; this.thrust = thrust; this.weapon = weapon; this.resist = resist; this.cost = cost;
        this.upkeepSelfDamage = upkeepSelfDamage; this.primary = primary; this.secondary = secondary;
    }

    /** The id used in file names (geo key {@code erza_<id>}), mode ids, lang keys and the worn-armour tag. */
    public String id() { return id; }
    public String displayName() { return displayName; }
    /** The armour's main colour (0xRRGGBB): flash, dust, bar colour. */
    public int theme() { return theme; }
    /** The colour of its glow maps and effects. */
    public int glow() { return glow; }
    public Particle particle() { return particle; }
    /** Mastery (0..1 of the skill's maximum) from which the armour can be put on (operators and creative players are never locked). */
    public double unlock() { return unlock; }
    public Stats stats() { return stats; }
    public Flight flight() { return flight; }
    /** Blocks per tick of flight thrust (GLIDE: forward push while sprinting in the air; HOVER: also holds the height while sneaking). */
    public double thrust() { return thrust; }
    /** The item id (nusmp:) of the weapon that is requipped into the hand with the armour, or null. */
    public String weapon() { return weapon; }
    public Resist resist() { return resist; }
    public Cost cost() { return cost; }
    /** Health points the armour takes from the wearer every second it is on (Purgatory); the wearer is never killed by it. */
    public double upkeepSelfDamage() { return upkeepSelfDamage; }
    public Attack primary() { return primary; }
    public Attack secondary() { return secondary; }
    public List<Attack> attacks() { return List.of(primary, secondary); }

    /** The geo key of the armour's pieces: {@code assets/nusmp/geo/entity/<key>_arm_<piece>.geo.json}. */
    public String geoKey() { return "erza_" + id; }

    /** Same-order lookup used by the skill's mode number (an ordinal outside the table is "no armour"). */
    public static Armour byOrdinal(int i) {
        Armour[] all = values();
        return i >= 0 && i < all.length ? all[i] : null;
    }

    public static Armour byId(String id) {
        for (Armour a : values()) if (a.id.equalsIgnoreCase(id)) return a;
        return null;
    }
}
