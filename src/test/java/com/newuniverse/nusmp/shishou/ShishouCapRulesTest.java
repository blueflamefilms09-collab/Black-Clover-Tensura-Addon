package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The one-per-world rules: the cap state machine, ghosts and duplicates, the anti-theft timer, the recovery rule, the windows, the clones, the storage rules, the concealment, the speech gate. */
class ShishouCapRulesTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes()), E1 = UUID.nameUUIDFromBytes("e1".getBytes()), E2 = UUID.nameUUIDFromBytes("e2".getBytes());

    @Test
    void onlyOneSwordCanEverExist() {
        ShishouCap c = new ShishouCap();
        assertFalse(c.exists());
        assertTrue(c.spawnWild(E1, "overworld", 0, 64, 0));
        assertFalse(c.spawnWild(E2, "overworld", 5, 64, 5), "a second wild sword is refused");
        assertFalse(c.adopt(A), "an item cannot be adopted while a sword exists");
        assertEquals(ShishouCap.Form.WILD, c.form());
        assertTrue(c.validEntity(E1));
        assertFalse(c.validEntity(E2));
        assertEquals(ShishouCap.Claim.NOT_DORMANT, c.claim(A, 0L), "a flying sword cannot be claimed");
        assertTrue(c.dormant(E1, "overworld", 0, 63, 0));
        assertFalse(c.spawnWild(E2, "overworld", 0, 0, 0), "not while dormant either");
        assertEquals(ShishouCap.Claim.OK, c.claim(A, 10L));
        assertEquals(ShishouCap.Form.HELD, c.form());
        assertTrue(c.isOwner(A));
        assertFalse(c.isOwner(B));
        assertFalse(c.validEntity(E1), "the dormant body is a ghost after the claim");
        assertEquals(ShishouCap.Claim.NOT_DORMANT, c.claim(B, 11L), "nobody else can claim it now");
    }

    @Test
    void throwAndRecallKeepOneBodyAtATime() {
        ShishouCap c = new ShishouCap();
        c.adopt(A);
        long t0 = c.token();
        assertTrue(c.validStack(t0));
        assertFalse(c.throwOut(t0 + 99, E1, "overworld", 0, 0, 0), "a stack with the wrong token cannot throw");
        assertTrue(c.throwOut(t0, E1, "overworld", 1, 2, 3));
        assertEquals(ShishouCap.Form.MINION, c.form());
        assertFalse(c.validStack(t0), "the thrown stack is dead");
        assertTrue(c.validEntity(E1));
        assertFalse(c.throwOut(t0, E2, "overworld", 0, 0, 0), "cannot throw twice");
        long t1 = c.recall();
        assertTrue(t1 != 0L && t1 != t0);
        assertFalse(c.validEntity(E1), "the minion is a ghost after the recall");
        assertTrue(c.validStack(t1));
        assertEquals(0L, c.recall(), "nothing to recall when it is in the hand");
    }

    @Test
    void aCopyOfTheItemIsAGhostOrADuplicate() {
        ShishouCap c = new ShishouCap();
        c.adopt(A);
        long t = c.token();
        assertEquals(ShishouCap.Beat.OK, c.beat(t, 111, 100L));
        assertEquals(ShishouCap.Beat.OK, c.beat(t, 111, 100L), "the same stack twice is fine");
        assertEquals(ShishouCap.Beat.DUPLICATE, c.beat(t, 222, 100L), "a second stack in the same tick is a copy");
        assertEquals(ShishouCap.Beat.OK, c.beat(t, 222, 101L), "a moved stack in the next tick is not");
        assertEquals(ShishouCap.Beat.GHOST, c.beat(t + 1, 111, 102L), "another token is a ghost");
        assertEquals(ShishouCap.Beat.GHOST, c.beat(0L, 111, 102L), "no token is a ghost");
        c.throwOut(t, E1, "w", 0, 0, 0);
        assertEquals(ShishouCap.Beat.GHOST, c.beat(t, 111, 103L), "an old stack is a ghost once the sword is thrown");
    }

    @Test
    void aReservedDormantSwordIsKeptForItsWielderThenFree() {
        ShishouCap c = new ShishouCap();
        c.adopt(A);
        c.recoverToDormant(E1, "overworld", 10, 70, 10, 1000L, ShishouRecovery.reserveTicks(7));
        assertEquals(ShishouCap.Form.DORMANT, c.form());
        assertEquals(ShishouCap.Claim.RESERVED, c.claim(B, 2000L), "another player is refused during the reserve");
        assertTrue(c.reservedAgainst(B, 2000L));
        assertFalse(c.reservedAgainst(A, 2000L));
        assertEquals(ShishouCap.Claim.OK, c.claim(A, 2000L), "the wielder may take it back");
        c.recoverToDormant(E2, "overworld", 0, 0, 0, 0L, ShishouRecovery.reserveTicks(7));
        assertEquals(ShishouCap.Claim.OK, c.claim(B, 8L * ShishouRules.DAY_TICKS), "after seven days anyone may");
        assertTrue(c.isOwner(B));
    }

    @Test
    void aLostSwordIsAlwaysRecoveredNeverDoubledAndNeverDestroyed() {
        ShishouCap c = new ShishouCap();
        c.adopt(A);
        long t = c.token();
        c.recoverToDormant(E1, "overworld", 0, 64, 0, 0L, 100L);
        assertFalse(c.validStack(t), "the lost stack is now a ghost");
        assertTrue(c.validEntity(E1));
        long t2 = c.recoverToHeld();
        assertTrue(t2 != 0L && !c.validEntity(E1));
        assertTrue(c.validStack(t2));
        ShishouCap none = new ShishouCap();
        none.recoverToDormant(E2, "overworld", 0, 64, 0, 0L, 100L);
        assertEquals(ShishouCap.Form.DORMANT, none.form(), "a repair works from nothing");
        assertEquals(0L, none.recoverToHeld(), "no wielder, no inventory");
        none.reset();
        assertFalse(none.exists());
        assertFalse(none.validEntity(E2));
    }

    @Test
    void theCapSurvivesSavingAndBadData() {
        ShishouCap c = new ShishouCap();
        c.adopt(A);
        c.setHome("minecraft:overworld", 5, 70, -9);
        c.throwOut(c.token(), E1, "minecraft:the_nether", 1, 2, 3);
        ShishouCap d = ShishouCap.fromMap(c.toMap());
        assertEquals(ShishouCap.Form.MINION, d.form());
        assertEquals(E1, d.entity());
        assertEquals(A, d.owner());
        assertEquals(c.token(), d.token());
        assertTrue(d.hasHome());
        assertEquals(-9, d.home()[2]);
        assertEquals("minecraft:the_nether", d.dim());
        long before = d.token();
        d.recall();
        assertTrue(d.token() > before, "tokens never repeat after a reload");
        java.util.Map<String, String> bad = new java.util.HashMap<>();
        bad.put("form", "NONSENSE");
        bad.put("entity", "not-a-uuid");
        bad.put("token", "x");
        ShishouCap e = ShishouCap.fromMap(bad);
        assertEquals(ShishouCap.Form.NONE, e.form());
        assertNull(e.entity());
        ShishouCap lost = ShishouCap.fromMap(java.util.Map.of("form", "DORMANT"));
        assertTrue(lost.needsRepair(), "a body with no id is repaired, not forgotten");
        assertEquals(ShishouCap.Form.DORMANT, lost.form());
    }

    @Test
    void aThiefHasTenSecondsAndOneCheckASecond() {
        ShishouTheft t = new ShishouTheft();
        assertEquals(ShishouTheft.Verdict.NONE, t.check(A, true, 0L), "the wielder is never touched");
        assertEquals(ShishouTheft.Verdict.PUNISH_FIRST, t.check(B, false, 100L));
        assertEquals(ShishouTheft.Verdict.PUNISH, t.check(B, false, 120L));
        assertEquals(ShishouTheft.Verdict.PUNISH, t.check(B, false, 100L + ShishouRules.THEFT_TICKS - 20));
        assertTrue(t.secondsLeft(B, 100L + ShishouRules.THEFT_TICKS - 20) <= 1);
        assertEquals(ShishouTheft.Verdict.CONFISCATE, t.check(B, false, 100L + ShishouRules.THEFT_TICKS));
        assertEquals(0, t.tracked(), "the clock is gone after the confiscation");
        assertEquals(ShishouTheft.Verdict.PUNISH_FIRST, t.check(B, false, 400L), "a second theft starts again");
        t.forget(B);
        assertEquals(0, t.tracked());
        t.check(B, false, 0L);
        assertEquals(ShishouTheft.Verdict.NONE, t.check(B, true, 5L), "giving it back to the wielder clears the clock");
        assertEquals(ShishouRules.THEFT_SLOW_AMP, 9, "Slowness X is amplifier 9");
        assertEquals("Let go of me, you aren't my wielder.", ShishouRules.THEFT_LINE);
        assertEquals(200, ShishouRules.THEFT_TICKS);
    }

    @Test
    void theRecoveryRuleHasThreeOutcomes() {
        assertEquals(ShishouRecovery.Plan.GIVE_TO_WIELDER, ShishouRecovery.plan(true, true, true));
        assertEquals(ShishouRecovery.Plan.GIVE_TO_WIELDER, ShishouRecovery.plan(true, true, false));
        assertEquals(ShishouRecovery.Plan.DORMANT_AT_HOME, ShishouRecovery.plan(true, false, true));
        assertEquals(ShishouRecovery.Plan.DORMANT_AT_SPAWN, ShishouRecovery.plan(true, false, false));
        assertEquals(ShishouRecovery.Plan.DORMANT_AT_SPAWN, ShishouRecovery.plan(false, false, true));
        assertEquals(ShishouRecovery.Plan.DORMANT_AT_SPAWN, ShishouRecovery.plan(false, true, true));
        assertTrue(ShishouRecovery.inVoid(-64.5, -64));
        assertFalse(ShishouRecovery.inVoid(10, -64));
        assertEquals(7L * 24000L, ShishouRecovery.reserveTicks(7));
    }

    @Test
    void cooldownsAreKeyedByTheWielderAndForgotten() {
        ShishouCooldowns cd = new ShishouCooldowns();
        assertTrue(cd.ready(A, ShishouSkill.BLACK_LIGHTNING, 0L));
        cd.start(A, ShishouSkill.BLACK_LIGHTNING, 100L, ShishouSkill.BLACK_LIGHTNING.cooldown());
        assertFalse(cd.ready(A, ShishouSkill.BLACK_LIGHTNING, 200L));
        assertTrue(cd.ready(B, ShishouSkill.BLACK_LIGHTNING, 200L), "another player has none");
        assertTrue(cd.ready(A, ShishouSkill.BLACK_LIGHTNING, 100L + 160L));
        assertEquals(1, cd.check(A, ShishouSkill.BLACK_LIGHTNING, 101L, 500, 40));
        assertEquals(2, cd.check(B, ShishouSkill.BLACK_LIGHTNING, 101L, 10, 40));
        assertEquals(0, cd.check(B, ShishouSkill.BLACK_LIGHTNING, 101L, 40, 40));
        assertEquals(0.5, cd.share(A, ShishouSkill.BLACK_LIGHTNING, 180L), 0.01);
        cd.forget(A);
        assertEquals(0, cd.tracked());
        assertTrue(ShishouSkill.DIVINE_SWORD_RELEASE.cooldown() >= 3000, "the ultimate has a long cooldown");
    }

    @Test
    void windowsAreCheapToAskAndCloseThemselves() {
        ShishouWindows w = new ShishouWindows();
        assertFalse(w.anyActive(ShishouWindows.Kind.BARRIER), "nothing open: handlers return at once");
        w.start(A, ShishouWindows.Kind.BARRIER, 0L, 100);
        assertTrue(w.anyActive(ShishouWindows.Kind.BARRIER));
        assertTrue(w.active(A, ShishouWindows.Kind.BARRIER, 50L));
        assertFalse(w.active(B, ShishouWindows.Kind.BARRIER, 50L));
        w.start(A, ShishouWindows.Kind.BARRIER, 10L, 100);
        assertEquals(1, countOpen(w), "a window extended twice is one window");
        assertTrue(w.dueRefresh(A, ShishouWindows.Kind.BARRIER, 10L, 40));
        assertFalse(w.dueRefresh(A, ShishouWindows.Kind.BARRIER, 20L, 40), "at most one refresh per period");
        assertTrue(w.dueRefresh(A, ShishouWindows.Kind.BARRIER, 50L, 40));
        assertEquals(0, w.sweep(50L));
        assertEquals(1, w.sweep(200L));
        assertFalse(w.anyActive(ShishouWindows.Kind.BARRIER));
        w.start(A, ShishouWindows.Kind.SENSE, 0L, 200);
        w.forget(A);
        assertFalse(w.anyOpen(), "a wielder who leaves closes every window");
        for (int i = 0; i < 3; i++) assertTrue(w.deflectAllowed(A, 100L, ShishouRules.BARRIER_DEFLECT_PER_SECOND));
        assertFalse(w.deflectAllowed(A, 105L, ShishouRules.BARRIER_DEFLECT_PER_SECOND), "three deflections a second at most");
        assertTrue(w.deflectAllowed(A, 125L, ShishouRules.BARRIER_DEFLECT_PER_SECOND));
    }

    private static int countOpen(ShishouWindows w) {
        int n = 0;
        for (ShishouWindows.Kind k : ShishouWindows.Kind.values()) if (w.anyActive(k)) n++;
        return n;
    }

    @Test
    void clonesAreCappedPerCastAndForTheWholeWorldAndNeverOutliveTheirLife() {
        ShishouClones c = new ShishouClones();
        assertEquals(4, c.spawn(A, E1, 99, 0L, ShishouRules.CLONE_LIFE, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL));
        assertEquals(2, c.spawn(A, E1, 4, 5L, ShishouRules.CLONE_LIFE, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL), "only two more fit in the world");
        assertEquals(0, c.spawn(B, E2, 4, 6L, ShishouRules.CLONE_LIFE, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL));
        assertEquals(6, c.count());
        assertEquals(6, c.count(A));
        assertEquals(4, c.sweep(ShishouRules.CLONE_LIFE), "the first four dissolve");
        assertEquals(2, c.count());
        assertEquals(2, c.forget(A));
        assertEquals(0, c.count());
        assertEquals(0, c.spawn(null, E1, 2, 0L, 10, 4, 6));
        assertEquals(0, c.spawn(A, E1, 0, 0L, 10, 4, 6));
    }

    @Test
    void theStomachRefusesLivingThingsContainersAndItself() {
        assertEquals(ShishouStorageRules.Refusal.NONE, ShishouStorageRules.refusal("minecraft:diamond", false, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.EMPTY, ShishouStorageRules.refusal("minecraft:air", true, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.SELF, ShishouStorageRules.refusal("nusmp:shishou", false, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.LIVING, ShishouStorageRules.refusal("minecraft:pig_spawn_egg", false, false, false, true));
        assertEquals(ShishouStorageRules.Refusal.LIVING, ShishouStorageRules.refusal("minecraft:axolotl_bucket", false, false, false, true));
        assertEquals(ShishouStorageRules.Refusal.BUNDLE, ShishouStorageRules.refusal("minecraft:bundle", false, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.BUNDLE, ShishouStorageRules.refusal("x:sack", false, false, true, false));
        assertEquals(ShishouStorageRules.Refusal.CONTAINER, ShishouStorageRules.refusal("minecraft:red_shulker_box", false, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.CONTAINER, ShishouStorageRules.refusal("x:anything", false, true, false, false), "any item with a container component");
        assertEquals(ShishouStorageRules.Refusal.CONTAINER, ShishouStorageRules.refusal("sophisticatedbackpacks:backpack", false, false, false, false));
        assertEquals(ShishouStorageRules.Refusal.CONTAINER, ShishouStorageRules.refusal("nusmp:bag_of_holding", false, false, false, false));
        assertEquals(3, ShishouStorageRules.rows(1));
        assertEquals(6, ShishouStorageRules.rows(40));
        assertTrue(ShishouRules.STORAGE_SLOTS >= 27 && ShishouRules.STORAGE_SLOTS <= 54);
    }

    @Test
    void aStrangerSeesARustySwordAndOnlyTheWielderSeesTheTruth() {
        ShishouConcealment.Readout truth = new ShishouConcealment.Readout("item.nusmp.shishou", 40f, 40f, 77, 300, 900, List.of("shishou.skill.skill_eater"), false);
        ShishouConcealment.Readout mine = ShishouConcealment.view(true, true, truth);
        assertEquals(77, mine.level());
        assertFalse(mine.concealed());
        ShishouConcealment.Readout theirs = ShishouConcealment.view(false, true, truth);
        assertEquals(ShishouConcealment.FALSE_NAME_KEY, theirs.nameKey());
        assertEquals(1, theirs.level());
        assertEquals(0, theirs.mana());
        assertTrue(theirs.skills().isEmpty(), "no skill leaks");
        assertTrue(theirs.concealed());
        assertEquals(40f, ShishouConcealment.view(false, false, truth).health(), "with the skill switched off the truth shows");
    }

    @Test
    void speechIsRateLimitedPerWielderGroupAndPerListener() {
        ShishouSpeechGate g = new ShishouSpeechGate();
        ShishouVoiceLines.Group lvl = ShishouVoiceLines.Group.LEVELUP;
        assertTrue(g.wielderMay(A, lvl, 1000L));
        assertFalse(g.wielderMay(A, lvl, 1050L), "the group cools down");
        assertTrue(g.wielderMay(A, ShishouVoiceLines.Group.EAT, 1050L), "another group has its own cooldown");
        assertTrue(g.listenerMay(A, 1000L));
        assertFalse(g.listenerMay(A, 1079L), "at most one line per listener per four seconds");
        assertTrue(g.listenerMay(A, 1080L));
        assertTrue(g.listenerMay(B, 1080L), "listeners do not share a limit");
        g.forget(A);
        assertTrue(g.listenerCould(A, 1081L));
        assertEquals(80L, ShishouSpeechGate.LISTENER_GAP);
        assertEquals(32.0, ShishouSpeechGate.RADIUS, 1e-9);
    }

    @Test
    void theVoiceManifestEnumeratesTheLinesAndNothingIsRegisteredWithoutAFile() {
        List<String> ids = ShishouVoiceLines.manifestIds();
        assertTrue(ids.size() >= 36 && ids.size() <= 50, "about forty lines: " + ids.size());
        assertEquals(ids.size(), new java.util.HashSet<>(ids).size());
        assertEquals("shishou_claim_1", ids.get(0));
        assertEquals(ShishouVoiceLines.Group.THIEF, ShishouVoiceLines.groupOf("shishou_thief_2"));
        assertNull(ShishouVoiceLines.groupOf("undine_link_1"));
        assertEquals("shishou_idle_1", ShishouVoiceLines.pickManifest(ShishouVoiceLines.Group.IDLE, 4), "round and round");
        assertEquals("shishou.line.shishou_claim_1", ShishouVoiceLines.textKey("shishou_claim_1"));
        for (String id : ShishouVoiceLines.ids()) assertTrue(ids.contains(id));
    }

    @Test
    void theNaturalAppearanceIsOneRollADayAndBounded() {
        assertFalse(ShishouNatural.due(100L, ShishouNatural.day(100L)));
        assertTrue(ShishouNatural.due(24000L, 0L));
        assertTrue(ShishouNatural.succeeds(0.001, ShishouNatural.DEFAULT_CHANCE));
        assertFalse(ShishouNatural.succeeds(0.5, ShishouNatural.DEFAULT_CHANCE));
        assertFalse(ShishouNatural.succeeds(0.0, 0.0), "chance 0 never succeeds");
        for (double a = 0; a < 1; a += 0.1)
            for (double d = 0; d <= 1; d += 0.5) {
                double[] o = ShishouNatural.offset(a, d);
                double r = Math.hypot(o[0], o[1]);
                assertTrue(r >= ShishouNatural.MIN_DISTANCE - 1e-6 && r <= ShishouNatural.MAX_DISTANCE + 1e-6, "ring " + r);
            }
    }

    @Test
    void appraisalNumbersAreBoundedAndWeaknessesFollowTheType() {
        assertEquals(1, ShishouAppraisal.levelish(0, 0, 0));
        assertEquals(999, ShishouAppraisal.levelish(100000, 1000, 100));
        assertTrue((ShishouAppraisal.weaknesses("minecraft:blaze") & ShishouAppraisal.W_ICE) != 0);
        assertTrue((ShishouAppraisal.weaknesses("minecraft:zombie") & ShishouAppraisal.W_HOLY) != 0);
        assertEquals(0, ShishouAppraisal.weaknesses("minecraft:cow"));
        assertEquals(0, ShishouAppraisal.weaknesses(null));
        assertTrue(ShishouAppraisal.manaish(20) > 0);
    }
}
