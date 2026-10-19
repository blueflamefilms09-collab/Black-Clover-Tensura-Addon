package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 (the Water / Dark / Lightning / Flame rebuild): the one server behaviour all eight summon props share. The props are the BODIES of the spells (the sea dragon, the javelin,
 * the black blade, the crescent, the moon and the hole, the thunderbird, the thunder lance, the flame lion, the phoenix): the damage stays in the spell's own code (SpellRuntime
 * bolts and zones, so the balance law and the safe wrappers apply as before), the prop only has to be where the spell is and face the way it moves.
 * <ul>
 *   <li><b>fly</b> (default): moves by its delta movement every tick, turns to face it (yaw and pitch) and ends with its life.</li>
 *   <li><b>follow</b> ({@link #FOLLOW} in the param): stays in front of its owner, facing where the owner looks (the black blade).</li>
 *   <li><b>bob</b> ({@link #BOB}): a small vertical sway (the phoenix, the moon).</li>
 *   <li>otherwise it stays where it is (the moon and the hole).</li>
 * </ul>
 * The low four bits of the param are the painter's variant ({@link #variant}); bits 8 to 17 are an optional stop tick ({@link #stopAfter}: the prop stands still from that tick on).
 */
public final class WaterPropFlight implements MagicProps.Behavior {
    public static final int FOLLOW = 16, BOB = 32, HOLD = 64;
    public static final WaterPropFlight INSTANCE = new WaterPropFlight();

    private WaterPropFlight() {}

    public static int variant(int param) { return param & 15; }

    /** The param bits that say 'stop moving after n ticks' (n up to 1023), to be OR-ed into a param. */
    public static int stopAfter(int ticks) { return (Math.max(0, Math.min(1023, ticks)) << 8); }

    /** The height of the model's centre above its feet, in blocks (at scale 1): where the spell's point has to be inside the model. */
    public static float centre(PropKind k, int param) {
        int v = variant(param);
        return switch (k) {
            case WATER_1 -> 1.125f;
            case WATER_2 -> 1.0f;
            case DARK_1 -> 1.0f;
            case DARK_2 -> v == 0 ? 1.5f : 1.25f;
            case LIGHTNING_1 -> 1.375f;
            case LIGHTNING_2 -> 1.0f;
            case FLAME_1 -> 0.0f;
            case FLAME_2 -> 1.375f;
            default -> 0f;
        };
    }

    /** Yaw (degrees, the game's convention: 0 = +z) of a horizontal direction. */
    public static float yawOf(Vec3 d) { return (float) (Mth.atan2(-d.x, d.z) * 57.29577951308232); }

    /** Pitch (degrees, positive = looking down) of a direction. */
    public static float pitchOf(Vec3 d) {
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) (-Mth.atan2(d.y, h) * 57.29577951308232);
    }

    /**
     * Spawns a prop whose model centre starts at {@code at}, facing {@code dir}, moving by {@code vel} per tick (zero = it holds). Returns null when the entity could not be made.
     */
    public static MagicPropEntity spawn(ServerLevel sl, PropKind kind, LivingEntity owner, Vec3 at, Vec3 dir, Vec3 vel, int life, float scale, int param) {
        float yaw = dir.lengthSqr() < 1e-8 ? (owner == null ? 0f : owner.getYRot()) : yawOf(dir);
        MagicPropEntity e = MagicProps.spawn(sl, kind, at.subtract(0, centre(kind, param) * scale, 0), yaw, scale, Math.max(4, life), param, owner);
        if (e == null) return null;
        e.setYRot(yaw);
        e.yRotO = yaw;
        float pitch = dir.lengthSqr() < 1e-8 ? 0f : pitchOf(dir);
        e.setXRot(pitch);
        e.xRotO = pitch;
        e.setDeltaMovement(vel);
        return e;
    }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        int p = e.param();
        if ((p & HOLD) != 0) return;
        if ((p & FOLLOW) != 0) {
            LivingEntity o = e.owner();
            if (o == null) { e.expire(); return; }
            Vec3 look = o.getViewVector(1f);
            float s = e.scale();
            Vec3 at = o.getEyePosition().add(look.scale(1.5 + 0.8 * s)).subtract(0, centre(e.kind(), p) * s + 0.25, 0);
            e.setPos(at.x, at.y, at.z);
            e.setYRot(o.getYRot());
            e.setXRot(o.getXRot());
            return;
        }
        int stop = (p >> 8) & 1023;
        if (stop > 0 && e.life() >= stop) return;
        Vec3 v = e.getDeltaMovement();
        if (v.lengthSqr() < 1e-8) return;
        double bob = (p & BOB) != 0 ? Math.sin(e.life() * 0.45) * 0.05 : 0.0;
        e.setPos(e.getX() + v.x, e.getY() + v.y + bob, e.getZ() + v.z);
        e.setYRot(yawOf(v));
        e.setXRot(pitchOf(v));
    }

    /** True for the entity that carries one of the rebuilt props (a spell asking whether its body is still there). */
    public static boolean alive(Entity e) { return e != null && e.isAlive() && !e.isRemoved(); }
}
