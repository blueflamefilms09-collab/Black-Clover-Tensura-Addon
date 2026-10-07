package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.BeastSummons;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 0.59 Beast Magic: the server behaviours of this magic's props.
 * <ul>
 *   <li>BEAST_1, the spirit beast: param bits 0..1 = the variant (0 lion-wolf, 1 bear, 2 rhino), bits 2.. = the attack phase in ticks (0 = not
 *       attacking; the painter plays the pounce from it). It follows its owner at a run, picks the nearest hostile within 12 blocks (monsters, things
 *       that target the owner or that the owner fought; never the owner, allies, tamed pets or other players unless they struck the owner), pounces
 *       and bites on the 8th tick of the attack and keeps doing so until its life (20 to 30 s) runs out, then flares away. The hit goes through the
 *       caster's book (BeastSummons.strike: balance law, EP scaling, allies spared). Bosses are not slowed or thrown.</li>
 *   <li>BEAST_2, a claw rake: three glowing gashes hanging in the air where the beast struck (param = the angle in degrees); it only lives 14 ticks.</li>
 * </ul>
 */
public final class BeastProps {
    private BeastProps() {}

    private static final double REACH = 2.5, SIGHT = 12.0, LEASH = 28.0;
    private static final String NEXT_ATTACK = "nusmp_beast_next";

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.BEAST_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                int variant = e.param() & 3;
                float s = e.scale();
                e.setGravity(true);
                e.setSize((variant == 0 ? 0.9f : 1.15f) * s, (variant == 2 ? 1.8f : 1.5f) * s);
            }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) { beast(e, sl); }

            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 at = e.position().add(0, 0.8 * e.scale(), 0);
                VfxSpawn.send(sl, VfxShape.BEAST_FX3, at, at.add(0, 1, 0), 0xFFFF9A2A, 22, 1.1f * e.scale());
                BeastSummons.unlink(e);
            }
        });
        MagicProps.register(PropKind.BEAST_2, (e, sl) -> e.setNoGravity(true));
    }

    // ------------------------------------------------------------------ the spirit beast
    private static void beast(MagicPropEntity e, ServerLevel sl) {
        LivingEntity owner = e.owner();
        if (owner == null) { e.expire(); return; }
        int param = e.param(), variant = param & 3, phase = param >> 2;
        float s = e.scale();
        if (e.life() < 10) return;                                                  // it is still forming (the appear clip)
        LivingEntity t = e.target() instanceof LivingEntity l && l.isAlive() && l.distanceToSqr(owner) < LEASH * LEASH ? l : null;
        if (t == null || e.tickCount % 10 == 0) t = pick(e, owner, sl);
        e.setTarget(t);
        Vec3 goal = t != null ? t.position() : owner.position();
        double dx = goal.x - e.getX(), dz = goal.z - e.getZ(), dist = Math.sqrt(dx * dx + dz * dz);
        double reach = REACH * s + (t != null ? t.getBbWidth() * 0.5 : 0);
        double stop = t != null ? reach * 0.8 : 3.2 + variant * 0.9;
        if (t == null && dist > LEASH) {                                            // left behind: it leaps back to its owner
            Vec3 back = owner.position().add(owner.getViewVector(1f).multiply(-1, 0, -1).scale(1.5));
            VfxSpawn.send(sl, VfxShape.BEAST_FX3, e.position().add(0, 0.6, 0), e.position().add(0, 1.6, 0), 0xFFFF9A2A, 16, 0.7f);
            e.teleportTo(back.x, owner.getY(), back.z);
            return;
        }
        double speed = (variant == 0 ? 0.36 : variant == 1 ? 0.30 : 0.27) * (t == null ? 0.9 : 1.0);
        if (phase > 0) speed *= phase < 8 ? 0.55 : 0.0;                              // it lunges into the bite and stops for the rest of the clip
        double want = dist > stop ? speed : 0;
        Vec3 v = e.getDeltaMovement();
        double ax = dist > 1e-3 ? dx / dist * want : 0, az = dist > 1e-3 ? dz / dist * want : 0;
        double vx = v.x + (ax - v.x) * 0.35, vz = v.z + (az - v.z) * 0.35;
        double vy = e.onGround() ? -0.05 : (v.y - 0.08) * 0.98;
        if (e.horizontalCollision && e.onGround() && want > 0) vy = 0.46;            // hop up a step or a block
        e.setDeltaMovement(vx, vy, vz);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (dist > 0.4) e.setYRot(Mth.rotLerp(0.35f, e.getYRot(), (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI)));

        long now = sl.getGameTime();
        if (phase > 0) {
            phase++;
            if (phase == 8 && t != null) bite(e, owner, t, variant, sl);
            if (phase > 18) {
                phase = 0;
                e.getPersistentData().putLong(NEXT_ATTACK, now + 14);
            }
            e.setParam(variant | (phase << 2));
        } else if (t != null && dist <= reach * 1.15 && Math.abs(t.getY() - e.getY()) < 2.5 * s && now >= e.getPersistentData().getLong(NEXT_ATTACK)) {
            e.setParam(variant | (1 << 2));
        }
    }

    /** The bite: the target and what stands next to it take the hit (EP scaled through the caster's book), are thrown a little and slowed. */
    private static void bite(MagicPropEntity e, LivingEntity owner, LivingEntity first, int variant, ServerLevel sl) {
        float s = e.scale();
        float raw = (variant == 0 ? 7f : variant == 1 ? 8.5f : 10f) * (0.9f + 0.1f * s);
        double spread = 1.6 * s;
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(spread),
                x -> x == first || hostile(owner, x));
        int n = 0;
        for (LivingEntity v : victims) {
            if (n++ >= 4) break;
            if (!BeastSummons.strike(e, v, v == first ? raw : raw * 0.6f)) return;
            if (!BalanceLaw.isBoss(v)) {
                double kx = v.getX() - e.getX(), kz = v.getZ() - e.getZ();
                v.knockback(variant == 2 ? 1.1 : 0.6, -kx, -kz);
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
        }
        Vec3 at = first.position().add(0, first.getBbHeight() * 0.55, 0);
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, at, at.add(0, 1, 0), 0xFFFF9A2A, 14, 0.55f * s);
        float yaw = (float) (Mth.atan2(first.getX() - e.getX(), -(first.getZ() - e.getZ())) * 180.0 / Math.PI);
        MagicPropEntity claw = MagicProps.spawn(sl, PropKind.BEAST_2, at, e.getYRot(), 1.1f * s, 14, Math.round(yaw) & 0x1FF, owner);
        if (claw != null) claw.setNoGravity(true);
    }

    private static LivingEntity pick(MagicPropEntity e, LivingEntity owner, ServerLevel sl) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity x : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(SIGHT), y -> hostile(owner, y))) {
            double d = x.distanceToSqr(e);
            if (d < bd && d <= SIGHT * SIGHT && x.distanceToSqr(owner) <= LEASH * LEASH) { bd = d; best = x; }
        }
        return best;
    }

    /** Fair game for the owner's summon: monsters, things that hunt the owner or that the owner is fighting; never allies, pets or bystander players. */
    private static boolean hostile(LivingEntity owner, LivingEntity x) {
        if (x == owner || !x.isAlive() || x.isSpectator() || x.isAlliedTo(owner)) return false;
        if (x instanceof TamableAnimal ta && ta.isOwnedBy(owner)) return false;
        boolean fought = x == owner.getLastHurtByMob() || x == owner.getLastHurtMob();
        if (x instanceof Player) return x == owner.getLastHurtByMob();
        return x instanceof Enemy || fought || (x instanceof Mob m && m.getTarget() == owner);
    }
}
