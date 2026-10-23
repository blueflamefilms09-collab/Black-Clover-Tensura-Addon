package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing of the Djinn-style dive can be profiled here, so its rules are pinned the only way they can be: by reading the real source. (1) Server safety: the common classes name no client class.
 * (2) Frame cost: the painters allocate nothing and build no location or string in their frame methods, hand out no render type of their own, and use the level of detail, the character cap per frame and the
 * probe section. (3) The old looks are still there: the V2 remake and the classic dive are untouched and still reachable by the option. (4) The option, the self-test and the sound switch are registered
 * where they have to be. (5) No hunger.
 */
class SpiritDiveDjinnScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) {
        try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + p, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    // ------------------------------------------------------------------ 1. server safety
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String[] COMMON = {"book/SpiritDiveDjinnPieces.java", "book/SpiritDiveForge.java", "book/SpiritDiveSound.java"};

    @Test
    void theCommonClassesOfTheDjinnLookNameNoClientClass() {
        List<String> bad = new ArrayList<>();
        for (String f : COMMON) if (CLIENT.matcher(code(read(JAVA.resolve(f)))).find()) bad.add(f);
        for (String f : new String[]{"book/SpiritDiveForm.java", "book/SpiritDiveConfig.java", "book/SpiritDiveSelfTests.java"}) if (CLIENT.matcher(code(read(JAVA.resolve(f)))).find()) bad.add(f);
        assertTrue(bad.isEmpty(), "a client class in common code: " + bad);
    }

    @Test
    void thePureClassesTouchNoMinecraftClassAtAllSoTheyRunInAnyTest() {
        for (String f : new String[]{"book/SpiritDiveDjinnPieces.java", "book/SpiritDiveForge.java"})
            assertFalse(code(read(JAVA.resolve(f))).contains("net.minecraft"), f + " must stay pure");
    }

    @Test
    void theSoundIsPerPlayerStateInThePlayersOwnDataAndNoStaticTable() {
        String c = code(read(JAVA.resolve("book/SpiritDiveSound.java"))) + code(read(JAVA.resolve("book/SpiritDiveForge.java")));
        assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:ServerPlayer|Player|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), "a static current player");
        assertFalse(c.contains("HashMap") || c.contains("static final Map"), "the cooldown lives in the player's data, so there is nothing to clean up at logout");
        assertTrue(c.contains("getPersistentData()") && c.contains("COOLDOWN_TICKS"));
        assertTrue(read(JAVA.resolve("book/SpiritDiveConfig.java")).contains("\"forgeSound\""), "the server switch of the sound");
    }

    // ------------------------------------------------------------------ 2. frame cost
    private static final String[] PAINTERS = {"client/aura/SpiritDiveDjinn.java", "client/aura/DiveFirstPerson.java"};
    private static final Pattern METHOD = Pattern.compile("(?m)^[ \\t]{0,8}(?:(?:public|private|protected|static|final)\\s+)+[\\w<>\\[\\],.? ]+?\\s+(\\w+)\\s*\\(([^;{}=]*)\\)\\s*(?:throws [\\w., ]+)?\\{");

    private static List<String[]> methods(String src) {
        String s = code(src).replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
        List<String[]> out = new ArrayList<>();
        Matcher m = METHOD.matcher(s);
        while (m.find()) {
            int depth = 1, i = m.end();
            while (i < s.length() && depth > 0) {
                char ch = s.charAt(i++);
                if (ch == '{') depth++;
                else if (ch == '}') depth--;
            }
            out.add(new String[]{m.group(1), s.substring(m.end(), i)});
        }
        return out;
    }

    @Test
    void thePaintersAllocateNothingAndBuildNoLocationOrStringInTheirFrameMethods() {
        Pattern alloc = Pattern.compile("new\\s+(?:Vec3|Vector3f|Vector4f|Quaternionf|Matrix4f|Matrix3f|ResourceLocation|ArrayList|HashMap|HashSet|int\\[|float\\[|double\\[|String\\b|StringBuilder|long\\[)"
                + "|ResourceLocation\\.(?:fromNamespaceAndPath|parse|withDefaultNamespace|tryParse)\\(|String\\.format\\(|AuraRender\\.tex\\(|\\.stream\\(\\)|Axis\\.\\w+\\.rotation");
        List<String> bad = new ArrayList<>();
        for (String f : PAINTERS)
            for (String[] m : methods(read(JAVA.resolve(f)))) {
                if (m[0].equals("register") || m[0].startsWith("<")) continue;
                Matcher a = alloc.matcher(m[1]);
                if (a.find()) bad.add(f + " " + m[0] + ": " + a.group());
            }
        assertTrue(bad.isEmpty(), "allocation in a frame method: " + bad);
    }

    @Test
    void thePiecesListIsReadAndNotCopiedPerFrame() {
        String c = code(read(JAVA.resolve("client/aura/SpiritDiveDjinn.java")));
        assertFalse(c.contains(".toArray(") || c.contains("new ArrayList") || c.contains("List.copyOf"), "no copy of the list per frame");
        assertTrue(c.contains("for (int pi = 0, pn = list.size(); pi < pn; pi++)"), "an index loop, no iterator per frame");
    }

    @Test
    void noPainterHandsOutARenderTypeOfItsOwn() {
        for (String f : PAINTERS) {
            String c = code(read(JAVA.resolve(f)));
            assertFalse(c.contains("RenderType.eyes(") || c.contains("entityTranslucentEmissive") || c.contains("RenderType.lightning"), f + " builds an additive type itself (use AuraRender.additive: the SafeAdditive rule)");
        }
        assertTrue(code(read(JAVA.resolve("client/aura/SpiritDiveDjinn.java"))).contains("AuraRender.additive("));
    }

    @Test
    void theDjinnPainterUsesTheDistanceBandsTheCharacterCapAndTheProbe() {
        String c = read(JAVA.resolve("client/aura/SpiritDiveDjinn.java"));
        assertTrue(c.contains("SpiritDiveTier.lod("), "level of detail by distance");
        assertTrue(c.contains("Lod.CULLED"), "culled beyond 64 blocks");
        assertTrue(c.contains("SpiritDiveFrame.admitCharacter()"), "at most three character dives a frame");
        assertTrue(c.contains("SpiritDiveDjinnPieces.drawn(") && c.contains("SpiritDiveDjinnPieces.motes("), "the budget numbers come from the table");
        assertTrue(c.contains("PerfProbe.section(\"c.spiritdive.djinn\")") && c.contains("PerfProbe.begin()") && c.contains("PerfProbe.end("), "the probe section");
        assertTrue(c.contains("EquipAnimation.assembled(") && c.contains("EquipAnimation.begin("), "the Djinn's forge-in arithmetic");
        assertTrue(c.contains("SpiritDiveDjinnPieces.alreadyOn("), "an upgrade does not forge the half dive again");
        assertTrue(c.contains("WaterAuraRig.slim("), "slim arms");
    }

    @Test
    void theFirstPersonGauntletsSkipFuegoleonsFlameArmAndAreSwitchable() {
        String c = read(JAVA.resolve("client/aura/DiveFirstPerson.java"));
        assertTrue(c.contains("Aura.FUEGOLEON_ARM") && c.contains("PlayerAuraClient.has("), "no right-arm plate where the flaming arm is on");
        assertTrue(c.contains("ENABLED") && c.contains("RenderArmEvent") && c.contains("RenderHandEvent"));
        assertTrue(c.contains("SpiritDiveDjinnPieces.Look.DJINN"), "only with the Djinn look");
        assertTrue(read(JAVA.resolve("client/aura/DiveClient.java")).contains("DiveFirstPerson::onRenderArm"), "registered");
    }

    // ------------------------------------------------------------------ 3. the old looks are kept
    @Test
    void theRemakeAndTheClassicLookAreStillThereAndReachableByTheOption() {
        String aura = code(read(JAVA.resolve("client/aura/SpiritDiveAura.java")));
        assertTrue(aura.contains("private static void remadeLod(") && aura.contains("private static void extras(") && aura.contains("shell(c, el, st, pulse, false, false, false)"), "the V2 and classic passes are still in the painter");
        assertTrue(aura.contains("SpiritDiveLook.classic()") && aura.contains("SpiritDiveDjinn.paint("), "the painter chooses between the three looks");
        assertTrue(Files.isRegularFile(JAVA.resolve("client/aura/SpiritDiveFx.java")), "the effects of the remake");
        for (Path p : new Path[]{ROOT.resolve("src/main/resources/assets/nusmp/geo/entity/spiritdive_arm_wind_halo.geo.json"), ROOT.resolve("src/main/resources/assets/nusmp/geo/entity/spiritdive_arm_fire_mane.geo.json"),
                ROOT.resolve("src/main/resources/assets/nusmp/geo/entity/spiritdive_arm_saint_halo.geo.json"), ROOT.resolve("src/main/resources/assets/nusmp/geo/entity/spiritdive_arm_fire_claws_r.geo.json")})
            assertTrue(Files.isRegularFile(p), "the model of an old look is gone: " + p);
        assertEquals(5, SpiritDiveDjinnPieces.Look.values().length, "DJINN, V2, CLASSIC and the appended ANIME and REEL");
    }

    @Test
    void theOptionIsInTheClientConfigWithTheDjinnLookAsDefaultAndTheOldKeyKept() {
        String cfg = code(read(JAVA.resolve("client/aura/DiveClientConfig.java")));
        assertTrue(cfg.contains("defineEnum(\"look\", Look.REEL)"), "look = REEL | ANIME | DJINN | V2 | CLASSIC, default REEL since 3.3");
        assertTrue(cfg.contains("define(\"classicLook\", false)"), "the old key stays");
        assertTrue(cfg.contains("CLASSIC.get() ? Look.CLASSIC : LOOK.get()"), "the old key still wins");
        assertTrue(read(JAVA.resolve("client/aura/DiveClient.java")).contains("ModConfig.Type.CLIENT") && read(JAVA.resolve("client/aura/DiveClient.java")).contains("\"nusmp-spiritdive-client.toml\""), "a CLIENT config");
    }

    @Test
    void theSelfTestIsRegisteredAndTheSoundIsHookedAtTheThreeMoments() {
        assertTrue(read(JAVA.resolve("book/SpiritDiveSelfTests.java")).contains("SelfTests.register(\"magic.spirit_dive_djinn\""));
        String form = read(JAVA.resolve("book/SpiritDiveForm.java"));
        assertEquals(3, count(form, "SpiritDiveSound.play("), "begin, upgrade and end");
        assertTrue(form.contains("SpiritDiveForge.Kind.BEGIN") && form.contains("SpiritDiveForge.Kind.UPGRADE") && form.contains("SpiritDiveForge.Kind.END"));
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }

    // ------------------------------------------------------------------ 4. no hunger, no packets of its own
    @Test
    void theDjinnLookNeverTouchesFoodAndAddsNoPacket() {
        List<String> files = new ArrayList<>(List.of(COMMON));
        files.addAll(List.of(PAINTERS));
        for (String f : files) {
            String c = code(read(JAVA.resolve(f)));
            for (String bad : new String[]{"causeFoodExhaustion", "FoodData", "getFoodData", "setFoodLevel", "addExhaustion", "setSaturation"}) assertFalse(c.contains(bad), f + " touches hunger: " + bad);
            for (String bad : new String[]{"PacketDistributor", "CustomPacketPayload", "registrar.play", "sendToServer", "sendToPlayer"}) assertFalse(c.contains(bad), f + " sends a packet: " + bad);
        }
    }
}
