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
 * Wynn Fimbulvetr (Riveria): a three-line chant that calls a blizzard down on the spot you look at. The first strike hits every foe in
 * the area, freezes and slows it; the blizzard then lingers and keeps chilling and hurting whatever stays inside for a few seconds.
 * Allies, pets and summons inside the storm are untouched. Numbers: {@link ChantRules} (blizzard*).
 */
public class WynnFimbulvetrSkill extends ChantedMagicSkill {
    static final SkillText TEXT = SkillText.of("wynn_fimbulvetr", "Wynn Fimbulvetr",
            "Chanted magic (3 lines). A blizzard falls on the spot you look at: it freezes everything in it and keeps chilling whatever stays.");

    public WynnFimbulvetrSkill() { super(TEXT, ChantRules.Spell.WYNN_FIMBULVETR); }

    @Override protected int color() { return 0xFF6FD0FF; }

    @Override
    protected MutableComponent chantLine(int line) {
        return switch (line) {
            case 1 -> SkillMsg.chant("wynn_fimbulvetr", 1, "Winter's breath, come to my call.");
            case 2 -> SkillMsg.chant("wynn_fimbulvetr", 2, "Let the frozen sky fall and the world turn white.");
            default -> SkillMsg.chant("wynn_fimbulvetr", 3, "Wynn Fimbulvetr: freeze everything that lives.");
        };
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier) {
        Vec3 c = DMUtil.lookPoint(player, 40);
        double radius = ChantRules.blizzardRadius(tier);
        double mastery = masteryFrac(instance);

        for (LivingEntity t : SkillTargets.foes(player, c, radius)) {
            SkillDamage.hit(player, t, ChantRules.blizzardDamage(tier), mastery, SkillDamage.Kind.MAGIC);
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + ChantRules.freezeTicks(tier));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ChantRules.slowTicks(tier)), 3));
        }
        SkillFx.follow(level, VfxShape.WIND_TORNADO, player, c, 0xFFCFF3FF, ChantRules.blizzardTicks(tier), (float) (radius * 0.5));
        SkillFx.shape(level, VfxShape.WATER_RING, c.add(0, 0.1, 0), c.add(0, 1, 0), 0xFF9ADFFF, 30, (float) (radius * 0.4));
        SkillFx.spray(level, false, c.add(0, 1.5, 0), 12, radius / 3, 1.5, radius / 3, 0.05);
        SkillFx.sound(level, c, SoundEvents.GLASS_BREAK, 2.0F, 0.5F);

        // the blizzard lingers: every second it chills and hurts whatever is still inside
        int pulses = ChantRules.pulses(ChantRules.blizzardTicks(tier), ChantRules.blizzardInterval()) + 1;   // + the instant first strike
        SkillZones.start(player, ChantRules.blizzardInterval(), pulses, (owner, lvl, index) -> {
            if (index == 0) return;                                   // the first strike was dealt above
            for (LivingEntity t : SkillTargets.foes(owner, c, radius)) {
                SkillDamage.hit(owner, t, ChantRules.blizzardTickDamage(tier), mastery, SkillDamage.Kind.MAGIC);
                t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 120, t.getTicksFrozen() + 80));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            }
            SkillFx.spray(lvl, false, c.add(0, 1.5, 0), 8, radius / 3, 1.5, radius / 3, 0.04);
        });
        return true;
    }
}
