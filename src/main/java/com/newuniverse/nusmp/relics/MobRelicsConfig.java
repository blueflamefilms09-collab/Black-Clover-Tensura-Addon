package com.newuniverse.nusmp.relics;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the switches of {@link MobRelics} in one plain ModConfigSpec (only define / defineInRange), so the owner can turn the feature off or open
 * it to every mob without code. Registered as the SERVER file {@code nusmp-mobrelics-server.toml}.
 */
public final class MobRelicsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, NAMED_ONLY;
    public static final ModConfigSpec.IntValue MAX_CARRIED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relics_mobs");
        ENABLED = b.comment("Named mobs (a goblin you named, a tamed monster) can be given relics and attune to them.").define("enabled", true);
        NAMED_ONLY = b.comment("true: only mobs with a name or an owner can take relics; false: any mob you sneak-use a relic on.").define("namedOnly", true);
        MAX_CARRIED = b.comment("How many relics one mob can carry in all (worn and in its pack). Attunement still caps at 3 per bearer.").defineInRange("maxCarried", 4, 1, 8);
        b.pop();
        SPEC = b.build();
    }

    private MobRelicsConfig() {}

    public static boolean enabled() { try { return ENABLED.get(); } catch (IllegalStateException e) { return true; } }
    public static boolean namedOnly() { try { return NAMED_ONLY.get(); } catch (IllegalStateException e) { return true; } }
    public static int maxCarried() { try { return MAX_CARRIED.get(); } catch (IllegalStateException e) { return 4; } }
}
