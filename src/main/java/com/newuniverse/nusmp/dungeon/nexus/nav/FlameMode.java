package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * The flame of the Ki-Resonance Lantern (docs/living_dungeon.md section 3): out when it has no fuel or is not held in the off-hand, amber
 * by default, <b>blue</b> when the player faces a wall of illusion stone, <b>red</b> when they face the nearest descent gate (blue wins if
 * both). {@link #property} is the value of the client item property {@code nusmp:flame} that picks the model. Pure.
 */
public enum FlameMode {
    OFF(0f, "_off"), AMBER(0.34f, ""), BLUE(0.67f, "_blue"), RED(1.0f, "_red");

    /** The value of the item property "flame": 0 off, 0.34 amber, 0.67 blue, 1.0 red (the model overrides switch at 0.17, 0.5 and 0.84). */
    public final float property;
    /** What this flame adds to the in-hand mesh id (the meshes are ki_resonance_lantern, _off, _blue and _red). */
    public final String meshSuffix;

    FlameMode(float property, String meshSuffix) {
        this.property = property;
        this.meshSuffix = meshSuffix;
    }

    /** Half angle of the cone the lantern looks along, in degrees. */
    public static final double CONE_DEG = 30.0;
    /** How far it looks for illusion stone (blocks) and for a descent gate (blocks), and how often it looks (ticks). */
    public static final int STONE_RANGE = 14, GATE_RANGE = 96, SCAN_EVERY = 5;

    public static final String MESH = "ki_resonance_lantern";

    public boolean lit() { return this != OFF; }

    /** The in-hand mesh id of this flame. */
    public String meshId() { return MESH + meshSuffix; }

    /** The mode with this ordinal (what the stack's flame component stores); anything unknown is out. */
    public static FlameMode of(int ordinal) {
        FlameMode[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : OFF;
    }

    /** Where the models and the in-hand mesh switch: halfway between the four property values (0, 0.34, 0.67, 1.0). */
    public static final float AMBER_FROM = 0.17f, BLUE_FROM = 0.5f, RED_FROM = 0.84f;

    /** The flame that a value of the item property "flame" stands for, read the way the model overrides read it (a NaN or negative value is out). */
    public static FlameMode ofProperty(float value) {
        if (!(value >= AMBER_FROM)) return OFF;
        if (value < BLUE_FROM) return AMBER;
        return value < RED_FROM ? BLUE : RED;
    }

    /** Which flame burns: out without fuel, else blue before an illusion wall, else red toward a gate, else amber. */
    public static FlameMode choose(boolean fuelled, boolean illusionAhead, boolean gateAhead) {
        if (!fuelled) return OFF;
        if (illusionAhead) return BLUE;
        return gateAhead ? RED : AMBER;
    }
}
