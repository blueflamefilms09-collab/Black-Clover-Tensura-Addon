package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why (docs/beastwill_canon_rework.md): the CI server smoke test boots the mod and runs these (prefix {@code beastcanon}), because the canon data cannot be seen by its author in a game.
 * <ul>
 * <li>{@code catalog}: with the rework on, the 27 wills of {@code data/anime_multiverse/beast_will_canon} were loaded, every one passes its own validator and the rank progression, and no two wills share a
 * signature; the four TBATE wills keep the abilities the older self-tests and the panther's quiet feet rely on.</li>
 * <li>{@code rules}: the three new kinds are registered and bounded, the milestones, the devour heal, the thunder falloff and the ignite gate are what the doc says, and no canon will passes an attribute cap at double strength.</li>
 * <li>{@code abilities}: the three new kinds run on a stand-in without spawning one entity.</li>
 * </ul>
 * With the rework off (or the classic wills on) the catalog check reports that it was skipped. Server safe: no client class is touched.
 */
public final class BeastCanonSelfTests {
    private BeastCanonSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("beastcanon.catalog.load", BeastCanonSelfTests::catalog);
        SelfTests.register("beastcanon.rules.bounds", BeastCanonSelfTests::rules);
        SelfTests.register("beastcanon.abilities.nospawn", BeastCanonSelfTests::abilities);
    }

    private static boolean canonLoaded() { return BeastWillConfig.canonRework() && !BeastWillConfig.classicWills(); }

    private static Result catalog(MinecraftServer server) {
        if (!canonLoaded()) return Result.ok("skipped: the canon rework is off, the previous wills are loaded");
        if (BeastWillCatalog.size() != 27) return Result.fail("the canon folder gave " + BeastWillCatalog.size() + " wills, not 27");
        List<String> bad = new ArrayList<>();
        for (BeastWillDef d : BeastWillCatalog.all()) {
            String p = d.problem();
            if (p != null) bad.add(p);
            String q = BeastWillCanon.progressionProblem(d);
            if (q != null) bad.add(q);
        }
        bad.addAll(BeastWillCanon.signatureProblems(BeastWillCatalog.all()));
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(3, bad.size()))));
        BeastWillDef lion = BeastWillCatalog.get("lion_will"), panther = BeastWillCatalog.get("shadow_panther_will"), dragon = BeastWillCatalog.get("dragon_will");
        if (lion == null || panther == null || dragon == null) return Result.fail("a TBATE will is missing");
        if (lion.ability(BeastWillRules.ACQUIRE, 1) == null || !"mane_surge".equals(lion.ability(BeastWillRules.ACQUIRE, 1).kind())) return Result.fail("the lion lost King's Wrath");
        if (!BeastWillStealth.quiet(panther)) return Result.fail("the shadow panther lost its quiet feet (a veil in the first form)");
        if (!"time_exclusion".equals(dragon.ability(BeastWillRules.ACQUIRE, 1).kind())) return Result.fail("the dragon lost Static Void");
        return Result.ok("27 canon wills load, validate, unlock by rank and have 27 distinct Acquire and 27 distinct Integrate signatures");
    }

    private static Result rules(MinecraftServer server) {
        for (String k : List.of("tail_sweep", "thunder_chain", "devour")) {
            BeastWillRules.KindLimit l = BeastWillRules.KINDS.get(k);
            if (l == null || l.maxDuration() != 0 || l.maxTargets() > 6 || l.maxRadius() > 6 || l.minCooldown() < 100) return Result.fail("the new kind " + k + " is missing or not bounded");
        }
        if (BeastWillCanon.milestone(0.24) != 0 || BeastWillCanon.milestone(0.25) != 1 || BeastWillCanon.milestone(0.5) != 2 || BeastWillCanon.milestone(0.75) != 3 || BeastWillCanon.milestone(1.0) != 4) return Result.fail("the milestones moved");
        if (BeastWillCanon.cooldownFactor(false, 1.0) != 1.0 || BeastWillCanon.cooldownFactor(true, 0.1) != 1.0 || BeastWillCanon.cooldownFactor(true, 1.0) < 0.9 - 1e-9) return Result.fail("the cooldown milestones moved");
        if (BeastWillCanon.devourHeal(1000) > BeastWillCanon.DEVOUR_MAX_HEAL + 1e-6 || BeastWillCanon.devourHeal(-5) != 0f) return Result.fail("the devour heal is not capped");
        if (BeastWillCanon.chainDamage(10, 2) >= BeastWillCanon.chainDamage(10, 1)) return Result.fail("the thunder chain does not fall off");
        if (BeastWillCanon.ignites(5, "Earth", true) || !BeastWillCanon.ignites(5, "Fire", true) || !BeastWillCanon.ignites(5, "Earth", false)) return Result.fail("the ignite gate moved");
        if (!"beast_will_canon".equals(BeastWillCanon.rootFor(false, true)) || !"beast_will".equals(BeastWillCanon.rootFor(false, false)) || !"beast_will_classic".equals(BeastWillCanon.rootFor(true, true))) return Result.fail("the data folder choice moved");
        for (BeastWillDef d : BeastWillCatalog.all())
            for (int phase = 0; phase <= 2; phase++)
                for (Map.Entry<String, Double> e : BeastWillAttributes.totals(d, phase, 2.0).entrySet()) {
                    BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(e.getKey().substring(0, e.getKey().indexOf('|')));
                    double max = e.getKey().endsWith("|ADD") ? cap.maxAdd() : cap.maxMult();
                    if (e.getValue() > max + 1e-9) return Result.fail(d.id() + " passes the cap on " + e.getKey() + " at double strength");
                }
        return Result.ok("the new kinds are bounded; milestones, devour heal, thunder falloff, ignite gate and folder choice hold; every will stays inside the attribute caps at double strength");
    }

    private static Result abilities(MinecraftServer server) {
        BeastWillDef.Ability sweep = find("tail_sweep"), chain = find("thunder_chain"), devour = find("devour");
        if (canonLoaded() && (sweep == null || chain == null || devour == null)) return Result.fail("a new kind is used by no canon will");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "beastcanon", at.x, at.y, at.z);
        try {
            BeastWillDef def = BeastWillCatalog.get("typhon_will");
            if (def == null) return Result.fail("the typhon will was not loaded");
            AABB box = new AABB(at, at).inflate(48);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            int ran = 0;
            for (BeastWillDef.Ability a : new BeastWillDef.Ability[] {sweep, chain, devour}) {
                if (a == null) continue;
                BeastWillAbilities.run(p, def, a, 1.0, 0.5);                                           // false with no foe in reach, true with one: either way nothing may be spawned
                ran++;
            }
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("a canon ability spawned " + (after - before) + " entities");
            return Result.ok(ran + " new-kind abilities ran on a stand-in and spawned nothing");
        } finally {
            BeastWillAttributes.clear(p);
        }
    }

    private static BeastWillDef.Ability find(String kind) {
        for (BeastWillDef d : BeastWillCatalog.all())
            for (int phase = 1; phase <= 2; phase++)
                for (BeastWillDef.Ability a : d.phase(phase).abilities()) if (a.kind().equals(kind)) return a;
        return null;
    }
}
