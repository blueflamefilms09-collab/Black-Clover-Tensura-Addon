package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;

import java.lang.reflect.Method;

/**
 * Why: the personality behaviours that serve the creator (Yandere guarding, frenzy, intercepting, lazy idling beside them) must stop once the Avatar
 * is no longer loyal (a Soul Awakening makes it independent, a betrayal turns it against the creator). The outcome state belongs to the creation
 * system, so this asks the entity's {@code isLoyal()} through one cached reflective lookup instead of a compile-time dependency on its outcome
 * classes; an entity without that method counts as loyal.
 */
public final class AvatarGates {
    private AvatarGates() {}

    private static final Method LOYAL = find();

    private static Method find() {
        try { return AvatarEntity.class.getMethod("isLoyal"); } catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }

    /** True while the Avatar still serves its creator. */
    public static boolean loyal(AvatarEntity avatar) {
        if (LOYAL == null) return true;
        try { return Boolean.TRUE.equals(LOYAL.invoke(avatar)); } catch (ReflectiveOperationException | RuntimeException e) { return true; }
    }
}
