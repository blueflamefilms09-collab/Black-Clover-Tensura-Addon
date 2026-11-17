package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Goku boss cannot be played by its author, so the CI server smoke test checks it in the real game ({@code /multiverse mega selftest}, keys {@code goku.boss.*}): the data
 * (ladder, scaling, settings, roster, registry, entity type), one real summoning with a form change and the clean removal of its modifiers, and the global cap. Cost: at most two entities,
 * made and discarded in the same tick, with no fight and no packets.
 */
public final class GokuBossSelfTests {
    private GokuBossSelfTests() {}

    public static void register() {
        SelfTests.register("goku.boss.data", GokuBossSelfTests::data);
        SelfTests.register("goku.boss.spawn", GokuBossSelfTests::spawn);
        SelfTests.register("goku.boss.cap", GokuBossSelfTests::cap);
    }

    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (GokuLadder.STEPS.size() != 10) bad.add("the ladder has " + GokuLadder.STEPS.size() + " steps");
        GokuBossMath.Reading r = GokuBossMath.resolve(true, 0.0, 0.0, GokuBossConfig.preset());
        if (!(r.ep() > 0.0) || r.source() != GokuBossMath.Source.PRESET) bad.add("the fallback chain gives " + r);
        GokuBossMath.Params p = GokuBossConfig.params();
        double h = GokuBossMath.healthFactor(r, 1, p), d = GokuBossMath.damageFactor(r, 1, p);
        if (!(h >= GokuBossMath.MIN_FACTOR && h <= GokuBossMath.MAX_HEALTH_FACTOR)) bad.add("health factor " + h);
        if (!(d >= GokuBossMath.MIN_FACTOR && d <= GokuBossMath.MAX_DAMAGE_FACTOR)) bad.add("damage factor " + d);
        if (GokuBossConfig.globalCap() < 1 || GokuBossConfig.globalCap() > 4) bad.add("globalCap " + GokuBossConfig.globalCap());
        if (GokuBossConfig.maxHitPercent() <= 0.0 || GokuBossConfig.maxHitPercent() > 0.9) bad.add("maxHitPercent " + GokuBossConfig.maxHitPercent());
        if (ArcRoster.byId(GokuBoss.ID) == null) bad.add("not in the arc roster");
        if (!BossRegistry.known(GokuBoss.ID)) bad.add("not in the boss registry");
        var type = ArcSpawner.typeOf(GokuBoss.ID);
        if (type == null) bad.add("no entity type");
        else if (DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType) type) == null) bad.add("no attributes");
        if (GokuBossMath.capHit(1.0e6f, 20f, 0.3) > 6.01f) bad.add("the per-hit cap does not hold");
        return bad.isEmpty() ? SelfTests.Result.ok("Goku: ladder, scaling (health x" + String.format(java.util.Locale.ROOT, "%.2f", h) + " at the preset), settings, roster and entity type are in place")
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    /** One real summoning: base form, the scan, a change to Super Saiyan 2 with its modifier, and no modifier left after the clean-up. */
    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + GokuBoss.liveCount() + " Goku bosses already stand");
        GokuBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8), 0f, List.of(), true);
            if (!GokuBossConfig.enabled()) return r.outcome() == ArcSpawner.Outcome.DISABLED ? SelfTests.Result.ok("the setting is off: summoning is refused") : SelfTests.Result.fail("enabled is off but summoning gave " + r.outcome());
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return SelfTests.Result.fail("summoning gave " + r.outcome());
            if (!(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("the boss is not a GokuBoss");
            boss = g;
            if (g.formIdx() != 0) return SelfTests.Result.fail("he does not start in Base (form " + g.formIdx() + ")");
            g.scanNow();
            if (!(g.reading().ep() > 0.0)) return SelfTests.Result.fail("the reading is " + g.reading());
            if (!(g.healthFactor() > 0.0) || !(g.damageFactorValue() > 0.0)) return SelfTests.Result.fail("the factors are " + g.healthFactor() + " / " + g.damageFactorValue());
            ArcSpawner.Outcome near = GokuBosses.gate(level, g.position().add(3, 0, 0));
            ArcSpawner.Outcome want = GokuBoss.liveCount() >= GokuBossConfig.globalCap() ? ArcSpawner.Outcome.FAILED : ArcSpawner.Outcome.ALREADY_PRESENT;
            if (near != want) return SelfTests.Result.fail("a summoning 3 blocks from him gave " + near + ", expected " + want);
            g.forceFormForTest(4);
            if (g.formIdx() != 4) return SelfTests.Result.fail("the form did not change (" + g.formIdx() + ")");
            if (!(g.formDamageModifier() > 0.0)) return SelfTests.Result.fail("form 4 puts no damage modifier on him");
            g.clearModsForTest();
            if (g.hasFormModifiers()) return SelfTests.Result.fail("modifiers stay after the clean-up");
            return SelfTests.Result.ok("Goku summoned: read " + Math.round(g.reading().ep()) + " EP (" + g.reading().source().name().toLowerCase(java.util.Locale.ROOT) + "), health " + (int) g.getMaxHealth() + ", form change and clean-up work");
        } finally {
            if (boss != null) boss.discard();
        }
    }

    /** The global cap: with the cap's worth of bosses standing, the gate refuses a further one; after they are gone it opens again. */
    private static SelfTests.Result cap(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!GokuBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (GokuBoss.liveCount() > 0) return SelfTests.Result.ok("skipped: a Goku boss already stands");
        List<GokuBoss> made = new ArrayList<>();
        try {
            for (int i = 0; i < GokuBossConfig.globalCap(); i++) {
                Vec3 spot = Vec3.atBottomCenterOf(at.above(2)).add(6 + i * (GokuBosses.ARENA_CLEARANCE + 6), 0, 0);
                if (!level.hasChunkAt(BlockPos.containing(spot))) return SelfTests.Result.ok("skipped: the chunk at " + (int) spot.x + " is not loaded");
                ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, spot, 0f, List.of(), true);
                if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("summoning " + (i + 1) + " gave " + r.outcome());
                made.add(g);
            }
            if (GokuBosses.gate() != ArcSpawner.Outcome.FAILED) return SelfTests.Result.fail("the gate lets one more through at " + GokuBoss.liveCount());
            ArcSpawner.Result extra = ArcSpawner.summon(level, GokuBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, -60), 0f, List.of(), true);
            if (extra.outcome() == ArcSpawner.Outcome.SPAWNED) {
                if (extra.boss() != null) extra.boss().discard();
                return SelfTests.Result.fail("a summoning above the cap was not refused");
            }
        } finally {
            for (GokuBoss g : made) g.discard();
        }
        if (GokuBoss.liveCount() != 0) return SelfTests.Result.fail(GokuBoss.liveCount() + " bosses are left after the clean-up");
        if (GokuBosses.gate() != null) return SelfTests.Result.fail("the gate stays closed after the clean-up");
        return SelfTests.Result.ok("the cap of " + GokuBossConfig.globalCap() + " holds and opens again after the clean-up");
    }
}
