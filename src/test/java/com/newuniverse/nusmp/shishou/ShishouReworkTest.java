package com.newuniverse.nusmp.shishou;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Shishou rework (docs/items/shishou_rework.md): pet mode brain, canon numbers, the borrowed-skill table and its JSON file, the effect geometry caps, the appended routing, the record's new fields (and that an
 * old save still loads), and source scans that pin the soft-compat rule (no optional mod is class-linked) and the server-safe, bounded shape of the new files.
 */
class ShishouReworkTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(SRC.resolve(rel)).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    // ================================================================ pet mode
    @Test
    void thePetDecidesByManaOrderAndTheWielderHealth() {
        ShishouPet.Order f = ShishouPet.Order.FOLLOW, s = ShishouPet.Order.STAY, a = ShishouPet.Order.ATTACK;
        assertEquals(ShishouPet.State.RECALL, ShishouPet.decide(f, 0.0, 1.0, false), "no mana: the service puts him back in the hand");
        assertEquals(ShishouPet.State.RETREAT, ShishouPet.decide(f, ShishouPet.RETREAT_SHARE - 0.01, 1.0, false));
        assertEquals(ShishouPet.State.RETREAT, ShishouPet.decide(f, 0.2, 1.0, true), "a retreating sword keeps retreating until he has mana again");
        assertEquals(ShishouPet.State.HUNT, ShishouPet.decide(f, ShishouPet.RESUME_SHARE + 0.01, 1.0, true));
        assertEquals(ShishouPet.State.HUNT, ShishouPet.decide(f, 0.9, 1.0, false));
        assertEquals(ShishouPet.State.GUARD, ShishouPet.decide(f, 0.9, ShishouPet.HURT_OWNER_SHARE, false), "the wielder is hurt: he guards them");
        assertEquals(ShishouPet.State.STAY, ShishouPet.decide(s, 0.9, 0.1, false), "a stay order is kept even when the wielder is hurt");
        assertEquals(ShishouPet.State.HUNT, ShishouPet.decide(a, 0.9, 1.0, false));
        assertTrue(ShishouPet.RETREAT_SHARE < ShishouPet.RESUME_SHARE, "a gap, so he does not flicker");
    }

    @Test
    void theHuntRadiusIsCappedAndFollowsTheState() {
        for (int leash : new int[]{4, 24, 48}) {
            for (ShishouPet.State st : ShishouPet.State.values()) for (ShishouPet.Order o : ShishouPet.Order.values()) {
                double r = ShishouPet.huntRadius(st, o, leash);
                assertTrue(r >= 0.0 && r <= ShishouPet.SCAN_CAP, st + " " + o + " " + leash + " -> " + r);
            }
        }
        assertEquals(0.0, ShishouPet.huntRadius(ShishouPet.State.RETREAT, ShishouPet.Order.FOLLOW, 24));
        assertEquals(ShishouPet.STAY_GUARD, ShishouPet.huntRadius(ShishouPet.State.STAY, ShishouPet.Order.STAY, 24));
        assertEquals(24 + ShishouPet.ATTACK_EXTRA, ShishouPet.huntRadius(ShishouPet.State.HUNT, ShishouPet.Order.ATTACK, 24));
        assertEquals(24.0, ShishouPet.huntRadius(ShishouPet.State.HUNT, ShishouPet.Order.FOLLOW, 24));
        assertFalse(ShishouPet.aroundOwner(ShishouPet.State.STAY));
        assertTrue(ShishouPet.aroundOwner(ShishouPet.State.HUNT));
        assertTrue(ShishouPet.tooFar(24 + ShishouPet.FAR_SLACK + 1, 24, ShishouPet.Order.FOLLOW));
        assertFalse(ShishouPet.tooFar(24 + ShishouPet.FAR_SLACK + 1, 24, ShishouPet.Order.ATTACK), "an attack order chases farther");
        assertTrue(ShishouPet.followStart(ShishouPet.State.RETREAT) < ShishouPet.followStart(ShishouPet.State.HUNT));
    }

    @Test
    void thePetNeverStrikesAProtectedKindAndOnlyHuntsWhatItMay() {
        for (ShishouPet.Reason r : ShishouPet.Reason.values()) assertFalse(ShishouPet.mayAttack(r, true, true, true), r + ": a protected kind is never attacked");
        assertTrue(ShishouPet.mayAttack(ShishouPet.Reason.HOSTILE, true, false, false));
        assertFalse(ShishouPet.mayAttack(ShishouPet.Reason.HOSTILE, false, true, false), "hostile only");
        assertFalse(ShishouPet.mayAttack(ShishouPet.Reason.OWNER_HIT, false, false, false), "a peaceful creature the wielder punched is not a target");
        assertTrue(ShishouPet.mayAttack(ShishouPet.Reason.OWNER_HIT, false, true, false), "but one that is going for the wielder is");
        assertTrue(ShishouPet.mayAttack(ShishouPet.Reason.OWNER_ATTACKER, false, false, false), "what hurt the wielder has made itself an enemy");
        assertTrue(ShishouPet.fresh(100, 150) && !ShishouPet.fresh(100, 100 + ShishouPet.OWNER_TARGET_TICKS) && !ShishouPet.fresh(0, 10) && !ShishouPet.fresh(200, 100));
    }

    @Test
    void aWallTakesTheVelocityAlongItsNormalAndAddsALift() {
        double[] v = ShishouPet.deflect(1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.12);
        assertEquals(0.0, v[0], 1e-9, "no speed into the wall");
        assertEquals(0.12, v[1], 1e-9, "a side wall gives a lift");
        double[] free = ShishouPet.deflect(-1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.12);
        assertEquals(-1.0, free[0], 1e-9, "already moving away from it: untouched");
        double[] floor = ShishouPet.deflect(0.5, -0.4, 0.0, 0.0, 1.0, 0.0, 0.12);
        assertEquals(0.0, floor[1], 1e-9);
        assertEquals(0.5, floor[0], 1e-9, "a floor keeps the sideways speed and adds no lift");
        assertEquals(10.0, ShishouPet.probesPerSecond(), 1e-9, "ten wall probes a second at most");
    }

    @Test
    void ordersCycleAndSurviveBadNumbers() {
        assertEquals(ShishouPet.Order.STAY, ShishouPet.Order.FOLLOW.next());
        assertEquals(ShishouPet.Order.ATTACK, ShishouPet.Order.STAY.next());
        assertEquals(ShishouPet.Order.FOLLOW, ShishouPet.Order.ATTACK.next());
        assertEquals(ShishouPet.Order.FOLLOW, ShishouPet.Order.byOrdinal(99));
        assertEquals(ShishouPet.Order.FOLLOW, ShishouPet.Order.byOrdinal(-1));
        assertEquals(ShishouPet.Order.ATTACK, ShishouPet.Order.byId("Attack"));
        assertNull(ShishouPet.Order.byId("bite"));
        assertEquals("shishou.order.stay", ShishouPet.Order.STAY.langKey());
    }

    // ================================================================ canon numbers
    @Test
    void theCanonNumbersAreBounded() {
        assertEquals(1.0, ShishouCanon.regenFactor(false, false), 1e-9);
        assertEquals(1.5, ShishouCanon.regenFactor(true, false), 1e-9);
        assertEquals(2.0, ShishouCanon.regenFactor(true, true), 1e-9);
        assertEquals(2.0, ShishouCanon.regenFactor(false, true), 1e-9, "High-Speed alone is the better of the two, not a sum");
        assertEquals(110L, ShishouCanon.evolvedExp(100L, true));
        assertEquals(100L, ShishouCanon.evolvedExp(100L, false));
        assertTrue(ShishouCanon.evolveDue(25) && ShishouCanon.evolveDue(50) && !ShishouCanon.evolveDue(0) && !ShishouCanon.evolveDue(26));
        assertEquals(115.0, ShishouCanon.indexed(100.0, true), 1e-9);
        assertEquals(100.0, ShishouCanon.indexed(100.0, false), 1e-9);
        assertEquals(30, ShishouCanon.maxBladeHits(), "five waves of six");
        assertEquals(120, ShishouCanon.maxSanctuaryTouches(), "ten pulses of twelve");
        assertEquals(10, ShishouCanon.sanctuaryPulses());
        assertEquals(30.0, ShishouCanon.bladeWave(100.0, 60.0), 1e-9);
        assertEquals(60.0, ShishouCanon.bladeWave(1000.0, 60.0), 1e-9, "capped by the strike cap");
        assertTrue(ShishouCanon.GRASP_TARGETS <= 6 && ShishouCanon.INDEX_TARGETS <= 8 && ShishouCanon.BLADE_TARGETS <= 8 && ShishouCanon.SANCTUARY_TARGETS <= 16);
    }

    @Test
    void theNewSkillsAreAppendedAndRoutedOnce() {
        assertEquals(38, ShishouSkill.values().length);
        for (int i = 0; i < 31; i++) assertTrue(ShishouSkill.values()[i].bit() < 31, "an old skill keeps a bit below 31: " + ShishouSkill.values()[i]);
        List<ShishouSkill> added = List.of(ShishouSkill.SELF_REPAIR, ShishouSkill.HIGH_SPEED_SELF_REPAIR, ShishouSkill.SELF_EVOLUTION, ShishouSkill.TELEKINETIC_GRASP, ShishouSkill.AKASHIC_INDEX, ShishouSkill.RECORD_BLADES, ShishouSkill.AKASHIC_SANCTUARY);
        for (int i = 0; i < added.size(); i++) assertEquals(31 + i, added.get(i).bit());
        assertEquals(ShishouModes.Mode.AKASHIC, ShishouModes.modeOf(ShishouSkill.AKASHIC_SANCTUARY));
        assertEquals(ShishouModes.Mode.AKASHIC, ShishouModes.modeOf(ShishouSkill.TELEKINETIC_GRASP));
        assertNull(ShishouModes.modeOf(ShishouSkill.SELF_REPAIR), "a passive: no mode");
        assertEquals(List.of(ShishouSkill.TELEKINETIC_GRASP), ShishouModes.available(ShishouModes.Mode.AKASHIC, 12, 0L));
        assertEquals(List.of(ShishouSkill.TELEKINETIC_GRASP, ShishouSkill.AKASHIC_INDEX, ShishouSkill.RECORD_BLADES, ShishouSkill.AKASHIC_SANCTUARY), ShishouModes.available(ShishouModes.Mode.AKASHIC, 100, 0L));
        assertEquals(9, ShishouModes.COUNT);
        assertEquals(ShishouModes.Mode.AKASHIC, ShishouModes.byIndex(7));
        assertEquals(ShishouModes.Mode.BORROWED, ShishouModes.byIndex(8));
        assertEquals(ShishouModes.Mode.COUNSEL, ShishouModes.byIndex(6), "the 2.1.0 mode indexes stay put");
    }

    @Test
    void thePressRoutesOrdersAndBorrowedSkills() {
        assertEquals(ShishouModes.Action.ORDER, ShishouModes.action(ShishouModes.Mode.TELEKINESIS, true, 1, true));
        assertEquals(ShishouModes.Action.THROW_RECALL, ShishouModes.action(ShishouModes.Mode.TELEKINESIS, true, 1, false), "pet mode off or the sword in the hand: the 2.1.0 routing");
        assertEquals(ShishouModes.Action.THROW_RECALL, ShishouModes.action(ShishouModes.Mode.TELEKINESIS, false, 1, true));
        assertEquals(ShishouModes.Action.BORROW_CAST, ShishouModes.action(ShishouModes.Mode.BORROWED, false, 0, true));
        assertEquals(ShishouModes.Action.BORROW_CYCLE, ShishouModes.action(ShishouModes.Mode.BORROWED, true, 0, false));
        assertEquals(ShishouModes.Action.CYCLE, ShishouModes.action(ShishouModes.Mode.AKASHIC, true, 4, true));
    }

    // ================================================================ the record
    @Test
    void theRecordKeepsTheNewFieldsAndAnOldSaveStillLoads() {
        ShishouRecord r = new ShishouRecord();
        r.setLevel(100);
        r.refill(0L);
        ShishouBorrowTable.Entry e = ShishouBorrowTable.entries().get(0);
        assertTrue(r.learnBorrowed(e));
        assertFalse(r.learnBorrowed(e), "learned once");
        r.selectBorrowed(e.idx());
        r.setPetOrder(ShishouPet.Order.ATTACK);
        ShishouRecord back = ShishouRecord.fromMap(r.toMap());
        assertEquals(r.borrowed(), back.borrowed());
        assertEquals(e.idx(), back.borrowedSel());
        assertEquals(ShishouPet.Order.ATTACK, back.petOrder());
        Map<String, Long> old = r.toMap();
        old.remove("bmask");
        old.remove("bsel");
        old.remove("ord");
        ShishouRecord legacy = ShishouRecord.fromMap(old);
        assertEquals(0L, legacy.borrowed());
        assertEquals(-1, legacy.borrowedSel());
        assertEquals(ShishouPet.Order.FOLLOW, legacy.petOrder());
        old.put("bsel", 9999L);
        old.put("ord", -5L);
        ShishouRecord wild = ShishouRecord.fromMap(old);
        assertTrue(wild.borrowedSel() < ShishouBorrowTable.MAX && wild.borrowedSel() >= -1, "a bad pick is clamped");
        assertEquals(ShishouPet.Order.FOLLOW, wild.petOrder());
    }

    @Test
    void theCanonRegenerationRaisesTheManaOnlyWhenTheSettingIsOn() {
        ShishouRecord r = new ShishouRecord();
        r.setLevel(100);
        r.learn(ShishouSkill.SELF_REPAIR);
        assertFalse(r.learn(ShishouSkill.SELF_REPAIR), "an unlocked-by-level skill is not an absorbed one: nothing to learn");
        assertTrue(r.has(ShishouSkill.HIGH_SPEED_SELF_REPAIR) && r.has(ShishouSkill.SELF_REPAIR));
        r.refill(0L);
        r.spend(0L, r.maxMana());
        assertEquals(1.0, r.regenFactor(), 1e-9, "the setting defaults to the 3.x numbers until the service mirrors it");
        double plain = r.mana(40L);
        r.setCanonRegen(true);
        assertEquals(2.0, r.regenFactor(), 1e-9);
        assertTrue(r.mana(40L) > plain, "faster with the canon setting");
        assertTrue(r.mana(1000000L) <= r.maxMana() + 1e-9, "never above the pool");
    }

    // ================================================================ the borrowed table
    @Test
    void theShippedTableLoadsIsStableAndBounded() throws IOException {
        List<ShishouBorrowTable.Entry> all = ShishouBorrowTable.entries();
        assertFalse(all.isEmpty(), "the jar carries the table");
        assertTrue(all.size() <= ShishouBorrowTable.MAX);
        Set<Integer> idx = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (ShishouBorrowTable.Entry e : all) {
            assertTrue(idx.add(e.idx()), "idx reused: " + e.idx());
            assertTrue(ids.add(e.id()), "id twice: " + e.id());
            assertTrue(e.mana() >= 1 && e.mana() <= ShishouBorrowTable.MANA_MAX);
            assertTrue(e.cooldown() >= ShishouBorrowTable.COOLDOWN_MIN && e.cooldown() <= ShishouBorrowTable.COOLDOWN_MAX);
            assertTrue(e.level() >= 0 && e.level() <= ShishouBorrowTable.LEVEL_MAX);
            assertTrue(e.level() > 0 || e.stealable(), e.id() + " can be had");
            assertEquals(e.kind() == ShishouBorrowTable.Kind.ECHO, e.id().startsWith("echo:"));
        }
        long mulWithIds = all.stream().filter(e -> e.source().equals(ShishouBorrowTable.MULTIVERSE)).count();
        assertTrue(mulWithIds >= 10, "the grimoire echoes are in");
        String raw = Files.readString(Path.of("src/main/resources/data/anime_multiverse/shishou/borrowed_skills.json"));
        assertEquals(all.size(), ShishouBorrowTable.parse(raw).size(), "the file on disk is the table in the jar, and no entry of it is dropped as invalid");
    }

    @Test
    void aBrokenEntryIsSkippedNotTheTable() {
        String json = "{\"skills\":[" +
                "{\"idx\":1,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:fire_ball\",\"label\":\"Fire Ball\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":1,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:dup\",\"label\":\"Dup\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":2,\"source\":\"evil\",\"kind\":\"SKILL\",\"id\":\"tensura:x\",\"label\":\"X\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":3,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"Not An Id\",\"label\":\"X\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":4,\"source\":\"tensura\",\"kind\":\"ECHO\",\"id\":\"tensura:flame\",\"label\":\"X\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":99,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:y\",\"label\":\"Y\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":5,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:z\",\"label\":\"Z\",\"level\":0,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":6,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:w\",\"label\":\"W\",\"level\":0,\"mana\":10,\"cooldown\":100,\"steal\":[\"zombie\"],\"percent\":20}," +
                "{\"idx\":7,\"source\":\"nightmares\",\"kind\":\"SKILL\",\"id\":\"trnightmare:v\",\"label\":\"V\",\"level\":5,\"mana\":9999,\"cooldown\":1}]}";
        List<ShishouBorrowTable.Entry> t = ShishouBorrowTable.parse(json);
        assertEquals(List.of(1, 6, 7), t.stream().map(ShishouBorrowTable.Entry::idx).toList(), "duplicate idx, unknown source, bad id, echo with a skill id, idx out of range and a never-had entry are skipped");
        assertEquals(ShishouBorrowTable.MANA_MAX, t.get(2).mana(), "numbers are clamped");
        assertEquals(ShishouBorrowTable.COOLDOWN_MIN, t.get(2).cooldown());
        assertTrue(ShishouBorrowTable.parse("not json").isEmpty());
        assertTrue(ShishouBorrowTable.parse("{}").isEmpty());
        assertTrue(ShishouBorrowTable.parse("[]").isEmpty());
    }

    @Test
    void availabilityStealingAndCyclingFollowTheLevelAndTheMask() {
        List<ShishouBorrowTable.Entry> all = ShishouBorrowTable.parse("{\"skills\":[" +
                "{\"idx\":0,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:a\",\"label\":\"A\",\"level\":10,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":3,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:b\",\"label\":\"B\",\"level\":50,\"mana\":10,\"cooldown\":100}," +
                "{\"idx\":9,\"source\":\"tensura\",\"kind\":\"SKILL\",\"id\":\"tensura:c\",\"label\":\"C\",\"level\":0,\"mana\":10,\"cooldown\":100,\"steal\":[\"golem\"],\"percent\":50}]}");
        assertEquals(1, ShishouBorrowTable.availableList(all, 10, 0L).size());
        assertEquals(2, ShishouBorrowTable.availableList(all, 50, 0L).size());
        assertEquals(3, ShishouBorrowTable.availableList(all, 50, 1L << 9).size(), "Skill Eater's bit opens the level-0 entry");
        ShishouBorrowTable.Entry c = all.get(2);
        assertSame(c, ShishouBorrowTable.stealMatch(all, "tensura:iron_golem_like", 10, 0L), "a part of the entity path names the word");
        assertNull(ShishouBorrowTable.stealMatch(all, "tensura:iron_golem_like", 10, 1L << 9), "already had");
        assertNull(ShishouBorrowTable.stealMatch(all, "minecraft:zombie", 10, 0L));
        assertNull(ShishouBorrowTable.stealMatch(all, null, 10, 0L));
        assertTrue(ShishouBorrowTable.steals(c, 100, 0.49) && !ShishouBorrowTable.steals(c, 100, 0.5));
        assertFalse(ShishouBorrowTable.steals(c, 0, 0.0), "a steal percentage of 0 steals nothing");
        assertTrue(ShishouBorrowTable.steals(c, 300, 0.99), "capped at certainty, never beyond");
        assertEquals(0, ShishouBorrowTable.cycle(all, -1, 50, 0L).idx());
        assertEquals(3, ShishouBorrowTable.cycle(all, 0, 50, 0L).idx());
        assertEquals(0, ShishouBorrowTable.cycle(all, 3, 50, 0L).idx(), "wraps");
        assertEquals(0, ShishouBorrowTable.current(all, 3, 10, 0L).idx(), "a pick that is no longer open falls back to the first open one");
        assertEquals(3, ShishouBorrowTable.current(all, 3, 50, 0L).idx());
        assertNull(ShishouBorrowTable.current(all, -1, 1, 0L));
        assertEquals(64, ShishouBorrowTable.lookupsPerCast());
    }

    // ================================================================ the geometry caps
    @Test
    void everyEffectStepStaysUnderTheParticleCap() {
        for (double r : new double[]{0.1, 0.7, 4.0, 6.0, 24.0, 100.0}) {
            assertTrue(ShishouVfx.ringPoints(r) >= 8 && ShishouVfx.ringPoints(r) <= ShishouVfx.RING_MAX, "ring " + r);
            assertTrue(ShishouVfx.bladeWaveParticles(r) <= ShishouVfx.STEP_MAX && ShishouVfx.sanctuaryPulseParticles(r) <= ShishouVfx.STEP_MAX, "step " + r);
        }
        assertTrue(ShishouVfx.sanctuaryPulseParticles(ShishouCanon.SANCTUARY_RADIUS) <= ShishouVfx.STEP_MAX);
        assertEquals(5.0, ShishouVfx.columnY(0, 8, 5.0), 1e-9);
        assertEquals(0.0, ShishouVfx.columnY(8, 8, 5.0), 1e-9);
        assertEquals(3.0, ShishouVfx.ringX(0, 8, 3.0, 0.0), 1e-9);
        assertEquals(0.0, ShishouVfx.ringZ(0, 8, 3.0, 0.0), 1e-9);
        assertEquals(-ShishouVfx.phase(7, false), ShishouVfx.phase(7, true), 1e-9, "the inner ring turns the other way");
        assertEquals(2.0, ShishouVfx.spiralY(10, 10, 2.0), 1e-9);
        assertEquals(3.0, ShishouVfx.ringX(0, 0, 3.0, 0.0), 1e-9, "no division by zero for an empty ring");
    }

    // ================================================================ source scans
    @Test
    void theNewPureClassesNameNoMinecraftClass() throws IOException {
        for (String f : new String[]{"ShishouPet", "ShishouCanon", "ShishouBorrowTable", "ShishouVfx"}) {
            String c = code("shishou/" + f + ".java");
            assertFalse(c.contains("import net.minecraft") || c.contains("import net.neoforged"), f + " is pure: it is unit tested without the game");
        }
    }

    @Test
    void noOptionalModIsClassLinkedAndTheGlueGatesOnTheModList() throws IOException {
        for (String f : new String[]{"ShishouBorrow", "ShishouBorrowTable", "ShishouConfig", "ShishouService", "ShishouAbilities", "ShishouPetGoals", "ShishouEntity"}) {
            String c = code("shishou/" + f + ".java");
            assertFalse(Pattern.compile("(?i)import\\s+(com|net|io)\\.[\\w.]*(trnightmare|mysticism)").matcher(c).find(), f + " links an optional mod");
        }
        String b = code("shishou/ShishouBorrow.java");
        assertTrue(b.contains("ModList.get().isLoaded(") && b.contains("getSkillRegistry().get("), "optional skills are looked up by id, behind the mod list");
        assertTrue(b.contains("catch (RuntimeException | LinkageError"), "a skill of another mod that throws never breaks the tick (narrowed from Throwable by the catch audit: an OutOfMemoryError is not swallowed)");
        assertFalse(b.contains("Minecraft.getInstance"), "server side only");
    }

    @Test
    void theNewWorkIsServerSafeAndBounded() throws IOException {
        StringBuilder all = new StringBuilder();
        for (String f : new String[]{"ShishouBorrow", "ShishouAbilities", "ShishouPetGoals", "ShishouPet", "ShishouCanon", "ShishouVfx", "ShishouBorrowTable"}) all.append(code("shishou/" + f + ".java")).append('\n');
        String s = all.toString();
        assertFalse(Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|Minecraft\\.getInstance\\(").matcher(s).find(), "no client class");
        assertFalse(s.contains("sendParticles"), "particles go through PerfParticles");
        assertFalse(s.contains("getEntitiesOfClass("), "every entity scan is the one capped helper of ShishouTargets");
        assertFalse(s.contains("setBlock(") || s.contains("destroyBlock(") || s.contains("addFreshEntity("), "no block changed, no entity spawned");
        assertFalse(s.contains("PacketDistributor"), "no packet of its own");
        assertFalse(Pattern.compile("while\\s*\\(true\\)").matcher(s).find(), "no unbounded loop");
        String ab = code("shishou/ShishouAbilities.java");
        assertTrue(ab.contains("SpellRuntime.gone"), "a timer that outlives its wielder ends");
        String live = code("shishou/ShishouLive.java");
        assertTrue(live.contains("ShishouAbilities.forget(who)") && live.contains("ShishouAbilities.clearAll()"), "the index names are dropped at every end");
        String ent = code("shishou/ShishouEntity.java");
        assertTrue(ent.contains("isPet() && ((tickCount + getId()) % ShishouPet.PROBE_GAP) == 0"), "the wall probe is throttled and only for the pet");
        assertTrue(ent.contains("hasChunkAt("), "the probe loads no chunk");
    }

    @Test
    void theNewSettingsExistAndDefaultToTheNewBehaviourWithAnOffSwitch() throws IOException {
        String cfg = code("shishou/ShishouConfig.java");
        for (String k : new String[]{"\"mode\"", "\"skills\"", "\"borrowedSkills\"", "\"tensura\"", "\"nightmares\"", "\"mysticism\"", "\"multiverse\"", "\"borrowGapTicks\""}) assertTrue(cfg.contains(k), "setting " + k);
        assertTrue(cfg.contains("b.push(\"pet\")") && cfg.contains("b.push(\"canon\")") && cfg.contains("b.push(\"compat\")"));
        String goals = code("shishou/ShishouGoals.java");
        assertTrue(goals.contains("sword.isPet()"), "the old follow goal stands down only in pet mode: off = the old behaviour");
        String svc = code("shishou/ShishouService.java");
        assertTrue(svc.contains("ShishouConfig.canonPassives()"), "the canon passives are one switch");
    }

    @Test
    void theNewLangKeysAreInTheFragmentsAndInEnUs() throws IOException {
        String en = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        String frag = Files.readString(Path.of("tools/lang/shishou_rework.json")) + Files.readString(Path.of("tools/lang/shishou.json"));
        for (ShishouSkill s : ShishouSkill.values()) assertTrue(en.contains("\"" + s.langKey() + "\"") && en.contains("\"" + s.langKey() + ".jp\""), "en_us: " + s.langKey());
        for (ShishouModes.Mode m : ShishouModes.Mode.values()) assertTrue(en.contains("\"" + m.langKey() + "\"") && en.contains("\"anime_multiverse.skill.mode.shishou." + m.id() + "\""), "en_us: mode " + m.id());
        for (ShishouPet.Order o : ShishouPet.Order.values()) assertTrue(en.contains("\"" + o.langKey() + "\"") && frag.contains("\"" + o.langKey() + "\""), "order text " + o.id());
        for (String k : new String[]{"shishou.msg.order", "shishou.msg.pet_off", "shishou.msg.borrowed_learned", "shishou.msg.borrow_throw", "shishou.msg.borrow_none", "shishou.msg.borrow_wait", "shishou.msg.borrow_pick"})
            assertTrue(en.contains("\"" + k + "\""), "en_us: " + k);
    }

    @Test
    void theNewSelfTestsAreRegisteredUnderTheSmokePrefix() throws IOException {
        String st = Files.readString(SRC.resolve("shishou/ShishouSelfTests.java"));
        for (String k : new String[]{"relic.shishou_pet", "relic.shishou_canon", "relic.shishou_borrow", "relic.shishou_abilities"}) assertTrue(st.contains("\"" + k + "\""), k);
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains("relic.shishou") || smoke.contains("relic."), "the smoke list covers the relic prefix");
    }
}
