package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Wave 16: the glue the seventeen charms share: charge pools on the stack (the dawn refill of {@link Wave6Pool}), what the wearer is looking at, who
 * is good and who is evil, magicules and aura given back, and a few particles. Statics only; all server side.
 */
final class Wave16Kit {
    private Wave16Kit() {}

    // ---------------------------------------------------------------- charge pools on the relic stack
    static int charges(ItemStack s, ServerPlayer p, String pool, int max, IntSupplier gain) {
        return Wave6Kit.pool(s, pool, max, Wave6Kit.today(p.serverLevel()), gain).charges();
    }

    /** Spends {@code n} charges of the pool if it has them (the dawn refill is applied first); false and nothing taken otherwise. */
    static boolean spend(ItemStack s, ServerPlayer p, String pool, int max, IntSupplier gain, int n) {
        Wave6Pool.State st = Wave6Kit.pool(s, pool, max, Wave6Kit.today(p.serverLevel()), gain);
        if (!Wave6Pool.canSpend(st, n)) return false;
        Wave6Kit.put(s, pool, Wave6Pool.spend(st, n));
        return true;
    }

    // ---------------------------------------------------------------- looking
    /** The living thing under the crosshair within {@code range} blocks (in line of sight), or null. */
    static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        AABB box = p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestD = range * range;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < bestD && p.hasLineOfSight(e)) { bestD = d; best = e; }
        }
        return best;
    }

    /** The block under the crosshair within {@code range} blocks, or null. */
    static BlockHitResult lookBlock(ServerPlayer p, double range) {
        HitResult h = p.pick(range, 1.0f, false);
        return h instanceof BlockHitResult b && h.getType() == HitResult.Type.BLOCK ? b : null;
    }

    // ---------------------------------------------------------------- good and evil
    /** The wearer, their allies and pets and summons, villagers and golems: the "good creatures" of the Holy Handgrenade. */
    static boolean good(ServerPlayer owner, LivingEntity e) {
        if (e == owner || e.isAlliedTo(owner)) return true;
        if (e instanceof Player) return false;
        if (e instanceof OwnableEntity own && owner.getUUID().equals(own.getOwnerUUID())) return true;
        if (e.getPersistentData().hasUUID(Wave16Tune.OWNER) && owner.getUUID().equals(e.getPersistentData().getUUID(Wave16Tune.OWNER))) return true;
        return e instanceof AbstractVillager || (e instanceof IronGolem g && g.isPlayerCreated());
    }

    /** Hostile monsters, the undead and anything of a daemon's kind; never a player. */
    static boolean evil(ServerPlayer owner, LivingEntity e) {
        if (e instanceof Player || good(owner, e)) return false;
        if (e instanceof Enemy || e.getType().is(EntityTypeTags.UNDEAD)) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        String path = id == null ? "" : id.getPath();
        return path.contains("daemon") || path.contains("demon") || path.contains("devil");
    }

    static boolean undead(LivingEntity e) { return e.getType().is(EntityTypeTags.UNDEAD); }

    /** A creature an innocent hand may not harm without a mark: villagers, player-made golems and pets that have an owner. */
    static boolean innocent(LivingEntity e) {
        return e instanceof AbstractVillager || (e instanceof IronGolem g && g.isPlayerCreated()) || (e instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }

    // ---------------------------------------------------------------- Tensura energy
    /** Gives back a share of the player's maximum magicules and aura (never above the maximum). */
    static void give(ServerPlayer p, double magiculeFraction, double auraFraction) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        if (magiculeFraction > 0) ex.setMagicule(Math.min(EnergyHelper.getMaxMagicule(p), ex.getMagicule() + EnergyHelper.getMaxMagicule(p) * magiculeFraction));
        if (auraFraction > 0) ex.setAura(Math.min(EnergyHelper.getMaxAura(p), ex.getAura() + EnergyHelper.getMaxAura(p) * auraFraction));
        ex.markDirty();
    }

    // ---------------------------------------------------------------- items and attributes
    static Optional<Item> item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
    }

    /** A permanent modifier on a summoned creature (stable id, so it is added once). */
    static void boost(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String name, double amount) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "w16_" + name);
        if (!inst.hasModifier(id)) inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    // ---------------------------------------------------------------- particles
    static void puff(ServerLevel l, Vec3 at, int rgb, int count, double spread) {
        if (l == null || at == null) return;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.3f);
        l.sendParticles(dust, at.x, at.y, at.z, Math.min(count, 48), spread, spread, spread, 0.02);
    }

    /** Alive, loaded entities of a list, capped (the targets of an area ability). */
    static <T extends LivingEntity> List<T> cap(List<T> in, int max) { return in.size() > max ? in.subList(0, max) : in; }
}
