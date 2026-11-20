package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: nothing of the Devil Contract system can be seen by its author in a game, so what a person can not check is checked by the real game: the CI server smoke test boots the mod and runs these three
 * (keys {@code devil.contract.registry}, {@code devil.rules.bounds}, {@code devil.store.roundtrip}). Registry, rule and tag reads only: no player, no world, no tick work (the stand-in has no connection).
 * <ul>
 * <li>{@code registry}: the save attachment is registered, the server config answers with sane numbers, the three payload types have distinct ids.</li>
 * <li>{@code rules}: the numbers the doc promises (stages 20 / 50 / 100, the union exactly 6000 ticks, the cooldown exactly 24 000 ticks, the cap while cooling, the drain ten times higher at 100 %, no stat past its cap).</li>
 * <li>{@code store}: a contract written to a tag and read back is the same, and a damaged tag is clamped.</li>
 * <li>{@code devil.npc} (3.97.0): the figure's entity type is registered; the cap and the off switch refuse a figure; a stand-in's ritual makes one figure in front of it, a second ritual replaces it (still one),
 * the offer, the merge and the dissolve set its state, a discard (what logout, death, a change of dimension and {@code /devil clear} call) removes it, and a figure whose owner is not online removes itself within ten ticks.</li>
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
        SelfTests.register("devil.npc", DevilSelfTests::npc);                                        // 3.97.0: the devil's figure (docs/devil_forms.md)
    }

    private static Result registry(MinecraftServer server) {
        if (!DevilStore.CONTRACT.isBound()) return Result.fail("the devil_contract attachment is not registered");
        if (!NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_contract"))) return Result.fail("the attachment is missing from the registry");
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

    /** 3.97.0: the devil's figure. The stand-in is not on the player list, so it is also the "owner gone" case of the figure's own check. */
    private static Result npc(MinecraftServer server) {
        if (!DevilEntities.NPC.isBound()) return Result.fail("the devil_npc entity type is not registered");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_npc"))) return Result.fail("the devil_npc entity type is missing from the registry");
        if (DevilNpcRules.mayStart(false, false, 0, 8) || DevilNpcRules.mayStart(true, true, 0, 8) || DevilNpcRules.mayStart(true, false, 8, 8) || !DevilNpcRules.mayStart(true, false, 7, 8)) return Result.fail("the figure's cap or switch rules moved");
        int max = DevilConfig.npcMax();
        long life = DevilConfig.npcLifetimeTicks();
        if (max < 0 || max > DevilNpcRules.MAX_MAX || life < DevilNpcRules.MIN_LIFE_SECONDS * 20L || life > DevilNpcRules.MAX_LIFE_SECONDS * 20L) return Result.fail("the figure's settings left their bounds: max " + max + ", life " + life);
        if (!DevilConfig.npcEnabled() || max == 0) return Result.ok("skipped the entity part: npc.enabled is " + DevilConfig.npcEnabled() + ", npc.max " + max + " (the old pictures-only ritual)");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "devil_npc", at.x, at.y, at.z);
        DevilContract c = DevilStore.of(a);
        try {
            WorldDevil d = WorldDevil.at(0);
            DevilRitualRun run = new DevilRitualRun(d.id(), 0L, at.x, at.y, at.z, 0.0, 1.0, level.dimension().location().toString());
            c.ritual = run;
            if (!DevilNpcs.spawn(a, c, d, run)) return Result.fail("a ritual made no figure");
            DevilNpc n = DevilNpcs.figure(server, c);
            if (n == null) return Result.fail("the figure is not found after it was made");
            if (!a.getUUID().equals(n.owner()) || !d.id().equals(n.devilId()) || n.state() != DevilNpcRules.RISING) return Result.fail("the figure's owner, devil or state is wrong");
            if (n.getAnimatableInstanceCache() == null) return Result.fail("the figure is not a GeckoLib entity (canon looks, docs/devil_trial_model.md)");         // the full-size canon figure plays rise / roar / idle from its state
            if (DevilCanonRules.riseParticlesTotal() > 80 || DevilCanonRules.riseBurst(DevilNpcRules.RISE_TICKS) <= 0 || DevilCanonRules.riseBurst(DevilNpcRules.RISE_TICKS + 1) != 0) return Result.fail("the figure's appearance effect is not bounded to its rise");
            double away = Math.hypot(n.getX() - at.x, n.getZ() - at.z);
            if (away < 1.0 || away > DevilNpcRules.FRONT + 0.1) return Result.fail("the figure stands " + away + " blocks from the circle, not in front of it");
            float wantYaw = DevilNpcRules.yawToward(at.x - n.getX(), at.z - n.getZ());               // 4.40.0: it faces the caller, and stands within reach of the circle's height (on the ground when there is any)
            if (Math.abs(net.minecraft.util.Mth.wrapDegrees(n.getYRot() - wantYaw)) > 1.0f) return Result.fail("the figure faces " + n.getYRot() + " degrees, not the caller (" + wantYaw + ")");
            if (n.getY() > at.y + DevilNpcRules.GROUND_UP + 1e-3 || n.getY() < at.y - DevilNpcRules.GROUND_DOWN - 1e-3) return Result.fail("the figure stands at y " + n.getY() + ", out of reach of the circle at " + at.y);
            if (n.isPickable() || n.isPushable() || n.canBeCollidedWith() || n.shouldBeSaved() || !n.isInvulnerableTo(level.damageSources().generic())) return Result.fail("the figure can be touched or saved");
            if (!DevilNpcs.spawn(a, c, d, run)) return Result.fail("a second ritual made no figure");
            DevilNpc second = DevilNpcs.figure(server, c);
            if (second == null || second == n || !n.isRemoved()) return Result.fail("a second figure did not replace the first");
            if (DevilNpcs.live(server, java.util.List.of(a)) != 1) return Result.fail("the caller has " + DevilNpcs.live(server, java.util.List.of(a)) + " figures, not one");
            if (!DevilNpcs.offer(a, c, d) || second.state() != DevilNpcRules.OFFER) return Result.fail("the offer did not set the OFFER state");
            DevilNpcs.merge(a, c, d);
            if (second.state() != DevilNpcRules.MERGE) return Result.fail("signing did not set the MERGE state");
            DevilNpcs.dissolve(a, c);
            if (second.state() != DevilNpcRules.MERGE) return Result.fail("a merging figure was turned into a dissolving one");
            DevilNpcs.discard(server, c);
            if (!second.isRemoved() || c.npc != null || DevilNpcs.figure(server, c) != null) return Result.fail("discard (logout, death, dimension change, /devil clear) left the figure");
            if (DevilNpcs.offer(a, c, d)) return Result.fail("an offer was made with no figure (the old shadow must play then)");

            if (!DevilNpcs.spawn(a, c, d, run)) return Result.fail("the third figure was not made");
            DevilNpcs.dissolve(a, c);
            DevilNpc third = DevilNpcs.figure(server, c);
            if (third == null || third.state() != DevilNpcRules.DISSOLVE) return Result.fail("a refused offer did not set the DISSOLVE state");
            DevilNpcs.discard(server, c);

            if (!DevilNpcs.spawn(a, c, d, run)) return Result.fail("the fourth figure was not made");
            DevilNpc fourth = DevilNpcs.figure(server, c);
            for (int i = 0; i < DevilNpcRules.CHECK_EVERY && fourth != null && !fourth.isRemoved(); i++) fourth.tick();
            if (fourth == null || !fourth.isRemoved()) return Result.fail("a figure whose owner is not online did not remove itself within " + DevilNpcRules.CHECK_EVERY + " ticks");
            return Result.ok("devil figure: type registered, made in front of the caller, one per caller, offer / merge / dissolve states, discard and the owner-gone check remove it; cap " + max + ", life " + life + " ticks");
        } finally {
            DevilNpcs.discard(server, c);
            c.ritual = null;
            c.npc = null;
        }
    }
}
