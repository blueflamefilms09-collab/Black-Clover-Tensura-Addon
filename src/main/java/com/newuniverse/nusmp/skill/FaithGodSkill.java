package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * One god of the Faith lines (docs/faith_gods.md), driven entirely by its {@link God} row in {@link FaithGodRules}. The same class is the Unique form ("Faith of Femto": 3 actives and 1 passive at 0.6 strength) and the god
 * form it evolves into ("Femto, God of Incarnation": the class's actives and passives at the class's strength, an {@link UltimateClass} of its own). Faith points are fuel: they are spent from the player's Faith (or
 * Goddess of Faith) skill, which the worshippers fill with offerings, and the worshippers also make the god stronger ({@link FaithGodRules#devotion}). What each active does is {@link FaithGodMoves}; the passives
 * (one potion effect each, once a second while the skill is switched on) are {@link FaithGodMoves#passives}. State is the player's own (the skill tag); nothing is shared and nothing is kept in a static map.
 * The canon pass (SERVER setting faithGods.canon) swaps the table the skill reads ({@link #cur()}) at run time: the registered skill, its id and its mode count never change, so saves load the same either way.
 */
public class FaithGodSkill extends NUSkill {
    private final God god;
    private final boolean ult;

    public FaithGodSkill(God god, boolean ult) {
        super(ult ? SkillType.ULTIMATE : SkillType.UNIQUE, SkillText.of(ult ? god.ultimateId() : god.uniqueId(), ult ? god.ultimateName() : god.name(), describe(god, ult)), ult);
        this.god = god;
        this.ult = ult;
    }

    private static String describe(God g, boolean ult) {
        StringBuilder b = new StringBuilder(ult ? "Ultimate (" + g.cls().key() + " class). Evolved from " + g.name() + ". " : "Unique, evolves into " + g.ultimateName() + " once mastered. ");
        b.append("God of ").append(g.domain().toLowerCase()).append(". Worshippers and offerings feed it. Passives (one ON/OFF switch): ");
        for (FaithGodRules.Passive p : FaithGodRules.passives(g, ult)) b.append(p.name()).append(", ");
        b.append("actives: ");
        for (Move m : FaithGodRules.actives(g, ult)) b.append(m.name()).append(", ");
        return b.substring(0, b.length() - 2) + ".";
    }

    /** The registered (4.6.0) row. */
    public God god() { return god; }

    /** The row in force right now: the canon table while faithGods.canon is on, else the 4.6.0 row. Same ids and same counts either way. */
    God cur() { return FaithGodRules.pick(god, NUConfig.faithGodCanon()); }

    public boolean isUltimate() { return ult; }
    public List<Move> wheel() { return FaithGodRules.actives(cur(), ult); }

    @Override public UltimateClass ultimateClass() { return ult ? god.cls() : null; }      // docs/faith_gods.md (the Unique stays unclassified)

    private Move at(int mode) { List<Move> w = wheel(); return w.get(Math.floorMod(mode, w.size())); }

    @Override public int getModes(ManasSkillInstance i) { return FaithGodRules.activeCount(god, ult); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), FaithGodRules.activeCount(god, ult)); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return (ult ? god.ultimateId() : god.uniqueId()) + "." + at(mode).id(); }
    @Override public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Move m = at(mode);
        Component name = Component.translatableWithFallback("anime_multiverse.skill.mode." + (ult ? god.ultimateId() : god.uniqueId()) + "." + m.id(), m.name());
        int cd = i.getCoolDown(Math.floorMod(mode, FaithGodRules.activeCount(god, ult)));
        return cd > 0 ? name.copy().append(Component.literal(" (" + cd + "s)")) : name;
    }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !FaithGods.active()) return;      // once a second per player holding the skill switched on
        FaithGodMoves.passives(cur(), ult, i, p);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (!FaithGods.active()) { SkillUtil.fail(p, SkillMsg.t("gods.off", "The Faith gods are switched off on this server.")); return; }
        God g = cur();
        List<Move> wheel = FaithGodRules.actives(g, ult);
        int idx = Math.floorMod(mode, wheel.size());
        Move m = wheel.get(idx);
        double mf = masteryFrac(instance);
        if (!FaithGodRules.unlocked(m, ult, mf)) { SkillUtil.fail(p, SkillMsg.t("gods.locked", "%s is not open yet: it opens at %s percent mastery of this skill.", m.name(), Math.round(m.unlock() * 100.0))); return; }
        if (instance.onCoolDown(idx)) { SkillUtil.fail(p, SkillMsg.t("gods.cooldown", "%s is recharging (%ss).", m.name(), instance.getCoolDown(idx))); return; }
        ManasSkillInstance pool = FaithGodMoves.faithPool(p, instance);
        int worshippers = FaithGodMoves.worshipperCount(pool);
        if (!FaithGodRules.enoughWorshippers(m, worshippers)) { SkillUtil.fail(p, SkillMsg.t("gods.need_worshippers", "%s needs at least %s worshippers (you have %s).", m.name(), m.minWorshippers(), worshippers)); return; }
        int faith = FaithSkill.getFaith(pool);
        if (!FaithRules.canSpend(faith, m.faith())) { SkillUtil.fail(p, SkillMsg.t("gods.no_faith", "%s needs %s Faith (you have %s). Worshippers' offerings and Pray fill it.", m.name(), m.faith(), faith)); return; }
        double cost = p.isCreative() ? 0 : price(instance, p, DMUtil.cost(m.mp()));
        if (!charge(instance, p, idx, cost)) return;
        double strength = FaithGodRules.strength(g, ult, worshippers, NUConfig.faithGodDevotionPercent(), NUConfig.faithGodPowerPercent());
        if (!FaithGodMoves.cast(g, m, ult, p, instance, strength, mf)) { SkillUtil.refund(p, cost); return; }
        FaithSkill.setFaith(pool, FaithRules.spend(faith, m.faith()));
        cooldown(instance, idx, m.cooldownTicks());
        SkillUtil.castVfx(p, 0xFF000000 | FaithGodRules.rgbOf(g, ult));
        grow(instance, p, FaithGodRules.masteryGain(g.canonRules() != null && !ult, instance.getMaxMastery()));
    }
}
