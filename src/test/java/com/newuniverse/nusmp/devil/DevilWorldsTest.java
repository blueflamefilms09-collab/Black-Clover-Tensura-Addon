package com.newuniverse.nusmp.devil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 3.76.0: the devils of the eight worlds, their rituals and what an attribute does in a fight, as pure rules. */
class DevilWorldsTest {
    private static DevilRitualRules.Facts ready() {
        return new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, true, 64, 40, 1.0, false);
    }

    @Test void everyWorldHasDevilsWithUniqueIdsAndAWorkingAttribute() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            assertTrue(ids.add(d.id()), "id twice: " + d.id());
            assertTrue(d.id().matches("[a-z_]+"), d.id());
            assertNotEquals(DevilTier.NONE, d.tier());
            assertTrue(DevilRules.effect(d.attribute()).bonus() > 0, d.id() + " lends an attribute that does nothing");
            assertSame(d, WorldDevil.byId(d.id().toUpperCase()));
        }
        for (DevilWorld w : DevilWorld.values()) assertTrue(WorldDevil.countOf(w) >= 1, w + " has no devil");
        assertNull(WorldDevil.byId("nobody"));
        assertEquals(8, DevilWorld.values().length);
    }

    @Test void everyAttributeButNoneActs() {
        for (DevilAttribute a : DevilAttribute.values()) {
            DevilRules.Effect e = DevilRules.effect(a);
            if (a == DevilAttribute.NONE) assertEquals(0.0, e.bonus());
            else {
                assertTrue(e.bonus() > 0 && e.bonus() <= 1.0, a + " bonus " + e.bonus());
                assertFalse(e.blurb().isEmpty(), a + " has no description");
                assertTrue(e.ticks() <= 200 && e.amp() <= 5 && e.heal() <= 0.5);
            }
        }
    }

    @Test void theAttributeNeedsTheDevilLetInAndStaysBounded() {
        assertEquals(0.0, DevilRules.effectPower(DevilTier.SUPREME, 19.9f, true));
        assertTrue(DevilRules.effectPower(DevilTier.LOW, 100f, false) < DevilRules.effectPower(DevilTier.SUPREME, 100f, false));
        assertEquals(1.0, DevilRules.effectPower(DevilTier.SUPREME, 100f, false), 1e-9);
        assertEquals(1.5, DevilRules.effectPower(DevilTier.SUPREME, 100f, true), 1e-9);
        DevilRules.Effect e = DevilRules.effect(DevilAttribute.BEAST);
        assertEquals(5f, DevilRules.effectBonus(10f, e, 1.0, 1.0), 1e-4);
        assertEquals(30f, DevilRules.effectBonus(10f, e, 1.5, 10.0), 1e-4);           // never more than three times the blow
        assertEquals(0f, DevilRules.effectBonus(10f, e, 0.0, 1.0));
        assertEquals(0f, DevilRules.effectBonus(-1f, e, 1.0, 1.0));
        assertTrue(DevilRules.effectReady(0, 20) && !DevilRules.effectReady(0, 19));
        assertEquals(12f, DevilRules.branded(10f), 1e-4);
    }

    @Test void everyRitualBeginsForAPreparedCallerAndRefusesWhatItMust() {
        for (DevilWorld w : DevilWorld.values()) assertEquals(DevilRitualRules.Denial.NONE, DevilRitualRules.check(w, ready()), w.toString());
        DevilRitualRules.Facts f = ready();
        assertEquals(DevilRitualRules.Denial.DISABLED, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(false, false, false, -1, 0.2, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.BOND, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(true, true, false, -1, 0.2, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.BUSY, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(true, false, true, -1, 0.2, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.COOLDOWN, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(true, false, false, 100, 0.2, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_RELIC, DevilRitualRules.check(DevilWorld.BLACK_CLOVER, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, false, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NOT_DESPAIR, DevilRitualRules.check(DevilWorld.BLACK_BUTLER, new DevilRitualRules.Facts(true, false, false, -1, 0.9, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_PAPER, DevilRitualRules.check(DevilWorld.BLUE_EXORCIST, new DevilRitualRules.Facts(true, false, false, -1, 0.9, 12f, true, false, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_BLOOD, DevilRitualRules.check(DevilWorld.BLUE_EXORCIST, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 5f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_OFFERING, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, true, 31, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_MAGICULES, DevilRitualRules.check(DevilWorld.TENSURA, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, true, 64, 40, 0.4, false)));
        assertEquals(DevilRitualRules.Denial.OPEN_SKY, DevilRitualRules.check(DevilWorld.DORORO, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, true, 64, 40, 1.0, true)));
        assertEquals(DevilRitualRules.Denial.NOT_LOWEST, DevilRitualRules.check(DevilWorld.BERSERK, new DevilRitualRules.Facts(true, false, false, -1, 0.5, 12f, true, true, true, 64, 40, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_LEVELS, DevilRitualRules.check(DevilWorld.BERSERK, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, true, 64, 29, 1.0, false)));
        assertEquals(DevilRitualRules.Denial.NO_BOOK, DevilRitualRules.check(DevilWorld.FAIRY_TAIL, new DevilRitualRules.Facts(true, false, false, -1, 0.2, 12f, true, true, false, 64, 40, 1.0, false)));
        assertNotNull(f);
    }

    @Test void aRitualBreaksOutsideTheCircleAndTheBlowsNeverKill() {
        assertNull(DevilRitualRules.broken(DevilWorld.BLACK_CLOVER, 3.0, 0.05));
        assertNotNull(DevilRitualRules.broken(DevilWorld.BLACK_CLOVER, 3.6, 1.0));
        assertNotNull(DevilRitualRules.broken(DevilWorld.BLUE_EXORCIST, 0.0, 0.3));
        assertNull(DevilRitualRules.broken(DevilWorld.BLUE_EXORCIST, 0.0, 0.4));
        assertEquals(3f, DevilRitualRules.bindingBlow(20f, 20f), 1e-4);
        assertEquals(1f, DevilRitualRules.bindingBlow(3f, 20f), 1e-4);
        assertEquals(0f, DevilRitualRules.bindingBlow(2f, 20f), 1e-4);
        int blows = 0;
        for (int t = 0; t <= DevilWorld.BLACK_CLOVER.ticks; t++) if (DevilRitualRules.blowAt(t)) blows++;
        assertEquals(4, blows);
        assertTrue(DevilRitualRules.offerOpen(100, 700) && !DevilRitualRules.offerOpen(100, 701) && !DevilRitualRules.offerOpen(0, 5));
    }

    @Test void pricesAreTheWorldsOwn() {
        assertEquals(0, WorldDevil.byId("liebe").priceMask());
        assertEquals(0, WorldDevil.byId("daemon_noir").priceMask());
        assertEquals(Sacrifice.HALF_HEALTH.bit(), WorldDevil.byId("curse_devil").priceMask());
        assertEquals(Sacrifice.LOST_EYE_RIGHT.bit(), WorldDevil.byId("future_devil").priceMask());
        assertEquals(Sacrifice.FLESH.bit(), WorldDevil.byId("fox_devil").priceMask());
        assertEquals(Sacrifice.LOST_EYE_RIGHT.bit() | Sacrifice.SOUL_DEBT.bit(), WorldDevil.byId("sebastian").priceMask());
        assertEquals(Sacrifice.HALF_HEALTH.bit() | Sacrifice.BRANDED.bit(), WorldDevil.byId("god_hand").priceMask());
    }

    @Test void everyEffectTheRitualsNameExistsAndTheWorldsAreThemed() throws Exception {
        Path main = Path.of("src/main");
        String shapes = Files.readString(main.resolve("java/com/newuniverse/nusmp/vfx/VfxShape.java"));
        String rituals = Files.readString(main.resolve("java/com/newuniverse/nusmp/devil/DevilRituals.java"));
        Matcher m = Pattern.compile("VfxShape\\.([A-Z0-9_]+)").matcher(rituals);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(Pattern.compile("(?m)^\\s*" + m.group(1) + "\\b").matcher(shapes).find() || Pattern.compile("[, ]" + m.group(1) + "[,;]").matcher(shapes).find(), "no effect " + m.group(1));
        }
        assertTrue(n >= 24, "the eight rituals share too few effects: " + n);
        String themes = Files.readString(main.resolve("resources/data/nusmp/particle_themes.json"));
        Matcher t = Pattern.compile("ThemeKey\\.of\\(\"([^\"]+)\"\\)").matcher(rituals);
        int k = 0;
        while (t.find()) {
            k++;
            assertTrue(themes.contains("\"" + t.group(1) + "\""), "no particle theme " + t.group(1));
        }
        assertEquals(DevilWorld.values().length, k);
    }
}
