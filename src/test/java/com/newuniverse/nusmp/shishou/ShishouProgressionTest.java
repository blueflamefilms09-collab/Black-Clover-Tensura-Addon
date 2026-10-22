package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The pure progression of Shishou: the XP curve, the tiers and unlocks, the mana math, the record and its saving, Skill Eater's table and the predation numbers. */
class ShishouProgressionTest {
    @Test
    void theCurveIsMonotonicCappedAndInvertible() {
        assertEquals(31L, ShishouLevels.toNext(1));
        assertEquals(285L, ShishouLevels.toNext(20));
        assertEquals(0L, ShishouLevels.toNext(ShishouLevels.MAX_LEVEL));
        long prev = -1;
        for (int l = 1; l <= ShishouLevels.MAX_LEVEL; l++) {
            long t = ShishouLevels.totalFor(l);
            assertTrue(t > prev, "totals grow at level " + l);
            prev = t;
            assertEquals(l, ShishouLevels.levelFor(t), "the start of level " + l);
            if (l < ShishouLevels.MAX_LEVEL) assertEquals(l, ShishouLevels.levelFor(ShishouLevels.totalFor(l + 1) - 1));
        }
        assertEquals(ShishouLevels.MAX_LEVEL, ShishouLevels.levelFor(Long.MAX_VALUE / 4), "above the cap stays at the cap");
        assertEquals(1, ShishouLevels.levelFor(-5));
        assertEquals(1.0, ShishouLevels.progress(ShishouLevels.capTotal()), 1e-9);
        assertTrue(ShishouLevels.capTotal() < 400_000L, "a hundred levels are reachable");
    }

    @Test
    void theTiersUnlockAtTwentyOneAndFifty() {
        assertEquals(1, ShishouSkill.tierOf(1));
        assertEquals(1, ShishouSkill.tierOf(20));
        assertEquals(2, ShishouSkill.tierOf(21));
        assertEquals(2, ShishouSkill.tierOf(49));
        assertEquals(3, ShishouSkill.tierOf(50));
        assertTrue(ShishouSkill.STOMACH.available(21, 0L));
        assertFalse(ShishouSkill.STOMACH.available(20, 0L));
        assertTrue(ShishouSkill.SPATIAL_MOTION.available(50, 0L));
        assertFalse(ShishouSkill.SPATIAL_MOTION.available(49, 0L));
        assertTrue(ShishouSkill.TELEKINESIS.available(1, 0L), "tier one is there from the first level");
        assertFalse(ShishouSkill.FIRE_MAGIC.available(100, 0L), "an absorbed skill is never given by the level");
        assertTrue(ShishouSkill.FIRE_MAGIC.available(1, 1L << ShishouSkill.FIRE_MAGIC.bit()));
    }

    @Test
    void theSkillTableIsStableAndComplete() {
        boolean[] seen = new boolean[64];
        for (ShishouSkill s : ShishouSkill.values()) {
            assertFalse(seen[s.bit()], "bit reused: " + s);
            seen[s.bit()] = true;
            assertSame(s, ShishouSkill.byBit(s.bit()));
            assertSame(s, ShishouSkill.byId(s.id()));
            assertTrue(s.langKey().startsWith("shishou.skill."));
            assertTrue(s.manaCost() >= 0 && s.cooldown() >= 0);
            if (s.kind() == ShishouSkill.Kind.ACTIVE) assertTrue(s.cooldown() > 0 && s.manaCost() > 0, s + " is cast: it has a cost and a cooldown");
            assertEquals(s.absorbed() ? s.tier() >= 1 : s.tier() == ShishouSkill.tierOf(s.unlockLevel()), true, s + " tier");
        }
        assertEquals(31, ShishouSkill.values().length);
        assertEquals(0, ShishouSkill.TELEKINESIS.bit(), "bits are only appended");
        assertEquals(30, ShishouSkill.SPATIAL_MOTION_SHORT.bit());
        assertEquals(List.of(ShishouSkill.MULTILAYER_BARRIER), ShishouSkill.unlockedBetween(9, 10));
        assertEquals(ShishouSkill.STOMACH, ShishouSkill.nextCastable(ShishouSkill.MULTILAYER_BARRIER, 21, 0L));
        assertEquals(ShishouSkill.MULTILAYER_BARRIER, ShishouSkill.nextCastable(ShishouSkill.STOMACH, 21, 0L), "the cycle wraps while Stomach is last");
        assertNull(ShishouSkill.nextCastable(null, 1, 0L), "nothing castable at level one");
    }

    @Test
    void manaIsLazyCappedAndSpendable() {
        ShishouRecord r = new ShishouRecord();
        r.refill(0L);
        double max = r.maxMana();
        assertEquals(ShishouRules.maxMana(1, 0), max, 1e-9);
        assertTrue(r.spend(0L, 50.0));
        assertEquals(max - 50.0, r.mana(0L), 1e-9);
        assertFalse(r.spend(0L, max), "cannot pay more than is there");
        assertEquals(max - 50.0, r.mana(0L), 1e-9, "a refused payment costs nothing");
        double after = r.mana(20L * 10);
        assertTrue(after > max - 50.0 && after < max, "ten seconds give back a little");
        assertEquals(max, r.mana(20L * 100000), 1e-9, "never above the pool");
        assertTrue(ShishouRules.regenPerTick(100) < 0.3, "slow");
        assertTrue(r.isLow(0L) == (r.mana(0L) < max * ShishouRules.LOW_MANA_SHARE));
    }

    @Test
    void experienceLevelsUpAndPaysTheSkillsOfTheTier() {
        ShishouRecord r = new ShishouRecord();
        ShishouRecord.Gain g = r.addExp(ShishouLevels.totalFor(21));
        assertEquals(1, g.from());
        assertEquals(21, g.to());
        assertTrue(g.leveled());
        assertTrue(r.has(ShishouSkill.STOMACH));
        assertFalse(r.has(ShishouSkill.SPATIAL_MOTION));
        assertEquals(0L, r.addExp(-10).added(), "no negative experience");
        r.addExp(Long.MAX_VALUE / 8);
        assertEquals(ShishouLevels.MAX_LEVEL, r.level());
        assertEquals(ShishouLevels.capTotal(), r.totalExp());
    }

    @Test
    void absorbedSkillsAndStepsAreCapped() {
        ShishouRecord r = new ShishouRecord();
        assertTrue(r.learn(ShishouSkill.FIRE_MAGIC));
        assertFalse(r.learn(ShishouSkill.FIRE_MAGIC), "learned once");
        assertFalse(r.learn(ShishouSkill.STOMACH), "a level skill is not absorbed");
        assertTrue(r.has(ShishouSkill.FIRE_MAGIC));
        for (int i = 0; i < 100; i++) r.step(true, true);
        assertEquals(ShishouRules.MAX_STEPS, r.manaSteps());
        assertEquals(ShishouRules.MAX_STEPS, r.attackSteps());
        assertFalse(r.step(true, true), "capped");
        assertTrue(r.maxMana() <= ShishouRules.maxMana(100, 20));
    }

    @Test
    void theRecordSurvivesSavingAndIgnoresBadData() {
        ShishouRecord r = new ShishouRecord();
        r.addExp(5000);
        r.learn(ShishouSkill.WIND_MAGIC);
        r.learn(ShishouSkill.HEALING_MAGIC);
        r.step(true, false);
        r.select(ShishouSkill.WIND_MAGIC);
        r.setBroadcast(true);
        r.countKill();
        r.spend(100L, 10.0);
        ShishouRecord b = ShishouRecord.fromMap(r.toMap());
        assertEquals(r.totalExp(), b.totalExp());
        assertEquals(r.learned(), b.learned());
        assertEquals(r.manaSteps(), b.manaSteps());
        assertEquals(r.mana(100L), b.mana(100L), 1e-9);
        assertEquals(ShishouSkill.WIND_MAGIC, b.selected());
        assertTrue(b.broadcast());
        assertEquals(1L, b.kills());
        Map<String, Long> bad = new java.util.HashMap<>();
        bad.put("exp", -50L);
        bad.put("learned", -1L);
        bad.put("msteps", 9999L);
        bad.put("sel", 777L);
        ShishouRecord c = ShishouRecord.fromMap(bad);
        assertEquals(0L, c.totalExp());
        assertEquals(ShishouRecord.validMask(), c.learned(), "only absorbed skill bits are kept");
        assertEquals(ShishouRules.MAX_STEPS, c.manaSteps());
        assertNull(c.selected());
        assertEquals(1, ShishouRecord.fromMap(null).level());
    }

    @Test
    void theWildPhaseEndsByKillsDaysOrMana() {
        assertFalse(ShishouRules.shouldGoDormant(0, 0L, 100.0, 25, 7));
        assertTrue(ShishouRules.shouldGoDormant(25, 0L, 100.0, 25, 7), "after the kills");
        assertTrue(ShishouRules.shouldGoDormant(0, 7L * ShishouRules.DAY_TICKS, 100.0, 25, 7), "after the days");
        assertTrue(ShishouRules.shouldGoDormant(0, 0L, 1.0, 25, 7), "when the wild mana is spent");
        ShishouRecord r = new ShishouRecord();
        r.startWild(50L);
        int strikes = 0;
        while (r.spendWild(ShishouRules.WILD_STRIKE_MANA)) strikes++;
        assertTrue(strikes >= 20 && strikes <= 30, "about twenty five strikes: " + strikes);
        assertTrue(ShishouRules.shouldGoDormant(0, 0L, r.wildMana(), 25, 7));
    }

    @Test
    void theNumbersAreCapped() {
        assertEquals(ShishouRules.STRIKE_CAP, ShishouRules.strikeDamage(100, 20, 4, ShishouRules.STRIKE_CAP), 1e-9);
        assertTrue(ShishouRules.strikeDamage(1, 0, 0, 60) < 8);
        assertEquals(30.0, ShishouRules.strikeDamage(100, 20, 4, 30.0), 1e-9, "the config cap holds");
        assertEquals(0.2, ShishouRules.vibrationBonus(1.0, 0.2, 0.3), 1e-9);
        assertEquals(0.3, ShishouRules.vibrationBonus(1.0, 0.9, 0.3), 1e-9, "never above the share cap");
        assertEquals(4, ShishouRules.barrierHearts(10));
        assertEquals(8, ShishouRules.barrierHearts(100));
        for (int l = 1; l <= 100; l++) {
            int h = ShishouRules.barrierHearts(l);
            assertTrue(h >= 4 && h <= 8);
            assertTrue(ShishouRules.barrierAmplifier(h) >= 1 && ShishouRules.barrierAmplifier(h) <= 3);
        }
        assertTrue(ShishouRules.blackFlamePulse(100, 5.0) <= 5.0);
        assertTrue(ShishouRules.blackLightning(100, 80.0) <= 80.0);
        assertTrue(ShishouRules.divine(100, ShishouRules.DIVINE_CAP) <= ShishouRules.DIVINE_CAP);
        assertTrue(ShishouRules.heldAttack(100) <= ShishouRules.BASE_ATTACK + ShishouRules.ATTACK_BONUS_CAP);
        assertTrue(ShishouRules.sharingSpeed(99) <= ShishouRules.SHARING_SPEED_CAP);
        assertEquals("aggro", ShishouRules.clip(ShishouRules.STATE_AGGRO));
        assertEquals("idle", ShishouRules.clip(99));
    }

    @Test
    void skillEaterTableStealsByTypeAndRespectsTheScale() {
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouStealTable.lookup("minecraft:blaze", false).skill());
        assertEquals(ShishouSkill.WATER_ICE_MAGIC, ShishouStealTable.lookup("minecraft:stray", false).skill());
        assertEquals(ShishouSkill.WATER_ICE_MAGIC, ShishouStealTable.lookup("minecraft:snow_golem", false).skill(), "the exact row comes before the golem keyword");
        assertNull(ShishouStealTable.lookup("minecraft:creeper", false), "only a charged creeper");
        assertEquals(ShishouSkill.WIND_MAGIC, ShishouStealTable.lookup("minecraft:creeper", true).skill());
        assertEquals(ShishouSkill.SPATIAL_MOTION_SHORT, ShishouStealTable.lookup("minecraft:enderman", false).skill());
        assertEquals(ShishouSkill.POISON_COAT, ShishouStealTable.lookup("minecraft:cave_spider", false).skill());
        assertEquals(ShishouSkill.DARKNESS_MAGIC, ShishouStealTable.lookup("minecraft:vex", false).skill());
        assertEquals(ShishouSkill.HEALING_MAGIC, ShishouStealTable.lookup("minecraft:witch", false).skill());
        assertEquals(ShishouSkill.PURIFICATION, ShishouStealTable.lookup("minecraft:zombie_villager", false).skill());
        assertEquals(ShishouSkill.EARTH_MAGIC, ShishouStealTable.lookup("minecraft:ravager", false).skill());
        assertEquals(ShishouSkill.FIRE_MAGIC, ShishouStealTable.lookup("tensura:flame_wolf", false).skill(), "another mod's mob by the words of its id");
        assertNull(ShishouStealTable.lookup("minecraft:pig", false));
        assertNull(ShishouStealTable.lookup("", false));
        assertNull(ShishouStealTable.lookup(null, false));
        ShishouStealTable.Row blaze = ShishouStealTable.lookup("minecraft:blaze", false);
        assertEquals(0.4, ShishouStealTable.chance(blaze, 100), 1e-9);
        assertEquals(0.8, ShishouStealTable.chance(blaze, 200), 1e-9);
        assertEquals(0.0, ShishouStealTable.chance(blaze, 0), 1e-9, "0 turns theft off");
        assertEquals(1.0, ShishouStealTable.chance(ShishouStealTable.lookup("minecraft:creeper", true), 10000), 1e-9, "never above one");
        assertTrue(ShishouStealTable.steals(blaze, 100, 0.39));
        assertFalse(ShishouStealTable.steals(blaze, 100, 0.41));
        assertTrue(ShishouStealTable.size() <= 64, "the table is bounded");
        for (ShishouStealTable.Row row : ShishouStealTable.rows()) assertTrue(row.skill().absorbed(), row.skill() + " is stolen, so it is an absorbed skill");
        int fire = ShishouStealTable.elements(1L << ShishouSkill.FIRE_MAGIC.bit() | 1L << ShishouSkill.POISON_COAT.bit());
        assertEquals(1 << ShishouStealTable.Element.FIRE.ordinal() | 1 << ShishouStealTable.Element.POISON.ordinal(), fire);
    }

    @Test
    void predationValuesDependOnTheMobTierAndTheStone() {
        assertEquals(0, ShishouPredation.tier(5));
        assertEquals(1, ShishouPredation.tier(20));
        assertEquals(2, ShishouPredation.tier(100));
        assertEquals(3, ShishouPredation.tier(300));
        assertEquals(4, ShishouPredation.tier(1000));
        assertTrue(ShishouPredation.stoneExp("tensura:magic_stone", 1, 4) > ShishouPredation.stoneExp("tensura:magic_stone", 1, 0), "a stone of a boss is worth more");
        assertTrue(ShishouPredation.stoneExp("tensura:high_quality_magic_crystal", 1, 0) > ShishouPredation.stoneExp("tensura:low_quality_magic_crystal", 1, 0));
        assertEquals(0L, ShishouPredation.stoneExp("minecraft:dirt", 64, 4));
        assertTrue(ShishouPredation.stoneExp("tensura:high_quality_magic_crystal", 64, 4) <= ShishouPredation.MAX_EXP_PER_ABSORB);
        assertTrue(ShishouPredation.killExp(20, false) > 0);
        assertTrue(ShishouPredation.killExp(10000, true) <= ShishouPredation.MAX_EXP_PER_KILL);
        assertTrue(ShishouPredation.dropExp(64, 3) <= ShishouPredation.MAX_EXP_PER_ABSORB);
        assertEquals(0L, ShishouPredation.dropExp(0, 0));
        assertEquals(ShishouPredation.MAX_EXP_PER_KILL, ShishouPredation.capKill(Long.MAX_VALUE));
    }
}
