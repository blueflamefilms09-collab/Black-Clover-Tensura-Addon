package com.newuniverse.nusmp.client.playermodel;

import java.util.Map;
import java.util.Set;

/**
 * Why: wave 2 of the redesigned Black Clover boss looks (docs/bosses_bc_wave2.md): the Dark Disciples, the Devil Avatar, the God of Order and the early / Spade arc bosses Heath Grice, Lotus Whomalt,
 * Mars, Revchi Salik, Vetto and Morris Libardirt. Same shape as {@link BcLooks} (old look id to {@code bc_<old id>}, written by {@code tools/bc_playermodels/gen_bc.py} into looks_bc.json, and the
 * particle theme of each look's shimmer), kept in its own pure class (no game class) so wave 1's table is not touched; {@link BcLooks#of} and {@link BcLooks#theme} fall through to it.
 *
 * <p>The God of Order is the one entry with its own switch: its Blender body (boss rework wave 3) is kept as the default, and this look is the selectable one ({@code bossLooks.godOfOrderPlayerModel},
 * default off). Every other entry follows {@code bossLooks.bcPlayerModel} like wave 1.</p>
 */
public final class BcWave2 {
    private BcWave2() {}

    /** Old look id to the redesigned look id. Add-only: an entry needs its look in looks_bc.json (BossesBcWave2Test, BcAudit). */
    private static final Map<String, String> NEW_OF = Map.ofEntries(
            Map.entry("dark_disciple", "bc_dark_disciple"), Map.entry("dark_disciple_lucifero", "bc_dark_disciple_lucifero"), Map.entry("dark_disciple_megicula", "bc_dark_disciple_megicula"),
            Map.entry("dark_disciple_beelzebub", "bc_dark_disciple_beelzebub"), Map.entry("dark_disciple_astaroth", "bc_dark_disciple_astaroth"),
            Map.entry("devil_avatar", "bc_devil_avatar"), Map.entry("devil_avatar_form2", "bc_devil_avatar_form2"),
            Map.entry("devil_avatar_megicula", "bc_devil_avatar_megicula"), Map.entry("devil_avatar_megicula_form2", "bc_devil_avatar_megicula_form2"),
            Map.entry("god_of_order", "bc_god_of_order"), Map.entry("god_of_order_form2", "bc_god_of_order_form2"),
            Map.entry("heath_grice", "bc_heath_grice"), Map.entry("heath_grice_form2", "bc_heath_grice_form2"),
            Map.entry("lotus_whomalt", "bc_lotus_whomalt"),
            Map.entry("mars", "bc_mars"), Map.entry("mars_form2", "bc_mars_form2"),
            Map.entry("revchi_salik", "bc_revchi_salik"),
            Map.entry("vetto", "bc_vetto"), Map.entry("vetto_form2", "bc_vetto_form2"),
            Map.entry("morris_libardirt", "bc_morris_libardirt"), Map.entry("morris_libardirt_form2", "bc_morris_libardirt_form2"));

    /** The particle theme of each look's shimmer (an id of particle_themes.json: a boss theme or the magic theme of the character's attribute). */
    private static final Map<String, String> THEME_OF = Map.ofEntries(
            Map.entry("bc_dark_disciple", "boss.zagred"), Map.entry("bc_dark_disciple_lucifero", "boss.zagred"), Map.entry("bc_dark_disciple_megicula", "boss.vanica"),
            Map.entry("bc_dark_disciple_beelzebub", "boss.zenon"), Map.entry("bc_dark_disciple_astaroth", "boss.lucius"),
            Map.entry("bc_devil_avatar", "boss.avatar"), Map.entry("bc_devil_avatar_form2", "boss.avatar"),
            Map.entry("bc_devil_avatar_megicula", "boss.avatar"), Map.entry("bc_devil_avatar_megicula_form2", "boss.avatar"),
            Map.entry("bc_god_of_order", "boss.celestial"), Map.entry("bc_god_of_order_form2", "boss.celestial"),
            Map.entry("bc_heath_grice", "magic.ICE"), Map.entry("bc_heath_grice_form2", "boss.celestial"),
            Map.entry("bc_lotus_whomalt", "magic.SMOKE"),
            Map.entry("bc_mars", "magic.CRYSTAL"), Map.entry("bc_mars_form2", "magic.CRYSTAL"),
            Map.entry("bc_revchi_salik", "magic.CHAIN"),
            Map.entry("bc_vetto", "magic.BEAST"), Map.entry("bc_vetto_form2", "magic.BEAST"),
            Map.entry("bc_morris_libardirt", "magic.DARK"), Map.entry("bc_morris_libardirt_form2", "boss.zagred"));

    /** The looks that follow their own switch ({@code bossLooks.godOfOrderPlayerModel}) instead of {@code bossLooks.bcPlayerModel}. */
    private static final Set<String> ORDER = Set.of("god_of_order", "god_of_order_form2");

    /** The redesigned look id of an old look id, or null. */
    public static String of(String oldId) { return oldId == null ? null : NEW_OF.get(oldId); }

    /** The particle theme of a redesigned look's shimmer, or null. */
    public static String theme(String bcId) { return bcId == null ? null : THEME_OF.get(bcId); }

    /** Is this old look id one of the God of Order's (its own switch)? */
    public static boolean isOrder(String oldId) { return oldId != null && ORDER.contains(oldId); }

    /** Every old look id that has a wave 2 look (for the audit, the tests and the docs). */
    public static Set<String> oldIds() { return NEW_OF.keySet(); }
}
