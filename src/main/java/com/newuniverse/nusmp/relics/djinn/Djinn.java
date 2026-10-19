package com.newuniverse.nusmp.relics.djinn;

/**
 * The thirty-six Djinn (names from the seventy-two retainers of the Magi guide): the first twelve, then the twenty-four of 1.93.0 appended after
 * Vassago. One of each exists in the world and one player holds a given Djinn at a time ({@link DjinnLedger}). Each is a row of pure data: its magic,
 * its colours, what its Half Equip does to a blow, how it looks in Full Equip and its Extreme Magic. The glue (engine, layer) only reads this table;
 * the numbers are checked by DjinnTableTest.
 *
 * <p>Enum entries are only ever appended: the ordinal travels in the sync packet and the name is saved in the world.
 */
public enum Djinn {
    BAAL("Baal", "Lord of Thunder", Affinity.LIGHTNING, 0x3C8CFF, 0xBFE4FF, "Thunder-Lord Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.BLADE, Look.Tail.WHIP, Look.Halo.SPOKES, true),
            new Trait(Special.CHAIN, "minecraft:slowness", 60, 0, 0.30, 7.5, "Every blow arcs to a second foe."),
            new Extreme("Baal Bolg: Judgement of Thunder", Shape.BOLTS, 15.0, 30.0, "minecraft:slowness", 90, 2,
                    "Nine bolts fall in a ring around you, one after another, each marked on the ground first.")),
    AMON("Amon", "Sovereign of Flame", Affinity.HEAT, 0xFF5A14, 0xFFD27A, "Flame-Sovereign Maso",
            new Look(Look.Horns.RAM, Look.Wings.FIRE, Look.Tail.FLAME, Look.Halo.RING, false),
            new Trait(Special.MELT, "minecraft:weakness", 90, 0, 0.35, 8.0, "Your blade melts guard: foes burn and lose Resistance."),
            new Extreme("Amon Salos: Crown of the Sun", Shape.NOVA, 13.0, 32.0, "nusmp:burn", 160, 0,
                    "A sun blooms over you and bursts outward in one white-hot wave.")),
    VINEA("Vinea", "Queen of the Deep", Affinity.WATER, 0x2D9CDB, 0xB5F0FF, "Tide-Queen Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.VEIL, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.FREEZE, "minecraft:slowness", 90, 2, 0.25, 6.5, "Cold water clings: foes slow to a crawl."),
            new Extreme("Vinea Zalg: The Drowning Sea", Shape.WAVE, 16.0, 25.0, "minecraft:slowness", 120, 2,
                    "A wall of sea rolls out from you, shoving everything before it.")),
    FOCALOR("Focalor", "Warden of Gales", Affinity.WIND, 0x62D6A0, 0xE6FFF1, "Gale-Warden Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.RING, true),
            new Trait(Special.PUSH, "", 0, 0, 0.30, 7.5, "Each blow throws the foe back on a gust."),
            new Extreme("Focalor Zaleph: Seven-Wind Cyclone", Shape.VORTEX, 15.0, 23.0, "minecraft:nausea", 150, 0,
                    "A cyclone gathers round you, drags foes in and flings them skyward.")),
    ZAGAN("Zagan", "Keeper of Life", Affinity.LIFE, 0x58C75A, 0xE4FFB8, "Verdant Maso",
            new Look(Look.Horns.STAG, Look.Wings.FEATHER, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.LIFESTEAL, "", 0, 0, 0.25, 6.5, "A share of every blow flows back into you."),
            new Extreme("Zagan Genesis: Bloom of Life", Shape.HEAL_FIELD, 14.0, 18.0, "minecraft:weakness", 150, 0,
                    "A garden opens round you: allies are healed, foes are drained.")),
    ZEPAR("Zepar", "Breaker of Giants", Affinity.STRENGTH, 0xD43A3A, 0xFFB9A0, "Titan Maso",
            new Look(Look.Horns.RAM, Look.Wings.MEMBRANE, Look.Tail.BLADE, Look.Halo.NONE, false),
            new Trait(Special.CRUSH, "minecraft:weakness", 90, 0, 0.35, 8.0, "Heavy blows: the more armour, the harder they land."),
            new Extreme("Zepar Fang: Titan's Fall", Shape.NOVA, 11.0, 34.0, "minecraft:slowness", 60, 3,
                    "You leap, and come down with the weight of a giant.")),
    FURFUR("Furfur", "Voice of Ruin", Affinity.SOUND, 0xB77BFF, 0xF1DDFF, "Resonant Maso",
            new Look(Look.Horns.CROWN, Look.Wings.VEIL, Look.Tail.TENDRIL, Look.Halo.RUNES, true),
            new Trait(Special.DEAFEN, "minecraft:nausea", 90, 0, 0.25, 6.5, "Blows ring in the skull: foes are silenced and dizzied."),
            new Extreme("Furfur Requiem: Chord of Ruin", Shape.NOVA, 17.0, 23.0, "minecraft:nausea", 180, 0,
                    "Three rising chords roll out and shake everything in earshot.")),
    MARCHOSIAS("Marchosias", "Hunter of Embers", Affinity.HEAT, 0xE23A22, 0xFFA878, "Ember-Wolf Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.MEMBRANE, Look.Tail.FLAME, Look.Halo.NONE, true),
            new Trait(Special.BRAND, "minecraft:glowing", 150, 0, 0.30, 7.5, "Foes are branded: set alight and lit up for the hunt."),
            new Extreme("Marchosias Hunt: Emberfang Run", Shape.BEAM, 22.0, 36.0, "nusmp:burn", 160, 0,
                    "You become a streak of fire and run a straight line through everything in it.")),
    SITRI("Sitri", "Dawn's Radiance", Affinity.LIGHT, 0xFFE070, 0xFFFFFF, "Radiant Maso",
            new Look(Look.Horns.CROWN, Look.Wings.FEATHER, Look.Tail.NONE, Look.Halo.RING, false),
            new Trait(Special.SMITE, "minecraft:glowing", 100, 0, 0.30, 7.5, "Light bites: twice as hard against the undead."),
            new Extreme("Sitri Dawn: Radiance of First Light", Shape.NOVA, 14.0, 27.0, "minecraft:blindness", 100, 0,
                    "The sky whitens and a second dawn breaks over you.")),
    BIFRON("Bifron", "Stormfang", Affinity.LIGHTNING, 0x7B4DFF, 0xD2C2FF, "Stormfang Maso",
            new Look(Look.Horns.STAG, Look.Wings.BLADE, Look.Tail.BLADE, Look.Halo.SPOKES, false),
            new Trait(Special.PARALYZE, "minecraft:slowness", 30, 3, 0.30, 7.5, "A held breath of stillness after each blow."),
            new Extreme("Bifron Skyfall: Pillars of Storm", Shape.COLUMNS, 14.0, 27.0, "minecraft:slowness", 60, 1,
                    "Eight pillars of storm light rise from the ground, each marked before it strikes.")),
    PAIMON("Paimon", "Sovereign of the Sky", Affinity.WIND, 0x4FC3D6, 0xDFFAFF, "Sky-Sovereign Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.RUNES, true),
            new Trait(Special.SLICE, "minecraft:wither", 60, 0, 0.30, 7.5, "Wind-thin cuts that keep bleeding."),
            new Extreme("Paimon Tempest: Heavenly Gale", Shape.VORTEX, 15.0, 25.0, "", 0, 0,
                    "The sky itself closes round you and spins everything near into the air.")),
    VASSAGO("Vassago", "Lord of the Abyss", Affinity.WATER, 0x2F55D8, 0xB8CCFF, "Abyssal Maso",
            new Look(Look.Horns.RAM, Look.Wings.MEMBRANE, Look.Tail.TENDRIL, Look.Halo.NONE, false),
            new Trait(Special.DROWN, "minecraft:slowness", 90, 1, 0.30, 7.5, "Foes choke on the deep: their breath is taken."),
            new Extreme("Vassago Maelstrom: Abyssal Tide", Shape.VORTEX, 16.0, 27.0, "minecraft:slowness", 120, 2,
                    "A black whirlpool opens at your feet and drags the world towards it.")),

    // ---------------------------------------------------------------- 1.93.0: the twenty-four appended after Vassago (the order is fixed)
    BAEL("Bael", "King of the First Throne", Affinity.STRENGTH, 0xC9A227, 0xFFF0A0, "First-Throne Maso",
            new Look(Look.Horns.CROWN, Look.Wings.BLADE, Look.Tail.NONE, Look.Halo.SPOKES, false),
            new Trait(Special.KNEEL, "minecraft:slowness", 90, 2, 0.30, 7.5, "A king's blow: the foe is driven to its knees and pinned."),
            new Extreme("Bael Thronus: Edict of the First King", Shape.QUAKE, 15.0, 30.0, "minecraft:slowness", 100, 3,
                    "Three shockwaves roll out from your throne, each one lifting and crushing everything it passes.")),
    AGARES("Agares", "Rider of Crocodile Tides", Affinity.WATER, 0x1FA37A, 0xB8FFE0, "Tide-Rider Maso",
            new Look(Look.Horns.RAM, Look.Wings.VEIL, Look.Tail.BLADE, Look.Halo.NONE, false),
            new Trait(Special.DRAG, "minecraft:slowness", 80, 1, 0.25, 6.5, "Tide-jaws: each blow drags the foe in towards you."),
            new Extreme("Agares Maw: Jaws of the Rising Tide", Shape.CONE, 17.0, 30.0, "minecraft:slowness", 100, 1,
                    "A wall of water shaped like a crocodile's jaws rears in front of you and snaps shut.")),
    VALEFOR("Valefor", "Thief of Storm-Hearts", Affinity.LIGHTNING, 0x5A3DB5, 0xE0D2FF, "Stormthief Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.BLADE, Look.Tail.WHIP, Look.Halo.RING, true),
            new Trait(Special.PLUNDER, "minecraft:weakness", 60, 0, 0.25, 6.5, "A thief's blow strips the foe's boons and lends you their speed."),
            new Extreme("Valefor Larceny: Storm-Heart Heist", Shape.RAIN, 14.0, 24.0, "minecraft:weakness", 120, 1,
                    "Fourteen stolen sparks of storm fall one after another, each marked on the ground first.")),
    BARBATOS("Barbatos", "Hunter of the Wild Green", Affinity.LIFE, 0x2E7D32, 0xCFFFB0, "Green-Hunter Maso",
            new Look(Look.Horns.STAG, Look.Wings.VEIL, Look.Tail.WHIP, Look.Halo.NONE, true),
            new Trait(Special.HUNT, "minecraft:glowing", 150, 0, 0.28, 7.0, "The hunter's mark: wounded prey (below half health) takes half again as much."),
            new Extreme("Barbatos Venatio: The Wild Hunt", Shape.COLUMNS, 15.0, 26.0, "minecraft:poison", 120, 1,
                    "Eight pillars of living thorn burst out of the ground, each marked before it strikes.")),
    GUSION("Gusion", "Seer of the Tide Mirror", Affinity.WATER, 0x3D7EAA, 0xD6F2FF, "Mirror-Seer Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.VEIL, Look.Tail.NONE, Look.Halo.RUNES, false),
            new Trait(Special.FORESIGHT, "minecraft:glowing", 120, 0, 0.25, 6.5, "A glimpse ahead: the foe is revealed and you take less from the next blow."),
            new Extreme("Gusion Speculum: Verdict of the Tide Mirror", Shape.CROSS, 18.0, 28.0, "minecraft:glowing", 160, 0,
                    "Four lances of mirrored water cut out from you in a cross.")),
    VEPAR("Vepar", "Mermaid Sovereign of Wrecks", Affinity.WATER, 0x00A6B8, 0xBFFCFF, "Wreck-Mermaid Maso",
            new Look(Look.Horns.NONE, Look.Wings.VEIL, Look.Tail.TENDRIL, Look.Halo.RING, true),
            new Trait(Special.WRECK, "tensura:fragility", 150, 0, 0.28, 7.0, "Wrecker's blow: the foe's armour is cracked and worn down."),
            new Extreme("Vepar Naufragium: Wrecker's Surge", Shape.WAVE, 17.0, 26.0, "tensura:fragility", 160, 0,
                    "A surge of splintered hulls and cold black water rolls out from you.")),
    SHAX("Shax", "Silencer of the Dove", Affinity.SOUND, 0x8E8EA8, 0xF0F0FF, "Dove-Silence Maso",
            new Look(Look.Horns.NONE, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.RING, true),
            new Trait(Special.HUSH, "minecraft:slowness", 60, 0, 0.22, 6.0, "A dove's hush: the foe is silenced, slowed and forgets whom it was fighting."),
            new Extreme("Shax Silentium: Hush of the White Dove", Shape.NOVA, 16.0, 22.0, "minecraft:slowness", 120, 2,
                    "A white silence spreads from you in one soundless wave.")),
    BUER("Buer", "Healer of the Wheel", Affinity.LIFE, 0x9CCC3B, 0xF4FFC0, "Wheel-Healer Maso",
            new Look(Look.Horns.STAG, Look.Wings.FEATHER, Look.Tail.NONE, Look.Halo.RING, true),
            new Trait(Special.MEND, "minecraft:weakness", 60, 0, 0.22, 6.0, "Each blow mends you and the allies beside you."),
            new Extreme("Buer Rota: The Wheel of Mending", Shape.HEAL_FIELD, 14.0, 17.0, "minecraft:weakness", 120, 0,
                    "A turning wheel of green light heals your allies and drains your foes.")),
    ANDREALPHUS("Andrealphus", "Peacock of the Stars", Affinity.LIGHT, 0x2F6FDB, 0xA8D4FF, "Star-Peacock Maso",
            new Look(Look.Horns.CROWN, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.SPOKES, true),
            new Trait(Special.DAZZLE, "minecraft:blindness", 50, 0, 0.28, 7.0, "A starburst: the foe is blinded and the foes round it are lit up."),
            new Extreme("Andrealphus Pavo: Fall of a Hundred Stars", Shape.RAIN, 16.0, 27.0, "minecraft:blindness", 80, 0,
                    "Stars fall around you one after another, each marked on the ground first.")),
    KIMARIS("Kimaris", "Rider of the Dark Steed", Affinity.STRENGTH, 0x3A2A3F, 0xFF6A8A, "Dark-Rider Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.MEMBRANE, Look.Tail.WHIP, Look.Halo.NONE, false),
            new Trait(Special.TRAMPLE, "minecraft:slowness", 40, 0, 0.30, 8.0, "Momentum: the faster you run, the harder the blow lands."),
            new Extreme("Kimaris Equus: Charge of the Dark Steed", Shape.BEAM, 22.0, 35.0, "minecraft:slowness", 60, 1,
                    "You ride a straight line of black fire through everything in front of you.")),
    AMDUSIAS("Amdusias", "Maestro of the Unicorn Horn", Affinity.SOUND, 0xD58AF0, 0xFFE3FF, "Unicorn-Maestro Maso",
            new Look(Look.Horns.SPIKE, Look.Wings.FEATHER, Look.Tail.TENDRIL, Look.Halo.RUNES, false),
            new Trait(Special.SHOCKWAVE, "minecraft:nausea", 80, 0, 0.25, 6.5, "The note rings outward: foes round the target are shoved back."),
            new Extreme("Amdusias Concerto: The Unicorn's Last Chord", Shape.CROSS, 17.0, 26.0, "minecraft:nausea", 120, 0,
                    "Four notes ring out from you in a cross and meet in the air.")),
    BELIAL("Belial", "Lord of Hollow Thrones", Affinity.HEAT, 0x7A0F2E, 0xFF7096, "Hollow-Lord Maso",
            new Look(Look.Horns.CROWN, Look.Wings.FIRE, Look.Tail.FLAME, Look.Halo.NONE, false),
            new Trait(Special.CINDER, "nusmp:burn", 140, 0, 0.30, 7.5, "Cinders spread: the fire leaps to foes near the target."),
            new Extreme("Belial Inanis: Fall of the Hollow Throne", Shape.IMPLODE, 14.0, 32.0, "nusmp:burn", 160, 0,
                    "The air is dragged in towards you, and a moment later the hollow collapses in fire.")),
    DECARABIA("Decarabia", "Star Crab of the Deep", Affinity.LIGHT, 0xE0B45A, 0xFFF4C8, "Star-Crab Maso",
            new Look(Look.Horns.NONE, Look.Wings.MEMBRANE, Look.Tail.BLADE, Look.Halo.SPOKES, true),
            new Trait(Special.SHELL, "minecraft:slowness", 50, 0, 0.25, 6.5, "Star-shell: every blow adds a little Absorption (to three hearts)."),
            new Extreme("Decarabia Astra: Star-Shell Nova", Shape.NOVA, 14.0, 26.0, "minecraft:slowness", 100, 1,
                    "A star ripens in a shell of light round you, then bursts.")),
    SEERE("Seere", "Swift Wind Prince", Affinity.WIND, 0xE8E8C0, 0xFFFFFF, "Wind-Prince Maso",
            new Look(Look.Horns.NONE, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.SPOKES, true),
            new Trait(Special.QUICKEN, "", 0, 0, 0.28, 7.0, "Each blow quickens you: Speed II for two seconds."),
            new Extreme("Seere Celer: Passage of the Swift Wind", Shape.BEAM, 24.0, 28.0, "minecraft:slowness", 40, 0,
                    "You become the wind and pass in one breath through everything in a long line.")),
    DANTALION("Dantalion", "Many-Faced Scholar", Affinity.LIGHT, 0xC0A0FF, 0xF2E8FF, "Many-Face Maso",
            new Look(Look.Horns.CROWN, Look.Wings.VEIL, Look.Tail.NONE, Look.Halo.RUNES, true),
            new Trait(Special.BEGUILE, "minecraft:nausea", 100, 0, 0.25, 6.5, "Many faces: a confused creature turns on its nearest neighbour."),
            new Extreme("Dantalion Facies: Lecture of a Thousand Faces", Shape.COLUMNS, 14.0, 25.0, "minecraft:nausea", 140, 0,
                    "Eight pillars of pale light rise, each with a face in it, each marked before it speaks.")),
    ANDROMALIUS("Andromalius", "Seeker of Hidden Things", Affinity.LIGHTNING, 0x2B2B6B, 0x9CA8FF, "Hidden-Seeker Maso",
            new Look(Look.Horns.STAG, Look.Wings.BLADE, Look.Tail.TENDRIL, Look.Halo.RUNES, false),
            new Trait(Special.REVEAL, "minecraft:glowing", 150, 0, 0.27, 7.0, "Hidden things are found: invisibility is stripped, and the hidden take half again."),
            new Extreme("Andromalius Inquisitio: Thunder Finds the Hidden", Shape.BOLTS, 16.0, 26.0, "minecraft:glowing", 160, 0,
                    "Nine bolts fall in a ring around you, one after another, each marked on the ground first.")),
    GLASYA_LABOLAS("Glasya-Labolas", "Hound of Slaughter", Affinity.STRENGTH, 0xA00000, 0xFF9090, "Slaughter-Hound Maso",
            new Look(Look.Horns.RAM, Look.Wings.MEMBRANE, Look.Tail.WHIP, Look.Halo.NONE, false),
            new Trait(Special.BLOODLUST, "minecraft:wither", 60, 0, 0.25, 7.0, "Slaughter: each blow in a row bites harder, up to forty percent more."),
            new Extreme("Glasya-Labolas Cruor: The Slaughter Howl", Shape.CONE, 15.0, 34.0, "minecraft:weakness", 100, 1,
                    "A howl and a rush of claws tear through a wide fan in front of you.")),
    HAAGENTI("Haagenti", "Alchemist of the Winged Bull", Affinity.STRENGTH, 0xB87333, 0xFFD9A0, "Winged-Bull Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.FEATHER, Look.Tail.WHIP, Look.Halo.NONE, true),
            new Trait(Special.TRANSMUTE, "", 0, 0, 0.27, 7.0, "Alchemy: the blow turns to a random boon for you and a random bane for the foe."),
            new Extreme("Haagenti Opus: The Great Work", Shape.VORTEX, 15.0, 25.0, "minecraft:poison", 100, 1,
                    "A cauldron of gold-bronze wind turns round you and throws everything in it into the air.")),
    CROCELL("Crocell", "Frost Sage", Affinity.WATER, 0x9ADCF0, 0xFFFFFF, "Frost-Sage Maso",
            new Look(Look.Horns.CROWN, Look.Wings.VEIL, Look.Tail.NONE, Look.Halo.RING, true),
            new Trait(Special.RIME, "minecraft:slowness", 100, 1, 0.22, 6.0, "Rime spreads: cold clings to every foe near the target."),
            new Extreme("Crocell Glacies: The Sage's Frozen Tide", Shape.WAVE, 16.0, 24.0, "minecraft:slowness", 140, 3,
                    "A tide of ice-water rolls out from you and freezes where it stops.")),
    FURCAS("Furcas", "Old Knight of the Ice Wind", Affinity.WIND, 0xAAB7C8, 0xFFFFFF, "Ice-Wind Maso",
            new Look(Look.Horns.STAG, Look.Wings.BLADE, Look.Tail.NONE, Look.Halo.RING, false),
            new Trait(Special.ICEWIND, "minecraft:slowness", 70, 1, 0.27, 7.0, "An ice wind throws the foe back and chills it."),
            new Extreme("Furcas Boreas: Charge of the Ice Wind", Shape.CONE, 18.0, 29.0, "minecraft:slowness", 120, 2,
                    "A knight's charge on an ice wind sweeps a wide fan in front of you.")),
    BALAM("Balam", "Three-Headed Prophet", Affinity.HEAT, 0xD1622A, 0xFFE0A0, "Three-Head Maso",
            new Look(Look.Horns.RAM, Look.Wings.FIRE, Look.Tail.WHIP, Look.Halo.SPOKES, true),
            new Trait(Special.TRIPLE, "nusmp:burn", 100, 0, 0.25, 6.5, "Three heads, three bites: two foes near the target take a share too."),
            new Extreme("Balam Trias: Prophecy of Three Flames", Shape.COLUMNS, 15.0, 28.0, "nusmp:burn", 140, 0,
                    "Eight pillars of foretold flame rise from the ground, each marked before it strikes.")),
    ALLOCES("Alloces", "Lion-Eyed Knight", Affinity.HEAT, 0xE8892B, 0xFFF0B0, "Lion-Eye Maso",
            new Look(Look.Horns.CROWN, Look.Wings.FIRE, Look.Tail.WHIP, Look.Halo.RING, true),
            new Trait(Special.ROAR, "minecraft:weakness", 80, 0, 0.27, 7.0, "The lion's roar weakens every foe near you."),
            new Extreme("Alloces Leonis: The Lion's Judgement", Shape.NOVA, 15.0, 27.0, "minecraft:weakness", 120, 1,
                    "A golden roar breaks over everything near you.")),
    CAMIO("Camio", "Songbird of the Crows", Affinity.SOUND, 0x2A3340, 0xA0C8FF, "Crow-Song Maso",
            new Look(Look.Horns.NONE, Look.Wings.FEATHER, Look.Tail.BLADE, Look.Halo.RUNES, true),
            new Trait(Special.CARRION, "minecraft:poison", 60, 0, 0.25, 6.5, "The crows pick at the weak: a foe with any ailment takes half again as much."),
            new Extreme("Camio Corvus: A Murder of Crows", Shape.RAIN, 15.0, 24.0, "tensura:fear", 100, 0,
                    "Crows dive around you one after another, each marked on the ground first.")),
    MURMUR("Murmur", "Death Drummer", Affinity.SOUND, 0x6B3FA0, 0xE8C8FF, "Death-Drum Maso",
            new Look(Look.Horns.CRESCENT, Look.Wings.MEMBRANE, Look.Tail.TENDRIL, Look.Halo.SPOKES, false),
            new Trait(Special.DRUMBEAT, "minecraft:slowness", 40, 0, 0.22, 7.0, "Every third blow is a drumbeat: twice the rider and a shove."),
            new Extreme("Murmur Tympanum: The Death Drum", Shape.QUAKE, 17.0, 28.0, "minecraft:nausea", 140, 0,
                    "Three drumbeats roll out of the ground, each one a wider ring of ruin."));

    // ---------------------------------------------------------------- the row types

    /**
     * What a Half Equip does to a blow (a melee hit by the bearer) besides its elemental extra damage. The first thirteen are the original twelve and
     * NONE; the rest are appended (1.93.0), one for each of the new Djinn.
     */
    public enum Special {
        NONE, CHAIN, MELT, FREEZE, PUSH, LIFESTEAL, CRUSH, DEAFEN, BRAND, SMITE, PARALYZE, SLICE, DROWN,
        KNEEL, DRAG, PLUNDER, HUNT, FORESIGHT, WRECK, HUSH, MEND, DAZZLE, TRAMPLE, SHOCKWAVE, CINDER, SHELL, QUICKEN, BEGUILE, REVEAL, BLOODLUST,
        TRANSMUTE, RIME, ICEWIND, TRIPLE, ROAR, CARRION, DRUMBEAT
    }

    /**
     * The geometry an Extreme Magic strikes in. {@link #steps()} is how many strikes it makes (four ticks apart) and {@link #maxScale()} the most one
     * target can take in a whole casting, as a multiple of the row's damage: it is what keeps the balance law's ceiling (DjinnTableTest).
     */
    public enum Shape {
        NOVA(1, 1.0), BOLTS(9, 1.4), COLUMNS(1, 1.0), WAVE(1, 1.0), VORTEX(1, 1.0), BEAM(1, 1.0), HEAL_FIELD(1, 1.0),
        RAIN(14, 1.0), CONE(1, 1.0), QUAKE(3, 1.0), CROSS(1, 1.0), IMPLODE(2, 1.3);

        private final int steps;
        private final double maxScale;

        Shape(int steps, double maxScale) {
            this.steps = steps;
            this.maxScale = maxScale;
        }

        public int steps() { return steps; }

        public double maxScale() { return maxScale; }
    }

    /**
     * Half Equip: extra damage as a share of the blow (capped; a hit on a player never gets more than {@link TraitRules#PLAYER_RIDER_CAP} points of it, so
     * the cap of a row only matters against mobs), the Djinn's special, and one mob effect (an id like "minecraft:slowness", "" for none; the special
     * "nusmp:burn" is a fire, not an effect).
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

    /** The lower-case id used in item paths, commands, texture names and the saved state ("baal", "glasya_labolas"). */
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
