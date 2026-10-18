package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.Locale;

/**
 * Why: the brief says a mechanic attaches to "a floor, a room, a boss fight or a hazard zone", and the Director plans each of these differently (a floor mechanic is
 * rolled once per floor, a room or zone one per chosen room, a boss one only for the arena of a boss floor), so the scope is data on every mechanic. FLOOR acts on every
 * player of the floor (outside safe rooms); ROOM on the players inside one room; ZONE on a radius round an anchor inside a room; BOSS on the arena while a boss lives.
 */
public enum Scope {
    FLOOR, ROOM, ZONE, BOSS;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Scope fromId(String id) {
        if (id == null) return null;
        for (Scope s : values()) if (s.id().equalsIgnoreCase(id)) return s;
        return null;
    }
}
