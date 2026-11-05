package com.newuniverse.nusmp.beastwill;

import com.mojang.serialization.JsonOps;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * Why: nothing of the Beast Will system can be seen by its author in a game, so what a person can not check is checked by the real game: the CI server smoke test boots the mod and runs these three
 * (keys {@code beastwill.catalog}, {@code beastwill.rules}, {@code beastwill.player}).
 * <ul>
 * <li>{@code catalog}: the four wills were loaded from the data files and every one passes its own validator; the core item, its component and the save attachment are registered; the config answers.</li>
 * <li>{@code rules}: the numbers the doc promises (strain tiers, rank thresholds, compatibility floor, day budget, premature-use penalty) and that no will, at double strength, passes the attribute caps.</li>
 * <li>{@code player}: on a stand-in player: a core is refused for a low rank before anything is taken; a will binds once; the form turns modifiers on and death turns them off and cuts the strain to 59; an ability
 * takes effect without spawning one entity; swapping keeps strain and cooldowns; the ledger takes a serial once; the core's component survives its codec; everything is cleaned up at the end.</li>
 * </ul>
 * Server safe: no client class is touched. The stand-in has no connection, so no packet is sent.
 */
public final class BeastWillSelfTests {
    private BeastWillSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("beastwill.catalog.load", BeastWillSelfTests::catalog);
        SelfTests.register("beastwill.rules.bounds", BeastWillSelfTests::rules);
        SelfTests.register("beastwill.player.roundtrip", BeastWillSelfTests::player);
        SelfTests.register("beastwill.look.kinds", BeastWillSelfTests::look);
    }

    private static final String[] IDS = {"dragon_will", "shadow_panther_will", "elderwood_guardian_will", "lion_will"};

    private static Result catalog(MinecraftServer server) {
        for (String id : IDS) {
            BeastWillDef d = BeastWillCatalog.get(id);
            if (d == null) return Result.fail("the will was not loaded: " + id + " (loaded " + BeastWillCatalog.size() + ")");
            String problem = d.problem();
            if (problem != null) return Result.fail(problem);
            if (!d.minRank().atLeast(BeastRank.A)) return Result.fail(id + " would drop from a beast below rank A");
        }
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "beast_will_core"))) return Result.fail("the core item is not registered");
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "beast_will_core"))) return Result.fail("the core component is not registered");
        if (!BeastWillConfig.enabled() && BeastWillConfig.dropScale() < 0) return Result.fail("the config answered nonsense");
        return Result.ok(BeastWillCatalog.size() + " wills loaded and valid; the core item, component and save attachment are registered");
    }

    /** 3.57.0: the style the server sends to the clients carries the phase, the look kind and the tint of every loaded will, and every shipped will has its own look kind (the client draws that beast's spirit from it). */
    private static Result look(MinecraftServer server) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (BeastWillDef d : BeastWillCatalog.all()) {
            int kind = BeastWillLook.kindOf(d.id());
            for (int phase = 1; phase <= 2; phase++) {
                int style = BeastWillManager.style(d, phase);
                if (BeastWillLook.phase(style) != phase || BeastWillLook.kind(style) != kind || BeastWillLook.tint(style) != (d.tint() & 0xFFFFFF)) return Result.fail(d.id() + ": the aura style lost its phase, look kind or tint");
            }
            if (kind != BeastWillLook.UNKNOWN && !seen.add(kind)) return Result.fail(d.id() + " shares a look kind with another will");
        }
        for (String id : IDS) if (BeastWillLook.kindOf(id) == BeastWillLook.UNKNOWN) return Result.fail(id + " has no spirit look");
        return Result.ok("the aura style carries phase, look kind and tint for " + BeastWillCatalog.size() + " wills; " + seen.size() + " distinct spirits");
    }

    private static Result rules(MinecraftServer server) {
        if (BeastWillRules.tier(0) != BeastWillRules.Tier.CALM || BeastWillRules.tier(30) != BeastWillRules.Tier.STRAINED || BeastWillRules.tier(60) != BeastWillRules.Tier.EXHAUSTED
                || BeastWillRules.tier(85) != BeastWillRules.Tier.OVERLOAD) return Result.fail("the strain tiers moved");
        if (BeastWillRules.rankOf(19_999, null) != BeastRank.B || BeastWillRules.rankOf(20_000, null) != BeastRank.A || BeastWillRules.rankOf(10, null) != null) return Result.fail("the rank thresholds moved");
        if (BeastWillRules.compat(false, 0.0) < 0.2 - 1e-9 || BeastWillRules.compat(true, 0.0) != 1.0) return Result.fail("the compatibility floor moved");
        if (BeastWillRules.budgeted(0.5, 0.14, 0.15) > 0.0100001) return Result.fail("the day budget lets more through than the cap");
        if (BeastWillRules.prematureMult(BeastWillRules.INTEGRATE, 1.0, 0.0) < 2.99 || BeastWillRules.prematureMult(BeastWillRules.ACQUIRE, 1.0, 1.0) != 1.0) return Result.fail("the premature-use penalty moved");
        for (String id : IDS) {
            BeastWillDef d = BeastWillCatalog.get(id);
            if (d == null) return Result.fail("the will was not loaded: " + id);
            for (int phase = 0; phase <= 2; phase++)
                for (Map.Entry<String, Double> e : BeastWillAttributes.totals(d, phase, 2.0).entrySet()) {
                    BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(e.getKey().substring(0, e.getKey().indexOf('|')));
                    double max = e.getKey().endsWith("|ADD") ? cap.maxAdd() : cap.maxMult();
                    if (e.getValue() > max + 1e-9) return Result.fail(id + " passes the cap on " + e.getKey() + " at double strength: " + e.getValue() + " > " + max);
                }
        }
        return Result.ok("tiers, ranks, compatibility, day budget, premature penalty and the attribute caps hold for all four wills at double strength");
    }

    private static Result player(MinecraftServer server) {
        BeastWillDef lion = BeastWillCatalog.get("lion_will"), dragon = BeastWillCatalog.get("dragon_will");
        if (lion == null || dragon == null) return Result.fail("the wills were not loaded");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "beastwill", at.x, at.y, at.z);
        try {
            BeastWillState s = BeastWillStore.of(p);
            s.wills.clear();
            s.coreDay.clear();
            s.active = "";
            s.phase = BeastWillRules.OFF;
            s.strain = 0;
            s.lockUntil = 0;
            s.swapLockUntil = 0;
            long now = level.getGameTime();

            // a rank below the will's is refused by the check, before anything is taken
            BeastWillAcquire.Verdict low = BeastWillAcquire.check(p, lion, BeastRank.C, null);
            if (low.ok() || !"rank".equals(low.reason())) return Result.fail("a rank C beast was not refused (" + low.reason() + ")");
            if (s.has("lion_will")) return Result.fail("the check bound the will");

            // binding
            if (!BeastWillAcquire.grant(p, lion, false) || !"lion_will".equals(s.active) || s.phase != BeastWillRules.OFF) return Result.fail("the will did not bind as the active one with the form off");
            if (BeastWillAcquire.check(p, lion, BeastRank.S, null).ok()) return Result.fail("a held will could be absorbed again");

            // the form turns the modifiers on; death turns them off and cuts the strain
            BeastWillState.Progress pr = s.activeProgress();
            pr.assimilation = 1f;
            pr.mastery = 1f;
            BeastWillManager.onAction(p, BeastWillNet.OP_FORM, BeastWillRules.ACQUIRE);
            if (s.phase != BeastWillRules.ACQUIRE) return Result.fail("the first form did not start on a stand-in with full assimilation (phase " + s.phase + ")");
            if (!BeastWillAttributes.any(p)) return Result.fail("the form is on but no modifier was applied");
            s.strain = 95f;
            BeastWillManager.onDeath(p);
            if (s.phase != BeastWillRules.OFF || BeastWillAttributes.any(p)) return Result.fail("death left the form or a modifier behind");
            if (s.strain > 59f) return Result.fail("death did not cut the strain to 59 (" + s.strain + ")");

            // an ability takes effect and spawns nothing
            s.phase = BeastWillRules.ACQUIRE;
            AABB box = new AABB(at, at).inflate(48);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            BeastWillDef.Ability mane = lion.ability(BeastWillRules.ACQUIRE, 2) != null ? lion.ability(BeastWillRules.ACQUIRE, 2) : lion.ability(BeastWillRules.ACQUIRE, 1);
            if (mane == null || !BeastWillAbilities.run(p, lion, mane, 1.0, 0.5)) return Result.fail("the lion's own ability did not take effect on its caster");
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("an ability spawned " + (after - before) + " entities");

            // swapping keeps the strain and the cooldowns
            s.phase = BeastWillRules.OFF;
            s.strain = 41f;
            s.startCooldown(pr, "mane_surge", now, 600);
            if (!s.bind("dragon_will")) return Result.fail("a second will did not bind");
            s.activate("dragon_will", now + 1200);
            if (s.strain != 41f || s.left(pr, "mane_surge", now) != 600) return Result.fail("swapping changed the strain or a cooldown");
            if (!s.swapLocked(now) || s.swapLocked(now + 1200)) return Result.fail("the swap lock is wrong");

            // the ledger takes a serial once; the core's component survives its codec
            String key = "selftest:" + UUID.randomUUID();
            BeastWillLedger ledger = BeastWillLedger.get(server);
            if (!ledger.consume(key) || ledger.consume(key)) return Result.fail("the ledger did not take a serial exactly once");
            var core = new BeastWillComponents.Core("lion_will", "Mana Lion", "A", key);
            var json = BeastWillComponents.Core.CODEC.encodeStart(JsonOps.INSTANCE, core).result();
            if (json.isEmpty() || !BeastWillComponents.Core.CODEC.parse(JsonOps.INSTANCE, json.get()).result().map(core::equals).orElse(false)) return Result.fail("the core component did not survive its codec");
            if (BeastWillItems.core("lion_will", "x", BeastRank.A).getCount() != 1 || BeastWillComponents.of(BeastWillItems.core("lion_will", "x", BeastRank.A)) == null) return Result.fail("a fresh core has no component");

            // the save format round-trips and clamps
            BeastWillState back = BeastWillStore.fromTag(BeastWillStore.toTag(s));
            if (!back.active.equals(s.active) || back.wills.size() != s.wills.size() || back.strain != s.strain) return Result.fail("the save format did not round-trip");
            return Result.ok("rank refusal, binding, the form and its modifiers, death cleanup, a no-spawn ability, swap keeping strain and cooldowns, the ledger, the core component and the save format hold");
        } finally {
            BeastWillState s = BeastWillStore.peek(p);
            if (s != null) {
                s.wills.clear();
                s.coreDay.clear();
                s.active = "";
                s.phase = BeastWillRules.OFF;
                s.strain = 0;
                s.lockUntil = 0;
                s.swapLockUntil = 0;
            }
            BeastWillManager.onLogout(p);
            BeastWillAttributes.clear(p);
        }
    }
}
