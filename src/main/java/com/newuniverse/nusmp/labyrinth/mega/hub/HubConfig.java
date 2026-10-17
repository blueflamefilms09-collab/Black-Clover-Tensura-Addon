package com.newuniverse.nusmp.labyrinth.mega.hub;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of Floor 0, the Sanctuary Hub (1.55.0), {@code world/serverconfig/nusmp-labyrinth-mega-hub-server.toml}, section {@code [hub]}.
 * Its own file (the pattern of {@code DndConfig}, {@code MegaConfig} and {@code EcoConfig}) so no two builders edit one spec and a server owner can
 * switch the Hub off, or relax one of its rules, without touching the Mega-Dungeon's own file. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet (the Sanctuary's spawn filter can be asked by a chunk that generates
 * before the server config is read). {@code enabled = false} is the old behaviour of 1.54.0: the Gateway leads straight into the Mega-Dungeon and the
 * lead's routing never calls {@link HubTravel}.
 */
public final class HubConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, PASSIVE_ONLY, BUILD_ENABLED, PROTECT_PLAZA;
    public static final ModConfigSpec.IntValue GATE_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hub");
        ENABLED = b.comment("The Overworld Gateway leads to Floor 0, the Sanctuary Hub, and the Hub's gates lead on to the Holy and Abyssal roads. false = the 1.54.0 behaviour: the Gateway leads straight into the Mega-Dungeon.")
                .define("enabled", true);
        PASSIVE_ONLY = b.comment("Only passive creatures live in the Sanctuary: hostile spawns are cancelled and hostile mobs that slip in another way are removed every five seconds. Mobs an admin spawns with a command or an egg, tamed or owned mobs and named or persistent mobs are left alone.")
                .define("passiveOnly", true);
        BUILD_ENABLED = b.comment("Players may break and place blocks in the Sanctuary to build a base. false = the whole dimension is read-only for everyone who is not in creative mode.")
                .define("buildEnabled", true);
        PROTECT_PLAZA = b.comment("The paved square (with the way home) and a sphere of 8 blocks round each polarity gate cannot be broken, built on, blown up or set alight by anyone who is not in creative mode. false = everything but the gate blocks themselves can be changed.")
                .define("protectPlaza", true);
        GATE_COOLDOWN = b.comment("Ticks after arriving through, or being refused by, a Hub gate during which no gate takes the player again (long enough to walk out of the gate block they were set down in).")
                .defineInRange("gateCooldownTicks", 40, 10, 200);
        b.pop();
        SPEC = b.build();
    }

    private HubConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
