package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.93.0 nature-magic rebuild: the helpers the spells of Tree, Vine, Plant, Poison Plant, Mud and Mucus Magic share (the spells themselves are in {@link TreeArts}, {@link VineArts},
 * {@link PlantArts}, {@link PoisonPlantArts}, {@link MudArts} and {@link MucusArts}). Everything is server side: it uses the book's own damage helper (so the EP law, the boss cap and the
 * mastery apply), the shared control gate and the status helpers, and never breaks a block that is not its own (walls are {@link SpellRuntime#tempBlock}).
 * <ul>
 *   <li>the numbers: {@link #k} (the EP and Spirit Dive scale of a cast), {@link #foes} / {@link #friends} (capped crowds),</li>
 *   <li>the statuses: {@link #root} (legs held, shorter on bosses), {@link #wound} (poison and a small cut in the magicule pool), {@link #knock}, {@link #pull}, {@link #lift},</li>
 *   <li>the temporary armour: {@link #armour} sets the player render layer (Aura.TREE ...) AND the passive that goes with it (tags NatureArmourUntil / NatureArmourKind on the skill instance, read by
 *       {@link TreeElementBook#onTakenDamage}); the armour of a cast ends with the aura,</li>
 *   <li>the summons: {@link #bind} / {@link #unbind} / {@link #bound} / {@link #hurt} remember which book, skill instance and page a living prop belongs to so that it can hurt in its caster's name.</li>
 * </ul>
 */
public final class TreeKit {
    private TreeKit() {}

    public static final String ARMOUR_UNTIL = "NatureArmourUntil", ARMOUR_KIND = "NatureArmourKind";

    // ------------------------------------------------------------------ numbers
    /** The size of a cast: Spirit Dive / build-up times the caster's EP scale (0.9 to 1.5). */
    public static float k(ManasSkillInstance i, ServerPlayer p) { return GrimoireBook.size(i, p) * EnergyBridge.scale(p); }

    public static Vec3 forward(ServerPlayer p) {
        double yaw = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** Enemies within r of c (at most 16, so a crowd cannot stall a pulse). */
    public static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> l = GrimoireBook.around(p, c, r);
        return l.size() > 16 ? l.subList(0, 16) : l;
    }

    /** The caster and its friends within r of c (at most 12). */
    public static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    /** The nearest enemy of the owner within r of c, or null. */
    public static LivingEntity nearest(LivingEntity owner, Vec3 c, double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e != owner && !e.isAlliedTo(owner))) {
            double d = e.distanceToSqr(c);
            if (d < bd && d <= r * r) { bd = d; best = e; }
        }
        return best;
    }

    // ------------------------------------------------------------------ statuses
    /** Legs held: Slowness, the sprint stopped, the horizontal speed cut (weaker on bosses). */
    public static void root(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, BalanceLaw.isBoss(t) ? Math.min(amp, 2) : amp));
        t.setSprinting(false);
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0.0), v.z * 0.2);
        t.hurtMarked = true;
    }

    /** Poison from thorns, spores or venom and a small cut in the magicule pool (half as long on bosses). */
    public static void wound(LivingEntity t, int ticks, int amp, double cut) {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, boss ? ticks / 2 : ticks, boss ? 0 : amp));
        if (cut > 0) Nullification.bleed(t, cut);
    }

    /** Throws a target away from c (not bosses beyond a third of the strength). */
    public static void knock(LivingEntity t, Vec3 c, double strength, double up) {
        Vec3 d = t.position().subtract(c).multiply(1, 0, 1);
        d = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        double s = BalanceLaw.isBoss(t) ? strength / 3 : strength;
        t.setDeltaMovement(t.getDeltaMovement().add(d.x * s, BalanceLaw.isBoss(t) ? 0 : up, d.z * s));
        t.hurtMarked = true;
    }

    /** Hauls a target toward c. */
    public static void pull(LivingEntity t, Vec3 c, double strength) {
        Vec3 d = c.subtract(t.position()).multiply(1, 0, 1);
        d = d.lengthSqr() < 1e-4 ? Vec3.ZERO : d.normalize();
        double s = BalanceLaw.isBoss(t) ? strength / 3 : strength;
        t.setDeltaMovement(t.getDeltaMovement().add(d.x * s, 0.1, d.z * s));
        t.hurtMarked = true;
    }

    public static void lift(LivingEntity t, double y) {
        if (BalanceLaw.isBoss(t)) return;
        t.setDeltaMovement(t.getDeltaMovement().add(0, y, 0));
        t.hurtMarked = true;
    }

    /** A self-restoring block, only where nobody stands. */
    public static boolean put(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!level.isLoaded(pos)) return false;
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return false;
        return SpellRuntime.tempBlock(level, pos, state, ticks);
    }

    // ------------------------------------------------------------------ the temporary armour
    /**
     * Arms the passive of the guard page of the nature magics (the kind is the magic's name; see {@link TreeElementBook#onTakenDamage}) for {@code ticks}. The caster also takes the
     * given resistance level for the same time. 1.99.0 (armour policy, {@link ArmourPolicy}): Tree, Vine, Plant, Poison Plant, Mud and Mucus Magic have no armour in the anime, so the
     * guard page no longer grows the render layer (Aura.TREE ... stay registered and on disk); {@code aura} is kept in the signature for the callers.
     */
    public static void armour(ManasSkillInstance i, ServerPlayer p, Aura aura, String kind, int ticks, int resistAmp) {
        var tag = i.getOrCreateTag();
        tag.putLong(ARMOUR_UNTIL, p.level().getGameTime() + ticks);
        tag.putString(ARMOUR_KIND, kind);
        if (resistAmp >= 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resistAmp));
    }

    /**
     * The passive of a running temporary armour: called by the book's onTakenDamage. While the armour lasts the blows are softened by the magic's own factor and whoever strikes is answered in the
     * magic's own way (Tree: bark thorns that root; Vine: thorns that root and prick; Plant: petals that mend the wearer; Poison Plant: venom; Mud: heavy mud that slows the attacker;
     * Mucus: the gel soaks the blow, feeds the wearer and drains the attacker's magicules).
     */
    public static void armourPassive(MagicType magic, ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (owner.level().getGameTime() >= tag.getLong(ARMOUR_UNTIL) || !magic.name().equals(tag.getString(ARMOUR_KIND))) return;
        float hit = amount.get();
        LivingEntity att = source.getEntity() instanceof LivingEntity a && a != owner && !a.isAlliedTo(owner) && !source.is(DamageTypes.THORNS) ? a : null;
        switch (magic) {
            case TREE -> {
                amount.set(hit * TreeRules.softening("TREE"));
                if (att != null) { att.hurt(owner.damageSources().thorns(owner), Math.min(6f, 1.5f + hit * 0.3f)); root(att, 30, 1); }
            }
            case VINE -> {
                amount.set(hit * TreeRules.softening("VINE"));
                if (att != null) { att.hurt(owner.damageSources().thorns(owner), Math.min(4f, 1f + hit * 0.2f)); root(att, 40, 2); wound(att, 60, 0, 0.004); }
            }
            case PLANT -> {
                amount.set(hit * TreeRules.softening("PLANT"));
                BalanceLaw.heal(owner, Math.min(2f, 0.5f + hit * 0.15f));
            }
            case POISON_PLANT -> {
                amount.set(hit * TreeRules.softening("POISON_PLANT"));
                if (att != null) { att.hurt(owner.damageSources().thorns(owner), 1.5f); wound(att, 100, 1, 0.006); }
            }
            case MUD -> {
                amount.set(hit * TreeRules.softening("MUD"));
                if (att != null) att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
            case MUCUS -> {
                amount.set(hit * TreeRules.softening("MUCUS"));
                BalanceLaw.heal(owner, Math.min(4f, hit * 0.2f));
                if (att != null) { EnergyBridge.drain(att, owner, 0.01); att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1)); }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ summons
    /** What a living prop needs to hurt things in its caster's name. */
    private record Ctx(GrimoireBook book, ManasSkillInstance inst, int mode, long until) {}

    private static final Map<UUID, Ctx> CTX = new HashMap<>();

    /** Remembers the page a prop was cast from (call it right after spawning); stale entries of props that never ended are dropped. */
    public static void bind(MagicPropEntity e, GrimoireBook b, ManasSkillInstance i, int mode, long until) {
        if (e == null) return;
        long now = e.level().getGameTime();
        CTX.values().removeIf(c -> c.until() < now);
        CTX.put(e.getUUID(), new Ctx(b, i, mode, until + 40));
    }

    public static boolean bound(MagicPropEntity e) { return CTX.containsKey(e.getUUID()); }

    public static void unbind(MagicPropEntity e) { CTX.remove(e.getUUID()); }

    /** Hurts a target in the name of the prop's caster through the book's damage helper (EP law, boss cap, mastery); false when the prop is not bound or the owner is gone. */
    public static boolean hurt(MagicPropEntity e, LivingEntity target, float raw) {
        Ctx c = CTX.get(e.getUUID());
        if (c == null || !(e.owner() instanceof ServerPlayer p)) return false;
        c.book().hurt(c.inst(), p, target, c.mode(), BalanceLaw.isBoss(target) ? raw * 0.5f : raw);
        return true;
    }

    /** Spawns a prop of the kind (variant in the low byte of the param) and binds it to the page it was cast from; returns it (null when it could not be created). */
    public static MagicPropEntity spawn(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, PropKind kind, Vec3 at, int variant, int life, float scale, float yaw) {
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), kind, at, yaw, scale, life, variant, p);
        if (e != null) bind(e, b, i, mode, p.level().getGameTime() + life);
        return e;
    }

    /** The y of the first non-air block at or under the point within 6 blocks (the point's own y when there is none). */
    public static double groundY(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int d = 0; d < 6; d++) {
            BlockPos q = pos.below(d);
            if (!level.getBlockState(q).isAir()) return q.getY() + 1.0;
        }
        return at.y;
    }

    /** The point with its y put on the ground. */
    public static Vec3 onGround(ServerLevel level, Vec3 at) { return new Vec3(at.x, groundY(level, at.add(0, 1, 0)), at.z); }

    /** The book a bound prop belongs to (for its visual effects), or null. */
    public static GrimoireBook bookOf(MagicPropEntity e) {
        Ctx c = CTX.get(e.getUUID());
        return c == null ? null : c.book();
    }
}
