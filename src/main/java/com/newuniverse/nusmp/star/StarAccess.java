package com.newuniverse.nusmp.star;

import java.util.Set;

/**
 * Star Magic for everybody (docs/star_vfx_for_all.md): who may open and cast the new Star pages, and so who sees the new Star effects. Pure (no Minecraft class), so a table of kingdoms, characters and
 * "nobody" runs in the unit tests and in the CI self-test.
 *
 * <p>Why it exists. The seven StarV2 effects belong to the seven new pages of {@code book_star}, and until 3.7.0 six of those pages (and the whip of the Quartile Flagellum) waited for the Grinberryall heir mark
 * (a Spade cover + the cleared Spade Kingdom Raid + the signature mastery). The look itself was never limited: the client layer, the prop painter and the effect packet filter nobody by cover or kingdom. So a
 * Star mage of any other kingdom (or the basic grimoire) only ever saw the Star Seed star and the Conjunction flash. With {@code star.vfxForAll} (default true) the shared pages open for every Star grimoire by the
 * ordinary mastery order, whatever its cover, character or lack of one. What stays heir-only whatever the setting is {@link #ALWAYS_HEIR}: Black Neverland (Yuno's Union Magic: the heir mark, the Sylph bond and
 * an anti-magic source). With the setting false the 3.7.0 table applies exactly ({@link StarRules#needsHeir}).
 *
 * <p>The heir keeps what only the heir had besides the pages: the eighth star ({@code maxPerHeir}), the 1.15 damage of the Hasta, the halved chance that its stars break to Anti-Magic. Nothing here reads a kingdom;
 * the one place that reads the cover is {@link StarGame#isHeir}, and only for those heir facts.
 */
public final class StarAccess {
    private StarAccess() {}

    public static final String CONFIG_KEY = "vfxForAll";
    public static final boolean DEFAULT_VFX_FOR_ALL = true;

    /** The pages that need the heir mark whatever {@code star.vfxForAll} says. */
    public static final Set<String> ALWAYS_HEIR = Set.of(StarRules.BLACK_NEVERLAND);

    /** The pages {@code star.vfxForAll} opens to every Star grimoire (the whip of the Quartile Flagellum included). */
    public static final Set<String> SHARED = Set.of(StarRules.SCUTUM, StarRules.HASTA, StarRules.FLAGELLUM, StarRules.QUINTILE_SCUTUM, StarRules.QUINTILE_HASTA, StarRules.QUINTILE_FLAGELLUM);

    /** Does casting this page need the heir mark? (The Flagellum counts: its whip is for the heir when the setting is off, a mage who is not one gets the old cone.) */
    public static boolean needsHeir(String pageId, boolean vfxForAll) {
        return vfxForAll ? ALWAYS_HEIR.contains(pageId) : StarRules.needsHeir(pageId);
    }

    /** May this mage open / cast this page? {@code heir} is only read when the page needs it. */
    public static boolean allowed(String pageId, boolean heir, boolean vfxForAll) {
        return heir || !needsHeir(pageId, vfxForAll);
    }

    /** Does the Quartile Flagellum throw the whip (true) or swing the old cone (false)? */
    public static boolean whip(boolean heir, boolean vfxForAll) { return vfxForAll || heir; }

    /**
     * The whole decision from the facts, for the tests and the self-test: a mage with a Star grimoire of this kingdom cover ({@code kingdom} is the kingdom's name, null or any other for none), at this mastery
     * and with the Spade Raid cleared or not. {@code hasStarGrimoire} false = no Star magic at all, nothing is allowed. The character a grimoire is bound to is deliberately not an argument: it changes nothing.
     */
    public static boolean mayCast(String pageId, boolean hasStarGrimoire, String kingdom, double mastery, double heirNeed, boolean raidCleared, boolean vfxForAll) {
        if (!hasStarGrimoire) return false;
        boolean heir = StarRules.isHeir("SPADE".equals(kingdom), mastery, heirNeed, raidCleared);
        return allowed(pageId, heir, vfxForAll);
    }
}
