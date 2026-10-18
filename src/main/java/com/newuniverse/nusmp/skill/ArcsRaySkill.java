package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.ChantRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Arcs Ray (Lefiya): a one-line chant, a piercing beam of light along your line of sight that strikes every foe on the way and stops at
 * the first wall. Light burns the undead half again as hard. The cheapest and fastest of the chants. Numbers: {@link ChantRules} (ray*).
 */
public class ArcsRaySkill extends ChantedMagicSkill {
    static final SkillText TEXT = SkillText.of("arcs_ray", "Arcs Ray",
            "Chanted magic (1 line). A piercing beam of light strikes everything in a line, and burns the undead even harder.");

    public ArcsRaySkill() { super(TEXT, ChantRules.Spell.ARCS_RAY); }

    @Override protected int color() { return 0xFFFFFFD2; }

    @Override
    protected MutableComponent chantLine(int line) {
        return SkillMsg.chant("arcs_ray", 1, "Arcs Ray: pierce them, arrow of light.");
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier) {
        Vec3 start = player.getEyePosition().add(0, -0.15, 0);
        Vec3 end = DMUtil.lookPoint(player, ChantRules.rayRange(tier));
        double mastery = masteryFrac(instance);
        for (LivingEntity t : SkillTargets.foesAlong(player, start, end, ChantRules.rayWidth())) {
            double dmg = ChantRules.rayDamage(tier) * (t.getType().is(EntityTypeTags.UNDEAD) ? ChantRules.rayUndeadFactor() : 1.0);
            SkillDamage.hit(player, t, dmg, mastery, SkillDamage.Kind.MAGIC);
            SkillFx.spray(level, false, t.position().add(0, t.getBbHeight() * 0.6, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }
        SkillFx.beam(level, start, end, false);
        SkillFx.shape(level, VfxShape.MIRROR_RAY, start, end, 0xFFFFFFD2, 12, 1.0f);
        SkillFx.shape(level, VfxShape.LIGHT_FLARE, end, end, 0xFFFFFFD2, 10, 0.8f);
        SkillFx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 1.5F);
        return true;
    }
}
