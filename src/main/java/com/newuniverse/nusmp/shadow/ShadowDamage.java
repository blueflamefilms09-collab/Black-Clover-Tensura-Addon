package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the Unite abilities (shadow wolves, spikes) hit enemies outside of a page cast, where there is no book and skill instance in hand, but
 * their damage must still follow the balance law, the caster's Existence Points, allies being spared, boss and PvP caps. This finds the caster's
 * Shadow grimoire and hits through it ({@code hurtAs}, darkness element), exactly as the Kotodama words do; without the grimoire it falls back to
 * plain magic damage run through the same balance law.
 */
public final class ShadowDamage {
    private ShadowDamage() {}

    public static void hit(ServerPlayer caster, LivingEntity target, float raw) {
        if (target == caster || target.isAlliedTo(caster) || !target.isAlive()) return;
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(caster).getSkill(NUSkills.BOOK_SHADOW.getId()).orElse(null);
        if (inst != null && inst.getSkill() instanceof GrimoireBook b) {
            b.hurtAs(inst, caster, target, 0, raw, TensuraDamageTypes.DARKNESS_ELEMENTAL);
            return;
        }
        target.hurt(caster.damageSources().indirectMagic(caster, caster), BalanceLaw.damage(caster, target, raw, 0.5));
    }
}
