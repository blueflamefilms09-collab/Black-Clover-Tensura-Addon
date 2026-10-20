package com.newuniverse.nusmp.labyrinth.mega.admin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Brigadier cannot be run in a unit test (it needs the game's classes around it), so this checks the tree where it can be checked: the plain-data spec of the tree
 * ({@link AdminSpec}) against the rules that make the tree safe next to the commands that already exist, and the SOURCE of the tree ({@code AdminTree.java}) against the spec, by
 * reading it as text, so the two cannot drift even before the in-game self-test {@code dungeon.admin} compares the real dispatcher with the spec. What is pinned: every admin
 * literal is gated at permission level 2 and no player subcommand is (the gate is {@code gated(...)}, a helper that adds {@code requires(hasPermission(2))}, and the root literal
 * has none); the admin literals do not reuse the name of a player subcommand (a merge would silently join the two); every usage has a help line; the integer ranges of the tree are
 * the owner's; the command tree is attached with the same root literal as the player's {@code /dungeon}; and the one-line registration is in {@code MegaSystem}.
 */
public class AdminSpecTest {
    private static final Path TREE = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega", "admin", "AdminTree.java");
    private static final Path PLAYER = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega", "lock", "DungeonCommand.java");
    private static final Path MEGA_SYSTEM = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega", "MegaSystem.java");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @Test
    public void thePermissionIsLevelTwoAndTheRootIsTheSameLiteralAsThePlayersDungeon() {
        assertEquals(2, AdminSpec.PERMISSION);
        assertEquals("dungeon", AdminSpec.ROOT);
    }

    @Test
    public void everyAdminLiteralIsDistinctFromEveryPlayerLiteral() {
        Set<String> seen = new HashSet<>();
        for (String lit : AdminSpec.ADMIN_LITERALS) {
            assertTrue(seen.add(lit), "listed twice: " + lit);
            assertFalse(AdminSpec.PLAYER_LITERALS.contains(lit), "the admin literal '" + lit + "' has the name of a player subcommand and would be merged into it");
        }
        assertEquals(8, AdminSpec.ADMIN_LITERALS.size());
        assertEquals(5, AdminSpec.PLAYER_LITERALS.size());
    }

    @Test
    public void everyEntryHangsFromAnAdminLiteralAndEveryAdminLiteralHasAnEntry() {
        assertEquals(new HashSet<>(AdminSpec.ADMIN_LITERALS), new HashSet<>(AdminSpec.topLiterals()), "the literals the entries use are exactly the admin literals");
        for (AdminSpec.Entry e : AdminSpec.ENTRIES) assertTrue(AdminSpec.ADMIN_LITERALS.contains(e.top()), e.usage());
    }

    @Test
    public void theUsagesAreDistinctAndEveryOneHasAHelpLine() {
        Set<String> seen = new HashSet<>();
        for (AdminSpec.Entry e : AdminSpec.ENTRIES) {
            assertTrue(seen.add(e.usage()), "listed twice: " + e.usage());
            assertNotNull(AdminText.englishOrNull(e.helpKey()), "no help text for " + e.usage());
            assertFalse(e.usage().startsWith("/") || e.usage().startsWith(AdminSpec.ROOT + " "), "a usage is written without the root: " + e.usage());
        }
        assertEquals(16, AdminSpec.ENTRIES.size());
        assertEquals(AdminSpec.ENTRIES.size(), AdminSpec.usages().size());
    }

    @Test
    public void theUsagesCoverEveryCommandOfTheOwnersSpec() {
        List<String> usages = AdminSpec.usages();
        for (String must : new String[] {"tp <floor>", "skip", "skip <floors>", "boss spawn <level>", "boss kill", "rift spawn pocket", "rift spawn shortcut", "skill unlock <skill>",
                "skill give_sp <amount>", "reset alignment", "reset skills", "reset waypoint", "reset all", "debug wfc", "debug info", "admin help"}) {
            assertTrue(usages.contains(must), "missing: " + must);
        }
    }

    @Test
    public void theRangesAreTheOwners() {
        assertEquals(-1000, AdminSpec.range("floor").min());
        assertEquals(1000, AdminSpec.range("floor").max());
        assertEquals(-1000, AdminSpec.range("floors").min());
        assertEquals(1000, AdminSpec.range("floors").max());
        assertEquals(-100, AdminSpec.range("level").min());
        assertEquals(100, AdminSpec.range("level").max());
        assertEquals(1, AdminSpec.range("amount").min());
        assertEquals(AdminSpec.SP_MAX, AdminSpec.range("amount").max());
        assertEquals(DungeonAdminMath.MAX_FLOOR, AdminSpec.range("floor").max());
        assertEquals(DungeonAdminMath.MAX_LEVEL, AdminSpec.range("level").max());
    }

    // ---------------------------------------------------------------- the source of the tree, read as text
    @Test
    public void theTreeIsGatedPerAdminChildAndNotAtTheRoot() throws IOException {
        String src = read(TREE);
        assertTrue(src.contains("Commands.literal(AdminSpec.ROOT)"), "the root is the player's literal");
        assertTrue(src.contains("requires(s -> s.hasPermission(AdminSpec.PERMISSION))"), "the gate helper");
        int rootStart = src.indexOf("Commands.literal(AdminSpec.ROOT)");
        int rootEnd = src.indexOf(".then(gated(", rootStart);
        assertFalse(src.substring(rootStart, rootEnd).contains("requires"), "the root literal must carry no permission, or the player's subcommands would be lost");
        Matcher gated = Pattern.compile("\\.then\\(gated\\(\"([a-z_]+)\"\\)").matcher(src);
        List<String> literals = new ArrayList<>();
        while (gated.find()) literals.add(gated.group(1));
        assertEquals(AdminSpec.ADMIN_LITERALS, literals, "the gated children, in order, are exactly the admin literals");
    }

    @Test
    public void theTreeUsesTheOwnersRangesAndTheSkillSuggestions() throws IOException {
        String src = read(TREE);
        assertTrue(src.contains("argument(\"floor\", IntegerArgumentType.integer(-maxFloor, maxFloor))"));
        assertTrue(src.contains("argument(\"floors\", IntegerArgumentType.integer(-maxFloor, maxFloor))"));
        assertTrue(src.contains("argument(\"level\", IntegerArgumentType.integer(-maxLevel, maxLevel))"));
        assertTrue(src.contains("argument(\"amount\", IntegerArgumentType.integer(1, AdminSpec.SP_MAX))"));
        assertTrue(src.contains("int maxFloor = DungeonAdminMath.MAX_FLOOR, maxLevel = DungeonAdminMath.MAX_LEVEL;"));
        assertTrue(src.contains("SharedSuggestionProvider.suggest(SkillAdmin.suggestions(), b)"), "tab completion comes from the skill registry");
        for (String arg : new String[] {"floor", "floors", "level", "skill", "amount"}) assertTrue(src.contains("argument(\"" + arg + "\""), arg);
    }

    @Test
    public void theTreeNeverReusesAPlayerSubcommandNameAtTheTopLevel() throws IOException {
        String player = read(PLAYER);
        Matcher m = Pattern.compile("Commands\\.literal\\(\"([a-z_]+)\"\\)").matcher(player.substring(player.indexOf("public static LiteralArgumentBuilder<CommandSourceStack> player()"), player.indexOf("public static LiteralArgumentBuilder<CommandSourceStack> admin()")));
        Set<String> real = new HashSet<>();
        while (m.find()) real.add(m.group(1));
        real.remove("dungeon");
        real.remove("party");                                                                    // the second word of /dungeon unstuck party
        assertEquals(new HashSet<>(AdminSpec.PLAYER_LITERALS), real, "the spec's list of the player's subcommands is what DungeonCommand.player() registers");
    }

    @Test
    public void theRegistrationIsOneLineInMegaSystem() throws IOException {
        String src = read(MEGA_SYSTEM);
        int n = 0;
        for (int i = src.indexOf("AdminRegistry.register(bus)"); i >= 0; i = src.indexOf("AdminRegistry.register(bus)", i + 1)) n++;
        assertEquals(1, n, "registered exactly once");
        assertTrue(src.indexOf("PuzzleRegistry.register(bus)") < src.indexOf("AdminRegistry.register(bus)"), "appended after the older registries");
    }
}
