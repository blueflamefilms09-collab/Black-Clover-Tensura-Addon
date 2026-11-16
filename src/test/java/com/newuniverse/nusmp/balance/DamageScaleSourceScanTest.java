package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the damage scale-down cannot be played here, so its wiring is pinned by reading the source: the settings are SERVER settings in their own file with the requested keys, the balance law's cap
 * and the damage event both use the ceilings, the Extreme Magic hit sets and clears its scope in a finally and uses the element's damage type, the pure classes name no game class, the event runs last,
 * nothing is registered on the client side, and the doc exists. Pure (file reads); the working directory is the project root.
 */
class DamageScaleSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    @Test
    void theSettingsAreServerSettingsWithTheRequestedKeys() throws IOException {
        String cfg = code("balance/DamageScaleConfig.java");
        for (String key : new String[]{"scaleDownEnabled", "weaponHitCap", "globalHitCap", "djinnMin", "djinnMax", "djinnFloor", "apexWeaponId", "apexHitCap", "extremeElementTypes"})
            assertTrue(cfg.contains("\"" + key + "\""), key);
        assertTrue(cfg.contains("b.push(\"damage\")"));
        String main = code("NUSMP.java");
        assertTrue(main.contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.balance.DamageScaleConfig.SPEC, \"nusmp-damage-server.toml\""));
        assertTrue(main.contains("DamageScaleEvents.register(NeoForge.EVENT_BUS)") && main.contains("DamageScaleSelfTests.register()") && main.contains("RankTooltip.register(NeoForge.EVENT_BUS)"));
    }

    @Test
    void theBalanceLawAndTheEventBothUseTheCeilings() throws IOException {
        String law = code("balance/BalanceLaw.java");
        assertTrue(law.contains("DamageScaleConfig.softCeiling()"), "BalanceLaw.cap must apply the widest ceiling");
        String ev = code("balance/DamageScaleEvents.java");
        assertTrue(ev.contains("EventPriority.LOWEST"), "the cap says the last word on a hit");
        assertTrue(ev.contains("DamageScale.apply(") && ev.contains("LivingIncomingDamageEvent"));
        assertFalse(ev.contains("net.minecraft.client") || ev.contains("Minecraft.getInstance"), "server side only");
        assertFalse(ev.contains("getEntitiesOfClass"), "no world scan inside a damage event");
    }

    @Test
    void theExtremeMagicHitUsesItsElementAndClearsItsScopeInAFinally() throws IOException {
        String magic = code("relics/djinn/DjinnMagic.java");
        int begin = magic.indexOf("DamageScale.beginExtreme(");
        int fin = magic.indexOf("finally", begin);
        int end = magic.indexOf("DamageScale.endExtreme()", fin);
        assertTrue(begin > 0 && fin > begin && end > fin, "beginExtreme ... finally { endExtreme }");
        assertTrue(magic.contains("DjinnExtremeTypes.typeId("));
    }

    @Test
    void thePureClassesNameNoGameClass() throws IOException {
        for (String rel : new String[]{"balance/DamageScale.java", "relics/Rankings.java", "relics/djinn/DjinnExtremeTypes.java"}) {
            String c = code(rel);
            assertFalse(c.contains("import net.minecraft") || c.contains("import net.neoforged"), rel + " must stay pure");
        }
    }

    @Test
    void theHardMaximumIsOneConstant() throws IOException {
        String c = code("balance/DamageScale.java");
        assertTrue(c.contains("ABSOLUTE_MAX = 15_000.0"));
        assertTrue(Files.exists(Path.of("docs/damage_scale_down.md")) && Files.exists(Path.of("docs/rankings.md")));
    }
}
