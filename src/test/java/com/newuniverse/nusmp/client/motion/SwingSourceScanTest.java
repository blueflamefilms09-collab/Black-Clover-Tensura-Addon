package com.newuniverse.nusmp.client.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Why: the swing motion (docs/vassal_forms_v2.md) must stay cheap on a public server's clients (rule 11) and look-only (rule 9). The source is scanned for what must stay true: the per-frame
 * paths allocate nothing, the holder state is bounded and cleaned up, the setting exists with the old (rigid) behaviour one switch away, the renderer only moves a mesh that has a valid
 * rig, near the camera, with a holder, and nothing of it is reachable from common (server) code.
 */
class SwingSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException {
        return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8);
    }

    /** The text of the method that starts at the first line containing {@code signature}, up to its closing brace. */
    static String body(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at >= 0, "missing " + signature);
        int open = src.indexOf('{', at);
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        throw new AssertionError("unbalanced " + signature);
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) n++;
        return n;
    }

    @Test
    void theFramePathsAllocateNothing() throws IOException {
        String rt = read("client/motion/SwingRuntime.java");
        String emit = body(rt, "public static void emit(");
        assertFalse(Pattern.compile("\\bnew\\b").matcher(emit).find(), "emit draws a whole mesh per frame: no allocation");
        String prepare = body(rt, "public static boolean prepare(");
        Matcher m = Pattern.compile("\\bnew\\s+([A-Za-z]+)").matcher(prepare);
        int news = 0;
        while (m.find()) {
            news++;
            assertEquals("State", m.group(1), "the only allocation of a frame is a holder's first state");
        }
        assertEquals(1, news);
        String math = Files.readString(SRC.resolve("client/motion/SwingMath.java"), StandardCharsets.UTF_8);
        assertFalse(Pattern.compile("\\bnew\\b").matcher(body(math, "public static void step(")).find(), "the spring step allocates nothing");
        assertTrue(rt.contains("private static final Matrix4f[] MAT") && rt.contains("private static final Matrix3f[] NRM"), "the bone matrices are pre-allocated");
    }

    @Test
    void theHolderStateIsBoundedAndCleanedUp() throws IOException {
        String rt = read("client/motion/SwingRuntime.java");
        assertTrue(rt.contains("MAX_HOLDERS = 48") && rt.contains("map.size() >= MAX_HOLDERS"), "a cap on tracked holders");
        assertTrue(rt.contains("KEEP_TICKS = 40") && rt.contains("it.remove()"), "stale entries are dropped");
        assertTrue(rt.contains("ClientTickEvent.Post") && rt.contains("now % 20L"), "the cleanup runs every 20th tick, not every frame");
        assertTrue(rt.contains("ClientPlayerNetworkEvent.LoggingOut"), "everything is dropped on logout");
        assertTrue(rt.contains("UUID") && !rt.contains("Minecraft.getInstance().player.getUUID"), "state is keyed by the holder's UUID, never 'the current player'");
    }

    @Test
    void farAwayAndHolderlessWeaponsAreRigid() throws IOException {
        String prepare = body(read("client/motion/SwingRuntime.java"), "public static boolean prepare(");
        assertTrue(prepare.contains("holder == null"), "no holder (ground, frame, menu): rigid");
        assertTrue(prepare.contains("SwingMath.near("), "beyond the level-of-detail distance: rigid");
        assertTrue(prepare.contains("!enabled()"), "the setting off: rigid");
        assertTrue(prepare.indexOf("SwingMath.near(") < prepare.indexOf("map.get(id)"), "the distance test comes before any state is touched");
    }

    @Test
    void theSettingExistsAndIsOnByDefault() throws IOException {
        String cfg = read("client/heroweapon/HeroClientConfig.java");
        assertTrue(cfg.contains("WEAPON_MOTION") && cfg.contains(".define(\"weaponMotion\", true)"), "client config models.weaponMotion, default on");
        assertTrue(cfg.contains(".define(\"classic\", false)"), "client config vassalForms.classic, default off");
        assertTrue(cfg.contains("public static boolean weaponMotion()") && cfg.contains("public static boolean classicVassalForms()"));
    }

    @Test
    void theRendererMovesOnlyAValidRigAndKeepsTheRigidPath() throws IOException {
        String wr = read("client/WeaponRenderer.java");
        assertTrue(wr.contains("mesh.rig != null && com.newuniverse.nusmp.client.motion.SwingRuntime.prepare("), "prepare is only asked for a mesh that has a rig");
        assertTrue(wr.contains("HolderTracker.holderFor(ctx)"), "the holder comes from the tracker");
        assertTrue(wr.contains("!mesh.rig.valid(counts)"), "an invalid rig is dropped at load");
        assertTrue(wr.contains("Arrays.equals(probe, probeVertex(mesh))"), "a mesh the loader moved is drawn rigid");
        assertEquals(1, count(wr, "swingNow = skinned ? mesh.rig : null;"));
        assertTrue(wr.contains("swingNow = null;"), "the rig is released at the end of a draw");
        assertTrue(wr.contains("case \"bone\"") && wr.contains("case \"skin\""));
        assertTrue(wr.contains("isRemodelledVassalForm(id)") && wr.contains("weapon_meshes/classic_vassal/"), "the old axe / book / carriage forms are one setting away");
        assertTrue(wr.contains("SwingRuntime.init()"), "the cleanup tick is registered");
    }

    @Test
    void nothingOfItIsReachableFromServerCode() throws IOException {
        try (var s = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/")) continue;
                String text = Files.readString(p, StandardCharsets.UTF_8);
                assertFalse(text.contains("client.motion"), rel + " reaches into client.motion");
            }
        }
    }
}
