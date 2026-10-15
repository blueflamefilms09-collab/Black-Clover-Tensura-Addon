package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
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
 * Lucifero's moves and Body Magic's pulse, as plain static functions on any {@link ZogratisBoss}: Dante uses them, and Lucius
 * (who took Lucifero's gravity for his own) calls the same four public ones. Everything is bounded: a field is a zone that looks at
 * a few dozen foes every four ticks, a rock sword is a point that moves along a clipped ray, a singularity is one zone of thirty
 * ticks, and the only world change is the bite, which goes through the boss's block ledger (griefBlocks rule, per-tick budget,
 * put back when the fight is over).
 * <ul>
 *   <li>{@link #gravityField}: a well that slows (up to Slowness VI at the core), forces foes down and crushes them once a second.
 *       It builds up over 16 ticks and fades towards the rim, so there is always a way out, and it never causes fall damage.</li>
 *   <li>{@link #rockSwordVolley}: compressed rock swords hurled in a fan, each a small hit.</li>
 *   <li>{@link #singularity}: a sphere that appears over a marked zone, drags foes towards it for the last 18 of 30 ticks and then
 *       collapses: heavy damage to everyone in the zone unless they left it, jumped it, raised a shield or hold anti-magic.</li>
 *   <li>{@link #regenerate}: Body Magic's pulse.</li>
 * </ul>
 */
public final class DanteMoves {
    private DanteMoves() {}

    private static final int RGB = DevilStyle.color(Devil.LUCIFERO), ACCENT = DevilStyle.accent(Devil.LUCIFERO);
    /** ARGB for the VFX payloads. */
    static final int VIOLET = 0xFF000000 | ACCENT, BODY = 0xFFFF8A4A;
    private static final String K_SING = "nusmp_dante_sing", K_FIELDS_OFF = "nusmp_dante_fields_off";

    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of Dante's goes through here: a step
     * that throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] Dante's {} stopped: {}", what, t.toString());
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
     * A gravity field of 'radius' blocks at 'centre' for 'ticks' ticks. 'strength' is 0.3 .. 3.5 (1 = heavy, 2 = crushing, 3 = Slowness VI
     * at the core). Foes inside are slowed, forced down and crushed once a second; it is strongest at the core, builds up over 16
     * ticks and ends when the ticks run out or the boss does.
     */
    public static void gravityField(ZogratisBoss boss, Vec3 centre, double radius, int ticks, float strength) {
        if (!(boss.level() instanceof ServerLevel sl) || centre == null) return;
        final double r = GravityMath.clampRadius(radius), s = GravityMath.clampStrength(strength);
        final int life = GravityMath.clampTicks(ticks);
        final Vec3 c = centre;
        final long born = sl.getGameTime();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), VIOLET, life, (float) (r / 1.6));
        ZogratisVfx.well(sl, c, r, life, VIOLET, false);                       // 1.35.0: the field is a well of warped space
        sl.playSound(null, BlockPos.containing(c), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.4f);
        SpellRuntime.zone(sl, life, GravityMath.STEP, safe("gravity field", age -> fieldStep(boss, sl, c, r, s, age, born)));
    }

    /**
     * Ends every gravity field this boss has running right now (fields cast later are not affected). The Singularity calls it when it
     * begins, so nobody has to dodge its spheres while a field is slowing and pinning them.
     */
    public static void releaseFields(ZogratisBoss boss) {
        if (boss.level() instanceof ServerLevel sl) boss.getPersistentData().putLong(K_FIELDS_OFF, sl.getGameTime());
    }

    private static void fieldStep(ZogratisBoss boss, ServerLevel sl, Vec3 c, double r, double s, int age, long born) {
        if (!boss.isAlive() || boss.isRemoved() || boss.getPersistentData().getLong(K_FIELDS_OFF) >= born) return;
        double ramp = GravityMath.ramp(age);
        if (age % 16 == 0) Tells.ring(sl, c, r, RGB, 1.2f, (int) Math.min(72, 12 + r * 4));
        Tells.gather(sl, c.add(0, 2.5, 0), r * 0.55, ACCENT, 10);
        int seen = 0;
        for (LivingEntity e : boss.foesAround(c.add(0, 4, 0), Math.sqrt(r * r + 100) + 1)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (dy < -4 || dy > 10 || dx * dx + dz * dz > r * r) continue;
            if (++seen > GravityMath.MAX_VICTIMS) break;
            double w = GravityMath.weight(Math.sqrt(dx * dx + dz * dz) / r);
            double g = s * guard(e);
            int amp = GravityMath.slowAmplifier(g, w, ramp);
            if (amp >= 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GravityMath.STEP * 2 + 2, amp));
            grounded(e);
            Vec3 v = e.getDeltaMovement();
            double sink = GravityMath.sinkSpeed(g, w, ramp);
            e.setDeltaMovement(v.x * 0.85, e.onGround() ? Math.min(v.y, -0.08) : Math.min(v.y, sink), v.z * 0.85);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (age >= 8 && age % 20 == 8) crush(boss, e, (float) GravityMath.crushBase(g, w, ramp), TensuraDamageTypes.GRAVITY_PRESS);
        }
    }

    /**
     * 'count' compressed rock swords hurled at 'target' in a fan (two ticks apart, each re-aimed as it leaves); each is a point that
     * flies 1.25 blocks a tick for 28 ticks, stops at the first wall and hurts the first foe it touches for 'damage' (a plain blow:
     * a raised shield stops it).
     */
    public static void rockSwordVolley(ZogratisBoss boss, LivingEntity target, int count, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return;
        int n = Math.max(1, Math.min(GravityMath.MAX_SWORDS, count));
        double[] fan = GravityMath.fan(n);
        Vec3[] last = {null};
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            Runnable launch = () -> {
                Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.75, 0);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : last[0];
                if (aim == null || aim.lengthSqr() < 1e-4) aim = boss.getViewVector(1f);
                last[0] = aim;
                Vec3 dir = rotateY(aim.normalize(), offset);
                RockSword sword = new RockSword(from.add(dir.scale(0.8)), dir.scale(GravityMath.SWORD_SPEED));
                sl.playSound(null, boss.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.9f, 0.6f);
                SpellRuntime.zone(sl, GravityMath.SWORD_LIFE, 1, safe("rock sword", age -> sword.step(sl, boss, damage)));
            };
            if (i == 0) launch.run();
            else boss.later(i * GravityMath.SWORD_STAGGER, safe("rock sword", launch));
        }
    }

    /**
     * The Gravity Singularity over one marked spot. A black sphere appears above the ground at 'at'; the zone (radius 6.25 to 8.5
     * from 'power' 0.5 to 2, ringed in dust with a ring that shrinks as the countdown) is drawn from the first tick; for the last 18 of
     * 30 ticks the sphere drags foes in (gently: running away works, and anything flying is brought down to the ground); at tick 30 it collapses. Everyone in the zone takes the
     * collapse (34 base at power 1, through the boss's damage law, which also caps a hit at 40% of a player's health) unless they
     * are outside it, their feet are over 1.1 blocks off the floor (they jumped it), and a raised shield or anti-magic cuts it to
     * 35% or 40%. With the griefBlocks rule on the sphere bites a bowl out of the ground (at most 420 blocks, through the boss's
     * ledger, put back when the fight ends). Several spheres can be cast at once: a foe is only ever hit by one per collapse.
     */
    public static void singularity(ZogratisBoss boss, Vec3 at, float power) {
        if (!(boss.level() instanceof ServerLevel sl) || at == null) return;
        final double p = Math.max(0.5, Math.min(2.0, power));
        final Vec3 c = ZogratisBoss.ground(sl, at);
        final double r = GravityMath.zoneRadius(p);
        final float damage = (float) GravityMath.collapseDamage(p);
        VfxSpawn.send(sl, VfxShape.DARK_BLACK_HOLE, c.add(0, 2.5, 0), c.add(0, 3.5, 0), VIOLET, GravityMath.GATHER + 6, (float) (r * 0.9));
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), VIOLET, GravityMath.GATHER, (float) (r / 1.6));
        ZogratisVfx.well(sl, c, r, GravityMath.GATHER, VIOLET, true);            // 1.35.0: the rim closes as the sphere gathers
        ZogratisVfx.singularity(sl, c.add(0, 2.8, 0), r, GravityMath.GATHER + 6, VIOLET);
        sl.playSound(null, BlockPos.containing(c), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.5f);
        SpellRuntime.zone(sl, GravityMath.GATHER, 1, safe("singularity", age -> singularityStep(boss, sl, c, r, p, damage, age)));
    }

    private static void singularityStep(ZogratisBoss boss, ServerLevel sl, Vec3 c, double r, double p, float damage, int age) {
        if (!boss.isAlive() || boss.isRemoved()) return;
        int left = GravityMath.GATHER - age;
        if (left > 0 && age % 3 == 0) {
            Tells.ring(sl, c, r, RGB, 1.5f, (int) Math.min(56, 16 + r * 4));
            Tells.ring(sl, c, Math.max(0.8, r * left / GravityMath.GATHER), ACCENT, 1.1f, 28);
            Tells.column(sl, c, 5, ACCENT);
        }
        if (left > 0 && left <= GravityMath.PULL && age % 2 == 0) {
            int seen = 0;
            for (LivingEntity e : boss.foesAround(c.add(0, 3, 0), Math.sqrt((r + GravityMath.PULL_REACH) * (r + GravityMath.PULL_REACH) + 144))) {
                double dx = c.x - e.getX(), dz = c.z - e.getZ(), dy = e.getY() - c.y;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + GravityMath.PULL_REACH || dy < -4 || dy > 12) continue;
                if (++seen > GravityMath.MAX_VICTIMS) break;
                double g = guard(e);
                grounded(e);
                Vec3 v = e.getDeltaMovement();
                double pull = d > 1.5 ? 0.12 * Math.max(0.4, g) : 0;
                double sink = floorHeight(sl, e) > 2.4 ? -0.45 : v.y;                 // fliers are brought down; a plain jump is left alone
                e.setDeltaMovement(v.x + dx / Math.max(d, 1e-3) * pull, Math.min(v.y, sink), v.z + dz / Math.max(d, 1e-3) * pull);
                e.hurtMarked = true;
                e.fallDistance = 0;
                int amp = GravityMath.slowAmplifier(1.1 * g, d <= r ? 1.0 : 0.5, 1.0);
                if (amp >= 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, amp));
            }
        }
        if (left <= 0) collapse(boss, sl, c, r, p, damage);
    }

    private static void collapse(ZogratisBoss boss, ServerLevel sl, Vec3 c, double r, double p, float damage) {
        long now = sl.getGameTime();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, c.add(0, 0.05, 0), c.add(0, 1, 0), VIOLET, 22, (float) (r / 1.4));
        ZogratisVfx.gravityImpact(sl, c, r, VIOLET);
        sl.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        Tells.burst(sl, c.add(0, 1.2, 0), ACCENT, 80);
        for (LivingEntity e : boss.foesAround(c.add(0, 4, 0), Math.sqrt(r * r + 100) + 1)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (dy < -4 || dy > 10 || dx * dx + dz * dz > r * r) continue;
            if (e.getPersistentData().getLong(K_SING) > now) continue;
            e.getPersistentData().putLong(K_SING, now + 10);
            if (GravityMath.jumpsCollapse(floorHeight(sl, e))) {
                if (e instanceof ServerPlayer sp) sp.displayClientMessage(net.minecraft.network.chat.Component.literal("You leapt over the collapse."), true);
                continue;
            }
            crush(boss, e, (float) (damage * guard01(e)), TensuraDamageTypes.GRAVITY_EXPLODE);
            grounded(e);
            e.knockback(1.2, c.x - e.getX(), c.z - e.getZ());
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));      // short, so a second set of spheres can always be outrun
            e.fallDistance = 0;
        }
        bite(boss, sl, c, p);
    }

    /** The sphere eats a bowl out of the ground: queued on the boss (griefBlocks rule, ledger, per-tick budget), the centre first. */
    static int bite(ZogratisBoss boss, ServerLevel sl, Vec3 c, double power) {
        if (!boss.blocksMayChange()) return 0;
        int r = GravityMath.craterRadius(power), depth = GravityMath.craterDepth(r), queued = 0;
        BlockPos base = BlockPos.containing(c);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int[] o : GravityMath.crater(r, depth)) {
            if (queued >= GravityMath.CRATER_CAP) break;
            BlockPos p = base.offset(o[0], o[1], o[2]);
            if (!sl.isLoaded(p)) continue;
            BlockState st = sl.getBlockState(p);
            if (st.isAir() || !st.getFluidState().isEmpty() || st.getBlock() instanceof FallingBlock) continue;
            if (sl.getBlockState(p.above()).getBlock() instanceof FallingBlock) continue;      // never drop a column of sand into the hole
            if (!boss.enqueueBlockChange(p, air)) break;                                       // the queue is full
            queued++;
        }
        return queued;
    }

    /** Body Magic's pulse: restores 'fraction' (at most half) of the boss's max health at once, with the burst of flesh and steam. */
    public static void regenerate(ZogratisBoss boss, float fraction) {
        if (!boss.isAlive() || !com.newuniverse.nusmp.boss.rules.BossHealGuard.mayHeal(boss)) return;       // 1.37.0: no healing pulse in a fight
        float f = Math.max(0f, Math.min(0.5f, fraction));
        boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth() + boss.getMaxHealth() * f));
        if (boss.level() instanceof ServerLevel sl) {
            Vec3 at = boss.position();
            VfxSpawn.send(sl, VfxShape.BODY_FX3, at.add(0, 0.9, 0), at.add(0, 1.9, 0), BODY, 28, 1.4f);
            sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5f, 0.7f);
        }
    }

    // ================================================================ Dante's own moves
    /**
     * Titan Fist: a heavy blow on everything within 'reach' in front of the boss (a half-circle), with the knockback of a man who
     * lifts mountains.
     */
    public static void titanFist(ZogratisBoss boss, double reach, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        Vec3 look = boss.getViewVector(1f).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
        final Vec3 facing = look.normalize();
        Vec3 fist = boss.position().add(facing.scale(reach * 0.6));
        VfxSpawn.send(sl, VfxShape.BODY_FX3, fist.add(0, 1.2, 0), fist.add(0, 1.2, 0).add(facing), BODY, 20, 1.1f);
        ZogratisVfx.gravityImpact(sl, ZogratisBoss.ground(sl, fist), 3.2, BODY);
        sl.playSound(null, boss.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.6f, 0.5f);
        for (LivingEntity e : boss.foes(reach + 1.5)) {
            Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach + e.getBbWidth() / 2 || (d > 1.2 && to.normalize().dot(facing) < 0.1)) continue;
            if (boss.strike(e, damage, false)) {
                Vec3 away = d < 1e-3 ? facing : to.normalize();
                grounded(e);
                e.knockback(1.6, -away.x, -away.z);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.35, 0));
                e.hurtMarked = true;
            }
        }
    }

    /**
     * A low ring wave from 'centre' out to 'reach': it travels 1.1 blocks a tick and hurts each foe it passes once, unless they have
     * jumped (feet 0.75 blocks off the floor) or a shield takes the blow. Knocks them outwards.
     */
    public static void stompWave(ZogratisBoss boss, Vec3 centre, double reach, float damage, double knock) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        final Set<Integer> hit = new HashSet<>();
        final int ticks = GravityMath.waveTicks(reach);
        VfxSpawn.send(sl, VfxShape.EARTH_RISE, centre, centre.add(0, 1, 0), 0xFFFFB45A, 24, (float) Math.max(1.0, reach / 4));
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, centre.add(0, 0.05, 0), centre.add(0, 1, 0), VIOLET, 14, (float) (reach / 3));
        ZogratisVfx.gravityImpact(sl, centre, reach, VIOLET);
        sl.playSound(null, BlockPos.containing(centre), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2f, 0.4f);
        SpellRuntime.zone(sl, ticks, 1, safe("stomp wave", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            double wr = GravityMath.waveRadius(age);
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(wr, reach), RGB, 1.5f, (int) Math.min(80, 12 + Math.min(wr, reach) * 6));
            for (LivingEntity e : boss.foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                // the wave runs a little past 'reach' before it ends, but the ring in the tell stops at 'reach': nobody outside it is hit
                if (dist > reach + e.getBbWidth() / 2 || !GravityMath.waveHits(dist, wr) || GravityMath.jumpsWave(floorHeight(sl, e))) continue;
                hit.add(e.getId());
                if (boss.strike(e, damage, false)) {
                    double d = Math.max(1e-3, dist);
                    grounded(e);
                    e.knockback(knock, -dx / d, -dz / d);
                }
            }
        }));
    }

    /**
     * Gravity Pull: for 12 ticks every foe within 'radius' is dragged towards the boss (never closer than two blocks), then a stomp
     * wave of 5.5 blocks slams out of him. Anti-magic loosens the pull. The wind-up shows a line from each foe to him and the ring
     * of the slam.
     */
    public static void pullAndSlam(ZogratisBoss boss, double radius, double strength, float slamDamage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        List<LivingEntity> caught = boss.foes(radius);
        if (caught.size() > 16) caught = caught.subList(0, 16);
        final List<LivingEntity> victims = caught;
        Vec3 at = boss.position();
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, at, at.add(0, 1, 0), VIOLET, 28, (float) Math.min(9, radius * 0.35));
        ZogratisVfx.well(sl, at, 8, 28, VIOLET, false);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5f, 0.6f);
        final int pullTicks = 12;
        SpellRuntime.zone(sl, pullTicks, 1, safe("gravity pull", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            if (age % 3 == 0) Tells.ring(sl, boss.position(), 5.5, RGB, 1.4f, 40);
            for (LivingEntity e : victims) {
                if (!e.isAlive()) continue;
                Vec3 to = boss.position().subtract(e.position());
                Vec3 flat = to.multiply(1, 0, 1);
                double d = flat.length();
                if (d < 2.0) continue;
                double v = Math.min(d * 0.5, GravityMath.pullSpeed(d, strength) * guard(e));
                grounded(e);
                Vec3 push = flat.normalize().scale(v);
                e.setDeltaMovement(push.x, e.onGround() && age < 2 ? 0.3 : Math.max(e.getDeltaMovement().y, -0.1), push.z);
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
        }));
        boss.later(pullTicks + 2, safe("gravity slam", () -> stompWave(boss, boss.position(), 5.5, slamDamage, 1.1)));
    }

    /**
     * Stone Spikes: a line of spikes erupts from the boss along 'dir' for 'length' blocks, one block every two ticks; each stretch
     * of the line hurts whoever is still standing on it when the spikes reach it and throws them up. Step off the line and it misses.
     */
    public static void stoneSpikes(ZogratisBoss boss, Vec3 dir, double length, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 flat = dir.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) return;
        final Vec3 d = flat.normalize();
        final Vec3 from = boss.position();
        final double len = Math.max(4, Math.min(30, length));
        Vec3 to = from.add(d.scale(len));
        VfxSpawn.send(sl, VfxShape.STONE_SPIKES, from, to, 0xFFFFFFFF, 44, 1.0f);
        ZogratisVfx.lane(sl, from, to, 3.4, 30, VIOLET, false);
        sl.playSound(null, boss.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
        final Set<Integer> hit = new HashSet<>();
        for (double along = 2; along <= len; along += 2) {
            final double a = along;
            boss.later(GravityMath.spikeDelay(a), safe("stone spikes", () -> {
                Vec3 p = from.add(d.scale(a));
                for (LivingEntity e : boss.foesAround(p, 2.4)) {
                    double lat = Math.abs((e.getX() - p.x) * -d.z + (e.getZ() - p.z) * d.x);
                    if (lat > 1.7 || hit.contains(e.getId()) || e.getY() - p.y > 3 || e.getY() - p.y < -4) continue;
                    hit.add(e.getId());
                    if (boss.strike(e, damage, false)) {
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                        e.hurtMarked = true;
                    }
                }
            }));
        }
    }

    /**
     * Lucifero's Weight: a column of crushing gravity on one spot. Everyone within 'radius' of 'mark' is slammed into the ground and
     * crushed, then dragged at Slowness V for a moment (not frozen: they can still crawl out). Step out of the ring in the wind-up.
     */
    public static void weightColumn(ZogratisBoss boss, Vec3 mark, double radius, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, mark.add(0, 0.05, 0), mark.add(0, 1, 0), VIOLET, 18, (float) (radius / 1.2));
        ZogratisVfx.gravityImpact(sl, mark, radius * 1.6, VIOLET);
        ZogratisVfx.singularity(sl, mark.add(0, 2.2, 0), radius * 1.4, 10, VIOLET);
        sl.playSound(null, BlockPos.containing(mark), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.6f, 0.7f);
        Tells.burst(sl, mark.add(0, 1, 0), ACCENT, 40);
        List<LivingEntity> slammed = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(mark.add(0, 4, 0), Math.sqrt(radius * radius + 100) + 1)) {
            double dx = e.getX() - mark.x, dz = e.getZ() - mark.z, dy = e.getY() - mark.y;
            if (dy < -4 || dy > 10 || dx * dx + dz * dz > radius * radius) continue;
            double g = guard(e);
            grounded(e);
            e.setDeltaMovement(0, -1.2, 0);
            e.hurtMarked = true;
            e.fallDistance = 0;
            crush(boss, e, (float) (damage * guard01(e)), TensuraDamageTypes.GRAVITY_PRESS);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, g < 0.8 ? 2 : 4));
            slammed.add(e);
        }
        cushion(sl, slammed);
    }

    // ================================================================ helpers
    /** How strongly gravity grips this foe: 1, or 0.5 for someone fighting with anti-magic. */
    static double guard(LivingEntity e) { return e instanceof Player p && antiMagicHeld(p) ? 0.5 : 1.0; }

    /**
     * Gravity wins over wings: a foe gliding on an elytra is brought out of the glide before any of Dante's forced pushes, so a fast
     * shove into a wall can never turn into the game's "kinetic energy" damage.
     */
    static void grounded(LivingEntity e) {
        if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
    }

    /**
     * After a forced slam the victims keep a zero fall counter until they land (at most 30 ticks), so the push into the ground is
     * the whole of the move and a slam from the air is never a fall-damage kill on top of the crush.
     */
    static void cushion(ServerLevel sl, List<LivingEntity> victims) {
        if (victims.isEmpty()) return;
        final List<LivingEntity> held = new ArrayList<>(victims);
        SpellRuntime.zone(sl, 30, 1, safe("cushion", age -> {
            held.removeIf(e -> !e.isAlive() || e.isRemoved() || (age > 2 && e.onGround()));
            for (LivingEntity e : held) e.fallDistance = 0;
        }));
    }

    /** The factor on a single crushing blow: a raised shield 0.35, anti-magic 0.4 (the better of the two). */
    static double guard01(LivingEntity e) {
        return GravityMath.mitigation(e.isBlocking(), e instanceof Player p && antiMagicHeld(p));
    }

    /** A demon sword in hand, or the Anti-Magic Lord. */
    public static boolean antiMagicHeld(Player p) {
        return BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_") || AntiMagic.lord(p).isPresent();
    }

    /** Is this blow anti-magic: a demon sword, the Lord's own blows, or a player fighting with their Anti-Magic grimoire out? */
    public static boolean antiMagicHit(DamageSource s) {
        if (!(s.getEntity() instanceof Player p)) return false;
        if (BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_")) return true;
        if (AntiMagic.lord(p).isPresent() && s.getDirectEntity() == p) return true;
        return GrimoireSummon.isFloating(p, MagicType.ANTI_MAGIC)
                && GrimoirePages.grimoireOf(p).map(i -> GrimoirePages.magicOf(i) == MagicType.ANTI_MAGIC).orElse(false);
    }

    /** How far the feet of 'e' are above the floor under them (0 if there is none close by). */
    static double floorHeight(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /**
     * A crushing blow through Tensura's own gravity damage type (so gravity resistance counts), plain magic if the type is missing.
     * The amount is the boss's damage law applied to 'base'.
     */
    static boolean crush(ZogratisBoss boss, LivingEntity victim, float base, ResourceKey<DamageType> type) {
        if (victim == null || !victim.isAlive() || !boss.isFoe(victim)) return false;
        float amount = boss.bossDamage(victim, base);
        if (amount <= 0f) return false;
        DamageSource src = boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, boss))
                .orElseGet(() -> boss.damageSources().indirectMagic(boss, boss));
        return victim.hurt(src, amount);
    }

    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ================================================================ a flying rock sword
    /** One compressed rock sword in flight: a point on a clipped ray, drawn as a blade of chips and violet dust. */
    private static final class RockSword {
        Vec3 pos;
        final Vec3 vel;
        int life = GravityMath.SWORD_LIFE;
        boolean done;

        RockSword(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.vel = vel;
        }

        void step(ServerLevel sl, ZogratisBoss boss, float damage) {
            if (done) return;
            if (!boss.isAlive() || boss.isRemoved()) { done = true; return; }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            AABB sweep = new AABB(pos, next).inflate(GravityMath.SWORD_REACH);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (boss.strike(e, damage, false)) {
                    e.knockback(0.5, -vel.x, -vel.z);
                    e.hurtMarked = true;
                }
                shatter(sl, next);
                done = true;
                return;
            }
            pos = next;
            draw(sl);
            if (blocked || --life <= 0) {
                shatter(sl, pos);
                done = true;
            }
        }

        private void draw(ServerLevel sl) {
            BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
            Vec3 d = vel.normalize();
            Vec3 tail = pos.subtract(d.scale(0.6));
            sl.sendParticles(chip, pos.x, pos.y, pos.z, 2, 0.12, 0.12, 0.12, 0.0);
            sl.sendParticles(chip, tail.x, tail.y, tail.z, 1, 0.08, 0.08, 0.08, 0.0);
            sl.sendParticles(new DustParticleOptions(new Vector3f(DevilStyle.red(ACCENT), DevilStyle.green(ACCENT), DevilStyle.blue(ACCENT)), 0.9f),
                    pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
        }

        private static void shatter(ServerLevel sl, Vec3 at) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.08);
            sl.playSound(null, BlockPos.containing(at), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.8f, 0.8f);
        }
    }
}
