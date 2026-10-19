package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: a saved Avatar remembers its hair as one of the sixteen ids the first hair generator numbered (0 "short crop" to 15 "hime cut", each with its own colour), and a save must keep the same
 * person after the hair system was replaced. This table maps every old id to the new style that is the same haircut and the colour the old model was painted in, so an old Avatar looks like
 * itself with better hair. Ids outside 0..15 (a bad packet, a newer save) fall back to the nearest valid one. Pure.
 */
public final class LegacyHair {
    private LegacyHair() {}

    /** How many old ids there are (the count {@code AppearanceData.HAIR_COUNT} had when the table was written). */
    public static final int COUNT = 16;

    /** A haircut: the style id and the colour. */
    public record Cut(String style, int rgb) {}

    private static final Cut[] TABLE = {
            new Cut("short", 0x704E34), new Cut("spiky", 0x242230), new Cut("long", 0xD6DCE8), new Cut("ponytail", 0xE8C460),
            new Cut("twin_tails", 0xEE8AB0), new Cut("bob", 0x263878), new Cut("bun", 0xB02C2C), new Cut("mohawk", 0x46AA60),
            new Cut("messy", 0xE87830), new Cut("swept", 0xF4F4FA), new Cut("braid", 0x885434), new Cut("ahoge", 0xFAE296),
            new Cut("drills", 0xEEBE3C), new Cut("pompadour", 0x1C203C), new Cut("layered", 0x7A4CA8), new Cut("hime", 0x14141C)};

    /** The haircut of an old hair id (clamped into 0..15). */
    public static Cut of(int hairId) { return TABLE[Math.max(0, Math.min(COUNT - 1, hairId))]; }
}
