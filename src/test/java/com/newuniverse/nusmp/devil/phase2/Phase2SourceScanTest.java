package com.newuniverse.nusmp.devil.phase2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the contract rules of phase 2 must stay server friendly and cheap (CLAUDE.md rules 9 and 11); the properties that keep them so are pinned by reading the source (the package {@code devil.phase2} is outside
 * the older scans). Pure (file reads); the working directory is the project root.
 */
class Phase2SourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = ROOT.resolve("devil/phase2");

    private static String code(Path file) throws IOException {
        String s = Files.readString(file, StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static List<Path> files() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DIR)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(out::add); }
        return out;
    }

    @Test
    void noClientClassNoMinecraftInstance() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        assertFalse(files().isEmpty());
        for (Path f : files()) assertFalse(client.matcher(code(f)).find(), f.getFileName() + " names a client-only class");
    }

    @Test
    void noStaticPlayerNoMutableStaticCollectionNoWorldScan() throws IOException {
        for (Path f : files()) {
            String c = code(f), n = f.getFileName().toString();
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), n + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;={}(]*(?:Map|List|Set)<[^;=(){}]*>\\s+\\w+\\s*[=;]").matcher(c).find(), n + " keeps a static collection: state is per player UUID in the ledger");
            assertFalse(Pattern.compile("getEntitiesOfClass|getEntities\\(|getNearbyEntities|getChunk\\(|forceChunk|setChunkForced|\\.setBlock\\(|setBlockAndUpdate|\\.explode\\(|Math\\.random|getAllEntities").matcher(c).find(),
                    n + " scans the world, loads chunks or uses unseeded randomness");
            assertFalse(c.contains("TickEvent"), n + " has tick work of its own: the rules run at events only");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload"), n + " sends a packet of its own: chat lines and the existing contract sync only");
            if (!n.equals("Trials.java")) assertFalse(c.contains("addFreshEntity"), n + " spawns an entity: only Trials may (one per player, capped by trial.max)");
        }
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : List.of("Phase2Rules.java", "Phase2Table.java"))
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|com\\.mojang)").matcher(code(DIR.resolve(f))).find(), f + " must stay pure (it is unit tested without a game)");
    }

    @Test
    void theTrialIsCappedAndTimed() throws IOException {
        String t = code(DIR.resolve("Trials.java"));
        assertTrue(t.contains("Phase2Config.trialMax()"), "the number of trials on the server is capped");
        assertTrue(code(DIR.resolve("DevilTrialEntity.java")).contains("Trials.check(this)"), "the trial devil is checked (caller gone, far, out of time)");
        assertTrue(t.contains("trialTicks()"), "a trial runs out");
        assertTrue(code(DIR.resolve("DevilTrialEntity.java")).contains("shouldBeSaved() { return false; }"), "the trial devil is never saved");
    }

    @Test
    void theLedgerIsBoundedAndPerPlayerUuid() throws IOException {
        String l = code(DIR.resolve("DevilLedger.java"));
        for (String must : new String[] {"MAX_PLAYERS", "MAX_PACTS", "MAX_BANS", "UUID"}) assertTrue(l.contains(must), "the ledger lacks " + must);
        assertTrue(l.contains("nusmp_devil_phase2"), "its own saved-data name (not the grimoire pact table)");
    }

    @Test
    void everyRuleHasASettingThatRestoresTheOldBehaviour() throws IOException {
        String c = code(DIR.resolve("Phase2Config.java"));
        for (String must : new String[] {"push(\"summon\")", "\"betrayal\"", "push(\"ep\")", "push(\"trial\")", "push(\"kinds\")", "\"confirm\"", "push(\"stack\")", "push(\"shared\")", "push(\"sever\")", "push(\"sabotage\")", "\"pactDevilsOneOfOne\"", "\"sameDayTripleHook\"", "\"enabled\""})
            assertTrue(c.contains(must), "no setting for " + must);
        assertTrue(code(ROOT.resolve("devil/DevilBoot.java")).contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.devil.phase2.Phase2Config.SPEC, \"nusmp-devil-phase2-server.toml\""), "gameplay settings are a server config with their own file name");
    }

    @Test
    void everyPhase2TextHasItsEnglishInTheLangFile() throws IOException {
        String lang = Files.readString(Path.of("").toAbsolutePath().resolve("src/main/resources/assets/anime_multiverse/lang/en_us.json"), StandardCharsets.UTF_8);
        Pattern call = Pattern.compile("Phase2Text\\.t\\(\\s*\"([a-z0-9_.]+)\"\\s*,\\s*\"");
        int n = 0;
        for (Path f : files()) {
            Matcher m = call.matcher(code(f));
            while (m.find()) {
                n++;
                assertTrue(lang.contains("\"anime_multiverse.devil.phase2." + m.group(1) + "\""), f.getFileName() + " says '" + m.group(1) + "' but en_us.json has no such line (run tools/lang/gen_devil_phase2.py)");
            }
        }
        assertTrue(n > 40, "the scan found only " + n + " texts");
    }

    @Test
    void everyPhase2TextCallHasALiteralKeyAndALiteralEnglish() throws IOException {
        Pattern any = Pattern.compile("Phase2Text\\.t\\(");
        Pattern good = Pattern.compile("Phase2Text\\.t\\(\\s*\"[a-z0-9_.]+\"\\s*,\\s*\"");
        for (Path f : files()) {
            if (f.getFileName().toString().equals("Phase2Text.java")) continue;
            String c = code(f);
            Matcher a = any.matcher(c), g = good.matcher(c);
            int na = 0, ng = 0;
            while (a.find()) na++;
            while (g.find()) ng++;
            assertTrue(na == ng, f.getFileName() + " has a Phase2Text.t call with a computed key or English (the lang generator can not read it)");
        }
    }
}
