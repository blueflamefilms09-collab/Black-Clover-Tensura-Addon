package com.newuniverse.nusmp.boss.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: a boss that is spawned OUTSIDE the dungeons (by an egg, a command or a summon in the overworld) meets a world the dungeon halls never had: the colossal Guardian and the other big bosses are
 * several blocks tall, so they stand half inside a tree, a cliff or a cave wall and suffocate ({@code in_wall}), are crammed, fall from a spawn height, wade into water, touch a cactus, a
 * berry bush or a stalagmite, stand on magma, freeze in powder snow, are struck by a storm's lightning, or (the undead) burn in daylight. None of that has an attacker, none of it is part of
 * the fight, and to the player it looks like the boss losing health "randomly". The dungeons are built so that none of it can happen there, which is why it only shows outside. So a boss
 * of this mod ({@link BossIdentity}) takes none of this world damage: the damage event is cancelled for exactly these damage types, and only when nothing attacked (a player's own fire, fall-damage
 * trap or drowning spell carries an attacker and stays a real blow). Real fights are untouched: lava, void, {@code /kill}, magic, weapons and every boss ability keep working. The rule is one
 * switch ({@code bossRules.environmentImmune}, default on) so the old behaviour is reachable.
 */
public final class BossEnvironment {
    private BossEnvironment() {}

    /** Is this blow the world's own damage on a boss (no attacker, an environmental type), which a boss shrugs off? */
    public static boolean shrugsOff(LivingEntity boss, DamageSource source) {
        if (source == null || source.getEntity() != null || source.getDirectEntity() != null) return false;
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.DRY_OUT) || source.is(DamageTypes.FREEZE) || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.LIGHTNING_BOLT)) return true;
        if ((source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE)) && boss.getType().is(EntityTypeTags.UNDEAD)) {          // sunlight: the undead's daylight burn
            BlockPos at = boss.blockPosition();
            return boss.level().isDay() && boss.level().canSeeSky(at);
        }
        return false;
    }
}
