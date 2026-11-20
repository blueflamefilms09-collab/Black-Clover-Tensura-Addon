package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.beastwill.BeastWillManager;
import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.beastwill.BeastWillRules;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroNet;
import com.newuniverse.nusmp.heroweapon.skill.HeroKeys;
import com.newuniverse.nusmp.relics.djinn.DjinnEngine;
import com.newuniverse.nusmp.relics.djinn.DjinnNet;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules.Mode;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Weapon skills (docs/weapon_skills.md): one class for the ten Vassal weapon skills, the Djinn Equip skill and the Beast Will skill, so the weapon, the Djinn and the will are on the skill list and the skill wheel and the separate keys
 * are not needed. Everything is a table row ({@link WeaponSkillRules}): the wheel is the family's mode list and each mode stands for ONE {@link WeaponSkillRules.Act}, which here becomes the very call the old key's packet makes (HeroKeys.onKey /
 * onAction, DjinnEngine.onKey / onSkillTarget, BeastWillManager.onAction), so every rule, cost, cooldown and refusal of the key is the skill's. The skill works only for the owner (the bonded weapon, the Djinn bound or lent, the
 * will bound), says so on the action bar otherwise, and is switched off by the SERVER setting {@code weaponSkills}. It costs nothing of its own (no magicules, no cooldown, no tick work): at most one action per player per tick.
 */
public class WeaponSkill extends NUSkill {
    private final WeaponSkillRules.Kind kind;
    private final HeroKind hero;
    private final List<Mode> wheel;

    /** The skill of one Vassal weapon. */
    public WeaponSkill(HeroKind hero) { this(WeaponSkillRules.Kind.HERO, hero, textOf(hero)); }

    /** The Djinn Equip skill ({@link WeaponSkillRules.Kind#DJINN}) or the Beast Will skill ({@link WeaponSkillRules.Kind#BEAST}). */
    public WeaponSkill(WeaponSkillRules.Kind kind) { this(kind, null, textOf(kind)); }

    private WeaponSkill(WeaponSkillRules.Kind kind, HeroKind hero, SkillText text) {
        super(SkillType.EXTRA, text);
        this.kind = kind;
        this.hero = hero;
        this.wheel = WeaponSkillRules.modes(kind);
    }

    private static SkillText textOf(HeroKind k) {
        return SkillText.of(WeaponSkillRules.heroId(k), WeaponSkillRules.heroName(k), describe(WeaponSkillRules.Kind.HERO, "the " + k.display() + " you are bonded to"));
    }

    private static SkillText textOf(WeaponSkillRules.Kind k) {
        return k == WeaponSkillRules.Kind.DJINN
                ? SkillText.of(WeaponSkillRules.DJINN_ID, "Djinn Equip", describe(WeaponSkillRules.Kind.DJINN, "the Djinn you hold (bound, or lent to you)"))
                : SkillText.of(WeaponSkillRules.BEAST_ID, "Beast Will", describe(WeaponSkillRules.Kind.BEAST, "the Beast Will you carry"));
    }

    /** The description (also the source of tools/lang/weapon_skills.json): what the skill is for and every mode with the key it replaces. */
    public static String describe(WeaponSkillRules.Kind kind, String of) {
        StringBuilder b = new StringBuilder("Extra. Uses " + of + " from the skill wheel, so its keys are not needed (they still work). It works only while you own it. Modes: ");
        for (Mode m : WeaponSkillRules.modes(kind)) b.append(m.name()).append(" (").append(m.blurb()).append("), ");
        return b.substring(0, b.length() - 2) + ".";
    }

    public WeaponSkillRules.Kind family() { return kind; }
    public HeroKind heroKind() { return hero; }
    public List<Mode> wheel() { return wheel; }

    private Mode at(int mode) { return wheel.get(Math.floorMod(mode, wheel.size())); }

    @Override public int getModes(ManasSkillInstance i) { return wheel.size(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), wheel.size()); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return text().id() + "." + at(mode).id(); }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Mode m = at(mode);
        return Component.translatableWithFallback("anime_multiverse.skill.mode.weapon." + m.id(), m.name());
    }

    /** True while this player owns what the skill speaks for (server side; the bond, the Djinn state and the will state are the systems' own). */
    private boolean owned(ServerPlayer p) {
        return switch (kind) {
            case HERO -> HeroBonds.kindOf(p) == hero;
            case DJINN -> DjinnState.of(p).active() != null;
            case BEAST -> {
                BeastWillState s = BeastWillStore.peek(p);
                yield s != null && !s.active.isEmpty();
            }
        };
    }

    private Component notOwned() {
        return switch (kind) {
            case HERO -> SkillMsg.t("weapon.hero_unbound", "The %s is not bonded to you.", hero.display());
            case DJINN -> SkillMsg.t("weapon.djinn_none", "You hold no Djinn. Bind a Djinn Seal to a Legend-grade vessel.");
            case BEAST -> SkillMsg.t("weapon.beast_none", "You carry no Beast Will.");
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if (!WeaponSkills.turn(p)) return;                                      // one action per player per tick
        WeaponSkillRules.Verdict v = WeaponSkillRules.verdict(NUConfig.WEAPON_SKILLS.get(), owned(p));
        if (v == WeaponSkillRules.Verdict.SWITCHED_OFF) { SkillUtil.fail(p, SkillMsg.t("weapon.off", "The weapon skills are switched off on this server.")); return; }
        if (v == WeaponSkillRules.Verdict.NOT_OWNED) { SkillUtil.fail(p, notOwned()); return; }
        dispatch(p, at(mode).act());
    }

    /** The server action of a mode: the same entry point the key's packet reaches. No default: a new {@link WeaponSkillRules.Act} does not compile until it is wired here. */
    private static boolean dispatch(ServerPlayer p, WeaponSkillRules.Act act) {
        return switch (act) {
            case H_TECH_1 -> technique(p, 0);
            case H_TECH_2 -> technique(p, 1);
            case H_TECH_3 -> technique(p, 2);
            case H_TOGGLE -> { HeroKeys.onAction(p, HeroNet.ACT_TOGGLE); yield true; }
            case H_CYCLE -> { HeroKeys.onAction(p, HeroNet.ACT_CYCLE); yield true; }
            case D_LESSER -> { DjinnEngine.onSkillTarget(p, DjinnPhaseRules.LESSER); yield true; }
            case D_HALF -> { DjinnEngine.onSkillTarget(p, DjinnPhaseRules.HALF); yield true; }
            case D_FULL -> { DjinnEngine.onSkillTarget(p, DjinnPhaseRules.FULL); yield true; }
            case D_EXTREME -> { DjinnEngine.onSkillTarget(p, DjinnPhaseRules.EXTREME); yield true; }
            case D_DOWN -> { DjinnEngine.onKey(p, DjinnNet.ACT_DOWN); yield true; }
            case D_OFF -> { DjinnEngine.onKey(p, DjinnNet.ACT_OFF); yield true; }
            case D_RUKH -> { DjinnEngine.onKey(p, DjinnNet.ACT_RUKH); yield true; }
            case B_ACQUIRE -> { BeastWillManager.onAction(p, BeastWillNet.OP_FORM, BeastWillRules.ACQUIRE); yield true; }
            case B_INTEGRATE -> { BeastWillManager.onAction(p, BeastWillNet.OP_FORM, BeastWillRules.INTEGRATE); yield true; }
            case B_ABILITY_1 -> { BeastWillManager.onAction(p, BeastWillNet.OP_ABILITY, 1); yield true; }
            case B_ABILITY_2 -> { BeastWillManager.onAction(p, BeastWillNet.OP_ABILITY, 2); yield true; }
            case B_SWAP_NEXT -> { BeastWillManager.onAction(p, BeastWillNet.OP_SWAP, 1); yield true; }
            case B_SWAP_PREV -> { BeastWillManager.onAction(p, BeastWillNet.OP_SWAP, -1); yield true; }
        };
    }

    /** A technique key tapped: a press, and at once the release (the key was not held; the skill is a tap), through the same door as the packets. */
    private static boolean technique(ServerPlayer p, int slot) {
        HeroKeys.onKey(p, slot, HeroNet.PRESS);
        HeroKeys.onKey(p, slot, HeroNet.RELEASE);
        return true;
    }
}
