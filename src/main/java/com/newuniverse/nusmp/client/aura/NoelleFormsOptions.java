package com.newuniverse.nusmp.client.aura;

/**
 * Why: the one client switch of Noelle's remade armour forms (docs/magic/noelle_forms.md): {@code noelleForms.classicLook} brings the first Valkyrie Dress back (the Water armour set of
 * 1.93.0, {@link WaterAura}) instead of the remade one. It only changes what is drawn; the config class that reads the toml ({@code NoelleFormsClientConfig}) writes it here, so the painter and
 * the preview tool never touch a config class. Read on the render thread only; false until the config is loaded.
 */
public final class NoelleFormsOptions {
    private NoelleFormsOptions() {}

    private static volatile boolean classic;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }
}
