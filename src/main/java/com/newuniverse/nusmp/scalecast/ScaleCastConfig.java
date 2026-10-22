package com.newuniverse.nusmp.scalecast;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Scale Magic now runs through a floating balance scale and a suppression aura (docs/magic/scale_casting.md). The aura changes gameplay (it takes a share of a foe's blows and spell
 * power), so its switches are a SERVER config: {@code serverconfig/nusmp-scale-casting-server.toml}. {@code magic.scaleCasting = false} is the old execution: no Scale, no aura, the pages
 * cast exactly as before. The numbers can only make the aura gentler than the hard caps of {@link ScaleCastRules} (35 percent of a foe's damage, 30 percent of its spell power), never stronger.
 */
public final class ScaleCastConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue RADIUS, DAMAGE_CAP, MAGIC_CAP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        ENABLED = b.comment("Scale Magic through the floating balance scale and the suppression aura. false = the old execution: no Scale, no aura, the pages cast as before.")
                .define("scaleCasting", true);
        RADIUS = b.comment("Reach of the suppression aura in blocks (the Scale presses down foes inside it, at most 8 per caster).")
                .defineInRange("scaleAuraRadius", ScaleCastRules.RADIUS, ScaleCastRules.RADIUS_MIN, ScaleCastRules.RADIUS_MAX);
        DAMAGE_CAP = b.comment("The most of a foe's outgoing damage the aura takes (a bosses feels half of it). The hard limit is 0.35.")
                .defineInRange("scaleDamageCap", ScaleCastRules.DAMAGE_CAP, 0.0, ScaleCastRules.DAMAGE_CAP);
        MAGIC_CAP = b.comment("The most of a casting foe's spell power the aura takes. The hard limit is 0.30.")
                .defineInRange("scaleMagicCap", ScaleCastRules.MAGIC_CAP, 0.0, ScaleCastRules.MAGIC_CAP);
        b.pop();
        SPEC = b.build();
    }

    private ScaleCastConfig() {}

    /** {@code magic.scaleCasting}; true while the file is not loaded (the new execution is the default). */
    public static boolean enabled() {
        try { return ENABLED.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double radius() {
        try { return ScaleCastRules.clampRadius(RADIUS.get()); } catch (RuntimeException notLoaded) { return ScaleCastRules.RADIUS; }
    }

    public static double damageCap() {
        try { return ScaleCastRules.clampCap(DAMAGE_CAP.get()); } catch (RuntimeException notLoaded) { return ScaleCastRules.DAMAGE_CAP; }
    }

    public static double magicCap() {
        try { return Math.max(0.0, Math.min(ScaleCastRules.MAGIC_CAP, MAGIC_CAP.get())); } catch (RuntimeException notLoaded) { return ScaleCastRules.MAGIC_CAP; }
    }
}
