package com.newuniverse.nusmp.core.xform;

import net.minecraft.resources.ResourceLocation;

/**
 * Why: the one documented answer to "what happens to a transformation when ...", as pure functions so it can be tested without a game (docs/transformation_fixes.md).
 * DEFAULT POLICY: a form ends when its wearer dies and NOTHING resumes in the new body ({@link Reason#DEATH}, {@link Reason#RESPAWN}); the server setting {@code transformations.resetOnDeath}
 * (default true) turns that off. Coming back from the End is not a death ({@link Reason#END_RETURN}); a change of dimension, a logout and a login are decided by each system itself (a toggled armour
 * survives a relog, a Saiyan form does not survive a dimension change): the guard only reverts what a death must revert.
 */
public final class TransformPolicy {
    private TransformPolicy() {}

    /** The moments of a body's life the guard cares about. */
    public enum Reason { DEATH, RESPAWN, END_RETURN, DIMENSION, LOGOUT, LOGIN }

    /** True when the guard must end every form now. */
    public static boolean endsForms(Reason why, boolean resetOnDeath) {
        return resetOnDeath && (why == Reason.DEATH || why == Reason.RESPAWN);
    }

    /** True when the guard leaves the decision to the owning system (nothing is reverted by the guard). */
    public static boolean ownerDecides(Reason why) {
        return why == Reason.END_RETURN || why == Reason.DIMENSION || why == Reason.LOGOUT || why == Reason.LOGIN;
    }

    /** True when the flags copied from the old body to the new one (a clone after a death) must be stripped. */
    public static boolean stripsClone(boolean wasDeath, boolean resetOnDeath) { return wasDeath && resetOnDeath; }

    /** A modifier id the guard may sweep: this mod's own namespace only (never another mod's, never a vanilla id). */
    public static boolean sweepable(ResourceLocation id) { return id != null && "anime_multiverse".equals(id.getNamespace()); }

    /** A persistent-data key the guard may strip: this mod's own {@code nusmp_} flags only. */
    public static boolean strippable(String key) { return key != null && key.startsWith("nusmp_") && key.length() > 6; }
}
