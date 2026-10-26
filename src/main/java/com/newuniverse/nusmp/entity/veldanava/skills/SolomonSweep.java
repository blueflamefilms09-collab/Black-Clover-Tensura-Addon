package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.skill.SummonerSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Arcane Convergence (draws in what is flying and lingering and turns it into Magicule) and Mana Zone Erasure (deletes what the enemy has put out and stutters the ones who put it) are the same sweep over
 * the same list of things, so there is one sweep with two readers. It looks at: projectiles (at most one box query), the spell bolts of {@link SpellRuntime}, lingering clouds, the fields of
 * {@link JudgmentFields} (a damage field, a sanctuary, a realm, a cage) and, for the erasure, summons. Everything owned by the caster or by an ally of the caster stays. Each kind has its own cap, so one
 * press touches a bounded number of things whatever stands in the radius; the owners of what was removed are collected (at most {@link SolomonRules#ERASURE_MAX_CASTERS}).
 */
final class SolomonSweep {
    private SolomonSweep() {}

    /** What one sweep took. */
    record Result(int projectiles, int clouds, int fields, int summons, List<LivingEntity> casters) {
        int total() { return projectiles + clouds + fields + summons; }
    }

    private static final String RELIC_OWNER = "nusmp_relic_owner";

    /** True when {@code owner} (may be null) is the caster or on the caster's side. */
    private static boolean mine(ServerPlayer p, Entity owner) {
        return owner == null ? false : owner == p || owner instanceof LivingEntity le && Foes.isAlly(p, le);
    }

    /** Collects distinct living casters, nearest to the list's cap. */
    private static void caster(ServerPlayer p, Map<UUID, LivingEntity> out, Entity owner) {
        if (owner instanceof LivingEntity le && !mine(p, le) && le.isAlive() && out.size() < SolomonRules.ERASURE_MAX_CASTERS) out.putIfAbsent(le.getUUID(), le);
    }

    /**
     * Sweeps the radius round the caster. {@code maxProjectiles}, {@code maxClouds} and {@code maxSummons} are the caps (a bolt counts as a projectile); {@code summons} false leaves summons alone.
     */
    static Result sweep(ServerPlayer p, double radius, int maxProjectiles, int maxClouds, int maxSummons, boolean summons) {
        ServerLevel level = p.serverLevel();
        MinecraftServer server = level.getServer();
        Vec3 at = p.position();
        double r2 = radius * radius;
        AABB box = p.getBoundingBox().inflate(radius, Math.min(radius, 64.0), radius);
        Map<UUID, LivingEntity> casters = new LinkedHashMap<>();

        int projectiles = 0;
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box, x -> x.isAlive() && x.distanceToSqr(p) <= r2)) {
            if (projectiles >= maxProjectiles) break;
            Entity owner = pr.getOwner();
            if (mine(p, owner)) continue;
            caster(p, casters, owner);
            pr.discard();
            projectiles++;
        }
        if (projectiles < maxProjectiles) projectiles += SpellRuntime.dissolveBolts(level, at, radius, p, maxProjectiles - projectiles, o -> caster(p, casters, o));

        int clouds = 0;
        for (AreaEffectCloud c : level.getEntitiesOfClass(AreaEffectCloud.class, box, x -> x.isAlive() && x.distanceToSqr(p) <= r2)) {
            if (clouds >= maxClouds) break;
            Entity owner = c.getOwner();
            if (mine(p, owner)) continue;
            caster(p, casters, owner);
            c.discard();
            clouds++;
        }

        int fields = JudgmentFields.clearHostile(level, at, radius, id -> {
            if (id.equals(p.getUUID())) return true;
            ServerPlayer o = server == null ? null : server.getPlayerList().getPlayer(id);
            return o != null && Foes.isAlly(p, o);
        }, SolomonRules.ERASURE_MAX_CASTERS, id -> {
            ServerPlayer o = server == null ? null : server.getPlayerList().getPlayer(id);
            caster(p, casters, o);
        });

        int gone = 0;
        if (summons) {
            for (LivingEntity s : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && x != p && x.distanceToSqr(p) <= r2 && summonOwner(x) != null)) {
                if (gone >= maxSummons) break;
                UUID ownerId = summonOwner(s);
                ServerPlayer o = server == null ? null : server.getPlayerList().getPlayer(ownerId);
                if (ownerId.equals(p.getUUID()) || (o != null && Foes.isAlly(p, o)) || Foes.belongsToSide(p, s)) continue;
                caster(p, casters, o);
                s.discard();
                gone++;
            }
        }
        return new Result(projectiles, clouds, fields, gone, new ArrayList<>(casters.values()));
    }

    /** The owner of a summon of this mod (a card summon or a relic summon), or null for any other creature. */
    static UUID summonOwner(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        if (d.hasUUID(RELIC_OWNER)) return d.getUUID(RELIC_OWNER);
        if (e.getTags().contains(SummonerSkill.SUMMON_TAG) && d.hasUUID(SummonerSkill.OWNER_KEY)) return d.getUUID(SummonerSkill.OWNER_KEY);
        return null;
    }
}
