package com.newuniverse.nusmp.labyrinth.mega.system;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner's rule for the floor report is "no spam", and what counts as spam is a taste, so the switches are config and not code:
 * {@code world/serverconfig/nusmp-labyrinth-mega-floorinfo-server.toml}, section {@code [floorinfo]}. Its own file (the pattern of {@code CyclicSettings}), registered
 * by the class itself from the registry's {@code init} through the mod container that is active while the mod is being constructed, so no line is added to the mod
 * class. Everything is a plain {@code define} / {@code defineInRange} and every reader answers its default while the spec is not loaded yet. The player's own switch
 * is on the CLIENT (the System client config has {@code floorCard}), because only the player can see whether the window is in the way.
 */
public final class FloorInfoConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, MECHANIC_UPDATES, CHAT_COPY;
    public static final ModConfigSpec.IntValue ARRIVE_COOLDOWN_SECONDS, UPDATE_COOLDOWN_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("floorinfo");
        ENABLED = b.comment("The System floor report: on every arrival the System window says which floor this is and which way it goes, the THEME of the floor (name, one line, road) "
                        + "and the floor MECHANICS in effect (what each does and how to beat it), with the depth's effect on the monsters. false = the report is never sent "
                        + "(the command /dungeon floor says the feature is off).")
                .define("enabled", true);
        ARRIVE_COOLDOWN_SECONDS = b.comment("Seconds before the report for the SAME floor may appear again (walking a staircase up and down does not repeat it). A different floor always shows its report.")
                .defineInRange("arriveCooldownSeconds", 30, 0, 3600);
        MECHANIC_UPDATES = b.comment("Tell the player, once, when a mechanic wakes, is overcome or ends while they stand on the floor (a pop-up line and a refreshed report).")
                .define("mechanicUpdates", true);
        UPDATE_COOLDOWN_SECONDS = b.comment("The shortest gap, in seconds, between two mid-floor updates for one player.")
                .defineInRange("updateCooldownSeconds", 10, 1, 600);
        CHAT_COPY = b.comment("Also write the report in the chat on every arrival (for players whose window is switched off). false = the window only; /dungeon floor always writes it to the chat.")
                .define("chatCopy", false);
        b.pop();
        SPEC = b.build();
    }

    private FloorInfoConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static FloorInfoRules.Gap gap() {
        return new FloorInfoRules.Gap(get(ARRIVE_COOLDOWN_SECONDS) * 20, get(UPDATE_COOLDOWN_SECONDS) * 20, FloorInfoRules.Gap.DEFAULT.requestGap());
    }

    /** Registers the spec; called once from {@link FloorInfoRegistry#init} while the mod is being constructed. Never throws: a failure leaves the defaults in force. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-floorinfo-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The floor report config could not be registered ({}); the defaults apply", e.toString());
        }
    }
}
