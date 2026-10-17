package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Why: the monsters of this mod's world come in two kinds of mind. Vanilla mobs and our own think with goal lists; Tensura's think with
 * brains (SmartBrainLib behaviours reading memories), and a brain that is not asked politely undoes an order a pass later. The rest of the
 * mod already learned the polite way to steer both from outside (the hive of the Tempest Mirage, the born creatures of the Sentient Dungeon):
 * walk with the navigation after erasing the brain's own walk target, aim by setting the plain target slot AND the brain's attack-target memory,
 * let go by clearing both. Every brain call sits in a try block because a mob without a brain, or with a different one, simply has no such
 * memory: that is not an error, it means the plain slot was enough. This is that recipe in one place for the Director's hunters and the
 * wandering bosses.
 */
final class MobSteer {
    private MobSteer() {}

    /** Walk to a point at a speed modifier. */
    static boolean walk(Mob m, double x, double y, double z, double speed) {
        eraseWalkTarget(m);
        return m.getNavigation().moveTo(x, y, z, speed);
    }

    /** Walk after an entity (the navigation re-paths as it moves). */
    static boolean follow(Mob m, Entity target, double speed) {
        eraseWalkTarget(m);
        return m.getNavigation().moveTo(target, speed);
    }

    /** Tells the mob who it is after, in the plain slot and in the brain when it has the memory. */
    static void aim(Mob m, LivingEntity target) {
        try {
            m.setTarget(target);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } catch (RuntimeException | LinkageError ignored) {
            // the walk does not depend on it
        }
    }

    /** Lets go of whoever the mob was after, so it does not chase on its own. */
    static void release(Mob m) {
        try {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (RuntimeException | LinkageError ignored) {
            // nothing more to undo
        }
    }

    private static void eraseWalkTarget(Mob m) {
        try {
            m.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        } catch (RuntimeException | LinkageError ignored) {
            // a mob without a brain has no walk to erase
        }
    }
}
