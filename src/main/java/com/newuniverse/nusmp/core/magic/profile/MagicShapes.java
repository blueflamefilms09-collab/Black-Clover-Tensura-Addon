package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Which three effects a magic draws, found from its name alone (pure, so it is unit tested): the cast (a bolt / trail from 'from' to
 * 'to'), the field (a zone round a point, power = radius / 3) and the burst (an impact at a point). Order of the search:
 * <ol>
 *   <li>the magic's own layer: VfxShape &lt;NAME&gt;_FX1 / _FX2 / _FX3 (every attribute added since 0.53);</li>
 *   <li>the hand-picked legacy sets below (Flame, Water, Wind, Earth, Time ... which were drawn before the _FX naming);</li>
 *   <li>the shared wiki layers, when the caller says the magic is a wiki attribute;</li>
 *   <li>the generic wiki-style effect (never null, never throws).</li>
 * </ol>
 * A new magic therefore needs NO entry anywhere: add its three _FX shapes and it is picked up; add nothing and it still draws.
 */
public final class MagicShapes {
    private MagicShapes() {}

    /** The three shapes and how they were found. */
    public record ShapeSet(VfxShape cast, VfxShape field, VfxShape burst, VfxStyle style) {}

    /** The generic set every unknown magic draws: the wiki layers read their colour from the payload, so any colour works. */
    public static final ShapeSet GENERIC = new ShapeSet(VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_FIELD, VfxShape.WIKI_MAGIC_BURST, VfxStyle.GENERIC);
    public static final ShapeSet WIKI = new ShapeSet(VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_FIELD, VfxShape.WIKI_MAGIC_BURST, VfxStyle.WIKI);

    private static final Map<String, ShapeSet> LEGACY = new HashMap<>();

    private static void legacy(String name, VfxShape cast, VfxShape field, VfxShape burst) { LEGACY.put(name, new ShapeSet(cast, field, burst, VfxStyle.NATIVE)); }

    private static void legacy(String name, VfxShape all) { legacy(name, all, all, all); }

    static {
        legacy("FLAME", VfxShape.FLAME_TRAIL, VfxShape.FIRE_PILLAR, VfxShape.FLAME_EXPLOSION);
        legacy("EXPLOSION", VfxShape.FIRE_SPEAR, VfxShape.FIRE_WILD, VfxShape.FIRE_BURST);
        legacy("MAGMA", VfxShape.FLAME_TRAIL, VfxShape.FIRE_WILD, VfxShape.FIRE_BURST);
        legacy("WATER", VfxShape.WATER_JAVELIN, VfxShape.WATER_NEST, VfxShape.WATER_BURST);
        legacy("MIST", VfxShape.MIST_MAGIC);
        legacy("WIND", VfxShape.WIND_SLASH, VfxShape.WIND_RING, VfxShape.WIND_GALE);
        legacy("STAR", VfxShape.STAR_MAGIC);
        legacy("STORM", VfxShape.WIND_GALE, VfxShape.WIND_TORNADO, VfxShape.LIGHTNING_GOD);
        legacy("EARTH", VfxShape.STONE_SPIKES, VfxShape.EARTH_RISE, VfxShape.EARTH_FISSURE);
        legacy("PLANT", VfxShape.ELF_CIRCLE, VfxShape.TREE_ROOTS, VfxShape.ELF_CIRCLE);
        legacy("SAND", VfxShape.SAND_MAGIC);
        legacy("LIGHTNING", VfxShape.LIGHTNING_SPEAR, VfxShape.LIGHTNING_FIEND, VfxShape.LIGHTNING_GOD);
        legacy("SWORD", VfxShape.WIND_SLASH, VfxShape.WIND_RING, VfxShape.MAGIC_CIRCLE_EXPLOSION);
        legacy("DARK", VfxShape.DARK_SLASH_AVIDYA, VfxShape.SHADOW_POOL, VfxShape.DARK_BLACK_HOLE);
        legacy("SHADOW", VfxShape.SHADOW_POOL, VfxShape.SHADOW_POOL, VfxShape.SHADOW_UNITE);
        legacy("POISON", VfxShape.POISON_BREATH, VfxShape.POISON_CURTAIN, VfxShape.POISON_BREATH);
        legacy("SPATIAL", VfxShape.SPATIAL_RIFT, VfxShape.SPACE_PORTAL, VfxShape.SPACE_CUBE);
        legacy("MIRROR", VfxShape.MIRROR_RAY, VfxShape.MIRROR_ARRAY, VfxShape.MIRROR_SHATTER);
        legacy("TIME", VfxShape.TIME_REWIND, VfxShape.TIME_STASIS, VfxShape.TIME_REWIND);
        legacy("THREAD", VfxShape.THREAD_LINE, VfxShape.THREAD_WEB, VfxShape.THREAD_LINE);
        legacy("DREAM", VfxShape.DREAM_MANIFEST, VfxShape.DREAM_DOME, VfxShape.DREAM_TRANSITION);
        legacy("ANTI_MAGIC", VfxShape.ANTI_MAGIC_SLASH, VfxShape.NIHILITY_ZONE, VfxShape.DEMON_METEOR);
        legacy("TRANSMUTATION", VfxShape.ALCHEMY_CIRCLE);
        legacy("ASH", VfxShape.ASH_FORMATION);
        legacy("COTTON", VfxShape.COTTON_SHEEP);
        legacy("RECOMBINATION", VfxShape.RECOMBINE_CONSTRUCT);
        legacy("GAME", VfxShape.GAME_BOARD);
    }

    /** True when the table (not the _FX naming) hands the magic hand-picked shapes. */
    public static boolean hasLegacySet(String id) { return LEGACY.containsKey(MagicKeys.enumKey(id)); }

    /** The VfxShape called exactly this, or null. Never throws. */
    public static VfxShape shapeNamed(String name) {
        if (name == null || name.isEmpty()) return null;
        try { return VfxShape.valueOf(name.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }

    /** True when the magic has its own &lt;NAME&gt;_FX1..3 effects. */
    public static boolean hasFxTriple(String id) {
        String k = MagicKeys.enumKey(id);
        return !k.isEmpty() && shapeNamed(k + "_FX1") != null && shapeNamed(k + "_FX2") != null && shapeNamed(k + "_FX3") != null;
    }

    /**
     * The shapes of a magic; never null. {@code wikiAttribute} = the magic uses the shared wiki layers (book.WikiBooks.isNewAttribute);
     * only consulted when the magic has no layer of its own.
     */
    public static ShapeSet of(String id, boolean wikiAttribute) {
        String k = MagicKeys.enumKey(id);
        if (k.isEmpty() || MagicKeys.isMerged(id)) return GENERIC;
        if (hasFxTriple(k)) return new ShapeSet(shapeNamed(k + "_FX1"), shapeNamed(k + "_FX2"), shapeNamed(k + "_FX3"), VfxStyle.NATIVE);
        ShapeSet s = LEGACY.get(k);
        if (s != null) return s;
        return wikiAttribute ? WIKI : GENERIC;
    }
}
