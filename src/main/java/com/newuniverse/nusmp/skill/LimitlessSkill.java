package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.GojoRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.EntityEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Limitless (Satoru Gojo): the Ultimate evolution of Six Eyes. Four modes. Infinity (on/off, kept in the skill tag "Infinity"): a barrier
 * that stops every attack and every projectile before it reaches you, for 120 magicules a second (60 when mastered), switching itself
 * off when they run out. Blue: pulls every foe around the spot you look at into it and hurts them. Red: blasts every foe around you
 * away. Hollow Purple: an erasing beam 64 blocks long that kills what it can and heavily damages bosses and protected players. Only
 * foes are ever pulled, pushed or struck; allies, pets and summons are not. Numbers: {@link GojoRules}.
 */
public class LimitlessSkill extends NUSkill {
    private static final String[] KEYS = {"infinity", "blue", "red", "purple"};

    static final SkillText TEXT = SkillText.of("limitless", "Limitless",
            "Ultimate. Infinity stops every attack before it reaches you. Blue, Red and Hollow Purple bend space itself.");

    public LimitlessSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return GojoRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return r ? (mode + GojoRules.MODES - 1) % GojoRules.MODES : (mode + 1) % GojoRules.MODES; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "limitless." + KEYS[Math.floorMod(mode, KEYS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (mode) {
            case GojoRules.INFINITY -> SkillMsg.mode("limitless.infinity", "Infinity (on/off)");
            case GojoRules.BLUE -> SkillMsg.mode("limitless.blue", "Blue");
            case GojoRules.RED -> SkillMsg.mode("limitless.red", "Red");
            default -> SkillMsg.mode("limitless.purple", "Hollow Purple");
        };
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    private static boolean infinityOn(ManasSkillInstance i) { return i.getOrCreateTag().getBoolean(GojoRules.INFINITY_KEY); }

    /** Infinity costs magicules every second and collapses when they run out. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || !infinityOn(instance) || player.tickCount % 20 != 0) return;
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(GojoRules.infinityDrain(instance.isMastered(player))))) {
            instance.getOrCreateTag().putBoolean(GojoRules.INFINITY_KEY, false);
            SkillUtil.actionbar(player, SkillMsg.t("limitless.collapsed", "Infinity collapsed.").withStyle(ChatFormatting.GRAY));
        }
        instance.markDirty();
    }

    /** Infinity: an attack from something never reaches you. */
    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity entity, DamageSource source, float amount) {
        if (!infinityOn(instance) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        if (source.getEntity() == null && source.getDirectEntity() == null) return true; // only attacks from something
        if (entity.level() instanceof ServerLevel level) {
            SkillFx.spray(level, false, entity.position().add(0, 1, 0), 8, 0.5, 0.7, 0.5, 0.0);
        }
        return false; // the attack never reaches you
    }

    /** Infinity also stops projectiles: they are turned back at the shooter before they land. */
    @Override
    public void onProjectileHit(ManasSkillInstance instance, LivingEntity living, EntityHitResult hitResult, Projectile projectile,
                                Changeable<ProjectileDeflection> deflection, Changeable<EntityEvents.ProjectileHitResult> result) {
        if (!infinityOn(instance)) return;
        deflection.set(ProjectileDeflection.REVERSE);
        result.set(EntityEvents.ProjectileHitResult.PASS);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);

        if (mode == GojoRules.INFINITY) {
            boolean on = !infinityOn(instance);
            instance.getOrCreateTag().putBoolean(GojoRules.INFINITY_KEY, on);
            instance.markDirty();
            SkillUtil.actionbar(player, on ? SkillMsg.t("limitless.on", "Infinity: ON").withStyle(ChatFormatting.AQUA)
                    : SkillMsg.t("limitless.off", "Infinity: OFF").withStyle(ChatFormatting.GRAY));
            if (on) SkillFx.spray(level, false, player.position().add(0, 1, 0), 12, 0.6, 0.8, 0.6, 0.0);
            return;
        }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("limitless.cooldown", "Cursed technique on cooldown.")); return; }
        if (!SkillUtil.spendMagicules(player, DMUtil.cost(GojoRules.cost(mode, mastered)))) return;

        switch (mode) {
            case GojoRules.BLUE -> blue(instance, player, level, mastered);
            case GojoRules.RED -> red(instance, player, level, mastered);
            default -> purple(instance, player, level, mastered);
        }
        cooldown(instance, mode, DMUtil.cooldown(GojoRules.cooldown(mode)));
        SkillUtil.castVfx(player, 0xFF7FB2FF);
        grow(instance, player, 1);
    }

    /** Blue: everything near the spot you look at is pulled into it. */
    private void blue(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 c = DMUtil.lookPoint(player, GojoRules.blueRange());
        for (LivingEntity t : SkillTargets.foes(player, c, GojoRules.blueRadius(mastered))) {
            Vec3 pull = c.subtract(t.position());
            if (pull.lengthSqr() > 1.0e-4) {
                Vec3 d = pull.normalize().scale(GojoRules.bluePull());
                t.setDeltaMovement(d.x, d.y + 0.2, d.z);
                t.hurtMarked = true;
            }
            SkillDamage.hit(player, t, GojoRules.blueDamage(mastered), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        SkillFx.shape(level, VfxShape.SPATIAL_RIFT, c.add(0, 1, 0), c.add(0, 2, 0), 0xFF4A8CFF, 30, 1.4f);
        SkillFx.spray(level, false, c.add(0, 1, 0), 12, 1.5, 1.5, 1.5, 0.0);
        SkillFx.sound(level, c, SoundEvents.BEACON_AMBIENT, 1.5F, 1.4F);
    }

    /** Red: everything around you is blasted away. */
    private void red(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 at = player.position();
        for (LivingEntity t : SkillTargets.foes(player, at, GojoRules.redRadius(mastered))) {
            Vec3 away = t.position().subtract(at);
            if (away.lengthSqr() > 1.0e-4) {
                Vec3 d = away.normalize().scale(GojoRules.redPush());
                t.setDeltaMovement(d.x, 0.8, d.z);
                t.hurtMarked = true;
            }
            SkillDamage.hit(player, t, GojoRules.redDamage(mastered), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        SkillFx.shape(level, VfxShape.FIRE_BURST, at, at.add(0, 1, 0), 0xFFFF3A3A, 20, (float) (GojoRules.redRadius(mastered) * 0.3));
        SkillFx.spray(level, true, at.add(0, 1, 0), 12, 2.5, 1.0, 2.5, 0.0);
        SkillFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.4F);
    }

    /** Hollow Purple: an erasing beam. */
    private void purple(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 from = player.getEyePosition().add(0, -0.2, 0);
        Vec3 to = DMUtil.lookPoint(player, GojoRules.purpleLength());
        for (LivingEntity t : SkillTargets.foesAlong(player, from, to, GojoRules.purpleRadius())) {
            if (GodTier.fullyAffected(t)) GodTier.kill(player, t);
            else SkillDamage.hit(player, t, GojoRules.purpleDamage(mastered), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        SkillFx.beam(level, from, to, true);
        SkillFx.shape(level, VfxShape.DARK_SLASH_DIMENSION, from, to, 0xFFB06AFF, 20, 2.0f);
        SkillFx.sound(level, player.position(), SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.6F);
    }
}
