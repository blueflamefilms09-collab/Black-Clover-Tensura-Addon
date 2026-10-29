package com.newuniverse.nusmp.relics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure table: the grade and the weapon type of the fixed weapons that are not Relic defs (the Black Clover weapons, the hero
 * weapons, the Otherworld Trident, the Magic Tool Sword and Spear). A relic def has its own grade; a Djinn weapon takes {@link #djinn}.
 * The damage of each is {@link GradeDamage#damage(int, String)} of its entry (docs/grade_damage.md has the before and after).
 * Keys are the lower-case enum or registry names. Elsdocia is not listed: it keeps its own 400.
 */
public final class WeaponGrades {
    private WeaponGrades() {}

    public record Entry(int grade, String type) {
        public double damage() { return GradeDamage.damage(grade, type); }
    }

    private static final Map<String, Entry> T = new LinkedHashMap<>();

    private static void e(String key, int grade, String type) { T.put(key, new Entry(grade, type)); }

    static {
        // Black Clover / reference weapons (MagicWeaponItem.Kind names); the grades of the adopted ones match their Relic cards (Wave0)
        e("demon_slasher_katana", 3, "katana");
        e("miasma_katana", 3, "katana");
        e("spell_forged_rapier", 2, "rapier");
        e("severing_greatsword", 4, "greatsword");
        e("demon_slayer", 6, "greatsword");                  // the Genesis Demon-Slayer
        e("demon_dweller", 4, "longsword");
        e("demon_destroyer", 4, "longsword");
        e("licht_dweller", 4, "longsword");
        e("licht_destroyer", 4, "longsword");
        e("rimeheart", 4, "greatsword");
        e("last_word", 3, "shortsword");
        e("quicklings_ivy", 2, "rapier");
        e("magebane_longsword", 3, "longsword");
        e("prism_blade", 2, "longsword");
        e("thundercleave", 2, "greatsword");
        e("bloodbound_katana", 4, "katana");
        e("permafrost_sabre", 2, "sabre");
        e("cyclone_rapier", 2, "rapier");
        e("bolt_thrower", 2, "crossbow");
        e("crossbow_of_swift_death", 2, "crossbow");
        e("club_of_rot", 1, "club");
        e("doom_hammer", 1, "hammer");
        e("magebane_shield", 3, "shield");
        e("weapon_of_resurgence", 1, "longsword");
        e("brooch_pin", 1, "dagger");
        e("hand_of_providence", 3, "handitem");
        e("butcher_cleaver", 1, "cleaver");
        // other fixed weapons
        e("otherworld_trident", 3, "trident");
        e("magic_tool_sword", 0, "longsword");
        e("magic_tool_spear", 0, "spear");
        // hero weapons (HeroKind.id()): the four Holy Weapons are Legend, the Seven Star Weapons Unique, the Vassal Weapons Rare
        e("legendary_shield", 4, "shield");
        e("legendary_sword", 4, "longsword");
        e("legendary_spear", 4, "spear");
        e("legendary_bow", 4, "bow");
        e("vassal_hammer", 3, "hammer");
        e("vassal_claw", 3, "dagger");
        e("vassal_axe", 3, "battleaxe");
        e("vassal_whip", 3, "whip");
        e("vassal_staff", 3, "staff");
        e("vassal_gauntlet", 3, "shortsword");
        e("vassal_projectile", 3, "bow");
        e("vassal_carriage", 2, "shield");
        e("vassal_fan", 2, "sabre");
        e("vassal_scythe", 2, "scythe");
        e("vassal_katana", 2, "katana");
        e("vassal_book", 2, "wand");
        e("vassal_mirror", 2, "wand");
        e("vassal_instrument", 2, "wand");
        e("vassal_harpoon", 2, "spear");
        e("vassal_sewing_set", 2, "wand");
        e("vassal_chain", 2, "whip");
    }

    /** The entry of a key (case-insensitive), or null. */
    public static Entry of(String key) { return key == null ? null : T.get(key.toLowerCase(java.util.Locale.ROOT)); }

    public static Map<String, Entry> all() { return java.util.Collections.unmodifiableMap(T); }

    /** The total attack damage of a listed weapon, or {@code fallback} if the key is not listed. */
    public static double damageOr(String key, double fallback) {
        Entry en = of(key);
        return en == null ? fallback : en.damage();
    }

    /** A Djinn weapon: Legend in its light form, God in its dark form (the dark form was 10% stronger); the type is the weapon kind of its row. */
    public static Entry djinn(String kind, boolean dark) { return new Entry(dark ? 5 : 4, kind); }
}
