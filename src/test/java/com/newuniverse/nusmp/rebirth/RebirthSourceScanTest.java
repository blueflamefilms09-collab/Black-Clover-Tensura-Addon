package com.newuniverse.nusmp.rebirth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The rebirth code stays server-safe: no client class, per-player state keyed by UUID and cleared on logout / death / dimension change / server stop, one master setting, the hooks in place. */
class RebirthSourceScanTest {
    private static final Path DIR = Path.of("src/main/java/com/newuniverse/nusmp/rebirth");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static List<Path> sources() throws IOException {
        try (Stream<Path> w = Files.list(DIR)) { return w.filter(x -> x.toString().endsWith(".java")).toList(); }
    }

    @Test
    void noClientClassAndNoStaticCurrentPlayer() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : sources()) {
            for (String line : read(p).split("\n")) {
                String t = line.strip();
                if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
                if (t.contains("net.minecraft.client") || t.contains("com.mojang.blaze3d") || t.contains("com.newuniverse.nusmp.client") || t.contains("Minecraft.getInstance")
                        || t.contains("ParticleTypes.") || t.contains("net.neoforged.api.distmarker") || t.contains("Dist.CLIENT"))
                    bad.add(p.getFileName() + ": " + t);
                if (t.matches(".*static\\s+(?:final\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=].*")) bad.add(p.getFileName() + " keeps a static player: " + t);
            }
        }
        assertTrue(bad.isEmpty(), String.valueOf(bad));
    }

    @Test
    void everyStaticMapIsKeyedByUuid() throws IOException {
        int maps = 0;
        for (Path p : sources()) {
            for (String line : read(p).split("\n")) {
                String t = line.strip();
                if (t.startsWith("*") || t.startsWith("//")) continue;
                if (t.matches(".*static\\b.*\\bMap<.*")) {
                    maps++;
                    assertTrue(t.contains("Map<UUID,"), p.getFileName() + " has a static map not keyed by UUID: " + t);
                }
            }
        }
        assertEquals(1, maps, "the feature keeps exactly one small map (RebirthState)");
    }

    @Test
    void theMapIsClearedOnLogoutDeathDimensionAndStop() throws IOException {
        String boot = read(DIR.resolve("RebirthBoot.java"));
        for (String ev : new String[]{"PlayerLoggedOutEvent", "LivingDeathEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"})
            assertTrue(boot.contains(ev), "RebirthBoot does not listen to " + ev);
        assertTrue(boot.contains("RebirthState.forget(p.getUUID())") && boot.contains("RebirthState.clearAll()"));
        assertTrue(boot.contains("ModConfig.Type.SERVER") && boot.contains("\"nusmp-rebirth-server.toml\""), "gameplay settings are a SERVER config");
        assertFalse(boot.contains("ModConfig.Type.CLIENT"));
    }

    @Test
    void theMasterSettingAndTheServiceRunOnTheServerOnly() throws IOException {
        assertTrue(read(DIR.resolve("RebirthConfig.java")).contains("define(\"rebirth\", true)"), "master setting 'rebirth' default true");
        assertTrue(read(DIR.resolve("RebirthConfig.java")).matches("(?s).*TWO_BOOKS_ON_FUSION = .*define\\(\"twoBooksOnFusion\", false\\).*"), "twoBooksOnFusion defaults to false");
        String item = read(DIR.resolve("SigilItem.java"));
        assertTrue(item.contains("level.isClientSide") && item.contains("RebirthService.use"), "the item decides nothing on the client");
        String svc = read(DIR.resolve("RebirthService.java"));
        assertTrue(svc.contains("RebirthConfig.enabled()") && svc.contains("RebirthRules.verdict") && svc.contains("RebirthRules.confirmed"));
        for (Path p : sources()) assertFalse(read(p).contains("TickEvent"), p.getFileName() + " must not run per tick");
    }

    @Test
    void theWizardHooksAreTheOnlyEditsAndTheCommandStillResets() throws IOException {
        String w = read(Path.of("src/main/java/com/newuniverse/nusmp/wizard/WizardSystem.java"));
        for (String hook : new String[]{"RebirthWizardHooks.pending(p)", "RebirthWizardHooks.poolFor(p, pool)", "RebirthWizardHooks.isSecond(p)", "RebirthWizardHooks.onArrive(p, r)", "RebirthWizardHooks.twinPlan(dual, r, cover)"})
            assertTrue(w.contains(hook), "missing hook " + hook);
        String cmd = read(Path.of("src/main/java/com/newuniverse/nusmp/blackclover/GrimoireCommand.java"));
        assertTrue(cmd.contains("GrimoireErasure.skills(p)"), "the admin reset command shares the erasure");
        String er = read(DIR.resolve("GrimoireErasure.java"));
        assertTrue(er.contains("forgetSkill(id)") && er.contains("markRolled(p, false)"), "the shared erasure keeps what the command always did");
        assertFalse(er.contains("EnderChest"), "the ender chest is never touched");
    }
}
