package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: the shape of the warning a telegraphed attack draws on the floor. It is a small closed set because each one is drawn (and tested) by {@link TelegraphGeometry}: a CIRCLE around a point
 * (a slam, a leap's landing), a RING (a circle with an inner twin, the pulse of an aura), a CONE (a cleave, a bash), a LINE (a dash, a dive, a pull). NONE draws nothing (a heal, a summon).
 * The ids are the ones the data file writes.
 */
public enum TelegraphShape {
    NONE("none"), CIRCLE("circle"), RING("ring"), CONE("cone"), LINE("line");

    private final String id;

    TelegraphShape(String id) { this.id = id; }

    public String id() { return id; }

    /** The shape with this id (any case), or null for an unknown one. */
    public static TelegraphShape of(String id) {
        if (id == null) return null;
        for (TelegraphShape s : values()) if (s.id.equalsIgnoreCase(id.trim())) return s;
        return null;
    }
}
