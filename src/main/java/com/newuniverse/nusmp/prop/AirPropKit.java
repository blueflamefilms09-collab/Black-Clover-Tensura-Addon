package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the 1.93.0 rebuild gives Smoke, Snow, Wing, Vortex, Air and Ash Magic summons that really move and strike (smoke clones, a snow golem, a feather hawk, a
 * vortex core, air blades, ash soldiers ...). All six are one {@link MagicPropEntity} kind each (no new entity types) and share the same small vocabulary, kept here:
 * the synced state word that tells the painter which model and which clip to play, who counts as an enemy for a prop (a hostile mob, or a mob that targets the
 * owner: props never start a player fight, the spells do that through the book's hurt), the nearest-enemy search, balance-law damage with a Tensura damage type
 * and a turn-to-face helper. Pure arithmetic (the word's packing) is unit-tested in {@code AirPropKitTest}.
 *
 * <p>The word in {@code param}: bits 0-7 the variant (which summon / model of the magic), bits 8-9 the state (0 idle, 1 act, 2 act2 / vanish), bits 10-25 the game
 * time (mod 65536) at which the state began, so the painter starts one-shot clips from their first frame.
 */
public final class AirPropKit {
    private AirPropKit() {}

    public static final int IDLE = AirPropWord.IDLE, ACT = AirPropWord.ACT, ACT2 = AirPropWord.ACT2;

    // ------------------------------------------------------------------ the synced word (pure)
    public static int pack(int variant, int state, long gameTime) { return AirPropWord.pack(variant, state, gameTime); }

    public static int variant(int param) { return AirPropWord.variant(param); }

    public static int state(int param) { return AirPropWord.state(param); }

    public static int since(int param, long gameTime) { return AirPropWord.since(param, gameTime); }

    public static void setState(MagicPropEntity e, int st) { e.setParam(pack(variant(e.param()), st, e.level().getGameTime())); }

    // ------------------------------------------------------------------ enemies
    /** A hostile mob, or a mob that is after the owner. Never a player, an ally or the owner. */
    public static boolean hostile(LivingEntity owner, LivingEntity t) {
        if (t == owner || !t.isAlive() || t.isSpectator() || t.isAlliedTo(owner) || t instanceof Player) return false;
        if (t instanceof Enemy) return true;
        return t instanceof Mob m && m.getTarget() == owner;
    }

    public static LivingEntity nearest(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, Math.max(3, range * 0.5), range),
                t -> hostile(owner, t));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : list) {
            double d = t.distanceToSqr(e);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    /** Anything the magic may hit: not the owner, not an ally, alive, not spectating. */
    public static boolean fair(LivingEntity owner, LivingEntity t) { return t != owner && t.isAlive() && !t.isSpectator() && !t.isAlliedTo(owner); }

    // ------------------------------------------------------------------ damage and movement
    /** Balance-law damage from the prop as a Tensura damage type of the owner (the prop is the direct entity). */
    public static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw, ResourceKey<DamageType> type) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, e, owner))
                .orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    /** Turns the prop towards a point, at most {@code maxTurn} degrees a tick. */
    public static void face(MagicPropEntity e, Vec3 to, float maxTurn) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f;
        float cur = e.getYRot();
        e.setYRot(cur + Mth.clamp(Mth.wrapDegrees(want - cur), -maxTurn, maxTurn));
    }

    /** A summon's life in ticks: {@code base}, longer with the owner's EP (at most +50 %). */
    public static int lifeFor(LivingEntity owner, int base) { return (int) (base * Math.min(1.5, 1.0 + (BalanceLaw.epScale(owner) - 1.0) * 0.05)); }

    /** Damage scale of a summon from the owner's EP (1.0 at the base, at most 2.0). */
    public static float powerOf(LivingEntity owner) { return (float) Mth.clamp(BalanceLaw.epScale(owner), 1.0, 2.0); }
}
