package com.newuniverse.nusmp.skill.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner wants Faith to grow into eight named gods (docs/faith_gods.md), each from one franchise the mod covers, each with its own abilities, a ritual to become it and a god-form Ultimate whose
 * size follows the Ultimate classes of Tensura Reincarnated Nightmares (Lord, King, God, Dragon). The table of the eight lines, the class table (how many actives and passives a class has, how strong they are, the
 * EP a class wants before it evolves), the ritual rules and the devotion maths live here with no Minecraft class so they are unit-tested; the skill, the commands and the self-test only read them.
 * Every number that changes gameplay is also a SERVER setting (NUConfig, section faithGods); the numbers here are the defaults and the clamps.
 */
public final class FaithGodRules {
    private FaithGodRules() {}

    /** What an active does. Each verb is one small effect built from code the mod already has (Goddess of Faith, Loki's blink, the skill hit and heal helpers). */
    public enum Verb {
        BLESS, HEAL, SMITE, BEAM, WARD, BIND, BLINK, DRAIN, PURGE, SURGE, DOMAIN,
        /** Canon pass (faithGods.canon), appended: a gravity well that pulls foes in, a press that forces them down, a holy smite that bites the undead, Fairy Law (foes only, costs life per foe), a beam paid for with the caster's own life. */
        WELL, PRESS, HOLY, LAW, LIFE_BEAM
    }

    /** What a passive does: one potion effect the god keeps on itself (and on its worshippers, for the first passive) while the skill is switched on. */
    public enum Pas { REGEN, RESIST, STRENGTH, SPEED, ABSORB, SIGHT, LEAP, FIRE, HASTE, WATER }

    /** One active: {@code power} is raw damage / healing before the class and devotion scaling, {@code faith} the Faith points it burns, {@code mp} its flat magicule price, {@code radius} its reach in blocks. */
    public record Move(String id, String name, Verb verb, double power, int faith, double mp, int cooldownTicks, double radius, String text,
                       double unlock, int minWorshippers, double hpFrac) {
        /** The 4.6.0 form: always open, no worshipper minimum, no health price. */
        public Move(String id, String name, Verb verb, double power, int faith, double mp, int cooldownTicks, double radius, String text) {
            this(id, name, verb, power, faith, mp, cooldownTicks, radius, text, 0.0, 0, 0.0);
        }
    }

    public record Passive(String id, String name, Pas effect, String text) {}

    /**
     * One god line. {@code name} is the Unique ("Faith of Femto"), {@code ultimateName} the god form it evolves into. {@code sigil} is a SkillAnimeFx sigil style, {@code rgb} the colour of every effect.
     * {@code offering} / {@code offeringCount} are what the ritual burns (a vanilla item id), {@code source} where the canon facts came from.
     */
    public record God(String id, String ultimateId, String name, String ultimateName, String franchise, String domain, String canon, String source,
                      UltimateClass cls, int rgb, int sigil, String offering, int offeringCount, List<Move> moves, List<Passive> passives, Canon canonRules) {
        /** The 4.6.0 table rows: no canon rules. */
        public God(String id, String ultimateId, String name, String ultimateName, String franchise, String domain, String canon, String source,
                   UltimateClass cls, int rgb, int sigil, String offering, int offeringCount, List<Move> moves, List<Passive> passives) {
            this(id, ultimateId, name, ultimateName, franchise, domain, canon, source, cls, rgb, sigil, offering, offeringCount, moves, passives, null);
        }

        public String uniqueId() { return "faith_of_" + id; }
    }

    /**
     * The canon pass (docs/faith_gods.md "Canon pass"): what the series says about this god's faith. {@code ultRgb} the colour of the god form (-1 = the line's), {@code worship} how worship works in the series,
     * {@code limit} the drawback, {@code classWhy} why the class, {@code base} / {@code devotionMult} the strength before / per worshipper, {@code gates} a worshipper-count gate instead of linear devotion (Lucifero),
     * {@code ritualWorshippers} the worshippers the ritual refuses without (Super Saiyan God), {@code unmasteredToll} the Unique drains the body until mastered (Super Saiyan God).
     */
    public record Canon(int ultRgb, String worship, String limit, String classWhy, double base, double devotionMult, boolean gates, int ritualWorshippers, boolean unmasteredToll) {}

    /** What a class gives its god form: how many actives and passives, the strength factor, the EP it wants before the evolution, the passive amplifier. */
    public record ClassSpec(UltimateClass cls, int actives, int passives, double power, double epGate, int passiveAmp) {}

    /** The Unique half: its actives, its passives and its strength factor (the Ultimate's class table is {@link #CLASSES}). */
    public static final int UNIQUE_ACTIVES = 3, UNIQUE_PASSIVES = 1;
    public static final double UNIQUE_POWER = 0.6;
    /** Every god line holds this many actives and passives in its table; a class uses the first few. */
    public static final int POOL_ACTIVES = 5, POOL_PASSIVES = 3;

    /**
     * The class table, read from the Tensura Reincarnated Nightmares wiki pages of the Lord, King, God and Dragon Ultimates (docs/faith_gods.md section 3): Lord averages 2.6 passives and 4.4 actives (50 pages),
     * King 2.7 and 3.4 (11 pages), God 2 to 3 passives and 3 to 7 actives (6 pages, obtain cost 2.5M MP up to 35M EP), Dragon 1 and 2 (the Veldora / Velzard pages). The EP gates are the EP those pages ask for:
     * Lord about 2M, King about 15M, God 10M to 30M (30M here); Dragon has no EP number on the wiki (5M is this mod's choice). The power factor rises Lord < Dragon < King < God like the class damage caps.
     */
    public static final List<ClassSpec> CLASSES = List.of(
            new ClassSpec(UltimateClass.LORD, 4, 2, 1.00, 2_000_000.0, 0),
            new ClassSpec(UltimateClass.KING, 4, 3, 1.25, 15_000_000.0, 0),
            new ClassSpec(UltimateClass.GOD, 5, 3, 1.60, 30_000_000.0, 1),
            new ClassSpec(UltimateClass.DRAGON, 3, 2, 1.35, 5_000_000.0, 1));

    public static ClassSpec spec(UltimateClass c) {
        for (ClassSpec s : CLASSES) if (s.cls() == c) return s;
        return CLASSES.get(0);
    }

    // ------------------------------------------------------------------ the eight gods
    // SkillAnimeFx's sigil styles (FAITH 0, GODDESS 1, FORGE 2, MALLET 3, WILL 4, HELL 5, GRAVITY 6, FIRE_FORCE 7) as plain numbers, so this class stays free of the skill package.
    static final int SIG_FAITH = 0, SIG_GODDESS = 1, SIG_WILL = 4, SIG_HELL = 5, SIG_GRAVITY = 6, SIG_FIRE_FORCE = 7;

    private static Move m(String id, String name, Verb v, double power, int faith, double mp, int cd, double r, String text) { return new Move(id, name, v, power, faith, mp, cd, r, text); }
    private static Passive p(String id, String name, Pas e, String text) { return new Passive(id, name, e, text); }

    public static final List<God> GODS = List.of(
            new God("femto", "femto_god_of_incarnation", "Faith of Femto", "Femto, God of Incarnation", "berserk", "Incarnation and ambition",
                    "Griffith sacrifices his followers at the Eclipse to be reborn as Femto, a member of the God Hand; after the Incarnation Ceremony he rebuilds his army with Apostles, merges the astral and physical worlds into Falconia, and wields causality and spatial manipulation.",
                    "en.wikipedia.org List of Berserk characters (fetched 2026-11-16)", UltimateClass.GOD, 0xE8E0FF, SIG_HELL, "minecraft:ender_eye", 8,
                    List.of(m("eclipse", "Eclipse", Verb.DOMAIN, 0, 3, 900, 1200, 12, "The sky darkens: foes in reach are slowed and weakened, allies are strengthened."),
                            m("spatial_step", "Spatial Step", Verb.BLINK, 8, 1, 400, 200, 14, "Step through space along your gaze and strike what is there when you arrive."),
                            m("causality_lance", "Causality Lance", Verb.BEAM, 14, 2, 700, 300, 22, "A lance of light through every foe in a line."),
                            m("falconia", "Falconia", Verb.WARD, 0, 2, 600, 700, 10, "A kingdom of light: you and allies near you are wrapped in absorbing light."),
                            m("incarnation", "Incarnation", Verb.SURGE, 0, 2, 500, 900, 0, "You are reborn stronger for a while: strength and speed.")),
                    List.of(p("falcon_light", "Falcon of Light", Pas.SIGHT, "You see in the dark."), p("god_hand_vigor", "God Hand Vigor", Pas.REGEN, "Wounds close slowly."), p("apostle_might", "Apostle Might", Pas.STRENGTH, "Your blows are heavier."))),

            new God("lucifero", "lucifero_god_of_devils", "Faith of Lucifero", "Lucifero, God of Devils", "black_clover", "Devils and the Underworld",
                    "Lucifero is the Devil King of the Black Clover underworld, whom the devils (Zagred, Megicula) serve; the mod already fights him as the Spade Kingdom's boss.",
                    "the mod's own boss roster (docs/black_clover_bosses.md); en.wikipedia.org Black Clover returned no match for the devils (not verified on a wiki, flagged in docs/faith_gods.md)",
                    UltimateClass.DRAGON, 0xB01030, SIG_GRAVITY, "minecraft:wither_skeleton_skull", 1,
                    List.of(m("devil_dominion", "Devil's Dominion", Verb.DOMAIN, 0, 3, 900, 1200, 12, "Foes in reach are bound by dread, allies feel the devil's strength."),
                            m("soul_rend", "Soul Rend", Verb.DRAIN, 12, 2, 600, 260, 14, "Tear life from the foe you look at and keep half of it."),
                            m("underworld_maw", "Underworld Maw", Verb.BEAM, 14, 2, 700, 300, 20, "A beam of black fire through a line of foes."),
                            m("blood_pact", "Blood Pact", Verb.SURGE, 0, 2, 500, 900, 0, "A pact of blood: strength and speed for a while."),
                            m("infernal_veil", "Infernal Veil", Verb.WARD, 0, 2, 600, 700, 10, "A veil of hellfire absorbs blows for you and your allies.")),
                    List.of(p("devil_hide", "Devil's Hide", Pas.RESIST, "A devil's skin turns blows aside."), p("dread_sight", "Dread Sight", Pas.SIGHT, "You see in the dark."), p("hellfire_blood", "Hellfire Blood", Pas.FIRE, "Fire cannot burn you."))),

            new God("luminus", "luminus_goddess_of_light", "Faith of Luminus", "Luminus, Goddess of Holy Light", "tensura", "Holy light and worship",
                    "Luminus Valentine, a Demon Lord, is worshipped as the goddess of the Western Holy Empire's Church; her believers are her power.",
                    "Tensura canon from memory, no wiki page fetched (fandom 403); the Nightmares wiki (Mechanics/Faith) gave the 'god and faithful' idea",
                    UltimateClass.KING, 0xFFF3B0, SIG_GODDESS, "minecraft:golden_apple", 3,
                    List.of(m("divine_judgment", "Divine Judgment", Verb.SMITE, 10, 2, 600, 260, 8, "Holy light judges every foe around you."),
                            m("blessing_of_luminus", "Blessing of Luminus", Verb.BLESS, 0, 2, 500, 700, 14, "The faithful near you are strengthened, healed over time and shielded."),
                            m("holy_cleansing", "Holy Cleansing", Verb.PURGE, 0, 2, 400, 500, 12, "Every harmful effect is washed from you and your allies."),
                            m("eternal_night", "Eternal Night", Verb.BIND, 0, 2, 500, 600, 10, "A night falls on foes in reach: they are slowed and weakened."),
                            m("sacred_mending", "Sacred Mending", Verb.HEAL, 8, 2, 400, 400, 12, "Heals you and your allies.")),
                    List.of(p("holy_aura", "Holy Aura", Pas.REGEN, "Wounds close slowly."), p("vampiric_vitality", "Vampiric Vitality", Pas.ABSORB, "A shield of vitality."), p("night_sovereign", "Night Sovereign", Pas.SIGHT, "You see in the dark."))),

            new God("mavis", "mavis_goddess_of_fairy_law", "Faith of Mavis", "Mavis, Goddess of Fairy Law", "fairy_tail", "Bonds and the guild",
                    "Mavis Vermilion, the First Master and Fairy Tactician of Fairy Tail, whose spells are Fairy Sphere, Fairy Law and Fairy Glitter. In canon she is the guild's guardian, not a god: flagged as the weakest god fit of the eight.",
                    "en.wikipedia.org Fairy Tail (fetched, no character detail); spell names from memory, fandom 403",
                    UltimateClass.LORD, 0xF4E3A0, SIG_FAITH, "minecraft:emerald", 16,
                    List.of(m("fairy_sphere", "Fairy Sphere", Verb.WARD, 0, 2, 500, 700, 10, "A sphere of light: you and allies near you gain absorbing shields."),
                            m("fairy_law", "Fairy Law", Verb.SMITE, 10, 3, 800, 400, 9, "Fairy Law: holy light judges every foe in reach."),
                            m("fairy_glitter", "Fairy Glitter", Verb.BEAM, 12, 2, 600, 300, 22, "A beam of light through a line of foes."),
                            m("tacticians_call", "Tactician's Call", Verb.BLESS, 0, 2, 500, 700, 14, "Allies near you are strengthened and healed over time."),
                            m("guild_bond", "Guild Bond", Verb.PURGE, 0, 1, 300, 500, 12, "Bonds wash harmful effects from you and your allies.")),
                    List.of(p("fairy_heart", "Fairy Heart", Pas.REGEN, "Wounds close slowly."), p("tacticians_eye", "Tactician's Eye", Pas.SIGHT, "You see in the dark."), p("guild_haste", "Guild Haste", Pas.HASTE, "You work faster."))),

            new God("ill_ilah", "ill_ilah_god_of_rukh", "Faith of Ill Ilah", "Ill Ilah, God of the Rukh", "magi", "The Rukh and fate",
                    "Ill Ilah is the deity of Alma Toran that King Solomon sealed; later it is corrupted by David, who seeks godhood, and Aladdin and the Magi fight it.",
                    "en.wikipedia.org Magi: The Labyrinth of Magic (fetched 2026-11-16)",
                    UltimateClass.KING, 0xE070FF, SIG_WILL, "minecraft:amethyst_shard", 16,
                    List.of(m("rukh_storm", "Rukh Storm", Verb.SMITE, 10, 2, 600, 260, 8, "A storm of Rukh strikes every foe around you."),
                            m("rukh_wings", "Wings of Rukh", Verb.WARD, 0, 2, 600, 700, 10, "Rukh wings shield you and your allies."),
                            m("destiny_binding", "Destiny Binding", Verb.BIND, 0, 2, 500, 600, 10, "Foes in reach are bound by fate: slowed and weakened."),
                            m("rukh_blessing", "Rukh Blessing", Verb.BLESS, 0, 2, 500, 700, 14, "Allies near you are strengthened and healed over time."),
                            m("rukh_beam", "Rukh Beam", Verb.BEAM, 14, 2, 700, 300, 22, "A beam of Rukh through a line of foes.")),
                    List.of(p("rukh_flow", "Rukh Flow", Pas.SPEED, "You move faster."), p("rukh_sight", "Rukh Sight", Pas.SIGHT, "You see in the dark."), p("rukh_vitality", "Rukh Vitality", Pas.REGEN, "Wounds close slowly."))),

            new God("super_saiyan_god", "ssj_blue_god_of_divine_ki", "Faith of Super Saiyan God", "Super Saiyan Blue, God of Divine Ki", "dragon_ball", "Divine ki",
                    "Goku has his fellow Saiyans perform a ritual so that he can attain the deity-like Super Saiyan God form, then a form beyond it, Super Saiyan Blue; Beerus, the God of Destruction, sought him.",
                    "en.wikipedia.org Dragon Ball Super (fetched 2026-11-16)",
                    UltimateClass.GOD, 0xFF3030, SIG_FIRE_FORCE, "minecraft:redstone_block", 6,
                    List.of(m("god_ki_blast", "God Ki Blast", Verb.BEAM, 14, 2, 700, 300, 24, "A beam of red divine ki through a line of foes."),
                            m("divine_dash", "Divine Dash", Verb.BLINK, 8, 1, 400, 200, 16, "A dash along your gaze that strikes what is there."),
                            m("red_aura", "Red Aura", Verb.SURGE, 0, 2, 500, 900, 0, "Divine ki: strength and speed for a while."),
                            m("ritual_of_six", "Ritual of Six", Verb.BLESS, 0, 3, 900, 900, 14, "Your worshippers lend their ki: allies near you are strengthened and healed over time."),
                            m("hakai_wave", "Destruction Wave", Verb.SMITE, 12, 3, 900, 400, 9, "A wave of destruction around you.")),
                    List.of(p("divine_ki_vitality", "Divine Ki Vitality", Pas.REGEN, "Wounds close slowly."), p("divine_strength", "Divine Strength", Pas.STRENGTH, "Your blows are heavier."), p("god_ki_sense", "God Ki Sense", Pas.SIGHT, "You see in the dark."))),

            new God("ra", "ra_god_of_the_sun", "Faith of Ra", "Ra, God of the Sun", "yu_gi_oh", "The sun and sacrifice",
                    "The Winged Dragon of Ra is one of the three Egyptian God Cards; it is summoned by sacrificing three monsters, which is why offerings are its fuel here.",
                    "Yu-Gi-Oh! canon from memory, the en.wikipedia.org pages were rate limited (429)",
                    UltimateClass.KING, 0xFFC830, SIG_GODDESS, "minecraft:gold_ingot", 32,
                    List.of(m("blaze_cannon", "God Blaze Cannon", Verb.BEAM, 14, 2, 700, 300, 22, "A beam of sun fire through a line of foes."),
                            m("phoenix_flames", "Phoenix Flames", Verb.SMITE, 10, 2, 600, 260, 8, "Flames of the sun burn every foe around you."),
                            m("mighty_sacrifice", "Mighty Sacrifice", Verb.DRAIN, 12, 2, 600, 260, 14, "Take life from the foe you look at: the sacrifice feeds you."),
                            m("suns_blessing", "Sun's Blessing", Verb.HEAL, 8, 2, 400, 400, 12, "The sun heals you and your allies."),
                            m("wings_of_ra", "Wings of Ra", Verb.SURGE, 0, 2, 500, 900, 0, "Wings of fire: strength and speed for a while.")),
                    List.of(p("solar_vigor", "Solar Vigor", Pas.REGEN, "Wounds close slowly."), p("suns_fire", "Sun's Fire", Pas.FIRE, "Fire cannot burn you."), p("radiance", "Radiance", Pas.SIGHT, "You see in the dark."))),

            new God("aqua", "aqua_goddess_of_water", "Faith of Aqua", "Aqua, Goddess of Water", "konosuba", "Water, healing and the Axis cult",
                    "Aqua is the goddess of water, worshipped by the Axis Church, who is sent to the isekai world as a companion of Kazuma; her gift is purification and resurrection.",
                    "Konosuba canon from memory; en.wikipedia.org KonoSuba fetched (series page only); the reference mods also build a Konosuba Faith",
                    UltimateClass.LORD, 0x60C8FF, SIG_FAITH, "minecraft:prismarine_crystals", 16,
                    List.of(m("sacred_cure", "Sacred Cure", Verb.HEAL, 10, 2, 400, 300, 12, "Heals you and your allies."),
                            m("purify", "Purify", Verb.PURGE, 0, 1, 300, 400, 12, "Washes every harmful effect from you and your allies."),
                            m("turn_undead", "Turn Undead", Verb.SMITE, 10, 2, 600, 300, 9, "Holy water judges every foe around you."),
                            m("goddess_blessing", "Goddess's Blessing", Verb.BLESS, 0, 2, 500, 700, 14, "Allies near you are strengthened and healed over time."),
                            m("water_decree", "Water Goddess's Decree", Verb.BEAM, 12, 2, 600, 300, 20, "A beam of water through a line of foes.")),
                    List.of(p("divine_protection", "Divine Protection", Pas.RESIST, "Blows are softened."), p("water_grace", "Water's Grace", Pas.WATER, "You breathe under water."), p("goddess_cure", "Goddess's Cure", Pas.REGEN, "Wounds close slowly."))));

    // ------------------------------------------------------------------ the canon pass (SERVER setting faithGods.canon, docs/faith_gods.md "Canon pass")
    private static Move cm(String id, String name, Verb v, double power, int faith, double mp, int cd, double r, String text, double unlock, int minWorshippers, double hpFrac) {
        return new Move(id, name, v, power, faith, mp, cd, r, text, unlock, minWorshippers, hpFrac);
    }

    /**
     * The same eight lines as {@link #GODS} (same ids, franchises, classes, pool sizes) with the moves, passives, colours and rules read from each series' English wiki. {@link #pick} chooses between the two tables
     * at run time, so the setting off gives the 4.6.0 table untouched. Moves are listed in unlock order: the Unique opens its first three as its mastery grows (0 / 25 / 50 percent), the god form has them all.
     * Slots a class cannot reach (Dragon has 3 actives, Lord and King 4) stay in the table so a class change loses nothing.
     */
    public static final List<God> CANON_GODS = List.of(
            new God("femto", "femto_god_of_incarnation", "Faith of Femto", "Femto, Falcon of Light", "berserk", "Ambition, sacrifice and the Eclipse",
                    "Griffith, the White Falcon, wills the sacrifice of the Band of the Falcon at the Eclipse and is reborn as Femto, fifth of the God Hand, the angels of desire who carry out the will of the Idea of Evil. After the Incarnation Ceremony he returns as the Falcon of Light. He bends space and gravity, repels blows, and no blade of the physical world can harm him.",
                    "berserk.fandom.com (English wiki, fetched): Griffith, God Hand, Idea of Evil, Beherit, Causality, Void", UltimateClass.GOD, 0xE6E6F5, SIG_HELL, "minecraft:heart_of_the_sea", 1,
                    List.of(cm("band_of_the_falcon", "Band of the Falcon", Verb.BLESS, 0.0, 2, 500.0, 700, 14.0, "The White Falcon's charisma: allies near you are strengthened, healed over time and shielded.", 0.0, 0, 0.0),
                            cm("eclipse", "Eclipse", Verb.DOMAIN, 0.0, 4, 900.0, 1200, 12.0, "The Eclipse falls: foes in reach are slowed and weakened, the faithful are empowered. Needs 2 worshippers (the Band is the sacrifice).", 0.25, 2, 0.0),
                            cm("gravity_well", "Gravity Well", Verb.WELL, 10.0, 2, 700.0, 300, 12.0, "Femto's gravity well: foes are pulled in, slowed and crushed.", 0.5, 0, 0.0),
                            cm("spatial_distortion", "Spatial Distortion", Verb.BLINK, 8.0, 1, 400.0, 200, 16.0, "Warp space along your gaze and strike what is there when you arrive.", 0.0, 0, 0.0),
                            cm("falconia", "Falconia", Verb.WARD, 0.0, 2, 600.0, 700, 14.0, "Falconia, the one safe haven: you and allies near you are wrapped in absorbing light.", 0.0, 0, 0.0)),
                    List.of(p("wings_of_darkness", "Wings of Darkness", Pas.LEAP, "Raven black wings: you leap higher."), p("nigh_absoluteness", "Nigh-Absoluteness", Pas.RESIST, "No blade of the physical world cuts you deep."), p("causal_awareness", "Causal Awareness", Pas.SIGHT, "You see the flow of causality: night vision.")),
                    new Canon(0x9A1030, "Faith is the Falcon's following: the dream of the Falcon of Light and the Band that gave itself at the Eclipse. The Eclipse asks a willed sacrifice, so it needs at least 2 worshippers.", "Nothing is gained without sacrifice: Eclipse costs 4 Faith and needs 2 worshippers. Canon says he is not absolute against those outside his story; that is not modelled.", "God Hand member, the fifth angel of desire, nigh-absolute in the physical world (Griffith, God Hand pages).", 1.0, 1.0, false, 0, false)),

            new God("lucifero", "lucifero_god_of_devils", "Faith of Lucifero", "Lucifero, King of Devils", "black_clover", "Gravity and the underworld",
                    "Lucifero is a highest-ranking devil joined to the Tree of Qliphoth, one of the three rulers of the underworld and King of Devils. A devil cannot enter the living world without a gate: Forbidden Magic and a sacrifice call one, and a host can wield only as much power as the gate lets through. He bends gravity, warps space and seizes lesser devils. Anti Magic harms him.",
                    "blackclover.fandom.com (English wiki, fetched): Lucifero, Devil, Zagred, Unite, Magic Knights vs. Lucifero", UltimateClass.DRAGON, 0xB01030, SIG_GRAVITY, "minecraft:wither_skeleton_skull", 1,
                    List.of(cm("gravity_prostration", "Gravity Prostration", Verb.PRESS, 6.0, 3, 800.0, 900, 12.0, "Gravity Magic: the ground drags everyone in reach down; foes are forced to their knees (slowed, weakened) and hurt a little.", 0.0, 0, 0.0),
                            cm("space_warp", "Space Warp", Verb.BLINK, 8.0, 1, 400.0, 200, 14.0, "Warp space along your gaze to dodge a blow and strike what is there.", 0.25, 0, 0.0),
                            cm("half_manifestation", "Half Manifestation", Verb.SURGE, 0.0, 3, 700.0, 1200, 0.0, "Spikes rise on your shoulders: strength and speed for a while.", 0.5, 0, 0.0),
                            cm("crushing_fist", "Crushing Fist", Verb.SMITE, 14.0, 2, 600.0, 260, 6.0, "Brute strength, no craft: a blow that hurts everyone close.", 0.0, 0, 0.0),
                            cm("heart_seizure", "Heart Seizure", Verb.DRAIN, 12.0, 2, 600.0, 260, 14.0, "Take the life of the foe you look at and keep half of it.", 0.0, 0, 0.0)),
                    List.of(p("immense_durability", "Immense Durability", Pas.RESIST, "Slash and fire barely mark you."), p("immense_strength", "Immense Strength", Pas.STRENGTH, "Your blows are heavier."), p("hellfire_hide", "Hellfire Hide", Pas.FIRE, "Fire cannot burn you.")),
                    new Canon(-1, "Faith is the gate: worshippers open the gates of the underworld. Under 3 worshippers the first gate gives x0.7, 3 to 5 give x1.0, 6 or more give x1.3 (no linear devotion).", "The gate caps the power a host can wield. Canon: Anti Magic hurts him; that is not modelled.", "Highest-ranking devil and ruler of the lowest underworld; this repo's Dragon track is the Black Clover devil track (docs/ultimate_classes.md).", 1.0, 1.0, true, 0, false)),

            new God("luminus", "luminus_goddess_of_light", "Faith of Luminus", "Luminus, Lord of Radiance", "tensura", "Holy light, faith and grace",
                    "Luminous Valentine, queen of the vampires and a Demon Lord, is worshipped as the one God of Luminism by the Western Holy Church. Her Secret of Faith and Grace turns believers' faith into holy magic, and Sanctuary borrows the minds of her followers, so the more believers the greater the power. Her skill Lust became Asmodeus and then Ahura Mazda, Lord of Radiance.",
                    "tensura.fandom.com (English wiki, fetched): Luminous Twilight Valentine and its Abilities and gear page, Holy magic, Sanctuary, Holy Field, Western Holy Church, Asmodeus, Ahura Mazda, Embrace Drain, Memory End Requiem, Day-Night Inversion. 'Ruler of the Dark' has no page there", UltimateClass.KING, 0xC2294E, SIG_GODDESS, "minecraft:golden_apple", 3,
                    List.of(cm("holy_field", "Holy Field", Verb.DOMAIN, 0.0, 2, 600.0, 700, 12.0, "A purification barrier: foes in reach are slowed and weakened, allies are strengthened.", 0.0, 0, 0.0),
                            cm("high_heal", "High Heal", Verb.HEAL, 10.0, 2, 400.0, 400, 14.0, "The miracle of High Heal: heals you and your allies.", 0.25, 0, 0.0),
                            cm("energy_drain", "Energy Drain", Verb.DRAIN, 12.0, 2, 600.0, 260, 14.0, "Embrace of life and death: take the life of the foe you look at and keep half of it.", 0.5, 0, 0.0),
                            cm("sanctuary", "Sanctuary Disintegration", Verb.SMITE, 16.0, 4, 1000.0, 1800, 14.0, "The followers' faith crystallised: holy ruin on every foe in reach. Needs 3 worshippers.", 0.0, 3, 0.0),
                            cm("memory_end_requiem", "Memory End Requiem", Verb.BEAM, 14.0, 2, 700.0, 300, 20.0, "A crimson-glowing blade of reaped life through a line of foes.", 0.0, 0, 0.0)),
                    List.of(p("godspeed_regeneration", "Godspeed Regeneration", Pas.REGEN, "Wounds close at once."), p("day_night_inversion", "Day-Night Inversion", Pas.SIGHT, "Night is your element: night vision."), p("dimension_fault", "Dimension Fault", Pas.RESIST, "A dimensional fault turns blows aside.")),
                    new Canon(0xFFF3B0, "Secret of Faith and Grace: faith is her holy magic and Sanctuary borrows her followers' minds, so each worshipper adds 1.5 times the normal devotion.", "Sanctuary needs 3 worshippers and long preparation (90 s). Canon: Holy Field does nothing against holy beings; not modelled.", "Demon Lord of the Octagram with 20,000,000+ EP, between this mod's King gate (15M) and God gate (30M), and the ruler of a faith, not its creator.", 1.0, 1.5, false, 0, false)),

            new God("mavis", "mavis_goddess_of_fairy_law", "Faith of Mavis", "Mavis, the Fairy Tactician", "fairy_tail", "Bonds and the guild",
                    "Mavis Vermillion, the First Master of Fairy Tail, is the guild's founder and guardian, not a god: her body sleeps inside the Fairy Heart and her thought projection watches over it. Fairy Sphere turns the members' bonds and faith into magic power, Fairy Law strikes only those the caster truly sees as enemies, Fairy Glitter is a merciless light gathered from sun, moon and stars.",
                    "fairytail.fandom.com (English wiki, fetched): Mavis Vermillion, Fairy Sphere, Fairy Law, Fairy Glitter, Fairy Heart, Fairy Heart Form, Fairy Tail (Guild)", UltimateClass.LORD, 0xF5E6A8, SIG_FAITH, "minecraft:emerald", 16,
                    List.of(cm("tenrou_illusion", "Tenrou Illusion", Verb.BIND, 0.0, 2, 500.0, 600, 10.0, "An illusory army and beasts of Tenrou: foes in reach are slowed, weakened and made to glow.", 0.0, 0, 0.0),
                            cm("fairy_sphere", "Fairy Sphere", Verb.WARD, 0.0, 3, 700.0, 900, 10.0, "Bonds and faith become an absolute defence: you and allies near you are wrapped in absorbing light. Needs 2 worshippers.", 0.25, 2, 0.0),
                            cm("fairy_law", "Fairy Law", Verb.LAW, 12.0, 3, 800.0, 400, 9.0, "Light on every foe you truly see as an enemy; friends and bystanders are unharmed. The more foes, the more it costs your life.", 0.5, 0, 0.0),
                            cm("fairy_glitter", "Fairy Glitter", Verb.BEAM, 14.0, 3, 800.0, 300, 22.0, "A merciless line of light gathered from sun, moon and stars.", 0.0, 0, 0.0),
                            cm("fairy_heart_form", "Fairy Heart Form", Verb.SURGE, 0.0, 3, 900.0, 1500, 0.0, "White wings and endless magic: strength and speed for a while.", 0.0, 0, 0.0)),
                    List.of(p("fairy_heart", "Fairy Heart", Pas.REGEN, "The Eternal Magic keeps you going: wounds close slowly."), p("fairy_tactician", "Fairy Tactician", Pas.SIGHT, "You read the battle before it happens: night vision."), p("guild_bond", "Guild Bond", Pas.HASTE, "Your hands work faster.")),
                    new Canon(0xE8FFD8, "Fairy Sphere converts the members' bonds and faith into magic power and needs other members' power in unison: it needs 2 worshippers.", "Fairy Law costs the caster's life for every foe beyond the first (4 percent of max health each, at most 24 percent, never below 1 heart).", "Guild founder and guardian, not a god (Mavis page); a guardian-class Lord. Owner decision open: swap for Acnologia, or drop the line.", 1.0, 1.0, false, 0, false)),

            new God("ill_ilah", "ill_ilah_god_of_rukh", "Faith of Ill Ilah", "Ill Ilah, the Black God", "magi", "The Rukh and fate",
                    "Ill Ilah, the Black God, is the will-less lump of power that made Alma Torran, gave humans magic and takes every soul back into the Rukh. Al-Thamen worship it and draw it down through the Black Spot with black rukh born of hatred. Touching the surface of the world, it drains magoi from every living thing. Solomon sealed it away.",
                    "magi.fandom.com (English wiki, fetched): Ill Ilah, Rukh, Al-Thamen, Dark Metal Vessel, Wisdom of Solomon, Solomon Jehoahaz Abraham, Alma Torran", UltimateClass.KING, 0xF2F2FF, SIG_WILL, "minecraft:amethyst_shard", 16,
                    List.of(cm("rukh_guidance", "Rukh's Guidance", Verb.BIND, 0.0, 2, 500.0, 600, 10.0, "Fate pulls: foes in reach are slowed, weakened and made to glow.", 0.0, 0, 0.0),
                            cm("gift_of_magic", "Gift of Magic", Verb.BLESS, 0.0, 2, 500.0, 700, 14.0, "The god's gift: allies near you are strengthened, healed over time and shielded.", 0.25, 0, 0.0),
                            cm("magoi_drain", "Magoi Drain", Verb.DRAIN, 12.0, 2, 600.0, 260, 14.0, "A tendril touches a foe and drains its magoi: you keep half of it.", 0.5, 0, 0.0),
                            cm("black_spot", "Black Spot", Verb.WELL, 12.0, 4, 1000.0, 1200, 14.0, "A hole opens in the sky: foes are pulled in, slowed and crushed.", 0.0, 0, 0.0),
                            cm("black_rukh", "Black Rukh", Verb.SMITE, 12.0, 2, 600.0, 260, 8.0, "A storm of black rukh strikes every foe around you.", 0.0, 0, 0.0)),
                    List.of(p("great_flow", "Great Flow", Pas.REGEN, "Souls return to the Rukh: wounds close slowly."), p("source_of_fate", "Source of Fate", Pas.SIGHT, "You see where the Rukh flows: night vision."), p("black_rukh_veil", "Black Rukh Veil", Pas.ABSORB, "A veil of black rukh soaks blows.")),
                    new Canon(0x5B2A86, "Al-Thamen worship it and feed the Black Spot with black rukh; the god itself has no will, so its faith is plain devotion.", "Black Spot costs 4 Faith. Canon: the god can be sealed again; not modelled.", "Creator of Alma Torran, but a will-less lump of power: a King, not a God. Owner decision open: the page would also support the God class.", 1.0, 1.0, false, 0, false)),

            new God("super_saiyan_god", "ssj_blue_god_of_divine_ki", "Faith of Super Saiyan God", "Super Saiyan Blue", "dragon_ball", "Divine ki",
                    "Six righteous Saiyans pour their ki into one of them, who rises as the Super Saiyan God: crimson hair, a calm mind, godly ki that others can hardly sense. The form drains the body and ends after a short time until it is mastered. Trained further, the Saiyan keeps its power in the natural body and joins it to Super Saiyan as Super Saiyan Blue.",
                    "dragonball.fandom.com (English wiki, fetched): Super Saiyan God, Super Saiyan Blue, Saiyan beyond God, Super Saiyan God (race)", UltimateClass.GOD, 0xFF4A2A, SIG_FIRE_FORCE, "minecraft:redstone_block", 6,
                    List.of(cm("ritual_of_six", "Ritual of Six", Verb.BLESS, 0.0, 3, 900.0, 900, 14.0, "The ritual's ki: allies near you are strengthened, healed over time and shielded. Needs 2 worshippers.", 0.0, 2, 0.0),
                            cm("palm_blast", "Invisible Palm Blast", Verb.BEAM, 10.0, 2, 600.0, 250, 16.0, "Excess ki focused in the palms: an unseen blast through a line of foes.", 0.25, 0, 0.0),
                            cm("aura_of_a_god", "Aura of a God", Verb.BIND, 0.0, 2, 500.0, 700, 8.0, "Godly ki paralyses: foes in reach are slowed and weakened.", 0.5, 0, 0.0),
                            cm("absorb_and_nullify", "Absorb and Nullify", Verb.WARD, 0.0, 2, 600.0, 700, 10.0, "Ki that swallows enemy attacks: you and allies near you gain absorbing shields.", 0.0, 0, 0.0),
                            cm("limitbreaker_kamehameha", "Limitbreaker Kamehameha", Verb.BEAM, 16.0, 3, 1000.0, 500, 26.0, "The beam that cleared the Sphere of Destruction.", 0.0, 0, 0.0)),
                    List.of(p("divine_ki", "Divine Ki", Pas.REGEN, "Godly ki heals you: wounds close slowly."), p("divine_speed", "Divine Speed", Pas.SPEED, "The god form is fast."), p("godly_ki_sense", "Godly Ki Sense", Pas.SIGHT, "You sense ki: night vision.")),
                    new Canon(0x38B8FF, "The ritual needs six righteous Saiyans: the ritual refuses with fewer than 5 worshippers beside the target. Their shared ki is the Ritual of Six.", "Until mastered the form tires the body (weakness). Canon: a short time limit; here it is the weakness, which ends at mastery.", "A form that matches a God of Destruction (Super Saiyan God page: Beerus used 70 percent of his power) reached by a mortal: a God class, as its Blue evolution surpasses it.", 1.0, 1.0, false, 5, true)),

            new God("ra", "ra_god_of_the_sun", "Faith of Ra", "Ra, the Winged Dragon", "yu_gi_oh", "The sun and tribute",
                    "The Winged Dragon of Ra, golden Egyptian God, can be called only by the chosen Pharaoh with three Tributes. Its God Blaze Cannon is fed with life points paid by the summoner, its Phoenix Mode burns as a flaming bird, and in Sphere Mode it is a golden orb. No spell or trap can pick it out.",
                    "yugioh.fandom.com (English wiki, fetched): The Winged Dragon of Ra (original, character), The Sun Dragon Ra, Egyptian God, Ra", UltimateClass.KING, 0xFFC830, SIG_GODDESS, "minecraft:gold_block", 3,
                    List.of(cm("sphere_mode", "Sphere Mode", Verb.WARD, 0.0, 2, 500.0, 700, 8.0, "The golden orb: you and allies near you are wrapped in absorbing shields.", 0.0, 0, 0.0),
                            cm("god_blaze_cannon", "God Blaze Cannon", Verb.BEAM, 14.0, 2, 700.0, 300, 22.0, "A beam of sun fire through a line of foes.", 0.25, 0, 0.0),
                            cm("point_to_point", "Point-to-Point Transfer", Verb.LIFE_BEAM, 14.0, 3, 800.0, 600, 22.0, "Pay your own life points for power: 30 percent of your health becomes up to x1.6 damage in a line of foes.", 0.5, 0, 0.3),
                            cm("egyptian_god_phoenix", "Egyptian God Phoenix", Verb.SMITE, 12.0, 3, 900.0, 500, 9.0, "Ra as a flaming bird: fire of the sun burns every foe around you.", 0.0, 0, 0.0),
                            cm("wrath_of_ra", "Wrath of Ra", Verb.SMITE, 14.0, 3, 900.0, 600, 8.0, "Lightning on those who dare: a storm strikes every foe around you.", 0.0, 0, 0.0)),
                    List.of(p("divine_immunity", "Divine Immunity", Pas.RESIST, "No spell or trap can pick you out: blows are softened."), p("winged_god_dragon", "Winged God Dragon", Pas.LEAP, "Golden wings: you leap higher."), p("blaze_of_the_sun", "Blaze of the Sun", Pas.FIRE, "Fire cannot burn you.")),
                    new Canon(-1, "Three Tributes call Ra (the ritual offering is 3 gold blocks) and the god's power is paid in life points: Point-to-Point Transfer burns your own health for damage.", "Point-to-Point Transfer costs 30 percent of your health (never below 1 heart) for up to x1.6 damage.", "One of three Egyptian God Cards, the strongest card in the series, yet a summoned monster spirit: King class (Egyptian God, Ra pages).", 1.0, 1.0, false, 0, false)),

            new God("aqua", "aqua_goddess_of_water", "Faith of Aqua", "Aqua, Goddess of Water", "konosuba", "Water, healing and the Axis Order",
                    "Aqua, goddess of water and patron of the Axis Order, fell to the mortal world as an Arch Priest. Her power follows the devotion of her followers, she heals, purifies, dispels and even resurrects, but her attacks only hurt demons and the undead. She walks on water, breathes under it and turns what she touches into holy water.",
                    "konosuba.fandom.com (English wiki, fetched): Aqua, Axis Order", UltimateClass.LORD, 0x60C8FF, SIG_FAITH, "minecraft:prismarine_crystals", 16,
                    List.of(cm("heal", "Heal", Verb.HEAL, 10.0, 1, 300.0, 200, 12.0, "Instantly heals you and your allies.", 0.0, 0, 0.0),
                            cm("purification", "Purification", Verb.PURGE, 0.0, 1, 300.0, 400, 12.0, "Purifies: every harmful effect is washed from you and your allies.", 0.25, 0, 0.0),
                            cm("turn_undead", "Turn Undead", Verb.HOLY, 10.0, 2, 600.0, 300, 9.0, "A light that sends the undead away: heavy damage to undead, little to anything else.", 0.5, 0, 0.0),
                            cm("resurrection", "Resurrection", Verb.HEAL, 18.0, 3, 900.0, 1200, 12.0, "The Arch Priest's greatest spell: a mighty heal for you and your allies (a true revival of the dead is not possible here).", 0.0, 0, 0.0),
                            cm("sacred_exorcism", "Sacred Exorcism", Verb.HOLY, 14.0, 3, 900.0, 400, 12.0, "A pillar of light: heavy damage to undead, little to anything else.", 0.0, 0, 0.0)),
                    List.of(p("divine_protection", "Divine Protection", Pas.RESIST, "A light turns blows aside."), p("goddess_of_water", "Goddess of Water", Pas.WATER, "You breathe under water."), p("enhanced_vision", "Enhanced Vision", Pas.SIGHT, "You see in the dark.")),
                    new Canon(-1, "As a goddess her power correlates to the devotion of her followers: base strength x0.7, each worshipper counts double.", "Her attacks are effective only against the undead (x1.6), weak against everything else (x0.6). Weakened after the fall: base x0.7.", "A goddess of the lowest rank who guides souls, weakened on the mortal plane, healer-specialised: Lord class (Aqua page).", 0.7, 2.0, false, 0, false)));

    /** The table the setting selects: a god's canon row when {@code canon} is on, else the 4.6.0 row itself. */
    public static God pick(God g, boolean canon) {
        if (g == null || !canon) return g;
        for (God c : CANON_GODS) if (c.id().equals(g.id())) return c;
        return g;
    }

    /** The effect colour of a form: the god form's own colour in canon when it has one (Super Saiyan Blue is blue), else the line's. */
    public static int rgbOf(God g, boolean ultimate) {
        Canon c = g.canonRules();
        return ultimate && c != null && c.ultRgb() >= 0 ? c.ultRgb() : g.rgb();
    }

    /** The Unique opens its moves with mastery (canon); the god form has all of its moves; a 4.6.0 move (unlock 0) is always open. */
    public static boolean unlocked(Move m, boolean ultimate, double masteryFrac) {
        return ultimate || m.unlock() <= 0.0 || (Double.isFinite(masteryFrac) && masteryFrac + 1e-9 >= m.unlock());
    }

    /** Mastery points one cast earns: 1 as before, or 0.5 percent of the maximum (at least 1) for a canon Unique, so its unlocks come at a playable rate. */
    public static double masteryGain(boolean canonUnique, int maxMastery) {
        return canonUnique ? Math.max(1.0, Math.max(0, maxMastery) * 0.005) : 1.0;
    }

    /** True when the worshippers are enough for the move (canon minimum; 0 = none). */
    public static boolean enoughWorshippers(Move m, int worshippers) { return m.minWorshippers() <= 0 || worshippers >= m.minWorshippers(); }

    /** True when the ritual may run for this worshipper count (canon: Super Saiyan God needs five). */
    public static boolean ritualWorshippersOk(God g, int worshippers) {
        Canon c = g.canonRules();
        return c == null || worshippers >= c.ritualWorshippers();
    }

    /** Lucifero's gates: 1 under 3 worshippers, 2 from 3, 3 from 6. */
    public static int gateOf(int worshippers) { return worshippers >= 6 ? 3 : worshippers >= 3 ? 2 : 1; }

    public static double gateFactor(int worshippers) { return switch (gateOf(worshippers)) { case 3 -> 1.3; case 2 -> 1.0; default -> 0.7; }; }

    /** Holy magic against the undead is heavy, against everything else light (Aqua: her attacks are effective only against the undead). */
    public static double holyFactor(boolean undead) { return undead ? 1.6 : 0.6; }

    /** The health Fairy Law costs, as a fraction of max health: 4 percent for every foe beyond the first, at most 24 percent. */
    public static double lawCostFrac(int foes) { return Math.min(0.24, 0.04 * Math.max(0, foes - 1)); }

    /** The health actually paid: the wished fraction of max health, never taking the payer under 2 health points (one heart). */
    public static double hpPaid(double hp, double maxHp, double frac) {
        if (!(hp > 0) || !(maxHp > 0) || !(frac > 0)) return 0.0;
        return Math.max(0.0, Math.min(maxHp * Math.min(1.0, frac), hp - 2.0));
    }

    /** Point-to-Point Transfer: the damage factor of the health paid (the full 30 percent of max health gives x1.6, nothing gives x1). */
    public static double lifeBeamFactor(double paid, double maxHp) { return maxHp > 0 ? 1.0 + 2.0 * Math.max(0.0, paid) / maxHp : 1.0; }

    /** True while the canon Unique that has not been mastered drains the body (Super Saiyan God: the form tires you until you master it). */
    public static boolean tollApplies(God g, boolean ultimate, boolean mastered) {
        Canon c = g.canonRules();
        return c != null && c.unmasteredToll() && !ultimate && !mastered;
    }

    public static God god(String id) {
        if (id == null) return null;
        String k = id.toLowerCase(Locale.ROOT);
        for (God g : GODS) if (g.id().equals(k) || g.uniqueId().equals(k) || g.ultimateId().equals(k)) return g;
        return null;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (God g : GODS) out.add(g.id());
        return out;
    }

    // ------------------------------------------------------------------ what a form has
    public static int activeCount(God g, boolean ultimate) { return ultimate ? Math.min(POOL_ACTIVES, spec(g.cls()).actives()) : UNIQUE_ACTIVES; }

    public static int passiveCount(God g, boolean ultimate) { return ultimate ? Math.min(POOL_PASSIVES, spec(g.cls()).passives()) : UNIQUE_PASSIVES; }

    public static List<Move> actives(God g, boolean ultimate) { return g.moves().subList(0, activeCount(g, ultimate)); }

    public static List<Passive> passives(God g, boolean ultimate) { return g.passives().subList(0, passiveCount(g, ultimate)); }

    // ------------------------------------------------------------------ strength
    /** The most the worshippers can add to a god's strength (x1 + this). */
    public static final double DEVOTION_CAP = 1.0;

    /** One worshipper adds {@code percentEach} % to the god's strength, at most {@link #DEVOTION_CAP}; a bad input is x1. */
    public static double devotion(int worshippers, int percentEach) {
        if (worshippers <= 0 || percentEach <= 0) return 1.0;
        double add = Math.min(FaithWorshipRules.MAX_CAP, worshippers) * Math.min(20, percentEach) / 100.0;
        return 1.0 + Math.min(DEVOTION_CAP, add);
    }

    /** The factor an active's raw number is multiplied by: the form's strength, the devotion, the server's percent. */
    public static double strength(God g, boolean ultimate, int worshippers, int devotionPercent, int powerPercent) {
        double form = ultimate ? spec(g.cls()).power() : UNIQUE_POWER;
        double pct = Math.max(10, Math.min(300, powerPercent)) / 100.0;
        Canon c = g.canonRules();
        if (c == null) return form * devotion(worshippers, devotionPercent) * pct;
        double dev = c.gates() ? gateFactor(worshippers) : devotion(worshippers, (int) Math.round(Math.max(0, devotionPercent) * c.devotionMult()));
        return form * dev * pct * c.base();
    }

    /** The potion amplifier of this god's passives: the class's, +1 from 8 worshippers, at most 2. */
    public static int passiveAmp(God g, boolean ultimate, int worshippers) {
        int base = ultimate ? spec(g.cls()).passiveAmp() : 0;
        return Math.min(2, base + (worshippers >= 8 ? 1 : 0));
    }

    /** How many of a god's worshippers share its first passive (a cap so one second of passives is bounded). */
    public static final int MAX_SHARED = 16;

    // ------------------------------------------------------------------ evolution
    /** The EP the god form wants before it evolves, with the server's percent of the table (0 = no EP gate). */
    public static double epGate(God g, int gatePercent) {
        int pct = Math.max(0, Math.min(1000, gatePercent));
        return spec(g.cls()).epGate() * pct / 100.0;
    }

    public static boolean epReady(God g, double ep, int gatePercent) {
        double need = epGate(g, gatePercent);
        return need <= 0 || (Double.isFinite(ep) && ep >= need);
    }

    // ------------------------------------------------------------------ the ritual (/faith ascend): adapted from the Nightmares 'become a god' rule, 3 of 4 proofs
    public enum Ritual { OK, DISABLED, NOT_A_FAITH_HOLDER, ALREADY, TOO_MANY_LINES, COOLDOWN, NO_SHRINE, NO_OFFERING, FEW_PROOFS, NO_FAITH }

    public static final int PROOFS_NEEDED = 3;

    /** How many of the four proofs are met: an Ultimate skill held, enough worshippers, enough Faith offered by them, the Faith skill mastered. */
    public static int proofs(boolean ultimateHeld, int worshippers, int offered, boolean faithMastered, int minWorshippers, int minOffered) {
        int n = 0;
        if (ultimateHeld) n++;
        if (worshippers >= Math.max(0, minWorshippers)) n++;
        if (offered >= Math.max(0, minOffered)) n++;
        if (faithMastered) n++;
        return n;
    }

    /** The first reason the ritual cannot run, or OK (the order is the order the player is told in). */
    public static Ritual ritual(boolean enabled, boolean faithHolder, boolean alreadyHas, int linesHeld, int maxLines, boolean cooledDown,
                                boolean onShrine, boolean hasOffering, int proofs, int faith, int faithCost) {
        if (!enabled) return Ritual.DISABLED;
        if (!faithHolder) return Ritual.NOT_A_FAITH_HOLDER;
        if (alreadyHas) return Ritual.ALREADY;
        if (linesHeld >= Math.max(1, Math.min(GODS.size(), maxLines))) return Ritual.TOO_MANY_LINES;
        if (!cooledDown) return Ritual.COOLDOWN;
        if (!onShrine) return Ritual.NO_SHRINE;
        if (!hasOffering) return Ritual.NO_OFFERING;
        if (proofs < PROOFS_NEEDED) return Ritual.FEW_PROOFS;
        if (faith < Math.max(0, faithCost)) return Ritual.NO_FAITH;
        return Ritual.OK;
    }

    /** True once {@code last} is far enough behind {@code now} (a clock that went back counts as ready). */
    public static boolean ritualReady(long last, long now, int gapTicks) { return last < 0 || now < last || now - last >= Math.max(0, gapTicks); }
}
