package com.newuniverse.nusmp.client.devil;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the CLIENT settings of the Devil Contract system ({@code config/nusmp-devil-client.toml}, 3.48.0). They only change what this player sees, never what happens: the Weg markings, horns and wings on
 * devils (all of them, including your own body), the HUD panel, and how strong the shade of a lost eye is on your own screen.
 */
public final class DevilClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue WEG, HUD, FORMS, CLASSIC, ICONS;
    private static final ModConfigSpec.DoubleValue EYE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("devil");
        WEG = b.comment("Draw the Weg markings, horns and wings on devils (yours and other players'). false = no body changes are drawn (the system itself still works).").define("wegLayer", true);
        HUD = b.comment("Draw the assimilation panel (level, magicule drain, union and cooldown timers). It can also be moved or hidden with the GUI Move settings.").define("hud", true);
        EYE = b.comment("How strong the shade of a lost eye is on your own screen: 0 = no shade and no narrower view, 1 = full.").defineInRange("eyeOverlayStrength", 1.0, 0.0, 1.0);
        FORMS = b.comment("3.97.0: a devil of another world has its own form: its Blender mesh worn on the body (head, arms, wings, tail, legs by stage) and the markings in its colour. false = the generic horns and wings exactly as before (docs/devil_forms.md).")
                .define("customForms", true);
        CLASSIC = b.comment("true = the look of before 4.40.0: the old Weg dots, the old horns and wings, and no creeping corruption on the screen (the server still counts it).").define("classicLook", false);
        ICONS = b.comment("4.40.0: the icons of Devil Magic and Devil Bond are composed from small emblems (your devil's and your grimoire's) and animate. false = the plain still icons.").define("animatedIcons", true);
        b.pop();
        SPEC = b.build();
    }

    private DevilClientConfig() {}

    public static boolean weg() { try { return WEG.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean classicLook() { try { return CLASSIC.get(); } catch (RuntimeException notLoaded) { return false; } }

    public static boolean animatedIcons() { try { return ICONS.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean customForms() { try { return FORMS.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean hud() { try { return HUD.get(); } catch (RuntimeException notLoaded) { return true; } }

    /** 0 to 1. */
    public static double eyeStrength() { try { return Math.max(0.0, Math.min(1.0, EYE.get())); } catch (RuntimeException notLoaded) { return 1.0; } }
}
