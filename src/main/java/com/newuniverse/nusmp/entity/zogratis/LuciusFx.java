package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Small tools shared by Lucius's moves, his clones and his Paladins: guarded zone steps (a step that throws stops, it never takes the
 * server tick down), the anti-magic checks, foe selection, dust and sparkle telegraphs, the colour riders of the magics the towers
 * answer with, a procedural spell effect, and {@link Shot}, a bounded homing bolt.
 */
final class LuciusFx {
    private LuciusFx() {}

    /** 0xRRGGBB colours: Astaroth's gold, its pale accent, soul blue and white. */
    static final int GOLD = DevilStyle.color(Devil.ASTAROTH), PALE = DevilStyle.accent(Devil.ASTAROTH), SOUL = 0xBFE4FF, WHITE = 0xFFFFFF, WARN = 0xFF5A4A;
    /** Gold as ARGB for the VFX payloads. */
    static final int GOLD_ARGB = 0xFF000000 | GOLD;

    // ---------------------------------------------------------------- guards
    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of Lucius's goes through here: a
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
                LogUtils.getLogger().warn("[nusmp] Lucius's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ---------------------------------------------------------------- anti-magic
    /** A demon sword in hand, or the Anti-Magic Lord. */
    static boolean antiMagicHeld(Player p) {
        return BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_") || AntiMagic.lord(p).isPresent();
    }

    /** Is this blow anti-magic: a demon sword, the Lord's own blows, or a player fighting with their Anti-Magic grimoire out? */
    static boolean antiMagicHit(DamageSource s) {
        if (!(s.getEntity() instanceof Player p)) return false;
        if (BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_")) return true;
        if (AntiMagic.lord(p).isPresent() && s.getDirectEntity() == p) return true;
        return GrimoireSummon.isFloating(p, MagicType.ANTI_MAGIC)
                && GrimoirePages.grimoireOf(p).map(i -> GrimoirePages.magicOf(i) == MagicType.ANTI_MAGIC).orElse(false);
    }

    /** A blow that can cut a Soul Paladin's soul: anti-magic, or a holy, soul, severance or spirit damage type. */
    static boolean soulCutting(DamageSource s) {
        if (antiMagicHit(s)) return true;
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        return path.contains("holy") || path.contains("soul") || path.contains("sever") || path.contains("spirit");
    }

    // ---------------------------------------------------------------- foes
    /** Players in survival and the summons of players: never the animals and villagers standing about, never the Zogratis family. */
    static boolean isFoe(Entity self, Entity e) {
        if (!(e instanceof LivingEntity le) || e == self || !le.isAlive() || le.isSpectator() || ZogratisEntities.isFamily(e) || PaladinTurns.isTurned(e)) return false;
        if (e instanceof Player p) return !p.isCreative();
        return e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    /** The players among 'foes'. */
    static List<ServerPlayer> players(List<LivingEntity> foes) {
        List<ServerPlayer> out = new ArrayList<>();
        for (LivingEntity e : foes) if (e instanceof ServerPlayer p) out.add(p);
        return out;
    }

    /** Shows a line on the action bar of every player within 'r' of 'at'. */
    static void bar(ServerLevel sl, Vec3 at, double r, Component text) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(at) < r * r) p.displayClientMessage(text, true);
    }

    // ---------------------------------------------------------------- geometry
    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** The horizontal unit vector from 'a' towards 'b' (a fallback direction if they are on top of each other). */
    static Vec3 flatDir(Vec3 a, Vec3 b, Vec3 fallback) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) {
            d = fallback.multiply(1, 0, 1);
            if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        }
        return d.normalize();
    }

    /** A spot near 'want' where 'who' fits on solid floor (or a block above or below it), or null: never a spot in mid-air over a pit or the void. */
    static Vec3 standable(Level level, Entity who, Vec3 want) {
        Vec3 g = ZogratisBoss.groundOrNull(level, want);
        if (g == null) return null;
        for (double dy : new double[]{0, 1, -1, 2}) {
            Vec3 c = new Vec3(g.x, g.y + dy, g.z);
            if (level.noCollision(who, who.getBoundingBox().move(c.subtract(who.position())))) return c;
        }
        return null;
    }

    /** Where a straight line from 'a' to 'b' first meets a wall (b itself if it is clear). */
    static Vec3 clipWall(ServerLevel sl, Entity by, Vec3 a, Vec3 b) {
        BlockHitResult hit = sl.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        return hit.getType() == HitResult.Type.MISS ? b : hit.getLocation();
    }

    // ---------------------------------------------------------------- dust and sound
    static DustParticleOptions dustOf(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    static void dust(ServerLevel sl, Vec3 at, int rgb, float size, int count, double spread) {
        sl.sendParticles(dustOf(rgb, size), at.x, at.y, at.z, count, spread, spread * 0.6, spread, 0);
    }

    static void sparkle(ServerLevel sl, Vec3 at, int count, double spread) {
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    static void souls(ServerLevel sl, Vec3 at, int count, double spread) {
        sl.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, count, spread, spread, spread, 0.03);
    }

    /** A beam of dust from 'a' to 'b' with a soul mote every few blocks. */
    static void beam(ServerLevel sl, Vec3 a, Vec3 b, int rgb) {
        Vec3 v = b.subtract(a);
        int n = (int) Math.max(2, Math.min(60, v.length() * 2));
        DustParticleOptions d = dustOf(rgb, 1.1f);
        for (int i = 1; i < n; i++) {
            Vec3 p = a.add(v.scale(i / (double) n));
            sl.sendParticles(d, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
            if (i % 5 == 0) sl.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.01);
        }
    }

    static void sound(ServerLevel sl, Vec3 at, net.minecraft.sounds.SoundEvent ev, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }

    static void chime(ServerLevel sl, Vec3 at, float pitch) { sound(sl, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, pitch); }

    // ---------------------------------------------------------------- spell effects
    /**
     * A procedural two-colour spell effect (BORN_BEAM, BORN_NOVA, BORN_WAVE or BORN_RAIN): the payload colour is the first colour, the high
     * half of the seed the second colour, the low byte a style.
     */
    static void born(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int argbA, int argbB, int ticks, float power, int style) {
        long seed = ((long) argbB << 32) | ((sl.random.nextInt() & 0xFFFFFF00L) | (style & 0xFF));
        VfxSpawn.send(sl, new VfxPayload(shape.ordinal(), from, to, argbA, ticks, power, -1, seed));
    }

    // ---------------------------------------------------------------- riders
    /** What a hit of a magic does besides damage: its colour of the grimoire towers (short, so no magic holds anyone for long). */
    static void rider(MagicType.HitEffect hit, LivingEntity target, Entity from) {
        switch (hit) {
            case BURN -> target.igniteForSeconds(2);
            case FREEZE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            case SHOCK, WEAKEN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case SLOW -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0));
            case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            case BLIND -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            case LEVITATE -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0));
            case PUSH -> target.knockback(0.7, from.getX() - target.getX(), from.getZ() - target.getZ());
            case PULL -> target.knockback(0.5, target.getX() - from.getX(), target.getZ() - from.getZ());
            default -> { }
        }
    }

    // ---------------------------------------------------------------- a homing bolt
    /**
     * A bolt that flies 'speed' blocks a tick for 'life' ticks, bends towards 'homing' by 'turn' (0 = straight), stops at the first wall
     * and hurts the first foe it touches with 'onHit'. It is a point, not an entity: one zone step per tick, ended by its own life.
     */
    static final class Shot {
        private Vec3 pos, vel;
        private int life;
        private final double speed, reach, turn;
        private final Entity owner;
        private final Predicate<LivingEntity> foe;
        private final Consumer<LivingEntity> onHit;
        private final LivingEntity homing;
        private final int rgb;
        private boolean done;

        Shot(Entity owner, Vec3 start, Vec3 dir, double speed, int life, double reach, double turn, LivingEntity homing, int rgb,
             Predicate<LivingEntity> foe, Consumer<LivingEntity> onHit) {
            this.owner = owner;
            this.pos = start;
            this.speed = speed;
            this.vel = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, speed) : dir.normalize().scale(speed);
            this.life = Math.max(1, life);
            this.reach = reach;
            this.turn = Math.max(0, Math.min(1, turn));
            this.homing = homing;
            this.rgb = rgb;
            this.foe = foe;
            this.onHit = onHit;
        }

        /** Starts the bolt: one guarded zone step per tick for its whole life. */
        void launch(ServerLevel sl) {
            SpellRuntime.zone(sl, life + 2, 1, safe("bolt", age -> step(sl)));
        }

        private void step(ServerLevel sl) {
            if (done) return;
            if (owner.isRemoved()) { done = true; return; }
            if (homing != null && homing.isAlive() && turn > 0) {
                Vec3 want = homing.getBoundingBox().getCenter().subtract(pos);
                if (want.lengthSqr() > 1e-4) {
                    vel = vel.scale(1 - turn).add(want.normalize().scale(speed * turn));
                    double l = vel.length();
                    if (l > 1e-4) vel = vel.scale(speed / l);
                }
            }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(reach), foe)) {
                onHit.accept(e);
                burst(sl, next);
                done = true;
                return;
            }
            pos = next;
            sl.sendParticles(dustOf(rgb, 1.0f), pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            if (life % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
            if (blocked || --life <= 0) {
                burst(sl, pos);
                done = true;
            }
        }

        private void burst(ServerLevel sl, Vec3 at) {
            sl.sendParticles(dustOf(rgb, 1.5f), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8f, 1.2f);
        }
    }
}
