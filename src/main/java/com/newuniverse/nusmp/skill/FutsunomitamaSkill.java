package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.ChantRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Futsunomitama (Mikoto): a two-line chant that turns the spot you look at into a gravity field. The first crush slams every foe to the
 * ground, pins it (short, by the balance law's control caps) and weakens it; while the field lasts it keeps dragging foes toward its
 * centre and crushing them a little every half second. Allies, pets and summons are untouched. Numbers: {@link ChantRules} (gravity*, pin*).
 */
public class FutsunomitamaSkill extends ChantedMagicSkill {
    static final SkillText TEXT = SkillText.of("futsunomitama", "Futsunomitama",
            "Chanted magic (2 lines). A gravity field at the spot you look at crushes foes down, pulls them in and pins them.");

    public FutsunomitamaSkill() { super(TEXT, ChantRules.Spell.FUTSUNOMITAMA); }

    @Override protected int color() { return 0xFFD6A8FF; }

    @Override
    protected MutableComponent chantLine(int line) {
        return line == 1
                ? SkillMsg.chant("futsunomitama", 1, "Heavy heavens, bend toward me.")
                : SkillMsg.chant("futsunomitama", 2, "Futsunomitama: press them to the earth.");
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier) {
        Vec3 c = DMUtil.lookPoint(player, 32);
        double radius = ChantRules.gravityRadius(tier);
        double mastery = masteryFrac(instance);

        for (LivingEntity t : SkillTargets.foes(player, c, radius)) {
            SkillDamage.hit(player, t, ChantRules.gravityDamage(tier), mastery, SkillDamage.Kind.MAGIC);
            t.setDeltaMovement(0, -1.5, 0);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ChantRules.pinTicks(tier)), 6));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
        SkillFx.shape(level, VfxShape.ASH_FORMATION, c, c.add(0, 1, 0), 0xFFB070FF, ChantRules.gravityTicks(tier), (float) radius);
        SkillFx.spray(level, true, c.add(0, 0.5, 0), 12, radius / 3, 0.3, radius / 3, 0.0);
        SkillFx.sound(level, c, SoundEvents.ANVIL_LAND, 1.5F, 0.5F);

        int pulses = ChantRules.pulses(ChantRules.gravityTicks(tier), ChantRules.gravityInterval());
        SkillZones.start(player, ChantRules.gravityInterval(), pulses, (owner, lvl, index) -> {
            if (index == 0) return;                                   // the first crush was dealt above
            for (LivingEntity t : SkillTargets.foes(owner, c, radius)) {
                Vec3 pull = c.subtract(t.position());
                if (pull.lengthSqr() > 0.25) {
                    t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(ChantRules.pullStrength(tier))));
                    t.hurtMarked = true;
                }
                SkillDamage.hit(owner, t, ChantRules.gravityTickDamage(tier), mastery, SkillDamage.Kind.MAGIC);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
            }
            SkillFx.spray(lvl, true, c.add(0, 0.5, 0), 6, radius / 3, 0.3, radius / 3, 0.0);
        });
        return true;
    }
}
