package com.newuniverse.nusmp.core.xform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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
 * Why: the guard's registrations name ids and flags that live in other classes; nobody can run the game here, so the source is read: every modifier id the guard sweeps really is applied somewhere,
 * the guard runs at the right moments (death after the systems, clone and respawn before them), the dead-body guards of the Saiyan and Beast Will systems stay, the setting exists with its default,
 * and the guard stays server-only and tick-free.
 */
class TransformGuardSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static String code(String rel) throws IOException {
        return read(rel).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    private static List<String> allSources() throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(SRC)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java") && !f.toString().replace('\\', '/').contains("core/xform")).toList()) out.add(Files.readString(p));
        }
        return out;
    }

    @Test
    void everyModifierTheGuardSweepsIsReallyAppliedBySomeSystem() throws IOException {
        List<String> sources = allSources();
        Matcher m = Pattern.compile("modifier\\([^,]+,\\s*id\\(\"([^\"]+)\"\\)\\)").matcher(read("core/xform/FormHooks.java"));
        int n = 0;
        List<String> orphans = new ArrayList<>();
        while (m.find()) {
            n++;
            String path = m.group(1);
            String token = path.contains("/") ? path.substring(path.indexOf('/') + 1) : path.startsWith("unite_") ? path.substring("unite_".length()) : path;
            boolean found = false;
            for (String s : sources) if (s.contains("\"" + token + "\"")) { found = true; break; }
            if (!found) orphans.add(path);
        }
        assertTrue(n >= 15, "the guard registers the form modifiers of the Saiyan, Black Form, armour and Unite systems, found " + n);
        assertTrue(orphans.isEmpty(), "a swept modifier id that no system applies (renamed?): " + orphans);
    }

    @Test
    void everyFlagTheGuardStripsIsAConstantOrALiteralOfASystem() throws IOException {
        String f = code("core/xform/FormHooks.java");
        for (String c : new String[]{"DevilFormLimit.SINCE", "DevilFormLimit.AFTER", "NoelleFormState.AFTER", "ArmourState.KEY", "ModeArmor.KEY", "ModeArmor.UNTIL", "ModeArmor.YUNO", "UniteState.KIND", "UniteState.UNTIL", "UniteState.OLD"}) {
            assertTrue(f.contains("TransformGuard.key(" + c + ")"), c + " must be registered");
        }
        for (String lit : new String[]{"nusmp_devil_until", "nusmp_devil_src"}) {
            boolean found = false;
            for (String s : allSources()) if (s.contains("\"" + lit + "\"")) { found = true; break; }
            assertTrue(found, lit + " is not a flag of any system any more");
            assertTrue(f.contains("\"" + lit + "\""), lit + " must be registered");
        }
        assertFalse(f.contains("nusmp_devil_paid"), "the devil coat's debt timer is not a form flag and must survive a death");
    }

    @Test
    void thePermanentPricesAndRacialBonusesAreNeverSwept() throws IOException {
        String f = code("core/xform/FormHooks.java");
        for (String keep : new String[]{"devil_price_hp", "rewind_price_hp", "saiyan/speed", "saiyan/ki_capacity", "saiyan/ki_base", "saiyan/race_", "bw/", "devil_"})
            assertFalse(f.contains("\"" + keep), keep + " is not a form and must survive a death");
    }

    @Test
    void theGuardRunsAtTheRightMomentsAndHoldsNoTickWork() throws IOException {
        String g = code("core/xform/TransformGuard.java");
        assertTrue(g.contains("EventPriority.LOWEST, TransformGuard::onDeath"), "death: after every system's own handler");
        assertTrue(g.contains("EventPriority.HIGHEST, TransformGuard::onClone") && g.contains("EventPriority.HIGHEST, TransformGuard::onRespawn"), "clone and respawn: before the systems rebuild");
        assertTrue(g.contains("NUConfig.transformResetOnDeath()"), "the setting is read live");
        assertFalse(g.contains("PlayerTickEvent") || g.contains("ServerTickEvent"), "nothing runs per tick");
        assertFalse(g.contains("net.minecraft.client") || g.contains("com.newuniverse.nusmp.client"), "server code only");
        assertTrue(code("NUSMP.java").contains("TransformGuard.register(NeoForge.EVENT_BUS)"), "the guard is registered");
        assertTrue(code("NUSMP.java").contains("XformGuardSelfTests.register()"), "the self-test is registered");
    }

    @Test
    void theSettingExistsWithItsDefaultOn() throws IOException {
        String c = code("NUConfig.java");
        assertTrue(c.contains("b.push(\"transformations\")") && c.contains(".define(\"resetOnDeath\", true)"), "transformations.resetOnDeath, default true");
        assertTrue(c.contains("public static boolean transformResetOnDeath()"));
    }

    @Test
    void aDeadBodyIsNotUpkeptAndCannotStartAForm() throws IOException {
        assertTrue(code("saiyan/SaiyanManager.java").contains("if (!p.isAlive()) return;"), "the Saiyan tick skips a dead body");
        assertTrue(code("saiyan/SaiyanNet.java").contains("if (!p.isAlive()) return;"), "a Saiyan key pressed on the death screen is dropped");
        assertTrue(code("beastwill/BeastWillNet.java").contains("!p.isAlive()"), "a Beast Will key pressed on the death screen is dropped");
        assertTrue(code("beastwill/BeastWillManager.java").contains("s == null || !p.isAlive()"), "the Beast Will tick skips a dead body");
        assertTrue(code("saiyan/SaiyanManager.java").contains("endLook(p, RUN.remove(p.getUUID()))"), "a dropped Saiyan record ends its look on the clients");
    }

    @Test
    void clearAllSendsOnlyToTheTrackingPlayersAndOnlyForRunningLayers() throws IOException {
        String a = code("aura/PlayerAuras.java");
        int at = a.indexOf("public static int clearAll(");
        assertTrue(at > 0, "PlayerAuras.clearAll exists");
        String body = a.substring(at, a.indexOf("public static boolean active(", at));
        assertTrue(body.contains("sendToPlayersTrackingEntityAndSelf") && !body.contains("sendToAllPlayers"), "never a broadcast to everyone");
        assertTrue(body.contains("now >= en.getValue()[1]"), "a lease that already ran out costs no packet");
    }
}
