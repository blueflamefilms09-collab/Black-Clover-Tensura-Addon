package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.rules.HeroXp;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;

/**
 * Why: mastery (the series' proficiency of a form) is earned by using the form, and the things that count are done in the core's code (a blow, a technique, a block),
 * not in the forms system. The core calls these three methods at those moments; the forms system adds the mastery to the ACTIVE form of the bond (a point per hit,
 * two per technique, one per block, by its config), finishes a form when its mastery is full (the permanent ability bonus, the quest hook, a title, a little weapon
 * experience) and refreshes the wielder. A technique also starts the short lock on changing form. The arithmetic, including "finishes exactly once", is
 * {@link MasteryRules}; this class moves the numbers in and out of the bond.
 */
public final class FormMastery {
    private FormMastery() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The wielder landed a melee hit with the summoned weapon. */
    public static void hit(ServerPlayer player) { add(player, FormsConfig.get(FormsConfig.MASTERY_PER_HIT)); }

    /** The wielder cast a technique (after it succeeded). */
    public static void skill(ServerPlayer player, HeroSkill skill) {
        try {
            FormLocks.technique(player.getUUID(), player.level().getGameTime());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: could not note a technique", e);
        }
        add(player, FormsConfig.get(FormsConfig.MASTERY_PER_SKILL));
    }

    /** The wielder's guard stopped a blow. */
    public static void block(ServerPlayer player) { add(player, FormsConfig.get(FormsConfig.MASTERY_PER_BLOCK)); }

    /** Adds points to the active form, finishing it when they fill it. Never throws. */
    static void add(ServerPlayer player, int points) {
        try {
            HeroBond bond = HeroBonds.of(player);
            if (!bond.isBound() || points <= 0) return;
            HeroForm form = HeroForms.active(bond);
            if (form.mastery() <= 0) return;
            int before = bond.mastery(form.id());
            MasteryRules.Step step = MasteryRules.add(before, points, form.mastery());
            if (step.value() > before) {
                bond.addMastery(form.id(), step.value() - before);
                FormsSync.markDirty(player);
            }
            if (step.finished() && bond.grantBonus(form.id())) finish(player, bond, form);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: mastery failed", e);
        }
    }

    /** Sets a form's mastery to an exact value (the operator's command); finishing it if that fills it. */
    static void set(ServerPlayer player, HeroForm form, int value) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound() || form.mastery() <= 0) return;
        int target = Math.max(0, Math.min(form.mastery(), value));
        int before = bond.mastery(form.id());
        bond.addMastery(form.id(), target - before);
        FormsSync.markDirty(player);
        if (target >= form.mastery() && bond.grantBonus(form.id())) finish(player, bond, form);
        else HeroForms.refresh(player);
    }

    private static void finish(ServerPlayer player, HeroBond bond, HeroForm form) {
        HeroForms.refresh(player);
        if (FormsConfig.get(FormsConfig.ANNOUNCE))
            FormsFeedback.title(player, Component.literal(form.name()).withStyle(ChatFormatting.GOLD), FormsLang.t("hero.forms.mastered", "Mastered"));
        FormsFeedback.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        NusmpEvents.fire(player, "hero.mastery", bond.kind().itemId() + ":" + form.id());
        HeroXp.gain(player, MasteryRules.reward(FormsConfig.get(FormsConfig.MASTERY_EXPERIENCE), form.tier()));
        FormsSync.markDirty(player);
    }
}
