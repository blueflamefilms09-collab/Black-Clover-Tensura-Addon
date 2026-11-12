package com.newuniverse.nusmp.devil;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Devil Contract system ({@code serverconfig/nusmp-devil-server.toml}, 3.48.0). Everything here changes gameplay, so it is a server setting (rule 9); every value is
 * clamped again when it is read, so a hand-edited file can not go past a bound. {@code enabled = false} restores the game as it was before the feature: the key does nothing, no stat is added, the union can
 * not begin, a level in progress ends, and the new sacrifices (half health, a lost eye) stop having an effect (they stay on the record). What only changes looks (the markings, the horns and wings, the eye
 * shade) is a CLIENT setting: {@code client.devil.DevilClientConfig}.
 */
public final class DevilConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, ALLOW_FLIGHT, HALF_HEALTH, EYE_REACH, DEATH_COOLDOWN, WORLDS, EFFECTS;
    private static final ModConfigSpec.DoubleValue DRAIN_SCALE, STAT_SCALE, EFFECT_SCALE;
    private static final ModConfigSpec.IntValue UNION_SECONDS, COOLDOWN_HOURS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("devil");
        ENABLED = b.comment("The Devil Contract system: assimilation key, the Devil Union and the new sacrifices. false = off: the key does nothing, no stat is added, a level in progress ends, the new sacrifices have no effect.")
                .define("enabled", true);
        b.push("union");
        UNION_SECONDS = b.comment("How long the Devil Union lasts, in seconds. Strict: it ends then, whatever the player does (default 5 minutes).")
                .defineInRange("unionSeconds", DevilRules.DEFAULT_UNION_SECONDS, 20, 3600);
        COOLDOWN_HOURS = b.comment("How long after the union before it can begin again, in in-game hours (1000 ticks each; the default 24 is one Minecraft day, 24 000 ticks). Counted in game time, so sleeping does not shorten it.")
                .defineInRange("cooldownHours", DevilRules.DEFAULT_COOLDOWN_HOURS, 0, 240);
        DEATH_COOLDOWN = b.comment("Dying inside the union starts the cooldown. false = a death ends the union without a cooldown.").define("deathStartsCooldown", true);
        b.pop();
        b.push("balance");
        DRAIN_SCALE = b.comment("Multiplier on the magicules assimilation and the union drain every second.").defineInRange("drainScale", 1.0, 0.0, 4.0);
        STAT_SCALE = b.comment("Multiplier on the attack damage and movement speed bonuses (each also has a hard cap in the rules).").defineInRange("statScale", 1.0, 0.0, 2.0);
        ALLOW_FLIGHT = b.comment("From 50 % the wings give creative-style flight. false = the wings are only for show.").define("allowFlight", true);
        b.pop();
        b.push("sacrifices");
        HALF_HEALTH = b.comment("The 'half' price halves the maximum health for good (it stays through death). false = it is only recorded.").define("halfHealth", true);
        EYE_REACH = b.comment("A lost eye also shortens how far the player can reach (depth perception). false = a lost eye only changes the screen (client side).").define("eyeReachPenalty", true);
        b.pop();
        b.push("worlds");
        WORLDS = b.comment("3.76.0: devils of eight anime worlds can be called with /devil worlds (Black Clover, Chainsaw Man, Black Butler, Blue Exorcist, Tensura, Dororo, Berserk, Fairy Tail), each by its own ritual.",
                "false = the rituals do not begin (contracts already signed stay); the old grimoire devils are not touched either way.").define("enabled", true);
        b.pop();
        b.push("attribute");
        EFFECTS = b.comment("3.76.0: the devil's attribute acts in a fight while the devil is let in (from 20 % assimilation): extra damage and its own effect on the host's blows. false = the attribute is a label, as before 3.76.0.")
                .define("combatEffects", true);
        EFFECT_SCALE = b.comment("Multiplier on the extra damage of the attribute (the bonus is never more than three times the blow).").defineInRange("effectScale", 1.0, 0.0, 10.0);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private DevilConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return DevilRules.clampD(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return DevilRules.clampInt(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean enabled() { return on(ENABLED, true); }
    public static long unionTicks() { return DevilRules.unionTicks(whole(UNION_SECONDS, DevilRules.DEFAULT_UNION_SECONDS, 20, 3600)); }
    public static long cooldownTicks() { return DevilRules.cooldownTicks(whole(COOLDOWN_HOURS, DevilRules.DEFAULT_COOLDOWN_HOURS, 0, 240)); }
    public static boolean deathStartsCooldown() { return on(DEATH_COOLDOWN, true); }
    public static double drainScale() { return num(DRAIN_SCALE, 1.0, 0, 4); }
    public static double statScale() { return num(STAT_SCALE, 1.0, 0, 2); }
    public static boolean allowFlight() { return on(ALLOW_FLIGHT, true); }
    public static boolean halfHealth() { return on(HALF_HEALTH, true); }
    public static boolean eyeReach() { return on(EYE_REACH, true); }
    public static boolean worlds() { return on(WORLDS, true); }
    public static boolean attributeEffects() { return on(EFFECTS, true); }
    public static double effectScale() { return num(EFFECT_SCALE, 1.0, 0, 10); }
}
