package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 3.56.0: one Dragon Ball skill, driven entirely by its {@link Spec} row ({@link DbRules}). The same class is the Unique form (UNIQUE: the first passive and the first two actives, at half strength) and the
 * Ultimate it evolves into (ULTIMATE: every passive under the skill's ON/OFF toggle, every active on the wheel, and the mastery-only finisher). What each mode does is {@link DbMoves}; the passives are
 * {@link DbPassives}. State is the player's own (the skill tag and the player's data), nothing is shared, nothing ticks except one look a second for an owner whose skill is toggled on.
 */
public class DbSkill extends NUSkill {
    private final Spec spec;
    private final boolean ult;
    private final List<Mode> wheel;

    public DbSkill(Spec spec, boolean ult) {
        super(ult ? SkillType.ULTIMATE : SkillType.UNIQUE, SkillText.of(ult ? spec.id() : spec.uniqueId(), ult ? spec.title() + ", " + spec.epithet() : spec.uniqueName(), describe(spec, ult)), ult);
        this.spec = spec;
        this.ult = ult;
        this.wheel = ult ? spec.actives() : spec.uniqueActives();
    }

    private static String describe(Spec s, boolean ult) {
        StringBuilder b = new StringBuilder(ult ? "Ultimate. " : "Unique, evolves into " + s.title() + ", " + s.epithet() + " once mastered. ");
        b.append(s.power()).append(". ");
        if (ult) {
            b.append("Passives (one ON/OFF switch): ");
            for (Mode m : s.passives()) b.append(m.name()).append(", ");
            b.append("actives: ");
            for (Mode m : s.actives()) b.append(m.name()).append(m.mastery() ? " (mastery)" : "").append(", ");
        } else {
            Mode p = s.firstPassive();
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
        if (!(e instanceof ServerPlayer p) || p.tickCount % DbRules.PASSIVE_PERIOD_TICKS != 0) return;
        DbPassives.second(i, p, spec, ult, i.isMastered(p), NUConfig.DB_ENABLED.get());
    }

    @Override public void onToggleOff(ManasSkillInstance i, LivingEntity e) { if (e instanceof ServerPlayer p) DbPassives.clear(p); }

    @Override
    public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
        super.onForgetSkill(i, e);
        if (e instanceof ServerPlayer p) DbPassives.clear(p);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (!NUConfig.DB_ENABLED.get()) { SkillUtil.fail(p, SkillMsg.t("db.off", "The Dragon Ball skills are switched off on this server.")); return; }
        int idx = Math.floorMod(mode, wheel.size());
        Mode m = wheel.get(idx);
        boolean mastered = instance.isMastered(p);
        if (m.mastery() && !mastered) { SkillUtil.fail(p, SkillMsg.t("db.mastery", "%s answers only a mastered skill.", m.name())); return; }
        if (m.crouch() && !p.isShiftKeyDown()) { SkillUtil.fail(p, SkillMsg.t("db.crouch", "Crouch and press to use %s.", m.name())); return; }
        if (instance.onCoolDown(idx)) { SkillUtil.fail(p, SkillMsg.t("db.cooldown", "%s is recharging (%ss).", m.name(), instance.getCoolDown(idx))); return; }
        double cost = p.isCreative() ? 0 : DMUtil.cost(DbRules.cost(m, mastered, 1.0) * (ult ? 1.0 : DbRules.UNIQUE_FACTOR + 0.5));
        if (!SkillUtil.spendMagicules(p, cost)) return;
        if (!DbMoves.cast(spec, ult, m, p, mastered ? 1.0 : 0.4)) { SkillUtil.refund(p, cost); return; }
        int seconds = DbPassives.cooldownSeconds(DbRules.cooldown(m, mastered), p, spec);
        if (seconds > 0) instance.setCoolDown(seconds, idx);
        SkillUtil.castVfx(p, m.move().rgb() == 0xFFFFFFFF ? 0xFFE0F0FF : m.move().rgb());
        grow(instance, p, 1);
    }
}
