package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Megicula's and Blood Magic's moves as plain static functions on any {@link ZogratisBoss}: Vanica uses all of them, and Lucius
 * (who took Blood and Curse-Warding for his own) calls the four public ones. Everything is bounded: a lance is a point on a clipped
 * ray, a field is a zone that looks at a dozen foes every twelve ticks, the beasts are capped, and the only world change is the rot,
 * which goes through the boss's block ledger (griefBlocks rule, a per-tick budget, at most {@link VanicaMath#ROT_CAP} blocks per
 * field, all put back when the fight is over).
 * <ul>
 *   <li>{@link #bloodLances}: blood shaped into lances and flung from her hands, each a small hit that bleeds.</li>
 *   <li>{@link #bloodBeasts}: Blood Beasts rise round the boss, up to the power's cap.</li>
 *   <li>{@link #curseRot}: a rotting field that spreads from its centre, weakens and withers what it touches and rots the ground,
 *       the leaves, the wood and the grass (reversibly).</li>
 *   <li>{@link #weakeningCurse}: the Curse of Decay (see {@link VanicaCurse}).</li>
 * </ul>
 */
public final class VanicaMoves {
    private VanicaMoves() {}

    private static final int RGB = DevilStyle.color(Devil.MEGICULA), ACCENT = DevilStyle.accent(Devil.MEGICULA);
    /** 0xRRGGBB for the dust. */
    static final int BLOOD = 0xB0121E;
    /** ARGB for the VFX payloads. */
    static final int BLOOD_ARGB = 0xFFE0182C, CURSE_ARGB = 0xFFFF5A4A, THREAD_ARGB = 0xFFB0121E;
    static final double THORN_RADIUS = 1.9;

    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of Vanica's goes through here: a
     * step that throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] Vanica's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ the four moves other bosses reuse
    /**
     * 'count' blood lances flung at 'target' in a fan (two ticks apart, each re-aimed as it leaves): the blood gathers at her hands
     * for a few ticks, then each lance flies 1.6 blocks a tick along a straight line, stops at the first wall and hurts the first foe it
     * touches for 'damage' (magic: armour does not help; a raised shield does not stop it). From 60% power the wound bleeds. Heals her
     * a little when it lands (blood drain, Vanica only).
     */
    public static void bloodLances(ZogratisBoss boss, LivingEntity target, int count, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return;
        final int n = Math.max(1, Math.min(VanicaMath.MAX_LANCES, count));
        final double[] fan = VanicaMath.lanceFan(n);
        final boolean bleed = boss.power() >= 60;
        final Vec3[] last = {null};
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            Runnable shape = () -> {
                Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.75, 0);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : last[0];
                if (aim == null || aim.lengthSqr() < 1e-4) aim = boss.getViewVector(1f);
                last[0] = aim;
                final Vec3 dir = rotateY(aim.normalize(), offset);
                final Vec3 start = from.add(dir.scale(0.8));
                double reach = VanicaMath.LANCE_SPEED * VanicaMath.LANCE_LIFE;
                BlockHitResult wall = sl.clip(new ClipContext(start, start.add(dir.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
                Vec3 stop = wall.getType() == HitResult.Type.MISS ? start.add(dir.scale(reach)) : wall.getLocation();
                final int flight = Math.max(2, (int) Math.ceil(start.distanceTo(stop) / VanicaMath.LANCE_SPEED));
                int total = Math.max(12, (int) Math.round(flight / 0.75));              // the effect spends its first quarter gathering at the hand
                VfxSpawn.send(sl, VfxShape.BLOOD_FX1, from, stop, BLOOD_ARGB, total, 1.0f);
                ZogratisVfx.bloodLance(sl, from, stop, 1.0, Math.max(8, flight + 2), BLOOD_ARGB, false);
                sl.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.9f, 1.4f);
                boss.later(Math.max(1, total - flight), safe("blood lance", () -> {
                    Lance lance = new Lance(start, dir.scale(VanicaMath.LANCE_SPEED));
                    SpellRuntime.zone(sl, flight + 2, 1, safe("blood lance", age -> lance.step(sl, boss, damage, bleed)));
                }));
            };
            if (i == 0) shape.run();
            else boss.later(i * VanicaMath.LANCE_STAGGER, safe("blood lance", shape));
        }
    }

    /**
     * Shapes up to 'count' Blood Beasts in a ring 3 to 6 blocks round the boss (they rise out of puddles for a second before they act).
     * Never more than the boss's power allows alive at once ({@link VanicaMath#beastCap}); the beasts' stats come from the boss's power.
     */
    public static void bloodBeasts(ZogratisBoss boss, int count) { spawnBeasts(boss, count, false); }

    /**
     * A rotting field of 'radius' blocks (3 to 16) at 'centre' for 'ticks' ticks (40 to 400). It spreads from the centre outwards
     * (fast at 100% power), and every twelve ticks Weakness (II at the Gates) and, from 40%, Wither fall on every foe inside the part
     * it has reached; every two seconds a pulse hurts them. With the griefBlocks rule on it rots the ground to coarse dirt, grass and
     * flowers to dead bushes, leaves to nothing and logs to mud roots, at most {@link VanicaMath#ROT_CAP} blocks, through the boss's
     * ledger: all put back when the fight is over.
     */
    public static void curseRot(ZogratisBoss boss, Vec3 centre, double radius, int ticks) {
        if (!(boss.level() instanceof ServerLevel sl) || centre == null) return;
        final double max = Math.max(3, Math.min(16, radius));
        final int life = Math.max(40, Math.min(400, ticks));
        final int spread = VanicaMath.spreadTicks(boss.power());
        final int weak = boss.gatesOpen() ? 1 : 0;
        final boolean wither = boss.power() >= 40;
        final Vec3 c = centre;
        final double[] reached = {-1};
        final int[] rotted = {0};
        VfxSpawn.send(sl, VfxShape.CURSE_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), CURSE_ARGB, life, (float) max);
        ZogratisVfx.bloodSigil(sl, c, max, Math.min(life, 120), CURSE_ARGB, true);          // 1.35.0: the sigil of the rot
        sl.playSound(null, BlockPos.containing(c), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 2f, 0.5f);
        SpellRuntime.zone(sl, life, 4, safe("rot field", age -> {
            if (!boss.isAlive() || boss.isRemoved() || !boss.fightActive()) return;        // the boss fell, was removed, or the fight was abandoned: no more rot, no more weakness
            double r = VanicaMath.rotRadius(age, max, spread);
            if (age % 8 == 0) {
                Tells.ring(sl, c, r, RGB, 1.3f, (int) Math.min(80, 12 + r * 5));
                Tells.gather(sl, c.add(0, 0.5, 0), r * 0.5, ACCENT, 8);
            }
            if (r > reached[0] + 0.25) {
                if (rotted[0] < VanicaMath.ROT_CAP && boss.blocksMayChange()) rotted[0] += rot(boss, sl, c, reached[0], r, VanicaMath.ROT_CAP - rotted[0]);
                reached[0] = r;
            }
            if (age % 12 != 0) return;
            boolean pulse = age > 0 && age % 48 == 0;
            int seen = 0;
            for (LivingEntity e : boss.foesAround(c.add(0, 2, 0), r + 2)) {
                double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
                if (dx * dx + dz * dz > r * r || dy < -5 || dy > 8) continue;
                if (++seen > 12) break;
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, weak));
                if (wither) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                if (pulse) hit(boss, e, VanicaTable.ROT_PULSE, true);
            }
        }));
    }

    /**
     * Megicula's Curse of Decay: a sigil on the ground under 'target' brands it (and any other foe standing in the sigil, at most six)
     * for 'ticks' ticks at 'level' (1 to 3): Weakness, healing cut to 60%, 40% or 25%, and a bite once a second that grows the longer
     * the victim stays in the sigil. It breaks when they leave the sigil, hold anti-magic, are cleansed or it runs out. See {@link VanicaCurse}.
     */
    public static void weakeningCurse(ZogratisBoss boss, LivingEntity target, int ticks, int level) {
        if (target == null || !(boss.level() instanceof ServerLevel sl)) return;
        VanicaCurse.sigil(boss, ZogratisBoss.ground(sl, target.position()), target, ticks, level);
    }

    // ================================================================ Vanica's own moves
    /** The blood scythe: a crescent cut in front of the boss, 150 degrees wide and 5 blocks long. */
    static void scythe(ZogratisBoss boss, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        final Vec3 f = facing(boss);
        double half = VanicaMath.SCYTHE_HALF_ANGLE;
        for (int i = 0; i <= 8; i++) {
            Vec3 d = rotateY(f, -half + i * (2 * half / 8));
            splash(sl, boss.position().add(0, 1.1, 0).add(d.scale(VanicaMath.SCYTHE_REACH * 0.85)), 4, 0.12);
        }
        Vec3 mid = boss.position().add(f.scale(3.0)).add(0, 1.1, 0);
        VfxSpawn.send(sl, VfxShape.BLOOD_FX3, mid, mid.add(f), BLOOD_ARGB, 20, 1.0f);
        ZogratisVfx.crescent(sl, boss.position().add(0, 1.1, 0), boss.position().add(0, 1.1, 0).add(f), VanicaMath.SCYTHE_REACH, 14, BLOOD_ARGB, false);
        sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (LivingEntity e : boss.foes(VanicaMath.SCYTHE_REACH + 1.5)) {
            Vec3 to = e.position().subtract(boss.position());
            if (Math.abs(to.y) > 3.0 || !VanicaMath.inCone(to.x, to.z, f.x, f.z, half, VanicaMath.SCYTHE_REACH, e.getBbWidth() / 2)) continue;
            if (hit(boss, e, damage, false) > 0f) {
                Vec3 away = to.multiply(1, 0, 1);
                Vec3 n = away.lengthSqr() < 1e-4 ? f : away.normalize();
                e.knockback(0.9, -n.x, -n.z);
                if (boss.power() >= 40) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
            }
        }
    }

    /**
     * The blood whip: a lash along the aim, 13 blocks long and 2.6 wide. The first foe on it is hurt and yanked towards the boss (to
     * 2.2 blocks, never faster than 1.5 blocks a tick) and slowed for a second. Walls stop it.
     */
    static void whip(ZogratisBoss boss, LivingEntity aimAt, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.7, 0);
        Vec3 aim = aimAt != null && aimAt.isAlive() ? aimAt.getBoundingBox().getCenter().subtract(from) : boss.getViewVector(1f);
        if (aim.lengthSqr() < 1e-4) aim = boss.getViewVector(1f);
        Vec3 dir = aim.normalize();
        Vec3 far = from.add(dir.scale(VanicaMath.WHIP_LENGTH));
        BlockHitResult wall = sl.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
        LivingEntity first = null;
        double firstAt = 2;
        for (LivingEntity e : boss.foes(VanicaMath.WHIP_LENGTH + 2)) {
            double dist = VanicaMath.distToSegment(e.getX(), e.getZ(), from.x, from.z, end.x, end.z);
            if (dist > VanicaMath.WHIP_WIDTH + e.getBbWidth() / 2 || Math.abs(e.getY() - boss.getY()) > 3.0) continue;
            double t = VanicaMath.alongSegment(e.getX(), e.getZ(), from.x, from.z, end.x, end.z);
            if (t < firstAt) {
                firstAt = t;
                first = e;
            }
        }
        Vec3 tip = first != null ? first.getBoundingBox().getCenter() : end;
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from, tip, THREAD_ARGB, 8, 1.6f);
        ZogratisVfx.bloodArcs(sl, from, tip, 1.0, 12, BLOOD_ARGB);
        Tells.line(sl, from, tip, BLOOD);
        splash(sl, tip, 10, 0.25);
        sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 1.3f);
        if (first == null) return;
        if (hit(boss, first, damage, true) > 0f) {
            Vec3 pull = boss.position().subtract(first.position()).multiply(1, 0, 1);
            double d = pull.length();
            if (d > VanicaMath.WHIP_STOP) {
                Vec3 v = pull.normalize().scale(VanicaMath.yankSpeed(d));            // sized so the victim comes to rest near her, not past her
                first.setDeltaMovement(v.x, 0.25, v.z);
                first.hurtMarked = true;
                first.fallDistance = 0;
            }
            first.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0));
        }
    }

    /**
     * The crimson dash: she vanishes along the line from where she stands to 'end' (as far as she fits and has a floor, at most 22
     * blocks) and reappears there; everyone within 1.8 blocks of the line takes the blow and is thrown aside. The wind-up draws the line.
     */
    static void dash(ZogratisBoss boss, Vec3 end, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || end == null) return;
        final Vec3 from = boss.position();
        Vec3 flat = end.subtract(from).multiply(1, 0, 1);
        double len = Math.min(flat.length(), VanicaMath.DASH_MAX);
        if (len < 1.5) return;
        final Vec3 dir = flat.normalize();
        Vec3 land = from;
        for (double d = 1; d <= len + 1e-6; d += 1) {
            Vec3 cand = ZogratisBoss.ground(sl, new Vec3(from.x + dir.x * d, land.y, from.z + dir.z * d));
            boolean floor = sl.getBlockState(BlockPos.containing(cand.x, cand.y - 0.5, cand.z)).blocksMotion();
            boolean fits = sl.noCollision(boss, boss.getBoundingBox().move(cand.subtract(boss.position())));
            if (!floor || !fits || cand.y - land.y > 1.6 || land.y - cand.y > 3.0) break;             // a wall, no floor, or a drop she would not take
            land = cand;
        }
        if (land.distanceToSqr(from) < 2.25) return;
        Vec3 a = from.add(0, 1, 0), b = land.add(0, 1, 0);
        VfxSpawn.send(sl, VfxShape.BLOOD_FX1, a, b, BLOOD_ARGB, 12, 1.6f);
        ZogratisVfx.bloodArcs(sl, a, b, 1.6, 14, BLOOD_ARGB);
        Tells.line(sl, a, b, BLOOD);
        splash(sl, a, 12, 0.3);
        for (LivingEntity e : boss.foes(len + 4)) {
            if (Math.abs(e.getY() - from.y) > 2.5) continue;
            if (VanicaMath.distToSegment(e.getX(), e.getZ(), from.x, from.z, land.x, land.z) > VanicaMath.DASH_WIDTH + e.getBbWidth() / 2) continue;
            if (hit(boss, e, damage, false) > 0f) {
                double sign = Math.signum((e.getX() - from.x) * -dir.z + (e.getZ() - from.z) * dir.x);
                if (sign == 0) sign = 1;
                e.knockback(1.0, dir.z * sign, -dir.x * sign);
                splash(sl, e.position().add(0, 1, 0), 8, 0.25);
            }
        }
        boss.teleportTo(land.x, land.y, land.z);
        boss.getNavigation().stop();
        splash(sl, b, 12, 0.3);
        sl.playSound(null, BlockPos.containing(land), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4f, 0.5f);
    }

    /** Blood Burst: the blood in everyone near her is pulled out in a ring. Foes within 'radius' take the blow, bleed and are thrown out. */
    static void burst(ZogratisBoss boss, double radius, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 at = boss.position();
        VfxSpawn.send(sl, VfxShape.BLOOD_FX3, at.add(0, 0.1, 0), at.add(0, 1.2, 0), BLOOD_ARGB, 24, (float) Math.max(1.2, radius / 3));
        ZogratisVfx.bloodSigil(sl, at, radius, 26, BLOOD_ARGB, false);
        Tells.ring(sl, at, radius, BLOOD, 1.5f, (int) Math.min(90, 14 + radius * 6));
        splash(sl, at.add(0, 0.8, 0), 30, radius * 0.35);
        sl.playSound(null, boss.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.4f);
        for (LivingEntity e : boss.foes(radius + 2)) {
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (dx * dx + dz * dz > radius * radius || Math.abs(e.getY() - at.y) > 3.5) continue;
            if (hit(boss, e, damage, true) > 0f) {
                double d = Math.max(1e-3, Math.sqrt(dx * dx + dz * dz));
                e.knockback(1.1, -dx / d, -dz / d);
                if (boss.power() >= 40) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
        }
    }

    /**
     * Blood falls on each of 'spots' (one every 'stagger' ticks, in order): everyone within 'radius' blocks of a spot takes 'damage' once
     * in all and is thrown up by 'lift'. Used by the Crimson Rain (the spots were drawn in the wind-up) and by the Crown's thorns.
     */
    static void strikeSpots(ZogratisBoss boss, List<Vec3> spots, double radius, float damage, int stagger, double lift, boolean bleed) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        final Set<Integer> struck = new HashSet<>();
        for (int i = 0; i < spots.size(); i++) {
            final Vec3 s = spots.get(i);
            Runnable fall = () -> {
                VfxSpawn.send(sl, VfxShape.BLOOD_FX3, s.add(0, 0.05, 0), s.add(0, 1, 0), BLOOD_ARGB, 22, (float) Math.max(0.8, radius / 1.6));
                ZogratisVfx.downpour(sl, s, radius, 18, BLOOD_ARGB);
                Tells.line(sl, s.add(0, 10, 0), s.add(0, 0.2, 0), BLOOD);
                splash(sl, s.add(0, 0.4, 0), 18, radius * 0.4);
                sl.playSound(null, BlockPos.containing(s), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4f, 0.5f);
                for (LivingEntity e : boss.foesAround(s, radius + 2)) {
                    if (struck.contains(e.getId())) continue;
                    double dx = e.getX() - s.x, dz = e.getZ() - s.z;
                    if (dx * dx + dz * dz > radius * radius || Math.abs(e.getY() - s.y) > 4) continue;
                    struck.add(e.getId());
                    if (hit(boss, e, damage, true) > 0f) {
                        e.setDeltaMovement(e.getDeltaMovement().add(0, lift, 0));
                        e.hurtMarked = true;
                        if (bleed) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                    }
                }
            };
            if (i * stagger == 0) fall.run();
            else boss.later(i * stagger, safe("blood fall", fall));
        }
    }

    /**
     * Crimson Crown: the ring of blood thorns erupts at 'thorns' (the spots the wind-up drew), a stampede of short-lived Blood Beasts
     * rises between them and a rotting field spreads from the boss. Everything but the thorns' hit is carried by the beasts and the
     * field, and she is exposed afterwards.
     */
    static void crown(ZogratisBoss boss, List<Vec3> thorns) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        int p = boss.power();
        Vec3 c = boss.position();
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), 0xFF000000 | RGB, 50, 3.5f);
        ZogratisVfx.bloodSigil(sl, c, VanicaMath.crownRadius(p), 50, BLOOD_ARGB, false);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5f, 0.7f);
        strikeSpots(boss, thorns, THORN_RADIUS, VanicaTable.THORN, 1, 0.75, true);
        int made = spawnBeasts(boss, VanicaMath.stampedeCount(p), true);
        curseRot(boss, c, Math.min(16, VanicaMath.rotMaxRadius(p) * 1.15), VanicaMath.rotTicks(p));
        if (made > 0) boss.announce("A stampede of blood answers the Crimson Crown: " + made + " beasts rise!");
    }

    // ================================================================ beasts
    /** Shapes beasts round the boss; a stampede takes a ring at 60% of the crown's radius. Returns how many came. */
    static int spawnBeasts(ZogratisBoss boss, int count, boolean stampede) {
        if (!(boss.level() instanceof ServerLevel sl) || !boss.isAlive()) return 0;
        int alive = BloodBeastEntity.countOf(boss);
        int room = stampede ? VanicaMath.stampedeRoom(count, alive) : Math.min(count, VanicaMath.beastCap(boss.power()) - alive);
        if (room <= 0) return 0;
        int made = 0;
        double phase = boss.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < room; i++) {
            double a = phase + i * Math.PI * 2 / room;
            double r = stampede ? VanicaMath.crownRadius(boss.power()) * 0.6 : 3 + boss.getRandom().nextDouble() * 3;
            Vec3 at = ZogratisBoss.ground(sl, boss.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (BloodBeastEntity.spawnFor(boss, sl, at, stampede) != null) made++;
        }
        if (made > 0) sl.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2f, 0.7f);
        return made;
    }

    /** Sends the boss's Blood Beasts away (the fight is over). */
    static void dismissBeasts(ZogratisBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        java.util.UUID me = boss.getUUID();
        for (BloodBeastEntity b : sl.getEntitiesOfClass(BloodBeastEntity.class, boss.getBoundingBox().inflate(110), e -> e.isAlive() && me.equals(e.ownerId()))) {
            Tells.burst(sl, b.position().add(0, 0.6, 0), BLOOD, 14);
            b.discard();
        }
    }

    // ================================================================ the rot
    private static int rot(ZogratisBoss boss, ServerLevel sl, Vec3 c, double from, double to, int budget) {
        BlockPos centre = BlockPos.containing(c);
        int n = 0;
        for (int[] o : VanicaMath.band(from, to)) {
            if (n >= budget) break;
            n += rotColumn(boss, sl, centre.getX() + o[0], centre.getY(), centre.getZ() + o[1], Math.min(2, budget - n));
        }
        return n;
    }

    /** Queues the rot of up to 'most' blocks in one column (from five above the centre to four below it, topmost first). */
    private static int rotColumn(ZogratisBoss boss, ServerLevel sl, int x, int cy, int z, int most) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int made = 0;
        for (int y = cy + 5; y >= cy - 4 && made < most; y--) {
            p.set(x, y, z);
            if (!sl.isLoaded(p)) return made;
            BlockState st = sl.getBlockState(p);
            if (st.isAir()) continue;
            BlockState to = rotOf(sl, p, st);
            if (to == null) continue;
            if (!boss.enqueueBlockChange(p.immutable(), to)) return made;          // the queue is full
            made++;
        }
        return made;
    }

    /**
     * What a block rots into, or null if it does not rot: soil with open air above to coarse dirt, grass, ferns and small flowers to
     * dead bushes, leaves to nothing, logs to mud roots. Block entities and double-height plants are left alone.
     */
    static BlockState rotOf(ServerLevel sl, BlockPos pos, BlockState s) {
        Block b = s.getBlock();
        if (s.hasBlockEntity() || b instanceof DoublePlantBlock) return null;
        if (b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.PODZOL || b == Blocks.MYCELIUM || b == Blocks.ROOTED_DIRT) {
            BlockState up = sl.getBlockState(pos.above());
            return up.isAir() || up.canBeReplaced() || !up.blocksMotion() ? Blocks.COARSE_DIRT.defaultBlockState() : null;
        }
        if (b == Blocks.SHORT_GRASS || b == Blocks.FERN || s.is(BlockTags.SMALL_FLOWERS)) return Blocks.DEAD_BUSH.defaultBlockState();
        if (s.is(BlockTags.LEAVES)) return Blocks.AIR.defaultBlockState();
        if (s.is(BlockTags.LOGS)) return Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState();
        return null;
    }

    // ================================================================ helpers
    /** The boss's facing on the ground plane (never zero). */
    static Vec3 facing(ZogratisBoss boss) {
        Vec3 look = boss.getViewVector(1f).multiply(1, 0, 1);
        return look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    private static final DustParticleOptions DUST_BLOOD = new DustParticleOptions(new Vector3f(0.69f, 0.07f, 0.12f), 1.1f);
    private static final DustParticleOptions DUST_DARK = new DustParticleOptions(new Vector3f(0.55f, 0.04f, 0.1f), 1.0f);
    private static BlockParticleOption chips;

    /** Red chips (redstone block), built once on first use (the block registry is ready by then). */
    static BlockParticleOption chips() {
        if (chips == null) chips = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
        return chips;
    }

    /** A spray of blood: red chips and dark red dust. */
    static void splash(ServerLevel sl, Vec3 at, int count, double spread) {
        sl.sendParticles(chips(), at.x, at.y, at.z, count, spread, spread, spread, 0.08);
        sl.sendParticles(DUST_BLOOD, at.x, at.y, at.z, count / 2 + 1, spread, spread, spread, 0.0);
    }

    /**
     * A hit through the boss's damage law that feeds her: returns the health the victim lost (0 if it did not land). Blood drain heals
     * Vanica herself by a share of that, never more than the per-hit and per-window caps; other bosses that borrow the moves do not heal.
     */
    static float hit(ZogratisBoss boss, LivingEntity victim, float base, boolean magic) {
        float before = victim.getHealth() + victim.getAbsorptionAmount();
        if (!boss.strike(victim, base, magic)) return 0f;
        float dealt = Math.max(0f, before - (victim.getHealth() + victim.getAbsorptionAmount()));
        drain(boss, victim, dealt);
        return dealt;
    }

    /** Blood drain: Vanica heals by a share of 'dealt', limited per hit and per second; a thin line of blood runs back to her. */
    static void drain(ZogratisBoss boss, LivingEntity from, float dealt) {
        if (!(boss instanceof VanicaBoss v) || dealt <= 0f || !boss.isAlive() || boss.getHealth() >= boss.getMaxHealth()) return;
        boolean frenzy = v.frenzied();
        double want = VanicaMath.drainHeal(dealt, boss.getMaxHealth(), boss.power(), frenzy);
        double got = v.drainLimiter().take(boss.level().getGameTime(), want, boss.getMaxHealth(), frenzy);
        if (got < 0.05) return;
        boss.heal((float) got);
        if (boss.level() instanceof ServerLevel sl && boss.tickCount % 2 == 0) Tells.line(sl, from.position().add(0, 1.0, 0), boss.position().add(0, 1.2, 0), BLOOD);
    }

    // ================================================================ a flying blood lance
    /** One blood lance in flight: a point on a clipped ray, drawn as a blade of red chips and dark dust. */
    private static final class Lance {
        Vec3 pos;
        final Vec3 vel;
        boolean done;

        Lance(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.vel = vel;
        }

        void step(ServerLevel sl, ZogratisBoss boss, float damage, boolean bleed) {
            if (done) return;
            if (!boss.isAlive() || boss.isRemoved()) { done = true; return; }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            AABB sweep = new AABB(pos, next).inflate(VanicaMath.LANCE_REACH);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (hit(boss, e, damage, true) > 0f) {
                    if (bleed) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    e.knockback(0.4, -vel.x, -vel.z);
                    e.hurtMarked = true;
                }
                splash(sl, next, 10, 0.25);
                done = true;
                return;
            }
            pos = next;
            Vec3 d = vel.normalize();
            Vec3 tail = pos.subtract(d.scale(0.7));
            splash(sl, pos, 2, 0.08);
            sl.sendParticles(DUST_DARK, tail.x, tail.y, tail.z, 2, 0.05, 0.05, 0.05, 0.0);
            if (blocked) {
                splash(sl, pos, 10, 0.25);
                done = true;
            }
        }
    }

    /** The thorn spots of the Crown, on the ground round 'centre'. */
    static List<Vec3> thornSpots(ServerLevel sl, Vec3 centre, int power, double phase) {
        List<Vec3> out = new ArrayList<>();
        for (double[] t : VanicaMath.thornRing(VanicaMath.thornCount(power), centre.x, centre.z, VanicaMath.crownRadius(power), phase))
            out.add(ZogratisBoss.ground(sl, new Vec3(t[0], centre.y, t[1])));
        return out;
    }
}
