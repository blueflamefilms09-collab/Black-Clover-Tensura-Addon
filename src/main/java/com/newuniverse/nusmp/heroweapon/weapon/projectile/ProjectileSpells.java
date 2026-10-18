package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Geometry;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Tally;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Timed;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.projectile.ProjectileNumbers.*;

/**
 * Why: the bodies of the Projectile's ten throws. A thrown blade here is a hitscan line (it stops at a wall, strikes the first foes on it, and a thread of light
 * is drawn along its path), so there is no entity to track, and a blade that "returns" is a second line drawn back to the wielder with its own strike, scheduled
 * through {@link Timed} (which re-checks the wielder when it runs). Every foe a blade hits is MARKED, which is what Recall Blades uses. The per-foe and per-cast
 * limits of the budget are kept by a {@link Tally} per cast, a repeat hit on a foe resets its hurt time so that it lands, a bolt never passes through stone,
 * and every technique above 8 raw (the bolas burst, the field of blades) shows a ring or a cocoon before it lands. All damage goes through {@code HeroCtx.hit};
 * the only hard control (Shadow Bind) asks {@code HeroCtx.control()} first and lets {@code BalanceLaw.controlTicks} cap its length.
 */
final class ProjectileSpells {
    private ProjectileSpells() {}

    /** The projectile's colours: 0xRRGGBB for ground rings, 0xAARRGGBB for effects. */
    static final int TEAL_RGB = 0x38F0C0, TEAL = 0xFF38F0C0;

    // ================================================================ shared pieces
    private static Vec3 centre(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    private static void swing(HeroCtx c) { c.player.swing(InteractionHand.MAIN_HAND); }

    /** The thread of light a blade leaves along its path. */
    private static void line(HeroCtx c, Vec3 from, Vec3 to) { c.fx(VfxShape.THREAD_LINE, from, to, TEAL, 8, 0.8f); }

    /** Marks a foe that a blade hit; the mark lasts {@link ProjectileNumbers#MARK_TICKS} and shows a lock sigil the first time. */
    static void mark(HeroCtx c, LivingEntity t) {
        if (!c.form.has("mark_prey")) return;
        long now = c.level.getGameTime();
        UUID who = c.player.getUUID();
        boolean fresh = !ProjectileState.marked(who, t.getUUID(), now);
        ProjectileState.mark(who, t.getUUID(), now + MARK_TICKS, now);
        if (fresh) {
            Vec3 at = t.position().add(0.0, t.getBbHeight() * 0.5, 0.0);
            c.fx(VfxShape.COMPASS_LOCK, at, at, TEAL, 20, 0.8f);
        }
    }

    /** A hit that lands even if the foe was hurt a moment ago by an earlier blade of the same cast. */
    private static boolean strike(HeroCtx c, LivingEntity t, double raw, Tally tally) {
        if (!tally.allows(t.getUUID())) return false;
        if (tally.seen(t.getUUID())) t.invulnerableTime = 0;
        if (!c.hit(t, (float) raw)) return false;
        tally.add(t.getUUID());
        mark(c, t);
        return true;
    }

    /** Where a flight ended, whether a wall ended it, that wall's normal, and the foes it struck. */
    private record Thrown(Vec3 end, boolean wall, Vec3 face, List<LivingEntity> struck) {}

    /** One blade: stops at a wall, strikes up to {@code pierce} foes on its line (as the tally allows) and ends at the last of them. */
    private static Thrown blade(HeroCtx c, Vec3 from, Vec3 dir, double range, int pierce, double raw, Tally tally) {
        Rays.Reach reach = Rays.reach(c, from, dir, range);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity t : Foes.along(c, from, reach.end(), THROW_WIDTH, Budget.MAX_TARGETS)) {
            if (struck.size() >= pierce) break;
            if (strike(c, t, raw, tally)) struck.add(t);
        }
        boolean stoppedByFoe = struck.size() >= pierce;
        Vec3 end = stoppedByFoe ? centre(struck.get(struck.size() - 1)) : reach.end();
        line(c, from, end);
        return new Thrown(end, !stoppedByFoe && reach.wall(), reach.face(), struck);
    }

    /** The nearest foe within {@code maxDist} of {@code from} that the blade can see and that is not in {@code skip}, or null. */
    private static LivingEntity nearestVisible(HeroCtx c, Vec3 from, double maxDist, Tally tally) {
        for (LivingEntity t : Foes.around(c, from, maxDist, Budget.MAX_TARGETS)) {
            if (!tally.allows(t.getUUID()) || tally.seen(t.getUUID())) continue;
            Vec3 to = centre(t);
            if (!Rays.reach(c, from, to.subtract(from), from.distanceTo(to)).wall()) return t;
        }
        return null;
    }

    // ================================================================ Air Strike Throw
    static boolean instantThrow(HeroCtx c) {
        Tally tally = new Tally(INSTANT_THROW.maxTargets(), INSTANT_THROW.hitsPerTarget());
        swing(c);
        c.sound(SoundEvents.SNOWBALL_THROW, 0.9f, 1.3f);
        blade(c, c.eye(), c.look(), THROW_RANGE, THROW_PIERCE, INSTANT_THROW.rawBase(), tally);
        if (c.form.has("second_throw")) {
            Timed.later(c, SECOND_DELAY, live -> {
                live.player.swing(InteractionHand.MAIN_HAND);
                live.sound(SoundEvents.SNOWBALL_THROW, 0.9f, 1.5f);
                blade(live, live.eye(), live.look(), THROW_RANGE, THROW_PIERCE, INSTANT_THROW.rawBase(), tally);
            });
        }
        return true;
    }

    // ================================================================ Returning Edge
    static boolean boomerang(HeroCtx c) {
        Tally tally = new Tally(BOOMERANG.maxTargets(), BOOMERANG.hitsPerTarget());
        Vec3 look = c.look();
        swing(c);
        c.sound(SoundEvents.SNOWBALL_THROW, 0.9f, 0.7f);
        Thrown out = blade(c, c.eye(), look, BOOMERANG_RANGE, BOOMERANG_PIERCE, BOOMERANG.rawBase(), tally);
        Vec3 turn = out.end();
        double[] h = Geometry.turnYaw(look.x, look.z, 90.0);
        Vec3 side = h[0] * h[0] + h[1] * h[1] < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(h[0], 0.0, h[1]).normalize();
        Timed.later(c, BOOMERANG_RETURN, live -> {
            Vec3 home = live.eye().subtract(0.0, 0.3, 0.0);
            Vec3 mid = turn.add(home).scale(0.5).add(side.scale(BOOMERANG_SIDE));
            live.sound(SoundEvents.TRIDENT_RETURN, 0.9f, 1.4f);
            leg(live, turn, mid, tally);
            leg(live, mid, home, tally);
        });
        return true;
    }

    private static void leg(HeroCtx c, Vec3 from, Vec3 to, Tally tally) {
        line(c, from, to);
        for (LivingEntity t : Foes.along(c, from, to, THROW_WIDTH, Budget.MAX_TARGETS)) strike(c, t, BOOMERANG.rawBase(), tally);
    }

    // ================================================================ Ricochet Throw
    static boolean ricochet(HeroCtx c) {
        boolean wide = c.form.has("wide_ricochet");
        int jumps = RICOCHET_JUMPS + (wide ? 1 : 0);
        double jump = RICOCHET_JUMP + (wide ? RICOCHET_WIDE_JUMP : 0.0);
        Tally tally = new Tally(RICOCHET.maxTargets(), RICOCHET.hitsPerTarget());
        Vec3 eye = c.eye();
        Rays.Reach reach = Rays.reach(c, eye, c.look(), RICOCHET_RANGE);
        List<LivingEntity> direct = Foes.along(c, eye, reach.end(), THROW_WIDTH, 1);
        List<LivingEntity> sequence = new ArrayList<>();
        Vec3 origin;
        if (!direct.isEmpty()) {
            sequence.add(direct.get(0));
            origin = centre(direct.get(0));
        } else if (reach.wall()) {
            origin = reach.end().add(reach.face().scale(0.2));
        } else {
            c.say("Ricochet Throw needs a foe or a wall to glance off");
            return false;
        }
        Vec3 at = origin;
        Tally plan = new Tally(RICOCHET.maxTargets(), 1);
        if (!sequence.isEmpty()) plan.add(sequence.get(0).getUUID());
        while (sequence.size() < jumps + (direct.isEmpty() ? 0 : 1)) {
            LivingEntity next = nearestVisible(c, at, jump, plan);
            if (next == null) break;
            sequence.add(next);
            plan.add(next.getUUID());
            at = centre(next);
        }
        if (sequence.isEmpty()) {
            c.say("Nothing for the blade to ricochet to");
            return false;
        }
        swing(c);
        c.sound(SoundEvents.SNOWBALL_THROW, 0.9f, 1.1f);
        Vec3 from = eye;
        if (direct.isEmpty()) {
            line(c, eye, origin);
            from = origin;
            c.sound(SoundEvents.CHAIN_HIT, 0.8f, 1.6f);
        }
        for (LivingEntity t : sequence) {
            strike(c, t, RICOCHET.rawBase(), tally);
            Vec3 to = centre(t);
            line(c, from, to);
            from = to;
        }
        return true;
    }

    // ================================================================ Knife Rain
    /** What a fan of knives from {@code eye} along {@code look} meets: the path of each knife, and how many knives each foe takes (the first foe each knife meets). */
    private record Fan(List<Vec3[]> paths, Map<UUID, Integer> counts, Map<UUID, LivingEntity> foes) {}

    private static Fan fan(HeroCtx c, Vec3 eye, Vec3 look) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        Map<UUID, LivingEntity> foes = new LinkedHashMap<>();
        List<Vec3[]> paths = new ArrayList<>();
        for (double angle : Geometry.fan(KNIFE_COUNT, KNIFE_SPREAD)) {
            double[] h = Geometry.turnYaw(look.x, look.z, angle);
            Vec3 dir = new Vec3(h[0], look.y, h[1]);
            Rays.Reach reach = Rays.reach(c, eye, dir, KNIFE_RANGE);
            Vec3 end = reach.end();
            List<LivingEntity> hit = Foes.along(c, eye, reach.end(), KNIFE_WIDTH, 1);
            if (!hit.isEmpty()) {
                LivingEntity t = hit.get(0);
                end = centre(t);
                counts.merge(t.getUUID(), 1, Integer::sum);
                foes.put(t.getUUID(), t);
            }
            paths.add(new Vec3[]{eye.add(dir.normalize().scale(0.5)), end});
        }
        return new Fan(paths, counts, foes);
    }

    static boolean knifeRain(HeroCtx c) {
        Vec3 look = c.look();
        if (fan(c, c.eye(), look).counts().isEmpty()) {
            c.say("No foe in the fan of knives");
            return false;
        }
        swing(c);
        // the tell: the two edges and the middle of the fan; the aim is locked now and the knives leave the hand a moment later
        Vec3 eye = c.eye();
        for (double edge : new double[]{-KNIFE_SPREAD / 2.0, 0.0, KNIFE_SPREAD / 2.0}) {
            double[] h = Geometry.turnYaw(look.x, look.z, edge);
            Tells.line(c.level, eye.add(0.0, -0.3, 0.0), eye.add(h[0] * KNIFE_RANGE, look.y * KNIFE_RANGE - 0.3, h[1] * KNIFE_RANGE), TEAL_RGB);
        }
        c.sound(SoundEvents.SNOWBALL_THROW, 0.7f, 0.9f);
        Timed.later(c, KNIFE_DELAY, live -> {
            Fan f = fan(live, live.eye(), look);
            if (f.counts().isEmpty()) return;
            live.sound(SoundEvents.SNOWBALL_THROW, 0.9f, 1.8f);
            for (Vec3[] p : f.paths()) live.fx(VfxShape.THREAD_LINE, p[0], p[1], TEAL, 6, 0.5f);
            Tally tally = new Tally(KNIFE_RAIN.maxTargets(), 1);
            for (Map.Entry<UUID, Integer> e : f.counts().entrySet()) {
                LivingEntity t = f.foes().get(e.getKey());
                if (tally.allows(t.getUUID())) strike(live, t, KNIFE_RAIN.rawBase() * Math.min(KNIFE_CAP, e.getValue()), tally);
            }
        });
        return true;
    }

    // ================================================================ Tornado Throw
    static boolean tornadoThrow(HeroCtx c) {
        LivingEntity t = Foes.aimed(c, TORNADO_RANGE, 0.9);
        if (t == null) {
            c.say("Tornado Throw needs a foe in your line of sight");
            return false;
        }
        swing(c);
        line(c, c.eye(), centre(t));
        VfxSpawn.sendFollowing(c.level, VfxShape.WIND_TORNADO, t, t.position(), TEAL, TORNADO_TICKS + 8, 1.2f);
        c.sound(SoundEvents.BREEZE_WHIRL, 0.8f, 1.4f);
        int slowTicks = TORNADO_SLOW + (c.form.has("heavy_hatchet") ? TORNADO_HEAVY_SLOW : 0);
        Tally tally = new Tally(1, TORNADO_THROW.hitsPerTarget());
        Timed.zone(c, TORNADO_TICKS, TORNADO_INTERVAL, (live, age) -> {
            if (!t.isAlive()) return;
            if (age == 0) Status.slow(t, slowTicks, 2);
            strike(live, t, TORNADO_THROW.rawBase(), tally);
        });
        return true;
    }

    // ================================================================ Bolas Blast
    static boolean bolasBlast(HeroCtx c) {
        LivingEntity t = Foes.aimed(c, BOLAS_RANGE, 0.9);
        if (t == null) {
            c.say("Bolas Blast needs a foe in your line of sight");
            return false;
        }
        double radius = BOLAS_RADIUS + (c.form.has("heavy_hatchet") ? BOLAS_HEAVY_RADIUS : 0.0);
        Vec3 last = t.position();
        swing(c);
        line(c, c.eye(), centre(t));
        Status.slow(t, BOLAS_SLOW, 2);
        Tells.ring(c.level, last, radius, TEAL_RGB);
        VfxSpawn.sendFollowing(c.level, VfxShape.THREAD_COCOON, t, last, TEAL, BOLAS_DELAY, 1.0f);
        c.sound(SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.9f);
        Timed.later(c, BOLAS_DELAY, live -> {
            Vec3 at = t.isAlive() ? t.position() : last;
            Tally tally = new Tally(BOLAS_BLAST.maxTargets(), 1);
            for (LivingEntity f : Foes.around(live, at, radius, BOLAS_BLAST.maxTargets())) strike(live, f, BOLAS_BLAST.rawBase(), tally);
            live.fx(VfxShape.FIRE_BURST, at, at, TEAL, 20, (float) (radius * 0.7));
            live.sound(SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        });
        return true;
    }

    // ================================================================ Dritte Throw
    static boolean dritteThrow(HeroCtx c) {
        List<LivingEntity> picks = Foes.cone(c, DRITTE_RANGE, DRITTE_HALF_ANGLE, DRITTE_COUNT);
        if (picks.isEmpty()) {
            c.say("Dritte Throw needs a foe in front of you");
            return false;
        }
        Tally tally = new Tally(DRITTE_THROW.maxTargets(), DRITTE_THROW.hitsPerTarget());
        swing(c);
        for (int i = 0; i < DRITTE_COUNT; i++) {
            LivingEntity t = picks.get(i % picks.size());
            Timed.later(c, i * DRITTE_STAGGER, live -> {
                if (!t.isAlive()) return;
                Vec3 from = live.eye(), to = centre(t);
                if (Rays.reach(live, from, to.subtract(from), from.distanceTo(to)).wall()) return;
                live.sound(SoundEvents.SNOWBALL_THROW, 0.8f, 1.6f);
                line(live, from, to);
                strike(live, t, DRITTE_THROW.rawBase(), tally);
            });
        }
        return true;
    }

    // ================================================================ Shadow Bind
    static boolean shadowBind(HeroCtx c) {
        LivingEntity t = Foes.aimed(c, BIND_RANGE, 0.9);
        if (t == null) {
            c.say("Shadow Bind needs a foe in your line of sight");
            return false;
        }
        if (!c.control()) {
            c.say("Your binding magic needs " + (BalanceLaw.controlCooldownLeft(c.player) + 19) / 20 + " s to recover");
            return false;
        }
        swing(c);
        int ticks = BalanceLaw.controlTicks(t, BIND_TICKS);
        Status.slow(t, ticks, 6);
        Vec3 shadow = t.position();
        line(c, c.eye(), shadow);
        c.fx(VfxShape.SEAL_CHAINS, shadow, shadow, TEAL, ticks, 1.1f);
        c.sound(SoundEvents.CHAIN_PLACE, 1.0f, 0.8f);
        mark(c, t);
        return true;
    }

    // ================================================================ Recall Blades
    static boolean recall(HeroCtx c) {
        long now = c.level.getGameTime();
        UUID who = c.player.getUUID();
        List<UUID> ids = ProjectileState.markedIds(who, now);
        if (ids.isEmpty()) {
            c.say("No foe is marked");
            return false;
        }
        Vec3 here = c.player.position();
        AABB box = new AABB(here, here).inflate(RECALL_RANGE);
        List<LivingEntity> marked = new ArrayList<>(c.level.getEntitiesOfClass(LivingEntity.class, box,
                e -> ids.contains(e.getUUID()) && e.distanceToSqr(here) <= RECALL_RANGE * RECALL_RANGE && Foes.isFoe(c.player, e)));
        marked.sort((a, b) -> Double.compare(a.distanceToSqr(here), b.distanceToSqr(here)));
        if (marked.isEmpty()) {
            c.say("The marked foes are out of reach");
            return false;
        }
        swing(c);
        c.sound(SoundEvents.TRIDENT_RETURN, 1.0f, 1.2f);
        Vec3 home = c.eye().subtract(0.0, 0.3, 0.0);
        Tally tally = new Tally(RECALL.maxTargets(), 1);
        for (LivingEntity t : marked) {
            if (!tally.allows(t.getUUID())) break;
            Vec3 at = centre(t);
            line(c, at, home);
            if (strike(c, t, RECALL.rawBase(), tally)) ProjectileState.unmark(who, t.getUUID());
        }
        return true;
    }

    // ================================================================ Thousand Blade Rain (ultimate)
    static boolean thousandBlades(HeroCtx c) {
        Vec3 p = c.aim(BLADES_REACH);
        Vec3 center = Rays.reach(c, p.add(0.0, 0.6, 0.0), new Vec3(0.0, -1.0, 0.0), 10.0).end().add(0.0, 0.05, 0.0);
        swing(c);
        Tells.ring(c.level, center, BLADES_RADIUS, TEAL_RGB);
        Tells.column(c.level, center, 5.0, TEAL_RGB);
        c.fx(VfxShape.SEAL_BASIC, center, center.add(0.0, 1.0, 0.0), TEAL, BLADES_TELL + (BLADES_VOLLEYS - 1) * BLADES_INTERVAL + 10, (float) BLADES_RADIUS);
        c.sound(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.6f);
        Tally tally = new Tally(THOUSAND_BLADES.maxTargets(), THOUSAND_BLADES.hitsPerTarget());
        Timed.later(c, BLADES_TELL, live -> Timed.zone(live, (BLADES_VOLLEYS - 1) * BLADES_INTERVAL, BLADES_INTERVAL, (cc, age) -> volley(cc, center, tally)));
        return true;
    }

    private static void volley(HeroCtx c, Vec3 center, Tally tally) {
        for (LivingEntity t : Foes.around(c, center, BLADES_RADIUS, THOUSAND_BLADES.maxTargets())) strike(c, t, THOUSAND_BLADES.rawBase(), tally);
        for (int i = 0; i < 4; i++) {
            double a = c.level.random.nextDouble() * Math.PI * 2.0, r = Math.sqrt(c.level.random.nextDouble()) * BLADES_RADIUS;
            Vec3 land = center.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
            c.fx(VfxShape.THREAD_LINE, land.add(0.0, 12.0, 0.0), land, TEAL, 6, 0.6f);
        }
        c.sound(SoundEvents.TRIDENT_HIT_GROUND, 0.5f, 1.8f);
    }
}
