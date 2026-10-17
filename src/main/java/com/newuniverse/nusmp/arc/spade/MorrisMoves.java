package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: what Morris Libardirt's moves DO to the world, as plain static functions on any {@link ZogratisBoss}, so {@code MorrisBoss} stays a list of
 * moves, their telegraphs and the passive rules (the tuning, the rewrite, the thread). Everything is bounded the way the Zogratis moves are: a needle is a
 * point on a clipped ray with a life and a per-victim hit budget, a tendril is a hit along a line locked in the wind-up, a cage is one zone that looks at
 * a few dozen foes every other tick, a hard drag is asked of the Balance Law first and capped by it, and nothing here ever changes a block. His magic is
 * Modification, which the mod draws as rotating rune rings in orange (the colour of its wiki book); the gravity he borrows from Lucifero's contract is
 * drawn in the gravity violet. Numbers: {@link MorrisTable}, {@link CageMath}, {@link TetherMath}.
 */
final class MorrisMoves {
    private MorrisMoves() {}

    private static final String WHO = "Morris";
    /** Plain RGB for the dust, and the ARGB of Modification Magic's palette entry (the wiki-magic layer picks its rune-ring style from exactly this colour). */
    static final int ORANGE_RGB = SpadeRoster.MORRIS_COLOR, PALE_RGB = SpadeRoster.MORRIS_ACCENT, WARN_RGB = 0xFF4A4A, MODIFICATION_ARGB = 0xFFFF9B58;
    static final int VIOLET_RGB = SpadeRoster.LUCIFERO_COLOR, VIOLET_ACCENT_RGB = SpadeRoster.LUCIFERO_ACCENT;

    static final double NEEDLE_SPEED = 1.4, NEEDLE_REACH = 0.55, TENDRIL_REACH = 18, TENDRIL_HALF = 1.3, SHOCK_RADIUS = 4.6, PIN_REACH = 16;
    static final int NEEDLE_LIFE = 26;
    static final double PIN_COS = Math.cos(Math.toRadians(38));

    // ================================================================ Dissection Needles
    /** One needle in flight: a point on a clipped ray that hurts the first foe it touches. */
    private static final class Needle {
        Vec3 pos;
        final Vec3 vel;
        int life = NEEDLE_LIFE;
        boolean done;

        Needle(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.vel = vel;
        }

        void step(ServerLevel sl, ZogratisBoss boss, float damage, VolleyBudget budget) {
            if (done) return;
            if (!boss.isAlive() || boss.isRemoved()) { done = true; return; }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            AABB sweep = new AABB(pos, next).inflate(NEEDLE_REACH);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (budget.tryHit(e.getId()) && boss.strike(e, (float) (damage * SpadeFx.magicGuard(e)), true)) {
                    e.knockback(0.3, -vel.x, -vel.z);
                    e.hurtMarked = true;
                }
                spark(sl, next);
                done = true;
                return;
            }
            Vec3 tail = pos.subtract(vel.normalize().scale(0.5));
            sl.sendParticles(SpadeFx.dust(ORANGE_RGB, 1.0f), next.x, next.y, next.z, 2, 0.04, 0.04, 0.04, 0.0);
            sl.sendParticles(SpadeFx.dust(PALE_RGB, 0.8f), tail.x, tail.y, tail.z, 1, 0.03, 0.03, 0.03, 0.0);
            pos = next;
            if (blocked || --life <= 0) {
                spark(sl, pos);
                done = true;
            }
        }

        private static void spark(ServerLevel sl, Vec3 at) {
            Tells.burst(sl, at, ORANGE_RGB, 8);
            sl.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.8f, 1.4f);
        }
    }

    /**
     * {@code count} needles thrown at {@code target} in a fan (two ticks apart, each re-aimed as it leaves); each flies 1.4 blocks a tick for 26 ticks, stops at
     * the first wall, and hurts the first foe it touches for {@code damage} as a spell (it ignores armour, a shield does not stop it, anti-magic cuts it to
     * 40%). A foe takes at most {@code maxHitsPerFoe} of one volley.
     */
    static void needles(ZogratisBoss b, LivingEntity target, int count, float damage, int maxHitsPerFoe) {
        if (!(b.level() instanceof ServerLevel sl) || target == null) return;
        int n = Math.max(1, Math.min(GravityMath.MAX_SWORDS, count));
        double[] fan = GravityMath.fan(n);
        VolleyBudget budget = new VolleyBudget(maxHitsPerFoe);
        Vec3[] last = {null};
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            Runnable launch = () -> {
                Vec3 from = b.position().add(0, b.getBbHeight() * 0.75, 0);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : last[0];
                if (aim == null || aim.lengthSqr() < 1e-4) aim = b.getViewVector(1f);
                last[0] = aim;
                Vec3 dir = SpadeFx.rotateY(aim.normalize(), offset);
                Needle needle = new Needle(from.add(dir.scale(0.8)), dir.scale(NEEDLE_SPEED));
                sl.playSound(null, b.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9f, 1.6f);
                SpellRuntime.zone(sl, NEEDLE_LIFE, 1, SpadeFx.safe(WHO, "needle", age -> needle.step(sl, b, damage, budget)));
            };
            if (i == 0) launch.run();
            else b.later(i * 2, SpadeFx.safe(WHO, "needle launch", launch));
        }
    }

    // ================================================================ Structure Shock
    /** A blast of rewritten air all round him: whoever is within {@code radius} takes a spell's hit and is thrown outwards. */
    static void shock(ZogratisBoss b, double radius, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 at = b.position();
        VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_BURST, at.add(0, 0.1, 0), at.add(0, 1.0, 0), MODIFICATION_ARGB, 22, (float) (radius / 2));
        Tells.ring(sl, at, radius, ORANGE_RGB, 1.6f, 60);
        sl.playSound(null, b.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.7f);
        for (LivingEntity e : b.foes(radius + 1.5)) {
            double dx = e.getX() - at.x, dz = e.getZ() - at.z, d = Math.max(1e-3, Math.hypot(dx, dz));
            if (d > radius + e.getBbWidth() / 2) continue;
            if (b.strike(e, (float) (damage * SpadeFx.magicGuard(e)), true)) {
                SpadeFx.grounded(e);
                e.knockback(1.5, -dx / d, -dz / d);
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ Clawed Tendrils
    /** The foes a tendril fan reaches for: the target first, then the nearest others, at most four, within 20 blocks. */
    static List<LivingEntity> tendrilTargets(ZogratisBoss b, LivingEntity target) {
        List<LivingEntity> out = new java.util.ArrayList<>();
        if (target != null && target.isAlive()) out.add(target);
        List<LivingEntity> others = new java.util.ArrayList<>(b.foes(20));
        others.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(b)));
        for (LivingEntity e : others) {
            if (out.size() >= 4) break;
            if (!out.contains(e)) out.add(e);
        }
        return out;
    }

    /**
     * Claws of rewritten matter along the lines locked in the wind-up: the nearest foe on each line within 18 blocks takes a spell's hit and, when the
     * Balance Law allows one hard drag (the shared 12 second lock, asked once for the whole fan and only when a tendril really finds a foe to drag), is
     * dragged towards him. Anti-magic cuts a tendril: 40% of the hit and no drag. A foe is hit by at most one tendril. Returns how many were caught.
     */
    static int tendrils(ZogratisBoss b, List<Vec3> dirs, float damage) {
        if (!(b.level() instanceof ServerLevel sl) || dirs.isEmpty()) return 0;
        SpadeFx.Control law = new SpadeFx.Control(b);
        Set<Integer> hit = new HashSet<>();
        Vec3 from = b.position();
        int caught = 0;
        sl.playSound(null, b.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.2f, 1.4f);
        for (Vec3 dir : dirs) {
            ZogratisVfx.lane(sl, from.add(0, 1, 0), from.add(dir.scale(TENDRIL_REACH)).add(0, 1, 0), 1.4, 16, ORANGE_RGB, false);
            LivingEntity best = null;
            double bestAlong = Double.MAX_VALUE;
            for (LivingEntity e : b.foes(TENDRIL_REACH + 2)) {
                Vec3 rel = SpadeFx.flat(e.position().subtract(from));
                double along = rel.dot(dir), lateral = Math.abs(rel.x * -dir.z + rel.z * dir.x);
                if (along < 0.5 || along > TENDRIL_REACH || lateral > TENDRIL_HALF + e.getBbWidth() / 2 || along >= bestAlong || hit.contains(e.getId())) continue;
                best = e;
                bestAlong = along;
            }
            if (best == null) continue;
            hit.add(best.getId());
            boolean cut = SpadeFx.antiMagic(best);
            if (b.strike(best, (float) (damage * SpadeFx.magicGuard(best)), true)) {
                caught++;
                if (best instanceof net.minecraft.world.entity.player.Player p && cut) SpadeFx.hint(p, "message.nusmp.spade.tendril_cut", "Your anti-magic cuts the tendril.");
                if (!cut && law.granted()) drag(b, best, BalanceLaw.controlTicks(best, 10));
            }
        }
        return caught;
    }

    /** Drags {@code e} towards the boss for {@code ticks} ticks at half a block a tick, stopping within 2.5 blocks of him. */
    private static void drag(ZogratisBoss b, LivingEntity e, int ticks) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        SpellRuntime.zone(sl, Math.max(2, ticks), 1, SpadeFx.safe(WHO, "tendril drag", age -> {
            if (!b.isAlive() || !e.isAlive()) return;
            Vec3 to = SpadeFx.flat(b.position().subtract(e.position()));
            if (to.length() <= 2.5) return;
            SpadeFx.grounded(e);
            Vec3 v = e.getDeltaMovement(), pull = to.normalize().scale(0.5);
            e.setDeltaMovement(pull.x, Math.min(v.y, 0.1), pull.z);
            e.hurtMarked = true;
            e.fallDistance = 0;
        }));
    }

    // ================================================================ Gravity Pin
    /**
     * The gravity he borrows, as a wedge and not a circle: for {@code ticks} ticks everything within 16 blocks of {@code origin} inside 38 degrees of
     * {@code dir} is slowed (up to Slowness IV), driven down, and crushed once a second. The wedge stays where it was cast. Anti-magic halves the grip.
     */
    static void pinDown(ZogratisBoss b, Vec3 origin, Vec3 dir, int ticks, float strength, float tickDamage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        ZogratisVfx.lane(sl, origin.add(0, 0.1, 0), origin.add(dir.scale(PIN_REACH)), 9.0, ticks, VIOLET_ACCENT_RGB, false);
        sl.playSound(null, BlockPos.containing(origin), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.4f);
        SpellRuntime.zone(sl, ticks, GravityMath.STEP, SpadeFx.safe(WHO, "gravity pin", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            double ramp = GravityMath.ramp(age);
            if (age % 8 == 0) for (double side : new double[]{-Math.toRadians(38), Math.toRadians(38)})
                Tells.line(sl, origin.add(0, 0.2, 0), origin.add(SpadeFx.rotateY(dir, side).scale(PIN_REACH)).add(0, 0.2, 0), VIOLET_ACCENT_RGB);
            for (LivingEntity e : b.foesAround(origin.add(0, 3, 0), PIN_REACH + 3)) {
                if (!SpadeFx.inWedge(origin, dir, e, PIN_REACH, PIN_COS) || Math.abs(e.getY() - origin.y) > 5) continue;
                double g = strength * (SpadeFx.antiMagic(e) ? 0.5 : 1.0);
                int amp = GravityMath.slowAmplifier(g, 1.0, ramp);
                if (amp >= 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GravityMath.STEP * 2 + 2, Math.min(3, amp)));
                SpadeFx.grounded(e);
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x * 0.85, e.onGround() ? Math.min(v.y, -0.08) : Math.min(v.y, GravityMath.sinkSpeed(g, 1.0, ramp)), v.z * 0.85);
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (age >= 8 && age % 20 == 8) SpadeFx.crush(b, e, (float) (tickDamage * g * ramp + 0.5f), TensuraDamageTypes.GRAVITY_PRESS);
            }
        }));
    }

    // ================================================================ Dissection Theatre
    /** Which directions beyond the ring have standing room: 12 samples round {@code centre}, a few blocks past the starting radius. */
    static boolean[] openDirections(ZogratisBoss b, Vec3 centre) {
        boolean[] open = new boolean[12];
        for (int i = 0; i < open.length; i++) {
            double a = i * Math.PI * 2 / open.length;
            Vec3 p = centre.add(Math.cos(a) * (CageMath.START + 3), 0, Math.sin(a) * (CageMath.START + 3));
            open[i] = ZogratisBoss.groundOrNull(b.level(), p) != null;
        }
        return open;
    }

    /**
     * The cage of tendrils: a ring that closes from 16 to 5 blocks over {@code closeTicks} ticks, with doors (see {@link CageMath}). Whoever touches the
     * wall from inside is hit (once every 15 ticks) and thrown inwards; whoever stands in a door, or outside, is never touched. When it is shut, everything
     * inside 6.5 blocks of the middle is squeezed.
     */
    static void theatre(ZogratisBoss b, Vec3 centre, double[] doors, int closeTicks, float wall, float squeeze) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Map<Integer, Long> lastWall = new HashMap<>();
        final boolean[] squeezed = {false};
        sl.playSound(null, BlockPos.containing(centre), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2f, 0.6f);
        SpellRuntime.zone(sl, closeTicks + 8, 2, SpadeFx.safe(WHO, "dissection theatre", age -> {
            if (!b.isAlive() || b.isRemoved() || squeezed[0]) return;
            long now = sl.getGameTime();
            double r = CageMath.radius(age, closeTicks);
            SpadeFx.ringWithDoors(sl, centre, r, ORANGE_RGB, doors, (int) Math.min(96, 24 + r * 5));
            if (age % 8 == 0) VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_FIELD, centre, centre.add(0, 1, 0), MODIFICATION_ARGB, 12, (float) (r / 3));
            for (LivingEntity e : b.foesAround(centre.add(0, 3, 0), r + 8)) {
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, dist = Math.hypot(dx, dz);
                if (Math.abs(e.getY() - centre.y) > 6 || dist > r + 0.4) continue;
                if (!CageMath.touches(dist, CageMath.angle(dx, dz), r, doors) || now - lastWall.getOrDefault(e.getId(), -99L) < 15) continue;
                lastWall.put(e.getId(), now);
                if (b.strike(e, (float) (wall * SpadeFx.magicGuard(e)), true)) {
                    Vec3 in = SpadeFx.flatUnit(centre.subtract(e.position()));
                    SpadeFx.grounded(e);
                    e.knockback(0.9, -in.x, -in.z);
                    e.hurtMarked = true;
                    if (e instanceof net.minecraft.world.entity.player.Player p) SpadeFx.hint(p, "message.nusmp.spade.cage_wall", "The tendril wall: leave through a door (a gap in the ring).");
                }
            }
            if (age >= closeTicks) {
                squeezed[0] = true;
                VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_BURST, centre, centre.add(0, 1, 0), MODIFICATION_ARGB, 24, 3f);
                Tells.burst(sl, centre.add(0, 1.0, 0), ORANGE_RGB, 70);
                sl.playSound(null, BlockPos.containing(centre), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.5f, 0.6f);
                for (LivingEntity e : b.foesAround(centre.add(0, 3, 0), CageMath.END + 3)) {
                    if (Math.hypot(e.getX() - centre.x, e.getZ() - centre.z) > CageMath.END + 1.5 || Math.abs(e.getY() - centre.y) > 6) continue;
                    if (b.strike(e, (float) (squeeze * SpadeFx.magicGuard(e)), true)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            }
        }));
    }

    // ================================================================ the thread
    /** The foe with the most existence on the field, within 30 blocks: the specimen he finds "interesting". */
    static LivingEntity mostInteresting(ZogratisBoss b, LivingEntity fallback) {
        LivingEntity best = fallback;
        double top = fallback == null ? -1 : com.newuniverse.nusmp.antimagic.Nullification.maxEP(fallback);
        for (LivingEntity e : b.foes(30)) {
            double ep = com.newuniverse.nusmp.antimagic.Nullification.maxEP(e);
            if (ep > top + 1e-9) { top = ep; best = e; }
        }
        return best;
    }

    /** Draws the thread between the owner and the specimen (a line of dust, a rune ring on the specimen). */
    static void drawThread(ServerLevel sl, Vec3 from, Vec3 to) {
        Tells.line(sl, from, to, ORANGE_RGB);
        Tells.ring(sl, to.subtract(0, 0.9, 0), 1.2, PALE_RGB, 1.1f, 20);
    }
}
