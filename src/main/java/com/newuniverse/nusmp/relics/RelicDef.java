package com.newuniverse.nusmp.relics;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * One relic, as the waves describe it. Built fluently from {@link #of}; every call returns a changed copy, so a def is immutable once it is
 * handed to the catalog. The core registers a def as an item (or, for {@link #adopt}, layers it onto an item the mod already has) and the
 * engine runs its passives and acts.
 * <p>
 * Numbers a wave may leave out get defaults from the grade ({@link #meleeDamage()}, {@link #armorDefence()}).
 */
public final class RelicDef {
    private final String id, cardName;
    private final Grade grade;
    private final RelicSlot slot;
    private boolean attune, model3d, vessel, paletteSet;
    private String desc = "";
    private List<Passive> passives = List.of();
    private List<Act> acts = List.of();
    private List<String> engravings = List.of();
    private double damage = Double.NaN, speed = Double.NaN, toughness = -1;
    private int defence = -1;
    private int main = 0x8C8C9C, trim = 0xD8C070, glow = 0xFFE9A0;
    private Look look;
    private Supplier<? extends Item> adopted;
    private RangedKind ranged = RangedKind.NONE;
    /** 1.29.0: a PAIR (two hand items that come together). {@code pairLeftName} is set on the right half (the primary def), {@code pairRightId} on the generated left half. */
    private String pairLeftName, pairRightId;

    private RelicDef(String id, String cardName, Grade grade, RelicSlot slot) {
        this.id = id;
        this.cardName = cardName;
        this.grade = grade;
        this.slot = slot;
    }

    private RelicDef copy() {
        RelicDef d = new RelicDef(id, cardName, grade, slot);
        d.attune = attune;
        d.model3d = model3d;
        d.vessel = vessel;
        d.paletteSet = paletteSet;
        d.desc = desc;
        d.passives = passives;
        d.acts = acts;
        d.engravings = engravings;
        d.damage = damage;
        d.speed = speed;
        d.toughness = toughness;
        d.defence = defence;
        d.main = main;
        d.trim = trim;
        d.glow = glow;
        d.look = look;
        d.adopted = adopted;
        d.ranged = ranged;
        d.pairLeftName = pairLeftName;
        d.pairRightId = pairRightId;
        return d;
    }

    /** {@code id} is the registry path (snake_case, unique across all waves); {@code cardName} is the name on the card. */
    public static RelicDef of(String id, String cardName, Grade grade, RelicSlot slot) {
        if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("relic id must be snake_case: " + id);
        if (grade == null || slot == null) throw new IllegalArgumentException("relic " + id + " needs a grade and a slot");
        return new RelicDef(id, cardName == null ? id : cardName, grade, slot);
    }

    /** The registry path of a card name: lower case, apostrophes dropped, every other run of non-alphanumerics becomes one "_", trimmed. */
    public static String slug(String name) {
        if (name == null) return "";
        String s = name.toLowerCase(Locale.ROOT).replace("'", "").replace("’", "").replace("‘", "");
        s = s.replaceAll("[^a-z0-9]+", "_");
        return s.replaceAll("^_+|_+$", "");
    }

    // ---------------------------------------------------------------- the fluent part
    /** Needs attunement: it works only for the player it is attuned to (at most three such relics per player). */
    public RelicDef attune() { RelicDef d = copy(); d.attune = true; return d; }

    /** A 3D hand mesh exists (weapon_meshes/&lt;id&gt;.txt, textures/entity/weapon/&lt;id&gt;.png and _glow.png, models/item/&lt;id&gt;_icon.json). */
    public RelicDef model3d() { RelicDef d = copy(); d.model3d = true; return d; }

    /** One to three sentences shown in the tooltip (the long card text lives in the lang file under item.nusmp.&lt;id&gt;.card). */
    public RelicDef desc(String shortText) { RelicDef d = copy(); d.desc = shortText == null ? "" : shortText; return d; }

    public RelicDef passive(Passive p) {
        if (p == null) return this;
        RelicDef d = copy();
        List<Passive> l = new ArrayList<>(passives);
        l.add(p);
        d.passives = List.copyOf(l);
        return d;
    }

    public RelicDef act(Act a) {
        if (a == null) return this;
        RelicDef d = copy();
        List<Act> l = new ArrayList<>(acts);
        l.add(a);
        d.acts = List.copyOf(l);
        return d;
    }

    /** Tensura engravings (the #tensura:engraving enchantments: "severance", "holy_weapon", ...; "ns:id" for another namespace), applied once. */
    public RelicDef engrave(String... tensuraEngravingIds) {
        if (tensuraEngravingIds == null || tensuraEngravingIds.length == 0) return this;
        RelicDef d = copy();
        List<String> l = new ArrayList<>(engravings);
        for (String e : tensuraEngravingIds) if (e != null && !e.isBlank() && !l.contains(e)) l.add(e);
        d.engravings = List.copyOf(l);
        return d;
    }

    /**
     * Weapons: {@code damage} is the total attack damage the tooltip shows (the player's own 1 included), {@code speed} the attacks per second
     * (a sword is 1.6); a speed of zero or less is read as vanilla's attack speed modifier instead (-2.4 is a sword).
     */
    public RelicDef melee(double damage, double speed) { RelicDef d = copy(); d.damage = damage; d.speed = speed; return d; }

    /** HEAD and CHEST relics: vanilla armour points and toughness of the piece. */
    public RelicDef armor(int defence, double toughness) { RelicDef d = copy(); d.defence = Math.max(0, defence); d.toughness = Math.max(0, toughness); return d; }

    /** Can be a Djinn's Metal Vessel. Only legal from LEGEND up; the catalog drops the flag (and logs it) on a lower grade. */
    public RelicDef vessel() { RelicDef d = copy(); d.vessel = true; return d; }

    /**
     * A remake of an item the mod already has: no new item is registered. The grade, tooltip, uniqueness, attunement, Curios slot, vessel flag and
     * the card's abilities are layered onto the existing item, whose id stays registered and whose art and behaviour stay unless the card supersedes them.
     */
    public RelicDef adopt(Supplier<? extends Item> existing) { RelicDef d = copy(); d.adopted = existing; return d; }

    /** 0xRRGGBB colours of the worn look and the glow: main material, trim, glow. */
    public RelicDef palette(int main, int trim, int glow) {
        RelicDef d = copy();
        d.main = main & 0xFFFFFF;
        d.trim = trim & 0xFFFFFF;
        d.glow = glow & 0xFFFFFF;
        d.paletteSet = true;
        return d;
    }

    public RelicDef look(Look look) { RelicDef d = copy(); d.look = look; return d; }

    /** 1.25.0: what kind of drawn weapon this MAIN_HAND relic is. A BOW or CROSSBOW registers as {@link RelicBowItem} / {@link RelicCrossbowItem}. */
    public enum RangedKind { NONE, BOW, CROSSBOW }

    /** A bow or crossbow: draws and shoots like the vanilla weapon (its acts go on sneak + use and on the worn-relic key). Needs the MAIN_HAND slot. */
    public RelicDef ranged(RangedKind kind) { RelicDef d = copy(); d.ranged = kind == null ? RangedKind.NONE : kind; return d; }

    /** Shorthand for {@code ranged(RangedKind.BOW)}: a bow; ship its draw-stage meshes as &lt;id&gt;_pull1 / _pull2 / _pull3 (docs/asset_pipeline.md). */
    public RelicDef bow() { return ranged(RangedKind.BOW); }

    /** Shorthand for {@code ranged(RangedKind.CROSSBOW)}: a crossbow; ship &lt;id&gt;_pull1..3 (loading) and &lt;id&gt;_charged (loaded). */
    public RelicDef crossbow() { return ranged(RangedKind.CROSSBOW); }

    /** The same def without the vessel flag (the catalog uses it on a def whose grade cannot carry a Djinn). */
    public RelicDef dropVessel() { RelicDef d = copy(); d.vessel = false; return d; }

    // ---------------------------------------------------------------- pairs (1.29.0)
    /** Suffix of the left half's registry id: the right half keeps the card's id (so old worlds load), the left is {@code <id>_left}. */
    public static final String LEFT_SUFFIX = "_left";

    /**
     * Declares this MAIN_HAND relic a PAIR: the def itself is the RIGHT half (its id stays the id the card always had), and the catalog adds the
     * LEFT half as the item {@code <id>_left} named {@code leftCardName}. The left half copies the grade, look, palette, melee numbers, engravings,
     * passives and acts of the right one (so either half can be the main-hand one), is a separate stack with its own world-unique ledger entry, and
     * needs the right half's attunement (attuning one half attunes the other). Both halves are obtained together (see docs/relic_pairs.md).
     */
    public RelicDef pair(String leftCardName) {
        if (slot != RelicSlot.MAIN_HAND) throw new IllegalArgumentException("only a MAIN_HAND relic can be a pair: " + id);
        RelicDef d = copy();
        d.pairLeftName = leftCardName == null || leftCardName.isBlank() ? cardName + " (Left)" : leftCardName;
        return d;
    }

    /** Is this def the right half of a pair (the one that declared {@link #pair})? */
    public boolean hasPair() { return pairLeftName != null; }

    /** Is this def the generated left half of a pair? */
    public boolean isPairLeft() { return pairRightId != null; }

    /** True for either half. */
    public boolean isPairHalf() { return hasPair() || isPairLeft(); }

    /** The id both halves share: the right half's own id. */
    public String pairBaseId() { return isPairLeft() ? pairRightId : id; }

    /** The id of the other half (left for the right half and the other way round), or null if this is no pair half. */
    public String pairPartnerId() { return isPairLeft() ? pairRightId : hasPair() ? id + LEFT_SUFFIX : null; }

    /** The left half's def, generated from this (right half) def; null if it is no pair. Its tooltip names the right half it needs. */
    public RelicDef pairLeft() {
        if (!hasPair()) return null;
        RelicDef d = copy();
        RelicDef left = new RelicDef(id + LEFT_SUFFIX, pairLeftName, grade, slot);
        left.attune = attune;
        left.model3d = model3d;
        left.vessel = false;
        left.paletteSet = paletteSet;
        left.desc = desc;
        left.passives = d.passives;
        left.acts = d.acts;
        left.engravings = d.engravings;
        left.damage = d.damage;
        left.speed = d.speed;
        left.main = main;
        left.trim = trim;
        left.glow = glow;
        left.look = look;
        left.pairRightId = id;
        return left;
    }

    // ---------------------------------------------------------------- getters
    public String id() { return id; }
    public String cardName() { return cardName; }
    public Grade grade() { return grade; }
    public RelicSlot slot() { return slot; }
    public boolean needsAttunement() { return attune; }
    public boolean hasModel3d() { return model3d; }
    public boolean isVessel() { return vessel; }
    public String desc() { return desc; }
    public List<Passive> passives() { return passives; }
    public List<Act> acts() { return acts; }
    public List<String> engravings() { return engravings; }
    public Supplier<? extends Item> adopted() { return adopted; }
    public boolean isAdopted() { return adopted != null; }
    public boolean hasPalette() { return paletteSet; }
    public RangedKind rangedKind() { return ranged; }
    public boolean isRanged() { return ranged != RangedKind.NONE; }
    public int paletteMain() { return main; }
    public int paletteTrim() { return trim; }
    public int paletteGlow() { return glow; }
    public Look look() { return look != null ? look : Look.defaultFor(slot); }

    /** Every relic is one of a kind in the world except the consumables. */
    public boolean isUnique() { return slot != RelicSlot.CONSUMABLE; }

    public Act firstAct() { return acts.isEmpty() ? null : acts.get(0); }

    public Act actByKey(String key) {
        for (Act a : acts) if (a.key().equals(key)) return a;
        return null;
    }

    /** Total attack damage of a weapon (the tooltip figure, the player's 1 included). */
    public double meleeDamage() { return Double.isNaN(damage) ? 3.0 + grade.tier() : Math.max(1.0, damage); }

    /** Attacks per second of a weapon. */
    public double meleeSpeed() { return 4.0 + meleeSpeedModifier(); }

    /** The attack speed attribute modifier a weapon carries (vanilla form: the player's base is 4.0, a sword is -2.4). */
    public double meleeSpeedModifier() {
        if (Double.isNaN(speed)) return -2.4;
        if (speed <= 0) return Math.max(-3.9, speed);
        return Math.max(-3.9, Math.min(0.0, speed - 4.0));
    }

    public int armorDefence() {
        if (defence >= 0) return defence;
        int chest = Math.min(8, 2 + grade.tier());
        return slot == RelicSlot.HEAD || slot == RelicSlot.FEET ? Math.max(1, Math.round(chest * 0.4f)) : chest;
    }

    public double armorToughness() { return toughness >= 0 ? toughness : Math.max(0.0, (grade.tier() - 2) * 0.75); }

    @Override
    public String toString() { return "RelicDef[" + id + " " + grade + " " + slot + "]"; }
}
