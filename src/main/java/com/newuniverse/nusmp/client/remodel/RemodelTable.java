package com.newuniverse.nusmp.client.remodel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: which mob is drawn by which art-kit model, in ONE pure place (no game classes), so the renderer registration, the tests and the doc agree. Every custom mob of the mod that is not the
 * Old Witch (an arc NPC), a boss that already has a kit model (the arc bosses), the Mirror Double or the Player Avatar (those two are meant to look like the player) is listed here with the
 * models its renderer may draw: the art kit writes {@code assets/nusmp/{geo,animations}/entity/arc_<model>.*} and {@code textures/entity/arc_<model>[_glow].png} (tools/arc/char_<model>.py,
 * docs/arc/remodel.md), a second form is {@code arc_<model>_form2}, and a skin is a texture {@code arc_<model>_<skin>} that tools/remodel/gen_remodel.py repaints from the same body.
 * A mob that is added to the mod and remodelled gets a row here, a character file, and nothing else (the registration loops over this table).
 */
public final class RemodelTable {
    private RemodelTable() {}

    /** Which renderer draws it: a framework boss, any other living mob, the cotton cloud (not a living entity), or the legacy Zagred (aura shader and target ring kept). */
    public enum Family { BOSS, MOB, CLOUD, ZAGRED }

    /** One mob: the registered entity id ({@code nusmp:<id>}), its renderer family, and every model name its renderer may draw (the first is the default). */
    public record Entry(String entityId, Family family, List<String> models) {
        public Entry {
            models = List.copyOf(models);
        }
    }

    private static Entry e(String id, Family family, String... models) { return new Entry(id, family, List.of(models)); }

    /** Every remodelled mob. Entity ids never change (saves keep working); only the model, texture, animation and renderer did. */
    public static final List<Entry> ENTRIES = List.of(
            e("dante_zogratis", Family.BOSS, "dante_zogratis"),
            e("vanica_zogratis", Family.BOSS, "vanica_zogratis"),
            e("zenon_zogratis", Family.BOSS, "zenon_zogratis"),
            e("lucius_zogratis", Family.BOSS, "lucius_zogratis"),
            e("zogratis_clone", Family.MOB, "lucius_zogratis"),
            e("dark_disciple", Family.MOB, "dark_disciple"),
            e("blood_beast", Family.MOB, "blood_beast"),
            e("devil_avatar", Family.BOSS, "devil_avatar"),
            e("god_of_order", Family.BOSS, "god_of_order"),
            e("goliath", Family.BOSS, "goliath"),
            e("charybdis_spawn", Family.BOSS, "charybdis_spawn"),
            e("floor_warden", Family.MOB, "floor_warden"),
            e("labyrinth_guardian", Family.MOB, "floor_warden"),
            e("living_armor", Family.MOB, "living_armor"),
            e("dungeon_direwolf", Family.MOB, "dungeon_direwolf"),
            e("basilisk", Family.MOB, "basilisk"),
            e("dungeon_slime", Family.MOB, "dungeon_slime"),
            e("spirit_lord", Family.MOB, "spirit_salamander", "spirit_undine", "spirit_sylph", "spirit_gnome", "spirit_black_devil"),
            e("elemental_spirit_lord", Family.MOB, "spirit_salamander", "spirit_undine", "spirit_sylph", "spirit_gnome"),
            e("wind_spirit_lord", Family.MOB, "spirit_sylph"),
            e("grimoire_daemon", Family.MOB, "grimoire_daemon", "grimoire_daemon_greater", "grimoire_daemon_arch", "grimoire_daemon_stone"),
            e("painted_construct", Family.MOB, "painted_beast", "painted_knight", "painted_giant"),
            e("rouge", Family.MOB, "rouge"),
            e("tower_wizard", Family.MOB, "tower_wizard"),
            e("cotton_sheep", Family.MOB, "cotton_sheep", "cotton_sheep_cook"),
            e("cotton_cloud", Family.CLOUD, "cotton_cloud"),
            e("zagred_legacy", Family.ZAGRED, "zagred_legacy"));

    /** The models that also have a second form ({@code arc_<model>_form2}): the one a boss wears from its transformation on. */
    public static final Set<String> SECOND_FORMS = Set.of("dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis", "devil_avatar", "god_of_order", "goliath",
            "charybdis_spawn", "zagred_legacy");

    /** Skins: a model repainted from the same body, {@code arc_<model>_<skin>.png} (and {@code _glow}); a skin of the second form is {@code <skin>_form2}. */
    public static final Map<String, List<String>> SKINS = Map.of(
            "dark_disciple", List.of("lucifero", "megicula", "beelzebub", "astaroth"),
            "devil_avatar", List.of("megicula", "megicula_form2"));

    /** The ids of the mobs that stay as they are, and why (docs/arc/remodel.md): they are meant to look like the player. */
    public static final List<String> LEFT_AS_THEY_ARE = List.of("mirror_double", "player_avatar");

    /** The entry of an entity id, or null when it is not remodelled. */
    public static Entry of(String entityId) {
        for (Entry en : ENTRIES) if (en.entityId().equals(entityId)) return en;
        return null;
    }

    /** Every model name the table draws, in table order without repeats (the second forms are added by {@link #withSecondForms}). */
    public static List<String> models() {
        Set<String> out = new LinkedHashSet<>();
        for (Entry en : ENTRIES) out.addAll(en.models());
        return new ArrayList<>(out);
    }

    /** {@link #models()} plus the second form of every model that has one. */
    public static List<String> withSecondForms() {
        List<String> out = new ArrayList<>();
        for (String m : models()) {
            out.add(m);
            if (SECOND_FORMS.contains(m)) out.add(m + "_form2");
        }
        return out;
    }

    /** The suffix of the name of a second form's model. */
    public static final String FORM2 = "_form2";

    /** The name of the second form of a model. */
    public static String secondForm(String model) { return model + FORM2; }

    /** The resource stem of a model: {@code arc_<model>}. */
    public static String stem(String model) { return "arc_" + model; }

    /** The texture name of a skin of a model: {@code arc_<model>_<skin>}. */
    public static String skinStem(String model, String skin) { return stem(model) + "_" + skin; }
}
