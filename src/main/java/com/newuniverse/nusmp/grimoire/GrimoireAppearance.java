package com.newuniverse.nusmp.grimoire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.UUID;

/**
 * Everything the renderer needs to draw one grimoire, stored on the ItemStack as the {@code nusmp:appearance} data component.
 * <p>
 * Four colour channels feed the item model's tintindex: 0 = primary (cover), 1 = trim (metal), 2 = insignia (derived from the
 * crest), 3 = aura (emissive glow). The enums choose which of the 32 greyscale base textures and which 3D parts are assembled
 * (see {@link GrimoireModelPlan}); colour is multiplied on top. That is how 32 files give 100,000+ distinct looks.
 */
public record GrimoireAppearance(Kingdom kingdom, InsigniaType insignia, int leafCount, CoverMaterial cover, TrimMetal trimMetal,
                                 int primaryColor, int trimColor, int auraColor, Thickness thickness, ClaspType clasp) {

    /** Number of grimoire ids {@link #fromGrimoireId(int)} accepts: 0 &lt;= id &lt; ID_COUNT. */
    public static final int ID_COUNT = 103_000;

    private static final int INSIGNIA_N = InsigniaType.values().length;
    private static final int COVER_N = CoverMaterial.values().length;
    private static final int METAL_N = TrimMetal.values().length;
    private static final int THICK_N = Thickness.values().length;
    private static final int CLASP_N = ClaspType.values().length;
    private static final int AURA_N = AuraAffinity.values().length;
    /** Distinct (crest, cover, metal, thickness, clasp, affinity) combinations: 16*5*6*4*4*14 = 107,520 >= ID_COUNT. */
    public static final int COMBINATIONS = INSIGNIA_N * COVER_N * METAL_N * THICK_N * CLASP_N * AURA_N;
    /** Odd and coprime with 2^10*3*5*7 = 107,520, so id -> id*K mod N is a bijection: no two ids collide, neighbours look unrelated. */
    private static final long SCRAMBLE = 31_337L;

    public GrimoireAppearance {
        if (kingdom == null) kingdom = Kingdom.CLOVER;
        if (insignia == null) insignia = InsigniaType.THREE_LEAF;
        if (cover == null) cover = CoverMaterial.PLAIN;
        if (trimMetal == null) trimMetal = TrimMetal.BRONZE;
        if (thickness == null) thickness = Thickness.STANDARD;
        if (clasp == null) clasp = ClaspType.OPEN;
        leafCount = Mth.clamp(leafCount, 0, 5);
        primaryColor &= 0xFFFFFF;
        trimColor &= 0xFFFFFF;
        auraColor &= 0xFFFFFF;
    }

    // ------------------------------------------------------------------ codecs

    private static <E extends Enum<E>> Codec<E> enumCodec(E[] values) {
        return Codec.STRING.comapFlatMap(s -> {
            for (E e : values) if (e.name().equalsIgnoreCase(s)) return DataResult.success(e);
            return DataResult.error(() -> "Unknown grimoire attribute: " + s);
        }, e -> e.name().toLowerCase(Locale.ROOT));
    }

    public static final Codec<GrimoireAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            enumCodec(Kingdom.values()).fieldOf("kingdom").forGetter(GrimoireAppearance::kingdom),
            enumCodec(InsigniaType.values()).fieldOf("insignia").forGetter(GrimoireAppearance::insignia),
            Codec.intRange(0, 5).fieldOf("leaf_count").forGetter(GrimoireAppearance::leafCount),
            enumCodec(CoverMaterial.values()).fieldOf("cover").forGetter(GrimoireAppearance::cover),
            enumCodec(TrimMetal.values()).fieldOf("trim_metal").forGetter(GrimoireAppearance::trimMetal),
            Codec.INT.fieldOf("primary_color").forGetter(GrimoireAppearance::primaryColor),
            Codec.INT.fieldOf("trim_color").forGetter(GrimoireAppearance::trimColor),
            Codec.INT.fieldOf("aura_color").forGetter(GrimoireAppearance::auraColor),
            enumCodec(Thickness.values()).fieldOf("thickness").forGetter(GrimoireAppearance::thickness),
            enumCodec(ClaspType.values()).fieldOf("clasp_type").forGetter(GrimoireAppearance::clasp)
    ).apply(i, GrimoireAppearance::new));

    /** 7 enum/count bytes + 3 x 24-bit colours = 16 bytes per stack on the wire. Ordinals are stable (the enums are append-only). */
    public static final StreamCodec<ByteBuf, GrimoireAppearance> STREAM_CODEC = StreamCodec.of(
            (buf, a) -> {
                buf.writeByte(a.kingdom.ordinal());
                buf.writeByte(a.insignia.ordinal());
                buf.writeByte(a.leafCount);
                buf.writeByte(a.cover.ordinal());
                buf.writeByte(a.trimMetal.ordinal());
                buf.writeByte(a.thickness.ordinal());
                buf.writeByte(a.clasp.ordinal());
                buf.writeMedium(a.primaryColor);
                buf.writeMedium(a.trimColor);
                buf.writeMedium(a.auraColor);
            },
            buf -> {
                // read strictly in the order written above (the record's own component order is different)
                Kingdom kingdom = Kingdom.values()[Math.floorMod(buf.readUnsignedByte(), Kingdom.values().length)];
                InsigniaType insignia = InsigniaType.byOrdinal(buf.readUnsignedByte());
                int leafCount = buf.readUnsignedByte();
                CoverMaterial cover = CoverMaterial.byOrdinal(buf.readUnsignedByte());
                TrimMetal trimMetal = TrimMetal.byOrdinal(buf.readUnsignedByte());
                Thickness thickness = Thickness.byOrdinal(buf.readUnsignedByte());
                ClaspType clasp = ClaspType.byOrdinal(buf.readUnsignedByte());
                int primary = buf.readUnsignedMedium(), trim = buf.readUnsignedMedium(), aura = buf.readUnsignedMedium();
                return new GrimoireAppearance(kingdom, insignia, leafCount, cover, trimMetal, primary, trim, aura, thickness, clasp);
            });

    // ------------------------------------------------------------------ tint channels

    public static final int TINT_PRIMARY = 0, TINT_TRIM = 1, TINT_INSIGNIA = 2, TINT_AURA = 3;

    public int insigniaColor() { return insignia.color; }

    /** Packed 0xRRGGBB for a model tintindex, or -1 (no tint) for anything else. */
    public int tint(int tintIndex) {
        return switch (tintIndex) {
            case TINT_PRIMARY -> primaryColor;
            case TINT_TRIM -> trimColor;
            case TINT_INSIGNIA -> insignia.color;
            case TINT_AURA -> auraColor;
            default -> -1;
        };
    }

    /** The geometry-relevant part of this appearance (colours are applied by tint, so they never rebuild geometry). */
    public GrimoireRenderKey renderKey() { return GrimoireRenderKey.of(this); }

    // ------------------------------------------------------------------ the procedural generator

    /**
     * Deterministically maps 0 &lt;= id &lt; {@link #ID_COUNT} to one grimoire. Distinct ids always give a distinct
     * (crest, cover, metal, thickness, clasp, affinity) combination, because the id is scrambled by a bijection and then
     * decoded in a mixed radix.
     */
    public static GrimoireAppearance fromGrimoireId(int id) {
        if (id < 0 || id >= ID_COUNT) throw new IllegalArgumentException("grimoire id out of range 0.." + (ID_COUNT - 1) + ": " + id);
        int v = (int) ((id * SCRAMBLE) % COMBINATIONS);
        InsigniaType insignia = InsigniaType.byOrdinal(v % INSIGNIA_N); v /= INSIGNIA_N;
        CoverMaterial cover = CoverMaterial.byOrdinal(v % COVER_N); v /= COVER_N;
        TrimMetal metal = TrimMetal.byOrdinal(v % METAL_N); v /= METAL_N;
        Thickness thickness = Thickness.byOrdinal(v % THICK_N); v /= THICK_N;
        ClaspType clasp = ClaspType.byOrdinal(v % CLASP_N); v /= CLASP_N;
        AuraAffinity aura = AuraAffinity.byOrdinal(v % AURA_N);

        // The special outliers belong to no kingdom; the generator lets the rest of the id decide which palette they wear.
        Kingdom kingdom = insignia.kingdom != null ? insignia.kingdom
                : Kingdom.values()[(cover.ordinal() * 7 + metal.ordinal() * 3 + thickness.ordinal() + aura.ordinal()) % Kingdom.values().length];
        int primary = scale(lerp(kingdomBase(kingdom), aura.color, 0.40f), cover.brightness);
        return new GrimoireAppearance(kingdom, insignia, insignia.count, cover, metal, primary, metal.color, aura.color, thickness, clasp);
    }

    /** Any 64-bit seed -> one of the {@link #ID_COUNT} grimoires (SplitMix64 finaliser, so close seeds do not give close ids). */
    public static GrimoireAppearance fromSeed(long seed) { return fromGrimoireId(idOfSeed(seed)); }

    /** The grimoire id a seed lands on. */
    public static int idOfSeed(long seed) { return (int) Long.remainderUnsigned(splitMix(seed), ID_COUNT); }

    /** The mana affinity (aura colour family) {@link #fromGrimoireId} gives an id; handy for choosing a matching magic type. */
    public static AuraAffinity affinityOfId(int id) {
        if (id < 0 || id >= ID_COUNT) throw new IllegalArgumentException("grimoire id out of range 0.." + (ID_COUNT - 1) + ": " + id);
        int v = (int) ((id * SCRAMBLE) % COMBINATIONS);
        v /= INSIGNIA_N * COVER_N * METAL_N * THICK_N * CLASP_N;
        return AuraAffinity.byOrdinal(v % AURA_N);
    }

    private static long splitMix(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static int kingdomBase(Kingdom k) {
        return switch (k) { case CLOVER -> 0x2F6B3A; case HEART -> 0xA83A5A; case SPADE -> 0x3A3F52; case DIAMOND -> 0x3A66A8; };
    }

    // ------------------------------------------------------------------ the grimoires the mod already hands out

    private static final GrimoireAppearance[] COVER_CACHE = new GrimoireAppearance[GrimoireCover.values().length * MagicType.values().length];

    /** Fallback for a grimoire with no data at all (declared after the cache: static initialisers run in textual order). */
    public static final GrimoireAppearance DEFAULT = fromCover(GrimoireCover.THREE_LEAF, MagicType.FLAME);

    /** The look of a regular in-game grimoire: crest from its cover, colours from its magic. Cached, so the legacy path allocates nothing. */
    public static GrimoireAppearance fromCover(GrimoireCover cover, MagicType magic) {
        int idx = cover.ordinal() * MagicType.values().length + magic.ordinal();
        GrimoireAppearance a = COVER_CACHE[idx];
        if (a == null) COVER_CACHE[idx] = a = buildFromCover(cover, magic);
        return a;
    }

    private static GrimoireAppearance buildFromCover(GrimoireCover cover, MagicType magic) {
        InsigniaType insignia = InsigniaType.of(cover);
        // Canon (Black Clover): the insignia of the kingdom sits at the centre of the front cover, and the binding reflects the
        // owner's magic attribute (so the cover colour is the magic's). Three-leaf = common, plain leather with a simple border.
        // Four-leaf = rare, gold insignia with intricate gilded ornaments around the border (Yuno's is gold and green). A
        // four-leaf that turns into a devil's five-leaf gets a darker cover and a black clover (Asta's is black, tattered and filthy).
        // Grimoires carry no straps, chains, locks, corner caps or glow while closed.
        boolean astas = cover == GrimoireCover.FIVE_LEAF && magic == MagicType.ANTI_MAGIC;
        boolean devil = cover.tier >= 5;
        CoverMaterial material = cover.isCracked() || astas ? CoverMaterial.TATTERED
                : (cover.tier == 4 && cover.kingdom == Kingdom.CLOVER) || (devil && cover.kingdom == Kingdom.CLOVER) ? CoverMaterial.METALLIC_TRIM
                : devil ? CoverMaterial.LEATHER_STITCHED : CoverMaterial.PLAIN;
        // gilded ornaments (gold on a rare cover, dark on a corrupted one) are the only "hardware"; the mod's own invented
        // covers (triple spade, cracked, kingdom variants) stay bare leather
        TrimMetal metal = devil && cover.kingdom == Kingdom.CLOVER && !astas ? TrimMetal.OBSIDIAN
                : cover.tier == 4 && cover.kingdom == Kingdom.CLOVER ? TrimMetal.GOLD : TrimMetal.NONE;
        Thickness thickness = cover.isCracked() ? Thickness.THIN : devil ? Thickness.TOME : Thickness.STANDARD;
        int primary = MagicPalette.primary(magic);
        if (astas) primary = 0x4A423E;                          // grimy leather, so the black clover still shows
        else if (devil) primary = scale(primary, 0.55f);        // the corrupted cover turns darker
        return new GrimoireAppearance(cover.kingdom, insignia, insignia.count, material, metal,
                primary, metal.color, MagicPalette.aura(magic), thickness, ClaspType.OPEN);
    }

    // ------------------------------------------------------------------ one look per owner

    // [group][choices]; group: 0 common, 1 rare (four-leaf style), 2 forbidden, 3 cracked. The cover's own default is always a choice.
    // Only what a canon grimoire can vary in: plain vs stitched border on ordinary books, and how thick it is. Trim, clasps and
    // rarity-defining material come from the cover itself (see buildFromCover); the owner's colour drift does the rest.
    private static final CoverMaterial[] OWNER_COMMON_MATERIALS = {CoverMaterial.PLAIN, CoverMaterial.LEATHER_STITCHED};
    private static final Thickness[][] OWNER_THICKNESS = {
            {Thickness.THIN, Thickness.STANDARD}, {Thickness.STANDARD, Thickness.TOME}, {Thickness.TOME, Thickness.STANDARD}, {Thickness.THIN, Thickness.STANDARD}};

    /**
     * The look of a grimoire bound to {@code owner}: the crest, trim and magic colours stay those of the cover/magic (so the book
     * is still recognisable and canon), while the border style of ordinary books, the thickness and a small colour shift are
     * drawn from the owner's uuid ("each grimoire has its own binding"). Same owner + same cover + same magic = same look every time the stack is rebuilt,
     * so nothing needs to be stored.
     */
    public static GrimoireAppearance forOwner(GrimoireCover cover, MagicType magic, UUID owner) {
        GrimoireAppearance base = fromCover(cover, magic);
        int g = cover.isCracked() ? 3 : cover.tier >= 5 ? 2 : cover.tier == 4 ? 1 : 0;
        long z = splitMix(owner.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ owner.getLeastSignificantBits()
                ^ ((long) magic.ordinal() << 40) ^ ((long) cover.ordinal() << 52));
        CoverMaterial material = g == 0 ? pick(OWNER_COMMON_MATERIALS, z) : base.cover;
        TrimMetal metal = base.trimMetal;
        Thickness thickness = pick(OWNER_THICKNESS[g], z >>> 14);
        ClaspType clasp = ClaspType.OPEN;
        // slight, stable colour drift: +-10% brightness and a few steps per channel
        float f = 0.90f + (float) ((z >>> 28) & 0xFF) / 255f * 0.22f;
        int dr = (int) ((z >>> 36) & 15) - 8, dg = (int) ((z >>> 40) & 15) - 8, db = (int) ((z >>> 44) & 15) - 8;
        int r = Mth.clamp(Math.round(((base.primaryColor >> 16) & 255) * f) + dr, 0, 255);
        int gr = Mth.clamp(Math.round(((base.primaryColor >> 8) & 255) * f) + dg, 0, 255);
        int b = Mth.clamp(Math.round((base.primaryColor & 255) * f) + db, 0, 255);
        return new GrimoireAppearance(base.kingdom, base.insignia, base.leafCount, material, metal,
                (r << 16) | (gr << 8) | b, metal.color, base.auraColor, thickness, clasp);
    }

    private static <T> T pick(T[] options, long bits) {
        return options[(int) Long.remainderUnsigned(bits & Long.MAX_VALUE, options.length)];
    }

    // ------------------------------------------------------------------ reading a stack

    /**
     * The appearance to draw for a stack: the {@code nusmp:appearance} component when present, otherwise derived from the
     * grimoire's legacy custom data (older saves). Never null; allocation-free on both paths.
     */
    public static GrimoireAppearance of(ItemStack stack) {
        GrimoireAppearance a = stack.get(GrimoireComponents.APPEARANCE.get());
        if (a != null) return a;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return DEFAULT;
        CompoundTag tag = data.getUnsafe();      // read-only use: no copy
        if (!tag.contains("Magic")) return DEFAULT;
        return fromCover(GrimoireCover.byName(tag.getString("Cover"), tag.getInt("Leaves")), MagicType.byName(tag.getString("Magic")));
    }

    // ------------------------------------------------------------------ colour helpers

    private static int lerp(int a, int b, float t) {
        int r = Mth.floor(Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255));
        int g = Mth.floor(Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255));
        int bl = Mth.floor(Mth.lerp(t, a & 255, b & 255));
        return (r << 16) | (g << 8) | bl;
    }

    private static int scale(int rgb, float f) {
        int r = Mth.clamp(Math.round(((rgb >> 16) & 255) * f), 0, 255);
        int g = Mth.clamp(Math.round(((rgb >> 8) & 255) * f), 0, 255);
        int b = Mth.clamp(Math.round((rgb & 255) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
