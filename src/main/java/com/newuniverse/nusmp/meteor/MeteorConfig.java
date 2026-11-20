package com.newuniverse.nusmp.meteor;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the shared meteor: {@code serverconfig/nusmp-meteor-server.toml} (docs/meteor_fall.md). These change the game (what falls and when it lands), so they are SERVER settings.
 * {@code realMeteors = false} restores every ability that was wired to the meteor to exactly what it did before. Reads fall back to the defaults before the config is loaded.
 */
public final class MeteorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REAL_METEORS;
    public static final ModConfigSpec.IntValue MAX_PER_CASTER, MAX_GLOBAL, MAX_FALL_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("meteor");
        REAL_METEORS = b.comment("true (default): the meteor abilities (Meteor Sword, Meteor of Bones, Meteor Swarm, the Visionary's meteor, Black Comet, Merlin's Starfall) drop a real falling meteor that crashes into the target spot",
                "and does the old damage when it lands. false = every one of them does exactly what it did before the meteor existed (no entity). The caps below still apply when it is on.").define("realMeteors", true);
        MAX_PER_CASTER = b.comment("The most meteors one caster can have in the air at once (a cast that would pass it lands its extra meteors the old way, at once).")
                .defineInRange("maxPerCaster", MeteorRules.DEFAULT_PER_CASTER, 1, MeteorRules.ABS_MAX_PER_CASTER);
        MAX_GLOBAL = b.comment("The most meteors in the air in the whole server at once.").defineInRange("maxGlobal", MeteorRules.DEFAULT_GLOBAL, 1, MeteorRules.ABS_MAX_GLOBAL);
        MAX_FALL_TICKS = b.comment("The longest a meteor may fall, in ticks (20 = one second); a caller's longer fall is cut to this.").defineInRange("maxFallTicks", MeteorRules.MAX_FALL, MeteorRules.MIN_FALL, MeteorRules.MAX_FALL);
        b.pop();
        SPEC = b.build();
    }

    private MeteorConfig() {}

    public static boolean realMeteors() {
        try { return REAL_METEORS.get(); } catch (IllegalStateException unloaded) { return true; }
    }

    public static int maxPerCaster() {
        try { return MAX_PER_CASTER.get(); } catch (IllegalStateException unloaded) { return MeteorRules.DEFAULT_PER_CASTER; }
    }

    public static int maxGlobal() {
        try { return MAX_GLOBAL.get(); } catch (IllegalStateException unloaded) { return MeteorRules.DEFAULT_GLOBAL; }
    }

    public static int maxFallTicks() {
        try { return MAX_FALL_TICKS.get(); } catch (IllegalStateException unloaded) { return MeteorRules.MAX_FALL; }
    }
}
