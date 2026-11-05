package com.newuniverse.nusmp.skill.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.56.0: the Dragon Ball skills as pure data (no Minecraft types), so the skill, the passives, the tests and the CI self-test all read one table. Fifteen characters; each is a Unique skill that evolves
 * (SkillEvolution, once mastered) into an Ultimate. A Mode is a passive (under the skill's own ON/OFF toggle) or an active (a press, a crouch + press, or the mastery-only finisher). What a mode DOES is its
 * {@link Move}: a primitive ({@link Prim}) plus numbers, so a new character is a new table row, not new code. Power is scaled by {@link Spec#scale()}: the Super Hero (2022) standing of the form the skill is
 * based on (Gohan Beast = 1.0, the top; Bulma the lowest).
 */
public final class DbRules {
    private DbRules() {}

    /** What a mode does. Passives first, then actives. */
    public enum Prim {
        // passives (the numbers of a passive: dmg = melee dodge chance or the bonus, range = projectile dodge chance or the reach, radius, n = a percentage, lock = interval ticks)
        SPEED, DODGE, BIND, CLEANSE_REGEN, SENSE, AURA, EXTEND, RAGE, EGO, ORANGE, DEATHSAVE, FLOAT, REACTOR, PACK, GENIUS, STAMINA,
        // actives
        BEAM, BLAST, STOP, RUSH, GUARD, BARRAGE, DISC, SEAL, VEIL, SCAN, CAPSULE, PSYCHIC, DETONATE
    }

    public static final int BYPASS = 1, STRIP = 2, PARALYZE = 4, STUN = 8, FREEZE_PROJ = 16, DEBT_BLAST = 32, REJECT = 64, SNAP = 128, BLIND = 256, SILENCE = 512, BLEED = 1024,
            HOMING = 2048, IGNORE_FRAMES = 4096, ARMOR_BREAK = 8192, STUTTER = 16384, SELF_COST = 32768, HEAL = 65536, BURY = 131072;

    /** dmg = base damage (before the spec's scale), range/radius in blocks, n = hits or pierce, debt = Temporal Debt stacks, lock = ticks of control (before the boss/player limits), flags, rgb = the cast colour. */
    public record Move(Prim prim, double dmg, double range, double radius, int n, int debt, int lock, int flags, int rgb) {
        public boolean has(int f) { return (flags & f) != 0; }
        public boolean passive() { return prim.ordinal() <= Prim.STAMINA.ordinal(); }
    }

    /** One mode: the id (lang key part, a save key: never renamed), the name, passive or active, crouch + press, mastery-only, cost / mastered cost (Magicule), cooldown / mastered cooldown (seconds), the move. */
    public record Mode(String id, String name, boolean crouch, boolean mastery, int cost, int masteredCost, int cooldown, int masteredCooldown, Move move) {
        public boolean passive() { return move.passive(); }
    }

    /** One character. {@code uniqueName} is the Unique form's name; {@code power} is the one-line Super Hero scaling note shown in the description. */
    public record Spec(String id, String title, String epithet, String uniqueName, double scale, String power, List<Mode> modes) {
        public String uniqueId() { return id + "_unique"; }
        public List<Mode> passives() { List<Mode> l = new ArrayList<>(); for (Mode m : modes) if (m.passive()) l.add(m); return l; }
        public List<Mode> actives() { List<Mode> l = new ArrayList<>(); for (Mode m : modes) if (!m.passive()) l.add(m); return l; }
        /** The actives the Unique form has: the first two that are not the mastery finisher. */
        public List<Mode> uniqueActives() { List<Mode> l = new ArrayList<>(); for (Mode m : actives()) if (!m.mastery() && l.size() < 2) l.add(m); return l; }
        public Mode firstPassive() { List<Mode> p = passives(); return p.isEmpty() ? null : p.get(0); }
        public Mode finisher() { for (Mode m : modes) if (m.mastery()) return m; return null; }
    }

    // ---------------------------------------------------------------- the numbers of every skill
    /** The Unique form is this fraction of an Ultimate's strength. */
    public static final double UNIQUE_FACTOR = 0.5;
    /** Most creatures one press can hit, most projectiles removed, the seconds between two passive scans (the work is bounded by these, rule 11). */
    public static final int MAX_TARGETS = 24, MAX_PROJECTILES = 24, PASSIVE_PERIOD_TICKS = 20, DEATHSAVE_SECONDS = 180, MAX_LOCK_TICKS = 200;
    public static final double REACTOR_SHARE = 0.06, RAGE_PER_DAMAGE = 0.5, RAGE_CAP = 60.0, EGO_CAP = 80.0, ORANGE_BELOW = 0.35, ORANGE_DAMAGE = 1.6;

    /** The strength factor of a form: 1.0 for an Ultimate, the unique fraction for a Unique, times the character's Super Hero scale. */
    public static double power(Spec s, boolean ultimate) { return s.scale() * (ultimate ? 1.0 : UNIQUE_FACTOR); }

    /** The raw damage of a move for a form. */
    public static double damage(Spec s, Move m, boolean ultimate) { return m.dmg() * power(s, ultimate); }

    /** The Magicule a mode costs (the mastered price once mastered), times the server's scale; 0 for a skill with the reactor under it is decided by the caller. */
    public static double cost(Mode m, boolean mastered, double scale) { return Math.max(0.0, (mastered ? m.masteredCost() : m.cost()) * Math.max(0.0, scale)); }

    public static int cooldown(Mode m, boolean mastered) { return Math.max(0, mastered ? m.masteredCooldown() : m.cooldown()); }

    /** Ticks of control after the dispatcher's cap. */
    public static int lock(Move m) { return Math.max(0, Math.min(MAX_LOCK_TICKS, m.lock())); }

    /** The stored pressure/rage bonus damage from damage {@code taken}: half of it, at most {@code cap}. */
    public static double stored(double taken, double cap) { return Math.max(0.0, Math.min(cap, taken * RAGE_PER_DAMAGE)); }

    /** True when the Orange awakening is on: at or below the share of the maximum health. */
    public static boolean lowHealth(double health, double max) { return max > 0.0 && health / max <= ORANGE_BELOW; }

    // ---------------------------------------------------------------- tiny builders
    private static Move m(Prim p, double dmg, double range, double radius, int n, int debt, int lock, int flags, int rgb) { return new Move(p, dmg, range, radius, n, debt, lock, flags, rgb); }

    private static Mode pas(String id, String name, Prim p, double a, double b, double radius, int n) {
        return new Mode(id, name, false, false, 0, 0, 0, 0, m(p, a, b, radius, n, 0, 0, 0, 0xFFFFFFFF));
    }

    private static Mode act(String id, String name, boolean crouch, int cost, int cd, Move mv) { return new Mode(id, name, crouch, false, cost, cost, cd, cd, mv); }

    private static Mode fin(String id, String name, int cost, int mCost, int cd, int mCd, Move mv) { return new Mode(id, name, false, true, cost, mCost, cd, mCd, mv); }

    // ---------------------------------------------------------------- the fifteen
    public static final Spec GOKU = new Spec("db_goku", "Wukong", "Lord of Limitless Combat", "Saiyan Prodigy", 0.97, "Goku: Ultra Instinct / divine ki, Super Hero tier one", List.of(
            pas("autonomous_ultra_instinct", "Autonomous Ultra Instinct", Prim.DODGE, 0.35, 0.5, 0, 0),
            pas("god_bind", "God Bind", Prim.BIND, 0, 0, 0, 0),
            act("energy_landmine", "Energy Landmine", false, 1800, 12, m(Prim.BLAST, 55, 0, 9, 24, 0, 0, SNAP, 0xFFFFB040)),
            act("evil_containment_wave", "Evil Containment Wave", true, 4200, 40, m(Prim.SEAL, 0, 36, 0, 1, 10, 100, SILENCE | PARALYZE | BURY, 0xFF60FF90)),
            fin("god_kamehameha", "God Kamehameha", 24000, 16000, 120, 70, m(Prim.BEAM, 120, 64, 3.2, 24, 12, 40, BYPASS | DEBT_BLAST | FREEZE_PROJ, 0xFF40D0FF))));

    public static final Spec VEGETA = new Spec("db_vegeta", "Vegeta", "Lord of Pride", "Saiyan Prince", 0.95, "Vegeta: Super Saiyan Blue Evolved, Super Hero tier one", List.of(
            pas("royal_authority", "Royal Authority", Prim.AURA, 0, 0, 10, 2),
            pas("egos_fuel", "Ego's Fuel", Prim.EGO, 0, 0, 0, 0),
            act("galick_gun", "Galick Gun", false, 2600, 10, m(Prim.BEAM, 70, 56, 1.8, 8, 8, 80, REJECT | STUN, 0xFFB060FF)),
            act("big_bang_attack", "Big Bang Attack", true, 3600, 16, m(Prim.BLAST, 85, 44, 8, 24, 0, 0, SNAP | BYPASS, 0xFFFFE060)),
            fin("final_flash", "Final Flash", 26000, 18000, 130, 80, m(Prim.BEAM, 130, 64, 4.2, 24, 15, 60, REJECT | BYPASS | FREEZE_PROJ, 0xFFFFD020))));

    public static final Spec GOHAN = new Spec("db_gohan", "Gohan", "Lord of the Beast", "Half-Saiyan Scholar", 1.00, "Gohan Beast: the strongest mortal of the era, Super Hero tier one", List.of(
            pas("beasts_awakening", "Beast's Awakening", Prim.SPEED, 0.45, 0.25, 0, 0),
            pas("latent_rage", "Latent Rage", Prim.RAGE, 0, 0, 16, 0),
            act("masenko", "Masenko", false, 1800, 8, m(Prim.STOP, 60, 30, 3.0, 12, 0, 40, FREEZE_PROJ | SNAP, 0xFFFFC040)),
            act("super_kamehameha", "Super Kamehameha", true, 4400, 16, m(Prim.BEAM, 95, 60, 3.0, 24, 10, 80, FREEZE_PROJ | STUN, 0xFF50C8FF)),
            fin("beast_special_beam_cannon", "Beast Special Beam Cannon", 30000, 20000, 140, 80, m(Prim.BEAM, 150, 72, 1.3, 24, 20, 100, BYPASS | IGNORE_FRAMES | PARALYZE | ARMOR_BREAK, 0xFFFF4040))));

    public static final Spec PICCOLO = new Spec("db_piccolo", "Piccolo", "Lord of Namek", "Namekian Tactician", 0.92, "Ultimate / Orange Piccolo, Super Hero tier one", List.of(
            pas("pride_of_namek", "Pride of Namek", Prim.CLEANSE_REGEN, 0.02, 0, 0, 0),
            pas("orange_awakening", "Orange Awakening", Prim.ORANGE, 0, 0, 0, 0),
            act("light_grenade", "Light Grenade", false, 2400, 10, m(Prim.BLAST, 90, 34, 11, 24, 0, 0, SNAP, 0xFFFFFF80)),
            act("hellzone_grenade", "Hellzone Grenade", true, 5200, 22, m(Prim.BARRAGE, 14, 40, 0, 10, 4, 50, STUN, 0xFFC0FF60)),
            fin("special_beam_cannon", "Special Beam Cannon", 28000, 19000, 120, 70, m(Prim.BEAM, 135, 64, 0.9, 12, 25, 60, BYPASS | ARMOR_BREAK, 0xFFFFA030))));

    public static final Spec KRILLIN = new Spec("db_krillin", "Krillin", "Lord of Technique", "Earth's Strongest", 0.60, "Krillin, veteran Z-Fighter, Super Hero tier three", List.of(
            pas("veterans_instinct", "Veteran's Instinct", Prim.DODGE, 0.30, 0.30, 0, 0),
            act("expanding_energy_blast", "Expanding Energy Blast", false, 1500, 9, m(Prim.STOP, 40, 24, 3.5, 12, 0, 30, FREEZE_PROJ | SNAP, 0xFFFFFFFF)),
            act("krillin_kamehameha", "Kamehameha", true, 2600, 12, m(Prim.BEAM, 55, 50, 1.6, 6, 4, 50, STUN, 0xFF60D0FF)),
            fin("destructo_disc", "Destructo Disc", 14000, 9500, 80, 45, m(Prim.DISC, 105, 56, 1.4, 1, 6, 0, BYPASS | IGNORE_FRAMES | STRIP | HOMING, 0xFFFFF040))));

    public static final Spec ANDROID18 = new Spec("db_android18", "Lazuli", "Lord of Infinite Energy", "Infinite Cyborg", 0.78, "Android 18: 300 trillion in the Super Hero conflict", List.of(
            pas("infinite_reactor", "Infinite Reactor", Prim.REACTOR, 0, 0, 0, 0),
            act("energy_shield", "Energy Shield", false, 0, 14, m(Prim.GUARD, 0, 0, 0, 8, 0, 160, HEAL, 0xFF80FFFF)),
            act("high_speed_rush", "High Speed Rush", true, 0, 9, m(Prim.RUSH, 18, 14, 0, 7, 4, 20, STUTTER, 0xFFFFFFFF)),
            fin("lazuli_destructo_disc", "Destructo-Disc", 0, 0, 60, 30, m(Prim.DISC, 100, 64, 1.4, 1, 8, 0, IGNORE_FRAMES | HOMING | BYPASS, 0xFFFFE040))));

    public static final Spec ANDROID17 = new Spec("db_android17", "Lapis", "Lord of Barriers", "Park Ranger Defender", 0.76, "Android 17: peak defender of the Super Hero conflict", List.of(
            pas("perpetual_stamina", "Perpetual Stamina", Prim.STAMINA, 0, 0, 0, 0),
            act("lapis_energy_shield", "Energy Shield", false, 2200, 16, m(Prim.GUARD, 0, 0, 7, 10, 0, 200, FREEZE_PROJ, 0xFF40FF80)),
            act("android_kick", "Android Kick", true, 2800, 10, m(Prim.RUSH, 70, 18, 0, 1, 5, 0, IGNORE_FRAMES, 0xFF40FF80)),
            fin("super_electric_strike", "Super Electric Strike", 22000, 15000, 110, 65, m(Prim.BLAST, 90, 0, 20, 24, 8, 140, PARALYZE | FREEZE_PROJ | BYPASS, 0xFF60FF40))));

    public static final Spec GOTEN = new Spec("db_goten", "Goten", "Lord of Inheritance", "Hybrid Prodigy", 0.70, "Goten (teen): 88 million in Super Hero", List.of(
            pas("prodigys_pace", "Prodigy's Pace", Prim.PACK, 0.12, 0, 24, 5),
            act("assault", "Assault!", false, 1600, 8, m(Prim.RUSH, 35, 16, 0, 3, 3, 20, SNAP | STUTTER, 0xFFFFA040)),
            act("energy_blast_volley", "Full Power Energy Blast Volley", true, 3000, 14, m(Prim.BARRAGE, 9, 30, 0, 12, 3, 50, STUN, 0xFFFFD060)),
            fin("goten_super_kamehameha", "Super Kamehameha", 20000, 14000, 100, 60, m(Prim.BEAM, 100, 56, 2.6, 24, 12, 40, DEBT_BLAST | FREEZE_PROJ, 0xFF60C0FF))));

    public static final Spec TRUNKS = new Spec("db_trunks", "Trunks", "Lord of the Future", "Capsule Corp Heir", 0.72, "Trunks (teen): 95 million in Super Hero", List.of(
            pas("tactical_retreat", "Tactical Retreat", Prim.DEATHSAVE, 0.5, 0, 0, 0),
            act("trunks_high_speed_rush", "High Speed Rush", false, 1700, 8, m(Prim.RUSH, 30, 16, 0, 4, 4, 20, SNAP | STUTTER, 0xFFC090FF)),
            act("full_power_energy_wave", "Full Power Energy Wave", true, 3200, 15, m(Prim.BLAST, 60, 0, 12, 24, 3, 0, SNAP | STUTTER, 0xFFB080FF)),
            fin("trunks_finish_buster", "Finish Buster", 21000, 14500, 100, 60, m(Prim.BLAST, 120, 40, 12, 24, 10, 0, DEBT_BLAST | SNAP, 0xFFFF80FF))));

    public static final Spec TIEN = new Spec("db_tien", "Tien", "Lord of the Crane", "Tri-Clops Grandmaster", 0.62, "Tien Shinhan, veteran, Super Hero tier three", List.of(
            pas("third_eye_vision", "Third Eye Vision", Prim.SENSE, 0.2, 0, 28, 0),
            act("ki_blast_cannon", "Ki Blast Cannon", false, 1800, 9, m(Prim.STOP, 50, 22, 4.0, 12, 0, 20, FREEZE_PROJ | SNAP, 0xFFFFFF80)),
            act("dodon_ray", "Dodon Ray", true, 2400, 10, m(Prim.BEAM, 75, 60, 0.7, 4, 3, 0, BYPASS, 0xFFFFE040)),
            fin("neo_tri_beam", "Neo Tri-Beam", 20000, 14000, 110, 65, m(Prim.BEAM, 125, 58, 2.4, 24, 10, 100, BYPASS | PARALYZE | SELF_COST, 0xFFFFFF60))));

    public static final Spec YAMCHA = new Spec("db_yamcha", "Yamcha", "Lord of the Wolf", "Desert Wolf", 0.45, "Yamcha, foundational Z-Fighter, Super Hero tier four", List.of(
            pas("desert_bandits_agility", "Desert Bandit's Agility", Prim.DODGE, 0.30, 0.30, 24, 0),
            act("yamcha_kamehameha", "Kamehameha", false, 1500, 10, m(Prim.BEAM, 48, 46, 1.4, 5, 8, 0, 0, 0xFF50B8FF)),
            act("wolf_fang_fist", "Wolf Fang Fist", true, 2000, 9, m(Prim.BARRAGE, 7, 6, 0, 10, 2, 30, BLEED | STUN, 0xFFFFA060)),
            fin("chou_soukidan", "Chou Soukidan", 14000, 9500, 90, 50, m(Prim.DISC, 70, 48, 1.8, 6, 4, 20, HOMING | IGNORE_FRAMES, 0xFFFFFF60))));

    public static final Spec CHIAOTZU = new Spec("db_chiaotzu", "Chiaotzu", "Lord of Telekinesis", "Psychic Emperor", 0.42, "Chiaotzu, psychic Crane School fighter, Super Hero tier four", List.of(
            pas("psychic_float", "Psychic Float", Prim.FLOAT, 0, 0, 0, 0),
            act("telekinesis", "Telekinesis", false, 1800, 8, m(Prim.PSYCHIC, 0, 36, 0, 1, 3, 80, PARALYZE, 0xFFB0A0FF)),
            act("psychic_rock_throw", "Psychic Rock Throw", true, 2600, 12, m(Prim.PSYCHIC, 60, 36, 0, 6, 3, 20, SNAP, 0xFFB0A0FF)),
            fin("farewell_mr_tien", "Farewell, Mr. Tien", 8000, 5000, 150, 90, m(Prim.DETONATE, 160, 4, 14, 24, 10, 60, SELF_COST | BYPASS, 0xFFFFFFFF))));

    public static final Spec ROSHI = new Spec("db_roshi", "Roshi", "Lord of the Turtle School", "Turtle Hermit", 0.58, "Master Roshi, legendary Max Power form, Super Hero tier three", List.of(
            pas("hermits_wisdom", "Hermit's Wisdom", Prim.EXTEND, 0, 0, 16, 0),
            act("afterimage_strike", "Afterimage Strike", false, 1500, 12, m(Prim.VEIL, 40, 10, 0, 1, 2, 0, STUTTER, 0xFFFFFFFF)),
            act("roshi_evil_containment_wave", "Evil Containment Wave", true, 4200, 40, m(Prim.SEAL, 0, 34, 0, 1, 8, 120, SILENCE | PARALYZE | BURY, 0xFF60FF90)),
            fin("max_power_kamehameha", "MAX Power Kamehameha", 20000, 14000, 120, 70, m(Prim.BEAM, 115, 70, 4.0, 24, 10, 40, FREEZE_PROJ | BYPASS, 0xFF60D8FF))));

    public static final Spec FUTURE_TRUNKS = new Spec("db_future_trunks", "Trunks", "Lord of Hope", "Timeline Savior", 0.80, "Future Trunks: the veteran of erased timelines, Super Hero tier two", List.of(
            pas("chronology_denial", "Chronology Denial", Prim.DEATHSAVE, 0.4, 1, 0, 0),
            act("burning_attack", "Burning Attack", false, 2200, 9, m(Prim.BEAM, 70, 52, 2.0, 8, 5, 30, 0, 0xFFFFA0FF)),
            act("future_finish_buster", "Finish Buster", true, 4000, 15, m(Prim.BLAST, 95, 40, 12, 24, 15, 0, DEBT_BLAST | SNAP, 0xFFFF80FF)),
            fin("shining_sword_attack", "Shining Sword Attack", 24000, 16000, 120, 70, m(Prim.RUSH, 22, 20, 0, 8, 12, 40, STRIP | BYPASS | IGNORE_FRAMES | DEBT_BLAST, 0xFFD0A0FF))));

    public static final Spec BULMA = new Spec("db_bulma", "Bulma", "Lord of Invention", "Genius Inventor", 0.30, "Bulma, support and technology, Super Hero tier five", List.of(
            pas("genius_intellect", "Genius Intellect", Prim.GENIUS, 0.3, 0, 20, 0),
            act("scan_mode_3d", "3D Scan Mode", false, 1200, 10, m(Prim.SCAN, 0, 48, 48, 24, 0, 0, 0, 0xFF60FFFF)),
            act("capsule_toss", "Capsule Toss", true, 1500, 20, m(Prim.CAPSULE, 0, 0, 14, 24, 0, 0, HEAL, 0xFFFFE0A0)),
            fin("dragon_radar_ping", "Dragon Radar Ping", 12000, 8000, 100, 55, m(Prim.SCAN, 0, 64, 64, 24, 12, 60, REJECT | ARMOR_BREAK, 0xFF60FF80))));

    public static final List<Spec> SPECS = List.of(GOKU, VEGETA, GOHAN, PICCOLO, KRILLIN, ANDROID18, ANDROID17, GOTEN, TRUNKS, TIEN, YAMCHA, CHIAOTZU, ROSHI, FUTURE_TRUNKS, BULMA);

    public static Spec spec(String id) {
        if (id == null) return null;
        for (Spec s : SPECS) if (s.id().equals(id) || s.uniqueId().equals(id)) return s;
        return null;
    }
}
