package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Why: the owner's rule for these lines is that the only particles they may use are the Light and Dark Rukh and the mod's own effect shapes ({@code VfxShape}, the shape names of the catalogue),
 * never vanilla particle spam, and that a line should LOOK like its theme. So every visible thing the engine does goes through these few helpers: a shape drawn through {@code AbilityKit.fx}
 * (which is {@code VfxSpawn}, the mod's effect packet) in the line's colour, and a Rukh burst whose alignment follows the line's background theme (the dark themes get the Dark Rukh, the others the Light
 * Rukh). The Rukh helper already enforces a per-tick particle budget, so a stance or an aura that pulses cannot flood a client. Sounds are free and are played by the callers.
 */
public final class LineFx {
    private LineFx() {}

    /** The themes drawn with the Dark Rukh; every other theme gets the Light Rukh. */
    private static final Set<String> DARK_THEMES = Set.of("void", "blood", "ruin", "iron", "storm");

    /** The opaque colour of the line, as the 0xAARRGGBB the effect packet takes. */
    public static int argb(SkillRoute route) { return 0xFF000000 | (route == null ? 0xFFFFFF : route.color() & 0xFFFFFF); }

    public static RukhFx.Alignment alignment(SkillRoute route) {
        return route != null && DARK_THEMES.contains(route.backgroundTheme() == null ? "" : route.backgroundTheme()) ? RukhFx.Alignment.DARK : RukhFx.Alignment.LIGHT;
    }

    /** Draws one effect shape from {@code from} to {@code to} (a shape that only needs one point is given the same point twice). */
    public static void shape(ServerLevel level, VfxShape shape, SkillRoute route, Vec3 from, Vec3 to, int ticks, float size) {
        AbilityKit.fx(level, shape, from, to, argb(route), ticks, size);
    }

    /** A few Rukh around a point, in the line's alignment. */
    public static void rukh(ServerLevel level, SkillRoute route, Vec3 at, int count) { RukhFx.burst(level, at, alignment(route), count); }

    /** A few Rukh gathered on a creature, in the line's alignment. */
    public static void rukh(ServerLevel level, SkillRoute route, Entity at, int count) { RukhFx.burst(level, at, alignment(route), count); }
}
