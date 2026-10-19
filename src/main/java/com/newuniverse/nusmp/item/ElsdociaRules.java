package com.newuniverse.nusmp.item;

import net.minecraft.world.item.ItemStack;

/**
 * 1.97.0: the numbers of Elsdocia, the first Wizard King's sword (docs/elsdocia_legacy.md). The weapon reads them through {@link ElsdociaEvents} (the
 * attribute path). Elsdocia is Genesis grade without attunement, and its base damage is NOT the generic Genesis range of {@code relics/GradeDamage}:
 * a clamp that applies that range must skip a stack for which {@link #isLegacyException(ItemStack)} is true. The values live in {@link ElsdociaMath}
 * (pure, tested); this class is the same constants plus the stack check.
 */
public final class ElsdociaRules {
    private ElsdociaRules() {}

    /** Base damage of the first owner (the explicit exception to the generic Genesis range). */
    public static final int BASE_DAMAGE = ElsdociaMath.BASE_DAMAGE;
    /** Damage gained each time it is passed to a new, distinct Grand Magic rank (or higher) owner. */
    public static final int GRAND_STEP_DAMAGE = ElsdociaMath.GRAND_STEP_DAMAGE;
    public static final int GRAND_RANK_STEP = ElsdociaMath.GRAND_RANK_STEP;
    public static final int TOTAL_MAGICS = ElsdociaMath.TOTAL_MAGICS;
    public static final int MAX_MOTES = ElsdociaMath.MAX_MOTES;
    public static final int BASE_SLOTS = ElsdociaMath.BASE_SLOTS;
    public static final int USE_LOCK_TICKS = ElsdociaMath.USE_LOCK_TICKS;
    public static final double COST_SHARE = ElsdociaMath.COST_SHARE, FLOOR_SHARE = ElsdociaMath.FLOOR_SHARE, COOLDOWN_SHARE = ElsdociaMath.COOLDOWN_SHARE;

    /** Base damage after {@code bonuses} passes to distinct Grand Magic rank owners: 400, 425, 450 ... */
    public static int damage(int bonuses) { return ElsdociaMath.damage(bonuses); }

    /** The attack-damage modifier that shows this damage (the player's own fist is 1). */
    public static double attributeAmount(int bonuses) { return ElsdociaMath.attributeAmount(bonuses); }

    /** Grand Magic Knight or above. */
    public static boolean isGrand(int rankStep) { return ElsdociaMath.isGrand(rankStep); }

    /** True for an Elsdocia stack: the generic Genesis damage range (relics/GradeDamage) must leave its damage alone. */
    public static boolean isLegacyException(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof MagicWeaponItem w && w.kind == MagicWeaponItem.Kind.ELSDOCIA;
    }
}
