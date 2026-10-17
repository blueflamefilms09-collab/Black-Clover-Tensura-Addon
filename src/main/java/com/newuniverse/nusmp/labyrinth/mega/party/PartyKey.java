package com.newuniverse.nusmp.labyrinth.mega.party;

import java.util.Objects;
import java.util.UUID;

/**
 * Why: since 1.58.0 the Mega-Dungeon is instanced per party, so the mod has to recognise "the same party" at any later moment, after a relog, after a restart, on the
 * day a teammate finally joins in. The only thing that is stable, that every teammate agrees on and that a player without a team has as well, is an id: the id of the
 * FTB team when the player is in one, otherwise the player's own id (a party of one). That id IS the key; nothing else (a name, a colour, the member list) may ever be
 * part of it, because every one of those changes while the party stays the same party. The {@link #team()} flag only says which of the two kinds the id is, so the
 * admin's lists can say "team" or "solo"; it is deliberately left out of {@code equals} and {@code hashCode} (the same id is the same party whatever we believed it
 * to be). Pure (no Minecraft type), so the rule that picks the key is tested without a game.
 */
public final class PartyKey {
    private final UUID id;
    private final boolean team;

    private PartyKey(UUID id, boolean team) {
        this.id = Objects.requireNonNull(id, "a party key needs an id");
        this.team = team;
    }

    /** The party of one player on their own: the key is the player's id. */
    public static PartyKey solo(UUID player) { return new PartyKey(player, false); }

    /** The party of a team: the key is the team's id. */
    public static PartyKey team(UUID teamId) { return new PartyKey(teamId, true); }

    /**
     * The key of a player given the id of the team they belong to (null when there is none, or when the team bridge is absent or failed). A team whose id is the player's own
     * id is the player's PERSONAL team (FTB Teams gives every player one, and its id is the player's id): that is no group, so it is the party of one.
     */
    public static PartyKey of(UUID teamId, UUID playerId) {
        if (teamId == null || teamId.equals(playerId)) return solo(playerId);
        return team(teamId);
    }

    /** The stable id: the team's id, or the player's own. */
    public UUID id() { return id; }

    /** Is this a team (two or more people can share it) rather than a player on their own? */
    public boolean team() { return team; }

    /** The form that is saved: {@code t:<uuid>} for a team, {@code p:<uuid>} for a player. */
    public String text() { return (team ? "t:" : "p:") + id; }

    /** Reads {@link #text()} back; a bare uuid is read as a player; null for anything else (a damaged save must not throw). */
    public static PartyKey parse(String text) {
        if (text == null) return null;
        String t = text.trim();
        boolean team = false;
        if (t.startsWith("t:")) {
            team = true;
            t = t.substring(2);
        } else if (t.startsWith("p:")) {
            t = t.substring(2);
        }
        try {
            UUID id = UUID.fromString(t);
            return new PartyKey(id, team);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) { return o instanceof PartyKey k && k.id.equals(id); }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return text(); }
}
