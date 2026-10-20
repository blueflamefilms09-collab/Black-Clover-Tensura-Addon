package com.newuniverse.nusmp.particle.fx;

/**
 * Why: the old Light and Dark Rukh were the mod's two moods of the same effect (a bright version and a shadowed version), and a great many call sites pick one by a flag. {@code FxTone}
 * keeps that choice (so the codemod can swap the type and keep the logic of the call site) but now it selects the theme's light or dark flavour: the same family of particles in the theme colour,
 * or the darker, shadowed set. Pure.
 */
public enum FxTone {
    LIGHT, DARK;

    public static FxTone of(boolean dark) {
        return dark ? DARK : LIGHT;
    }

    public boolean dark() {
        return this == DARK;
    }
}
