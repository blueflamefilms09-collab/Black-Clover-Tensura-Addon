package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the owner's rule for these lines is that the only particles they may use are the Light and Dark Rukh and the mod's own effect shapes ({@code VfxShape}, the shape names of the catalogue),
 * never vanilla particle spam, and that a line should LOOK like its theme. So every visible thing the engine does goes through these few helpers: a shape drawn through {@code AbilityKit.fx}
 * (which is {@code VfxSpawn}, the mod's effect packet) in the line's colour, and a Rukh burst whose alignment follows the line's background theme (the dark themes get the Dark Rukh, the others the Light
 * Rukh). The Rukh helper already enforces a per-tick particle budget, so a stance or an aura that pulses cannot flood a client. Sounds are free and are played by the callers.
 */
public final class LineFx {
    private LineFx() {}

    /** The themes drawn in the dark flavour of the skill-tree particle theme; every other theme gets the light one. */
    private static final Set<String> DARK_THEMES = Set.of("void", "blood", "ruin", "iron", "storm");

    /** The opaque colour of the line, as the 0xAARRGGBB the effect packet takes. */
    public static int argb(SkillRoute route) { return 0xFF000000 | (route == null ? 0xFFFFFF : route.color() & 0xFFFFFF); }

    public static FxTone alignment(SkillRoute route) {
        return route != null && DARK_THEMES.contains(route.backgroundTheme() == null ? "" : route.backgroundTheme()) ? FxTone.DARK : FxTone.LIGHT;
    }

    /** Draws one effect shape from {@code from} to {@code to} (a shape that only needs one point is given the same point twice). */
    public static void shape(ServerLevel level, VfxShape shape, SkillRoute route, Vec3 from, Vec3 to, int ticks, float size) {
        AbilityKit.fx(level, shape, from, to, argb(route), ticks, size);
    }

    /** A few particles around a point, in the line's tone. */
    public static void rukh(ServerLevel level, SkillRoute route, Vec3 at, int count) { Fx.burst(level, FxThemes.SKILL_TREE, at, alignment(route), count); }

    /** A few particles gathered on a creature, in the line's tone. */
    public static void rukh(ServerLevel level, SkillRoute route, Entity at, int count) { Fx.burst(level, FxThemes.SKILL_TREE, at, alignment(route), count); }
}
