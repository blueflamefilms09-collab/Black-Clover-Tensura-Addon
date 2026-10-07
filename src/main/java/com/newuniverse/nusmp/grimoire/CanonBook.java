package com.newuniverse.nusmp.grimoire;

/**
 * Named grimoires from the Black Clover wiki's grimoire tables (Clover, Spade, Heart and Diamond kingdoms), drawn after their art:
 * cover colour, frame metal, emblem colour (0 = the cover's default) and motif. Magic and cover are stored by name (MagicType /
 * GrimoireCover) to keep this class free of game code. Where the series uses a magic this mod doesn't have, the closest one is used
 * (noted per entry).
 */
public enum CanonBook {
    // Clover Kingdom
    FUEGOLEON("Fuegoleon Vermillion", "THREE_LEAF", "FLAME", 0x8E1A14, 0xE8B850, 0, null),
    MEREOLEONA("Mereoleona Vermillion", "THREE_LEAF", "FLAME", 0xC2401A, 0xE8B850, 0, null),
    LEOPOLD("Leopold Vermillion", "THREE_LEAF", "FLAME", 0xB02A1C, 0xD8A848, 0, null),
    YUNO("Yuno Grinberryall", "FOUR_LEAF", "WIND", 0x2E3A1C, 0xD8B860, 0, null),
    ASTA("Asta", "FIVE_LEAF", "ANTI_MAGIC", 0x1A1716, 0x6A5A56, 0x0A0707, BookMotif.TATTERED),
    NOELLE("Noelle Silva", "THREE_LEAF", "WATER", 0xA8C8E8, 0xD8DEE8, 0, null),
    NOZEL("Nozel Silva", "THREE_LEAF", "MERCURY", 0xC8CCD8, 0x8890A8, 0, null),
    YAMI("Yami Sukehiro", "THREE_LEAF", "DARK", 0x4A1450, 0xC8A050, 0, null),
    JULIUS("Julius Novachrono", "THREE_LEAF", "TIME", 0xE8DCC0, 0xD8B050, 0, null),
    MIMOSA("Mimosa Vermillion", "THREE_LEAF", "PLANT", 0xC8C858, 0xE8D070, 0, null),
    CHARMY("Charmy Pappitson", "THREE_LEAF", "COTTON", 0xF0EAD8, 0xE8C8A0, 0, null),            // Cotton + Food Magic (0.34)
    FINRAL("Finral Roulacase", "THREE_LEAF", "SPATIAL", 0x3E5A4A, 0xC8A050, 0, null),
    VANESSA("Vanessa Enoteca", "THREE_LEAF", "THREAD", 0xB890C8, 0xE8C8E0, 0, null),
    LUCK("Luck Voltia", "THREE_LEAF", "LIGHTNING", 0x9CC8D8, 0xE8F0F8, 0, null),
    GAUCHE("Gauche Adlai", "THREE_LEAF", "MIRROR", 0x6A60C8, 0xD0D8F0, 0, null),
    MAGNA("Magna Swing", "THREE_LEAF", "FLAME", 0xA02010, 0x302020, 0, null),
    ZORA("Zora Ideale", "THREE_LEAF", "ASH", 0xC8C8C8, 0x707070, 0, null),                    // Ash Magic (0.34)
    KLAUS("Klaus Lunettes", "THREE_LEAF", "STEEL", 0x9AB0C0, 0xD8E0E8, 0, null),
    KIRSCH("Kirsch Vermillion", "THREE_LEAF", "PLANT", 0xE88AB8, 0xF8D0E0, 0, null),            // Cherry Blossom Magic
    KARNA("Karna Freese", "THREE_LEAF", "LIGHT", 0x2A3E8E, 0xC0303A, 0, BookMotif.STRAPS),                 // Moonlight Magic
    WILLIAM("William Vangeance", "THREE_LEAF", "WORLD_TREE", 0xE0D0A0, 0xC8A050, 0, null),     // World Tree Magic (0.45; was PLANT)
    LANGRIS("Langris Vaude", "THREE_LEAF", "SPATIAL", 0x5AA8B8, 0xC8E0E8, 0, null),
    LEMIEL("Lemiel Silvamillion Clover", "THREE_LEAF", "LIGHT", 0xE0C060, 0xF8E8A0, 0, null),
    // Spade Kingdom (the Dark Triad)
    ZENON("Zenon Zogratis", "SPADE", "BONE", 0x8C8C94, 0x2A2A30, 0xD8D8E0, null),
    VANICA("Vanica Zogratis", "SPADE", "BLOOD", 0x6A0A14, 0x2A0A10, 0xE0182C, null),
    DANTE("Dante Zogratis", "SPADE", "GRAVITY", 0x6A4A8A, 0x3A2A4A, 0, null),                  // Body Magic
    LUCIUS("Lucius Zogratis", "SPADE", "DARK", 0xE8E8E8, 0x202020, 0x202020, null),            // Soul Magic
    // Heart Kingdom
    FLOGA("Floga", "HEART", "FLAME", 0xC0301C, 0xE8B850, 0, null),
    GADJAH("Gadjah", "HEART", "LIGHTNING", 0xC8A040, 0x6A4A20, 0, null),
    LOLOPECHKA("Lolopechka", "HEART", "WATER", 0x4A8AD8, 0xD8E8F8, 0, null),
    // Diamond Kingdom
    MARS("Mars", "DIAMOND", "EARTH", 0xD8D8E0, 0xB04030, 0, null),                            // Mineral Magic
    // 0.34: the remaining squad captains (wiki: Magic Knights) and the Black Bulls with the new attributes; appended
    CHARLOTTE("Charlotte Roselei", "THREE_LEAF", "PLANT", 0x2A6A9A, 0xC8D8E8, 0, null),        // Blue Rose captain; Briar Magic
    JACK("Jack the Ripper", "THREE_LEAF", "SLASH", 0x2E8A5A, 0xC8E0C8, 0, null),               // Green Mantis captain; Slash Magic (0.45; was SWORD)
    RILL("Rill Boismortier", "THREE_LEAF", "PAINTING", 0x9AD0C0, 0xE8F0E8, 0, null),          // Aqua Deer captain; Painting Magic (0.41; was CREATION)
    DOROTHY("Dorothy Unsworth", "THREE_LEAF", "DREAM", 0xE88AA0, 0xF0D8E0, 0, null),          // Coral Peacock captain; Dream Magic
    KAISER("Kaiser Granvorka", "THREE_LEAF", "STORM", 0x6A4A8A, 0xD0C0E0, 0, null),           // Purple Orca captain; Vortex Magic
    NACHT("Nacht Faust", "THREE_LEAF", "SHADOW", 0x1A1A22, 0x8A8AA0, 0, null),                // Black Bull (captain after Yami); Shadow Magic
    GREY("Grey", "THREE_LEAF", "TRANSMUTATION", 0x6A7280, 0xD0D8E0, 0, null),                 // Black Bull; Transmutation Magic
    GORDON("Gordon Agrippa", "THREE_LEAF", "POISON", 0x3A4A2A, 0xB0C890, 0, null),            // Black Bull; Poison Magic
    HENRY("Henry Legolant", "THREE_LEAF", "RECOMBINATION", 0x3A3430, 0xC8A050, 0, null),      // Black Bull; Recombination Magic
    // 0.45: appended
    LETOILE("Letoile Becquerel", "THREE_LEAF", "COMPASS", 0x3A6A4A, 0xD8B860, 0, null);       // Golden Dawn; Compass Magic

    public final String owner, cover, magic;
    public final int coverColor, trimColor, emblemColor;
    public final BookMotif motif;

    CanonBook(String owner, String cover, String magic, int coverColor, int trimColor, int emblemColor, BookMotif motif) {
        this.owner = owner; this.cover = cover; this.magic = magic;
        this.coverColor = coverColor; this.trimColor = trimColor; this.emblemColor = emblemColor;
        this.motif = motif != null ? motif : BookMotif.forCover(cover);            // the art-pack design of its cover (0.24)
    }

    public String id() { return name().toLowerCase(); }

    public static CanonBook byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (CanonBook b : values()) if (b.id().equalsIgnoreCase(id)) return b;
        return null;
    }
}
