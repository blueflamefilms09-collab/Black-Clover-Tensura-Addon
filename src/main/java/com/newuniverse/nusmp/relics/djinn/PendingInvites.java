package com.newuniverse.nusmp.relics.djinn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Household invitations waiting to be answered: an owner offers their Djinn to an ally, and the ally has a minute to accept (nobody is made to carry
 * a Djinn: it would stop them taking a Spirit or a Devil). Memory only; a restart forgets them, which is the safe side.
 */
final class PendingInvites {
    private PendingInvites() {}

    static final long WINDOW = 1200L;

    private record Invite(UUID owner, long expires) {}

    private static final Map<UUID, Invite> BY_ALLY = new HashMap<>();

    static void offer(UUID ally, UUID owner, long now) { BY_ALLY.put(ally, new Invite(owner, now + WINDOW)); }

    /** The owner who invited this ally and has not been forgotten or timed out, or null. Consumes the invitation. */
    static UUID take(UUID ally, long now) {
        Invite i = BY_ALLY.remove(ally);
        return i == null || now > i.expires() ? null : i.owner();
    }

    static boolean pending(UUID ally, long now) {
        Invite i = BY_ALLY.get(ally);
        return i != null && now <= i.expires();
    }

    /** A player left, or died: their offers and invitations go with them. */
    static void forget(UUID player) {
        BY_ALLY.remove(player);
        BY_ALLY.values().removeIf(i -> i.owner().equals(player));
    }

    static void clear() { BY_ALLY.clear(); }
}
