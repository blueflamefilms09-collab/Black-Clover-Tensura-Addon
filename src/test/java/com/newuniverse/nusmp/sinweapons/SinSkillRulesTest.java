package com.newuniverse.nusmp.sinweapons;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rules of the five Berserk weapon skills are pure ({@link SinSkillRules}) so every decision is pinned without the game: who a stack belongs to, when an unbound stack is bound, who may use a technique (and
 * the setting that lets anyone), what the Manifest mode does and how its cooldown behaves, how the once-a-second look is staggered and bounded. The skills' files (icon, lang, ids) are checked from the repo. Pure: java.nio only, CWD = repo root.
 */
class SinSkillRulesTest {
    private static final Path RES = Path.of("src", "main", "resources", "assets", "anime_multiverse");
    private static final String ME = "00000000-0000-0000-0000-000000000001", YOU = "00000000-0000-0000-0000-000000000002";

    @Test
    void ownerIsReadFromTheStoredText() {
        assertEquals(SinSkillRules.Owner.UNBOUND, SinSkillRules.ownerOf(null, ME));
        assertEquals(SinSkillRules.Owner.UNBOUND, SinSkillRules.ownerOf("", ME));
        assertEquals(SinSkillRules.Owner.MINE, SinSkillRules.ownerOf(ME, ME));
        assertEquals(SinSkillRules.Owner.OTHER, SinSkillRules.ownerOf(YOU, ME));
    }

    @Test
    void bindingHappensOnlyForAnUnboundStackAndOnlyOnePerKind() {
        assertTrue(SinSkillRules.shouldBind(SinSkillRules.Owner.UNBOUND, false));
        assertFalse(SinSkillRules.shouldBind(SinSkillRules.Owner.UNBOUND, true), "one bound weapon per player per kind");
        assertFalse(SinSkillRules.shouldBind(SinSkillRules.Owner.MINE, false), "already theirs");
        assertFalse(SinSkillRules.shouldBind(SinSkillRules.Owner.OTHER, false), "another player's stack is never taken over");
    }

    @Test
    void theTechniqueNeedsTheOwnerUnlessTheSettingIsOff() {
        assertTrue(SinSkillRules.mayUseTechnique(true, SinSkillRules.Owner.MINE));
        assertTrue(SinSkillRules.mayUseTechnique(true, SinSkillRules.Owner.UNBOUND), "a /give'd stack works until the first carrier binds it");
        assertFalse(SinSkillRules.mayUseTechnique(true, SinSkillRules.Owner.OTHER), "bound to another player: a plain melee weapon");
        assertTrue(SinSkillRules.mayUseTechnique(false, SinSkillRules.Owner.OTHER), "soulBound=false restores the old behaviour");
    }

    @Test
    void manifestStoresForgesOrWaits() {
        assertEquals(SinSkillRules.Manifest.STORE, SinSkillRules.manifest(true, true, 100, 10_000), "holding it stores it, cooldown or not");
        assertEquals(SinSkillRules.Manifest.ALREADY_CARRIED, SinSkillRules.manifest(false, true, 100, 0));
        assertEquals(SinSkillRules.Manifest.FORGE, SinSkillRules.manifest(false, false, 100, 0));
        assertEquals(SinSkillRules.Manifest.WAIT, SinSkillRules.manifest(false, false, 100, 150));
        assertEquals(SinSkillRules.Manifest.FORGE, SinSkillRules.manifest(false, false, 150, 150), "ready on the tick the cooldown ends");
    }

    @Test
    void theForgeCooldownIsAtLeastOneHundredTicksAndSurvivesAClockSetBack() {
        assertTrue(SinSkillRules.FORGE_COOLDOWN_TICKS >= 100);
        long now = 5_000;
        long next = SinSkillRules.nextForgeAt(now);
        assertEquals(now + SinSkillRules.FORGE_COOLDOWN_TICKS, next);
        assertFalse(SinSkillRules.forgeReady(now, next));
        assertFalse(SinSkillRules.forgeReady(next - 1, next));
        assertTrue(SinSkillRules.forgeReady(next, next));
        assertTrue(SinSkillRules.forgeReady(10, 1_000_000), "a clock set back does not lock the player out for ever");
    }

    @Test
    void theLookIsStaggeredAndBounded() {
        for (int id = 0; id < 40; id++) {
            int turns = 0;
            for (int t = 0; t < SinSkillRules.SCAN_PERIOD * 5; t++) if (SinSkillRules.scanTurn(t, id)) turns++;
            assertEquals(5, turns, "one look per second per player, id " + id);
        }
        assertTrue(SinSkillRules.scanTurn(-3, 3), "never a negative modulo");
        assertEquals(41, SinSkillRules.slotsScanned(41));
        assertEquals(41, SinSkillRules.slotsScanned(5000), "at most the player's own 41 slots");
        assertEquals(0, SinSkillRules.slotsScanned(-1));
        assertEquals(20, SinSkillRules.SCAN_PERIOD);
    }

    @Test
    void everyWeaponHasItsOwnSkillWithIconNameAndText() throws IOException {
        String frag = Files.readString(Path.of("tools", "lang", "berserk_skills.json"), StandardCharsets.UTF_8);
        String en = Files.readString(RES.resolve("lang/en_us.json"), StandardCharsets.UTF_8);
        Set<String> seen = new HashSet<>();
        Set<String> pics = new HashSet<>();
        for (SinRules.Kind k : SinRules.Kind.values()) {
            String id = SinSkillRules.skillId(k);
            assertTrue(seen.add(id), "two weapons share a skill id: " + id);
            for (String key : new String[]{"anime_multiverse.skill." + id, "anime_multiverse.skill." + id + ".description"}) {
                assertTrue(frag.contains("\"" + key + "\""), "tools/lang/berserk_skills.json lacks " + key);
                assertTrue(en.contains("\"" + key + "\""), "en_us.json lacks " + key);
            }
            BufferedImage im = ImageIO.read(RES.resolve("textures/skill/" + id + ".png").toFile());
            assertEquals(32, im.getWidth(), id);
            assertEquals(32, im.getHeight(), id);
            assertEquals(0, im.getRGB(0, 0) >>> 24, id + ": a rounded, transparent corner like Tensura's frames");
            assertEquals(255, im.getRGB(16, 16) >>> 24, id + ": opaque middle");
            assertTrue(pics.add(java.util.Arrays.toString(im.getRGB(0, 0, 32, 32, null, 0, 32))), id + ": every skill has its own icon");
        }
        for (String key : new String[]{"anime_multiverse.skill.mode.sinskill.technique", "anime_multiverse.skill.mode.sinskill.manifest", "sinskill.msg.granted", "sinskill.msg.not_yours", "sinskill.msg.hold",
                "sinskill.msg.stored", "sinskill.msg.forged", "sinskill.msg.carried", "sinskill.msg.wait", "sinskill.msg.full"}) {
            assertTrue(frag.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "no text for " + key);
        }
    }
}
