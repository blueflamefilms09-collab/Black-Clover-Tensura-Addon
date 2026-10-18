package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.ChantRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Rea Laevateinn (Riveria): a three-line chant after which pillars of flame erupt in waves all around you, out to ten blocks (fourteen
 * when mastered). Each wave raises several pillars at random spots in the ring; a foe is hit at most once per wave, takes the pillar's
 * damage and burns. You and your allies stand safe in the middle. Numbers: {@link ChantRules} (pillar*).
 */
public class ReaLaevateinnSkill extends ChantedMagicSkill {
    static final SkillText TEXT = SkillText.of("rea_laevateinn", "Rea Laevateinn",
            "Chanted magic (3 lines). Wave after wave of flame pillars erupt around you and burn every foe that comes close.");

    public ReaLaevateinnSkill() { super(TEXT, ChantRules.Spell.REA_LAEVATEINN); }

    @Override protected int color() { return 0xFFFF7A2A; }

    @Override
    protected MutableComponent chantLine(int line) {
        return switch (line) {
            case 1 -> SkillMsg.chant("rea_laevateinn", 1, "Flames of the dawn, rise from the ground.");
            case 2 -> SkillMsg.chant("rea_laevateinn", 2, "Circle the one who calls you and burn all who approach.");
            default -> SkillMsg.chant("rea_laevateinn", 3, "Rea Laevateinn: let the pillars of fire arise.");
        };
    }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier) {
        double radius = ChantRules.pillarRadius(tier);
        double mastery = masteryFrac(instance);
        int perWave = ChantRules.pillarsPerWave(tier);
        SkillFx.ring(level, player.position(), Math.min(radius * 0.5, 8.0), 0xFF7A2A);

        SkillZones.start(player, ChantRules.pillarInterval(), ChantRules.pillarWaves(tier), (owner, lvl, index) -> {
            Vec3 centre = owner.position();
            Set<LivingEntity> hitThisWave = new HashSet<>();
            for (int i = 0; i < perWave; i++) {
                double angle = (Math.PI * 2.0 / perWave) * i + owner.getRandom().nextDouble() * 0.8;
                double dist = ChantRules.pillarMinRadius() + owner.getRandom().nextDouble() * (radius - ChantRules.pillarMinRadius());
                Vec3 at = centre.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                SkillFx.shape(lvl, VfxShape.FIRE_PILLAR, at, at.add(0, 1, 0), 0xFFFF7A2A, 22, 1.4f);
                for (LivingEntity t : SkillTargets.foes(owner, at, ChantRules.pillarHitRadius())) {
                    if (!hitThisWave.add(t)) continue;
                    if (SkillDamage.hit(owner, t, ChantRules.pillarDamage(tier), mastery, SkillDamage.Kind.MAGIC) > 0f) {
                        t.igniteForSeconds(ChantRules.pillarBurnSeconds(tier));
                    }
                }
            }
            SkillFx.spray(lvl, false, centre.add(0, 0.5, 0), 10, radius / 3, 0.4, radius / 3, 0.02);
            SkillFx.sound(lvl, centre, SoundEvents.BLAZE_SHOOT, 1.6F, 0.5F + 0.1F * index);
        });
        return true;
    }
}
