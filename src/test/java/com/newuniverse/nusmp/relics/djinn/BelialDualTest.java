package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.weapon.ActClock;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The third act, the act selection, the dual-Djinn link and the source rules of the second Belial rework (owner notes). Everything old stays as it was. */
class BelialDualTest {
    private static final Path MAIN = Path.of("src", "main");
    private static final Path DJINN = MAIN.resolve("java/com/newuniverse/nusmp/relics/djinn");

    @Test
    void everyOtherWeaponKeepsItsTwoActsAndTheOldSelection() {
        for (Djinn d : Djinn.values()) assertNull(DjinnWeaponRules.spec(d).three(), d.name());
        assertEquals(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.pickAct(false, false, false, 0, false));
        assertEquals(DjinnWeaponRules.ACT_TWO, DjinnWeaponRules.pickAct(false, true, false, 0, false));
        assertEquals(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.pickAct(false, false, true, 90, true), "sprinting or looking down changes nothing for a normal weapon");
    }

    @Test
    void theCanonScytheAppendsTheThirdActAndKeepsTheFirstTwo() {
        DjinnWeaponRules.Spec s = DjinnWeaponRules.canonSpec();
        assertEquals("spirit_sever", s.one().key());
        assertEquals("exile", s.two().key());
        assertEquals("belior_zakera", s.three().key());
        assertSame(s.three(), s.act(DjinnWeaponRules.ACT_THREE));
        assertSame(s.one(), s.act(DjinnWeaponRules.ACT_ONE));
        assertSame(s.two(), s.act(DjinnWeaponRules.ACT_TWO));
        assertTrue(s.three().cooldown() >= 20 && s.three().cooldown() <= ActClock.MAX_COOLDOWN);
        assertEquals(10.0, s.three().range(), 1.0E-9, "10 blocks through the Half Equip; the Full Equip's 24 is BelialMindRules'");
        assertEquals("belior_zakera", DjinnWeaponRules.spec(Djinn.BELIAL, true).three().key());
        assertNull(DjinnWeaponRules.spec(Djinn.BELIAL, false).three(), "the old greatsword row is untouched");
    }

    @Test
    void theScytheSelectsThreeActsAndTheDualCastNeedsZaganAndALookDown() {
        assertEquals(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.pickAct(true, false, false, 0, false));
        assertEquals(DjinnWeaponRules.ACT_TWO, DjinnWeaponRules.pickAct(true, true, false, 0, false));
        assertEquals(DjinnWeaponRules.ACT_THREE, DjinnWeaponRules.pickAct(true, false, true, 0, false), "sprint + right-click");
        assertEquals(DjinnWeaponRules.ACT_TWO, DjinnWeaponRules.pickAct(true, true, true, 90, true), "sneaking is always the second act");
        assertEquals(DjinnWeaponRules.ACT_DUAL, DjinnWeaponRules.pickAct(true, false, false, 80, true));
        assertEquals(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.pickAct(true, false, false, 80, false), "no Zagan shared: looking down is just the first act");
        assertEquals(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.pickAct(true, false, false, 30, true), "not looking down far enough");
    }

    @Test
    void theNewActsOpenFromTheHalfEquipAndTheOldRuleStays() {
        assertTrue(DjinnWeaponRules.open(DjinnWeaponRules.ACT_ONE, DjinnWeaponRules.HALF));
        assertFalse(DjinnWeaponRules.open(DjinnWeaponRules.ACT_TWO, DjinnWeaponRules.HALF));
        assertTrue(DjinnWeaponRules.open(DjinnWeaponRules.ACT_TWO, DjinnWeaponRules.FULL));
        assertTrue(DjinnWeaponRules.open(DjinnWeaponRules.ACT_THREE, DjinnWeaponRules.HALF));
        assertTrue(DjinnWeaponRules.open(DjinnWeaponRules.ACT_DUAL, DjinnWeaponRules.HALF));
        assertFalse(DjinnWeaponRules.open(DjinnWeaponRules.ACT_THREE, 0));
        assertTrue(DjinnWeaponRules.DUAL_POWER <= 1.0, "Zagan's act is never stronger through Belial");
    }

    @Test
    void theClockHasFourIndependentSlots() {
        ActClock c = new ActClock();
        for (int a = 0; a < 4; a++) c.start(a, 100, 100 + a * 10);
        for (int a = 0; a < 4; a++) assertEquals(100 + a * 10, c.left(a, 100), "slot " + a);
        c.start(2, 100, 0);
        assertTrue(c.ready(2, 100));
        assertFalse(c.ready(3, 100));
        c.clear();
        for (int a = 0; a < 4; a++) assertTrue(c.ready(a, 0));
    }

    @Test
    void theDualLinkOnlyLetsACanonBelialOwnerShareZagan() {
        assertTrue(HouseholdRules.dualLink(true, true, Djinn.ZAGAN));
        assertFalse(HouseholdRules.dualLink(false, true, Djinn.ZAGAN), "the setting off restores the old rule");
        assertFalse(HouseholdRules.dualLink(true, false, Djinn.ZAGAN), "someone who does not own the canon Belial");
        assertFalse(HouseholdRules.dualLink(true, true, Djinn.BAAL), "only Zagan, the other Life Magic Djinn");
        // the old rule: an ally who already carries a Djinn is refused; the dual link only lifts that one bit
        assertEquals(HouseholdRules.Verdict.ALLY_HAS_DJINN, HouseholdRules.invite(true, false, 0, false, HouseholdRules.allyMaskFor(BondRules.DJINN, false), false));
        assertEquals(HouseholdRules.Verdict.OK, HouseholdRules.invite(true, false, 0, false, HouseholdRules.allyMaskFor(BondRules.DJINN, true), false));
        assertEquals(HouseholdRules.Verdict.ALLY_BONDED, HouseholdRules.invite(true, false, 0, false, HouseholdRules.allyMaskFor(BondRules.DJINN | BondRules.SPIRIT, true), false), "a Spirit still refuses");
        assertEquals(HouseholdRules.Verdict.FULL, HouseholdRules.invite(true, false, DjinnPhaseRules.HOUSEHOLD_MAX, false, HouseholdRules.allyMaskFor(BondRules.DJINN, true), false), "Zagan's household is still two");
    }

    @Test
    void theNewServerCodeHasNoClientClassNoFoodEffectAndCleansUp() throws IOException {
        for (String f : new String[]{"BelialMind.java", "BelialMindRules.java", "BelialGrip.java", "BelialGripRules.java", "BelialDual.java"}) {
            String src = Files.readString(DJINN.resolve(f));
            assertFalse(src.contains("net.minecraft.client") || src.contains("com.mojang.blaze3d") || src.contains("nusmp.client") || src.contains("Minecraft.getInstance"), f + ": server code only");
            String low = src.toLowerCase(java.util.Locale.ROOT);
            assertFalse(low.contains("hunger") || low.contains("exhaustion"), f + ": the mod has no food effects");
            assertFalse(src.contains("ChunkSource.getChunk(") || src.contains("getEntitiesOfClass") && f.equals("BelialMind.java"), f + ": no chunk load, no world scan in the mind magic");
        }
        String grip = Files.readString(DJINN.resolve("BelialGrip.java"));
        assertTrue(grip.contains("onLogout") && grip.contains("onDimension") && grip.contains("onDeath") && grip.contains("onServerStopping") && grip.contains("restoreMarked"), "grip cleanup");
        String mind = Files.readString(DJINN.resolve("BelialMind.java"));
        assertTrue(mind.contains("onLogout") && mind.contains("onDimension") && mind.contains("onDeath") && mind.contains("onServerStopping"), "mind cleanup");
        String events = Files.readString(DJINN.resolve("DjinnEvents.java"));
        assertTrue(events.contains("BelialMind.register(bus)") && events.contains("BelialGrip.register(bus)"), "registered");
    }

    @Test
    void theNewLangKeysAndSettingsExist() throws IOException {
        String lang = Files.readString(MAIN.resolve("resources/assets/anime_multiverse/lang/en_us.json"));
        for (String key : new String[]{"djinn_weapon.act.belior_zakera", "djinn_weapon.act.belior_zakera.desc", "tooltip.anime_multiverse.djinn_weapon.act3", "belial.chant.zauto", "belial.lore"})
            assertTrue(lang.contains("\"" + key + "\""), key);
        assertTrue(lang.contains("\"djinn_weapon.act.spirit_sever\": \"Belior Goldoreza\""), "the first act is shown as Belior Goldoreza");
        assertTrue(lang.contains("Spirit of Truth and Judgement — Assemble your power and bring to the earth the nothingness to judge everything!"));
        String cfg = Files.readString(DJINN.resolve("BelialConfig.java"));
        for (String k : new String[]{"\"zakera\"", "\"senses\"", "\"dualDjinn\"", "\"boneGrip\"", "\"deadWeight\""}) assertTrue(cfg.contains(k), k);
        assertTrue(Files.exists(Path.of("tools", "lang", "belial_zakera.json")));
    }
}
