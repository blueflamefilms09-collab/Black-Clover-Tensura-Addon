package com.newuniverse.nusmp.fairytail;

import java.util.List;

/**
 * Why: the timed forms of the Fairy Tail skills (Dragon Force, Lightning Flame Dragon Mode, Fire Dragon King's Mode, Ice Devil's Rage, the
 * Sky Dragon's Armor, Lightning Body, Iron Dragon Scales, the Shadow Iron Dragon, Water Body, Rain Woman) are all "armour on the body for
 * a while, a set of effects and a damage bonus". Each is one row here; the row's index is also the STYLE number the armour render layer is
 * sent ({@code Aura.FAIRY_TAIL}, drawn by {@code client.aura.FairyTailAura} with the geo pieces {@code ft_<key>_arm_*} made by
 * tools/gen_fairy_tail_geo.py), so the list is append-only: an index is never reused. Minecraft-free.
 *
 * @param physicalCut share of non-elemental damage the form turns aside (Water Body, Iron Scales)
 * @param immune      which damage family the form is immune to: "fire", "lightning", "freezing", "" for none
 */
public record FtForms(String key, String name, FtElement element, double dmgMul, int speedAmp, int strengthAmp, int resistAmp, int absorbAmp,
                      int regenAmp, double physicalCut, String immune) {

    public static final int DRAGON_FORCE = 0, LIGHTNING_FLAME = 1, FIRE_KING = 2, ICE_DEVIL = 3, SKY_ARMOR = 4, LIGHTNING_BODY = 5,
            IRON_SCALES = 6, SHADOW_IRON = 7, WATER_BODY = 8, RAIN_WOMAN = 9;

    /** Index = the armour style sent to the client. Append only. */
    public static final List<FtForms> ALL = List.of(
            new FtForms("dragon_force", "Dragon Force", FtElement.FIRE, 1.45, 1, 1, 1, 1, 0, 0.0, "fire"),
            new FtForms("lightning_flame", "Lightning Flame Dragon Mode", FtElement.FIRE, 1.75, 2, 2, 1, 1, 0, 0.0, "fire"),
            new FtForms("fire_king", "Fire Dragon King's Mode", FtElement.FIRE, 2.2, 2, 3, 2, 2, 0, 0.1, "fire"),
            new FtForms("ice_devil", "Ice Devil's Rage", FtElement.ICE, 1.5, 0, 1, 1, 2, 0, 0.0, "freezing"),
            new FtForms("sky_armor", "Sky Dragon's Armor", FtElement.SKY, 1.2, 1, 0, 1, 1, 0, 0.0, ""),
            new FtForms("lightning_body", "Lightning Body", FtElement.LIGHTNING, 1.35, 3, 0, 0, 0, 0, 0.0, "lightning"),
            new FtForms("iron_scales", "Iron Dragon Scales", FtElement.IRON, 1.2, 0, 1, 2, 2, 0, 0.25, ""),
            new FtForms("shadow_iron", "Shadow Iron Dragon", FtElement.IRON, 1.6, 1, 2, 1, 2, 0, 0.2, ""),
            new FtForms("water_body", "Water Body", FtElement.WATER, 1.15, 1, 0, 0, 0, 0, 0.8, ""),
            new FtForms("rain_woman", "Rain Woman", FtElement.WATER, 1.1, 1, 0, 0, 1, 1, 0.1, ""));

    public static FtForms get(int index) { return index >= 0 && index < ALL.size() ? ALL.get(index) : null; }

    /** Armour piece names every form has (tools/gen_fairy_tail_geo.py builds exactly these, the painter lists the same). */
    public static final List<String> PIECES = List.of("helm", "chest", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "accent");

    /** The geo key of a form: assets/nusmp/geo/entity/ft_&lt;key&gt;_arm_&lt;piece&gt;.geo.json. */
    public String geoKey() { return "ft_" + key; }
}
