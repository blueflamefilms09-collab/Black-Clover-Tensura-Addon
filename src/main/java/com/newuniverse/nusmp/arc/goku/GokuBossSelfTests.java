package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.ArtTestFoe;
import com.newuniverse.nusmp.arc.lines.BossLines;
import com.newuniverse.nusmp.arc.lines.BossSpeech;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
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
        SelfTests.register("goku.boss.arts", GokuBossSelfTests::arts);
        SelfTests.register("goku.boss.pools", GokuBossSelfTests::pools);
        SelfTests.register("goku.boss.formbar", GokuBossSelfTests::formbar);
        GokuOmegaSelfTests.register();                                  // keys gokuomega.* (Goku Omega)
    }

    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (GokuLadder.STEPS.size() != 11) bad.add("the ladder has " + GokuLadder.STEPS.size() + " steps");
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

    /**
     * 4.96.0 (the "he kills himself" bug): Tensura hurts a creature with an empty magicule pool for its full max health (damage type tensura:energy_drain, no attacker). Summons Goku, empties his pool,
     * checks that the top-up refills it and that the real damage handler cancels an attackerless energy-drain hit on him. Cost: one entity made and discarded in this tick.
     */
    private static SelfTests.Result pools(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!GokuBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + GokuBoss.liveCount() + " Goku bosses already stand");
        GokuBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 16), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = g;
            var ex = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(g);
            if (ex == null) return SelfTests.Result.ok("skipped: no Tensura data on the boss");
            double max = io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(g);
            ex.setMagicule(0.0);
            com.newuniverse.nusmp.boss.rules.BossPools.topUp(g);
            if (max > 0.0 && ex.getMagicule() < max * 0.9) return SelfTests.Result.fail("the empty pool was not refilled: " + ex.getMagicule() + " of " + max);
            net.minecraft.world.damagesource.DamageSource starve = io.github.manasmods.tensura.damage.TensuraDamageTypes.getDamageSource(level, io.github.manasmods.tensura.damage.TensuraDamageTypes.ENERGY_DRAIN);
            var hit = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(g, new net.neoforged.neoforge.common.damagesource.DamageContainer(starve, g.getMaxHealth()));
            com.newuniverse.nusmp.boss.rules.BossRulesHooks.onIncomingDamage(hit);
            boolean on = com.newuniverse.nusmp.boss.rules.BossRulesConfig.KEEP_POOLS_FULL.get() && com.newuniverse.nusmp.boss.rules.BossRulesConfig.ENVIRONMENT_IMMUNE.get();
            if (on && !hit.isCanceled()) return SelfTests.Result.fail("Tensura's attackerless energy-drain damage was not cancelled on Goku");
            return SelfTests.Result.ok("pool refilled to " + Math.round(ex.getMagicule()) + " of " + Math.round(max) + (on ? ", starvation damage cancelled" : ", rules off"));
        } finally {
            if (boss != null) boss.discard();
        }
    }

    /**
     * 4.96.0: a form's bar runs out. Summons Goku in Base, kills his bar with the real die() call, checks he stays alive at 1 health, cannot be hurt, then (windup run at once) is in the next form with a
     * FULL bar. Cost: one entity made and discarded in this tick.
     */
    private static SelfTests.Result formbar(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!GokuBossConfig.enabled() || !GokuBossConfig.formBars()) return SelfTests.Result.ok("skipped: the boss or the form bars are switched off");
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + GokuBoss.liveCount() + " Goku bosses already stand");
        GokuBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 24), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = g;
            g.scanNow();
            int before = g.formIdx();
            g.setHealth(0f);
            g.die(g.damageSources().generic());
            if (!g.isAlive() || g.getHealth() < 0.5f || g.getHealth() > 1.5f) return SelfTests.Result.fail("the empty bar did not leave him at 1 health: " + g.getHealth());
            if (!g.barBreakForTest() || !g.windingUp()) return SelfTests.Result.fail("the power-up did not start");
            if (g.hurt(g.damageSources().generic(), 50f)) return SelfTests.Result.fail("he could be hurt while powering up");
            g.finishChangeForTest();
            if (g.formIdx() != before + 1) return SelfTests.Result.fail("the next form did not take hold (" + g.formIdx() + ")");
            if (g.getHealth() < g.getMaxHealth() - 0.5f) return SelfTests.Result.fail("the new form's bar is not full: " + g.getHealth() + " of " + g.getMaxHealth());
            return SelfTests.Result.ok("form " + before + " -> " + g.formIdx() + " with a full bar of " + Math.round(g.getMaxHealth()));
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

    /**
     * Fires each of his three new arts for real on the server (docs/boss_redesign_goku_sinbad.md): the Zanzoken Cage (its telegraph twice, then the four streaks and the slam, run at once), the Solar
     * Flare (a foe that faces him is blinded) and the Kienzan Boomerang (all 28 ticks of the flight, out and back). The foe is a tamed wolf of a random owner with a huge health pool. Each art must hit it,
     * the cage must forget its centre once fired, and nothing may throw. Cost: two entities made and discarded in this tick, a few hundred budgeted particle packets to nobody.
     */
    private static SelfTests.Result arts(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!GokuBossConfig.enabled()) return SelfTests.Result.ok("skipped: the boss is switched off");
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + GokuBoss.liveCount() + " Goku bosses already stand");
        GokuBoss boss = null;
        LivingEntity foe = null;
        try {
            Vec3 where = Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8);
            ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, where, 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = g;
            foe = ArtTestFoe.make(level, where.add(5, 0, 0), g.getEyePosition());
            if (foe == null) return SelfTests.Result.ok("skipped: no test foe could be made");
            if (!g.foesAround(foe.position(), 3.0).contains(foe)) return SelfTests.Result.ok("skipped: the test foe is not counted as a foe here");
            List<String> bad = new ArrayList<>();
            // the three are in his move list
            for (String id : new String[]{GokuArts.CAGE, GokuArts.FLARE, GokuArts.DISC}) if (g.moveIndex(id) < 0) bad.add(id + " is not in his move list");
            // Solar Flare: the foe looks at him, so it is blinded
            int blinded = GokuArts.flareFire(g);
            boolean sees = foe.hasLineOfSight(g);
            if (sees && blinded < 1) bad.add("the Solar Flare blinded nobody although the foe faced him with a clear line");
            // Zanzoken Cage
            GokuArts.tellsForTest(g, foe);
            foe.invulnerableTime = 0;
            int cage = GokuArts.cageForTest(g, foe);
            if (cage < 1) bad.add("the Zanzoken Cage hit nothing (" + cage + ")");
            if (GokuArts.centreKept(g)) bad.add("the cage keeps its centre after firing");
            // Kienzan Boomerang along the line to the foe
            Vec3 o = g.position().add(0, 1.1, 0);
            Vec3 dir = foe.position().subtract(g.position()).multiply(1, 0, 1).normalize();
            foe.invulnerableTime = 0;
            int disc = GokuArts.discForTest(g, o, dir, 12.0);
            if (disc < 1) bad.add("the Kienzan Boomerang hit nothing (" + disc + ")");
            if (foe.getHealth() >= foe.getMaxHealth()) bad.add("the foe lost no health");
            // his own voice: the first line of the fight is said once and then rests (the chat text always goes out; a registered line is also sent as a sound)
            if (!BossSpeech.say(g, BossLines.GOKU, "intro")) bad.add("his intro line was not said");
            if (BossSpeech.say(g, BossLines.GOKU, "taunt") && BossSpeech.say(g, BossLines.GOKU, "taunt")) bad.add("the taunt group did not rest");
            BossSpeech.forget(g.getUUID());
            return bad.isEmpty() ? SelfTests.Result.ok("Goku's arts fired: flare blinded " + blinded + ", cage hit " + cage + ", disc hit " + disc)
                    : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad));
        } finally {
            if (foe != null) foe.discard();
            if (boss != null) boss.discard();
        }
    }
}
