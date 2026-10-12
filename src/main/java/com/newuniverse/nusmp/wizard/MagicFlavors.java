package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.blackclover.MagicType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.16.0: how the Tower Wizard's questionnaire reaches every magic attribute, and which pairs fuse into one.
 * <ul>
 *   <li><b>Flavors.</b> Sixteen temperaments. Each answer on the questionnaire adds points to flavors (never to a magic by name, so
 *       nothing in it gives the answer away); each magic attribute carries one or two flavors, and its score is the points of its
 *       flavors. Every attribute that can be rolled for a soul (all but Anti-Magic, Kotodama, Curse and the devil-only Demon magics)
 *       is reachable, not just Fire, Water, Wind and Earth.</li>
 *   <li><b>Hybrids.</b> When two attributes from different flavors are nearly tied, the mage does not get two grimoires: the pair
 *       fuses. If the two flavors have a well-known blend (Fire and Water give Glass, Plant and Making give Cotton as for Charmy, Fire
 *       and Earth give Magma ...) the grimoire is that attribute's book, with the fused pair written on it; otherwise it is the main
 *       attribute's book with the other attribute fused in. Either way the grimoire gets a Signature Spell built from both parents
 *       (book.SignatureSpell).</li>
 * </ul>
 */
public final class MagicFlavors {
    private MagicFlavors() {}

    public enum Flavor { HOT, COLD, FLOW, GUST, STONE, METAL, GEM, GREEN, BRIGHT, SHADE, SOUL, SPACE, TIME, BODY, MIND, MAKER }

    private static final Map<MagicType, Flavor[]> FLAVORS = new EnumMap<>(MagicType.class);

    private static void f(MagicType m, Flavor... fl) { FLAVORS.put(m, fl); }

    static {
        f(MagicType.FLAME, Flavor.HOT);                                         f(MagicType.EXPLOSION, Flavor.HOT, Flavor.GUST);
        f(MagicType.MAGMA, Flavor.HOT, Flavor.STONE);                           f(MagicType.ASH, Flavor.HOT, Flavor.SHADE);
        f(MagicType.SMOKE, Flavor.SHADE, Flavor.HOT);
        f(MagicType.WATER, Flavor.FLOW);                                        f(MagicType.ICE, Flavor.COLD, Flavor.FLOW);
        f(MagicType.MERCURY, Flavor.FLOW, Flavor.METAL);                        f(MagicType.MIST, Flavor.FLOW, Flavor.GUST);
        f(MagicType.GEL, Flavor.FLOW, Flavor.MAKER);                            f(MagicType.BUBBLE, Flavor.FLOW, Flavor.BRIGHT);
        f(MagicType.ICE_WEDGE, Flavor.COLD);                                    f(MagicType.SNOW, Flavor.COLD, Flavor.GUST);
        f(MagicType.MUCUS, Flavor.FLOW, Flavor.BODY);
        f(MagicType.WIND, Flavor.GUST);                                         f(MagicType.STAR, Flavor.GUST, Flavor.BRIGHT);
        f(MagicType.STORM, Flavor.GUST, Flavor.FLOW);                           f(MagicType.AIR, Flavor.GUST);
        f(MagicType.SOUND, Flavor.GUST, Flavor.MAKER);                          f(MagicType.VORTEX, Flavor.GUST);
        f(MagicType.WING, Flavor.GUST, Flavor.BODY);
        f(MagicType.EARTH, Flavor.STONE);                                       f(MagicType.PLANT, Flavor.GREEN);
        f(MagicType.SAND, Flavor.STONE, Flavor.GUST);                           f(MagicType.TRANSMUTATION, Flavor.STONE, Flavor.MAKER);
        f(MagicType.WORLD_TREE, Flavor.GREEN, Flavor.STONE);                    f(MagicType.STONE, Flavor.STONE);
        f(MagicType.ROCK, Flavor.STONE);                                        f(MagicType.MINERAL, Flavor.STONE, Flavor.GEM);
        f(MagicType.SANDSTONE, Flavor.STONE);                                   f(MagicType.RED_OCHRE, Flavor.STONE, Flavor.MAKER);
        f(MagicType.MUD, Flavor.STONE, Flavor.FLOW);                            f(MagicType.SPIKE, Flavor.STONE, Flavor.BODY);
        f(MagicType.GLASS, Flavor.GEM, Flavor.HOT);                             f(MagicType.CRYSTAL, Flavor.GEM);
        f(MagicType.CORUNDUM, Flavor.GEM, Flavor.METAL);                        f(MagicType.BRONZE, Flavor.METAL, Flavor.STONE);
        f(MagicType.COPPER, Flavor.METAL);                                      f(MagicType.IRON, Flavor.METAL);
        f(MagicType.SHAKUDO, Flavor.METAL, Flavor.GEM);                         f(MagicType.STEEL, Flavor.METAL, Flavor.BODY);
        f(MagicType.TREE, Flavor.GREEN);                                        f(MagicType.VINE, Flavor.GREEN);
        f(MagicType.BRIAR, Flavor.GREEN, Flavor.BODY);                          f(MagicType.CHERRY_BLOSSOM, Flavor.GREEN, Flavor.GUST);
        f(MagicType.FUNGUS, Flavor.GREEN, Flavor.SHADE);                        f(MagicType.POISON_PLANT, Flavor.GREEN, Flavor.SOUL);
        f(MagicType.POISON, Flavor.SOUL, Flavor.SHADE);
        f(MagicType.LIGHT, Flavor.BRIGHT);                                      f(MagicType.LIGHTNING, Flavor.BRIGHT, Flavor.GUST);
        f(MagicType.SWORD, Flavor.BRIGHT, Flavor.BODY);
        f(MagicType.DARK, Flavor.SHADE);                                        f(MagicType.SHADOW, Flavor.SHADE, Flavor.SPACE);
        f(MagicType.BLACK_OIL, Flavor.SHADE, Flavor.FLOW);                      f(MagicType.EYEBALL, Flavor.SHADE, Flavor.MIND);
        f(MagicType.SOUL, Flavor.SOUL);                                         f(MagicType.SOUL_CORPSE, Flavor.SOUL, Flavor.SHADE);
        f(MagicType.CURSE_WARDING, Flavor.SOUL, Flavor.TIME);
        f(MagicType.SPATIAL, Flavor.SPACE);                                     f(MagicType.MIRROR, Flavor.SPACE, Flavor.BRIGHT);
        f(MagicType.GRAVITY, Flavor.SPACE, Flavor.BODY);                        f(MagicType.BARRIER, Flavor.SPACE);
        f(MagicType.KEY, Flavor.SPACE, Flavor.MIND);                            f(MagicType.COMPASS, Flavor.SPACE, Flavor.MIND);
        f(MagicType.SWITCHING, Flavor.SPACE, Flavor.MIND);                      f(MagicType.PERMEATION, Flavor.SPACE, Flavor.SHADE);
        f(MagicType.TIME, Flavor.TIME);                                         f(MagicType.SEALING, Flavor.TIME, Flavor.SPACE);
        f(MagicType.REINFORCEMENT, Flavor.BODY);                                f(MagicType.BEAST, Flavor.BODY, Flavor.GREEN);
        f(MagicType.BONE, Flavor.BODY, Flavor.SOUL);                            f(MagicType.BLOOD, Flavor.BODY, Flavor.SOUL);
        f(MagicType.BODY, Flavor.BODY);                                         f(MagicType.HAIR, Flavor.BODY);
        f(MagicType.NAIL, Flavor.BODY, Flavor.METAL);                           f(MagicType.SCALE, Flavor.BODY);
        f(MagicType.SKIN, Flavor.BODY);                                         f(MagicType.TONGUE, Flavor.BODY);
        f(MagicType.SLASH, Flavor.BODY, Flavor.BRIGHT);                         f(MagicType.CHAIN, Flavor.METAL, Flavor.BODY);
        f(MagicType.CREATION, Flavor.MAKER);                                    f(MagicType.COPY, Flavor.MIND);
        f(MagicType.ILLUSION, Flavor.MIND, Flavor.BRIGHT);                      f(MagicType.DREAM, Flavor.MIND, Flavor.TIME);
        f(MagicType.THREAD, Flavor.MAKER);                                      f(MagicType.COTTON, Flavor.MAKER, Flavor.GREEN);
        f(MagicType.RECOMBINATION, Flavor.MAKER, Flavor.BODY);                  f(MagicType.PAINTING, Flavor.MAKER, Flavor.MIND);
        f(MagicType.DICE, Flavor.MIND);                                         f(MagicType.GAME, Flavor.MIND, Flavor.MAKER);
        f(MagicType.EYE, Flavor.MIND, Flavor.BRIGHT);                           f(MagicType.FOOD, Flavor.MAKER, Flavor.HOT);
        f(MagicType.LEGION, Flavor.MAKER, Flavor.BODY);                         f(MagicType.BUTOH, Flavor.MAKER, Flavor.BODY);
        f(MagicType.MEMORY, Flavor.MIND, Flavor.TIME);                          f(MagicType.MODIFICATION, Flavor.MAKER, Flavor.BODY);
        f(MagicType.SONG, Flavor.MAKER, Flavor.GUST);
    }

    /** The flavors of an attribute (empty for the ones that are never rolled). */
    public static Flavor[] flavors(MagicType m) { return FLAVORS.getOrDefault(m, new Flavor[0]); }

    /** Every attribute the tower can give: all the ones that carry flavors (never Anti-Magic, Kotodama, Curse or the Demon magics). */
    public static List<MagicType> pool() {
        List<MagicType> l = new ArrayList<>();
        for (MagicType m : MagicType.values()) if (FLAVORS.containsKey(m) && !m.restricted()) l.add(m);
        return l;
    }

    private static final Set<MagicType> RARE = EnumSet.of(MagicType.TIME, MagicType.SPATIAL, MagicType.GRAVITY, MagicType.SEALING, MagicType.CREATION,
            MagicType.SOUL, MagicType.DREAM, MagicType.WORLD_TREE, MagicType.KEY, MagicType.COMPASS, MagicType.BARRIER, MagicType.MEMORY, MagicType.COPY,
            MagicType.LEGION, MagicType.SOUL_CORPSE, MagicType.PERMEATION, MagicType.SWITCHING);

    /** A mild tilt so that the rare attributes are rarer than Fire (a tie still breaks at random). */
    public static double rarity(MagicType m) { return RARE.contains(m) ? 0.8 : 1.0; }

    /** Do two attributes share a flavor (then they are not a pair: they are the same temperament)? */
    public static boolean shares(MagicType a, MagicType b) {
        for (Flavor x : flavors(a)) for (Flavor y : flavors(b)) if (x == y) return true;
        return false;
    }

    /** The attribute's main flavor. */
    public static Flavor main(MagicType m) { Flavor[] f = FLAVORS.get(m); return f == null || f.length == 0 ? null : f[0]; }

    // ---------------------------------------------------------------- the blends
    private static final Map<Long, MagicType> BLENDS = new HashMap<>();

    private static long key(Flavor a, Flavor b) { return a.ordinal() < b.ordinal() ? a.ordinal() * 64L + b.ordinal() : b.ordinal() * 64L + a.ordinal(); }

    private static void blend(Flavor a, Flavor b, MagicType m) { BLENDS.put(key(a, b), m); }

    static {
        blend(Flavor.HOT, Flavor.FLOW, MagicType.GLASS);             // fire and water: steam-melted glass
        blend(Flavor.HOT, Flavor.STONE, MagicType.MAGMA);
        blend(Flavor.HOT, Flavor.GUST, MagicType.EXPLOSION);
        blend(Flavor.HOT, Flavor.COLD, MagicType.MIST);
        blend(Flavor.HOT, Flavor.BRIGHT, MagicType.STAR);
        blend(Flavor.HOT, Flavor.SHADE, MagicType.SMOKE);
        blend(Flavor.HOT, Flavor.GREEN, MagicType.ASH);
        blend(Flavor.HOT, Flavor.METAL, MagicType.STEEL);
        blend(Flavor.HOT, Flavor.MAKER, MagicType.FOOD);
        blend(Flavor.HOT, Flavor.BODY, MagicType.BODY);
        blend(Flavor.FLOW, Flavor.COLD, MagicType.ICE_WEDGE);
        blend(Flavor.FLOW, Flavor.STONE, MagicType.MUD);
        blend(Flavor.FLOW, Flavor.GUST, MagicType.STORM);
        blend(Flavor.FLOW, Flavor.GREEN, MagicType.FUNGUS);
        blend(Flavor.FLOW, Flavor.SHADE, MagicType.BLACK_OIL);
        blend(Flavor.FLOW, Flavor.BRIGHT, MagicType.BUBBLE);
        blend(Flavor.FLOW, Flavor.METAL, MagicType.MERCURY);
        blend(Flavor.FLOW, Flavor.MAKER, MagicType.GEL);
        blend(Flavor.COLD, Flavor.GUST, MagicType.SNOW);
        blend(Flavor.GUST, Flavor.STONE, MagicType.SAND);
        blend(Flavor.GUST, Flavor.BRIGHT, MagicType.LIGHTNING);
        blend(Flavor.GUST, Flavor.GREEN, MagicType.CHERRY_BLOSSOM);
        blend(Flavor.GUST, Flavor.MAKER, MagicType.SONG);
        blend(Flavor.GUST, Flavor.BODY, MagicType.WING);
        blend(Flavor.STONE, Flavor.GREEN, MagicType.TREE);
        blend(Flavor.STONE, Flavor.METAL, MagicType.IRON);
        blend(Flavor.STONE, Flavor.GEM, MagicType.CRYSTAL);
        blend(Flavor.STONE, Flavor.MAKER, MagicType.TRANSMUTATION);
        blend(Flavor.STONE, Flavor.BODY, MagicType.REINFORCEMENT);
        blend(Flavor.METAL, Flavor.GEM, MagicType.CORUNDUM);
        blend(Flavor.METAL, Flavor.BODY, MagicType.SCALE);
        blend(Flavor.METAL, Flavor.MAKER, MagicType.CREATION);
        blend(Flavor.BRIGHT, Flavor.SHADE, MagicType.ILLUSION);
        blend(Flavor.BRIGHT, Flavor.SPACE, MagicType.MIRROR);
        blend(Flavor.BRIGHT, Flavor.BODY, MagicType.SWORD);
        blend(Flavor.SHADE, Flavor.SPACE, MagicType.PERMEATION);
        blend(Flavor.SHADE, Flavor.SOUL, MagicType.SOUL_CORPSE);
        blend(Flavor.SHADE, Flavor.MIND, MagicType.DREAM);
        blend(Flavor.SPACE, Flavor.TIME, MagicType.SEALING);
        blend(Flavor.SPACE, Flavor.MIND, MagicType.COMPASS);
        blend(Flavor.MIND, Flavor.MAKER, MagicType.PAINTING);
        blend(Flavor.MIND, Flavor.TIME, MagicType.MEMORY);
        blend(Flavor.SOUL, Flavor.BODY, MagicType.BLOOD);
        blend(Flavor.SOUL, Flavor.TIME, MagicType.CURSE_WARDING);
        blend(Flavor.BODY, Flavor.GREEN, MagicType.BEAST);
        blend(Flavor.BODY, Flavor.SHADE, MagicType.BONE);
        blend(Flavor.BODY, Flavor.MAKER, MagicType.RECOMBINATION);
        blend(Flavor.GREEN, Flavor.MAKER, MagicType.COTTON);          // as for Charmy
    }

    /** The attribute two flavors blend into, or null when they have no well-known blend. */
    public static MagicType blendOf(Flavor a, Flavor b) { return a == null || b == null || a == b ? null : BLENDS.get(key(a, b)); }
}
