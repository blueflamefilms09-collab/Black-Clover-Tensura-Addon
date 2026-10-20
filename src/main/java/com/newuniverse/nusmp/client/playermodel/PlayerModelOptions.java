package com.newuniverse.nusmp.client.playermodel;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the player owner can switch the dungeon's humanoids back to the art kit's own models, and trade detail for frames, without a new jar. Client options, file
 * {@code config/nusmp-playermodel-client.toml}, section {@code playermodel}:
 * <ul>
 *   <li>{@code dungeon} (default true): the humanoids of the dungeon (the Mob Foundry's people, the Living Armor, the Floor Warden, the Labyrinth Guardian, the Goliath, the soul-corpse puppets)
 *       are drawn on the player model; off hands every one of them back to the renderer it had before phase 2. The flag is read every frame (a switch needs no restart);</li>
 *   <li>{@code detailDistance} (default 24 blocks): beyond it a character is drawn without its second skin layer and without hair ({@link PlayerLod});</li>
 *   <li>{@code farDistance} (default 64 blocks): beyond it the additive glow passes go too.</li>
 * </ul>
 * Registered once from {@link PlayerModelRegistry#init} (the mod's client constructor). Every read is guarded: a config that is not loaded yet (the first frames, a test) gives the defaults.
 */
public final class PlayerModelOptions {
    private PlayerModelOptions() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DUNGEON;
    public static final ModConfigSpec.IntValue DETAIL_DISTANCE, FAR_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("playermodel");
        DUNGEON = b.comment("Draw the dungeon's humanoids (Mob Foundry people, Living Armor, Floor Warden, Labyrinth Guardian, Goliath, soul-corpse puppets) on the player model with their HD skins. "
                + "Off = the art-kit renderers they had before (no restart needed).").define("dungeon", true);
        DETAIL_DISTANCE = b.comment("Blocks beyond which a character is drawn without its second skin layer and without hair.").defineInRange("detailDistance", PlayerLod.DEFAULT_DETAIL, 4, 128);
        FAR_DISTANCE = b.comment("Blocks beyond which the additive glow passes are dropped as well.").defineInRange("farDistance", PlayerLod.DEFAULT_FAR, 8, 256);
        b.pop();
        SPEC = b.build();
    }

    /** True when the dungeon's humanoids are drawn on the player model (the config value; true while it is not loaded). */
    public static boolean dungeon() {
        try {
            return DUNGEON.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static double detailSqr() { return PlayerLod.sqr(blocks(DETAIL_DISTANCE, PlayerLod.DEFAULT_DETAIL)); }

    public static double farSqr() { return PlayerLod.sqr(blocks(FAR_DISTANCE, PlayerLod.DEFAULT_FAR)); }

    private static int blocks(ModConfigSpec.IntValue v, int dflt) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return dflt;
        }
    }
}
