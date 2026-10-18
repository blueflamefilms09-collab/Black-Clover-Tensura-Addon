package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: a forge recipe belongs to one of four kinds of work, and the specialization a smith chose pays off only in its own kind. Pure Java.
 */
public enum Category {
    WEAPON("weapon"), ARMOR("armor"), TOOL("tool"), RUNE("rune");

    private final String id;

    Category(String id) { this.id = id; }

    public String id() { return id; }

    public static Category byId(String id) {
        if (id == null) return null;
        for (Category c : values()) if (c.id.equalsIgnoreCase(id)) return c;
        return null;
    }
}
