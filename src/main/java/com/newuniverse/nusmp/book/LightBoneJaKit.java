package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the casts of Light Magic ({@link LightJaArts}) and Bone Magic ({@link BoneJaArts}) after the wiki need the same small plumbing: the config switch, props that are spawned
 * pointing along a direction, the particle themes, "the nearest N foes" so that no cast touches an unbounded crowd, the ground under a point, and a guard for casts that finish a few ticks
 * after the key was released. Server only (nothing here reads a client class); every player's state is the player's own entity, nothing is kept in a static field.
 */
public final class LightBoneJaKit {
    private LightBoneJaKit() {}

    /** The particle themes of the two magics (their particles are the custom ones of particle/fx: motes, glints and stars for light, bone chips and cinders for bone). */
    public static final ThemeKey LIGHT_FX = ThemeKey.of("magic.LIGHT"), BONE_FX = ThemeKey.of("magic.BONE");

    /** {@code magic.lightBoneJa}: the remake is on (a server config; before the config is loaded, and in a test, it is on). */
    public static boolean enabled() {
        try {
            return NUConfig.LIGHT_BONE_JA.get();
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    // ================================================================ props
    /** The yaw (degrees) a prop must have to face along (dx, dz). */
    public static float yaw(Vec3 d) { return (float) (Mth.atan2(-d.x, d.z) * (180.0 / Math.PI)); }

    /** The pitch (degrees, positive = looking down) a prop must have to follow the direction. */
    public static float pitch(Vec3 d) {
        Vec3 n = d.lengthSqr() < 1e-9 ? new Vec3(0, 0, 1) : d.normalize();
        return (float) -Math.toDegrees(Math.asin(Mth.clamp(n.y, -1, 1)));
    }

    /**
     * Spawns a prop of the kind with the variant (and data above the low byte) pointing along dir, if the caster and the server are under their prop caps ({@link LightBoneJaBudget}); null when
     * the world refused or the caps said no (every caller works without its model).
     */
    public static MagicPropEntity prop(ServerPlayer p, PropKind kind, int param, Vec3 at, Vec3 dir, float scale, int life) {
        if (!LightBoneJaBudget.admit(p)) return null;
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), kind, at, yaw(dir), scale, life, param, p);
        if (e == null) return null;
        e.setXRot(pitch(dir));
        LightBoneJaBudget.track(p, e);
        return e;
    }

    /**
     * Fires a projectile: the hitbox is a SpellRuntime bolt, the model a still prop at the start that the painter moves along the flight from its age (the server never moves it, so a flying prop
     * costs no packet per tick). The prop ends with the bolt. {@code speedBits} comes from {@link LightBoneJa#speedBits}.
     */
    public static void projectile(ServerPlayer p, PropKind kind, int variant, Vec3 start, Vec3 dir, double speed, double radius, int life, boolean pierce,
                                  SpellRuntime.OnHit hit, SpellRuntime.OnEnd end, float scale) {
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        MagicPropEntity prop = prop(p, kind, variant | LightBoneJa.speedBits(speed), start, d, scale, life + 2);
        SpellRuntime.bolt(p, start, d.scale(speed), radius, life, pierce, null, hit, (bolt, at) -> {
            if (prop != null) prop.expire();
            if (end != null) end.end(bolt, at);
        });
    }

    /** The caster can still finish a cast that was scheduled a few ticks earlier: alive, still in the world and still in the dimension the cast started in. */
    public static boolean here(ServerPlayer p, ServerLevel level) { return p != null && p.isAlive() && !p.isRemoved() && p.serverLevel() == level; }

    /** A delayed part of a cast, timed by the performance probe (sections {@code s.book.light_ja} / {@code s.book.bone_ja}). */
    public static void later(ServerLevel level, int delay, PerfProbe.Section section, Runnable run) {
        SpellRuntime.later(level, delay, () -> {
            long t0 = PerfProbe.begin();
            try {
                run.run();
            } finally {
                PerfProbe.end(section, t0);
            }
        });
    }

    public static final PerfProbe.Section LIGHT_PROBE = PerfProbe.section("s.book.light_ja"), BONE_PROBE = PerfProbe.section("s.book.bone_ja");

    /** The same page (id, tier cost, cooldown, place) with another name, cast and release animation: how a page of the remake replaces the old spell of the same name. */
    public static BookPage recast(BookPage old, String name, BookPage.Cast cast, String anim) {
        return new BookPage(old.id(), name, name, old.costPercent(), old.costFloor(), old.cooldown(), cast, anim != null ? anim : old.anim(), old.weaponTag());
    }

    /** The list with the page of the id swapped by {@link #recast}; the list is copied. */
    public static java.util.List<BookPage> swap(java.util.List<BookPage> pages, String id, String name, BookPage.Cast cast, String anim) {
        java.util.List<BookPage> l = new java.util.ArrayList<>(pages);
        for (int k = 0; k < l.size(); k++) if (l.get(k).id().equals(id)) { l.set(k, recast(l.get(k), name, cast, anim)); break; }
        return l;
    }

    /** A page cast timed by the performance probe. */
    public static BookPage.Cast probed(PerfProbe.Section section, BookPage.Cast cast) {
        return (b, i, p, m) -> {
            long t0 = PerfProbe.begin();
            try {
                return cast.cast(b, i, p, m);
            } finally {
                PerfProbe.end(section, t0);
            }
        };
    }

    // ================================================================ targets
    /** The nearest {@code limit} of the list to the point. */
    public static List<LivingEntity> nearest(List<LivingEntity> all, Vec3 c, int limit) {
        if (all.size() <= limit) return all;
        List<LivingEntity> l = new ArrayList<>(all);
        l.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return new ArrayList<>(l.subList(0, limit));
    }

    /** The point where a ray from {@code from} straight down meets the ground within {@code depth} blocks (the start point itself when it meets nothing). */
    public static Vec3 ground(ServerLevel level, ServerPlayer p, Vec3 from, double depth) {
        var hit = level.clip(new ClipContext(from, from.add(0, -depth, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? from : hit.getLocation();
    }

    /** The end of a ray from {@code from} along {@code dir} for {@code reach} blocks, stopped by a wall. */
    public static Vec3 rayEnd(ServerLevel level, ServerPlayer p, Vec3 from, Vec3 dir, double reach) {
        Vec3 end = from.add(dir.normalize().scale(reach));
        var hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
    }

    public static Vec3 centre(LivingEntity e) { return e.getBoundingBox().getCenter(); }

    /** Sideways (to the caster's right) horizontal unit vector of a look direction. */
    public static Vec3 right(Vec3 look) {
        Vec3 r = new Vec3(-look.z, 0, look.x);
        return r.lengthSqr() < 1e-9 ? new Vec3(1, 0, 0) : r.normalize();
    }

    /** A direction turned by {@code deg} degrees about the vertical axis. */
    public static Vec3 turn(Vec3 d, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(d.x * c + d.z * s, d.y, -d.x * s + d.z * c);
    }

    // ================================================================ particles (the custom ones, through the budgets)
    public static void lightPuff(ServerLevel level, Vec3 at, int count, double spread) { Fx.send(level, LIGHT_FX, at.x, at.y, at.z, count, spread); }

    public static void lightBurst(ServerLevel level, Vec3 at, int count, double spread) { Fx.burst(level, LIGHT_FX, at.x, at.y, at.z, count, spread); }

    public static void bonePuff(ServerLevel level, Vec3 at, int count, double spread) { Fx.send(level, BONE_FX, at.x, at.y, at.z, count, spread); }

    public static void boneBurst(ServerLevel level, Vec3 at, int count, double spread) { Fx.burst(level, BONE_FX, at.x, at.y, at.z, count, spread); }
}
