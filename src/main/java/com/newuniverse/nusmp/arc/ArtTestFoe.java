package com.newuniverse.nusmp.arc;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: the CI self-tests of the Goku and Sinbad abilities (keys {@code goku.boss.arts}, {@code sinbad.boss.arts}) need something a boss counts as a foe without a player: a tamed wolf whose owner is a
 * random id (the boss rules count the summons of players as foes). It has a huge health pool so a test can hit it many times, stands where the test puts it, looks at a point, and is discarded by the
 * caller. Never used by the game. Cost: one entity made and discarded inside one test.
 */
public final class ArtTestFoe {
    private ArtTestFoe() {}

    /** A foe at {@code at}, looking at {@code lookAt}; null when the wolf could not be made. The caller discards it. */
    public static LivingEntity make(ServerLevel level, Vec3 at, Vec3 lookAt) {
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) return null;
        wolf.setPos(at.x, at.y, at.z);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.setTame(true, false);
        if (wolf.getAttribute(Attributes.MAX_HEALTH) != null) wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
        wolf.setHealth(wolf.getMaxHealth());
        wolf.lookAt(EntityAnchorArgument.Anchor.EYES, lookAt);
        wolf.setYHeadRot(wolf.getYRot());
        level.addFreshEntity(wolf);
        return wolf;
    }
}
