package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import com.newuniverse.nusmp.vfx.client.layer.ManaZoneGeometry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The seams of the Mana Zone: the numbers the client layer repeats are the server's (parity), the Avatar and the old entry points cannot open the technique early,
 * the shared files were only appended to, and every text the code builds has its line in the lang fragment.
 */
class ManaZoneWiringTest {
    private static String src(String p) throws Exception { return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + p)); }

    @Test
    void theClientLayersNumbersAreTheServers() {
        for (GrandZoneRegion.Theme t : GrandZoneRegion.Theme.values()) {
            int expected = switch (t) {
                case SEABED -> ManaZoneGeometry.THEME_SEABED;
                case FOREST -> ManaZoneGeometry.THEME_FOREST;
                case VOLCANO -> ManaZoneGeometry.THEME_VOLCANO;
                case GRAVITO -> ManaZoneGeometry.THEME_GRAVITO;
                case HAVEN -> ManaZoneGeometry.THEME_HAVEN;
                case WILD -> ManaZoneGeometry.THEME_WILD;
            };
            assertEquals(expected, t.ordinal(), "theme " + t);
        }
        assertEquals(ManaZoneGeometry.FX_GRAVITY, GrandZoneRegion.Effect.CHAOTIC_GRAVITY.bit());
        assertEquals(ManaZoneGeometry.FX_BOULDERS, GrandZoneRegion.Effect.FLOATING_BOULDERS.bit());
        assertEquals(ManaZoneGeometry.FX_CURRENTS, GrandZoneRegion.Effect.VIOLENT_CURRENTS.bit());
        assertEquals(ManaZoneGeometry.FX_MAGMA, GrandZoneRegion.Effect.MAGMA_HEAT.bit());
        assertEquals(ManaZoneGeometry.FX_STORM, GrandZoneRegion.Effect.MANA_STORM.bit());
        assertEquals(ManaZoneGeometry.FX_WITCH, GrandZoneRegion.Effect.WITCH_GLOW.bit());
        assertEquals(ManaZoneGeometry.FX_CALM, GrandZoneRegion.Effect.CALM.bit());
        assertEquals(ManaZoneGeometry.FX_PRESSURE, GrandZoneRegion.Effect.PRESSURE.bit());
        assertEquals(ManaZoneGeometry.STYLE_FLAME, ManaZoneStyle.of("FLAME"));
        assertEquals(ManaZoneGeometry.STYLE_WATER, ManaZoneStyle.of("water"));
        assertEquals(ManaZoneGeometry.STYLE_WIND, ManaZoneStyle.of("WIND"));
        assertEquals(ManaZoneGeometry.STYLE_EARTH, ManaZoneStyle.of("EARTH"));
        assertEquals(ManaZoneGeometry.STYLE_LIGHT, ManaZoneStyle.of("LIGHT"));
        assertEquals(ManaZoneGeometry.STYLE_DARK, ManaZoneStyle.of("DARKNESS"));
        assertEquals(ManaZoneGeometry.STYLE_SPACE, ManaZoneStyle.of("SPACE"));
        assertEquals(ManaZoneGeometry.STYLE_TIME, ManaZoneStyle.of("TIME"));
        assertEquals(ManaZoneGeometry.STYLE_ARCANE, ManaZoneStyle.of("BATTLE"));
        assertEquals(ManaZoneGeometry.STYLE_ARCANE, ManaZoneStyle.of(null));
        for (double x = -0.5; x <= 1.5; x += 0.0625) assertEquals((float) ManaZoneMath.ease(x), ManaZoneGeometry.ease((float) x), 1e-5f, "ease at " + x);
        for (double d : new double[]{0, 10, 30, 31, 59, 61, 100, 119, 121, 300, 335, 337, 1e6}) {
            boolean culled = ManaZoneMath.lodSegments(d, 30, 40, 8) == 0;
            assertEquals(culled, ManaZoneGeometry.level(d, 30, 1f) < 0, "cull at " + d);
        }
        assertTrue(ManaZoneGeometry.level(5, 30, 1f) == 0 && ManaZoneGeometry.level(50, 30, 1f) == 1 && ManaZoneGeometry.level(100, 30, 1f) == 2);
        assertEquals(1, ManaZoneGeometry.level(5, 30, 0.5f), "Decreased particles drop a level");
    }

    @Test
    void anAvatarNeverCastsTheTechnique() {
        for (String id : new String[]{"mana_zone", "zone_omni_volley", "zone_pressure_space", "true_magic_rune_array"})
            assertFalse(AvatarSkillList.allowedPage(id, 6.0), id);
    }

    @Test
    void theOldEntryPointsAreGatedAndTheGrimoireCannotListThePagesEarly() throws Exception {
        String law = src("mana/ManaLaw.java");
        assertTrue(law.contains("ManaZoneManager.toggle(p)") && law.contains("ManaZoneManager.active(p)") && law.contains("ManaZoneManager.startForced(p, ticks)"), "ManaLaw delegates to the manager");
        assertFalse(law.contains("getLong(\"ZoneUntil\")"), "the 1.5.0 zone timer is no longer read");
        String book = src("book/GrimoireBook.java");
        int usable = book.indexOf("public boolean usable(");
        int gate = book.indexOf("isZoneGated(mode)) return com.newuniverse.nusmp.mana.zone.ManaZoneManager.gateOpenFor(e)", usable);
        int instant = book.indexOf("if (instant(mode)) return true;", usable);
        assertTrue(usable > 0 && gate > usable && gate < instant, "the gate is asked before the instant-page rule that used to open the Mana Zone to everyone");
        int volley = book.indexOf("ManaZoneActions.volleyPage()"), space = book.indexOf("ManaZoneActions.spacePage()"), truth = book.indexOf("TrueMagic.page()"), sig = book.indexOf("SignatureSpell.pages()");
        assertTrue(sig > 0 && volley > sig && space > volley && truth > space, "the new pages are appended after everything else");
        assertTrue(book.contains("page(mode).id().equals(ZONE_ID)") || book.contains("pg.id().equals(ZONE_ID)"));
    }

    @Test
    void theSharedFilesOnlyGainedHooks() throws Exception {
        String shapes = src("vfx/VfxShape.java");
        int tail = shapes.indexOf("MUCUS_FX3,");
        assertTrue(tail > 0, "MUCUS_FX3 is no longer the last entry: the new shapes follow it");
        List<String> order = List.of("MANA_ZONE_SHELL", "MANA_ZONE_GROUND", "MANA_ZONE_BURST", "MANA_ZONE_BUBBLE", "GRAND_ZONE_ENV");
        int at = tail;
        for (String s : order) {
            int i = shapes.indexOf(s, at);
            assertTrue(i > at, s + " is appended in order after MUCUS_FX3");
            at = i;
        }
        assertTrue(shapes.indexOf("GRAND_ZONE_ENV;") > 0, "the enum ends with the new shapes");
        assertTrue(src("vfx/client/VfxManager.java").contains("new com.newuniverse.nusmp.vfx.client.layer.ManaZoneLayer()"));
        assertTrue(src("NUSMP.java").contains("ManaZoneBoot.init(modEventBus, modContainer)"));
        assertTrue(src("multiverse/MultiverseCommands.java").contains("GrandZoneCommands.build()"));
    }

    @Test
    void noTextInTheTechniqueTouchesHunger() throws Exception {
        for (String f : new String[]{"mana/zone/ManaZoneManager.java", "mana/zone/GrandZones.java", "mana/zone/ManaZoneActions.java", "mana/zone/ManaZonePlayer.java", "mana/zone/TrueMagic.java"}) {
            String s = src(f);
            assertFalse(s.contains("MobEffects.HUNGER"), f);
            assertFalse(s.contains("causeFoodExhaustion"), f);
            assertFalse(s.contains("getFoodData().setFoodLevel"), f);
            assertFalse(s.contains("getFoodData().addExhaustion"), f);
        }
    }

    @Test
    void everyTextTheCodeBuildsIsInTheLangFragment() throws Exception {
        String frag = Files.readString(Path.of("tools/lang/mana_zone.json"));
        List<String> keys = new ArrayList<>();
        Pattern p = Pattern.compile("translatableWithFallback\\(\"([a-z_.]+)\"");
        for (String f : new String[]{"mana/zone/ManaZoneManager.java", "mana/zone/GrandZones.java"}) {
            Matcher m = p.matcher(src(f));
            while (m.find()) keys.add(m.group(1));
        }
        assertTrue(keys.size() >= 8, "the code builds its texts with a fallback: " + keys);
        for (String k : keys) if (!k.endsWith(".")) assertTrue(frag.contains("\"" + k + "\""), "lang fragment lacks " + k);
        for (String e : new String[]{"cancel", "collapse", "expired", "death", "dimension", "mount", "logout", "admin"}) assertTrue(frag.contains("\"nusmp.mana_zone.end." + e + "\""), "end text for " + e);
        for (String k : new String[]{"haven", "crushing", "heavy", "pressing", "fed", "balanced"}) assertTrue(frag.contains("\"nusmp.zone.feel." + k + "\""), k);
    }

    @Test
    void theHandOverDocumentExistsAndNamesWhatItReplaced() throws Exception {
        String doc = Files.readString(Path.of("docs/mana/mana_zone_redesign.md"));
        for (String must : new String[]{"NEW", "REPLACED", "KEPT", "could not be verified", "not reachable", "ManaZoneGate", "mana.zone_gate", "mana.zone_core"})
            assertTrue(doc.contains(must), "the doc mentions " + must);
    }
}
