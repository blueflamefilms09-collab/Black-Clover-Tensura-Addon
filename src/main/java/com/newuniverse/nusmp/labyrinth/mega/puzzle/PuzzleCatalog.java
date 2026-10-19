package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.BeamMirrors;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.MemoryTiles;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.ColourMix;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.LightsOut;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.SlidingBlock;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.RuneCopy;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.RiddleDoor;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.RiddleOffering;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.Sundial;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.WaterLevel;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.TorchOrder;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.GravityMaze;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.ShadowCast;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.Melody;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.PipeConnect;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.KeyAndLock;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.EscortSpirit;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.SentryStealth;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.PendulumCrossing;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.PitJump;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.NumberLock;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.Constellation;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.MimicChests;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.ElementalMix;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.ChainReaction;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.PlateSequence;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.RotatingPillars;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.SimonLevers;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.rules.WeightScales;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why: the one list of the puzzles the dungeon knows, so the planner, the admin command, the self-tests and the JUnit sweep all iterate the same thing and a puzzle that is written but not
 * registered fails a test instead of silently never appearing. Each entry has the id (the lang keys, the weights file and the self-test name all use it), the family it belongs to (for the
 * docs and for variety: the planner does not put two of a family on one floor if it can help it) and a constructor. New puzzles are appended; ids never change (a world remembers a solved
 * puzzle by its id). Pure.
 */
public final class PuzzleCatalog {
    private PuzzleCatalog() {}

    public record Entry(String id, String family, Function<Params, PuzzleBase> make) {}

    private static final Map<String, Entry> ALL = new LinkedHashMap<>();

    private static void add(String id, String family, Function<Params, PuzzleBase> make) {
        ALL.put(id, new Entry(id, family, make));
    }

    static {
        add("plate_sequence", "floor", PlateSequence::new);
        add("rotating_pillars", "mechanism", RotatingPillars::new);
        add("beam_mirrors", "light", BeamMirrors::new);
        add("weight_scales", "mechanism", WeightScales::new);
        add("simon_levers", "memory", SimonLevers::new);
        add("memory_tiles", "memory", MemoryTiles::new);
        add("colour_mix", "colour", ColourMix::new);
        add("lights_out", "grid", LightsOut::new);
        add("sliding_block", "grid", SlidingBlock::new);
        add("rune_copy", "memory", RuneCopy::new);
        add("riddle_door", "riddle", RiddleDoor::new);
        add("riddle_offering", "riddle", RiddleOffering::new);
        add("sundial", "timing", Sundial::new);
        add("water_level", "mechanism", WaterLevel::new);
        add("torch_order", "logic", TorchOrder::new);
        add("gravity_maze", "mechanism", GravityMaze::new);
        add("shadow_cast", "light", ShadowCast::new);
        add("melody_notes", "sound", Melody::new);
        add("pipe_connect", "grid", PipeConnect::new);
        add("key_and_lock", "carry", KeyAndLock::new);
        add("escort_spirit", "floor", EscortSpirit::new);
        add("sentry_stealth", "timing", SentryStealth::new);
        add("pendulum_crossing", "timing", PendulumCrossing::new);
        add("pit_jump", "floor", PitJump::new);
        add("number_lock", "logic", NumberLock::new);
        add("constellation", "logic", Constellation::new);
        add("mimic_chests", "logic", MimicChests::new);
        add("elemental_mix", "carry", ElementalMix::new);
        add("chain_reaction", "mechanism", ChainReaction::new);
    }

    public static List<String> ids() { return new ArrayList<>(ALL.keySet()); }

    public static List<Entry> all() { return new ArrayList<>(ALL.values()); }

    public static boolean has(String id) { return ALL.containsKey(id); }

    public static String family(String id) {
        Entry e = ALL.get(id);
        return e == null ? "" : e.family();
    }

    /** A built puzzle, or null for an unknown id. */
    public static PuzzleRule create(String id, Params p) {
        Entry e = ALL.get(id);
        return e == null ? null : e.make().apply(p).build();
    }
}
