package com.newuniverse.nusmp.stones;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether the Infinity Stone slot exists, what EP it asks for, how long a holder may be away and what happens to a stone at death change gameplay, so they are SERVER settings
 * ({@code nusmp-stones-server.toml}, registered by {@link StoneBoot}). {@code enabled = false} restores the Wave 30 stones exactly as they were (carried, no gate, no slot acts, no passives, no
 * container rule, no away rule).
 */
public final class StoneConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, PASSIVES, CONTAINER_RULE, WHISPERS, EP_SCALE;
    public static final ModConfigSpec.DoubleValue EP_GATE;
    public static final ModConfigSpec.IntValue AWAY_DAYS;
    public static final ModConfigSpec.ConfigValue<String> ON_DEATH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("infinityStones");
        ENABLED = b.comment("The Infinity Stone slot: a stone worn in it (needs base max EP at the gate) gains two slot abilities and passives. false = the Wave 30 stones exactly as before.").define("enabled", true);
        EP_GATE = b.comment("Base maximum EP (not the Akashic grade's +20%) needed to wear a stone in the infinity_stone slot. Below it the stone's slot powers pause (the stone is never destroyed).").defineInRange("epGate", StoneRules.GATE_EP, 1.0e6, 1.0e18);
        PASSIVES = b.comment("The slot passives of the worn stone (speed, health, mental shield, ...). false = only the slot abilities.").define("passives", true);
        EP_SCALE = b.comment("Slot abilities and passives grow with 1 + log10(EP / gate), at most x2. false = always x1.").define("scaleWithEp", true);
        AWAY_DAYS = b.comment("A stone held by a player who has been offline this many real days is declared lost to the Order's ledger and returns to the world. 0 = never.").defineInRange("awayDays", 7, 0, 365);
        ON_DEATH = b.comment("What a stone worn in the slot does when its bearer dies: \"keep\" (it is given back at the respawn) or \"drop\" (it falls like the rest of the loot).").define("onDeath", "keep", o -> o instanceof String s && (s.equals("keep") || s.equals("drop")));
        CONTAINER_RULE = b.comment("Stones cannot be stored in chests, barrels or ender chests: they return to the player's pack when the container closes. false = the Wave 30 behaviour (they can).").define("noContainers", true);
        WHISPERS = b.comment("Rare whispers in the action bar from the worn stone.").define("whispers", true);
        b.pop();
        SPEC = b.build();
    }

    private StoneConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static double epGate() { return get(EP_GATE); }

    public static boolean passives() { return get(PASSIVES); }

    public static boolean epScale() { return get(EP_SCALE); }

    public static int awayDays() { return get(AWAY_DAYS); }

    public static boolean keepOnDeath() { return !"drop".equals(get(ON_DEATH)); }

    public static boolean containerRule() { return get(CONTAINER_RULE); }

    public static boolean whispers() { return get(WHISPERS); }
}
