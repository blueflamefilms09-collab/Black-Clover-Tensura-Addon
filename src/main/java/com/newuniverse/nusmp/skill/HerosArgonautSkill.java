package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.UltimateClass;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ChargeRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Hero's Argonaut: the Ultimate evolution of {@link ArgonautSkill} (unlocked automatically by mastering it). Not an alias: it charges TEN
 * bells instead of five, its cone strike hits about three times harder and reaches further, and it adds a second mode, Hero's Beam: the
 * same charge, released as a piercing beam of light (12 to 42 blocks, growing with the charge) that strikes every creature along it
 * and stops at the first wall. Mode order is append-only (Strike stays mode 0).
 */
public class HerosArgonautSkill extends ArgonautSkill {
    static final SkillText HERO_TEXT = SkillText.of("heros_argonaut", "Bell Cranel, Hero Argonaut",
            "Ultimate. Bell Cranel, reincarnation of Argonaut, the Hero of the Beginning. Charge up to ten bells while walking and strike with triple power, or release the charge as a piercing beam, a pseudo-Afterglow that burns what it pierces. Also Firebolt, the Charged Firebolt and Argo Vesta.");

    public HerosArgonautSkill() { super(SkillType.ULTIMATE, HERO_TEXT, true); }
    @Override public UltimateClass ultimateClass() { return UltimateClass.LORD; }      // docs/ultimate_classes.md

    // modes (Hero's Strike 0, Hero's Beam 1, Firebolt 2 with the merge on): ArgonautSkill.getModes / nextMode / getModeId / getModeName

    @Override
    protected int release(ManasSkillInstance instance, ServerPlayer player, int charge, int mode) {
        if (kind(mode) != ChargeRules.Strike.BEAM) return super.release(instance, player, charge, mode);

        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);
        double damage = ChargeRules.damage(NUConfig.ARGONAUT_DAMAGE_PER_CHARGE.get(), charge, tier, instance.isMastered(player))
                * ChargeRules.beamFactor();
        Vec3 from = player.getEyePosition().add(0, -0.2, 0);
        Vec3 to = DMUtil.lookPoint(player, ChargeRules.beamLength(charge));      // a wall ends the beam
        int hits = 0;
        int burn = fireSeconds(charge, ChargeRules.Strike.BEAM);                                          // Firebolt folded in: a charged beam burns what it pierces
        for (LivingEntity t : SkillTargets.foesAlong(player, from, to, ChargeRules.beamRadius(charge))) {
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance), SkillDamage.Kind.MAGIC, instance) > 0f) {
                if (burn > 0) t.igniteForSeconds(burn);
                hits++;
            }
        }
        SkillFx.beam(level, from, to, false);
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, from, to, 0xFFFFF2B0, 14, 2.0f + charge * 0.2f);
        SkillAnimeFx.heroBeam(level, from, to, charge);                                   // VFX wave 3: the hero's beam with its bell rings
        SkillFx.shape(level, VfxShape.LIGHT_FLARE, to, to, 0xFFFFE9A0, 12, 1.5f);
        SkillFx.sound(level, player.position(), SoundEvents.BELL_RESONATE, 2.0F, 0.7F);
        SkillFx.sound(level, player.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.6F);
        return hits;
    }
}
