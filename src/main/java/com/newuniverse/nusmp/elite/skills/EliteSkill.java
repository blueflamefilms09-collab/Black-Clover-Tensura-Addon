package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.NUSkill;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the Extra, Intrinsic, Unique and Ultimate skills of the elite set share one script: refuse if the subsystem is switched off or the mode is on cooldown, pay the magicules, do the
 * power, give the magicules back if the power could not be used, start the cooldown, show the cast circle, earn mastery. That script lives here once, with the mode list (a mode is an id, an
 * English name, a cost and a cooldown, so the mode names reach the lang generator as plain literals) and the elite config multipliers on top of the pack-wide ones. Skills that are
 * passive, toggled or held override the pieces they need. It extends {@link NUSkill}, so icon, translatable name and description, and the tier come with it.
 */
public abstract class EliteSkill extends NUSkill {
    protected EliteSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    /** The skill's modes, in mode-number order. A skill must never reorder or drop one (saved cooldowns are per mode number). */
    protected abstract List<EliteMode> modes();

    /** Whether pressing this mode runs {@link #use} at once (true) or the mode is a held / toggled / passive one handled by an override (false). */
    protected boolean instant(int mode) { return true; }

    /** The power of a mode. Return false if it could not be used (the cost is refunded and no cooldown starts). */
    protected abstract boolean use(ManasSkillInstance instance, ServerPlayer player, int mode);

    /** ARGB colour of the cast circle. */
    protected int color() { return 0xFFE8D8FF; }

    /** Called about once a second for every owner, from the elite player tick (ManasCore may not tick passive skills by itself). */
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {}

    protected final boolean masteredBy(ManasSkillInstance instance, LivingEntity entity) { return entity != null && instance.isMastered(entity); }

    protected final EliteMode mode(int mode) {
        List<EliteMode> m = modes();
        return m.get(Math.max(0, Math.min(m.size() - 1, mode)));
    }

    @Override public int getModes(ManasSkillInstance instance) { return modes().size(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int n = modes().size();
        return Math.floorMod(mode + (reverse ? -1 : 1), n);
    }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { return mode(mode).id(); }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        EliteMode m = mode(mode);
        int cd = instance.getCoolDown(mode);
        var name = SkillMsg.mode(m.id(), m.name());
        return cd > 0 ? name.append(" (" + cd + "s)") : name;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive() || !instant(mode)) return;
        if (!EliteSkillsConfig.enabled()) {
            EliteBridge.fail(player, SkillMsg.t("elite.off", "The elite skills are switched off on this server."));
            return;
        }
        EliteMode m = mode(mode);
        if (instance.onCoolDown(mode)) {
            EliteBridge.fail(player, SkillMsg.t("elite.cooldown", "%s is on cooldown (%s s).", m.name(), instance.getCoolDown(mode)));
            return;
        }
        double price = EliteSkillsConfig.cost(m.cost());
        if (!EliteBridge.spendMagicules(player, price)) return;
        boolean done;
        try {
            done = use(instance, player, mode);
        } catch (RuntimeException broken) {
            EliteBridge.refund(player, price);                              // a bug in one power must not cost the player or crash the tick
            throw broken;
        }
        if (!done) {
            EliteBridge.refund(player, price);
            return;
        }
        cooldown(instance, mode, EliteSkillsConfig.cooldown(m.cooldown()));
        EliteBridge.castVfx(player, color());
        grow(instance, player, 1);
    }
}
