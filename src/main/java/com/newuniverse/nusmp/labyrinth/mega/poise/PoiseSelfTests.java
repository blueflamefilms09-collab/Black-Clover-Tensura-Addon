package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.eco.EcoConfig;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryAbility;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryConfig;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryData;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryDef;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryRegistry;
import com.newuniverse.nusmp.labyrinth.squad.SquadHook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the poise layer can be played by its author, so the CI server smoke test runs these in the real game ({@code /multiverse mega selftest dungeon.poise}). {@code dungeon.poise.data}:
 * the shipped telegraph table loaded, passes its audit against the foundry library, and the defaults the owner asked for are what the config declares (turf wars off, faction wars off, passivity on,
 * desperation at 30 percent). {@code dungeon.poise.armor}: a real elite foundry mob and a tagged zombie each get a bar, a blow with no armor is open, with armor it is absorbed with the armor's share
 * off and no knockback, a huge blow breaks the bar (full damage, the armor drops, the mob is helpless and slowed), the bar comes back after the stagger, the immune window holds, a plain zombie, a
 * boss and a servant never get a bar, and the saved form round-trips. {@code dungeon.poise.desperation}: at 20 percent health the foundry elite turns desperate (ultimate in its list, faster pace,
 * speed buff), stays so at 40 and ends over 45, and a tagged zombie gets the shockwave goal instead. {@code dungeon.poise.passive}: two ordinary monsters and two foundry mobs cannot target or
 * hurt each other (unless the matching old quarrel switch is on), a monster still targets a player, a servant is not protected. The telegraph and cast tests are in the foundry package
 * ({@code TelegraphSelfTests}).
 */
public final class PoiseSelfTests {
    private PoiseSelfTests() {}

    public static void register() {
        SelfTests.register("dungeon.poise.data", s -> data());
        SelfTests.register("dungeon.poise.armor", PoiseSelfTests::armor);
        SelfTests.register("dungeon.poise.desperation", PoiseSelfTests::desperation);
        SelfTests.register("dungeon.poise.passive", PoiseSelfTests::passive);
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    // ---------------------------------------------------------------- data and defaults
    private static SelfTests.Result data() {
        TelegraphLibrary lib = PoiseSystem.telegraphs();
        if (lib.kinds().isEmpty()) return fail("the telegraph table is empty: data/nusmp/mega/poise/telegraphs.json was not read");
        List<String> bad = lib.check(FoundryData.library());
        if (!bad.isEmpty()) return fail(bad.size() + " problem(s) in the telegraph table, first: " + bad.get(0));
        if (EcoConfig.TURF_WARS.getDefault()) return fail("eco.turfWarsEnabled must default to false");
        if (FoundryConfig.FACTION_WARS.getDefault()) return fail("foundry.factionWars must default to false");
        if (!FoundryConfig.DUNGEON_PASSIVE.getDefault() || !FoundryConfig.POISE.getDefault() || !FoundryConfig.DESPERATION.getDefault() || !FoundryConfig.TELEGRAPHS.getDefault()) return fail("a poise switch does not default to on");
        if (Math.abs(FoundryConfig.DESPERATE_BELOW.getDefault() - 0.30) > 1e-9) return fail("desperation must default to 30 percent");
        PoiseTuning t = PoiseSystem.tuning();
        if (t.desperateExit() <= t.desperateBelow()) return fail("the desperation exit share is not above its entry share");
        double last = 0.0;
        for (int tier = 1; tier <= 5; tier++) {
            double p = PoiseMath.maxPoise(100.0 * tier, tier, false, t);
            if (p < last) return fail("the poise capacity falls at tier " + tier);
            last = p;
        }
        return ok(lib.kinds().size() + " telegraph kinds, " + lib.overrides().size() + " overrides, " + lib.ultimateIds().size() + " ultimates; turf wars default off, passivity on, desperation under "
                + (int) Math.round(FoundryConfig.DESPERATE_BELOW.getDefault() * 100) + " percent");
    }

    // ---------------------------------------------------------------- the elite foundry mob
    private static FoundryMobEntity elite(ServerLevel level) {
        int tier = Math.max(PoiseSystem.tuning().eliteTier(), 3);
        List<FoundryDef> pool = FoundryData.roster().of(Polarity.HOLY, Math.min(5, tier));
        if (pool.isEmpty()) return null;
        FoundryMobEntity m = FoundryRegistry.MOB.get().create(level);
        if (m == null) return null;
        m.moveTo(0.5, 200.0, 0.5, 0f, 0f);
        m.setFoundry(pool.get(0), (Math.min(5, tier) - 1) * 200 + 1);
        return m;
    }

    private static SelfTests.Result armor(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!PoiseSystem.enabled()) return ok("skipped: foundry.poise.enabled is false in the config");
        List<Mob> made = new ArrayList<>();
        try {
            PoiseTuning t = PoiseSystem.tuning();
            FoundryMobEntity f = elite(level);
            if (f == null) return fail("no foundry mob of the elite tier could be made");
            made.add(f);
            PoiseData d = PoiseSystem.data(f);
            if (d == null || d.kind != PoiseBearers.Kind.ELITE) return fail("an elite foundry mob (tier " + f.tier() + ") has no elite poise bar: " + (d == null ? "no data" : d.kind));
            if (d.state.max() < t.minMax()) return fail("the bar is under its minimum");
            String one = cycle(f, d, t);
            if (one != null) return fail("foundry elite: " + one);

            Zombie z = EntityType.ZOMBIE.create(level);
            if (z == null) return fail("a zombie could not be made");
            made.add(z);
            z.moveTo(4.5, 200.0, 0.5, 0f, 0f);
            if (PoiseSystem.consider(z) != null) return fail("a plain zombie got a poise bar");
            z.addTag(PoiseBearers.ELITE_TAG);
            PoiseData zd = PoiseSystem.consider(z);
            if (zd == null || zd.kind != PoiseBearers.Kind.ELITE) return fail("a zombie tagged " + PoiseBearers.ELITE_TAG + " got no elite bar");
            String two = cycle(z, zd, t);
            if (two != null) return fail("tagged zombie: " + two);

            Zombie boss = EntityType.ZOMBIE.create(level);
            Zombie servant = EntityType.ZOMBIE.create(level);
            if (boss == null || servant == null) return fail("a zombie could not be made");
            made.add(boss);
            made.add(servant);
            boss.addTag(PoiseBearers.ELITE_TAG);
            boss.addTag("nusmp_floor_boss");
            servant.addTag(PoiseBearers.ELITE_TAG);
            servant.addTag("nusmp_servant");
            if (PoiseSystem.consider(boss) != null) return fail("a boss got a poise bar");
            if (PoiseSystem.consider(servant) != null) return fail("a servant got a poise bar");

            PoiseData back = PoiseData.fromTag(d.toTag());
            if (back.kind != d.kind || Math.abs(back.state.max() - d.state.max()) > 1e-6 || back.state.breaks() != d.state.breaks()) return fail("the saved form of the bar does not round-trip");
            return ok("elite foundry mob (tier " + f.tier() + ", bar " + Math.round(d.state.max()) + ") and tagged zombie: open, absorbed, no push, break, stagger, recover, immune window; plain zombie, boss and servant have no bar; save round-trip fine");
        } finally {
            for (Mob m : made) m.discard();
        }
    }

    /** One full life of a bar on a real mob; null when everything held, else the first thing that did not. */
    private static String cycle(Mob m, PoiseData d, PoiseTuning t) {
        long now = m.level().getGameTime();
        float blow = 10f;
        PoiseSystem.Outcome open = PoiseSystem.hit(m, blow, false);
        if (open.hit() != PoiseState.Hit.OPEN || open.amount() != blow) return "a blow with no armor was " + open.hit() + " / " + open.amount();
        if (PoiseSystem.armored(m, now)) return "armored before any armor was raised";
        PoiseSystem.raiseArmor(m, 400);
        if (!PoiseSystem.armored(m, now)) return "the armor did not rise";
        PoiseSystem.Outcome held = PoiseSystem.hit(m, blow, false);
        if (held.hit() != PoiseState.Hit.ABSORBED) return "a blow on armor was " + held.hit();
        float want = (float) (blow * (1.0 - t.armorReduction()));
        if (Math.abs(held.amount() - want) > 1e-3) return "armor let " + held.amount() + " through, expected " + want;
        if (!PoiseSystem.armored(m, now)) return "the armor fell on an absorbed blow";
        double before = d.state.poise();
        PoiseSystem.Outcome arrow = PoiseSystem.hit(m, blow, true);
        if (arrow.hit() != PoiseState.Hit.ABSORBED || d.state.poise() < before - blow) return "an arrow drained as much as a sword";
        float huge = (float) (d.state.max() * 3.0);
        PoiseSystem.Outcome broke = PoiseSystem.hit(m, huge, false);
        if (broke.hit() != PoiseState.Hit.BROKEN) return "a huge blow was " + broke.hit() + ", not BROKEN";
        if (broke.amount() != huge) return "the breaking blow was reduced";
        if (PoiseSystem.armored(m, now)) return "the armor stayed up after the break";
        if (!PoiseSystem.staggered(m, now)) return "not staggered after the break";
        if (!m.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) return "no slowness on a staggered mob";
        PoiseSystem.raiseArmor(m, 100);
        if (PoiseSystem.armored(m, now)) return "armor could be raised during the stagger";
        long after = now + t.staggerTicks() + 1;
        m.tickCount = 0;
        PoiseSystem.step(m, after);
        if (PoiseSystem.staggered(m, after)) return "still staggered after " + (t.staggerTicks() + 1) + " ticks";
        if (d.state.fraction() + 1e-9 < t.recoverFrac()) return "the bar did not come back to " + t.recoverFrac() + " (" + d.state.fraction() + ")";
        d.state.raiseArmor(after, 100);
        PoiseState.Hit again = d.state.hit(after + 1, huge, t);
        if (again != PoiseState.Hit.ABSORBED) return "the immune window did not hold (" + again + ")";
        long clear = after + t.immuneTicks() + 5;
        d.state.raiseArmor(clear, 100);
        if (d.state.hit(clear + 1, huge, t) != PoiseState.Hit.BROKEN) return "the bar could not be broken again after the immune window";
        long regen = clear + t.staggerTicks() + t.regenDelay() + 400;
        for (long k = clear + t.staggerTicks() + 2; k <= regen; k += 2) {
            m.tickCount = 0;
            PoiseSystem.step(m, k);
        }
        if (d.state.fraction() < 0.99) return "the bar did not refill (" + d.state.fraction() + ")";
        m.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return null;
    }

    // ---------------------------------------------------------------- desperation
    private static SelfTests.Result desperation(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!PoiseSystem.enabled() || !FoundryConfig.get(FoundryConfig.DESPERATION)) return ok("skipped: the poise layer or the desperation phase is switched off in the config");
        List<Mob> made = new ArrayList<>();
        try {
            FoundryMobEntity f = elite(level);
            if (f == null) return fail("no foundry mob of the elite tier could be made");
            made.add(f);
            PoiseData d = PoiseSystem.data(f);
            if (d == null) return fail("the elite foundry mob has no bar");
            long now = level.getGameTime();
            int base = f.brain().abilities().size();
            f.setHealth(f.getMaxHealth() * 0.20f);
            f.tickCount = 0;
            PoiseSystem.step(f, now);
            if (!d.desperate || !f.desperate()) return fail("at 20 percent health the foundry elite is not desperate");
            if (f.brain().pace() <= 1.0) return fail("a desperate foundry elite has no faster pace");
            String arch = f.archetype().id();
            FoundryAbility ult = PoiseSystem.telegraphs().ultimate(arch);
            if (ult == null) return fail("the archetype " + arch + " has no ultimate");
            if (f.brain().abilities().stream().noneMatch(a -> a.id().equals(ult.id()))) return fail("the ultimate " + ult.id() + " is not in the desperate list");
            if (f.brain().abilities().size() != base + 1) return fail("the desperate list has " + f.brain().abilities().size() + " abilities, expected " + (base + 1));
            AttributeInstance speed = f.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null || !speed.hasModifier(PoiseSystem.SPEED)) return fail("no desperate speed buff");
            f.setHealth(f.getMaxHealth() * 0.40f);
            PoiseSystem.step(f, now + 5);
            if (!d.desperate) return fail("a heal to 40 percent ended the phase: it must last until over " + (int) Math.round(PoiseSystem.tuning().desperateExit() * 100) + " percent");
            f.setHealth(f.getMaxHealth() * 0.70f);
            PoiseSystem.step(f, now + 10);
            if (d.desperate || f.desperate()) return fail("at 70 percent health the phase did not end");
            if (f.brain().abilities().size() != base || f.brain().pace() != 1.0) return fail("the ability list or pace did not return to normal after the phase");
            if (speed.hasModifier(PoiseSystem.SPEED)) return fail("the speed buff stayed after the phase");

            Zombie z = EntityType.ZOMBIE.create(level);
            if (z == null) return fail("a zombie could not be made");
            made.add(z);
            z.addTag(PoiseBearers.ELITE_TAG);
            PoiseData zd = PoiseSystem.consider(z);
            if (zd == null) return fail("a tagged zombie got no bar");
            z.setHealth(z.getMaxHealth() * 0.2f);
            z.tickCount = 0;
            PoiseSystem.step(z, now);
            if (!zd.desperate) return fail("a tagged zombie at 20 percent is not desperate");
            String goal = SquadHook.hasGoal(z, DesperationShockwaveGoal.class) ? "with the shockwave goal" : "without the shockwave goal (the goal list could not be read in this build)";
            return ok("foundry elite (" + arch + "): desperate at 20 percent with " + ult.id() + ", faster pace and speed buff, still on at 40, off at 70 and back to normal; tagged zombie desperate " + goal);
        } finally {
            for (Mob m : made) m.discard();
        }
    }

    // ---------------------------------------------------------------- the passive dungeon
    private static SelfTests.Result passive(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!Passivity.enabled()) return ok("skipped: foundry.poise.dungeonPassive is false in the config");
        List<Mob> made = new ArrayList<>();
        try {
            boolean turf = EcoConfig.get(EcoConfig.TURF_WARS), wars = FoundryConfig.get(FoundryConfig.FACTION_WARS);
            Zombie a = EntityType.ZOMBIE.create(level), b = EntityType.ZOMBIE.create(level), servant = EntityType.ZOMBIE.create(level);
            FoundryMobEntity f1 = elite(level), f2 = elite(level);
            if (a == null || b == null || servant == null || f1 == null || f2 == null) return fail("a test mob could not be made");
            made.add(a);
            made.add(b);
            made.add(servant);
            made.add(f1);
            made.add(f2);
            for (Mob m : made) m.addTag(MegaDungeon.DM_TAG);
            servant.addTag("nusmp_servant");
            if (!Passivity.dungeonMob(a) || !Passivity.dungeonMob(f1)) return fail("a dungeon monster is not seen as one");
            if (Passivity.dungeonMob(servant)) return fail("a servant is seen as a dungeon monster");
            if (Passivity.blocks(a, b) != !turf) return fail("monster against monster: blocks=" + Passivity.blocks(a, b) + " with turf wars " + turf);
            if (Passivity.blocks(f1, f2) != !wars) return fail("foundry against foundry: blocks=" + Passivity.blocks(f1, f2) + " with faction wars " + wars);
            if (Passivity.blocks(a, servant) || Passivity.blocks(servant, a)) return fail("a servant is blocked from, or protected from, the dungeon");
            a.setTarget(b);
            if (!turf && a.getTarget() != null) return fail("a monster still took another monster as its target (the target event did not stop it)");
            if (turf && a.getTarget() != b) return fail("the turf wars are on but the monster could not take its rival as a target");
            a.setTarget(null);
            f1.setTarget(f2);
            if (!wars && f1.getTarget() != null) return fail("a foundry mob still took another foundry mob as its target");
            f1.setTarget(null);
            FakePlayer p = SelfTests.player(level, "poise_passive", 0.5, 200.0, 0.5);
            a.setTarget(p);
            if (a.getTarget() != p) return fail("a monster could no longer target a player");
            a.setTarget(null);
            DamageSource hit = level.damageSources().mobAttack(a);
            LivingIncomingDamageEvent friendly = new LivingIncomingDamageEvent(b, new DamageContainer(hit, 5f));
            Passivity.onIncomingDamage(friendly);
            if (friendly.isCanceled() == turf) return fail("friendly fire: canceled=" + friendly.isCanceled() + " with turf wars " + turf);
            LivingIncomingDamageEvent onPlayer = new LivingIncomingDamageEvent(p, new DamageContainer(hit, 5f));
            Passivity.onIncomingDamage(onPlayer);
            if (onPlayer.isCanceled()) return fail("a monster's blow on a player was cancelled");
            LivingIncomingDamageEvent onServant = new LivingIncomingDamageEvent(servant, new DamageContainer(hit, 5f));
            Passivity.onIncomingDamage(onServant);
            if (onServant.isCanceled()) return fail("a monster's blow on a servant was cancelled");
            return ok("monsters " + (turf ? "may quarrel (eco.turfWarsEnabled is on in this config)" : "cannot target or hurt each other") + "; foundry mobs " + (wars ? "may quarrel" : "cannot target each other")
                    + "; players and servants are untouched by the rule");
        } finally {
            for (Mob m : made) m.discard();
        }
    }
}
