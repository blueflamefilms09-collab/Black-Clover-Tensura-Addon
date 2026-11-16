package com.newuniverse.nusmp.client.playermodel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every devil of the Devil Contract roster ({@code WorldDevil}) has a player-model look in the style of the Black Clover boss looks (docs/devil_looks.md): id {@code dv_<devil id>}, in
 * {@code looks_dv.json}, written by {@code tools/bc_playermodels/gen_dv.py}. This pure table (no game class, so the unit test and the CI self-test read it) says which devil ids have one; a renderer asks
 * {@link #of} with the devil's id and gets the look id only while the client option {@code bossLooks.devilPlayerModel} is on (see {@link DevilExtras#lookFor}). The old devil figures (the Blender
 * meshes of docs/devil_forms.md and the generic horns and wings) are not touched: they are one switch away. A lookup is one map get, no string is built per frame (the map is built once).
 */
public final class DevilLooks {
    private DevilLooks() {}

    /** The prefix of every devil look. */
    public static final String PREFIX = "dv_";

    /** The devil ids that have a look, in the roster's order of WorldDevil. Add-only: an entry here needs its look in looks_dv.json (DevilPlayerModelsTest). */
    private static final List<String> IDS = List.of("liebe", "lucifero", "beelzebub", "astaroth", "megicula", "lucifugus", "fox_devil", "curse_devil", "future_devil", "ghost_devil", "sebastian",
            "kin_of_iblis", "kin_of_egyn", "kin_of_amaimon", "kin_of_astaroth", "kin_of_lucifer", "daemon_noir", "daemon_blanc", "daemon_jaune", "daemon_violet", "daemon_rouge", "daemon_vert",
            "daemon_bleu", "hall_demon", "god_hand", "etherious");

    private static final Map<String, String> LOOK_OF = new HashMap<>();

    static {
        for (String id : IDS) LOOK_OF.put(id, PREFIX + id);
    }

    /** The look id of a devil id, or null when the devil has none. */
    public static String of(String devilId) { return devilId == null ? null : LOOK_OF.get(devilId); }

    /** Is this the id of a devil look? */
    public static boolean isDevil(String lookId) { return lookId != null && lookId.startsWith(PREFIX); }

    /** Every devil id that has a look (for the tests and docs). */
    public static List<String> devilIds() { return IDS; }
}
