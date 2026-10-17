package com.newuniverse.nusmp.avatar;

import java.util.UUID;

/**
 * Why: the owner's brief says the Avatar must be a native Tensura subordinate that "responds to Follow/Stay commands" and whose bond is severed by a betrayal.
 * Extending the vanilla {@code TamableAnimal} makes Tensura treat it as an {@code ISubordinate} (Tensura's own mixin adds that interface to every tamable), but
 * Tensura also keeps its own record of who a subordinate belongs to (the "permanent owner" in its existence storage), and its Stay / Follow command is a state
 * on the mob. This class keeps those in step with what the Avatar actually is: while it serves its creator the record names the creator; after a betrayal or an
 * awakening the record is cleared (the bond is severed); a Stay given by Tensura's command, by the vanilla "ordered to sit" flag or by this mod's stance menu
 * means the same thing everywhere. Every Tensura call goes through {@link TensuraBridge} and is guarded, and the whole thing can be switched off
 * ({@code avatar.tensuraBond}).
 */
public final class TensuraBond {
    private TensuraBond() {}

    /** Brings Tensura's owner record in line with the Avatar's loyalty (cheap: it writes only when the value differs). Server side only. */
    public static void sync(AvatarEntity a) {
        if (a == null || a.level().isClientSide || !AvatarAiConfig.tensuraBond()) return;
        UUID wanted = a.isLoyal() ? a.creatorUuid() : null;
        TensuraBridge.setPermanentOwner(a, wanted);
    }

    /**
     * Is the Avatar told to stay put: by the vanilla flag or by Tensura's Stay command? (This mod's Stay stance IS the vanilla flag; the Avatar's stance is derived from
     * the flag and from Tensura's own state, never kept apart, so a Tensura Follow command, which clears both, cancels it too.)
     */
    public static boolean stays(AvatarEntity a) {
        return a.isOrderedToSit() || TensuraBridge.orderedToStay(a);
    }

    /** Does Tensura's own command say stay (read through its helper, guarded; false when the bond is switched off)? */
    public static boolean tensuraStays(AvatarEntity a) { return AvatarAiConfig.tensuraBond() && TensuraBridge.orderedToStay(a); }

    /** Orders the Avatar to stay (or to follow again) in every system at once. */
    public static void order(AvatarEntity a, boolean stay) {
        a.setOrderedToSit(stay);
        TensuraBridge.orderStay(a, stay);
    }
}
