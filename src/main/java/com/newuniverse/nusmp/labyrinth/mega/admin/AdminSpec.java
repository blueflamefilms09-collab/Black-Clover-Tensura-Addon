package com.newuniverse.nusmp.labyrinth.mega.admin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the command tree is built from Brigadier nodes, and Brigadier cannot be run in a unit test (it needs the game's classes around it), so the SHAPE of the tree is written down
 * a second time here, as plain data, and checked twice. The unit test checks the data against itself and against the rules that matter (every admin literal is gated at
 * permission level 2, none of them has the name of a player subcommand that already exists, every usage has a help line, the ranges are the owner's), and the in-game self-test
 * {@code dungeon.admin} walks the REAL dispatcher with a level-1 and a level-2 source and compares what it finds, usage by usage, with this list: a literal that was added to the
 * tree and forgotten here, or the other way round, or one that lost its permission gate, fails the CI smoke run. The usage strings are the ones Brigadier prints for a tree
 * ({@code CommandDispatcher.getAllUsage}): literals as they are, arguments as {@code <name>}; a node that runs and also has an argument below it shows twice ({@code skip},
 * {@code skip <floors>}). Pure.
 */
public final class AdminSpec {
    private AdminSpec() {}

    /** The root every admin node hangs from: the SAME literal as the player's {@code /dungeon}, so Brigadier merges the two registrations into one tree. */
    public static final String ROOT = "dungeon";
    /** The permission level of every admin node (an operator). */
    public static final int PERMISSION = 2;

    /** The subcommands of {@code /dungeon} that existed before this tree and stay open to every player; the admin tree must not reuse or gate any of them. */
    public static final List<String> PLAYER_LITERALS = List.of("floor", "status", "release", "unstuck", "help");

    /** The top-level admin literals: each one carries the permission gate itself, so the player's subcommands beside them stay usable by everyone. */
    public static final List<String> ADMIN_LITERALS = List.of("tp", "skip", "boss", "rift", "skill", "reset", "debug", "admin");

    /** The most mastery points one {@code give_sp} adds. */
    public static final int SP_MAX = 100_000;

    /** The range of an integer argument, as the tree states it to Brigadier. */
    public record Range(String argument, int min, int max) {}

    /** The ranges of the integer arguments. {@code level} also refuses 0 at run time (the Hub has no boss). */
    public static final List<Range> RANGES = List.of(
            new Range("floor", -DungeonAdminMath.MAX_FLOOR, DungeonAdminMath.MAX_FLOOR),
            new Range("floors", -DungeonAdminMath.MAX_FLOOR, DungeonAdminMath.MAX_FLOOR),
            new Range("level", -DungeonAdminMath.MAX_LEVEL, DungeonAdminMath.MAX_LEVEL),
            new Range("amount", 1, SP_MAX));

    /** One runnable path of the tree with the key of its one-line help text. */
    public record Entry(String usage, String helpKey) {
        /** The first word of the usage: the top-level literal. */
        public String top() {
            int i = usage.indexOf(' ');
            return i < 0 ? usage : usage.substring(0, i);
        }
    }

    /** Every runnable path under {@code /dungeon} that the admin tree adds, in the order of {@code /dungeon admin help}. */
    public static final List<Entry> ENTRIES = List.of(
            new Entry("tp <floor>", AdminText.HELP_TP),
            new Entry("skip", AdminText.HELP_SKIP),
            new Entry("skip <floors>", AdminText.HELP_SKIP_N),
            new Entry("boss spawn <level>", AdminText.HELP_BOSS_SPAWN),
            new Entry("boss kill", AdminText.HELP_BOSS_KILL),
            new Entry("rift spawn pocket", AdminText.HELP_RIFT_POCKET),
            new Entry("rift spawn shortcut", AdminText.HELP_RIFT_SHORTCUT),
            new Entry("skill unlock <skill>", AdminText.HELP_SKILL_UNLOCK),
            new Entry("skill give_sp <amount>", AdminText.HELP_SKILL_SP),
            new Entry("reset alignment", AdminText.HELP_RESET_ALIGNMENT),
            new Entry("reset skills", AdminText.HELP_RESET_SKILLS),
            new Entry("reset waypoint", AdminText.HELP_RESET_WAYPOINT),
            new Entry("reset all", AdminText.HELP_RESET_ALL),
            new Entry("debug wfc", AdminText.HELP_DEBUG_WFC),
            new Entry("debug info", AdminText.HELP_DEBUG_INFO),
            new Entry("admin help", AdminText.HELP_ADMIN_HELP));

    /** The usage strings of {@link #ENTRIES}, in order. */
    public static List<String> usages() {
        List<String> out = new ArrayList<>(ENTRIES.size());
        for (Entry e : ENTRIES) out.add(e.usage());
        return out;
    }

    /** The distinct top-level literals the entries use, in order of first appearance. */
    public static Set<String> topLiterals() {
        Set<String> out = new LinkedHashSet<>();
        for (Entry e : ENTRIES) out.add(e.top());
        return out;
    }

    /** The range of an integer argument by name, or null. */
    public static Range range(String argument) {
        for (Range r : RANGES) if (r.argument().equals(argument)) return r;
        return null;
    }
}
