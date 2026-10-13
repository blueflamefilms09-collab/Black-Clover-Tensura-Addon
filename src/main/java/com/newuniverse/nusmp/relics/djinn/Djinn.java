package com.newuniverse.nusmp.relics.djinn;

/**
 * The twelve Djinn (names from the seventy-two retainers of the Magi guide). One of each exists in the world and one player holds a given Djinn at
 * a time ({@link DjinnLedger}). Each is a row of pure data: its magic, its colours, what its Half Equip does to a blow, how it looks in Full Equip
 * and its Extreme Magic. The glue (engine, layer) only reads this table; the numbers are checked by DjinnTableTest.
 *
 * <p>Enum entries are only ever appended: the ordinal travels in the sync packet and the name is saved in the world.
 */
public enum Djinn {
    BAAL("Baal", "Lord of Thunder", Affinity.LIGHTNING, 0x3C8CFF, 0xBFE4FF, "Thunder-Lord Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.BLADE, Look.Tail.WHIP, Look.Halo.SPOKES, true),
            new Trait(Special.CHAIN, "minecraft:slowness", 40, 0, 0.25, 6.0, "Every blow arcs to a second foe."),
            new Extreme("Baal Bolg: Judgement of Thunder", Shape.BOLTS, 14.0, 26.0, "minecraft:slowness", 60, 2,
                    "Nine bolts fall in a ring around you, one after another, each marked on the ground first.")),
    AMON("Amon", "Sovereign of Flame", Affinity.HEAT, 0xFF5A14, 0xFFD27A, "Flame-Sovereign Maso",
            new Look(Look.Horns.RAM, Look.Wings.FIRE, Look.Tail.FLAME, Look.Halo.RING, false),
            new Trait(Special.MELT, "minecraft:weakness", 60, 0, 0.30, 6.0, "Your blade melts guard: foes burn and lose Resistance."),
            new Extreme("Amon Salos: Crown of the Sun", Shape.NOVA, 12.0, 28.0, "nusmp:burn", 120, 0,
                    "A sun blooms over you and bursts outward in one white-hot wave.")),
    VINEA("Vinea", "Queen of the Deep", Affinity.WATER, 0x2D9CDB, 0xB5F0FF, "Tide-Queen Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.VEIL, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.FREEZE, "minecraft:slowness", 60, 2, 0.20, 5.0, "Cold water clings: foes slow to a crawl."),
            new Extreme("Vinea Zalg: The Drowning Sea", Shape.WAVE, 15.0, 22.0, "minecraft:slowness", 80, 2,
                    "A wall of sea rolls out from you, shoving everything before it.")),
    FOCALOR("Focalor", "Warden of Gales", Affinity.WIND, 0x62D6A0, 0xE6FFF1, "Gale-Warden Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.RING, true),
            new Trait(Special.PUSH, "", 0, 0, 0.25, 6.0, "Each blow throws the foe back on a gust."),
            new Extreme("Focalor Zaleph: Seven-Wind Cyclone", Shape.VORTEX, 14.0, 20.0, "minecraft:nausea", 100, 0,
                    "A cyclone gathers round you, drags foes in and flings them skyward.")),
    ZAGAN("Zagan", "Keeper of Life", Affinity.LIFE, 0x58C75A, 0xE4FFB8, "Verdant Maso",
            new Look(Look.Horns.STAG, Look.Wings.FEATHER, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.LIFESTEAL, "", 0, 0, 0.20, 5.0, "A share of every blow flows back into you."),
            new Extreme("Zagan Genesis: Bloom of Life", Shape.HEAL_FIELD, 12.0, 16.0, "minecraft:weakness", 100, 0,
                    "A garden opens round you: allies are healed, foes are drained.")),
    ZEPAR("Zepar", "Breaker of Giants", Affinity.STRENGTH, 0xD43A3A, 0xFFB9A0, "Titan Maso",
            new Look(Look.Horns.RAM, Look.Wings.MEMBRANE, Look.Tail.BLADE, Look.Halo.NONE, false),
            new Trait(Special.CRUSH, "minecraft:weakness", 60, 0, 0.30, 6.0, "Heavy blows: the more armour, the harder they land."),
            new Extreme("Zepar Fang: Titan's Fall", Shape.NOVA, 10.0, 30.0, "minecraft:slowness", 40, 3,
                    "You leap, and come down with the weight of a giant.")),
    FURFUR("Furfur", "Voice of Ruin", Affinity.SOUND, 0xB77BFF, 0xF1DDFF, "Resonant Maso",
            new Look(Look.Horns.CROWN, Look.Wings.VEIL, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.DEAFEN, "minecraft:nausea", 60, 0, 0.20, 5.0, "Blows ring in the skull: foes are silenced and dizzied."),
            new Extreme("Furfur Requiem: Chord of Ruin", Shape.NOVA, 16.0, 20.0, "minecraft:nausea", 120, 0,
                    "Three rising chords roll out and shake everything in earshot.")),
    MARCHOSIAS("Marchosias", "Hunter of Embers", Affinity.HEAT, 0xE23A22, 0xFFA878, "Ember-Wolf Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.MEMBRANE, Look.Tail.FLAME, Look.Halo.NONE, true),
            new Trait(Special.BRAND, "minecraft:glowing", 100, 0, 0.25, 6.0, "Foes are branded: set alight and lit up for the hunt."),
            new Extreme("Marchosias Hunt: Emberfang Run", Shape.BEAM, 20.0, 32.0, "nusmp:burn", 120, 0,
                    "You become a streak of fire and run a straight line through everything in it.")),
    SITRI("Sitri", "Dawn's Radiance", Affinity.LIGHT, 0xFFE070, 0xFFFFFF, "Radiant Maso",
            new Look(Look.Horns.CROWN, Look.Wings.FEATHER, Look.Tail.NONE, Look.Halo.RING, false),
            new Trait(Special.SMITE, "minecraft:glowing", 60, 0, 0.25, 6.0, "Light bites: twice as hard against the undead."),
            new Extreme("Sitri Dawn: Radiance of First Light", Shape.NOVA, 13.0, 24.0, "minecraft:blindness", 60, 0,
                    "The sky whitens and a second dawn breaks over you.")),
    BIFRON("Bifron", "Stormfang", Affinity.LIGHTNING, 0x7B4DFF, 0xD2C2FF, "Stormfang Maso",
            new Look(Look.Horns.STAG, Look.Wings.BLADE, Look.Tail.BLADE, Look.Halo.SPOKES, false),
            new Trait(Special.PARALYZE, "minecraft:slowness", 20, 3, 0.25, 6.0, "A held breath of stillness after each blow."),
            new Extreme("Bifron Skyfall: Pillars of Storm", Shape.COLUMNS, 13.0, 24.0, "minecraft:slowness", 40, 1,
                    "Eight pillars of storm light rise from the ground, each marked before it strikes.")),
    PAIMON("Paimon", "Sovereign of the Sky", Affinity.WIND, 0x4FC3D6, 0xDFFAFF, "Sky-Sovereign Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.RUNES, true),
            new Trait(Special.SLICE, "minecraft:wither", 40, 0, 0.25, 6.0, "Wind-thin cuts that keep bleeding."),
            new Extreme("Paimon Tempest: Heavenly Gale", Shape.VORTEX, 14.0, 22.0, "", 0, 0,
                    "The sky itself closes round you and spins everything near into the air.")),
    VASSAGO("Vassago", "Lord of the Abyss", Affinity.WATER, 0x2F55D8, 0xB8CCFF, "Abyssal Maso",
            new Look(Look.Horns.RAM, Look.Wings.MEMBRANE, Look.Tail.TENDRIL, Look.Halo.NONE, false),
            new Trait(Special.DROWN, "minecraft:slowness", 60, 1, 0.25, 6.0, "Foes choke on the deep: their breath is taken."),
            new Extreme("Vassago Maelstrom: Abyssal Tide", Shape.VORTEX, 15.0, 24.0, "minecraft:slowness", 80, 2,
                    "A black whirlpool opens at your feet and drags the world towards it."));

    // ---------------------------------------------------------------- the row types

    /** What a Half Equip does to a blow (a melee hit by the bearer) besides its elemental extra damage. */
    public enum Special { NONE, CHAIN, MELT, FREEZE, PUSH, LIFESTEAL, CRUSH, DEAFEN, BRAND, SMITE, PARALYZE, SLICE, DROWN }

    /** The geometry an Extreme Magic strikes in. */
    public enum Shape { NOVA, BOLTS, COLUMNS, WAVE, VORTEX, BEAM, HEAL_FIELD }

    /**
     * Half Equip: extra damage as a share of the blow (capped, so a PvP hit never gets past 30% of a 20 HP player), the Djinn's special, and one
     * mob effect (an id like "minecraft:slowness", "" for none; the special "nusmp:burn" is a fire, not an effect).
     */
    public record Trait(Special special, String effectId, int effectTicks, int effectAmp, double bonusFrac, double bonusCap, String blurb) {}

    /** Full Equip: the horns, wings, tail and halo the layer draws (the styles are read by DjinnShapes). */
    public record Look(Horns horns, Wings wings, Tail tail, Halo halo, boolean ears) {
        public enum Horns { NONE, CRESCENT, RAM, STAG, SPIKE, CROWN }
        public enum Wings { NONE, FEATHER, MEMBRANE, BLADE, VEIL, FIRE }
        public enum Tail { NONE, WHIP, FLAME, BLADE, TENDRIL }
        public enum Halo { NONE, RING, RUNES, SPOKES }
    }

    /** Extreme Magic: a name, the shape it strikes in, its reach (blocks), its raw damage (run through the balance law) and one effect on the foes it hits. */
    public record Extreme(String name, Shape shape, double radius, double damage, String effectId, int effectTicks, int effectAmp, String blurb) {}

    // ---------------------------------------------------------------- the row

    private final String displayName, title, formName;
    private final Affinity affinity;
    private final int color, glow;
    private final Look look;
    private final Trait trait;
    private final Extreme extreme;

    Djinn(String displayName, String title, Affinity affinity, int color, int glow, String formName, Look look, Trait trait, Extreme extreme) {
        this.displayName = displayName;
        this.title = title;
        this.affinity = affinity;
        this.color = color;
        this.glow = glow;
        this.formName = formName;
        this.look = look;
        this.trait = trait;
        this.extreme = extreme;
    }

    public String displayName() { return displayName; }
    /** "Baal, Lord of Thunder". */
    public String fullName() { return displayName + ", " + title; }
    public String title() { return title; }
    public Affinity affinity() { return affinity; }
    /** The body colour (0xRRGGBB). */
    public int color() { return color; }
    /** The bright glow colour (0xRRGGBB): runes, eyes, the blow's flash. */
    public int glow() { return glow; }
    /** The name of the Full Equip form ("Thunder-Lord Maso"). */
    public String formName() { return formName; }
    public Look look() { return look; }
    public Trait trait() { return trait; }
    public Extreme extreme() { return extreme; }

    /** The lower-case id used in item paths, commands, texture names and the saved state ("baal"). */
    public String id() { return name().toLowerCase(); }

    public static Djinn byName(String name) {
        if (name == null) return null;
        for (Djinn d : values()) if (d.name().equalsIgnoreCase(name)) return d;
        return null;
    }

    /** By ordinal for the network; null for -1 or anything out of range. */
    public static Djinn byOrdinal(int ordinal) {
        Djinn[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
