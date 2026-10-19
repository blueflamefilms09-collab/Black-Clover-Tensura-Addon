package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: an art executor needs the same handful of facts about the cast it is running (who, which line, which node, at which level, how practised) and the same handful of conveniences (read a number of
 * the node's tables at its level, with the catalogue's base value when the file left it out; hit through the balance law; draw the node's effect). Bundling them in one object means an executor is
 * a method of pure intent, "damage {@code num("damage")} to everyone in a cone", and the rules that apply to every art (numbers from the data, damage through {@code AbilityKit.hit} so the balance law
 * caps it, effects only from the allowed set) are in this one class rather than in 26 copies. It is created per cast, holds no state beyond the cast, and keeps no reference after the cast ends
 * (delayed parts capture the pieces they need, and re-check that the caster is still there with {@code KitClock.present}).
 */
public final class ActiveCtx {
    public final ServerPlayer p;
    public final SkillRoute route;
    public final SkillNode node;
    public final int level;
    public final double mastery;

    public ActiveCtx(ServerPlayer p, SkillRoute route, SkillNode node, int level, double mastery) {
        this.p = p;
        this.route = route;
        this.node = node;
        this.level = Math.max(1, level);
        this.mastery = mastery;
    }

    public ServerLevel world() { return p.serverLevel(); }

    /** A number of the node's tables at its level; the catalogue's base value when the node has no such table. */
    public double num(String key) { return node.value(key, level, LineCatalog.get().base(node.kind(), key, 0.0)); }

    /** {@link #num} rounded to a whole number (ticks, counts). */
    public int whole(String key) { return (int) Math.round(num(key)); }

    /** An enum parameter of the node. */
    public String str(String key, String fallback) { return node.param(key, fallback); }

    /** The effect shape of the node's {@code vfx} parameter. */
    public VfxShape shape() { return LineLookups.shape(node.param("vfx", ""), VfxShape.MAGIC_CIRCLE); }

    public Vec3 eye() { return p.getEyePosition(); }

    public Vec3 look() { return p.getViewVector(1f); }

    /** A hit through the balance law (the technique's base damage, the player's EP and the practice with this art decide the rest). */
    public boolean hit(LivingEntity target, double damage) { return AbilityKit.hit(p, target, (float) damage, mastery); }

    /** The node's effect shape from {@code from} to {@code to}, in the line's colour. */
    public void fx(Vec3 from, Vec3 to, int ticks, float size) { LineFx.shape(world(), shape(), route, from, to, ticks, size); }

    /** A few Rukh at a point. */
    public void rukh(Vec3 at, int count) { LineFx.rukh(world(), route, at, count); }

    /** The centre of a creature's body. */
    public static Vec3 centre(LivingEntity e) { return e.getBoundingBox().getCenter(); }
}
