package com.newuniverse.nusmp.client.aura;

/**
 * Why: the one client switch of the remade Beast Will spirits (docs/beastwill_remake.md): {@code beastwill.classicSpirits} brings back the animal-silhouette spirits of 3.57.0 / 3.59.0 instead of the
 * remade ones (a running figure with a burning piece in Acquire, an armoured beast-humanoid in Integrate). It only changes what is drawn; {@code BeastWillSpiritClientConfig} writes it here, so the
 * painter never touches a config class. Read on the render thread only; false until the config is loaded.
 */
public final class BeastWillSpiritOptions {
    private BeastWillSpiritOptions() {}

    private static volatile boolean classic;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }

    private static volatile boolean flat;

    /** 3.61.0: true = the flat painted sprites of 3.60.0 (client setting {@code beastwill.flatSpirits}) instead of the 3D cube forms. */
    public static boolean flat() { return flat; }

    public static void setFlat(boolean on) { flat = on; }

    private static volatile boolean classicForms;

    /** 3.76.0: true = the 3D forms as they were before the look pass (client setting {@code beastwill.classicForms}; the files {@code bwf_<stem>_v1...}). */
    public static boolean classicForms() { return classicForms; }

    public static void setClassicForms(boolean on) { classicForms = on; }
}
