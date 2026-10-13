package com.newuniverse.nusmp.entity.avatar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.mana.ManaLaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The small tools of the Devil Avatar's moves: guarded zone steps, who may be pushed and how hard it grips them, the floor and the roof over
 * a foe, a hit through the boss's damage law as Tensura's gravity damage, the cushion that keeps a slammed foe from a fall's damage, and the
 * dust, motes and sounds of the telegraphs. Server side only; everything is bounded and none of it changes a block.
 */
final class AvatarFx {
    private AvatarFx() {}

    /** The warning red of a mark (0xRRGGBB). */
    static final int WARN = 0xFF4A4A;

    // ---------------------------------------------------------------- guards
    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of the Avatar's goes through here: a
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
                LogUtils.getLogger().warn("[nusmp] The Devil Avatar's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ---------------------------------------------------------------- colours
    static int rgb(ZogratisBoss b) { return DevilStyle.color(b.devil()); }

    static int accent(ZogratisBoss b) { return DevilStyle.accent(b.devil()); }

    /** The accent as ARGB for the VFX payloads. */
    static int argb(ZogratisBoss b) { return 0xFF000000 | DevilStyle.accent(b.devil()); }

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    // ---------------------------------------------------------------- who is pushed, and how hard it grips
    /** Creative players, spectators and the dead are never pushed (the foe list leaves the first two out already; this is the belt to those braces). */
    static boolean pushable(LivingEntity e) {
        return e.isAlive() && !e.isRemoved() && !e.isSpectator() && !(e instanceof Player p && p.isCreative());
    }

    /** Gravity wins over wings: a foe gliding on an elytra is brought out of the glide before any forced push. */
    static void grounded(LivingEntity e) {
        if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
    }

    /** A forced push: the velocity is set and the client told (hurtMarked), exactly as every forced push of the Zogratis fights is. */
    static void push(LivingEntity e, double vx, double vy, double vz) {
        grounded(e);
        e.setDeltaMovement(vx, vy, vz);
        e.hurtMarked = true;
    }

    /** 1, or 0.5 for a foe that holds a Mana Zone or fights with anti-magic: gravity is made of mana, and a Mana Zone keeps half of it out. */
    static double resistOf(LivingEntity e) {
        if (!(e instanceof Player p)) return 1.0;
        boolean zone = false;
        try {
            zone = ManaLaw.zoneActive(p);
        } catch (RuntimeException | LinkageError ignored) { /* no mana data: no zone */ }
        return AvatarMath.resist(zone, DanteMoves.antiMagicHeld(p));
    }

    /** The factor on a crushing blow: a raised shield 0.35, anti-magic 0.4 (the better of the two). */
    static double guard(LivingEntity e) {
        return GravityMath.mitigation(e.isBlocking(), e instanceof Player p && DanteMoves.antiMagicHeld(p));
    }

    // ---------------------------------------------------------------- the floor under a foe and the roof over it
    /** The y of the first solid surface under 'from' within 'maxDrop' blocks, or NaN when there is none. */
    static double floorY(ServerLevel sl, Entity by, Vec3 from, double maxDrop) {
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, -maxDrop, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }

    /** How far the feet of 'e' are above the floor under them (0 on the ground; 64 when there is no floor within sight). */
    static double feetAboveFloor(ServerLevel sl, Entity by, LivingEntity e) {
        if (e.onGround()) return 0;
        double fy = floorY(sl, by, e.position().add(0, 0.5, 0), 64);
        return Double.isNaN(fy) ? 64 : Math.max(0, e.getY() - fy);
    }

    /** The clear air from the top of the hitbox of 'e' to the first solid block over it (a little more than the highest lift when there is none). */
    static double headGap(ServerLevel sl, LivingEntity e) {
        double reach = AvatarMath.LIFT_MAX + 8;
        Vec3 from = new Vec3(e.getX(), e.getBoundingBox().maxY, e.getZ());
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, reach, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.MISS ? reach : Math.max(0, hit.getLocation().y - from.y);
    }

    /** Is there no room for 'e' to rise another half block? */
    static boolean blockedAbove(ServerLevel sl, LivingEntity e) {
        return !sl.noCollision(e, e.getBoundingBox().move(0, 0.6, 0));
    }

    // ---------------------------------------------------------------- damage
    /**
     * A hit through the boss's damage law as Tensura's own damage type 'type' (so a victim's resistance to it counts), plain magic damage if
     * the data pack lacks the type. Returns whether it landed.
     */
    static boolean hurtWith(ZogratisBoss boss, LivingEntity victim, float base, ResourceKey<DamageType> type) {
        if (victim == null || !victim.isAlive() || !boss.isFoe(victim)) return false;
        float amount = boss.bossDamage(victim, base);
        if (amount <= 0f) return false;
        DamageSource src = boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, boss))
                .orElseGet(() -> boss.damageSources().indirectMagic(boss, boss));
        return victim.hurt(src, amount);
    }

    /**
     * After a forced slam the victims keep a zero fall counter until they land (at most 30 ticks), so the push into the floor is the whole of
     * the move and a slam from the air is never a fall-damage kill on top of the crush.
     */
    static void cushion(ServerLevel sl, List<LivingEntity> victims) {
        if (victims.isEmpty()) return;
        final List<LivingEntity> held = new ArrayList<>(victims);
        SpellRuntime.zone(sl, 30, 1, safe("cushion", age -> {
            held.removeIf(e -> !e.isAlive() || e.isRemoved() || (age > 2 && e.onGround()));
            for (LivingEntity e : held) e.fallDistance = 0;
        }));
    }

    // ---------------------------------------------------------------- geometry
    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** The boss's facing on the ground plane (never zero). */
    static Vec3 facing(ZogratisBoss boss) {
        Vec3 look = boss.getViewVector(1f).multiply(1, 0, 1);
        return look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    // ---------------------------------------------------------------- particles and sound
    /** Specks across a disc of 'radius' round 'centre': rising from the floor when 'vy' is positive, falling from several blocks up when it is negative. */
    static void motes(ServerLevel sl, ParticleOptions p, Vec3 centre, double radius, int count, double vy) {
        RandomSource r = sl.getRandom();
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
            double y = centre.y + (vy > 0 ? 0.2 : 5.0 + r.nextDouble() * 4.0);
            sl.sendParticles(p, centre.x + Math.cos(a) * d, y, centre.z + Math.sin(a) * d, 0, 0.0, vy, 0.0, 1.0);
        }
    }

    static void sound(ServerLevel sl, Vec3 at, SoundEvent ev, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }
}
