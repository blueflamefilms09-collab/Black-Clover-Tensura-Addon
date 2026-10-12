package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.BookPalette;
import com.newuniverse.nusmp.wizard.MagicFlavors.Flavor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.16.0: a magic that is born. When two attributes fuse and the game has no attribute for their blend, a new magic comes into being
 * for that mage: it gets a name made of the two parents' temperaments ("Pyro" + "tide" = Pyrotide), a palette made of the two parents'
 * glows, a cover of its own (grimoire.FusionDesign) and its own spells (book.SignatureSpell: the Signature Spell and the born spells,
 * drawn by the procedural effect layer vfx.client.layer.BornMagicLayer). Nothing has to exist in the game beforehand: everything about
 * a born magic is generated from the pair, so any two attributes can be born into something.
 */
public final class BornMagic {
    private BornMagic() {}

    private static final Map<Flavor, String[]> WORDS = new EnumMap<>(Flavor.class);   // {prefix, suffix}

    static {
        WORDS.put(Flavor.HOT, new String[]{"Pyro", "fire"});          WORDS.put(Flavor.COLD, new String[]{"Cryo", "frost"});
        WORDS.put(Flavor.FLOW, new String[]{"Hydro", "tide"});        WORDS.put(Flavor.GUST, new String[]{"Aero", "gale"});
        WORDS.put(Flavor.STONE, new String[]{"Geo", "stone"});        WORDS.put(Flavor.METAL, new String[]{"Ferro", "steel"});
        WORDS.put(Flavor.GEM, new String[]{"Prism", "gem"});          WORDS.put(Flavor.GREEN, new String[]{"Flora", "bloom"});
        WORDS.put(Flavor.BRIGHT, new String[]{"Lumi", "light"});      WORDS.put(Flavor.SHADE, new String[]{"Umbra", "shade"});
        WORDS.put(Flavor.SOUL, new String[]{"Anima", "soul"});        WORDS.put(Flavor.SPACE, new String[]{"Void", "rift"});
        WORDS.put(Flavor.TIME, new String[]{"Chrono", "time"});       WORDS.put(Flavor.BODY, new String[]{"Soma", "flesh"});
        WORDS.put(Flavor.MIND, new String[]{"Psyche", "mind"});       WORDS.put(Flavor.MAKER, new String[]{"Forge", "craft"});
    }

    /** The name of the magic born of two attributes: the first one's prefix and the second one's suffix. */
    public static String nameOf(MagicType a, MagicType b) {
        Flavor fa = MagicFlavors.main(a), fb = MagicFlavors.main(b);
        String pre = fa == null ? a.word : WORDS.get(fa)[0];
        String suf = fb == null ? b.word.toLowerCase(java.util.Locale.ROOT) : WORDS.get(fb)[1];
        return pre + suf;
    }

    /** The two parents' glows as ARGB (the born magic's palette). */
    public static int colorOf(MagicType m) { return 0xFF000000 | BookPalette.glow(m.name()); }

    /** "FLAME:WATER" for a pair. */
    public static String fusionOf(MagicType a, MagicType b) { return a.name() + ":" + b.name(); }
}
