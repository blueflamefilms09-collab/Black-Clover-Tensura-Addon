package com.newuniverse.nusmp.elite.races;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the switches and the dials of the elite races, {@code nusmp-elite-races-server.toml}, section {@code [races]}. The master switch {@code enabled} turns the BEHAVIOUR off
 * (no transformation, no auto trigger, no kill counting, no Zenkai, no evolution through the races' requirements, the commands answer "off"); the five races themselves stay
 * registered either way, because a registry cannot lose entries at run time and a world that holds a Saiyan must still load. Every value is a plain {@code define} /
 * {@code defineInRange} (never a list-validated one: that throws while the server is still loading) and every reader goes through {@link #get}, which answers the default while
 * the spec is not loaded yet (the races are asked for their stats at registration, long before any config exists).
 */
public final class EliteRacesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, TRANSFORMATIONS, AUTO_TRIGGERS, GREAT_APE, GREAT_APE_ANY_NIGHT, ZENKAI, COUNT_KILLS, PRIMORDIAL_RITE, VFX, ANNOUNCE;
    public static final ModConfigSpec.DoubleValue EP_GATE_SCALE, DURATION_SCALE, COOLDOWN_SCALE, DRAIN_SCALE, KAIOKEN_OVERLAP, KILL_MIN_HEALTH;
    public static final ModConfigSpec.ConfigValue<String> KAIOKEN_SKILLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("races");
        ENABLED = b.comment("Master switch of the elite races (Saiyan tiers, Primordial). Off: no transformations, no kill counting, no Zenkai, no evolution through the races' requirements, and the",
                "/multiverse elite race commands answer that it is off. The races stay registered (a registry cannot change at run time).")
                .define("enabled", true);
        TRANSFORMATIONS = b.comment("The transformation states (ki boost, ascension, Great Ape, Divine) and the hotkey / command that enter them.").define("transformations", true);
        AUTO_TRIGGERS = b.comment("States that start by themselves: the rage awakening at low health and the Great Ape under a full moon. Off: every state is entered by the player.")
                .define("autoTriggers", true);
        GREAT_APE = b.comment("Allow the Great Ape form at all.").define("greatApe", true);
        GREAT_APE_ANY_NIGHT = b.comment("True: any night counts as the full moon for the Great Ape's moon rule. False: a real full moon (moon phase 0) is needed.")
                .define("greatApeAnyNight", false);
        ZENKAI = b.comment("The Zenkai trait: a Saiyan who survives a hit at or under 25% health grows a little stronger (a stack, once every 5 minutes, up to 3 per tier).").define("zenkai", true);
        COUNT_KILLS = b.comment("Count the player's kills (any, hostile, boss, player) for the evolution rules.").define("countKills", true);
        PRIMORDIAL_RITE = b.comment("Players may attempt the Primordial Rite on themselves with /multiverse elite race rite (needs the rite's rules to be met). Off: administrators only.")
                .define("primordialRite", true);
        VFX = b.comment("Draw the aura of a transformation (the mod's spirit aura and Light / Dark Rukh).").define("vfx", true);
        ANNOUNCE = b.comment("Tell the player (action bar) when a state starts or ends and why it could not start.").define("announce", true);
        EP_GATE_SCALE = b.comment("Multiplier on every EP gate (evolution rules and state entries). 2.0 = twice as much EP needed.").defineInRange("epGateScale", 1.0, 0.01, 100.0);
        DURATION_SCALE = b.comment("Multiplier on how long a state lasts.").defineInRange("durationScale", 1.0, 0.1, 10.0);
        COOLDOWN_SCALE = b.comment("Multiplier on how long a state rests before it can be entered again. 0 = no rest.").defineInRange("cooldownScale", 1.0, 0.0, 10.0);
        DRAIN_SCALE = b.comment("Multiplier on the aura (ki) a state drains every second. 0 = free.").defineInRange("drainScale", 1.0, 0.0, 10.0);
        KAIOKEN_OVERLAP = b.comment("While the Kaioken skill is on, the ki states' attack and speed multipliers are scaled by this (1.0 = they stack fully, 0 = Kaioken alone counts).")
                .defineInRange("kaiokenOverlapFactor", 0.5, 0.0, 1.0);
        KAIOKEN_SKILLS = b.comment("Registry ids (comma separated) of the skills that count as Kaioken for the overlap rule. Ids that do not exist are ignored.")
                .define("kaiokenSkillIds", "nusmp:kaioken");
        KILL_MIN_HEALTH = b.comment("A creature must have at least this much maximum health to count as a kill (stops chicken farming). Players and bosses always count.")
                .defineInRange("killMinHealth", 10.0, 0.0, 1000.0);
        b.pop();
        SPEC = b.build();
    }

    private EliteRacesConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean transformations() { return enabled() && get(TRANSFORMATIONS); }

    public static boolean autoTriggers() { return transformations() && get(AUTO_TRIGGERS); }

    public static boolean greatApe() { return get(GREAT_APE); }

    public static boolean greatApeAnyNight() { return get(GREAT_APE_ANY_NIGHT); }

    public static boolean zenkai() { return enabled() && get(ZENKAI); }

    public static boolean countKills() { return enabled() && get(COUNT_KILLS); }

    public static boolean primordialRite() { return get(PRIMORDIAL_RITE); }

    public static boolean vfx() { return get(VFX); }

    public static boolean announce() { return get(ANNOUNCE); }

    public static double epGateScale() { return get(EP_GATE_SCALE); }

    public static double durationScale() { return get(DURATION_SCALE); }

    public static double cooldownScale() { return get(COOLDOWN_SCALE); }

    public static double drainScale() { return get(DRAIN_SCALE); }

    public static double kaiokenOverlap() { return get(KAIOKEN_OVERLAP); }

    public static double killMinHealth() { return get(KILL_MIN_HEALTH); }

    /** The configured Kaioken skill ids (never null, trimmed, empties dropped). */
    public static List<String> kaiokenSkillIds() {
        List<String> out = new ArrayList<>();
        for (String s : get(KAIOKEN_SKILLS).split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
