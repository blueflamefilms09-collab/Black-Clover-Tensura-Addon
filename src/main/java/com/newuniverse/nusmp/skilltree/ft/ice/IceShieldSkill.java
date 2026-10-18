package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * Why: Ice-Make: Shield is, in the story, a large barrier of ice that Gray raises in front of himself to protect whatever is behind it, and it is the defensive heart of the STANDING branch.
 * The game cannot place blocks for this route (the owner's rule), so the barrier is a wall the server keeps in its head for the node's {@code life} (never over 10 seconds): an oriented box
 * ({@link IceNumbers.Wall}) set in front of the caster and turned the way they face, checked on EVERY tick against every projectile near it, because arrows fly several blocks a tick
 * and a check every few ticks would let them through. A projectile that is inside the box or about to cross it is deleted unless it was fired by the caster or someone in their party, so the
 * wall protects the group from outside and never blocks its own archers; walking foes are not stopped, because a wall that traps people is hard control and the owner reserves that for one
 * lock per twelve seconds. The caster also gains Absorption (the shield "takes" some of the next blows), sized by the node level. It takes one of the caster's four construct slots, is refused
 * before it costs anything when they are full, and the drawing is a row of the mod's Wedge Field effects (upright fans of ice) standing along the wall for its whole life, with one eruption of wedges when it rises.
 */
public final class IceShieldSkill extends FtTreeSkill {
    /** How far in front of the caster the wall stands, and how thick it is (half the depth of its box). */
    public static final double AHEAD = 2.2, HALF_DEPTH = 0.8;

    public IceShieldSkill() { super(IceRoute.ID, IceRoute.SHIELD, "block/blue_ice", IceNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        double width = num("width", lv), height = num("height", lv);
        int life = IceNumbers.clampLife(whole("life", lv)), absorb = whole("absorb", lv);
        if (!IceConstructs.hasRoom(p)) {
            AbilityKit.say(p, "Four ice constructs already stand.");
            return false;
        }
        if (!pay(p, lv)) return false;
        IceConstructs.record(p, life);

        ServerLevel level = p.serverLevel();
        Vec3 facing = FtWorld.flatLook(p), middle = p.position().add(facing.scale(AHEAD));
        IceNumbers.Wall wall = new IceNumbers.Wall(middle.x, middle.y, middle.z, facing.x, facing.z, width / 2.0, HALF_DEPTH, height);
        Set<UUID> friends = FtWorld.friendIds(p);
        if (absorb >= 4) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, life + 20, absorb / 4 - 1, true, false, true));

        draw(level, wall, life);
        FtWorld.soundAt(level, middle, SoundEvents.GLASS_PLACE, 1.0f, 0.6f);
        FtWorld.soundAt(level, middle, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 0.8f);
        FtClock.zone(level, life, 1, age -> {
            if (!FtClock.present(p, level)) return;
            IceConstructs.erase(level, friends, wall);
        });
        return true;
    }

    /** The drawing of the wall: a field of upright ice wedges every two blocks along its width, standing for its whole life, and one eruption of wedges from its middle when it rises. */
    private static void draw(ServerLevel level, IceNumbers.Wall wall, int life) {
        double rx = -wall.fz(), rz = wall.fx();
        int n = Math.max(2, (int) Math.round(wall.halfWidth()));
        for (int i = 0; i < n; i++) {
            double u = -wall.halfWidth() + (2.0 * wall.halfWidth()) * i / (n - 1);
            Vec3 at = new Vec3(wall.cx() + rx * u, wall.cy(), wall.cz() + rz * u);
            AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX2, at, at, IceRoute.ICE, life, 1.2f);
        }
        Vec3 middle = new Vec3(wall.cx(), wall.cy(), wall.cz());
        AbilityKit.fx(level, VfxShape.ICE_WEDGE_FX3, middle, middle.add(0, 1.0, 0), IceRoute.ICE, 28, (float) (wall.height() / 3.0));
    }
}
