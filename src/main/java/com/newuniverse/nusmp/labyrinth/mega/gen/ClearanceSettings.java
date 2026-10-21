package com.newuniverse.nusmp.labyrinth.mega.gen;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the 7 x 7 minimum clearance of every transition ({@link Clearance7}) changes how every NEW piece of the Mega-Dungeon is finished, and the owner asked for the old
 * size to stay available, so it is a switch, {@code gen.clearance7}, ON by default. It is its own server config ({@code world/serverconfig/nusmp-labyrinth-mega-clearance-server.toml},
 * section {@code [gen]}), following the pattern of {@link CyclicSettings}: plain {@code define} only, every reader goes through {@link #get} (which answers the default
 * while the spec is not loaded yet). The switch is read each time a piece is placed, so a sector that is being generated or repaired while the switch is flipped is
 * finished with whatever the switch said when each of its pieces was placed; a sector that is already finished is never changed (turning the switch off does not
 * re-wall a cut doorway, turning it on does not widen an old one: regenerate the sector for that).
 */
public final class ClearanceSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLEARANCE7, LIGHTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("gen");
        CLEARANCE7 = b.comment("Tensura scale (docs/labyrinth/clearance7.md). true = every transition of a NEW piece of the Mega-Dungeon is at least 7 blocks wide and 7 blocks high: connected doorways are cut to 7 x 7, corridors and hallways one module wide are cut to a 7 x 7 interior, staircases get 7 blocks of head room, the opened Way Onward seal is 7 x 7. false = the old sizes (doorways 3 x 4, corridors 5 wide and 6 high). Read each time a piece is placed; finished sectors are not changed.")
                .define("clearance7", true);
        LIGHTS = b.comment("After a corridor or hallway was cut, put invisible light blocks in its ceiling row (every 4th block) to replace the hanging lanterns the cut removed, so the gloom clock still finds light. Only has an effect while gen.clearance7 is true.")
                .define("clearance7Light", true);
        b.pop();
        SPEC = b.build();
    }

    private ClearanceSettings() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Is the 7 x 7 clearance on? */
    public static boolean enabled() { return get(CLEARANCE7); }

    public static boolean lights() { return get(LIGHTS); }

    /** Registers the spec as {@code nusmp-labyrinth-mega-clearance-server.toml}; called once from {@code MegaGenRegistry.init} while the mod is being constructed. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-clearance-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The clearance config could not be registered ({}); the defaults apply (gen.clearance7 = true)", e.toString());
        }
    }
}
