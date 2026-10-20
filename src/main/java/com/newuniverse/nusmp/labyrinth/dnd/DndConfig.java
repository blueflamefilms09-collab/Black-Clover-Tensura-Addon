package com.newuniverse.nusmp.labyrinth.dnd;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the D&amp;D layer of the Labyrinth (traps, the monster economy, the dungeon's gloom, the Vital Strike),
 * {@code nusmp-labyrinth-dnd-server.toml}, section {@code [dnd]}. Its own file (not more keys in {@code LabyrinthConfig}) so three builders could add
 * their switches without editing one spec, and so a server owner who wants only the anime half (Spire, seals, vault) flips one file's keys.
 * Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is
 * still loading) and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class DndConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TRAPS, PUZZLES, DROPS, NO_XP, GLOOM, HP_BUFFER, VITAL, VITAL_SHOWY;
    public static final ModConfigSpec.DoubleValue PIT_DAMAGE, STONE_CHANCE, MEAT_CHANCE, HP_SCALE, VITAL_THRESHOLD, VITAL_EXECUTE;
    public static final ModConfigSpec.IntValue PUZZLE_RESET_SECONDS, GLOOM_LIGHT, GLOOM_SECONDS, VITAL_COOLDOWN_TICKS, VITAL_RELIC_GRADE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("dnd");
        TRAPS = b.comment("Hidden pitfalls and snuffer glyphs in the Labyrinth's trap rooms.").define("traps", true);
        PUZZLES = b.comment("Puzzle-locked doors (Sigil Plates and a Riddle Stone) in the Labyrinth's puzzle rooms.").define("puzzles", true);
        PIT_DAMAGE = b.comment("Damage of a pitfall on top of the fall.").defineInRange("pitDamage", 6.0, 0.0, 100.0);
        PUZZLE_RESET_SECONDS = b.comment("Seconds a puzzle waits after a wrong sequence before it can be tried again.").defineInRange("puzzleResetSeconds", 5, 1, 300);
        DROPS = b.comment("Monsters of the Labyrinth drop physical Magic Stones and monster meat (the dungeon economy).").define("drops", true);
        NO_XP = b.comment("Monsters of the Labyrinth drop no vanilla experience orbs (their value is in the stones and the meat).").define("noExperience", true);
        STONE_CHANCE = b.comment("Chance per death of the stone drop (scaled by the monster's toughness).").defineInRange("stoneChance", 0.85, 0.0, 1.0);
        MEAT_CHANCE = b.comment("Chance per death of a monster-meat drop.").defineInRange("meatChance", 0.45, 0.0, 1.0);
        GLOOM = b.comment("The dungeon's gloom: standing in the dark without a light bites harder in the Labyrinth (Darkness and Weakness). The food bar is never touched (the old Labyrinth hunger multiplier was removed in 1.99.0).").define("gloom", true);
        GLOOM_LIGHT = b.comment("Block light at or below this counts as dark.").defineInRange("gloomLight", 4, 0, 15);
        GLOOM_SECONDS = b.comment("Seconds in the dark (with no light in hand) before the gloom sets in.").defineInRange("gloomSeconds", 20, 1, 600);
        HP_BUFFER = b.comment("Labyrinth monsters get a D&D-style health buffer (their maximum health is multiplied).").define("hpBuffer", true);
        HP_SCALE = b.comment("The multiplier of that buffer.").defineInRange("hpScale", 2.5, 1.0, 20.0);
        VITAL = b.comment("The Vital Strike: a heavy blow or a high-grade relic bypasses the buffer with a hyper-lethal execution.").define("vitalStrike", true);
        VITAL_SHOWY = b.comment("Spectacle (sound and the Rukh burst) on a Vital Strike.").define("vitalStrikeEffects", true);
        VITAL_THRESHOLD = b.comment("A single hit of at least this share of the monster's maximum health is a Vital Strike.").defineInRange("vitalThreshold", 0.18, 0.01, 1.0);
        VITAL_EXECUTE = b.comment("A Vital Strike removes at least this share of the monster's maximum health.").defineInRange("vitalExecute", 0.35, 0.05, 1.0);
        VITAL_RELIC_GRADE = b.comment("A blow with a relic of this grade or higher (0 NORMAL .. 6 GENESIS) charges the Vital meter.").defineInRange("vitalRelicGrade", 2, 0, 6);
        VITAL_COOLDOWN_TICKS = b.comment("Ticks between two Vital Strikes by the same player.").defineInRange("vitalCooldownTicks", 100, 0, 6000);
        b.pop();
        SPEC = b.build();
    }

    private DndConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
