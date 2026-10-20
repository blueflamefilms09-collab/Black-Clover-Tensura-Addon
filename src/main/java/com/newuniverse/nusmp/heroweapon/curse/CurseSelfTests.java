package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroSummon;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.safe.RestState;
import com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the Curse Series can be played by its author, so the CI server smoke test runs this in the real game ({@code /multiverse mega selftest}, key
 * {@code hero.curse}; the smoke script expects the prefix {@code hero}). A stand-in player is bound to a Seven Star weapon (no Holy uniqueness rule in the way) and
 * the REAL service is driven with an explicit clock and the default numbers, so the check does not depend on the server's config or on waiting: a despair report is
 * counted once per pause and a tamed wolf is recognised as an ally who matters to its owner; an offer opens, and the Embrace takes 4 hearts, then 2, then 2 and the
 * maximum health never goes under four hearts (also for a wielder whose maximum is already small); corruption at 1000 starts an Overrun that LOCKS the wielder
 * ({@code HeroCurse.locked}), swings at the nearest living thing, and, when its time is up, releases, leaves 400 corruption and starts the weakness; the charm clears
 * the corruption and gives hearts back once; a finished rest clears half; every sin's downside stays inside its bounds (Wrath's floor, Gluttony's magicule bite that leaves the food bar alone, Sloth's
 * and Greed's slow, Lust's cliff check); and a technique with no curse form worn changes nothing. Everything the test changes is put back: the bond is released, the
 * entities removed, the attribute and the rest record restored.
 */
final class CurseSelfTests {
    private CurseSelfTests() {}

    private static final String PLAYER = "hero_curse_tester";

    static void register() { SelfTests.register("hero.curse", CurseSelfTests::curse); }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException(what);
    }

    private static HeroForm synthetic(Sin sin, Map<String, String> skills) {
        return new HeroForm(HeroKind.HAMMER, "curse_probe", "Probe", "", 5, List.of(), List.of(), 0, Map.of(), Map.of(), skills, List.of(), HeroForm.LOOK_CURSE, true, sin, 6);
    }

    private static SelfTests.Result curse(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.player(level, PLAYER, 0.5, 120.0, 0.5);
        CurseTuning t = CurseTuning.defaults();
        List<LivingEntity> spawned = new ArrayList<>();
        double baseMax = fp.getMaxHealth();
        long restBefore = fp.hasData(SafeRegistry.REST_STATE) ? fp.getData(SafeRegistry.REST_STATE).lastRest : -1L;
        List<String> done = new ArrayList<>();
        try {
            HeroBonds.release(fp);
            String refused = HeroBonds.bind(fp, HeroKind.HAMMER);
            if (refused != null) return SelfTests.Result.fail("the stand-in could not bind the Vassal Hammer: " + refused);
            HeroBond bond = HeroBonds.of(fp);
            long now = level.getGameTime() + 1000L;
            CurseEffects.clearModifiers(fp);
            baseMax = fp.getMaxHealth();

            // ---- the config reaches the machine with sane numbers
            CurseTuning cfg = CurseConfig.tuning();
            check(cfg.offerThreshold >= 50 && cfg.taxFirstHearts >= 0 && cfg.minHearts >= 1 && cfg.overrunMinSeconds >= 1, "the config produced a broken tuning");
            check(HeroCurse.despair(fp) == 0 && HeroCurse.corruption(fp) == 0 && !HeroCurse.locked(fp) && !HeroCurse.active(fp), "a fresh bond is not cursed");
            done.add("fresh bond clean");

            // ---- despair: counted once per pause, an owned wolf is an ally who matters to its owner
            check(CurseService.addDespair(fp, Cause.PET_DEATH, t.amount(Cause.PET_DEATH), now, t) == 150, "a pet's death adds 150 despair");
            check(CurseService.addDespair(fp, Cause.PET_DEATH, 150, now + 50, t) == 0, "the same cause inside its pause is not counted twice");
            check(CurseService.addDespair(fp, Cause.PET_DEATH, 150, now + 100, t) == 150, "after its pause it counts again");
            check(HeroCurse.despair(fp) == 300, "despair is 300 after two counted pet deaths: " + HeroCurse.despair(fp));
            if (CurseConfig.enabled()) {                                  // the public report is ignored while the master switch is off
                HeroCurse.addDespair(fp, 40, "a_new_system");
                check(HeroCurse.despair(fp) == 340, "another system's report is added: " + HeroCurse.despair(fp));
            } else {
                CurseService.addDespair(fp, Cause.OTHER, 40, now + 10, t);
            }
            boolean chunk = level.getChunkSource().getChunkNow(0, 0) != null;
            if (chunk) {
                Wolf wolf = EntityType.WOLF.create(level);
                if (wolf != null) {
                    wolf.setPos(2.5, 120.0, 0.5);
                    wolf.tame(fp);
                    CurseAllies.Who who = CurseAllies.classify(wolf);
                    check(who.kind() == CurseAllies.Kind.PET && fp.getUUID().equals(who.owner()), "a tamed wolf is a pet of its owner");
                    check(CurseAllies.mattersTo(who, wolf, fp), "the wolf matters to its owner");
                    check(CurseAllies.isBetrayal(fp, wolf), "a creature the wielder owns hurting them is a betrayal");
                    wolf.discard();
                }
            }
            done.add("despair paused, pet is an ally");

            // ---- the offer: a weapon whose forms hold no curse form never whispers; a forced offer opens and can be declined
            CurseAdmin.setDespair(fp, 900, now + 150, t);
            CurseService.tick(fp, now + 200, true, t);
            List<Sin> candidates = CurseCandidates.of(HeroKind.HAMMER, HeroForms.of(HeroKind.HAMMER));
            if (candidates.isEmpty()) check(CurseStore.load(bond).offered.isEmpty(), "a weapon whose forms hold no curse form never whispers");
            else check(CurseMachine.offerOpen(CurseStore.load(bond), now + 210, t), "a weapon with curse forms whispers at 900 despair");
            if (CurseMachine.offerOpen(CurseStore.load(bond), now + 210, t)) check(CurseService.decline(fp, now + 220, t), "an offer the weapon made can be declined");
            CurseAdmin.setDespair(fp, 700, now + 300, t);
            String forced = CurseAdmin.offer(fp, Sin.WRATH, now + 300, t);
            check(forced == null, "an offer can be forced: " + forced);
            check(CurseMachine.offerOpen(CurseStore.load(bond), now + 310, t), "the offer is open");
            check(CurseService.decline(fp, now + 320, t), "the offer can be declined");
            check(HeroCurse.despair(fp) == 300 && !CurseMachine.offerOpen(CurseStore.load(bond), now + 330, t), "declining closes it and lowers the despair to 300");
            check(!CurseStore.load(bond).hasSeries(Sin.WRATH), "declining unlocks nothing");
            done.add("offer, decline");

            // ---- the Embrace: only an open offer, 4 hearts then 2 then 2, never under four hearts
            check(!CurseService.embrace(fp, now + 340, t), "no offer, no Embrace");
            long offerAt = now + 5000L;
            check(CurseAdmin.offer(fp, Sin.WRATH, offerAt, t) == null, "the weapon whispers again");
            check(CurseService.embrace(fp, offerAt + 100, t), "the open offer can be embraced");
            check(HeroCurse.hasSeries(fp, Sin.WRATH) && HeroCurse.tax(fp) == 4, "Wrath is embraced for 4 hearts");
            check(!CurseMachine.offerOpen(CurseStore.load(bond), offerAt + 101, t), "the offer is closed");
            check(Math.abs(fp.getMaxHealth() - Math.max(2.0 * t.minHearts, baseMax - 8.0)) < 1.0e-6, "4 hearts off the maximum: " + baseMax + " -> " + fp.getMaxHealth());
            check(CurseAdmin.unlock(fp, Sin.PRIDE, true, offerAt + 200, t) == null && HeroCurse.tax(fp) == 6, "the second series takes 2 hearts");
            check(CurseAdmin.unlock(fp, Sin.GREED, true, offerAt + 200, t) == null && HeroCurse.tax(fp) == 8, "the third takes 2 more");
            check(fp.getMaxHealth() >= 2.0 * t.minHearts - 1.0e-6, "the tax never leaves less than four hearts: " + fp.getMaxHealth());
            check(CurseAdmin.unlock(fp, Sin.GREED, true, offerAt + 200, t) != null, "the same series cannot be unlocked twice");
            fp.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0);
            CurseEffects.ensureTax(fp, 8, t.minHearts, true);
            check(Math.abs(fp.getMaxHealth() - 8.0) < 1.0e-6, "a wielder of 5 hearts is taxed down to exactly 4: " + fp.getMaxHealth());
            fp.getAttribute(Attributes.MAX_HEALTH).setBaseValue(6.0);
            CurseEffects.ensureTax(fp, 8, t.minHearts, true);
            check(Math.abs(fp.getMaxHealth() - 6.0) < 1.0e-6, "a wielder already under four hearts loses nothing: " + fp.getMaxHealth());
            fp.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseMax);
            CurseEffects.ensureTax(fp, 8, t.minHearts, true);
            done.add("embrace 4+2+2 hearts, floor 4");

            // ---- the Overrun: locks, swings at the nearest thing, then releases with 400 corruption and weakness
            long run = offerAt + 10_000L;
            CurseAdmin.setCorruption(fp, 990, run, t);
            check(!HeroCurse.locked(fp), "990 corruption is not an Overrun");
            CurseService.addCorruption(fp, 45, run, t);
            check(HeroCurse.locked(fp) || CurseMachine.inOverrun(CurseStore.load(bond), run + 1), "1000 corruption starts an Overrun");
            long until = CurseStore.overrunUntil(bond);
            check(until - run >= 200L && until - run <= 400L, "the Overrun lasts 10 to 20 seconds: " + (until - run) + " ticks");
            check(!CurseService.embrace(fp, run + 5, t), "the weapon holds the wielder: no Embrace");
            check(!CurseService.charm(fp, run + 5, t), "the weapon holds the wielder: the charm cannot reach them");
            if (chunk) {
                Pig pig = EntityType.PIG.create(level);
                if (pig != null) {
                    pig.setPos(2.0, 120.0, 0.5);
                    if (level.addFreshEntity(pig)) {
                        spawned.add(pig);
                        float before = pig.getHealth();
                        LivingEntity target = CurseService.nearestTarget(fp, t);
                        check(target != null, "the Overrun finds a living thing in reach");
                        CurseService.tick(fp, run + 10, false, t);
                        if (target == pig) check(!pig.isAlive() || pig.getHealth() < before, "the forced swing hurt the nearest thing");
                    }
                }
            }
            CurseService.tick(fp, until + 1, false, t);
            check(!HeroCurse.locked(fp), "the weapon lets go when its time is up");
            check(HeroCurse.corruption(fp) == 400, "400 corruption is left: " + HeroCurse.corruption(fp));
            check(fp.hasEffect(MobEffects.WEAKNESS), "a minute of weakness begins");
            done.add("overrun lock, swing, release");

            // ---- the charm: clears all, gives hearts back once
            CurseAdmin.setCorruption(fp, 700, until + 100, t);
            int taxBefore = HeroCurse.tax(fp);
            check(CurseService.charm(fp, until + 200, t), "the charm works");
            check(HeroCurse.corruption(fp) == 0 && HeroCurse.tax(fp) == taxBefore - 2, "all corruption cleared, 2 hearts given back: " + HeroCurse.tax(fp));
            CurseAdmin.setCorruption(fp, 300, until + 300, t);
            check(CurseService.charm(fp, until + 400, t) && HeroCurse.tax(fp) == taxBefore - 2, "the second charm gives no hearts");
            check(!CurseService.charm(fp, until + 500, t), "nothing to cleanse: not used up");
            done.add("charm");

            // ---- a finished rest clears half
            CurseAdmin.setCorruption(fp, 800, until + 600, t);
            RestState rest = fp.getData(SafeRegistry.REST_STATE);
            rest.lastRest = until + 590;
            CurseService.tick(fp, until + 600, true, t);
            int left = HeroCurse.corruption(fp);
            check(left >= 380 && left <= 410, "a rest clears half of 800: " + left);
            done.add("rest clears half");

            // ---- the seven downsides stay inside their bounds
            Map<String, String> skills = Map.of("use", "hammer.probe_a", "key1", "hammer.probe_b");
            fp.setHealth(3.0f);
            for (int i = 0; i < 40; i++) CurseEffects.second(fp, synthetic(Sin.WRATH, skills), Sin.WRATH, t, until + 700 + i * 20L);
            check(fp.getHealth() >= 2.0f - 1.0e-4f, "Wrath never burns under one heart: " + fp.getHealth());
            fp.setHealth(fp.getMaxHealth());
            for (int i = 0; i < 400; i++) CurseEffects.second(fp, synthetic(Sin.WRATH, skills), Sin.WRATH, t, until + 1000 + i * 20L);
            check(fp.getHealth() >= SinMath.wrathFloor(fp.getMaxHealth(), t.wrathFloorShare) - 1.0e-3 && fp.getHealth() >= 2.0f, "Wrath stops at its floor: " + fp.getHealth());
            // 1.99.0, the hunger ban: Gluttony devours magicules and never touches the food bar (food level, saturation and exhaustion stay exactly as they were)
            fp.getFoodData().setFoodLevel(15);
            fp.getFoodData().setExhaustion(0f);
            com.newuniverse.nusmp.balance.HungerBan.guard(fp);
            CurseEffects.second(fp, synthetic(Sin.GLUTTONY, skills), Sin.GLUTTONY, t, until + 9000);
            CurseEffects.second(fp, synthetic(Sin.GLUTTONY, skills), Sin.GLUTTONY, t, until + 9020);
            check(com.newuniverse.nusmp.balance.HungerBan.guard(fp), "Gluttony leaves the food bar alone");
            check(fp.getFoodData().getExhaustionLevel() == 0f && fp.getFoodData().getFoodLevel() == 15, "Gluttony adds no exhaustion and takes no food point");
            com.newuniverse.nusmp.balance.HungerBan.disarm(fp);
            fp.getFoodData().setFoodLevel(20);
            fp.getFoodData().setExhaustion(0f);
            double speed = fp.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            CurseEffects.ensureSinModifiers(fp, Sin.SLOTH, t);
            check(Math.abs(fp.getAttribute(Attributes.MOVEMENT_SPEED).getValue() - speed * (1.0 - t.slothSlow)) < 1.0e-6, "Sloth takes its share of speed");
            CurseEffects.ensureSinModifiers(fp, null, t);
            check(Math.abs(fp.getAttribute(Attributes.MOVEMENT_SPEED).getValue() - speed) < 1.0e-9, "Sloth's slow is taken off again");
            fp.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
            check(CurseEffects.treasureScore(fp) == 64, "64 diamonds weigh 64: " + CurseEffects.treasureScore(fp));
            CurseEffects.ensureSinModifiers(fp, Sin.GREED, t);
            check(Math.abs(fp.getAttribute(Attributes.MOVEMENT_SPEED).getValue() - speed * (1.0 - 0.5 * t.greedMaxSlow)) < 1.0e-6, "Greed slows by half its maximum for half the full burden");
            fp.getInventory().setItem(0, ItemStack.EMPTY);
            CurseEffects.ensureSinModifiers(fp, null, t);
            if (chunk) check(!CurseEffects.safeStep(level, new Vec3(0.5, level.getMaxBuildHeight() - 20.0, 0.5), 1.0, 0.0), "Lust never pulls over a drop");
            done.add("downsides bounded");

            // ---- a technique with no curse form worn changes nothing, and the curse refuses nothing
            HeroSkill probe = SimpleSkill.of("hammer.probe_a", "Probe", "", 100, 0.02, c -> true);
            int corruptionBefore = HeroCurse.corruption(fp);
            HeroCurse.onTechnique(fp, probe);
            check(HeroCurse.corruption(fp) == corruptionBefore, "a technique with no curse form worn adds no corruption");
            check(HeroCurse.refuses(fp, probe) == null, "the curse refuses nothing when no curse form is worn");
            done.add("technique hooks quiet");

            // ---- the wipe
            CurseAdmin.clear(fp, until + 10_000L, t);
            check(!HeroCurse.hasSeries(fp, Sin.WRATH) && HeroCurse.tax(fp) == 0 && HeroCurse.corruption(fp) == 0, "the wipe leaves nothing");
            check(Math.abs(fp.getMaxHealth() - baseMax) < 1.0e-6, "the wipe gives the hearts back: " + fp.getMaxHealth());
            done.add("wipe");
            return SelfTests.Result.ok(String.join(", ", done));
        } finally {
            for (LivingEntity e : spawned) e.discard();
            try {
                fp.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseMax);
                CurseEffects.clearModifiers(fp);
                CurseEffects.ensureSinModifiers(fp, null, CurseTuning.defaults());
                fp.removeEffect(MobEffects.WEAKNESS);
                fp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                fp.getInventory().setItem(0, ItemStack.EMPTY);
                fp.getFoodData().setFoodLevel(20);
                fp.setHealth(fp.getMaxHealth());
                if (fp.hasData(SafeRegistry.REST_STATE)) fp.getData(SafeRegistry.REST_STATE).lastRest = restBefore;
                HeroBond leftover = HeroBonds.of(fp);
                if (leftover.isBound() && leftover.summoned()) {
                    CurseState s = CurseStore.load(leftover);
                    s.overrunUntil = 0L;
                    CurseStore.save(leftover, s);
                    HeroSummon.dismiss(fp);
                }
                HeroBonds.release(fp);
                CurseSync.forget(fp.getUUID());
                CurseEffects.forget(fp.getUUID());
            } catch (RuntimeException cleanup) {
                // the result of the test is already decided; a failed tidy-up must not hide it
            }
        }
    }
}
