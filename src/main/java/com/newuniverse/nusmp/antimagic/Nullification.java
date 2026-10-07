package com.newuniverse.nusmp.antimagic;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * 0.48: anti-magic against Tensura's own magic and skills (the Genesis Demon-Slayer's field, Black Meteorite's Nihility zone).
 * <b>Everything here is temporary for players:</b> skill cooldowns run out, toggles can be switched back on, drained magicules
 * regenerate, and a shattered barrier can be cast again. Nothing touches a player's learned skills, mastery or max EP.
 */
public final class Nullification {
    private Nullification() {}

    static final String TENSURA_MAGIC = "io.github.manasmods.tensura.entity.magic.";

    /**
     * Ultimate interference: each Tensura ULTIMATE skill of 't' is caught with chance 'chance': it is pushed onto a cooldown of at
     * least 'seconds' (in every mode), and if it is toggled on it misfires off with chance 'misfire'. Returns how many were hit.
     */
    public static int interfere(LivingEntity t, int seconds, float chance, float misfire) {
        return jam(t, seconds, chance, misfire, true);
    }

    /**
     * Every active skill of 't' at once (Nihility: no magic at all). Resistances and intrinsic traits are left alone.
     */
    public static int jamAll(LivingEntity t, int seconds, float misfire) {
        return jam(t, seconds, 1f, misfire, false);
    }

    private static int jam(LivingEntity t, int seconds, float chance, float misfire, boolean ultimateOnly) {
        Skills skills;
        try { skills = SkillAPI.getSkillsFrom(t); } catch (Throwable noSkills) { return 0; }
        if (skills == null) return 0;
        int n = 0;
        for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
            if (!(i.getSkill() instanceof Skill s)) continue;
            Skill.SkillType type = s.getType();
            if (ultimateOnly ? type != Skill.SkillType.ULTIMATE
                    : type == Skill.SkillType.RESISTANCE || type == Skill.SkillType.INTRINSIC) continue;
            if (chance < 1f && t.getRandom().nextFloat() >= chance) continue;     // this one slips through
            boolean changed = false;
            int modes = Math.max(1, i.getModes());
            for (int m = 0; m < modes; m++) if (i.getCoolDown(m) < seconds) { i.setCoolDown(seconds, m); changed = true; }
            if (i.isToggled() && t.getRandom().nextFloat() < misfire) {
                i.setToggled(false);
                try { i.onToggleOff(t); } catch (RuntimeException ignored) {}
                changed = true;
            }
            if (changed) { i.markDirty(); n++; }
        }
        if (n > 0) skills.markDirty();
        return n;
    }

    /** EP bleed: takes 'frac' of the target's max magicules from its current pool (it regenerates). Returns the amount. */
    public static double bleed(LivingEntity t, double frac) {
        return com.newuniverse.nusmp.book.EnergyBridge.drain(t, null, frac);
    }

    /**
     * Shatters magic constructs within r of c: Tensura barriers, jails and area spells (boss arena walls are spared), spell
     * projectiles, barrier effects, and barrier skills toggled on. What belongs to 'by' or its allies is left alone.
     * Returns how many things broke.
     */
    public static int shatterBarriers(ServerLevel level, Vec3 c, double r, LivingEntity by) {
        int n = 0;
        AABB box = new AABB(c, c).inflate(r);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof LivingEntity) && e.isAlive() && e.position().distanceToSqr(c) <= r * r)) {
            String cls = e.getClass().getName();
            boolean tensuraMagic = cls.startsWith(TENSURA_MAGIC) && !cls.endsWith("BossBarrierEntity") && !cls.contains("BarrierPart");
            boolean spell = e instanceof Projectile;
            if (!tensuraMagic && !spell) continue;
            if (friendly(e, by)) continue;
            e.discard();
            n++;
        }
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != by && x.isAlive() && !x.isAlliedTo(by)
                && x.distanceToSqr(c) <= r * r)) {
            for (MobEffectInstance m : new ArrayList<>(t.getActiveEffects())) {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(m.getEffect().value());
                if (id != null && id.getPath().contains("barrier")) { t.removeEffect(m.getEffect()); n++; }
            }
            n += barrierSkillsOff(t);
        }
        return n;
    }

    /** Toggles off every barrier skill of 't' (Multilayer Barrier, Ranged Barrier, ...). */
    static int barrierSkillsOff(LivingEntity t) {
        Skills skills;
        try { skills = SkillAPI.getSkillsFrom(t); } catch (Throwable noSkills) { return 0; }
        if (skills == null) return 0;
        int n = 0;
        for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
            if (!i.isToggled()) continue;
            ResourceLocation id = i.getSkillId();
            if (id == null || !id.getPath().contains("barrier")) continue;
            i.setToggled(false);
            try { i.onToggleOff(t); } catch (RuntimeException ignored) {}
            i.markDirty();
            n++;
        }
        if (n > 0) skills.markDirty();
        return n;
    }

    /** Owned by 'by' or one of its allies. */
    static boolean friendly(Entity e, LivingEntity by) {
        Entity owner = e instanceof TraceableEntity te ? te.getOwner() : null;
        return owner != null && (owner == by || owner.isAlliedTo(by) || by.isAlliedTo(owner));
    }

    /**
     * Belongs to a player: players themselves, their tamed animals and Tensura summons. Anything that would carry a lasting
     * change back to a player only ever gets temporary effects.
     */
    public static boolean playerSide(LivingEntity t) {
        if (t instanceof Player) return true;
        if (t instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        try { return io.github.manasmods.tensura.storage.ep.ExistenceStorage.isSummon(t); } catch (Throwable ignored) { return false; }
    }

    /** Max EP, 0 without Tensura data. */
    public static double maxEP(LivingEntity e) {
        try { return EnergyHelper.getMaxEP(e); } catch (Throwable ignored) { return 0; }
    }
}
