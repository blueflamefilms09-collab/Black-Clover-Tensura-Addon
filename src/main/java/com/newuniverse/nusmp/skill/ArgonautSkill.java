package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ChargeRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Argonaut (Bell Cranel): HOLD the skill key to charge: a bell chimes every second (up to five) and each chime costs magicules; RELEASE to
 * unleash a heroic strike on everything in the cone in front of you. Damage grows with the charge (and 1.5x when mastered). Letting go
 * early still strikes with what you have; a strike that hits nothing teaches nothing. Hero's Argonaut (the Ultimate) extends this with ten
 * bells and a piercing beam. Numbers: {@link ChargeRules} and the config (argonautDamagePerCharge, argonautMagiculePerCharge, argonautCooldown).
 */
public class ArgonautSkill extends NUSkill {
    private static final String PAID = "Paid", BLOCKED = "Blocked";

    static final SkillText TEXT = SkillText.of("argonaut", "Argonaut",
            "Unique. The wish to become a hero. Hold to charge with the chime of bells (up to five), release to strike everything in front of you.");

    public ArgonautSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected ArgonautSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return ChargeRules.maxHeldTicks(ultimate ? Tier.ULTIMATE : Tier.BASE);
    }

    /** A charge that ran out of magicules still strikes with what was paid for. */
    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        return true;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        tag.putInt(PAID, 0);
        boolean cooling = instance.onCoolDown(mode);
        tag.putBoolean(BLOCKED, cooling);
        if (cooling) SkillUtil.fail(player, SkillMsg.t("argonaut.cooldown", "Argonaut is recovering."));
        instance.markDirty();
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        var tag = instance.getOrCreateTag();
        if (tag.getBoolean(BLOCKED)) return false;
        Tier tier = tier(instance, player);
        if (!ChargeRules.chimeNow(heldTicks, tier)) return true;

        int paid = tag.getInt(PAID);
        if (paid >= ChargeRules.maxCharge(tier)) return true;
        if (!SkillUtil.spendMagicules(player, NUConfig.ARGONAUT_MAGICULE_PER_CHARGE.get())) return false;
        paid++;
        tag.putInt(PAID, paid);
        instance.markDirty();

        ServerLevel level = player.serverLevel();
        SkillFx.sound(level, player.position(), SoundEvents.BELL_BLOCK, 1.0F, 0.8F + paid * 0.12F);
        SkillFx.spray(level, false, player.position().add(0, 1, 0), 4 + paid, 0.4, 0.6, 0.4, 0.02);
        if (paid >= ChargeRules.maxCharge(tier)) {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.full", "Fully charged: release!").withStyle(ChatFormatting.GOLD));
        } else {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.charge", "Charge: %s/%s", paid, ChargeRules.maxCharge(tier)).withStyle(ChatFormatting.WHITE));
        }
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        int charge = tag.getInt(PAID);
        boolean blocked = tag.getBoolean(BLOCKED);
        tag.putInt(PAID, 0);
        tag.putBoolean(BLOCKED, false);
        instance.markDirty();
        if (blocked || charge <= 0 || !player.isAlive()) return;

        int hits = release(instance, player, charge, mode);
        cooldown(instance, mode, NUConfig.ARGONAUT_COOLDOWN.get());
        SkillUtil.castVfx(player, 0xFFFFFFFF);
        grow(instance, player, ChargeRules.masteryGain(charge, hits));
    }

    /** The strike itself; returns how many creatures it hit. Hero's Argonaut overrides this for its beam. */
    protected int release(ManasSkillInstance instance, ServerPlayer player, int charge, int mode) {
        return strike(instance, player, charge);
    }

    /** The cone strike: everything in front of the caster takes the charge's damage and is thrown back. */
    protected final int strike(ManasSkillInstance instance, ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);
        double damage = ChargeRules.damage(NUConfig.ARGONAUT_DAMAGE_PER_CHARGE.get(), charge, tier, instance.isMastered(player));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        int hits = 0;
        for (LivingEntity t : SkillTargets.foesInCone(player, ChargeRules.strikeReach(tier), ChargeRules.strikeCone(tier))) {
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance)) > 0f) {
                t.knockback(ChargeRules.knockback(charge), -look.x, -look.z);
                hits++;
            }
        }
        Vec3 tip = eye.add(look.scale(ChargeRules.strikeReach(tier)));
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, eye.add(look.scale(0.8)).add(0, -0.3, 0), tip, 0xFFFFF2B0, 10, 1.0f + charge * 0.12f);
        SkillFx.spray(level, false, eye.add(look.scale(2.0)).add(0, -0.3, 0), 6 + charge, 1.0, 0.3, 1.0, 0.0);
        SkillFx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
        return hits;
    }
}
