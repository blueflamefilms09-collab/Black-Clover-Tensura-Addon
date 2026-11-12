package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: nothing of the Devil Contract system can be seen by its author in a game, so what a person can not check is checked by the real game: the CI server smoke test boots the mod and runs these three
 * (keys {@code devil.contract.registry}, {@code devil.rules.bounds}, {@code devil.store.roundtrip}). Registry, rule and tag reads only: no player, no world, no tick work (the stand-in has no connection).
 * <ul>
 * <li>{@code registry}: the save attachment is registered, the server config answers with sane numbers, the three payload types have distinct ids.</li>
 * <li>{@code rules}: the numbers the doc promises (stages 20 / 50 / 100, the union exactly 6000 ticks, the cooldown exactly 24 000 ticks, the cap while cooling, the drain ten times higher at 100 %, no stat past its cap).</li>
 * <li>{@code store}: a contract written to a tag and read back is the same, and a damaged tag is clamped.</li>
 * </ul>
 */
public final class DevilSelfTests {
    private DevilSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("devil.contract.registry", DevilSelfTests::registry);
        SelfTests.register("devil.rules.bounds", DevilSelfTests::rules);
        SelfTests.register("devil.store.roundtrip", DevilSelfTests::store);
        SelfTests.register("devil.worlds", DevilSelfTests::worlds);                                  // 3.76.0
    }

    private static Result registry(MinecraftServer server) {
        if (!DevilStore.CONTRACT.isBound()) return Result.fail("the devil_contract attachment is not registered");
        if (!NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "devil_contract"))) return Result.fail("the attachment is missing from the registry");
        long u = DevilConfig.unionTicks(), c = DevilConfig.cooldownTicks();
        if (u < 400 || u > 72000) return Result.fail("the union length left its bounds: " + u);
        if (c < 0 || c > 240_000) return Result.fail("the cooldown left its bounds: " + c);
        if (DevilNet.Toggle.TYPE.id().equals(DevilNet.Sync.TYPE.id()) || DevilNet.Sync.TYPE.id().equals(DevilNet.Visual.TYPE.id()) || DevilNet.Toggle.TYPE.id().equals(DevilNet.Visual.TYPE.id())) return Result.fail("two payload types share an id");
        return Result.ok("attachment registered; union " + u + " ticks, cooldown " + c + " ticks; devilEnabled=" + DevilConfig.enabled());
    }

    private static Result rules(MinecraftServer server) {
        if (DevilRules.stage(19.9f) != 0 || DevilRules.stage(20f) != 1 || DevilRules.stage(49.9f) != 1 || DevilRules.stage(50f) != 2 || DevilRules.stage(99.9f) != 2 || DevilRules.stage(100f) != 3) return Result.fail("the stage thresholds moved");
        if (DevilRules.unionTicks(DevilRules.DEFAULT_UNION_SECONDS) != 6000L) return Result.fail("the union is not 5 minutes (6000 ticks)");
        if (DevilRules.cooldownTicks(DevilRules.DEFAULT_COOLDOWN_HOURS) != 24_000L) return Result.fail("the cooldown is not 24 in-game hours (24 000 ticks)");
        if (DevilRules.cap(true) >= 100f || DevilRules.cap(false) != 100f) return Result.fail("the cap while cooling moved");
        for (DevilTier t : DevilTier.values()) {
            if (t == DevilTier.NONE) continue;
            double a = DevilRules.drain(t, 0f), b = DevilRules.drain(t, 100f);
            if (Math.abs(b / a - 10.0) > 1e-9) return Result.fail(t + " drain is not ten times higher at 100 %");
            if (DevilRules.strength(t, 100f, true, 2.0) > DevilRules.MAX_STRENGTH || DevilRules.speed(t, 100f, true, 2.0) > DevilRules.MAX_SPEED) return Result.fail(t + " passed a stat cap");
            if (DevilRules.climb(t) <= 0f) return Result.fail(t + " can not climb");
        }
        return Result.ok("stages 20/50/100, union 6000 ticks, cooldown 24000 ticks, cap while cooling, drain x10, stat caps hold");
    }

    private static Result store(MinecraftServer server) {
        DevilContract c = new DevilContract();
        c.tier = DevilTier.HIGH;
        c.attribute = DevilAttribute.GRAVITY;
        c.assimilation = 42.5f;
        c.unionUntil = 12345L;
        c.unionReadyAt = 67890L;
        c.sacrifices.add(Sacrifice.HALF_HEALTH);
        c.sacrifices.add(Sacrifice.LOST_EYE_LEFT);
        DevilContract r = DevilContract.fromTag(DevilContract.toTag(c));
        if (r.tier != c.tier || r.attribute != c.attribute || r.assimilation != c.assimilation || r.unionUntil != c.unionUntil || r.unionReadyAt != c.unionReadyAt || !r.sacrifices.equals(c.sacrifices)) return Result.fail("a contract changed on the way through its tag");
        CompoundTag bad = new CompoundTag();
        bad.putString("tier", "GODLIKE");
        bad.putFloat("level", 5000f);
        bad.putLong("union", -5L);
        DevilContract d = DevilContract.fromTag(bad);
        if (d.tier != DevilTier.NONE || d.assimilation != 100f || d.unionUntil != 0L) return Result.fail("a damaged tag was not clamped");
        return Result.ok("a contract survives its tag; a damaged tag is clamped");
    }

    /** 3.76.0: the roster of the eight worlds is whole, every devil has a working attribute and a price the store keeps, the ritual rules refuse what they must, and the numbers stay bounded. */
    private static Result worlds(MinecraftServer server) {
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            if (!ids.add(d.id())) return Result.fail("two devils share the id " + d.id());
            if (d.tier() == DevilTier.NONE || d.attribute() == DevilAttribute.NONE) return Result.fail(d.id() + " has no tier or no attribute");
            DevilRules.Effect fx = DevilRules.effect(d.attribute());
            if (fx.bonus() <= 0 || fx.blurb().isEmpty()) return Result.fail("the attribute of " + d.id() + " does nothing");
            DevilContract c = new DevilContract();
            c.tier = d.tier();
            c.attribute = d.attribute();
            c.devil = d.id();
            for (Sacrifice s : Sacrifice.values()) if ((d.priceMask() & s.bit()) != 0) c.sacrifices.add(s);
            DevilContract back = DevilContract.fromTag(DevilContract.toTag(c));
            if (!back.devil.equals(d.id()) || back.tier != d.tier() || back.attribute != d.attribute() || back.sacrificeMask() != c.sacrificeMask()) return Result.fail("the contract with " + d.id() + " does not survive a save");
        }
        for (DevilWorld w : DevilWorld.values()) {
            if (WorldDevil.countOf(w) < 1) return Result.fail(w + " has no devil");
            if (w.ticks < 40 || w.ticks > 400) return Result.fail("the ritual of " + w + " runs " + w.ticks + " ticks");
            DevilRitualRules.Facts none = new DevilRitualRules.Facts(true, false, false, -1, 1.0, 20f, false, false, false, 0, 0, 0.0, true);
            DevilRitualRules.Denial d = DevilRitualRules.check(w, none);
            if (w != DevilWorld.CHAINSAW_MAN && d == DevilRitualRules.Denial.NONE) return Result.fail(w + " began for a caller who brought nothing");
            if (DevilRitualRules.check(w, new DevilRitualRules.Facts(true, true, false, -1, 0.1, 20f, true, true, true, 64, 99, 1.0, false)) != DevilRitualRules.Denial.BOND) return Result.fail(w + " began for a caller who holds a bond");
        }
        if (DevilRules.effectPower(DevilTier.SUPREME, 19f, false) != 0.0 || DevilRules.effectPower(DevilTier.SUPREME, 100f, true) > 1.5 + 1e-9) return Result.fail("the attribute's power left [0, 1.5]");
        if (DevilRules.effectBonus(10f, DevilRules.effect(DevilAttribute.CAUSALITY), 1.5, 10.0) > 30f) return Result.fail("an attribute bonus passed three times the blow");
        if (DevilRitualRules.bindingBlow(3f, 20f) > 1f + 1e-6 || DevilRitualRules.bindingBlow(1f, 20f) != 0f) return Result.fail("the devil's blow could take the last heart");
        return Result.ok("devil worlds: " + DevilWorld.values().length + " worlds, " + WorldDevil.count() + " devils, every attribute acts, prices survive a save, rituals refuse the unprepared; worlds=" + DevilConfig.worlds());
    }
}
