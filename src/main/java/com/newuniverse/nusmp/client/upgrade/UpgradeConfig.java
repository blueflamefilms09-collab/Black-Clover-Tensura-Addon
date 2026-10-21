package com.newuniverse.nusmp.client.upgrade;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the client switches of the visual upgrade (config/nusmp-visual-upgrade-god-client.toml (it was nusmp-visual-upgrade-client.toml in 1.102.0 to 1.107.0, a name the Genesis weapons also used), this machine only). {@code visual.upgrade = false} puts every upgraded weapon back on its old mesh, skin
 * and glow (the old files stay in the jar), switches off its extra passes, its ambient layers and its swing trail; the act signatures are drawn by the effect system and follow
 * {@code visual.signatures}. The other switches trim the cost: the density of the ambient layers (0 = none), the orbiting parts, the extra glow passes.
 * Reads never throw: before the file is loaded the defaults answer.
 */
public final class UpgradeConfig {
    private UpgradeConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue UPGRADE, ORBITS, PASSES, TRAILS, SIGNATURES;
    private static final ModConfigSpec.DoubleValue DENSITY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        UPGRADE = b.comment("The upgraded look of the God and Genesis weapons (new meshes, skins, passes, ambient layers, swing trails). false = every weapon looks as it did before 1.102.0.")
                .define("upgrade", true);
        ORBITS = b.comment("Orbiting parts (rings, shards, ravens ...) of the upgraded weapons turn. false = they are drawn still.").define("orbitParts", true);
        PASSES = b.comment("The extra glow passes of the upgraded weapons (flowing veins, halo shell). false = only the core glow.").define("extraPasses", true);
        TRAILS = b.comment("The swing trail particles of the upgraded weapons.").define("swingTrails", true);
        SIGNATURES = b.comment("The signature effect an upgraded weapon draws on each of its acts.").define("signatures", true);
        DENSITY = b.comment("Density of the ambient particle layers round a held upgraded weapon: 0 = none, 1 = as designed. The effect budget (clientParticlesPerTick) and the distance cut-off apply on top.")
                .defineInRange("ambientDensity", 1.0, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private static boolean bool(ModConfigSpec.BooleanValue v, boolean d) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return d;
        }
    }

    public static boolean enabled() { return bool(UPGRADE, true); }

    public static boolean orbits() { return bool(ORBITS, true); }

    public static boolean extraPasses() { return bool(PASSES, true); }

    public static boolean trails() { return bool(TRAILS, true); }

    public static boolean signatures() { return bool(SIGNATURES, true); }

    public static float density() {
        try {
            return DENSITY.get().floatValue();
        } catch (IllegalStateException notLoaded) {
            return 1f;
        }
    }
}
