package com.newuniverse.nusmp.client.vupgrade.godb;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the visual upgrade of the God melee weapons (release 1.102.0, slice god_melee_b): config/nusmp-visualupgrade-god_melee_b-client.toml.
 * {@code visual.upgrade} (default true) draws the nine upgraded weapons (the new meshes, skins, passes, orbiting pieces, swing trail and ambient particles); off draws every one
 * of them exactly as before 1.102.0 (the old meshes are still shipped). {@code visual.guiModel} (default off, the sprite stays the inventory icon) shows the new 3D model in the
 * inventory, tilted. {@code visual.particles} switches only the ambient particle layers. Registered from {@link GodBUpgrade#init}.
 */
public final class UpgradeConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue UPGRADE, GUI_MODEL, PARTICLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        UPGRADE = b.comment("Draw the upgraded God melee weapons (Aurliel's Seraph Halberd, Chronarch's Pendulum Sabre, Deepwatch Harpoon, Oriel's Starfall Staff, Reflected King's Mirrorblade, Rimeheart Greatblade, "
                        + "Sanguine Warden's Cleaver, Valkarin's Stormlance, Paradox, and since 1.108.0 the 36 dark Djinn weapons): new meshes, energy veins, halo shell, orbiting pieces, swing trail and particles. Off = the models of before 1.102.0.")
                .define("upgrade", true);
        GUI_MODEL = b.comment("Show the upgraded 3D model, tilted, in the inventory slot instead of the flat sprite (the sprite stays the default icon).")
                .define("guiModel", false);
        PARTICLES = b.comment("The ambient particle layers of the upgraded weapons (motes, a halo ring, sparks along the blade, a pool of light at the feet, a flare at the hand, a light shaft, rare flares). "
                        + "They follow the effect quality and the client particle budget of nusmp-performance-client.toml; off = none (the models and their glow stay).")
                .define("particles", true);
        b.pop();
        SPEC = b.build();
    }

    private UpgradeConfig() {}

    /** {@code visual.upgrade}; true while the file is not loaded (the safe default is the new look). */
    public static boolean upgrade() {
        try { return UPGRADE.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean guiModel() {
        try { return GUI_MODEL.get(); } catch (IllegalStateException notLoaded) { return false; }
    }

    public static boolean particles() {
        try { return PARTICLES.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
