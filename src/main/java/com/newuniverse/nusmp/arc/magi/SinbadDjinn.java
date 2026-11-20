package com.newuniverse.nusmp.arc.magi;

/**
 * Why: Sinbad's seven djinn (Magi): the vessel each one lives in, its tribe title and its colour. Canon: he wears ONE djinn at a time and switches fast, which is the core of the fight
 * (see {@link SinbadEquip}). Pure (no game types), so the tests read it. The tribe titles and vessels come from the lore file written by another session ({@code docs/sinbad_boss_lore.md});
 * no wiki was fetched while porting. {@code argb} is for the effect packets, {@link #rgb} for the dust telegraphs.
 */
enum SinbadDjinn {
    NONE("none", "Unequipped", "", "", 0xFFE8D9A8),
    BAAL("baal", "Baal", "Wrath and Heroes", "sword", 0xFF5BB8FF),                                    // lightning
    VALEFOR("valefor", "Valefor", "Falsehood and Confidence", "gold necklace", 0xFFA8E6FF),          // ice
    ZEPAR("zepar", "Zepar", "Spirit and Puppetry", "middle-finger ring", 0xFFD7A6FF),                // sound
    FOCALOR("focalor", "Focalor", "Control and Obedience", "right bracelet", 0xFF9BF0C0),            // wind
    VEPAR("vepar", "Vepar", "", "index-finger ring", 0xFFC0C8D8),                                    // magnetism / swords
    CROCELL("crocell", "Crocell", "", "left bracelet", 0xFFFFF2A8),                                  // light, saber
    FURFUR("furfur", "Furfur", "Ecstasy and Rending Darkness", "silver necklace", 0xFF6A3A8A);       // darkness

    final String id;
    final String displayName;
    /** Tribe title, empty when the lore gives none. */
    final String tribe;
    final String vessel;
    final int argb;

    SinbadDjinn(String id, String displayName, String tribe, String vessel, int argb) {
        this.id = id;
        this.displayName = displayName;
        this.tribe = tribe;
        this.vessel = vessel;
        this.argb = argb;
    }

    int rgb() { return argb & 0xFFFFFF; }

    /** The djinn with this id; {@link #NONE} for an unknown or empty id (a save from another version). */
    static SinbadDjinn byId(String id) {
        for (SinbadDjinn d : values()) if (d.id.equals(id)) return d;
        return NONE;
    }
}
