package com.newuniverse.nusmp.entity.veldanava.skills;

import net.minecraft.world.entity.LivingEntity;

/**
 * Why: several Veldanava skills hand their owner an immunity that OTHER skills' code has to respect (a pull, a cage, a mind scan, a debuff). The question is always the same - "does this creature
 * carry that immunity right now?" - so it is answered here once, from the one int the owner's skill keeps on the player ({@link JudgmentState}), with no skill walk. Only players can carry one; every
 * method returns false at once for anything else. Server thread only.
 *
 * <p>Israfil (code 2): Spatial Domination (toggle 0) = no fall damage, no displacement, no entrapment; Labyrinthian Mind (toggle 2) = no mind-altering effect, no presence scan, no prediction.
 */
public final class VeldanavaGuard {
    private VeldanavaGuard() {}

    /** Israfil's skill code in the mask. */
    public static final int ISRAFIL = 2;

    /** No fall damage, no pull or swap, no cage or binding: Israfil's Spatial Domination is on. */
    public static boolean spatiallyFree(LivingEntity e) { return JudgmentState.onFor(e, ISRAFIL, 0); }

    /** Cannot be pulled, swapped, thrown or teleported by another creature's ability. */
    public static boolean displacementImmune(LivingEntity e) { return spatiallyFree(e); }

    /** Cannot be caged, bound or held in place. */
    public static boolean entrapmentImmune(LivingEntity e) { return spatiallyFree(e); }

    /** No mind-altering effect lands, and presence scans and predictions skip this creature: Israfil's Labyrinthian Mind is on. */
    public static boolean mindImmune(LivingEntity e) { return JudgmentState.onFor(e, ISRAFIL, 2); }
}
