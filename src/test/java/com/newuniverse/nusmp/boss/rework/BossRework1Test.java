package com.newuniverse.nusmp.boss.rework;

import com.newuniverse.nusmp.arc.tier.TierMove;
import com.newuniverse.nusmp.arc.tier.TierRoster;
import com.newuniverse.nusmp.arc.tier.TierRow;
import com.newuniverse.nusmp.arc.tier.TierSignature;
import com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisTable;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathTable;
import com.newuniverse.nusmp.relics.order.OrderTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): the move tables only grew at the end (the client reads the move being wound up by index), every appended move is
 * telegraphed and named in the lang, the setting off gives back the old lists, Lucifero's own files no longer make vanilla dust, the three Dungeon monsters each throw a
 * signature effect, and the self-test key sits in the smoke list before {@code boss.stats}. Pure: tables and file reads (the working directory is the project root).
 */
class BossRework1Test {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String path) throws IOException { return Files.readString(Path.of(path)); }

    /** The old move lists, pinned as they were before the rework: they must stay at the head of the tables, in this order. */
    private static final List<String> GOLIATH_OLD = List.of("ground_slam", "bedrock_fist", "roaring_shockwave", "twin_stomp", "seismic_line", "rubble_fall", "bedrock_quake"),
            CHARYBDIS_OLD = List.of("scale_volley", "tentacle_sweep", "whirlpool", "beak_lunge", "summon_megalodons", "tidal_crush", "maelstrom_of_scales"),
            ORDER_OLD = List.of("decree", "writ", "judgement", "erasure");

    private static List<String> goliath() { return GoliathTable.specs().stream().map(GoliathTable.Spec::id).toList(); }

    private static List<String> charybdis() { return CharybdisTable.specs().stream().map(CharybdisTable.Spec::id).toList(); }

    private static List<String> order() { return OrderTable.specs().stream().map(OrderTable.Spec::id).toList(); }

    @Test
    void theTablesOnlyGrewAtTheEnd() {
        assertEquals(GOLIATH_OLD, goliath().subList(0, GoliathTable.OLD_MOVES));
        assertEquals(CHARYBDIS_OLD, charybdis().subList(0, CharybdisTable.OLD_MOVES));
        assertEquals(ORDER_OLD, order().subList(0, OrderTable.OLD_MOVES));
        assertTrue(goliath().size() - GoliathTable.OLD_MOVES >= 4, "the Goliath gets four or more moves");
        assertTrue(charybdis().size() - CharybdisTable.OLD_MOVES >= 4, "the Charybdis Spawn gets four or more moves");
        assertTrue(order().size() >= 8, "the God of Order fights with eight or more");
        for (List<String> ids : List.of(goliath(), charybdis(), order())) assertEquals(ids.size(), ids.stream().distinct().count(), "unique ids: " + ids);
    }

    @Test
    void theSettingOffGivesBackTheOldLists() {
        assertEquals(GOLIATH_OLD, BossRework.trim(goliath(), GoliathTable.OLD_MOVES, false));
        assertEquals(goliath(), BossRework.trim(goliath(), GoliathTable.OLD_MOVES, true));
        assertEquals(ORDER_OLD, BossRework.trim(order(), OrderTable.OLD_MOVES, false));
        assertEquals(List.of(1, 2), BossRework.trim(List.of(1, 2), 5, false), "a shorter list is left alone");
        for (TierRow row : TierRoster.all()) {
            List<TierMove> off = TierSignature.moves(row.moves(), false);
            assertEquals(row.moves().subList(0, off.size()), off, row.id() + ": the appended move is the last row, so no index moves");
            assertSame(row.moves(), TierSignature.moves(row.moves(), true));
        }
    }

    @Test
    void sevenTierBossesGotOneSignatureMoveEachAppendedLast() {
        assertEquals(7, TierSignature.ROWS.size());
        for (String[] r : TierSignature.ROWS) {
            TierRow row = TierRoster.all().stream().filter(x -> x.id().equals(r[0])).findFirst().orElseThrow(() -> new AssertionError("no row " + r[0]));
            TierMove last = row.moves().get(row.moves().size() - 1);
            assertEquals(r[1], last.id(), r[0] + ": the signature move is appended last");
            assertEquals(5, row.moves().size() - 1, r[0] + ": five moves before it");
            assertFalse(last.ultimate(), r[0] + " keeps one ultimate");
            assertTrue(last.windUp() >= 30 && last.cooldown() >= 300 && last.minPower() >= 40, r[0] + ": a heavy, telegraphed, rare move");
        }
        long rows = TierRoster.all().stream().filter(x -> x.moves().stream().anyMatch(m -> TierSignature.is(m.id()))).count();
        assertEquals(7, rows, "no other row carries one");
    }

    @Test
    void everyAppendedMoveIsTelegraphedLongEnough() {
        for (GoliathTable.Spec s : GoliathTable.specs().subList(GoliathTable.OLD_MOVES, GoliathTable.specs().size())) {
            assertTrue(s.windUpTicks() >= 24, s.id());
            assertFalse(GoliathTable.warning(s.id()).isBlank(), s.id() + " warns");
        }
        for (CharybdisTable.Spec s : CharybdisTable.specs().subList(CharybdisTable.OLD_MOVES, CharybdisTable.specs().size())) {
            assertTrue(s.windUpTicks() >= 24, s.id());
            assertFalse(CharybdisTable.warning(s.id()).isBlank(), s.id() + " warns");
            assertFalse(s.ultimate(), "one ultimate stays");
        }
        for (OrderTable.Spec s : OrderTable.specs().subList(OrderTable.OLD_MOVES, OrderTable.specs().size())) {
            assertTrue(s.windUp() >= 24, s.id());
            assertFalse(s.ultimate(), "the Erasure stays the only ultimate");
        }
    }

    /** The body of {@code tellOf} in a rework file: from its signature to the first {@code default -> null}. */
    private static String tellOf(String file) throws IOException {
        String c = read(SRC + file);
        int a = c.indexOf("static Move.MoveTell tellOf"), b = c.indexOf("default -> null", a);
        assertTrue(a >= 0 && b > a, file + " has a tellOf switch");
        return c.substring(a, b);
    }

    @Test
    void everyAppendedMoveHasATellCaseAndTheOldSwitchesReachTheReworkFiles() throws IOException {
        String g = tellOf("dungeon/floor/goliath/GoliathRework.java");
        for (String c : new String[]{"MAGEBANE_ROAR", "BOULDER_HURL", "STONE_SKIN", "POUND_SHOCKWAVE", "STOMP_CHAIN"}) assertTrue(g.contains("GoliathTable." + c), c);
        String ch = tellOf("dungeon/floor/charybdis/CharybdisRework.java");
        for (String c : new String[]{"SCALE_SHARD_STORM", "TEMPEST_CURRENT", "WHIRLPOOL_MAW", "MAGIC_INTERFERENCE"}) assertTrue(ch.contains("CharybdisTable." + c), c);
        String o = tellOf("relics/order/OrderRework.java");
        for (String c : new String[]{"SEAL_OF_SILENCE", "SCALES_OF_ORDER", "CHAINS_OF_LAW", "VERDICT_RAIN"}) assertTrue(o.contains("OrderTable." + c), c);
        assertTrue(read(SRC + "dungeon/floor/goliath/GoliathBoss.java").contains("default -> GoliathRework.tellOf(this, id)"));
        assertTrue(read(SRC + "dungeon/floor/charybdis/CharybdisMoves.java").contains("default -> CharybdisRework.tellOf(id)"));
        assertTrue(read(SRC + "relics/order/OrderMoves.java").contains("default -> OrderRework.tellOf(god, id)"));
        for (String f : new String[]{"dungeon/floor/goliath/GoliathBoss.java", "dungeon/floor/charybdis/CharybdisSpawnBoss.java", "relics/order/OrderMoves.java"})
            assertTrue(read(SRC + f).contains("BossRework.trim("), f + " trims its list with the setting");
        assertTrue(read(SRC + "arc/tier/TierMoves.java").contains("TierSignature.moves(row.moves()"), "the tier lists drop the appended move with the setting off");
    }

    @Test
    void luciferoNoLongerMakesVanillaDust() throws IOException {
        for (String f : new String[]{"arc/spade/LuciferoBoss.java", "arc/spade/LuciferoMoves.java"}) {
            String c = read(SRC + f);
            for (String bad : new String[]{"DustParticleOptions", "SpadeFx.dust", "SpadeFx.spoke", "Tells.burst"}) assertFalse(c.contains(bad), f + " still uses " + bad);
        }
        String fx = read(SRC + "arc/spade/LuciferoFx.java");
        for (String shape : new String[]{"ZogratisVfx.pillar", "ZogratisVfx.well", "ZogratisVfx.singularity", "ZogratisVfx.gravityImpact", "ZogratisVfx.lane"}) assertTrue(fx.contains(shape), shape);
        assertFalse(fx.contains("DustParticleOptions"));
        assertEquals(count(fx, "Tells.burst") + count(fx, "SpadeFx.spoke"), count(fx, "else Tells.burst") + count(fx, "            SpadeFx.spoke"),
                "the old dust only behind the setting");
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    @Test
    void theThreeDungeonMonstersEachThrowTheirOwnSignature() throws IOException {
        String base = read(SRC + "arc/danmachi/DanMachiBoss.java");
        assertTrue(base.contains("ReworkFx.on() && level() instanceof ServerLevel sl && target != null) signature(sl, s, target);"), "one signature per cast, behind the setting");
        Map<String, String> shapes = Map.of("arc/danmachi/BlackGoliathBoss.java", "BOSS_DEVIL_CRACKS", "arc/danmachi/MinotaurBoss.java", "SLASH_SCYTHE",
                "arc/danmachi/AmphisbaenaBoss.java", "WATER_DRAGON");
        for (Map.Entry<String, String> e : shapes.entrySet()) {
            String c = read(SRC + e.getKey());
            assertTrue(c.contains("protected void signature(ServerLevel sl, DmSpec s, LivingEntity target)") && c.contains(e.getValue()), e.getKey());
        }
    }

    @Test
    void everyAppendedMoveIsNamedInTheLangAndItsSource() throws IOException {
        String en = read("src/main/resources/assets/anime_multiverse/lang/en_us.json"), frag = read("tools/lang/boss_rework1.json");
        List<String[]> names = new ArrayList<>();
        for (GoliathTable.Spec s : GoliathTable.specs().subList(GoliathTable.OLD_MOVES, GoliathTable.specs().size())) names.add(new String[]{"goliath", s.id(), s.label()});
        for (CharybdisTable.Spec s : CharybdisTable.specs().subList(CharybdisTable.OLD_MOVES, CharybdisTable.specs().size())) names.add(new String[]{"charybdis_spawn", s.id(), s.label()});
        for (OrderTable.Spec s : OrderTable.specs().subList(OrderTable.OLD_MOVES, OrderTable.specs().size())) names.add(new String[]{"god_of_order", s.id(), s.label()});
        for (String[] r : TierSignature.ROWS) {
            TierRow row = TierRoster.all().stream().filter(x -> x.id().equals(r[0])).findFirst().orElseThrow();
            names.add(new String[]{r[0], r[1], row.moves().get(row.moves().size() - 1).label()});
        }
        assertEquals(20, names.size());
        for (String[] n : names) {
            String line = "\"anime_multiverse.boss_move." + n[0] + "." + n[1] + "\": \"" + n[2] + "\"";
            assertTrue(en.contains(line), "en_us.json lacks " + line);
            assertTrue(frag.contains(line), "tools/lang/boss_rework1.json lacks " + line);
        }
    }

    @Test
    void theSettingsTheSelfTestAndTheSmokeList() throws IOException {
        String rules = read(SRC + "boss/rules/BossRulesConfig.java");
        assertTrue(rules.contains(".define(\"bossRework1Moves\", true)") && rules.contains(".define(\"bossRework1Vfx\", true)"), "two server switches, on by default");
        String look = read(SRC + "client/playermodel/PlayerModelOptions.java");
        assertTrue(look.contains(".define(\"goliathStoneModel\", true)"), "the Goliath's stone model is the default again, a client setting");
        assertTrue(read(SRC + "client/playermodel/DungeonRenderers.java").contains("PlayerModelOptions.goliathStone()"));
        assertTrue(read(SRC + "boss/rework/BossReworkSelfTests.java").contains("\"boss.rework1\""));
        assertTrue(read(SRC + "boss/BossHooks.java").contains("BossReworkSelfTests.register()"));
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",boss.rework1,boss.rework3,boss.stats,tier\""), "boss.rework1 sits before boss.stats, which stays just before the final tier");
    }

    @Test
    void theNewCommonCodeTouchesNoClientClass() throws IOException {
        for (String f : new String[]{"boss/rework/BossRework.java", "boss/rework/ReworkFx.java", "boss/rework/BossReworkSelfTests.java", "dungeon/floor/goliath/GoliathRework.java",
                "dungeon/floor/charybdis/CharybdisRework.java", "relics/order/OrderRework.java", "arc/spade/LuciferoFx.java", "arc/tier/TierSignature.java"}) {
            String c = read(SRC + f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.newuniverse.nusmp.client") || c.contains("Minecraft.getInstance") || c.contains("com.mojang.blaze3d"), f);
            assertFalse(c.contains("getEntities(") || c.contains("addFreshEntity") || c.contains("ServerTickEvent"), f + ": no world scan, no spawn, no tick hook");
        }
        String pure = read(SRC + "boss/rework/BossRework.java");
        assertFalse(pure.contains("net.minecraft") || pure.contains("net.neoforged"), "the rules are pure");
    }

    @Test
    void thePureRules() {
        assertEquals(10f, BossRework.shielded(20f, 0.5f), 1e-6);
        assertEquals(2f, BossRework.shielded(20f, 0f), 1e-6, "never under a tenth");
        assertEquals(20f, BossRework.shielded(20f, 3f), 1e-6, "never over the hit");
        double[][] c = BossRework.chain(0, 0, 10, 0, 0, 1, 4, 3, 3.5, 9);
        assertEquals(4, c.length);
        assertArrayEquals(new double[]{3, 0}, c[0], 1e-9);
        assertArrayEquals(new double[]{6.5, 0}, c[1], 1e-9);
        assertArrayEquals(new double[]{9, 0}, c[3], 1e-9, "never past the reach");
        assertArrayEquals(new double[]{0, 3}, BossRework.chain(1e-5, 0, 0, 0, 0, 1, 1, 3, 1, 9)[0], 1e-3, "a foe on top: along the facing");
        assertEquals(8, BossRework.chain(0, 0, 1, 0, 0, 1, 99, 1, 1, 9).length, "at most eight");
        assertEquals(0.0, BossRework.pull(3, 20, 4, 0.16), "nothing in the core");
        assertEquals(0.0, BossRework.pull(21, 20, 4, 0.16), "nothing outside");
        assertEquals(0.16, BossRework.pull(20, 20, 4, 0.16), 1e-9, "strongest at the rim");
        assertTrue(CharybdisTable.CURRENT_SPEED < 0.2, "the current never beats walking");
    }

    @Test
    void theDocNamesEveryAppendedMove() throws IOException {
        String doc = read("docs/bosses/rework_wave1.md");
        List<String> ids = new ArrayList<>(goliath().subList(GoliathTable.OLD_MOVES, goliath().size()));
        ids.addAll(charybdis().subList(CharybdisTable.OLD_MOVES, charybdis().size()));
        ids.addAll(order().subList(OrderTable.OLD_MOVES, order().size()));
        ids.addAll(TierSignature.IDS);
        for (String id : ids) assertTrue(doc.contains("`" + id + "`"), "the doc names " + id);
        for (String w : new String[]{"NEW", "REPLACED", "KEPT", "Worst-case cost", "Blender"}) assertTrue(doc.contains(w), w);
    }
}
