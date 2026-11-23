package com.newuniverse.nusmp.client.heroweapon;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: where the hero weapon's HUD sits and whether it shows at all is each player's taste, not the server's, so it is a CLIENT config,
 * {@code nusmp-heroweapon-client.toml}: a switch, two offsets from the bottom-left corner of the screen (in pixels, so the panel can be moved clear of the chat or
 * another mod's overlay), and a scale. Plain {@code define} / {@code defineInRange} values read through {@link #get}, which answers the default while the file is not
 * loaded yet.
 */
public final class HeroClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    /** 3.79.0: the 21 hero weapons (4 Legendary, 17 Vassal) got new models; true draws the models they had before (weapon_meshes/classic, textures/entity/weapon/classic). Looks only. */
    public static final ModConfigSpec.BooleanValue CLASSIC_LEGENDARY;
    /** 4.23.0: one 3D model per hero weapon form (weapon_meshes/<weapon>__<form>.txt). Looks only; the classic models never use them. */
    public static final ModConfigSpec.BooleanValue FORM_MODELS;

    /** 4.45.0 (docs/weapon_hold.md): a hero weapon is held so that no part of it passes through the forearm or the body, and a form is held by the hand of its item (look only). false = the placement of 4.44.x. */
    public static final ModConfigSpec.BooleanValue HOLD_FIT;
    /** 4.48.0: whips, flails and morning stars whip, trail and lag behind the swing (client/motion). false = every mesh is drawn rigid. Looks only. */
    public static final ModConfigSpec.BooleanValue WEAPON_MOTION;
    /** 4.48.0: true draws the vassal axe, book and carriage forms with the models they had before the v2 remodel (weapon_meshes/classic_vassal). Looks only. */
    public static final ModConfigSpec.BooleanValue CLASSIC_VASSAL_FORMS;
    /** Style wave 2 (docs/heroweapon/style/wave2.md): true draws the 14 weapons restyled in that pass (Seven Star base weapons, vassal staff / book forms, light metal shield) with the models they had before it (weapon_meshes/classic_style). Looks only. */
    public static final ModConfigSpec.BooleanValue CLASSIC_STYLE_WAVE2;
    public static final ModConfigSpec.BooleanValue CLASSIC_STYLE_WAVE3;
    /** 5.0.0: true (the default) does not draw the Vassal Carriage item in the hand while its carriage stands in the world (the carriage IS the weapon then); false keeps the item drawn. Looks only. */
    public static final ModConfigSpec.BooleanValue CARRIAGE_IN_WORLD;
    /** 5.0.0: true (the default) stacks the weapon, race and system panels in one column per side instead of each at its own place. false = every panel at the place it had before. Looks only. */
    public static final ModConfigSpec.BooleanValue UNIFIED_HUD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD = b.comment("Show the hero weapon's HUD (name, level, form, techniques with their cooldowns) at the bottom left.").define("show", true);
        HUD_X = b.comment("Pixels from the left edge of the screen.").defineInRange("x", 6, 0, 4000);
        HUD_Y = b.comment("Pixels from the bottom edge of the screen to the bottom of the panel.").defineInRange("y", 6, 0, 4000);
        HUD_SCALE = b.comment("Size of the panel (1.0 is the normal size).").defineInRange("scale", 1.0, 0.5, 3.0);
        b.pop();
        UNIFIED_HUD = b.comment("true = the hero weapon, Lord weapon, Greed, Beast Will, Saiyan, Shishou, Devil contract and relic key panels are stacked in one column on the left and one on the right (no overlapping boxes). false = each panel at its own old place. A panel moved with GUI Move keeps the place you gave it.")
                .define("unifiedHud", true);
        b.push("models");
        CLASSIC_LEGENDARY = b.comment("true = draw the hero weapons (the 4 Legendary with their curse looks, and the 17 Vassal Weapons) with the models they had before 3.79.0. Only the look changes.")
                .define("classicHeroWeaponModels", false);
        FORM_MODELS = b.comment("true = a hero weapon in a form that has a model of its own (Legendary, Seven Star, Vassal) is drawn with it. false = always the base (or curse) look. Looks only; ignored while classicHeroWeaponModels is on.")
                .define("formModels", true);
        HOLD_FIT = b.comment("true = a hero weapon (Legendary, Seven Star, Vassal, every form) is held by the grip of the hand: a form uses its item's hand placement, and a part that would pass through the forearm or the body is moved clear. false = the placement of version 4.44.x. Looks only.")
                .define("holdFit", true);
        WEAPON_MOTION = b.comment("true = a whip, flail or morning star swings: its chain and lash bend and lag behind the swing and sway while held (only weapons near you, a few per frame). false = rigid models. Looks only.")
                .define("weaponMotion", true);
        b.pop();
        b.push("vassalForms");
        CLASSIC_VASSAL_FORMS = b.comment("true = draw the Vassal axe, book and carriage forms with the models they had before the 4.48.0 remodel. Only the look changes.")
                .define("classic", false);
        CLASSIC_STYLE_WAVE2 = b.comment("true = draw the weapons restyled in style wave 2 (Seven Star hammer, axe, staff, claw, gauntlet, whip, projectile; Vassal staff and book with their forms; light metal shield) with the models they had before. Only the look changes.")
                .define("classicStyleWave2", false);
        CLASSIC_STYLE_WAVE3 = b.comment("true = draw the weapons restyled in style wave 3 (Legendary spear and its curse look, Portal spear, Dragon Minute Hand; Siderite and White Tiger katana; War carriage and Queen's chariot; Yata mirror; Phoenix sword) with the models they had before. Only the look changes.")
                .define("classicStyleWave3", false);
        CARRIAGE_IN_WORLD = b.comment("true = while the Vassal Carriage stands in the world its item is not drawn in the wielder's hand (sneak + right click on the carriage picks it up). false = the item stays drawn. Only the look changes.")
                .define("carriageInWorld", true);
        b.pop();
        SPEC = b.build();
    }

    private HeroClientConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean classicModels() { return get(CLASSIC_LEGENDARY); }

    public static boolean formModels() { return get(FORM_MODELS); }

    public static boolean holdFit() { return get(HOLD_FIT); }
    public static boolean weaponMotion() { return get(WEAPON_MOTION); }

    public static boolean unifiedHud() { return get(UNIFIED_HUD); }

    public static boolean carriageInWorld() { return get(CARRIAGE_IN_WORLD); }

    public static boolean classicVassalForms() { return get(CLASSIC_VASSAL_FORMS); }

    public static boolean classicStyleWave2() { return get(CLASSIC_STYLE_WAVE2); }

    public static boolean classicStyleWave3() { return get(CLASSIC_STYLE_WAVE3); }

    public static boolean hud() { return get(HUD); }

    public static int x() { return get(HUD_X); }

    public static int y() { return get(HUD_Y); }

    public static float scale() { return get(HUD_SCALE).floatValue(); }
}
