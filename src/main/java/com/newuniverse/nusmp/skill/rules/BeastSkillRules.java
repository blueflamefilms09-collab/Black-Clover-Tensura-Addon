package com.newuniverse.nusmp.skill.rules;

import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Move;
import com.newuniverse.nusmp.skill.rules.DbRules.Prim;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.58.0: the Beast Will skills as pure data (no Minecraft types). Every one of the 27 Beast Wills (the nine of 3.45.0 and the eighteen legendary and mythic ones) is a PAIR of Tensura skills: a Unique
 * ({@code bw_<key>_unique}) that evolves, once mastered, into an Ultimate ({@code bw_<key>}), exactly the Dragon Ball pattern of {@link DbRules}. The skill ids are static (skills register before the
 * datapacks load), so this table is fixed: the key is the will id without {@code _will}. The skills are independent of the will binding and the will assimilation (those stay as they are).
 * A passive is a {@link Pas} kind with two numbers; an active is a {@link DbRules.Mode} whose {@link Move} is a primitive the dragon-ball dispatcher already runs (beam, blast, rush, ...) or one of the four
 * the beast dispatcher runs itself (guard, veil, mend, mark). Power is the beast's own {@link Spec#scale()}: legendary beasts are stronger.
 */
public final class BeastSkillRules {
    private BeastSkillRules() {}

    /** The passive kinds (what a beast does while its skill is switched ON). Numbers: amount and radius per {@link Passive}. */
    public enum Pas { SWIFT, REGEN, ARMOR, STEALTH, VENOM, BLIGHT, FLIGHT, ILLUSION, FROST, FURY, BOND, TIDE, MIGHT, SENSE, SCORCH }

    /** One passive: the id (lang key part, never renamed), the name, the kind, an amount and a radius in blocks. */
    public record Passive(String id, String name, Pas kind, double amount, double radius) {}

    /**
     * One beast. {@code fx} is the finisher's own look as {@code CLASS:SHAPE}: L = a line from the caster to the aimed point, G = on the ground at the aimed point, F = a gate or maw in front of the caster
     * facing the aim, S = round the caster. SHAPE is a name of {@code vfx.VfxShape}.
     */
    public record Spec(String key, String title, String epithet, String uniqueName, int rgb, double scale, boolean legendary, String fx, String power, List<Passive> passives, List<Mode> modes) {
        public String id() { return "bw_" + key; }
        public String uniqueId() { return id() + "_unique"; }
        public String willId() { return key + "_will"; }
        public List<Mode> actives() { return modes; }
        /** The actives the Unique form has: the first two that are not the mastery finisher. */
        public List<Mode> uniqueActives() { List<Mode> l = new ArrayList<>(); for (Mode m : modes) if (!m.mastery() && l.size() < 2) l.add(m); return l; }
        public Passive firstPassive() { return passives.isEmpty() ? null : passives.get(0); }
        public Mode finisher() { for (Mode m : modes) if (m.mastery()) return m; return null; }
        /** The passives a form has: all of an Ultimate, the first of a Unique. */
        public List<Passive> passivesOf(boolean ult) { return ult ? passives : (passives.isEmpty() ? List.of() : List.of(passives.get(0))); }
        /** The same beast as a Dragon Ball spec, so the shared move dispatcher can run its actives. */
        public DbRules.Spec asDb() { return new DbRules.Spec(id(), title, epithet, uniqueName, scale, power, modes); }
    }

    // ---------------------------------------------------------------- the numbers
    /** The Unique form is this fraction of an Ultimate's strength (the same as the Dragon Ball pairs). */
    public static final double UNIQUE_FACTOR = DbRules.UNIQUE_FACTOR;
    /** Most creatures one press or one passive second may touch, most projectiles removed, the ticks between two passive seconds. */
    public static final int MAX_TARGETS = DbRules.MAX_TARGETS, MAX_PROJECTILES = DbRules.MAX_PROJECTILES, PASSIVE_PERIOD_TICKS = DbRules.PASSIVE_PERIOD_TICKS;
    /** Most foes a passive aura touches per second, most allies a bond touches. */
    public static final int AURA_FOES = 8, BOND_ALLIES = 8;
    /** Share of the maximum health a mend heals, and a healing finisher heals the caster. */
    public static final double MEND_SHARE = 0.30, FINISHER_HEAL_SHARE = 0.20, FURY_BELOW = 0.35;

    /** The strength factor of a form: 1.0 for an Ultimate, the unique fraction for a Unique, times the beast's own scale. */
    public static double power(Spec s, boolean ultimate) { return s.scale() * (ultimate ? 1.0 : UNIQUE_FACTOR); }

    /** Melee dodge chance of an illusion passive for a form; projectiles are dodged 80 % as often. */
    public static double dodge(Passive p, boolean ultimate) { return Math.min(0.6, p.amount() * (ultimate ? 1.0 : UNIQUE_FACTOR + 0.25)); }

    /** True when the low-health surge is on: at or below the share of the maximum health. */
    public static boolean lowHealth(double health, double max) { return max > 0.0 && health / max <= FURY_BELOW; }

    // ---------------------------------------------------------------- tiny builders
    private static Passive p(String id, String name, Pas k, double amount, double radius) { return new Passive(id, name, k, amount, radius); }

    private static Mode act(String id, String name, boolean crouch, Prim prim, int cost, int cd, double dmg, double range, double radius, int n, int lock, int flags) {
        return new Mode(id, name, crouch, false, cost, cost, cd, cd, new Move(prim, dmg, range, radius, n, 0, lock, flags, 0));
    }

    private static Mode a(String id, String name, Prim prim, int cost, int cd, double dmg, double range, double radius, int n, int lock, int flags) { return act(id, name, false, prim, cost, cd, dmg, range, radius, n, lock, flags); }

    private static Mode c(String id, String name, Prim prim, int cost, int cd, double dmg, double range, double radius, int n, int lock, int flags) { return act(id, name, true, prim, cost, cd, dmg, range, radius, n, lock, flags); }

    /** The mastery-only finisher: the mastered price is two thirds, the mastered cooldown a little over half. */
    private static Mode f(String id, String name, Prim prim, int cost, int cd, double dmg, double range, double radius, int n, int lock, int flags) {
        return new Mode(id, name, false, true, cost, cost * 2 / 3, cd, cd * 55 / 100, new Move(prim, dmg, range, radius, n, 0, lock, flags, 0));
    }

    private static Spec b(String key, String title, String epithet, String uniqueName, int rgb, double scale, boolean legendary, String fx, String power, List<Passive> passives, Mode... modes) {
        List<Mode> l = new ArrayList<>();
        for (Mode m : modes) {
            Move v = m.move();
            l.add(new Mode(m.id(), m.name(), m.crouch(), m.mastery(), m.cost(), m.masteredCost(), m.cooldown(), m.masteredCooldown(),
                    new Move(v.prim(), v.dmg(), v.range(), v.radius(), v.n(), v.debt(), v.lock(), v.flags(), rgb)));
        }
        return new Spec(key, title, epithet, uniqueName, rgb, scale, legendary, fx, power, List.copyOf(passives), List.copyOf(l));
    }

    private static final int BYP = DbRules.BYPASS, PAR = DbRules.PARALYZE, STUN = DbRules.STUN, FRZ = DbRules.FREEZE_PROJ, SNAP = DbRules.SNAP, BLIND = DbRules.BLIND, BLEED = DbRules.BLEED,
            HOME = DbRules.HOMING, IGN = DbRules.IGNORE_FRAMES, ARM = DbRules.ARMOR_BREAK, HEAL = DbRules.HEAL, MARK = DbRules.BURY;

    // ---------------------------------------------------------------- the nine wills of 3.45.0 (remade in 3.60.0)
    public static final Spec DRAGON = b("dragon", "Dragon Sovereign", "Wrath of the Mana Dragon", "Wyrmheart", 0xFFB9A4FF, 1.0, true, "G:SPACE_TEAR",
            "Dragon: The Dragon's Will, an Asuran dragon's will of the Indrath line (Aether) that Sylvia gave to Arthur",
            List.of(p("draconic_hide", "Draconic Hide", Pas.ARMOR, 6, 0), p("dragon_sight", "Dragon Sight", Pas.SENSE, 0, 28), p("draconic_vigor", "Draconic Vigor", Pas.REGEN, 0.015, 0)),
            a("dragon_breath", "Dragon Breath", Prim.BEAM, 3000, 12, 85, 52, 2.0, 12, 40, ARM),
            a("aether_stasis", "Aether Stasis (Static Void)", Prim.STOP, 2600, 14, 40, 28, 4, 24, 30, FRZ | SNAP),
            f("dragon_kings_annihilation", "Dragon King's Annihilation", Prim.BEAM, 28000, 130, 150, 70, 4, 24, 60, BYP | FRZ | ARM | STUN));

    public static final Spec SHADOW_PANTHER = b("shadow_panther", "Moonless Sovereign", "The Silence That Hunts", "Night Prowler", 0xFF5B3FA8, 0.7, false, "G:SHADOW_POOL",
            "Shadow Panther: A panther of moonless dark that hushes wind and sound until it is only a rumour; the holder first borrows its reflexes",
            List.of(p("night_stride", "Night Stride", Pas.SWIFT, 0.25, 0), p("shadow_cloak", "Shadow Cloak", Pas.STEALTH, 0, 0), p("predators_grace", "Predator's Grace", Pas.SENSE, 0, 16)),
            a("ambush_strike", "Windsilence Pounce", Prim.RUSH, 1800, 8, 40, 16, 0, 1, 20, SNAP | BLIND),
            a("claw_flurry", "Sound-Shear Claws", Prim.BARRAGE, 3200, 14, 8, 8, 0, 10, 40, BLEED | STUN),
            f("eclipse_of_a_thousand_claws", "Moonless Hush", Prim.SEAL, 20000, 110, 0, 26, 0, 1, 160, PAR));

    public static final Spec ELDERWOOD_GUARDIAN = b("elderwood_guardian", "Elder Grove Sovereign", "Heart of the Corrupted Grove", "Corrupted Grove", 0xFF1FBF5A, 0.7, false, "G:TREE_ROOTS",
            "Elderwood Guardian: The will of a Vritra-corrupted, Hamadryad-like grove guardian",
            List.of(p("bark_hide", "Bark Hide", Pas.ARMOR, 6, 0), p("creeping_blight", "Creeping Blight", Pas.BLIGHT, 0, 8), p("grove_sense", "Grove Sense", Pas.SENSE, 0, 16)),
            a("vine_lance", "Vine Lance", Prim.BEAM, 1800, 8, 45, 20, 1.0, 4, 30, STUN),
            a("restraining_roots", "Restraining Roots", Prim.SEAL, 3400, 22, 0, 26, 0, 1, 100, PAR),
            f("heart_of_the_elder_grove", "Heart of the Elder Grove", Prim.BLAST, 22000, 120, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec LION = b("lion", "Sovereign of the Savanna", "King of the Golden Savanna", "Pride King", 0xFFE8B020, 0.68, false, "L:FIRE_LION",
            "Lion: A pride-king lion's will: the first stage lends a lion's poise and a cub's roar, the second makes a lion king whose",
            List.of(p("mane_of_might", "Mane of Might", Pas.MIGHT, 2.0, 0), p("pride_bond", "Pride's Watch", Pas.SENSE, 0, 16), p("lions_courage", "Lion's Courage", Pas.FURY, 0, 0)),
            a("world_howl_beam", "World Howl", Prim.BEAM, 3000, 12, 85, 52, 2.0, 12, 40, ARM),
            a("royal_pounce", "Royal Pounce", Prim.RUSH, 2800, 10, 65, 18, 0, 1, 20, SNAP),
            f("sovereigns_roar", "Sovereign's Roar", Prim.STOP, 24000, 120, 100, 24, 6, 24, 100, STUN | SNAP | BYP | FRZ));

    public static final Spec RIDGE_WOLF = b("ridge_wolf", "Moonfang Alpha", "Alpha of the Ridge", "Pack Hunter", 0xFF79B1BA, 0.62, false, "L:SLASH_WAVE",
            "Ridge-Elder Fang: The will of the elder wolf that rules a mountain ridge: it never loses a trail, hamstrings fleeing prey and calls its",
            List.of(p("pack_runner", "Pack Runner", Pas.SWIFT, 0.22, 0), p("keen_nose", "Keen Nose", Pas.SENSE, 0, 24), p("pack_bond", "Pack Bond", Pas.BOND, 0, 20)),
            a("fang_bite", "Fang Bite", Prim.RUSH, 1500, 8, 35, 14, 0, 2, 0, BLEED),
            a("pin_down", "Pin Down", Prim.SEAL, 2400, 16, 0, 20, 0, 1, 80, PAR),
            f("hunt_of_a_thousand_fangs", "The Long Hunt", Prim.BARRAGE, 15000, 100, 11, 10, 0, 20, 60, BLEED | IGN | STUN));

    public static final Spec CRESTED_BOAR = b("crested_boar", "Avalanche Sovereign", "Unstoppable Tusk of the Highlands", "Highland Tusker", 0xFF8E7541, 0.62, false, "L:EARTH_RAMPAGE",
            "Crested Boar: The will of a highland tusker so old its bristle crest has turned to stone: it does not dodge, it walks through the",
            List.of(p("thick_hide", "Thick Hide", Pas.ARMOR, 5, 0), p("unstoppable_charge", "Unstoppable Charge", Pas.MIGHT, 1.5, 0), p("boars_fury", "Boar's Fury", Pas.FURY, 0, 0)),
            a("gore", "Gore", Prim.RUSH, 1800, 9, 55, 16, 0, 1, 20, SNAP | ARM),
            a("trample", "Trample", Prim.STOP, 2800, 14, 45, 22, 3, 16, 40, SNAP | FRZ),
            f("crested_avalanche", "Crested Avalanche", Prim.BLAST, 16000, 100, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec WIND_SKIMMER = b("wind_skimmer", "Tempest Skimmer", "Rider of the Endless Gale", "Gale Skimmer", 0xFF2CFAFF, 0.62, false, "L:WIND_ZEPHYR",
            "Wind-Skimmer Raptor: The will of an elder raptor that skims the top of the gale: it never lands hard, fades into clouds and falls on prey",
            List.of(p("gale_stride", "Gale Stride", Pas.SWIFT, 0.3, 0), p("featherfall", "Featherfall", Pas.FLIGHT, 0, 0), p("wind_veil", "Wind Veil", Pas.ILLUSION, 0.22, 0)),
            a("gale_cut", "Gale Cut", Prim.BEAM, 1700, 7, 40, 36, 1.2, 6, 0, 0),
            a("skimming_dash", "Skimming Dash", Prim.RUSH, 1800, 9, 35, 22, 0, 1, 20, SNAP),
            f("skimmers_tempest", "Skimmer's Tempest", Prim.BEAM, 18000, 100, 80, 60, 3, 24, 40, FRZ | SNAP));

    public static final Spec FLAME_SERPENT = b("flame_serpent", "Cremation Sovereign", "Elder of the Cooling Lava", "Molten Coil", 0xFFFF3A00, 0.66, false, "L:FIRE_SPIRAL",
            "Flame-Serpent: The will of an elder serpent whose scales glow like cooling lava: it senses living heat, hides in heat haze, spits",
            List.of(p("ember_scales", "Ember Scales", Pas.ARMOR, 3, 0), p("serpents_heat", "Serpent's Heat", Pas.SCORCH, 0, 5), p("coil_regeneration", "Coil Regeneration", Pas.REGEN, 0.012, 0)),
            a("fire_fang", "Fire Fang", Prim.RUSH, 1800, 8, 50, 14, 0, 1, 0, BLEED | SNAP),
            a("ember_spit", "Ember Spit", Prim.BEAM, 2000, 9, 55, 40, 1.2, 6, 0, ARM),
            f("serpents_cremation", "Serpent's Cremation", Prim.BEAM, 22000, 120, 115, 56, 3, 24, 60, BYP | ARM));

    public static final Spec THUNDER_FANG_CHIMERA = b("thunder_fang_chimera", "Thunder-Fang Sovereign", "Lion, Ram and Serpent of the Storm", "Three-Headed Tempest", 0xFFF4F25C, 0.72, false, "G:LIGHTNING_GOD",
            "Thunder-Fang Chimera: The will of a three-bodied storm beast: a lion's fangs, a ram's charge and a venomous serpent tail, all crackling with",
            List.of(p("thunder_pace", "Thunder Pace", Pas.SWIFT, 0.22, 0), p("chimeras_hunger", "Chimera's Hunger", Pas.MIGHT, 2.0, 0), p("storm_hide", "Storm Hide", Pas.ARMOR, 4, 0)),
            a("thunder_fang", "Thunder Fang", Prim.RUSH, 2000, 8, 60, 18, 0, 1, 20, IGN | STUN),
            a("chain_bolt", "Ram's Chain Bolt", Prim.BEAM, 2200, 9, 60, 46, 1.4, 8, 30, STUN),
            f("skyfall_of_the_chimera", "Skyfall of the Chimera", Prim.BLAST, 24000, 120, 110, 40, 14, 24, 100, PAR | FRZ | BYP | SNAP));

    // ---------------------------------------------------------------- the eighteen legendary and mythic wills (remade in 3.60.0)
    public static final Spec FENRIR = b("fenrir", "Fenrir, Chain-Breaker", "The Wolf Who Breaks the Chain", "Loyal Soul-Light", 0xFF87F1FF, 0.95, false, "G:SEAL_CHAINS",
            "Fenrir: A great pale wolf whose will is loyalty itself: a chain-breaker whose jaws gape wide at the end of the world",
            List.of(p("soul_light", "Soul-Light", Pas.REGEN, 0.02, 0), p("frostbound_coat", "Frostbound Coat", Pas.FROST, 0, 9), p("loyal_stride", "Loyal Stride", Pas.SWIFT, 0.08, 0)),
            a("garms_pounce", "Garm's Pounce", Prim.RUSH, 2800, 10, 60, 20, 0, 2, 20, SNAP | IGN),
            a("soul_frost_howl", "Soul-Frost Howl", Prim.BLAST, 2400, 12, 55, 0, 10, 24, 40, SNAP | STUN),
            f("gleipnir_breaks", "Gleipnir Breaks", Prim.BEAM, 26000, 130, 150, 70, 4, 24, 60, BYP | FRZ | ARM | STUN));

    public static final Spec JORMUNGANDR = b("jormungandr", "The World-Circling Serpent", "The Serpent Who Circles the World", "Sea Serpent Jormungandr", 0xFF1F4FB0, 0.96, false, "G:WATERFX_ZONE",
            "Jormungandr: The Midgard Serpent circles the world sea with its tail in its mouth; its venom meets Thor at Ragnarok",
            List.of(p("world_serpent_scales", "World-Serpent Scales", Pas.ARMOR, 7, 0), p("encircling_venom", "Encircling Venom", Pas.VENOM, 0, 6), p("ocean_blood", "Ocean Blood", Pas.TIDE, 0, 0)),
            a("venom_fang", "Venom Fang", Prim.RUSH, 2200, 9, 55, 16, 0, 1, 20, BLEED),
            a("tail_lash", "Tail Lash", Prim.STOP, 2600, 12, 50, 24, 4, 20, 30, FRZ | SNAP),
            f("world_ring", "World Ring", Prim.STOP, 30000, 140, 100, 24, 6, 24, 100, STUN | SNAP | BYP | FRZ));

    public static final Spec CERBERUS = b("cerberus", "Threefold Hellgate", "Keeper of the Threshold", "Hound of the Gate", 0xFF8C1010, 0.85, false, "F:SEAL_TRINITY",
            "Cerberus: A giant mana hound with three snarling heads whose will keeps a threshold: nothing it guards gets out and nothing",
            List.of(p("gatekeeper_hide", "Gatekeeper's Hide", Pas.ARMOR, 5, 0), p("three_heads_vigil", "Three Heads' Vigil", Pas.SENSE, 0, 26), p("hellhound_fury", "Hellhound Fury", Pas.FURY, 0, 0)),
            a("fire_bite", "Hellfire Bite", Prim.RUSH, 2200, 9, 55, 16, 0, 2, 0, BLEED | SNAP),
            a("triple_bark", "Triple Bark", Prim.BLAST, 2400, 12, 45, 0, 9, 24, 40, STUN | SNAP),
            f("hellgate_closes", "Hellgate Closes", Prim.SEAL, 24000, 120, 0, 26, 0, 1, 160, PAR));

    public static final Spec MINOTAUR = b("minotaur", "Asterion, Lord of the Labyrinth", "The Beast at the Maze's Heart", "Bull of the Labyrinth", 0xFF534429, 0.75, false, "G:TRIAL_LABYRINTH",
            "Minotaur: A bull-headed giant of the stone maze, enraged and untiring, who always finds the way to its prey",
            List.of(p("bull_hide", "Bull Hide", Pas.ARMOR, 6, 0), p("maze_sense_pas", "Maze-Sense", Pas.SENSE, 0, 24), p("endless_rage", "Endless Rage", Pas.FURY, 0, 0)),
            a("labyrinth_charge", "Labyrinth Charge", Prim.RUSH, 2400, 9, 70, 20, 0, 1, 20, SNAP | ARM),
            a("labrys_cleave", "Labrys Cleave", Prim.BLAST, 2000, 14, 30, 0, 10, 24, 80, STUN | SNAP),
            f("maze_closes", "The Maze Closes", Prim.SEAL, 20000, 110, 0, 26, 0, 1, 160, PAR));

    public static final Spec HYDRA = b("hydra", "Lernaean Hydra, the Head That Grows Back", "Cut One, Two Grow", "Marsh Serpent", 0xFFB8E03A, 0.84, false, "L:POISON_BREATH",
            "Hydra: The Lernaean Hydra of the marsh: poison breath, many necks, heads that grow back when cut",
            List.of(p("regrowing_flesh", "Regrowing Flesh", Pas.REGEN, 0.025, 0), p("hydra_venom", "Hydra Venom", Pas.VENOM, 0, 7), p("many_heads_vigil", "Many Heads' Vigil", Pas.SENSE, 0, 24)),
            a("venom_bite", "Venom Bite", Prim.RUSH, 2000, 9, 45, 16, 0, 2, 0, BLEED | SNAP),
            a("acid_spray", "Acid Spray", Prim.BEAM, 2400, 10, 50, 30, 3, 12, 0, ARM | BLEED),
            f("regrowing_heads", "Regrowing Heads", Prim.BARRAGE, 22000, 110, 14, 10, 0, 22, 80, BLEED | HEAL | STUN));

    public static final Spec KRAKEN = b("kraken", "Kraken, Fleet-Dragger of the Abyss", "Terror of the Deep Sea", "Deep Terror", 0xFFFF6153, 0.88, false, "G:WATER_NEST",
            "Kraken: The Kraken of the northern sea: arms that seize ships, ink that blots the sea and a whirlpool when it dives",
            List.of(p("abyssal_hide", "Abyssal Hide", Pas.ARMOR, 4, 0), p("tide_caller", "Tide Caller", Pas.TIDE, 0, 0), p("ink_cloak", "Ink Cloak", Pas.STEALTH, 0, 0)),
            a("tentacle_lash", "Tentacle Lash", Prim.BEAM, 2200, 10, 60, 14, 2, 6, 30, STUN),
            a("crushing_grip", "Crushing Grip", Prim.SEAL, 3600, 20, 0, 24, 0, 1, 120, PAR),
            f("fleet_drowning_maelstrom", "Fleet-Drowning Maelstrom", Prim.BLAST, 28000, 130, 115, 0, 18, 24, 120, STUN | SNAP | ARM | FRZ));

    public static final Spec HAFGUFA = b("hafgufa", "Hafgufa, the Island That Swallows Fleets", "The Sea-Island That Lures and Swallows", "Island-Fish", 0xFF00BABD, 0.8, false, "G:BOSS_TIDE_SPOUT",
            "Hafgufa: The Hafgufa floats like an island and breathes a sweet scent that draws fish, then ships, into its maw",
            List.of(p("island_hide", "Island Hide", Pas.ARMOR, 8, 0), p("deep_tide", "Deep Tide", Pas.TIDE, 0, 0), p("bait_glow_pas", "Lure Light", Pas.SENSE, 0, 30)),
            a("sweet_breath_cloud", "Sweet Breath", Prim.SEAL, 3000, 20, 0, 26, 0, 1, 100, PAR),
            a("gaping_maw", "Gaping Maw", Prim.BLAST, 2600, 13, 55, 0, 9, 20, 40, SNAP | STUN),
            f("island_swallows_fleet", "The Island Swallows the Fleet", Prim.BLAST, 26000, 130, 110, 6, 16, 24, 100, PAR | ARM | STUN));

    public static final Spec TYPHON = b("typhon", "Typhon, Father of All Monsters", "Father of All Monsters", "Storm Giant", 0xFFD80023, 1.0, true, "G:LIGHTNING_FIEND",
            "Typhon: Typhon, Father of Monsters: a hundred dragon heads that roar with every voice, fire in the eyes and vipers at the thighs",
            List.of(p("hundred_throats", "Hundred Throats", Pas.MIGHT, 3.0, 0), p("storm_hide", "Storm Hide", Pas.ARMOR, 6, 0), p("viper_coils", "Viper Coils", Pas.VENOM, 0, 0)),
            a("storm_bolt", "Storm Bolt", Prim.BEAM, 3200, 11, 90, 52, 2, 12, 40, STUN),
            a("viper_hiss", "Viper Hiss", Prim.STOP, 3000, 14, 60, 30, 5, 24, 40, FRZ | SNAP),
            f("hundred_headed_tempest", "Hundred-Headed Tempest", Prim.BLAST, 30000, 150, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec SCYLLA_CHARYBDIS = b("scylla_charybdis", "Scylla and Charybdis, the Devouring Strait", "Scylla Above, Charybdis Below", "Strait Pair", 0xFF047A73, 0.8, false, "G:BOSS_TIDE_VORTEX",
            "Scylla and Charybdis: On opposite sides of the narrow strait: Scylla with six snapping dog heads on the cliff, Charybdis the whirlpool beneath",
            List.of(p("whirlpool_hide", "Whirlpool Hide", Pas.TIDE, 0, 0), p("six_heads_vigil", "Six Heads' Vigil", Pas.SENSE, 0, 24), p("narrow_strait", "Narrow Strait", Pas.ARMOR, 4, 0)),
            a("scyllas_snatch", "Scylla's Snatch", Prim.RUSH, 2400, 9, 60, 16, 0, 2, 20, SNAP | BLEED),
            a("six_headed_bite", "Six-Headed Bite", Prim.BARRAGE, 3000, 14, 9, 8, 0, 6, 30, BLEED | STUN),
            f("between_scylla_and_charybdis", "Between Scylla and Charybdis", Prim.STOP, 26000, 130, 100, 24, 6, 24, 100, STUN | SNAP | BYP | FRZ));

    public static final Spec SPHINX = b("sphinx", "The Sphinx, Keeper of the Last Riddle", "Who Walks on Four, Two, Then Three", "Riddle-Keeper", 0xFF3A6FE8, 0.8, false, "G:RUNE_WRITING",
            "Sphinx: A winged stone-lion with a calm human face that asks one question and keeps the silence for those who cannot answer",
            List.of(p("stone_poise", "Stone Poise", Pas.ARMOR, 5, 0), p("riddle_sight", "Riddle Sight", Pas.SENSE, 0, 24), p("calm_mind", "Calm Mind", Pas.ILLUSION, 0.15, 0)),
            a("riddle_of_ages", "Riddle of the Ages", Prim.PSYCHIC, 2800, 16, 0, 36, 0, 3, 80, PAR | BLIND),
            a("winged_pounce", "Winged Pounce", Prim.RUSH, 2400, 9, 55, 16, 0, 1, 20, SNAP),
            f("the_last_riddle", "The Last Riddle", Prim.SEAL, 24000, 130, 0, 26, 0, 1, 160, PAR));

    public static final Spec PEGASUS = b("pegasus", "Pegasus, Star-Mane of the Heavens", "The Horse That Became a Constellation", "Winged Steed's Gallop", 0xFFFFFFFF, 0.7, false, "L:WING_FX1",
            "Pegasus: A winged stallion of pure light whose hoofbeat can open a healing spring and whose gallop crosses the sky like a",
            List.of(p("wing_beat", "Wing Beat", Pas.FLIGHT, 0, 0), p("star_swiftness", "Star Swiftness", Pas.SWIFT, 0.12, 0), p("divine_mend", "Divine Mend", Pas.REGEN, 0.015, 0)),
            a("hoof_strike", "Hoof Strike", Prim.BLAST, 2000, 10, 60, 0, 9, 24, 0, SNAP | FRZ),
            a("thunderbearer", "Thunderbearer", Prim.BEAM, 3000, 12, 85, 52, 2.0, 12, 40, ARM),
            f("star_gallop", "Star Gallop", Prim.RUSH, 20000, 110, 125, 36, 0, 4, 40, BYP | IGN | SNAP | ARM));

    public static final Spec CU_SITH = b("cu_sith", "Harbinger of the Third Bark", "The Silent Green Death-Omen", "Moor Hound's Bay", 0xFF7CFF8A, 0.7, false, "L:MIST_MAGIC",
            "Cu Sith: A green-furred ghost hound of the moors whose paws leave no sound; its will carries a death-omen across mist and",
            List.of(p("silent_paws", "Silent Paws", Pas.STEALTH, 0, 0), p("moor_swiftness", "Moor Swiftness", Pas.SWIFT, 0.1, 0), p("terror_sense", "Terror Sense", Pas.SENSE, 0, 22)),
            a("first_bark", "First Bark", Prim.BLAST, 2400, 14, 35, 0, 9, 24, 60, STUN | SNAP),
            a("moor_fade", "Moor Fade", Prim.VEIL, 2400, 18, 0, 10, 0, 1, 0, 0),
            f("the_third_bark", "The Third Bark", Prim.BLAST, 17000, 100, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec NIDHOGGR = b("nidhoggr", "Nidhoggr, Gnawer at the Root of the World", "The Hunger Under Yggdrasil", "Root Gnawer", 0xFF579F0B, 0.94, false, "G:TREE_DRAIN",
            "Nidhoggr: Nidhoggr gnaws Yggdrasil's roots in Niflheim and feeds on the dead; its will carries rot and hunger, a Vritra-like decay",
            List.of(p("root_gnawer", "Root Gnawer", Pas.BLIGHT, 0, 6), p("corpse_hide", "Corpse Hide", Pas.ARMOR, 6, 0), p("deathless_hunger", "Deathless Hunger", Pas.FURY, 0, 0)),
            a("root_bite", "Root Bite", Prim.RUSH, 2400, 9, 65, 16, 0, 2, 0, BLEED | ARM),
            a("corpse_breath", "Corpse Breath", Prim.BEAM, 3000, 11, 70, 40, 2.5, 10, 0, BLEED),
            f("ragnaroks_gnawing", "Ragnarok's Gnawing", Prim.BEAM, 28000, 130, 135, 60, 4, 24, 80, BYP | ARM | BLEED | STUN));

    public static final Spec YAMATA_NO_OROCHI = b("yamata_no_orochi", "Yamata no Orochi, Eight Heads, Eight Valleys", "Eight Heads, Eight Tails, Eight Floods", "Eight-Headed Serpent", 0xFFE01070, 0.98, true, "L:WATER_DRAGON",
            "Yamata no Orochi: The eight-headed, eight-tailed serpent of Izumo, with eyes red as winter cherries, brought floods and was drowned in",
            List.of(p("eight_heads_vigil", "Eight Heads' Vigil", Pas.SENSE, 0, 28), p("eight_tails_scales", "Eight Tails' Scales", Pas.ARMOR, 6, 0), p("orochi_regeneration", "Orochi Regeneration", Pas.REGEN, 0.02, 0)),
            a("poison_sake_mist", "Poison Sake Mist", Prim.BLAST, 2800, 13, 50, 0, 10, 24, 40, STUN | BLEED),
            a("eight_head_bite", "Eight-Head Bite", Prim.BARRAGE, 3200, 14, 9, 8, 0, 8, 30, BLEED),
            f("eight_valleys_flood", "Eight Valleys Flood", Prim.STOP, 30000, 150, 100, 24, 6, 24, 100, STUN | SNAP | BYP | FRZ));

    public static final Spec KITSUNE = b("kitsune", "Nine-Tailed Dream Sovereign", "The Fox Who Owns the Dream", "Fox Glamour", 0xFFFF7FB0, 0.8, false, "G:DREAM_DOME",
            "Kitsune: A fox spirit whose tails grow with its mana; its will bends light and mind with foxfire and glamour",
            List.of(p("glamour_veil", "Glamour Veil", Pas.ILLUSION, 0, 0), p("foxfire_ward", "Foxfire Ward", Pas.SCORCH, 0, 5), p("light_paws", "Light Paws", Pas.SWIFT, 0.1, 0)),
            a("fox_step", "Fox Step", Prim.RUSH, 3400, 11, 50, 18, 0, 2, 20, ARM | SNAP),
            a("kitsunebi", "Kitsunebi", Prim.BARRAGE, 3200, 14, 8, 8, 0, 10, 40, BLEED | STUN),
            f("nine_tails_unveiled", "Nine Tails Unveiled", Prim.VEIL, 24000, 120, 0, 10, 0, 1, 0, 0));

    public static final Spec NUE = b("nue", "Nue, Chimera of the Black Cloud", "The Cry That Calls the Storm", "Omen of the Night-Cry", 0xFF7A55B8, 0.8, false, "G:STORM_CLOUD",
            "Nue: A night-chimera wrapped in black cloud whose cry is an omen: those who hear it fall ill and are struck by lightning",
            List.of(p("chimera_hide", "Chimera Hide", Pas.ARMOR, 4, 0), p("snake_venom", "Snake-Tail Venom", Pas.VENOM, 0, 6), p("night_senses", "Night Senses", Pas.SENSE, 0, 20)),
            a("ominous_cry", "Ominous Cry", Prim.BLAST, 2400, 13, 50, 0, 9, 24, 40, STUN | SNAP),
            a("tiger_pounce", "Tiger Pounce", Prim.RUSH, 2200, 9, 50, 22, 0, 1, 20, BLIND | SNAP),
            f("black_storm_omen", "Black Storm Omen", Prim.BARRAGE, 22000, 120, 12, 12, 0, 16, 60, BYP | BLEED | PAR));

    public static final Spec AMMIT = b("ammit", "Devourer of Judgement", "The Feather and the Maw", "Heart-Weigher", 0xFFD6CC8E, 0.85, false, "G:JUDGMENT_SIGIL",
            "Ammit: A mana beast of the dust-lands that weighs the heart of every foe it meets and devours the ones found wanting; its will",
            List.of(p("weighed_heart", "Weighed Heart", Pas.SENSE, 0, 22), p("hippo_hide", "Hippo Hide", Pas.ARMOR, 5, 0), p("sin_taste", "Sin's Taste", Pas.FURY, 0, 0)),
            a("feather_scale", "Feather Scale", Prim.SEAL, 3200, 20, 0, 26, 0, 1, 100, PAR | MARK),
            a("maw_of_the_nile", "Maw of the Nile", Prim.RUSH, 2400, 9, 65, 16, 0, 1, 0, BLEED | ARM),
            f("devour_the_unworthy", "Devour the Unworthy", Prim.BLAST, 26000, 130, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec APOPHIS = b("apophis", "Apep, Devourer of the Sun", "Serpent of the Unending Night", "Chaos Serpent", 0xFF6A0F8C, 0.97, true, "G:NIHILITY_ZONE",
            "Apophis: Apep, the endless serpent that swallows Ra's sun each dusk",
            List.of(p("chaos_blight", "Chaos Blight", Pas.BLIGHT, 0, 7), p("dusk_cloak", "Dusk Cloak", Pas.STEALTH, 0, 0), p("endless_hunger", "Endless Hunger", Pas.REGEN, 0.015, 0)),
            a("hypnotic_gaze", "Hypnotic Gaze", Prim.SEAL, 3200, 18, 0, 28, 0, 1, 100, PAR | BLIND),
            a("coil_of_chaos", "Coil of Chaos", Prim.BLAST, 3000, 13, 60, 0, 10, 24, 40, STUN | SNAP),
            f("night_without_dawn", "Night Without Dawn", Prim.BLAST, 30000, 150, 130, 0, 20, 24, 160, BLIND | PAR | BYP | ARM | STUN | FRZ));

    /** All 27, the nine of 3.45.0 first, then the eighteen. Order is the registration order: only ever append. */
    public static final List<Spec> SPECS = List.of(DRAGON, SHADOW_PANTHER, ELDERWOOD_GUARDIAN, LION, RIDGE_WOLF, CRESTED_BOAR, WIND_SKIMMER, FLAME_SERPENT, THUNDER_FANG_CHIMERA,
            FENRIR, JORMUNGANDR, CERBERUS, MINOTAUR, HYDRA, KRAKEN, HAFGUFA, TYPHON, SCYLLA_CHARYBDIS, SPHINX, PEGASUS, CU_SITH, NIDHOGGR, YAMATA_NO_OROCHI, KITSUNE, NUE, AMMIT, APOPHIS);

    /** By skill id (Ultimate or Unique), by key, or by will id; null when unknown. */
    public static Spec spec(String id) {
        if (id == null) return null;
        for (Spec s : SPECS) if (s.id().equals(id) || s.uniqueId().equals(id) || s.key().equals(id) || s.willId().equals(id)) return s;
        return null;
    }

    /** The cost of a mode, as the Dragon Ball skills charge it. */
    public static double cost(Mode m, boolean mastered) { return DbRules.cost(m, mastered, 1.0); }

    public static int cooldown(Mode m, boolean mastered) { return DbRules.cooldown(m, mastered); }
}
