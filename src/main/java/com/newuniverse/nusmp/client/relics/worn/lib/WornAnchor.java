package com.newuniverse.nusmp.client.relics.worn.lib;

/** The body part of the player model a worn model is attached to (its pivot is the model's origin). Pure; the names are the ones of the model files and the catalog. */
public enum WornAnchor {
    HEAD("head"), BODY("body"), RIGHT_ARM("rarm"), LEFT_ARM("larm"), RIGHT_LEG("rleg"), LEFT_LEG("lleg");

    public final String key;

    WornAnchor(String key) { this.key = key; }

    /** The anchor of a file or catalog name ("head", "rarm" ...), or null. */
    public static WornAnchor byKey(String key) {
        if (key == null) return null;
        for (WornAnchor a : values()) if (a.key.equals(key.trim())) return a;
        return null;
    }
}
