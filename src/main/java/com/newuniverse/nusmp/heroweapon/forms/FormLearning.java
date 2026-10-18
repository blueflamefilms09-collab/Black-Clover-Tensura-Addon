package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.rules.HeroXp;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a form becomes the wielder's in four ways (the gem eats enough and it opens by itself, the Learn button, an operator's command, and the catch-up at login
 * when new data made a form available), and all four must do the same things in the same order: check what is allowed, record the form, tell the wielder, play the
 * sound, fire the quest hook, pay the small experience, and let the Weapon Book know. One class owns that, so the four routes cannot drift. The question the item
 * registry alone can answer (does this item belong to this tag) is {@link FormTags}, which {@link FormTree} and {@link AbsorbRules} ask through an interface so they
 * stay free of Minecraft types.
 */
public final class FormLearning {
    private FormLearning() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The forms that could be learned right now for this bond (nothing learned yet), in the order they would open. */
    public static List<String> learnable(HeroBond bond) {
        if (!bond.isBound()) return List.of();
        return FormTree.learnable(HeroForms.of(bond.kind()), bond.unlocked(), bond.absorbedMap(), FormTags.MATCHER, CurseLink.series(bond));
    }

    /**
     * Learns every form that is available, if the config {@code autoLearn} says so (or {@code force} is set), announcing each. {@code via} names what the gem just took
     * in ("Oak Leaves"), or is null for a learn that is not from a meal. Returns the forms learned.
     */
    public static List<HeroForm> learnAvailable(ServerPlayer player, Component via, boolean force) {
        List<HeroForm> out = new ArrayList<>();
        try {
            HeroBond bond = HeroBonds.of(player);
            if (!bond.isBound() || !force && !FormsConfig.get(FormsConfig.AUTO_LEARN)) return out;
            for (String id : learnable(bond)) {
                HeroForm f = HeroForms.get(bond.kind(), id);
                if (f != null && learn(player, bond, f, via)) out.add(f);
            }
            if (!out.isEmpty()) {
                HeroForms.refresh(player);
                FormsSync.markDirty(player);
            }
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: learning failed", e);
        }
        return out;
    }

    /** The Learn button of the Weapon Book: learn one named form if it is available. Returns the reason it was refused (a line to show), or null on success. */
    public static Component tryLearn(ServerPlayer player, String formId) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) return FormsLang.t("hero.forms.no_weapon", "You have no hero weapon.");
        HeroForm f = HeroForms.get(bond.kind(), formId);
        if (f == null) return FormsLang.t("hero.forms.unknown", "There is no form '%s'.", String.valueOf(formId));
        if (bond.isUnlocked(f.id())) return FormsLang.t("hero.forms.already", "%s is already learned.", f.name());
        if (!FormTree.parentsLearned(f, bond.unlocked())) return FormsLang.t("hero.forms.need_parents", "%s needs its earlier forms first.", f.name());
        if (FormTree.sealed(f, CurseLink.series(bond)))
            return FormsLang.t("hero.forms.sealed", "The %s series is sealed.", f.sin() == null ? "curse" : f.sin().label());
        if (!FormTree.requirementsMet(f, bond.absorbedMap(), FormTags.MATCHER)) return FormsLang.t("hero.forms.need_items", "The gem has not absorbed enough for %s yet.", f.name());
        if (learn(player, bond, f, null)) {
            HeroForms.refresh(player);
            FormsSync.markDirty(player);
            learnAvailable(player, null, false);                       // with autoLearn on, the form just learned may be the parent of one already satisfied
        }
        return null;
    }

    /** An operator's command: learn the form regardless of parents and requirements. Returns false when it was already learned. */
    public static boolean force(ServerPlayer player, HeroForm form) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound() || !learn(player, bond, form, null)) return false;
        HeroForms.refresh(player);
        FormsSync.markDirty(player);
        return true;
    }

    /** Records one form as learned and does everything that goes with it. False when it was already learned. */
    private static boolean learn(ServerPlayer player, HeroBond bond, HeroForm form, Component via) {
        if (!bond.unlock(form.id())) return false;
        HeroKind kind = bond.kind();
        if (FormsConfig.get(FormsConfig.ANNOUNCE)) {
            Component line = via != null
                    ? FormsLang.t("hero.forms.learned_from", "The %s takes in %s: it becomes %s.", kind.display(), via, form.name())
                    : FormsLang.t("hero.forms.learned", "The %s has learned a new form: %s.", kind.display(), form.name());
            FormsFeedback.chat(player, line);
        }
        FormsFeedback.sound(player, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.4f);
        FormsFeedback.circle(player, FormsFeedback.gemColor(bond));
        NusmpEvents.fire(player, "hero.form", kind.itemId() + ":" + form.id());
        HeroXp.gain(player, MasteryRules.reward(FormsConfig.get(FormsConfig.LEARN_EXPERIENCE), form.tier()));
        return true;
    }

    /**
     * Forgets everything the weapon has learned and eaten, keeping the weapon, its experience and the other systems' state: the active form goes back to the base
     * form and the permanent bonuses are taken off. Done through the bond's own save format because the bond has no method that clears only the forms.
     */
    public static void resetForms(ServerPlayer player) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) return;
        boolean summoned = bond.summoned();
        CompoundTag tag = bond.toTag();
        tag.remove("unlocked");
        tag.remove("bonus");
        tag.remove("mastery");
        tag.remove("absorbed");
        tag.putString("form", HeroForm.BASE_ID);
        bond.copyFrom(HeroBond.fromTag(tag));
        bond.setSummoned(summoned);
        HeroForms.refresh(player);
        FormsSync.markDirty(player);
    }
}
