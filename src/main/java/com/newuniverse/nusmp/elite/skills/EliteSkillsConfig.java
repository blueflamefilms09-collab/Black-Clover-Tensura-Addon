package com.newuniverse.nusmp.elite.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the elite skills were designed without ever being played, so a server owner needs honest dials that do not need a data pack, and a master switch that makes the whole subsystem
 * do nothing (no skill acts, no evolution is checked, no handler touches an event) while the skills stay registered so saved worlds still load. {@code nusmp-elite-skills-server.toml}.
 * Every value is a plain {@code define} / {@code defineInRange} (a list-validated value throws while the server is still loading) and every reader answers the default while the spec is not
 * loaded yet. The three global multipliers apply on top of the pack-wide DanMachi multipliers; the rest are the few switches whose default a server may reasonably want to change.
 */
public final class EliteSkillsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue DAMAGE_SCALE, COST_SCALE, COOLDOWN_SCALE, KAIOKEN_DAMAGE_SCALE, KAIOKEN_DRAIN_SCALE, MEDITATION_REGEN_SCALE;
    public static final ModConfigSpec.IntValue GAIA_DAILY_VEINS, EVOLUTION_CHECK_SECONDS;
    public static final ModConfigSpec.BooleanValue GAIA_BLOCK_GUARD, GAIA_BLOOM, GAIA_PILLARS, SCAVENGER_DROPS, VOID_NULLIFY_TELEPORT, VOID_NULLIFY_PROJECTILES,
            OUROBOROS_AUTO_REWIND, EVOLUTION_ENABLED, EVOLUTION_BROADCAST, KILL_TRACKING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("elite_skills");
        ENABLED = b.comment("Master switch of the elite skills (Kamehameha, Gravity Well, Static Chain, Kaioken, Gaia, ... and their evolution engine). Off: the skills do nothing and nothing is checked; they stay registered so worlds still load.")
                .define("enabled", true);
        b.push("balance");
        DAMAGE_SCALE = b.comment("Multiplies the damage of every elite skill (on top of the balance law and the DanMachi multiplier). The numbers were designed without being played: this is the first dial to turn.")
                .defineInRange("damageScale", 1.0, 0.0, 10.0);
        COST_SCALE = b.comment("Multiplies the magicule cost of every elite skill.").defineInRange("costScale", 1.0, 0.0, 10.0);
        COOLDOWN_SCALE = b.comment("Multiplies the cooldown of every elite skill.").defineInRange("cooldownScale", 1.0, 0.0, 10.0);
        KAIOKEN_DAMAGE_SCALE = b.comment("Multiplies Kaioken's melee damage and speed bonuses (1.0 = as designed, +30% at x2 up to +400% at x20).")
                .defineInRange("kaiokenDamageScale", 1.0, 0.0, 3.0);
        KAIOKEN_DRAIN_SCALE = b.comment("Multiplies the health Kaioken burns per second (it always shuts itself off before it can kill its wearer).")
                .defineInRange("kaiokenDrainScale", 1.0, 0.0, 5.0);
        MEDITATION_REGEN_SCALE = b.comment("Multiplies the magicules Meditation returns per second.").defineInRange("meditationRegenScale", 1.0, 0.0, 5.0);
        b.pop();
        b.push("gaia");
        GAIA_BLOCK_GUARD = b.comment("Inside a raised Gaia domain, strangers cannot break blocks and explosions cannot destroy them.").define("blockGuard", true);
        GAIA_BLOOM = b.comment("Gaia may grow veins of ore in the stone of its domain (limited by the bloom ledger and the daily cap).").define("bloom", true);
        GAIA_DAILY_VEINS = b.comment("Most veins one owner can bloom per game day. 0 turns blooming off.").defineInRange("dailyVeins", 8, 0, 64);
        GAIA_PILLARS = b.comment("Allow the Pillars power (earth erupting in a line; places no blocks).").define("pillars", true);
        b.pop();
        b.push("other");
        SCAVENGER_DROPS = b.comment("Scavenger may duplicate one stack of a kill's drops (at most one extra stack per kill).").define("scavengerDrops", true);
        VOID_NULLIFY_TELEPORT = b.comment("Void Sovereign's domain stops the teleporting of foes inside it (ender pearls, Endermen, chorus fruit).").define("voidNullifyTeleport", true);
        VOID_NULLIFY_PROJECTILES = b.comment("Void Sovereign's domain erases foreign projectiles that enter it.").define("voidNullifyProjectiles", true);
        OUROBOROS_AUTO_REWIND = b.comment("Ouroboros rewinds its owner by itself when a killing blow lands (once per its long cooldown).").define("ouroborosAutoRewind", true);
        b.pop();
        b.push("evolution");
        EVOLUTION_ENABLED = b.comment("The data-driven evolution engine (data/nusmp/elite/skills/evolutions.json). It is an additional path next to the old mastery-based Unique -> Ultimate evolution, which is untouched.")
                .define("enabled", true);
        EVOLUTION_CHECK_SECONDS = b.comment("How often each player is checked for a satisfied evolution, in seconds.").defineInRange("checkSeconds", 5, 1, 600);
        EVOLUTION_BROADCAST = b.comment("Announce an evolution to the whole server (like the old Voice of the World line).").define("broadcast", true);
        KILL_TRACKING = b.comment("Count kills per skill for the kill_evolution trigger (stored in the player's data).").define("killTracking", true);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private EliteSkillsConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static double damageScale() { return get(DAMAGE_SCALE); }

    public static double costScale() { return get(COST_SCALE); }

    public static double cooldownScale() { return get(COOLDOWN_SCALE); }

    /** Magicule cost after the elite scale AND the pack-wide multiplier. */
    public static double cost(double base) { return com.newuniverse.nusmp.skill.EliteBridge.cost(base * costScale()); }

    /** Cooldown ticks after the elite scale AND the pack-wide multiplier. */
    public static int cooldown(int baseTicks) { return com.newuniverse.nusmp.skill.EliteBridge.cooldown((int) Math.round(baseTicks * cooldownScale())); }
}
