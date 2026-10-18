package com.newuniverse.nusmp.elite.records;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: a server owner must be able to switch the whole records system off, silence the toasts, switch the title bonuses or the synergies off, or scale every bonus without a rebuild. Own file
 * {@code nusmp-elite-records-server.toml}, section {@code [eliteRecords]}, so the other elite subsystems never share a spec. A SERVER config on purpose (the values decide server-side rules). Every
 * reader goes through the static methods, which answer the default while the spec is not loaded yet (the main menu, a unit test, an early event), because a config value read too early throws.
 */
public final class RecordsConfig {
    private RecordsConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, TOASTS, ANNOUNCE_RARE, TITLE_BONUSES, SYNERGIES, TRACK_STRUCTURES;
    public static final ModConfigSpec.DoubleValue BONUS_SCALE;
    public static final ModConfigSpec.IntValue BEAT_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("eliteRecords");
        ENABLED = b.comment("Master switch of the Records, Achievements and Titles system. When false nothing is tracked, unlocked or applied (saved data is kept) and the hooks answer their defaults.")
                .define("enabled", true);
        TOASTS = b.comment("Show a toast on the player's screen when a record or title is unlocked.").define("toasts", true);
        ANNOUNCE_RARE = b.comment("Tell everyone in chat when a player unlocks a LEGENDARY or MYTHIC record.").define("announceRare", true);
        TITLE_BONUSES = b.comment("Apply the active title's attribute modifiers, effects and custom stats. When false titles are cosmetic.").define("titleBonuses", true);
        SYNERGIES = b.comment("Apply the bonuses of synergies (sets of HELD titles). Needs titleBonuses.").define("synergies", true);
        BONUS_SCALE = b.comment("Multiplier on every attribute amount and custom stat of titles and synergies (effects are unchanged). 1.0 = as written in the data files; 0.5 = half.")
                .defineInRange("bonusScale", 1.0, 0.0, 5.0);
        BEAT_TICKS = b.comment("Ticks between two checks of one player (unlock conditions, title bonuses). Lower is snappier, higher is cheaper.").defineInRange("beatTicks", 40, 10, 400);
        TRACK_STRUCTURES = b.comment("Detect the structure the player stands in (for Structure Discovery records). Costs one structure lookup per player every 4 beats.").define("trackStructures", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { try { return ENABLED.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean toasts() { try { return TOASTS.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean announceRare() { try { return ANNOUNCE_RARE.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean titleBonuses() { try { return TITLE_BONUSES.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean synergies() { try { return SYNERGIES.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static boolean trackStructures() { try { return TRACK_STRUCTURES.get(); } catch (RuntimeException notLoaded) { return true; } }

    public static double bonusScale() { try { return BONUS_SCALE.get(); } catch (RuntimeException notLoaded) { return 1.0; } }

    public static int beatTicks() { try { return BEAT_TICKS.get(); } catch (RuntimeException notLoaded) { return 40; } }
}
