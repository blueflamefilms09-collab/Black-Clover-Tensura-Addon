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
    /** Boss rework wave 1: the Goliath on its own stone model (true, the default again: docs/mobs/remake_plan.md quick win 1) or as in 1.99.0 (false). */
    public static final ModConfigSpec.BooleanValue GOLIATH_STONE;
    /** Boss rework wave 2: the stone Goliath drawn with its Blender-made body (true, the default) or with the 3.96.0 stone geo model (false). */
    public static final ModConfigSpec.BooleanValue GOLIATH_BLENDER;
    /** Boss rework wave 2: the DanMachi Minotaur drawn with its Blender-made body (true, the default) or with the art kit's geo model (false). */
    public static final ModConfigSpec.BooleanValue MINOTAUR_BLENDER;
    /** Boss rework wave 3 (appended): Lucifero and the God of Order drawn with their Blender-made bodies (true, the default) or with the kit's player-model look (false). */
    public static final ModConfigSpec.BooleanValue LUCIFERO_BLENDER, GOD_OF_ORDER_BLENDER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("playermodel");
        DUNGEON = b.comment("Draw the dungeon's humanoids (Mob Foundry people, Living Armor, Floor Warden, Labyrinth Guardian, Goliath, soul-corpse puppets) on the player model with their HD skins. "
                + "Off = the art-kit renderers they had before (no restart needed).").define("dungeon", true);
        DETAIL_DISTANCE = b.comment("Blocks beyond which a character is drawn without its second skin layer and without hair.").defineInRange("detailDistance", PlayerLod.DEFAULT_DETAIL, 4, 128);
        FAR_DISTANCE = b.comment("Blocks beyond which the additive glow passes are dropped as well.").defineInRange("farDistance", PlayerLod.DEFAULT_FAR, 8, 256);
        GOLIATH_STONE = b.comment("Draw the Goliath with its own stone giant model (moss, cracks, its own clips). Off = the 1.99.0 look: the player model's body when 'dungeon' is on, else the art kit's.")
                .define("goliathStoneModel", true);
        GOLIATH_BLENDER = b.comment("With goliathStoneModel on: draw the Goliath with its Blender-made body (parts moved by procedural animation). Off = the 3.96.0 stone geo model.")
                .define("goliathBlenderModel", true);
        MINOTAUR_BLENDER = b.comment("Draw the DanMachi Minotaur with its Blender-made body (parts moved by procedural animation; Asterius keeps his model). Off = the art kit's model.")
                .define("minotaurBlenderModel", true);
        LUCIFERO_BLENDER = b.comment("Boss rework wave 3: draw Lucifero with his Blender-made body (bat wings, horns, hair; parts moved by procedural animation). Off = the kit's look.")
                .define("luciferoBlenderModel", true);
        GOD_OF_ORDER_BLENDER = b.comment("Boss rework wave 3: draw the God of Order with his Blender-made body (robes, halo, staff, ledger; a second halo and floating pages at the Gates). Off = the kit's look.")
                .define("godOfOrderBlenderModel", true);
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

    /** True when the Goliath is drawn with its own stone model (the config value; true while it is not loaded). */
    public static boolean goliathStone() {
        try {
            return GOLIATH_STONE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the stone Goliath is drawn with its Blender-made body (the config value; true while it is not loaded). */
    public static boolean goliathBlender() {
        try {
            return GOLIATH_BLENDER.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the Minotaur is drawn with its Blender-made body (the config value; true while it is not loaded). */
    public static boolean minotaurBlender() {
        try {
            return MINOTAUR_BLENDER.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** Boss rework wave 3: true when the Blender body of {@code mesh} ("lucifero", "god_of_order" and their forms) is switched on (true while the config is not loaded). */
    public static boolean blender(String mesh) {
        try {
            if (mesh.startsWith("lucifero")) return LUCIFERO_BLENDER.get();
            if (mesh.startsWith("god_of_order")) return GOD_OF_ORDER_BLENDER.get();
            return true;
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
