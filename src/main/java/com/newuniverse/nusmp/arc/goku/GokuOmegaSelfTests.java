package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.ArtTestFoe;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: Goku Omega cannot be played by its author, so the CI server smoke test checks it in the real game ({@code /multiverse mega selftest}, keys {@code gokuomega.*}): the data (the ladder's new top, the
 * strongest-boss proof over every boss entity type that is really registered, the settings), the saved record and the training arithmetic on a scratch copy (the real world's record is never changed), and
 * one real summoning with the Omega factors, Mastered Ultra Instinct and the hacks' caps. Cost: at most two entities, made and discarded in the same tick, no fight, no packets that matter.
 */
public final class GokuOmegaSelfTests {
    private GokuOmegaSelfTests() {}

    public static void register() {
        SelfTests.register("gokuomega.data", GokuOmegaSelfTests::data);
        SelfTests.register("gokuomega.world", GokuOmegaSelfTests::world);
        SelfTests.register("gokuomega.spawn", GokuOmegaSelfTests::spawn);
    }

    /** The stat lines of every boss entity type that is registered in the game (BossRegistry, the arcs included), read from its real attribute supplier. */
    private static List<GokuOmegaMath.Sheet> registeredRivals() {
        List<GokuOmegaMath.Sheet> out = new ArrayList<>();
        for (String path : BossRegistry.paths()) {
            if (GokuBoss.ID.equals(path)) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(BossRegistry.NAMESPACE, path));
            if (type == null) continue;
            @SuppressWarnings({"unchecked", "rawtypes"})
            AttributeSupplier sup = DefaultAttributes.getSupplier((EntityType) type);
            if (sup == null || !sup.hasAttribute(Attributes.MAX_HEALTH) || !sup.hasAttribute(Attributes.ATTACK_DAMAGE)) continue;
            out.add(new GokuOmegaMath.Sheet(path, sup.getBaseValue(Attributes.MAX_HEALTH), sup.getBaseValue(Attributes.ATTACK_DAMAGE),
                    sup.hasAttribute(Attributes.MOVEMENT_SPEED) ? sup.getBaseValue(Attributes.MOVEMENT_SPEED) : 0.0,
                    sup.hasAttribute(Attributes.ARMOR) ? sup.getBaseValue(Attributes.ARMOR) : 0.0));
        }
        return out;
    }

    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (GokuLadder.size() != 11 || !GokuLadder.MASTERED.equals(GokuLadder.step(GokuLadder.last()).id())) bad.add("the ladder does not end in Mastered Ultra Instinct");
        if (GokuLadder.CLASSIC_LAST != 9) bad.add("the classic top moved");
        if (GokuLadder.damageMul(10, 1.0) < GokuLadder.damageMul(9, 1.0) || GokuLadder.speedMul(10, 1.0) < 1.0) bad.add("Mastered Ultra Instinct is weaker than True Ultra Instinct");
        if (GokuOmegaMath.hitShare(true, 0.3, 0.5) != 0.5 || GokuOmegaMath.hitShare(false, 0.3, 0.5) != 0.3) bad.add("the hit cap rule is wrong");
        if (GokuOmegaMath.unlockedCount(GokuOmegaMath.START_LEVEL) < 4) bad.add("he starts with fewer than four hacks");
        if (!GokuBossConfig.omega()) {
            return bad.isEmpty() ? SelfTests.Result.ok("Omega is switched off: the old Goku (ladder top, caps) is in place") : SelfTests.Result.fail(String.join(" | ", bad));
        }
        List<GokuOmegaMath.Sheet> live = registeredRivals();
        List<GokuOmegaMath.Sheet> roster = GokuOmega.rivals();
        if (live.isEmpty()) bad.add("no other boss entity type was found in the registry");
        double[] floors = GokuOmega.floors();
        if (!(floors[0] > 1.0) || floors[0] > GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR || !(floors[1] > 1.0) || floors[1] > GokuOmegaMath.OMEGA_MAX_DAMAGE_FACTOR) bad.add("the floors are out of range: " + floors[0] + " / " + floors[1]);
        GokuBossMath.Params p = GokuBossConfig.params();
        int worst = 0;
        for (double ep : new double[]{0.0, 800, 1e4, 1e5, 1e6, 1e8, 1e12}) {
            for (int players : new int[]{1, 4, 10}) {
                GokuBossMath.Reading r = GokuBossMath.resolve(p.epScaling(), ep, 0.0, p.preset());
                double h = GokuOmegaMath.omegaHealth(GokuBossMath.healthFactor(r, players, p), GokuBossConfig.get(GokuBossConfig.OMEGA_HEALTH), GokuOmegaMath.START_LEVEL, floors[0]);
                double d = GokuOmegaMath.omegaDamage(GokuBossMath.damageFactor(r, players, p), GokuBossConfig.get(GokuBossConfig.OMEGA_DAMAGE), GokuOmegaMath.START_LEVEL, floors[1]);
                if (!GokuOmegaMath.strongest(GokuOmega.sheet(), h, d, roster)) { bad.add("not above the roster at " + ep + " EP, " + players + " players"); worst++; }
                if (!GokuOmegaMath.strongest(GokuOmega.sheet(), h, d, live)) { bad.add("not above the registered boss types at " + ep + " EP, " + players + " players"); worst++; }
                if (worst > 3) break;
            }
        }
        double share = GokuBossConfig.hitShare();
        if (share < GokuBossConfig.maxHitPercent() - 1e-9 || share > GokuOmegaMath.HARD_HIT_SHARE + 1e-9) bad.add("the Omega hit share is " + share);
        if (GokuBossConfig.dodgeCap() > 0.95 + 1e-9) bad.add("the dodge cap is " + GokuBossConfig.dodgeCap());
        return bad.isEmpty() ? SelfTests.Result.ok("Goku Omega: ladder to Mastered, above " + roster.size() + " roster bosses and " + live.size() + " registered boss types at every EP and party size (floors x"
                + String.format(Locale.ROOT, "%.1f", floors[0]) + " health, x" + String.format(Locale.ROOT, "%.1f", floors[1]) + " damage), hit share " + share)
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    /** The saved record round-trips, the training and fight arithmetic run on a scratch copy, and the real world's record answers (it is only read). */
    private static SelfTests.Result world(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        GokuWorld w = new GokuWorld();
        GokuOmegaMath.Progress p = w.progress;
        p.train(1000, 60, 1500);
        long gained = p.train(1000 + 5 * GokuOmegaMath.TICKS_PER_DAY, 60, 1500);
        if (gained != 5 * 1440L) bad.add("five days of training gave " + gained + " instead of " + 5 * 1440L);
        long fightXp = p.fight(GokuOmegaMath.Outcome.WIN, 3, 2, 1000 + 5 * GokuOmegaMath.TICKS_PER_DAY, 6000);
        if (fightXp != GokuOmegaMath.fightXp(GokuOmegaMath.Outcome.WIN, 3, 2)) bad.add("a win gave " + fightXp);
        CompoundTag tag = w.save(new CompoundTag(), null);
        GokuWorld back = GokuWorld.load(tag, null);
        if (back.progress.xp != p.xp || back.progress.lastTrain != p.lastTrain || back.progress.wins != p.wins || back.progress.trainToday != p.trainToday) bad.add("the record does not round-trip");
        if (GokuWorld.load(new CompoundTag(), null).progress.xp != GokuOmegaMath.totalXpFor(GokuOmegaMath.START_LEVEL)) bad.add("an empty record does not start at the start level");
        GokuWorld scratch = new GokuWorld();
        long first = scratch.settle(server, false);
        long second = scratch.settle(server, true);
        if (first != 0L || second != 0L) bad.add("the first settle gave " + first + " and a settle in a fight " + second);
        GokuWorld real = GokuWorld.get(server);
        if (real != GokuWorld.get(server)) bad.add("the world record is not cached");
        int lv = real.level();
        if (lv < 1 || lv > GokuOmegaMath.HARD_MAX_LEVEL) bad.add("the real level is " + lv);
        return bad.isEmpty() ? SelfTests.Result.ok("Goku's record round-trips, training and fight experience add up, the world's level is " + lv)
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad));
    }

    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        if (!GokuBossConfig.enabled() || !GokuBossConfig.omega()) return SelfTests.Result.ok("skipped: the boss or Omega is switched off");
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return SelfTests.Result.ok("skipped: " + GokuBoss.liveCount() + " Goku bosses already stand");
        GokuBoss boss = null;
        LivingEntity foe = null;
        try {
            Vec3 where = Vec3.atBottomCenterOf(at.above(2)).add(0, 0, 8);
            ArcSpawner.Result r = ArcSpawner.summon(level, GokuBoss.CHARACTER, where, 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED || !(r.boss() instanceof GokuBoss g)) return SelfTests.Result.fail("summoning gave " + r.outcome());
            boss = g;
            List<String> bad = new ArrayList<>();
            GokuOmega om = g.omega();
            om.setLevelForTest(GokuOmegaMath.START_LEVEL);
            g.scanNow();
            double[] floors = GokuOmega.floors();
            if (g.healthFactor() < floors[0] - 1e-6 || g.healthFactor() > GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR + 1e-6) bad.add("health factor " + g.healthFactor() + " vs floor " + floors[0]);
            if (g.damageFactorValue() < floors[1] - 1e-6 || g.damageFactorValue() > GokuOmegaMath.OMEGA_MAX_DAMAGE_FACTOR + 1e-6) bad.add("damage factor " + g.damageFactorValue() + " vs floor " + floors[1]);
            double hLow = g.healthFactor();
            om.setLevelForTest(60);
            g.scanNow();
            if (g.healthFactor() < hLow - 1e-9) bad.add("a higher level lowered his health factor");
            om.setLevelForTest(GokuOmegaMath.START_LEVEL);
            if (om.ladderTop() != GokuLadder.last()) bad.add("the ladder top is not Mastered Ultra Instinct");
            double before = g.formDamageModifier();
            g.forceFormForTest(GokuLadder.last());
            if (g.formIdx() != GokuLadder.last() || !(g.formDamageModifier() > 0.0) || g.formDamageModifier() < before) bad.add("Mastered Ultra Instinct puts no damage modifier on him");
            if (!g.barTitle().contains("Lv.")) bad.add("the boss bar does not show his level: " + g.barTitle());
            DamageSource melee = level.damageSources().generic();
            if (GokuOmega.kind(level.damageSources().onFire()) != GokuOmegaMath.FIRE || GokuOmega.kind(level.damageSources().explosion(null, null)) != GokuOmegaMath.EXPLOSION) bad.add("damage kinds are mis-read");
            if (GokuOmega.kind(melee) == GokuOmegaMath.FIRE) bad.add("a generic hit reads as fire");
            // the hacks that need a foe: Energy Drain and Skill Seal on a test animal (no Tensura data: they must not throw), the Second Wind cap
            foe = ArtTestFoe.make(level, where.add(5, 0, 0), g.getEyePosition());
            if (foe != null) {
                om.setLevelForTest(30);
                om.drain(foe);
                if (!om.seal(foe)) bad.add("the Skill Seal reported failure");
            }
            om.fightOpenForTest();
            om.setLevelForTest(30);
            for (int i = 0; i < 20; i++) om.onFormChanged();
            if (om.healedForTest() > g.getMaxHealth() * GokuOmegaMath.WIND_FIGHT_CAP + 1f) bad.add("the Second Wind healed " + om.healedForTest() + " (cap " + g.getMaxHealth() * GokuOmegaMath.WIND_FIGHT_CAP + ")");
            if (om.usesForTest(GokuOmegaMath.Hack.SECOND_WIND) > GokuOmegaMath.Hack.SECOND_WIND.fightCap) bad.add("the Second Wind passed its fight cap");
            om.clear();
            g.clearModsForTest();
            return bad.isEmpty() ? SelfTests.Result.ok("Goku Omega summoned: health x" + String.format(Locale.ROOT, "%.1f", hLow) + ", damage x" + String.format(Locale.ROOT, "%.1f", g.damageFactorValue())
                    + ", Mastered Ultra Instinct, drain and seal run, the Second Wind holds its cap")
                    : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad));
        } finally {
            if (foe != null) foe.discard();
            if (boss != null) boss.discard();
        }
    }
}
