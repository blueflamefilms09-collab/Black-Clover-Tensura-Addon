package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Passive;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Spec;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 3.58.0: one Beast Will skill, driven entirely by its {@link Spec} row ({@link BeastSkillRules}). The same class is the Unique form (UNIQUE: the first passive and the first two actives, at half strength) and
 * the Ultimate it evolves into (ULTIMATE: every passive under the skill's ON/OFF toggle, every active on the wheel, and the mastery-only finisher). What each active does is {@link BeastMoves}; the passives are
 * {@link BeastPassives}. The skill is independent of the Beast Will binding and assimilation. State is the player's own; nothing ticks except one look a second for an owner whose skill is toggled on.
 */
public class BeastSkill extends NUSkill {
    private final Spec spec;
    private final DbRules.Spec db;
    private final boolean ult;
    private final List<Mode> wheel;

    public BeastSkill(Spec spec, boolean ult) {
        super(ult ? SkillType.ULTIMATE : SkillType.UNIQUE, SkillText.of(ult ? spec.id() : spec.uniqueId(), ult ? spec.title() + ", " + spec.epithet() : spec.uniqueName(), describe(spec, ult)), ult);
        this.spec = spec;
        this.db = spec.asDb();
        this.ult = ult;
        this.wheel = ult ? spec.actives() : spec.uniqueActives();
    }

    private static String describe(Spec s, boolean ult) {
        StringBuilder b = new StringBuilder(ult ? "Ultimate. " : "Unique, evolves into " + s.title() + ", " + s.epithet() + " once mastered. ");
        b.append(s.power()).append(". ");
        if (ult) {
            b.append("Passives (one ON/OFF switch): ");
            for (Passive m : s.passives()) b.append(m.name()).append(", ");
            b.append("actives: ");
            for (Mode m : s.actives()) b.append(m.name()).append(m.mastery() ? " (mastery)" : "").append(", ");
        } else {
            Passive p = s.firstPassive();
            if (p != null) b.append("Passive: ").append(p.name()).append(". ");
            b.append("Actives: ");
            for (Mode m : s.uniqueActives()) b.append(m.name()).append(", ");
        }
        return b.substring(0, b.length() - 2) + ".";
    }

    public Spec spec() { return spec; }
    public boolean isUltimate() { return ult; }
    public List<Mode> wheel() { return wheel; }

    private Mode at(int mode) { return wheel.get(Math.floorMod(mode, wheel.size())); }

    @Override public int getModes(ManasSkillInstance i) { return wheel.size(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), wheel.size()); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return spec.id() + "." + at(mode).id(); }
    @Override public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Mode m = at(mode);
        Component name = Component.translatableWithFallback("nusmp.skill.mode." + spec.id() + "." + m.id(), m.name());
        int cd = i.getCoolDown(Math.floorMod(mode, wheel.size()));
        return cd > 0 ? name.copy().append(Component.literal(" (" + cd + "s)")) : name;
    }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % BeastSkillRules.PASSIVE_PERIOD_TICKS != 0) return;
        BeastPassives.second(i, p, spec, ult, NUConfig.BEAST_SKILLS_ENABLED.get());
    }

    @Override public void onToggleOff(ManasSkillInstance i, LivingEntity e) { if (e instanceof ServerPlayer p) BeastPassives.clear(p); }

    @Override
    public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
        super.onForgetSkill(i, e);
        if (e instanceof ServerPlayer p) BeastPassives.clear(p);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (!NUConfig.BEAST_SKILLS_ENABLED.get()) { SkillUtil.fail(p, SkillMsg.t("beast.off", "The Beast Will skills are switched off on this server.")); return; }
        int idx = Math.floorMod(mode, wheel.size());
        Mode m = wheel.get(idx);
        boolean mastered = instance.isMastered(p);
        if (m.mastery() && !mastered) { SkillUtil.fail(p, SkillMsg.t("beast.mastery", "%s answers only a mastered skill.", m.name())); return; }
        if (m.crouch() && !p.isShiftKeyDown()) { SkillUtil.fail(p, SkillMsg.t("beast.crouch", "Crouch and press to use %s.", m.name())); return; }
        if (instance.onCoolDown(idx)) { SkillUtil.fail(p, SkillMsg.t("beast.cooldown", "%s is recharging (%ss).", m.name(), instance.getCoolDown(idx))); return; }
        double cost = p.isCreative() ? 0 : DMUtil.cost(BeastSkillRules.cost(m, mastered) * (ult ? 1.0 : DbRules.UNIQUE_FACTOR + 0.5));
        if (!SkillUtil.spendMagicules(p, cost)) return;
        if (!BeastMoves.cast(spec, db, ult, m, p, mastered ? 1.0 : 0.4)) { SkillUtil.refund(p, cost); return; }
        int seconds = BeastPassives.cooldownSeconds(BeastSkillRules.cooldown(m, mastered));
        if (seconds > 0) instance.setCoolDown(seconds, idx);
        SkillUtil.castVfx(p, spec.rgb());
        if (m.mastery()) p.displayClientMessage(Component.translatableWithFallback("nusmp.beastwill.incant." + spec.key(), ""), true);   // 3.60.0: the finisher's own incantation, one action-bar line to the caster
        grow(instance, p, 1);
    }
}
