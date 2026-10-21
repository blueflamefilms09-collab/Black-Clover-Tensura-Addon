package com.newuniverse.nusmp.visual.shields;

import java.util.List;
import java.util.Set;

/**
 * 1.110.0: the shield-class items of the mod, in one place (no Minecraft classes: the renderer, the item classes' self-test and the tests all read it).
 * <p>
 * A shield-class item is one that is held up and blocks like a vanilla shield: its class answers {@code UseAnim.BLOCK}, {@code SHIELD_BLOCK} and a long use
 * duration, its model is a {@code builtin/entity} model with vanilla's shield display and a {@code blocking} override, and WeaponRenderer draws its 3D mesh on
 * vanilla's shield frame ({@link ShieldFrame}).
 * <p>
 * Not in the list on purpose: the Eisen and Heathcliff shield straps (worn on the belt / back, never held), the Vassal Mirror (a hand mirror cast from; the class
 * table files it under "shield" for its size only) and the Vassal Carriage.
 */
public final class ShieldSet {
    private ShieldSet() {}

    /** The item ids (registry paths under nusmp:), in the order the previews and the doc list them. */
    public static final List<String> IDS = List.of(
            "magebane_shield", "legendary_shield", "aegis_of_the_abyss", "celestial_support", "heros_shield", "thunder_shield", "calamity_wave_aegis");

    /** Ids the class table (assets/nusmp/weapon_classes.json) files under "shield" that are not held-up shields. */
    public static final Set<String> CLASS_ONLY = Set.of("vassal_mirror");

    /** The suffixes of the extra meshes of one shield (a curse form, the level-of-detail mesh, an orbiting piece of an upgraded mesh). */
    private static final String[] MESH_SUFFIXES = {"_curse", "_lod", "_orbit"};

    /** The item id a mesh id belongs to ({@code legendary_shield_curse} -> {@code legendary_shield}). */
    public static String baseId(String meshId) {
        String id = meshId;
        boolean again = true;
        while (again) {
            again = false;
            for (String s : MESH_SUFFIXES) {
                if (id.endsWith(s)) {
                    id = id.substring(0, id.length() - s.length());
                    again = true;
                }
            }
        }
        return id;
    }

    public static boolean isShieldItem(String itemId) { return itemId != null && IDS.contains(itemId); }

    /** True for the mesh of a shield (its own, its curse form, its level-of-detail mesh). */
    public static boolean isShieldMesh(String meshId) { return meshId != null && IDS.contains(baseId(meshId)); }
}
