package com.newuniverse.nusmp.meteor;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The CI self-test of the shared meteor (run by the server smoke test with a stand-in player, see labyrinth.mega.SelfTests): {@code meteor.fall}.
 * It drops real meteors on a dedicated server and drives their ticks by hand: the entity exists and sits at its start, it comes closer every tick, it lands exactly on its last tick and
 * runs the caller's landing once, the books are clean afterwards; the per-caster cap refuses the extra meteor and the cleanup (logout, death, dimension change) takes the rest out of the air;
 * the generic landing hurts a foe inside the radius and spares one outside it and the caster; and with {@code meteor.realMeteors} off a drop is refused and the caller's old code runs
 * ({@link MeteorFall#dropOr}). It also checks that every wired ability has a spec.
 */
public final class MeteorSelfTests {
    private MeteorSelfTests() {}

    public static void register() { SelfTests.register("meteor.fall", MeteorSelfTests::fall); }

    private static Result fall(MinecraftServer server) {
        for (String id : MeteorWiring.WIRED.keySet()) {
            MeteorSpec s = MeteorWiring.spec(id);
            if (s.style() == null || s.size() <= 0f || s.fallTicks() < MeteorRules.MIN_FALL) return Result.fail("the wired ability " + id + " has a bad spec");
        }
        if (MeteorWiring.WIRED.size() < 6) return Result.fail("fewer than six abilities are wired");
        ServerLevel sl = server.overworld();
        FakePlayer p = SelfTests.player(sl, "meteor_fall", 8.5, 100, 8.5);
        Vec3 impact = new Vec3(8.5, 100, 12.5);
        if (!MeteorFall.enabled()) return Result.fail("meteor.realMeteors is off in the test world");
        MeteorFall.clear(p.getUUID());
        int base = MeteorFall.inFlightTotal();

        // ---- one meteor, driven by hand
        AtomicInteger landed = new AtomicInteger();
        int fall = 12;
        if (!MeteorFall.drop(p, impact, MeteorSpec.of(MeteorStyle.FIRE).fall(fall).onImpact((lvl, at, who) -> landed.incrementAndGet())))
            return Result.fail("drop refused a plain meteor (is the chunk at the impact point loaded?)");
        MeteorEntity m = find(sl, impact);
        if (m == null) return Result.fail("the meteor entity is not in the level after drop");
        if (MeteorFall.inFlight(p.getUUID()) != 1 || MeteorFall.inFlightTotal() != base + 1) return Result.fail("the books do not count the meteor");
        double h0 = m.getY();
        if (h0 < impact.y + 11.0 || h0 > impact.y + MeteorRules.MAX_HEIGHT + 2.0) return Result.fail("the start is " + (h0 - impact.y) + " blocks above the impact");
        double last = m.position().distanceTo(impact);
        for (int i = 1; i < fall; i++) {
            m.tickCount = i;
            m.tick();
            double d = m.position().distanceTo(impact);
            if (d >= last) return Result.fail("the meteor did not come closer on tick " + i);
            last = d;
            if (landed.get() != 0) return Result.fail("the landing ran before the last tick");
        }
        m.tickCount = fall;
        m.tick();
        if (landed.get() != 1) return Result.fail("the landing ran " + landed.get() + " times on the last tick");
        if (!m.isRemoved() || MeteorFall.inFlight(p.getUUID()) != 0 || MeteorFall.inFlightTotal() != base) return Result.fail("the meteor is still on the books after it landed");

        // ---- the caps and the cleanup
        int cap = Math.min(MeteorConfig.maxPerCaster(), MeteorConfig.maxGlobal() - base);
        int made = 0;
        for (int i = 0; i < cap + 1; i++) if (MeteorFall.drop(p, impact.add(i, 0, 0), MeteorSpec.of(MeteorStyle.ICE).fall(30))) made++;
        if (made != cap) return Result.fail("the cap of " + cap + " meteors let " + made + " through");
        List<MeteorEntity> up = sl.getEntitiesOfClass(MeteorEntity.class, new AABB(impact, impact).inflate(80));
        MeteorFall.clear(p.getUUID());
        for (MeteorEntity e : up) if (!e.isRemoved()) return Result.fail("cleanup left a meteor in the air");
        if (MeteorFall.inFlight(p.getUUID()) != 0) return Result.fail("cleanup left the books dirty");

        // ---- the generic landing
        Mob near = foe(sl, impact.add(1.0, 0, 0)), far = foe(sl, impact.add(9.0, 0, 0));
        try {
            MeteorSpec g = MeteorSpec.of(MeteorStyle.KI).radius(3.0).damage(8f, "minecraft:magic").knockback(0.3).clamped(60);
            float nearHp = near.getHealth(), farHp = far.getHealth(), selfHp = p.getHealth();
            MeteorEntity stand = MeteorEntities.METEOR.get().create(sl);
            MeteorFall.genericLanding(sl, impact, g, p, stand);
            if (!(near.getHealth() < nearHp)) return Result.fail("the foe inside the radius took no damage");
            if (far.getHealth() < farHp || p.getHealth() < selfHp) return Result.fail("the landing hurt something outside the radius or the caster");
        } finally {
            near.discard();
            far.discard();
        }

        // ---- the setting off restores the old behaviour
        String setting;
        try {
            MeteorConfig.REAL_METEORS.set(false);
            AtomicInteger oldRan = new AtomicInteger();
            boolean flew = MeteorFall.dropOr(p, impact, MeteorSpec.of(MeteorStyle.FIRE), oldRan::incrementAndGet);
            if (flew || oldRan.get() != 1 || MeteorFall.inFlight(p.getUUID()) != 0) return Result.fail("with meteor.realMeteors off the old code did not run in place of the meteor");
            setting = "off path ok";
        } catch (IllegalStateException | UnsupportedOperationException cannotSet) {
            setting = "off path not tested (config not settable)";
        } finally {
            try { MeteorConfig.REAL_METEORS.set(true); } catch (IllegalStateException | UnsupportedOperationException ignored) { }
        }
        return Result.ok("a meteor falls " + fall + " ticks from " + Math.round(h0 - impact.y) + " blocks, lands once, cap " + cap + ", cleanup, generic landing, " + setting + ", " + MeteorWiring.WIRED.size() + " abilities wired");
    }

    private static MeteorEntity find(ServerLevel sl, Vec3 impact) {
        List<MeteorEntity> l = sl.getEntitiesOfClass(MeteorEntity.class, new AABB(impact, impact).inflate(80));
        return l.isEmpty() ? null : l.get(0);
    }

    private static Mob foe(ServerLevel sl, Vec3 at) {
        Mob z = EntityType.ZOMBIE.create(sl);
        z.setPos(at.x, at.y, at.z);
        z.setNoAi(true);
        z.setPersistenceRequired();
        sl.addFreshEntity(z);
        return z;
    }
}
