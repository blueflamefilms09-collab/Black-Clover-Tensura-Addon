package com.newuniverse.nusmp.grimoire;

/**
 * Named grimoires from the Black Clover wiki's grimoire tables (Clover, Spade, Heart and Diamond kingdoms), drawn after their art:
 * cover colour, frame metal, emblem colour (0 = the cover's default) and motif. Magic and cover are stored by name (MagicType /
 * GrimoireCover) to keep this class free of game code. Where the series uses a magic this mod doesn't have, the closest one is used
 * (noted per entry).
 */
public enum CanonBook {
    // Clover Kingdom
    FUEGOLEON("Fuegoleon Vermillion", "THREE_LEAF", "FLAME", 0x8E1A14, 0xE8B850, 0, BookMotif.ORNATE),
    MEREOLEONA("Mereoleona Vermillion", "THREE_LEAF", "FLAME", 0xC2401A, 0xE8B850, 0, BookMotif.FILIGREE),
    LEOPOLD("Leopold Vermillion", "THREE_LEAF", "FLAME", 0xB02A1C, 0xD8A848, 0, BookMotif.FILIGREE),
    YUNO("Yuno Grinberryall", "FOUR_LEAF", "WIND", 0x2E3A1C, 0xD8B860, 0, BookMotif.ORNATE),
    ASTA("Asta", "FIVE_LEAF", "ANTI_MAGIC", 0x1A1716, 0x6A5A56, 0x0A0707, BookMotif.TATTERED),
    NOELLE("Noelle Silva", "THREE_LEAF", "WATER", 0xA8C8E8, 0xD8DEE8, 0, BookMotif.FILIGREE),
    NOZEL("Nozel Silva", "THREE_LEAF", "MERCURY", 0xC8CCD8, 0x8890A8, 0, BookMotif.FILIGREE),
    YAMI("Yami Sukehiro", "THREE_LEAF", "DARK", 0x4A1450, 0xC8A050, 0, BookMotif.FILIGREE),
    JULIUS("Julius Novachrono", "THREE_LEAF", "TIME", 0xE8DCC0, 0xD8B050, 0, BookMotif.PLAIN),
    MIMOSA("Mimosa Vermillion", "THREE_LEAF", "PLANT", 0xC8C858, 0xE8D070, 0, BookMotif.FLORAL),
    CHARMY("Charmy Pappitson", "THREE_LEAF", "CREATION", 0xF0EAD8, 0xE8C8A0, 0, BookMotif.PLAIN),          // Cotton Magic
    FINRAL("Finral Roulacase", "THREE_LEAF", "SPATIAL", 0x3E5A4A, 0xC8A050, 0, BookMotif.FILIGREE),
    VANESSA("Vanessa Enoteca", "THREE_LEAF", "THREAD", 0xB890C8, 0xE8C8E0, 0, BookMotif.FLORAL),
    LUCK("Luck Voltia", "THREE_LEAF", "LIGHTNING", 0x9CC8D8, 0xE8F0F8, 0, BookMotif.FILIGREE),
    GAUCHE("Gauche Adlai", "THREE_LEAF", "MIRROR", 0x6A60C8, 0xD0D8F0, 0, BookMotif.FILIGREE),
    MAGNA("Magna Swing", "THREE_LEAF", "FLAME", 0xA02010, 0x302020, 0, BookMotif.PLAIN),
    ZORA("Zora Ideale", "THREE_LEAF", "CREATION", 0xC8C8C8, 0x707070, 0, BookMotif.LATTICE),               // Ash Magic
    KLAUS("Klaus Lunettes", "THREE_LEAF", "STEEL", 0x9AB0C0, 0xD8E0E8, 0, BookMotif.ORNATE),
    KIRSCH("Kirsch Vermillion", "THREE_LEAF", "PLANT", 0xE88AB8, 0xF8D0E0, 0, BookMotif.FLORAL),            // Cherry Blossom Magic
    KARNA("Karna Freese", "THREE_LEAF", "LIGHT", 0x2A3E8E, 0xC0303A, 0, BookMotif.STRAPS),                 // Moonlight Magic
    WILLIAM("William Vangeance", "THREE_LEAF", "PLANT", 0xE0D0A0, 0xC8A050, 0, BookMotif.FLORAL),          // World Tree Magic
    LANGRIS("Langris Vaude", "THREE_LEAF", "SPATIAL", 0x5AA8B8, 0xC8E0E8, 0, BookMotif.FILIGREE),
    LEMIEL("Lemiel Silvamillion Clover", "THREE_LEAF", "LIGHT", 0xE0C060, 0xF8E8A0, 0, BookMotif.ORNATE),
    // Spade Kingdom (the Dark Triad)
    ZENON("Zenon Zogratis", "SPADE", "BONE", 0x8C8C94, 0x2A2A30, 0xD8D8E0, BookMotif.WHEELS),
    VANICA("Vanica Zogratis", "SPADE", "BLOOD", 0x6A0A14, 0x2A0A10, 0xE0182C, BookMotif.ORNATE),
    DANTE("Dante Zogratis", "SPADE", "GRAVITY", 0x6A4A8A, 0x3A2A4A, 0, BookMotif.WHEELS),                  // Body Magic
    LUCIUS("Lucius Zogratis", "SPADE", "DARK", 0xE8E8E8, 0x202020, 0x202020, BookMotif.WHEELS),            // Soul Magic
    // Heart Kingdom
    FLOGA("Floga", "HEART", "FLAME", 0xC0301C, 0xE8B850, 0, BookMotif.FLORAL),
    GADJAH("Gadjah", "HEART", "LIGHTNING", 0xC8A040, 0x6A4A20, 0, BookMotif.LATTICE),
    LOLOPECHKA("Lolopechka", "HEART", "WATER", 0x4A8AD8, 0xD8E8F8, 0, BookMotif.FLORAL),
    // Diamond Kingdom
    MARS("Mars", "DIAMOND", "EARTH", 0xD8D8E0, 0xB04030, 0, BookMotif.LATTICE);                            // Mineral Magic

    public final String owner, cover, magic;
    public final int coverColor, trimColor, emblemColor;
    public final BookMotif motif;

    CanonBook(String owner, String cover, String magic, int coverColor, int trimColor, int emblemColor, BookMotif motif) {
        this.owner = owner; this.cover = cover; this.magic = magic;
        this.coverColor = coverColor; this.trimColor = trimColor; this.emblemColor = emblemColor; this.motif = motif;
    }

    public String id() { return name().toLowerCase(); }

    public static CanonBook byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (CanonBook b : values()) if (b.id().equalsIgnoreCase(id)) return b;
        return null;
    }
}
