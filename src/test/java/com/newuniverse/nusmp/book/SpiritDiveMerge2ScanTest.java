package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: merge 2 cannot be profiled here, so its rules are pinned by reading the real source. (1) Server safety: the common classes name no client class, the pure ones no Minecraft class; nothing is sent, decided or
 * kept per player. (2) Frame cost: the draw methods allocate nothing, the first person merge draws only for the local player in first person, only inside the entry clock and only with its option. (3) The hooks are
 * where they have to be (the calm entry in the shared entry and ribbons, the crown in each look that has one, the options and the self-test registration), and the old behaviour stays behind the options.
 */
class SpiritDiveMerge2ScanTest {
    private static final Path JAVA = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    private static String body(String src, String signature) {
        String s = code(src);
        int i = s.indexOf(signature);
        assertTrue(i >= 0, "no method " + signature);
        int open = s.indexOf('{', i), depth = 1, j = open + 1;
        while (j < s.length() && depth > 0) { char ch = s.charAt(j++); if (ch == '{') depth++; else if (ch == '}') depth--; }
        return s.substring(open, j);
    }

    @Test
    void theCommonClassesNameNoClientClassAndTheRulesNoMinecraftClassAtAll() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/SpiritDiveMerge2Rules.java", "book/SpiritDiveMerge2SelfTests.java"})
            if (CLIENT.matcher(code(read(f))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
        assertFalse(code(read("book/SpiritDiveMerge2Rules.java")).contains("net.minecraft"), "the rules stay pure");
    }

    @Test
    void nothingIsSentDecidedOrKeptPerPlayer() {
        for (String f : new String[]{"client/aura/SpiritDiveMerge2.java", "client/aura/SpiritDiveMergeFirstPerson.java", "client/aura/DiveMergeFirstPerson.java", "book/SpiritDiveMerge2Rules.java"}) {
            String c = code(read(f));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|AbstractClientPlayer|LocalPlayer|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), f + ": a static current player");
            assertFalse(c.contains("CustomPacketPayload") || c.contains("PacketDistributor") || c.contains("StreamCodec") || c.contains("ServerLevel") || c.contains("MinecraftServer"), f + ": merge 2 sends and decides nothing");
            assertFalse(c.contains("static final Map") || c.contains("HashMap"), f + ": a table of players");
        }
    }

    @Test
    void theFirstPersonMergeIsForTheLocalPlayerInFirstPersonInsideTheEntryAndBehindItsOption() {
        String h = code(read("client/aura/DiveMergeFirstPerson.java"));
        assertTrue(h.contains("mc.player") && h.contains("isFirstPerson()"), "local player, first person only");
        assertTrue(h.contains("SpiritDiveMerge2.firstPerson()") && h.contains("SpiritDiveMerge.enabled()"), "both options");
        assertTrue(h.contains("Stage.AFTER_PARTICLES") && h.contains("flowing(m)"), "one stage, only while the merge flows");
        assertTrue(h.contains("PerfProbe.begin()") && h.contains("PerfProbe.end("), "timed");
        assertFalse(h.contains("coreMesh") || h.contains("slimCore") || h.contains("inner("), "no inner silhouette in first person");
        String d = code(read("client/aura/SpiritDiveMergeFirstPerson.java"));
        assertFalse(d.contains("slimCore") || d.contains("coreMesh"), "no inner silhouette in first person");
        assertTrue(d.contains("fpExtent") && d.contains("fpAlpha") && d.contains("fpFlashRadius"), "every size goes through the coverage clamp");
        assertFalse(d.contains("Blur") || d.contains("blur") || d.contains("PostChain") || d.contains("ShaderInstance"), "no blur");
    }

    @Test
    void theDrawMethodsAllocateNothing() {
        for (String[] m : new String[][]{{"client/aura/SpiritDiveMergeFirstPerson.java", "static void draw("}, {"client/aura/SpiritDiveMerge2.java", "static void crown("},
                {"client/aura/SpiritDiveMerge2.java", "static void fit("}, {"client/aura/SpiritDiveMerge.java", "static void draw("}, {"client/aura/SpiritDiveMerge.java", "private static void emit("}}) {
            String b = body(read(m[0]), m[1]);
            assertFalse(Pattern.compile("\\bnew\\s+[A-Z]").matcher(b).find(), m[0] + " " + m[1] + ": allocates");
            assertFalse(b.contains("String.format") || b.contains("new ResourceLocation") || b.contains("ResourceLocation.fromNamespaceAndPath"), m[0] + " " + m[1] + ": builds a location or string");
        }
    }

    @Test
    void theHooksAreWhereTheyHaveToBeAndTheOldLooksStayBehindTheOptions() {
        String reel = code(read("client/aura/SpiritDiveReel.java"));
        assertTrue(reel.contains("SpiritDiveMerge2.calmEntry()") && reel.contains("SpiritDiveMerge2Rules.flashAlpha") && reel.contains("SpiritDiveMerge2Rules.ribbonK"), "the calm entry in the shared entry and ribbons");
        assertTrue(reel.contains("SpiritDiveReelRules.reveal(u)"), "the old entry is still there");
        assertTrue(code(read("client/aura/SpiritDiveReelPieces.java")).contains("SpiritDiveMerge2.crownDistinct()"), "REEL crown");
        assertTrue(code(read("client/aura/SpiritDiveReelPieces.java")).contains("block(pose, body, 0f, 0f, 0f, edge"), "the old cube crown is kept");
        assertTrue(code(read("client/aura/SpiritDiveWind4.java")).contains("SpiritDiveMerge2.crownDistinct()") && code(read("client/aura/SpiritDiveWind4.java")).contains("CROWN_YUNO_HALF"), "Wind A crown, old kept");
        for (String f : new String[]{"client/aura/SpiritDiveWind.java", "client/aura/SpiritDiveWind3.java"}) assertTrue(code(read(f)).contains("SpiritDiveMerge2.fit(pose)"), f + " scales its crown");
        assertTrue(code(read("client/aura/SpiritDiveAnime.java")).contains("SpiritDiveMerge2.crownK()"), "the ANIME sprite crown");
        String cfg = code(read("client/aura/DiveClientConfig.java"));
        for (String k : new String[]{"\"mergeModels\"", "\"calmEntry\"", "\"mergeFirstPerson\"", "\"crownDistinct\"", "\"merge\""}) assertTrue(cfg.contains(k), "option " + k);
        assertTrue(cfg.contains("SpiritDiveMerge2.set("), "the options are mirrored");
        assertTrue(code(read("client/aura/DiveClient.java")).contains("DiveMergeFirstPerson::onRender"), "the first person hook is registered");
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("SpiritDiveMerge2SelfTests.register()"), "the self-test is registered");
        assertTrue(code(read("client/aura/SpiritDiveMerge.java")).contains("SpiritDiveMerge2.slim()"), "the models option");
    }
}
