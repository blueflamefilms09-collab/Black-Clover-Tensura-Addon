package com.newuniverse.nusmp.grimoire;

/**
 * How one grimoire looks: emblem (its cover: clover leaves or a kingdom suit), motif, colours and wear. Resolved from what the item
 * already stores (cover, magic, optional canon book, optional per-owner seed), so every grimoire, old ones included, gets the look
 * with no conversion. Pure: no game classes.
 */
public record BookLook(String emblem, BookMotif motif, boolean tattered, int coverColor, int trimColor, int emblemColor, int glowColor,
                       CanonBook canon, String magic, String fusion) {

    /** The look of a book with a single attribute (no fusion). */
    public BookLook(String emblem, BookMotif motif, boolean tattered, int coverColor, int trimColor, int emblemColor, int glowColor, CanonBook canon, String magic) {
        this(emblem, motif, tattered, coverColor, trimColor, emblemColor, glowColor, canon, magic, "");
    }

    /** Tint layers the model and the item colour handler share. */
    public static final int TINT_COVER = 0, TINT_TRIM = 1, TINT_EMBLEM = 2;

    public static final BookLook DEFAULT = resolve("THREE_LEAF", "FLAME", null, 0);

    /**
     * @param cover  GrimoireCover name (THREE_LEAF, SPADE, ...)
     * @param magic  MagicType name
     * @param canon  CanonBook id, or null/empty
     * @param seed   per-owner variation (0 = none): a slight shade of the cover leather so no two mages' books are identical
     */
    public static BookLook resolve(String cover, String magic, String canon, int seed) { return resolve(cover, magic, canon, seed, ""); }

    /**
     * 1.16.0: a mage with two attributes (a fused grimoire, "FLAME:WATER") gets a look of their own: the leather is the two parents'
     * colours blended, the metal and the glow take one parent each / both, and the cover grows a generated filigree ({@link FusionDesign},
     * built by the plan) so it is never mistaken for a single-attribute book. Canon books keep their own look.
     *
     * @param fusion "PARENT_A:PARENT_B" (MagicType names) or empty
     */
    public static BookLook resolve(String cover, String magic, String canon, int seed, String fusion) {
        CanonBook c = CanonBook.byId(canon);
        String emblem = cover == null || cover.isEmpty() ? "THREE_LEAF" : cover;
        if (c != null) {
            return new BookLook(c.cover, c.motif, c.motif == BookMotif.TATTERED, c.coverColor, c.trimColor,
                    c.emblemColor != 0 ? c.emblemColor : BookPalette.emblem(c.cover), BookPalette.glow(c.magic), c,
                    magic == null ? "" : magic);
        }
        String m = magic == null ? "" : magic;
        String[] parents = FusionDesign.parents(fusion);
        int coverColor = parents == null ? BookPalette.cover(m) : BookPalette.mix(BookPalette.cover(parents[0]), BookPalette.cover(parents[1]), 0.5f);
        if (seed != 0) coverColor = BookPalette.scale(coverColor, 0.88f + ((seed * 0x9E3779B9) >>> 24) / 255f * 0.24f);   // 88-112%
        boolean forbidden = emblem.equals("FIVE_LEAF") || emblem.equals("TRIPLE_SPADE");
        if (forbidden && !m.equals("ANTI_MAGIC")) coverColor = BookPalette.scale(coverColor, 0.55f);    // a devil darkens the book
        BookMotif motif = BookPalette.motif(emblem, m);
        int trim0 = parents == null ? BookPalette.trim(m) : BookPalette.trim(parents[1]);                  // the second parent's metal on the first parent's leather
        int trim = forbidden ? BookPalette.scale(trim0, 0.8f) : trim0;   // tarnished, but the black clover still reads
        int glow = parents == null ? BookPalette.glow(m) : BookPalette.mix(BookPalette.glow(parents[0]), BookPalette.glow(parents[1]), 0.5f);
        if (emblem.equals("BLACK_MAGIC")) {                    // black magic: the book itself turns black, its metal blood-red
            coverColor = BookPalette.scale(coverColor, 0.28f);
            trim = BookPalette.BLACK_MAGIC_TRIM;
        } else if (emblem.equals("GOD_TIER")) {                // god-tier: ivory and bright gold, a white-gold glow
            coverColor = BookPalette.mix(coverColor, BookPalette.GOD_TIER_COVER, 0.6f);
            trim = BookPalette.GOD_TIER_TRIM;
            glow = BookPalette.GOD_TIER_GLOW;
        }
        return new BookLook(emblem, motif, motif == BookMotif.TATTERED, coverColor, trim, BookPalette.emblem(emblem), glow, null, m, parents == null ? "" : fusion);
    }

    /** RGB for a tint layer, or -1 for none. */
    public int tint(int layer) {
        return switch (layer) {
            case TINT_COVER -> coverColor;
            case TINT_TRIM -> trimColor;
            case TINT_EMBLEM -> emblemColor;
            default -> -1;
        };
    }

    /**
     * What the geometry depends on (colours are tints, not geometry). {@code open}: 0 = the closed book; 1..3 = the summoned book
     * opening into a V, spine towards the onlookers ({@link GrimoireBookPlan#OPEN_DEGREES}).
     */
    public record Key(String emblem, BookMotif motif, boolean tattered, boolean held, int open, String magic, String fusion) {
        public Key(String emblem, BookMotif motif, boolean tattered, boolean held, int open, String magic) {
            this(emblem, motif, tattered, held, open, magic, "");
        }

        public Key(String emblem, BookMotif motif, boolean tattered, boolean held, int open) {
            this(emblem, motif, tattered, held, open, "", "");
        }

        public Key(String emblem, BookMotif motif, boolean tattered, boolean held) {
            this(emblem, motif, tattered, held, 0, "", "");
        }
    }

    public Key key(boolean held) { return new Key(emblem, motif, tattered, held, 0, magic, fusion); }

    public Key key(boolean held, int open) {
        return new Key(emblem, motif, tattered, held, Math.max(0, Math.min(GrimoireBookPlan.OPEN_STEPS, open)), magic, fusion);
    }
}
