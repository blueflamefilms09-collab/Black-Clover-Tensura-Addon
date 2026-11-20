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
    /** Lucifero v2 (docs/bosses/lucifero_v2.md): the player-shaped body with separate armour parts (true, the default) or the Blender body of wave 3 (false). Key {@code lucifero.newBody}. */
    public static final ModConfigSpec.BooleanValue LUCIFERO_NEW_BODY;
    /** Black Clover bosses on the redesigned player model (docs/bc_playermodels.md), key {@code bossLooks.bcPlayerModel}, CLIENT config like every look option here (it changes only how a boss is drawn). */
    public static final ModConfigSpec.BooleanValue BC_PLAYER_MODEL;
    /** Wave 2 (docs/bosses_bc_wave2.md), key {@code bossLooks.godOfOrderPlayerModel}: the God of Order on the redesigned player model instead of his Blender body (off by default: the Blender body stays). CLIENT config. */
    public static final ModConfigSpec.BooleanValue GOD_OF_ORDER_PLAYER_MODEL;
    /** The contract devils on their own player-model looks (docs/devil_looks.md), key {@code bossLooks.devilPlayerModel}, CLIENT config (appended after the Black Clover option). */
    public static final ModConfigSpec.BooleanValue DEVIL_PLAYER_MODEL;
    /** Goku and Sinbad (docs/boss_redesign_goku_sinbad.md), key {@code bossLooks.bossLookClassic}, CLIENT config (appended): true brings back their earlier geo-model look exactly. Default false. */
    public static final ModConfigSpec.BooleanValue BOSS_LOOK_CLASSIC;
    /** Kakashi Hatake on the kit's player model (docs/bosses/kakashi.md), key {@code bossLooks.kakashiPlayerModel}, CLIENT config (appended after the devil option). Off = his Blender body, the classic look. */
    public static final ModConfigSpec.BooleanValue KAKASHI_PLAYER_MODEL;
    /** The trial devil's own model (docs/devil_trial_model.md), key {@code bossLooks.devilTrialModel}, CLIENT config (appended after the Kakashi option). Off = the earlier vanilla-humanoid stand-in. */
    public static final ModConfigSpec.BooleanValue DEVIL_TRIAL_MODEL;
    /** Sinbad's Djinn Equip armour fit (docs/boss_redesign_goku_sinbad.md), key {@code bossLooks.bossArmorFit}, CLIENT config (appended last). True (default): the pieces his look already carries (crown, collar, belt, cuffs, shoulders, sandals) dissolve into the look once forged in. False = every piece stays over the look, as before. */
    public static final ModConfigSpec.BooleanValue BOSS_ARMOR_FIT;
    /** Canon looks (docs/devil_trial_model.md, 'Canon looks'), key {@code bossLooks.devilCanonLooks}, CLIENT config (appended): each contract devil's own model from its anime design, in the trial fight and in the ritual. Off = the 4.64.0 generic horned brute. */
    public static final ModConfigSpec.BooleanValue DEVIL_CANON_LOOKS;
    /** Canon looks, key {@code bossLooks.devilRitualFigure}, CLIENT config (appended): the summoned devil of the ritual is drawn as the full-size canon model (true, the default). Off = the earlier figure (the player-model look or the Blender mesh). */
    public static final ModConfigSpec.BooleanValue DEVIL_RITUAL_FIGURE;

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
        b.push("lucifero");
        LUCIFERO_NEW_BODY = b.comment("Lucifero v2: draw him with the player-shaped body and his separate armour parts (three bodies: the king, wings spread, the Gates). Off = the wave 3 body (bat wings on a thin frame).")
                .define("newBody", true);
        b.pop();
        b.pop();
        b.push("bossLooks");
        BC_PLAYER_MODEL = b.comment("Black Clover bosses (Lucius, the Dark Triad, Zagred, Lucifero and the clones of Lucius) on the redesigned player model: anime face, layered cube hair, crowns, horns, bat wings, "
                + "bracers, mantles, halo and a faint shimmer (docs/bc_playermodels.md). Wave 2 adds the Dark Disciples, the Devil Avatar, Heath Grice, Lotus Whomalt, Mars, Revchi Salik, Vetto and Morris "
                + "(docs/bosses_bc_wave2.md). Off = exactly the previous look, Lucifero included (his Blender body returns).").define("bcPlayerModel", true);
        GOD_OF_ORDER_PLAYER_MODEL = b.comment("Draw the God of Order on the redesigned player model (hood, white robe, gold stole, closed serene face; docs/bosses_bc_wave2.md) instead of his Blender body. "
                + "Off (default) = the Blender body (or the kit's look when godOfOrderBlenderModel is off).").define("godOfOrderPlayerModel", false);
        DEVIL_PLAYER_MODEL = b.comment("The devils of the Devil Contract (the figure in the ritual, and the form a devil's contract gives its signer) on the anime player-model looks: painted face, layered cube hair, "
                + "horns, wings, tails, bracers and glowing gems in each devil's colours (docs/devil_looks.md). Off = exactly the previous figures (the Blender meshes, or the generic horns and wings).").define("devilPlayerModel", true);
        BOSS_LOOK_CLASSIC = b.comment("Draw the Goku and Sinbad bosses with their earlier look (the shared geo body in a skin) instead of the redesigned player model with its form hair, aura and the Djinn Equip armour "
                + "(docs/boss_redesign_goku_sinbad.md). Off (default) = the redesigned look.").define("bossLookClassic", false);
        KAKASHI_PLAYER_MODEL = b.comment("Kakashi Hatake on the same player-model kit as the Black Clover bosses: painted anime face with the Sharingan, silver cube hair, hitai-ate, mask, flak jacket, forearm guards, "
                + "and the glowing Perfect Susanoo as an extras model (docs/bosses/kakashi.md). Off = the classic look, his Blender body.").define("kakashiPlayerModel", true);
        DEVIL_TRIAL_MODEL = b.comment("The devil that fights you in the trial of a Devil Contract (docs/devil_trial_model.md) with its own model: a horned brute with wings, a spade tail and clips for idle, walking and "
                + "attacking, painted in the devil's colours, with glowing eyes, brands, gems and horn tips. Off = the earlier stand-in (the vanilla humanoid with the zombie skin).").define("devilTrialModel", true);
        BOSS_ARMOR_FIT = b.comment("Sinbad's Djinn Equip: the armour pieces his new look already carries (crown, collar and chains, belt, cuffs, shoulders, sandals) are forged in and then dissolve into the look, so they "
                + "no longer clip through it; wings, tail, halo, gauntlets, mantle and the rest stay (docs/boss_redesign_goku_sinbad.md). Off = every piece of the player's set stays drawn over the look, as before.").define("bossArmorFit", true);
        DEVIL_CANON_LOOKS = b.comment("Each devil of the Devil Contract drawn as itself in the trial fight and in the summoning (docs/devil_trial_model.md, 'Canon looks'): its own model, painted sheet and glow from its "
                + "anime design (Liebe, Lucifero, the Fox Devil, Sebastian, the God Hand ...). Off = the generic horned brute every devil shared in 4.64.0.").define("devilCanonLooks", true);
        DEVIL_RITUAL_FIGURE = b.comment("The devil that rises from the summoning circle drawn as the full-size devil model (the same model and scale as in the trial; the old figure drew a child-sized body with the wings floating above it). "
                + "Off = the earlier figure (the player-model look, or the Blender mesh).").define("devilRitualFigure", true);
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
            if (mesh.startsWith("lucifero")) return !bcPlayerModel() && LUCIFERO_BLENDER.get();      // the redesigned player-model Lucifero wins while bossLooks.bcPlayerModel is on
            if (mesh.equals("kakashi")) return !kakashiPlayerModel();                                 // the kit's Kakashi is the default; the Blender body is the classic look behind the switch
            if (mesh.startsWith("god_of_order")) return GOD_OF_ORDER_BLENDER.get() && !godOfOrderPlayerModel();      // the selectable player-model look wins while it is on
            return true;
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the Black Clover bosses are drawn with the redesigned player-model looks (the config value; true while it is not loaded). */
    public static boolean bcPlayerModel() {
        try {
            return BC_PLAYER_MODEL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when Kakashi is drawn on the kit's player model (the config value; true while it is not loaded). */
    public static boolean kakashiPlayerModel() {
        try {
            return KAKASHI_PLAYER_MODEL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the trial devil is drawn with its own model (the config value; true while it is not loaded). */
    public static boolean devilTrialModel() {
        try {
            return DEVIL_TRIAL_MODEL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when each devil is drawn with its own canon model (the config value; true while it is not loaded). */
    public static boolean devilCanonLooks() {
        try {
            return DEVIL_CANON_LOOKS.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the ritual's devil is drawn as the full-size canon model (the config value; true while it is not loaded). */
    public static boolean devilRitualFigure() {
        try {
            return DEVIL_RITUAL_FIGURE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the God of Order is drawn with the redesigned player-model look (the config value; false while it is not loaded: the Blender body stays the default). */
    public static boolean godOfOrderPlayerModel() {
        try {
            return GOD_OF_ORDER_PLAYER_MODEL.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /** True when the contract devils are drawn with their player-model looks (the config value; true while it is not loaded). */
    public static boolean devilPlayerModel() {
        try {
            return DEVIL_PLAYER_MODEL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the boss Djinn Equip hides the pieces the look replaces (the config value; true while it is not loaded). */
    public static boolean bossArmorFit() {
        try {
            return BOSS_ARMOR_FIT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when Goku and Sinbad are drawn with their earlier look (the config value; false while it is not loaded: the redesigned look is the default). */
    public static boolean bossLookClassic() {
        try {
            return BOSS_LOOK_CLASSIC.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /** Lucifero v2: true when the player-shaped body is switched on (true while the config is not loaded). */
    public static boolean luciferoNewBody() {
        try {
            return LUCIFERO_NEW_BODY.get();
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
