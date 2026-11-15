package com.newuniverse.nusmp.migrate;

/**
 * Why (3.84.0): the mod id changed from the old short id to {@code anime_multiverse}. Everything stored by id (items, skills, components, attributes, advancements, saved commands, data
 * packs) still says the old namespace; this one function is the whole migration: the {@code ResourceLocation} mixin runs every namespace through it, so an old id reads as the new one.
 * Pure, no Minecraft type.
 */
public final class LegacyId {
    private LegacyId() {}

    /** The id the mod had before 3.84.0. Kept only here. */
    public static final String OLD = "nus" + "mp";
    public static final String NEW = "anime_multiverse";

    /** The namespace to use: the new one for the old id, every other namespace unchanged. */
    public static String namespace(String ns) { return OLD.equals(ns) ? NEW : ns; }
}
