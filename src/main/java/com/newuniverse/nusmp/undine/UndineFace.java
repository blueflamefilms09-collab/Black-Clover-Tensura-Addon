package com.newuniverse.nusmp.undine;

/**
 * Why: the faces of the Undine (docs/magic/undine.md): seven expressions. The calm one is painted into the body sheet; the six others are one soft card each, a window of the painted face (eyes, brows,
 * mouth) drawn over the calm one, so a face change is a texture swap and the geometry does not grow: the body model keeps its face quad and the overlay adds one quad only while an expression is shown
 * (counted in {@link UndineLod}). The server stores the face id in three bits of the prop word. Pure data.
 */
public enum UndineFace {
    CALM(0, "calm"), SMILE(1, "smile"), SAD(2, "sad"), STERN(3, "stern"), ANGRY(4, "angry"), SURPRISED(5, "surprised"),
    /** Eyes closed, serene. */
    SERENE(6, "serene");

    private final int id;
    private final String key;

    UndineFace(int id, String key) { this.id = id; this.key = key; }

    public int id() { return id; }

    /** The name in the file names: {@code undine_face_<key>.geo.json}. */
    public String key() { return key; }

    /** Is there an overlay to draw (the calm face is the body's own)? */
    public boolean hasOverlay() { return this != CALM; }

    public static UndineFace of(int id) {
        for (UndineFace f : values()) if (f.id == id) return f;
        return CALM;
    }

    /** The prop word keeps three bits for the face. */
    public static int maxId() { return SERENE.id; }
}
