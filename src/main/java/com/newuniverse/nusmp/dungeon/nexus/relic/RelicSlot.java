package com.newuniverse.nusmp.dungeon.nexus.relic;

import net.minecraft.util.StringRepresentable;

/** What a relic pedestal ({@link ArtifactBlock}) holds: nothing, or one of the five relics. The serialized names are the relics' item ids. */
public enum RelicSlot implements StringRepresentable {
    EMPTY("empty", null),
    GLUTTONS_CHALICE("glutton_chalice", Relic.GLUTTONS_CHALICE),
    MANTLE_OF_THE_SCAVENGER("mantle_of_the_scavenger", Relic.MANTLE_OF_THE_SCAVENGER),
    BAND_OF_THE_SPATIAL_ANCHOR("band_of_the_spatial_anchor", Relic.BAND_OF_THE_SPATIAL_ANCHOR),
    DEVIL_BOUND_SHEATH("devil_bound_sheath", Relic.DEVIL_BOUND_SHEATH),
    GAMBLERS_GRIMOIRE_PAGE("gamblers_grimoire_page", Relic.GAMBLERS_GRIMOIRE_PAGE);

    private final String name;
    private final Relic relic;

    RelicSlot(String name, Relic relic) {
        this.name = name;
        this.relic = relic;
    }

    @Override
    public String getSerializedName() { return name; }

    /** The relic held, or null when empty. */
    public Relic relic() { return relic; }

    public static RelicSlot of(Relic relic) {
        for (RelicSlot s : values()) if (s.relic == relic) return s;
        return EMPTY;
    }
}
