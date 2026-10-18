package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroSummon;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dispatcher is the place where the owner's balance rules are kept for all 21 weapons, so the CI server smoke test runs this self-test
 * ({@code hero.use}) in the real game: a bonded, summoned stand-in casts private test techniques through exactly the path of a key press, and the test checks
 * that a good cast starts the cooldown the level and the floor give it and the lock between two techniques, that a repeat is refused, that a technique that
 * returns false costs nothing and starts nothing (the magicules are given back when the stand-in has a pool), that a wind-up waits, refuses a second technique
 * while it runs and is cancelled cleanly, and that a weapon that is put away casts nothing. The stand-in is released and its cooldowns cleared afterwards.
 */
public final class HeroUseSelfTests {
    private HeroUseSelfTests() {}

    public static void register() { SelfTests.register("hero.use", HeroUseSelfTests::use); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static void reset(ServerPlayer p) {
        HeroUse.cancelPending(p, false);
        if (HeroBonds.has(p)) HeroBonds.release(p);
        HeroUse.clearCooldowns(p.getUUID());
        p.getInventory().clearContent();
    }

    private static SelfTests.Result use(MinecraftServer server) {
        if (!HeroConfig.enabled()) return ok("skipped: heroweapon.enabled is false in the config");
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "hero_core_use", 0.5, 100, 0.5);
        reset(p);
        List<String> bad = new ArrayList<>();
        String note = "";
        try {
            if (HeroBonds.bind(p, HeroKind.HAMMER) != null) return ok("skipped: the Hammer could not be bound to a stand-in");
            if (!HeroSummon.summon(p)) return fail("a bonded stand-in could not summon the weapon");
            HeroCtx c = HeroCtx.of(p);
            if (c == null || !c.summoned()) return fail("the context of a summoned stand-in does not say the weapon is out");
            int[] casts = {0};
            SimpleSkill good = SimpleSkill.of("hammer.selftest_good", "Self-test good", "a private technique of the dispatcher's self-test", 100, 0.0, x -> {
                casts[0]++;
                return true;
            });
            SimpleSkill other = SimpleSkill.of("hammer.selftest_other", "Self-test other", "a second private technique", 100, 0.0, x -> {
                casts[0]++;
                return true;
            });
            SimpleSkill refusing = SimpleSkill.of("hammer.selftest_refuses", "Self-test refusal", "a technique that always refuses", 100, 0.0, x -> false);
            SimpleSkill paidFails = SimpleSkill.of("hammer.selftest_paid_fails", "Self-test paid refusal", "costs a quarter, then refuses", 100, 0.25, x -> false);
            SimpleSkill paidGood = SimpleSkill.of("hammer.selftest_paid_good", "Self-test paid cast", "costs a quarter, then works", 100, 0.25, x -> {
                casts[0]++;
                return true;
            });
            SimpleSkill winding = SimpleSkill.of("hammer.selftest_wind", "Self-test wind-up", "waits 40 ticks", 100, 0.0, x -> {
                casts[0]++;
                return true;
            }).windUp(40);

            // ---- a good cast starts the cooldown and the lock; a repeat is refused
            if (!HeroUse.run(p, c, good)) bad.add("a ready technique was refused");
            if (casts[0] != 1) bad.add("a ready technique cast " + casts[0] + " times");
            int want = DispatchRules.cooldownTicks(100, c.bondLevel(), HeroConfig.minCooldownTicks());
            if (HeroUse.cooldownLeft(p, good.id()) != want) bad.add("the cooldown after a cast is " + HeroUse.cooldownLeft(p, good.id()) + ", expected " + want);
            if (HeroUse.lockLeft(p) != HeroConfig.skillGlobalLockTicks()) bad.add("the lock after a cast is " + HeroUse.lockLeft(p) + ", expected " + HeroConfig.skillGlobalLockTicks());
            if (HeroUse.run(p, c, good) || casts[0] != 1) bad.add("a technique on cooldown was cast again");
            boolean otherWanted = HeroConfig.skillGlobalLockTicks() <= 0;
            if (HeroUse.run(p, c, other) != otherWanted) bad.add("a second technique right after the first was " + (otherWanted ? "refused with no lock configured" : "allowed inside the lock"));

            // ---- a technique that refuses costs nothing and starts nothing
            HeroUse.clearCooldowns(p.getUUID());
            if (HeroUse.run(p, c, refusing)) bad.add("a technique that returns false was reported as cast");
            if (HeroUse.cooldownLeft(p, refusing.id()) != 0 || HeroUse.lockLeft(p) != 0) bad.add("a refusing technique started a cooldown or the lock");

            // ---- the cost is paid first and given back on a refusal
            var pool = TensuraStorages.getExistenceFrom(p);
            double max = pool == null ? 0.0 : EnergyHelper.getMaxMagicule(p);
            if (pool != null && max > 0.0) {
                pool.setMagicule(max);
                pool.markDirty();
                double before = pool.getMagicule();
                if (HeroUse.run(p, c, paidFails)) bad.add("a technique that returns false after being paid for was reported as cast");
                if (pool.getMagicule() < before - 1e-6) bad.add("a refused technique kept its cost: " + before + " became " + pool.getMagicule());
                HeroUse.clearCooldowns(p.getUUID());
                if (!HeroUse.run(p, c, paidGood)) bad.add("a technique the stand-in could pay for was refused");
                else if (before - pool.getMagicule() < max * 0.2) bad.add("a technique costing a quarter of the pool took only " + (before - pool.getMagicule()) + " of " + max);
                pool.setMagicule(max);
                pool.markDirty();
            } else {
                note = "; the stand-in has no magicule pool, so the cost and the refund were not exercised";
            }

            // ---- a wind-up waits, refuses another technique, and is cancelled cleanly
            HeroUse.clearCooldowns(p.getUUID());
            int castsBeforeWind = casts[0];
            if (!HeroUse.run(p, c, winding)) bad.add("a technique with a wind-up was refused");
            if (casts[0] != castsBeforeWind) bad.add("a technique with a wind-up cast at once");
            if (!HeroUse.windingUp(p)) bad.add("a wind-up is not recorded");
            if (HeroUse.run(p, c, good)) bad.add("a second technique was allowed while a wind-up ran");
            HeroUse.cancelPending(p, true);
            if (HeroUse.windingUp(p)) bad.add("a cancelled wind-up is still recorded");
            if (HeroUse.cooldownLeft(p, winding.id()) != 0) bad.add("a cancelled wind-up started a cooldown");

            // ---- a weapon that is put away casts nothing
            HeroUse.clearCooldowns(p.getUUID());
            if (!HeroSummon.dismiss(p)) bad.add("the weapon could not be put away");
            int beforeAway = casts[0];
            HeroCtx away = HeroCtx.of(p);
            if (away == null) bad.add("a bonded stand-in has no context after a dismiss");
            else if (HeroUse.run(p, away, good) || casts[0] != beforeAway) bad.add("a technique was cast with the weapon put away");
        } finally {
            reset(p);
        }
        if (!bad.isEmpty()) return fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return ok("cooldown, lock, repeat refusal, refund on refusal, wind-up (wait, busy, cancel) and the put-away weapon all hold" + note);
    }
}
