package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.skill.NUSkill;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: all eight Fairy Tail skills (seven Uniques and the Fire Dragon King) are one class driven by a {@link FtLine} table, so a spell can be added
 * by adding a row and no skill can ship with a missing mode name, a missing cooldown or an unguarded cast. It is a Tensura Unique skill
 * (the Fire Dragon King an Ultimate): the modes are the line's spells in table order, the ones the skill's mastery has not opened yet are
 * skipped when you cycle, a cast goes through the mod's fault guard ({@link SafeCast}) so a bug in one spell puts that skill on a short
 * recovery instead of crashing the tick, and the cooldown, the mastery gain and the mode name all come from the table. The passives live
 * here too: the element a skill embodies cannot hurt its user (fire for Natsu, cold for Gray, lightning for Laxus) and the forms turn
 * aside blows (Water Body, Iron Dragon Scales). Obtained from a {@code fairy_tail_skill_book}; the Fire Dragon Slayer evolves into the
 * Fire Dragon King by mastery (see {@link FairyTailSystem}).
 */
public class FtSkill extends NUSkill {
    private final FtLine line;

    public FtSkill(FtLine line) {
        super(line.ultimate ? SkillType.ULTIMATE : SkillType.UNIQUE, SkillText.of(line.id, line.name, line.description), line.ultimate);
        this.line = line;
    }

    public FtLine line() { return line; }

    @Override public int getModes(ManasSkillInstance instance) { return line.modes(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return FtRules.nextOpen(line, masteryFrac(instance), mode, reverse);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        FtMove mv = line.move(mode);
        return mv == null ? "none" : line.id + "." + mv.id();
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        FtMove mv = line.move(mode);
        return mv == null ? Component.literal("?") : SkillMsg.mode(line.id + "." + mv.id(), mv.name());
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        FtMove mv = line.move(mode);
        if (mv == null) return;
        double mastery = masteryFrac(instance);
        if (!FtRules.unlocked(mv, mastery)) {
            player.displayClientMessage(SkillMsg.t("fairytail.locked", "%s is not open yet: master %s more.", getModeName(instance, mode), mv.unlockPct() + "%").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (instance.onCoolDown(mode)) {
            player.displayClientMessage(SkillMsg.t("fairytail.cooldown", "%s is recovering.", getModeName(instance, mode)).withStyle(ChatFormatting.RED), true);
            return;
        }
        Tier tier = tier(instance, player);
        CompoundTag tag = instance.getOrCreateTag();
        FtCast.Outcome[] result = new FtCast.Outcome[1];
        long now = player.level().getGameTime();
        if (SafeCast.blocked(line.id, now)) {
            player.displayClientMessage(SkillMsg.t("fairytail.recovering", "This magic is recovering from a fault.").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean ran = SafeCast.run(line.id, now, () -> {
            result[0] = FtCast.cast(line, mv, tag, player, tier, mastery);
            return result[0].done();
        }, () -> { });
        if (!ran || result[0] == null || !result[0].done()) return;
        FtForms formOn = FtCast.activeForm(tag, player.level().getGameTime());
        cooldown(instance, mode, FtRules.cooldown(mv, mastery, formOn != null));
        grow(instance, player, mv.hurts() ? 1.0 + Math.min(4, result[0].hits()) * 0.3 : 1.0);
    }

    /** The passives: the element you embody cannot hurt you, and a form turns aside part of the blows. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0) return true;
        long now = owner.level().getGameTime();
        CompoundTag tag = instance.getOrCreateTag();
        FtForms form = FtCast.activeForm(tag, now);
        boolean fire = source.is(DamageTypeTags.IS_FIRE), cold = source.is(DamageTypeTags.IS_FREEZING), bolt = source.is(DamageTypeTags.IS_LIGHTNING);
        boolean immune = (fire && (line.element == FtElement.FIRE || form != null && form.immune().equals("fire")))
                || (cold && (line.element == FtElement.ICE || form != null && form.immune().equals("freezing")))
                || (bolt && (line.element == FtElement.LIGHTNING || form != null && form.immune().equals("lightning")));
        if (immune) {
            amount.set(0f);
            if (fire && line.eat.equals("fire")) {          // fire that would burn you is eaten
                FtCast.addCharge(tag, now, 1);
                instance.markDirty();
                if (owner.getHealth() < owner.getMaxHealth()) owner.heal(1.0f);
                owner.clearFire();
            }
            return true;
        }
        if (form != null && form.physicalCut() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount.set((float) FtRules.afterCut(amount.get(), form.physicalCut()));
        }
        return true;
    }
}
