package com.newuniverse.nusmp.grimoire;

/** The ornament pattern stamped on a grimoire's covers (texture ornament_<name>). */
public enum BookMotif {
    FILIGREE, ORNATE, WHEELS, STARS, LATTICE, FLORAL, STRAPS, TATTERED, PLAIN;

    public String texture() { return "ornament_" + name().toLowerCase(); }

    public String displayName() {
        return switch (this) {
            case FILIGREE -> "gold filigree"; case ORNATE -> "gilded ornament"; case WHEELS -> "radial wheels"; case STARS -> "starbursts";
            case LATTICE -> "lattice"; case FLORAL -> "flowers"; case STRAPS -> "strapped"; case TATTERED -> "tattered"; case PLAIN -> "plain";
        };
    }
}
