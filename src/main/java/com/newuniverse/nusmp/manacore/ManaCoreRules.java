package com.newuniverse.nusmp.manacore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the rank Mana Cores (3.51.0, docs/mana_cores.md) in one pure table (no Minecraft types, so it is unit tested): 8 ranks (E D C B A AA S SS) x 3 variants (standard, beast,
 * ancient) = 24 cores, each with a passive and two actives (Press = right-click, Crouch + Press = sneak + right-click). The Java side only reads this table: it never invents a number, and every
 * active carries its own hard caps (targets, radius, duration, cooldown floor) that {@link #validate} checks, so a typo can not ship an unbounded effect. The sector-key
 * {@code labyrinth.mega.guard.ManaCoreItem} is a different item and is not touched.
 */
public final class ManaCoreRules {
    private ManaCoreRules() {}

    public static final String[] RANKS = {"e", "d", "c", "b", "a", "aa", "s", "ss"};
    public static final String[] VARIANTS = {"standard", "beast", "ancient"};
    public static final String[] RANK_LABEL = {"E", "D", "C", "B", "A", "AA", "S", "SS"};
    /** Existence Points a wielder needs for the rank's passives and actives (a gate the server can switch off). */
    public static final double[] EP_GATE = {0, 300, 1_000, 5_000, 20_000, 50_000, 100_000, 500_000};
    /** The Existence Points a creature needs to count as this rank when it drops a core (a tenth of the wielder gate, E from 100). */
    public static final double[] EP_VICTIM = {100, 300, 1_000, 5_000, 20_000, 50_000, 100_000, 500_000};
    /** Base damage of an active by rank, run through the mod's balanced damage (so it still scales with EP and is capped on bosses). */
    public static final double[] POWER = {3, 5, 8, 12, 18, 26, 40, 60};
    /** Chance per qualifying kill of a standard core (the beast core is half of it); times the server's drop scale. */
    public static final double[] DROP = {0.03, 0.02, 0.015, 0.01, 0.006, 0.004, 0.002, 0.001};

    // hard caps no active may pass (validate())
    public static final int MAX_TARGETS = 32, MAX_DURATION = 600, MIN_COOLDOWN = 20, MAX_DELAY = 100;
    public static final double MAX_RADIUS = 30.0, MAX_RANGE = 32.0;
    /** The last-second countdown, its cooldown (600 s as the owner asked), the zone and what it may lift or hit at once. */
    public static final int LAST_SECOND_TICKS = 600, LAST_SECOND_COOLDOWN = 12_000, LAST_SECOND_TARGETS = 24;
    public static final double LAST_SECOND_RADIUS = 24.0;
    /** How long a stopped world lasts for the user (10 s), and for a player or a boss caught in it (the shared hard-control caps still cut those). */
    public static final int STILL_SECONDS_TICKS = 200;
    public static final int POSITION_KEEP = 4;                       // one remembered position per second, for Temporal Echo (1 s) and Temporal Safeguard (3 s)
    public static final int MAX_RUNES = 3, RUNE_LIFE_TICKS = 1200;
    public static final int LETHAL_REWIND_COOLDOWN = 3_000;          // Temporal Safeguard: once per 150 s
    public static final int OVERCLOCK_SELF_DAMAGE = 4;               // the owner asked 10 HP per second; capped to two hearts once, never lethal (needs more than 8 health)

    public enum Kind { BUFF, BURST, LINE, CHAIN, DASH, BLINK, BARRIER, PULL, LAUNCH, LEAP, FREEZE_TARGET, FREEZE_AREA, AREA, ECHO, RUNE, REDIRECT, CLEANSE, LAST_SECOND, BEAST_FORM }

    /**
     * One active. {@code mp} is the owner's listed cost in magicules (paid through the shared pool, times the server's costScale); {@code cd} cooldown ticks; {@code power} multiplies the rank damage (0 = no damage);
     * {@code radius} is the area or the range of a line; {@code delay} ticks before a burst lands (0 = at once); {@code effect} a status effect id applied to what is hit (or to the user for BUFF/BARRIER) for {@code dur} ticks.
     */
    public record Active(String key, Kind kind, int mp, int cd, double power, double radius, int targets, int dur, int delay, String effect, int amp) {}

    /** A passive attribute change: attribute id, operation ("add" or "mult"), amount. */
    public record Mod(String attribute, String op, double amount) {}
    public record Fx(String effect, int amp) {}

    /** A passive: attribute mods, renewed status effects, magicules per second as a share of the maximum (negative leaks), and named numeric flags the events read. */
    public record Passive(String key, List<Mod> mods, List<Fx> effects, double mpPct, Map<String, Double> flags) {
        public double flag(String f) { return flags.getOrDefault(f, 0.0); }
    }

    public record Core(int rank, int variant, String name, String will, Passive passive, Active a1, Active a2) {
        public String id() { return "core_" + RANKS[rank] + "_" + VARIANTS[variant]; }
        public Active active(int n) { return n == 2 ? a2 : a1; }
        public boolean beastForm() { return a2.kind == Kind.BEAST_FORM; }
    }

    // ------------------------------------------------------------------ builders (the table below reads like the owner's list)
    private static Active act(String k, Kind kind, int mp, int cd, double power, double radius, int targets, int dur, int delay, String eff, int amp) {
        return new Active(k, kind, mp, cd, power, radius, targets, dur, delay, eff, amp);
    }

    private static Mod add(String a, double v) { return new Mod("minecraft:generic." + a, "add", v); }
    private static Mod mult(String a, double v) { return new Mod("minecraft:generic." + a, "mult", v); }
    private static Fx fx(String e, int amp) { return new Fx("minecraft:" + e, amp); }

    private static Passive pas(String key, double mpPct, List<Mod> mods, List<Fx> fx, Object... flags) {
        Map<String, Double> f = new LinkedHashMap<>();
        for (int i = 0; i + 1 < flags.length; i += 2) f.put((String) flags[i], ((Number) flags[i + 1]).doubleValue());
        return new Passive(key, List.copyOf(mods), List.copyOf(fx), mpPct, Map.copyOf(f));
    }

    private static final List<Core> CORES = new ArrayList<>();

    private static void core(int rank, int variant, String name, String will, Passive p, Active a1, Active a2) { CORES.add(new Core(rank, variant, name, will, p, a1, a2)); }

    private static final List<Mod> NM = List.of();
    private static final List<Fx> NF = List.of();

    static {
        // ---------------------------------------------------------------- E
        core(0, 0, "Crude Mana Pebble", null, pas("mana_leak", -0.0003, List.of(mult("attack_speed", 0.05)), NF),
                act("core_spark", Kind.BUFF, 50, 400, 0, 0, 1, 200, 0, "minecraft:haste", 0),
                act("mana_shard_toss", Kind.LINE, 80, 100, 0.4, 12, 1, 40, 0, "minecraft:slowness", 0));
        core(0, 1, "Ridge-Wolf Fang Core", null, pas("beast_instinct", 0, List.of(add("attack_damage", 1.0)), List.of(fx("night_vision", 0)), "kill_heal", 0.03),
                act("wolf_howl", Kind.BURST, 100, 200, 0.2, 6, 8, 60, 0, "minecraft:nausea", 0),
                act("savage_lunge", Kind.DASH, 120, 120, 0.8, 6, 1, 0, 0, null, 0));
        core(0, 2, "Relic-Scrap Core", null, pas("dormant_circuit", 0, NM, NF, "proj_absorb", 4.0, "proj_mp", 0.002),
                act("overclock_pulse", Kind.BUFF, 150, 600, 0, 0, 1, 100, 0, "minecraft:haste", 2),
                act("temporal_echo", Kind.ECHO, 200, 600, 0, 0, 1, 0, 0, null, 0));
        // ---------------------------------------------------------------- D
        core(1, 0, "Refined Sphere", null, pas("stable_channeling", 0, NM, NF, "cost_mult", 0.95, "aura_shield", 5.0),
                act("mana_burst", Kind.BURST, 250, 160, 0.6, 5, 8, 0, 0, null, 0),
                act("elemental_infusion", Kind.BUFF, 300, 400, 0, 0, 1, 300, 0, "minecraft:strength", 0));
        core(1, 1, "Crested Boar Bile Core", null, pas("toxic_resilience", 0, NM, NF, "poison_immune", 1.0),
                act("venom_spit", Kind.LINE, 280, 160, 0.4, 10, 6, 120, 0, "minecraft:poison", 0),
                act("hardened_hide", Kind.BUFF, 350, 300, 0, 0, 1, 160, 0, "minecraft:resistance", 1));
        core(1, 2, "Asura-Touched D-Core", null, pas("minor_spatial_anchor", 0, List.of(add("knockback_resistance", 0.6)), NF),
                act("spatial_ripple", Kind.BLINK, 400, 140, 0.5, 4, 4, 0, 0, null, 0),
                act("inertial_lock", Kind.FREEZE_TARGET, 450, 300, 0, 16, 1, 30, 0, null, 0));
        // ---------------------------------------------------------------- C
        core(2, 0, "Luminous Crystal Orb", null, pas("mana_resonance", 0, NM, List.of(fx("night_vision", 0)), "aoe_mult", 1.1),
                act("luminous_beam", Kind.LINE, 600, 120, 1.0, 16, 3, 0, 0, null, 0),
                act("c_tier_barrier", Kind.BARRIER, 750, 400, 0, 0, 1, 80, 0, "minecraft:absorption", 1));
        core(2, 1, "Wind-Skimmer Core", null, pas("wind_rider", 0, List.of(mult("movement_speed", 0.15), add("safe_fall_distance", 256.0)), NF),
                act("air_cutter", Kind.LINE, 650, 100, 1.0, 14, 4, 60, 0, "minecraft:wither", 0),
                act("gale_burst", Kind.LEAP, 800, 200, 0, 0, 1, 60, 0, "minecraft:slow_falling", 0));
        core(2, 2, "Runebound C-Core", null, pas("spell_matrix", 0, NM, NF, "free_every", 3.0),
                act("rune_trapping", Kind.RUNE, 700, 200, 0, 1.5, 1, 60, 0, null, 0),
                act("kinetic_redirection", Kind.REDIRECT, 900, 300, 0, 6, 4, 0, 0, null, 0));
        // ---------------------------------------------------------------- B
        core(3, 0, "Plasma-Charged Sphere", null, pas("lightning_reflexes", 0, NM, NF, "dodge", 0.10, "chain_mult", 1.15),
                act("chain_lightning_arc", Kind.CHAIN, 1200, 120, 1.0, 6, 5, 15, 0, "minecraft:slowness", 4),
                act("static_field", Kind.AREA, 1500, 400, 0.5, 4, 8, 120, 0, "minecraft:slowness", 1));
        core(3, 1, "Flame-Serpent Core", null, pas("magma_blood", 0.0005, NM, NF, "fire_immune", 1.0),
                act("inferno_eruption", Kind.BURST, 1400, 200, 1.0, 7, 10, 100, 0, "fire", 0),
                act("serpent_dash", Kind.DASH, 1600, 160, 1.0, 8, 6, 100, 0, "fire", 0));
        core(3, 2, "Singularity B-Core", null, pas("gravity_well", 0, NM, NF, "item_pull", 5.0),
                act("micro_singularity", Kind.PULL, 1800, 300, 0.6, 8, 8, 0, 0, null, 0),
                act("mass_inversion", Kind.LAUNCH, 2000, 300, 0.3, 16, 1, 40, 0, "minecraft:levitation", 0));
        // ---------------------------------------------------------------- A
        core(4, 0, "High-Density Plasma Core", null, pas("core_condensation", 0.003, NM, NF, "thorns", 3.0),
                act("plasma_nova", Kind.BURST, 3500, 240, 1.2, 10, 10, 100, 0, "minecraft:weakness", 1),
                act("lightning_flash_step", Kind.BLINK, 4000, 160, 0.8, 15, 6, 0, 0, null, 0));
        core(4, 1, "Shadow Panther Core + Will", "shadow_panther_will", pas("acquire_shadow_stride", 0, List.of(mult("movement_speed", 0.10)), List.of(fx("night_vision", 0)), "beast_will", 1.0),
                act("apex_roar", Kind.BURST, 3800, 400, 0, 20, 10, 60, 0, "minecraft:slowness", 4),
                act("integrate_panther_avatar", Kind.BEAST_FORM, 0, 0, 0, 0, 1, 0, 0, null, 0));
        core(4, 2, "Dimension-Tear A-Core", null, pas("spatial_shifting", 0, NM, NF, "phase_cd", 300.0),
                act("rift_slash", Kind.LINE, 4500, 160, 1.5, 12, 6, 0, 0, null, 0),
                act("temporal_stutter_field", Kind.AREA, 5000, 500, 0, 10, 10, 120, 0, "minecraft:slowness", 3));
        // ---------------------------------------------------------------- AA
        core(5, 0, "Prismatic Faceted Core", null, pas("prismatic_refraction", 0, NM, NF, "magic_resist", 0.5),
                act("prismatic_beam_matrix", Kind.CHAIN, 7500, 240, 0.8, 24, 5, 0, 0, null, 0),
                act("crystalline_prison", Kind.FREEZE_TARGET, 8200, 800, 0, 20, 1, 100, 0, null, 0));
        core(5, 1, "Thunder-Fang Chimera Core + Will", "thunder_fang_chimera_will", pas("acquire_chimera_grace", 0, NM, NF, "beast_will", 1.0, "lightning_resist", 0.5, "thorns", 6.0),
                act("multi_head_strike", Kind.BURST, 7800, 240, 1.2, 9, 8, 0, 0, null, 0),
                act("integrate_chimera_transformation", Kind.BEAST_FORM, 0, 0, 0, 0, 1, 0, 0, null, 0));
        core(5, 2, "Void-Weaver AA-Core", null, pas("void_step", 0, NM, NF, "void_trail", 6.0),
                act("void_collapse", Kind.PULL, 9000, 400, 1.5, 10, 10, 0, 0, null, 0),
                act("chrono_lock", Kind.FREEZE_TARGET, 9500, 900, 0, 20, 1, 80, 0, null, 0));
        // ---------------------------------------------------------------- S
        core(6, 0, "Runed Geometric Star Sphere", null, pas("divine_geometry", 0, NM, NF, "proj_shield", 40.0, "cd_mult", 0.7),
                act("starburst_protocol", Kind.BURST, 15000, 400, 1.5, 14, 16, 100, 0, "fire", 0),
                act("absolute_domain", Kind.AREA, 18000, 800, 0, 15, 16, 160, 0, "minecraft:slowness", 3));
        core(6, 1, "Elderwood Guardian Core + Supreme Will", "elderwood_guardian_will", pas("acquire_guardian_roots", 0, NM, List.of(fx("regeneration", 0)), "beast_will", 1.0),
                act("natures_wrath_thorns", Kind.BURST, 16500, 400, 1.2, 20, 16, 80, 0, "minecraft:slowness", 4),
                act("integrate_elderwood_avatar", Kind.BEAST_FORM, 0, 0, 0, 0, 1, 0, 0, null, 0));
        core(6, 2, "Celestial Chrono-Core", null, pas("temporal_safeguard", 0, NM, NF, "lethal_rewind", 3.0, "time_immune", 1.0),
                act("causality_rewrite", Kind.CLEANSE, 22000, 600, 0, 20, 6, 0, 0, null, 0),
                act("world_stills", Kind.FREEZE_AREA, 25000, 1800, 0, 20, 24, 80, 0, null, 0));
        // ---------------------------------------------------------------- SS
        core(7, 0, "Cosmic Constellation Sphere", null, pas("infinite_horizon", 0, NM, NF, "combat_mp", 0.03),
                act("constellation_judgment", Kind.BURST, 45000, 600, 2.0, 18, 20, 100, 40, "fire", 0),
                act("stellar_singularity", Kind.BURST, 55000, 900, 3.0, 30, 24, 100, 60, "fire", 0));
        core(7, 1, "Nihil Dragon Core + Mythic Will", "dragon_will", pas("worlds_maw", 0, NM, NF, "beast_will", 1.0, "kill_heal", 0.25),
                act("cataclysmic_dragon_roar", Kind.BURST, 48000, 600, 2.0, 20, 24, 100, 0, "minecraft:weakness", 2),
                act("integrate_nihil_ascendancy", Kind.BEAST_FORM, 0, 0, 0, 0, 1, 0, 0, null, 0));
        core(7, 2, "World Line Core / Dragon Fragment", null, pas("timeline_mastery", 0, List.of(mult("movement_speed", 0.2), mult("attack_speed", 0.2)), NF, "time_immune", 1.0),
                act("world_of_still_seconds", Kind.FREEZE_AREA, 75000, 1800, 0, MAX_RADIUS - 6, 32, STILL_SECONDS_TICKS, 0, null, 0),
                act("last_second_of_creation", Kind.LAST_SECOND, 120000, LAST_SECOND_COOLDOWN, 4.0, LAST_SECOND_RADIUS, LAST_SECOND_TARGETS, LAST_SECOND_TICKS, 0, null, 0));
    }

    /** The cost text of a tooltip: the magicules, or the words "Beast Will" for a form the Beast Will system pays itself. */
    public static String mpText(Active a) { return a.kind == Kind.BEAST_FORM ? "Beast Will" : String.format(java.util.Locale.ROOT, "%,d MP", a.mp); }

    public static List<Core> all() { return List.copyOf(CORES); }

    public static Core byId(String id) {
        if (id != null) for (Core c : CORES) if (c.id().equals(id)) return c;
        return null;
    }

    public static Core of(int rank, int variant) {
        for (Core c : CORES) if (c.rank == rank && c.variant == variant) return c;
        return null;
    }

    public static int rankByName(String n) {
        if (n != null) for (int i = 0; i < RANKS.length; i++) if (RANKS[i].equalsIgnoreCase(n.trim())) return i;
        return -1;
    }

    public static int variantByName(String n) {
        if (n != null) for (int i = 0; i < VARIANTS.length; i++) if (VARIANTS[i].equalsIgnoreCase(n.trim())) return i;
        return -1;
    }

    /** The rank a creature of this many Existence Points counts as for a drop, or -1 under the lowest band. */
    public static int victimRank(double ep) {
        int r = -1;
        for (int i = 0; i < EP_VICTIM.length; i++) if (ep >= EP_VICTIM[i]) r = i;
        return r;
    }

    /** True when a wielder of this many Existence Points may use the rank (the gate can be switched off by the server). */
    public static boolean mayUse(int rank, double ep, boolean gate) { return !gate || ep >= EP_GATE[Math.max(0, Math.min(EP_GATE.length - 1, rank))]; }

    /** The cooldown of an active after the server scale, the S standard core's cooldown flag and the floor. */
    public static int cooldown(int cd, double serverScale, double flagMult) {
        return Math.max(MIN_COOLDOWN, (int) Math.round(cd * Math.max(0.5, Math.min(2.0, serverScale)) * (flagMult <= 0 ? 1.0 : Math.max(0.5, Math.min(1.0, flagMult)))));
    }

    /** A spent-in-a-row bonus: every {@code every}-th cast is free (Runebound Spell Matrix); 0 = never. */
    public static boolean freeCast(int count, double every) { return every >= 2 && count > 0 && count % (int) every == 0; }

    /** Position of a second-by-second history: {@code secondsAgo} (1..POSITION_KEEP-1) back from the newest. */
    public static int historyIndex(int size, int secondsAgo) { return size - 1 - secondsAgo; }

    /** Null when the whole table is sound; otherwise the first problem, in words. */
    public static String validate() {
        if (CORES.size() != RANKS.length * VARIANTS.length) return "expected 24 cores, found " + CORES.size();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Core c : CORES) {
            if (!ids.add(c.id())) return "duplicate id " + c.id();
            if (c.name().toLowerCase().contains("ista" + "roth")) return c.id() + " uses the old name (the SS ancient core is the Dragon Fragment)";
            if (c.variant == 1 && c.rank >= 4 && (c.will == null || !c.beastForm() || c.passive.flag("beast_will") != 1.0)) return c.id() + " must plug into a Beast Will";
            if (c.variant != 1 && c.will != null) return c.id() + " is not a beast core but names a will";
            for (int n = 1; n <= 2; n++) {
                Active a = c.active(n);
                String w = c.id() + "." + a.key + ": ";
                if (a.kind != Kind.BEAST_FORM && a.mp <= 0) return w + "no cost";
                if (a.kind != Kind.BEAST_FORM && a.cd < MIN_COOLDOWN) return w + "cooldown below the floor";
                if (a.targets < 1 || a.targets > 32) return w + "targets out of bounds";
                if (a.radius < 0 || a.radius > MAX_RADIUS) return w + "radius out of bounds";
                if (a.dur < 0 || a.dur > MAX_DURATION) return w + "duration out of bounds";
                if (a.delay < 0 || a.delay > MAX_DELAY) return w + "delay out of bounds";
                if (a.power < 0 || a.power > 4.0) return w + "power out of bounds";
            }
            for (Mod m : c.passive.mods) if (Math.abs(m.amount) > 300 || !(m.op.equals("add") || m.op.equals("mult"))) return c.id() + ": a passive mod is out of bounds";
            if (Math.abs(c.passive.mpPct) > 0.05) return c.id() + ": magicule regeneration passes 5% per second";
        }
        return null;
    }
}
