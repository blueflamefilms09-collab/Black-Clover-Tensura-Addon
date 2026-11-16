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
    public enum Verb { BLESS, HEAL, SMITE, BEAM, WARD, BIND, BLINK, DRAIN, PURGE, SURGE, DOMAIN }

    /** What a passive does: one potion effect the god keeps on itself (and on its worshippers, for the first passive) while the skill is switched on. */
    public enum Pas { REGEN, RESIST, STRENGTH, SPEED, ABSORB, SIGHT, LEAP, FIRE, HASTE, WATER }

    /** One active: {@code power} is raw damage / healing before the class and devotion scaling, {@code faith} the Faith points it burns, {@code mp} its flat magicule price, {@code radius} its reach in blocks. */
    public record Move(String id, String name, Verb verb, double power, int faith, double mp, int cooldownTicks, double radius, String text) {}

    public record Passive(String id, String name, Pas effect, String text) {}

    /**
     * One god line. {@code name} is the Unique ("Faith of Femto"), {@code ultimateName} the god form it evolves into. {@code sigil} is a SkillAnimeFx sigil style, {@code rgb} the colour of every effect.
     * {@code offering} / {@code offeringCount} are what the ritual burns (a vanilla item id), {@code source} where the canon facts came from.
     */
    public record God(String id, String ultimateId, String name, String ultimateName, String franchise, String domain, String canon, String source,
                      UltimateClass cls, int rgb, int sigil, String offering, int offeringCount, List<Move> moves, List<Passive> passives) {
        public String uniqueId() { return "faith_of_" + id; }
    }

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
        return form * devotion(worshippers, devotionPercent) * pct;
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
