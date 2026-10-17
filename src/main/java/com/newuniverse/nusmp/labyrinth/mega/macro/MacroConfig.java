package com.newuniverse.nusmp.labyrinth.mega.macro;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Hub's terrain generator, {@code nusmp-labyrinth-mega-macro-server.toml}, section {@code [macro]}. Its own file (the
 * {@code DndConfig} pattern) so the landmarks can be tuned or switched off without touching any other part of the Mega-Dungeon. {@code enabled = false} is the
 * emergency switch: the Hub generates as plain rolling plains with its two gates on small platforms at their coordinates, which keeps the whole feature usable
 * if the Spire or the Chasm ever misbehave in somebody's modpack. The size of a Wave Function Collapse cell, the roughness of the two landmarks and the height of
 * the hills are the numbers a server owner may want a different feel from. Changing a number does NOT redraw chunks that exist already (a changed world
 * would have a seam): it applies to chunks generated afterwards, so set it before the Hub is first visited. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class MacroConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue CELL, WFC_TRIES;
    public static final ModConfigSpec.DoubleValue SPIRE_NOISE, CHASM_NOISE, HILL_AMPLITUDE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("macro");
        ENABLED = b.comment("true = the Hub is drawn with the Holy Spire (east) and the Abyssal Chasm (west). false = plain rolling plains with the two gates on small platforms at their coordinates (reached by an admin teleport only); use it if the landmarks ever cause trouble. Applies to chunks generated from now on.")
                .define("enabled", true);
        CELL = b.comment("Size in blocks of one Wave Function Collapse cell (a trial chamber of the Spire, a cavern of the Chasm). The tile art is drawn at 6 and stretched to this size. Applies to chunks generated from now on.")
                .defineInRange("cell", 6, 4, 8);
        WFC_TRIES = b.comment("How many times the Wave Function Collapse solver may restart from a new seed when it runs into a contradiction before it falls back to plain rock and masonry. The shipped tilesets never needed a restart in the unit tests.")
                .defineInRange("wfcTries", 12, 1, 64);
        SPIRE_NOISE = b.comment("How many blocks the 3D noise may move the outside of the Spire's shell (0 = a perfectly smooth cone).")
                .defineInRange("spireNoise", 2.0, 0.0, 4.0);
        CHASM_NOISE = b.comment("How many blocks the 3D noise may push ribs of rock out of the Chasm's wall into the pit (0 = a smooth bowl).")
                .defineInRange("chasmNoise", 3.0, 0.0, 6.0);
        HILL_AMPLITUDE = b.comment("The height in blocks of the rolling hills of the plains (they fade to nothing round the plaza, the two landmarks and the roads).")
                .defineInRange("hillAmplitude", 5.0, 0.0, 12.0);
        b.pop();
        SPEC = b.build();
    }

    private MacroConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The numbers the pure model is built from. */
    public static MacroParams params() {
        return new MacroParams(get(CELL), get(SPIRE_NOISE), get(CHASM_NOISE), get(HILL_AMPLITUDE), get(WFC_TRIES), get(ENABLED));
    }
}
