package com.newuniverse.nusmp.fairytail;

import java.util.ArrayList;
import java.util.List;

import static com.newuniverse.nusmp.fairytail.FtKind.*;

/**
 * Why: the eight Fairy Tail skills (seven Uniques and the Ultimate the first one evolves into) are the same machine with different tables.
 * One enum row per skill holds its id, its text, its element and its spells in mode order; the registry, the skill class, the book item, the
 * command, the docs generator and the tests all read this one list, so a spell cannot exist in one place and not another. The mode order is
 * the save format of the selected mode: spells are only ever appended. Spells that need mastery show a number in the {@code unlockPct} column
 * (the same mastery fraction every Tensura skill has); the Ultimate repeats the Unique's spells (same ids, stronger by tier) and appends the
 * three forms Dragon Force, Lightning Flame Dragon Mode and Fire Dragon King's Mode. Canon: Fairy Tail (Hiro Mashima), where a name has no
 * exact English form the nearest canon name is used.
 */
public enum FtLine {
    FIRE_DRAGON_SLAYER("fire_dragon_slayer", "Fire Dragon Slayer Magic", false, FtElement.FIRE, "fire",
            "Unique. Natsu's Fire Dragon Slayer Magic: roar, fist, claw, wing attack, sword horn, brilliant flame and the Crimson Lotus Exploding Flame Blade. "
                    + "Eat flame to heal and to charge the next spell; fire cannot hurt you. Mastering it awakens the Fire Dragon King."),
    FIRE_DRAGON_KING("fire_dragon_king", "Fire Dragon King", true, FtElement.FIRE, "fire",
            "Ultimate. Evolved from Fire Dragon Slayer Magic by mastery. Every flame spell, far stronger, and the three Dragon Slayer forms: Dragon Force, "
                    + "Lightning Flame Dragon Mode and Fire Dragon King's Mode."),
    ICE_MAKE("ice_make", "Ice-Make Magic", false, FtElement.ICE, "",
            "Unique. Gray's Ice-Make: lance, hammer, shield, floor, cannon and the Saint, and Demon Slayer Ice, the Ice Devil's Rage. Cold cannot hurt you."),
    SKY_DRAGON_SLAYER("sky_dragon_slayer", "Sky Dragon Slayer Magic", false, FtElement.SKY, "",
            "Unique. Wendy's Sky Dragon Slayer Magic: roar, wing attack, sky drill, Troia, Vernier, the Sky Dragon's Armor and healing enchantments for the party."),
    LIGHTNING_DRAGON_SLAYER("lightning_dragon_slayer", "Lightning Dragon Slayer Magic", false, FtElement.LIGHTNING, "",
            "Unique. Laxus' Lightning Dragon Slayer Magic: roar, jaw, iron fist, halberd, breakdown fist, Lightning Body, Thunder Palace and the Raging Bolt. Lightning cannot hurt you."),
    IRON_DRAGON_SLAYER("iron_dragon_slayer", "Iron Dragon Slayer Magic", false, FtElement.IRON, "iron",
            "Unique. Gajeel's Iron Dragon Slayer Magic: club, sword, roar, scales, demon blade, the Shadow Iron Dragon, and eating iron to heal and to charge your spells."),
    CRASH("crash", "Crash", false, FtElement.CRASH, "",
            "Unique. Gildarts' Crash: whatever you touch is shattered. Crash, Crash Boom, Crash Bomb, the Gravity-free Crush and Magic Crash, which breaks the spells on your foes."),
    WATER_MAGIC("water_magic", "Water Magic", false, FtElement.WATER, "",
            "Unique. Juvia's Water Magic: Water Slicer, Water Lock, Water Nebula, Water Cane, Water Body, which turns most blows aside, and the Rain Woman.");

    public final String id, name, description, eat;
    public final boolean ultimate;
    public final FtElement element;
    private final List<FtMove> moves;

    FtLine(String id, String name, boolean ultimate, FtElement element, String eat, String description) {
        this.id = id; this.name = name; this.ultimate = ultimate; this.element = element; this.eat = eat; this.description = description;
        this.moves = List.copyOf(build(id));
    }

    /** The spells in mode order. */
    public List<FtMove> moves() { return moves; }

    public int modes() { return moves.size(); }

    public FtMove move(int mode) { return mode >= 0 && mode < moves.size() ? moves.get(mode) : null; }

    public static FtLine byId(String id) {
        for (FtLine l : values()) if (l.id.equals(id)) return l;
        return null;
    }

    /** What a mastered Unique evolves into, or null: the only evolution of the Fairy Tail skills. */
    public FtLine evolvesInto() { return this == FIRE_DRAGON_SLAYER ? FIRE_DRAGON_KING : null; }

    // ------------------------------------------------------------------------------------------------------------------ the tables
    private static List<FtMove> build(String id) {
        return switch (id) {
            case "fire_dragon_slayer" -> fire(false);
            case "fire_dragon_king" -> fire(true);
            case "ice_make" -> ice();
            case "sky_dragon_slayer" -> sky();
            case "lightning_dragon_slayer" -> lightning();
            case "iron_dragon_slayer" -> iron();
            case "crash" -> crash();
            case "water_magic" -> water();
            default -> List.of();
        };
    }

    private static FtMove m(String id, String name, FtKind kind, int unlock, double cost, int cd, double dmg, double range, double radius, int ticks, int count) {
        return FtMove.of(id, name, kind, unlock, cost, cd, dmg, range, radius, ticks, count);
    }

    private static List<FtMove> fire(boolean king) {
        List<FtMove> l = new ArrayList<>();
        l.add(m("roar", "Fire Dragon's Roar", BREATH, 0, 0.06, 100, 11, 16, 2.2, 0, 0));
        l.add(m("iron_fist", "Fire Dragon's Iron Fist", CONE, 0, 0.03, 40, 9, 3.8, 0.6, 0, 0));
        l.add(m("claw", "Fire Dragon's Claw", DASH, 0, 0.04, 70, 9, 7, 1.6, 0, 0));
        l.add(m("wing_attack", "Fire Dragon's Wing Attack", RING, 12, 0.05, 80, 8, 0, 6.5, 0, 0));
        l.add(m("sword_horn", "Fire Dragon's Sword Horn", DASH, 28, 0.07, 130, 15, 10, 1.8, 0, 0));
        l.add(m("brilliant_flame", "Fire Dragon's Brilliant Flame", TARGET_AREA, 45, 0.10, 190, 18, 26, 5.5, 0, 0));
        l.add(m("crimson_lotus", "Crimson Lotus Exploding Flame Blade", COMBO, 70, 0.14, 320, 12, 14, 5.0, 0, 3));
        l.add(m("eat_flame", "Eat Flame", EAT, 0, 0.0, 60, 0, 10, 0, 0, 0));
        if (king) {
            l.add(m("dragon_force", "Dragon Force", FORM, 0, 0.12, 900, 0, 0, 0, 1200, 0).withForm(FtForms.DRAGON_FORCE));
            l.add(m("lightning_flame_dragon_mode", "Lightning Flame Dragon Mode", FORM, 35, 0.18, 1200, 0, 0, 0, 1000, 0).withForm(FtForms.LIGHTNING_FLAME));
            l.add(m("fire_dragon_kings_mode", "Fire Dragon King's Mode", FORM, 70, 0.26, 1800, 0, 0, 0, 900, 0).withForm(FtForms.FIRE_KING));
        }
        return l;
    }

    private static List<FtMove> ice() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("lance", "Ice-Make: Lance", BARRAGE, 0, 0.04, 60, 7, 20, 0.8, 0, 3));
        l.add(m("hammer", "Ice-Make: Hammer", TARGET_AREA, 0, 0.06, 100, 14, 18, 4.0, 0, 0));
        l.add(m("shield", "Ice-Make: Shield", SHIELD, 0, 0.05, 200, 0, 0, 0, 200, 3));
        l.add(m("floor", "Ice-Make: Floor", RING, 15, 0.06, 160, 5, 0, 8.0, 100, 2));
        l.add(m("cannon", "Ice-Make: Cannon", BREATH, 32, 0.09, 170, 17, 26, 1.4, 0, 0));
        l.add(m("saint", "Ice-Make: Saint", RAIN, 50, 0.13, 320, 8, 24, 7.0, 0, 8));
        l.add(m("ice_devil_rage", "Demon Slayer Ice: Ice Devil's Rage", FORM, 65, 0.16, 1400, 0, 0, 0, 900, 0).withForm(FtForms.ICE_DEVIL));
        return l;
    }

    private static List<FtMove> sky() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("roar", "Sky Dragon's Roar", BREATH, 0, 0.06, 90, 9, 16, 2.4, 0, 0));
        l.add(m("wing_attack", "Sky Dragon's Wing Attack", RING, 0, 0.05, 70, 7, 0, 6.0, 0, 0));
        l.add(m("healing", "Troia", HEAL, 0, 0.07, 140, 9, 0, 8.0, 0, 0));
        l.add(m("sky_drill", "Shattering Light: Sky Drill", BREATH, 22, 0.08, 150, 13, 18, 1.0, 0, 0));
        l.add(m("vernier", "Vernier", ENCHANT, 15, 0.06, 260, 0, 0, 8.0, 900, 2));
        l.add(m("armor", "Sky Dragon's Armor", FORM, 30, 0.10, 500, 0, 0, 0, 800, 0).withForm(FtForms.SKY_ARMOR));
        l.add(m("enchant_troia", "Enchant: Troia", ENCHANT, 50, 0.09, 360, 0, 0, 9.0, 700, 100));
        return l;
    }

    private static List<FtMove> lightning() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("roar", "Lightning Dragon's Roar", BREATH, 0, 0.06, 100, 11, 17, 2.0, 0, 0));
        l.add(m("jaw", "Lightning Dragon's Jaw", CONE, 0, 0.04, 50, 10, 4.5, 0.55, 0, 0));
        l.add(m("iron_fist", "Lightning Dragon's Iron Fist", CONE, 0, 0.03, 40, 9, 3.6, 0.65, 0, 0));
        l.add(m("halberd", "Lightning Dragon's Halberd", BREATH, 15, 0.06, 90, 12, 20, 0.9, 0, 0));
        l.add(m("lightning_body", "Lightning Body", FORM, 22, 0.10, 600, 0, 0, 0, 500, 0).withForm(FtForms.LIGHTNING_BODY));
        l.add(m("breakdown_fist", "Lightning Dragon's Breakdown Fist", CONE, 38, 0.09, 150, 17, 4.2, 0.7, 0, 0));
        l.add(m("thunder_palace", "Thunder Palace", RAIN, 55, 0.14, 340, 9, 26, 8.0, 0, 8));
        l.add(m("raging_bolt", "Raging Bolt", BREATH, 75, 0.17, 380, 26, 28, 2.6, 0, 0));
        return l;
    }

    private static List<FtMove> iron() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("club", "Iron Dragon's Club", CONE, 0, 0.03, 50, 10, 6.5, 0.5, 0, 0));
        l.add(m("sword", "Iron Dragon's Sword", DASH, 0, 0.04, 60, 11, 7, 1.5, 0, 0));
        l.add(m("roar", "Iron Dragon's Roar", BREATH, 0, 0.06, 90, 10, 15, 2.0, 0, 0));
        l.add(m("scales", "Iron Dragon Scales", FORM, 10, 0.08, 500, 0, 0, 0, 800, 0).withForm(FtForms.IRON_SCALES));
        l.add(m("demon_blade", "Iron Dragon's Demon Blade", CONE, 30, 0.09, 130, 16, 5.5, 0.9, 0, 0));
        l.add(m("shadow_iron", "Shadow Iron Dragon", FORM, 60, 0.16, 1300, 0, 0, 0, 900, 0).withForm(FtForms.SHADOW_IRON));
        l.add(m("eat_iron", "Eat Iron", EAT, 0, 0.0, 60, 0, 0, 0, 0, 0));
        return l;
    }

    private static List<FtMove> crash() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("crash", "Crash", CONE, 0, 0.05, 60, 18, 3.8, 0.7, 0, 0));
        l.add(m("magic_crash", "Magic Crash", DISPEL, 10, 0.06, 120, 8, 6.0, 0.8, 0, 0));
        l.add(m("crash_boom", "Crash Boom", RING, 20, 0.10, 150, 14, 0, 8.5, 0, 0));
        l.add(m("crash_bomb", "Crash Bomb", BOMB, 40, 0.11, 190, 20, 20, 6.0, 40, 0));
        l.add(m("gravity_free_crush", "Gravity-free Crush", LIFT, 60, 0.15, 280, 22, 0, 8.0, 30, 0));
        l.add(m("aerial_crash", "Aerial Crash", DASH, 80, 0.12, 200, 24, 11, 2.0, 0, 0));
        return l;
    }

    private static List<FtMove> water() {
        List<FtMove> l = new ArrayList<>();
        l.add(m("water_slicer", "Water Slicer", BREATH, 0, 0.03, 40, 8, 15, 0.7, 0, 0));
        l.add(m("water_lock", "Water Lock", LOCK, 10, 0.06, 200, 3, 16, 1.5, 70, 0));
        l.add(m("water_nebula", "Water Nebula", RING, 20, 0.08, 110, 10, 0, 6.0, 0, 0));
        l.add(m("water_body", "Water Body", FORM, 30, 0.09, 450, 0, 0, 0, 160, 0).withForm(FtForms.WATER_BODY));
        l.add(m("water_cane", "Water Cane", BREATH, 45, 0.09, 120, 15, 22, 0.9, 0, 0));
        l.add(m("rain_woman", "Rain Woman", FORM, 60, 0.12, 900, 0, 0, 0, 800, 0).withForm(FtForms.RAIN_WOMAN));
        return l;
    }
}
