package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.chatter.ChatterTemplate;
import com.newuniverse.nusmp.avatar.chatter.ChunibyoNames;
import com.newuniverse.nusmp.avatar.chatter.HybridMixer;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Seeded, deterministic mixing of two personalities' lines. */
class HybridMixerTest {
    private static final List<String> TSUN = List.of("I am only doing this because I have to, idiot!", "Hmph. Do not misunderstand.", "Stay back! It is not like I care!");
    private static final List<String> HERO = List.of("Light of justice, strike!", "For the people! Attack!", "A hero never runs.");

    /** A scripted generator: doubles and ints are consumed from the lists in order. */
    private static Rng script(double[] doubles, int[] ints) {
        int[] di = {0}, ii = {0};
        return new Rng() {
            @Override public int nextInt(int bound) { return ints[ii[0]++ % ints.length] % bound; }
            @Override public double nextDouble() { return doubles[di[0]++ % doubles.length]; }
        };
    }

    @Test
    void sourceIsFiftyThirtyTwenty() {
        assertEquals(HybridMixer.Source.PRIMARY, HybridMixer.chooseSource(script(new double[]{0.0}, new int[]{0})));
        assertEquals(HybridMixer.Source.PRIMARY, HybridMixer.chooseSource(script(new double[]{0.4999}, new int[]{0})));
        assertEquals(HybridMixer.Source.SECONDARY, HybridMixer.chooseSource(script(new double[]{0.5}, new int[]{0})));
        assertEquals(HybridMixer.Source.SECONDARY, HybridMixer.chooseSource(script(new double[]{0.7999}, new int[]{0})));
        assertEquals(HybridMixer.Source.COMBINED, HybridMixer.chooseSource(script(new double[]{0.8}, new int[]{0})));
        assertEquals(HybridMixer.Source.COMBINED, HybridMixer.chooseSource(script(new double[]{0.9999}, new int[]{0})));
    }

    @Test
    void theShareIsRoughlyRightOverManyDraws() {
        Rng r = Rng.seeded(12345);
        int[] n = new int[3];
        for (int i = 0; i < 20000; i++) n[HybridMixer.chooseSource(r).ordinal()]++;
        assertEquals(0.5, n[0] / 20000.0, 0.02);
        assertEquals(0.3, n[1] / 20000.0, 0.02);
        assertEquals(0.2, n[2] / 20000.0, 0.02);
    }

    @Test
    void sameSeedSameLine() {
        for (long seed = 1; seed <= 50; seed++) {
            String a = HybridMixer.pick(Rng.seeded(seed), AvatarPersonality.TSUNDERE, TSUN, AvatarPersonality.HEROIC, HERO);
            String b = HybridMixer.pick(Rng.seeded(seed), AvatarPersonality.TSUNDERE, TSUN, AvatarPersonality.HEROIC, HERO);
            assertEquals(a, b);
        }
    }

    @Test
    void pickTakesTheRightPool() {
        assertTrue(TSUN.contains(HybridMixer.pick(script(new double[]{0.1}, new int[]{1}), AvatarPersonality.TSUNDERE, TSUN, AvatarPersonality.HEROIC, HERO)));
        assertTrue(HERO.contains(HybridMixer.pick(script(new double[]{0.6}, new int[]{1}), AvatarPersonality.TSUNDERE, TSUN, AvatarPersonality.HEROIC, HERO)));
    }

    @Test
    void anEmptyPoolIsSkipped() {
        assertTrue(HERO.contains(HybridMixer.pick(Rng.seeded(1), AvatarPersonality.TSUNDERE, List.of(), AvatarPersonality.HEROIC, HERO)));
        assertTrue(TSUN.contains(HybridMixer.pick(Rng.seeded(1), AvatarPersonality.TSUNDERE, TSUN, AvatarPersonality.HEROIC, null)));
        assertNull(HybridMixer.pick(Rng.seeded(1), AvatarPersonality.TSUNDERE, List.of(), AvatarPersonality.HEROIC, List.of()));
        assertNull(HybridMixer.pickOne(Rng.seeded(1), null));
    }

    @Test
    void tsundereLeadsHeroicTails() {
        String line = HybridMixer.combine(AvatarPersonality.TSUNDERE, "I am only saving you because it is the right thing to do, idiot! Do not thank me!",
                AvatarPersonality.HEROIC, "Evil stops here! Light of justice, strike!");
        assertEquals("I am only saving you because it is the right thing to do, idiot! Light of justice, strike!", line);
    }

    @Test
    void heroicPrimaryStillLetsTheTsundereLead() {
        String line = HybridMixer.combine(AvatarPersonality.HEROIC, "Evil stops here! Light of justice, strike!",
                AvatarPersonality.TSUNDERE, "Hmph. Not for you, idiot!");
        assertEquals("Hmph. Light of justice, strike!", line);
    }

    @Test
    void oneSentenceLinesAreUsedWhole() {
        assertEquals("Enemy. Behind me, {creator}.", HybridMixer.combine(AvatarPersonality.STOIC, "Enemy.", AvatarPersonality.LOYAL, "Behind me, {creator}."));
    }

    @Test
    void aTooLongGlueFallsBackToTheLeader() {
        String longTail = "x".repeat(140) + ".";
        String line = HybridMixer.combine(AvatarPersonality.TSUNDERE, "Short insult, idiot!", AvatarPersonality.HEROIC, longTail);
        assertEquals("Short insult, idiot!", line);
    }

    @Test
    void combineHandlesNullAndBlank() {
        assertEquals("B", HybridMixer.combine(AvatarPersonality.LOYAL, null, AvatarPersonality.LAZY, "B"));
        assertEquals("A", HybridMixer.combine(AvatarPersonality.LOYAL, "A", AvatarPersonality.LAZY, "  "));
    }

    @Test
    void combinedLinesKeepTheirPlaceholdersForLaterFilling() {
        String line = HybridMixer.combine(AvatarPersonality.TSUNDERE, "Hmph, {creator}!", AvatarPersonality.CHUNIBYO, "Eye of the Chosen, awaken! {skill}!");
        assertEquals(Set.of("creator", "skill"), ChatterTemplate.placeholders(line));
    }

    @Test
    void allCombinationsOfFileStyleLinesStayUnder160() {
        Rng r = Rng.seeded(7);
        for (AvatarPersonality a : AvatarPersonality.values())
            for (AvatarPersonality b : AvatarPersonality.values()) {
                String s = HybridMixer.combine(a, "One. Two sentences here.", b, "Three. Four sentences right here.");
                assertTrue(s.length() < 160 && !s.isBlank());
            }
        assertNotNull(r);
    }

    // ---------------------------------------------------------------- template and renaming
    @Test
    void fillReplacesKnownPlaceholdersAndBlanksUnknownOnes() {
        Map<String, String> v = new HashMap<>();
        v.put("name", "Kaito");
        v.put("creator", "Asta");
        assertEquals("Kaito guards Asta.", ChatterTemplate.fill("{name} guards {creator}.", v));
        assertEquals("Hello .", ChatterTemplate.fill("Hello {nobody}.", v));
        assertEquals("a b", ChatterTemplate.fill("a  {x}  b", v));
    }

    @Test
    void fillIsSafeAgainstDollarAndBackslashInValues() {
        Map<String, String> v = new HashMap<>();
        v.put("target", "$1\\Evil");
        assertEquals("Hit $1\\Evil now", ChatterTemplate.fill("Hit {target} now", v));
    }

    @Test
    void chunibyoRenamesKnownAndUnknownSpellsStably() {
        assertEquals("CRIMSON JUDGEMENT OF THE FALLEN SUN", ChunibyoNames.rename("Exploding Fireball"));
        String unknown = ChunibyoNames.rename("Totally Made Up Spell");
        assertEquals(unknown, ChunibyoNames.rename("Totally Made Up Spell"), "the same spell always gets the same title");
        assertTrue(unknown.contains("TOTALLY MADE UP SPELL"));
        assertEquals("THE TECHNIQUE THAT HAS NO NAME", ChunibyoNames.rename(null));
        Set<String> titles = new HashSet<>();
        for (int i = 0; i < 40; i++) titles.add(ChunibyoNames.rename("Spell " + i));
        assertTrue(titles.size() > 30);
    }
}
