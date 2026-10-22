package com.newuniverse.nusmp.labyrinth.mega.gen;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner asked to keep the dungeon's size and rebuild the insides of its rooms and hallways (docs/labyrinth/rooms_rebuilt.md), and the old designs stay in the code, so the
 * rebuilt rooms are a switch, {@code gen.roomsRebuilt}, ON by default. It is its own server config ({@code world/serverconfig/nusmp-labyrinth-mega-rooms-server.toml}, section
 * {@code [gen]}), following the pattern of {@link ClearanceSettings}: plain {@code define} only, every reader goes through {@link #get} (which answers the default while the spec is not
 * loaded yet). The switch is read ONCE per sector, when its pieces are built, and saved with the sector ({@link GenData.Stat#rooms}): a repair or a regeneration of a finished sector
 * builds the rooms it was built with, whatever the switch says now, so the switch never makes a sector look different from one day to the next. Server config: it decides what the
 * generator places.
 */
public final class RoomsSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ROOMS_REBUILT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("gen");
        ROOMS_REBUILT = b.comment("Rebuilt rooms, halls and hallways (docs/labyrinth/rooms_rebuilt.md). true = NEW pieces of the Mega-Dungeon are built from the rebuilt templates: the same size, doors and encounter markers as the shipped rooms, with new architecture inside (vaulted roofs, pilasters, door crowns, niches, floor patterns, lighting) in several variants chosen by seed. false = the shipped rooms, exactly as they were. Read once per sector when it is built and saved with it: sectors that exist keep the rooms they were built with.")
                .define("roomsRebuilt", true);
        b.pop();
        SPEC = b.build();
    }

    private RoomsSettings() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Should a sector built now use the rebuilt rooms? */
    public static boolean enabled() { return get(ROOMS_REBUILT); }

    /** Was sector {@code id} built with the rebuilt rooms (its saved statistics say so)? An older sector, or one with no statistics, answers false. */
    public static boolean sectorRebuilt(net.minecraft.server.MinecraftServer server, int id) {
        try {
            GenData.Stat st = server == null ? null : GenData.get(server).get(id);
            return st != null && st.rooms;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Registers the spec as {@code nusmp-labyrinth-mega-rooms-server.toml}; called once from {@code MegaGenRegistry.init} while the mod is being constructed. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-rooms-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The rebuilt rooms config could not be registered ({}); the defaults apply (gen.roomsRebuilt = true)", e.toString());
        }
    }
}
