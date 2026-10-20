package com.newuniverse.nusmp.requip;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the particles of the Equip Magic (the circle of a loadout swap, the glow of the pocket opening, the swords hanging round the caster, a glint where a sword lands). The skill package keeps to the
 * mod's own effect shapes and forbids a raw particle there, so the raw particles of this magic live here, as {@link RequipParticles} does for the armours. Every call is bounded.
 */
public final class RequipMagicFx {
    private RequipMagicFx() {}

    /** The magic circle of an instant swap: a turning ring of runes and light at the feet, a column of motes, the flash. */
    public static void swapCircle(ServerLevel level, ServerPlayer p, int rgb) {
        Vec3 c = p.position();
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.FLASH, c.x, c.y + 1.0, c.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 28; i++) {
            double a = i / 28.0 * Mth.TWO_PI, r = 1.25 + (i % 2 == 0 ? 0.0 : 0.25);
            Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.ENCHANT, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0.6, 0, 0.3);
            if (i % 2 == 0) level.sendParticles(RequipParticles.dust(rgb, 1.2f), c.x + Math.cos(a) * r, c.y + 0.12, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 12; i++) Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.END_ROD, c.x, c.y + 0.2 + i * 0.18, c.z, 1, 0.25, 0.02, 0.25, 0.01);
    }

    /** The pocket opening in the air in front of the hand: a small portal that swallows or gives a stack. */
    public static void pocket(ServerLevel level, ServerPlayer p) {
        Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.9)).add(0, -0.35, 0);
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.REVERSE_PORTAL, at.x, at.y, at.z, 22, 0.25, 0.25, 0.25, 0.05);
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.END_ROD, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.02);
    }

    /** One spectral sword hanging in the air: a glint. */
    public static void hover(ServerLevel level, Vec3 at, int rgb) {
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.END_ROD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        level.sendParticles(RequipParticles.dust(rgb, 0.9f), at.x, at.y, at.z, 1, 0.04, 0.04, 0.04, 0.0);
    }

    /** Where a sword lands. */
    public static void land(ServerLevel level, Vec3 at, int rgb) {
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.CRIT, at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0.25);
        level.sendParticles(RequipParticles.dust(rgb, 1.2f), at.x, at.y, at.z, 3, 0.15, 0.15, 0.15, 0.0);
    }

    /** A cut hanging open in the air (the Dimension-Cut): a dark seam with motes pouring out. */
    public static void seam(ServerLevel level, Vec3 from, Vec3 to, int rgb) {
        int n = Math.max(2, Math.min(18, (int) (from.distanceTo(to) * 3)));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.lerp(to, i / (double) n);
            Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.REVERSE_PORTAL, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.08);
            if (i % 2 == 0) level.sendParticles(RequipParticles.dust(rgb, 1.0f), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    /** A burst of light at the heart of a finisher (Circle Sword's centre, the cross of the Moonlight Cross). */
    public static void flare(ServerLevel level, Vec3 at, int rgb) {
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        Fx.spray(level, FxThemes.SKILL_REQUIP, FxOld.END_ROD, at.x, at.y, at.z, 24, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(RequipParticles.dust(rgb, 1.6f), at.x, at.y, at.z, 16, 0.6, 0.6, 0.6, 0.02);
    }
}
