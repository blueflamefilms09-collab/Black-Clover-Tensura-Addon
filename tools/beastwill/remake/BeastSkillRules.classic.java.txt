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

    // ---------------------------------------------------------------- the nine wills of 3.45.0
    public static final Spec DRAGON = b("dragon", "Dragon Sovereign", "Wrath of the Mana Dragon", "Will of the Dragon", 0xFFFF8A2A, 1.00, true, "G:FIRE_PILLAR",
            "Dragon: the strongest of the beast wills, legendary tier",
            List.of(p("draconic_hide", "Draconic Hide", Pas.ARMOR, 6, 0), p("dragon_sight", "Dragon Sight", Pas.SENSE, 0, 28), p("draconic_vigor", "Draconic Vigor", Pas.REGEN, 0.015, 0)),
            a("draconic_pulse", "Draconic Pulse", Prim.BLAST, 2000, 10, 60, 0, 9, 24, 0, SNAP | FRZ),
            a("dragon_breath", "Dragon Breath", Prim.BEAM, 3000, 12, 85, 52, 2.0, 12, 40, ARM),
            c("wing_gale", "Wing Gale", Prim.STOP, 2600, 14, 40, 28, 4, 24, 30, FRZ | SNAP),
            a("tail_crush", "Tail Crush", Prim.RUSH, 3400, 11, 50, 18, 0, 2, 20, ARM | SNAP),
            f("dragon_kings_annihilation", "Dragon King's Annihilation", Prim.BEAM, 28000, 130, 150, 70, 4, 24, 60, BYP | FRZ | ARM | STUN));

    public static final Spec SHADOW_PANTHER = b("shadow_panther", "Panther Sovereign", "Prowler of the Moonless Night", "Will of the Shadow Panther", 0xFF7A3CC8, 0.66, false, "G:SHADOW_POOL",
            "Shadow Panther: a swift stalker, rare tier",
            List.of(p("night_stride", "Night Stride", Pas.SWIFT, 0.25, 0), p("shadow_cloak", "Shadow Cloak", Pas.STEALTH, 0, 0), p("predators_grace", "Predator's Grace", Pas.ILLUSION, 0.20, 0)),
            a("shadow_dash", "Shadow Dash", Prim.RUSH, 1800, 8, 40, 16, 0, 1, 20, SNAP | BLIND),
            a("ambush_strike", "Ambush Strike", Prim.RUSH, 2600, 12, 70, 14, 0, 1, 0, IGN | BLEED),
            c("claw_flurry", "Claw Flurry", Prim.BARRAGE, 3200, 14, 8, 8, 0, 10, 40, BLEED | STUN),
            a("veil_of_night", "Veil of Night", Prim.VEIL, 2200, 20, 0, 10, 0, 1, 0, 0),
            f("eclipse_of_a_thousand_claws", "Eclipse of a Thousand Claws", Prim.BARRAGE, 20000, 110, 12, 12, 0, 16, 60, BYP | BLEED | PAR));

    public static final Spec ELDERWOOD_GUARDIAN = b("elderwood_guardian", "Elderwood Sovereign", "Heart of the Ancient Grove", "Will of the Elderwood Guardian", 0xFF4FA04A, 0.64, false, "G:TREE_ROOTS",
            "Elderwood Guardian: an ancient warden of the grove, rare tier",
            List.of(p("bark_hide", "Bark Hide", Pas.ARMOR, 7, 0), p("heartwood_mend", "Heartwood Mend", Pas.REGEN, 0.02, 0), p("creeping_roots", "Creeping Roots", Pas.FROST, 0, 8)),
            a("vine_lash", "Vine Lash", Prim.BEAM, 1800, 8, 45, 20, 1.0, 4, 30, STUN),
            a("bark_ward", "Bark Ward", Prim.GUARD, 2400, 16, 0, 0, 8, 10, 200, HEAL),
            c("restraining_roots", "Restraining Roots", Prim.SEAL, 3400, 22, 0, 26, 0, 1, 100, PAR),
            a("verdant_mend", "Verdant Mend", Prim.CAPSULE, 3000, 24, 0, 0, 14, 24, 0, HEAL),
            f("heart_of_the_elder_grove", "Heart of the Elder Grove", Prim.BLAST, 22000, 120, 90, 0, 14, 24, 120, PAR | SNAP | FRZ));

    public static final Spec LION = b("lion", "Lion Sovereign", "King of the Golden Savanna", "Will of the Mana Lion", 0xFFE6B030, 0.68, false, "L:FIRE_LION",
            "Lion: a pride king, rare tier",
            List.of(p("mane_of_might", "Mane of Might", Pas.MIGHT, 2.0, 0), p("pride_bond", "Pride Bond", Pas.BOND, 0, 16), p("lions_courage", "Lion's Courage", Pas.FURY, 0, 0)),
            a("roar_of_the_pride", "Roar of the Pride", Prim.BLAST, 2200, 12, 45, 0, 10, 24, 40, SNAP | STUN),
            a("mane_surge", "Mane Surge", Prim.GUARD, 2600, 18, 0, 0, 8, 8, 200, HEAL),
            a("royal_pounce", "Royal Pounce", Prim.RUSH, 2800, 10, 65, 18, 0, 1, 20, SNAP),
            c("rend_and_tear", "Rend and Tear", Prim.BARRAGE, 3200, 14, 9, 8, 0, 8, 30, BLEED),
            f("sovereigns_roar", "Sovereign's Roar", Prim.STOP, 24000, 120, 100, 24, 6, 24, 100, STUN | SNAP | BYP | FRZ));

    public static final Spec RIDGE_WOLF = b("ridge_wolf", "Wolf Sovereign", "Alpha of the Ridge", "Will of the Ridge Wolf", 0xFF8FA0B8, 0.50, false, "L:SLASH_WAVE",
            "Ridge Wolf: a pack hunter, common tier",
            List.of(p("pack_runner", "Pack Runner", Pas.SWIFT, 0.22, 0), p("keen_nose", "Keen Nose", Pas.SENSE, 0, 24), p("pack_bond", "Pack Bond", Pas.BOND, 0, 20)),
            a("fang_bite", "Fang Bite", Prim.RUSH, 1500, 8, 35, 14, 0, 2, 0, BLEED),
            a("ridge_howl", "Ridge Howl", Prim.BLAST, 2000, 14, 30, 0, 9, 24, 30, STUN | SNAP),
            c("pack_hunt", "Pack Hunt", Prim.BARRAGE, 2600, 14, 7, 8, 0, 12, 30, BLEED | STUN),
            a("pin_down", "Pin Down", Prim.SEAL, 2400, 16, 0, 20, 0, 1, 80, PAR),
            f("hunt_of_a_thousand_fangs", "Hunt of a Thousand Fangs", Prim.BARRAGE, 15000, 100, 11, 10, 0, 20, 60, BLEED | IGN | STUN));

    public static final Spec CRESTED_BOAR = b("crested_boar", "Boar Sovereign", "Unstoppable Tusk of the Highlands", "Will of the Crested Boar", 0xFFB06038, 0.52, false, "L:EARTH_RAMPAGE",
            "Crested Boar: a charging bruiser, common tier",
            List.of(p("thick_hide", "Thick Hide", Pas.ARMOR, 5, 0), p("unstoppable_charge", "Unstoppable Charge", Pas.MIGHT, 1.5, 0), p("boars_fury", "Boar's Fury", Pas.FURY, 0, 0)),
            a("gore", "Gore", Prim.RUSH, 1800, 9, 55, 16, 0, 1, 20, SNAP | ARM),
            a("tusk_sweep", "Tusk Sweep", Prim.BLAST, 2200, 12, 40, 0, 6, 16, 30, SNAP | STUN),
            a("bulwark", "Bulwark", Prim.GUARD, 2400, 18, 0, 0, 6, 8, 160, FRZ),
            c("trample", "Trample", Prim.STOP, 2800, 14, 45, 22, 3, 16, 40, SNAP | FRZ),
            f("crested_avalanche", "Crested Avalanche", Prim.RUSH, 16000, 100, 90, 24, 0, 3, 60, ARM | SNAP | STUN | IGN));

    public static final Spec WIND_SKIMMER = b("wind_skimmer", "Skimmer Sovereign", "Rider of the Endless Gale", "Will of the Wind Skimmer", 0xFF5CD6B8, 0.54, false, "L:WIND_EMPEROR",
            "Wind Skimmer: a fast flier, common tier",
            List.of(p("gale_stride", "Gale Stride", Pas.SWIFT, 0.30, 0), p("featherfall", "Featherfall", Pas.FLIGHT, 0, 0), p("wind_veil", "Wind Veil", Pas.ILLUSION, 0.22, 0)),
            a("gale_cut", "Gale Cut", Prim.BEAM, 1700, 7, 40, 36, 1.2, 6, 0, 0),
            a("skimming_dash", "Skimming Dash", Prim.RUSH, 1800, 9, 35, 22, 0, 1, 20, SNAP),
            a("cyclone_ring", "Cyclone Ring", Prim.BLAST, 2400, 13, 35, 0, 8, 16, 30, SNAP | FRZ),
            c("wind_wall", "Wind Wall", Prim.STOP, 2400, 14, 30, 20, 4, 16, 20, FRZ | SNAP),
            f("skimmers_tempest", "Skimmer's Tempest", Prim.BEAM, 18000, 100, 80, 60, 3, 24, 40, FRZ | SNAP));

    public static final Spec FLAME_SERPENT = b("flame_serpent", "Serpent Sovereign", "Coil of the Burning Sky", "Will of the Flame Serpent", 0xFFE8501C, 0.66, false, "L:FIRE_SPIRAL",
            "Flame Serpent: a molten coil, rare tier",
            List.of(p("ember_scales", "Ember Scales", Pas.SCORCH, 0, 5), p("serpents_heat", "Serpent's Heat", Pas.ARMOR, 3, 0), p("coil_regeneration", "Coil Regeneration", Pas.REGEN, 0.012, 0)),
            a("fire_fang", "Fire Fang", Prim.RUSH, 1800, 8, 50, 14, 0, 1, 0, BLEED | SNAP),
            a("ember_spit", "Ember Spit", Prim.BEAM, 2000, 9, 55, 40, 1.2, 6, 0, ARM),
            a("scorching_coil", "Scorching Coil", Prim.BLAST, 2600, 13, 50, 0, 8, 20, 40, SNAP | STUN),
            c("heat_haze", "Heat Haze", Prim.VEIL, 2200, 18, 0, 10, 0, 1, 0, 0),
            f("serpents_cremation", "Serpent's Cremation", Prim.BEAM, 22000, 120, 115, 56, 3, 24, 60, BYP | ARM));

    public static final Spec THUNDER_FANG_CHIMERA = b("thunder_fang_chimera", "Chimera Sovereign", "Three Heads of the Thunder", "Will of the Thunder Fang Chimera", 0xFFF2D83A, 0.72, false, "G:LIGHTNING_GOD",
            "Thunder Fang Chimera: a storm hybrid, epic tier",
            List.of(p("thunder_pace", "Thunder Pace", Pas.SWIFT, 0.22, 0), p("chimeras_hunger", "Chimera's Hunger", Pas.MIGHT, 2.0, 0), p("storm_hide", "Storm Hide", Pas.ARMOR, 4, 0)),
            a("thunder_fang", "Thunder Fang", Prim.RUSH, 2000, 8, 60, 18, 0, 1, 20, IGN | STUN),
            a("chain_bolt", "Chain Bolt", Prim.BEAM, 2200, 9, 60, 46, 1.4, 8, 30, STUN),
            a("thunder_roar", "Thunder Roar", Prim.BLAST, 2800, 14, 55, 0, 9, 24, 40, SNAP | PAR),
            c("triple_headed_assault", "Triple-Headed Assault", Prim.BARRAGE, 3400, 14, 10, 8, 0, 9, 30, STUN | BLEED),
            f("skyfall_of_the_chimera", "Skyfall of the Chimera", Prim.BLAST, 24000, 120, 110, 40, 14, 24, 100, PAR | FRZ | BYP | SNAP));

    // ---------------------------------------------------------------- the eighteen legendary and mythic wills
    /** God of War's Fenrir (the owner's word): Atreus's loyal wolf, whose soul-light was sealed into the giant wolf Garm. The moves are themed on that lore; the game's own moves are NOT verified. */
    public static final Spec FENRIR = b("fenrir", "Fenrir Sovereign", "Soul-Light of the Giant Wolf", "Fenrir's Loyal Soul", 0xFFB8E4FF, 0.95, true, "G:PAINT_BEAST",
            "Fenrir (God of War): the loyal wolf whose soul became a giant, legendary tier",
            List.of(p("soul_light", "Soul-Light", Pas.REGEN, 0.02, 0), p("frostbound_coat", "Frostbound Coat", Pas.FROST, 0, 9), p("loyal_guardian", "Loyal Guardian", Pas.BOND, 0, 18)),
            a("call_of_the_spirit_wolf", "Call of the Spirit Wolf", Prim.BLAST, 2400, 12, 55, 0, 10, 24, 40, SNAP | STUN),
            a("soul_frost_breath", "Soul-Frost Breath", Prim.BEAM, 3000, 12, 80, 48, 2.0, 10, 50, STUN),
            a("soul_seal", "Soul Seal", Prim.SEAL, 3600, 22, 0, 28, 0, 1, 100, PAR),
            c("garms_pounce", "Garm's Pounce", Prim.RUSH, 2800, 10, 60, 20, 0, 2, 20, SNAP | IGN),
            f("soul_of_the_wolf_unleashed", "Soul of the Wolf, Unleashed", Prim.BLAST, 26000, 130, 125, 14, 12, 24, 80, SNAP | BYP | STUN | FRZ | ARM));

    public static final Spec JORMUNGANDR = b("jormungandr", "Jormungandr Sovereign", "The Serpent Who Circles the World", "Will of the World Serpent", 0xFF3C8F7A, 0.96, true, "L:WATER_DRAGON",
            "Jormungandr: the world serpent, legendary tier",
            List.of(p("world_serpent_scales", "World-Serpent Scales", Pas.ARMOR, 7, 0), p("encircling_venom", "Encircling Venom", Pas.VENOM, 0, 6), p("ocean_blood", "Ocean Blood", Pas.TIDE, 0, 0)),
            a("venom_fang", "Venom Fang", Prim.RUSH, 2200, 9, 55, 16, 0, 1, 20, BLEED),
            a("tidal_coil", "Tidal Coil", Prim.BLAST, 2800, 13, 55, 0, 10, 24, 40, SNAP | STUN),
            a("constrict", "Constrict", Prim.SEAL, 3600, 20, 0, 24, 0, 1, 120, PAR),
            c("tail_lash", "Tail Lash", Prim.STOP, 2600, 12, 50, 24, 4, 20, 30, FRZ | SNAP),
            f("world_encircling_coil", "World-Encircling Coil", Prim.BLAST, 30000, 140, 120, 0, 18, 24, 140, PAR | STUN | ARM | FRZ | BYP));

    public static final Spec CERBERUS = b("cerberus", "Cerberus Sovereign", "Three Heads at the Gate of the Dead", "Will of the Hellhound", 0xFFC02828, 0.82, true, "F:SPACE_PORTAL",
            "Cerberus: the three-headed hound of Hades, mythic tier",
            List.of(p("gatekeepers_hide", "Gatekeeper's Hide", Pas.ARMOR, 5, 0), p("three_heads_vigil", "Three Heads' Vigil", Pas.SENSE, 0, 26), p("hellhound_fury", "Hellhound Fury", Pas.FURY, 0, 0)),
            a("hellfire_bite", "Hellfire Bite", Prim.RUSH, 2200, 9, 55, 16, 0, 2, 0, BLEED | SNAP),
            a("triple_bark", "Triple Bark", Prim.BLAST, 2400, 12, 45, 0, 9, 24, 40, STUN | SNAP),
            a("hellfire_breath", "Hellfire Breath", Prim.BEAM, 3000, 12, 70, 36, 2.5, 10, 0, ARM),
            c("guard_of_the_gate", "Guard of the Gate", Prim.GUARD, 2600, 18, 0, 0, 7, 8, 200, FRZ | HEAL),
            f("triple_gate_of_hades", "Triple Gate of Hades", Prim.BARRAGE, 24000, 120, 15, 12, 0, 18, 80, BYP | PAR | BLEED));

    public static final Spec MINOTAUR = b("minotaur", "Minotaur Sovereign", "Master of the Labyrinth", "Will of the Labyrinth Bull", 0xFFA6732E, 0.72, false, "G:EARTH_CLAWS",
            "Minotaur: the bull of the labyrinth, epic tier",
            List.of(p("labyrinth_hide", "Labyrinth Hide", Pas.ARMOR, 6, 0), p("bulls_rage", "Bull's Rage", Pas.FURY, 0, 0), p("maze_sense", "Maze Sense", Pas.SENSE, 0, 24)),
            a("horn_charge", "Horn Charge", Prim.RUSH, 2400, 9, 70, 20, 0, 1, 20, SNAP | ARM),
            a("labrys_cleave", "Labrys Cleave", Prim.BEAM, 2400, 10, 65, 8, 3, 6, 0, ARM),
            c("bellowing_challenge", "Bellowing Challenge", Prim.BLAST, 2000, 14, 30, 0, 10, 24, 80, STUN | SNAP),
            a("maze_wall", "Maze Wall", Prim.GUARD, 2400, 16, 0, 0, 6, 8, 180, FRZ),
            f("labyrinths_last_charge", "Labyrinth's Last Charge", Prim.RUSH, 20000, 110, 120, 26, 0, 3, 60, ARM | STUN | IGN | SNAP));

    public static final Spec HYDRA = b("hydra", "Hydra Sovereign", "The Head That Grows Back", "Will of the Many-Headed Serpent", 0xFF58C040, 0.84, true, "L:POISON_BREATH",
            "Hydra: the many-headed marsh serpent, mythic tier",
            List.of(p("regrowing_flesh", "Regrowing Flesh", Pas.REGEN, 0.025, 0), p("hydra_venom", "Hydra Venom", Pas.VENOM, 0, 7), p("many_heads_vigil", "Many Heads' Vigil", Pas.SENSE, 0, 24)),
            a("venom_bite", "Venom Bite", Prim.RUSH, 2000, 9, 45, 16, 0, 2, 0, BLEED | SNAP),
            a("acid_spray", "Acid Spray", Prim.BEAM, 2400, 10, 50, 30, 3, 12, 0, ARM | BLEED),
            c("multi_head_strike", "Multi-Head Strike", Prim.BARRAGE, 3200, 14, 8, 8, 0, 9, 30, BLEED),
            a("heads_together", "Heads Together", Prim.GUARD, 2600, 18, 0, 0, 6, 8, 180, HEAL),
            f("regrowing_heads", "Regrowing Heads", Prim.BARRAGE, 22000, 110, 14, 10, 0, 22, 80, BLEED | HEAL | STUN));

    public static final Spec KRAKEN = b("kraken", "Kraken Sovereign", "Terror of the Deep Sea", "Will of the Deep Terror", 0xFF2A5FA8, 0.88, true, "G:WATER_NEST",
            "Kraken: the tentacled terror of the deep, mythic tier",
            List.of(p("abyssal_hide", "Abyssal Hide", Pas.ARMOR, 4, 0), p("tide_caller", "Tide Caller", Pas.TIDE, 0, 0), p("ink_cloak", "Ink Cloak", Pas.STEALTH, 0, 0)),
            a("tentacle_lash", "Tentacle Lash", Prim.BEAM, 2200, 10, 60, 14, 2, 6, 30, STUN),
            a("ink_burst", "Ink Burst", Prim.VEIL, 2200, 18, 0, 10, 0, 1, 0, 0),
            a("crushing_grip", "Crushing Grip", Prim.SEAL, 3600, 20, 0, 24, 0, 1, 120, PAR),
            c("maelstrom_pull", "Maelstrom Pull", Prim.BLAST, 2800, 14, 50, 0, 10, 24, 40, SNAP),
            f("fleet_drowning_maelstrom", "Fleet-Drowning Maelstrom", Prim.BLAST, 28000, 130, 115, 0, 18, 24, 120, STUN | SNAP | ARM | FRZ));

    public static final Spec HAFGUFA = b("hafgufa", "Hafgufa Sovereign", "The Island That Swallows Fleets", "Will of the Island-Fish", 0xFF4A8A96, 0.80, true, "F:FOOD_MAW",
            "Hafgufa: the island-sized sea beast, mythic tier",
            List.of(p("island_hide", "Island Hide", Pas.ARMOR, 8, 0), p("deep_tide", "Deep Tide", Pas.TIDE, 0, 0), p("bait_glow", "Bait Glow", Pas.SENSE, 0, 30)),
            a("gaping_maw", "Gaping Maw", Prim.BLAST, 2600, 13, 55, 0, 9, 20, 40, SNAP | STUN),
            a("sweet_breath", "Sweet Breath", Prim.SEAL, 3000, 20, 0, 26, 0, 1, 100, PAR),
            a("tidal_spout", "Tidal Spout", Prim.BEAM, 2400, 10, 60, 40, 2, 10, 0, SNAP),
            c("island_back", "Island Back", Prim.GUARD, 2600, 18, 0, 0, 8, 10, 200, FRZ | HEAL),
            f("island_swallows_the_fleet", "The Island Swallows the Fleet", Prim.BLAST, 26000, 130, 110, 6, 16, 24, 100, PAR | ARM | STUN));

    public static final Spec TYPHON = b("typhon", "Typhon Sovereign", "Father of All Monsters", "Will of the Storm Giant", 0xFFA050E0, 1.00, true, "G:LIGHTNING_GOD",
            "Typhon: the father of monsters, legendary tier",
            List.of(p("hundred_throats", "Hundred Throats", Pas.MIGHT, 3.0, 0), p("storm_hide", "Storm Hide", Pas.ARMOR, 6, 0), p("serpent_legs", "Serpent Legs", Pas.FLIGHT, 0, 0), p("monstrous_vitality", "Monstrous Vitality", Pas.REGEN, 0.015, 0)),
            a("storm_bolt", "Storm Bolt", Prim.BEAM, 3200, 11, 90, 52, 2, 12, 40, STUN),
            a("serpent_coil_lash", "Serpent Coil Lash", Prim.BLAST, 3000, 12, 60, 0, 10, 24, 50, SNAP | STUN),
            c("venom_torrent", "Venom Torrent", Prim.STOP, 3000, 14, 60, 30, 5, 24, 40, FRZ | SNAP),
            a("fire_breath", "Fire Breath", Prim.BEAM, 3400, 13, 85, 44, 3, 12, 0, ARM | BLEED),
            f("hundred_headed_tempest", "Hundred-Headed Tempest", Prim.STOP, 30000, 150, 125, 40, 7, 24, 120, FRZ | SNAP | PAR | BYP));

    public static final Spec SCYLLA_CHARYBDIS = b("scylla_charybdis", "Strait Sovereign", "Scylla Above, Charybdis Below", "Will of the Devouring Strait", 0xFF5E7FB0, 0.80, true, "G:WATER_NEST",
            "Scylla and Charybdis: the six-headed rock and the whirlpool, mythic tier",
            List.of(p("whirlpool_hide", "Whirlpool Hide", Pas.TIDE, 0, 0), p("six_heads_vigil", "Six Heads' Vigil", Pas.SENSE, 0, 24), p("narrow_strait", "Narrow Strait", Pas.ARMOR, 4, 0)),
            a("scyllas_snatch", "Scylla's Snatch", Prim.RUSH, 2400, 9, 60, 16, 0, 2, 20, SNAP | BLEED),
            a("charybdis_whirl", "Charybdis Whirl", Prim.BLAST, 2800, 13, 50, 0, 9, 24, 40, SNAP | STUN),
            c("six_headed_bite", "Six-Headed Bite", Prim.BARRAGE, 3000, 14, 9, 8, 0, 6, 30, BLEED | STUN),
            a("strait_ward", "Strait Ward", Prim.GUARD, 2600, 18, 0, 0, 7, 8, 180, FRZ),
            f("between_scylla_and_charybdis", "Between Scylla and Charybdis", Prim.BLAST, 26000, 130, 115, 0, 16, 24, 120, ARM | PAR | SNAP));

    public static final Spec SPHINX = b("sphinx", "Sphinx Sovereign", "Keeper of the Last Riddle", "Will of the Riddler", 0xFFD8B050, 0.78, true, "G:GAME_BOARD",
            "Sphinx: the riddling guardian, mythic tier",
            List.of(p("riddlers_gaze", "Riddler's Gaze", Pas.SENSE, 0, 30), p("stone_poise", "Stone Poise", Pas.ARMOR, 5, 0), p("mirage_veil", "Mirage Veil", Pas.ILLUSION, 0.25, 0)),
            a("riddle_gaze", "Riddle Gaze", Prim.SEAL, 3000, 18, 0, 28, 0, 1, 100, PAR | BLIND),
            a("stone_paw", "Stone Paw", Prim.RUSH, 2400, 9, 55, 16, 0, 1, 20, SNAP),
            c("sandstorm_wing", "Sandstorm Wing", Prim.STOP, 2400, 12, 40, 22, 5, 20, 30, FRZ | BLIND),
            a("enigma_mark", "Enigma Mark", Prim.SCAN, 2000, 14, 0, 0, 24, 24, 0, MARK),
            f("riddle_of_judgement", "Riddle of Judgement", Prim.PSYCHIC, 24000, 130, 80, 40, 0, 3, 140, PAR | BLIND | ARM));

    public static final Spec PEGASUS = b("pegasus", "Pegasus Sovereign", "Star-Mane of the Heavens", "Will of the Winged Steed", 0xFFF4F0D8, 0.70, false, "L:LIGHT_BLADE",
            "Pegasus: the winged steed of the heavens, epic tier",
            List.of(p("winged_stride", "Winged Stride", Pas.SWIFT, 0.25, 0), p("sky_dancer", "Sky Dancer", Pas.FLIGHT, 0, 0), p("holy_mane", "Holy Mane", Pas.REGEN, 0.015, 0)),
            a("sky_charge", "Sky Charge", Prim.RUSH, 2200, 8, 55, 24, 0, 1, 20, SNAP),
            a("wing_gale_p", "Wing Gale", Prim.STOP, 2400, 12, 35, 24, 4, 16, 20, FRZ | SNAP),
            a("radiant_mane", "Radiant Mane", Prim.CAPSULE, 3000, 22, 0, 0, 12, 24, 0, HEAL),
            c("starlight_hooves", "Starlight Hooves", Prim.BARRAGE, 3000, 12, 9, 8, 0, 8, 20, STUN),
            f("meteor_charge", "Meteor Charge", Prim.RUSH, 20000, 110, 125, 36, 0, 4, 40, BYP | IGN | SNAP | ARM));

    public static final Spec CU_SITH = b("cu_sith", "Cu Sith Sovereign", "Green Hound of the Fairy Hills", "Will of the Faerie Hound", 0xFF2F7A52, 0.62, false, "L:WIND_GALE",
            "Cu Sith: the silent green faerie hound, rare tier",
            List.of(p("silent_paws", "Silent Paws", Pas.STEALTH, 0, 0), p("hounds_pace", "Hound's Pace", Pas.SWIFT, 0.25, 0), p("faerie_senses", "Faerie Senses", Pas.SENSE, 0, 26)),
            a("spectral_snap", "Spectral Snap", Prim.RUSH, 1800, 8, 40, 16, 0, 1, 0, BLEED | SNAP),
            a("three_barks", "Three Barks", Prim.BLAST, 2400, 14, 35, 0, 9, 24, 60, STUN | SNAP),
            a("hunters_mark", "Hunter's Mark", Prim.SCAN, 2000, 14, 0, 0, 24, 24, 0, MARK),
            c("faerie_mist", "Faerie Mist", Prim.VEIL, 2400, 18, 0, 10, 0, 1, 0, 0),
            f("hunt_of_the_green_hound", "Hunt of the Green Hound", Prim.BARRAGE, 17000, 100, 10, 14, 0, 18, 60, BLEED | IGN | PAR));

    public static final Spec NIDHOGGR = b("nidhoggr", "Nidhoggr Sovereign", "Gnawer at the Root of the World", "Will of the Root-Gnawer", 0xFF5A2E6E, 0.94, true, "G:KOTO_SLUDGE",
            "Nidhoggr: the dragon gnawing the world tree, legendary tier",
            List.of(p("root_gnawer", "Root Gnawer", Pas.BLIGHT, 0, 6), p("corpse_hide", "Corpse Hide", Pas.ARMOR, 6, 0), p("deathless_hunger", "Deathless Hunger", Pas.FURY, 0, 0)),
            a("root_bite", "Root Bite", Prim.RUSH, 2400, 9, 65, 16, 0, 2, 0, BLEED | ARM),
            a("corpse_breath", "Corpse Breath", Prim.BEAM, 3000, 11, 70, 40, 2.5, 10, 0, BLEED),
            a("hall_of_the_dead", "Hall of the Dead", Prim.BLAST, 2800, 13, 55, 0, 9, 20, 40, STUN),
            c("dread_hide", "Dread Hide", Prim.GUARD, 2800, 18, 0, 0, 7, 8, 200, FRZ),
            f("ragnaroks_gnawing", "Ragnarok's Gnawing", Prim.BEAM, 28000, 130, 135, 60, 4, 24, 80, BYP | ARM | BLEED | STUN));

    public static final Spec YAMATA_NO_OROCHI = b("yamata_no_orochi", "Orochi Sovereign", "Eight Heads, Eight Valleys", "Will of the Eight-Headed Serpent", 0xFFC02060, 0.98, true, "L:WATER_DRAGON",
            "Yamata no Orochi: the eight-headed serpent, legendary tier",
            List.of(p("eight_heads_vigil", "Eight Heads' Vigil", Pas.SENSE, 0, 28), p("eight_tails_scales", "Eight-Tail Scales", Pas.ARMOR, 6, 0), p("orochi_regeneration", "Orochi Regeneration", Pas.REGEN, 0.02, 0)),
            a("head_strike", "Head Strike", Prim.RUSH, 2400, 8, 65, 16, 0, 2, 20, SNAP | BLEED),
            a("sake_mist", "Poison Sake Mist", Prim.BLAST, 2800, 13, 50, 0, 10, 24, 40, STUN | BLEED),
            c("eight_head_bite", "Eight-Head Bite", Prim.BARRAGE, 3200, 14, 9, 8, 0, 8, 30, BLEED),
            a("valley_flood", "Valley Flood", Prim.STOP, 3000, 14, 55, 28, 5, 24, 40, FRZ | SNAP),
            f("eight_head_calamity", "Eight-Head Calamity", Prim.BARRAGE, 30000, 150, 16, 14, 0, 24, 100, BYP | BLEED | STUN | PAR | ARM));

    public static final Spec KITSUNE = b("kitsune", "Kitsune Sovereign", "Nine Tails of the Foxfire", "Will of the Fox Spirit", 0xFFF08A3A, 0.76, false, "G:FIRE_WILD",
            "Kitsune: the nine-tailed fox spirit, epic tier",
            List.of(p("illusionist", "Illusionist", Pas.ILLUSION, 0.30, 0), p("foxfire_mantle", "Foxfire Mantle", Pas.SCORCH, 0, 4), p("nine_tails_grace", "Nine Tails' Grace", Pas.SWIFT, 0.20, 0)),
            a("foxfire", "Foxfire", Prim.BEAM, 2200, 9, 55, 40, 1.4, 6, 0, BLEED),
            a("fox_glamour", "Fox Glamour", Prim.VEIL, 2400, 18, 0, 10, 0, 1, 0, 0),
            a("illusory_bind", "Illusory Bind", Prim.SEAL, 3200, 20, 0, 26, 0, 1, 100, PAR | BLIND),
            c("tail_whip", "Tail Whip", Prim.STOP, 2400, 12, 40, 20, 4, 16, 20, FRZ | SNAP),
            f("nine_tailed_foxfire_rite", "Nine-Tailed Foxfire Rite", Prim.BLAST, 24000, 120, 105, 0, 16, 24, 100, STUN | BLIND | SNAP | BYP));

    public static final Spec NUE = b("nue", "Nue Sovereign", "Omen of the Black Cloud", "Will of the Night-Cry Chimera", 0xFF4A4A6E, 0.74, false, "G:LIGHTNING_GOD",
            "Nue: the ominous chimera of the night, epic tier",
            List.of(p("black_cloud_veil", "Black Cloud Veil", Pas.STEALTH, 0, 0), p("tiger_strength", "Tiger Strength", Pas.MIGHT, 2.0, 0), p("serpent_tail", "Serpent Tail", Pas.VENOM, 0, 5)),
            a("thunder_cry", "Thunder Cry", Prim.BLAST, 2400, 13, 50, 0, 9, 24, 40, STUN | SNAP),
            a("shadow_flit", "Shadow Flit", Prim.RUSH, 2200, 9, 50, 22, 0, 1, 20, BLIND | SNAP),
            a("serpent_tail_whip", "Serpent Tail Whip", Prim.BEAM, 2000, 9, 50, 12, 2, 6, 0, BLEED),
            c("eerie_night_cry", "Eerie Night Cry", Prim.SEAL, 3000, 20, 0, 24, 0, 1, 100, PAR | BLIND),
            f("black_cloud_calamity", "Black Cloud Calamity", Prim.BEAM, 22000, 120, 110, 50, 4, 24, 80, PAR | BYP | FRZ));

    public static final Spec AMMIT = b("ammit", "Ammit Sovereign", "Devourer of the Unworthy Heart", "Will of the Heart-Eater", 0xFFD0A830, 0.76, true, "F:FOOD_MAW",
            "Ammit: the devourer of the dead, mythic tier",
            List.of(p("heart_weigher", "Heart Weigher", Pas.SENSE, 0, 28), p("devourer_hide", "Devourer's Hide", Pas.ARMOR, 5, 0), p("crocodile_jaws", "Crocodile Jaws", Pas.MIGHT, 2.0, 0)),
            a("devouring_bite", "Devouring Bite", Prim.RUSH, 2400, 9, 65, 16, 0, 1, 0, BLEED | ARM),
            a("judgement_gaze", "Judgement Gaze", Prim.SEAL, 3200, 20, 0, 26, 0, 1, 100, PAR | MARK),
            a("hippo_stomp", "Hippo Stomp", Prim.BLAST, 2600, 13, 50, 0, 8, 20, 40, SNAP | STUN),
            c("scales_of_maat", "Scales of Ma'at", Prim.SCAN, 2200, 16, 0, 0, 22, 24, 0, MARK),
            f("devourer_of_the_unworthy_heart", "Devourer of the Unworthy Heart", Prim.DISC, 26000, 130, 145, 40, 1.4, 1, 0, BYP | IGN | ARM | HOME));

    public static final Spec APOPHIS = b("apophis", "Apophis Sovereign", "Serpent of the Unending Night", "Will of the Chaos Serpent", 0xFF4B2A7A, 0.97, true, "G:NIHILITY_ZONE",
            "Apophis: the serpent of chaos who swallows the sun, legendary tier",
            List.of(p("chaos_blight", "Chaos Blight", Pas.BLIGHT, 0, 7), p("darkness_cloak", "Darkness Cloak", Pas.STEALTH, 0, 0), p("endless_hunger", "Endless Hunger", Pas.REGEN, 0.015, 0)),
            a("hypnotic_gaze", "Hypnotic Gaze", Prim.SEAL, 3200, 18, 0, 28, 0, 1, 100, PAR | BLIND),
            a("coil_of_chaos", "Coil of Chaos", Prim.BLAST, 3000, 13, 60, 0, 10, 24, 40, STUN | SNAP),
            a("venom_spit", "Venom Spit", Prim.BEAM, 3000, 11, 75, 40, 2, 10, 0, BLEED | ARM),
            c("veil_of_darkness", "Veil of Darkness", Prim.VEIL, 2800, 20, 0, 10, 0, 1, 0, 0),
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
