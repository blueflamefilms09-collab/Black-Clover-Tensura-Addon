package com.newuniverse.nusmp.mana.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.mana.zone.ZoneUpRules.OriginMode;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Omnidirectional casting of the Mana Zone (docs/magic/mana_zone_upgrade.md), the SERVER half. All decisions are here:
 * <ol>
 *   <li>{@link #onAim}: the client's aim packet (target point, optional foe id) is validated against the server's own zone record ({@link ZoneUpMath#checkAim}: switch, zone owner,
 *       dimension, finite coordinates, rate limit, target inside the zone) and kept as the caster's ONE pending aim (a reused object per zone owner, no allocation per packet);</li>
 *   <li>{@link #around}: wraps the page handler of {@code GrimoireBook.castPage}. When a fresh aim is pending, the page is eligible and the per-caster cooldown allows, the handler runs
 *       with an ORIGIN OVERRIDE: the caster is posed at the spell's origin (behind the target, at the zone's edge or above it, found with one ray) looking at the target, and the helpers
 *       {@code GrimoireBook.aim / target / along / vfx} are redirected to the target and the origin. Position, look and velocity are restored in a {@code finally}, before anything else
 *       of the server tick sees them, and the client is never told (no packet), so the existing 700 pages become omnidirectional without being rewritten;</li>
 *   <li>a page that moved the caster itself (a dash, a blink) is undone, and pages whose ids say they act on the caster ({@link ZoneUpMath#pageEligible}) are not zone-cast at all.</li>
 * </ol>
 * Cost: a packet is a handful of comparisons; a zone-cast is one ray, one entity lookup and one effect packet. State: one small object per zone owner, dropped when the zone ends
 * (death, logout, dimension change, expiry, collapse) and on server stop. Not covered, said plainly: effects a page schedules for LATER ticks (SpellRuntime.later) read the caster's real
 * pose again.
 */
public final class ZoneCast {
    private ZoneCast() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The one pending aim of a zone owner. */
    private static final class Pending {
        double x, y, z;
        int entityId = -1;
        long at;
    }

    /** What a running zone-cast changed. */
    static final class Ctx {
        ServerPlayer player;
        ServerLevel level;
        Vec3 target, realEye, originEye;
        LivingEntity entity;
        boolean posed;
        Vec3 savedPos, savedDelta;
        float savedYRot, savedXRot, savedHead;
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, long[]> LAST = new HashMap<>();            // [last accepted aim packet, last zone-cast]
    private static Ctx current;

    // ---------------------------------------------------------------- the packet
    /** The server side of the aim packet. Called on the server thread with the sender; everything in it is untrusted. */
    public static void onAim(ServerPlayer p, double x, double y, double z, int entityId) {
        if (p == null || p.level().isClientSide) return;
        onAim(p, x, y, z, entityId, p.level().getGameTime());
    }

    /** The same at a given game time (the self-test runs a zone's life without waiting). */
    static void onAim(ServerPlayer p, double x, double y, double z, int entityId, long now) {
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        ManaZoneManager.Zone zone = ManaZoneManager.zoneOf(p);
        long[] last = LAST.get(p.getUUID());
        double radius = zone == null ? 0.0 : ManaZoneManager.radius(zone, now);
        ZoneUpMath.Aim verdict = ZoneUpMath.checkAim(r.cast(), zone != null, zone != null && zone.dim == p.level().dimension(), x, y, z, p.getX(), p.getY() + 0.9, p.getZ(), radius,
                r.tolerance(), now, last == null ? Long.MIN_VALUE / 4 : last[0], r.aimGap());
        if (verdict != ZoneUpMath.Aim.OK) return;
        int id = ZoneUpMath.entityIdOk(entityId) ? entityId : -1;
        if (id >= 0) {
            Entity e = p.serverLevel().getEntity(id);
            boolean good = e instanceof LivingEntity le && le != p && le.isAlive() && !le.isSpectator()
                    && ManaZoneMath.inside(le.getX() - p.getX(), le.getY() - (p.getY() + 0.9), le.getZ() - p.getZ(), radius + r.tolerance())
                    && le.getBoundingBox().getCenter().distanceToSqr(x, y, z) <= 16.0;
            if (!good) id = -1;
        }
        if (last == null) {
            last = new long[]{Long.MIN_VALUE / 4, Long.MIN_VALUE / 4};
            LAST.put(p.getUUID(), last);
        }
        last[0] = now;
        Pending pd = PENDING.computeIfAbsent(p.getUUID(), k -> new Pending());
        pd.x = x;
        pd.y = y;
        pd.z = z;
        pd.entityId = id;
        pd.at = now;
    }

    // ---------------------------------------------------------------- the cast
    /** Runs a page handler, zone-cast when a fresh aim is pending. The idle path (nobody has an aim) is one map-empty check. */
    public static boolean around(ServerPlayer p, int mode, String pageId, BooleanSupplier body) {
        return aroundAt(p, pageId, p.level().getGameTime(), body);
    }

    static boolean aroundAt(ServerPlayer p, String pageId, long now, BooleanSupplier body) {
        Ctx c = null;
        if (!PENDING.isEmpty()) {
            try {
                c = begin(p, pageId, now);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] zone-cast could not start for page {}: {}", pageId, e.toString());
                restoreQuietly(current);
                current = null;
                c = null;
            }
        }
        if (c == null) return body.getAsBoolean();
        try {
            return body.getAsBoolean();
        } finally {
            end(c);
        }
    }

    private static Ctx begin(ServerPlayer p, String pageId, long now) {
        Pending pd = PENDING.remove(p.getUUID());                        // an aim serves one cast
        if (pd == null) return null;
        ZoneUpRules.Rules r = ManaZoneConfig.upgrade();
        ManaZoneManager.Zone zone = ManaZoneManager.zoneOf(p);
        if (!r.cast() || zone == null || zone.dim != p.level().dimension() || !ZoneUpMath.pageEligible(pageId)) return null;
        long[] last = LAST.get(p.getUUID());
        if (!ZoneUpMath.fresh(now, pd.at, r.aimTtl()) || (last != null && !ZoneUpMath.rateOk(now, last[1], r.castGap()))) return null;
        ServerLevel sl = p.serverLevel();
        double cx = p.getX(), cy = p.getY() + 0.9, cz = p.getZ(), radius = ManaZoneManager.radius(zone, now);
        Vec3 target = new Vec3(pd.x, pd.y, pd.z);
        LivingEntity ent = null;
        if (pd.entityId >= 0 && sl.getEntity(pd.entityId) instanceof LivingEntity le && le.isAlive() && le != p) {
            ent = le;
            target = le.getBoundingBox().getCenter();
        }
        if (!ManaZoneMath.inside(target.x - cx, target.y - cy, target.z - cz, radius + r.tolerance())) return null;
        if (last != null) last[1] = now;
        Ctx c = new Ctx();
        c.player = p;
        c.level = sl;
        c.target = target;
        c.entity = ent;
        c.realEye = p.getEyePosition();
        c.originEye = findOrigin(sl, p, r, cx, cy, cz, radius, c.realEye, target);
        current = c;
        if (c.originEye != null) {
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, c.originEye, c.originEye.add(0, 1, 0), ManaZoneManager.tintOf(p), 10, 0.4f);
            if (r.castRelocate() && !p.isPassenger() && !p.isSleeping() && p.isAlive() && !p.isSpectator()) pose(c, p);
        }
        return c;
    }

    /** The first usable origin of the configured mode, then straight above the target; null when none leaves a run worth moving for (the helpers still redirect to the target). */
    private static Vec3 findOrigin(ServerLevel sl, ServerPlayer p, ZoneUpRules.Rules r, double cx, double cy, double cz, double radius, Vec3 eye, Vec3 target) {
        double[] o = new double[3];
        OriginMode[] tries = r.castMode() == OriginMode.ABOVE ? new OriginMode[]{OriginMode.ABOVE} : new OriginMode[]{r.castMode(), OriginMode.ABOVE};
        for (OriginMode m : tries) {
            ZoneUpMath.origin(m, cx, cy, cz, radius, eye.x, eye.y, eye.z, target.x, target.y, target.z, r.castReach(), o);
            Vec3 want = new Vec3(o[0], o[1], o[2]);
            if (!sl.hasChunkAt(BlockPos.containing(want)) || !sl.hasChunkAt(BlockPos.containing(target))) continue;
            Vec3 dir = want.subtract(target);
            double len = dir.length();
            if (len < 1e-6) continue;
            HitResult hit = sl.clip(new ClipContext(target, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hit.getType() != HitResult.Type.MISS) {
                double run = hit.getLocation().distanceTo(target) - 0.6;               // stop short of the wall
                if (run < r.castMinRun()) continue;
                want = target.add(dir.scale(run / len));
            } else if (len < r.castMinRun()) {
                continue;
            }
            BlockPos at = BlockPos.containing(want);
            if (!sl.getBlockState(at).getCollisionShape(sl, at).isEmpty()) continue;
            return want;
        }
        return null;
    }

    private static void pose(Ctx c, ServerPlayer p) {
        float[] look = new float[2];
        ZoneUpMath.look(c.originEye.x, c.originEye.y, c.originEye.z, c.target.x, c.target.y, c.target.z, look);
        c.savedPos = p.position();
        c.savedDelta = p.getDeltaMovement();
        c.savedYRot = p.getYRot();
        c.savedXRot = p.getXRot();
        c.savedHead = p.getYHeadRot();
        p.setPos(c.originEye.x, c.originEye.y - p.getEyeHeight(), c.originEye.z);
        p.setYRot(look[0]);
        p.setXRot(look[1]);
        p.setYHeadRot(look[0]);
        c.posed = true;
    }

    private static void end(Ctx c) {
        current = null;
        restoreQuietly(c);
    }

    private static void restoreQuietly(Ctx c) {
        if (c == null || !c.posed) return;
        c.posed = false;
        ServerPlayer p = c.player;
        if (p.isRemoved() || p.level() != c.level) return;                    // the page took the caster elsewhere (another dimension, death): nothing to restore
        p.setPos(c.savedPos);
        p.setYRot(c.savedYRot);
        p.setXRot(c.savedXRot);
        p.setYHeadRot(c.savedHead);
        if (p.getDeltaMovement().distanceToSqr(c.savedDelta) > 1.0e-8) p.setDeltaMovement(c.savedDelta);       // a page does not launch the caster from a borrowed place
    }

    // ---------------------------------------------------------------- the helpers of GrimoireBook
    /** The point a page should aim at, or null when this caster is not being zone-cast. */
    public static Vec3 aimOverride(ServerPlayer p) {
        Ctx c = current;
        return c != null && c.player == p ? c.target : null;
    }

    /** The foe the aim named, or null (then the page's own ray from the posed eye decides). */
    public static LivingEntity targetOverride(ServerPlayer p) {
        Ctx c = current;
        return c != null && c.player == p && c.entity != null && c.entity.isAlive() ? c.entity : null;
    }

    /** A start point that lies at the caster's real hand or eye becomes the spell's origin. */
    public static Vec3 fromOverride(ServerPlayer p, Vec3 from) {
        Ctx c = current;
        if (c == null || c.originEye == null || c.player != p || from == null) return from;
        return from.distanceToSqr(c.realEye) <= 12.25 ? c.originEye : from;
    }

    // ---------------------------------------------------------------- lifecycle
    public static void clear(UUID id) {
        PENDING.remove(id);
        LAST.remove(id);
    }

    public static void clearAll() {
        PENDING.clear();
        LAST.clear();
        current = null;
    }

    static int pendingCount() { return PENDING.size(); }

    static int trackedCount() { return LAST.size(); }

    /** For the self-test: makes the aim count as arrived without a packet. */
    static boolean hasPending(UUID id) { return PENDING.containsKey(id); }
}
