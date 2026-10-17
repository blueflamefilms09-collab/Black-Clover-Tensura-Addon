package com.newuniverse.nusmp.avatar;

/**
 * Why: the creator tells an Avatar what to do with a handful of commands: stay at its side and fight (Follow, the default), stand where it is (Stay, which is
 * also what Tensura's own Stay command and the vanilla "ordered to sit" flag mean), or go into the Labyrinth on its own and come back with loot (Delve). One small
 * enum is the saved form of that choice (stored by name in the Avatar's NBT, constants only appended), shared by the inventory menu's buttons, the goals that
 * look at it and the command. Pure.
 */
public enum Stance {
    FOLLOW, STAY, DELVE;

    public static Stance byName(String name) {
        if (name != null) for (Stance s : values()) if (s.name().equalsIgnoreCase(name)) return s;
        return FOLLOW;
    }

    public static Stance byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : FOLLOW; }
}
