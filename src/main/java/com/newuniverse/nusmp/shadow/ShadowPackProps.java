package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Unite Canis (Gimodelo) calls a pack of shadow wolves. The mod already has one generic summon entity (the magic prop) with a server
 * behaviour and a client painter per kind, so the wolves are {@code PropKind.SHADOW_PACK_1}: no new entity type to register, no network code.
 * This is the server behaviour: they follow the caster at a run, pick the nearest hostile thing, and bite it through {@link ShadowDamage}
 * (balance law, Existence Point scaling, allies spared). They live as long as the Canis mode and vanish the moment it ends.
 *
 * <p>Param: bits 0-7 the bite phase (0 = not biting; the painter plays the bite from it), bits 8-11 the wolf's index in the pack.
 */
public final class ShadowPackProps {
    private ShadowPackProps() {}

    private static final double REACH = 1.9, SIGHT = 10.0, LEASH = 24.0;
    private static final int SHADOW = 0xFF3A3A55, BITE_AT = 6, BITE_END = 14, GAP = 12;
    private static final String NEXT = "nusmp_pack_next";

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SHADOW_PACK_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                e.setGravity(true);
                e.setSize(0.7f * e.scale(), 0.85f * e.scale());
            }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) { wolf(e, sl); }

            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 at = e.position().add(0, 0.4, 0);
                VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, 18, 0.6f);
            }
        });
    }

    /** Calls {@code count} wolves round the caster for {@code life} ticks (the old pack is dismissed first). Returns how many appeared. */
    public static int summon(ServerPlayer p, int count, int life) {
        ServerLevel sl = p.serverLevel();
        for (MagicPropEntity old : sl.getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                m -> m.kind() == PropKind.SHADOW_PACK_1 && p.getUUID().equals(m.ownerId()))) old.expire();
        int made = 0;
        for (int k = 0; k < count; k++) {
            double a = k * (Math.PI * 2 / Math.max(1, count)) + p.getYRot() * Math.PI / 180.0;
            Vec3 at = p.position().add(Math.cos(a) * 2.2, 0.1, Math.sin(a) * 2.2);
            MagicPropEntity e = MagicProps.spawn(sl, PropKind.SHADOW_PACK_1, at, (float) Math.toDegrees(a), 1f, life, (k & 15) << 8, p);
            if (e == null) continue;
            VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, 22, 0.8f);
            made++;
        }
        return made;
    }

    /** How many wolves a caster of this Existence Point scale gets: 3, 4 from x3.5, 5 from x6, never above the config. */
    public static int countFor(double epScale, int configMax) {
        int n = epScale >= 6 ? 5 : epScale >= 3.5 ? 4 : 3;
        return Math.max(0, Math.min(n, configMax));
    }

    private static void wolf(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer owner) || !owner.isAlive() || UniteState.active(owner) != UniteKind.CANIS) { e.expire(); return; }
        int param = e.param(), phase = param & 0xFF, idx = (param >> 8) & 15;
        LivingEntity t = e.target() instanceof LivingEntity l && l.isAlive() && l.distanceToSqr(owner) < LEASH * LEASH ? l : null;
        if (t == null || e.tickCount % 10 == 0) t = pick(e, owner, sl);
        e.setTarget(t);
        Vec3 goal = t != null ? t.position() : owner.position().add(Math.cos(idx * 1.3) * 2.4, 0, Math.sin(idx * 1.3) * 2.4);
        double dx = goal.x - e.getX(), dz = goal.z - e.getZ(), dist = Math.sqrt(dx * dx + dz * dz);
        double reach = REACH + (t != null ? t.getBbWidth() * 0.5 : 0);
        if (t == null && (dist > LEASH || e.distanceToSqr(owner) > LEASH * LEASH)) {                  // left behind: it steps out of the owner's shadow
            Vec3 back = owner.position();
            VfxSpawn.send(sl, VfxShape.SHADOW_POOL, e.position(), e.position().add(0, 1, 0), SHADOW, 14, 0.5f);
            e.teleportTo(back.x, back.y, back.z);
            return;
        }
        double speed = t == null ? 0.30 : 0.38;
        if (phase > 0) speed *= phase < BITE_AT ? 0.5 : 0.0;
        double want = dist > (t != null ? reach * 0.8 : 1.6) ? speed : 0;
        Vec3 v = e.getDeltaMovement();
        double ax = dist > 1e-3 ? dx / dist * want : 0, az = dist > 1e-3 ? dz / dist * want : 0;
        double vx = v.x + (ax - v.x) * 0.4, vz = v.z + (az - v.z) * 0.4;
        double vy = e.onGround() ? -0.05 : (v.y - 0.08) * 0.98;
        if (e.horizontalCollision && e.onGround() && want > 0) vy = 0.46;
        e.setDeltaMovement(vx, vy, vz);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (dist > 0.4) e.setYRot(Mth.rotLerp(0.4f, e.getYRot(), (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI)));

        long now = sl.getGameTime();
        if (phase > 0) {
            phase++;
            if (phase == BITE_AT && t != null && e.distanceToSqr(t) < (reach + 0.8) * (reach + 0.8)) bite(e, owner, t, sl);
            if (phase > BITE_END) { phase = 0; e.getPersistentData().putLong(NEXT, now + GAP); }
            e.setParam((idx << 8) | phase);
        } else if (t != null && dist <= reach * 1.15 && Math.abs(t.getY() - e.getY()) < 2.0 && now >= e.getPersistentData().getLong(NEXT)) {
            e.setParam((idx << 8) | 1);
        }
    }

    private static void bite(MagicPropEntity e, ServerPlayer owner, LivingEntity t, ServerLevel sl) {
        ShadowDamage.hit(owner, t, 3.5f);
        if (!BalanceLaw.isBoss(t)) {
            double kx = t.getX() - e.getX(), kz = t.getZ() - e.getZ();
            t.knockback(0.3, -kx, -kz);
        }
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, 10, 0.35f);
    }

    private static LivingEntity pick(MagicPropEntity e, ServerPlayer owner, ServerLevel sl) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity x : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(SIGHT), y -> hostile(owner, y))) {
            double d = x.distanceToSqr(e);
            if (d < bd && x.distanceToSqr(owner) <= LEASH * LEASH) { bd = d; best = x; }
        }
        return best;
    }

    /** Fair game for the pack: monsters, things that hunt the owner or that the owner is fighting; never allies, pets or bystander players. */
    private static boolean hostile(LivingEntity owner, LivingEntity x) {
        if (x == owner || !x.isAlive() || x.isSpectator() || x.isAlliedTo(owner)) return false;
        if (x instanceof TamableAnimal ta && ta.isOwnedBy(owner)) return false;
        boolean fought = x == owner.getLastHurtByMob() || x == owner.getLastHurtMob();
        if (x instanceof Player) return x == owner.getLastHurtByMob();
        return x instanceof Enemy || fought || (x instanceof Mob m && m.getTarget() == owner);
    }
}
