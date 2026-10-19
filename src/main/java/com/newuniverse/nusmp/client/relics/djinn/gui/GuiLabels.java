package com.newuniverse.nusmp.client.relics.djinn.gui;

import java.util.function.BiFunction;

/**
 * The words on the panel. The game fills them from the lang files (keys in {@link #KEYS}, English fallbacks in {@link #FALLBACK}); the preview and
 * the tests use {@link #DEFAULT}. Built once per language reload, never per frame.
 */
public final class GuiLabels {
    public static final String[] KEYS = {
            "nusmp.djinn_gui.phase.0", "nusmp.djinn_gui.phase.1", "nusmp.djinn_gui.phase.2", "nusmp.djinn_gui.phase.3", "nusmp.djinn_gui.phase.4",
            "nusmp.djinn_gui.trust", "nusmp.djinn_gui.maso", "nusmp.djinn_gui.extreme", "nusmp.djinn_gui.dark"};
    public static final String[] FALLBACK = {"Sealed", "Lesser Summon", "Djinn Weapon", "Full Equip", "Extreme Magic", "Trust", "Maso", "Ext", "Dark"};

    public static final GuiLabels DEFAULT = new GuiLabels(FALLBACK);

    public final String[] phase = new String[5];
    public final String trust, maso, extreme, dark;

    public GuiLabels(String[] v) {
        for (int i = 0; i < 5; i++) phase[i] = v[i];
        trust = v[5];
        maso = v[6];
        extreme = v[7];
        dark = v[8];
    }

    /** Looks every word up; {@code lookup} gets (key, fallback) and returns the translated text or the fallback. */
    public static GuiLabels from(BiFunction<String, String, String> lookup) {
        String[] v = new String[KEYS.length];
        for (int i = 0; i < v.length; i++) v[i] = lookup.apply(KEYS[i], FALLBACK[i]);
        return new GuiLabels(v);
    }
}
