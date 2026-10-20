package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialData.Site;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaChecks;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndRules;
import com.newuniverse.nusmp.labyrinth.seal.GatekeeperDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of the Decennial feature can be played by its author, so it registers SELF-TESTS ({@link SelfTests}) that the CI server smoke test runs in the real game.
 * {@code boss.decennial_roster} reads the roster the game itself loaded (not the files on disk): 200 bosses, 100 on each road, one per level, unique ids and names, every hall named exists, two
 * to four phases and one or two lair actions each, every lair action and signature known to the code, every look one of the 22 abyssal looks, the grade of the loot rising with the level, every
 * ability one the Guardian's D&D layer knows, the Underworld Root laid out as the canon says, and the public facade answering. {@code boss.decennial_arena} builds EVERY distinct hall of the roster
 * (36) in a scratch region far out in the Mega-Dungeon's dimension through the very placer a floor uses and reads the blocks back: the boundary is sealed all the way round except at the two
 * doorways, the ceiling is closed, every cell of every interactive element is the block the plan says, the Dungeon Guard's break rule knows exactly the cells of the elements that may be broken,
 * breaking one removes it and resetting the hall puts it back, the entry seal closes with bars and opens again, and a hall turned a quarter puts its Gatekeeper on the east side. The scratch halls
 * stay where they were built (the next run overwrites them), far from anything the dungeon uses, which is what the throw-away CI world is for.
 */
final class DecennialSelfTests {
    private DecennialSelfTests() {}

    /** The scratch region: far from every lane and from the other self-tests' rooms. Each hall gets its own 100-block step along x. */
    private static final int X0 = 6_600_000, Z0 = 6_600_000, STEP = 100;

    static void register() {
        SelfTests.register("boss.decennial_roster", DecennialSelfTests::roster);
        SelfTests.register("boss.decennial_arena", DecennialSelfTests::arena);
    }

    private static final class Expect extends RuntimeException {
        Expect(String message) { super(message, null, false, false); }
    }

    private static void expect(boolean condition, String message) { if (!condition) throw new Expect(message); }

    // ================================================================ the roster
    private static SelfTests.Result roster(MinecraftServer server) {
        try {
            return rosterChecks();
        } catch (Expect e) {
            return SelfTests.Result.fail(e.getMessage());
        }
    }

    private static SelfTests.Result rosterChecks() {
        List<DecennialProfile> all = DecennialRoster.all();
        expect(all.size() == 200, "the roster has " + all.size() + " bosses, not 200 (a datapack replaced the files, or one did not parse; see the log)");
        int holy = 0, evil = 0, lastGrade = -1;
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (int n = 1; n <= 100; n++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                DecennialProfile p = DecennialRoster.forLevel(sign * n);
                expect(p != null, "level " + (sign * n) + " has no boss");
                expect(p.level() == sign * n && p.floor() == DecennialRules.floorOf(sign * n), p.id() + " is not level " + (sign * n));
                expect(p.side().equals(sign < 0 ? "evil" : "holy"), p.id() + " is on the wrong road");
                if (sign < 0) evil++;
                else holy++;
                expect(ids.add(p.id()), "a duplicate id " + p.id());
                expect(names.add(p.name()), "a duplicate name " + p.name());
                expect(ArenaThemes.exists(p.theme()), p.id() + " names the unknown hall " + p.theme());
                expect(p.phaseCount() >= 2 && p.phaseCount() <= 4, p.id() + " has " + p.phaseCount() + " phases");
                expect(!p.lairs().isEmpty() && p.lairs().size() <= 2, p.id() + " has " + p.lairs().size() + " lair actions");
                for (DecennialProfile.LairRef l : p.lairs()) {
                    DecennialVocab.Lair def = DecennialVocab.lair(l.id());
                    expect(def != null, p.id() + " names the unknown lair action " + l.id());
                    expect(def.telegraph() >= 16, l.id() + " warns for less than 0.8 s");
                    expect(def.share() <= DecennialRules.MAX_LAIR_SHARE, l.id() + " hits harder than the cap");
                }
                expect(DecennialVocab.signature(p.signature()) != null, p.id() + " names the unknown signature " + p.signature());
                expect(AbyssalLooks.get(p.look()) != null, p.id() + " wears the unknown look '" + p.look() + "'");
                expect(p.loot().grade().equals(DecennialRules.gradeOf(sign * n)), p.id() + " pays the wrong grade");
                for (DecennialProfile.Phase ph : p.phases()) for (String a : ph.abilities()) expect(DndRules.LEGENDARY_IDS.contains(a), p.id() + " has the unknown ability " + a);
                DndProfile d = DecennialDnd.of(p, p.phaseCount());
                expect(DecennialDnd.isDecennial(d.id()) && d.lair().isEmpty() && !d.mythic(), p.id() + " translates into a wrong D&D profile");
            }
            int g = DecennialRules.gradeIndex(DecennialRules.gradeOf(n));
            expect(g >= lastGrade, "the grade falls at level " + n);
            lastGrade = g;
        }
        expect(holy == 100 && evil == 100, "the roads have " + holy + " and " + evil + " bosses");
        for (String theme : DecennialRoster.themes()) expect(ArenaThemes.exists(theme), "the roster uses the unknown hall " + theme);
        for (String id : DecennialVocab.lairs().keySet()) expect(DecennialVocab.lairs().get(id).effect() != null, id);
        // the Underworld Root
        for (int i = 0; i < DecennialRules.GATE_LEVELS.length; i++) {
            DecennialProfile gate = DecennialRoster.forLevel(-DecennialRules.GATE_LEVELS[i]);
            expect(gate != null && gate.theme().equals("qliphoth_gate_" + (i + 1)), "the gate of band " + (i + 1) + " does not stand in its gate hall");
            expect(gate.band() == i + 1, "the gate boss of band " + (i + 1) + " says band " + gate.band());
        }
        for (int d = 98; d <= 100; d++) {
            DecennialProfile s = DecennialRoster.forLevel(-d);
            expect(s != null && s.theme().equals("qliphoth_root_throne") && !s.devil().isEmpty(), "level -" + d + " is not a Supreme Devil on the Root Throne");
        }
        // the facade
        expect(DecennialBosses.levels().size() == 200, "the facade lists " + DecennialBosses.levels().size() + " levels");
        expect(DecennialBosses.levels().get(0) == -100 && DecennialBosses.levels().get(199) == 100, "the facade's levels are not -100 .. 100");
        String text = DecennialBosses.describe(-100);
        expect(text.contains(DecennialRoster.forLevel(-100).name()) && text.contains("qliphoth_root_throne"), "describe(-100) does not name the boss and its hall: " + text);
        expect(DecennialBosses.describe(0).contains("no Decennial boss"), "describe(0) did not say there is no boss");
        expect(DecennialBosses.spawnBoss(null, 5, BlockPos.ZERO) == null && DecennialBosses.killBosses(null, null) == 0, "the facade took a null level");
        int paying = 0;
        List<DecennialLoot.Candidate> catalog = DecennialRewards.catalog();
        for (DecennialProfile p : all) if (DecennialLoot.pick(p, catalog, Set.of()) != null) paying++;
        return SelfTests.Result.ok("200 bosses (100 + 100), unique names, 36 halls named, grades rise, Underworld Root intact; " + paying + " of 200 levels have an item of their grade in the catalog");
    }

    // ================================================================ the halls
    private static SelfTests.Result arena(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("no mega dimension");
        try {
            return arenaChecks(server, level);
        } catch (Expect e) {
            return SelfTests.Result.fail(e.getMessage());
        }
    }

    private static final boolean ALL_HALLS = Boolean.getBoolean("nusmp.selftest.allhalls");

    private static SelfTests.Result arenaChecks(MinecraftServer server, ServerLevel level) {
        List<String> themes = DecennialRoster.themes();
        expect(themes.size() >= 24, "the roster uses only " + themes.size() + " halls (the brief asks for at least 24)");
        int builtHalls = 0, elementCount = 0, elementCells = 0, wallCells = 0;
        DecennialData data = DecennialData.get(server);
        for (int i = 0; i < themes.size(); i++) {
            String theme = themes.get(i);
            BossArenaStructure plan = ArenaThemes.build(theme);
            expect(plan != null, theme + " could not be planned");
            List<String> problems = ArenaChecks.problems(plan);
            expect(problems.isEmpty(), theme + " plan: " + problems.get(0));
            // the CI server must not write millions of blocks on one tick (watchdog): every hall is PLANNED and checked above, a sample is BUILT (-Dnusmp.selftest.allhalls=true builds all)
            if (!ALL_HALLS && i % 6 != 0 && !theme.contains("graveyard") && !theme.contains("throne") && !theme.equals("qliphoth_gate_1")) continue;
            ArenaFrame f = new ArenaFrame(X0 + i * STEP, MegaGrid.FLOOR_Y, Z0, plan.size, 0);
            expect(ArenaPlacer.fits(level, plan, f.y0()), theme + " does not fit between the bottom and the top of the level");
            ArenaPlacer.buildNow(level, plan, f, true);
            builtHalls++;
            wallCells += checkWalls(level, plan, f, theme);
            int[] counted = checkElements(level, plan, f, theme);
            elementCount += counted[0];
            elementCells += counted[1];
            checkLiveHall(server, level, data, plan, f, theme);
        }
        checkRotation(level, themes.get(0));
        return SelfTests.Result.ok(builtHalls + " halls built and read back: " + wallCells + " wall cells sealed, " + elementCount + " interactive elements (" + elementCells
                + " blocks) match their plans, the break rule, the reset, the seal and a quarter turn all behave");
    }

    private static BlockPos at(ArenaFrame f, int x, int y, int z) { return ArenaPlacer.worldPos(f, x, y, z); }

    /** The boundary is closed (the two doorways excepted) all the way up, and the ceiling is closed. */
    private static int checkWalls(ServerLevel level, BossArenaStructure plan, ArenaFrame f, String theme) {
        int last = plan.size - 1, checked = 0;
        for (int y = 1; y <= plan.height - 2; y++) {
            for (int i = 0; i <= last; i++) {
                for (int layer = 0; layer <= 1; layer++) {
                    int[][] cells = {{i, layer}, {i, last - layer}, {layer, i}, {last - layer, i}};
                    for (int[] c : cells) {
                        if (BossArenaStructure.inDoorway(c[0], y, c[1], plan.size)) continue;
                        BlockState s = level.getBlockState(at(f, c[0], y, c[1]));
                        expect(!s.isAir(), theme + ": the wall is open at local (" + c[0] + ", " + y + ", " + c[1] + ")");
                        checked++;
                    }
                }
            }
        }
        for (int z = 0; z <= last; z += 3) for (int x = 0; x <= last; x += 3) expect(!level.getBlockState(at(f, x, plan.height - 1, z)).isAir(), theme + ": the ceiling is open at local (" + x + ", " + z + ")");
        BlockState gate = level.getBlockState(at(f, plan.c, 2, 1));
        expect(gate.getBlock() instanceof GatekeeperDoorBlock, theme + ": the Gatekeeper slab is not in the north doorway");
        expect(level.getBlockState(at(f, plan.c, 2, last)).isAir(), theme + ": the entry is not open");
        return checked;
    }

    /** Every cell of every element is the block the plan says; returns {elements, cells}. */
    private static int[] checkElements(ServerLevel level, BossArenaStructure plan, ArenaFrame f, String theme) {
        expect(!plan.elements().isEmpty(), theme + " has no interactive element");
        int cells = 0;
        for (BossArenaStructure.Element e : plan.elements()) {
            expect(!e.cells.isEmpty(), theme + ": element " + e.id + " has no cell");
            for (BossArenaStructure.Placed c : e.cells) {
                BlockState want = ArenaPlacer.state(c.spec(), f.rot()), have = level.getBlockState(at(f, c.x(), c.y(), c.z()));
                expect(have.getBlock() == want.getBlock(), theme + ": element " + e.id + " cell (" + c.x() + ", " + c.y() + ", " + c.z() + ") is " + have + " in the world but the plan says " + c.spec());
                cells++;
            }
        }
        return new int[] {plan.elements().size(), cells};
    }

    /** The break rule knows exactly the breakable cells; breaking removes, resetting restores; the seal closes and opens. */
    private static void checkLiveHall(MinecraftServer server, ServerLevel level, DecennialData data, BossArenaStructure plan, ArenaFrame f, String theme) {
        int levelOfTheme = 0;
        for (DecennialProfile p : DecennialRoster.all()) if (p.theme().equals(theme)) {
            levelOfTheme = p.level();
            break;
        }
        DecennialProfile owner = DecennialRoster.forLevel(levelOfTheme);
        expect(owner != null, theme + " is used by no boss");
        Site site = DecennialArenas.registerStandalone(level, owner, plan, f);
        site.cursor = ArenaPlacer.total(plan);
        site.built = true;
        try {
            DecennialArenas.Live live = DecennialArenas.live(site);
            expect(live != null, theme + " could not go live");
            int expectedCells = 0;
            BossArenaStructure.Element target = null;
            for (BossArenaStructure.Element e : plan.elements()) {
                if (e.locked) continue;
                expectedCells += e.cells.size();
                if (target == null) target = e;
            }
            if (DecennialConfig.get(DecennialConfig.ELEMENTS)) {
                expect(live.cells.size() == expectedCells, theme + ": the break rule knows " + live.cells.size() + " cells, the plan has " + expectedCells + " breakable ones");
                for (BossArenaStructure.Element e : plan.elements()) {
                    BlockPos p = at(f, e.x(), e.y(), e.z());
                    expect(BreakableCells.serverHas(p.asLong()) == !e.locked, theme + ": element " + e.id + (e.locked ? " is locked but breakable" : " is breakable but the rule does not know it"));
                }
                if (target != null) {
                    BlockPos p0 = at(f, target.x(), target.y(), target.z());
                    expect(DecennialArenas.breakAt(level, p0, null), theme + ": breaking element " + target.id + " did nothing");
                    for (BossArenaStructure.Placed c : target.cells) expect(level.getBlockState(at(f, c.x(), c.y(), c.z())).isAir(), theme + ": a broken element left a block at (" + c.x() + ", " + c.y() + ", " + c.z() + ")");
                    expect(site.broken.contains(target.id) && !BreakableCells.serverHas(p0.asLong()), theme + ": the broken element is still breakable");
                    DecennialArenas.reset(level, live);
                    for (BossArenaStructure.Placed c : target.cells) {
                        expect(level.getBlockState(at(f, c.x(), c.y(), c.z())).getBlock() == ArenaPlacer.state(c.spec(), f.rot()).getBlock(), theme + ": the reset did not put the element back");
                    }
                    for (BossArenaStructure.Placed r : target.rubble) expect(level.getBlockState(at(f, r.x(), r.y(), r.z())).isAir(), theme + ": the reset left rubble");
                    expect(site.broken.isEmpty() && BreakableCells.serverHas(p0.asLong()), theme + ": the reset did not make the element breakable again");
                }
            }
            if (DecennialConfig.get(DecennialConfig.SEAL) && !plan.sealCells().isEmpty()) {
                expect(DecennialArenas.closeSeal(level, live), theme + ": the seal did not close");
                for (BossArenaStructure.Placed c : plan.sealCells()) expect(level.getBlockState(at(f, c.x(), c.y(), c.z())).is(Blocks.IRON_BARS), theme + ": a seal cell is not iron bars");
                DecennialArenas.openSeal(level, live, "selftest");
                for (BossArenaStructure.Placed c : plan.sealCells()) expect(level.getBlockState(at(f, c.x(), c.y(), c.z())).isAir(), theme + ": a seal cell stayed closed");
            }
        } finally {
            DecennialArenas.forget(site);
            data.remove(site.key);
        }
    }

    /** A hall turned a quarter puts its Gatekeeper on the east side. */
    private static void checkRotation(ServerLevel level, String theme) {
        BossArenaStructure plan = ArenaThemes.buildForFloor(theme);
        ArenaFrame f = new ArenaFrame(X0, MegaGrid.FLOOR_Y, Z0 + 200, plan.size, 1);
        ArenaPlacer.buildNow(level, plan, f, true);
        BlockPos gate = at(f, plan.c, 2, 1);
        expect(gate.getX() > f.cx() + 15 && Math.abs(gate.getZ() - f.cz()) <= 1, "a hall turned a quarter does not put its Gatekeeper on the east side (" + gate + ")");
        expect(level.getBlockState(gate).getBlock() instanceof GatekeeperDoorBlock, "the rotated Gatekeeper slab is not where the frame says");
        BlockPos entry = at(f, plan.c, 2, plan.size - 1);
        expect(entry.getX() < f.cx() - 15 && level.getBlockState(entry).isAir(), "the rotated entry is not open on the west side");
    }
}
