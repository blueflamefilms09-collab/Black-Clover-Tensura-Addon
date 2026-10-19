package com.newuniverse.nusmp.client.playermodel;

import java.util.List;
import java.util.Set;

/**
 * Why: which entities the kit draws, as plain names with no game class, so the registration ({@link PlayerModelRegistry}, client only), the audit and the tests (which cannot load a renderer)
 * read one list. Keep in step with docs/playermodel.md, which lists the same conversions and, next to them, every humanoid that was NOT converted and the renderer class it still has.
 */
public final class PlayerModelTable {
    private PlayerModelTable() {}

    /** The arcs' bosses that are people (the others, the DanMachi monsters, keep their own models). Each id must have a look in looks.json. */
    public static final Set<String> ARC_BOSSES = Set.of("heath_grice", "vetto", "lotus_whomalt", "mars", "revchi_salik", "meliodas", "merlin", "king", "escanor", "ban", "diane", "gowther",
            "fana", "rhya", "rades_spirito", "patolli", "lucifero", "morris_libardirt", "zagred", "udaeus");

    /** The entity ids (the path of the registry name) the kit draws besides the arcs' bosses. */
    public static final List<String> CONVERTED_ENTITIES = List.of("arc_npc", "dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis", "dark_disciple", "zogratis_clone",
            "devil_avatar", "zagred_legacy", "god_of_order", "quest_giver_npc", "merchant_npc", "tower_wizard", "painted_construct", "player_avatar");

    /** The looks the bodies ask for by fixed name (a missing one is drawn with the default look and logged). */
    public static final List<String> FIXED_LOOKS = List.of("dante_zogratis", "dante_zogratis_form2", "vanica_zogratis", "vanica_zogratis_form2", "zenon_zogratis", "zenon_zogratis_form2",
            "lucius_zogratis", "lucius_zogratis_form2", "dark_disciple", "dark_disciple_lucifero", "dark_disciple_megicula", "dark_disciple_beelzebub", "dark_disciple_astaroth", "devil_avatar",
            "devil_avatar_form2", "devil_avatar_megicula", "devil_avatar_megicula_form2", "zagred_legacy", "zagred_legacy_form2", "god_of_order", "god_of_order_form2", "quest_giver_npc",
            "merchant_npc", "tower_wizard", "painted_knight", "npc_default");
}
