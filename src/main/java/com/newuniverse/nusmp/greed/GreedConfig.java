package com.newuniverse.nusmp.greed;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Black Sword Greed exists at all, how rare its bearer is, how fast hunger runs, how strong the secret techniques are and whether the dormant sword is released after 14 days.
 * These change gameplay, so they are SERVER settings: {@code nusmp-greed-server.toml} (registered by {@link GreedBoot}). The look of the starvation vignette is a client matter and has no setting of its own (it follows the sword).
 */
public final class GreedConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, CROWNED, INACTIVITY_RELEASE, STARVATION, STEAL_SKILLS;
    public static final ModConfigSpec.DoubleValue CROWNED_CHANCE;
    public static final ModConfigSpec.IntValue HUNGER_PERCENT, DAMAGE_PERCENT, ABSORB_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("greed");
        ENABLED = b.comment("The Black Sword Greed (Berserk of Gluttony). false = the item is inert, nobody is bound, nothing is cast. The world's record is kept.").define("greedEnabled", true);
        CROWNED = b.comment("A very rare Crowned monster (high EP or boss tier) may carry the dormant sword while it has not entered the world; it drops it when slain. false = only /give by an operator brings it in.").define("crownedBearer", true);
        CROWNED_CHANCE = b.comment("Chance that a boss-tier spawn is Crowned while the sword is not in the world (0.05 = 5%).").defineInRange("crownedChance", 0.05, 0.0, 1.0);
        INACTIVITY_RELEASE = b.comment("A wielder who has not been online for 14 days loses the sword (it leaves the world and may enter again). false = it waits for ever.").define("inactivityRelease", true);
        STARVATION = b.comment("Starvation: below 20% satiation the wielder is starving, at 0% the sword abandons a host that cannot pay. false = satiation never falls.").define("starvation", true);
        STEAL_SKILLS = b.comment("A kill may steal a skill into the Gluttony Skill Vault (always from a player, 35% from a mob).").define("stealSkills", true);
        HUNGER_PERCENT = b.comment("How fast satiation falls, in percent (100 = as listed).").defineInRange("hungerPercent", 100, 0, 1000);
        DAMAGE_PERCENT = b.comment("Strength of the secret techniques and form attacks, in percent (100 = as listed).").defineInRange("damagePercent", 100, 10, 1000);
        ABSORB_PERCENT = b.comment("Part of a victim's EP the sword absorbs in percent of the listed 15% (100 = as listed).").defineInRange("absorbPercent", 100, 0, 1000);
        b.pop();
        SPEC = b.build();
    }

    private GreedConfig() {}

    public static boolean enabled() { return ENABLED.get(); }
    public static double damage() { return DAMAGE_PERCENT.get() / 100.0; }
    public static double hunger() { return HUNGER_PERCENT.get(); }
    public static double absorb() { return ABSORB_PERCENT.get() / 100.0; }
}
