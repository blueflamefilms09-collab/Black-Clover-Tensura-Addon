package com.newuniverse.nusmp.arc.goku;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Goku boss, {@code nusmp-goku-boss-server.toml}, section {@code [goku_boss]}. All of them change gameplay, so they are SERVER settings (rule 9). The strength
 * of the fight comes from the difficulty preset and from the Tensura EP of the players near him ({@link GokuBossMath}); every number is documented in docs/goku_boss.md. Every value is a
 * plain {@code define} / {@code defineInRange} (never a list-validated one: it throws while the server is still loading) and every reader goes through {@link #get}, which answers the
 * default while the spec is not loaded yet.
 */
public final class GokuBossConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BLOCK_DAMAGE, EP_SCALING, FAREWELL;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER, DAMAGE_MULTIPLIER, EP_WEIGHT, EP_CAP, PER_PLAYER_BONUS, MAX_HIT_PERCENT, UI_DODGE_CAP;
    public static final ModConfigSpec.IntValue PLAYER_CAP, GLOBAL_CAP, ENRAGE_MINUTES;

    /** The defaults, also read by the tests. */
    public static final double DEFAULT_EP_WEIGHT = 0.6, DEFAULT_EP_CAP = 8.0, DEFAULT_PER_PLAYER_BONUS = 0.35, DEFAULT_MAX_HIT = 0.30, DEFAULT_DODGE_CAP = 0.35;
    public static final int DEFAULT_GLOBAL_CAP = 2, DEFAULT_ENRAGE_MINUTES = 12;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("goku_boss");
        ENABLED = b.comment("The Goku boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        DIFFICULTY = b.comment("The preset: normal (half health and damage, 60% of the form bonuses), overpowered (the default) or anime (double health, 1.6x damage, 130% of the form bonuses). Unknown words mean overpowered.")
                .define("difficulty", "overpowered");
        HEALTH_MULTIPLIER = b.comment("Multiplies his health on top of the preset and the EP scaling.")
                .defineInRange("healthMultiplier", 1.0, 0.1, 50.0);
        DAMAGE_MULTIPLIER = b.comment("Multiplies his damage on top of the preset and the EP scaling (one hit on a player still ends at maxHitPercent of their health).")
                .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
        EP_SCALING = b.comment("His health, damage and form pace follow the Tensura Existence Points of the strongest players near him (read at spawn and then every 100 ticks). False: Battle Power, then the preset's fixed value.")
                .define("epScaling", true);
        FAREWELL = b.comment("4.90.0: when Goku is beaten he is not killed: he is knocked down, says goodbye (\"I'll see you next time, when I'm stronger!\"), flashes away and drops his loot. The fight counts as won. False = he dies as before.")
                .define("farewell", true);
        EP_WEIGHT = b.comment("How strongly EP raises him: health x (1 + epWeight x (EP curve - 1)); damage takes half of it. 0 = EP changes nothing.")
                .defineInRange("epWeight", DEFAULT_EP_WEIGHT, 0.0, 5.0);
        EP_CAP = b.comment("The highest value of the EP curve that counts (the curve is 1 at 1,000 EP and +2.5 per decade of EP: 5 is reached at about 40,000 EP and 8 at about 630,000 EP). Lower = he stops growing earlier.")
                .defineInRange("epCap", DEFAULT_EP_CAP, 1.0, 12.0);
        PER_PLAYER_BONUS = b.comment("Extra health per player beyond the first near him (damage gets 40% of it).")
                .defineInRange("perPlayerBonus", DEFAULT_PER_PLAYER_BONUS, 0.0, 2.0);
        PLAYER_CAP = b.comment("How many players count toward that bonus (at most 10).")
                .defineInRange("playerCap", GokuBossMath.MAX_PLAYERS, 1, GokuBossMath.MAX_PLAYERS);
        MAX_HIT_PERCENT = b.comment("One hit of his takes at most this share of a player's maximum health (0.30 = 30%): he cannot one-shot through the mod's own balance law.")
                .defineInRange("maxHitPercent", DEFAULT_MAX_HIT, 0.05, 0.9);
        UI_DODGE_CAP = b.comment("The most chance his automatic Ultra Instinct dodge may ever have (it also has a cooldown and a limit per window).")
                .defineInRange("uiDodgeCap", DEFAULT_DODGE_CAP, 0.0, 0.6);
        ENRAGE_MINUTES = b.comment("The soft enrage timer: after this many minutes (shorter with high EP) he is in True Ultra Instinct whatever his health is.")
                .defineInRange("enrageMinutes", DEFAULT_ENRAGE_MINUTES, 3, 60);
        BLOCK_DAMAGE = b.comment("The Kamehameha and the Spirit Bomb may break blocks (a few, through the boss framework's block ledger, and only when the griefBlocks rule is on too). Off by default.")
                .define("blockDamage", false);
        GLOBAL_CAP = b.comment("How many Goku bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        b.pop();
        SPEC = b.build();
    }

    private GokuBossConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean blockDamage() { return get(BLOCK_DAMAGE); }

    public static GokuBossMath.Preset preset() { return GokuBossMath.Preset.parse(get(DIFFICULTY)); }

    public static double maxHitPercent() { return get(MAX_HIT_PERCENT); }

    public static double uiDodgeCap() { return get(UI_DODGE_CAP); }

    public static int globalCap() { return get(GLOBAL_CAP); }

    public static int enrageMinutes() { return get(ENRAGE_MINUTES); }

    /** All the scaling inputs at once. */
    public static GokuBossMath.Params params() {
        return new GokuBossMath.Params(preset(), get(HEALTH_MULTIPLIER), get(DAMAGE_MULTIPLIER), get(EP_SCALING), get(EP_WEIGHT), get(EP_CAP), get(PER_PLAYER_BONUS), get(PLAYER_CAP));
    }

    public static boolean epScaling() { return get(EP_SCALING); }

    public static boolean farewell() { return get(FAREWELL); }
}
