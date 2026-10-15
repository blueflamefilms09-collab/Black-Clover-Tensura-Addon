package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Wave 13: the small shared helpers of the nine blades (what a foe is, banishing and surrendering, picking a target in front). */
final class Wave13Util {
    private Wave13Util() {}

    static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() ? "" : Relics.of(s).map(RelicDef::id).orElse("");
    }

    /** Celestial, fiend, fey, elemental or undead (Tensura's daemons and spirits count). */
    static boolean otherworldly(LivingEntity e) {
        if (e == null || e instanceof Player) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return Wave13Rules.otherworldly(id.getNamespace(), id.getPath(), e.getType().is(EntityTypeTags.UNDEAD));
    }

    /** Creatures that count the wielder as one of theirs: teammates, their pets and summons, villagers and golems. */
    static boolean considersAlly(ServerPlayer p, LivingEntity t) {
        if (t == p) return false;
        if (t.isAlliedTo(p)) return true;
        if (t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())) return true;
        if (t.getPersistentData().hasUUID("nusmp_relic_owner") && p.getUUID().equals(t.getPersistentData().getUUID("nusmp_relic_owner"))) return true;
        return t instanceof AbstractVillager || t instanceof IronGolem;
    }

    /** Allies for the Mountainguard's aura: the wielder, teammates, pets and summons of the wielder. */
    static boolean friend(ServerPlayer p, LivingEntity t) {
        if (t == p) return true;
        if (t instanceof Player o && (o.isSpectator() || !o.isAlive())) return false;
        if (t.isAlliedTo(p)) return true;
        if (t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID())) return true;
        return t.getPersistentData().hasUUID("nusmp_relic_owner") && p.getUUID().equals(t.getPersistentData().getUUID("nusmp_relic_owner"));
    }

    /** The foe in front of the wielder: within the range and the narrow cone of the look direction. */
    static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestDot = 0.86;
        for (LivingEntity t : Fx.foesAround(p, range)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 0.1) continue;
            double dot = to.scale(1.0 / len).dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = t;
            }
        }
        return best;
    }

    /** Otherworldly creatures near a point (not players, at most 24). */
    static List<LivingEntity> otherworldlyNear(ServerPlayer p, double radius) {
        Vec3 c = p.position();
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(c) <= radius * radius && otherworldly(e));
        return found.size() > 24 ? new ArrayList<>(found.subList(0, 24)) : found;
    }

    /** "Banishment": the foe is sent beyond the plane for a few seconds, helpless and blind (a boss-sized foe only staggers). Never kills. */
    static void banish(ServerPlayer p, LivingEntity t, int ticks) {
        boolean big = t.getMaxHealth() > 200.0f;
        int n = big ? Math.min(ticks, 40) : ticks;
        Fx.addEffect(t, "minecraft:slowness", n, big ? 3 : 6);
        Fx.addEffect(t, "minecraft:weakness", n, 3);
        if (!big) {
            Fx.addEffect(t, "minecraft:blindness", n, 0);
            Fx.addEffect(t, "tensura:paralysis", Math.max(20, n / 2), 0);
        }
        if (t instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
        }
        ServerLevel level = p.serverLevel();
        RukhFx.spray(level, RukhFx.Alignment.DARK, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 40, 0.4, 0.6, 0.4, 0.4);
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
    }

    /** A foe that gives up the fight: it loses its target, cowers (Tensura's fear) and is slowed and weakened for a few seconds. Mobs only. */
    static void surrender(LivingEntity t) {
        if (!(t instanceof Mob m) || !m.isAlive()) return;
        Fx.addEffect(m, "tensura:fear", 160, 0);
        Fx.addEffect(m, "minecraft:slowness", 100, 0);
        Fx.addEffect(m, "minecraft:weakness", 100, 0);
        m.setTarget(null);
        m.getNavigation().stop();
        if (m.level() instanceof ServerLevel level) RukhFx.spray(level, RukhFx.Alignment.DARK, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 8, 0.25, 0.2, 0.25, 0.01);
    }
}
