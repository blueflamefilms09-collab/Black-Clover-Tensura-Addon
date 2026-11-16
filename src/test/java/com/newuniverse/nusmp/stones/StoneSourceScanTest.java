package com.newuniverse.nusmp.stones;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rules 9 and 11 for the Infinity Stone slot, pinned by reading the source: no client class in common code, the rules are pure, the gate reads base EP, the slot data and the tag exist, the settings are SERVER settings
 * with their own file, the shared files only gained their hooks, every per-player table is dropped on logout, death and dimension change, nothing scans the world per tick, no act controls a mind (owner decision),
 * and every text has its lang line. The smoke-list checks are order independent (the prefix is present and the tail is untouched).
 */
class StoneSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static List<Path> stones() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("stones"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeTouchesNoClientClassAndTheRulesArePure() throws IOException {
        List<Path> files = stones();
        assertTrue(files.size() >= 10);
        for (Path p : files) assertFalse(CLIENT.matcher(Files.readString(p)).find(), p + " names a client class");
        for (String pure : new String[]{"stones/StoneRules.java", "stones/InfinityStone.java"}) {
            String c = read(pure);
            assertFalse(c.contains("import net.") || c.contains("import com.mojang") || c.contains("import com.newuniverse"), pure + " must stay free of game types");
        }
    }

    @Test
    void theGateReadsBaseEpOnTheServerAndNeverTheBoostedOne() throws IOException {
        String slot = code(read("stones/StoneSlot.java"));
        assertTrue(slot.contains("TensuraBridge.baseMaxEp("), "the gate reads BASE max EP");
        assertFalse(slot.contains("maxEp(") && !slot.contains("baseMaxEp("), "never the grade-boosted EP");
        assertFalse(Pattern.compile("TensuraBridge\\.maxEp\\(").matcher(slot).find(), "never the boosted EP");
        assertTrue(slot.contains("StoneRules.gateOk(baseEp(p), StoneConfig.epGate())"));
        String kit = code(read("stones/StoneKit.java"));
        for (String must : new String[]{"StoneConfig.enabled()", "StoneSlot.refresh(p)", "st.working()", "InfinityStone.ofActKey("}) assertTrue(kit.contains(must), "refuse lost " + must);
    }

    @Test
    void theSlotDataTheTagAndTheSharedHooksExist() throws IOException {
        Path res = Path.of("src/main/resources/data");
        JsonObject slot = JsonParser.parseString(Files.readString(res.resolve("anime_multiverse/curios/slots/infinity_stone.json"))).getAsJsonObject();
        assertEquals(1, slot.get("size").getAsInt());
        JsonObject ent = JsonParser.parseString(Files.readString(res.resolve("anime_multiverse/curios/entities/infinity_stone.json"))).getAsJsonObject();
        assertTrue(ent.getAsJsonArray("entities").toString().contains("player") && ent.getAsJsonArray("slots").toString().contains("infinity_stone"));
        String tag = Files.readString(res.resolve("curios/tags/item/infinity_stone.json"));
        for (InfinityStone s : InfinityStone.values()) assertTrue(tag.contains("anime_multiverse:" + s.relicId()), s + " is not in the slot tag");
        assertTrue(tag.contains("\"replace\": false"), "the tag never replaces what Curios ships");
        assertTrue(read("relics/RelicCatalog.java").contains("StoneKit::extend"));
        assertTrue(read("relics/RelicCurioEquip.java").contains("StoneKit.slotFor("));
        String engine = code(read("relics/RelicEngine.java"));
        assertTrue(engine.contains("StoneKit.refuse(") && engine.indexOf("StoneKit.refuse(") < engine.indexOf("pay(p, act"), "a refused slot act costs nothing");
        assertTrue(engine.contains("StoneKit.costFactor(p)") && engine.contains("StoneKit.cooldownFactor(p)"));
        assertTrue(read("NUSMP.java").contains("stones.StoneBoot.init("));
        assertTrue(read("multiverse/MultiverseCommands.java").contains("stones.StoneCommand.node()"));
        assertTrue(read("relics/order/UniqueLedger.java").contains("loseAway("), "the away rule goes through the Order's ledger (no second registry)");
        for (Path p : stones()) assertFalse(code(Files.readString(p)).contains("SavedData"), p + " keeps its own saved registry; the Order's ledger already enforces one copy per server");
    }

    @Test
    void theSettingsAreServerSettingsInTheirOwnFile() throws IOException {
        String boot = Files.readString(SRC.resolve("stones/StoneBoot.java"));
        assertTrue(boot.contains("ModConfig.Type.SERVER, StoneConfig.SPEC, \"nusmp-stones-server.toml\""));
        assertFalse(boot.contains("ModConfig.Type.CLIENT"));
        assertTrue(boot.contains("StoneSelfTests.register()") && boot.contains("StoneEvents.register("));
        String cfg = read("stones/StoneConfig.java");
        for (String k : new String[]{"\"enabled\"", "\"epGate\"", "\"awayDays\"", "\"onDeath\"", "\"noContainers\"", "\"passives\"", "\"whispers\"", "\"scaleWithEp\""}) assertTrue(cfg.contains(k), "setting " + k);
        assertTrue(cfg.contains("b.push(\"infinityStones\")"));
    }

    @Test
    void theSmokeListHasTheStonesPrefixAndTheTailIsUntouched() throws IOException {
        String yml = Files.readString(Path.of(".github/workflows/smoke.yml"));
        Matcher m = Pattern.compile("SMOKE_EXPECT:\\s*\"([^\"]+)\"").matcher(yml);
        assertTrue(m.find());
        List<String> prefixes = List.of(m.group(1).split(","));
        assertTrue(prefixes.contains("stones"), "the stones. self-tests are not on the smoke list");
        assertTrue(m.group(1).endsWith(",scaling,boss.rework1,boss.rework3,boss.stats,tier"), "the tail of the list must stay as it was");
        String tests = code(read("stones/StoneSelfTests.java"));
        for (String t : new String[]{"stones.rules", "stones.defs", "stones.state", "stones.ledger"}) assertTrue(tests.contains("\"" + t + "\""), t);
    }

    @Test
    void everyPerPlayerTableIsDroppedAndNothingScansThePerTick() throws IOException {
        for (Path p : stones()) {
            String c = code(Files.readString(p));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), p + " keeps a static player");
            assertFalse(c.contains("getAllEntities") || c.contains("getChunk(") || c.contains("setBlock") || c.contains("Class.forName("), p + " scans or reflects");
            assertFalse(c.contains("EntityJoinLevelEvent"), p + " listens to every entity join");
        }
        String ev = code(read("stones/StoneEvents.java"));
        for (String e : new String[]{"PlayerLoggedOutEvent", "PlayerChangedDimensionEvent", "LivingDeathEvent", "ServerStoppingEvent", "PlayerRespawnEvent"}) assertTrue(ev.contains(e), "tables are not dropped on " + e);
        assertTrue(ev.contains("% 20 != 0"), "the slot pass runs once a second");
        assertTrue(ev.contains("PerfBudgets.effectPacket("), "the ambient effects go through the perf budget");
        String state = code(read("stones/StoneState.java"));
        assertTrue(state.contains("MAX_ZONES") && state.contains("SAMPLES"), "the tables are bounded");
        String acts = code(read("stones/StoneActs.java"));
        assertTrue(acts.contains("MOVE_CAP") && acts.contains("beginControl(") && acts.contains("controlTicks("), "movement and control go through the caps and the balance law");
        assertFalse(acts.contains("addFreshEntity") || acts.contains(".summon("), "no slot ability spawns anything");
        assertTrue(acts.contains("StoneRules.recoil("), "the recoil is the never-lethal one");
        assertTrue(acts.contains("UltimateClass.GOD") && !acts.contains("Fx.damage("), "stone damage is the GOD class of the balance law (x36), not the unclassified x72");
    }

    @Test
    void noSlotActControlsAMind() throws IOException {
        String kit = code(read("stones/StoneKit.java")).toLowerCase();
        for (String bad : new String[]{"dominion", "dominate", "charm(", "mind control", "mindcontrol", "puppet", "ally"}) assertFalse(kit.contains(bad), "a slot act is named or built as '" + bad + "'");
        String acts = code(read("stones/StoneActs.java"));
        assertFalse(acts.contains("tame(") || acts.contains("setOwnerUUID") || acts.contains("dominate("), "a slot act turns a will");
        assertTrue(kit.contains("psionic lance"), "Dominion was replaced by the Psionic Lance");
        String lang = Files.readString(Path.of("tools/lang/infinity_stones.json")).toLowerCase();
        assertFalse(lang.contains("dominion") || lang.contains("mind control"), "no lang line for mind control");
    }

    @Test
    void everyTextHasItsLangLine() throws IOException {
        JsonObject frag = JsonParser.parseString(Files.readString(Path.of("tools/lang/infinity_stones.json"))).getAsJsonObject();
        List<String> keys = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:message|curios)\\.[a-z_.]+)\"").matcher(read("stones/StoneEvents.java"));
        while (m.find()) keys.add(m.group(1));
        assertTrue(keys.size() >= 6);
        for (String k : keys) if (!k.endsWith(".whisper.")) assertTrue(frag.has(k) || k.endsWith(".whisper"), "tools/lang/infinity_stones.json lacks " + k);
        for (InfinityStone s : InfinityStone.values())
            for (int n = 1; n <= 3; n++) assertTrue(frag.has("message.anime_multiverse.stone.whisper." + s.id() + "." + n), "no whisper " + s.id() + " " + n);
        assertTrue(frag.has("curios.identifier.infinity_stone"));
        String ev = read("stones/StoneEvents.java");
        for (InfinityStone s : InfinityStone.values())
            for (int n = 0; n < 3; n++) assertTrue(ev.contains(frag.get("message.anime_multiverse.stone.whisper." + s.id() + "." + (n + 1)).getAsString()), "the code fallback of the whisper " + s.id() + " " + (n + 1) + " differs from the lang line");
    }
}
