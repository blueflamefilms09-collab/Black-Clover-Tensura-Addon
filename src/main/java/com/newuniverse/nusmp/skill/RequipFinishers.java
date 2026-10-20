package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.requip.BarrageMath;
import com.newuniverse.nusmp.requip.RequipMagicFx;
import com.newuniverse.nusmp.requip.RequipMagicRules;
import com.newuniverse.nusmp.requip.RequipMagicRules.Mode;
import com.newuniverse.nusmp.requip.RequipWeapons;
import com.newuniverse.nusmp.skill.RequipScripts.Script;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: Erza's named finishers and the Dimension Cut. Each is a short animation built from the mod's own effect shapes (the blade, the circle, the ring, the rift) and a few particles, run as a
 * {@link Script} (no camera work: the caster keeps control the whole time). Damage is the balance law's, from the table in {@link Mode}; a boss is capped per cast; allies and tamed or summoned
 * creatures are never hit ({@link SkillTargets}). The names: Circle Sword and Blumenblatt are the Heaven's Wheel techniques' own names; the others are descriptive or the owner's names (docs/requip_magic.md).
 */
final class RequipFinishers {
    private RequipFinishers() {}

    /** The colour of each finisher's blades (ARGB). */
    static int colour(Mode m) {
        return switch (m) {
            case CIRCLE_SWORD -> 0xFFFFD36B;
            case BLUMENBLATT -> 0xFFFFA0C8;
            case TITANIAS_RAMPAGE -> 0xFFFF5050;
            case DANCE_OF_SWORDS -> 0xFFB8E0FF;
            case SEA_OF_SWORDS -> 0xFF6FC3FF;
            case MOONLIGHT_CROSS -> 0xFFE6E6FF;
            default -> 0xFFB07CFF;
        };
    }

    /** Plans the finisher. Returns null when {@code m} is not a finisher. The caller pays and starts it. */
    static Script build(ServerPlayer p, Mode m, double mastery) {
        return switch (m) {
            case CIRCLE_SWORD -> circleSword(p, m, mastery);
            case BLUMENBLATT -> blumenblatt(p, m, mastery);
            case TITANIAS_RAMPAGE -> rampage(p, m, mastery);
            case DANCE_OF_SWORDS -> dance(p, m, mastery);
            case SEA_OF_SWORDS -> sea(p, m, mastery);
            case MOONLIGHT_CROSS -> moonlightCross(p, m, mastery);
            case DIMENSION_CUT -> dimensionCut(p, m, mastery);
            default -> null;
        };
    }

    private static double raw(Mode m, double mastery) { return m.raw() * BarrageMath.affinity(mastery); }

    private static void push(LivingEntity t, Vec3 from, double strength, double up) {
        Vec3 away = t.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0e-4) return;
        away = away.normalize();
        t.setDeltaMovement(away.x * strength, up, away.z * strength);
        t.hurtMarked = true;
    }

    // ------------------------------------------------------------------ Circle Sword (Heaven's Wheel): a ring of swords closes on the spot
    private static Script circleSword(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery);
        LivingEntity look = SkillTargets.lookFoe(p, m.range());
        Vec3 centre = look != null ? look.position() : DMUtil.lookPoint(p, m.range());
        int n = 12;
        double radius = 5.0;
        double[][] ring = BarrageMath.circle(n, radius, 0.0);
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            ServerLevel level = pl.serverLevel();
            SkillFx.shape(level, VfxShape.MAGIC_CIRCLE, centre.add(0, 0.1, 0), centre.add(0, 1.1, 0), argb, 26, (float) radius);
            SkillFx.ring(level, centre, radius, rgb);
            SkillFx.sound(level, centre, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
        });
        for (int k = 0; k < n; k++) {
            final Vec3 pos = centre.add(ring[k][0], 3.2, ring[k][1]);
            s.at(1 + k / 3, pl -> RequipMagicFx.hover(pl.serverLevel(), pos, rgb));
            s.at(2 + k / 3, pl -> RequipMagicFx.hover(pl.serverLevel(), pos.add(0, 0.2, 0), rgb));
            s.at(12, pl -> { SkillFx.shape(pl.serverLevel(), VfxShape.LIGHT_BLADE, pos, centre.add(0, 1.0, 0), argb, 6, 1.0f); s.launched++; });
        }
        s.at(18, pl -> {
            ServerLevel level = pl.serverLevel();
            for (LivingEntity t : SkillTargets.foes(pl, centre, 4.6)) for (int h = 0; h < 6; h++) s.hit(t, raw, 0.0);
            RequipMagicFx.flare(level, centre.add(0, 1.0, 0), rgb);
            SkillFx.shape(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, centre, centre.add(0, 1, 0), argb, 24, 4.6f);
            SkillFx.sound(level, centre, SoundEvents.ANVIL_LAND, 1.2F, 1.3F);
        });
        return s;
    }

    // ------------------------------------------------------------------ Blumenblatt (Heaven's Wheel): the petals of swords open round the caster
    private static Script blumenblatt(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery), reach = m.range();
        int samples = 20;
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            ServerLevel level = pl.serverLevel();
            SkillFx.shape(level, VfxShape.MAGIC_CIRCLE, pl.position().add(0, 0.1, 0), pl.position().add(0, 1.1, 0), argb, 24, 3.0f);
            SkillFx.sound(level, pl.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
        });
        for (int i = 0; i < samples; i++) {
            final double[] pt = BarrageMath.rose(5, i / (double) samples, reach);
            s.at(2 + i / 2, pl -> {
                Vec3 c = pl.position().add(0, 1.2, 0);
                SkillFx.shape(pl.serverLevel(), VfxShape.LIGHT_BLADE, c, c.add(pt[0], 0.3, pt[1]), argb, 7, 0.8f);
                s.launched++;
            });
        }
        s.at(8, pl -> SkillFx.shape(pl.serverLevel(), VfxShape.WIND_RING, pl.position(), pl.position().add(0, 1, 0), argb, 18, (float) (reach * 0.5)));
        for (int wave = 0; wave < 3; wave++) {
            s.at(6 + wave * 4, pl -> {
                for (LivingEntity t : SkillTargets.foes(pl, pl.position(), reach)) { s.hit(t, raw, 0.0); push(t, pl.position(), 0.5, 0.25); }
                SkillFx.ring(pl.serverLevel(), pl.position(), reach * 0.7, rgb);
            });
        }
        return s;
    }

    // ------------------------------------------------------------------ Titania's Rampage: a chain of slashes through the foes
    private static Script rampage(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery);
        List<LivingEntity> chain = new ArrayList<>();
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            LivingEntity look = SkillTargets.lookFoe(pl, m.range());
            if (look != null) chain.add(look);
            for (LivingEntity t : SkillTargets.foes(pl, pl.position(), m.range())) if (!chain.contains(t) && chain.size() < 5) chain.add(t);
            if (!chain.isEmpty()) {                                          // a lunge towards the first foe, not through it
                Vec3 to = chain.get(0).position().subtract(pl.position());
                to = new Vec3(to.x, 0, to.z);
                if (to.length() > 2.5) { Vec3 d = to.normalize().scale(Math.min(1.6, to.length() * 0.2)); pl.setDeltaMovement(d.x, 0.15, d.z); pl.hurtMarked = true; pl.fallDistance = 0; }
            }
            SkillFx.sound(pl.serverLevel(), pl.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.7F);
        });
        Vec3[] last = {null};
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            s.at(3 + i * 3, pl -> {
                ServerLevel level = pl.serverLevel();
                Vec3 from = last[0] != null ? last[0] : pl.getEyePosition().add(0, -0.4, 0);
                LivingEntity t = chain.isEmpty() ? null : chain.get(idx % chain.size());
                Vec3 to = t != null && t.isAlive() ? t.getBoundingBox().getCenter() : DMUtil.lookPoint(pl, 6.0);
                SkillFx.shape(level, VfxShape.WIND_SLASH, from, to, argb, 5, idx == 5 ? 1.8f : 1.2f);
                if (t != null && t.isAlive()) { s.hit(t, idx == 5 ? raw * 1.6 : raw, 0.0); push(t, pl.position(), 0.25, 0.1); }
                RequipMagicFx.land(level, to, rgb);
                SkillFx.sound(level, to, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.3F + idx * 0.08F);
                last[0] = to;
                s.launched++;
            });
        }
        return s;
    }

    // ------------------------------------------------------------------ Dance of the Swords: a turning wall of blades round the caster
    private static Script dance(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery), reach = m.range();
        int n = 16, pulses = 12;
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> SkillFx.sound(pl.serverLevel(), pl.position(), SoundEvents.BEACON_ACTIVATE, 0.9F, 1.9F));
        for (int j = 0; j < pulses; j++) {
            final int pulse = j;
            s.at(j * 5, pl -> {
                ServerLevel level = pl.serverLevel();
                double[][] ring = BarrageMath.circle(n, reach, pulse * 0.55);
                double[][] nextRing = BarrageMath.circle(n, reach, (pulse + 1) * 0.55);
                for (int k = 0; k < n; k++) {
                    Vec3 here = pl.position().add(ring[k][0], 0.8 + 0.5 * ((k + pulse) % 3), ring[k][1]);
                    RequipMagicFx.hover(level, here, rgb);
                    if (k % 2 == pulse % 2) SkillFx.shape(level, VfxShape.LIGHT_BLADE, here, pl.position().add(nextRing[k][0], 0.8 + 0.5 * ((k + pulse + 1) % 3), nextRing[k][1]), argb, 5, 0.7f);
                }
                for (LivingEntity t : SkillTargets.foes(pl, pl.position(), reach + 0.4)) { s.hit(t, raw, 0.0); push(t, pl.position(), 0.25, 0.05); }
                if (pulse % 3 == 0) SkillFx.sound(level, pl.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.4F);
                s.launched += n / 2;
            });
        }
        return s;
    }

    // ------------------------------------------------------------------ Sea of Swords: a field of blades falls on the spot
    private static Script sea(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery), radius = m.range();
        LivingEntity look = SkillTargets.lookFoe(p, 28);
        Vec3 centre = look != null ? look.position() : DMUtil.lookPoint(p, 28);
        Random rnd = new Random(p.getUUID().hashCode() ^ p.level().getGameTime());
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            SkillFx.shape(pl.serverLevel(), VfxShape.MAGIC_CIRCLE, centre.add(0, 0.1, 0), centre.add(0, 1.1, 0), argb, 90, (float) radius);
            SkillFx.ring(pl.serverLevel(), centre, radius, rgb);
            SkillFx.sound(pl.serverLevel(), centre, SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.9F);
        });
        for (int j = 0; j < 20; j++) {
            for (int k = 0; k < 3; k++) {
                double a = rnd.nextDouble() * Math.PI * 2, r = radius * Math.sqrt(rnd.nextDouble());
                final Vec3 down = centre.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                s.at(4 + j * 4, pl -> { SkillFx.shape(pl.serverLevel(), VfxShape.LIGHT_BLADE, down.add(0, 14, 0), down, argb, 5, 0.9f); s.launched++; });
                s.at(9 + j * 4, pl -> RequipMagicFx.land(pl.serverLevel(), down, rgb));
            }
        }
        for (int pulse = 0; pulse < 5; pulse++) {
            s.at(16 + pulse * 16, pl -> {
                for (LivingEntity t : SkillTargets.foes(pl, centre, radius)) s.hit(t, raw, 0.0);
                SkillFx.sound(pl.serverLevel(), centre, SoundEvents.ANVIL_LAND, 0.8F, 1.5F);
            });
        }
        return s;
    }

    // ------------------------------------------------------------------ Moonlight Cross: two crescents cross in front of the caster
    private static Script moonlightCross(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        double raw = raw(m, mastery);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 c = eye.add(look.scale(7.0));
        Vec3 d1 = right.add(up).normalize().scale(m.range() * 0.5), d2 = right.subtract(up).normalize().scale(m.range() * 0.5);
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            SkillFx.shape(pl.serverLevel(), VfxShape.WIND_SLASH, c.subtract(d1), c.add(d1), argb, 6, 2.0f);
            SkillFx.sound(pl.serverLevel(), pl.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        });
        s.at(3, pl -> { for (LivingEntity t : SkillTargets.foesAlong(pl, c.subtract(d1), c.add(d1), 1.8)) s.hit(t, raw, 0.0); s.launched++; });
        s.at(5, pl -> SkillFx.shape(pl.serverLevel(), VfxShape.WIND_SLASH, c.subtract(d2), c.add(d2), argb, 6, 2.0f));
        s.at(8, pl -> { for (LivingEntity t : SkillTargets.foesAlong(pl, c.subtract(d2), c.add(d2), 1.8)) s.hit(t, raw, 0.0); s.launched++; });
        s.at(10, pl -> {
            for (LivingEntity t : SkillTargets.foes(pl, c, 3.5)) { s.hit(t, raw, 0.0); push(t, c, 0.5, 0.2); }
            RequipMagicFx.flare(pl.serverLevel(), c, rgb);
            SkillFx.sound(pl.serverLevel(), c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3F, 0.8F);
        });
        return s;
    }

    // ------------------------------------------------------------------ Dimension Cut: a slash through the pocket
    private static Script dimensionCut(ServerPlayer p, Mode m, double mastery) {
        int argb = colour(m), rgb = argb & 0xFFFFFF;
        ItemStack weapon = RequipWeapons.aimedWeapon(p);
        double wdmg = weapon.isEmpty() ? 7.0 : RequipWeapons.damageOf(weapon);
        double raw = BarrageMath.cutRaw(wdmg, mastery);
        double bypass = BarrageMath.cutBypass(mastery);
        Vec3 look = p.getLookAngle();
        Vec3 from = p.getEyePosition().add(look.scale(0.8)).add(0, -0.3, 0), to = p.getEyePosition().add(look.scale(m.range())).add(0, -0.3, 0);
        Script s = new Script(p, m.id(), mastery);
        s.at(0, pl -> {
            ServerLevel level = pl.serverLevel();
            SkillFx.shape(level, VfxShape.SPATIAL_RIFT, from.lerp(to, 0.5), to, argb, 14, 1.4f);
            SkillFx.shape(level, VfxShape.ANTI_MAGIC_SLASH, from, to, argb, 8, 1.6f);
            RequipMagicFx.seam(level, from, to, rgb);
            SkillFx.sound(level, pl.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F);
        });
        s.at(2, pl -> { for (LivingEntity t : SkillTargets.foesAlong(pl, from, to, 1.6)) s.hit(t, raw, bypass); s.launched++; });
        s.at(6, pl -> {
            for (LivingEntity t : SkillTargets.foesAlong(pl, from, to, 1.6)) s.hit(t, raw * 0.5, bypass);        // the cut closes: an echo
            RequipMagicFx.seam(pl.serverLevel(), from, to, rgb);
        });
        return s;
    }
}
